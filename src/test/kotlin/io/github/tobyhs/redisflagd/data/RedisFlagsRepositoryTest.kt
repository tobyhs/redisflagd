package io.github.tobyhs.redisflagd.data

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.DescribeSpec
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.KeyValue
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex

@ExperimentalLettuceCoroutinesApi
@ExperimentalKotest
class RedisFlagsRepositoryTest : DescribeSpec({
    lateinit var redisCoroutines: RedisCoroutinesCommands<String, String>
    lateinit var redisConnection: StatefulRedisConnection<String, String>
    lateinit var repository: RedisFlagsRepository

    beforeEach {
        redisCoroutines = mockk()
        redisConnection = mockk()
        mockkStatic(StatefulRedisConnection<String, String>::coroutines)
        every { redisConnection.coroutines() } returns redisCoroutines
        repository = RedisFlagsRepository(redisConnection, Mutex())

        every { redisCoroutines.hgetall(RedisFlagsRepository.FLAGS_KEY) } returns flowOf(
                KeyValue.just("flag1", """{"state":"ENABLED","variants":{"on":true,"off":false},"defaultVariant":"on"}"""),
                KeyValue.just("flag2", """{"state":"ENABLED","variants":{"one":1,"two":2},"defaultVariant":"one"}"""),
        )
    }

    afterEach {
        unmockkStatic(StatefulRedisConnection<String, String>::coroutines)
    }

    fun checkFlagConfiguration(flagConfiguration: String) {
        flagConfiguration.shouldEqualJson(
                """{
                    "flags": {
                        "flag1": {"state":"ENABLED","variants":{"on":true,"off":false},"defaultVariant":"on"},
                        "flag2": {"state":"ENABLED","variants":{"one":1,"two":2},"defaultVariant":"one"}
                    }
                }"""
        )
    }

    describe("getFlagConfiguration") {
        it("returns the flag configuration and caches it") {
            repeat(2) { checkFlagConfiguration(repository.getFlagConfiguration()) }
            verify(exactly = 1) { redisCoroutines.hgetall(RedisFlagsRepository.FLAGS_KEY) }
        }

        context("when multiple coroutines grab the mutex") {
            lateinit var mutex: Mutex

            beforeEach {
                mutex = mockk()
                repository = RedisFlagsRepository(redisConnection, mutex)
            }

            it("only makes one Redis call") {
                val lockEntryBarrier: Channel<Unit> = Channel()
                val continuationMutex = Mutex(true)
                coEvery { mutex.lock(null) } coAnswers {
                    lockEntryBarrier.send(Unit)
                    continuationMutex.lock()
                }
                every { mutex.unlock(null) } answers { continuationMutex.unlock() }

                val numJobs = 2
                val jobs = List(numJobs) { async { repository.getFlagConfiguration() } }
                // Wait for all jobs to proceed past the first (unlocked) _flagConfiguration check
                repeat(numJobs) { lockEntryBarrier.receive() }
                continuationMutex.unlock()
                for (job in jobs) {
                    checkFlagConfiguration(job.await())
                }

                verify(exactly = 1) { redisCoroutines.hgetall(RedisFlagsRepository.FLAGS_KEY) }
            }
        }
    }

    describe("refreshFlagConfiguration") {
        it("refreshes the flag configuration and returns it") {
            repository.getFlagConfiguration()
            every { redisCoroutines.hgetall(RedisFlagsRepository.FLAGS_KEY) } returns flowOf()
            repository.refreshFlagConfiguration().shouldEqualJson("""{"flags":{}}""")
            repository.getFlagConfiguration().shouldEqualJson("""{"flags":{}}""")
            verify(exactly = 2) { redisCoroutines.hgetall(RedisFlagsRepository.FLAGS_KEY) }
        }
    }
})
