package io.github.omerfarukdemir.ktorate.storages

import io.github.omerfarukdemir.ktorate.models.RateLimitModel

interface RateLimitStorage<T : RateLimitModel> {

    suspend fun get(id: String): T?

    suspend fun upsert(model: T): T

    suspend fun all(): Collection<T>

    suspend fun delete(id: String): Boolean

    suspend fun delete(ids: Collection<String>): Int

}
