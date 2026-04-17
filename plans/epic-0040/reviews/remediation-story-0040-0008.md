# Review Remediation — story-0040-0008

**0 findings pending remediation (4 low-severity notes only, all
deferred as out-of-scope hardening opportunities).**

## Findings Tracker

| Finding ID | Engineer | Severity | Description | Status | Fix Commit SHA |
|------------|----------|----------|-------------|--------|----------------|
| FIND-001 | QA | Low | BSD/macOS `date +%s%3N` fallback produces second-granularity `durationMs`. Covered by IT via 1.1s sleep and schema tolerance. | Accepted — cross-platform constraint, not a functional bug | — |
| FIND-002 | Security | Low | Timer directory `${TMPDIR}/claude-telemetry/` created with default umask rather than explicit 700. Pattern matches existing telemetry helpers. | Deferred — track as epic-wide hardening opportunity (consistent pattern, TMPDIR is user-scoped). | — |
| FIND-003 | Performance | Low | BSD granularity same as FIND-001 viewed through the performance lens. | Accepted (see FIND-001). | — |
| FIND-004 | Performance | Low | Per-call fork overhead accumulates at very high N (>100 MCP calls/session). Bounded in practice by epic size. | Accepted — out-of-scope for this story; candidate for future batching optimization. | — |

## Remediation Summary

| Status | Count |
|--------|-------|
| Open | 0 |
| Fixed | 0 |
| Deferred | 2 |
| Accepted | 2 |

No CRITICAL / HIGH / MEDIUM findings — no automatic remediation
required. Merge gate is GREEN from the specialist side; awaiting Tech
Lead review.
