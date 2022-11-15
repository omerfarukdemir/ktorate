package io.github.omerfarukdemir.ktorate.storages

import io.github.omerfarukdemir.ktorate.models.FixedWindowModel

class InMemoryFixedWindowStorage : AbstractInMemoryStorage<FixedWindowModel>(), FixedWindowStorage
