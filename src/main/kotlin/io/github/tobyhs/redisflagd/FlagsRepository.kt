package io.github.tobyhs.redisflagd

/**
 * Repository to retrieve flag configuration
 */
interface FlagsRepository {
    /**
     * @return the flag configuration in JSON serialized form
     */
    suspend fun getFlagConfiguration(): String

    /**
     * Refreshes the (possibly cached) flag configuration
     *
     * @return the flag configuration in JSON serialized form
     */
    suspend fun refreshFlagConfiguration(): String
}
