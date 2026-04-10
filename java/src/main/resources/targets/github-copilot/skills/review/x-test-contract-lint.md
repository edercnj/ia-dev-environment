---
name: x-test-contract-lint
description: >
  Validates API contracts (OpenAPI 3.1, AsyncAPI 2.6, Protobuf 3) against their
  specifications. Reports structural errors, missing fields, and spec violations
  before contract approval. Use during Phase 0.5 of the lifecycle or standalone.
---

# Skill: Contract Lint

## When to Use

- Validate API contracts before approval in Phase 0.5
- Check OpenAPI 3.1, AsyncAPI 2.6, or Protobuf 3 specs
- Pre-PR contract validation

## Contract Format Detection

Detect the contract format from the file extension and content:

| File Pattern | Format | Spec Version |
|-------------|--------|-------------|
| `*-openapi.yaml` | OpenAPI | 3.1 |
| `*.proto` | Protobuf | 3 |
| `*-asyncapi.yaml` | AsyncAPI | 2.6 |

## Validation Rules

### OpenAPI 3.1

1. **Structure**: `openapi`, `info`, `paths`, `components` sections present
2. **Info**: `title`, `version` fields populated
3. **Paths**: Each path has at least one operation (GET/POST/PUT/DELETE/PATCH)
4. **Operations**: Each operation has `summary`, `operationId`, `responses`
5. **Responses**: Each response has `description` and `content` schema
6. **Schemas**: All `$ref` references resolve to `components/schemas`
7. **Error Responses**: 4xx/5xx responses use RFC 7807 ProblemDetail schema
8. **Servers**: At least one server entry with `url` and `description`
9. **Tags**: Operations are tagged for grouping

### AsyncAPI 2.6

1. **Structure**: `asyncapi`, `info`, `channels` sections present
2. **Info**: `title`, `version` fields populated
3. **Channels**: Each channel has `subscribe` or `publish` operation
4. **Messages**: Each operation has `message` with `payload` schema
5. **Payload**: Schema defines `type`, `properties`, `required` fields
6. **Servers**: At least one server with `protocol` (kafka, amqp, etc.)
7. **Components**: Reusable schemas defined in `components/schemas`

### Protobuf 3

1. **Syntax**: `syntax = "proto3"` declaration present
2. **Package**: `package` declaration present
3. **Services**: At least one `service` with `rpc` methods
4. **Messages**: Each `rpc` has request and response message types
5. **Fields**: Fields have sequential numbering, no gaps
6. **Options**: `java_package` or language-specific options present

## Output Format

```
CONTRACT LINT REPORT
====================
File: {contract-path}
Format: {format} {version}
Status: VALID | INVALID

Errors ({count}):
  - [E001] {description} at {location}
  - [E002] {description} at {location}

Warnings ({count}):
  - [W001] {description} at {location}

Summary: {count} errors, {count} warnings
```

## Integration

- Invoked by Phase 0.5 (Step 0.5.3) of `/x-dev-story-implement`
- Contract must pass validation before `CONTRACT PENDING APPROVAL`
- Errors block contract approval; warnings are informational
