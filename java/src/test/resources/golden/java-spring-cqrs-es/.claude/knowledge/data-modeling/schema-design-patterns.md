# Schema Design Patterns

> Advanced schema patterns for {{DB_TYPE}}. For foundational concepts (naming, mandatory columns, data types), see `database-patterns` KP and `knowledge/core/11-database-principles.md`.

## Soft Delete

Marks records as deleted instead of physically removing them.

### Implementation

```sql
ALTER TABLE orders ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE orders ADD COLUMN deleted_by TEXT;
CREATE INDEX idx_orders_deleted_at
    ON orders (deleted_at) WHERE deleted_at IS NOT NULL;
```

### Query Filter

Every query on soft-deletable tables MUST include the filter:

```sql
SELECT id, status, total FROM orders
WHERE deleted_at IS NULL;
```

### Reaping Policy

Schedule a background job to hard-delete records past retention period:

```sql
DELETE FROM orders
WHERE deleted_at < NOW() - INTERVAL '90 days';
```

### Trade-offs

| Pro | Con |
|-----|-----|
| Recoverable deletion | Increased storage |
| Audit trail for deletes | Every query needs filter |
| Referential integrity preserved | Unique constraints need partial index |

### Partial Unique Index

```sql
CREATE UNIQUE INDEX uq_orders_code_active
    ON orders (order_code)
    WHERE deleted_at IS NULL;
```

---

## Temporal Tables

Track historical state of records over time.

### System-Versioned Pattern

```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    price BIGINT NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    valid_to TIMESTAMPTZ NOT NULL DEFAULT '9999-12-31'::TIMESTAMPTZ
);

CREATE TABLE products_history (
    LIKE products INCLUDING ALL
);
```

### Trigger for History Tracking

```sql
CREATE FUNCTION track_product_history()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE products SET valid_to = NOW()
    WHERE id = OLD.id AND valid_to = '9999-12-31';
    INSERT INTO products_history SELECT OLD.*;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

### Point-in-Time Query

```sql
SELECT * FROM products
WHERE valid_from <= '2025-06-15'::TIMESTAMPTZ
  AND valid_to > '2025-06-15'::TIMESTAMPTZ;
```

### Trade-offs

| Pro | Con |
|-----|-----|
| Full history available | Storage grows with changes |
| Point-in-time queries | Complex join logic |
| Audit compliance | Trigger overhead on writes |

---

## Audit Trails

Dedicated audit logging for compliance and change tracking.

### Separate Audit Table

```sql
CREATE TABLE audit_trail (
    id BIGSERIAL PRIMARY KEY,
    entity_type TEXT NOT NULL,
    entity_id BIGINT NOT NULL,
    action TEXT NOT NULL,
    old_values JSONB,
    new_values JSONB,
    performed_by TEXT NOT NULL,
    performed_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_audit_entity
    ON audit_trail (entity_type, entity_id);
CREATE INDEX idx_audit_performed_at
    ON audit_trail (performed_at);
```

### Rules

- NEVER store audit data in the same table as operational data
- NEVER allow UPDATE or DELETE on audit tables
- Include correlation ID for distributed tracing
- Partition audit tables by time for query performance
- Encrypt sensitive field values in old/new JSONB columns

---

## Multi-Tenant Schemas

Data isolation strategies for SaaS applications.

### Strategy Comparison

| Strategy | Isolation | Complexity | Cost |
|----------|-----------|------------|------|
| Shared schema + tenant column | Low | Low | Low |
| Schema per tenant | Medium | Medium | Medium |
| Database per tenant | High | High | High |

### Shared Schema with Row-Level Security

```sql
ALTER TABLE orders ADD COLUMN tenant_id BIGINT NOT NULL;
CREATE INDEX idx_orders_tenant ON orders (tenant_id);

CREATE POLICY tenant_isolation ON orders
    USING (tenant_id = current_setting('app.tenant_id')::BIGINT);
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
```

### Application-Level Enforcement

- Set tenant context at request entry point
- Validate tenant_id in every repository query
- Never expose tenant_id in API responses
- Use connection-pool-per-tenant for schema isolation

---

## Slowly Changing Dimensions (SCD)

Patterns for tracking dimension changes in analytical models.

### SCD Type 1 -- Overwrite

Replace old value with new. No history preserved.

```sql
UPDATE customers SET address = 'New Address'
WHERE id = 42;
```

**Use when:** History is not needed (e.g., correcting typos).

### SCD Type 2 -- Add New Row

Create new record with validity period. Full history preserved.

```sql
UPDATE customers SET valid_to = NOW()
WHERE id = 42 AND valid_to = '9999-12-31';

INSERT INTO customers (id, name, address, valid_from, valid_to)
VALUES (42, 'Jane', 'New Address', NOW(), '9999-12-31');
```

**Use when:** Full history is required for compliance or analytics.

### SCD Type 3 -- Add Column

Store previous and current values in separate columns.

```sql
ALTER TABLE customers
    ADD COLUMN previous_address VARCHAR(500),
    ADD COLUMN address_changed_at TIMESTAMPTZ;

UPDATE customers
SET previous_address = address,
    address = 'New Address',
    address_changed_at = NOW()
WHERE id = 42;
```

**Use when:** Only the most recent change needs tracking.

### Comparison

| Type | History Depth | Storage | Query Complexity |
|------|--------------|---------|-----------------|
| Type 1 | None | Minimal | Simple |
| Type 2 | Full | Grows with changes | Moderate |
| Type 3 | One level | Fixed overhead | Simple |
