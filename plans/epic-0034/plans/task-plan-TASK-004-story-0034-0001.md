# Task Plan -- TASK-0034-0001-004

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0001-004 |
| Story ID | story-0034-0001 |
| Epic ID | 0034 |
| Source Agent | Architect + Security (merged) |
| Type | config (deletion) |
| TDD Phase | GREEN |
| Layer | adapter.outbound (resources) |
| Estimated Effort | S |
| Date | 2026-04-10 |

## Objective

Delete the `targets/github-copilot/` resource directory (~131 files) from the generator source tree. After TASK-003 removed all compile-time references, these resources are orphaned and cannot be loaded by any production code path. This task is mechanically trivial but security-sensitive (symlink safety).

## Implementation Guide

1. **Pre-delete safety check (SEC-003/CWE-22):** Run `find java/src/main/resources/targets/github-copilot/ -type l` to detect any symlinks that could escape the base directory during recursive delete. Expected: empty output. If non-empty, abort and review each symlink manually.
2. **Pre-delete file count audit:** `find java/src/main/resources/targets/github-copilot/ -type f | wc -l` — expected ~131 per baseline. Record in commit body.
3. Delete the directory recursively:
   ```bash
   rm -rf java/src/main/resources/targets/github-copilot/
   ```
4. **Post-delete credential grep (SEC-004):** `grep -rE 'copilot.*api|copilot.*token|copilot.*key' java/src/main` — expected: 0 matches.
5. Run `mvn clean compile test` from `java/`. Expected: BUILD SUCCESS with all tests passing. If any remaining test loads resources from `targets/github-copilot/`, it will fail here — document and fix in this same task (atomic).
6. Commit as a single atomic commit.

## Definition of Done

- [ ] `java/src/main/resources/targets/github-copilot/` directory no longer exists
- [ ] [SEC-003/CWE-22] Pre-delete symlink check performed and clean (empty result recorded in commit body)
- [ ] Pre-delete file count recorded (~131)
- [ ] [SEC-004] Post-delete grep `grep -rE 'copilot.*api|copilot.*token|copilot.*key' java/src/main` returns 0 matches
- [ ] `mvn clean compile` green
- [ ] `mvn test` green
- [ ] Commit message: `chore(resources)!: delete github-copilot target directory`
- [ ] Commit body records: pre-count, post-count (0), symlink check result

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-0034-0001-003 | Enum + CLI hygiene must complete first so no code attempts to `ResourceResolver.resolveResourcesRoot("targets/github-copilot/...")` during tests |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Symlinks inside `targets/github-copilot/` pointing outside the base dir | Very Low | High | Pre-delete `find -type l` check is mandatory. Abort on any finding. |
| A smoke test or resource loader still references `targets/github-copilot/` path | Medium | Medium | `mvn test` will catch it. Fix atomically in this task or revert. |
| Generator produces `.github/` output for `claude-code` platform because of a shared resource loader | Very Low | Medium | Verified: `ResourceResolver` calls in `AssemblerFactory.resolveConstitutionResources()` only load from `shared/templates/`, not from `targets/github-copilot/`. No coupling. |
