# Story Planning Report — story-0037-0002

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0037-0002 |
| Epic ID | 0037 |
| Date | 2026-04-13 |
| Agents | Architect, QA, Security, Tech Lead, PO |

## Planning Summary

Mechanism story. Adds the canonical `detect-context` operation (Operation 5) to `x-git-worktree`, providing a single source of truth for nesting detection. Consumers (STORIES 3–8) inline the `detect_worktree_context()` snippet. Docs-only: no Java code.

## Architecture Assessment

- Single file modified: `targets/claude/skills/core/x-git-worktree/SKILL.md`.
- New content: Operation 5 spec, bash snippet, Mermaid flow, Inline Use Pattern, security subsection, RULE-018 cross-reference.
- JSON output schema defined (Section 5.1 of story).
- No new dependencies; requires `jq` at runtime for consumers (documented as prereq).

## Test Strategy Summary

5 Gherkin scenarios in TPP order: degenerate (main repo) → happy (inside worktree) → error (not git) → edge (deep subdir) → boundary (empty worktrees dir). Plus PO-added scenarios: detached HEAD, symlinked paths. Smoke evidence attached to PR body. Golden regen covers output markdown.

## Security Assessment Summary

- **Risk**: path disclosure in JSON output (CWE-209). Mitigation: document redaction expectation in Security Considerations.
- **Risk**: unsafe JSON string construction (CWE-116). Mitigation: TASK-004 hardens escaping using safer quoting.
- **Risk level**: **LOW** (read-only operation, no write side-effects).
- OWASP Top 10: primarily A03 (Injection — JSON construction) / A09 (Logging — info disclosure).

## Implementation Approach

1. DoR gate (TASK-000) — story-0001 must be merged.
2. Add Operation 5 section with full spec + snippet + Mermaid + RULE-018 xref.
3. In parallel: Inline Use Pattern, Security subsection, JSON-escaping harden, PO gherkin amendments.
4. Manual smoke across 5+ scenarios; attach evidence.
5. Golden regen + `mvn verify`.
6. PR opened.

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 9 |
| Doc tasks | 4 |
| Security tasks | 2 |
| Verification/smoke | 2 |
| Quality gate | 1 |

## Consolidated Risk Matrix

| Risk | Source | Severity | Likelihood | Mitigation |
|------|--------|----------|------------|------------|
| Snippet diverges across consumers | ARCH | Medium | Medium | Documented as "canonical — any divergence is a bug"; STORIES 3-8 reference this section |
| Detection fails on symlinked worktrees | QA | Low | Low | Boundary gherkin added (PO amendment); documented behavior |
| Path leak in shared logs | SEC | Medium | Low | Security subsection documents redaction guidance |
| jq missing on consumer env | TL | Low | Medium | Documented as prereq in Inline Use Pattern |

## DoR Status

**READY** — see `dor-story-0037-0002.md`.
