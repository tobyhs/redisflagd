package io.github.tobyhs.redisflagd

import io.github.tobyhs.redisflagd.data.FlagsRepository
import io.github.tobyhs.redisflagd.data.FlagsUpdateSubscriber
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import sync.v1.SyncService
import sync.v1.SyncService.FetchAllFlagsRequest
import sync.v1.SyncService.SyncFlagsRequest
import sync.v1.SyncService.SyncFlagsResponse

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalKotest::class, ExperimentalStdlibApi::class)
class FlagSyncServiceTest : DescribeSpec({
    lateinit var flagsRepository: FlagsRepository
    lateinit var flagsUpdateSubscriber: FlagsUpdateSubscriber
    lateinit var service: FlagSyncService

    val initialFlagConfiguration = """{"flags": {}}"""

    beforeEach {
        flagsRepository = mockk()
        flagsUpdateSubscriber = mockk()
        service = FlagSyncService(flagsRepository, flagsUpdateSubscriber)

        coEvery { flagsRepository.getFlagConfiguration() } returns initialFlagConfiguration
    }

    describe("syncFlags").config(coroutineTestScope = true) {
        it("returns a flow that emits flag updates") {
            val updateFlow = MutableSharedFlow<SyncFlagsResponse>()
            every { flagsUpdateSubscriber.flow } returns updateFlow
            val responses = mutableListOf<SyncFlagsResponse>()
            val flowCollectJob = launch {
                service.syncFlags(SyncFlagsRequest.newBuilder().build()).collect(responses::add)
            }
            testCoroutineScheduler.advanceUntilIdle()

            val nextFlagConfiguration = """
                {
                    "flags": {
                        "myflag": {"state": "ENABLED", "variants": {"on": true, "off": false}, "defaultVariant": "on"}
                    }
                }""".trimIndent()
            val nextResponse = SyncFlagsResponse.newBuilder()
                    .setFlagConfiguration(nextFlagConfiguration)
                    .setState(SyncService.SyncState.SYNC_STATE_ALL)
                    .build()
            updateFlow.emit(nextResponse)
            testCoroutineScheduler.advanceUntilIdle()

            responses.shouldHaveSize(2)
            responses[0].flagConfiguration.shouldEqualJson(initialFlagConfiguration)
            responses[0].state.shouldBe(SyncService.SyncState.SYNC_STATE_ALL)
            responses[1].shouldBe(nextResponse)
            flowCollectJob.cancel()
        }
    }

    describe("fetchAllFlags") {
        it("returns all flags") {
            val response = service.fetchAllFlags(FetchAllFlagsRequest.newBuilder().build())
            response.flagConfiguration.shouldEqualJson(initialFlagConfiguration)
        }
    }
})
