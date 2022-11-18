package io.github.omerfarukdemir.ktorate.examples.redis

import com.google.gson.Gson
import io.github.omerfarukdemir.ktorate.KtorateConfiguration
import io.github.omerfarukdemir.ktorate.Ktorate
import io.github.omerfarukdemir.ktorate.limiters.FixedWindow
import io.github.omerfarukdemir.ktorate.models.FixedWindowModel
import io.github.omerfarukdemir.ktorate.storages.FixedWindowStorage
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import redis.clients.jedis.JedisPool
import kotlin.time.Duration.Companion.seconds

fun main() {
    embeddedServer(Netty, module = Application::jedis).start(wait = true)
}

class RedisFixedWindowStorage : FixedWindowStorage {

    private val redisPool = JedisPool()
    private val gson = Gson()

    override suspend fun get(id: String): FixedWindowModel? {
        return redisPool.resource
            .use { it.get(id) }
            ?.let { gson.fromJson(it, FixedWindowModel::class.java) }
    }

    override suspend fun upsert(model: FixedWindowModel): FixedWindowModel {
        return model.also { redisPool.resource.use { it.set(model.id, gson.toJson(model)) } }
    }

    override suspend fun all(): Collection<FixedWindowModel> {
        val keys = redisPool.resource.use { it.keys("*") }

        return if (keys.isEmpty()) listOf()
        else redisPool.resource
            .use { it.mget(*keys.toTypedArray()) }
            .map { gson.fromJson(it, FixedWindowModel::class.java) }
    }

    override suspend fun delete(id: String): Boolean {
        return redisPool.resource.use { it.del(id) == 1L }
    }

    override suspend fun delete(ids: Collection<String>): Int {
        return redisPool.resource.use { it.del(*ids.toTypedArray()).toInt() }
    }
}

fun Application.jedis() {
    install(Ktorate) {
        duration = 3.seconds
        limit = 5
        deleteExpiredRecordsPeriod = 5.seconds
        synchronizedReadWrite = true
        rateLimiter = FixedWindow(
            duration = duration,
            limit = limit,
            synchronizedReadWrite = synchronizedReadWrite,
            storage = RedisFixedWindowStorage()
        )
    }

    routing { get("/") { call.respondText("Evet") } }
}
