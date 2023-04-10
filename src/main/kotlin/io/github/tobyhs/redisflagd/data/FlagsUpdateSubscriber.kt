package io.github.tobyhs.redisflagd.data

import kotlinx.coroutines.flow.SharedFlow
import sync.v1.SyncService.SyncFlagsResponse

/**
 * Something that subscribes to flag configuration updates
 */
interface FlagsUpdateSubscriber {
    /** A [SharedFlow] that emits flag configuration updates */
    val flow: SharedFlow<SyncFlagsResponse>
}
