package io.github.omerfarukdemir.ktorate.models

data class FixedWindowModel(
    override val id: String,
    val startInSeconds: Int,
    val requestCount: Int
) : RateLimitModel {
    override fun startInSeconds(): Int {
        return startInSeconds
    }

    override fun requestCount(): Int {
        return requestCount
    }

    // TODO: remove
    fun incrementCount(): FixedWindowModel {
        return copy(requestCount = requestCount + 1)
    }
}
