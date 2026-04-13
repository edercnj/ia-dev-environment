# Story Planning Report — story-0037-0001

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0037-0001 |
| Epic ID | 0037 |
| Date | 2026-04-13 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |

## Planning Summary

Foundation story of EPIC-0037 (Worktree-First Branch Creation Policy). Documentation-only: creates `targets/claude/rules/14-worktree-lifecycle.md` with 7 sections, replaces inline naming-convention block in `x-git-worktree/SKILL.md` with one-line pointer, deletes drift section ("Integration with Epic Execution"), updates all `RULE-018` cross-references in `targets/`, regenerates golden files. Zero Java code changes. Blocks 7 other stories of the epic. Consolidated into 7 atomic tasks.

## Architecture Assessment

Doc-only scope ⇒ no architecture diagrams, no ADR. Five invariants govern: (1) RULE-001 SoT compliance (edits only in `targets/`); (2) Slot 14 justification (slots 10-12 reserved, 13 occupied); (3) cross-reference blast radius is small (~2 hits in `x-git-worktree/`); (4) rule file structural template mirrors existing `13-skill-invocation-protocol.md`; (5) golden regen determinism requires `mvn process-resources` first, then `GoldenFileRegenerator` from a clean working tree. No layer impact, no port/adapter changes.

## Test Strategy Summary

Outer loop: 5 acceptance tests, one per Gherkin scenario (degenerate → happy → cleanup → integration → smoke), TPP-ordered. Inner loop: file/content assertions instead of unit tests (no production code). Existing harness is sufficient — `GoldenFileTest`, `PlatformDirectorySmokeTest`, `ContentIntegritySmokeTest`, `FrontmatterSmokeTest`, `CrossProfileConsistencySmokeTest`. No new Java test class required; lightweight grep-based assertions added inline.

## Security Assessment Summary

OWASP categories: only **A05 (Security Misconfiguration)** applies — the rule establishes secure defaults for worktree placement, protected-branch policy, non-nesting invariant, and cleanup ownership. Indirect risks documented: (1) `.claude/worktrees/` must be `.gitignore`-covered to prevent leaking transient branch contents (incl. `.env`/secrets-in-progress); (2) anti-patterns must forbid worktree creation outside `.claude/worktrees/` and protected-branch checkout; (3) cross-references must use repo-relative paths only. No new dependencies, no auth/authz/network/IO logic, compliance flag = none.

## Implementation Approach

Tech Lead approves story Section 7 task decomposition with one refinement: split inline replacement (TASK-002) and drift deletion (TASK-003) into separate commits for git-history clarity (same PR). Quality gates: SoT compliance (zero direct edits to `.claude/`/`.github/`); markdown structural integrity (7 sections, table column counts); cross-ref completeness via grep; golden regen reproducibility; smoke + verify green; Conventional Commits with `(story-0037-0001)` scope using `docs:`/`chore:` types only; PR targets `develop` with label `epic-0037`.

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 7 |
| Architecture tasks | 5 (consolidated into impl tasks) |
| Test tasks | 15 raw (consolidated into impl + verify) |
| Security tasks | 3 (augmented into TASK-001 + TASK-004) |
| Quality gate tasks | 7 (consolidated to TASK-006 + criteria injected) |
| Validation tasks | 6 (consolidated into TASK-000 + DoD additions) |
| Merged tasks | 5 |
| Augmented tasks | 2 (TASK-001 ← SEC; TASK-004 ← SEC) |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|------------|----------|------------|------------|
| Golden regen produces noisy/spurious diffs | QA, TechLead | Medium | Medium | Run on clean working tree; `git diff --stat` review; re-run if needed |
| RULE-018 grep false positives (historical refs) | PO, TechLead | Low | Medium | Manual inspection; document each redirect in PR body |
| Slot 14 collision with concurrent rule additions | PO | Low | Low | TASK-000 verifies slot free at branch cut |
| `.claude/worktrees/` accidentally committed in downstream stories | Security | Medium | Low (out of scope) | Rule 14 documents `.gitignore` requirement; later stories enforce |
| Drift section deletion leaves orphan internal anchors elsewhere | PO | Low | Low | TASK-003 DoD includes orphan-link grep |

## DoR Status

**READY** — 10/10 mandatory checks pass; conditional checks N/A (compliance=none, contract_tests=false). See `dor-story-0037-0001.md`.
