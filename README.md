# ktorate

Naive ktor rate limiter plugin

### Supported Strategies

- Fixed Window
- Sliding Window
- Sliding Log

### Built-in Data Stores

- In Memory ([you can implement your one as well](https://github.com/omerfarukdemir/ktorate#custom-storage-examples))

### Examples

- Default Config (do not rely on defaults)

```kotlin
fun main() {
    embeddedServer(Netty, module = Application::inMemory).start(wait = true)
}

fun Application.inMemory() {
    install(Ktorate)

    routing { get("/") { call.respondText("Evet") } }
}
```

- Options

```kotlin
fun main() {
    embeddedServer(Netty, module = Application::inMemory).start(wait = true)
}

fun Application.inMemory() {
    // identity resolver function
    fun getUserId(request: ApplicationRequest): String {
        return UUID.randomUUID().toString()
    }

    install(Ktorate) {
        // to remove expired records in data store
        deleteExpiredRecordsPeriod = 5.minutes

        // default is client's IP
        identityFunction = ::getUserId

        rateLimiter = FixedWindow(
            // strategy window
            duration = 5.seconds,

            // max request in duration by defined strategy
            limit = 5,

            // blocking ops between read and write ops (only for same identity)
            synchronizedReadWrite = true
        )

        // count starting path with "/v1/api/" urls
        includedPaths = listOf(Regex("^/api/v1/.*$"))

        // do not count .html urls
        excludedPaths = listOf(Regex("^.*html$"))
    }

    routing { get("/") { call.respondText("Evet") } }
}
```

### Custom Storage Examples

- Redis ([jedis](https://github.com/omerfarukdemir/ktorate/tree/develop/src/test/kotlin/io/github/omerfarukdemir/ktorate/examples/redis/JedisApplication.kt))

- PostgreSQL ([exposed](https://github.com/omerfarukdemir/ktorate/tree/develop/src/test/kotlin/io/github/omerfarukdemir/ktorate/examples/postgresql/ExposedApplication.kt), [ktorm](https://github.com/omerfarukdemir/ktorate/tree/develop/src/test/kotlin/io/github/omerfarukdemir/ktorate/examples/postgresql/KtormApplication.kt))

### TODO's

- Distributed optimistic lock for external storages
- Mutex improvement
- Configurable response
- More detailed tests
- Token Bucket and Leaky Bucket strategies
