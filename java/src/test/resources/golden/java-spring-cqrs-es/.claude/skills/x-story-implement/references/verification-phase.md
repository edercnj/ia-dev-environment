# Verification Phase Reference

> **Context:** This reference details Phases 3-8 of `x-story-implement`
> (Documentation, Specialist Review, Remediation, Commit + PR, Tech Lead
> Review, and Final Verification). Phase numbering in `x-story-implement`
> follows the README: each phase is a distinct stage in the lifecycle.
>
> **Step numbering note:** historical step IDs in this file use `3.x`
> prefixes from a consolidated-phase draft. They have been retained
> verbatim to keep deep links stable; the table below maps each step
> ID to the current phase it belongs to.
>
> | Step ID | Current Phase | Subject |
> |---------|---------------|---------|
> | 3.1 | Phase 8 (Final Verification) | Coverage Consolidation |
> | 3.2 | Phase 8 (Final Verification) | Cross-File Consistency Check |
> | 3.3 | Phase 3 (Documentation) | Documentation Update |
> | 3.4 | Phase 4 (Specialist Review) | Specialist Review via x-review |
> | 3.5 | Phase 5 (Remediation + Fixes) | Remediation tracking and fixes |
> | 3.6 | Phase 7 (Tech Lead Review) | Tech Lead review via x-review-pr |
> | 3.7 | Phase 6 (Commit + PR) | Story-level PR (auto-approve mode) |
> | 3.8 | Phase 8 (Final Verification) | DoD checklist + cleanup |

## Phases 3-8 -- Documentation, Review, Remediation, PR, Tech Lead Review, Final Verification

These phases execute after all tasks have approved/merged PRs.

**Skip condition:** If `--skip-verification` is passed, skip the Phase 8 substeps (3.1, 3.2, 3.8 below) with log `"Phase 8 verification skipped (--skip-verification)"`. Phases 3-7 still execute.

### Step 3.1 -- Coverage Consolidation

1. Run `{{TEST_COMMAND}}` and `{{COVERAGE_COMMAND}}` across all story files
2. Validate coverage thresholds: line >= 95%, branch >= 90%
3. If below thresholds: identify gaps and emit WARNING with specific files/methods

### Step 3.2 -- Cross-File Consistency Check

1. Verify error handling patterns are uniform across classes of the same role
2. Verify constructor patterns, return types, and internal type definitions follow the same shape within a module
3. Inconsistency across files of the same role is a MEDIUM-severity violation
4. Report findings with file paths and specific inconsistencies

### Step 3.3 -- Documentation Update

Read the `interfaces` field from the project identity to determine which documentation generators to invoke.

**Interface Dispatch:**

| Interface | Generator | Output |
|-----------|-----------|--------|
| `rest` | OpenAPI/Swagger generator | `contracts/api/openapi.yaml` |
| `grpc` | gRPC/Proto documentation generator | `contracts/api/grpc-reference.md` |
| `cli` | CLI documentation generator | `contracts/api/cli-reference.md` |
| `graphql` | GraphQL schema documentation generator | `contracts/api/graphql-reference.md` |
| `websocket`, `kafka`, `event-consumer`, `event-producer` | Event-driven documentation generator | `contracts/api/event-reference.md` |

If no documentable interfaces configured: skip interface generators with log `"No documentable interfaces configured"`. Always generate changelog entry.

**Changelog Entry:**
- Read commits since branch point (`git log develop..HEAD --oneline`)
- Generate Conventional Commits summary by type (feat, fix, refactor, test, docs, chore)
- Append to CHANGELOG.md

**Architecture Document Update (Recommended):**
If an architecture plan exists at `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`:
1. Invoke `x-arch-update` via the Skill tool (Rule 13 — INLINE-SKILL pattern):

       Skill(skill: "x-arch-update", args: "plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md")

   This incrementally updates `steering/service-architecture.md`.
2. New components, integrations, flows, and ADR references are added to the appropriate sections
3. If `steering/service-architecture.md` does not exist, create it from the template

### Step 3.4 -- Review (invoke x-review via Skill tool)

Invoke the `x-review` skill via the Skill tool (Rule 13 — INLINE-SKILL pattern):

    Skill(skill: "x-review", args: "{STORY_ID}")

The review skill launches its own parallel subagents (one per specialist engineer), each reading their own knowledge pack.

**Template Reference (RULE-007):** Instruct each of the 8 specialist subagents: "Read template at `.claude/templates/_TEMPLATE-SPECIALIST-REVIEW.md` for required output format." If the template file does not exist, log `"WARNING: Template _TEMPLATE-SPECIALIST-REVIEW.md not found, using inline format"` and proceed with existing inline format as fallback (RULE-012).

If an architecture plan was generated in Phase 1, provide it as additional context to reviewers.

Collect the consolidated review report with scores and severity counts.

**Consolidated Review Dashboard (RULE-006):**
After collecting all specialist review results, generate a consolidated dashboard:
1. Read template at `.claude/templates/_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md` for required output format (RULE-007). If not found, use inline format as fallback (RULE-012).
2. Aggregate scores from all specialists into a single dashboard.
3. Save to `plans/epic-XXXX/reviews/dashboard-story-XXXX-YYYY.md`.

### Step 3.5 -- Fixes + Remediation

1. **Remediation Tracking (RULE-006):**
   - Read template at `.claude/templates/_TEMPLATE-REVIEW-REMEDIATION.md` for required output format (RULE-007). If not found, use inline format as fallback (RULE-012).
   - Map open findings from the review dashboard to remediation items.
   - For each finding: record original finding, assigned fix action, status (Open/Fixed/Deferred/Accepted), and resolution notes.
   - Save to `plans/epic-XXXX/reviews/remediation-story-XXXX-YYYY.md`.
2. Fix ALL failed items from review (every specialist must reach STATUS: Approved)
3. For each fix, follow TDD discipline: write/update the test FIRST, then apply the fix
4. Use atomic commits via `/x-git-commit` for fixes
5. Run `{{COMPILE_COMMAND}}` + `{{TEST_COMMAND}}`
6. Update remediation tracking: mark fixed items as "Fixed" with commit reference.

### Step 3.6 -- Tech Lead Review

Invoke the `x-review-pr` skill via the Skill tool (Rule 13 — INLINE-SKILL pattern):

    Skill(skill: "x-review-pr", args: "{STORY_ID}")

Requires all items passing for GO. If NO-GO, fix all failed items and re-review (max 2 cycles).

**Dashboard Update (RULE-006):**
After the Tech Lead review completes, update the consolidated review dashboard at `plans/epic-XXXX/reviews/dashboard-story-XXXX-YYYY.md` with Tech Lead findings.

### Step 3.7 -- Story-Level PR (Auto-Approve Mode Only)

If `--auto-approve-pr` is active:
1. Push parent branch: `git push -u origin feat/story-XXXX-YYYY-desc`
2. Create story-level PR via `gh pr create --base develop` with:
   - Title: `feat(story-XXXX-YYYY): {story title}`
   - Body: consolidated review summary, task list with PR links, coverage report
   - This PR requires human review -- it is NEVER auto-merged (RULE-004)

If NOT `--auto-approve-pr`: skip (individual task PRs already target develop).

### Step 3.8 -- Final Verification + Cleanup

1. Update README if needed
2. Update IMPLEMENTATION-MAP: find story row, update Status to `Concluida`
3. Update Story File Status: `Pendente` -> `Concluida`, mark completed sub-tasks `[x]`
4. Jira Status Sync (conditional, non-blocking): transition to "Done"
5. Update execution-state.json: set task statuses to DONE, storyStatus to COMPLETE
6. Run DoD checklist:
   - [ ] All task PRs approved/merged
   - [ ] Coverage >= 95% line, >= 90% branch
   - [ ] Zero compiler/linter warnings
   - [ ] Commits show test-first pattern
   - [ ] Acceptance tests exist and pass (AT-N GREEN)
   - [ ] Tests follow TPP ordering (simple to complex)
   - [ ] Story markdown file updated with Status: Concluida
   - [ ] IMPLEMENTATION-MAP Status column updated
   - [ ] At least 1 automated test validates primary acceptance criterion
   - [ ] Smoke test passes (if testing.smoke_tests == true)
7. Conditional DoD items: contract tests, event schemas, compliance, gateway config, proto/GraphQL compat, threat model
8. Post-Deploy Verification (conditional): health check, critical path, response time, error rate. Non-blocking.
9. Report PASS/FAIL/SKIP result with task-level summary
10. `git checkout develop && git pull origin develop`

**Phase 8 is the ONLY legitimate stopping point** (after the DoD checklist passes and execution-state.json is updated to COMPLETE).
