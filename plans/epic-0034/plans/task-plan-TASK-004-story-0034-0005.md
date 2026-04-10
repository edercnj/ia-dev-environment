# Task Plan -- TASK-0034-0005-004

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0005-004 |
| Story ID | story-0034-0005 |
| Epic ID | 0034 |
| Source Agent | merged(QA, Architect, Security, TechLead, ProductOwner) |
| Type | quality-gate + validation |
| TDD Phase | VERIFY |
| TPP Level | iteration |
| Layer | cross-cutting |
| Estimated Effort | M |
| Date | 2026-04-10 |

## Objective

Execute the full end-to-end verification of EPIC-0034 as documented in story §3.3. Confirm build green, coverage maintained, all 6 grep sanity checks clean, CLI rejects removed platforms, CLI succeeds for claude-code (default and explicit), file count targets met, and both RULE-003 (`.github/workflows/`) and RULE-004 (`resources/shared/templates/`) invariants hold. This task produces no commits - it is verification only, gating TASK-005.

## Implementation Guide

### Step 1 - Full build and test

```bash
cd /Users/edercnj/workspaces/ia-dev-environment
time mvn -f java/pom.xml clean verify 2>&1 | tee /tmp/epic-0034-final-build.log
```

Verify from the log:

- `BUILD SUCCESS`
- Final line: `BUILD SUCCESS`
- Test summary: 0 failures, 0 errors, 0 skipped (test count should be >= the baseline count minus tests deleted by stories 0001-0004, which is approximately 837 - ~34 = ~803 tests remaining, though some new tests may have been added)
- Build time recorded: expected <= 6:01 (baseline) with likely reduction due to fewer golden comparisons

### Step 2 - Extract JaCoCo coverage numbers

```bash
# JaCoCo HTML report is at:
ls java/target/site/jacoco/index.html

# Extract line and branch coverage from the XML report:
cat java/target/site/jacoco/jacoco.xml \
  | grep -E 'counter type="(LINE|BRANCH)"' \
  | head -20
```

Or use a parser:

```bash
# Quick extraction (adjust for actual JaCoCo XML schema):
python3 -c '
import xml.etree.ElementTree as ET
tree = ET.parse("java/target/site/jacoco/jacoco.xml")
root = tree.getroot()
# JaCoCo XML has <counter type="LINE" missed="X" covered="Y"/> at <report> level
for counter in root.findall("counter"):
    t = counter.get("type")
    missed = int(counter.get("missed"))
    covered = int(counter.get("covered"))
    total = missed + covered
    pct = 100.0 * covered / total if total else 0
    print(f"{t}: {covered}/{total} = {pct:.2f}%")
'
```

Assert:

- Line coverage >= 95.00% AND >= 93.69% (baseline 95.69% - 2pp per RULE-002)
- Branch coverage >= 90.00% AND >= 88.69% (baseline 90.69% - 2pp)

If either threshold fails, TASK-004 is blocked. Root-cause and reopen earlier tasks.

### Step 3 - 6 grep sanity checks (QA-003)

Run each check and record the result:

```bash
# Check 1: assembler class references
grep -rn 'GithubInstructionsAssembler\|CodexConfigAssembler\|AgentsAssembler' \
  java/src/main/java
echo "Check 1 exit: $?"
# Expected: exit 1 (no matches)

# Check 2: utility and flag references
grep -rn 'ReadmeGithubCounter\|hasCopilot\|hasCodex' \
  java/src/main
echo "Check 2 exit: $?"
# Expected: exit 1

# Check 3: target path references
grep -rn '\.codex/\|\.agents/' \
  java/src/main
echo "Check 3 exit: $?"
# Expected: exit 1

# Check 4: Platform.java enum
grep -rn 'COPILOT\|CODEX\|CODEX_AGENTS' \
  java/src/main/java/dev/iadev/domain/model/Platform.java
echo "Check 4 exit: $?"
# Expected: exit 1

# Check 5: AssemblerTarget.java enum
grep -rn 'COPILOT\|CODEX\|CODEX_AGENTS' \
  java/src/main/java/dev/iadev/application/assembler/AssemblerTarget.java
echo "Check 5 exit: $?"
# Expected: exit 1

# Check 6: PlatformConverter accepted values
grep -A5 'ACCEPTED_VALUES' \
  java/src/main/java/dev/iadev/cli/PlatformConverter.java
# Expected: if ACCEPTED_VALUES exists, shows only "claude-code"
# Per escalation note, ACCEPTED_VALUES may be dynamic (no literal).
# In that case, verify via runtime CLI smoke test in Step 4.
```

ALL 6 checks must return exit code 1 (no matches) or produce output containing only `claude-code`.

### Step 4 - CLI smoke tests (QA-004, QA-005, SEC-002)

First, confirm the jar is built:

```bash
ls -la java/target/*.jar
```

Test rejected platforms:

```bash
java -jar java/target/ia-dev-env-*.jar generate --platform copilot 2>&1 > /tmp/cli-reject-copilot.log
echo "copilot exit: $?"
# Expected: non-zero exit
grep -i 'invalid platform\|unknown\|not supported' /tmp/cli-reject-copilot.log
# Expected: match found

# SEC-002 / CWE-209: stderr must not expose internal details
grep -E 'Exception|at dev\.iadev|at java\.|\.java:[0-9]+' /tmp/cli-reject-copilot.log
# Expected: 0 matches (no stack trace leak)

java -jar java/target/ia-dev-env-*.jar generate --platform codex 2>&1 > /tmp/cli-reject-codex.log
echo "codex exit: $?"
# Expected: non-zero exit

java -jar java/target/ia-dev-env-*.jar generate --platform agents 2>&1 > /tmp/cli-reject-agents.log
echo "agents exit: $?"
# Expected: non-zero exit
```

Test accepted platforms:

```bash
rm -rf /tmp/gen-test
java -jar java/target/ia-dev-env-*.jar generate \
  --platform claude-code \
  --profile java-spring \
  --output /tmp/gen-test 2>&1 | tee /tmp/cli-accept-explicit.log
echo "claude-code exit: $?"
# Expected: 0

find /tmp/gen-test -type f | wc -l
# Expected: in [788, 872] (830 +/- 5%)

# Default flag
rm -rf /tmp/gen-default
java -jar java/target/ia-dev-env-*.jar generate \
  --profile java-spring \
  --output /tmp/gen-default 2>&1 | tee /tmp/cli-accept-default.log
echo "default exit: $?"
# Expected: 0

find /tmp/gen-default -type f | wc -l
# Expected: in [788, 872]
```

### Step 5 - RULE-003 workflow preservation check (QA-006)

```bash
find java/src/test/resources/golden -path '*/.github/workflows*' -type f | wc -l
# Expected: 95 (exact match to baseline)

# No .yml or .yaml file under workflows/ was deleted
find java/src/test/resources/golden -path '*/.github/workflows*' \
  -type f \
  \( -name '*.yml' -o -name '*.yaml' \) \
  | wc -l
# Expected: 95 (or equal to baseline counted in the same way)
```

### Step 6 - RULE-004 shared templates check (ARCH-002)

```bash
git diff origin/main -- java/src/main/resources/shared/templates/
# Expected: empty output (no diff)

find java/src/main/resources/shared/templates -type f | wc -l
# Expected: 57 (exact match to baseline)
```

### Step 7 - Golden total count (QA-008)

```bash
find java/src/test/resources/golden -type f | wc -l
# Expected: in [5801, 6413] (6107 +/- 5%)
# Baseline: 14285
# Reduction: ~8178 files
```

### Step 8 - Map each Gherkin scenario to evidence (PO-002)

Create a verification matrix in the run log:

| # | Gherkin Scenario | Evidence | Status |
|---|------------------|----------|--------|
| 1 | Build verde no estado final | Step 1 BUILD SUCCESS + Step 2 coverage | PASS/FAIL |
| 2 | Grep sanity checks limpos | Step 3 all 6 checks | PASS/FAIL |
| 3 | CLI smoke test -- rejects removed platforms | Step 4 reject tests | PASS/FAIL |
| 4 | CLI smoke test -- claude-code funciona | Step 4 accept tests + file count | PASS/FAIL |
| 5 | RULE-004 templates shared intactos | Step 6 | PASS/FAIL |
| 6 | RULE-003 workflows preservados | Step 5 | PASS/FAIL |
| 7 | CLAUDE.md atualizado sem resíduos | TASK-001 DoD (re-validate grep here) | PASS/FAIL |
| 8 | expected-artifacts.json regenerado | TASK-003 DoD (re-validate grep + smoke here) | PASS/FAIL |
| 9 | PR final mergeable em develop | Deferred to TASK-005 |

All scenarios 1-8 must be PASS. Scenario 9 is validated in TASK-005.

### Step 9 - Archive evidence and report

```bash
mkdir -p plans/epic-0034/reports/task-005-004
mv /tmp/epic-0034-final-build.log \
   /tmp/cli-reject-copilot.log \
   /tmp/cli-reject-codex.log \
   /tmp/cli-reject-agents.log \
   /tmp/cli-accept-explicit.log \
   /tmp/cli-accept-default.log \
   plans/epic-0034/reports/task-005-004/

cp java/target/site/jacoco/jacoco.xml \
   plans/epic-0034/reports/task-005-004/jacoco-final.xml
```

Write a verification report at `plans/epic-0034/reports/task-005-004/verification-report.md` capturing the Step 8 matrix + key metrics.

### Step 10 - No commit

This task produces no git commit. Its output is the verification report + archived evidence. TASK-005 picks up with PR creation.

## Definition of Done

- [ ] `mvn -f java/pom.xml clean verify` exits 0 with BUILD SUCCESS
- [ ] 0 test failures, 0 errors, 0 skipped
- [ ] JaCoCo line coverage >= 95.00% absolute AND >= 93.69% (RULE-002)
- [ ] JaCoCo branch coverage >= 90.00% absolute AND >= 88.69% (RULE-002)
- [ ] Build time <= 6:01 min baseline
- [ ] All 6 grep sanity checks return zero matches
- [ ] CLI `--platform copilot` rejected with non-zero exit and clear error
- [ ] CLI `--platform codex` rejected equivalently
- [ ] CLI `--platform agents` rejected equivalently
- [ ] CLI error messages contain NO stack traces, class names, or file paths (CWE-209)
- [ ] CLI `--platform claude-code --profile java-spring` exits 0 with ~830 files
- [ ] CLI default (no flag) exits 0 with ~830 files
- [ ] `.github/workflows/` golden file count == 95 (RULE-003)
- [ ] `resources/shared/templates/` file count == 57 (RULE-004)
- [ ] `git diff origin/main -- java/src/main/resources/shared/templates/` empty
- [ ] Golden total file count in [5801, 6413]
- [ ] All 8 of 9 Gherkin scenarios mapped to concrete evidence PASS (scenario 9 deferred to TASK-005)
- [ ] Verification report written to `plans/epic-0034/reports/task-005-004/`
- [ ] No commit (verification-only task)

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-0034-0005-003 | Manifest and golden files must be regenerated before running `mvn clean verify` (otherwise tests fail on stale expectations) |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Coverage degradation beyond 2pp threshold | Medium | High | If detected in Step 2, fail fast. Root cause by running `mvn -f java/pom.xml test` with detailed JaCoCo report and inspecting the class-level coverage deltas. Likely cause: deleted tests covered code that was not proportionally deleted. Re-open earlier story tasks if so. |
| `PlatformConverter.ACCEPTED_VALUES` grep returns empty (field is dynamic) | Medium | Low | Escalation note in tasks breakdown covers this. Use runtime CLI smoke test (Step 4) as the effective check. |
| CLI fails with runtime exception showing stack trace (CWE-209 breach) | Low | High | Step 4 grep for stack trace markers. If detected, Security Engineer escalation. |
| Default `--profile` value may differ from `java-spring` | Low | Medium | Explicitly specify `--profile java-spring` in Step 4 default test. If the CLI does not have a default profile, add the flag. |
| `.github/workflows/` file count drifts from 95 | Low | Critical | Step 5 is the RULE-003 gate. Any deviation requires halting and rolling back the regeneration. |
| Jar filename pattern `ia-dev-env-*.jar` does not match actual artifact | Low | Low | Use `find java/target -maxdepth 1 -name '*.jar' -not -name '*-tests.jar' -not -name '*-sources.jar'` to locate the production jar. |
