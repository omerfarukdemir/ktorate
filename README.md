# ktorate
naive ktor rate limiter plugin
### Supported Strategies
- Token Bucket &#x2612;
- Leaky Bucket &#x2612;
- Fixed Window &#x2611;
- Sliding Window &#x2611;
- Sliding Log &#x2611;
### Supported Data Stores
- In Memory
### Examples
- Default Config (do not rely on defaults)
```
fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    install(RateLimiter)

    routing { get("/") { call.respondText("Evet") } }
}
```
- Customized Options
```
fun Application.module() {
    // user resolver function
    fun getUserId(): String {
        return UUID.randomUUID().toString()
    }
    
    install(RateLimiter, configure = {
        duration = 1.hours                     // strategy window
        limit = 1000                           // max request in duration by defined strategy
        deleteExpiredRecordsPeriod = 5.minutes // to remove expired records in data store
        identityFunction = { getUserId() }     // default is client's IP
        synchronizedReadWrite = true           // blocking ops between read and write ops (only for same request dentity)
    })

    routing { get("/") { call.respondText("Evet") } }
}
```
- Strategy Selection
```
fun Application.module() {
    install(RateLimiter, configure = {
        duration = 1.hours
        limit = 100
        deleteExpiredRecordsPeriod = 5.minutes
        synchronizedReadWrite = false,
        rateLimiter = SlidingWindow(duration, limit, synchronizedReadWrite) // can be FixedWindow, SlidingWindow, SlidingLog
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
- External storage example usages (redis, sql?, memcached)
- CI (github actions?)
- Token Bucket and Leaky Bucket strategies
- Customizable limiter by route and HTTP method
- Batch delete support in RateLimitStorage for external storages
