package io.github.tobyhs.redisflagd

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Implementation of [FlagsRepository] that stores the flag configuration in a Redis hash
 */
@ExperimentalLettuceCoroutinesApi
@Singleton
class RedisFlagsRepository(
        private val redisConnection: StatefulRedisConnection<String, String>,
        private val mutex: Mutex
) : FlagsRepository {
    private var _flagConfiguration: String? = null

    override suspend fun getFlagConfiguration(): String {
        _flagConfiguration?.let { return it }
        mutex.withLock {
            _flagConfiguration?.let { return it }
            return refreshFlagConfiguration()
        }
    }

    override suspend fun refreshFlagConfiguration(): String {
        val keyValuePairs = redisConnection.coroutines().hgetall(FLAGS_KEY).toList()
        val newConfiguration = buildString {
            // This isn't proper, but JSON parsing each key-value pair and then JSON dumping everything back out felt
            // wasteful
            keyValuePairs.joinTo(this, separator = ",", prefix = "{", postfix = "}") { kv ->
                """"${kv.key}":${kv.value}"""
            }
        }
        _flagConfiguration = newConfiguration
        return newConfiguration
    }

    companion object {
        const val FLAGS_KEY = "flagd:flags"
    }
}
