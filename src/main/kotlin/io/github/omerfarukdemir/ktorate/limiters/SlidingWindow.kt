package io.github.omerfarukdemir.ktorate.limiters

import io.github.omerfarukdemir.ktorate.Result
import io.github.omerfarukdemir.ktorate.models.RateLimitModel
import io.github.omerfarukdemir.ktorate.models.SlidingWindowModel
import io.github.omerfarukdemir.ktorate.storages.InMemorySlidingWindowStorage
import io.github.omerfarukdemir.ktorate.storages.RateLimitStorage
import io.github.omerfarukdemir.ktorate.storages.SlidingWindowStorage
import java.lang.IllegalStateException
import kotlin.time.Duration

class SlidingWindow(
    duration: Duration,
    private val limit: Int,
    synchronizedReadWrite: Boolean,
    private val storage: SlidingWindowStorage = InMemorySlidingWindowStorage()
) : RateLimiter(duration, synchronizedReadWrite, storage as RateLimitStorage<RateLimitModel>) {

    override suspend fun apply(id: String, nowInSeconds: Int): Result {
        val diffFromWindowStart by lazy { nowInSeconds % durationInSeconds }

        val model = storage.get(id).let {
            val windowStart by lazy { nowInSeconds - diffFromWindowStart }

            if (it == null) {
                SlidingWindowModel(id, windowStart, 0, 0)
            } else if (nowInSeconds > it.startInSeconds + durationInSeconds) {
                val previousWindowStart = windowStart - durationInSeconds

                val previousCount = if (previousWindowStart == it.startInSeconds) it.requestCount else 0

                SlidingWindowModel(id, windowStart, 0, previousCount)
            } else {
                it
            }
        }

        val previousWeight = (durationInSeconds - diffFromWindowStart) / durationInSeconds.toDouble()

        val count = (previousWeight * model.previousRequestCount).toInt() + model.requestCount + 1

        val startInSeconds = nowInSeconds - durationInSeconds

        if (count > limit) {
            return model.toResult(startInSeconds = startInSeconds, requestCount = limit, exceeded = true)
        }

        return storage.upsert(model.incrementCount())
            .toResult(startInSeconds = startInSeconds, requestCount = count, exceeded = false)
    }

    override fun expired(model: RateLimitModel, nowInSeconds: Int): Boolean {
        if (model !is SlidingWindowModel) throw IllegalStateException()

        return nowInSeconds > model.startInSeconds + (durationInSeconds * 2)
    }
}
