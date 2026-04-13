# Story Planning Report — story-0037-0004

| Field | Value |
|-------|-------|
| Story ID | story-0037-0004 | Epic ID | 0037 | Date | 2026-04-13 |
| Agents | Architect, QA, Security, TechLead, PO |

## Planning Summary

Doc-only story adding opt-in `--worktree` flag to `x-git-push`. RULE-004 (backward compat) is mandatory blocker — without flag behavior MUST be byte-identical to baseline. 5 Gherkin scenarios. Pre-defined tasks 001-006 retained 1:1 with DoD criteria sharpened (slug sanitization, nested detection, jq fail-fast).

## Architecture Assessment

Insertion point: after current Step 1.2 where `git checkout -b` runs. Cross-reference to RULE-018 uses 4-level relative path. Slug derivation: regex `^[a-z]+/` strips prefix; multi-segment edge case needs spec clarification (e.g., `feature/sub/story`). Caller-owns-removal: skill does NOT auto-cleanup worktree. Mermaid decision tree from story §6.1 embedded.

## Test Strategy Summary

5 ATs (backward compat / happy main / happy nested / error / boundary slug). Inner loop: structural assertions (frontmatter `[--worktree]`, parameters table row, Step 1.3 4-step flow, backward-compat section). Two manual smoke tests are mandatory: (a) backward-compat regression (byte-identical output without flag) — RULE-004 blocker; (b) with-flag end-to-end including nested detection, error fail-fast, boundary slug handling.

## Security Assessment Summary

- **CWE-22 path traversal** via crafted branch name into slug → fold sanitization into Step 1.3 doc (regex `[a-z0-9-]+`, reject `..`, `/`, metachars)
- **Drift risk**: inline-copied `detect_worktree_context()` may diverge from x-git-worktree canonical → drift note in Step 1.3 designates x-git-worktree as authoritative
- **OWASP A05**: jq prereq must fail fast (consistent with story-0037-0002 inline-use pattern)

## Implementation Approach

TechLead enforces opt-in default (no behavior change without flag), atomic commits per concern (frontmatter / params / Step 1.3 / backward-compat smoke / worktree smoke / regen+PR), backward-compat regression as mandatory blocker. Standardize example slug as `story-0037-0003-foo` for cross-story consistency.

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 6 |
| Architecture | merged into TASK-001..003 |
| Test/Smoke | TASK-004 (regression), TASK-005 (worktree) |
| Security | augmented into TASK-003 + TASK-005 |
| Quality gate | TASK-006 |
| Validation | merged into TASK-005 |

## Risk Matrix

| Risk | Source | Sev | Likely | Mitigation |
|------|--------|-----|--------|-----------|
| Without-flag behavior diverges (RULE-004 violation) | TL | Critical | Low | TASK-004 byte-identical regression mandatory |
| Slug `[a-z0-9-]` regex too restrictive for legit names | Architect | Low | Medium | Document multi-segment edge case; provide examples |
| CWE-22 via crafted branch name | Security | High | Low | TASK-003 sanitization regex |
| Drift between inline snippet and x-git-worktree canonical | Security | Medium | Medium | Drift note + drift-detection test in story-0037-0002 follow-up |
| jq missing in CI environment | PO | Low | Medium | Fail-fast guard documented |

## DoR Status

**READY** — 10/10 mandatory pass. See `dor-story-0037-0004.md`.
