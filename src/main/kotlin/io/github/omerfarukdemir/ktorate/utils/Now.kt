package io.github.omerfarukdemir.ktorate.utils

object Now {
    fun seconds(): Int {
        return (System.currentTimeMillis() / 1000).toInt()
    }
}
