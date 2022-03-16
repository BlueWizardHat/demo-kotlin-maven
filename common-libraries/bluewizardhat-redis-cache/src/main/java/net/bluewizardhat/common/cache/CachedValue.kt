package net.bluewizardhat.common.cache

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import java.time.Duration
import java.time.OffsetDateTime

/**
 * Value that was cached along with info on when the value was cached and the expireAfter and refreshAfter values.
 */
@JsonInclude(NON_NULL)
data class CachedValue<T>(
    val expireAfter: Duration,
    val refreshAfter: Duration?,
    val value: T,
    val cacheTime: OffsetDateTime = OffsetDateTime.now()
) {
    constructor(valueToCache: ValueToCache<T>) : this(valueToCache.expireAfter, valueToCache.refreshAfter, valueToCache.value)
}
