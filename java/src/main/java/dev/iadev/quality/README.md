# `dev.iadev.quality` — SkillSizeLinter

Guard-rail that enforces **RULE-047-04** — every `SKILL.md` under
`java/src/main/resources/targets/claude/skills/` must either stay
below **500 lines** OR have a non-empty `references/` sibling
directory (at least one `.md` file other than `README.md`).

Introduced by [story-0047-0003](../../../../../plans/epic-0047/story-0047-0003.md)
as part of [EPIC-0047 — Skill Body Compression Framework](../../../../../plans/epic-0047/epic-0047.md).

## Classes

| Class | Responsibility |
| :--- | :--- |
| `LintFinding` (record) | One result per `SKILL.md` — path, line count, severity, references state, human-readable message. |
| `Severity` (enum) | Three tiers: `INFO` / `WARN` / `ERROR`. |
| `SkillSizeLinter` (static helper) | `lint(Path)` walks the tree; `errorFindings(List)` filters to `ERROR` only. |

## Tiers (RULE-047-04)

| Line count | `references/` sibling | Severity | Build effect |
| :--- | :--- | :--- | :--- |
| < 250 | any | `INFO` | silent pass |
| 250–500 | any | `WARN` | logged; never fails |
| > 500 | missing or empty or README-only | `ERROR` | **fails CI** |
| > 500 | contains ≥ 1 non-`README.md` markdown | `INFO` | silent pass (orchestrator carve-out allowed) |

## Tests

Three Maven tests run in the default `mvn test` scope (no profile
required, no external commands, total runtime well under 1 s):

| Test class | Purpose |
| :--- | :--- |
| `SkillSizeLinterTest` | 22 unit scenarios with `@TempDir` (TPP-ordered: nil → constant → scalar → collection → conditional → iteration; 3 boundary scenarios at 250, 500, 501; `_shared/` exclusion). |
| `LintFindingTest` | 5 unit scenarios for the record + enum contract. |
| `SkillSizeLinterAcceptanceTest` | Runs against the real source-of-truth tree; fails on **new** violations only. Tolerates entries in `audits/skill-size-baseline.txt`. |
| `SkillCorpusSizeAudit` | Corpus-wide aggregate check against RULE-047-07 (30,000-line cap). Soft-warn by default; hard-fail via `-Dskill.corpus.audit.enforce=true`. |

## Brownfield baseline

At the time this story landed, 25 existing `SKILL.md` files already
exceeded 500 lines without a non-empty `references/` sibling.
Enumerated in [`audits/skill-size-baseline.txt`](../../../../../../audits/skill-size-baseline.txt).

The acceptance test tolerates ONLY these paths. **NEW** violations
fail the build with `SKILL_SIZE_REGRESSION`. As stories 0047-0002
(flipped orientation; 5 orchestrator carve-outs) and 0047-0004 (KP
sweep; 5 largest knowledge packs) carve out existing offenders,
each PR MUST remove the corresponding lines from
`audits/skill-size-baseline.txt`. When the file is empty, the
brownfield phase is complete and the acceptance test becomes pure
green-field enforcement.

A third test (`SkillSizeLinterAcceptanceTest#baseline_stillMatchesReality_noStaleEntries`)
enforces the symmetric invariant: a SKILL.md that no longer
violates the threshold MUST be removed from the baseline so the
guard-rail stays tight over time.

## Debugging a failed `SkillSizeLinterAcceptanceTest`

If `mvn test -Dtest=SkillSizeLinterAcceptanceTest` reports
`SKILL_SIZE_REGRESSION` with a list of paths:

1. **Preferred:** shrink the offending `SKILL.md` back below 500
   lines by carving detail out to a `references/*.md` sibling
   (see ADR-0007 — Flipped Orientation) or to `_shared/`
   (see ADR-0011 — Shared Snippets Strategy).
2. **Acceptable:** add the carved-out file(s) to `references/` so
   the sibling becomes non-empty. The linter will then classify
   the `SKILL.md` as `INFO` again.
3. **Discouraged:** append the offending path to
   `audits/skill-size-baseline.txt`. This should only happen
   when the violation is intentional (e.g., a new orchestrator
   that legitimately needs > 500 lines; confirm with
   maintainers and reference the decision in the PR body).

## Debugging a failed `SkillCorpusSizeAudit`

Only fires when `-Dskill.corpus.audit.enforce=true` is set and
the total exceeds 30,000 lines. Resolution is always to carve
out — the threshold is not configurable without an ADR update to
RULE-047-07. Check the next pending story in EPIC-0047 and help
land it; the audit is fundamentally a ratchet that tightens with
each merged compression PR.

## Extending

- **New exclusions** (`_shared/`, etc.) go in `isSkillMdCandidate`
  in `SkillSizeLinter.java`.
- **Threshold changes** are encoded as `public static final`
  constants (`WARN_THRESHOLD_LINES`, `ERROR_THRESHOLD_LINES`);
  revisit annually per RULE-047-04 and update via a proper ADR
  + test change.
- **New severities** would require updating the enum and the
  `pickSeverity` method; unlikely to be needed.
