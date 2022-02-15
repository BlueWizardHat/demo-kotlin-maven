package net.bluewizardhat.common.cache

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.OffsetDateTime
import java.util.function.Supplier

@Component
class SimpleRedisCacheFactory(
    private val redisTemplate: RedisTemplate<Any, Any>,
    private val objectMapper: ObjectMapper
) {
    fun forPool(pool: String) = SimpleRedisCache(redisTemplate, objectMapper, pool)
}

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
    private val redisTemplate: RedisTemplate<Any, Any>,
    private val objectMapper: ObjectMapper,
    private val pool: String
) {
    private val log = KotlinLogging.logger {}

    private val valueOperations = redisTemplate.opsForValue() as ValueOperations<String, Any>

    /**
     * Main caching mechanism.
     *
     * @param key cache key.
     * @param expireAfter after how long the object should expire from the cache.
     * @param refreshAfter after how long the object should be refreshed in the cache.
     * @param supplier is used to supply the value to return if not found in cache.
     */
    inline fun <reified T> cached(key: String, expireAfter: Duration, refreshAfter: Duration? = null, supplier: Supplier<T>): T =
        cached(key, expireAfter, refreshAfter, object : TypeReference<CachedValue<T>>() {}, supplier)

    fun <T> cached(key: String, expireAfter: Duration, refreshAfter: Duration? = null, typeRef: TypeReference<CachedValue<T>>, supplier: Supplier<T>): T {
        var value: CachedValue<T>? = readFromCache(key, typeRef)
        if (value == null) {
            value = writeToCache(key, expireAfter, refreshAfter, supplier.get())
        }
        return value.value
    }

    /**
     * Invalidates keys in the cache.
     */
    fun invalidate(vararg keys: String) {
        val actualKeys = keys.map { "$pool:$it" }
        log.debug { "Invalidating [${actualKeys.joinToString(", ")}]" }
        valueOperations.operations.unlink(actualKeys)
    }

    /**
     * Invalidates all keys in a pool.
     */
    fun invalidateAll() {
        // TODO
    }

    private fun <T> readFromCache(key: String, typeRef: TypeReference<CachedValue<T>>): CachedValue<T>? {
        val actualKey = "$pool:$key"
        val serialized = valueOperations[actualKey] as String?
        if (serialized != null) {
            log.debug { "(Hit) Read '$actualKey' from cache" }
            val cachedValue = objectMapper.readValue(serialized, typeRef)
            if (cachedValue.refreshAfter != null && OffsetDateTime.now().isAfter(cachedValue.refreshAfter)) {
                log.debug { "Queueing '$actualKey' for refresh" }
                // TODO queue value for refresh
            }
            return cachedValue
        }
        log.debug { "(Miss) Key '$actualKey' not found in cache" }
        return null
    }

    private fun <T> writeToCache(key: String, expireAfter: Duration, refreshAfter: Duration?, value: T): CachedValue<T> {
        val actualKey = "$pool:$key"
        val cachedValue = CachedValue(value, if (refreshAfter != null) OffsetDateTime.now().plus(refreshAfter) else null)
        if (value != null) {
            val lock = "$actualKey.lock"
            val lockAcquired = valueOperations.setIfAbsent(lock, "true") ?: false
            if (lockAcquired) {
                log.debug { "Acquired lock '$lock'" }
                try {
                    log.debug { "Writing '$actualKey' to cache, expires after $expireAfter" }
                    valueOperations.set(actualKey, objectMapper.writeValueAsString(cachedValue), expireAfter)
                } finally {
                    log.debug { "Releasing lock '$lock'" }
                    redisTemplate.unlink(lock)
                }
            } else {
                log.debug { "Could not acquire lock '$lock', unable to update cache" }
            }
        }
        return cachedValue
    }

    companion object {
        data class CachedValue<T>(
            val value: T,
            val refreshAfter: OffsetDateTime?
        )
    }
}
