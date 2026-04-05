# Rule 10 — Anti-Patterns (java + spring-boot)

> Language-specific anti-patterns with incorrect and correct code examples.
> Each entry references the rule or knowledge pack it violates.

## Anti-Patterns

### ANTI-001: God Service (CRITICAL)
**Category:** SERVICE_LAYER
**Rule violated:** `03-coding-standards.md#solid` (SRP)

**Incorrect code:**
```java
// Service with multiple responsibilities — violates SRP
@Service
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
@Service
public class OrderService {
    private final InventoryPort inventoryPort;
    private final NotificationPort notificationPort;

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
// Controller bypasses application layer — architecture violation
@RestController
public class UserController {
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        return userRepository.findById(id).orElseThrow();
    }
}
```

**Correct code:**
```java
// Controller delegates to use case (application layer)
@RestController
public class UserController {
    private final FindUserUseCase findUserUseCase;

    UserController(FindUserUseCase findUserUseCase) {
        this.findUserUseCase = findUserUseCase;
    }

    @GetMapping("/users/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return findUserUseCase.execute(id);
    }
}
```

### ANTI-003: @Transactional on Private Method (HIGH)
**Category:** TRANSACTION
**Rule violated:** `03-coding-standards.md` (framework idioms)

**Incorrect code:**
```java
// Spring proxy cannot intercept private methods
@Service
public class PaymentService {
    @Transactional
    private void processPayment(Payment payment) {
        paymentRepository.save(payment);
        auditRepository.log(payment);
    }
}
```

**Correct code:**
```java
// @Transactional on public method so proxy can intercept
@Service
public class PaymentService {
    @Transactional
    public void processPayment(Payment payment) {
        paymentRepository.save(payment);
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
    Optional<User> user = userRepository.findById(id);
    return user.get(); // throws if absent
}
```

**Correct code:**
```java
// Safe handling with orElseThrow providing context
public User findUser(Long id) {
    return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(
                    "User not found: id=%d".formatted(id)));
}
```

### ANTI-005: Returning Entity Without DTO (HIGH)
**Category:** SERVICE_LAYER
**Rule violated:** `04-architecture-summary.md` (layer rules)

**Incorrect code:**
```java
// Exposes internal JPA entity and lacks pagination
@GetMapping("/products")
public List<Product> listProducts() {
    return productRepository.findAll();
}
```

**Correct code:**
```java
// Uses DTO and pagination, never exposes entity
@GetMapping("/products")
public Page<ProductResponse> listProducts(Pageable pageable) {
    return productUseCase.list(pageable)
            .map(ProductMapper::toResponse);
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

### ANTI-007: Field Injection with @Autowired (HIGH)
**Category:** SERVICE_LAYER
**Rule violated:** `03-coding-standards.md#solid` (DIP)

**Incorrect code:**
```java
// Field injection — hidden dependency, untestable
@Service
public class ReportService {
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private EmailService emailService;
}
```

**Correct code:**
```java
// Constructor injection — explicit, testable
@Service
public class ReportService {
    private final ReportRepository reportRepository;
    private final EmailService emailService;

    ReportService(ReportRepository reportRepository,
                  EmailService emailService) {
        this.reportRepository = reportRepository;
        this.emailService = emailService;
    }
}
```

### ANTI-008: @Scheduled with Inline Business Logic (MEDIUM)
**Category:** SERVICE_LAYER
**Rule violated:** `03-coding-standards.md#solid` (SRP)

**Incorrect code:**
```java
// Business logic mixed with scheduling concern
@Component
public class ReportScheduler {
    @Scheduled(cron = "0 0 2 * * ?")
    public void generateDailyReport() {
        List<Order> orders = orderRepo.findByDate(today());
        BigDecimal total = orders.stream()
                .map(Order::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        emailService.send("report@co.com",
                "Daily total: " + total);
    }
}
```

**Correct code:**
```java
// Scheduler delegates to use case
@Component
public class ReportScheduler {
    private final GenerateDailyReportUseCase useCase;

    ReportScheduler(GenerateDailyReportUseCase useCase) {
        this.useCase = useCase;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void trigger() {
        useCase.execute();
    }
}
```

### ANTI-009: JPA Entity with Business Logic (HIGH)
**Category:** PERSISTENCE
**Rule violated:** `04-architecture-summary.md` (domain purity)

**Incorrect code:**
```java
// Domain logic coupled to JPA entity
@Entity
public class Account {
    @Id private Long id;
    private BigDecimal balance;

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
// Domain model is pure — no JPA annotations
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

// JPA entity is in adapter layer only
@Entity
@Table(name = "accounts")
public class AccountEntity {
    @Id private Long id;
    private BigDecimal balance;
}
```

### ANTI-010: Thread.sleep() in Tests (MEDIUM)
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
