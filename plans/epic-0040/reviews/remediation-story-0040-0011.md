# Review Remediation — story-0040-0011

**Story:** /x-telemetry-trend — cross-epic regression detection
**Total findings (Round 1):** 3
**Open:** 0 | **Fixed:** 3 | **Deferred:** 0 | **Accepted:** 0

## Findings Tracker

| Finding ID | Engineer | Severity | Description | Status | Fix Commit SHA |
| :--- | :--- | :--- | :--- | :--- | :--- |
| FIND-001 | QA | MEDIUM | `TelemetryTrendCliIT.java` at 263 lines exceeds the 250-line ceiling in Rule 03. | Fixed | (in remediation commit) |
| FIND-002 | QA | MEDIUM | Duplicate `writeFixture` / `skillEnd` helpers across 4 test files. | Fixed | (in remediation commit) |
| FIND-003 | Performance | MEDIUM | `TelemetryIndexBuilder.java` at 270 lines exceeds the 250-line ceiling in Rule 03. | Fixed | (in remediation commit) |

## Resolution Notes

### FIND-001 & FIND-002 (shared fix)

Extracted `TelemetryTrendTestFixtures` package-private helper containing the
canonical `writeFixture` and `skillEnd` methods. Refactored `TelemetryTrendCliIT`
(now 214 lines), `TelemetryTrendCliEdgesTest` (70 lines), `TelemetryIndexBuilderIT`
(116 lines), and `TelemetryTrendPerfIT` (59 lines) to import from the helper.
All four duplicate copies removed.

### FIND-003

Extracted `EpicDirectoryScanner` package-private helper from
`TelemetryIndexBuilder`. The scanner encapsulates the `plans/epic-XXXX/telemetry/`
directory walk, the epic ID regex, and the mtime-based state collection.
`TelemetryIndexBuilder` is now 219 lines and focuses on cache read/write plus
aggregation orchestration (SRP improvement).

## Verification

- `mvn verify` green after remediation
- Coverage thresholds met (95% line / 90% branch overall)
- All 991 tests pass
- No behaviour change — only structural refactoring
