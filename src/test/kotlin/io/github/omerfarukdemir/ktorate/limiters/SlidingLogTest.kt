package io.github.omerfarukdemir.ktorate.limiters

import io.github.omerfarukdemir.ktorate.utils.Now
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class SlidingLogTest {

    private val duration = 10.seconds
    private val limit = 3
    private val rateLimiter = SlidingLog(duration, limit, true)

    @Test
    fun testFirstRequest() {
        val id = UUID.randomUUID().toString()
        val nowInSeconds = Now.seconds()

        runBlocking {
            val result = rateLimiter.rate(id, nowInSeconds)

            assertEquals(nowInSeconds, result.startInSeconds)
            assertEquals(1, result.count)
            assertEquals(false, result.exceeded)
        }
    }

    @Test
    fun testLastRequest() {
        val id = UUID.randomUUID().toString()
        val nowInSeconds = Now.seconds()

        runBlocking {
            rateLimiter.rate(id, nowInSeconds)
            rateLimiter.rate(id, nowInSeconds)

            val result = rateLimiter.rate(id, nowInSeconds)

            assertEquals(nowInSeconds, result.startInSeconds)
            assertEquals(3, result.count)
            assertEquals(false, result.exceeded)
        }
    }

    @Test
    fun testExceeded() {
        val id = UUID.randomUUID().toString()
        val nowInSeconds = Now.seconds()

        runBlocking {
            rateLimiter.rate(id, nowInSeconds)
            rateLimiter.rate(id, nowInSeconds)
            rateLimiter.rate(id, nowInSeconds)

            val result = rateLimiter.rate(id, nowInSeconds)

            assertEquals(nowInSeconds, result.startInSeconds)
            assertEquals(3, result.count)
            assertEquals(true, result.exceeded)
        }
    }

    @Test
    fun testExpired() {
        val id = UUID.randomUUID().toString()
        val nowInSeconds = Now.seconds()
        val beforeThisWindow = nowInSeconds - 20

        runBlocking {
            rateLimiter.rate(id, beforeThisWindow)

            val result = rateLimiter.rate(id, nowInSeconds)

            assertEquals(nowInSeconds, result.startInSeconds)
            assertEquals(1, result.count)
            assertEquals(false, result.exceeded)
        }
    }

}
