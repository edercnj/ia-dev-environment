# Rule 10 — Anti-Patterns (java + quarkus)

> Language-specific anti-patterns with incorrect and correct code examples.
> Each entry references the rule or knowledge pack it violates.

## Anti-Patterns

### ANTI-001: God Service (CRITICAL)
**Category:** SERVICE_LAYER
**Rule violated:** `03-coding-standards.md#solid` (SRP)

**Incorrect code:**
```java
// Service with multiple responsibilities — violates SRP
@ApplicationScoped
public class OrderService {
    public Order createOrder(OrderRequest req) { /* ... */ }
    public void sendEmail(String to, String body) { /* ... */ }
    public Invoice generateInvoice(Order order) { /* ... */ }
    public void updateInventory(Order order) { /* ... */ }
}
```

**Correct code:**
```java
// Each service has a single responsibility
@ApplicationScoped
public class OrderService {
    private final InventoryPort inventoryPort;
    private final NotificationPort notificationPort;

    @Inject
    OrderService(InventoryPort inventoryPort,
                 NotificationPort notificationPort) {
        this.inventoryPort = inventoryPort;
        this.notificationPort = notificationPort;
    }

    public Order createOrder(OrderRequest req) {
        Order order = Order.create(req);
        inventoryPort.reserve(order.items());
        notificationPort.orderCreated(order);
        return order;
    }
}
```

### ANTI-002: Controller Calling Repository Directly (CRITICAL)
**Category:** SERVICE_LAYER
**Rule violated:** `04-architecture-summary.md` (layer rules)

**Incorrect code:**
```java
// Resource bypasses application layer
@Path("/users")
public class UserResource {
    @Inject
    UserRepository userRepository;

    @GET
    @Path("/{id}")
    public User getUser(@PathParam("id") Long id) {
        return userRepository.findById(id);
    }
}
```

**Correct code:**
```java
// Resource delegates to use case
@Path("/users")
public class UserResource {
    private final FindUserUseCase findUserUseCase;

    @Inject
    UserResource(FindUserUseCase findUserUseCase) {
        this.findUserUseCase = findUserUseCase;
    }

    @GET
    @Path("/{id}")
    public UserResponse getUser(@PathParam("id") Long id) {
        return findUserUseCase.execute(id);
    }
}
```

### ANTI-003: @Transactional on Private Method (HIGH)
**Category:** TRANSACTION
**Rule violated:** `03-coding-standards.md` (framework idioms)

**Incorrect code:**
```java
// CDI proxy cannot intercept private methods
@ApplicationScoped
public class PaymentService {
    @Transactional
    private void processPayment(Payment payment) {
        paymentRepository.persist(payment);
        auditRepository.log(payment);
    }
}
```

**Correct code:**
```java
// @Transactional on public method so CDI proxy intercepts
@ApplicationScoped
public class PaymentService {
    @Transactional
    public void processPayment(Payment payment) {
        paymentRepository.persist(payment);
        auditRepository.log(payment);
    }
}
```

### ANTI-004: Optional.get() Without isPresent() (HIGH)
**Category:** ERROR_HANDLING
**Rule violated:** `03-coding-standards.md#error-handling`

**Incorrect code:**
```java
// NoSuchElementException at runtime if empty
public User findUser(Long id) {
    Optional<User> user = userRepository.findByIdOptional(id);
    return user.get(); // throws if absent
}
```

**Correct code:**
```java
// Safe handling with orElseThrow providing context
public User findUser(Long id) {
    return userRepository.findByIdOptional(id)
            .orElseThrow(() -> new UserNotFoundException(
                    "User not found: id=%d".formatted(id)));
}
```

### ANTI-005: Returning Entity Without DTO (HIGH)
**Category:** SERVICE_LAYER
**Rule violated:** `04-architecture-summary.md` (layer rules)

**Incorrect code:**
```java
// Exposes internal Panache entity and lacks pagination
@GET
public List<Product> listProducts() {
    return Product.listAll();
}
```

**Correct code:**
```java
// Uses DTO and pagination, never exposes entity
@GET
public PanacheQuery<ProductResponse> listProducts(
        @QueryParam("page") int page,
        @QueryParam("size") int size) {
    return productUseCase.list(page, size);
}
```

### ANTI-006: Silent Exception Swallowing (CRITICAL)
**Category:** ERROR_HANDLING
**Rule violated:** `03-coding-standards.md#error-handling`

**Incorrect code:**
```java
// Exception swallowed — no logging, no re-throw
public void importData(Path file) {
    try {
        List<String> lines = Files.readAllLines(file);
        process(lines);
    } catch (Exception e) {
        // silently ignored
    }
}
```

**Correct code:**
```java
// Exception logged with context and re-thrown
public void importData(Path file) {
    try {
        List<String> lines = Files.readAllLines(file);
        process(lines);
    } catch (IOException e) {
        throw new DataImportException(
                "Failed to import file: %s"
                        .formatted(file), e);
    }
}
```

### ANTI-007: Field Injection Without Constructor (HIGH)
**Category:** SERVICE_LAYER
**Rule violated:** `03-coding-standards.md#solid` (DIP)

**Incorrect code:**
```java
// Field injection — hidden dependency, harder to test
@ApplicationScoped
public class ReportService {
    @Inject
    ReportRepository reportRepository;
    @Inject
    EmailService emailService;
}
```

**Correct code:**
```java
// Constructor injection — explicit, testable
@ApplicationScoped
public class ReportService {
    private final ReportRepository reportRepository;
    private final EmailService emailService;

    @Inject
    ReportService(ReportRepository reportRepository,
                  EmailService emailService) {
        this.reportRepository = reportRepository;
        this.emailService = emailService;
    }
}
```

### ANTI-008: Panache Entity with Business Logic (HIGH)
**Category:** PERSISTENCE
**Rule violated:** `04-architecture-summary.md` (domain purity)

**Incorrect code:**
```java
// Domain logic coupled to Panache entity
@Entity
public class Account extends PanacheEntity {
    public BigDecimal balance;

    public void withdraw(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }
        this.balance = balance.subtract(amount);
    }
}
```

**Correct code:**
```java
// Domain model is pure — no Panache dependency
public class Account {
    private final AccountId id;
    private Money balance;

    public Account withdraw(Money amount) {
        if (balance.isLessThan(amount)) {
            throw new InsufficientFundsException(id, amount);
        }
        return new Account(id, balance.subtract(amount));
    }
}

// Panache entity is in adapter layer only
@Entity
@Table(name = "accounts")
public class AccountEntity extends PanacheEntity {
    public BigDecimal balance;
}
```

### ANTI-009: Thread.sleep() in Tests (MEDIUM)
**Category:** TESTING
**Rule violated:** `05-quality-gates.md#forbidden`

**Incorrect code:**
```java
// Fragile synchronization with sleep
@Test
void asyncProcess_shouldComplete() throws Exception {
    asyncService.startProcess();
    Thread.sleep(5000); // fragile, slow
    assertThat(asyncService.isComplete()).isTrue();
}
```

**Correct code:**
```java
// Condition-based polling with timeout
@Test
void asyncProcess_shouldComplete() {
    asyncService.startProcess();
    await().atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(100))
            .untilAsserted(() ->
                    assertThat(asyncService.isComplete())
                            .isTrue());
}
```

### ANTI-010: @Scheduled with Inline Business Logic (MEDIUM)
**Category:** SERVICE_LAYER
**Rule violated:** `03-coding-standards.md#solid` (SRP)

**Incorrect code:**
```java
// Business logic mixed with scheduling concern
@ApplicationScoped
public class ReportScheduler {
    @Scheduled(every = "24h")
    void generateDailyReport() {
        List<Order> orders = orderRepo.findByDate(today());
        BigDecimal total = orders.stream()
                .map(Order::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        emailService.send("report@co.com", "Total: " + total);
    }
}
```

**Correct code:**
```java
// Scheduler delegates to use case
@ApplicationScoped
public class ReportScheduler {
    private final GenerateDailyReportUseCase useCase;

    @Inject
    ReportScheduler(GenerateDailyReportUseCase useCase) {
        this.useCase = useCase;
    }

    @Scheduled(every = "24h")
    void trigger() {
        useCase.execute();
    }
}
```

### ANTI-011: static @Inject (CRITICAL)
**Category:** SERVICE_LAYER
**Rule violated:** `03-coding-standards.md` (Quarkus CDI)

**Incorrect code:**
```java
// CDI does not inject into static fields
@ApplicationScoped
public class CacheService {
    @Inject
    static CacheManager cacheManager; // always null

    public String getCached(String key) {
        return cacheManager.get(key); // NPE at runtime
    }
}
```

**Correct code:**
```java
// Instance field injection via constructor
@ApplicationScoped
public class CacheService {
    private final CacheManager cacheManager;

    @Inject
    CacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public String getCached(String key) {
        return cacheManager.get(key);
    }
}
```

### ANTI-012: Blocking I/O in Event Loop (CRITICAL)
**Category:** CONCURRENCY
**Rule violated:** `03-coding-standards.md` (Quarkus reactive)

**Incorrect code:**
```java
// Blocking call on Vert.x event loop thread
@Path("/data")
public class DataResource {
    @GET
    public Uni<String> getData() {
        // BLOCKS the event loop — causes thread starvation
        String result = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString()).body();
        return Uni.createFrom().item(result);
    }
}
```

**Correct code:**
```java
// Non-blocking reactive call
@Path("/data")
public class DataResource {
    @Inject
    WebClient webClient;

    @GET
    public Uni<String> getData() {
        return webClient.get("/external/data")
                .send()
                .onItem()
                .transform(HttpResponse::bodyAsString);
    }
}
```

### ANTI-013: @Transactional in Reactive Context (HIGH)
**Category:** TRANSACTION
**Rule violated:** `03-coding-standards.md` (Quarkus reactive)

**Incorrect code:**
```java
// @Transactional with Uni return — transaction closes
// before async completion
@ApplicationScoped
public class OrderService {
    @Transactional
    public Uni<Order> createOrder(OrderRequest req) {
        return orderRepository.persistAndFlush(
                OrderMapper.toEntity(req))
                .map(OrderMapper::toDomain);
    }
}
```

**Correct code:**
```java
// Use Panache.withTransaction for reactive context
@ApplicationScoped
public class OrderService {
    public Uni<Order> createOrder(OrderRequest req) {
        return Panache.withTransaction(() ->
                orderRepository.persistAndFlush(
                        OrderMapper.toEntity(req))
                        .map(OrderMapper::toDomain));
    }
}
```
