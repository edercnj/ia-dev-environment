# Performance Specialist Review — story-0040-0004

**ENGINEER:** Performance
**STORY:** story-0040-0004
**PR:** #414
**SCORE:** 24/26
**STATUS:** Partial

---

## Context

Pipeline-time assembly (`mvn process-resources`) plus hook invocation at runtime via `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-*.sh`. Story DoD bounds `mvn process-resources` to not increase by more than 500 ms.

## PASSED

- **[PERF-01] No N+1 or unbounded work** (2/2) — `HooksAssembler.copyTelemetryScripts` iterates a fixed list of 7 filenames with single `Files.copy` + `setPosixFilePermissions` per entry. Constant work per project generation.
- **[PERF-02] No synchronous blocking I/O in hot paths** (2/2) — Assembly runs in the Maven build, not the app runtime. Telemetry hook timeouts are set to 5 seconds at registration (story spec); hooks themselves are out-of-scope for this PR.
- **[PERF-03] No string concatenation in loops** (2/2) — JSON emission uses `StringBuilder` throughout (`HookConfigBuilder`, `JsonSettingsBuilder`). No `+=` in loops.
- **[PERF-04] Collection copies minimized** (2/2) — `List.copyOf(written)` called once at end of `HooksAssembler.assemble`; `TELEMETRY_SCRIPTS` is a static `List.of(...)` (already immutable).
- **[PERF-05] No autoboxing in hot paths** (2/2) — Counters are `int` primitives; no boxing overhead.
- **[PERF-06] No resource leaks** (2/2) — `Files.copy`/`Files.writeString` are static utilities that manage their own streams; no explicit stream opened in the new code paths.
- **[PERF-07] Timeout bounds on hooks** (2/2) — All 5 telemetry entries declare `"timeout": 5` (seconds). Aligns with story §5.1 and RULE-004 (fail-open).
- **[PERF-08] Idempotency without recomputation penalty** (2/2) — `assemble_twice_identicalOutput` test confirms deterministic output; regeneration cost equals single generation cost (no caching required — emission is O(n) in profile file count).
- **[PERF-09] No reflection in hot paths** (2/2) — Pure Java record + `StringBuilder` + `Files.*`. Zero reflection introduced.
- **[PERF-10] No regex compilation in loops** (2/2) — No regex introduced. `deduplicate` pre-existing, uses `LinkedHashSet`.
- **[PERF-11] Reasonable file I/O footprint** (2/2) — 7 additional small files (1-8 KB each) per project = ~50 KB total; well under the 500 ms DoD budget for a typical SSD filesystem.

## PARTIAL

- **[PERF-12] Bench data for DoD claim** (1/2) — Story DoD §4 "Performance: `mvn process-resources` não aumenta > 500ms". The change is plausibly well under that (+8 telemetry copies, +100 lines JSON emission) but there is no explicit benchmark captured in the PR. **Fix:** run `time mvn -q process-resources` on a sample project before/after and attach the delta in the PR description or in `results/benchmarks/`. Low priority — likely passes by a wide margin.

- **[PERF-13] StringBuilder initial capacity** (1/2) — `JsonSettingsBuilder.build` and `HookConfigBuilder.appendHooksSection` use default `StringBuilder()` (16 chars). Telemetry output adds ~1.5 KB of JSON. Not a real bottleneck at pipeline-time, but a 2-line nit: `new StringBuilder(4096)` avoids 2-3 intermediate array copies. Pre-existing nit — not introduced by this PR.

## FAILED

- None.

## Severity Distribution

- CRITICAL: 0
- HIGH: 0
- MEDIUM: 0
- LOW: 2 (benchmarks not captured; StringBuilder capacity nit)

## Notes

The hook shell scripts themselves (out-of-scope for story-0040-0004) already include `flock` and async emission patterns per story-0040-0003 design. This PR only wires them in; runtime perf behavior is owned by the shell scripts.
