package io.github.tobyhs.redisflagd.di

import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Prototype
import kotlinx.coroutines.sync.Mutex

@Factory
class MutexFactory {
    @Prototype
    fun mutex(): Mutex = Mutex()
}
