# Global Behavior & Language Policy
- **Output Language**: English ONLY. (Mandatory for all responses and internal reasoning).
- **Token Optimization**: Eliminate all greetings, apologies, and conversational fluff. Start responses directly with technical information.
- **Priority**: Maintain 100% fidelity to the technical constraints defined in the original rules below.

# gRPC & Proto3 Conventions

## Package Naming

| Rule | Convention | Example |
|------|-----------|---------|
| Format | Reverse domain + service + version | `com.company.payments.v1` |
| Version suffix | Always include major version | `v1`, `v2` |
| Lowercase only | No uppercase in package names | `com.company.order_service.v1` |
| Word separation | Underscores for multi-word segments | `payment_gateway`, not `paymentGateway` |

Each `.proto` file MUST declare its package. Files in the same logical service MUST share the same package prefix.

## Service Naming

| Rule | Convention |
|------|-----------|
| Case | PascalCase |
| Suffix | Always end with `Service` |
| Scope | One service per bounded context |
| File | One service definition per `.proto` file |

Examples: `OrderService`, `PaymentGatewayService`, `NotificationService`

NEVER combine unrelated RPCs in a single service. If a service exceeds 10 RPC methods, evaluate splitting by subdomain.

## Message Naming

| Element | Convention | Example |
|---------|-----------|---------|
| Messages | PascalCase | `CreateOrderRequest`, `OrderResponse` |
| Request messages | Suffix with `Request` | `GetOrderRequest`, `ListOrdersRequest` |
| Response messages | Suffix with `Response` | `GetOrderResponse`, `ListOrdersResponse` |
| Wrapper messages | Describe content | `OrderDetails`, `PaymentSummary` |
| Nested messages | PascalCase, scoped to parent | `Order.LineItem` |

Every RPC MUST have its own dedicated Request and Response message type. NEVER reuse a single message across multiple RPCs even if the fields are identical today -- they will diverge.

## Field Naming

| Rule | Convention | Example |
|------|-----------|---------|
| Case | snake_case | `order_id`, `created_at`, `line_items` |
| Boolean prefix | Use `is_`, `has_`, `can_`, `should_` | `is_active`, `has_discount` |
| ID fields | Suffix with `_id` | `customer_id`, `order_id` |
| Timestamp fields | Suffix with `_at` or `_time` | `created_at`, `expiry_time` |
| Duration fields | Suffix with unit or use `google.protobuf.Duration` | `timeout_seconds` or Duration type |

Field numbers 1-15 use 1 byte on the wire; reserve these for frequently populated fields. Field numbers 16-2047 use 2 bytes.

## Enum Conventions

| Rule | Convention |
|------|-----------|
| Enum name | PascalCase |
| Values | UPPER_SNAKE_CASE |
| Zero value | MUST be `UNSPECIFIED` and represent unknown/default |
| Prefix | All values prefixed with enum name |

Example structure:
- `OrderStatus` enum values: `ORDER_STATUS_UNSPECIFIED = 0`, `ORDER_STATUS_PENDING = 1`, `ORDER_STATUS_APPROVED = 2`, `ORDER_STATUS_REJECTED = 3`

**Rules:**
- The zero value MUST always be `_UNSPECIFIED` and MUST NOT carry business meaning
- NEVER reuse numeric values (even after deprecation; use `reserved` instead)
- Prefix all values with the enum type name to avoid namespace collisions
- New values MUST be added at the end (after the highest existing number)

## Oneof Usage Guidelines

| Use When | Avoid When |
|----------|------------|
| Exactly one of several fields must be set | Fields are independently optional |
| Polymorphic payloads (payment by card vs bank) | All fields can coexist |
| Mutually exclusive configuration options | Default behavior is acceptable |

**Rules:**
- Oneof fields MUST NOT be `repeated`
- Oneof fields MUST NOT be `map`
- Always handle the "none set" case in application code
- Document which oneof variant is expected for each use case

## Repeated vs Map Field Choices

| Use `repeated` When | Use `map` When |
|---------------------|----------------|
| Order matters | Lookup by key is primary access pattern |
| Duplicates are possible/valid | Keys are unique identifiers |
| Elements are complex messages | Values are simple or messages keyed by string/int |
| Pagination or streaming of elements | Metadata, labels, or configuration key-value pairs |

**Map constraints:**
- Keys MUST be string or integer types (not float, bytes, or enum)
- Maps are unordered; do not depend on iteration order
- NEVER use map when order is significant

## Streaming Patterns

| Pattern | Direction | Use Cases |
|---------|-----------|-----------|
| Unary | Request -> Response | Standard CRUD, simple queries |
| Server streaming | Request -> stream Response | Real-time feeds, large result sets, event subscriptions |
| Client streaming | stream Request -> Response | File upload, batch ingestion, telemetry reporting |
| Bidirectional streaming | stream Request <-> stream Response | Chat, collaborative editing, real-time sync |

**Guidelines:**
- Prefer unary RPCs for simple operations; do not over-use streaming
- Server streaming: send an initial metadata frame for client to validate before data flows
- Client streaming: respond with summary/acknowledgment after stream completes
- Bidirectional: define clear message sequencing protocol; document who sends first
- All streaming RPCs MUST handle cancellation gracefully
- Set maximum message size per stream; reject streams that exceed limits

## Deadline Propagation

- Every RPC call MUST set a deadline (timeout)
- Deadlines propagate automatically across service boundaries in gRPC
- Parent deadline MUST be shorter than or equal to the sum of child deadlines
- Recommended defaults: internal service calls 5s, external-facing 30s, batch operations 120s
- NEVER use infinite deadlines; always set an upper bound
- Check `context.Err()` / equivalent at each processing stage; abort early if deadline exceeded
- Log deadline violations with upstream service identity for capacity planning

## Error Model

### Standard Error Codes

| Code | Name | When to Use |
|------|------|-------------|
| 0 | OK | Success |
| 1 | CANCELLED | Client cancelled the request |
| 2 | UNKNOWN | Unknown error (catch-all; avoid) |
| 3 | INVALID_ARGUMENT | Client sent invalid data (validation) |
| 4 | DEADLINE_EXCEEDED | Operation timed out |
| 5 | NOT_FOUND | Resource does not exist |
| 6 | ALREADY_EXISTS | Duplicate creation attempt |
| 7 | PERMISSION_DENIED | Authenticated but not authorized |
| 8 | RESOURCE_EXHAUSTED | Rate limit or quota exceeded |
| 9 | FAILED_PRECONDITION | System not in required state |
| 10 | ABORTED | Concurrency conflict (retry may succeed) |
| 12 | UNIMPLEMENTED | RPC not implemented |
| 13 | INTERNAL | Internal server error (do not expose details) |
| 14 | UNAVAILABLE | Service temporarily unavailable (retry with backoff) |
| 16 | UNAUTHENTICATED | Missing or invalid authentication |

### Rich Error Details

Use `google.rpc.Status` with `details` field containing well-known types:
- `google.rpc.BadRequest` -- field-level validation violations
- `google.rpc.RetryInfo` -- minimum retry delay
- `google.rpc.DebugInfo` -- internal debug context (NEVER in production)
- `google.rpc.ErrorInfo` -- machine-readable error reason and metadata

NEVER expose internal error messages or stack traces through gRPC error details in production.

## Reflection API

| Environment | Enable Reflection? | Reasoning |
|-------------|:-----------------:|-----------|
| Local development | Yes | Enables tools like `grpcurl`, `grpcui` |
| Staging/testing | Yes | Facilitates integration testing |
| Production | No | Security risk; exposes service contract |

When reflection is disabled, provide the compiled `.proto` files or a proto registry for consumers.

## Health Check Protocol

All gRPC services MUST implement the `grpc.health.v1.Health` service.

**Requirements:**
- Return `SERVING` when the service and its critical dependencies are healthy
- Return `NOT_SERVING` when the service cannot process requests
- Support per-service health checks (empty service name = overall health)
- Integrate with load balancer health probes and Kubernetes readiness checks
- Health check MUST NOT have heavy dependencies (no database queries; use cached status)
- Response time for health checks MUST be under 100ms

## Anti-Patterns (FORBIDDEN)

- Missing `UNSPECIFIED` zero value in enums -- the zero value MUST always exist and mean "unknown"
- Reusing Request/Response messages across different RPCs -- each RPC gets its own pair
- Field numbers that skip values without `reserved` declarations
- Streaming RPCs without cancellation handling
- RPCs without deadlines -- every call MUST have a timeout
- Using `google.protobuf.Struct` or `google.protobuf.Any` as a lazy alternative to proper schema design
- Exposing reflection in production
- Health check endpoints that perform expensive operations
- Returning `UNKNOWN` error code for errors that have a specific code
- Mixing business logic error details with transport-level errors
- Proto files without package declaration
- Services with more than 10 RPCs that should be split
- Using `float`/`double` for monetary values -- use `int64` cents or `string` with explicit precision


---

# Global Behavior & Language Policy
- **Output Language**: English ONLY. (Mandatory for all responses and internal reasoning).
- **Token Optimization**: Eliminate all greetings, apologies, and conversational fluff. Start responses directly with technical information.
- **Priority**: Maintain 100% fidelity to the technical constraints defined in the original rules below.

# gRPC Versioning Conventions

## Package-Based Versioning

The package name is the primary versioning mechanism for gRPC services.

| Version | Package | When to Create |
|---------|---------|----------------|
| v1 | `com.company.orders.v1` | Initial stable release |
| v2 | `com.company.orders.v2` | Breaking changes required |
| v1alpha1 | `com.company.orders.v1alpha1` | Experimental, no stability guarantees |
| v1beta1 | `com.company.orders.v1beta1` | Feature-complete, may change before GA |

**Rules:**
- Major version in package name: `v1`, `v2`, `v3`
- Pre-release qualifiers: `alpha` and `beta` with numeric suffix
- Alpha APIs may break at any time without notice
- Beta APIs will provide at least 30 days notice before breaking changes
- GA (v1, v2) APIs follow the backward compatibility rules below
- Each version lives in its own directory: `proto/orders/v1/`, `proto/orders/v2/`
- Multiple versions MAY run concurrently on the same server with independent service registrations

## Backward Compatibility Rules

### Safe Changes (Non-Breaking)

| Change | Impact | Notes |
|--------|--------|-------|
| Add new field to a message | None | Existing clients ignore unknown fields |
| Add new RPC to a service | None | Existing clients do not call new RPCs |
| Add new enum value | Low risk | Clients MUST handle unknown enum values gracefully |
| Add new message type | None | Unused by existing clients |
| Add new service | None | Independent registration |
| Rename a field | None on wire | Field number stays the same; wire format unchanged |
| Add new oneof field | Low risk | Existing clients see the new field as a regular optional |

### Unsafe Changes (Breaking -- Require New Major Version)

| Change | Why It Breaks |
|--------|---------------|
| Remove or rename a field number | Existing clients will read wrong data or crash |
| Change a field type | Wire encoding changes; deserialization fails |
| Change a field number | Different wire position; data corruption |
| Remove an RPC method | Existing clients get UNIMPLEMENTED |
| Change RPC request or response type | Serialization mismatch |
| Remove an enum value | Clients using that value get unexpected behavior |
| Change enum numeric assignment | Clients decode to wrong value |
| Move a field into or out of a oneof | Wire format changes for that field |
| Change stream to unary or vice versa | Protocol mismatch |
| Change the package name of an existing version | Fully qualified names change; breaks routing |

### Conditional Changes (Require Careful Evaluation)

| Change | Condition for Safety |
|--------|---------------------|
| Change field from optional to repeated | Safe only if no client depends on single-value behavior |
| Add a required field (proto3 has no required) | Proto3 fields are always optional; this is a semantic concern |
| Change default behavior of zero value | Document clearly; coordinate with all consumers |

## Field Deprecation

### Marking Fields as Deprecated

Use the `deprecated` option to signal that a field should no longer be used:

**Deprecation process:**
1. Add `[deprecated = true]` option to the field
2. Add a comment above the field explaining the replacement
3. Update documentation to reference the new field
4. Set a sunset date (minimum 2 release cycles or 90 days)
5. After sunset, replace with `reserved` (never delete silently)

### Timeline

| Phase | Duration | Action |
|-------|----------|--------|
| Announcement | Day 0 | Mark field deprecated, document replacement |
| Migration | 90 days minimum | Both old and new fields accepted; log usage of deprecated field |
| Sunset warning | 2 weeks before removal | Emit warnings for clients still using deprecated field |
| Removal | After sunset | Move field number and name to `reserved` |

## Reserved Fields and Numbers

### When to Use Reserved

| Scenario | Reserve What |
|----------|-------------|
| Field removed after deprecation | Both field number AND field name |
| Field removed for safety | Both field number AND field name |
| Numbers pre-allocated for future use | Field numbers only |

### Rules

- When removing a field, ALWAYS reserve both the number and the name
- Reserving the number prevents wire-format collisions with future fields
- Reserving the name prevents code-generation collisions if someone reuses the name
- Reserved ranges can be specified: `reserved 6, 8 to 12;`
- Reserved names are specified separately: `reserved "old_field", "legacy_status";`
- NEVER reuse a field number that was previously assigned, even if the old field was removed long ago
- Maintain a comment block documenting why each reservation exists

### Reserved Number Ranges

| Range | Purpose |
|-------|---------|
| Specific numbers (e.g., `5, 8, 11`) | Previously used, now removed fields |
| Number ranges (e.g., `100 to 199`) | Pre-allocated for a future feature area |
| High ranges (e.g., `900 to 999`) | Extension fields for third-party integrations |

## Migration Strategies Between Versions

### Strategy 1: Parallel Deployment (Recommended)

| Step | Action |
|------|--------|
| 1 | Deploy v2 service alongside v1 on the same server |
| 2 | Route new clients to v2; existing clients remain on v1 |
| 3 | Provide adapter/translation layer if v1 and v2 share storage |
| 4 | Monitor v1 traffic; notify remaining consumers |
| 5 | Sunset v1 after all consumers migrate |

**Advantages:** Zero downtime, gradual migration, rollback is trivial.
**Constraints:** Server must handle both versions; shared state requires translation.

### Strategy 2: Gateway Translation

| Step | Action |
|------|--------|
| 1 | Deploy v2 service only |
| 2 | API gateway translates v1 requests to v2 format |
| 3 | Gateway translates v2 responses back to v1 format |
| 4 | Remove gateway translation after all clients migrate |

**Advantages:** Single backend implementation; v1 clients unaffected.
**Constraints:** Gateway must understand both schemas; adds latency; translation logic can be complex.

### Strategy 3: Client-Coordinated Migration

| Step | Action |
|------|--------|
| 1 | Publish v2 proto definitions |
| 2 | Set migration deadline (minimum 90 days for GA APIs) |
| 3 | Clients regenerate stubs and update code |
| 4 | Coordinated cutover date for all clients |
| 5 | Decommission v1 |

**Advantages:** Simplest server-side; clean cutover.
**Constraints:** Requires coordination; all clients must be ready; risky for large ecosystems.

### Migration Decision Guide

| Factor | Parallel | Gateway | Coordinated |
|--------|:--------:|:-------:|:-----------:|
| Many independent consumers | Best | Good | Poor |
| Few known consumers | Good | Good | Best |
| Complex schema changes | Good | Poor | Good |
| Minimal schema changes | Good | Best | Good |
| Need immediate v1 shutdown | Poor | Good | Best |
| Internal services only | Good | Good | Best |
| External/public API | Best | Good | Poor |

## Version Lifecycle

| Stage | Stability | Support Duration |
|-------|-----------|-----------------|
| Alpha | None | May be removed at any time |
| Beta | Partial | 90 days notice before breaking changes |
| GA (v1, v2, ...) | Full | Minimum 12 months after successor GA |
| Deprecated | Maintenance only | 6 months after deprecation announcement |
| Sunset | None | Removed; traffic rejected with UNIMPLEMENTED |

**Rules:**
- NEVER release a GA version without at least one beta cycle
- NEVER sunset a GA version without a successor GA version available
- Deprecation notice MUST include: sunset date, migration guide, successor version
- Monitor traffic to deprecated versions; alert if usage is not declining

## Anti-Patterns (FORBIDDEN)

- Reusing field numbers after removing a field -- always use `reserved`
- Removing fields without going through the deprecation lifecycle
- Making breaking changes within a GA version -- create a new major version
- Running alpha/beta versions in production without explicit opt-in from consumers
- Skipping beta for GA releases -- always validate with real consumers first
- Changing enum numeric values in an existing version
- Sunsetting a version without monitoring remaining traffic
- Multiple active GA major versions without a published migration guide for each
- Reserved declarations without comments explaining the reason
- Silently removing fields without `reserved` -- causes wire-format corruption risk
- Allowing deprecated fields to persist indefinitely without a sunset date
