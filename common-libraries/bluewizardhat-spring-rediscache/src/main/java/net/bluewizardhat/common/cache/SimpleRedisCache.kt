package net.bluewizardhat.common.cache

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration
import java.time.OffsetDateTime
import java.util.concurrent.Executor
import java.util.function.Supplier

/**
 * A simple redis cache implementation.
 *
 * * If a value is not found in the cache the supplier is called to fetch the value, the value will be written to cache
 * and the value is then returned.
 * * If a value is found in the cache, and it is newer than refreshAfter, the cached value will be returned.
 * * If a value is found in the cache, but it is older than refreshAfter, the cached value will be returned and the
 * supplier will be called asynchronously to refresh the value in the cache.
 *
 * If refreshAfter is longer than expireAfter or refreshAfter is null the object will simply expire without ever being refreshed.
 *
 * Note if the value is not fetched before it expires it will also not be refreshed. To keep a value cached it will need
 * to be fetched periodically.
 */
class SimpleRedisCache(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val pool: String,
    private val executor: Executor
) {
    private val log = KotlinLogging.logger {}

    private val valueOperations = redisTemplate.opsForValue()

    /**
     * Main caching mechanism.
     *
     * @param key cache key.
     * @param expireAfter after how long the object should expire from the cache.
     * @param refreshAfter after how long the object should be refreshed in the cache.
     * @param supplier is used to supply the value to return if not found in cache.
     */
    inline fun <reified T> cache(key: String, expireAfter: Duration, refreshAfter: Duration? = null, supplier: Supplier<T>): T =
        cache(key, expireAfter, refreshAfter, object : TypeReference<CachedValue<T>>() {}, supplier)

    /**
     * Updates or writes the cache without checking if the value already exists.
     */
    fun <T> cacheValue(key: String, expireAfter: Duration, refreshAfter: Duration? = null, value: T): T =
        writeToCacheBg("$pool:$key", expireAfter, refreshAfter, value)

    /**
     * Visible only so it can be called from inline function.
     * @see #cache(String, Duration, Duration, Supplier)
     */
    fun <T> cache(key: String, expireAfter: Duration, refreshAfter: Duration? = null, typeRef: TypeReference<CachedValue<T>>, supplier: Supplier<T>): T {
        val actualKey = "$pool:$key"
        return readFromCache(actualKey, expireAfter, refreshAfter, supplier, typeRef)
            ?: writeToCacheBg(actualKey, expireAfter, refreshAfter, supplier.get())
    }

    /**
     * Invalidates keys in the cache.
     */
    fun invalidate(vararg keys: String) {
        val actualKeys = keys.map { "$pool:$it" }
        log.debug { "Invalidating ['${actualKeys.joinToString("', '")}']" }
        redisTemplate.unlink(actualKeys)
    }

    /**
     * Invalidates all keys in a pool.
     */
    fun invalidateAll() {
        val keys = redisTemplate.keys("$pool:*")
        if (keys.isNotEmpty()) {
            log.debug { "Invalidating ['${keys.joinToString("', '")}']" }
            redisTemplate.unlink(keys)
        }
    }

    private fun <T> readFromCache(key: String, expireAfter: Duration, refreshAfter: Duration?, supplier: Supplier<T>, typeRef: TypeReference<CachedValue<T>>): T? {
        val serialized = valueOperations[key]
        if (serialized != null) {
            log.debug { "(Hit) Read '$key' from cache" }
            val cachedValue = objectMapper.readValue(serialized, typeRef)
            if (cachedValue.refreshAfter != null && OffsetDateTime.now().isAfter(cachedValue.refreshAfter)) {
                queueUpdate(key, expireAfter, refreshAfter, supplier, typeRef)
            }
            return cachedValue.value
        }
        log.debug { "(Miss) Key '$key' not found in cache" }
        return null
    }

    private fun <T> queueUpdate(key: String, expireAfter: Duration, refreshAfter: Duration?, supplier: Supplier<T>, typeRef: TypeReference<CachedValue<T>>) {
        doWithLock("$key.lock.refresh") {
            log.debug { "Queueing '$key' for refresh" }
            executor.execute {
                try {
                    Thread.sleep(250) // ensure the parent thread has enough time to release the lock
                    doWithLock("$key.lock.refresh") {
                        val valueRefreshAfter = valueOperations[key]?.let {
                            objectMapper.readValue(it, typeRef)
                        }?.refreshAfter
                        if (valueRefreshAfter == null || OffsetDateTime.now().isAfter(valueRefreshAfter)) {
                            writeToCache(key, expireAfter, refreshAfter, supplier.get())
                        }
                    }
                } catch (e: Throwable) {
                    log.error(e) { "Error while updating cache for '$key': ${e.message}" }
                }
            }
        }
    }

    private fun <T> writeToCacheBg(key: String, expireAfter: Duration, refreshAfter: Duration?, value: T): T {
        executor.execute {
            writeToCache(key, expireAfter, refreshAfter, value)
        }
        return value
    }

    private fun <T> writeToCache(key: String, expireAfter: Duration, refreshAfter: Duration?, value: T) {
        if (value != null) {
            doWithLock("$key.lock") {
                log.debug { "Writing '$key' to cache, expires after $expireAfter" }
                val cachedValue = CachedValue(value, if (refreshAfter != null) OffsetDateTime.now().plus(refreshAfter) else null)
                valueOperations.set(key, objectMapper.writeValueAsString(cachedValue), expireAfter)
            }
        }
    }

    private fun doWithLock(lock: String, task: Runnable) {
        val lockAcquired = valueOperations.setIfAbsent(lock, "true", Duration.ofMinutes(5)) ?: false
        if (lockAcquired) {
            log.debug { "Acquired lock '$lock'" }
            try {
                task.run()
            } finally {
                log.debug { "Releasing lock '$lock'" }
                redisTemplate.unlink(lock)
            }
        } else {
            log.debug { "Could not acquire lock '$lock', unable to update cache" }
        }
    }

    companion object {
        data class CachedValue<T>(
            val value: T,
            val refreshAfter: OffsetDateTime?
        )
    }
}