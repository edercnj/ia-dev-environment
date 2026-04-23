# Tech Lead Review

> **Story ID:** EPIC-0050 (aggregate — 9 stories)
> **PR:** #599 (`epic/0050 → develop`)
> **Date:** 2026-04-23
> **Score:** 48/55
> **Template Version:** 1.0

## Decision

**NO-GO** (automatic — coverage below threshold; see Verdict for nuance)

## Section Scores

| Section | ID | Score | Max Score |
| :--- | :--- | :--- | :--- |
| Clean Code | A | 4 | 5 |
| SOLID | B | 5 | 5 |
| Architecture | C | 5 | 5 |
| Framework Conventions | D | 5 | 5 |
| Tests | E | 3 | 5 |
| TDD Process | F | 3 | 5 |
| Security | G | 5 | 5 |
| Cross-File Consistency | H | 4 | 5 |
| API Design | I | 5 | 5 |
| Events/Messaging | J | 5 | 5 |
| Documentation | K | 4 | 5 |

48/55 | Status: Partial

## Test Execution Results (EPIC-0042 mandatory gates)

| Gate | Result | Evidence |
|---|---|---|
| Test suite (`mvn verify`) | **PASS** | 523 tests, 0 failures, 0 errors, 0 skipped |
| Line coverage (target ≥ 95%) | **FAIL** | 94.73% (5675/5991) — deficit 0.27 pp (16 uncovered lines) |
| Branch coverage (target ≥ 90%) | **FAIL** | 89.19% (1518/1702) — deficit 0.81 pp (~14 uncovered branches) |
| Instruction coverage | INFO | 94.76% |
| Method coverage | INFO | 98.94% |
| Class coverage | INFO | 100.00% |
| Smoke tests (Epic0047Compression + CliModesSmokeTest + ProfileRegistrationIntegrityTest + OutputDirectoryIntegrityTest + Onboarding + GoldenFileTest + PlatformGoldenFileTest + ...) | **PASS** | All smoke suites included in the 523-test run; zero failures |

**Important context:** EPIC-0050 modified **zero Java main-source files** (0 `java/src/main/**/*.java` in the diff). Therefore the coverage deficit is **pre-existing on `develop`** and not caused by this PR. The automatic NO-GO rule (per skill spec) still triggers because the skill enforces an absolute gate, but the Verdict section below records this as not blocking if the reviewer consciously overrides for a docs/metadata epic.

## Cross-File Consistency

**Scope observed:** 373 files changed (3293 +, 618 −). Breakdown:
- 330 regenerated golden fixtures (derived)
- 21 source-of-truth SKILL.md (frontmatter / Agent() / Skill() edits)
- 14 agent metadata files (Recommended Model tier)
- 1 new rule: `23-model-selection.md` (127 lines)
- 1 new script: `scripts/audit-model-selection.sh`
- 1 CI workflow step
- 1 test regex update (`XReviewSkillTemplateTest`)
- 1 root CLAUDE.md rules-table line
- 2 plan artifacts (execution plan + execution report)
- 1 unrelated `.gitignore` change (see Critical #3)

**Consistency assessment:**

- ✅ Frontmatter `model:` declarations across 17 skills are uniform (between `name:` and `description:`; tier matches Rule 23 matrix).
- ✅ Agent metadata changes uniform across 14 agents (format `**<Tier>** — <rationale>`; all cite Rule 23 RULE-004).
- ✅ `Agent(subagent_type: "general-purpose", model: "...", description: "...", prompt: "...")` block shape is consistent across x-story-plan (5 dispatches), x-arch-plan (2 dispatches), x-test-plan (1 dispatch). All declare `model:` explicitly.
- ✅ `Skill(skill: "...", model: "...", args: "...")` order is consistent: `skill:` → `model:` → `args:` across x-epic-implement, x-story-implement, x-review, x-task-implement.
- ⚠️ **Minor inconsistency:** story-0005 and story-0006 insert `prompt: "<see the blockquote below>"` as a literal placeholder string. This satisfies Rule 23 structure but the runtime interpreter will not actually use that value — the blockquote below is what drives the subagent. Pragmatic, but a reader could be confused. Not blocking.
- ⚠️ **Partial test coverage of Rule 23 contract in JUnit:** `XReviewSkillTemplateTest.java` updates regex for 3 of the 9 `x-review-*` Skill() calls (qa, perf, db). The other 6 calls (obs, devops, data-modeling, security, api, events) rely only on the bash audit script. Not inconsistent per se, but uneven coverage by test layer.

## Critical Issues

| # | File | Line | Description | Impact |
| :--- | :--- | :--- | :--- | :--- |
| 1 | project-wide coverage (JaCoCo) | — | Line coverage 94.73% < target 95% (by 0.27 pp, 16 lines); Branch coverage 89.19% < target 90% (by 0.81 pp). | Triggers automatic NO-GO per `x-review-pr` mandatory gate (EPIC-0042). **Pre-existing on develop** — not caused by this PR (0 main-source Java files changed). |
| 2 | — (commit `f699d63a8` on epic/0050 only) | N/A | Commit `chore(.gitignore): add telemetry events file to ignore list` is included in this PR but is outside EPIC-0050 scope (authored 2026-04-23 10:00, not tied to any story). It adds `plans/epic-0048/telemetry/events.ndjson` to `.gitignore`. | Scope pollution. A clean PR should not ship unrelated commits. Mitigation: either document in the PR body, or cherry-pick into a standalone hotfix PR to develop. |
| 3 | plans/epic-0050/reviews/review-*-story-*.md | — | **Specialist review reports do NOT exist** under `plans/epic-0050/reviews/`. `x-review` (9 specialist agents) was never executed during EPIC-0050. The execution was done via the orchestrator shortcut (direct Read/Edit/Bash) that bypassed the full `Skill(x-story-implement, ...)` Phase 3 review loop. | Specialists (Security, QA, Performance, Database, Observability, DevOps, API, Event, Data-Modeling) did not sign off on any of the 9 stories. Mitigation: run `/x-review 0050` (or per-story) before merging. |

## Medium Issues

| # | File | Line | Description | Recommendation |
| :--- | :--- | :--- | :--- | :--- |
| 1 | scripts/audit-model-selection.sh | 98-102 | Check C uses a regex hard-coded to exclude `x-internal-*`, `x-parallel-eval`, `x-pr-watch-ci` as exempt callees. This is inline in the script, not sourced from Rule 23. | Extract the exempt-callee list to a comment block near the top of the script OR cross-reference the Rule 23 Exceptions section by line number to make the link explicit. |
| 2 | java/src/main/resources/targets/claude/skills/core/plan/x-story-plan/SKILL.md | 297-470 | The 5 subagent sections now declare formal `Agent(...)` blocks, but the existing blockquote prompts (~200 lines) remain in natural-language form. The dual representation works but duplicates intent: the Agent block says "prompt: <see the blockquote below>" and the blockquote IS the prompt. | Either merge the blockquote INTO the Agent block's `prompt:` field (verbose but explicit) OR add a one-line note above each blockquote: `<!-- This blockquote is the prompt: of the Agent() above. -->` |
| 3 | XReviewSkillTemplateTest.java | 104-137 | Tests updated for 3 of 9 review-* Skill() call regexes. Remaining 6 calls (`x-review-obs`, `x-review-devops`, `x-review-data-modeling`, `x-review-security`, `x-review-api`, `x-review-events`) have no corresponding test enforcing `model: "sonnet"`. | Parametrize the existing test to cover all 9 review-* skills (one test method each, or `@ParameterizedTest` with the 9 skill names as source). |
| 4 | scripts/audit-model-selection.sh | 148-151 | Check D greps for `Adaptive` case-sensitively. Any valid user text containing the word "Adaptive" in a different role (e.g., "Adaptive tier", "non-Adaptive selection") would also trigger. | Tighten the regex to match the exact anti-pattern: `grep -rE "Recommended Model:\s*Adaptive"` (the contextual marker). |

## Low Issues

| # | File | Line | Description | Suggestion |
| :--- | :--- | :--- | :--- | :--- |
| 1 | java/src/main/resources/targets/claude/rules/23-model-selection.md | 34 | Matrix row for `architect` agent lists example as "Deep Planner" tier but `.claude/agents/core/architect.md` declares `**Opus**` under "Recommended Model". Both align to Opus — no bug — but the Matrix uses tier label "Deep Planner" while the agent uses model name "Opus". | Consider adding a parenthetical: `Deep Planner (Opus)` in the Matrix rationale column for consistency with the agent-side vocabulary. |
| 2 | plans/epic-0050/reports/epic-execution-plan-0050.md | — | Document is labelled "dry-run" but was actually committed after real execution. Historical value only. | Add a header note: `*Note: this plan was written for dry-run; kept for historical reference.*` |
| 3 | CLAUDE.md (repo root) | ~80 | Rules-table count updated to `Total: 11 rules` but project also has Rules 14, 19, 21, 22 that are NOT listed in this table (they're in `.claude/README.md` which IS auto-generated). The root CLAUDE.md's table is hand-maintained and selectively lists "core" rules. | Not a regression from this PR — pre-existing curation. Document the selection criteria in a one-line note above the table. |
| 4 | java/src/main/resources/targets/claude/agents/developers/java-developer.md | 67 | Section renamed from `## Adaptive Model Assignment` to `## Per-Task Tier Guidance`. Good. But the renamed section talks about tier selection for Skill delegations performed BY this agent — which conflates agent-level model (Sonnet, per Recommended Model) and delegated-skill-level model (variable). Reader might be confused about which scope the table describes. | Add a one-line preamble: `*The tiers below apply to Skill() delegations this agent emits. The agent itself runs on the tier declared in "Recommended Model" above.*` |

## TDD Compliance Assessment

EPIC-0050 is **not a TDD-native epic**. Of the 9 stories:

- 8 are metadata/documentation changes (frontmatter, agent tier strings, rule file authoring, `model:` param propagation) — TDD Red-Green-Refactor does not apply because no business logic is introduced.
- 1 story (0009 — CI audit script) ships executable bash. It was **not developed test-first**: the audit script was written, run against the post-0008 state, fixed (to skip prose-in-backticks), and then a self-test was performed (inject violation → confirm exit 1; restore → confirm exit 0). That is test-after, not Red-Green-Refactor.

This is acceptable for the nature of the epic (metadata-only), and the Rule 05 coverage thresholds for TDD apply to Java code — none was added. **Score reflects the mismatch:** 3/5 is appropriate. A follow-up story could add a JUnit harness around the bash audit script (e.g., a Java test that invokes it in a synthetic repo fixture) but is not required by the epic's scope.

## Specialist Review Validation

**Specialist reviews were not executed for this epic.** See Critical Issue #3. `/x-review` was skipped during orchestration because the loop used direct Read/Edit/Bash instead of dispatching `Skill(x-story-implement, ...)` whose Phase 3 normally invokes `x-review` automatically.

Without specialist reports to cross-validate against, the Tech Lead review is the only formal review this PR received. This reduces confidence in:

- **Security review** (Security Engineer) — did not assess the new `scripts/audit-model-selection.sh` for path traversal, permission escalation, or injection surfaces.
- **Performance review** (Performance Engineer) — did not assess the bash grep-based audit for N+1 file I/O or redundant work.
- **QA review** (QA Engineer) — did not assess test coverage at the model-selection-enforcement level.
- **Others** (Database / Observability / DevOps / API / Event / Data-Modeling) — largely N/A for this epic (no DB, metrics, API, or event changes).

**Recommendation:** run `/x-review 0050` before merging OR accept the single-reviewer (Tech Lead only) risk explicitly.

## Verdict

**NO-GO** per the skill's automatic coverage gate (EPIC-0042): line 94.73% < 95% and branch 89.19% < 90%. This is the letter of the rule.

**Nuance the reviewer should consider before acting:**

1. **Coverage deficit is pre-existing on `develop`** — EPIC-0050 changed 0 Java main-source files (metadata-only epic). The gate triggers because it's absolute, not because this PR regressed coverage.
2. **Score 48/55 (87%)** is above the typical GO threshold (~84%) on rubric alone. The quality of the deliverables is high.
3. **Missing specialist review (Critical #3)** is a gap that this Tech Lead review alone cannot fill. If the reviewer overrides the automatic NO-GO to merge, the specialist reviews SHOULD be run post-merge as follow-up, or the decision should be accepted with risk acknowledged.
4. **Rogue `.gitignore` commit (Critical #2)** is cosmetic — not blocking, but a clean operator would split it off.

**Options for the operator:**

- **A. Address the automatic NO-GO by closing the coverage gap** on `develop` first (a separate story), then merge EPIC-0050 once develop is at ≥95%/≥90%. Most rigorous path.
- **B. Override the automatic NO-GO** because the deficit is pre-existing and EPIC-0050 neither caused nor fixed it. Document the override in the PR merge comment. Run `/x-review 0050` as follow-up.
- **C. Merge-blocker-split:** cherry-pick the rogue `.gitignore` commit to a standalone hotfix PR to develop; split EPIC-0050 merge from that concern; run specialist reviews; then merge.

Default recommendation (per skill as written): **Option A** or **C**. The skill mandates NO-GO when coverage is below threshold; that's what was written, so that's the decision.

The concrete next step, per the skill's Step 8 (auto-remediation), would be to dispatch a `COVERAGE_GAP` remediation agent. That agent would try to add tests for the uncovered lines/branches on `develop` — not on EPIC-0050 — which is out of scope for this PR. Hence Option A is the correct path if following the skill strictly.
