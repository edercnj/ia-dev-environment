---
name: x-dependency-audit
description: "Checks project dependencies for vulnerabilities, outdated versions, and license issues. Detects build tool automatically, runs language-specific audit commands, and generates a severity-categorized report."
user-invocable: true
allowed-tools: Read, Write, Bash, Grep, Glob
argument-hint: "[--scope all|vulnerabilities|outdated|licenses|sbom|license-report|tree]"
context-budget: medium
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Dependency Audit

## Purpose

Audits all dependencies of {{PROJECT_NAME}} for security vulnerabilities, outdated versions, and license compliance. Generates a structured report with severity-categorized findings and remediation recommendations. Also supports SBOM generation, license attribution reports, and dependency tree visualization.

## Triggers

- `/x-dependency-audit` — full audit (vulnerabilities + outdated + licenses)
- `/x-dependency-audit --scope vulnerabilities` — security vulnerabilities only
- `/x-dependency-audit --scope outdated` — outdated packages only
- `/x-dependency-audit --scope licenses` — license compliance only
- `/x-dependency-audit --scope sbom` — generate CycloneDX SBOM only
- `/x-dependency-audit --scope license-report` — generate license attribution report
- `/x-dependency-audit --scope tree` — generate dependency tree visualization

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `--scope` | Enum | `all` | Audit scope: all, vulnerabilities, outdated, licenses, sbom, license-report, tree |

## Workflow

```
1. DETECT     -> Identify build tool and package manager
2. AUDIT      -> Run language-specific audit commands
3. PARSE      -> Parse results into structured findings
4. CATEGORIZE -> Assign severity to each finding
5. REPORT     -> Generate audit report
```

### Step 1 — Detect Build Tool

The project uses **{{BUILD_TOOL}}** as its build tool. Detect the package manager and lock files:

| Build Tool | Lock File | Language |
|-----------|-----------|----------|
| npm | package-lock.json | JavaScript/TypeScript |
| yarn | yarn.lock | JavaScript/TypeScript |
| pnpm | pnpm-lock.yaml | JavaScript/TypeScript |
| maven | pom.xml | Java/Kotlin |
| gradle | build.gradle / build.gradle.kts | Java/Kotlin |
| cargo | Cargo.lock | Rust |
| pip | requirements.txt / Pipfile.lock | Python |
| poetry | poetry.lock | Python |
| go mod | go.sum | Go |

### Step 2 — Run Audit Commands

#### Vulnerabilities

| Build Tool | Command |
|-----------|---------|
| npm | `npm audit --json` |
| yarn | `yarn audit --json` |
| pnpm | `pnpm audit --json` |
| maven | `mvn org.sonatype.ossindex.maven:ossindex-maven-plugin:audit` |
| gradle | `gradle dependencyCheckAnalyze` (OWASP plugin) |
| cargo | `cargo audit --json` |
| pip | `pip-audit --format json` |
| poetry | `poetry audit` |
| go mod | `govulncheck -json ./...` |

#### Outdated Packages

| Build Tool | Command |
|-----------|---------|
| npm | `npm outdated --json` |
| yarn | `yarn outdated --json` |
| pnpm | `pnpm outdated --format json` |
| maven | `mvn versions:display-dependency-updates` |
| gradle | `gradle dependencyUpdates` (versions plugin) |
| cargo | `cargo outdated --format json` |
| pip | `pip list --outdated --format json` |
| poetry | `poetry show --outdated` |
| go mod | `go list -m -u all` |

#### License Check

| Build Tool | Command |
|-----------|---------|
| npm | `npx license-checker --json` |
| yarn | `npx license-checker --json` |
| maven | `mvn license:third-party-report` |
| gradle | `gradle generateLicenseReport` (license plugin) |
| cargo | `cargo license --json` |
| pip | `pip-licenses --format json` |
| go mod | `go-licenses report ./...` |

### Step 3 — Parse Results

For each audit dimension, extract:

**Vulnerabilities:**
```
- Package name and version
- CVE identifier (if available)
- Severity (CRITICAL/HIGH/MEDIUM/LOW)
- Description
- Fixed version (if available)
- Path (dependency chain)
```

**Outdated:**
```
- Package name
- Current version
- Latest version
- Update type (major/minor/patch)
- Breaking changes risk
```

**Licenses:**
```
- Package name
- License type (MIT, Apache-2.0, GPL-3.0, etc.)
- Compatibility with project license
- Copyleft risk
```

### Step 4 — Categorize Findings

| Severity | Criteria |
|----------|----------|
| **CRITICAL** | Known exploited CVE, RCE vulnerability, data exposure |
| **HIGH** | CVE with CVSS >= 7.0, GPL license in proprietary project |
| **MEDIUM** | CVE with CVSS 4.0-6.9, major version behind, LGPL license |
| **LOW** | CVE with CVSS < 4.0, minor/patch version behind, permissive license issue |

### Step 5 — Generate Report

Write report to `results/audits/dependency-audit-YYYY-MM-DD.md`:

```markdown
# Dependency Audit Report — {{PROJECT_NAME}}

**Date:** YYYY-MM-DD
**Build Tool:** {{BUILD_TOOL}}
**Total Dependencies:** {count}

## Summary

| Dimension | CRITICAL | HIGH | MEDIUM | LOW | Total |
|-----------|----------|------|--------|-----|-------|
| Vulnerabilities | N | N | N | N | N |
| Outdated | — | N | N | N | N |
| Licenses | — | N | N | N | N |

## Vulnerabilities

### [V-001] {Package} — {CVE ID}
- **Severity:** CRITICAL
- **Current:** {version}
- **Fixed:** {fixed_version}
- **Description:** {description}
- **Action:** `{command to update}`

## Outdated Dependencies

| Package | Current | Latest | Type | Risk |
|---------|---------|--------|------|------|
| {name} | {current} | {latest} | major | Breaking changes possible |
| {name} | {current} | {latest} | minor | Low risk |

## License Issues

| Package | License | Issue |
|---------|---------|-------|
| {name} | GPL-3.0 | Copyleft — incompatible with project license |

## Recommendations

1. **Immediate:** Fix CRITICAL vulnerabilities
2. **Short-term:** Update HIGH-risk outdated packages
3. **Long-term:** Review license compliance strategy
```

## SBOM Generation

Generate a CycloneDX JSON Software Bill of Materials listing all direct and transitive dependencies.

### SBOM Workflow

```
1. DETECT     -> Identify build tool (reuse Step 1)
2. GENERATE   -> Run CycloneDX generation command
3. VALIDATE   -> Verify SBOM contains required fields
4. OUTPUT     -> Write CycloneDX JSON to results/audits/sbom-YYYY-MM-DD.json
```

### Generation Commands

| Build Tool | Command |
|-----------|---------|
| npm | `npx @cyclonedx/cdxgen -o sbom.json` |
| maven | `mvn org.cyclonedx:cyclonedx-maven-plugin:makeAggregateBom -DoutputFormat=json` |
| gradle | `gradle cyclonedxBom` (cyclonedx-gradle-plugin) |
| cargo | `cargo cyclonedx --format json` |
| pip | `cyclonedx-py environment -o sbom.json --output-format json` |
| poetry | `cyclonedx-py environment -o sbom.json --output-format json` |
| go mod | `cyclonedx-gomod mod -json -output sbom.json` |

### Required SBOM Fields

Each component in the generated SBOM must include:

```
- name: Package name
- version: Exact version
- purl: Package URL (pkg:maven/group/artifact@version)
- licenses[]: SPDX license identifier(s)
- hashes[]: SHA-256 digest for integrity verification
- scope: required | optional | excluded
```

### SBOM Output

Write CycloneDX JSON to `results/audits/sbom-YYYY-MM-DD.json` and generate a human-readable summary:

```markdown
# SBOM Summary — {{PROJECT_NAME}}

**Date:** YYYY-MM-DD
**Format:** CycloneDX 1.6
**Total Components:** {count}

| Type | Count |
|------|-------|
| Direct dependencies | N |
| Transitive dependencies | N |
| Dev dependencies | N |

## License Distribution

| License | Count | Copyleft |
|---------|-------|----------|
| MIT | N | No |
| Apache-2.0 | N | No |
| GPL-3.0 | N | Yes |
```

## License Attribution Report

Generate a comprehensive license attribution report for all dependencies, highlighting copyleft licenses that may impose obligations.

### License Report Workflow

```
1. DETECT     -> Identify build tool (reuse Step 1)
2. COLLECT    -> Gather license data for all dependencies
3. CLASSIFY   -> Categorize by license type and copyleft risk
4. REPORT     -> Generate attribution report
```

### License Report Output

Write report to `results/audits/license-attribution-YYYY-MM-DD.md`:

```markdown
# License Attribution Report — {{PROJECT_NAME}}

**Date:** YYYY-MM-DD
**Total Dependencies:** {count}

## Summary

| Category | Count | Percentage |
|----------|-------|------------|
| Permissive (MIT, Apache-2.0, BSD) | N | N% |
| Weak copyleft (LGPL, MPL, EPL) | N | N% |
| Strong copyleft (GPL, AGPL) | N | N% |
| Unknown / Custom | N | N% |

## Copyleft Dependencies (Requires Review)

| Package | Version | License | Risk | Action |
|---------|---------|---------|------|--------|
| {name} | {version} | GPL-3.0 | HIGH | Legal review required |
| {name} | {version} | LGPL-2.1 | MEDIUM | Check linking compatibility |

## Full Attribution

| Package | Version | License | URL |
|---------|---------|---------|-----|
| {name} | {version} | MIT | {repo_url} |
```

## Dependency Tree Visualization

Generate a visual dependency tree showing transitive relationships and risk scores.

### Tree Workflow

```
1. DETECT     -> Identify build tool (reuse Step 1)
2. RESOLVE    -> Build full dependency graph
3. SCORE      -> Assign risk score to each node
4. RENDER     -> Generate tree visualization
```

### Tree Commands

| Build Tool | Command |
|-----------|---------|
| npm | `npm ls --all --json` |
| maven | `mvn dependency:tree -DoutputType=text` |
| gradle | `gradle dependencies` |
| cargo | `cargo tree` |
| pip | `pipdeptree --json` |
| go mod | `go mod graph` |

### Risk Scoring

| Factor | Weight | Description |
|--------|--------|-------------|
| Known CVE | 40% | Active vulnerabilities in the dependency |
| Depth | 20% | Distance from direct dependency (deeper = harder to update) |
| Maintainer activity | 15% | Last update, number of maintainers |
| License risk | 15% | Copyleft or unknown license |
| Popularity | 10% | Download count, dependents (low = higher risk) |

### Tree Output

Write tree to `results/audits/dependency-tree-YYYY-MM-DD.md`:

```markdown
# Dependency Tree — {{PROJECT_NAME}}

**Date:** YYYY-MM-DD
**Total Nodes:** {count}
**Max Depth:** {depth}

## Risk Summary

| Risk Level | Count |
|------------|-------|
| HIGH (score >= 7) | N |
| MEDIUM (score 4-6) | N |
| LOW (score < 4) | N |

## Tree

{package}@{version} [risk: LOW]
├── {dep-a}@{version} [risk: LOW]
│   ├── {transitive-1}@{version} [risk: MEDIUM]
│   └── {transitive-2}@{version} [risk: LOW]
└── {dep-b}@{version} [risk: HIGH]
    └── {transitive-3}@{version} [risk: HIGH]
```

## Error Handling

| Scenario | Action |
|----------|--------|
| Audit tool not installed | Suggest installation command, continue with available tools |
| No lock file found | Warn and attempt audit without lock file |
| Audit command fails | Report error, continue with other dimensions |
| No dependencies found | Report "No dependencies found" |
| Offline mode | Skip vulnerability check, proceed with outdated and license |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-supply-chain-audit` | complementary | Handles deeper supply chain risks (maintainer, typosquatting, SLSA) |
| `x-ci-cd-generate` | called-by | Dependency audit pipeline references audit commands from this skill |
| `x-security-dashboard` | reads | Dashboard aggregates results from this skill |
