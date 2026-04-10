---
name: x-security-dashboard
description: >
  Aggregates results from all security scanning skills into a unified posture
  view with score 0-100, trend tracking, OWASP risk heatmap, per-dimension
  breakdown, and remediation priority queue. Never executes scans (RULE-011).
  Reference: `.github/skills/x-security-dashboard/SKILL.md`
---

# Skill: Security Posture Dashboard

## Purpose

Generates a consolidated security posture dashboard for {{PROJECT_NAME}} by aggregating results from all security scanning skills. This is the central visibility point for the project's security status.

**Critical constraint (RULE-011):** This skill NEVER executes scans. It reads existing results from `results/security/` only.

## Triggers

- `/x-security-dashboard` -- full dashboard with all available dimensions
- `/x-security-dashboard --period last-30d` -- dashboard for the last 30 days
- `/x-security-dashboard --format json` -- JSON output
- `/x-security-dashboard --compare-previous` -- include trend comparison

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `--period` | String | all | Time window: last-7d, last-30d, last-90d, all |
| `--format` | String | markdown | Output format: markdown, json |
| `--compare-previous` | boolean | false | Compare with previous period |

## Aggregated Dimensions

| Dimension | Source Skill(s) | Weight |
|-----------|----------------|--------|
| Static Analysis | x-security-sast | 20% |
| Dynamic Analysis | x-security-dast | 15% |
| Secrets | x-security-secret-scan | 15% |
| Container Security | x-security-container | 10% |
| Infrastructure | x-security-infra | 10% |
| OWASP Compliance | x-owasp-scan | 10% |
| Code Quality (Security) | x-security-sonar | 5% |
| Hardening | x-hardening-eval | 5% |
| Runtime Protection | x-runtime-protection | 5% |
| Supply Chain | x-supply-chain-audit + x-dependency-audit | 5% |

Missing dimensions have their weight redistributed proportionally.

## Workflow

```
1. SCAN DIRECTORY  -> List files in results/security/
2. FILTER PERIOD   -> Keep files within selected time window
3. PARSE RESULTS   -> Read SARIF files, extract findings per dimension
4. SCORE           -> Compute per-dimension and overall scores
5. TREND           -> Compare with previous period (if --compare-previous)
6. HEATMAP         -> Build OWASP category x severity matrix
7. TOP FINDINGS    -> Select top 10 findings by risk
8. QUEUE           -> Build remediation priority queue
9. RENDER          -> Output in selected format
```

## Scoring

Per-dimension: `score = max(0, 100 - sum(severity_weight * count))` with weights CRITICAL=10, HIGH=5, MEDIUM=2, LOW=1, INFO=0.

Overall: weighted average of available dimension scores.

Grades: A (90-100), B (80-89), C (70-79), D (60-69), F (0-59).

## Trend Analysis

| Trend | Condition |
|-------|-----------|
| improving | current - previous >= 5 |
| stable | abs(current - previous) < 5 |
| degrading | previous - current >= 5 |

## Risk Heatmap

Matrix of OWASP Top 10 categories (A01-A10 + CUSTOM) x severity (CRITICAL, HIGH, MEDIUM, LOW) with finding counts per cell.

## Graceful Handling

- **No results**: score=0, grade=F, all dimensions listed as missing
- **Partial results**: score from available dimensions only, weights redistributed
- **Malformed SARIF**: dimension excluded, listed in missing with reason
- **No previous data**: trend omitted with explanatory note

## Composability (RULE-011)

This skill is an aggregator. It NEVER invokes scanning skills and NEVER duplicates scanning logic. It reads only from `results/security/`.
