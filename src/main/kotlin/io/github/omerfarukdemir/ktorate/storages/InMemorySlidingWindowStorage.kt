package io.github.omerfarukdemir.ktorate.storages

import io.github.omerfarukdemir.ktorate.models.SlidingWindowModel

class InMemorySlidingWindowStorage : AbstractInMemoryStorage<SlidingWindowModel>(), SlidingWindowStorage
