# Test Data Patterns

> Strategies for creating, managing, and protecting test data. For test framework conventions and coverage thresholds, see `testing` KP.

## Test Data Factories

Programmatic generation of test data with randomized but valid values.

### Factory Pattern

```java
public class OrderFactory {

    private static final Faker FAKER = new Faker();

    public static Order create() {
        return new Order(
            generateId(),
            FAKER.commerce().productName(),
            randomAmount(),
            OrderStatus.PENDING,
            Instant.now());
    }

    public static Order withStatus(OrderStatus status) {
        Order base = create();
        return new Order(
            base.id(), base.product(),
            base.amount(), status, base.createdAt());
    }

    private static long generateId() {
        return ThreadLocalRandom.current()
            .nextLong(1, Long.MAX_VALUE);
    }

    private static long randomAmount() {
        return ThreadLocalRandom.current()
            .nextLong(100, 100_000);
    }
}
```

### Rules

- Factories MUST generate valid domain objects by default
- Use method overloads for specific test scenarios
- NEVER hardcode IDs or timestamps (risk of order-dependent tests)
- Use `ThreadLocalRandom` or `SecureRandom`, not `Math.random()`
- Keep factory classes in the test source tree only

### Uniqueness

- Generate unique identifiers per test execution
- Use UUID or random long for entity IDs
- Append random suffix to string fields that must be unique
- Use atomic counters for sequential IDs in integration tests

---

## Fixtures

Reusable predefined datasets loaded before test execution.

### SQL Fixture Files

```sql
-- fixtures/orders.sql
INSERT INTO orders (id, product, amount, status, created_at)
VALUES
    (1001, 'Widget A', 5000, 'PENDING', '2025-01-15'),
    (1002, 'Widget B', 7500, 'CONFIRMED', '2025-01-16'),
    (1003, 'Widget C', 3000, 'CANCELLED', '2025-01-17');
```

### JSON Fixtures

```json
{
  "orders": [
    {"id": 1001, "product": "Widget A", "amount": 5000},
    {"id": 1002, "product": "Widget B", "amount": 7500}
  ]
}
```

### Rules

- Store fixtures in `src/test/resources/fixtures/`
- Use descriptive file names: `orders-pending.sql`, `orders-all-statuses.sql`
- NEVER include production data or real PII in fixtures
- Clean up fixtures after each test (use `@Transactional` or truncate)
- Keep fixtures minimal: only the data needed for the test scenario

### Fixture Organization

| Type | Location | Usage |
|------|----------|-------|
| SQL scripts | `fixtures/sql/` | Database integration tests |
| JSON payloads | `fixtures/json/` | API request/response tests |
| CSV datasets | `fixtures/csv/` | Parametrized data-driven tests |

---

## Data Builders

Fluent API for constructing complex test objects with readable test code.

### Builder Pattern

```java
public class OrderBuilder {

    private long id = 1L;
    private String product = "Default Product";
    private long amount = 1000;
    private OrderStatus status = OrderStatus.PENDING;

    public static OrderBuilder anOrder() {
        return new OrderBuilder();
    }

    public OrderBuilder withId(long id) {
        this.id = id;
        return this;
    }

    public OrderBuilder withProduct(String product) {
        this.product = product;
        return this;
    }

    public OrderBuilder withAmount(long amount) {
        this.amount = amount;
        return this;
    }

    public OrderBuilder confirmed() {
        this.status = OrderStatus.CONFIRMED;
        return this;
    }

    public OrderBuilder cancelled() {
        this.status = OrderStatus.CANCELLED;
        return this;
    }

    public Order build() {
        return new Order(
            id, product, amount, status, Instant.now());
    }
}
```

### Usage in Tests

```java
Order order = OrderBuilder.anOrder()
    .withProduct("Premium Widget")
    .withAmount(15000)
    .confirmed()
    .build();
```

### Rules

- Provide sensible defaults for all fields
- Use semantic methods: `.confirmed()` not `.withStatus(CONFIRMED)`
- Keep builders in test source tree only
- Prefer builders over constructors with many parameters

---

## Anonymization

Sanitize production data for use in non-production environments.

### Techniques

| Technique | Description | Use Case |
|-----------|-------------|----------|
| Masking | Replace characters with `*` or `X` | Email, phone |
| Substitution | Replace with realistic fake values | Names, addresses |
| Shuffling | Redistribute values across records | Salaries, dates |
| Nulling | Replace with NULL | Optional sensitive fields |
| Hashing | One-way hash with salt | Identifiers needing referential integrity |

### Masking Examples

```sql
-- Email masking
UPDATE users SET email =
    CONCAT(LEFT(email, 1), '***@',
           SUBSTRING(email FROM '@(.*)$'));

-- Phone masking
UPDATE users SET phone =
    CONCAT('***-***-', RIGHT(phone, 4));

-- Name substitution with MD5-based pseudonym
UPDATE users SET name =
    CONCAT('User-', LEFT(MD5(name || 'salt'), 8));
```

### Rules

- NEVER copy production data to non-production without anonymization
- Anonymization MUST be irreversible (no way to recover original)
- Preserve data distribution and referential integrity
- Validate anonymized data still satisfies schema constraints
- Document which fields are anonymized and the technique used
- Run anonymization in a pipeline, never manually

### Compliance Mapping

| Regulation | Requirement | Technique |
|------------|------------|-----------|
| GDPR | Right to erasure | Hard delete or irreversible anonymization |
| HIPAA | De-identification | Safe Harbor or Expert Determination |
| PCI-DSS | Mask PAN display | Show first 6 + last 4 only |

---

## Database Seeding

Initialize environments with baseline data.

### Seeding Strategy

| Environment | Data Source | Volume |
|-------------|-----------|--------|
| Local dev | Seed scripts with minimal data | ~100 records |
| CI/CD | Fixtures per test suite | Per-test scope |
| Staging | Anonymized production subset | ~10% of production |
| Production | Migration scripts only | No seed data |

### Seed Script Pattern

```sql
-- seeds/V9999__seed_reference_data.sql
-- Repeatable migration for reference/lookup data

INSERT INTO countries (code, name)
VALUES ('US', 'United States'),
       ('BR', 'Brazil'),
       ('DE', 'Germany')
ON CONFLICT (code) DO NOTHING;
```

### Rules

- Seed scripts MUST be idempotent (use `ON CONFLICT DO NOTHING`)
- Separate reference data seeds from test data
- NEVER include credentials or secrets in seed data
- Version seed scripts alongside schema migrations
- Tag seed scripts as repeatable (run every time)

### Test Data Lifecycle

```
Test Start -> Load Fixtures -> Execute Test -> Assert
           -> Rollback/Truncate -> Next Test
```

- Each test MUST start with a known state
- Use database transactions for automatic rollback
- Use truncate + reseed for integration test suites
- NEVER depend on data from a previous test
