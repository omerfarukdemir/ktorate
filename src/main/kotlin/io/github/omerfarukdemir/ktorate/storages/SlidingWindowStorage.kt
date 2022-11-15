package io.github.omerfarukdemir.ktorate.storages

import io.github.omerfarukdemir.ktorate.models.SlidingWindowModel

interface SlidingWindowStorage : RateLimitStorage<SlidingWindowModel>
