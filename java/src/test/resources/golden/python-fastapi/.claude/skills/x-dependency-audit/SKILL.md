---
name: x-dependency-audit
description: "Checks project dependencies for vulnerabilities, outdated versions, and license issues. Detects build tool automatically, runs language-specific audit commands, and generates a severity-categorized report."
allowed-tools: Read, Write, Bash, Grep, Glob
argument-hint: "[--scope all|vulnerabilities|outdated|licenses]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Dependency Audit

## Purpose

Audits all dependencies of {{PROJECT_NAME}} for security vulnerabilities, outdated versions, and license compliance. Generates a structured report with severity-categorized findings and remediation recommendations.

## Triggers

- `/x-dependency-audit` -- full audit (vulnerabilities + outdated + licenses)
- `/x-dependency-audit --scope vulnerabilities` -- security vulnerabilities only
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

### Step 2 -- Run Audit Commands

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

### Step 3 -- Parse Results

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

### Step 4 -- Categorize Findings

| Severity | Criteria |
|----------|----------|
| **CRITICAL** | Known exploited CVE, RCE vulnerability, data exposure |
| **HIGH** | CVE with CVSS >= 7.0, GPL license in proprietary project |
| **MEDIUM** | CVE with CVSS 4.0-6.9, major version behind, LGPL license |
| **LOW** | CVE with CVSS < 4.0, minor/patch version behind, permissive license issue |

### Step 5 -- Generate Report

Write report to `docs/audits/dependency-audit-YYYY-MM-DD.md`:

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

## Error Handling

| Scenario | Action |
|----------|--------|
| Audit tool not installed | Suggest installation command, continue with available tools |
| No lock file found | Warn and attempt audit without lock file |
| Audit command fails | Report error, continue with other dimensions |
| No dependencies found | Report "No dependencies found" |
| Offline mode | Skip vulnerability check, proceed with outdated and license |
