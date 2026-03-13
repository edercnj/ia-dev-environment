# Global Behavior & Language Policy
- **Output Language**: English ONLY. (Mandatory for all responses and internal reasoning).
- **Token Optimization**: Eliminate all greetings, apologies, and conversational fluff. Start responses directly with technical information.
- **Priority**: Maintain 100% fidelity to the technical constraints defined in the original rules below.

# OpenAPI 3.1 Specification Conventions

## Specification Version

All API specifications MUST use OpenAPI 3.1.x (aligned with JSON Schema 2020-12). Do NOT use OpenAPI 2.0 (Swagger) or 3.0.x for new projects.

## File Organization

### Single-File Approach (Small APIs)

Use a single `openapi.yaml` when the API has fewer than 10 endpoints and fewer than 15 schemas. Place at `docs/openapi.yaml` or `src/main/resources/openapi.yaml` depending on framework convention.

### Multi-File Approach (Recommended for Larger APIs)

| Path | Contents |
|------|----------|
| `openapi/openapi.yaml` | Root document: info, servers, security, tags, path references |
| `openapi/paths/` | One file per resource (e.g., `orders.yaml`, `customers.yaml`) |
| `openapi/components/schemas/` | One file per schema (e.g., `Order.yaml`, `Customer.yaml`) |
| `openapi/components/parameters/` | Reusable parameters (pagination, filters) |
| `openapi/components/responses/` | Reusable responses (errors, pagination wrappers) |
| `openapi/components/security-schemes/` | Authentication definitions |

Use `$ref` to link files. Relative paths MUST be used for internal references. All `$ref` paths MUST resolve without external network calls.

## Schema Reuse with $ref

- Every schema used in more than one endpoint MUST be defined in `components/schemas` and referenced via `$ref`
- NEVER inline complex schemas in path definitions; always extract to components
- Compose schemas with `allOf` for inheritance, `oneOf` for polymorphism, `anyOf` for flexible unions
- Use `$ref` for shared parameters (pagination, sorting, common filters) in `components/parameters`
- Use `$ref` for shared response structures (error responses, paginated wrappers) in `components/responses`

## Example Values

**Every schema and every field MUST have an `example` value.** This is mandatory, not optional.

| Element | Requirement |
|---------|-------------|
| Schema-level | `example` object showing a complete, realistic instance |
| String fields | Realistic value (not "string" or "test") |
| Numeric fields | Realistic value within documented constraints |
| Date/time fields | ISO 8601 with timezone (e.g., `2026-02-19T14:30:00Z`) |
| Enum fields | One of the valid enum values |
| Array fields | Array with 1-2 realistic items |
| Nested objects | Fully populated example |

Use `examples` (plural) for multiple named scenarios on request/response bodies to document edge cases and variants.

## Security Schemes

### Supported Schemes

| Scheme | Type | When to Use |
|--------|------|-------------|
| API Key (header) | `apiKey` in `header` | Machine-to-machine, internal services |
| API Key (query) | `apiKey` in `query` | ONLY when header is impossible (webhooks) |
| Bearer Token (JWT) | `http` with `scheme: bearer` | User-facing APIs, OAuth2 resource servers |
| OAuth2 | `oauth2` with flows | Public APIs with delegated authorization |

### Documentation Requirements

- Every security scheme MUST include a `description` explaining how to obtain credentials
- Scopes MUST be documented with human-readable descriptions
- Apply security globally in the root `security` field; override per-operation only when an endpoint has different requirements
- Public endpoints (health checks, docs) MUST explicitly declare `security: []` to indicate no auth

## Operation IDs

Operation IDs MUST follow `camelCase` with the pattern: `verb` + `Resource` (+ optional qualifier).

| Pattern | Example | Notes |
|---------|---------|-------|
| List collection | `listOrders` | Always "list" for collection GET |
| Get single | `getOrder` | Always "get" for single resource GET |
| Create | `createOrder` | Always "create" for POST |
| Full update | `updateOrder` | Always "update" for PUT |
| Partial update | `patchOrder` | Always "patch" for PATCH |
| Delete | `deleteOrder` | Always "delete" for DELETE |
| Sub-resource | `listOrderLineItems` | Parent + child resource |
| Action | `cancelOrder` | Domain verb for non-CRUD actions |

**Constraints:**
- Operation IDs MUST be unique across the entire specification
- Operation IDs MUST NOT contain underscores, hyphens, or dots
- Operation IDs are used for SDK client method generation; choose names that produce clean SDKs

## Tags for Grouping

- Every operation MUST have exactly one tag
- Tags MUST match resource names in PascalCase plural (e.g., `Orders`, `Customers`, `PaymentMethods`)
- Define tags at the root level with `name`, `description`, and optional `externalDocs`
- Order tags in the root definition to control documentation rendering order
- NEVER use more than one tag per operation (it duplicates entries in generated docs)

## Request/Response Documentation Requirements

### Request Documentation

| Element | Required | Notes |
|---------|:--------:|-------|
| `summary` on operation | Yes | Short one-line description (under 80 chars) |
| `description` on operation | Yes (if complex) | Explain business rules, side effects, preconditions |
| `description` on every parameter | Yes | Explain purpose, constraints, default value |
| `required` flag on parameters | Yes | Explicit true/false, never rely on defaults |
| `description` on request body | Yes | Explain expected payload |
| `example` on request body | Yes | Realistic, complete example |

### Response Documentation

| Element | Required | Notes |
|---------|:--------:|-------|
| All possible status codes | Yes | Document every status code the endpoint can return |
| `description` on each response | Yes | Explain when this status is returned |
| `example` on each response body | Yes | Realistic example per status code |
| Error responses | Yes | Reference shared `ProblemDetail` schema via `$ref` |
| `headers` on responses | When applicable | Rate limit headers, Location header, etc. |

## Discriminator for Polymorphic Types

Use `discriminator` with `oneOf` when a field can be one of several typed objects.

**Rules:**
- The discriminator property MUST be a required string field on every sub-schema
- Define explicit `mapping` between discriminator values and schema references
- Discriminator field name: use `type` or `kind` consistently across the API
- Each variant schema MUST include the discriminator property in its own `required` and `properties`
- Document the available discriminator values in the parent schema description

**When to use discriminator:**
- Payment methods (card, bank transfer, wallet)
- Notification channels (email, SMS, push)
- Event payloads with different structures per event type

**When NOT to use discriminator:**
- Simple optional fields (use nullable fields instead)
- Two variants that differ by only one field (use optional fields)

## Additional Conventions

### Servers

- Define at least `development`, `staging`, and `production` server URLs
- Use server variables for configurable parts (e.g., region, version)

### Schema Conventions

| Rule | Convention |
|------|-----------|
| Field names | camelCase |
| Required fields | Explicitly listed in `required` array; NEVER rely on defaults |
| Nullable fields | Use `type: ["string", "null"]` (OpenAPI 3.1 JSON Schema syntax) |
| Date fields | `format: date-time` with ISO 8601 |
| Money fields | Integer cents (`type: integer`) or `type: string` with documented precision |
| Enums | Uppercase strings (`APPROVED`, `PENDING`, `REJECTED`) |
| UUIDs | `format: uuid` |
| Emails | `format: email` |
| URIs | `format: uri` |

### Specification Validation

- Run `spectral lint` or equivalent on every CI build
- Specification MUST be valid against the OpenAPI 3.1 JSON Schema
- Breaking change detection MUST be part of the CI pipeline (use `oasdiff` or equivalent)

## Anti-Patterns (FORBIDDEN)

- Inline schemas in path definitions for types used more than once -- always extract to `components/schemas`
- Missing `example` values on schemas or fields -- every schema MUST have examples
- Generic operation IDs (`operation1`, `endpoint2`) -- use the `verbResource` convention
- Multiple tags per operation -- use exactly one tag
- Undocumented status codes -- every possible response MUST be declared
- Using `additionalProperties: true` without explicit documentation -- it hides the actual contract
- Defining security schemes but not applying them globally or per-operation
- Using `default` response as a catch-all instead of documenting specific error codes
- Referencing external URLs in `$ref` that require network access to resolve
- Missing `description` on parameters, schemas, or operations
- Using OpenAPI 2.0 / Swagger syntax in 3.1 specifications
- Specification files that do not pass linting in CI


---

# Global Behavior & Language Policy
- **Output Language**: English ONLY. (Mandatory for all responses and internal reasoning).
- **Token Optimization**: Eliminate all greetings, apologies, and conversational fluff. Start responses directly with technical information.
- **Priority**: Maintain 100% fidelity to the technical constraints defined in the original rules below.

# REST API Conventions

> Extends: `core/06-api-design-principles.md` (summary). This file is the authoritative reference for REST conventions.

## Resource Naming

| Rule | Convention | Example |
|------|-----------|---------|
| Collection resources | Plural nouns | `/orders`, `/customers`, `/invoices` |
| Multi-word resources | kebab-case | `/payment-methods`, `/line-items` |
| Singleton resources | Singular noun | `/cart`, `/profile` (per-user context) |
| Sub-resources | Nested under parent | `/orders/{id}/line-items` |
| Resource identifiers | Opaque strings or UUIDs | `/orders/550e8400-e29b-41d4` |
| Maximum nesting depth | 2 levels | `/customers/{id}/orders/{orderId}` |

**Naming constraints:**
- NEVER use verbs in URIs (`/createOrder` is forbidden; use `POST /orders`)
- NEVER use CRUD prefixes (`/getUsers` is forbidden; use `GET /users`)
- NEVER expose internal implementation (`/db/table/row` is forbidden)
- Use lowercase only in path segments

## HTTP Methods Mapping

| Method | Semantics | Idempotent | Safe | Request Body | Typical Success |
|--------|-----------|:----------:|:----:|:------------:|:---------------:|
| GET | Read resource(s) | Yes | Yes | No | 200 OK |
| POST | Create resource / trigger action | No | No | Yes | 201 Created |
| PUT | Full replace of resource | Yes | No | Yes | 200 OK |
| PATCH | Partial update of resource | No* | No | Yes | 200 OK |
| DELETE | Remove resource | Yes | No | No | 204 No Content |
| HEAD | Metadata only (same as GET, no body) | Yes | Yes | No | 200 OK |
| OPTIONS | Supported methods / CORS preflight | Yes | Yes | No | 204 No Content |

*PATCH is idempotent only when using JSON Merge Patch with the same payload repeatedly.

## Status Codes (Comprehensive)

### Success (2xx)

| Code | Meaning | When to Use |
|------|---------|-------------|
| 200 OK | Request succeeded | GET, PUT, PATCH returning updated resource |
| 201 Created | Resource created | POST; MUST include `Location` header |
| 202 Accepted | Async processing started | Long-running operations; return task/job URI |
| 204 No Content | Success with no body | DELETE; PUT/PATCH when not returning resource |

### Redirection (3xx)

| Code | Meaning | When to Use |
|------|---------|-------------|
| 301 Moved Permanently | Resource URI changed permanently | API version sunset with new URI |
| 304 Not Modified | Cached response is still valid | Conditional GET with ETag/If-None-Match |

### Client Errors (4xx)

| Code | Meaning | When to Use |
|------|---------|-------------|
| 400 Bad Request | Malformed or invalid input | Validation failures, malformed JSON |
| 401 Unauthorized | Authentication required or failed | Missing or invalid credentials |
| 403 Forbidden | Authenticated but not authorized | Insufficient permissions |
| 404 Not Found | Resource does not exist | Unknown ID or URI |
| 405 Method Not Allowed | HTTP method not supported | PUT on a read-only resource |
| 406 Not Acceptable | Cannot produce requested format | Unsupported Accept header |
| 409 Conflict | State conflict | Duplicate creation, concurrent edit collision |
| 410 Gone | Resource permanently deleted | Distinguishes from 404 for hard-deleted resources |
| 412 Precondition Failed | Conditional update failed | Optimistic locking with If-Match / ETag |
| 415 Unsupported Media Type | Wrong Content-Type | Sending XML when only JSON is accepted |
| 422 Unprocessable Entity | Semantic validation failure | Well-formed but business-rule-invalid request |
| 429 Too Many Requests | Rate limit exceeded | MUST include `Retry-After` header |

### Server Errors (5xx)

| Code | Meaning | When to Use |
|------|---------|-------------|
| 500 Internal Server Error | Unexpected failure | Unhandled exceptions (never expose details) |
| 502 Bad Gateway | Upstream service failure | Downstream dependency returned invalid response |
| 503 Service Unavailable | Temporarily overloaded | MUST include `Retry-After` header |
| 504 Gateway Timeout | Upstream timeout | Downstream dependency did not respond in time |

## RFC 7807 Error Format (Problem Details)

All error responses MUST use `application/problem+json` content type.

**Required fields:**

| Field | Type | Description |
|-------|------|-------------|
| type | URI | Machine-readable error type identifier (e.g., `/errors/validation-error`) |
| title | string | Short human-readable summary of the problem type |
| status | integer | HTTP status code (duplicated for convenience in logs) |
| detail | string | Human-readable explanation specific to this occurrence |
| instance | URI | URI that identifies the specific request (for correlation) |

**Optional extension fields:** Include additional context relevant to debugging (e.g., `violations`, `retryAfter`, `conflictingResource`). NEVER include stack traces or internal system details in production.

## Pagination

### Cursor-Based Pagination (Preferred for Large/Real-Time Data)

| Parameter | Description | Default |
|-----------|-------------|---------|
| `cursor` | Opaque token pointing to position in dataset | (none = start) |
| `limit` | Maximum items to return | 20 |

Response includes `next_cursor` (null when no more results). Use cursor-based when: data changes frequently, dataset is large (100k+ rows), consistent ordering is required, or deep pagination is expected.

### Offset-Based Pagination (Simple UI Use Cases)

| Parameter | Description | Default |
|-----------|-------------|---------|
| `page` | 0-based page index | 0 |
| `limit` | Items per page | 20 |

Response includes `total`, `totalPages`. Use offset-based when: UI needs page numbers, dataset is small/stable, or total count is needed.

**Shared constraints:**
- Default `limit`: 20
- Maximum `limit`: 100
- All list endpoints MUST be paginated with no exceptions

## Filtering with Query Parameters

- Use flat query parameters: `?status=active&type=debit`
- Date ranges: `?from=2026-01-01&to=2026-01-31` (ISO 8601)
- Multi-value: `?status=active,pending` (comma-separated) or `?status=active&status=pending` (repeated)
- Partial match: `?name=like:acme` (prefixed operator)
- Range filters: `?amount=gte:1000&amount=lte:5000`
- NEVER use request body for GET filtering

## Sorting Conventions

- Query parameter: `?sort=createdAt,desc` or `?sort=-createdAt` (prefix minus for descending)
- Multiple sort fields: `?sort=status,asc&sort=createdAt,desc`
- Default sort: MUST be documented per endpoint
- Only allow sorting on indexed fields; reject unknown sort fields with 400

## HATEOAS Decision Guide

| Scenario | Use HATEOAS? | Reasoning |
|----------|:------------:|-----------|
| Public API consumed by unknown third parties | Yes | Clients discover capabilities dynamically |
| Internal microservice-to-microservice | No | Services share contracts; links add overhead |
| API with complex state machines | Yes | Links communicate available transitions |
| Simple CRUD API with fixed workflows | No | Overhead outweighs benefit |

When using HATEOAS, include a `_links` object with `rel` and `href` for each available action on the resource.

## Versioning Strategies

| Strategy | Format | Pros | Cons |
|----------|--------|------|------|
| URI path | `/api/v1/orders` | Explicit, easy to route, cache-friendly | URL pollution, harder to sunset |
| Custom header | `Api-Version: 1` | Clean URLs | Easy to forget, not visible in browser |
| Accept header | `Accept: application/vnd.api.v1+json` | Standards-compliant | Complex, poor tooling support |
| Query param | `?version=1` | Simple | Pollutes cache keys, easy to forget |

**Recommendation:** Use URI path versioning (`/api/v1/`) for external APIs. Use header versioning only when URI versioning is impractical (e.g., GraphQL endpoints).

## Content Negotiation

- Requests MUST include `Content-Type: application/json` for bodies
- Responses MUST include `Content-Type: application/json` (or `application/problem+json` for errors)
- Support `Accept` header; return 406 if the requested format is unsupported
- Character encoding: always UTF-8

## Bulk Operations

| Operation | Method | URI | Notes |
|-----------|--------|-----|-------|
| Batch create | POST | `/orders/batch` | Body contains array of resources |
| Batch update | PATCH | `/orders/batch` | Body contains array of partial updates with IDs |
| Batch delete | POST | `/orders/batch-delete` | Body contains array of IDs |

**Constraints:**
- Maximum batch size: 100 items per request (configurable)
- Return individual results per item (success/failure) with overall 200 or 207 Multi-Status
- Batch operations MUST be atomic (all-or-nothing) OR return per-item status; document which strategy is used

## Partial Updates: PATCH Strategies

| Strategy | Content-Type | Behavior | When to Use |
|----------|-------------|----------|-------------|
| JSON Merge Patch | `application/merge-patch+json` | Null removes field; missing fields unchanged | Simple updates, no array manipulation |
| JSON Patch | `application/json-patch+json` | Operations array (add, remove, replace, move, copy, test) | Complex updates, array element manipulation |

**Default recommendation:** JSON Merge Patch for most APIs due to simplicity. Use JSON Patch only when array element operations are required.

## Rate Limiting Headers

All rate-limited endpoints MUST include these response headers:

| Header | Description | Example |
|--------|-------------|---------|
| `X-RateLimit-Limit` | Maximum requests per window | `1000` |
| `X-RateLimit-Remaining` | Requests remaining in current window | `742` |
| `X-RateLimit-Reset` | Unix timestamp when the window resets | `1706140800` |
| `Retry-After` | Seconds to wait (only on 429 responses) | `30` |

Rate limit scope: per API key, per user, or per IP -- document which applies. Use sliding window or token bucket algorithms; fixed windows create thundering herd at reset boundaries.

## Anti-Patterns (FORBIDDEN)

- Verbs in URIs (`/api/v1/createOrder`) -- use HTTP methods with noun resources
- Returning 200 for error conditions with `{ "success": false }` -- use proper HTTP status codes
- Exposing ORM/database entities directly as API responses -- always use DTOs
- Unpaginated list endpoints -- every collection MUST be paginated
- Using POST for idempotent reads -- use GET
- Nested resources deeper than 2 levels (`/a/{id}/b/{id}/c/{id}/d`) -- flatten or use query params
- Inconsistent naming (mixing camelCase and snake_case in URIs)
- Including sensitive data (passwords, tokens, secrets) in query parameters or URIs
- Ignoring `Accept` and `Content-Type` headers
- Returning 500 with stack traces in production
- Designing RPC-style endpoints disguised as REST (`/api/v1/doSomething`)
- Using DELETE with a request body (some proxies/clients drop it)
- Returning different response shapes for the same endpoint based on conditions
