package io.github.tobyhs.redisflagd.data

import io.github.tobyhs.redisflagd.di.AppCoroutineScope
import io.lettuce.core.RedisURI
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.micronaut.context.annotation.Property
import jakarta.annotation.PostConstruct
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/**
 * @see subscribe
 */
@Singleton
class RedisFlagsUpdateSubscriber(
        private val flagsRepository: FlagsRepository,
        private val pubSubConnection: StatefulRedisPubSubConnection<String, String>,
        @Property(name = "redis.uri") private val redisUri: String,
        @AppCoroutineScope private val appScope: CoroutineScope,
) : FlagsUpdateSubscriber {
    private val _flow = MutableSharedFlow<String>()
    override val flow: SharedFlow<String> = _flow

    /**
     * Subscribes to the Redis keyspace notification channels for flag configuration updates
     */
    @PostConstruct
    fun subscribe() {
        pubSubConnection.addListener {
            appScope.launch {
                _flow.emit(flagsRepository.refreshFlagConfiguration())
            }
        }
        val db = RedisURI.create(redisUri).database
        pubSubConnection.async().subscribe("__keyspace@${db}__:${RedisFlagsRepository.FLAGS_KEY}")
    }
}
