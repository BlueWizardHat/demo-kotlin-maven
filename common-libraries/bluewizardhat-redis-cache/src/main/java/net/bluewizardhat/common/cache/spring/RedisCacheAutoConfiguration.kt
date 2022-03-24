package net.bluewizardhat.common.cache.spring

import mu.KotlinLogging
import net.bluewizardhat.common.cache.SimpleRedisCacheFactory
import net.bluewizardhat.common.cache.SimpleRedisCacheFactoryBasic
import net.bluewizardhat.common.cache.SimpleRedisCacheFactoryWeb
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
class RedisCacheAutoConfiguration(
    @Value("\${bluewizardhat.common.simplerediscache.corePoolSize:0}")
    private val corePoolSize: Int,
    @Value("\${bluewizardhat.common.simplerediscache.maxPoolSize:10}")
    private val maxPoolSize: Int,
    @Value("\${bluewizardhat.common.simplerediscache.queueCapacity:20}")
    private val queueCapacity: Int,

    private val redisTemplate: StringRedisTemplate
) {
    private val log = KotlinLogging.logger {}

    private var defaultExecutor: ThreadPoolTaskExecutor? = null

    @Bean
    @ConditionalOnMissingClass(value = ["javax.servlet.http.HttpServletResponse"])
    fun simpleRedisCacheFactory(): SimpleRedisCacheFactory {
        log.info("Creating SimpleRedisCacheFactory (Non-Web)")
        return SimpleRedisCacheFactoryBasic(this::defaultExecutor, SpringRedisAdapter(redisTemplate))
    }

    @Bean
    @ConditionalOnClass(name = ["javax.servlet.http.HttpServletResponse"])
    fun simpleRedisCacheFactoryWeb(): SimpleRedisCacheFactoryWeb {
        log.info("Creating SimpleRedisCacheFactoryWeb")
        return SimpleRedisCacheFactoryWeb(this::defaultExecutor, SpringRedisAdapter(redisTemplate))
    }

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
