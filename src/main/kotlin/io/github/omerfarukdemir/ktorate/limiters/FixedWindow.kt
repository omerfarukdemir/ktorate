package io.github.omerfarukdemir.ktorate.limiters

import io.github.omerfarukdemir.ktorate.Result
import io.github.omerfarukdemir.ktorate.models.FixedWindowModel
import io.github.omerfarukdemir.ktorate.models.RateLimitModel
import io.github.omerfarukdemir.ktorate.storages.FixedWindowStorage
import io.github.omerfarukdemir.ktorate.storages.InMemoryFixedWindowStorage
import io.github.omerfarukdemir.ktorate.storages.RateLimitStorage
import kotlin.time.Duration

class FixedWindow(
    duration: Duration,
    limit: Int,
    synchronizedReadWrite: Boolean,
    private val storage: FixedWindowStorage = InMemoryFixedWindowStorage()
) : RateLimiter(duration, limit, synchronizedReadWrite, storage as RateLimitStorage<RateLimitModel>) {

    override suspend fun apply(id: String, nowInSeconds: Int): Result {
        val model = storage.get(id)

        if (model == null || expired(model, nowInSeconds)) {
            val windowStart = nowInSeconds - (nowInSeconds % durationInSeconds)

            return storage.upsert(FixedWindowModel(id, windowStart, 1)).toResult()
        }

        if (model.requestCount >= limit) {
            return model.toResult(exceeded = true)
        }

        return storage.upsert(model.copy(requestCount = model.requestCount + 1)).toResult()
    }

    override fun expired(model: RateLimitModel, nowInSeconds: Int): Boolean {
        check(model is FixedWindowModel)

        return nowInSeconds > model.startInSeconds + durationInSeconds
    }
}
