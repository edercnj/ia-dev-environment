---
name: x-review-api
description: "Validates REST API endpoints for RFC 7807 error responses, pagination, URL versioning, OpenAPI documentation, status codes, and DTO patterns."
user-invocable: true
allowed-tools: Read, Grep, Glob, Bash
argument-hint: "[endpoint-path or feature-name]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: REST API Design Review

## Purpose

Review REST API design for compliance with best practices: RFC 7807 error responses, pagination wrappers, URL versioning, OpenAPI annotations, proper HTTP status codes, and DTO separation from domain models.

## Activation Condition

Include this skill when the project uses REST protocol.

## Triggers

- `/x-review-api merchants` -- review API endpoints for a specific feature
- `/x-review-api /api/v1/transactions` -- review a specific endpoint path
- `/x-review-api` -- review all REST endpoints

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `endpoint-or-feature` | String | No | (all) | Endpoint path or feature name to review |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| api-design | `skills/api-design/references/api-design-principles.md` | URL structure, status codes, error format, pagination |
| protocols | `skills/protocols/references/rest-conventions.md` | REST resource naming, HTTP methods, versioning, RFC 7807 |

## Prerequisites

- REST API endpoints exist in the codebase
- {{FRAMEWORK}} is configured with REST/HTTP support
- OpenAPI/Swagger dependency is available

## Workflow

### Step 1 — Discover Endpoints

Search for REST controller/resource classes:
- Scan for route annotations or decorators (e.g., `@Path`, `@RestController`, `@Get`, `@app.route`)
- List all endpoints with their HTTP methods and paths

### Step 2 — Validate URL Structure

Check each endpoint:
- URLs use nouns, not verbs (`/api/v1/resources`, not `/api/v1/createResource`)
- Versioning in path (`/api/v1/`)
- Sub-resources follow hierarchy (`/api/v1/parents/{id}/children`)
- Collection endpoints use plural nouns

### Step 3 — Validate Status Codes

For each endpoint:
- GET list returns 200
- GET item returns 200 (or 404)
- POST returns 201 with Location header
- PUT returns 200 (or 404)
- DELETE returns 204 (or 404)
- Validation errors return 400
- Duplicates return 409

### Step 4 — Validate Error Responses (RFC 7807)

Check for:
- ProblemDetail record/class with fields: type, title, status, detail, instance
- Factory methods for each error type (notFound, conflict, badRequest, validationError, internalError)
- ExceptionMapper/handler that converts domain exceptions to ProblemDetail
- No stack traces exposed in production responses

### Step 5 — Validate Pagination

Check list endpoints:
- Paginated wrapper with data + pagination metadata (page, limit, total, totalPages)
- Query parameters for page, limit, sort
- Default values for pagination parameters

### Step 6 — Validate DTOs

Check request/response separation:
- Request DTOs have input validation annotations
- Response DTOs are immutable (records or final classes)
- No domain entities exposed directly in REST responses
- Sensitive data masked in responses (PAN, documents, etc.)

### Step 7 — Validate OpenAPI Documentation

Check for:
- Schema annotations on DTOs with descriptions and examples
- OpenAPI spec auto-generated and accessible
- Swagger UI available in dev profile

### Step 8 — Generate Report

Summarize findings as checklist:
- List compliant items
- List violations with file paths and line numbers
- Suggest fixes for each violation

## Error Handling

| Scenario | Action |
|----------|--------|
| No REST endpoints found | Report INFO: no endpoints discovered in the codebase |
| OpenAPI dependency missing | Warn about missing documentation support |
| Endpoint missing error handling | Report violation with file path and remediation guidance |

## Review Checklist

- [ ] URLs follow RESTful pattern (nouns, no verbs)
- [ ] Proper versioning in URL path (/api/v1/)
- [ ] Correct HTTP status codes per operation
- [ ] Request DTOs have validation annotations
- [ ] Response DTOs are immutable, no domain entities exposed
- [ ] Error responses follow RFC 7807 (ProblemDetail)
- [ ] Pagination implemented for list endpoints
- [ ] Sensitive data masked in responses
- [ ] OpenAPI/Swagger documentation generated
- [ ] ExceptionMapper covers all domain exceptions
- [ ] No stack traces in production error responses
- [ ] Rate limit responses return 429 + Retry-After header
