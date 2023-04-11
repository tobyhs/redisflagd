package io.github.tobyhs.redisflagd.data

import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.lettuce.core.api.push.PushListener
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import sync.v1.SyncService
import sync.v1.SyncService.SyncFlagsResponse

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class, ExperimentalKotest::class)
class RedisFlagsUpdateSubscriberTest : DescribeSpec({
    lateinit var flagsRepository: FlagsRepository
    lateinit var pubSubConnection: StatefulRedisPubSubConnection<String, String>
    val redisUri = "redis://test-redis:6379/2"
    lateinit var subscriber: RedisFlagsUpdateSubscriber

    beforeEach {
        flagsRepository = mockk()
        pubSubConnection = mockk()
    }

    describe("subscribe").config(coroutineTestScope = true) {
        it("adds a listener and subscribes") {
            val listeners = mutableListOf<PushListener>()
            every { pubSubConnection.addListener(any<PushListener>()) } answers { listeners.add(firstArg()) }
            val asyncCommands: RedisPubSubAsyncCommands<String, String> = mockk()
            every { pubSubConnection.async() } returns asyncCommands
            every { asyncCommands.subscribe(any()) } answers { mockk() }
            val flagConfiguration = """
                {
                    "flags": {
                        "myflag": {"state": "ENABLED", "variants": {"on": true, "off": false}, "defaultVariant": "on"}
                    }
                }""".trimIndent()
            coEvery { flagsRepository.refreshFlagConfiguration() } returns flagConfiguration

            subscriber = RedisFlagsUpdateSubscriber(flagsRepository, pubSubConnection, redisUri, this)
            val responses = mutableListOf<SyncFlagsResponse>()
            val flowCollectJob = launch {
                subscriber.flow.collect { r -> responses.add(r) }
            }

            subscriber.subscribe()
            verify { asyncCommands.subscribe("__keyspace@2__:${RedisFlagsRepository.FLAGS_KEY}") }
            listeners.shouldHaveSize(1)
            listeners.first().onPushMessage(mockk())
            testCoroutineScheduler.advanceUntilIdle()

            responses.shouldHaveSize(1)
            val response = responses.first()
            response.flagConfiguration.shouldBe(flagConfiguration)
            response.state.shouldBe(SyncService.SyncState.SYNC_STATE_ALL)
            flowCollectJob.cancel()
        }
    }
})
