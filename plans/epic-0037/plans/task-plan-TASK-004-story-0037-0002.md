# Task Plan — TASK-004 (story-0037-0002)

| Field | Value |
|-------|-------|
| Source Agent | Security | Type | security | TDD Phase | GREEN | Effort | XS |

## Objective
Harden JSON escaping in `detect_worktree_context()` snippet to guarantee well-formed output when worktree paths contain JSON metacharacters (`"`, `\`, newline, tab). Addresses CWE-116 / OWASP A03.

## Implementation Guide
1. Replace fragile `[ "$wt_path" = "null" ] && echo "null" || echo "\"$wt_path\""` substitution.
2. Preferred approach (jq-based): `printf` builds JSON via `jq -Rn --arg p "$wt_path" --arg m "$main_repo" '{inWorktree: $iw, worktreePath: ($p|select(length>0)), mainRepoPath: $m}'` (note: requires jq — already declared as inline-use prereq, but canonical block needs to either depend on jq or use bash escape).
3. Alternative (pure bash): write `json_escape()` helper that escapes `\`, `"`, newline (`\n`), tab (`\t`), and control chars (< 0x20).
4. Add Gherkin scenario "Edge — worktree path contains double-quote" to story Section 7.
5. Update Sample Output #4 (or new) to demonstrate escaped path.

## DoD
- [ ] Snippet emits valid JSON when wt_path contains `"`, `\`, newline (CWE-116)
- [ ] Approach documented (jq-based or pure-bash json_escape helper)
- [ ] OWASP A03 referenced in comment within snippet
- [ ] New Gherkin scenario added in story Section 7
- [ ] TASK-005 smoke includes the double-quote scenario verifying JSON parses with `jq empty`

## Dependencies
TASK-001 (parallel with TASK-002, TASK-003, TASK-006).
