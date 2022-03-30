package net.bluewizardhat.common.cache

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import java.time.Duration
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executor
import java.util.function.Supplier
import javax.servlet.http.HttpServletResponse

/**
 * A simple redis cache implementation.
 *
 * * If a value is not found in the cache the supplier is called to fetch the value, the value will be written to cache
 * asynchronously and the value is returned.
 * * If a value is found in the cache, and it is newer than refreshAfter, the cached value will be returned.
 * * If a value is found in the cache, but it is older than refreshAfter, the cached value will be returned and the
 * supplier will be called asynchronously to refresh the value in the cache.
 *
 * If refreshAfter is longer than expireAfter or refreshAfter is null the object will simply expire without ever being
 * refreshed.
 *
 * Note if the value is not requested before it expires it will also not be refreshed asynchronously. To keep a value
 * cached it will need to be requested periodically.
 */
sealed class SimpleRedisCache(
    private val redisAdapter: RedisAdapter,
    private val objectMapper: ObjectMapper,
    private val pool: String,
    private val lockDuration: Duration,
    private val executor: Executor
) {
    private val log = KotlinLogging.logger {}

    /**
     * Main caching mechanism.
     *
     * @param key cache key.
     * @param expireAfter after how long the object should expire from the cache.
     * @param refreshAfter after how long the object should be refreshed in the cache.
     * @param supplier is used to supply the value to return if not found in cache.
     */
    inline fun <reified T> cache(key: String, expireAfter: Duration, refreshAfter: Duration? = null, supplier: Supplier<T>): T =
        cachedValue(key, expireAfter, refreshAfter, object : TypeReference<CachedValue<T>>() {}, supplier).value

    /**
     * Alternate caching mechanism where you get the value along with info on when the value was cached and the
     * expireAfter and refreshAfter values.
     *
     * @param key cache key.
     * @param expireAfter after how long the object should expire from the cache. If expireAfter is less than 1 second
     * the value will not be cached.
     * @param refreshAfter after how long the object should be refreshed in the cache.
     * @param supplier is used to supply the value to return if not found in cache.
     */
    inline fun <reified T> cachedValue(key: String, expireAfter: Duration, refreshAfter: Duration? = null, supplier: Supplier<T>): CachedValue<T> =
        cachedValue(key, expireAfter, refreshAfter, object : TypeReference<CachedValue<T>>() {}, supplier)

    /**
     * Main caching mechanism for specifying expireAfter and refreshAfter in the supplier.
     *
     * @param key cache key.
     * @param supplier is used to supply the value to return if not found in cache.
     */
    inline fun <reified T> cache(key: String, supplier: Supplier<ValueToCache<T>>): T =
        cachedValue(key, object : TypeReference<CachedValue<T>>() {}, supplier).value

    /**
     * Alternate caching mechanism where you get the value along with info on when the value was cached and the
     * expireAfter and refreshAfter values.
     *
     * @param key cache key.
     * @param supplier is used to supply the value to return if not found in cache.
     */
    inline fun <reified T> cachedValue(key: String, supplier: Supplier<ValueToCache<T>>): CachedValue<T> =
        cachedValue(key, object : TypeReference<CachedValue<T>>() {}, supplier)

    /**
     * Invalidates keys in the cache.
     */
    fun invalidate(vararg keys: String) {
        val actualKeys = keys.map { "$pool:$it" }
        log.debug { "Invalidating ['${actualKeys.joinToString("', '")}']" }
        redisAdapter.unlink(actualKeys)
    }

    /**
     * Invalidates all keys in a pool.
     */
    fun invalidateAll() {
        val keys = redisAdapter.keys("$pool:*")
        if (keys.isNotEmpty()) {
            log.debug { "Invalidating ['${keys.joinToString("', '")}']" }
            redisAdapter.unlink(keys)
        }
    }

    /**
     * Visible only so it can be called from inline function.
     * @see #cache(String, Duration, Duration?, Supplier)
     */
    abstract fun <T> cachedValue(key: String, expireAfter: Duration, refreshAfter: Duration? = null, typeRef: TypeReference<CachedValue<T>>, supplier: Supplier<T>): CachedValue<T>

    /**
     * Visible only so it can be called from inline function.
     * @see #cachedValue(String, Supplier)
     */
    abstract fun <T> cachedValue(key: String, typeRef: TypeReference<CachedValue<T>>, supplier: Supplier<ValueToCache<T>>): CachedValue<T>

    protected fun <T> cacheInternal(key: String, expireAfter: Duration, refreshAfter: Duration?, typeRef: TypeReference<CachedValue<T>>, supplier: Supplier<T>): CachedValue<T> {
        val actualKey = "$pool:$key"
        return readFromCache(actualKey, refreshAfter, { ValueToCache(supplier.get(), expireAfter, refreshAfter) }, typeRef)
            ?: writeToCacheBg(actualKey, CachedValue(expireAfter, refreshAfter, supplier.get()))
    }

    protected fun <T> cacheInternal(key: String, typeRef: TypeReference<CachedValue<T>>, supplier: Supplier<ValueToCache<T>>): CachedValue<T> {
        val actualKey = "$pool:$key"
        return readFromCache(actualKey, null, supplier, typeRef)
            ?: writeToCacheBg(actualKey, CachedValue(supplier.get()))
    }

    private fun <T> readFromCache(key: String, refreshAfter: Duration?, supplier: Supplier<ValueToCache<T>>, typeRef: TypeReference<CachedValue<T>>): CachedValue<T>? {
        val serialized: String? = redisAdapter.get(key)
        if (serialized != null) {
            log.debug { "(Hit) Read '$key' from cache" }
            val cachedValue = objectMapper.readValue(serialized, typeRef)
            val refreshAfterActual = refreshAfter ?: cachedValue.refreshAfter
            if (refreshAfterActual != null && OffsetDateTime.now().isAfter(cachedValue.cacheTime.plus(refreshAfterActual))) {
                queueUpdate(key, refreshAfterActual, supplier, typeRef)
            }
            return cachedValue
        }
        log.debug { "(Miss) Key '$key' not found in cache" }
        return null
    }

    private fun <T> queueUpdate(key: String, refreshAfter: Duration, supplier: Supplier<ValueToCache<T>>, typeRef: TypeReference<CachedValue<T>>) {
        log.debug { "Queueing '$key' for refresh" }
        executor.execute {
            try {
                doWithLock("$key.refresh.lock") {
                    val cacheTime = redisAdapter.get(key)?.let { objectMapper.readValue(it, typeRef).cacheTime }
                    if (cacheTime == null || OffsetDateTime.now().isAfter(cacheTime.plus(refreshAfter))) {
                        val valueToCache = supplier.get()
                        writeToCache(key, valueToCache.expireAfter, CachedValue(valueToCache))
                    }
                }
            } catch (e: Throwable) {
                log.error(e) { "Error while updating cache for '$key': ${e.message}" }
            }
        }
    }

    private fun <T> writeToCacheBg(key: String, value: CachedValue<T>): CachedValue<T> {
        executor.execute {
            try {
                writeToCache(key, value.expireAfter, value)
            } catch (e: Throwable) {
                log.error(e) { "Error while updating cache for '$key': ${e.message}" }
            }
        }
        return value
    }

    private fun <T> writeToCache(key: String, expireAfter: Duration, cachedValue: CachedValue<T>) {
        if (expireAfter.seconds > 0) {
            doWithLock("$key.lock") {
                log.debug { "Writing '$key' to cache, expires after $expireAfter" }
                redisAdapter.set(key, objectMapper.writeValueAsString(cachedValue), expireAfter)
            }
        } else {
            log.info { "Skip writing '$key' to cache as expireAfter is less than 1 second ($expireAfter)" }
        }
    }

    private fun doWithLock(lock: String, task: Runnable) {
        val lockAcquired = redisAdapter.setIfAbsent(lock, "true", lockDuration) ?: false
        if (lockAcquired) {
            log.debug { "Acquired lock '$lock'" }
            try {
                task.run()
            } finally {
                log.debug { "Releasing lock '$lock'" }
                redisAdapter.unlink(lock)
            }
        } else {
            log.warn { "Could not acquire lock '$lock', unable to update cache" }
        }
    }
}

/**
 * Concrete implementation of SimpleRedisCache because sealed classes are abstract.
 */
class SimpleRedisCacheBasic(
    redisAdapter: RedisAdapter,
    objectMapper: ObjectMapper,
    pool: String,
    lockDuration: Duration,
    executor: Executor
) : SimpleRedisCache(redisAdapter, objectMapper, pool, lockDuration, executor) {
    override fun <T> cachedValue(key: String, expireAfter: Duration, refreshAfter: Duration?, typeRef: TypeReference<CachedValue<T>>, supplier: Supplier<T>): CachedValue<T> =
        cacheInternal(key, expireAfter, refreshAfter, typeRef, supplier)

    override fun <T> cachedValue(key: String, typeRef: TypeReference<CachedValue<T>>, supplier: Supplier<ValueToCache<T>>): CachedValue<T> =
        cacheInternal(key, typeRef, supplier)
}

/**
 * Extension of SimpleRedisCache that provides the ability to set http cache headers.
 */
class SimpleRedisCacheWeb(
    private val redisAdapter: RedisAdapter,
    private val objectMapper: ObjectMapper,
    private val pool: String,
    private val lockDuration: Duration,
    private val executor: Executor
) : SimpleRedisCache(redisAdapter, objectMapper, pool, lockDuration, executor) {
    override fun <T> cachedValue(key: String, expireAfter: Duration, refreshAfter: Duration?, typeRef: TypeReference<CachedValue<T>>, supplier: Supplier<T>): CachedValue<T> =
        cacheInternal(key, expireAfter, refreshAfter, typeRef, supplier)

    override fun <T> cachedValue(key: String, typeRef: TypeReference<CachedValue<T>>, supplier: Supplier<ValueToCache<T>>): CachedValue<T> =
        cacheInternal(key, typeRef, supplier)

    /**
     * Set cache-control header to limit external caching. When using NoCacheDirectives the header is set
     * immediately.
     */
    fun cacheControl(response: HttpServletResponse, vararg directives: NoCacheDirectives): SimpleRedisCache {
        if (directives.isEmpty()) {
            throw IllegalArgumentException("directives for cache-control header should not be empty")
        }
        response.addHeader("Cache-Control", directives.joinToString(", ") { it.value })
        return this
    }

    /**
     * Set cache-control headers for external caches.
     *
     * Note the redis cache cannot invalidate external caches, so for data you may need to invalidate
     * it might be good idea to add non-caching directives instead of caching ones.
     */
    fun cacheControl(response: HttpServletResponse, vararg directives: CacheDirectives): SimpleRedisCache {
        if (directives.isEmpty()) {
            throw IllegalArgumentException("directives for cache-control header should not be empty")
        }
        return SimpleRedisCacheHeaders(redisAdapter, objectMapper, pool, lockDuration, executor, response, directives)
    }

    private class SimpleRedisCacheHeaders(
        redisAdapter: RedisAdapter,
        objectMapper: ObjectMapper,
        pool: String,
        lockDuration: Duration,
        executor: Executor,
        private val response: HttpServletResponse,
        private val directives: Array<out CacheDirectives>
    ) : SimpleRedisCache(redisAdapter, objectMapper, pool, lockDuration, executor) {
        override fun <T> cachedValue(key: String, expireAfter: Duration, refreshAfter: Duration?, typeRef: TypeReference<CachedValue<T>>, supplier: Supplier<T>): CachedValue<T> {
            val cachedValue = cacheInternal(key, expireAfter, refreshAfter, typeRef, supplier)
            val age = ChronoUnit.SECONDS.between(cachedValue.cacheTime, OffsetDateTime.now())
            response.addHeader("Cache-Control", directives.joinToString(", ") { it.value(expireAfter.seconds, age, refreshAfter?.seconds) })
            return cachedValue
        }

        override fun <T> cachedValue(key: String, typeRef: TypeReference<CachedValue<T>>, supplier: Supplier<ValueToCache<T>>): CachedValue<T> {
            val cachedValue = cacheInternal(key, typeRef, supplier)
            val age = ChronoUnit.SECONDS.between(cachedValue.cacheTime, OffsetDateTime.now())
            response.addHeader("Cache-Control", directives.joinToString(", ") { it.value(cachedValue.expireAfter.seconds, age, cachedValue.refreshAfter?.seconds) })
            return cachedValue
        }
    }

    /**
     * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cache-Control
     */
    enum class NoCacheDirectives(val value: String) {
        /** Adds the "no-cache" directive */
        NoCache("no-cache"),
        /** Adds the "no-store" directive */
        NoStore("no-store"),
        /** Adds the "max-age" directive with a value of 0 */
        MaxAge0("max-age=0"),
        /** Adds the "s-maxage" directive with a value of 0 */
        SMaxAge0("s-maxage=0"),
        /** Adds the "must-revalidate" directive */
        MustRevalidate("must-revalidate")
    }
    /**
     * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cache-Control
     */
    enum class CacheDirectives {
        /** Adds the "max-age" directive with a value of expireAfter */
        MaxAgeExpireAfter {
            override fun value(expireAfter: Long, age: Long, refreshAfter: Long?) = "max-age=$expireAfter"
        },
        /** Adds the "max-age" directive with a value of expireAfter minus age of the cached object */
        MaxAgeExpireAfterAged {
            override fun value(expireAfter: Long, age: Long, refreshAfter: Long?) = "max-age=${expireAfter - age}"
        },
        /** Adds the "max-age" directive with a value of refreshAfter */
        MaxAgeRefreshAfter {
            override fun value(expireAfter: Long, age: Long, refreshAfter: Long?) =
                refreshAfter?.let { "max-age=$refreshAfter" }
                    ?: throw IllegalArgumentException("When using MaxAgeRefreshAfter refreshAfter cannot be null")
        },
        /** Adds the "max-age" directive with a value of refreshAfter minus age of the cached object */
        MaxAgeRefreshAfterAged {
            override fun value(expireAfter: Long, age: Long, refreshAfter: Long?) =
                refreshAfter?.let { "max-age=${it - age}" }
                    ?: throw IllegalArgumentException("When using MaxAgeRefreshAfterAged refreshAfter cannot be null")
        },
        /** Adds the "s-maxage" directive with a value of expireAfter */
        SMaxAgeExpireAfter {
            override fun value(expireAfter: Long, age: Long, refreshAfter: Long?) = "s-maxage=$expireAfter"
        },
        /** Adds the "s-maxage" directive with a value of expireAfter minus age of the cached object */
        SMaxAgeExpireAfterAged {
            override fun value(expireAfter: Long, age: Long, refreshAfter: Long?) = "s-maxage=${expireAfter - age}"
        },
        /** Adds the "s-maxage" directive with a value of refreshAfter */
        SMaxAgeRefreshAfter {
            override fun value(expireAfter: Long, age: Long, refreshAfter: Long?) =
                refreshAfter?.let { "s-maxage=$refreshAfter" }
                    ?: throw IllegalArgumentException("When using SMaxAgeRefreshAfter refreshAfter cannot be null")
        },
        /** Adds the "s-maxage" directive with a value of refreshAfter minus age of the cached object */
        SMaxAgeRefreshAfterAged {
            override fun value(expireAfter: Long, age: Long, refreshAfter: Long?) =
                refreshAfter?.let { "s-maxage=${it - age}" }
                    ?: throw IllegalArgumentException("When using SMaxAgeRefreshAfterAged refreshAfter cannot be null")
        },
        /** Adds the "must-revalidate" directive */
        MustRevalidate {
            override fun value(expireAfter: Long, age: Long, refreshAfter: Long?) = "must-revalidate"
        },
        /** Adds the "proxy-revalidate" directive */
        ProxyRevalidate {
            override fun value(expireAfter: Long, age: Long, refreshAfter: Long?) = "proxy-revalidate"
        },
        /** Adds the "private" directive */
        Private {
            override fun value(expireAfter: Long, age: Long, refreshAfter: Long?) = "private"
        },
        /** Adds the "public" directive */
        Public {
            override fun value(expireAfter: Long, age: Long, refreshAfter: Long?) = "public"
        },
        /** Adds the "no-transform" directive */
        NoTransform {
            override fun value(expireAfter: Long, age: Long, refreshAfter: Long?) = "no-transform"
        },
        /** Adds the "immutable" directive */
        Immutable {
            override fun value(expireAfter: Long, age: Long, refreshAfter: Long?) = "immutable"
        }
        ;

        abstract fun value(expireAfter: Long, age: Long, refreshAfter: Long?): String
    }
}
