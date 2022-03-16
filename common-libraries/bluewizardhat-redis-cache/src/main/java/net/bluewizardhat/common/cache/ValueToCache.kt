package net.bluewizardhat.common.cache

import java.time.Duration

/**
 * Return type for values that should be cached.
 */
data class ValueToCache<T>(
    val value: T,
    val expireAfter: Duration,
    val refreshAfter: Duration? = null
)
