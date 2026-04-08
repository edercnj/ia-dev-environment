# x-dependency-audit

> Checks project dependencies for vulnerabilities, outdated versions, and license issues. Detects build tool automatically, runs language-specific audit commands, and generates a severity-categorized report.

| | |
|---|---|
| **Category** | Security |
| **Invocation** | `/x-dependency-audit [--scope all\|vulnerabilities\|outdated\|licenses\|sbom\|license-report\|tree]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Audits all project dependencies for security vulnerabilities, outdated versions, and license compliance. Automatically detects the build tool (npm, maven, gradle, cargo, pip, go mod, etc.), runs language-specific audit commands, and generates a structured report with severity-categorized findings and remediation recommendations. Also supports SBOM generation (CycloneDX), license attribution reports, and dependency tree visualization with risk scoring.

## Usage

```
/x-dependency-audit
/x-dependency-audit --scope vulnerabilities
/x-dependency-audit --scope sbom
/x-dependency-audit --scope license-report
/x-dependency-audit --scope tree
```

## Workflow

1. Detect build tool and package manager from project files
2. Run language-specific audit commands (vulnerabilities, outdated, licenses)
3. Parse results into structured findings with CVE identifiers
4. Categorize findings by severity (CRITICAL/HIGH/MEDIUM/LOW)
5. Generate audit report with remediation recommendations

## Outputs

| Artifact | Path |
|----------|------|
| Audit report | `results/audits/dependency-audit-YYYY-MM-DD.md` |
| SBOM (CycloneDX) | `results/audits/sbom-YYYY-MM-DD.json` |
| License attribution | `results/audits/license-attribution-YYYY-MM-DD.md` |
| Dependency tree | `results/audits/dependency-tree-YYYY-MM-DD.md` |

## See Also

- [x-supply-chain-audit](../x-supply-chain-audit/) -- Advanced supply chain analysis (maintainer risk, typosquatting, EPSS)
- [x-owasp-scan](../x-owasp-scan/) -- OWASP Top 10 verification (delegates A06 to this skill)
- [x-security-dashboard](../x-security-dashboard/) -- Aggregated security posture view from all scanning skills
