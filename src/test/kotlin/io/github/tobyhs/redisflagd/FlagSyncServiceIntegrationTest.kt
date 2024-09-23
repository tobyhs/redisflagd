package io.github.tobyhs.redisflagd

import com.redis.testcontainers.RedisContainer
import dev.openfeature.flagd.grpc.sync.FlagSyncServiceGrpcKt
import dev.openfeature.flagd.grpc.sync.Sync.FetchAllFlagsRequest
import dev.openfeature.flagd.grpc.sync.Sync.SyncFlagsRequest
import dev.openfeature.flagd.grpc.sync.Sync.SyncFlagsResponse
import io.github.tobyhs.redisflagd.data.RedisFlagsRepository
import io.grpc.ManagedChannel
import io.kotest.assertions.fail
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.DescribeSpec
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Tags("Integration")
@MicronautTest
class FlagSyncServiceIntegrationTest : TestPropertyProvider, DescribeSpec({
    this as FlagSyncServiceIntegrationTest
    lateinit var redis: RedisCommands<String, String>
    lateinit var serviceStub: FlagSyncServiceGrpcKt.FlagSyncServiceCoroutineStub

    val flag1Value = """{"state": "ENABLED", "variants": {"on": true, "off": false}, "defaultVariant": "on"}"""

    beforeEach {
        redis = redisConnection.sync()
        redis.del(RedisFlagsRepository.FLAGS_KEY)
        redis.hset(RedisFlagsRepository.FLAGS_KEY, "flag1", flag1Value)
        serviceStub = FlagSyncServiceGrpcKt.FlagSyncServiceCoroutineStub(grpcChannel)
    }

    describe("syncFlags") {
        it("returns a stream of flag updates") {
            val responses = mutableListOf<SyncFlagsResponse>()
            val flowCollectJob = launch {
                serviceStub.syncFlags(SyncFlagsRequest.newBuilder().build()).collect(responses::add)
            }
            delayUntil { responses.size == 1 }

            val flag2Value = """{"state": "ENABLED", "variants": {"one": 1, "two": 2}, "defaultVariant": "one"}"""
            redis.hset(RedisFlagsRepository.FLAGS_KEY, "flag2", flag2Value)
            delayUntil { responses.size == 2 }

            responses[0].flagConfiguration.shouldEqualJson("""{"flags": {"flag1": ${flag1Value}}}""")
            responses[1].flagConfiguration.shouldEqualJson(
                    """{"flags": {"flag1": ${flag1Value}, "flag2": ${flag2Value}}}"""
            )
            flowCollectJob.cancel()
        }
    }

    describe("fetchAllFlags") {
        it("returns all flags") {
            val response = serviceStub.fetchAllFlags(FetchAllFlagsRequest.newBuilder().build())
            response.flagConfiguration.shouldEqualJson("""{"flags": {"flag1": ${flag1Value}}}""")
        }
    }
}) {
    private val redisContainer: RedisContainer = RedisContainer(RedisContainer.DEFAULT_IMAGE_NAME.withTag("7.0-alpine"))
            .withKeyspaceNotifications()

    @Inject
    private lateinit var redisConnection: StatefulRedisConnection<String, String>

    @Inject
    @field:GrpcChannel(GrpcServerChannel.NAME)
    private lateinit var grpcChannel: ManagedChannel

    override fun getProperties(): MutableMap<String, String> {
        if (!redisContainer.isRunning) {
            redisContainer.start()
        }
        return mutableMapOf("redis.uri" to redisContainer.redisURI)
    }

    override suspend fun afterSpec(spec: Spec) {
        super.afterSpec(spec)
        redisContainer.stop()
    }

    private suspend fun delayUntil(intervalMs: Long = 100, maxDelays: Int = 50, conditionAction: () -> Boolean) {
        var delays = 0
        while (!conditionAction()) {
            if (delays >= maxDelays) {
                fail("delayUntil condition failed after $maxDelays delays")
            }
            delay(intervalMs)
            delays++
        }
    }
}
