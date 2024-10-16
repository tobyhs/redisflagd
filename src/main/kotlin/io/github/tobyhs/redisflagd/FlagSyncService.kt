package io.github.tobyhs.redisflagd

import dev.openfeature.flagd.grpc.sync.FlagSyncServiceGrpcKt
import dev.openfeature.flagd.grpc.sync.Sync.FetchAllFlagsRequest
import dev.openfeature.flagd.grpc.sync.Sync.FetchAllFlagsResponse
import dev.openfeature.flagd.grpc.sync.Sync.SyncFlagsRequest
import dev.openfeature.flagd.grpc.sync.Sync.SyncFlagsResponse
import io.github.tobyhs.redisflagd.data.FlagsRepository
import io.github.tobyhs.redisflagd.data.FlagsUpdateSubscriber
import io.micronaut.grpc.annotation.GrpcService
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

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
            .build()
        emit(firstResult)
        emitAll(flagsUpdateSubscriber.flow.map { flagConfiguration ->
            SyncFlagsResponse.newBuilder().setFlagConfiguration(flagConfiguration).build()
        })
    }

    override suspend fun fetchAllFlags(request: FetchAllFlagsRequest): FetchAllFlagsResponse {
        return FetchAllFlagsResponse.newBuilder().setFlagConfiguration(flagsRepository.getFlagConfiguration()).build()
    }
}
