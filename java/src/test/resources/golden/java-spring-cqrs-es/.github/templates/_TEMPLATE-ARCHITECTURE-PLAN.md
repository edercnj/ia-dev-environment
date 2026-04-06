# Architecture Plan -- {{STORY_ID}}

## Header

| Field | Value |
|-------|-------|
| Story ID | {{STORY_ID}} |
| Epic ID | {{EPIC_ID}} |
| Date | {{DATE}} |
| Author | {{AUTHOR}} |
| Template Version | 1.0.0 |

## Executive Summary

{{EXECUTIVE_SUMMARY}}

## Component Diagram

```mermaid
graph TD
    {{COMPONENT_DIAGRAM}}
```

## Sequence Diagrams

### {{FLOW_NAME}}

```mermaid
sequenceDiagram
    {{SEQUENCE_DIAGRAM}}
```

## Deployment Diagram

<!-- CONDITIONAL: orchestrator != none -->

```mermaid
graph TD
    {{DEPLOYMENT_DIAGRAM}}
```

## External Connections

| System | Protocol | Direction | SLO | Notes |
|--------|----------|-----------|-----|-------|
| {{SYSTEM}} | {{PROTOCOL}} | {{DIRECTION}} | {{SLO}} | {{NOTES}} |

## Architecture Decisions

### ADR-{{N}}: {{DECISION_TITLE}}

- **Context:** {{CONTEXT}}
- **Decision:** {{DECISION}}
- **Rationale:** {{RATIONALE}}
- **Consequences:** {{CONSEQUENCES}}
- **Story Reference:** {{STORY_ID}}

## Technology Stack

| Component | Technology | Version | Justification |
|-----------|-----------|---------|---------------|
| Language | {{LANGUAGE}} | {{LANGUAGE_VERSION}} | {{JUSTIFICATION}} |
| Framework | {{FRAMEWORK}} | {{FRAMEWORK_VERSION}} | {{JUSTIFICATION}} |
| Database | {{DATABASE}} | {{DATABASE_VERSION}} | {{JUSTIFICATION}} |

## Non-Functional Requirements

| NFR | Target | Measurement | Priority |
|-----|--------|-------------|----------|
| {{NFR}} | {{TARGET}} | {{MEASUREMENT}} | {{PRIORITY}} |

## Data Model

<!-- CONDITIONAL: database != none -->

```mermaid
erDiagram
    {{ER_DIAGRAM}}
```

### Entity Descriptions

| Entity | Description | Key Fields |
|--------|------------|------------|
| {{ENTITY}} | {{DESCRIPTION}} | {{KEY_FIELDS}} |

## Observability Strategy

### Traces

| Span Name | Attributes | Parent Span |
|-----------|-----------|-------------|
| {{SPAN_NAME}} | {{ATTRIBUTES}} | {{PARENT_SPAN}} |

### Metrics

| Metric Name | Type | Labels | Description |
|-------------|------|--------|-------------|
| {{METRIC_NAME}} | {{TYPE}} | {{LABELS}} | {{DESCRIPTION}} |

### Logs

| Log Event | Level | Fields | When |
|-----------|-------|--------|------|
| {{LOG_EVENT}} | {{LEVEL}} | {{FIELDS}} | {{WHEN}} |

### Health Checks

| Check | Type | Endpoint | Dependencies |
|-------|------|----------|-------------|
| Liveness | probe | /health/live | none |
| Readiness | probe | /health/ready | {{DEPENDENCIES}} |

## Resilience Strategy

| Pattern | Component | Configuration | Fallback |
|---------|-----------|--------------|----------|
| Circuit Breaker | {{COMPONENT}} | {{CONFIGURATION}} | {{FALLBACK}} |
| Retry | {{COMPONENT}} | {{CONFIGURATION}} | {{FALLBACK}} |
| Timeout | {{COMPONENT}} | {{CONFIGURATION}} | {{FALLBACK}} |

## Impact Analysis

| Existing Component | Impact | Risk | Migration Strategy |
|-------------------|--------|------|-------------------|
| {{COMPONENT}} | {{IMPACT}} | {{RISK}} | {{MIGRATION_STRATEGY}} |
