---
name: x-supply-chain-audit
description: "Enhanced supply chain security audit beyond x-dependency-audit. Analyzes maintainer risk, typosquatting detection, phantom dependencies, dependency age, EPSS scoring, and SLSA assessment. Produces SARIF 2.1.0 output with weighted risk scoring."
user-invocable: true
allowed-tools: Read, Write, Bash, Grep, Glob
argument-hint: "[--depth shallow|deep] [--include-dev-deps] [--risk-threshold 0-100] [--focus all|maintainer|typosquatting|phantom|age|epss|slsa]"
context-budget: medium
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Enhanced Supply Chain Audit

## Purpose

Performs advanced supply chain security analysis for {{PROJECT_NAME}} that complements (does NOT replace) the existing `x-dependency-audit` skill. While `x-dependency-audit` focuses on known CVEs, outdated versions, and license compliance, this skill identifies deeper supply chain risks: single-maintainer dependencies, typosquatting suspects, phantom dependencies, stale packages, EPSS exploit prediction, and SLSA integrity assessment.

## Relationship with x-dependency-audit

| Capability | x-dependency-audit | x-supply-chain-audit |
|------------|-------------------|----------------------|
| Known CVEs | Yes | No (defers to x-dependency-audit) |
| Outdated versions | Yes | No (defers to x-dependency-audit) |
| License compliance | Yes | Extends with copyleft risk scoring |
| SBOM generation | Yes (CycloneDX) | No (defers to x-dependency-audit) |
| Maintainer risk | No | Yes (bus factor analysis) |
| Typosquatting | No | Yes (Levenshtein distance) |
| Phantom dependencies | No | Yes (AST scan vs manifest diff) |
| Dependency age | No | Yes (registry metadata) |
| EPSS scoring | No | Yes (FIRST.org API) |
| SLSA assessment | No | Yes (provenance verification) |
| Risk scoring | Basic (severity only) | Multi-dimensional (5 weighted factors) |

Both skills can be executed independently. Results from both feed into the security dashboard.

## Triggers

- `/x-supply-chain-audit` — full supply chain audit (all 6 capabilities)
- `/x-supply-chain-audit --depth deep` — deep analysis including transitive dependencies
- `/x-supply-chain-audit --focus maintainer` — maintainer risk analysis only
- `/x-supply-chain-audit --focus typosquatting` — typosquatting detection only
- `/x-supply-chain-audit --focus phantom` — phantom dependency detection only
- `/x-supply-chain-audit --focus age` — dependency age analysis only
- `/x-supply-chain-audit --focus epss` — EPSS exploit prediction only
- `/x-supply-chain-audit --focus slsa` — SLSA level assessment only
- `/x-supply-chain-audit --risk-threshold 50` — filter findings below score 50
- `/x-supply-chain-audit --include-dev-deps` — include dev dependencies

## Parameters

| Parameter | Type | Default | Validation | Description |
|-----------|------|---------|------------|-------------|
| `--depth` | String | shallow | enum: shallow, deep | shallow = direct deps only; deep = includes transitive |
| `--include-dev-deps` | boolean | false | — | Include development dependencies in analysis |
| `--risk-threshold` | int | 0 | 0-100 | Exclude findings with risk score below this value |
| `--focus` | String | all | enum: all, maintainer, typosquatting, phantom, age, epss, slsa | Analyze specific risk category only |

## Workflow

```
1. DETECT      -> Identify build tool and parse manifest
2. RESOLVE     -> Build dependency graph (direct + transitive)
3. ANALYZE     -> Run 6 parallel analysis capabilities
4. SCORE       -> Calculate weighted risk score per dependency
5. FILTER      -> Apply risk-threshold and focus filters
6. REPORT      -> Generate SARIF 2.1.0 + Markdown report
```

### Step 1 — Detect Build Tool and Parse Manifest

The project uses **{{BUILD_TOOL}}** as its build tool. Parse the dependency manifest:

| Build Tool | Manifest File | Lock File |
|-----------|--------------|-----------|
| npm | package.json | package-lock.json |
| yarn | package.json | yarn.lock |
| pnpm | package.json | pnpm-lock.yaml |
| maven | pom.xml | — |
| gradle | build.gradle / build.gradle.kts | gradle.lockfile |
| cargo | Cargo.toml | Cargo.lock |
| pip | requirements.txt / pyproject.toml | requirements.txt |
| poetry | pyproject.toml | poetry.lock |
| go mod | go.mod | go.sum |

### Step 2 — Resolve Dependency Graph

Build the full dependency graph distinguishing direct vs transitive:

| Build Tool | Command |
|-----------|---------|
| npm | `npm ls --all --json` |
| maven | `mvn dependency:tree -DoutputType=text` |
| gradle | `gradle dependencies --configuration runtimeClasspath` |
| cargo | `cargo tree --format {p}:{l}` |
| pip | `pipdeptree --json` |
| poetry | `poetry show --tree` |
| go mod | `go mod graph` |

### Step 3 — Analysis Capabilities

#### 3.1 — Maintainer Risk Analysis

Identify dependencies with single-maintainer risk (bus factor = 1):

| Registry | API Endpoint | Maintainer Field |
|----------|-------------|-----------------|
| npm | `https://registry.npmjs.org/{pkg}` | `maintainers[]` |
| Maven Central | `https://search.maven.org/solrsearch/select?q=a:{artifact}` | POM developers section |
| PyPI | `https://pypi.org/pypi/{pkg}/json` | `info.maintainers[]` |
| crates.io | `https://crates.io/api/v1/crates/{pkg}` | `crate.owners[]` |
| Go | Module repository metadata | — |

**Scoring:**
- 1 maintainer: risk = 100 (HIGH severity)
- 2-3 maintainers: risk = 50 (MEDIUM severity)
- 4+ maintainers: risk = 0 (no risk)

#### 3.2 — Typosquatting Detection

Detect dependency names suspiciously similar to popular packages using Levenshtein distance:

1. For each dependency, compute Levenshtein distance against a known list of popular packages for the ecosystem
2. Flag packages with distance < 2 from a popular package
3. Popular package lists per ecosystem:

| Registry | Popular Packages Source |
|----------|----------------------|
| npm | Top 1000 by weekly downloads |
| Maven Central | Top artifacts by usage |
| PyPI | Top 1000 by monthly downloads |
| crates.io | Top crates by downloads |

**Scoring:**
- Levenshtein distance = 1: risk = 100 (CRITICAL severity)
- Same name different scope/org: risk = 75 (HIGH severity)

#### 3.3 — Phantom Dependency Detection

Identify imports in source code that are not declared in the manifest:

1. Scan source files for import statements:

| Language | Import Pattern |
|----------|---------------|
| Java/Kotlin | `import {package}.{Class}` |
| TypeScript/JS | `import ... from '{module}'` / `require('{module}')` |
| Python | `import {module}` / `from {module} import ...` |
| Go | `import "{module}"` |
| Rust | `use {crate}::...` / `extern crate {name}` |

2. Cross-reference detected imports against declared dependencies in manifest
3. Flag imports that resolve to undeclared dependencies

**Scoring:**
- Phantom dependency found: risk = 75 (MEDIUM severity)

#### 3.4 — Dependency Age Analysis

Check time since last release for each dependency:

| Registry | Last Release API |
|----------|-----------------|
| npm | `https://registry.npmjs.org/{pkg}` -> `time.modified` |
| Maven Central | `https://search.maven.org/solrsearch/select` -> `timestamp` |
| PyPI | `https://pypi.org/pypi/{pkg}/json` -> `urls[].upload_time` |
| crates.io | `https://crates.io/api/v1/crates/{pkg}` -> `crate.updated_at` |

**Scoring:**
- Last release > 2 years: risk = 100 (HIGH severity)
- Last release 1-2 years: risk = 75 (MEDIUM severity)
- Last release 6-12 months: risk = 25 (LOW severity)
- Last release < 6 months: risk = 0 (no risk)

#### 3.5 — EPSS Scoring (Exploit Prediction)

Query FIRST.org EPSS API for exploit probability of known CVEs:

- **API:** `https://api.first.org/data/v1/epss?cve={CVE-ID}`
- **Response field:** `epss` (probability 0.0-1.0 of exploit within 30 days)

**Scoring:**
- EPSS >= 0.5: risk = 100 (CRITICAL severity — high probability of exploit)
- EPSS 0.1-0.49: risk = 75 (HIGH severity)
- EPSS 0.01-0.09: risk = 50 (MEDIUM severity)
- EPSS < 0.01: risk = 25 (LOW severity)

#### 3.6 — SLSA Assessment

Evaluate Supply-chain Levels for Software Artifacts compliance:

| SLSA Level | Requirements | Score |
|-----------|-------------|-------|
| SLSA 0 | No guarantees | risk = 100 |
| SLSA 1 | Build process documented | risk = 75 |
| SLSA 2 | Hosted build, signed provenance | risk = 50 |
| SLSA 3 | Hardened builds, non-falsifiable provenance | risk = 25 |

Check for:
- Signed releases / provenance attestations
- Reproducible builds
- Build system documentation
- Source-to-build mapping

### Step 4 — Risk Scoring

Calculate weighted risk score for each dependency:

```
risk_score = (cve_severity * 0.40)
           + (depth_score * 0.20)
           + (maintainer_risk * 0.15)
           + (license_risk * 0.15)
           + (popularity_inverse * 0.10)
```

**Factor Ranges (all 0-100):**

| Factor | Calculation |
|--------|------------|
| cve_severity | CVSS score normalized to 0-100 |
| depth_score | direct = 100, depth 1 = 75, depth 2 = 50, depth 3+ = 25 |
| maintainer_risk | single = 100, 2-3 = 50, 4+ = 0 |
| license_risk | copyleft = 100, weak-copyleft = 50, permissive = 0, unknown = 75 |
| popularity_inverse | downloads < 1000/week = 100, < 10000 = 50, >= 10000 = 0 |

**Severity Classification:**

| Risk Score | Severity |
|-----------|----------|
| 80-100 | CRITICAL |
| 60-79 | HIGH |
| 40-59 | MEDIUM |
| 20-39 | LOW |
| 0-19 | INFO |

### Step 5 — Apply Filters

1. If `--focus` is not `all`, include only findings matching the specified `riskCategory`
2. If `--risk-threshold` > 0, exclude findings with `riskScore` below the threshold
3. If `--include-dev-deps` is false, exclude dev/test-scoped dependencies
4. If `--depth` is `shallow`, exclude transitive dependencies

### Step 6 — Generate Reports

#### 6.1 — SARIF 2.1.0 Output

Write SARIF to `results/audits/supply-chain-audit-YYYY-MM-DD.sarif.json`:

Follow the SARIF template from `knowledge/security/sarif-template.md`. Use tool name `x-supply-chain-audit` and rule IDs:

| Rule ID | Name | Category |
|---------|------|----------|
| SCA-MAINT-001 | SingleMaintainerDependency | maintainer |
| SCA-TYPO-001 | TyposquattingSuspect | typosquatting |
| SCA-PHANTOM-001 | PhantomDependency | phantom |
| SCA-AGE-001 | StaleDependency | age |
| SCA-EPSS-001 | HighExploitProbability | epss |
| SCA-SLSA-001 | LowSlsaLevel | slsa |

#### 6.2 — Markdown Report

Write report to `results/audits/supply-chain-audit-YYYY-MM-DD.md`:

```markdown
# Supply Chain Audit Report — {{PROJECT_NAME}}

**Date:** YYYY-MM-DD
**Build Tool:** {{BUILD_TOOL}}
**Depth:** {shallow|deep}
**Focus:** {focus}

## Summary

| Metric | Value |
|--------|-------|
| Overall Score | {overallScore}/100 |
| Grade | {grade} |
| Total Dependencies | {totalDependencies} |
| Direct Dependencies | {directDependencies} |
| Transitive Dependencies | {transitiveDependencies} |

## Risk Distribution

| Category | Count | Highest Severity |
|----------|-------|-----------------|
| Maintainer Risk | {singleMaintainerCount} | {severity} |
| Typosquatting Suspects | {typosquattingSuspects} | {severity} |
| Phantom Dependencies | {phantomDependencies} | {severity} |
| Stale Dependencies | {staleDependencies} | {severity} |
| EPSS High Risk | {epssHighRisk} | {severity} |
| SLSA Level | {slsaLevel} | {severity} |

## Findings

### [SCA-MAINT-001] {packageName}@{version}
- **Category:** Maintainer Risk
- **Risk Score:** {riskScore}/100
- **Severity:** {severity}
- **Detail:** {detail}
- **Recommendation:** {fixRecommendation}

### [SCA-TYPO-001] {packageName}@{version}
- **Category:** Typosquatting
- **Risk Score:** {riskScore}/100
- **Severity:** CRITICAL
- **Detail:** Name similar to "{popularPackage}" (Levenshtein distance: {distance})
- **Recommendation:** Verify the correct package is being used

## Scoring Methodology

Risk Score = (CVE Severity * 0.40) + (Depth * 0.20)
           + (Maintainer * 0.15) + (License * 0.15)
           + (Popularity * 0.10)

## Grade Scale

| Grade | Score Range | Description |
|-------|------------|-------------|
| A | 90-100 | Excellent supply chain posture |
| B | 80-89 | Good posture with minor risks |
| C | 70-79 | Acceptable but improvement needed |
| D | 60-69 | Below standard, remediation required |
| F | 0-59 | Failing, immediate action required |

## Recommendations

1. **Immediate:** Address CRITICAL typosquatting suspects
2. **Short-term:** Evaluate alternatives for single-maintainer deps
3. **Medium-term:** Declare phantom dependencies explicitly
4. **Long-term:** Upgrade stale dependencies and improve SLSA level
```

## Error Handling

| Scenario | Action |
|----------|--------|
| Registry API unavailable | Skip that capability, continue with others, note in report |
| No manifest file found | Report error and exit |
| No dependencies declared | Report score 100, grade A, zero findings |
| EPSS API unavailable | Skip EPSS scoring, continue with other capabilities |
| AST scan fails for language | Skip phantom detection, note unsupported language |
| Rate limited by registry | Implement exponential backoff, partial results if timeout |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-dependency-audit` | complementary | Handles CVEs, outdated versions, licenses, and SBOM generation |
| `x-security-dashboard` | reads | Dashboard aggregates results from this skill |
| `x-ci-generate` | called-by | Security pipeline may invoke supply chain audit |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| security | `knowledge/security/sarif-template.md` | SARIF 2.1.0 output format |
| security | `knowledge/security/security-scoring.md` | Scoring model and grade scale |
| security | `knowledge/security/supply-chain-hardening.md` | SLSA framework and hardening patterns |
| security | `knowledge/security/sbom-generation-guide.md` | SBOM format reference |
