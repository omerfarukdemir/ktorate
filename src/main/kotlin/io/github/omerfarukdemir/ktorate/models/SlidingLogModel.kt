package io.github.omerfarukdemir.ktorate.models

data class SlidingLogModel(
    override val id: String,
    val requestTimesInSeconds: Collection<Int>
) : RateLimitModel {
    override fun startInSeconds(): Int {
        return requestTimesInSeconds.min()
    }

    override fun requestCount(): Int {
        return requestTimesInSeconds.count()
    }

    fun addRequestTime(requestTime: Int): SlidingLogModel {
        return copy(requestTimesInSeconds = requestTimesInSeconds.plus(requestTime))
    }

    fun removeExpiredRequestTimes(nowInSeconds: Int, expireInSeconds: Int): SlidingLogModel {
        return copy(requestTimesInSeconds = requestTimesInSeconds.filter { it + expireInSeconds > nowInSeconds })
    }
}
