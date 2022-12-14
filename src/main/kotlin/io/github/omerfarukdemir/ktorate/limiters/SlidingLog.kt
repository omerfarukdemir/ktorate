package io.github.omerfarukdemir.ktorate.limiters

import io.github.omerfarukdemir.ktorate.Result
import io.github.omerfarukdemir.ktorate.models.RateLimitModel
import io.github.omerfarukdemir.ktorate.models.SlidingLogModel
import io.github.omerfarukdemir.ktorate.storages.InMemorySlidingLogStorage
import io.github.omerfarukdemir.ktorate.storages.RateLimitStorage
import io.github.omerfarukdemir.ktorate.storages.SlidingLogStorage
import kotlin.time.Duration

class SlidingLog(
    duration: Duration,
    limit: Int,
    synchronizedReadWrite: Boolean,
    private val storage: SlidingLogStorage = InMemorySlidingLogStorage()
) : RateLimiter(duration, limit, synchronizedReadWrite, storage as RateLimitStorage<RateLimitModel>) {

    override suspend fun apply(id: String, nowInSeconds: Int): Result {
        val model = (storage.get(id) ?: SlidingLogModel(id, listOf()))
            .addRequestTime(nowInSeconds)
            .removeExpiredRequestTimes(nowInSeconds, durationInSeconds)

        if (model.requestCount() > limit) {
            return model.toResult(requestCount = limit, exceeded = true)
        }

        return storage.upsert(model).toResult()

    }

    override fun expired(model: RateLimitModel, nowInSeconds: Int): Boolean {
        check(model is SlidingLogModel)

        return nowInSeconds > model.requestTimesInSeconds.max() + durationInSeconds
    }
}
