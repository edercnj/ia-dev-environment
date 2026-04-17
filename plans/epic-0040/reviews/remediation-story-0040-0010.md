# Review Remediation — story-0040-0010

**PR:** #420
**Total pending:** 5 findings (2 MEDIUM, 3 LOW)

---

## Findings Tracker

| ID | Engineer | Severity | Description | Status | Fix Commit SHA |
| --- | --- | --- | --- | --- | --- |
| FIND-001 | QA | MEDIUM | Exit code 3 (corrupt NDJSON) documented in SKILL.md but unreachable because `streamSkippingInvalid()` is used. Either drop the claim or wire a non-skipping variant. | Open | — |
| FIND-002 | Performance | MEDIUM | `aggregateEpics` materializes all events into `List` before aggregation; refactor to concat per-epic streams to preserve O(1) memory toward the 100k boundary. | Open | — |
| FIND-003 | Performance | LOW | Unconditional `Collections.sort` on 1-sample stat groups. Guard with `if (n > 1)`. | Open | — |
| FIND-004 | Security | LOW | `--out` path lacks prefix validation; could clobber files outside the expected tree. Document or defensively reject. | Open | — |
| FIND-005 | Security | LOW | Report write is not atomic; JVM failure mid-flush leaves a partial file. Use write-to-temp + `Files.move` atomic-rename. | Open | — |

---

## Remediation Summary

| Status | Count |
| --- | --- |
| Open | 5 |
| Under Review | 0 |
| Fixed | 0 |
| Deferred | 0 |
| Accepted | 0 |
