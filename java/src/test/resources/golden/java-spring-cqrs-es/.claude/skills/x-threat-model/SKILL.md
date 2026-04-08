---
name: x-threat-model
description: "Generate threat models using STRIDE analysis: identify components, map data flows, analyze threats per category, classify severity, suggest mitigations, and produce threat model document."
user-invocable: true
allowed-tools: Read, Write, Glob, Grep, Agent
argument-hint: "[architecture-plan-path] [--format stride|pasta|linddun] [--output results/security/]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Threat Model (STRIDE Analysis)

## Purpose

Generates automated threat models for {{PROJECT_NAME}} using STRIDE analysis. Identifies components, maps data flows, analyzes threats per STRIDE category, classifies severity, suggests mitigations, and produces a structured threat model document.

## Triggers

- `/x-threat-model` — analyze codebase and generate STRIDE threat model
- `/x-threat-model steering/plan.md` — generate from architecture plan
- `/x-threat-model --format stride` — STRIDE analysis (default)
- `/x-threat-model --format pasta` — PASTA analysis (risk-centric)
- `/x-threat-model --format linddun` — LINDDUN analysis (privacy-focused)
- `/x-threat-model --output results/security/` — specify output directory

## Parameters

| Parameter | Type | Default | Values | Description |
|-----------|------|---------|--------|-------------|
| `path` | String | none | file path | Architecture plan path (optional) |
| `--format` | String | stride | stride, pasta, linddun | Analysis methodology |
| `--output` | String | results/security/ | directory path | Output directory for threat model |

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

### Step 1 — Read Architecture Plan

If a path argument is provided, read the architecture plan directly:

```bash
# Read explicit path
cat steering/plan.md
```

If no path is provided, discover automatically:

1. Search `steering/` for architecture plans
2. Search for ADRs in `adr/`
3. If no plan exists, **fallback to codebase analysis**: scan package structure, configuration files, and dependency declarations to identify components

**Fallback — Codebase Analysis:**

When no architecture plan is available, analyze the codebase directly:

- Scan package/module structure to identify services and layers
- Read configuration files (application.yaml, docker-compose.yml, k8s manifests)
- Identify external integrations from dependency declarations
- Map communication protocols from adapter implementations

### Step 2 — Identify Components

Extract all system components:

| Component Type | Discovery Method |
|----------------|------------------|
| Services | Package structure, deployment configs |
| Databases | Connection configs, ORM entities |
| External APIs | HTTP client configurations, API specs |
| Message Brokers | Producer/consumer configs |
| Caches | Cache configuration, Redis/Memcached clients |
| API Gateway | Ingress/gateway configurations |
| Auth Service | Security configurations, OAuth/OIDC setup |
| File Storage | S3/blob storage client configurations |

### Step 3 — Map Data Flows and Trust Boundaries

For each component pair, identify:

- **Protocol**: HTTP/HTTPS, gRPC, TCP, AMQP, WebSocket
- **Data sensitivity**: PII, financial, credentials, public
- **Trust boundary**: Internal (same network), External (public internet), DMZ
- **Authentication**: mTLS, JWT, API key, none

Trust Boundary Categories:

| Boundary | Description | Risk Level |
|----------|-------------|------------|
| External to Internal | Public internet to internal services | HIGH |
| Internal to Internal | Service-to-service within trust zone | MEDIUM |
| Internal to External | Outbound to third-party APIs | MEDIUM |
| User to System | End-user interaction points | HIGH |

### Step 4 — Analyze with STRIDE

Apply STRIDE analysis to each component. For every component, evaluate all 6 categories:

#### 4.1 — S (Spoofing — Identity and Authentication)

| Threat | Example | Affected Components |
|--------|---------|---------------------|
| Token forgery | Forged JWT, stolen session | API Gateway, Auth Service |
| Session hijacking | Cookie theft, session fixation | Web endpoints |
| Identity impersonation | Spoofed service identity | Service-to-service calls |
| Credential stuffing | Brute force with leaked creds | Login endpoints |

**Mitigations:** Strong authentication (OAuth 2.0/OIDC), mTLS for service-to-service, token rotation, rate limiting on auth endpoints.

#### 4.2 — T (Tampering — Data Integrity)

| Threat | Example | Affected Components |
|--------|---------|---------------------|
| SQL injection | Malicious SQL in input | Database adapters |
| Request tampering | Modified request payload | API endpoints |
| Man-in-the-middle | Intercepted unencrypted traffic | All network calls |
| Data corruption | Unauthorized data modification | Database, file storage |

**Mitigations:** Input validation, parameterized queries, TLS everywhere, request signing, integrity checksums.

#### 4.3 — R (Repudiation — Audit and Traceability)

| Threat | Example | Affected Components |
|--------|---------|---------------------|
| Insufficient logging | Actions not recorded | All services |
| Log tampering | Modified audit logs | Logging infrastructure |
| Non-attributable actions | Actions without user identity | Background jobs, async |
| Missing timestamps | Events without temporal context | Event producers |

**Mitigations:** Structured logging with correlation IDs, immutable audit logs, distributed tracing, event sourcing for critical operations.

#### 4.4 — I (Information Disclosure — Confidentiality)

| Threat | Example | Affected Components |
|--------|---------|---------------------|
| Data leakage | PII in logs, verbose errors | All services |
| Excessive exposure | Over-fetching in API responses | API endpoints |
| Cache poisoning | Sensitive data in shared cache | Cache layer |
| Side-channel attacks | Timing attacks, error messages | Auth, crypto operations |

**Mitigations:** Data classification, field-level encryption, response filtering, error sanitization per security KP.

#### 4.5 — D (Denial of Service — Availability)

| Threat | Example | Affected Components |
|--------|---------|---------------------|
| Resource exhaustion | Unbounded queries, large payloads | Database, API endpoints |
| DDoS | Volumetric attacks on public endpoints | API Gateway |
| Cascading failure | Uncontrolled retry storms | Service mesh |
| Deadlock/livelock | Resource contention | Database, message broker |

**Mitigations:** Rate limiting, circuit breakers, bulkheads, request size limits, connection pooling, auto-scaling.

#### 4.6 — E (Elevation of Privilege — Authorization)

| Threat | Example | Affected Components |
|--------|---------|---------------------|
| Broken access control | IDOR, missing authz checks | API endpoints |
| Privilege escalation | Regular user gaining admin | Auth Service, RBAC |
| Insecure defaults | Overly permissive roles | Configuration |
| JWT claim manipulation | Modified role claims | Token-based auth |

**Mitigations:** RBAC/ABAC enforcement, least privilege principle, authorization at every layer, claim validation.

### Step 5 — Classify Threat Severity

Classify each identified threat using impact x probability:

| Severity | Impact | Probability | Action Required |
|----------|--------|-------------|-----------------|
| **CRITICAL** | High impact + high probability | Exploit known/easy | Fix before release |
| **HIGH** | High impact or high probability | Exploit possible | Fix in current sprint |
| **MEDIUM** | Moderate impact | Exploit requires effort | Fix in next sprint |
| **LOW** | Low impact | Exploit unlikely | Track in backlog |

**Impact Assessment Criteria:**

| Factor | HIGH | MEDIUM | LOW |
|--------|------|--------|-----|
| Data exposure | PII, credentials, financial | Internal metadata | Public data |
| Service impact | Complete outage | Degraded performance | Minor feature |
| Blast radius | Multiple services affected | Single service | Single endpoint |
| Regulatory | GDPR/SOX/PCI violation | Audit finding | Best practice gap |

### Step 6 — Mitigate with Security KP References

For each identified threat, suggest concrete mitigations referencing the security knowledge pack:

| STRIDE Category | Security KP Section | Key Mitigations |
|-----------------|---------------------|-----------------|
| Spoofing | Authentication and Identity | OAuth 2.0, mTLS, token rotation |
| Tampering | Input Validation | Parameterized queries, request signing |
| Repudiation | Logging and Audit | Structured logging, distributed tracing |
| Information Disclosure | Data Protection | Encryption at rest/transit, data masking |
| Denial of Service | Resilience Patterns | Rate limiting, circuit breakers |
| Elevation of Privilege | Authorization | RBAC, least privilege, claim validation |

### Step 7 — Generate Threat Model Document

Generate the threat model document at the specified output path (default: `results/security/threat-model.md`).

**Document Structure:**

```markdown
# Threat Model — {{PROJECT_NAME}}

**Date:** YYYY-MM-DD
**Methodology:** STRIDE
**Scope:** [components analyzed]

## Executive Summary

[High-level risk overview with counts per severity]

## System Overview

[Components, data flows, trust boundaries diagram]

## Threat Matrix

| Component | S | T | R | I | D | E |
|-----------|---|---|---|---|---|---|
| API Gateway | HIGH | MEDIUM | LOW | MEDIUM | CRITICAL | HIGH |
| Auth Service | CRITICAL | HIGH | MEDIUM | HIGH | MEDIUM | CRITICAL |
| Database | LOW | HIGH | MEDIUM | HIGH | MEDIUM | LOW |
| Message Broker | LOW | MEDIUM | MEDIUM | LOW | HIGH | LOW |

## Detailed Findings

### [TM-001] {Component} — {Threat Title}
- **STRIDE Category:** Spoofing
- **Severity:** CRITICAL
- **Description:** {detailed description}
- **Attack Vector:** {how the threat could be exploited}
- **Mitigation:** {concrete mitigation steps}
- **Security KP Reference:** {section reference}

## Risk Summary

| Severity | Count | Status |
|----------|-------|--------|
| CRITICAL | N | Fix before release |
| HIGH | N | Fix in current sprint |
| MEDIUM | N | Fix in next sprint |
| LOW | N | Track in backlog |

## Recommendations

1. **Immediate:** Address CRITICAL findings
2. **Short-term:** Resolve HIGH findings
3. **Long-term:** Review MEDIUM/LOW findings
```

## Supported Formats

### STRIDE (Default)

Standard threat modeling analyzing 6 categories: Spoofing, Tampering, Repudiation, Information Disclosure, Denial of Service, Elevation of Privilege.

### PASTA (Process for Attack Simulation and Threat Analysis)

Risk-centric methodology with 7 stages:

1. Define objectives
2. Define technical scope
3. Application decomposition
4. Threat analysis
5. Vulnerability analysis
6. Attack modeling
7. Risk and impact analysis

### LINDDUN (Privacy-Focused)

Privacy threat modeling covering:

- **L**inkability — Can actions be linked to an individual?
- **I**dentifiability — Can a person be identified?
- **N**on-repudiation — Can a person deny an action?
- **D**etectability — Can the existence of data be detected?
- **D**isclosure of information — Is data exposed?
- **U**nawareness — Is the user unaware of data processing?
- **N**on-compliance — Are privacy regulations violated?

## Error Handling

| Scenario | Action |
|----------|--------|
| No architecture plan found | Fallback to codebase analysis |
| Empty or invalid plan | Warn and attempt codebase analysis |
| No components identified | Report "No components found" with suggestions |
| Partial analysis | Generate partial threat model, note gaps |
| Unknown format requested | Default to STRIDE, warn user |

## Knowledge Pack References

| # | Knowledge Pack | Path | Purpose |
|---|----------------|------|---------|
| 1 | Security | `skills/security/SKILL.md` | Mitigation recommendations and OWASP references |
| 2 | Security References | `skills/security/references/application-security.md` | Detailed security controls and patterns |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| x-dev-architecture-plan | Invoked from | Threat model can be generated as part of architecture planning |
| security-engineer agent | Delegates to | Uses security-engineer agent for in-depth analysis via Agent tool |
| x-owasp-scan | Complements | Threat model informs A04 (Insecure Design) verification in OWASP scan |
