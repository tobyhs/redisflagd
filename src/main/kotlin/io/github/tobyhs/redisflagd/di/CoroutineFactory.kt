package io.github.tobyhs.redisflagd.di

import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@Factory
internal class CoroutineFactory {
    @AppCoroutineScope
    @Singleton
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob())
}
