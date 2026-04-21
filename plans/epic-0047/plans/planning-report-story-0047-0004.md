# Story Planning Report -- story-0047-0004

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0047-0004 |
| Epic ID | 0047 |
| Date | 2026-04-21 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |

## Planning Summary

Story compresses the 5 largest knowledge packs (click-cli-patterns 1222 LoC, k8s-helm 945, axum-patterns 889, iac-terraform 862, dotnet-patterns 815 — total ~4734 LoC) by moving inline code-block examples to `references/examples-<pattern>.md` files (1 per pattern). SKILL.md of each KP becomes narrative + Patterns Index + Stack Compatibility + References pointer, target ≤250 LoC. click-cli-patterns executes first as pilot (most linear structure); remaining 4 parallelize after validation. Final smoke + corpus measurement updates epic §6. Expected saving ~3500 LoC from hot-path.

## Architecture Assessment

- **Affected layers:** doc (KP SKILL.md + references) + test (goldens + smoke).
- **New components:** ~50 `references/examples-*.md` files (10 per KP × 5 KPs) + 5 rewritten SKILL.md + smoke extension.
- **Dependency direction:** no runtime code change; source-file reorganization only.
- **Layer discipline:** RULE-001 maintained; KP structure `knowledge-packs/<category>/<kp>/{SKILL.md,references/**}` preserved; taxonomy (ADR-0003) unchanged.
- **Reuses:** optional — may link `_shared/example-header.md` from story-0047-0001 if consistent header pattern emerges.

## Test Strategy Summary

- **Acceptance tests (AT):** 4 Gherkin scenarios — (AT-1) KP slim lists all original patterns; (AT-2) code samples byte-identical post-carve; (AT-3) SKILL.md still functions as entry point; (AT-4) 0047-0003 lint passes on each slim KP.
- **Unit tests:** N/A (no Java code).
- **Smoke:** `Epic0047CompressionSmokeTest.smoke_kpsHaveCarvedExamples` validates — per KP — `references/examples-*.md` exists, SKILL.md ≤250 LoC, Patterns Index lists all original patterns (collection TPP: iterate 5 KPs).
- **Golden diff:** 17 profiles × 5 KPs regenerated; byte-diff per commit.
- **Measurement:** `SkillCorpusSizeAudit` (from 0047-0003) consumed; epic §6 updated; issue opened if delta < −20% vs baseline (RULE-047-07).

## Security Assessment Summary

- **OWASP mapping:** Not applicable — pure documentation refactor.
- **Content integrity:** byte-identical code preservation is a reviewer concern, not a security concern.
- **Risk level:** LOW.

## Implementation Approach

- **Tech Lead decision:** 1 PR per KP (5 PRs) for isolated reviewability per lang/stack expertise. Pilot (click-cli) validates pattern before fan-out.
- **Quality gates:** each KP SKILL.md post-carve must pass 0047-0003 lint (or soft-gate if 0047-0003 not yet merged).
- **Coding standards:** N/A (no Java); Markdown discipline: consistent heading hierarchy `# Example: <pattern>` / `## Use Case` / `## Implementation` / `## Notes`.
- **Coordination:** parallel regen of goldens is the main conflict risk (RULE-004 hotspot `src/test/resources/golden/**`). Consider serializing regen step per KP merge.

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 6 |
| Architecture tasks | 5 (001-005) |
| Test tasks | 1 (006 smoke + measurement) |
| Security tasks | 0 |
| Quality gate tasks | 0 (external — 0047-0003 lint) |
| Validation tasks | 0 (PO validation via Gherkin AT-3 within each refactor) |
| Merged tasks | 0 |
| Augmented tasks | 0 |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|------------|----------|------------|------------|
| Code samples not byte-identical post-carve | QA | HIGH | MEDIUM | Checksum each extracted block vs inline; golden diff catches | 
| Patterns Index missing a pattern from original | QA | HIGH | MEDIUM | Smoke test explicitly lists patterns pre/post carve |
| 4-way parallel golden regen contention | Tech Lead | MEDIUM | HIGH | Serialize regen; or queue merges; RULE-004 hotspot |
| KP slim < 250 LoC infeasible for some KP | Architect | MEDIUM | MEDIUM | Documented exception in PR; 0047-0003 lint absorbs via non-empty references/ |
| Delta < target −40% | Tech Lead | MEDIUM | LOW | RULE-047-07 mandates investigation issue; story does best-effort |

## DoR Status

**READY** — all 10 mandatory checks pass.

## File Footprint

- `write:` 5 × `java/src/main/resources/targets/claude/skills/knowledge-packs/**/SKILL.md`, ~50 × `java/src/main/resources/targets/claude/skills/knowledge-packs/**/references/examples-*.md`, `java/src/test/java/dev/iadev/smoke/Epic0047CompressionSmokeTest.java` (extends), `plans/epic-0047/epic-0047.md` (§6 measurement)
- `read:` 5 KP source SKILL.md; optional `_shared/example-header.md` from story-0047-0001; 0047-0003 lint output
- `regen:` `java/src/test/resources/golden/**` (17 profiles × 5 KPs) — HARD-CONFLICT hotspot per RULE-004
- `write:` `CHANGELOG.md` [Unreleased] — SOFT-CONFLICT hotspot
