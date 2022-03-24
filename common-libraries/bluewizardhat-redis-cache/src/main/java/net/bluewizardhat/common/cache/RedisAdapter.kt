package net.bluewizardhat.common.cache

import java.time.Duration

interface RedisAdapter {
    fun get(key: String): String?
    fun set(key: String, value: String, timeout: Duration)
    fun setIfAbsent(key: String, value: String, timeout: Duration): Boolean?
    fun keys(pattern: String): Set<String>
    fun unlink(key: String)
    fun unlink(keys: Collection<String>)
}
