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

### Custom Storage Examples

- Redis ([jedis](https://github.com/omerfarukdemir/ktorate/tree/develop/src/test/kotlin/io/github/omerfarukdemir/ktorate/examples/redis/JedisApplication.kt))

- PostgreSQL ([ktorm](https://github.com/omerfarukdemir/ktorate/tree/develop/src/test/kotlin/io/github/omerfarukdemir/ktorate/examples/postgresql/KtormApplication))

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
