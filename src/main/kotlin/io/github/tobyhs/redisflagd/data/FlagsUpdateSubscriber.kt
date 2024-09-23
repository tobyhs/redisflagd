package io.github.tobyhs.redisflagd.data

import dev.openfeature.flagd.grpc.sync.Sync.SyncFlagsResponse
import kotlinx.coroutines.flow.SharedFlow

/**
 * Something that subscribes to flag configuration updates
 */
interface FlagsUpdateSubscriber {
    /** A [SharedFlow] that emits flag configuration updates */
    val flow: SharedFlow<SyncFlagsResponse>
}
