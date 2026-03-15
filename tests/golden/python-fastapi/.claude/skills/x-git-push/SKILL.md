---
name: x-git-push
description: "Git operations: branch creation, atomic commits (Conventional Commits), push, and PR creation. Use for any git workflow task including branching, committing, pushing, creating PRs, or managing version control."
allowed-tools: Bash, Read
argument-hint: "[branch-name or commit-message]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Commit and Push

## Purpose

Standardizes the Git workflow for {{PROJECT_NAME}}. Every feature starts with a branch and ends with a clean commit history following Conventional Commits.

## Branch Strategy

```
main (stable, always green)
  +-- feat/story-XXXX-YYYY-short-description
```

### Branch Naming

**Pattern:** `feat/story-XXXX-YYYY-short-kebab-description`

**Rules:**
- Always prefix with `feat/` (or `fix/`, `refactor/` as appropriate)
- Include the story/issue identifier
- Short description in kebab-case (English)
- Maximum 50 characters total

### Creating a Branch

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

### PR Creation

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

- **Format:** `feat(scope): implement story-XXXX-YYYY -- short title`
- **Max length:** 70 characters

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
| `feat(scope): implement [behavior] [TDD]` | **Recommended.** Test + implementation in one commit (one Red-Green cycle) |
| `test(scope): add test for [behavior] [TDD:RED]` | Test only (Red phase, when separating test from implementation) |
| `feat(scope): implement [behavior] [TDD:GREEN]` | Implementation only (Green phase, paired with a prior RED commit) |
| `refactor(scope): [improvement] [TDD:REFACTOR]` | Refactoring only (no new behavior, immediately after Green) |

The combined format `[TDD]` is the **recommended default**. Use the separate `[TDD:RED]`, `[TDD:GREEN]`, `[TDD:REFACTOR]` tags only when you need finer granularity in the git history.

TDD tags are **additive suffixes** -- they do not replace the Conventional Commits type. All existing types (`feat`, `test`, `fix`, `refactor`, `docs`, `build`, `chore`, `infra`) remain valid.

## Atomic TDD Commit Rules

1. **One commit per Red-Green-Refactor cycle** -- each commit represents a complete TDD cycle
2. **Test and implementation in the SAME commit** -- avoid orphaned test-only commits
3. **Refactoring may be a separate commit** -- but must immediately follow the Green commit
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
- Can be used standalone for any git workflow task
- Commit message scopes should match the project's package/module structure
