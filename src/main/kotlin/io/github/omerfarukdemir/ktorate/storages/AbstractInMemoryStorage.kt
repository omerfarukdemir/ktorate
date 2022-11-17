package io.github.omerfarukdemir.ktorate.storages

import io.github.omerfarukdemir.ktorate.models.RateLimitModel
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractInMemoryStorage<T: RateLimitModel> : RateLimitStorage<T> {

    private val storage = ConcurrentHashMap<String, T>()

    override suspend fun get(id: String): T? {
        return storage[id]
    }

    override suspend fun upsert(model: T): T {
        return model.also { storage[model.id] = model }
    }

    override suspend fun all(): Collection<T> {
        return storage.values
    }

    override suspend fun delete(id: String): Boolean {
        return storage.remove(id) != null
    }

    override suspend fun delete(ids: Collection<String>): Int {
        return ids.toSet()
            .map { delete(it) }
            .count { it }
    }
}
