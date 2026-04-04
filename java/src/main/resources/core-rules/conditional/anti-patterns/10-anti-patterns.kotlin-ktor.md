# Rule 10 — Anti-Patterns ({LANGUAGE_NAME} + {FRAMEWORK_NAME})

> Language-specific anti-patterns with incorrect and correct code examples.
> Each entry references the rule or knowledge pack it violates.

## Anti-Patterns

### ANTI-001: Blocking Calls in Coroutine (CRITICAL)
**Category:** CONCURRENCY
**Rule violated:** `03-coding-standards.md` (coroutine patterns)

**Incorrect code:**
```kotlin
// Blocking I/O inside coroutine — blocks the dispatcher
fun Route.userRoutes() {
    get("/users/{id}") {
        // Thread.sleep blocks coroutine dispatcher
        val user = runBlocking { userService.findById(id) }
        call.respond(user)
    }
}
```

**Correct code:**
```kotlin
// Suspend function with proper dispatcher
fun Route.userRoutes() {
    get("/users/{id}") {
        val id = call.parameters["id"]
                ?: throw BadRequestException("Missing id")
        val user = withContext(Dispatchers.IO) {
            userService.findById(id)
        }
        call.respond(UserResponse.from(user))
    }
}
```

### ANTI-002: God Service (CRITICAL)
**Category:** SERVICE_LAYER
**Rule violated:** `03-coding-standards.md#solid` (SRP)

**Incorrect code:**
```kotlin
// Service with multiple responsibilities
class OrderService(
    private val db: Database,
    private val mailer: Mailer,
) {
    suspend fun createOrder(req: OrderRequest): Order { /* ... */ }
    suspend fun sendConfirmation(order: Order) { /* ... */ }
    suspend fun generateInvoice(order: Order): Invoice { /* ... */ }
    suspend fun updateStock(order: Order) { /* ... */ }
}
```

**Correct code:**
```kotlin
// Each service has a single responsibility
class OrderService(
    private val inventoryPort: InventoryPort,
    private val notificationPort: NotificationPort,
) {
    suspend fun createOrder(req: OrderRequest): Order {
        val order = Order.create(req)
        inventoryPort.reserve(order.items)
        notificationPort.orderCreated(order)
        return order
    }
}
```

### ANTI-003: Exposed Entity Without DTO (HIGH)
**Category:** SERVICE_LAYER
**Rule violated:** `04-architecture-summary.md` (layer rules)

**Incorrect code:**
```kotlin
// Exposes database entity directly in API response
get("/products") {
    val products = ProductTable.selectAll().map { it.toProduct() }
    call.respond(products) // leaks internal model
}
```

**Correct code:**
```kotlin
// Maps to response DTO before responding
get("/products") {
    val products = productUseCase.listAll()
    call.respond(products.map(ProductResponse::from))
}
```

### ANTI-004: Exception Swallowing (CRITICAL)
**Category:** ERROR_HANDLING
**Rule violated:** `03-coding-standards.md#error-handling`

**Incorrect code:**
```kotlin
// Exception silently swallowed
suspend fun importData(path: Path) {
    try {
        val lines = path.readLines()
        process(lines)
    } catch (e: Exception) {
        // silently ignored — no logging, no re-throw
    }
}
```

**Correct code:**
```kotlin
// Exception logged and re-thrown with context
suspend fun importData(path: Path) {
    try {
        val lines = path.readLines()
        process(lines)
    } catch (e: IOException) {
        throw DataImportException(
            "Failed to import file: $path", e,
        )
    }
}
```

### ANTI-005: Mutable Shared State (HIGH)
**Category:** CONCURRENCY
**Rule violated:** `03-coding-standards.md#forbidden`

**Incorrect code:**
```kotlin
// Mutable global map — race condition in coroutines
val cache = mutableMapOf<String, String>()

fun Application.configureRouting() {
    routing {
        get("/cache/{key}") {
            val key = call.parameters["key"]!!
            if (key !in cache) {
                cache[key] = expensiveLookup(key)
            }
            call.respondText(cache[key]!!)
        }
    }
}
```

**Correct code:**
```kotlin
// Thread-safe cache with coroutine mutex
class CacheService {
    private val mutex = Mutex()
    private val cache = mutableMapOf<String, String>()

    suspend fun getOrFetch(key: String): String {
        return mutex.withLock {
            cache.getOrPut(key) { expensiveLookup(key) }
        }
    }
}
```
