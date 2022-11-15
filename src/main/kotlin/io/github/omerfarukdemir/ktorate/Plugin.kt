package io.github.omerfarukdemir.ktorate

import io.github.omerfarukdemir.ktorate.limiters.RateLimiter
import io.github.omerfarukdemir.ktorate.limiters.SlidingWindow
import io.github.omerfarukdemir.ktorate.utils.Now
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class Configuration(
    var duration: Duration = 1.hours,
    var limit: Int = 1000,
    var deleteExpiredRecordsPeriod: Duration = 10.minutes,
    var identityFunction: (request: ApplicationRequest) -> String = { it.origin.remoteHost },
    var synchronizedReadWrite: Boolean = true,
    var rateLimiter: RateLimiter = SlidingWindow(duration, limit, synchronizedReadWrite),
)

data class Result(
    val startInSeconds: Int,
    val count: Int,
    val exceeded: Boolean
)

val RateLimiter by lazy {
    createApplicationPlugin(name = "RateLimiter", createConfiguration = ::Configuration) {
        on(MonitoringEvent(ApplicationStarted)) { application ->
            application.log.debug("Starting RateLimiter expired records cleaner!")

            CoroutineScope(Job()).launch {
                while (true) {
                    delay(pluginConfig.deleteExpiredRecordsPeriod)

                    val count = pluginConfig.rateLimiter.deleteExpiredRecords(Now.seconds())

                    if (count > 0) {
                        application.log.debug("Removed $count expired RateLimit records!")
                    }
                }
            }
        }

        onCall { call ->
            val identity = pluginConfig.identityFunction(call.request)
            val result = pluginConfig.rateLimiter.rate(identity, Now.seconds())
            val remainingRequestCount = pluginConfig.limit - result.count

            call.response.header("X-RateLimit-Strategy", pluginConfig.rateLimiter.javaClass.simpleName)
            call.response.header("X-RateLimit-Limit", pluginConfig.limit)
            call.response.header("X-RateLimit-Remaining", remainingRequestCount)

            if (pluginConfig.rateLimiter !is SlidingWindow) {
                val reset = result.startInSeconds + pluginConfig.duration.inWholeSeconds

                call.response.header("X-RateLimit-Reset", reset)
            }

            if (result.exceeded) {
                call.respond(HttpStatusCode.TooManyRequests)
            }
        }
    }
}
