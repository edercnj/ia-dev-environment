---
name: x-review-gateway
description: "Reviews API gateway configuration for routing rules, authentication, rate limiting, CORS, security headers, TLS, and observability integration."
user-invocable: true
allowed-tools: Read, Grep, Glob, Bash
argument-hint: "[gateway config files or PR]"
context-budget: light
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: API Gateway Review

## Purpose

Review API gateway configuration against best practices for routing rules, authentication, rate limiting, CORS, security headers, TLS configuration, and observability integration.

## Activation Condition

Include this skill when the project uses an API gateway (Kong, Istio, AWS APIGW, Traefik, etc.).

## Triggers

- `/x-review-gateway` -- review all gateway configuration files
- `/x-review-gateway gateway.yaml` -- review a specific config file
- `/x-review-gateway 42` -- review gateway changes in PR #42

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `target` | String | No | (all) | Gateway config file paths or PR number |

## Workflow

### Step 1 — Identify Gateway Configuration

Scan for gateway configuration files in the change set or project.

### Step 2 — Verify Routing Rules

Check route definitions for correctness, path conflicts, and upstream targets.

### Step 3 — Validate Authentication

Verify authentication middleware is applied to protected routes.

### Step 4 — Check Rate Limiting

Validate rate limiting configuration per route or globally.

### Step 5 — Validate CORS and Security Headers

Check CORS policy and security headers (HSTS, CSP, X-Frame-Options, etc.).

### Step 6 — Validate TLS Configuration

Verify TLS settings: minimum version, cipher suites, certificate management.

### Step 7 — Check Observability Integration

Validate access logs, tracing propagation, and metrics collection.

### Step 8 — Generate Report

Produce gateway review report with findings and verdict.

## Output Format

```
## Gateway Review — [Change Description]

### Gateway Type: [Kong/Istio/AWS APIGW/Traefik]

### Findings
1. [Finding with file, line, remediation]

### Verdict: APPROVE / REQUEST CHANGES
```

## Error Handling

| Scenario | Action |
|----------|--------|
| No gateway config files found | Report INFO: no gateway configuration discovered |
| Unknown gateway type | Warn and apply generic best practices review |
| Missing authentication on public routes | REQUEST CHANGES with remediation guidance |
