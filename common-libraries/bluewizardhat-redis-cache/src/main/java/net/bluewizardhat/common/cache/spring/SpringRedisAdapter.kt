package net.bluewizardhat.common.cache.spring

import net.bluewizardhat.common.cache.RedisAdapter
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration

class SpringRedisAdapter(
    private val redisTemplate: StringRedisTemplate
) : RedisAdapter {
    private val valueOperations = redisTemplate.opsForValue()

    override fun get(key: String): String? = valueOperations.get(key)
    override fun set(key: String, value: String, timeout: Duration) { valueOperations.set(key, value, timeout) }
    override fun setIfAbsent(key: String, value: String, timeout: Duration): Boolean? = valueOperations.setIfAbsent(key, value, timeout)
    override fun keys(pattern: String): Set<String> = redisTemplate.keys(pattern)
    override fun unlink(key: String) { redisTemplate.unlink(key) }
    override fun unlink(keys: Collection<String>) { redisTemplate.unlink(keys) }
}
