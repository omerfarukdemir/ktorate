# ktorate

Naive ktor rate limiter plugin

### Supported Strategies

- Fixed Window
- Sliding Window
- Sliding Log

### Supported Data Stores

- In Memory

### Examples

- Default Config (do not rely on defaults)

```kotlin
fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::inMemory).start(wait = true)
}

fun Application.inMemory() {
    install(Ktorate)

    routing { get("/") { call.respondText("Evet") } }
}
```

- Customized Options

```kotlin
fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::inMemory).start(wait = true)
}

fun Application.inMemory() {
    // identity resolver function
    fun getUserId(request: ApplicationRequest): String {
        return UUID.randomUUID().toString()
    }

    install(Ktorate, configure = {
        duration = 1.hours                     // strategy window
        limit = 1000                           // max request in duration by defined strategy
        deleteExpiredRecordsPeriod = 5.minutes // to remove expired records in data store
        identityFunction = ::getUserId         // default is client's IP
        synchronizedReadWrite = true           // blocking ops between read and write ops (only for same identity)
    })

    routing { get("/") { call.respondText("Evet") } }
}
```

- Strategy Selection

```kotlin
fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::inMemory).start(wait = true)
}

fun Application.inMemory() {
    install(Ktorate, configure = {
        duration = 1.hours
        limit = 100
        deleteExpiredRecordsPeriod = 5.minutes
        synchronizedReadWrite = false
        rateLimiter = SlidingWindow(duration, limit, synchronizedReadWrite) // can be FixedWindow, SlidingWindow, SlidingLog
    })

    routing { get("/") { call.respondText("Evet") } }
}
```

- Custom Storage (redis)

```kotlin
fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::redis).start(wait = true)
}

class RedisFixedWindowStorage : FixedWindowStorage {

    private val redisPool = JedisPool()
    private val gson = Gson()

    override fun get(id: String): FixedWindowModel? {
        return redisPool.resource
            .use { it.get(id) }
            ?.let { gson.fromJson(it, FixedWindowModel::class.java) }
    }

    override fun upsert(model: FixedWindowModel): FixedWindowModel {
        val jsonString = gson.toJson(model)

        return model.also { redisPool.resource.use { it.set(model.id, jsonString) } }
    }

    override fun all(): Collection<FixedWindowModel> {
        val keys = redisPool.resource.use { it.keys("*") }

        return if (keys.isEmpty()) listOf()
        else redisPool.resource
            .use { it.mget(*keys.toTypedArray()) }
            .map { gson.fromJson(it, FixedWindowModel::class.java) }
    }

    override fun delete(id: String): Boolean {
        return redisPool.resource.use { it.del(id) == 1L }
    }

    override fun delete(ids: Collection<String>): Int {
        return redisPool.resource.use { it.del(*ids.toTypedArray()).toInt() }
    }
}

fun Application.redis() {
    install(ktorate, configure = {
        duration = 3.seconds
        limit = 5
        deleteExpiredRecordsPeriod = 5.seconds
        synchronizedReadWrite = true
        rateLimiter = FixedWindow(
            duration = duration,
            limit = limit,
            synchronizedReadWrite = synchronizedReadWrite,
            storage = RedisFixedWindowStorage()
        )
    })

    routing { get("/") { call.respondText("Evet") } }
}
```

- Custom Storage (postgresql with ktorm query dsl)

```kotlin
fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::postgreSQL).start(wait = true)
}

object FixedWindowTable : Table<Nothing>("fixed_window") {
    val id = varchar("id").primaryKey()
    val startInSeconds = int("start_in_seconds")
    val requestCount = int("request_count")
}

class PostgreSQLFixedWindowStorage : FixedWindowStorage {

    // make sure schema is ready bofore run this example
    // CREATE DATABASE ktorate;
    // CREATE TABLE fixed_window (id varchar NOT NULL PRIMARY KEY, start_in_seconds INT NOT NULL, request_count INT NOT NULL);

    private val database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/ktorate",
        user = "postgres",
        password = "postgres"
    )

    override fun get(id: String): FixedWindowModel? {
        return database.from(FixedWindowTable)
            .select()
            .where(FixedWindowTable.id eq id)
            .map { it.fixedWindowModel() }
            .firstOrNull()
    }

    override fun upsert(model: FixedWindowModel): FixedWindowModel {
        return model.also {
            database.insertOrUpdate(FixedWindowTable) {
                set(it.id, model.id)
                set(it.startInSeconds, model.startInSeconds)
                set(it.requestCount, model.requestCount)
                onConflict {
                    set(it.startInSeconds, model.startInSeconds)
                    set(it.requestCount, model.requestCount)
                }
            }
        }
    }

    override fun all(): Collection<FixedWindowModel> {
        return database.from(FixedWindowTable)
            .select()
            .map { it.fixedWindowModel() }
    }

    override fun delete(id: String): Boolean {
        return database.delete(FixedWindowTable) { it.id eq id } == 1
    }

    override fun delete(ids: Collection<String>): Int {
        return database.delete(FixedWindowTable) { it.id inList ids }
    }

    private fun QueryRowSet.fixedWindowModel(): FixedWindowModel {
        return FixedWindowModel(
            this[FixedWindowTable.id]!!,
            this[FixedWindowTable.startInSeconds]!!,
            this[FixedWindowTable.requestCount]!!
        )
    }
}

fun Application.postgreSQL() {
    install(ktorate, configure = {
        duration = 3.seconds
        limit = 5
        deleteExpiredRecordsPeriod = 5.seconds
        synchronizedReadWrite = true
        rateLimiter = FixedWindow(
            duration = duration,
            limit = limit,
            synchronizedReadWrite = synchronizedReadWrite,
            storage = PostgreSQLFixedWindowStorage()
        )
    })

    routing { get("/") { call.respondText("Evet") } }
}

```

### TODO's

- Fix confusion between Configuration and RateLimiters
- Distributed optimistic lock for external storages
- Mutex improvement
- Configurable response
- Publish (github packages)
- More detailed tests
- External storage example usages (sql?, memcached)
- CI (github actions?)
- Token Bucket and Leaky Bucket strategies
- Customizable limiter by route and HTTP method
- Move external storage examples to source with its of packages
