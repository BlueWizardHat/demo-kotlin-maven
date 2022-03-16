package net.bluewizardhat.common.cache

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import java.util.concurrent.Executor
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class SimpleRedisCacheTest {
    private val valueOperations: ValueOperations<String, String> = mock()

    private val redisTemplate: StringRedisTemplate = mock {
        on { opsForValue() } doReturn valueOperations
    }

    private val objectMapper: ObjectMapper = mock {
        on { writeValueAsString(any()) } doReturn "serializedValue"
    }
    private val executor: Executor = mock {
        on { execute(any()) } doAnswer { (it.arguments[0] as Runnable).run() }
    }

    private val cache: SimpleRedisCache = SimpleRedisCacheBasic(redisTemplate, objectMapper, "testPool", Duration.ofMinutes(5), executor)
    private val webCache: SimpleRedisCacheWeb = SimpleRedisCacheWeb(redisTemplate, objectMapper, "testPool", Duration.ofMinutes(5), executor)

    @BeforeEach
    internal fun before() {
        reset(valueOperations)
        whenever(valueOperations.setIfAbsent(any(), any(), any())).thenReturn(true)
    }

    @Test
    internal fun testNotCached() {
        val captor = argumentCaptor<CachedValue<String>>()
        val value: String = cache.cache(key = "testKey", expireAfter = Duration.ofMinutes(1), refreshAfter = Duration.ofSeconds(45)) {
            "cacheValue"
        }
        verify(objectMapper).writeValueAsString(captor.capture())
        verify(valueOperations).set(eq("testPool:testKey"), eq("serializedValue"), any<Duration>())
        assertEquals("cacheValue", value)
        captor.firstValue.let {
            assertEquals("cacheValue", it.value)
            assertEquals(Duration.ofMinutes(1), it.expireAfter)
            assertEquals(Duration.ofSeconds(45), it.refreshAfter)
            assertNotNull(it.cacheTime)
        }
    }

    @Test
    internal fun testCachedNoRefresh() {
        // TODO
    }

    @Test
    internal fun testCachedBeforeRefresh() {
        // TODO
    }

    @Test
    internal fun testCachedAfterRefresh() {
        // TODO
    }
}
