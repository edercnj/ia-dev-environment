---
name: x-dependency-audit
description: >
  Checks project dependencies for vulnerabilities, outdated versions, and license
  issues. Detects build tool automatically, runs language-specific audit commands,
  and generates a severity-categorized report.
  Reference: `.github/skills/x-dependency-audit/SKILL.md`
---

# Skill: Dependency Audit

## Purpose

Audits all dependencies of {{PROJECT_NAME}} for security vulnerabilities, outdated versions, and license compliance.

## Triggers

- `/x-dependency-audit` -- full audit
- `/x-dependency-audit --scope vulnerabilities` -- security only
- `/x-dependency-audit --scope outdated` -- outdated packages only
- `/x-dependency-audit --scope licenses` -- license compliance only

## Workflow

```
1. DETECT     -> Identify build tool and package manager
2. AUDIT      -> Run language-specific audit commands
3. PARSE      -> Parse results into structured findings
4. CATEGORIZE -> Assign severity to each finding
5. REPORT     -> Generate audit report
```

### Step 1 -- Detect Build Tool

Project uses **{{BUILD_TOOL}}**. Detect lock files:

| Build Tool | Lock File | Language |
|-----------|-----------|----------|
| npm | package-lock.json | JavaScript/TypeScript |
| maven | pom.xml | Java/Kotlin |
| gradle | build.gradle(.kts) | Java/Kotlin |
| cargo | Cargo.lock | Rust |
| pip/poetry | requirements.txt / poetry.lock | Python |
| go mod | go.sum | Go |

### Step 2 -- Run Audit Commands

#### Vulnerabilities

| Build Tool | Command |
|-----------|---------|
| npm | `npm audit --json` |
| maven | `mvn org.sonatype.ossindex.maven:ossindex-maven-plugin:audit` |
| gradle | `gradle dependencyCheckAnalyze` |
| cargo | `cargo audit --json` |
| pip | `pip-audit --format json` |
| go mod | `govulncheck -json ./...` |

#### Outdated

| Build Tool | Command |
|-----------|---------|
| npm | `npm outdated --json` |
| maven | `mvn versions:display-dependency-updates` |
| cargo | `cargo outdated --format json` |
| pip | `pip list --outdated --format json` |
| go mod | `go list -m -u all` |

#### Licenses

| Build Tool | Command |
|-----------|---------|
| npm | `npx license-checker --json` |
| maven | `mvn license:third-party-report` |
| cargo | `cargo license --json` |
| pip | `pip-licenses --format json` |

### Step 3 -- Categorize Findings

| Severity | Criteria |
|----------|----------|
| **CRITICAL** | Known exploited CVE, RCE, data exposure |
| **HIGH** | CVSS >= 7.0, GPL in proprietary project |
| **MEDIUM** | CVSS 4.0-6.9, major version behind |
| **LOW** | CVSS < 4.0, minor/patch behind |

### Step 4 -- Generate Report

Write to `results/audits/dependency-audit-YYYY-MM-DD.md`:

```markdown
# Dependency Audit — {{PROJECT_NAME}}

**Date:** YYYY-MM-DD | **Build Tool:** {{BUILD_TOOL}}

| Dimension | CRITICAL | HIGH | MEDIUM | LOW |
|-----------|----------|------|--------|-----|
| Vulnerabilities | N | N | N | N |
| Outdated | — | N | N | N |
| Licenses | — | N | N | N |

## Vulnerabilities
### [V-001] {Package} — {CVE}
- **Severity:** CRITICAL
- **Fix:** `{update command}`

## Outdated Dependencies
| Package | Current | Latest | Type |
|---------|---------|--------|------|

## License Issues
| Package | License | Issue |
|---------|---------|-------|

## Recommendations
1. Fix CRITICAL vulnerabilities immediately
2. Update HIGH-risk outdated packages
3. Review license compliance
```

## Error Handling

| Scenario | Action |
|----------|--------|
| Tool not installed | Suggest install, continue with others |
| No lock file | Warn, attempt without |
| Command fails | Report error, continue |

## SBOM Generation

Generate a CycloneDX JSON Software Bill of Materials listing all direct and transitive dependencies.

- `/x-dependency-audit --scope sbom` -- generate SBOM only

| Build Tool | Command |
|-----------|---------|
| npm | `npx @cyclonedx/cdxgen -o sbom.json` |
| maven | `mvn org.cyclonedx:cyclonedx-maven-plugin:makeAggregateBom -DoutputFormat=json` |
| gradle | `gradle cyclonedxBom` |
| cargo | `cargo cyclonedx --format json` |
| pip | `cyclonedx-py environment -o sbom.json --output-format json` |
| go mod | `cyclonedx-gomod mod -json -output sbom.json` |

Output: `results/audits/sbom-YYYY-MM-DD.json` (CycloneDX 1.6 JSON)

## License Attribution Report

Generate license attribution report highlighting copyleft dependencies.

- `/x-dependency-audit --scope license-report` -- generate license report

Classifies dependencies as: Permissive, Weak copyleft (LGPL, MPL), Strong copyleft (GPL, AGPL), Unknown.
Output: `results/audits/license-attribution-YYYY-MM-DD.md`

## Dependency Tree Visualization

Generate dependency tree with risk scoring per node.

- `/x-dependency-audit --scope tree` -- generate dependency tree

| Build Tool | Command |
|-----------|---------|
| npm | `npm ls --all --json` |
| maven | `mvn dependency:tree -DoutputType=text` |
| gradle | `gradle dependencies` |
| cargo | `cargo tree` |
| pip | `pipdeptree --json` |
| go mod | `go mod graph` |

Risk factors: Known CVE (40%), Depth (20%), Maintainer activity (15%), License risk (15%), Popularity (10%).
Output: `results/audits/dependency-tree-YYYY-MM-DD.md`
