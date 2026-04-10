# Task Plan -- TASK-0034-0005-003

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0005-003 |
| Story ID | story-0034-0005 |
| Epic ID | 0034 |
| Source Agent | merged(Architect, QA, Security) |
| Type | migration |
| TDD Phase | VERIFY |
| TPP Level | boundary |
| Layer | adapter.test + config |
| Estimated Effort | S |
| Date | 2026-04-10 |

## Objective

Regenerate `java/src/test/resources/smoke/expected-artifacts.json` to reflect the claude-only output (~830 entries per profile vs. ~9500 baseline). Also regenerate golden files via `GoldenFileRegenerator` if not already in sync. Verify the manifest contains zero references to `.codex/`, `.agents/`, or `.github/` (except `.github/workflows/` if applicable). Ensure all smoke tests pass against the regenerated manifest.

## Implementation Guide

### Step 1 - Locate and read the canonical regeneration procedure

**CRITICAL:** Per `MEMORY.md`, the canonical regen command is buried in `README.md` around line 820. Read it FIRST:

```bash
sed -n '800,900p' /Users/edercnj/workspaces/ia-dev-environment/README.md \
  | grep -A 20 -i 'regenerat'
```

Copy the exact command into the implementation run log. Do NOT improvise.

The expected flow is approximately:

1. `mvn -f java/pom.xml process-resources` (CRITICAL - skipping this causes stale output per MEMORY.md)
2. Run `GoldenFileRegenerator` (rebuilds golden profiles from source resources)
3. Run `ExpectedArtifactsGenerator` (rebuilds `expected-artifacts.json` manifest from golden files)
4. Run `mvn -f java/pom.xml test` to validate

### Step 2 - Verify tool identity (ARCH-001)

```bash
find /Users/edercnj/workspaces/ia-dev-environment/java/src/main/java \
  -name 'ExpectedArtifactsGenerator*.java' \
  -type f

find /Users/edercnj/workspaces/ia-dev-environment/java/src/main/java \
  -name 'GoldenFileRegenerator*.java' \
  -type f
```

Expected locations (per `baseline-pre-epic.md`):

- `dev.iadev.golden.GoldenFileRegenerator`
- `dev.iadev.smoke.ExpectedArtifactsGenerator`

Confirm main method signature, argument conventions, and output path (should be hardcoded, not user-controllable - SEC-003 CWE-22).

### Step 3 - Execute process-resources

```bash
cd /Users/edercnj/workspaces/ia-dev-environment
mvn -f java/pom.xml process-resources
```

Must complete successfully. Check exit code = 0.

### Step 4 - Run GoldenFileRegenerator

Per the canonical command from Step 1, run the regenerator. Example (verify against README.md):

```bash
mvn -f java/pom.xml exec:java \
  -Dexec.mainClass=dev.iadev.golden.GoldenFileRegenerator \
  -Dexec.classpathScope=test
```

Or if there is a dedicated Maven profile / script, use that. Verify from README.md §"Regenerating Golden Files".

After regeneration:

```bash
find java/src/test/resources/golden -type f | wc -l
# Expected: in [5801, 6413] (6107 +/- 5%)
# Baseline: 14285
# Reduction target: ~8178 files

find java/src/test/resources/golden -path '*/.github/workflows*' -type f | wc -l
# Expected: 95 (unchanged - RULE-003)
```

### Step 5 - Run ExpectedArtifactsGenerator

Per canonical command:

```bash
mvn -f java/pom.xml exec:java \
  -Dexec.mainClass=dev.iadev.smoke.ExpectedArtifactsGenerator \
  -Dexec.classpathScope=test
```

(Actual command per README.md.)

After regeneration:

```bash
# File is updated
stat java/src/test/resources/smoke/expected-artifacts.json

# Validate structural expectations
# Using jq (if available) or grep:
jq 'keys' java/src/test/resources/smoke/expected-artifacts.json 2>/dev/null \
  || head -100 java/src/test/resources/smoke/expected-artifacts.json

# Entry count per profile (expected ~830 for java-spring)
# The manifest structure may be {profileName: [file, ...]} or similar.
# Query depends on schema; verify by reading one sample entry.
```

### Step 6 - Residual reference check (QA-007)

```bash
grep -nE '\.agents|\.codex|"\.github/' \
  java/src/test/resources/smoke/expected-artifacts.json \
  | grep -v '\.github/workflows/'
# Expected: 0 matches
```

If matches are found, the regeneration did not pick up the deleted targets. Re-run `mvn process-resources` and regenerate.

### Step 7 - Path traversal check (SEC-003, CWE-22)

```bash
# Confirm the generator hardcodes its output path
grep -n 'expected-artifacts\.json\|java/src/test/resources/smoke' \
  java/src/main/java/dev/iadev/smoke/ExpectedArtifactsGenerator.java
```

Expected: the output path is a compile-time constant (String literal or Path.of(...) with no user-controlled fragment). If the path is derived from CLI args or environment variables, flag as CWE-22 risk and escalate.

### Step 8 - Run smoke tests (QA-007)

```bash
mvn -f java/pom.xml test \
  -Dtest='PlatformDirectorySmokeTest,AssemblerRegressionSmokeTest,CliModesSmokeTest'
```

Expected: all 3 tests green. Any failure blocks TASK-004.

### Step 9 - Commit

```
test(smoke): regenerate expected-artifacts.json for claude-only output

Regenerate golden files and expected-artifacts.json manifest to reflect
the claude-only generator output (~830 entries per profile, down from
~9500). Preserves .github/workflows/ fixtures (RULE-003). Preserves
resources/shared/templates/ (RULE-004).

Smoke tests validated:
- PlatformDirectorySmokeTest
- AssemblerRegressionSmokeTest
- CliModesSmokeTest

Ref: EPIC-0034, TASK-0034-0005-003
```

## Definition of Done

- [ ] Canonical regeneration procedure read from README.md before executing (no improvisation)
- [ ] `mvn process-resources` executed before any regenerator (MEMORY.md enforcement)
- [ ] `GoldenFileRegenerator` executed successfully
- [ ] `ExpectedArtifactsGenerator` executed successfully
- [ ] Output path for `ExpectedArtifactsGenerator` is hardcoded (SEC-003, CWE-22)
- [ ] Manifest contains ~830 entries per profile (+/- 5%) for `java-spring`
- [ ] `grep -nE '\.agents|\.codex|"\.github/' expected-artifacts.json` returns 0 (except workflows)
- [ ] Golden total file count: `find java/src/test/resources/golden -type f | wc -l` in [5801, 6413]
- [ ] `.github/workflows/` golden count == 95 (RULE-003)
- [ ] `resources/shared/templates/` golden count == 57 (RULE-004) - verified via separate check in TASK-004
- [ ] `PlatformDirectorySmokeTest` passes
- [ ] `AssemblerRegressionSmokeTest` passes
- [ ] `CliModesSmokeTest` passes
- [ ] Conventional Commit message
- [ ] Pre-commit hooks pass

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-0034-0005-002 | Documentation must be updated before regeneration so the regenerator picks up the new source content (if CLAUDE.md / rules are part of the source tree driving output) |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Stale output because `mvn process-resources` was skipped | Medium | High | Step 3 is explicit. MEMORY.md is the authoritative reference. |
| Canonical regen command differs from assumption | Medium | High | Step 1 mandates reading README.md §820 BEFORE executing. Do NOT assume Maven exec:java; the real command may be different. |
| ExpectedArtifactsGenerator output path is user-controllable (CWE-22) | Low | High | Step 7 verifies the path is a compile-time constant. If not, escalate and pause the task. |
| Smoke tests fail against regenerated manifest due to schema drift | Medium | Medium | Step 8 runs the 3 smoke tests. Any failure requires root-cause analysis (likely a target reference lingering in a resource template). |
| Golden regeneration accidentally deletes `.github/workflows/` | Low | Critical | Step 4 post-check explicitly counts workflows files = 95. RULE-003 enforcement. |
| Count is 830 +/- 5% but off for non-java-spring profiles | Low | Low | Only `java-spring` is the reference profile per baseline. Other profiles may differ; story does not mandate a per-profile exact count. |
