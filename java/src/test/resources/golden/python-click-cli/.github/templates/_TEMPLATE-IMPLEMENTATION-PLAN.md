# Implementation Plan -- {{STORY_ID}}

## Header

| Field | Value |
|-------|-------|
| Story ID | {{STORY_ID}} |
| Epic ID | {{EPIC_ID}} |
| Plan Level | {{PLAN_LEVEL}} |
| Date | {{DATE}} |
| Author | {{AUTHOR}} |
| Template Version | 1.0.0 |

## Executive Summary

{{EXECUTIVE_SUMMARY}}

## Affected Layers and Components

| Layer | Package | Component | Action |
|-------|---------|-----------|--------|
| {{LAYER}} | {{PACKAGE}} | {{COMPONENT}} | {{ACTION}} |

## New Classes/Interfaces

| Class Name | Package | Type | Purpose |
|------------|---------|------|---------|
| {{CLASS_NAME}} | {{PACKAGE}} | {{TYPE}} | {{PURPOSE}} |

## Existing Classes to Modify

| Class Name | Package | Change Description | Risk |
|------------|---------|-------------------|------|
| {{CLASS_NAME}} | {{PACKAGE}} | {{CHANGE_DESCRIPTION}} | {{RISK}} |

## Class Diagram

```mermaid
classDiagram
    class {{CLASS_NAME}} {
        {{FIELDS}}
        {{METHODS}}
    }
    {{RELATIONSHIPS}}
```

## Method Signatures

### {{CLASS_NAME}}

| Method | Parameters | Return Type | Visibility |
|--------|-----------|-------------|------------|
| {{METHOD_NAME}} | {{PARAMETERS}} | {{RETURN_TYPE}} | {{VISIBILITY}} |

## Dependency Direction Validation

| Source Layer | Target Layer | Direction | Status |
|-------------|-------------|-----------|--------|
| adapter.inbound | application | inward | {{STATUS}} |
| application | domain | inward | {{STATUS}} |
| adapter.outbound | domain.port | inward | {{STATUS}} |

> **Golden Rule:** Dependencies point inward toward the domain. Domain NEVER imports adapter or framework code.

## DB Schema Changes

<!-- CONDITIONAL: database != none -->

### Migration

```sql
-- {{MIGRATION_DESCRIPTION}}
{{MIGRATION_SQL}}
```

### Entity Mapping

| Entity | Table | Columns | Indexes |
|--------|-------|---------|---------|
| {{ENTITY}} | {{TABLE}} | {{COLUMNS}} | {{INDEXES}} |

## API Changes

<!-- CONDITIONAL: interfaces contains rest|grpc|graphql -->

### Endpoints

| Method | Path | Request DTO | Response DTO | Status Codes |
|--------|------|------------|-------------|-------------|
| {{HTTP_METHOD}} | {{PATH}} | {{REQUEST_DTO}} | {{RESPONSE_DTO}} | {{STATUS_CODES}} |

## Event Changes

<!-- CONDITIONAL: event-driven == true -->

| Event Name | Topic | Payload Schema | Producer | Consumer |
|------------|-------|---------------|----------|----------|
| {{EVENT_NAME}} | {{TOPIC}} | {{PAYLOAD_SCHEMA}} | {{PRODUCER}} | {{CONSUMER}} |

## Configuration Changes

| Property | Default Value | Environment Variable | Description |
|----------|--------------|---------------------|-------------|
| {{PROPERTY}} | {{DEFAULT_VALUE}} | {{ENV_VAR}} | {{DESCRIPTION}} |

## TDD Strategy

### Test Mapping

| Class/Component | Unit Tests | Acceptance Tests | Integration Tests |
|----------------|-----------|-----------------|-------------------|
| {{COMPONENT}} | {{UT_REFS}} | {{AT_REFS}} | {{IT_REFS}} |

### TDD Execution Order

| Order | Test ID | Type | TPP Level | Description |
|-------|---------|------|-----------|-------------|
| 1 | UT-1 | Unit | nil | {{DESCRIPTION}} |
| 2 | UT-2 | Unit | constant | {{DESCRIPTION}} |
| 3 | AT-1 | Acceptance | scalar | {{DESCRIPTION}} |

## Architecture Decisions

### ADR-{{N}}: {{DECISION_TITLE}}

- **Context:** {{CONTEXT}}
- **Decision:** {{DECISION}}
- **Rationale:** {{RATIONALE}}
- **Consequences:** {{CONSEQUENCES}}

## Integration Points

| System | Protocol | Direction | SLO | Error Handling |
|--------|----------|-----------|-----|----------------|
| {{SYSTEM}} | {{PROTOCOL}} | {{DIRECTION}} | {{SLO}} | {{ERROR_HANDLING}} |

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| {{RISK}} | {{PROBABILITY}} | {{IMPACT}} | {{MITIGATION}} |

## Language-Specific Considerations

### {{LANGUAGE}} {{FRAMEWORK}} Patterns

- {{CONSIDERATION_1}}
- {{CONSIDERATION_2}}

### Approved Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| {{LIBRARY}} | {{VERSION}} | {{PURPOSE}} |

### {{LANGUAGE}}-Specific Idioms

{{LANGUAGE_IDIOMS}}
