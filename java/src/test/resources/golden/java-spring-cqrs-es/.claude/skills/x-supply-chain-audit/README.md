# x-supply-chain-audit

> Enhanced supply chain security audit beyond x-dependency-audit. Analyzes maintainer risk, typosquatting detection, phantom dependencies, dependency age, EPSS scoring, and SLSA assessment. Produces SARIF 2.1.0 output with weighted risk scoring.

| | |
|---|---|
| **Category** | Security |
| **Invocation** | `/x-supply-chain-audit [--depth shallow\|deep] [--include-dev-deps] [--risk-threshold 0-100] [--focus all\|maintainer\|typosquatting\|phantom\|age\|epss\|slsa]` |
| **Reads** | `knowledge/security/sarif-template.md`, `knowledge/security/security-scoring.md`, `knowledge/security/supply-chain-hardening.md`, `knowledge/security/sbom-generation-guide.md` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Performs advanced supply chain security analysis that complements `x-dependency-audit`. While that skill covers known CVEs and outdated versions, this skill identifies deeper risks: single-maintainer dependencies (bus factor), typosquatting suspects via Levenshtein distance, phantom dependencies not declared in manifests, stale packages, EPSS exploit prediction from FIRST.org, and SLSA integrity assessment. Produces a multi-dimensional weighted risk score per dependency.

## Usage

```
/x-supply-chain-audit
/x-supply-chain-audit --depth deep
/x-supply-chain-audit --focus typosquatting
/x-supply-chain-audit --risk-threshold 50
```

## Workflow

1. Detect build tool and parse dependency manifest
2. Resolve dependency graph (direct and transitive)
3. Run 6 parallel analysis capabilities (maintainer, typosquatting, phantom, age, EPSS, SLSA)
4. Calculate weighted risk score per dependency
5. Apply risk-threshold and focus filters
6. Generate SARIF 2.1.0 and Markdown reports

## Outputs

| Artifact | Path |
|----------|------|
| SARIF report | `results/audits/supply-chain-audit-YYYY-MM-DD.sarif.json` |
| Markdown report | `results/audits/supply-chain-audit-YYYY-MM-DD.md` |

## See Also

- [x-dependency-audit](../x-dependency-audit/) -- CVE scanning, outdated versions, license compliance, and SBOM generation
- [x-security-dashboard](../x-security-dashboard/) -- Aggregated security posture view from all scanning skills
- [x-security-pipeline](../x-security-pipeline/) -- CI/CD pipeline generation with security stages
