# x-security-dashboard

> Aggregates results from all security scanning skills into a unified posture view with score 0-100, trend tracking, OWASP risk heatmap, per-dimension breakdown, and remediation priority queue. Never executes scans -- reads existing results only (RULE-011).

| | |
|---|---|
| **Category** | Security |
| **Invocation** | `/x-security-dashboard [--period last-7d\|last-30d\|last-90d\|all] [--format markdown\|json] [--compare-previous]` |
| **Reads** | `knowledge/security/security-scoring.md`, `knowledge/security/security-skill-template.md` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Generates a consolidated security posture dashboard by aggregating SARIF results from all 10 security scanning dimensions (SAST, DAST, secrets, container, infrastructure, OWASP, SonarQube, hardening, runtime protection, supply chain). Computes weighted overall scores, builds an OWASP risk heatmap, ranks the top 10 findings, and produces a prioritized remediation queue. This skill never executes scans -- it reads existing results from `results/security/` only.

## Usage

```
/x-security-dashboard
/x-security-dashboard --period last-30d
/x-security-dashboard --format json
/x-security-dashboard --period last-7d --compare-previous
```

## Workflow

1. Scan `results/security/` directory for SARIF files
2. Filter files by selected time period
3. Parse SARIF results and extract findings per dimension
4. Compute per-dimension and weighted overall scores
5. Compare with previous period for trend analysis (if requested)
6. Build OWASP category x severity risk heatmap
7. Select top 10 findings and build remediation priority queue
8. Render dashboard in Markdown or JSON format

## Outputs

| Artifact | Path |
|----------|------|
| Markdown dashboard | `results/security/dashboard-{YYYYMMDD}-{HHMMSS}.md` |
| JSON dashboard | `results/security/dashboard-{YYYYMMDD}-{HHMMSS}.json` |

## See Also

- [x-owasp-scan](../x-owasp-scan/) -- OWASP Top 10 verification (feeds into dashboard)
- [x-hardening-eval](../x-hardening-eval/) -- Application hardening evaluation (feeds into dashboard)
- [x-runtime-eval](../x-runtime-eval/) -- Runtime protection evaluation (feeds into dashboard)
- [x-security-pipeline](../x-security-pipeline/) -- CI/CD pipeline generation with security stages
