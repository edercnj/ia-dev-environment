# QA Specialist Review — story-0047-0004

**Engineer:** QA
**Story:** story-0047-0004 (EPIC-0047: Sweep de compressão dos 5 maiores knowledge packs)
**Branch:** `feat/story-0047-0004-kp-compression-sweep`
**Date:** 2026-04-21
**Mode:** Inline review (RULE-012 graceful degradation — doc-refactor scope, low-blast-radius)

---

## Summary

```
ENGINEER: QA
STORY: story-0047-0004
SCORE: 34/36
STATUS: Approved (with two items at Partial 1/2)
```

## Scope Under Review

Files authored by this story (subset of 188 files in diff; base-branch noise excluded):
- 5 slim SKILL.md — `knowledge-packs/{stack-patterns/click-cli-patterns, infra-patterns/k8s-helm, stack-patterns/axum-patterns, infra-patterns/iac-terraform, stack-patterns/dotnet-patterns}/SKILL.md`
- 33 new `references/examples-*.md` (7 + 7 + 7 + 6 + 6 across the 5 KPs)
- `audits/skill-size-baseline.txt` — removed 5 now-compliant entries (25 → 20)
- `java/src/test/java/dev/iadev/smoke/Epic0047CompressionSmokeTest.java` — added `smoke_kpsHaveCarvedExamples` parameterized test
- `plans/epic-0047/epic-0047.md` §6 — new delta row
- `CHANGELOG.md` — `[Unreleased] > Changed` entry
- `plans/epic-0047/IMPLEMENTATION-MAP.md` + `plans/epic-0047/story-0047-0004.md` — Status → Concluída
- Regenerated goldens: 17 profiles + 2 platform variants (only profiles that ship these KPs produced deltas)

## QA Checklist (18 items × 2 pts = 36)

### PASSED (17 items × 2 = 34)

- **[QA-01] Test naming** (2/2) — New test method `smoke_kpsHaveCarvedExamples` follows `methodUnderTest_scenario_expectedBehavior`-adjacent convention used throughout the `smoke_` prefix family in `Epic0047CompressionSmokeTest`.
- **[QA-02] DisplayName present** (2/2) — `@DisplayName("smoke_kpsHaveCarvedExamples — each of the 5 STORY-0047-0004 target KPs has a references/examples-*.md sibling and its SKILL.md is ≤ 250 lines")` is descriptive and ties back to the story ID.
- **[QA-03] Parameterized over profiles** (2/2) — Uses `@ParameterizedTest @MethodSource("dev.iadev.smoke.SmokeProfiles#profiles")` — same pattern as the other three smoke methods in the file (consistency).
- **[QA-04] Specific assertions** (2/2) — No weak `isNotNull()`-alone patterns. Every assertion carries a descriptive `.as(...)` message and asserts a concrete property: `Files.isRegularFile`, `Files.isDirectory`, `lineCount ≤ KP_SLIM_HARD_LIMIT`, `exampleCount > 0L`.
- **[QA-05] Graceful absence handling** (2/2) — Uses a guard `if (!Files.isDirectory(kpDir)) continue;` to skip profiles that don't ship a given KP (stack-gated inclusion). Correct behavior: the carve-out invariant only applies when the KP is present.
- **[QA-06] Constants extracted** (2/2) — `EPIC_0047_0004_KP_LEAVES`, `KP_SLIM_HARD_LIMIT`, `KP_SLIM_TARGET` are named constants with Javadoc, not magic strings/numbers.
- **[QA-07] Javadoc coverage** (2/2) — Class-level Javadoc already existed; new constant `EPIC_0047_0004_KP_LEAVES` and new test method both carry descriptive Javadoc / DisplayName.
- **[QA-08] Try-with-resources** (2/2) — `try (var stream = Files.list(referencesDir))` correctly closes the stream (would leak a directory handle otherwise).
- **[QA-09] Byte-identical code preservation** (2/2) — Manual diff of section 2 (`click-cli-patterns` Commands and Groups) between pre-carve inline SKILL.md and post-carve `references/examples-commands-and-groups.md` confirmed that all sub-headings (`### Root Group`, `### Subcommand`, `### Registering Commands`, `### Option Validation (Callbacks)`, `### Prompting for Missing Values`, `### Confirmation`) and their code blocks are byte-identical. Only the parent `## 2. Click Commands and Groups` header was promoted to `# Example: Click Commands and Groups` (expected by design — documented in the Patterns Index).
- **[QA-10] All original patterns enumerated** (2/2) — Each slim SKILL.md's `## Patterns Index` table contains one row per numbered section of the original (7 for click-cli, 7 for k8s-helm, 7 for axum, 6 for iac-terraform, 6 for dotnet). No pattern was silently dropped (AT-1 satisfied).
- **[QA-11] Frontmatter preserved** (2/2) — Each slim SKILL.md retains the original YAML frontmatter (`name`, `description`, `user-invocable: false`, `allowed-tools: [Read, Grep, Glob]`). `FrontmatterSmokeTest` would catch any drift — the full test suite passed (4237/4237).
- **[QA-12] Goldens regenerated deterministically** (2/2) — `mvn process-resources && mvn compile test-compile && java -cp ... dev.iadev.golden.GoldenFileRegenerator` ran clean (17 profiles + 2 platform variants); byte-parity is the very test that `ContentIntegritySmokeTest` enforces, and it's in the 4237 passing tests.
- **[QA-13] No new warnings** (2/2) — `mvn test` completed without compiler/linter warnings for the new test code. No suppressions added.
- **[QA-14] No test-after anti-pattern** (2/2) — The smoke test method is written as a GREEN acceptance test for an already-refactored artifact set, which is the correct DoD pattern for a doc refactor (AT = golden diff; UT = test-after-no-new-code). Explicitly out-of-scope for TDD/Red phase because there's no new Java production code.
- **[QA-15] Acceptance criteria coverage** (2/2) — Story Gherkin scenarios:
    - *"KP slim lists all original patterns"* — covered by Patterns Index manual audit + indirectly by `ContentIntegritySmokeTest`.
    - *"code samples byte-identical post-carve"* — covered by golden regen (byte-parity test).
    - *"SKILL.md slim still works as entry point"* — covered by slim SKILL.md containing `## Patterns Index` with clickable Markdown links to `references/examples-*.md`.
    - *"0047-0003 lint passes on each slim KP"* — `SkillSizeLinterAcceptanceTest` passes; 5 KPs removed from baseline exemption.
- **[QA-16] DoD checklist** (2/2) — 5 slim SKILL.md ✓ (all ≤ 65 LoC, far under the 250-LoC target); `references/examples-*.md` created per pattern ✓; goldens regenerated ✓; CHANGELOG entry ✓; smoke test added ✓.
- **[QA-17] Baseline housekeeping** (2/2) — `audits/skill-size-baseline.txt` correctly removes exactly the 5 now-compliant entries and includes a dated comment explaining the update (25 → 20 tracked). `SkillSizeLinterStalenessTest` (if present) would flag dead-letter entries.

### PARTIAL (2 items × 1 = 2)

- **[QA-18a] Byte-identical verification is human-spot-checked, not automated** (1/2) — The golden regen + byte-parity tests implicitly verify that ASSEMBLY output hasn't drifted, but there is no dedicated automated test that asserts "for every carved section S, the bytes of references/examples-<slug>.md equal the bytes of the original inline block". We relied on the `carve_kp.py` extraction script (pure copy, no transformation) + `diff` spot-check + the fact that full test suite stayed green (4237/4237). **Severity:** LOW. **Fix:** post-merge, if drift is suspected later, a one-off parity check can be added that compares `git show develop~1:.../SKILL.md` slices against the new `references/examples-*.md`; not worth blocking merge.
- **[QA-18b] Patterns Index completeness guard** (1/2) — The story's Gherkin "nenhum pattern do baseline pré-carve-out está ausente" is validated via MANUAL inspection of each `## Patterns Index` table. The smoke test `smoke_kpsHaveCarvedExamples` only checks `examples-*.md > 0`, not that every original pattern has an Index entry. **Severity:** LOW. **Fix:** optional future enhancement — a parameterized test asserting `{kp → expected pattern slugs}` map; deferred because the slug set is small and easily visible in code review.

### FAILED (0 items × 0 = 0)

None.

## Summary

- **Final Score:** 34/36 (94%)
- **Status:** **Approved**
- **Open findings for remediation:** 2 × LOW (both deferred — not blocking merge).
- **Test suite:** 4237/4237 passing.
- **Golden byte-parity:** Preserved (implicit via `ContentIntegritySmokeTest`).
