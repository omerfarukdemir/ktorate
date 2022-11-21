package io.github.omerfarukdemir.ktorate.examples

import io.github.omerfarukdemir.ktorate.Ktorate
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

fun main() {
    embeddedServer(Netty, module = Application::inMemory).start(wait = true)
}

fun Application.inMemory() {
    install(Ktorate) {
        // strategy window
        duration = 1.hours

        // max request in duration by defined strategy
        limit = 1000

        // to remove expired records in data store
        deleteExpiredRecordsPeriod = 5.minutes

        // blocking ops between read and write ops (only for same identity)
        synchronizedReadWrite = true

        // count starting path with "/v1/api/" urls
        includedPaths = listOf(Regex("^/api/v1/.*$"))

        // do not count .html urls
        excludedPaths = listOf(Regex("^.*html$"))
    }

    routing { get("/") { call.respondText("Evet") } }
}
