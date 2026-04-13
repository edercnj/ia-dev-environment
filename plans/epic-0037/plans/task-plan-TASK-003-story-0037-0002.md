# Task Plan — TASK-003 (story-0037-0002)

| Field | Value |
|-------|-------|
| Source Agent | Security | Type | security | TDD Phase | VERIFY | Effort | XS |

## Objective
Add "Security Considerations" subsection under Operation 5 documenting absolute-path leakage risk (CWE-209) and providing a redaction example.

## Implementation Guide
1. Add `#### Security Considerations` subsection after Inline Use Pattern.
2. State that JSON output contains absolute filesystem paths (`worktreePath`, `mainRepoPath`) — risk of leaking local layout to CI logs, bug reports, or shared transcripts.
3. Provide concrete redaction example using jq: `... | jq '.worktreePath |= sub(env.HOME; "~")'`.
4. Cross-reference Rule 06 (security baseline) and CWE-209.
5. Recommend consumers redact before logging, never log raw output in production telemetry.

## DoD
- [ ] `#### Security Considerations` subsection present
- [ ] CWE-209 (Information Exposure Through Error/Output) referenced
- [ ] Explicit warning about absolute paths in JSON output
- [ ] Redaction example using jq present and runnable
- [ ] Rule 06 cross-reference present

## Dependencies
TASK-001 (parallel with TASK-002, TASK-004, TASK-006).
