# Task Plan -- TASK-0034-0003-002

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0003-002 |
| Story ID | story-0034-0003 |
| Epic ID | 0034 |
| Source Agent | Architect + Security + Tech Lead (consolidated) |
| Type | implementation (edit) |
| TDD Phase | GREEN |
| Layer | domain + adapter.inbound + util + adapter.test |
| Estimated Effort | S |
| Date | 2026-04-10 |

## Objective

Remove `AssemblerTarget.CODEX_AGENTS` from the enum, reducing `AssemblerTarget` to its final single-value state (`CLAUDE` only). Clean up residual `.agents/` references in `FileCategorizer`, `OverwriteDetector`, and `AssemblerTargetTest`. Do NOT touch `PlatformFilter.java` — it belongs to story-0034-0004 (higienization).

## Implementation Guide

1. **Edit `AssemblerTarget.java`:**
   - Remove the `CODEX_AGENTS(".agents")` enum constant declaration.
   - Confirm only `CLAUDE(".claude")` remains.
   - Class should stay well under 250 lines.
2. **Edit `FileCategorizer.java`:**
   - Grep for `".agents"` or `.agents/` literals. If any remain, remove those branches.
   - If already absent (possibly cleaned by story-0002), this is a no-op — document in commit body.
3. **Edit `OverwriteDetector.java`:**
   - Remove `".agents"` from the `ARTIFACT_DIRS` set (or equivalent collection).
   - If already absent, no-op — document in commit body.
4. **Edit `AssemblerTargetTest.java`:**
   - Remove all assertions, parametrized test cases, and imports referencing `CODEX_AGENTS`.
   - If test cases were parametrized over all enum values, verify the parametrized data source now yields exactly 1 element and the test still runs.
   - Ensure test class stays passing.
5. **Verify `PlatformFilter.java` UNTOUCHED:**
   - `git diff --name-only` must NOT include `PlatformFilter.java`.
   - If the file appears in diff accidentally, revert it.
6. **Grep sanity:** `grep -rn "CODEX_AGENTS" java/src/main` → 0 matches.
7. **Compile and test:** `mvn compile` → green; `mvn test-compile` → green; `mvn test` → green.
8. **Commit:** `refactor(cli)!: remove AssemblerTarget.CODEX_AGENTS and .agents/ categorization` with BREAKING CHANGE footer.

## Definition of Done

- [ ] `AssemblerTarget.CODEX_AGENTS(".agents")` constant removed
- [ ] `AssemblerTarget.values().length == 1` (only `CLAUDE` remains)
- [ ] `FileCategorizer.java` — `.agents/` branches removed (or documented no-op)
- [ ] `OverwriteDetector.ARTIFACT_DIRS` — `".agents"` entry removed (or documented no-op)
- [ ] `AssemblerTargetTest.java` — all `CODEX_AGENTS` references removed
- [ ] [TL-007] `PlatformFilter.java` NOT in `git diff --name-only` (scope boundary enforced)
- [ ] [SEC-002/CWE-209] no exception messages expose internal paths or class names
- [ ] [TL-003] `grep -rn "CODEX_AGENTS" java/src/main` returns 0
- [ ] `AssemblerTarget.java` class length ≤ 250 lines (trivially satisfied)
- [ ] `mvn compile` green
- [ ] `mvn test-compile` green
- [ ] `mvn test` green
- [ ] [TL-006] Conventional commit: `refactor(cli)!: remove AssemblerTarget.CODEX_AGENTS and .agents/ categorization` with BREAKING CHANGE footer

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-0034-0003-001 | Main classes + tests must be deleted first; otherwise compile fails because `AgentsAssembler` would still reference a deleted enum or vice versa |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| `AssemblerTargetTest` uses parametrized tests over enum values and breaks with a single-element source | MEDIUM | Test failure after edit | Run test immediately after edit; adjust parametrization if needed |
| Developer accidentally edits `PlatformFilter.java` while cleaning `.agents/` references | MEDIUM | Scope boundary violation | Explicit `git diff --name-only` check pre-commit; list of permitted files enforced by reviewer |
| `FileCategorizer` branch removal causes golden display regression | LOW | Cosmetic CLI regression | Run smoke test (CLI with `--platform claude-code`) to verify display unchanged |
