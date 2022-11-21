package io.github.omerfarukdemir.ktorate.utils

object Now {
    private const val millisInSecond = 1000

    fun seconds(): Int {
        return (System.currentTimeMillis() / millisInSecond).toInt()
    }
}
