---
name: x-security-dashboard
description: "Aggregates results from all security scanning skills into a unified posture view with score 0-100, trend tracking, OWASP risk heatmap, per-dimension breakdown, and remediation priority queue. Never executes scans — reads existing results only (RULE-011)."
user-invocable: true
allowed-tools: Read, Write, Bash, Grep, Glob
argument-hint: "[--period last-7d|last-30d|last-90d|all] [--format markdown|json] [--compare-previous]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Security Posture Dashboard

## Purpose

Generates a consolidated security posture dashboard for {{PROJECT_NAME}} by aggregating results from all security scanning skills. This is the central visibility point for the project's security status.

**Critical constraint (RULE-011 — Skill Composability):** This skill NEVER executes scans. It reads existing scan results from `results/security/` and aggregates them. Each scanning dimension is owned by its atomic skill; the dashboard only consumes their output.

## Triggers

- `/x-security-dashboard` — full dashboard with all available dimensions (period: all)
- `/x-security-dashboard --period last-30d` — dashboard for the last 30 days
- `/x-security-dashboard --format json` — JSON output instead of Markdown
- `/x-security-dashboard --compare-previous` — include trend comparison with previous period
- `/x-security-dashboard --period last-7d --compare-previous` — 7-day window with trend

## Parameters

| Parameter | Type | Default | Validation | Description |
|-----------|------|---------|------------|-------------|
| `--period` | String | all | enum: last-7d, last-30d, last-90d, all | Analysis time window |
| `--format` | String | markdown | enum: markdown, json | Output format |
| `--compare-previous` | boolean | false | -- | Include comparison with the preceding period of equal length |

### Parameter Rules

- `--period` controls which scan result files are included (by timestamp in filename)
- `--compare-previous` requires `--period` to be one of `last-7d`, `last-30d`, `last-90d` (not `all`)
- When `--compare-previous` is used with `--period all`, ignore the flag and omit trend data

## Aggregated Dimensions

The dashboard aggregates results from 10 security dimensions. Each dimension maps to one or more atomic scanning skills:

| Dimension | Source Skill(s) | Weight | Results Pattern |
|-----------|----------------|--------|-----------------|
| Static Analysis | x-security-sast | 20% | `results/security/sast-*.sarif.json` |
| Dynamic Analysis | x-security-dast | 15% | `results/security/dast-*.sarif.json` |
| Secrets | x-security-secrets | 15% | `results/security/secret-scan-*.sarif.json` |
| Container Security | x-security-container | 10% | `results/security/container-scan-*.sarif.json` |
| Infrastructure | x-security-infra | 10% | `results/security/infra-scan-*.sarif.json` |
| OWASP Compliance | x-owasp-scan | 10% | `results/security/owasp-scan-*.sarif.json` |
| Code Quality (Security) | x-security-sonar | 5% | `results/security/sonar-gate-*.sarif.json` |
| Hardening | x-hardening-eval | 5% | `results/security/hardening-eval-*.sarif.json` |
| Runtime Protection | x-runtime-eval | 5% | `results/security/runtime-protection-*.sarif.json` |
| Supply Chain | x-supply-chain-audit + x-dependency-audit | 5% | `results/security/supply-chain-*.sarif.json`, `results/security/dependency-audit-*.sarif.json` |

**Weight redistribution:** When a dimension has no results, its weight is redistributed proportionally among available dimensions. The formula:

```
adjusted_weight[i] = base_weight[i] / sum(base_weight[available_dimensions])
```

## Workflow

```
1. SCAN DIRECTORY  -> List all files in results/security/
2. FILTER PERIOD   -> Keep only files within the selected time window
3. PARSE RESULTS   -> Read SARIF files and extract findings per dimension
4. SCORE           -> Compute per-dimension and overall scores
5. TREND           -> Compare with previous period (if --compare-previous)
6. HEATMAP         -> Build OWASP category x severity matrix
7. TOP FINDINGS    -> Select top 10 findings by risk
8. QUEUE           -> Build remediation priority queue
9. RENDER          -> Output dashboard in selected format
```

### Step 1 — Scan Results Directory

Read the `results/security/` directory. For each file, extract the scan type and timestamp from the filename:

```
results/security/{scan-type}-{YYYYMMDD}-{HHMMSS}.sarif.json
```

Map each file to its dimension using the Results Pattern column from the dimensions table.

### Step 2 — Filter by Period

Apply the `--period` filter based on the timestamp extracted from filenames:

| Period | Include Files From |
|--------|--------------------|
| last-7d | Now minus 7 days to now |
| last-30d | Now minus 30 days to now |
| last-90d | Now minus 90 days to now |
| all | All available results |

For each dimension, use the **most recent** file within the period.

### Step 3 — Parse SARIF Results

For each dimension's SARIF file, extract:
- `runs[0].results[]` — all findings
- Per finding: `ruleId`, `level`, `message.text`, `locations[].physicalLocation`, `properties.severity`, `properties.owasp-category`
- Count findings by severity: CRITICAL, HIGH, MEDIUM, LOW, INFO

### Step 4 — Compute Scores

Per-dimension score uses the security scoring model:

```
dimension_score = max(0, 100 - sum(severity_weight * count_per_severity))
```

Severity weights (from `references/security-scoring.md`):

| Severity | Weight |
|----------|--------|
| CRITICAL | 10 |
| HIGH | 5 |
| MEDIUM | 2 |
| LOW | 1 |
| INFO | 0 |

Grade thresholds:

| Grade | Score Range |
|-------|------------|
| A | 90-100 |
| B | 80-89 |
| C | 70-79 |
| D | 60-69 |
| F | 0-59 |

Overall score is the weighted average of available dimension scores:

```
overall_score = round(
    sum(dimension_score[i] * adjusted_weight[i])
    for i in available_dimensions
)
```

### Step 5 — Trend Analysis

When `--compare-previous` is active, compute the previous period window:

| Current Period | Previous Period |
|---------------|----------------|
| last-7d | 14 days ago to 7 days ago |
| last-30d | 60 days ago to 30 days ago |
| last-90d | 180 days ago to 90 days ago |

Trend classification per dimension and overall:

| Trend | Condition |
|-------|-----------|
| improving | current_score - previous_score >= 5 |
| stable | abs(current_score - previous_score) < 5 |
| degrading | previous_score - current_score >= 5 |

When no previous period data exists for a dimension, set trend to `null` (omit from output).

### Step 6 — Risk Heatmap

Build a matrix of OWASP Top 10 categories (rows) x severity levels (columns):

**Rows:** A01 (Broken Access Control), A02 (Cryptographic Failures), A03 (Injection), A04 (Insecure Design), A05 (Security Misconfiguration), A06 (Vulnerable Components), A07 (Auth Failures), A08 (Data Integrity Failures), A09 (Logging Failures), A10 (SSRF), CUSTOM (unmapped findings)

**Columns:** CRITICAL, HIGH, MEDIUM, LOW

Each cell contains the count of findings matching that category-severity pair across ALL dimensions.

### Step 7 — Top 10 Findings

Select the 10 highest-risk findings across all dimensions, ordered by:
1. Severity (CRITICAL > HIGH > MEDIUM > LOW > INFO)
2. OWASP category priority (A01-A03 higher than A04-A10)
3. Most recent scan date

For each finding, include: dimension, ruleId, severity, OWASP category, file location, message, and remediation recommendation.

### Step 8 — Remediation Priority Queue

Order ALL findings by remediation priority:

```
priority = severity_weight * (1 + owasp_priority_bonus) / estimated_effort
```

Where:
- `severity_weight`: CRITICAL=10, HIGH=5, MEDIUM=2, LOW=1
- `owasp_priority_bonus`: A01-A03=0.5, A04-A06=0.3, A07-A10=0.1, CUSTOM=0.0
- `estimated_effort`: based on fix complexity (default 1.0 when unknown)

### Step 9 — Render Output

#### 9.1 — Markdown Format

```markdown
# Security Posture Dashboard

**Project**: {{PROJECT_NAME}}
**Period**: {period}
**Generated**: {YYYY-MM-DD HH:MM:SS}
**Dimensions Available**: {count}/{total}

## Overall Score

| Metric | Value |
|--------|-------|
| Score | {overall_score}/100 |
| Grade | {overall_grade} |
| Trend | {overall_trend} {delta} |
| Total Findings | {total_findings} |
| Critical | {critical_count} |
| High | {high_count} |
| Medium | {medium_count} |
| Low | {low_count} |

## Per-Dimension Scores

| Dimension | Source | Score | Grade | Trend | Findings | Critical | High |
|-----------|--------|-------|-------|-------|----------|----------|------|
| Static Analysis | x-security-sast | {score} | {grade} | {trend} | {count} | {c} | {h} |
| Dynamic Analysis | x-security-dast | {score} | {grade} | {trend} | {count} | {c} | {h} |
| ... | ... | ... | ... | ... | ... | ... | ... |

### Missing Dimensions

{list of dimensions with no results, if any}

## Risk Heatmap (OWASP Top 10 x Severity)

| Category | CRITICAL | HIGH | MEDIUM | LOW | Total |
|----------|----------|------|--------|-----|-------|
| A01 Broken Access Control | {n} | {n} | {n} | {n} | {n} |
| A02 Cryptographic Failures | {n} | {n} | {n} | {n} | {n} |
| A03 Injection | {n} | {n} | {n} | {n} | {n} |
| ... | ... | ... | ... | ... | ... |
| **Total** | **{n}** | **{n}** | **{n}** | **{n}** | **{n}** |

## Top 10 Findings

### 1. [{severity}] {ruleId} ({dimension})
- **File**: {file}:{line}
- **OWASP**: {category}
- **Message**: {message}
- **Fix**: {remediation}

...

## Remediation Priority Queue

| # | Severity | Rule | Dimension | File | Priority Score |
|---|----------|------|-----------|------|---------------|
| 1 | CRITICAL | {rule} | {dim} | {file} | {score} |
| 2 | HIGH | {rule} | {dim} | {file} | {score} |
| ... | ... | ... | ... | ... | ... |
```

#### 9.2 — JSON Format

```json
{
  "project": "{{PROJECT_NAME}}",
  "period": "{period}",
  "generatedAt": "{ISO-8601}",
  "overallScore": 74,
  "overallGrade": "C",
  "overallTrend": "improving",
  "totalFindings": 87,
  "dimensionsAvailable": 8,
  "dimensionsMissing": ["x-security-container", "x-security-infra"],
  "dimensions": [
    {
      "dimension": "Static Analysis",
      "source": "x-security-sast",
      "weight": 0.20,
      "score": 85,
      "grade": "B",
      "findingsCount": 12,
      "criticalCount": 1,
      "highCount": 3,
      "trend": "improving",
      "lastScanDate": "2026-04-05",
      "available": true
    }
  ],
  "riskHeatmap": {
    "rows": ["A01", "A02", "A03", "A04", "A05", "A06", "A07", "A08", "A09", "A10", "CUSTOM"],
    "columns": ["CRITICAL", "HIGH", "MEDIUM", "LOW"],
    "cells": {
      "A01": {"CRITICAL": 1, "HIGH": 3, "MEDIUM": 2, "LOW": 0},
      "A03": {"CRITICAL": 0, "HIGH": 2, "MEDIUM": 5, "LOW": 1}
    },
    "totals": {"CRITICAL": 3, "HIGH": 12, "MEDIUM": 18, "LOW": 8}
  },
  "top10Findings": [
    {
      "rank": 1,
      "dimension": "Static Analysis",
      "ruleId": "SAST-001",
      "severity": "CRITICAL",
      "owaspCategory": "A03",
      "file": "src/main/java/Handler.java",
      "line": 42,
      "message": "SQL Injection vulnerability",
      "remediation": "Use parameterized queries"
    }
  ],
  "remediationQueue": [
    {
      "priority": 1,
      "severity": "CRITICAL",
      "ruleId": "SAST-001",
      "dimension": "Static Analysis",
      "file": "src/main/java/Handler.java",
      "priorityScore": 15.0
    }
  ]
}
```

## Error Handling

| Scenario | Action |
|----------|--------|
| `results/security/` empty or missing | Set score to 0, grade to F, list all 10 dimensions as missing |
| Partial results (some dimensions available) | Score from available dimensions, redistribute weights, list missing |
| Malformed SARIF file | Log warning with filename and parse error, exclude dimension, continue |
| No previous period data (with --compare-previous) | Set trend to null, note "No previous period data available" |
| --compare-previous with --period all | Ignore the flag, omit trend data |
| Invalid --period value | Error with valid options list |

## Idempotency

### Output Files

```
results/security/dashboard-{YYYYMMDD}-{HHMMSS}.md
results/security/dashboard-{YYYYMMDD}-{HHMMSS}.json
```

- Each dashboard run creates a new timestamped file
- Previous dashboard outputs are never modified
- Both Markdown and JSON files are generated regardless of `--format` flag
- The `--format` flag controls what is displayed to the user

### Performance

- Dashboard generation MUST complete in under 5 seconds
- SARIF file parsing uses streaming when files exceed 1MB
- Dimension aggregation can be parallelized (no shared state between dimensions)

## Composability (RULE-011 — Skill Composability)

This skill is an **aggregator**, not a scanner. It MUST:
- NEVER invoke x-security-sast, x-security-dast, or any other scanning skill
- NEVER duplicate scanning logic from any atomic skill
- Read ONLY from `results/security/` (the shared output directory)
- Respect the SARIF 2.1.0 schema defined in `references/sarif-template.md`
- Use the scoring model defined in `references/security-scoring.md`

If a user needs fresh scan data, run the individual scanning skills first, then invoke this dashboard.

## Knowledge Pack References

| # | Knowledge Pack | Path | Purpose |
|---|----------------|------|---------|
| 1 | Security Scoring | `skills/security/references/security-scoring.md` | Scoring model and grade thresholds |
| 2 | Security Skill Template | `skills/security/references/security-skill-template.md` | Output conventions for scan results |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| x-security-sast | Consumes output | Static analysis SARIF results (20% weight) |
| x-security-dast | Consumes output | Dynamic analysis SARIF results (15% weight) |
| x-security-secrets | Consumes output | Secrets detection SARIF results (15% weight) |
| x-owasp-scan | Consumes output | OWASP compliance SARIF results (10% weight) |
| x-dependency-audit | Consumes output | Supply chain SARIF results (5% weight) |
| x-supply-chain-audit | Consumes output | Supply chain SARIF results (5% weight) |
