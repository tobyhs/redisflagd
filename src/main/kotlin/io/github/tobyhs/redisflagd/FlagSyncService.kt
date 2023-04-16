package io.github.tobyhs.redisflagd

import io.github.tobyhs.redisflagd.data.FlagsRepository
import io.github.tobyhs.redisflagd.data.FlagsUpdateSubscriber
import io.micronaut.grpc.annotation.GrpcService
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import sync.v1.FlagSyncServiceGrpcKt
import sync.v1.SyncService.FetchAllFlagsRequest
import sync.v1.SyncService.FetchAllFlagsResponse
import sync.v1.SyncService.SyncFlagsRequest
import sync.v1.SyncService.SyncFlagsResponse
import sync.v1.SyncService.SyncState

/**
 * Implementation of flagd's FlagSyncService that uses Redis
 */
@GrpcService
@Singleton
class FlagSyncService(
        private val flagsRepository: FlagsRepository,
        private val flagsUpdateSubscriber: FlagsUpdateSubscriber,
) : FlagSyncServiceGrpcKt.FlagSyncServiceCoroutineImplBase() {
    override fun syncFlags(request: SyncFlagsRequest): Flow<SyncFlagsResponse> = flow {
        val firstResult = SyncFlagsResponse.newBuilder()
                .setFlagConfiguration(flagsRepository.getFlagConfiguration())
                .setState(SyncState.SYNC_STATE_ALL)
                .build()
        emit(firstResult)
        emitAll(flagsUpdateSubscriber.flow)
    }

    override suspend fun fetchAllFlags(request: FetchAllFlagsRequest): FetchAllFlagsResponse {
        return FetchAllFlagsResponse.newBuilder().setFlagConfiguration(flagsRepository.getFlagConfiguration()).build()
    }
}
