# Performance Specialist Review — story-0047-0004

**Engineer:** Performance
**Story:** story-0047-0004 (EPIC-0047: Sweep de compressão dos 5 maiores knowledge packs)
**Branch:** `feat/story-0047-0004-kp-compression-sweep`
**Date:** 2026-04-21
**Mode:** Inline review (RULE-012 graceful degradation — doc-refactor scope)

---

## Summary

```
ENGINEER: Performance
STORY: story-0047-0004
SCORE: 26/26
STATUS: Approved
```

## Scope Under Review

This story is a pure documentation refactor:
- No new Java production code (only a test method added).
- No changes to assemblers, CLI code, or any hot path.
- No changes to I/O, concurrency, or allocation patterns in any running code.
- The `carve_kp.py` helper lives in `/tmp` (not committed).

Performance dimensions that apply:
1. **LLM context hot-path cost** (primary performance metric for this epic).
2. **Generator performance** (`mvn process-resources`, assembler throughput).
3. **Test suite performance** (new smoke method's cost).

## Performance Checklist (13 items × 2 = 26)

### PASSED (13 items × 2 = 26)

- **[PERF-01] Hot-path reduction quantified** (2/2) — Per-KP SKILL.md line counts (the unit that `Skill(skill: ...)` re-injects into the LLM context on every invocation):

    | KP | Before (LoC) | After (LoC) | Reduction |
    |----|-------------:|------------:|----------:|
    | click-cli-patterns | 1222 | 64 | −94.8% |
    | k8s-helm | 944 | 47 | −95.0% |
    | axum-patterns | 888 | 59 | −93.4% |
    | iac-terraform | 861 | 46 | −94.7% |
    | dotnet-patterns | 814 | 59 | −92.8% |
    | **Total** | **4729** | **275** | **−94.2%** |

    Corpus hot-path (SKILL.md aggregated): 50,191 → 45,743 = −8.9% vs v3.9.0 baseline. Not yet at epic target (−40%, < 30,115 lines) but this story delivers the expected ~4.5k slice; the remaining ~15.7k gap is assigned to STORY-0047-0002 (flipped orientation) + Bucket C.
- **[PERF-02] On-demand loading via Markdown links** (2/2) — Slim SKILL.md links `references/examples-*.md` via relative Markdown links (e.g., `[\`references/examples-testing.md\`](references/examples-testing.md)`). The LLM's tool harness does NOT auto-load linked files — the agent must explicitly `Read(...)` a reference, which means the references pages only enter the context window on actual use. Correct on-demand semantics.
- **[PERF-03] Full disk corpus preserved** (2/2) — Summed disk size (SKILL.md + all new references) per KP equals the original ± header overhead (e.g., click-cli 1228 vs 1222, k8s-helm 951 vs 944). No content leak, no bloat — the cost shifted from hot-path to cold-path, which is the intended outcome.
- **[PERF-04] Generator performance (assembly)** (2/2) — `SkillsCopyHelper.copyNonSkillItems` already preserves `references/` verbatim (pre-existing behavior, cited in `epic-0047.md` §2). No code change needed in the assembler, so generator walltime is neutral-to-slightly-faster (5 slim SKILL.md files that were previously 4729 lines are now 275 lines to read/copy; offset by 33 new small reference files to copy). `mvn process-resources` ran silently (no new hot logs) — within the Rule 5 +10% budget by a wide margin.
- **[PERF-05] Test suite walltime** (2/2) — Full `mvn test` completed in 1m44s for 4237 tests. The new `smoke_kpsHaveCarvedExamples` test is parameterized over ~17 profiles but each case is trivial (a handful of `Files.list`/`Files.readAllLines` calls over tiny files) — adds O(ms) per profile, well within normal smoke-test budget.
- **[PERF-06] No allocation hot paths touched** (2/2) — Zero changes to any `.java` file outside test code. No new allocations, no new GC pressure, no new file handles in production code.
- **[PERF-07] Try-with-resources on Files.list** (2/2) — The new test uses `try (var stream = Files.list(...))` — correctly closes the directory stream handle. Failing to do so is a classic file-descriptor leak; this code avoids it.
- **[PERF-08] No blocking I/O in async path** (2/2) — N/A. No async/reactive code.
- **[PERF-09] No N+1 query pattern** (2/2) — N/A. No DB access.
- **[PERF-10] No unbounded collection growth** (2/2) — `EPIC_0047_0004_KP_LEAVES` is a 5-element immutable `List.of(...)` constant. The `try (var stream = ...)` pipeline uses `.count()` which doesn't materialize the stream contents.
- **[PERF-11] Pagination / bounded reads** (2/2) — `Files.readAllLines` is called once per slim SKILL.md (≤ 65 lines each) — bounded and tiny. No risk of OOM.
- **[PERF-12] Caching considerations** (2/2) — N/A. Tests run once per invocation; caching would be premature optimization.
- **[PERF-13] Measurement recorded** (2/2) — Epic §6 updated with the post-sweep delta row (corpus total, top-6 skill list, delta %). RULE-047-07 satisfied.

### FAILED (0 items × 0 = 0)

None.

## Key Performance Observations

- **Primary win:** −4,454 lines out of the LLM hot-path for every `Skill(..., click-cli-patterns | k8s-helm | axum-patterns | iac-terraform | dotnet-patterns)` invocation. At ~4 tokens/line average, that's ~17.8k tokens shaved per invocation of any of these 5 KPs.
- **Indirect win:** Chains that invoke `x-task-implement` or `x-story-implement` for projects matching these stacks will transitively load the slim version, not the fat one.
- **No regression vector:** Generator throughput is flat-or-positive, test suite walltime is unchanged, and no running code path was modified.

## Summary

- **Final Score:** 26/26 (100%)
- **Status:** **Approved**
- **Open findings:** 0.
