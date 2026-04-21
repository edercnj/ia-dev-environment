# Story Planning Report -- story-0047-0001

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0047-0001 |
| Epic ID | 0047 |
| Date | 2026-04-21 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |

## Planning Summary

Story establishes `_shared/` directory (sibling of `core/`/`conditional/`/`knowledge-packs/`) with 3 initial cross-cutting snippets plus README, formalizes inclusion strategy via ADR-0006 (choice among: (a) `{{INCLUDE:...}}` placeholder resolved by `SkillsAssembler`, (b) Markdown relative links, (c) symlinks), and pilots the chosen strategy on the pre-commit cluster (`x-git-commit`, `x-code-format`, `x-code-lint`). Golden regen of 17 profiles validates byte-parity; new smoke test ensures `_shared/` ships to all profiles.

## Architecture Assessment

- **Affected layers:** cross-cutting (source-of-truth directory), application (if opt (a) — new `SnippetIncluder` Java helper), test (golden + smoke).
- **New components:** `_shared/README.md` + 3 snippet files; `adr/ADR-0006-*`; optionally `SnippetIncluder.java` + unit test.
- **Dependency direction:** pure source files + assembler-layer helper. No domain changes.
- **Layer discipline:** RULE-001 (no edits to `.claude/**` or goldens directly) maintained; changes only to `java/src/main/resources/targets/claude/skills/_shared/` source.

## Test Strategy Summary

- **Acceptance tests (AT):** 3 Gherkin scenarios from Section 7 — (AT-1) opt-a/b/c produces valid output for pre-commit cluster; (AT-2) missing snippet fails assembly early; (AT-3) non-consumer skill unaffected.
- **Unit tests (UT, TPP):** If opt (a): placeholder present (constant) → placeholder absent (nil) → malformed (conditional). ≥95% Line / 90% Branch target.
- **Smoke:** `Epic0047CompressionSmokeTest.smoke_sharedDirShipsToAllProfiles` validates 17 profiles + 2 platform variants copy `_shared/`.
- **Golden diff:** all 17 profiles regenerated; byte-diff documented in commit.

## Security Assessment Summary

- **OWASP mapping:** Not applicable — no runtime input, no auth, no network. Source-file organization and assembler path resolution only.
- **Path traversal concern (if opt a):** `{{INCLUDE:...}}` resolver MUST normalize the included path and reject traversal outside `_shared/` (rule 06 Secure Defaults — Path operations). DoD of TASK-003 must include path-traversal rejection test.
- **Secrets / PII:** none involved.
- **Risk level:** LOW.

## Implementation Approach

- **Tech Lead decision:** Choose opt (a) `{{INCLUDE:...}}` if assembler complexity acceptable; opt (b) Markdown links if minimal-change preferred. Tie-breaker: golden byte-stability favors (a) because inclusion happens before golden diff capture.
- **Quality gates:** `SkillSizeLinter` (from story-0047-0003) must not flag `_shared/` (excluded by design — no SKILL.md); pre-commit cluster SKILLs remain < 500 LoC after inclusion.
- **Coding standards:** `SnippetIncluder` ≤ 25 lines/method; record-based `IncludeRequest` DTO for traversal guard.

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 5 |
| Architecture tasks | 2 (001, 002) |
| Test tasks | 2 (003 unit, 005 smoke) |
| Security tasks | 0 (covered inline in 003 DoD) |
| Quality gate tasks | 0 (external to story — lint from 0047-0003) |
| Validation tasks | 1 (004 pilot = PO validation) |
| Merged tasks | 0 |
| Augmented tasks | 1 (003 augmented with path-traversal check) |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|------------|----------|------------|------------|
| ADR-0006 decision deadlock | Tech Lead | MEDIUM | MEDIUM | DoR gate: ADR reviewed by another maintainer before TASK-003 |
| Golden regression outside pilot cluster | QA | HIGH | LOW | Full `mvn verify`; per-profile byte diff review |
| Path traversal via malicious include | Security | HIGH | LOW | Normalize + prefix-check in `SnippetIncluder` (opt a only) |
| Assembly performance regression > 10% | Tech Lead | MEDIUM | LOW | Wall-clock measurement in PR description |

## DoR Status

**READY** — all 10 mandatory checks pass; compliance and contract-test checks N/A for this project (`compliance=none`, `contract_tests=false` per project identity).

## File Footprint

- `write:` `java/src/main/resources/targets/claude/skills/_shared/**`, `adr/ADR-0006-shared-snippets-inclusion-strategy.md`, `java/src/main/java/dev/iadev/application/assembler/SnippetIncluder.java` (if opt a), `java/src/test/java/dev/iadev/application/assembler/SnippetIncluderTest.java` (if opt a), `java/src/main/resources/targets/claude/skills/core/git/x-git-commit/SKILL.md`, `java/src/main/resources/targets/claude/skills/core/code/x-code-format/SKILL.md`, `java/src/main/resources/targets/claude/skills/core/code/x-code-lint/SKILL.md`, `java/src/test/java/dev/iadev/smoke/Epic0047CompressionSmokeTest.java`
- `read:` story file, ADR-0006, `SkillsAssembler.java`, `SkillsCopyHelper.java`
- `regen:` `java/src/test/resources/golden/**` (17 profiles) — HARD-CONFLICT hotspot per RULE-004
