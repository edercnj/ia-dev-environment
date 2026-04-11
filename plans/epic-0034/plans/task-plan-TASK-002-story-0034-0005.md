# Task Plan -- TASK-0034-0005-002

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0005-002 |
| Story ID | story-0034-0005 |
| Epic ID | 0034 |
| Source Agent | merged(TechLead, Security, ProductOwner) |
| Type | documentation |
| TDD Phase | VERIFY |
| TPP Level | N/A |
| Layer | cross-cutting |
| Estimated Effort | M |
| Date | 2026-04-10 |

## Objective

Clean residual references to removed targets in `.claude/rules/*.md`, `README.md` root, `docs/*` (if any), and add the BREAKING CHANGE disclosure to `CHANGELOG.md` under `[Unreleased]` with Removed, Changed, Migration, and Rollback sections per Rule 08.

## Implementation Guide

### Step 1 - Inventory residual references

```bash
grep -rln 'copilot\|codex\|\.agents/' .claude/rules/ README.md docs/ 2>/dev/null \
  | tee /tmp/residual-refs-pre.txt
```

Record the file list. These are the edit targets.

### Step 2 - Identify generated vs source-of-truth files

Per CLAUDE.md `.claude/` and `.github/` directories are **generated outputs** produced by `ia-dev-env`. The source of truth for rules is `java/src/main/resources/targets/claude/rules/`.

For each file in `/tmp/residual-refs-pre.txt`:

- If the path starts with `.claude/` - DO NOT edit directly. Find the source under `java/src/main/resources/targets/claude/` and edit there. Regenerate via the generator or via `GoldenFileRegenerator` if applicable.
- If the path is `README.md` (repo root) - hand-edit directly.
- If the path starts with `docs/` - usually hand-edited; confirm it is not generated.

### Step 3 - Clean rules files (source-of-truth edit + regeneration)

For each `.claude/rules/NN-name.md` file flagged:

1. Open the corresponding source file under `java/src/main/resources/targets/claude/rules/NN-name.md`.
2. Delete or rewrite paragraphs/tables/bullets that reference Copilot, Codex, `.agents/`, or `.codex/`.
3. Preserve `.github/workflows/` references in CI/CD context (RULE-003).
4. Save.

After all source files are edited, run the generator to rewrite `.claude/rules/*.md`:

```bash
# From repo root - canonical regen procedure per README.md §"Regenerating Golden Files"
cd java && mvn process-resources
# Then invoke the generator - exact command per README.md, something like:
# mvn exec:java -Dexec.mainClass=dev.iadev.generator.Main -Dexec.args="generate --platform claude-code --output .."
# Or via ia-dev-env CLI if published.
```

Verify:

```bash
grep -rn 'copilot\|codex\|\.agents/' .claude/rules/ \
  | grep -v '\.github/workflows/'
# Expected: 0 matches
```

### Step 4 - Clean README.md (hand-edit)

```bash
grep -niE 'copilot|codex|\.agents/' README.md
```

For each match, rewrite or delete as appropriate:

- Architecture diagrams / bullet lists: drop Copilot/Codex/Agents branches.
- Feature matrix tables: drop rows for removed targets.
- Installation instructions: ensure no `--platform copilot` examples remain.
- Badges or links referring to removed targets: remove.

### Step 5 - Clean docs/ (if exists)

```bash
test -d docs && grep -rnE 'copilot|codex|\.agents/' docs/ 2>/dev/null
```

Edit each match. Same rules as Step 4.

### Step 6 - Update CHANGELOG.md

Open `CHANGELOG.md` at repo root. Under the `[Unreleased]` section, add the following (create the section if absent):

```markdown
## [Unreleased]

### Removed

- **BREAKING:** `Platform.COPILOT` enum value (generator no longer emits `.github/` Copilot artifacts)
- **BREAKING:** `Platform.CODEX` enum value (generator no longer emits `.codex/` artifacts)
- **BREAKING:** `AssemblerTarget.GITHUB` - `.github/` target retired
- **BREAKING:** `AssemblerTarget.CODEX` - `.codex/` target retired
- **BREAKING:** `AssemblerTarget.CODEX_AGENTS` - `.agents/` target retired
- **BREAKING:** 18 Java assemblers deleted (8 Copilot + 7 Codex + 2 Agents + `ReadmeGithubCounter`)
- **BREAKING:** ~34 test classes and 2 fixtures for removed targets
- **BREAKING:** Golden file fixtures for `.github/`, `.codex/`, `.agents/` (~8178 files; `.github/workflows/` preserved)
- Resource directories `java/src/main/resources/targets/github-copilot/` and `java/src/main/resources/targets/codex/`

### Changed

- **BREAKING:** CLI `--platform` flag now accepts only `claude-code`. Previous values `copilot`, `codex`, `agents`, and `all` are rejected with an error.
- Default value of `--platform` flag is now `claude-code` (when flag is omitted).
- `ExpectedArtifactsGenerator` manifest reduced from ~9500 to ~830 entries per profile (~91% reduction).
- Generator output per profile reduced from ~9500 to ~830 artifacts.
- `CLAUDE.md` at repo root reduced by ~180 lines to reflect single-target scope.

### Migration

Users with automated scripts or CI pipelines invoking `ia-dev-env` must update as follows:

- Replace `--platform copilot` or `--platform codex` or `--platform agents` or `--platform all` with `--platform claude-code`, OR drop the flag entirely (the new default is `claude-code`).
- Remove any downstream tooling that consumes `.github/instructions/`, `.github/skills/`, `.github/prompts/`, `.codex/config.toml`, or `.agents/` artifacts - these are no longer produced.
- `.github/workflows/` files in golden fixtures are preserved (CI/CD is unaffected).
- Claude Code users: no action required.

### Rollback

This release introduces no database migrations and no persistent state changes (the generator is stateless). To roll back, revert the merge commit for EPIC-0034 on `develop` and re-run builds. Prior behavior (multi-target support) is restored atomically.

### Security

- Removed `ReadmeGithubCounter` class and all GitHub-specific readme generation paths.
- CLI error messages for rejected platform values contain no class names, stack traces, or file paths (CWE-209 compliance).
```

**Important formatting notes:**

- Use "Keep a Changelog" format exactly (sections: Added, Changed, Deprecated, Removed, Fixed, Security, plus Migration and Rollback for this BREAKING release).
- Every bullet in Removed/Changed prefixed with `**BREAKING:**` to trigger MAJOR version bump per Rule 08 SemVer table.
- Do not set a version number yet; leave the section as `[Unreleased]`. Version bump happens at release time, not in this epic.

### Step 7 - Final grep validation

```bash
# Residual multi-target references (allow .github/workflows/)
grep -rn 'copilot\|codex\|\.agents/' .claude/rules/ README.md docs/ 2>/dev/null \
  | grep -v '\.github/workflows/'
# Expected: 0 matches

# Secret leakage (CWE-798)
grep -riE 'password|secret|token|api[-_]?key|bearer' \
  .claude/rules/ README.md docs/ CHANGELOG.md 2>/dev/null \
  | grep -vE '(placeholder|example|\.md:.*# )'
# Expected: 0 true positives (allowlist placeholders and comments)
```

### Step 8 - Commit

```
docs(changelog)!: document BREAKING removal of non-Claude targets

Add [Unreleased] Removed, Changed, Migration, and Rollback sections to
CHANGELOG.md following Keep a Changelog format. Clean residual references
to Copilot/Codex/Agents in .claude/rules/, README.md, and docs/. Preserve
.github/workflows/ references in CI/CD context (RULE-003).

BREAKING CHANGE: CLI --platform accepts only claude-code. Values copilot,
codex, agents, and all are now rejected. Users with automated scripts must
update per the Migration section in CHANGELOG.md.

Ref: EPIC-0034
```

## Definition of Done

- [ ] `.claude/rules/`, `README.md`, `docs/` grep returns 0 residual target references (except `.github/workflows/`)
- [ ] Source-of-truth edits applied under `java/src/main/resources/targets/claude/rules/` (if applicable) before regenerating `.claude/rules/`
- [ ] CHANGELOG.md `[Unreleased] > Removed` lists all 5 enum values + supporting classes
- [ ] CHANGELOG.md `[Unreleased] > Changed` documents CLI behavior change
- [ ] CHANGELOG.md `[Unreleased] > Migration` provides clear upgrade path
- [ ] CHANGELOG.md `[Unreleased] > Rollback` documents revert procedure
- [ ] CHANGELOG.md `[Unreleased] > Security` notes CWE-209 compliance
- [ ] Every BREAKING bullet explicitly marked with `**BREAKING:**` prefix
- [ ] Secret-leak grep returns 0 true positives (CWE-798)
- [ ] Conventional Commit message with `BREAKING CHANGE:` footer
- [ ] Pre-commit hooks pass

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-0034-0005-001 | CLAUDE.md must be cleaned first so rules files can be cleaned in consistent voice and CHANGELOG can reference the docs state accurately |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| `.claude/rules/*.md` are generated outputs; direct edits get overwritten | High | High | Step 2 explicitly identifies source-of-truth files. Edit source + regenerate, do NOT hand-edit generated outputs. |
| CHANGELOG format drift (not Keep a Changelog compliant) | Low | Medium | Step 6 provides exact section structure. Existing CHANGELOG.md format is the reference for existing entries. |
| Migration instructions incomplete | Medium | Medium | Step 6 Migration section explicitly enumerates all changed flags and downstream consumers. Product Owner validation (PO-003) gates this. |
| `BREAKING CHANGE:` footer missing from commit - Conventional Commits parser will not bump MAJOR | Medium | High | Step 8 commit template includes the footer. Verify with `git log -1 --format=%B` after commit. |
