# Prompt: Epic and Story Generation — ia-dev-environment Git Flow Branching Model

> **Usage**: Run `/x-story-epic-full` with this file as input spec.
> Example: `/x-story-epic-full specs/SPEC-gitflow-branching-model-v1.md`

---

## System

**Project**: `ia-dev-environment` — CLI generator of AI-assisted development environments.

**Base version analyzed**: branch `main`, ~1100 commits.

**Objective of this specification**: Migrate the project's branching model from trunk-based
development (everything merges to `main`) to Git Flow, introducing the `develop` integration
branch, `release/*` branches for stabilization, `hotfix/*` branches from `main`, and changing
the default PR merge behavior to `--no-merge` (PRs are created but never auto-merged by default).

**Central principle of all stories**: Today ALL feature branches merge directly to `main`, which
means untested or unstable code can reach production with a single merge. The Git Flow model adds
a safety layer: features integrate into `develop`, are stabilized in `release/*` branches, and
only promoted to `main` when ready for production. This change affects the generated skills
(SKILL.md files), rules, CI/CD pipeline templates, and the epic orchestrator. Since all artifacts
in `.claude/` and `.github/` are **generated outputs** of `ia-dev-env`, the changes must be made
in the generator's source code (Java assemblers, templates, and resources), NOT by manually
editing generated files.

---

## Scope of the Epic

### Business Context

The `ia-dev-environment` generator produces skills, rules, and CI/CD workflows that define the
Git workflow for downstream projects. Currently, these generated artifacts implement a trunk-based
model where:

1. **All features merge to `main`** — Feature branches (`feat/*`) create PRs targeting `main`
   directly. Any merge puts code into the production branch immediately.

2. **No integration branch** — There is no `develop` branch for staging and validating feature
   integration before production promotion.

3. **Releases cut from `main`** — The `x-release` skill tags releases directly on `main`, with
   no stabilization period on a release branch.

4. **No hotfix isolation** — Bug fixes follow the same flow as features: branch from `main`,
   PR to `main`. There is no distinct `hotfix/*` pattern with dual merge (to `main` AND `develop`).

5. **PRs can be auto-merged** — The `x-dev-epic-implement` skill defaults to `interactive` mode,
   and offers `--auto-merge` which immediately merges PRs. The user wants the default to be
   `--no-merge` (PRs created but not merged automatically).

### Dimensions of improvement

1. **New rule `09-branching-model.md`** — Documents the Git Flow branching model as the project
   standard, defining branch types, naming conventions, and merge direction.

2. **Skill `x-git-push` update** — Change default base branch from `main` to `develop` for
   feature/fix branches. Add `hotfix/*` flow branching from `main` with dual merge targets.

3. **Skill `x-dev-lifecycle` update** — Phase 0 branches from `develop`, Phase 6 PR targets
   `develop`. Version bump moves to release branch (not during story implementation).

4. **Skill `x-dev-epic-implement` update** — All 15+ references to `main` change to `develop`.
   Auto-rebase runs against `develop`. Default merge mode changes from `interactive` to `no-merge`.
   Execution state schema adds `baseBranch` field.

5. **Skill `x-release` update** — Implement release branch workflow: branch from `develop`,
   merge to both `main` and `develop`, tag on `main`. Add hotfix release support.

6. **Skill `x-ci-cd-generate` update** — Generated CI/CD pipelines trigger on `develop`,
   `release/*`, `hotfix/*` branches. CD staging deploys from `develop`, CD production from `main`.

7. **Skill `x-fix-epic-pr-comments` update** — Change `--base main` to `--base develop` in
   correction PR creation.

8. **Knowledge Pack `release-management` update** — Activate Git Flow as the default strategy
   (currently one of 3 documented options). Update branching guide reference.

### Affected Files Summary

The following generated SKILL.md files contain hardcoded `main` branch references:

| Skill | File | Approx. `main` refs |
|:---|:---|:---|
| x-git-push | `.claude/skills/x-git-push/SKILL.md` | 6 |
| x-dev-lifecycle | `.claude/skills/x-dev-lifecycle/SKILL.md` | 4 |
| x-dev-epic-implement | `.claude/skills/x-dev-epic-implement/SKILL.md` | 15+ |
| x-release | `.claude/skills/x-release/SKILL.md` | 5 |
| x-ci-cd-generate | `.claude/skills/x-ci-cd-generate/SKILL.md` | 4 |
| x-fix-epic-pr-comments | `.claude/skills/x-fix-epic-pr-comments/SKILL.md` | 2 |

The following rules are affected:

| Rule | File | Change |
|:---|:---|:---|
| Rule 08 | `.claude/rules/08-release-process.md` | Add Git Flow branching references |
| NEW Rule 09 | `.claude/rules/09-branching-model.md` | Full branching model definition |

---

## Cross-Cutting Business Rules

**RULE-001**: **Git Flow Branch Structure** — The branching model MUST follow this structure:
- `main` — Production-ready code only. Receives merges ONLY from `release/*` and `hotfix/*`.
- `develop` — Integration branch. All feature branches merge here via PR.
- `feature/*` or `feat/*` — Created from `develop`, merged to `develop`.
- `release/*` — Created from `develop` for stabilization. Merged to BOTH `main` and `develop`.
- `hotfix/*` — Created from `main` for urgent fixes. Merged to BOTH `main` and `develop`.

**RULE-002**: **No Direct Merge to Main** — Feature branches MUST NEVER target `main` directly.
The only branches allowed to merge into `main` are `release/*` and `hotfix/*`. PRs targeting
`main` from `feat/*` branches MUST be rejected.

**RULE-003**: **Default No-Merge** — The default merge mode in `x-dev-epic-implement` MUST be
`no-merge`. PRs are created but NOT merged automatically. Users must explicitly opt-in via
`--auto-merge` or `--interactive-merge` to enable automatic merging.

**RULE-004**: **Develop as Default Base** — All feature and fix branches MUST use `develop` as
the base branch for PR creation (`gh pr create --base develop`). The only exceptions are
`hotfix/*` branches (which target `main`) and `release/*` branches (which target `main`).

**RULE-005**: **Release Branch Workflow** — Releases MUST follow:
1. Create `release/X.Y.Z` from `develop`
2. Only bug fixes allowed on release branch (no new features)
3. Merge `release/X.Y.Z` to `main` via PR
4. Tag `vX.Y.Z` on `main`
5. Merge `release/X.Y.Z` back to `develop` (or merge `main` into `develop`)
6. Delete the release branch

**RULE-006**: **Hotfix Dual Merge** — Hotfix branches MUST be merged to BOTH `main` (for
immediate production fix) AND `develop` (to include the fix in ongoing development). If a
`release/*` branch exists, the hotfix merges to the release branch instead of `develop`.

**RULE-007**: **Backward Compatibility** — Existing generated artifacts for projects using
trunk-based development MUST continue to work. The branching model SHOULD be configurable via
the project YAML config (e.g., `branching-model: gitflow` vs `branching-model: trunk`), with
`gitflow` as the default.

**RULE-008**: **CI/CD Branch Awareness** — Generated CI/CD pipelines MUST differentiate between:
- CI: triggers on `develop`, `release/*`, `hotfix/*`, and PRs to `develop` and `main`
- CD Staging: triggers on push to `develop`
- CD Production: triggers on push to `main` (from release/hotfix merges) or version tags

**RULE-009**: **Execution State Schema** — The `execution-state.json` used by
`x-dev-epic-implement` MUST include a `baseBranch` field (default: `develop`) to track which
branch stories target. This enables proper resume behavior after interruption.

**RULE-010**: **Branch Protection Guidance** — Generated artifacts SHOULD include documentation
or GitHub Actions configuration recommending branch protection rules for both `main` and `develop`:
- `main`: require PR, require status checks, require reviews, no direct push
- `develop`: require PR, require status checks, no direct push

---

## Stories

---

### STORY-0001: Branching Model Rule Definition

**Scope**: Create rule `09-branching-model.md` defining the Git Flow branching model as the
project standard. Update rule `08-release-process.md` to reference the new branching model.

**Details**:
- New file `.claude/rules/09-branching-model.md` with complete Git Flow documentation
- Branch types table: `main`, `develop`, `feature/*`, `release/*`, `hotfix/*`
- Merge direction rules: which branch can merge where
- Branch naming conventions for each type
- Forbidden actions: direct push to `main`, feature PRs to `main`
- Update Rule 08 to add "See Rule 09 for branching model" cross-reference
- This is a generated artifact: the rule content lives in the generator's resources

**Acceptance Criteria**:
- Rule 09 exists with complete Git Flow documentation
- Rule 08 references Rule 09 for branching details
- Branch naming conventions documented for all 5 branch types
- Merge direction rules clearly stated as non-negotiable
- Forbidden actions listed explicitly

---

### STORY-0002: x-git-push Skill — Develop as Default Base Branch

**Scope**: Update the `x-git-push` skill to use `develop` as the default base branch for
feature and fix branches. Add hotfix workflow support.

**Details**:
- Change "Branch Strategy" section: `develop` replaces `main` as the stable integration branch
- Update "Creating a Branch": `git checkout develop && git pull origin develop`
- Update "Workflow Per Story": all `main` references become `develop`
- Update "PR Creation": add `--base develop` to `gh pr create`
- Update "Review changes": `git diff develop...HEAD` instead of `git diff main...HEAD`
- Add new "Hotfix Workflow" section:
  - `git checkout main && git pull origin main`
  - `git checkout -b hotfix/description`
  - PR to `main`: `gh pr create --base main`
  - After merge to `main`: create PR from `main` to `develop` or cherry-pick
- Update "Integration Notes": document interaction with x-dev-lifecycle phases

**Acceptance Criteria**:
- All 6 references to `main` in x-git-push replaced with `develop` (feature flow)
- Hotfix workflow documented with dual merge (main + develop)
- PR creation always includes explicit `--base develop` (or `--base main` for hotfix)
- Branch strategy diagram shows: `develop → feat/* → develop`, `main → hotfix/* → main + develop`
- Integration notes updated for x-dev-lifecycle phases

---

### STORY-0003: x-dev-lifecycle Skill — Develop Branch Integration

**Scope**: Update the `x-dev-lifecycle` skill to branch from `develop` in Phase 0 and target
`develop` in Phase 6 PR creation. Remove version bump from story lifecycle (moved to release).

**Details**:
- Phase 0 (Preparation): `git checkout develop && git pull origin develop`
- Phase 6 (PR): `gh pr create --base develop`
- Remove or conditionalize version bump logic in Phase 6 (version bumps belong to release
  branches, not feature branches)
- Update all `main` references in diff comparisons and push commands
- Update "Integration Notes" at end of skill

**Acceptance Criteria**:
- Phase 0 branches from `develop`, not `main`
- Phase 6 PR targets `develop` with explicit `--base develop`
- Version bump in Phase 6 is removed or conditioned to release-only mode
- All diff/log commands reference `develop` as the comparison base
- No remaining hardcoded `main` references in feature flow

---

### STORY-0004: x-dev-epic-implement Skill — Develop Base and No-Merge Default

**Scope**: Update the `x-dev-epic-implement` skill to target `develop` for all story PRs and
change the default merge mode from `interactive` to `no-merge`.

**Details**:
- Replace all 15+ references to `main` with `develop` in the feature flow
- Change default merge mode: `Neither → mergeMode = "no-merge"` (was `interactive`)
- Rename flag: `--interactive-merge` replaces the old default interactive behavior as an opt-in
- Update execution-state.json schema: add `baseBranch` field (default: `"develop"`)
- Update auto-rebase logic: rebase against `develop` instead of `main`
- Update integrity gates: run against `develop`
- Update `--single-pr` legacy flow: epic branch targets `develop`
- Update RULE-011 (auto-rebase): `git fetch origin develop && git rebase origin/develop`
- Update all `git checkout main && git pull origin main` to `git checkout develop && git pull origin develop`
- The orchestrator remains on `develop` during resume (was `main`)
- PR merge command unchanged: `gh pr merge {prNumber} --merge`

**Acceptance Criteria**:
- Zero references to `main` in the standard feature flow (hotfix flow may reference `main`)
- Default merge mode is `no-merge` (PRs created but not merged)
- `--auto-merge` and new `--interactive-merge` flags opt-in to merge behavior
- execution-state.json includes `baseBranch: "develop"` in all new executions
- Auto-rebase targets `develop` branch
- Resume logic uses `develop` as the orchestrator branch
- All diagrams and ASCII art updated to reflect `develop`

---

### STORY-0005: x-release Skill — Release Branch Workflow

**Scope**: Update the `x-release` skill to implement the full Git Flow release branch workflow:
create release branch from `develop`, stabilize, merge to `main` AND `develop`, tag on `main`.

**Details**:
- Step 1 (DETERMINE): unchanged (version detection from Conventional Commits)
- Step 2 (VALIDATE): branch validation changes — must be on `develop` to start a release, OR
  on an existing `release/*` branch to continue stabilization
- New Step 2.5 (BRANCH): `git checkout -b release/X.Y.Z` from `develop`
- Step 3 (UPDATE): version file updates happen on release branch
- Step 4 (CHANGELOG): changelog generation on release branch
- Step 5 (COMMIT): release commit on release branch
- New Step 5.5 (MERGE TO MAIN): `git checkout main && git merge release/X.Y.Z --no-ff`
- Step 6 (TAG): tag on `main`: `git tag -a vX.Y.Z`
- New Step 6.5 (MERGE BACK): `git checkout develop && git merge release/X.Y.Z --no-ff`
  OR `git merge main --no-ff`
- Step 7 (DRY-RUN): show full plan including branch creation, dual merge, and tag
- Step 8 (PUBLISH): push `main`, `develop`, and tag
- New Step 9 (CLEANUP): `git branch -d release/X.Y.Z`
- Add hotfix release support: same flow but branch from `main`, tag on `main`, merge to `develop`
- Update SNAPSHOT handling: develop branch always has SNAPSHOT, release branch strips it

**Acceptance Criteria**:
- Running `/x-release minor` from `develop` creates `release/X.Y.Z` branch
- Release branch receives version bump and changelog
- Release branch merges to `main` with `--no-ff`
- Tag `vX.Y.Z` created on `main`
- Release branch merges back to `develop` with `--no-ff`
- Release branch deleted after successful merge
- Hotfix releases work from `main` with merge to `develop`
- Dry-run shows complete plan including all merge steps
- SNAPSHOT stripped on release branch, next SNAPSHOT set on `develop`

---

### STORY-0006: x-ci-cd-generate Skill — Multi-Branch Pipeline Triggers

**Scope**: Update the CI/CD pipeline generation skill to produce workflows aware of
`develop`, `release/*`, and `hotfix/*` branches.

**Details**:
- CI pipeline triggers:
  ```yaml
  on:
    push:
      branches: [develop, 'release/**', 'hotfix/**']
    pull_request:
      branches: [develop, main]
  ```
- CD Staging pipeline:
  ```yaml
  on:
    push:
      branches: [develop]
  ```
- CD Production pipeline:
  ```yaml
  on:
    push:
      branches: [main]
      tags: ['v*']
  ```
- Release pipeline: triggered on version tags (unchanged, tags are on `main`)
- Security pipeline: runs on `develop` and `main` (add `develop`)
- Add branch protection workflow suggestion (optional GitHub Actions)

**Acceptance Criteria**:
- Generated CI workflow triggers on `develop`, `release/*`, `hotfix/*`
- Generated CI workflow validates PRs to both `develop` and `main`
- CD Staging deploys on push to `develop`
- CD Production deploys on push to `main` or version tags
- No workflow triggers on direct push to `main` for CI (only via merge)
- Generated comments in YAML explain branch strategy

---

### STORY-0007: x-fix-epic-pr-comments Skill — Develop Base Branch

**Scope**: Update the PR comment remediation skill to target `develop` instead of `main`.

**Details**:
- Change `gh pr create --base main` to `gh pr create --base develop` in correction PR creation
- Update any `main` references in the skill's git operations
- Ensure the skill respects the `baseBranch` from execution-state.json if available

**Acceptance Criteria**:
- Correction PRs target `develop` by default
- `--base develop` explicitly used in `gh pr create`
- If execution-state.json has `baseBranch`, use that value
- No remaining hardcoded `--base main` in feature flow

---

### STORY-0008: Release Management Knowledge Pack — Git Flow as Default

**Scope**: Update the `release-management` knowledge pack to make Git Flow the default and
recommended branching strategy, adjusting the selection matrix accordingly.

**Details**:
- Move Git Flow from "option 3 of 3" to the primary recommended strategy
- Update the selection matrix to recommend Git Flow for most scenarios
- Keep Trunk-Based and Release Branches as alternatives with clear criteria for when to use them
- Update branching guide reference document
- Add decision criteria: when to use trunk-based vs Git Flow
- Cross-reference with new Rule 09

**Acceptance Criteria**:
- Git Flow is the default recommended strategy
- Selection matrix updated with clear decision criteria
- Trunk-based and release branches remain as documented alternatives
- Cross-reference to Rule 09 added
- Branching guide reference updated with Git Flow as primary

---

### STORY-0009: Branching Model Configuration in YAML

**Scope**: Add `branching-model` configuration to the project YAML schema, enabling projects
to choose between `gitflow` (default) and `trunk` branching models. Skills read this setting
to determine the correct branch targets.

**Details**:
- New YAML config field: `branching-model: gitflow` (or `trunk`)
- Default: `gitflow` when field is absent (RULE-007 backward compatibility caveat: existing
  projects without the field get `gitflow` behavior, which IS a breaking change for trunk-based
  users; document in migration guide)
- Skills read `branching-model` to determine: base branch (`develop` vs `main`), PR targets,
  release workflow
- Update all 8 profile config templates with explicit `branching-model: gitflow`
- `ProjectConfig` gains `BranchingModel` enum field
- `ConfigSourceLoader` parses the new field
- Validation in `StackValidator`

**Acceptance Criteria**:
- YAML config supports `branching-model: gitflow` and `branching-model: trunk`
- Default is `gitflow` when field is absent
- All 8 profile templates updated with explicit `branching-model: gitflow`
- Generated skills reflect the correct branch targets based on config
- Trunk mode reproduces current behavior (all targets `main`)
- Validation rejects invalid values with clear error message

---

### STORY-0010: Integration Tests and Golden File Updates

**Scope**: Update the test suite and golden files to validate the Git Flow branching model
in all generated artifacts.

**Details**:
- Update golden files for all 8 profiles to reflect `develop` as default base branch
- Add test cases for `branching-model: gitflow` (default) vs `branching-model: trunk`
- Verify generated x-git-push skill references `develop`
- Verify generated x-dev-lifecycle references `develop`
- Verify generated x-dev-epic-implement references `develop` and has `--no-merge` default
- Verify generated x-release skill has release branch workflow
- Verify generated CI/CD workflows have correct multi-branch triggers
- Verify generated rules include 09-branching-model.md
- Smoke tests validate end-to-end generation with gitflow config

**Acceptance Criteria**:
- All existing tests continue to pass (no regression)
- Golden files updated for all 8 profiles
- New tests cover gitflow-specific behavior in generated skills
- New tests cover trunk fallback behavior
- Coverage remains >= 95% line, >= 90% branch
- Smoke tests validate complete Git Flow artifact generation
