package io.github.omerfarukdemir.ktorate

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::inMemory).start(wait = true)
}

fun Application.inMemory() {
    fun getUserId(request: ApplicationRequest): String {
        return UUID.randomUUID().toString()
    }

    install(ktorate, configure = {
        duration = 1.hours                     // strategy window
        limit = 1000                           // max request in duration by defined strategy
        deleteExpiredRecordsPeriod = 5.minutes // to remove expired records in data store
        identityFunction = ::getUserId         // default is client's IP
        synchronizedReadWrite = true           // blocking ops between read and write ops (only for same identity)
    })

    routing { get("/") { call.respondText("Evet") } }
}
