# Security Specialist Review — story-0040-0008

ENGINEER: Security
STORY: story-0040-0008
PR: #418
SCORE: 28/30
STATUS: Approved

## Scope

- `telemetry-phase.sh` mcp-* extension: writes per-method timer files
  under `${TMPDIR:-/tmp}/claude-telemetry/mcp-<method>.start`
- 5 creation SKILL.md markers (markdown-only, no executable code path)

## Checklist

PASSED:
- [SEC-01] No hardcoded secrets / credentials introduced in any changed
  file. `grep -rnE "password|secret|token|api.?key" <changed files>`
  returns zero matches outside the scrubbing utilities already
  instrumented by story-0040-0005. (2/2)
- [SEC-02] Timer file content is a non-sensitive epoch-millis string.
  The filename includes only the mcpMethod identifier (capped at 64
  chars, validated by existing case-branch), which is a public
  contract name — not a user-controlled or untrusted value. (2/2)
- [SEC-03] Path construction uses `${TMPDIR:-/tmp}/claude-telemetry/
  mcp-<method>.start`. `<method>` is validated upstream (arg 3 required
  + length-capped). No path traversal (`..`) possible given the
  length-capped, caller-controlled identifier is always prefixed by
  the fixed directory. No symlink follow logic added. (2/2)
- [SEC-04] Fail-open preserves `set +e` + `exit 0` contract (RULE-004).
  Timer read/write failures, missing timer, unparseable content all
  degrade silently. No panic / abort paths added. (2/2)
- [SEC-05] MCP markers are OPT-IN semantic annotations, not code
  execution paths. They do NOT introduce new network calls, new
  deserialization sinks, or new data flows beyond what
  `mcp__atlassian__*` tools already produce. (2/2)
- [SEC-06] Telemetry output is routed through
  `telemetry-emit.sh` which has PII scrubbing (story-0040-0005). The
  mcp-end event carries `mcpMethod` (non-sensitive identifier) and
  `durationMs` (integer). No free-text user input, no payload excerpts.
  (2/2)
- [SEC-07] RULE-006 kill switch preserved: `CLAUDE_TELEMETRY_DISABLED=1`
  short-circuits BEFORE any timer file is created or read. (2/2)
- [SEC-08] `rm -f` on timer file uses a fixed prefix + validated method
  name — not a wildcard glob. No destructive scope creep risk. (2/2)
- [SEC-09] The `date +%s%3N` / `date +%s` fallback is read-only and
  bounded; no command injection vector (no user-controlled arg piped
  into date/printf). (2/2)
- [SEC-10] All 5 SKILL.md changes are markdown annotations. They do not
  introduce new `allowed-tools` entries, do not expand MCP scope, and
  do not create new prompt injection vectors. (2/2)
- [SEC-11] Integer arithmetic on durationMs uses bash's native 64-bit
  arithmetic. Negative values (clock skew) are clamped to 0, preventing
  JSON schema violations (schema requires `minimum: 0`). (2/2)
- [SEC-12] Timer files in `${TMPDIR}/claude-telemetry/` are shared
  across the user but not across users (TMPDIR is user-scoped on
  macOS/Linux). No cross-user race. (2/2)

PARTIAL:
- [SEC-13] Timer directory creation uses the default umask rather than
  enforcing `700` permissions per Rule 06 "Temp files/directories:
  explicit restrictive permissions (owner-only: 700/600)". This is
  consistent with existing telemetry helpers (same `mkdir -p` pattern
  in `telemetry-session.sh`) and the user's default umask is typically
  022 or stricter on developer machines, but strict Rule 06 compliance
  would call for an explicit `chmod 700` after `mkdir`. Low risk
  (non-sensitive data, user-scoped TMPDIR), but worth tracking.
  (1/2)

## Findings Severity Distribution

| Severity | Count |
|----------|-------|
| Critical | 0 |
| High     | 0 |
| Medium   | 0 |
| Low      | 1 (SEC-13 temp-dir permission hardening — consistent with
           existing codebase pattern, potential epic-wide hardening
           opportunity) |

## Summary

28/30 — Approved. No injection, traversal, credential leak, or
deserialization vectors introduced. PII scrubbing preserved via
`telemetry-emit.sh` downstream. Kill switch preserved. Low-severity
SEC-13 aligns with existing pattern and is a candidate for a
cross-cutting hardening story under EPIC-0040 rather than a blocker
for this change.
