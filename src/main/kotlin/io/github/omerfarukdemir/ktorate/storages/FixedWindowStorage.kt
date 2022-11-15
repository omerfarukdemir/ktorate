package io.github.omerfarukdemir.ktorate.storages

import io.github.omerfarukdemir.ktorate.models.FixedWindowModel

interface FixedWindowStorage : RateLimitStorage<FixedWindowModel>
