package io.github.omerfarukdemir.ktorate.storages

import io.github.omerfarukdemir.ktorate.models.SlidingLogModel

interface SlidingLogStorage : RateLimitStorage<SlidingLogModel>
