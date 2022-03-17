package net.bluewizardhat.common.cache

import java.time.Duration

/**
 * Return type for values that should be cached.
 */
data class ValueToCache<T>(
    /**
     * The value to cache.
     */
    val value: T,
    /**
     * After how long the object should expire from the cache. If expireAfter is less than 1 second
     * the value will not be cached.
     */
    val expireAfter: Duration,
    /**
     * After how long the object should be refreshed in the cache.
     */
    val refreshAfter: Duration? = null
)
