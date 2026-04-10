# Task Plan -- TASK-0034-0003-001

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0003-001 |
| Story ID | story-0034-0003 |
| Epic ID | 0034 |
| Source Agent | Architect + QA + Security + Tech Lead (consolidated) |
| Type | implementation (delete) |
| TDD Phase | GREEN (compile-verified removal) |
| Layer | application.assembler + adapter.test |
| Estimated Effort | M |
| Date | 2026-04-10 |

## Objective

Delete the 2 Agents main classes and their 6 test classes + 1 test fixture in a single atomic commit. Edit `AssemblerFactory.java` to remove any builder method or registration that references `AgentsAssembler` or `AgentsSelection`. Leave `mvn compile` and `mvn test-compile` green.

## Implementation Guide

1. **Pre-check prerequisite state.** Run `grep -rn "GithubAgentsAssembler\|CodexAgentsMdAssembler" java/src/main/java`. If either class file still exists, stories 0034-0001 or 0034-0002 have not been integrated yet — STOP and escalate. This story's baseline assumes both are already deleted.
2. **Pre-check baseline test state.** Confirm baseline build was green with 837 tests passing (`plans/epic-0034/baseline-pre-epic.md` §"Baseline Validation"). Record the expected count of tests for regression comparison: 837 minus the test count inside the 6 Agents* test classes (exact count to be recorded at delete time).
3. **Delete main classes:**
   - `java/src/main/java/dev/iadev/application/assembler/AgentsAssembler.java`
   - `java/src/main/java/dev/iadev/application/assembler/AgentsSelection.java`
4. **Edit `AssemblerFactory.java`:**
   - Grep for any method or line referencing `AgentsAssembler`, `AgentsSelection`, or `CODEX_AGENTS`.
   - Remove the referenced registrations, imports, and any helper builders (e.g., a hypothetical `buildAgentsAssemblers()` method).
   - If no references exist (possible if stories 0001/0002 already cleaned them up in their `AssemblerFactory` edits), document in the commit body: "no-op: AssemblerFactory already clean of Agents* after stories 0001/0002".
   - Verify class length stays ≤ 250 lines.
5. **Delete test classes + fixture:**
   - `AgentsAssemblerTest.java`
   - `AgentsAssemblerCoverageTest.java`
   - `AgentsSelectionTest.java`
   - `AgentsGoldenMatchTest.java`
   - `AgentsConditionalGoldenTest.java`
   - `AgentsCoreAndDevTest.java`
   - `AgentsTestFixtures.java` (fixture)
6. **Verify no orphan imports:** Run `grep -rn "import.*AgentsAssembler\|import.*AgentsSelection\|import.*AgentsTestFixtures" java/src` — expect zero.
7. **Compile and test:** `mvn compile` → green; `mvn test-compile` → green; `mvn test` → green (surviving tests).
8. **Commit:** `refactor(assembler)!: remove AgentsAssembler and AgentsSelection classes` with BREAKING CHANGE footer explaining removal of generic agents target. Include list of deleted files in body.

## Definition of Done

- [ ] [RULE-006/QA-001] Baseline build confirmed green (6 Agents* tests + fixture were passing)
- [ ] `AgentsAssembler.java` deleted
- [ ] `AgentsSelection.java` deleted
- [ ] `AgentsAssemblerTest.java` deleted
- [ ] `AgentsAssemblerCoverageTest.java` deleted
- [ ] `AgentsSelectionTest.java` deleted
- [ ] `AgentsGoldenMatchTest.java` deleted
- [ ] `AgentsConditionalGoldenTest.java` deleted
- [ ] `AgentsCoreAndDevTest.java` deleted
- [ ] `AgentsTestFixtures.java` deleted
- [ ] `AssemblerFactory.java` edited (or documented no-op) — no references to `AgentsAssembler` / `AgentsSelection`
- [ ] `AssemblerFactory.java` class length ≤ 250 lines
- [ ] [SEC-001] `grep -rn "AgentsAssembler\|AgentsSelection" java/src/main/java` returns 0
- [ ] [SEC-004] `grep -rn "import.*AgentsAssembler\|import.*AgentsSelection" java/src/test/java` returns 0
- [ ] `mvn compile` green
- [ ] `mvn test-compile` green
- [ ] `mvn test` green (surviving tests pass)
- [ ] [TL-006] Conventional commit with BREAKING CHANGE footer: `refactor(assembler)!: remove AgentsAssembler and AgentsSelection classes`

## Dependencies

| Depends On | Reason |
|-----------|--------|
| (external) story-0034-0002 merged | Prior stories must have deleted `GithubAgents*` and `CodexAgentsMdAssembler` so grep sanity for *Agents* matches story scope |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Stories 0001/0002 not yet merged into working branch | MEDIUM | Task aborts with confusion | Pre-check step 1 — grep for `GithubAgents*`/`CodexAgentsMd*`; escalate if found |
| `AssemblerFactory` has non-obvious registration pattern (reflection, annotation scan) | LOW | Silent missed reference | Thorough grep + compile check; if compile fails after deletion, investigate unknown consumer |
| Test count regression masks coverage drop | LOW | Coverage below threshold in TASK-004 | Record pre/post test count in commit body; JaCoCo will catch real regression in TASK-004 |
