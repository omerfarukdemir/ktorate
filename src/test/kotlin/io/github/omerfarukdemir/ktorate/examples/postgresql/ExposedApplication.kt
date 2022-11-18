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
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import org.postgresql.util.PSQLException
import java.lang.Exception
import kotlin.time.Duration.Companion.seconds

fun main() {
    embeddedServer(Netty, module = Application::exposed).start(wait = true)
}

object ExposedFixedWindowTable : Table("fixed_window") {
    val id = varchar("id", 128)
    val startInSeconds = integer("start_in_seconds")
    val requestCount = integer("request_count")

    override val primaryKey = PrimaryKey(id)
}

class ExposedFixedWindowStorage : FixedWindowStorage {

    // make sure schema is ready before run this example
    // CREATE DATABASE ktorate;
    // CREATE TABLE fixed_window (id varchar NOT NULL PRIMARY KEY, start_in_seconds INT NOT NULL, request_count INT NOT NULL);

    private val database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/ktorate",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "postgres"
    )

    override suspend fun get(id: String): FixedWindowModel? {
        return query {
            ExposedFixedWindowTable
                .select { ExposedFixedWindowTable.id eq id }
                .map { it.fixedWindowModel() }
                .singleOrNull()
        }
    }

    override suspend fun upsert(model: FixedWindowModel): FixedWindowModel {
        val query = """
            INSERT INTO
                fixed_window (id, start_in_seconds, request_count)
            VALUES
                ('${model.id}', ${model.startInSeconds}, ${model.requestCount})
            ON CONFLICT
                (id)
            DO UPDATE
                SET start_in_seconds = ${model.startInSeconds}, request_count = ${model.requestCount}
            """.trimIndent()

        return model.also {
            transaction {
                TransactionManager.current().exec(query)
            }
        }
    }

    override suspend fun all(): Collection<FixedWindowModel> {
        return query {
            ExposedFixedWindowTable.selectAll().map { it.fixedWindowModel() }
        }
    }

    override suspend fun delete(id: String): Boolean {
        return query {
            ExposedFixedWindowTable.deleteWhere { ExposedFixedWindowTable.id eq id } == 1
        }
    }

    override suspend fun delete(ids: Collection<String>): Int {
        return query {
            ExposedFixedWindowTable.deleteWhere { ExposedFixedWindowTable.id inList ids }
        }
    }

    private suspend fun <T> query(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    private fun ResultRow.fixedWindowModel(): FixedWindowModel {
        return FixedWindowModel(
            this[ExposedFixedWindowTable.id],
            this[ExposedFixedWindowTable.startInSeconds],
            this[ExposedFixedWindowTable.requestCount],
        )
    }
}

fun Application.exposed() {
    install(Ktorate) {
        duration = 3.seconds
        limit = 5
        deleteExpiredRecordsPeriod = 5.seconds
        synchronizedReadWrite = true
        rateLimiter = FixedWindow(
            duration = duration,
            limit = limit,
            synchronizedReadWrite = synchronizedReadWrite,
            storage = ExposedFixedWindowStorage()
        )
    }

    routing { get("/") { call.respondText("Evet") } }
}
