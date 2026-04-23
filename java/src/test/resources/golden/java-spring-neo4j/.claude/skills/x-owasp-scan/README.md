# x-owasp-scan

> Automated OWASP Top 10 (2021) verification mapped to ASVS levels (L1/L2/L3). Checks all 10 categories (A01-A10) with per-category pass/fail, ASVS coverage percentage, score grading, SARIF 2.1.0 output, and CI integration. Delegates A06 to x-dependency-audit.

| | |
|---|---|
| **Category** | Security |
| **Invocation** | `/x-owasp-scan [--level L1\|L2\|L3] [--category A01-A10\|all] [--report-format markdown\|sarif\|both]` |
| **Reads** | `knowledge/security/`, `knowledge/security/application-security.md` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Verifies the project against the OWASP Top 10 (2021) with verification items mapped to ASVS chapters and levels. Produces per-category pass/fail results, an overall score (0-100), ASVS coverage percentage, and generates both SARIF 2.1.0 and Markdown reports. A06 (Vulnerable Components) is automatically delegated to `x-dependency-audit`.

## Usage

```
/x-owasp-scan
/x-owasp-scan --level L2
/x-owasp-scan --category A03
/x-owasp-scan --report-format sarif
```

## Workflow

1. Parse CLI parameters (level, category, report-format)
2. Load ASVS verification items from security knowledge pack
3. Map OWASP Top 10 categories to ASVS chapters
4. Execute verification checks per category (A01-A05, A07-A10)
5. Delegate A06 to `x-dependency-audit`
6. Calculate per-category and overall scores with grade mapping
7. Generate SARIF 2.1.0 and Markdown reports

## Outputs

| Artifact | Path |
|----------|------|
| SARIF report | `results/security/owasp-scan-YYYY-MM-DD.sarif.json` |
| Markdown report | `results/security/owasp-scan-YYYY-MM-DD.md` |

## See Also

- [x-dependency-audit](../x-dependency-audit/) -- Dependency vulnerability, outdated version, and license audit (handles A06)
- [x-hardening-eval](../x-hardening-eval/) -- Application hardening posture evaluation against CIS/OWASP benchmarks
- [x-security-dashboard](../x-security-dashboard/) -- Aggregated security posture view from all scanning skills
