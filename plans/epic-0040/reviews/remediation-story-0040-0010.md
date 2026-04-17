# Review Remediation — story-0040-0010

**PR:** #420
**Total pending:** 3 LOW findings accepted for follow-up (2 MEDIUM addressed in 4f7d05eaa).

---

## Findings Tracker

| ID | Engineer | Severity | Description | Status | Fix Commit SHA |
| --- | --- | --- | --- | --- | --- |
| FIND-001 | QA | MEDIUM | Exit code 3 (corrupt NDJSON) documented in SKILL.md but unreachable because `streamSkippingInvalid()` is used. Either drop the claim or wire a non-skipping variant. | Fixed | 4f7d05eaa (SKILL.md clarified) |
| FIND-002 | Performance | MEDIUM | `aggregateEpics` materializes all events into `List` before aggregation; refactor to concat per-epic streams to preserve O(1) memory toward the 100k boundary. | Fixed | 4f7d05eaa (streaming refactor) |
| FIND-003 | Performance | LOW | Unconditional `Collections.sort` on 1-sample stat groups. Guard with `if (n > 1)`. | Deferred | — (micro-optimization, not SLA-blocking) |
| FIND-004 | Security | LOW | `--out` path lacks prefix validation; could clobber files outside the expected tree. Document or defensively reject. | Deferred | — (developer CLI, out of threat model) |
| FIND-005 | Security | LOW | Report write is not atomic; JVM failure mid-flush leaves a partial file. Use write-to-temp + `Files.move` atomic-rename. | Deferred | — (reports are regeneratable) |

---

## Remediation Summary

| Status | Count |
| --- | --- |
| Open | 0 |
| Under Review | 0 |
| Fixed | 2 |
| Deferred | 3 |
| Accepted | 0 |
