package net.bluewizardhat.common.cache

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.util.concurrent.Executor

internal class SimpleRedisCacheTest {
    private val redisTemplate: StringRedisTemplate = mock()
    private val valueOperations: ValueOperations<String, String> = mock()

    private val objectMapper: ObjectMapper = mock {
        on { writeValueAsString(any()) } doReturn "serializedValue"
    }
    private val executor: Executor = mock {
        on { execute(any()) } doAnswer { (it.arguments[0] as Runnable).run() }
    }

    @BeforeEach
    internal fun before() {
        reset(redisTemplate, valueOperations)
        whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
    }

    @Test
    internal fun testNotCached() {
        // TODO
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
