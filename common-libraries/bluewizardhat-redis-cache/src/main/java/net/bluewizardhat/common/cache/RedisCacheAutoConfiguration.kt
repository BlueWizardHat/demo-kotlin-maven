package net.bluewizardhat.common.cache

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate

@Configuration
class RedisCacheAutoConfiguration(
    @Value("\${bluewizardhat.common.simplerediscache.corePoolSize:0}")
    private val corePoolSize: Int,
    @Value("\${bluewizardhat.common.simplerediscache.maxPoolSize:10}")
    private val maxPoolSize: Int,
    @Value("\${bluewizardhat.common.simplerediscache.queueCapacity:20}")
    private val queueCapacity: Int,

    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) {
    private val log = KotlinLogging.logger {}

    @Bean
    @ConditionalOnMissingClass(value = ["javax.servlet.http.HttpServletResponse"])
    fun simpleRedisCacheFactory(): SimpleRedisCacheFactory {
        log.info("Creating SimpleRedisCacheFactory (Non-Web)")
        return SimpleRedisCacheFactoryBasic(corePoolSize, maxPoolSize, queueCapacity, redisTemplate, objectMapper)
    }

    @Bean
    @ConditionalOnClass(name = ["javax.servlet.http.HttpServletResponse"])
    fun simpleRedisCacheFactoryWeb(): SimpleRedisCacheFactoryWeb {
        log.info("Creating SimpleRedisCacheFactoryWeb")
        return SimpleRedisCacheFactoryWeb(corePoolSize, maxPoolSize, queueCapacity, redisTemplate, objectMapper)
    }
}
