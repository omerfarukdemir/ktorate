package io.github.omerfarukdemir.ktorate.examples

import io.github.omerfarukdemir.ktorate.Ktorate
import io.ktor.server.application.*
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
        duration = 1.hours                                    // strategy window
        limit = 1000                                          // max request in duration by defined strategy
        deleteExpiredRecordsPeriod = 5.minutes                // to remove expired records in data store
        synchronizedReadWrite = true                          // blocking ops between read and write ops (only for same identity)
        includedPaths = listOf(Regex("^/api/v1/.*$")) // count starting path with "/v1/api/" urls
        excludedPaths = listOf(Regex("^.*html$"))     // do not count .html urls
    }

    routing { get("/") { call.respondText("Evet") } }
}
