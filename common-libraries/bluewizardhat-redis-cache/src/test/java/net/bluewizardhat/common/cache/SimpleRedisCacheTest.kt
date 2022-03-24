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
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Duration
import java.util.concurrent.Executor
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class SimpleRedisCacheTest {
    private val redisAdapter: RedisAdapter = mock()

    private val objectMapper: ObjectMapper = mock {
        on { writeValueAsString(any()) } doReturn "serializedValue"
    }
    private val executor: Executor = mock {
        on { execute(any()) } doAnswer { (it.arguments[0] as Runnable).run() }
    }

    private val cache: SimpleRedisCache = SimpleRedisCacheBasic(redisAdapter, objectMapper, "testPool", Duration.ofMinutes(5), executor)
    private val webCache: SimpleRedisCacheWeb = SimpleRedisCacheWeb(redisAdapter, objectMapper, "testPool", Duration.ofMinutes(5), executor)

    @BeforeEach
    internal fun before() {
        reset(redisAdapter)
        whenever(redisAdapter.setIfAbsent(any(), any(), any())).thenReturn(true)
    }

    @Test
    internal fun testNotCached() {
        // Setup
        val captor = argumentCaptor<CachedValue<String>>()

        // Execute
        val value: String = cache.cache(key = "testKey", expireAfter = Duration.ofMinutes(1), refreshAfter = Duration.ofSeconds(45)) {
            "cacheValue"
        }

        // Verify
        verify(objectMapper).writeValueAsString(captor.capture())
        verify(redisAdapter).set(eq("testPool:testKey"), eq("serializedValue"), any<Duration>())
        assertEquals("cacheValue", value)
        with(captor.firstValue) {
            assertEquals("cacheValue", value)
            assertEquals(Duration.ofMinutes(1), expireAfter)
            assertEquals(Duration.ofSeconds(45), refreshAfter)
            assertNotNull(cacheTime)
        }
    }

    @Test
    internal fun testNotCachedWithNull() {
        // Setup
        val captor = argumentCaptor<CachedValue<String>>()

        // Execute
        val value: String? = cache.cache(key = "testKey", expireAfter = Duration.ofMinutes(1)) {
            null
        }

        // Verify
        verify(objectMapper).writeValueAsString(captor.capture())
        verify(redisAdapter).set(eq("testPool:testKey"), eq("serializedValue"), any<Duration>())
        assertNull(value)
        with(captor.firstValue) {
            assertNull(value)
            assertEquals(Duration.ofMinutes(1), expireAfter)
            assertNull(refreshAfter)
            assertNotNull(cacheTime)
        }
    }

    @Test
    internal fun testDurationZero() {
        // Execute
        val value: String = cache.cache(key = "testKey", expireAfter = Duration.ZERO) {
            "cacheValue"
        }

        // Verify
        verify(objectMapper, never()).writeValueAsString(any())
        verify(redisAdapter, never()).set(any(), any(), any<Duration>())
        assertEquals("cacheValue", value)
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
