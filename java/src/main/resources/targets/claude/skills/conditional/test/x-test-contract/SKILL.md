---
name: x-test-contract
description: "Runs consumer-driven contract tests (Pact, Spring Cloud Contract) to verify API compatibility between services."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
argument-hint: "[--provider | --consumer | --all]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Contract Tests

## Purpose

Run consumer-driven contract tests to verify API compatibility between services. Support both consumer-side (pact generation) and provider-side (pact verification) workflows using Pact or Spring Cloud Contract.

## Activation Condition

Include this skill when `testing.contract_tests == true` in the project configuration.

## Triggers

- `/x-test-contract --consumer` -- generate pact files from consumer tests
- `/x-test-contract --provider` -- verify provider against published pacts
- `/x-test-contract --all` -- run both consumer generation and provider verification

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `--consumer` | Flag | No | false | Generate pact files from consumer tests |
| `--provider` | Flag | No | false | Verify provider against published pacts |
| `--all` | Flag | No | false | Run both consumer generation and provider verification |

## Prerequisites

- Contract testing framework installed:
  - Java: Pact JVM or Spring Cloud Contract
  - TypeScript: Pact JS
  - Python: Pact Python
  - Go: Pact Go
- Consumer and/or provider test files exist
- Build tool configured for contract test execution

## Workflow

### Step 1 — Verify Framework

Check contract testing framework installed:
- Scan build file for Pact/Spring Cloud Contract dependencies
- Verify framework version compatibility

### Step 2 — Determine Mode

Based on argument:
- `--consumer`: generate pact files
- `--provider`: verify against pacts
- `--all`: both in sequence
- (none): show usage/help

### Step 3 — Run Consumer Tests

1. Discover consumer test files (scan for `@PactTest`, `Pact`, `pact.describe`)
2. Run consumer tests: generate pact files (JSON contracts)
3. Verify pact files generated in `target/pacts/` or `pacts/` directory
4. Validate pact content: interactions defined, request/response match expectations

### Step 4 — Run Provider Verification

1. Discover provider verification configuration
2. Start provider application (or use test server)
3. Run provider verification against published pacts
4. Report: which interactions passed/failed
5. Publish verification results (if pact broker configured)

### Step 5 — Report Results

Generate structured output:

```
## Contract Test Results — [Mode: Consumer/Provider]

### Summary
- Interactions tested: [N]
- Passed: [N]
- Failed: [N]

### Failed Interactions
1. [Consumer -> Provider: interaction description, expected vs actual]

### Checklist Results
[Items that passed / failed / not applicable]

### Verdict: PASS / FAIL
```

## Contract Checklist (10 points)

1. Consumer defines explicit expectations (request headers, body, status code)
2. Provider states defined for each interaction (setup test data)
3. Pact files versioned and published to broker (or committed to repo)
4. Provider verification runs in CI pipeline
5. Breaking changes detected before deployment
6. Contract covers happy path AND error responses
7. Contract includes required headers (Content-Type, Authorization pattern)
8. No sensitive data in pact files (use example data)
9. Consumer version tagged (branch, commit SHA)
10. Can-I-Deploy check integrated in CI (if using Pact Broker)

## Error Handling

| Scenario | Action |
|----------|--------|
| Contract framework not installed | Report missing dependency with install instructions for the detected language |
| Consumer-provider interaction fails | Report failed interactions with expected vs actual details |
| Pact broker unreachable | Warn and continue with local pact files |
| Sensitive data detected in pact files | FAIL with file location and remediation guidance |

## Rules

- FAIL if any consumer-provider interaction verification fails
- FAIL if pact files contain sensitive data
- Warn if pact broker not configured (local-only contracts are fragile)
- Warn if provider states not defined (tests may pass with wrong data)
- Verify both happy path and error scenarios are covered
