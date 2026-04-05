---
name: x-threat-model
description: >
  Generate threat models using STRIDE analysis: identify components, map data
  flows, analyze threats per category, classify severity, suggest mitigations,
  and produce threat model document.
  Reference: `.github/skills/x-threat-model/SKILL.md`
---

# Skill: Threat Model (STRIDE Analysis)

## Purpose

Generates automated threat models for {{PROJECT_NAME}} using STRIDE analysis. Identifies components, maps data flows, analyzes threats per STRIDE category, classifies severity, suggests mitigations, and produces a structured threat model document.

## Triggers

- `/x-threat-model` -- analyze codebase and generate STRIDE threat model
- `/x-threat-model steering/plan.md` -- generate from architecture plan
- `/x-threat-model --format stride` -- STRIDE analysis (default)
- `/x-threat-model --format pasta` -- PASTA analysis (risk-centric)
- `/x-threat-model --format linddun` -- LINDDUN analysis (privacy-focused)

## Workflow

```
1. READ       -> Read architecture plan or discover components from codebase
2. IDENTIFY   -> Extract system components (services, databases, APIs, brokers, caches)
3. MAP        -> Map data flows, trust boundaries, and communication protocols
4. ANALYZE    -> Apply STRIDE analysis per component (6 categories)
5. CLASSIFY   -> Classify each threat by severity (CRITICAL/HIGH/MEDIUM/LOW)
6. MITIGATE   -> Suggest mitigations referencing security KP
7. GENERATE   -> Produce threat model document with Threat Matrix
```

### Step 1 -- Read Architecture Plan

If a path argument is provided, read the architecture plan directly. If no path is provided:

1. Search `steering/` for architecture plans
2. Search for ADRs in `adr/`
3. **Fallback to codebase analysis**: scan package structure, configuration files, and dependency declarations

### Step 2 -- IDENTIFY Components

| Component Type | Discovery Method |
|---------------|-----------------|
| Services | Package structure, deployment configs |
| Databases | Connection configs, ORM entities |
| External APIs | HTTP client configurations |
| Message Brokers | Producer/consumer configs |
| Caches | Cache configuration |
| API Gateway | Ingress/gateway configurations |

### Step 3 -- MAP Data Flows

Map trust boundaries and data sensitivity:

| Boundary | Risk Level |
|----------|------------|
| External to Internal | HIGH |
| Internal to Internal | MEDIUM |
| Internal to External | MEDIUM |
| User to System | HIGH |

### Step 4 -- ANALYZE with STRIDE

Apply all 6 STRIDE categories per component:

| Category | Focus | Example Threat |
|----------|-------|---------------|
| **S**poofing | Identity and authentication | Token forgery, session hijacking |
| **T**ampering | Data integrity | SQL injection, request tampering |
| **R**epudiation | Audit and traceability | Insufficient logging |
| **I**nformation Disclosure | Confidentiality | Data leakage, verbose errors |
| **D**enial of Service | Availability | Resource exhaustion, DDoS |
| **E**levation of Privilege | Authorization | Broken access control |

### Step 5 -- CLASSIFY Severity

| Severity | Criteria | Action |
|----------|----------|--------|
| **CRITICAL** | High impact + high probability | Fix before release |
| **HIGH** | High impact or high probability | Fix in current sprint |
| **MEDIUM** | Moderate impact | Fix in next sprint |
| **LOW** | Low impact, exploit unlikely | Track in backlog |

### Step 6 -- MITIGATE

Reference security KP for concrete mitigations per STRIDE category.

### Step 7 -- GENERATE Document

Output Threat Matrix and detailed findings to `docs/security/threat-model.md`:

```markdown
## Threat Matrix

| Component | S | T | R | I | D | E |
|-----------|---|---|---|---|---|---|
| API Gateway | HIGH | MEDIUM | LOW | MEDIUM | CRITICAL | HIGH |
| Auth Service | CRITICAL | HIGH | MEDIUM | HIGH | MEDIUM | CRITICAL |
```

## Supported Formats

- **STRIDE** (default): 6-category threat analysis
- **PASTA**: Risk-centric, 7-stage process
- **LINDDUN**: Privacy-focused threat modeling

## Error Handling

| Scenario | Action |
|----------|--------|
| No architecture plan | Fallback to codebase analysis |
| No components found | Report with suggestions |
| Unknown format | Default to STRIDE |
