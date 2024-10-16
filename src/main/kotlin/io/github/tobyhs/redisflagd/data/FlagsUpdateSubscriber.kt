package io.github.tobyhs.redisflagd.data

import kotlinx.coroutines.flow.SharedFlow

/**
 * Something that subscribes to flag configuration updates
 */
interface FlagsUpdateSubscriber {
    /** A [SharedFlow] that emits updated flag configurations */
    val flow: SharedFlow<String>
}
