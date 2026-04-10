# Task Plan -- TASK-0034-0002-004

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0002-004 |
| Story ID | story-0034-0002 |
| Epic ID | 0034 |
| Source Agent | Architect + Security (merged) |
| Type | config (delete) |
| TDD Phase | GREEN |
| Layer | adapter.outbound |
| Estimated Effort | S |
| Date | 2026-04-10 |

## Objective

Delete the `java/src/main/resources/targets/codex/` directory recursively (15 files including templates for `config.toml.njk`, `requirements.toml.njk`, `AGENTS.md.njk`, and section sub-templates). These resources were consumed exclusively by the 7 Codex assembler classes deleted in TASK-001 and are now orphaned. Pre-delete security checks: verify no symlinks escape the base directory (CWE-22) and no credentials leak in the resource contents (CWE-798). Parallelizable with TASK-005.

## Implementation Guide

### Pre-delete security checks (SEC-003, SEC-004)

```bash
cd java/src/main/resources/targets/codex

# SEC-003 / CWE-22: no symlinks
find . -type l
# Expected: empty output

# SEC-004 / CWE-798: no hard-coded credentials
grep -rniE 'codex.*(api|token|key|secret|password)' .
# Expected: empty OR only allowlisted template placeholders like {{ api_key }}
```

If either check fails, HALT and escalate before deletion.

### Delete

```bash
cd java
rm -rf src/main/resources/targets/codex
```

Expected: 15 files removed (confirmed against baseline count).

### Build & verify

- Run `mvn clean compile test-compile` from `java/`. Expected: BUILD SUCCESS (resources are not on the classpath in a way that breaks compilation).
- Run `mvn test`. Expected: all tests green; `AssemblerRegressionSmokeTest` may fail against stale `expected-artifacts.json` (deferred to TASK-006).
- Grep check: `find java/src/main/resources/targets -type d -name codex` returns empty.
- Grep check: `find java/src/main/resources/targets/codex -type f 2>/dev/null | wc -l` returns 0.

### Commit

```
chore(resources)!: delete codex target directory

Remove java/src/main/resources/targets/codex/ (15 files). These
templates (config.toml.njk, requirements.toml.njk, AGENTS.md.njk,
sections/*.md.njk) were consumed exclusively by the Codex
assembler classes deleted in TASK-0034-0002-001.

Pre-delete security checks (CWE-22, CWE-798) passed.

Refs: EPIC-0034, story-0034-0002, RULE-005
```

## Definition of Done

- [ ] [SEC-003/CWE-22] Pre-delete `find java/src/main/resources/targets/codex -type l` returns empty
- [ ] [SEC-004] Pre-delete `grep -rniE 'codex.*(api|token|key|secret|password)' java/src/main/resources/targets/codex/` returns 0 matches or only allowlisted examples
- [ ] Directory `java/src/main/resources/targets/codex/` deleted recursively
- [ ] 15 files removed (verified by git diff file count)
- [ ] Post-delete `find java/src/main/resources/targets -type d -name codex` returns empty
- [ ] `mvn clean compile test-compile` green
- [ ] `mvn test` green (allow exception for `AssemblerRegressionSmokeTest` if `expected-artifacts.json` not yet regenerated)
- [ ] Commit follows Conventional Commits with `chore(resources)!:` prefix

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-0034-0002-003 | Production code must no longer reference `.codex/` resource paths before the directory can be safely deleted |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Symlinks inside `targets/codex/` escape base dir during rm -rf | Very Low | HIGH | Pre-delete `find -type l` check mandatory |
| Hard-coded API keys or tokens present in templates (accidental commit in a past release) | Low | HIGH | Pre-delete `grep` scan; if positive, halt and escalate to Security before proceeding with deletion |
| A shared assembler secretly reads a template from `targets/codex/` | Very Low | Medium | If build/test fails after deletion, restore and investigate. Shared assemblers should only read from `resources/shared/templates/` (RULE-004). |
| Parallel task TASK-005 touches the same files | None | None | Tasks touch disjoint paths: TASK-004 touches `src/main/resources/targets/codex/`; TASK-005 touches `src/test/resources/golden/*/.codex/`. No overlap. |
