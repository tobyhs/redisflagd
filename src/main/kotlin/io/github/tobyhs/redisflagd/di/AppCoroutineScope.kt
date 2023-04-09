package io.github.tobyhs.redisflagd.di

import jakarta.inject.Qualifier

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
internal annotation class AppCoroutineScope
