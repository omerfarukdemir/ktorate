package io.github.omerfarukdemir.ktorate.examples.postgresql

import io.github.omerfarukdemir.ktorate.ktorate
import io.github.omerfarukdemir.ktorate.limiters.FixedWindow
import io.github.omerfarukdemir.ktorate.models.FixedWindowModel
import io.github.omerfarukdemir.ktorate.storages.FixedWindowStorage
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import org.ktorm.support.postgresql.insertOrUpdate
import kotlin.time.Duration.Companion.seconds

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::ktorm).start(wait = true)
}

object FixedWindowTable : Table<Nothing>("fixed_window") {
    val id = varchar("id").primaryKey()
    val startInSeconds = int("start_in_seconds")
    val requestCount = int("request_count")
}

class PostgreSQLFixedWindowStorage : FixedWindowStorage {

    // make sure schema is ready before run this example
    // CREATE DATABASE ktorate;
    // CREATE TABLE fixed_window (id varchar NOT NULL PRIMARY KEY, start_in_seconds INT NOT NULL, request_count INT NOT NULL);

    private val database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/ktorate",
        user = "postgres",
        password = "postgres"
    )

    override fun get(id: String): FixedWindowModel? {
        return database.from(FixedWindowTable)
            .select()
            .where(FixedWindowTable.id eq id)
            .limit(1)
            .map { it.fixedWindowModel() }
            .firstOrNull()
    }

    override fun upsert(model: FixedWindowModel): FixedWindowModel {
        return model.also {
            database.insertOrUpdate(FixedWindowTable) {
                set(it.id, model.id)
                set(it.startInSeconds, model.startInSeconds)
                set(it.requestCount, model.requestCount)
                onConflict {
                    set(it.startInSeconds, model.startInSeconds)
                    set(it.requestCount, model.requestCount)
                }
            }
        }
    }

    override fun all(): Collection<FixedWindowModel> {
        return database.from(FixedWindowTable)
            .select()
            .map { it.fixedWindowModel() }
    }

    override fun delete(id: String): Boolean {
        return database.delete(FixedWindowTable) { it.id eq id } == 1
    }

    override fun delete(ids: Collection<String>): Int {
        return database.delete(FixedWindowTable) { it.id inList ids }
    }

    private fun QueryRowSet.fixedWindowModel(): FixedWindowModel {
        return FixedWindowModel(
            this[FixedWindowTable.id]!!,
            this[FixedWindowTable.startInSeconds]!!,
            this[FixedWindowTable.requestCount]!!
        )
    }
}

fun Application.ktorm() {
    install(ktorate, configure = {
        duration = 3.seconds
        limit = 5
        deleteExpiredRecordsPeriod = 5.seconds
        synchronizedReadWrite = true
        rateLimiter = FixedWindow(
            duration = duration,
            limit = limit,
            synchronizedReadWrite = synchronizedReadWrite,
            storage = PostgreSQLFixedWindowStorage()
        )
    })

    routing { get("/") { call.respondText("Evet") } }
}
