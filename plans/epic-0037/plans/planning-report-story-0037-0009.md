# Story Planning Report — story-0037-0009

| Story ID | story-0037-0009 | Epic ID | 0037 | Date | 2026-04-13 |
| Agents | Architect, QA, Security, TechLead, PO |

## Planning Summary
ADR-0004 — Worktree-First Branch Creation Policy. Doc-only, transcription task (full body pre-authored in story §3.1). Independent (no Blocked By), can run in parallel with all other stories. Single sync point: TASK-003 cross-ref needs Rule 14 (story-0037-0001) merged. 4 Gherkin scenarios. 6 consolidated tasks.

## Architecture
Single coherent ADR (rejected per-skill fragmentation per Alternative C). 7 mandatory sections following `_TEMPLATE-ADR.md`. Per-skill consequence matrix covers all 7 affected skills. Located in `/adr/` root (NOT targets/) — confirmed per project convention. Mutual cross-reference with Rule 14.

## Test Strategy
4 ATs (creation / indexing / cross-refs / single-ADR integrity). No golden file impact (ADRs excluded per story §4 DoD). Validation via file existence, section-count grep, link resolution.

## Security
Minimal scope — ADR documentation accuracy is the only concern. No new attack surface, no dependencies, no code.

## Implementation Approach
TechLead: single PR with atomic commits (file / index / cross-ref). PO added downstream-reference audit task. Forward-reference to Rule 14 must be resolved before TASK-003 can be marked complete.

## Risk Matrix
| Risk | Sev | Likely | Mitigation |
|------|-----|--------|-----------|
| ADR fragmented (multiple files instead of one) | Medium | Low | TASK-001 explicit single-file requirement; Alternatives Considered section justifies single-ADR |
| Cross-ref broken (Rule 14 not yet merged) | Low | Medium | TASK-003 explicit dependency on story-0037-0001 merge |
| Per-skill consequence matrix missed a skill | Low | Low | TASK-001 DoD enumerates all 7 skills |
| Date in ADR header drifts from merge date | Low | Medium | Update at merge time, not at PR open |

## DoR Status
**READY** — 10/10 mandatory pass. See `dor-story-0037-0009.md`.
