# Story Planning Report — story-0037-0002

| Field | Value |
|-------|-------|
| Story ID | story-0037-0002 |
| Epic ID | 0037 |
| Date | 2026-04-13 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |

## Planning Summary

Mechanism story of EPIC-0037: documents canonical `detect_worktree_context()` bash snippet as Operation 5 of `x-git-worktree` skill. Doc-only; depends on story-0037-0001 (Rule 14). Provides single source of truth for non-nesting-invariant detection (RULE-002) consumed inline by stories 3-8. Consolidated to 9 atomic tasks including security hardening of JSON escaping.

## Architecture Assessment

Insertion point: between Operation 4 (cleanup, ~L329) and "Git Flow Integration (RULE-005)" (L331). Snippet design is pure POSIX (no jq dep in canonical block; jq only in inline-use example). JSON output matches Section 5.1 schema. Mermaid flowchart (Section 6.1) embedded under workflow subsection. Cross-reference to RULE-018: relative path `../../../../rules/14-worktree-lifecycle.md` valid for current 4-level SoT layout (`skills/core/x-git-worktree/`); becomes 5-level (`../../../../../rules/...`) post-EPIC-0036 — verify at branch cut.

## Test Strategy Summary

Outer loop: 5 ATs mapping to TPP-ordered Gherkin (degenerate → happy → error → edge → boundary), with PO amendments adding detached-HEAD and symlinked-path scenarios. Inner loop: structural assertions in a new `XGitWorktreeDetectContextTest` class — section header presence, sample-output literals, function-name regex, JSON schema parity, cross-reference resolution, golden parity. Manual smoke (5 scenarios) executed against fixture script, evidence committed to plan dir + linked from PR.

## Security Assessment Summary

OWASP categories applicable: **A03 (Injection)** via JSON output not properly escaping `"`, `\`, newlines (CWE-116); **A05 (Security Misconfiguration)** via undocumented jq prereq in inline-use; **CWE-209 (Information Disclosure)** via absolute paths in JSON output leaking local filesystem layout to CI logs. Mitigations: harden snippet to use jq-based escaping, document `Security Considerations` subsection with redaction example, declare jq prereq with fail-fast guard, document single-quoted heredoc invariant. Read-only nature of snippet (no eval, no unquoted expansion to shell) limits attack surface.

## Implementation Approach

Tech Lead approves story decomposition with refinements: split snippet creation (TASK-001) from inline-use docs (TASK-002), security subsection (TASK-003), JSON escaping hardening (TASK-004), and PO amendments (TASK-006). Manual smoke (TASK-005) MUST embed fixture script in PR body for reviewer reproducibility — not blocker. Quality gates: snippet contract correctness, schema↔snippet↔samples three-way parity, Mermaid fidelity, inline-use justification, RULE-018 cross-ref path resolves, PR hygiene per Rule 09.

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 9 |
| Architecture tasks | 6 (consolidated into TASK-001 + TASK-007) |
| Test tasks | 12 raw (consolidated into TASK-001 + TASK-005 + TASK-007) |
| Security tasks | 3 (TASK-003 + TASK-004; SEC-002 augments TASK-001) |
| Quality gate tasks | 7 (consolidated into TASK-008 + criteria injected) |
| Validation tasks | 6 (consolidated into TASK-006) |
| Merged tasks | 6 |
| Augmented tasks | 2 (TASK-001 ← SEC; TASK-005 ← PO) |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|------------|----------|------------|------------|
| Relative path to RULE-018 wrong if EPIC-0036 renames merged mid-flight | Architect | Medium | Low | Verify directory depth at branch cut; PR body documents path choice |
| JSON escaping breaks downstream jq consumers on unusual paths | Security | Medium | Low | TASK-004 hardens via jq escaping; smoke covers double-quote scenario |
| Absolute paths in JSON leak to CI logs | Security | Low | Medium | TASK-003 documents redaction; consumers responsible for redaction |
| Manual smoke not reproducible by reviewer | TechLead | Medium | Medium | TASK-005 embeds fixture script in PR body |
| Story spec mixes path conventions (`/Users/dev/repo` vs `/repo`) creating test churn | PO | Low | High | TASK-006 standardizes early to `/repo` |
| Downstream stories 3-8 may reimplement detection rather than reuse Operation 5 | PO | Medium | Medium | PO-004 adds explicit "Reuses Operation 5" callout to each downstream story dependencies section |

## DoR Status

**READY** — 10/10 mandatory pass; conditional checks N/A (compliance=none, contract_tests=false). See `dor-story-0037-0002.md`.
