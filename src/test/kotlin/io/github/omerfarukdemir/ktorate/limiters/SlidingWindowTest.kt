package io.github.omerfarukdemir.ktorate.limiters

import io.github.omerfarukdemir.ktorate.utils.Now
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class SlidingWindowTest {

    private val duration = 10.seconds
    private val limit = 3
    private val rateLimiter = SlidingWindow(duration, limit, true)

    @Test
    fun testFirstRequest() {
        val id = UUID.randomUUID().toString()
        val nowInSeconds = Now.seconds()
        val slidingWindowStart = nowInSeconds - duration.inWholeSeconds.toInt()

        runBlocking {
            val result = rateLimiter.rate(id, nowInSeconds)

            assertEquals(slidingWindowStart, result.startInSeconds)
            assertEquals(1, result.count)
            assertEquals(false, result.exceeded)
        }
    }

    @Test
    fun testLastRequest() {
        val id = UUID.randomUUID().toString()
        val nowInSeconds = Now.seconds()
        val slidingWindowStart = nowInSeconds - duration.inWholeSeconds.toInt()

        runBlocking {
            rateLimiter.rate(id, nowInSeconds)
            rateLimiter.rate(id, nowInSeconds)

            val result = rateLimiter.rate(id, nowInSeconds)

            assertEquals(slidingWindowStart, result.startInSeconds)
            assertEquals(3, result.count)
            assertEquals(false, result.exceeded)
        }
    }

    @Test
    fun testWithFormerWindowWeight() {
        val id = UUID.randomUUID().toString()

        val rateLimiter = SlidingWindow(duration, 10, true)

        runBlocking {
            rateLimiter.rate(id, 9)
            rateLimiter.rate(id, 9)
            rateLimiter.rate(id, 9)
            rateLimiter.rate(id, 9)

            val result = rateLimiter.rate(id, 15)

            assertEquals(5, result.startInSeconds)
            assertEquals(3, result.count)
            assertEquals(false, result.exceeded)
        }
    }

    @Test
    fun testExceeded() {
        val id = UUID.randomUUID().toString()
        val nowInSeconds = Now.seconds()
        val slidingWindowStart = nowInSeconds - duration.inWholeSeconds.toInt()

        runBlocking {
            rateLimiter.rate(id, nowInSeconds)
            rateLimiter.rate(id, nowInSeconds)
            rateLimiter.rate(id, nowInSeconds)

            val result = rateLimiter.rate(id, nowInSeconds)

            assertEquals(slidingWindowStart, result.startInSeconds)
            assertEquals(3, result.count)
            assertEquals(true, result.exceeded)
        }
    }

    @Test
    fun testExpired() {
        val id = UUID.randomUUID().toString()
        val nowInSeconds = Now.seconds()
        val beforeThisWindow = nowInSeconds - 20
        val slidingWindowStart = nowInSeconds - duration.inWholeSeconds.toInt()

        runBlocking {
            rateLimiter.rate(id, beforeThisWindow)

            val result = rateLimiter.rate(id, nowInSeconds)

            assertEquals(slidingWindowStart, result.startInSeconds)
            assertEquals(1, result.count)
            assertEquals(false, result.exceeded)
        }
    }

}
