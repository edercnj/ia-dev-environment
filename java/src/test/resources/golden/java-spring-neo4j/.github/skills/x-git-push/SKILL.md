---
name: x-git-push
description: >
  Git operations: branch creation, atomic commits (Conventional Commits),
  push, and PR creation. Use for any git workflow task including branching,
  committing, pushing, creating PRs, or managing version control.
  Reference: `.github/skills/x-git-push/SKILL.md`
---

# Skill: Commit and Push

## Purpose

Standardizes the Git workflow for {{PROJECT_NAME}}. Every feature starts with a branch and ends with a clean commit history following Conventional Commits.

## Branch Strategy

```
main (stable, always green)
  +-- feat/story-XXXX-YYYY-short-description       (parent/story branch)
  |     +-- feat/task-XXXX-YYYY-NNN-short-desc      (task branch)
```

### Branch Naming

#### Task Branch (Task-Centric Model)

**Pattern:** `feat/task-XXXX-YYYY-NNN-short-desc`

| Component | Format | Example |
|-----------|--------|---------|
| XXXX | 4-digit epic ID | `0029` |
| YYYY | 4-digit story ID | `0015` |
| NNN | 3-digit task sequence (001-999) | `001` |
| short-desc | lowercase-hyphenated description | `implement-phase2` |

**Rules:**
- Maximum **60 characters** total (truncate description to fit, preserving whole words)
- All characters must be `[a-z0-9/-]` (lowercase, digits, hyphens, forward slashes)

#### Parent Branch (Auto-Approve Mode)

**Pattern:** `feat/story-XXXX-YYYY-short-desc`

**Rules:**
- Maximum **60 characters** total
- Created from `develop`; task branches are created from this parent

#### Story Branch (Legacy/Standard)

**Pattern:** `feat/story-XXXX-YYYY-short-kebab-description`

**Rules:**
- Always prefix with `feat/` (or `fix/`, `refactor/` as appropriate)
- Include the story/issue identifier
- Short description in kebab-case (English)
- Maximum 100 characters total

#### Branch Name Validation

| Rule | Validation | Error |
|------|------------|-------|
| Epic ID (XXXX) | 4 numeric digits | "Invalid epic ID: must be 4 digits" |
| Story ID (YYYY) | 4 numeric digits | "Invalid story ID: must be 4 digits" |
| Task seq (NNN) | 3 numeric digits (001-999) | "Invalid task sequence: must be 3 digits" |
| Description | lowercase, hyphens only | "Invalid description: use lowercase-hyphenated" |
| Task branch length | <= 60 chars total | "Branch name exceeds 60 chars: truncating description" |
| Characters | `[a-z0-9/-]` only | "Invalid characters in branch name" |

### Creating a Branch

**Task branch:**

```bash
git checkout feat/story-XXXX-YYYY-description  # or develop
git pull
git checkout -b feat/task-XXXX-YYYY-NNN-short-desc
```

**Story/parent branch:**

```bash
git checkout main
git pull origin main
git checkout -b feat/story-XXXX-YYYY-description
```

## Commit Convention (Conventional Commits)

### Format

```
<type>(<scope>): <subject>

<optional body>

<optional footer>
```

### Types

| Type       | When to use                                |
| ---------- | ------------------------------------------ |
| `feat`     | New feature                                |
| `test`     | Adding or modifying tests only             |
| `fix`      | Bug fix                                    |
| `refactor` | Restructuring without behavior change      |
| `docs`     | Documentation changes                      |
| `build`    | Build system changes                       |
| `chore`    | Maintenance tasks                          |
| `infra`    | Infrastructure and deployment changes      |

### Scopes

**Task-centric scope (preferred for task branches):**

Use `TASK-XXXX-YYYY-NNN` as the scope when working on a task branch:

```
feat(TASK-0029-0015-001): implement task execution loop [TDD:GREEN]
```

**Module-based scope (for non-task work):**

Use the module, package, or component name as scope. Define project-specific scopes based on the architecture (e.g., `domain`, `rest`, `persistence`, `config`).

### Rules

1. **Atomic commits** -- One logical change per commit
2. **Subject line** -- Max 72 characters, imperative mood ("add" not "added"), no period at end
3. **Body** -- Optional. Explain "why" not "what". Wrap at 72 characters
4. **Tests with features** -- Feature commits should include their tests

## Workflow Per Story

### Starting a Story

```bash
git checkout main
git pull origin main
git checkout -b feat/story-XXXX-YYYY-description
git status
```

### During Implementation

Make frequent, atomic commits:

```bash
git add src/main/path/to/Feature.{{LANGUAGE}}
git add src/test/path/to/FeatureTest.{{LANGUAGE}}
git commit -m "feat(TASK-XXXX-YYYY-NNN): add feature description [TDD]"
```

For non-task branches, use module-based scopes:

```bash
git commit -m "feat(scope): add feature description"
```

### Finishing a Story

```bash
# 1. Run full build
{{BUILD_COMMAND}}

# 2. Review changes
git log --oneline main..HEAD
git diff main...HEAD --stat

# 3. Push
git push -u origin feat/story-XXXX-YYYY-description
```

## Pull Request

### Task PR Creation

When creating a PR for a task branch:

```bash
gh pr create \
  --base feat/story-XXXX-YYYY-description \
  --title "feat(TASK-XXXX-YYYY-NNN): description" \
  --body "$(cat <<'EOF'
## Summary
<1-3 bullet points describing what was built>

Story: story-XXXX-YYYY
Epic: epic-XXXX
Task Plan: plans/epic-XXXX/plans/task-plan-XXXX-YYYY-NNN.md

## Changed Files

| File | Layer | Change |
|------|-------|--------|
| `src/domain/model/Entity.{{LANGUAGE}}` | domain | Added |

## TDD Summary
- RED-GREEN-REFACTOR cycles: N
- Test count: N unit, N integration

## Checklist
- [ ] Build passes (`{{BUILD_COMMAND}}`)
- [ ] Coverage thresholds met
- [ ] All acceptance criteria covered
EOF
)"
```

### Story PR Creation (Legacy/Standard)

```bash
gh pr create \
  --title "feat(scope): implement story-XXXX-YYYY -- title" \
  --body "$(cat <<'EOF'
## Summary
<1-3 bullet points describing what was built>

## Test plan
- [ ] Build passes (`{{BUILD_COMMAND}}`)
- [ ] Coverage thresholds met
- [ ] All acceptance criteria covered
EOF
)"
```

### PR Title Convention

**Task PR format:**
- **Format:** `<type>(TASK-XXXX-YYYY-NNN): description`
- **Max length:** 70 characters
- **Example:** `feat(TASK-0029-0015-001): implement Phase 2 task loop`

**Story PR format (legacy):**
- **Format:** `feat(scope): implement story-XXXX-YYYY -- short title`
- **Max length:** 70 characters

### PR Body Requirements (Task PRs)

| Section | Content | Required |
|---------|---------|----------|
| Story reference | `Story: story-XXXX-YYYY` | Yes |
| Epic reference | `Epic: epic-XXXX` | Yes |
| Task plan link | `Task Plan: plans/epic-XXXX/plans/task-plan-XXXX-YYYY-NNN.md` | Optional |
| Changed files | Table with file path, layer, and change type | Yes |
| TDD summary | Number of RED/GREEN/REFACTOR cycles | Yes |
| Checklist | DoD items from the task | Yes |

### Useful Commands

```bash
gh pr list
gh pr view <number>
gh pr checks <number>
gh pr merge <number> --squash --delete-branch
```

## Tagging Releases

```bash
git tag -a v0.1.0 -m "Milestone description"
git push origin v0.1.0
```

## TDD Commit Format

When following TDD, use these commit format variants:

| Format | When to use |
| ------ | ----------- |
| `feat(scope): implement [behavior] [TDD]` | **Recommended default.** Complete Red-Green(-Refactor) cycle in one commit: test + implementation (+ trivial refactor) |
| `test(scope): add test for [behavior] [TDD:RED]` | **Optional, fine-grained.** Test-only Red phase; must be paired with a `[TDD:GREEN]` commit before push |
| `feat(scope): implement [behavior] [TDD:GREEN]` | **Optional, fine-grained.** Implementation-only Green phase; must immediately follow a paired `[TDD:RED]` commit |
| `refactor(scope): [improvement] [TDD:REFACTOR]` | **Optional.** Non-trivial refactor-only commit (no new behavior), immediately after the corresponding Green commit |

The combined format `[TDD]` is the **recommended default** for most work: one green commit per complete Red-Green(-Refactor) cycle. Use the separate `[TDD:RED]`, `[TDD:GREEN]`, `[TDD:REFACTOR]` tags **only** when you need finer granularity in the git history, and always keep RED/GREEN commits paired (no orphaned test-only commits on shared branches).

TDD tags are **additive suffixes** -- they do not replace the Conventional Commits type. All existing types (`feat`, `test`, `fix`, `refactor`, `docs`, `build`, `chore`, `infra`) remain valid.

## Atomic TDD Commit Rules

1. **Default: one combined commit per Red-Green-Refactor cycle** -- use the `[TDD]` suffix for a complete cycle in a single commit
2. **Test and implementation in the SAME commit** -- RED-only `[TDD:RED]` commits are allowed locally for fine-grained work, but must be paired with a corresponding `[TDD:GREEN]` commit before push (no orphaned test-only commits on shared branches)
3. **Refactoring may be a separate commit for non-trivial changes** -- use `[TDD:REFACTOR]` immediately after the Green commit, adding no new behavior
4. **Each commit adds ONE testable behavior** -- keep changes focused and reviewable
5. **Maximum ~50 lines changed per commit** -- larger commits should be split into smaller TDD cycles

## Git History Storytelling

The sequence of commits should tell the story of TDD progression from simple to complex:

1. **First commit:** acceptance test + test infrastructure setup
2. **Following commits:** incremental unit tests following Transformation Priority Premise (TPP) order
   - Degenerate / nil cases first
   - Constants, then variables
   - Simple conditionals, then iterations
   - Complex / composition cases last
3. **Final commits:** refactoring and polish (no new behavior)

The git log should read as a **progression from the simplest case to the most complex**, making the development process transparent and reviewable.

## Integration Notes

- Used by `x-dev-lifecycle` during Phase 0 (branch) and Phase 5 (push + PR)
- Delegates to `x-commit` for commit creation with task ID scope and TDD tags
- Delegates to `x-pr-create` for PR creation with task references and body template
- Can be used standalone for any git workflow task
- For task branches, commit scope MUST use `TASK-XXXX-YYYY-NNN` format
- For non-task branches, commit scope should match the project's package/module structure
- Task PRs target the parent story branch; story PRs target `develop`
- Reference: `.github/skills/x-git-push/SKILL.md`
