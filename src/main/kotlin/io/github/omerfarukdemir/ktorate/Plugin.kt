package io.github.omerfarukdemir.ktorate

import io.github.omerfarukdemir.ktorate.limiters.FixedWindow
import io.github.omerfarukdemir.ktorate.limiters.RateLimiter
import io.github.omerfarukdemir.ktorate.limiters.SlidingWindow
import io.github.omerfarukdemir.ktorate.utils.Now
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.log
import io.ktor.server.plugins.origin
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.path
import io.ktor.server.response.header
import io.ktor.server.response.respond
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class KtorateConfiguration {
    var deleteExpiredRecordsPeriod: Duration = 10.minutes
    var identityFunction: (request: ApplicationRequest) -> String = { it.origin.remoteHost }
    var rateLimiter: RateLimiter = FixedWindow(1.hours, 1000, true)
    var includedPaths: Collection<Regex>? = null
    var excludedPaths: Collection<Regex>? = null
}

data class Result(
    val startInSeconds: Int,
    val count: Int,
    val exceeded: Boolean
)

val Ktorate by lazy {
    createApplicationPlugin("Ktorate", ::KtorateConfiguration) {
        on(MonitoringEvent(ApplicationStarted)) { application ->
            application.log.debug("Starting ktorate expired records cleaner!")

            CoroutineScope(Job()).launch {
                while (true) {
                    delay(pluginConfig.deleteExpiredRecordsPeriod)

                    val count = pluginConfig.rateLimiter.deleteExpiredRecords(Now.seconds())

                    if (count > 0) {
                        application.log.debug("Removed $count expired ktorate records!")
                    }
                }
            }
        }

        onCall { call ->
            val path = call.request.path()

            fun anyMatch(regex: Collection<Regex>?): Boolean? = regex
                ?.map { it.matches(path) }
                ?.fold(false) { x, y -> x || y }

            val includedMatch = anyMatch(pluginConfig.includedPaths)
            val excludedMatch = anyMatch(pluginConfig.excludedPaths)

            val shouldWork = when (includedMatch to excludedMatch) {
                null to null -> true
                null to false -> true
                null to true -> false
                false to null -> false
                false to false -> false
                false to true -> false
                true to null -> true
                true to false -> true
                true to true -> {
                    call.application.log.warn("Path ($path) is matching with both included and excluded paths")
                    true
                }
                else -> true
            }

            if (shouldWork) {
                val identity = pluginConfig.identityFunction(call.request)
                val result = pluginConfig.rateLimiter.rate(identity, Now.seconds())
                val remainingRequestCount = pluginConfig.rateLimiter.limit - result.count

                call.response.header("X-RateLimit-Strategy", pluginConfig.rateLimiter.javaClass.simpleName)
                call.response.header("X-RateLimit-Limit", pluginConfig.rateLimiter.limit)
                call.response.header("X-RateLimit-Remaining", remainingRequestCount)

                if (pluginConfig.rateLimiter !is SlidingWindow) {
                    val reset = result.startInSeconds + pluginConfig.rateLimiter.duration.inWholeSeconds

                    call.response.header("X-RateLimit-Reset", reset)
                }

                if (result.exceeded) {
                    call.respond(HttpStatusCode.TooManyRequests)
                }
            }
        }
    }
}
