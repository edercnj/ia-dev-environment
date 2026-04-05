---
name: x-supply-chain-audit
description: >
  Enhanced supply chain security audit beyond x-dependency-audit. Analyzes
  maintainer risk, typosquatting detection, phantom dependencies, dependency
  age, EPSS scoring, and SLSA assessment. Produces SARIF 2.1.0 output with
  weighted risk scoring.
  Reference: `.github/skills/x-supply-chain-audit/SKILL.md`
---

# Skill: Enhanced Supply Chain Audit

## Purpose

Performs advanced supply chain security analysis for {{PROJECT_NAME}} that complements (does NOT replace) the existing `x-dependency-audit` skill. Identifies deeper supply chain risks: single-maintainer dependencies, typosquatting suspects, phantom dependencies, stale packages, EPSS exploit prediction, and SLSA integrity assessment.

## Relationship with x-dependency-audit

| Capability | x-dependency-audit | x-supply-chain-audit |
|------------|-------------------|----------------------|
| Known CVEs | Yes | No |
| Outdated versions | Yes | No |
| License compliance | Yes | Extends with copyleft risk scoring |
| Maintainer risk | No | Yes (bus factor analysis) |
| Typosquatting | No | Yes (Levenshtein distance) |
| Phantom dependencies | No | Yes (AST scan vs manifest diff) |
| Dependency age | No | Yes (registry metadata) |
| EPSS scoring | No | Yes (FIRST.org API) |
| SLSA assessment | No | Yes (provenance verification) |

## Triggers

- `/x-supply-chain-audit` -- full supply chain audit
- `/x-supply-chain-audit --depth deep` -- include transitive dependencies
- `/x-supply-chain-audit --focus maintainer` -- maintainer risk only
- `/x-supply-chain-audit --focus typosquatting` -- typosquatting detection only
- `/x-supply-chain-audit --focus phantom` -- phantom dependency detection only
- `/x-supply-chain-audit --focus age` -- dependency age analysis only
- `/x-supply-chain-audit --focus epss` -- EPSS exploit prediction only
- `/x-supply-chain-audit --focus slsa` -- SLSA level assessment only
- `/x-supply-chain-audit --risk-threshold 50` -- filter findings below score 50

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `--depth` | String | shallow | shallow = direct deps only; deep = includes transitive |
| `--include-dev-deps` | boolean | false | Include development dependencies |
| `--risk-threshold` | int | 0 | Exclude findings with risk score below this value |
| `--focus` | String | all | Analyze specific category: all, maintainer, typosquatting, phantom, age, epss, slsa |

## Workflow

```
1. DETECT      -> Identify build tool and parse manifest
2. RESOLVE     -> Build dependency graph (direct + transitive)
3. ANALYZE     -> Run 6 parallel analysis capabilities
4. SCORE       -> Calculate weighted risk score per dependency
5. FILTER      -> Apply risk-threshold and focus filters
6. REPORT      -> Generate SARIF 2.1.0 + Markdown report
```

## Analysis Capabilities

### Maintainer Risk

Identify dependencies with single-maintainer risk (bus factor = 1). Query registry APIs for maintainer count. Scoring: 1 maintainer = 100 (HIGH), 2-3 = 50 (MEDIUM), 4+ = 0.

### Typosquatting Detection

Detect names similar to popular packages using Levenshtein distance < 2. Scoring: distance 1 = 100 (CRITICAL), same name different scope = 75 (HIGH).

### Phantom Dependencies

Identify imports in source code not declared in the manifest via AST scan vs manifest diff. Scoring: phantom found = 75 (MEDIUM).

### Dependency Age

Check time since last release. Scoring: > 2 years = 100 (HIGH), 1-2 years = 75 (MEDIUM), 6-12 months = 25 (LOW), < 6 months = 0.

### EPSS Scoring

Query FIRST.org EPSS API for exploit probability. Scoring: >= 0.5 = 100 (CRITICAL), 0.1-0.49 = 75 (HIGH), 0.01-0.09 = 50 (MEDIUM), < 0.01 = 25 (LOW).

### SLSA Assessment

Evaluate Supply-chain Levels for Software Artifacts compliance. SLSA 0 = 100, SLSA 1 = 75, SLSA 2 = 50, SLSA 3 = 25.

## Risk Scoring

```
risk_score = (cve_severity * 0.40) + (depth_score * 0.20)
           + (maintainer_risk * 0.15) + (license_risk * 0.15)
           + (popularity_inverse * 0.10)
```

| Grade | Score Range | Description |
|-------|------------|-------------|
| A | 90-100 | Excellent supply chain posture |
| B | 80-89 | Good posture with minor risks |
| C | 70-79 | Acceptable but improvement needed |
| D | 60-69 | Below standard, remediation required |
| F | 0-59 | Failing, immediate action required |

## SARIF Rule IDs

| Rule ID | Category |
|---------|----------|
| SCA-MAINT-001 | maintainer |
| SCA-TYPO-001 | typosquatting |
| SCA-PHANTOM-001 | phantom |
| SCA-AGE-001 | age |
| SCA-EPSS-001 | epss |
| SCA-SLSA-001 | slsa |

## Output

- SARIF: `results/audits/supply-chain-audit-YYYY-MM-DD.sarif.json`
- Markdown: `results/audits/supply-chain-audit-YYYY-MM-DD.md`

## Error Handling

| Scenario | Action |
|----------|--------|
| Registry API unavailable | Skip capability, continue with others |
| No manifest found | Report error and exit |
| No dependencies | Report score 100, grade A, zero findings |
| EPSS API unavailable | Skip EPSS scoring, continue |
