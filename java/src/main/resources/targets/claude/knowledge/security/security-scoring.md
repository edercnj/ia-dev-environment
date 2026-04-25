# Security Scoring Model

## Overview

The security scoring model provides a quantitative measure of security posture based on scan findings. It produces a score from 0 to 100 with letter grades A through F, enabling teams to track security trends over time and set quality gates in CI/CD pipelines.

## Formula

```
score = max(0, 100 - sum(severity_weight * count_per_severity))
```

The score starts at 100 (perfect) and decreases based on the weighted sum of findings by severity. The floor is 0 -- the score never goes negative.

## Severity Weights

| Severity | Weight | SARIF Level | CVSS Range |
|----------|--------|-------------|------------|
| CRITICAL | 10 | `error` | 9.0 - 10.0 |
| HIGH | 5 | `error` | 7.0 - 8.9 |
| MEDIUM | 2 | `warning` | 4.0 - 6.9 |
| LOW | 1 | `note` | 0.1 - 3.9 |
| INFO | 0 | `none` | 0.0 |

## Grade Scale

| Grade | Score Range | Description |
|-------|------------|-------------|
| A | 90 - 100 | Excellent security posture |
| B | 80 - 89 | Good security posture with minor findings |
| C | 70 - 79 | Acceptable but improvement needed |
| D | 60 - 69 | Below standard, remediation required |
| F | 0 - 59 | Failing, immediate action required |

## Calculation Examples

### Zero Findings (Grade A)

```
Findings: none
Score = max(0, 100 - 0) = 100
Grade = A
```

### Mixed Findings (Grade C)

```
Findings: 1 CRITICAL, 2 HIGH, 3 MEDIUM
Score = max(0, 100 - (1*10 + 2*5 + 3*2))
     = max(0, 100 - (10 + 10 + 6))
     = max(0, 100 - 26)
     = 74
Grade = C
```

### Heavy Findings (Grade F)

```
Findings: 20 CRITICAL
Score = max(0, 100 - (20*10))
     = max(0, 100 - 200)
     = max(0, -100)
     = 0
Grade = F
```

### Edge Case: Score at Grade Boundary

```
Findings: 2 HIGH
Score = max(0, 100 - (2*5))
     = max(0, 100 - 10)
     = 90
Grade = A
```

### INFO Findings Only (Grade A)

```
Findings: 50 INFO
Score = max(0, 100 - (50*0))
     = max(0, 100 - 0)
     = 100
Grade = A
```

## Score Data Contract

The scoring output MUST include the following fields:

| Field | Type | Required | Constraints | Example |
|-------|------|----------|-------------|---------|
| `score` | int | Yes | 0-100 | `74` |
| `grade` | String | Yes | A, B, C, D, F | `"C"` |
| `totalFindings` | int | Yes | >= 0 | `6` |
| `criticalCount` | int | Yes | >= 0 | `1` |
| `highCount` | int | Yes | >= 0 | `2` |
| `mediumCount` | int | Yes | >= 0 | `3` |
| `lowCount` | int | Yes | >= 0 | `0` |
| `infoCount` | int | Yes | >= 0 | `0` |

## Output Convention

### Directory Structure

All security scan outputs MUST be written to the `results/security/` directory:

```
results/
  security/
    {scan-type}-{timestamp}.sarif.json
    {scan-type}-{timestamp}-report.md
```

### Naming Pattern

| Component | Format | Example |
|-----------|--------|---------|
| `scan-type` | Lowercase scan identifier | `sast`, `sca`, `secrets`, `container` |
| `timestamp` | ISO-8601 compact (UTC) | `20240115T143022Z` |
| SARIF file | `{scan-type}-{timestamp}.sarif.json` | `sast-20240115T143022Z.sarif.json` |
| Report file | `{scan-type}-{timestamp}-report.md` | `sast-20240115T143022Z-report.md` |

### Markdown Report Structure

The companion markdown report MUST include:

1. **Summary Table** -- total findings by severity, score, grade
2. **Findings Detail** -- each finding with location, description, remediation
3. **Score Breakdown** -- weighted calculation showing how score was derived
4. **Trend** (optional) -- comparison with previous scan if history is available

#### Summary Table Example

```markdown
## Security Scan Summary

| Metric | Value |
|--------|-------|
| Score | 74 / 100 |
| Grade | C |
| Total Findings | 6 |
| Critical | 1 |
| High | 2 |
| Medium | 3 |
| Low | 0 |
| Info | 0 |
```

## CI/CD Integration

### Quality Gate Thresholds

Recommended CI/CD quality gates based on grade:

| Gate | Condition | Action |
|------|-----------|--------|
| Block merge | Grade F (score < 60) | PR cannot be merged |
| Warn | Grade D (score 60-69) | Warning annotation on PR |
| Pass | Grade C or above (score >= 70) | PR proceeds normally |
| Enforce zero critical | criticalCount > 0 | Block regardless of score |

### GitHub Actions Integration

SARIF files can be uploaded to GitHub Advanced Security:

```yaml
- name: Upload SARIF
  uses: github/codeql-action/upload-sarif@v3
  with:
    sarif_file: results/security/
```
