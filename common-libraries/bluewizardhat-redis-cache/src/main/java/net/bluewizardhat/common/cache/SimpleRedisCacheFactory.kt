package net.bluewizardhat.common.cache

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.slf4j.MDC
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.time.Duration
import java.util.concurrent.Executor

sealed class SimpleRedisCacheFactory(
    private val corePoolSize: Int,
    private val maxPoolSize: Int,
    private val queueCapacity: Int
) {
    private val log = KotlinLogging.logger {}

    private var defaultExecutor: ThreadPoolTaskExecutor? = null

    /**
     * Creates a SimpleRedisCache for a pool. A pool is basically a prefix to group all keys in the cache on.
     * Optionally it is possible to supply an Executor that is used to refresh values in the background,
     * if no Executor is supplied a default ThreadPoolTaskExecutor will be used.
     *
     * The default ThreadPoolTaskExecutor will carry over logging context, but not other things like security
     * context, etc. If the thread refreshing the cache need more than logging context or more advanced behaviour
     * you should supply your own Executor that can handle this.
     */
    abstract fun forPool(pool: String, lockDuration: Duration = Duration.ofMinutes(5), executor: Executor = defaultExecutor()): SimpleRedisCache

    @Synchronized
    protected fun defaultExecutor(): Executor {
        var executor = defaultExecutor
        if (executor == null) {
            executor = ThreadPoolTaskExecutor()
            executor.corePoolSize = corePoolSize
            executor.maxPoolSize = maxPoolSize
            executor.setQueueCapacity(queueCapacity)
            executor.setTaskDecorator(this::wrapLoggingContext)
            executor.afterPropertiesSet()
            defaultExecutor = executor
            log.debug { "Initialized default ThreadPoolTaskExecutor; corePoolSize=$corePoolSize, maxPoolSize=$maxPoolSize, queueCapacity=$queueCapacity" }
        }
        return executor
    }

    /**
     * Wraps a Runnable so that the executing thread will retain the logging context of the
     * originating thread.
     */
    private fun wrapLoggingContext(runnable: Runnable): Runnable {
        val context: Map<String, String>? = MDC.getCopyOfContextMap()
        return Runnable {
            val original: Map<String, String>? = MDC.getCopyOfContextMap()
            try {
                overwriteLoggingContext(context)
                runnable.run()
            } finally {
                overwriteLoggingContext(original)
            }
        }
    }

    private fun overwriteLoggingContext(context: Map<String, String>?) {
        MDC.clear()
        if (context != null) {
            MDC.setContextMap(context)
        }
    }
}

class SimpleRedisCacheFactoryBasic(
    corePoolSize: Int,
    maxPoolSize: Int,
    queueCapacity: Int,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) : SimpleRedisCacheFactory(corePoolSize, maxPoolSize, queueCapacity) {
    override fun forPool(pool: String, lockDuration: Duration, executor: Executor): SimpleRedisCache =
        SimpleRedisCacheBasic(redisTemplate, objectMapper, pool, lockDuration, executor)
}

class SimpleRedisCacheFactoryWeb(
    corePoolSize: Int,
    maxPoolSize: Int,
    queueCapacity: Int,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) : SimpleRedisCacheFactory(corePoolSize, maxPoolSize, queueCapacity) {
    /**
     * Creates a SimpleRedisCacheWeb for a pool. A pool is basically a prefix to group all keys in the cache on.
     * Optionally it is possible to supply an Executor that is used to refresh values in the background,
     * if no Executor is supplied a default ThreadPoolTaskExecutor will be used.
     *
     * The default ThreadPoolTaskExecutor will carry over logging context, but not other things like security
     * context, etc. If the thread refreshing the cache need more than logging context or more advanced behaviour
     * you should supply your own Executor that can handle this.
     */
    override fun forPool(pool: String, lockDuration: Duration, executor: Executor): SimpleRedisCacheWeb =
        SimpleRedisCacheWeb(redisTemplate, objectMapper, pool, lockDuration, executor)
}
