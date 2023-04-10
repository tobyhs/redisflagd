package io.github.tobyhs.redisflagd.data

import io.github.tobyhs.redisflagd.di.AppCoroutineScope
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import jakarta.annotation.PostConstruct
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import sync.v1.SyncService
import sync.v1.SyncService.SyncFlagsResponse

/**
 * @see subscribe
 */
@Singleton
class RedisFlagsUpdateSubscriber(
        private val flagsRepository: FlagsRepository,
        private val pubSubConnection: StatefulRedisPubSubConnection<String, String>,
        @AppCoroutineScope private val appScope: CoroutineScope,
) : FlagsUpdateSubscriber {
    private val _flow = MutableSharedFlow<SyncFlagsResponse>()
    override val flow: SharedFlow<SyncFlagsResponse> = _flow

    /**
     * Subscribes to the Redis keyspace notification channels for flag configuration updates
     */
    @PostConstruct
    fun subscribe() {
        pubSubConnection.addListener {
            appScope.launch {
                val flagConfiguration = flagsRepository.refreshFlagConfiguration()
                val response = SyncFlagsResponse.newBuilder()
                        .setFlagConfiguration(flagConfiguration)
                        .setState(SyncService.SyncState.SYNC_STATE_ALL)
                        .build()
                _flow.emit(response)
            }
        }
        pubSubConnection.async().psubscribe("__keyspace@*__:${RedisFlagsRepository.FLAGS_KEY}")
    }
}
