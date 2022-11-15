package io.github.omerfarukdemir.ktorate.models

import io.github.omerfarukdemir.ktorate.Result

interface RateLimitModel {
    val id: String

    fun startInSeconds(): Int

    fun requestCount(): Int

    fun toResult(
        startInSeconds: Int = startInSeconds(),
        requestCount: Int = requestCount(),
        exceeded: Boolean = false
    ): Result {
        return Result(startInSeconds, requestCount, exceeded)
    }
}
