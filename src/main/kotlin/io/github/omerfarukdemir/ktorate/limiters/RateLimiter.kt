package io.github.omerfarukdemir.ktorate.limiters

import io.github.omerfarukdemir.ktorate.Result
import io.github.omerfarukdemir.ktorate.models.RateLimitModel
import io.github.omerfarukdemir.ktorate.storages.RateLimitStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.time.Duration

abstract class RateLimiter(
    duration: Duration,
    private val synchronizedReadWrite: Boolean,
    private val storage: RateLimitStorage<RateLimitModel>
) {

    private val idMutexMapping: ConcurrentMap<String, WeakReference<Mutex>> by lazy { ConcurrentHashMap() }

    protected val durationInSeconds = duration.inWholeSeconds.toInt()

    protected abstract suspend fun apply(id: String, nowInSeconds: Int): Result

    protected abstract fun expired(model: RateLimitModel, nowInSeconds: Int): Boolean

    suspend fun rate(id: String, nowInSeconds: Int): Result {
        if (synchronizedReadWrite) mutex(id).withLock { return apply(id, nowInSeconds) }
        else return apply(id, nowInSeconds)
    }

    suspend fun deleteExpiredRecords(nowInSeconds: Int): Int {
        val expiredIds = storage.all()
            .filter { expired(it, nowInSeconds) }
            .map { it.id }

        return if (synchronizedReadWrite) {
            expiredIds.map { mutex(it).withLock { storage.delete(it)} }.count { it }
        } else {
            storage.delete(expiredIds)
        }
    }

    private fun mutex(id: String): Mutex {
        return idMutexMapping.getOrPut(id) { WeakReference(Mutex()) }.get()!!
    }
}
