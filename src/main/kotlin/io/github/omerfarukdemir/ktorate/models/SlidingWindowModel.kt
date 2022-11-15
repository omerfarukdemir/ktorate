package io.github.omerfarukdemir.ktorate.models

data class SlidingWindowModel(
    override val id: String,
    val startInSeconds: Int,
    val requestCount: Int,
    val previousRequestCount: Int
) : RateLimitModel {
    override fun startInSeconds(): Int {
        return startInSeconds
    }

    override fun requestCount(): Int {
        return requestCount
    }

    // TODO: remove
    fun incrementCount(): SlidingWindowModel {
        return copy(requestCount = requestCount + 1)
    }
}
