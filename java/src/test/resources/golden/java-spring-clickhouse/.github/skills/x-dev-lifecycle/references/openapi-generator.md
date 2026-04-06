# OpenAPI/Swagger Documentation Generator (REST)

> **Invoke when:** The project identity's `interfaces` array contains an entry with `type: "rest"`.
> **Skip when:** No REST interface is configured. Do not generate API documentation for non-REST projects.

## Purpose

This generator instructs the documentation phase subagent to scan inbound REST adapters and produce
an OpenAPI 3.1 specification in YAML format. The output file is `contracts/api/openapi.yaml`.

---

## 1. Scanning Instructions

### 1.1 What to Scan

Scan all **inbound REST adapters** in the project source code:

- **Controllers** (annotation-based: `@Controller`, `@RestController`, `@Path`)
- **Handlers** (e.g., route handler functions)
- **Resources** (e.g., JAX-RS `@Path` annotated classes)
- **Router definitions** (e.g., Express/Gin/Axum route registrations)

Look for files in the `adapter/inbound/` or equivalent package following the project's architecture.

### 1.2 What to Extract Per Endpoint

For each discovered endpoint, extract:

| Data | Source | Example |
|------|--------|---------|
| HTTP Method | Route annotation/decorator | GET, POST, PUT, DELETE, PATCH |
| Path | Route path parameter | `/api/v1/items`, `/api/v1/items/{id}` |
| Path parameters | Route variables | `{id}`, `{slug}` |
| Query parameters | Query param annotations | `?page=1&size=20` |
| Request body DTO | Method parameter type | `CreateItemRequest` |
| Response DTO | Return type / response wrapper | `ItemResponse` |
| Status codes | Response annotations or constants | 200, 201, 204, 400, 404, 422, 500 |
| Tags | Controller/resource class name | `Items`, `Users` |

---

## 2. OpenAPI 3.1 YAML Structure

Generate the specification following OpenAPI 3.1.0 format. The output MUST be valid YAML.

### 2.1 Root Structure

```yaml
openapi: "3.1.0"
info:
  title: "my-spring-clickhouse API"
  description: "REST API specification for my-spring-clickhouse"
  version: "1.0.0"
servers:
  - url: "http://localhost:8080"
    description: "Local development"
tags:
  - name: "ResourceName"
    description: "Operations for ResourceName"
paths:
  /api/v1/resource:
    get:
      ...
    post:
      ...
components:
  schemas:
    RequestDTO:
      ...
    ResponseDTO:
      ...
```

### 2.2 Sections Required

| Section | Content | Required |
|---------|---------|----------|
| `openapi` | Version string `"3.1.0"` | Mandatory |
| `info` | API title, description, version | Mandatory |
| `info.title` | `my-spring-clickhouse API` | Mandatory |
| `info.version` | Service version from config or `"1.0.0"` | Mandatory |
| `servers` | At least local development URL | Mandatory |
| `tags` | One tag per controller/resource class | Mandatory |
| `paths` | All discovered endpoints with operations | Mandatory |
| `components/schemas` | All DTOs as JSON Schema definitions | Mandatory |

---

## 3. Path Definitions

### 3.1 Operation Object

Each endpoint operation MUST include:

```yaml
paths:
  /api/v1/items:
    get:
      summary: "List items"
      operationId: "listItems"
      tags:
        - "Items"
      parameters:
        - name: "page"
          in: "query"
          schema:
            type: "integer"
      responses:
        "200":
          description: "Successful response"
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ItemListResponse"
        "400":
          description: "Bad request"
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ProblemDetail"
    post:
      summary: "Create item"
      operationId: "createItem"
      tags:
        - "Items"
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/CreateItemRequest"
      responses:
        "201":
          description: "Resource created"
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ItemResponse"
        "400":
          description: "Validation error"
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ProblemDetail"
        "422":
          description: "Unprocessable entity"
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ProblemDetail"
```

### 3.2 Common Status Codes

Document ALL status codes returned by each endpoint:

| Code | Usage | When |
|------|-------|------|
| 200 | Success with body | GET, PUT, PATCH |
| 201 | Resource created | POST |
| 204 | Success without body | DELETE |
| 400 | Bad request | Invalid input format |
| 404 | Not found | Resource does not exist |
| 422 | Unprocessable entity | Business rule violation |
| 500 | Internal server error | Unexpected failures |

---

## 4. Schema Definitions (components/schemas)

### 4.1 Schema Deduplication with $ref

**CRITICAL:** Never duplicate schema definitions inline. All DTOs MUST be defined once in
`components/schemas` and referenced via `$ref`:

```yaml
# CORRECT — use $ref
schema:
  $ref: "#/components/schemas/ItemResponse"

# INCORRECT — inline duplication
schema:
  type: object
  properties:
    id:
      type: string
    name:
      type: string
```

### 4.2 DTO Mapping Rules

Map source DTOs to JSON Schema types:

| Common DTO Type | JSON Schema Type |
|----------------------|------------------|
| `string` | `type: "string"` |
| `number` / `int` | `type: "integer"` |
| `float` / `double` | `type: "number"` |
| `boolean` | `type: "boolean"` |
| `Date` / `Instant` | `type: "string", format: "date-time"` |
| `UUID` | `type: "string", format: "uuid"` |
| `Array<T>` / `List<T>` | `type: "array", items: { $ref: ... }` |
| `Optional<T>` | nullable field |

### 4.3 Request and Response Schema Example

```yaml
components:
  schemas:
    CreateItemRequest:
      type: object
      required:
        - name
        - category
      properties:
        name:
          type: string
          minLength: 1
          maxLength: 255
        category:
          type: string
        description:
          type: string

    ItemResponse:
      type: object
      required:
        - id
        - name
        - category
        - createdAt
      properties:
        id:
          type: string
          format: uuid
        name:
          type: string
        category:
          type: string
        description:
          type: string
        createdAt:
          type: string
          format: date-time
```

---

## 5. Error Responses — RFC 7807 (Problem Details)

All error responses MUST follow RFC 7807 Problem Details format.

### 5.1 Problem Detail Schema

```yaml
components:
  schemas:
    ProblemDetail:
      type: object
      required:
        - type
        - title
        - status
        - detail
      properties:
        type:
          type: string
          format: uri
          description: "URI reference identifying the problem type"
        title:
          type: string
          description: "Short human-readable summary"
        status:
          type: integer
          description: "HTTP status code"
        detail:
          type: string
          description: "Human-readable explanation specific to this occurrence"
        instance:
          type: string
          format: uri
          description: "URI reference identifying the specific occurrence"
```

### 5.2 Error Response Usage

Every error status code (4xx, 5xx) MUST reference the `ProblemDetail` schema:

```yaml
responses:
  "400":
    description: "Bad request"
    content:
      application/json:
        schema:
          $ref: "#/components/schemas/ProblemDetail"
  "404":
    description: "Resource not found"
    content:
      application/json:
        schema:
          $ref: "#/components/schemas/ProblemDetail"
  "422":
    description: "Unprocessable entity — business rule violation"
    content:
      application/json:
        schema:
          $ref: "#/components/schemas/ProblemDetail"
  "500":
    description: "Internal server error"
    content:
      application/json:
        schema:
          $ref: "#/components/schemas/ProblemDetail"
```

---

## 6. Output

- **File path:** `contracts/api/openapi.yaml`
- **Format:** YAML (not JSON)
- **Encoding:** UTF-8
- **Validation:** The generated spec MUST be valid OpenAPI 3.1.0

### 6.1 Incremental Updates

If `contracts/api/openapi.yaml` already exists:
- **Preserve** existing endpoints and schemas
- **Add** newly discovered endpoints and DTOs
- **Update** modified endpoints (changed parameters, responses, or schemas)
- **Do not remove** endpoints that still exist in the source code

---

## 7. Framework-Specific Patterns

Adapt scanning patterns based on the project's {{FRAMEWORK}} framework and {{LANGUAGE}} language:

### REST Adapter Discovery Patterns

Scan for framework-specific annotations, decorators, or route registration patterns
to discover REST endpoints. Common patterns include:
- Annotation-based routing (Spring `@GetMapping`, Quarkus `@GET`, NestJS `@Get()`)
- Function-based routing (Express `app.get()`, Gin `r.GET()`, Axum `Router::new().route()`)
- Decorator-based routing (FastAPI `@app.get()`, Flask `@route()`)

### DTO Discovery Patterns

Locate request/response DTOs by scanning:
- Method parameter types in controller/handler methods
- Return types or response wrapper generics
- Dedicated DTO/model directories (`dto/`, `model/`, `schema/`)

---

## 8. Quality Checklist

Before finalizing the OpenAPI spec, verify:

- [ ] `openapi: "3.1.0"` is set
- [ ] `info.title` and `info.version` are populated
- [ ] `servers` array has at least one entry
- [ ] All discovered endpoints appear in `paths`
- [ ] All request/response DTOs are in `components/schemas`
- [ ] All schema references use `$ref` (no inline duplication)
- [ ] All error responses use RFC 7807 `ProblemDetail` schema
- [ ] All HTTP methods are documented (GET, POST, PUT, DELETE, PATCH as applicable)
- [ ] Tags group endpoints by controller/resource
- [ ] Output is valid YAML at `contracts/api/openapi.yaml`
