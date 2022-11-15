package io.github.omerfarukdemir.ktorate.storages

import io.github.omerfarukdemir.ktorate.models.SlidingLogModel

class InMemorySlidingLogStorage : AbstractInMemoryStorage<SlidingLogModel>(), SlidingLogStorage
