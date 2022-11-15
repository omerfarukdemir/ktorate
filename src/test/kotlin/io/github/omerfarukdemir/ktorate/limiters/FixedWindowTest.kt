package io.github.omerfarukdemir.ktorate.limiters

import io.github.omerfarukdemir.ktorate.utils.Now
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class FixedWindowTest {

    private val duration = 10.seconds
    private val limit = 3
    private val rateLimiter = FixedWindow(duration, limit, true)

    @Test
    fun testFirstRequest() {
        val id = UUID.randomUUID().toString()
        val nowInSeconds = Now.seconds()
        val windowStart = nowInSeconds - (nowInSeconds % duration.inWholeSeconds.toInt())

        runBlocking {
            val result = rateLimiter.rate(id, nowInSeconds)

            assertEquals(windowStart, result.startInSeconds)
            assertEquals(1, result.count)
            assertEquals(false, result.exceeded)
        }
    }

    @Test
    fun testLastRequest() {
        val id = UUID.randomUUID().toString()
        val nowInSeconds = Now.seconds()
        val windowStart = nowInSeconds - (nowInSeconds % duration.inWholeSeconds.toInt())

        runBlocking {
            rateLimiter.rate(id, nowInSeconds)
            rateLimiter.rate(id, nowInSeconds)

            val result = rateLimiter.rate(id, nowInSeconds)

            assertEquals(windowStart, result.startInSeconds)
            assertEquals(3, result.count)
            assertEquals(false, result.exceeded)
        }
    }

    @Test
    fun testExceeded() {
        val id = UUID.randomUUID().toString()
        val nowInSeconds = Now.seconds()
        val windowStart = nowInSeconds - (nowInSeconds % duration.inWholeSeconds.toInt())

        runBlocking {
            rateLimiter.rate(id, nowInSeconds)
            rateLimiter.rate(id, nowInSeconds)
            rateLimiter.rate(id, nowInSeconds)

            val result = rateLimiter.rate(id, nowInSeconds)

            assertEquals(windowStart, result.startInSeconds)
            assertEquals(3, result.count)
            assertEquals(true, result.exceeded)
        }
    }

    @Test
    fun testExpired() {
        val id = UUID.randomUUID().toString()
        val nowInSeconds = Now.seconds()
        val beforeThisWindow = nowInSeconds - 20
        val windowStart = nowInSeconds - (nowInSeconds % duration.inWholeSeconds.toInt())

        runBlocking {
            rateLimiter.rate(id, beforeThisWindow)

            val result = rateLimiter.rate(id, nowInSeconds)

            assertEquals(windowStart, result.startInSeconds)
            assertEquals(1, result.count)
            assertEquals(false, result.exceeded)
        }
    }
}
