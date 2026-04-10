# Task Plan -- TASK-0034-0002-006

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0002-006 |
| Story ID | story-0034-0002 |
| Epic ID | 0034 |
| Source Agent | QA Engineer + Security + Tech Lead + Product Owner (merged) |
| Type | quality-gate + validation |
| TDD Phase | VERIFY |
| Layer | config + test (final gate) |
| Estimated Effort | S |
| Date | 2026-04-10 |

## Objective

Final verification gate for story-0034-0002. Cleans up the 18 shared YAML config templates (`setup-config.*.yaml`), runs the full build-verify cycle with coverage enforcement, executes the 6 acceptance test scenarios from story §7, regenerates `expected-artifacts.json` if needed, and produces the PR. This is the task where RULE-001 (build always green) and RULE-002 (coverage floor) are finally enforced as hard gates.

## Implementation Guide

### Edit 1: 18 setup-config.*.yaml files

18 YAML files in `java/src/main/resources/shared/config-templates/` currently contain `codex` references (confirmed via grep baseline: 18 files, 1 match each):

```
setup-config.go-gin.yaml
setup-config.java-picocli-cli.yaml
setup-config.java-quarkus.yaml
setup-config.java-spring-clickhouse.yaml
setup-config.java-spring-cqrs-es.yaml
setup-config.java-spring-elasticsearch.yaml
setup-config.java-spring-event-driven.yaml
setup-config.java-spring-fintech-pci.yaml
setup-config.java-spring-hexagonal.yaml
setup-config.java-spring-neo4j.yaml
setup-config.java-spring.yaml
setup-config.kotlin-ktor.yaml
setup-config.python-click-cli.yaml
setup-config.python-fastapi-timescale.yaml
setup-config.python-fastapi.yaml
setup-config.rust-axum.yaml
setup-config.typescript-commander-cli.yaml
setup-config.typescript-nestjs.yaml
```

For each file, remove `codex` references in the platform options. Typical edit:

```yaml
# Before
platform:
  # Options: claude-code, codex, all   # (post-story-0001 — copilot already removed)
  value: "claude-code"

# After
platform:
  # Options: claude-code, all
  value: "claude-code"
```

Or, if `codex` appears as a valid enumerated value in a multi-select, remove that entry from the list.

Post-edit grep: `grep -l 'codex' java/src/main/resources/shared/config-templates/setup-config.*.yaml` returns 0 files.

### Edit 2 (conditional): Regenerate `expected-artifacts.json`

If `AssemblerRegressionSmokeTest` was deferred-failing in TASK-003, 004, or 005, regenerate the manifest now:

```bash
cd java
mvn compile test-compile
mvn test -Dtest=ExpectedArtifactsGenerator
# OR run the generator main directly per README §"Regenerating Golden Files"
```

Verify: `git diff java/src/test/resources/expected-artifacts.json` shows only removals of Codex-related entries.

### Full verification

```bash
cd java

# Clean verify with coverage
mvn clean verify

# JaCoCo coverage check
cat target/site/jacoco/jacoco.csv | awk -F, '{line+=$8+$9; covered+=$9; branch_total+=$6+$7; branch_covered+=$7} END {printf "Line: %.2f%% Branch: %.2f%%\n", covered*100/line, branch_covered*100/branch_total}'
```

Expected:
- BUILD SUCCESS
- Line coverage ≥ 95% (target; baseline 95.69%, degradation ≤ 2pp per RULE-002)
- Branch coverage ≥ 90% (target; baseline 90.69%, degradation ≤ 2pp per RULE-002)

### Smoke tests (acceptance tests from story §7)

```bash
# AT-1: build green (already validated by mvn clean verify above)

# AT-2: CLI rejects --platform codex
java -jar java/target/ia-dev-env.jar generate --platform codex 2>&1 | tee /tmp/at2.log
# Expected: exit code != 0; stderr contains "Invalid platform"; stderr does NOT contain "codex" in accepted list
echo "Exit: $?"

# AT-3: CLI works for claude-code
rm -rf /tmp/test-out && java -jar java/target/ia-dev-env.jar generate --platform claude-code --output-dir /tmp/test-out
test -d /tmp/test-out/.claude && echo "OK"
test ! -d /tmp/test-out/.codex && echo "OK (no .codex)"

# AT-4: golden files .codex/ fully removed
find java/src/test/resources/golden -type d -name '.codex' | wc -l  # Expected: 0
find java/src/test/resources/golden -name 'config.toml' | wc -l      # Expected: 0

# AT-5: grep sanity
grep -rn 'CodexConfigAssembler\|CodexSkillsAssembler\|CodexRequirementsAssembler\|CodexOverrideAssembler\|CodexAgentsMdAssembler\|CodexScanner\|CodexShared' java/src/main/java  # Expected: 0
grep -rn 'Platform\.CODEX' java/src/main  # Expected: 0
grep -rnE 'AssemblerTarget\.CODEX\b' java/src/main  # Expected: 0 (word boundary preserves CODEX_AGENTS)

# AT-6: Degenerate — shared classes don't break (PlatformDirectorySmokeTest + CliModesSmokeTest pass)
mvn test -Dtest='PlatformDirectorySmokeTest,CliModesSmokeTest'

# PO-003 cross-story: --platform agents still accepted (agents removal is story 0003)
java -jar java/target/ia-dev-env.jar generate --platform agents --output-dir /tmp/test-out-agents 2>&1
echo "Agents exit: $?"  # Expected: 0 (still valid until story 0003)
```

### Commit

```
chore(cleanup)!: purge codex references from shared yamls

Strip codex from the 18 shared config-template YAMLs. Regenerate
expected-artifacts.json manifest if required by
AssemblerRegressionSmokeTest.

Closes RULE-001 (build green) and RULE-002 (coverage floor) for
story-0034-0002.

Final state:
- 7 Codex classes deleted
- 6 Codex test classes deleted
- 15 resource files deleted (targets/codex/)
- 2944 golden files deleted (.codex/ in 17 profiles)
- 18 YAMLs cleaned
- 17 dependent test files adjusted
- Platform.CODEX + AssemblerTarget.CODEX removed
- AssemblerFactory.buildAllAssemblers() returns 19 descriptors

Refs: EPIC-0034, story-0034-0002
```

### PR creation

Create the PR with the following body structure:

```markdown
## story-0034-0002 — Remove Codex Target Support

### Summary
Second atomic removal in EPIC-0034. Deletes GitHub Codex target support
end-to-end: 7 assembler classes, 6 test classes, 15 resource files, 2944
golden files, 17 dependent test adjustments, 18 YAML cleanups.

### Metrics (Before → After)

| Metric | Before | After | Delta |
|--------|--------|-------|-------|
| `buildAllAssemblers()` descriptors | 26 | 19 | -7 |
| Codex Java classes | 7 | 0 | -7 |
| Codex test classes | 6 | 0 | -6 |
| `targets/codex/` resources | 15 | 0 | -15 |
| Golden `.codex/` files | 2944 | 0 | -2944 |
| `Platform` enum values | `{CLAUDE_CODE, CODEX, SHARED}` (post-0001) | `{CLAUDE_CODE, SHARED}` | -1 |
| `AssemblerTarget` enum values | `{ROOT, CLAUDE, CODEX, CODEX_AGENTS}` (post-0001) | `{ROOT, CLAUDE, CODEX_AGENTS}` | -1 |

### Acceptance Tests
- [x] AT-1: `mvn clean verify` green
- [x] AT-2: `--platform codex` rejected
- [x] AT-3: `--platform claude-code` works
- [x] AT-4: `.codex/` golden files fully removed
- [x] AT-5: grep sanity returns 0 matches
- [x] AT-6: shared classes don't break (PlatformDirectorySmokeTest + CliModesSmokeTest pass)

### Coverage (JaCoCo)
- Line: XX.XX% (baseline 95.69%, delta ≤ 2pp per RULE-002)
- Branch: XX.XX% (baseline 90.69%, delta ≤ 2pp per RULE-002)

### Commits
6 atomic commits following Conventional Commits. TASK-003 carries
BREAKING CHANGE footer for CLI --platform contract.

### Cross-story note
- `--platform agents` is STILL accepted (agents removal is story 0034-0003).
- `.agents/` golden files remain in 17 profiles (story 0034-0003 scope).
- `AssemblerTarget.CODEX_AGENTS` still exists (story 0034-0003 scope).
```

## Definition of Done

- [ ] 18 YAMLs cleaned: `grep -l 'codex' java/src/main/resources/shared/config-templates/setup-config.*.yaml` returns 0 files
- [ ] [SEC-001] `grep -rE '(password\|secret\|token\|api_?key)' java/src/main/resources/shared/config-templates/setup-config.*.yaml` returns 0 matches or only allowlisted examples
- [ ] `expected-artifacts.json` regenerated if TASK-003/004/005 deferred the regeneration; git diff shows only Codex-related removals
- [ ] [QA-006/RULE-002/TL-004] `mvn clean verify` green with JaCoCo line ≥ 95% AND branch ≥ 90%; degradation ≤ 2pp vs baseline (95.69% / 90.69%)
- [ ] [AT-1] Build green validated by `mvn clean verify`
- [ ] [AT-2] CLI smoke: `--platform codex` exits non-zero with "Invalid platform" stderr
- [ ] [AT-3] CLI smoke: `--platform claude-code` produces `.claude/` but not `.codex/`
- [ ] [AT-4] `find golden -type d -name .codex` returns 0; `find golden -name config.toml` returns 0
- [ ] [AT-5] All 7 Codex class names + `Platform.CODEX` + `AssemblerTarget.CODEX\b` return 0 grep matches in `java/src/main`
- [ ] [AT-6] `PlatformDirectorySmokeTest` and `CliModesSmokeTest` pass
- [ ] [PO-003] CLI smoke: `--platform agents` STILL accepted (exit code 0) — cross-story invariant
- [ ] [TL-005] All 6 commits on branch follow Conventional Commits with target scope and `!` on the breaking commit (TASK-003)
- [ ] [TL-007] PR created with story §3.5 metrics table in body, JaCoCo report link, coverage deltas, and acceptance test checklist
- [ ] Commit follows Conventional Commits with `chore(cleanup)!:` prefix

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-0034-0002-004 | Resources must be deleted before final verify |
| TASK-0034-0002-005 | Golden files must be deleted before final verify |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Coverage degradation > 2pp | Low | HIGH | Hard gate via `mvn verify`; if fails, investigate whether deleted tests covered shared utilities; add targeted tests or halt PR |
| `expected-artifacts.json` regeneration picks up unintended drift | Medium | Medium | Strict review of `git diff expected-artifacts.json`; only Codex-related entries may be removed |
| YAML edit introduces YAML syntax error | Low | Medium | `yq` or `python -c 'import yaml; yaml.safe_load(open(f))'` post-edit validation of all 18 files |
| Smoke test runs leave temp dirs in `/tmp` that fail next CI run | Low | Low | Use unique temp dirs per run; clean up in task body |
| CLI smoke for `--platform agents` fails because something else changed | Very Low | Medium | Cross-story invariant check. If fails, investigate immediately and halt PR — agents functionality must remain intact until story 0003 |
| Manifest regeneration runs out of order with golden deletion | Very Low | Medium | Regenerate manifest AFTER all golden deletions have landed in git working tree, not before |
