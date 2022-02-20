package net.bluewizardhat.common.cache

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import java.util.concurrent.Executor

@Component
class SimpleRedisCacheFactory(
    @Value("\${bluewizardhat.common.simplerediscache.corePoolSize:-0}")
    private val corePoolSize: Int,
    @Value("\${bluewizardhat.common.simplerediscache.maxPoolSize:10}")
    private val maxPoolSize: Int,
    @Value("\${bluewizardhat.common.simplerediscache.queueCapacity:20}")
    private val queueCapacity: Int,

    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) {
    private val log = KotlinLogging.logger {}

    /**
     * Creates a SimpleRedisCache for a pool. A pool is basically a prefix to group all keys in the cache on.
     * Optionally it is possible to supply an Executor that is used to refresh values in the background,
     * if no Executor is supplied a default ThreadPoolTaskExecutor will be used.
     *
     * The default ThreadPoolTaskExecutor will carry over logging context, but not other things like security
     * context, etc. If the thread refreshing the cache need more than logging context or more advanced behaviour
     * you should supply your own Executor that can handle this.
     */
    fun forPool(pool: String, executor: Executor = defaultExecutor()) =
        SimpleRedisCache(redisTemplate, objectMapper, pool, executor)

    private var defaultExecutor: ThreadPoolTaskExecutor? = null

    @Synchronized
    private fun defaultExecutor(): Executor {
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
