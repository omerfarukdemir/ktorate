package io.github.omerfarukdemir.ktorate.examples.postgresql

import io.github.omerfarukdemir.ktorate.Ktorate
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
    embeddedServer(Netty, module = Application::ktorm).start(wait = true)
}

object KtormFixedWindowTable : Table<Nothing>("fixed_window") {
    val id = varchar("id").primaryKey()
    val startInSeconds = int("start_in_seconds")
    val requestCount = int("request_count")
}

class KtormFixedWindowStorage : FixedWindowStorage {

    // make sure schema is ready before run this example
    // CREATE DATABASE ktorate;
    // CREATE TABLE fixed_window (id varchar NOT NULL PRIMARY KEY, start_in_seconds INT NOT NULL, request_count INT NOT NULL);

    private val database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/ktorate",
        user = "postgres",
        password = "postgres"
    )

    override suspend fun get(id: String): FixedWindowModel? {
        return database.from(KtormFixedWindowTable)
            .select()
            .where(KtormFixedWindowTable.id eq id)
            .limit(1)
            .map { it.fixedWindowModel() }
            .firstOrNull()
    }

    override suspend fun upsert(model: FixedWindowModel): FixedWindowModel {
        return model.also {
            database.insertOrUpdate(KtormFixedWindowTable) {
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

    override suspend fun all(): Collection<FixedWindowModel> {
        return database.from(KtormFixedWindowTable)
            .select()
            .map { it.fixedWindowModel() }
    }

    override suspend fun delete(id: String): Boolean {
        return database.delete(KtormFixedWindowTable) { it.id eq id } == 1
    }

    override suspend fun delete(ids: Collection<String>): Int {
        return database.delete(KtormFixedWindowTable) { it.id inList ids }
    }

    private fun QueryRowSet.fixedWindowModel(): FixedWindowModel {
        return FixedWindowModel(
            this[KtormFixedWindowTable.id]!!,
            this[KtormFixedWindowTable.startInSeconds]!!,
            this[KtormFixedWindowTable.requestCount]!!
        )
    }
}

fun Application.ktorm() {
    install(Ktorate) {
        duration = 3.seconds
        limit = 5
        deleteExpiredRecordsPeriod = 5.seconds
        synchronizedReadWrite = true
        rateLimiter = FixedWindow(
            duration = duration,
            limit = limit,
            synchronizedReadWrite = synchronizedReadWrite,
            storage = KtormFixedWindowStorage()
        )
    }

    routing { get("/") { call.respondText("Evet") } }
}
