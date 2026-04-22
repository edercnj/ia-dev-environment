# Story Planning Report -- story-0047-0002

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0047-0002 |
| Epic ID | 0047 |
| Date | 2026-04-21 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |

## Planning Summary

Story formally retires the broken `## Slim Mode` append-only pattern (introduced by story-0030-0006, deleted by Bucket A item A5) and replaces it with **flipped orientation**: SKILL.md as minimum viable behavioral contract (default-slim) + `references/full-protocol.md` on-demand. ADR-0007 documents the architectural decision. The 5 target skills (`x-test-tdd`, `x-story-implement`, `x-git-commit`, `x-code-format`, `x-code-lint`) are rewritten as slim with target line limits (≤200 for pre-commit cluster; ≤250 for larger orchestrators). A new smoke test validates the pattern. No Java code changes; doc-heavy refactor.

## Architecture Assessment

- **Affected layers:** doc (SKILL.md source + references); test (goldens + smoke).
- **New components:** ADR-0007; 5 `references/full-protocol.md`.
- **Dependency direction:** source-only refactor; runtime behavior unchanged.
- **Layer discipline:** RULE-001 maintained; no edits to `.claude/**` or goldens directly.
- **Slim contract mandatory sections:** `## Triggers`, `## Parameters`, `## Output Contract`, `## Error Envelope`, `## Full Protocol` (pointer).

## Test Strategy Summary

- **Acceptance tests (AT):** 4 Gherkin scenarios from Section 7 — (AT-1) slim SKILL.md executable standalone for happy path; (AT-2) edge case requires `references/full-protocol.md`; (AT-3) ADR-0007 documents decision; (AT-4) golden regen byte-identical for 17 profiles.
- **Unit tests:** N/A (no Java code new).
- **Smoke:** `Epic0047CompressionSmokeTest.smoke_slimSkillsHaveFullProtocolReference` validates presence of `full-protocol.md` in each target skill + section headers + line limits (TPP: collection — iterate over 5 skills).
- **Golden diff:** 17 profiles × 5 skills regenerated; byte-diff documented per commit.

## Security Assessment Summary

- **OWASP mapping:** Not applicable — pure documentation refactor, no runtime changes.
- **Data exposure:** none introduced.
- **Risk level:** LOW.

## Implementation Approach

- **Tech Lead decision:** Per-skill PR strategy (1 PR per skill) for reviewability and isolated rollback; branch per task already declared in story §8.
- **Quality gates:** After each slim rewrite, `mvn process-resources && mvn verify` must be green; line limit enforced (target asserted in smoke test per-skill).
- **Coordination:** TASK-006 (x-story-implement) must wait for Bucket A item A4 to merge in develop to avoid conflict with duplicate-content extraction.
- **Coding standards:** N/A for Java (no new code); Markdown style: consistent heading hierarchy, tables not prose for contract sections.

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 7 |
| Architecture tasks | 1 (001 ADR) |
| Test tasks | 1 (007 smoke) |
| Security tasks | 0 |
| Quality gate tasks | 0 (external — line limit enforced by 0047-0003 lint and by smoke assertion) |
| Validation tasks | 5 (002-006 are implementation + PO validation via Gherkin happy-path AT) |
| Merged tasks | 0 |
| Augmented tasks | 0 |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|------------|----------|------------|------------|
| Conflict with Bucket A A4 on x-story-implement | Tech Lead | HIGH | MEDIUM | DoR gate: confirm A4 merged; TASK-006 waits |
| Slim skill missing essential runtime info | QA | HIGH | LOW | Gherkin AT-1 "happy path standalone"; smoke validates 4 mandatory sections |
| Golden regression outside 5 target skills | QA | HIGH | LOW | Full `mvn verify`; per-profile byte diff review |
| LLM context cost of `references/full-protocol.md` extra Read | Tech Lead | LOW | HIGH | ADR-0007 explicitly documents trade-off; acceptable by design |

## DoR Status

**READY** — all 10 mandatory checks pass; conditional N/A.

## File Footprint

- `write:` `adr/ADR-0007-skill-body-slim-by-default.md`, 5 × `java/src/main/resources/targets/claude/skills/core/**/SKILL.md`, 5 × `java/src/main/resources/targets/claude/skills/core/**/references/full-protocol.md`, `java/src/test/java/dev/iadev/smoke/Epic0047CompressionSmokeTest.java` (extends)
- `read:` 5 source SKILL.md, ADR-0007 draft, Bucket A A4 diff
- `regen:` `java/src/test/resources/golden/**` (17 profiles × 5 skills) — HARD-CONFLICT with story-0047-0001 if executed concurrently; RULE-004 hotspot
