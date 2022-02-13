package net.bluewizardhat.demoapp.template.cache

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.function.Supplier

@Component
class RedisCache(
    private val redisTemplate: RedisTemplate<Any, Any>,
    private val objectMapper: ObjectMapper
) {
    private val log = KotlinLogging.logger {}

    private val listOperations = redisTemplate.opsForList()
    private val valueOperations = redisTemplate.opsForValue()
    private val lockOperations = redisTemplate.opsForValue()

    final inline fun <reified T> cached(key: String, supplier: Supplier<T>): T =
        cached(key, T::class.java, supplier)

    fun <T> cached(key: String, cl: Class<T>, supplier: Supplier<T>): T {
        var value = readFromCache(key, cl)
        if (value == null) {
            log.debug { "'$key' not found in cache" }
            value = supplier.get()
            writeToCache(key, value)
        }
        return value as T
    }

    private fun <T> readFromCache(key: String, cl: Class<T>): T? {
        log.debug { "Reading '$key' from cache" }
        val serialized = valueOperations[key] as String?
        return if (serialized == null) null else objectMapper.readValue(serialized, cl)
    }

    private fun <T> writeToCache(key: String, value: T) {
        if (value != null) {
            log.debug { "Writing '$key' to cache" }
            valueOperations[key] = objectMapper.writeValueAsString(value)
        }
    }
}
