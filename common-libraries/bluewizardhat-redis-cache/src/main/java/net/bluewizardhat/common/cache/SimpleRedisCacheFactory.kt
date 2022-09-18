package net.bluewizardhat.common.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import mu.KotlinLogging
import java.time.Duration
import java.util.concurrent.Executor
import java.util.function.Supplier

/**
 * Factory for SimpleRedisCache.
 */
sealed class SimpleRedisCacheFactory(
    private val defaultExecutor: Supplier<Executor>
) {
    private val log = KotlinLogging.logger {}

    private var defaultObjectMapper: ObjectMapper? = null

    /**
     * Creates a SimpleRedisCache for a pool. A pool is basically a prefix to group all keys in the cache on.
     * Optionally it is possible to supply an Executor that is used to refresh values in the background,
     * if no Executor is supplied a default ThreadPoolTaskExecutor will be used.
     *
     * The default ThreadPoolTaskExecutor will carry over logging context, but not other things like security
     * context, etc. If the thread refreshing the cache need more than logging context or more advanced behaviour
     * you should supply your own Executor that can handle this.
     */
    abstract fun forPool(
        pool: String,
        lockDuration: Duration = Duration.ofMinutes(5),
        objectMapper: ObjectMapper = defaultObjectMapper(),
        executor: Executor = defaultExecutor.get()
    ): SimpleRedisCache

    @Synchronized
    protected fun defaultObjectMapper(): ObjectMapper {
        var objectMapper = defaultObjectMapper
        if (objectMapper == null) {
            objectMapper = ObjectMapper()
                .registerModule(JavaTimeModule())
                .registerModule(ParameterNamesModule())
                .registerModule(KotlinModule.Builder().build())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // ISO timestamps please
            defaultObjectMapper = objectMapper
            log.debug { "Initialized default ObjectMapper" }
        }
        return objectMapper!!
    }
}

/**
 * Concrete implementation of SimpleRedisCacheFactory because sealed classes are abstract.
 */
class SimpleRedisCacheFactoryBasic(
    defaultExecutor: Supplier<Executor>,
    private val redisAdapter: RedisAdapter
) : SimpleRedisCacheFactory(defaultExecutor) {
    override fun forPool(pool: String, lockDuration: Duration, objectMapper: ObjectMapper, executor: Executor): SimpleRedisCache =
        SimpleRedisCacheBasic(redisAdapter, objectMapper, pool, lockDuration, executor)
}

/**
 * Factory for SimpleRedisCacheWeb.
 */
class SimpleRedisCacheFactoryWeb(
    defaultExecutor: Supplier<Executor>,
    private val redisAdapter: RedisAdapter
) : SimpleRedisCacheFactory(defaultExecutor) {
    /**
     * Creates a SimpleRedisCacheWeb for a pool. A pool is basically a prefix to group all keys in the cache on.
     * Optionally it is possible to supply an Executor that is used to refresh values in the background,
     * if no Executor is supplied a default ThreadPoolTaskExecutor will be used.
     *
     * The default ThreadPoolTaskExecutor will carry over logging context, but not other things like security
     * context, etc. If the thread refreshing the cache need more than logging context or more advanced behaviour
     * you should supply your own Executor that can handle this.
     */
    override fun forPool(pool: String, lockDuration: Duration, objectMapper: ObjectMapper, executor: Executor): SimpleRedisCacheWeb =
        SimpleRedisCacheWeb(redisAdapter, objectMapper, pool, lockDuration, executor)
}
