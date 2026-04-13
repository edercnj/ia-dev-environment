# Task Plan — TASK-002 (story-0037-0002)

| Field | Value |
|-------|-------|
| Source Agent | merged(Architect, Security, TechLead) | Type | documentation | TDD Phase | GREEN | Effort | XS |

## Objective
Document "Inline Use Pattern" subsection under Operation 5 with x-git-push example, jq prerequisite, and heredoc safety invariant.

## Implementation Guide
1. Add subsection `#### Inline Use Pattern` immediately after the Sample Outputs of Operation 5.
2. Rationale paragraph: declares snippet as canonical source of truth; consumers MUST inline rather than shell-out (hot-path cost).
3. x-git-push example using `source <(cat <<'BASH' ... BASH)` (single-quoted delimiter prevents variable expansion injection — document this invariant).
4. Show `jq -r '.inWorktree'` to extract field.
5. Add jq prereq fail-fast guard: `command -v jq >/dev/null || { echo "jq required" >&2; exit 127; }`.
6. Cross-reference Rule 06 for security baseline.

## DoD
- [ ] `#### Inline Use Pattern` subsection present
- [ ] Function name `detect_worktree_context` reused verbatim (no drift)
- [ ] Heredoc delimiter is single-quoted `'BASH'` (variable-expansion safe)
- [ ] jq prereq documented with fail-fast snippet (exit 127)
- [ ] Heredoc invariant explicitly stated as security requirement
- [ ] Rule 06 cross-reference present

## Dependencies
TASK-001.
