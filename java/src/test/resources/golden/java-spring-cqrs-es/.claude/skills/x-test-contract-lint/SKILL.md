---
name: x-test-contract-lint
description: "Validates API contracts (OpenAPI 3.1, AsyncAPI 2.6, Protobuf 3) against their specifications. Reports structural errors, missing fields, and spec violations."
user-invocable: true
allowed-tools: Read, Grep, Glob, Bash
argument-hint: "[contract-file-path]"
context-budget: light
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Contract Lint

## Purpose

Validate API contracts before approval. Check OpenAPI 3.1, AsyncAPI 2.6, or Protobuf 3 specs for structural errors, missing fields, and specification violations.

## Activation Condition

Include this skill when the project uses API contracts (OpenAPI, AsyncAPI, or Protobuf).

## Triggers

- `/x-test-contract-lint path/to/contract-openapi.yaml` -- validate an OpenAPI contract
- `/x-test-contract-lint path/to/service.proto` -- validate a Protobuf contract
- `/x-test-contract-lint path/to/events-asyncapi.yaml` -- validate an AsyncAPI contract

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `contract-file-path` | String | Yes | (none) | Path to the contract file to validate |

## Workflow

### Step 1 — Detect Contract Format

Detect the contract format from the file extension and content:

| File Pattern | Format | Spec Version |
|-------------|--------|-------------|
| `*-openapi.yaml` | OpenAPI | 3.1 |
| `*.proto` | Protobuf | 3 |
| `*-asyncapi.yaml` | AsyncAPI | 2.6 |

### Step 2 — Validate Contract

#### OpenAPI 3.1

1. **Structure**: `openapi`, `info`, `paths`, `components` sections present
2. **Info**: `title`, `version` fields populated
3. **Paths**: Each path has at least one operation (GET/POST/PUT/DELETE/PATCH)
4. **Operations**: Each operation has `summary`, `operationId`, `responses`
5. **Responses**: Each response has `description` and `content` schema
6. **Schemas**: All `$ref` references resolve to `components/schemas`
7. **Error Responses**: 4xx/5xx responses use RFC 7807 ProblemDetail schema
8. **Servers**: At least one server entry with `url` and `description`
9. **Tags**: Operations are tagged for grouping

#### AsyncAPI 2.6

1. **Structure**: `asyncapi`, `info`, `channels` sections present
2. **Info**: `title`, `version` fields populated
3. **Channels**: Each channel has `subscribe` or `publish` operation
4. **Messages**: Each operation has `message` with `payload` schema
5. **Payload**: Schema defines `type`, `properties`, `required` fields
6. **Servers**: At least one server with `protocol` (kafka, amqp, etc.)
7. **Components**: Reusable schemas defined in `components/schemas`

#### Protobuf 3

1. **Syntax**: `syntax = "proto3"` declaration present
2. **Package**: `package` declaration present
3. **Services**: At least one `service` with `rpc` methods
4. **Messages**: Each `rpc` has request and response message types
5. **Fields**: Fields have sequential numbering, no gaps
6. **Options**: `java_package` or language-specific options present

### Step 3 — Generate Report

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

## Error Handling

| Scenario | Action |
|----------|--------|
| Contract file not found | Report error with provided path |
| Unrecognized file format | Report unsupported format with list of supported formats |
| Validation errors found | Report INVALID status with all errors listed; errors block contract approval |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| x-dev-story-implement | called-by | Invoked by Phase 0.5 (Step 0.5.3) for contract validation before approval |
