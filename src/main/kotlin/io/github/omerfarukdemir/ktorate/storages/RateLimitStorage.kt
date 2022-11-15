package io.github.omerfarukdemir.ktorate.storages

import io.github.omerfarukdemir.ktorate.models.RateLimitModel

interface RateLimitStorage<T : RateLimitModel> {

    fun get(id: String): T?

    fun upsert(model: T): T

    fun all(): Collection<T>

    fun delete(id: String): Boolean

    fun delete(ids: Collection<String>): Int

}
