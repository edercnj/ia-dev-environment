# Task Breakdown -- story-0004-0009: CLI Documentation Generator

## Overview

This story adds a **CLI documentation generator** section to the `x-dev-lifecycle` skill's documentation phase. The generator instructs the AI subagent to scan CLI command definitions and produce `docs/api/cli-reference.md` when the project identity `interfaces` field contains `"cli"`.

**Key constraint:** NO new TypeScript source code. All changes are Markdown template content additions. The existing `SkillsAssembler.assembleCore()` pipeline already copies `x-dev-lifecycle/SKILL.md` to all output profiles without modification.

**Prerequisite:** story-0004-0005 (Documentation Phase) must be implemented first, providing the phase skeleton and interface dispatch mechanism that this story plugs into.

**Total files: 2 source templates + 24 golden files = 26 files.**

---

## G1: Source Template (Claude Code) -- `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`

### TASK-1: Add CLI Documentation Generator section to Documentation Phase

**Description:** Add a new subsection within the Documentation Phase (created by story-0004-0005) that defines the CLI documentation generator. This subsection instructs the AI subagent to detect CLI interface, scan command definitions, and generate structured Markdown documentation.

**File:** `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`
**Section:** Documentation Phase (added by story-0004-0005), new subsection
**Change type:** Content addition (~30-50 lines)
**Depends On:** story-0004-0005 (external dependency, not a task in this breakdown)
**Parallel:** no (first task, establishes the content that all subsequent tasks mirror)
**Estimated complexity:** medium

Content to add (within the Documentation Phase dispatch section):

1. **Subsection heading:** `### CLI Documentation Generator (interface: cli)`
2. **Trigger condition:** Invoked when project identity `interfaces` contains `"cli"`. Skip silently if not present (RULE-004).
3. **Output path:** `docs/api/cli-reference.md`
4. **Scan instructions** -- framework-specific patterns:
   - Commander.js: `.command()`, `.option()`, `.argument()` chains
   - Click: `@click.command()`, `@click.option()`, `@click.argument()` decorators
   - Cobra: `cobra.Command{}` structs
   - Clap: `#[derive(Parser)]` and `#[arg()]` attributes
5. **Output structure:**
   - `# CLI Reference` -- title with project name
   - `## Quick Start` -- at least 2 basic usage examples in code blocks
   - `## Global Flags` -- table of flags applicable to all commands (Flag, Type, Default, Description)
   - `## Command: {name}` -- one section per top-level command with:
     - Usage line: `$ {tool-name} {command} [flags] [args]`
     - Flags table: | Flag | Type | Default | Required | Description |
     - Arguments table: | Argument | Type | Required | Description |
     - At least 1 example in code block
   - `### Subcommand: {parent} {child}` -- nested sections with same structure
   - `## Exit Codes` -- table: | Code | Meaning |
6. **Skip behavior:** If `interfaces` does NOT contain `"cli"`: skip silently (no output, no warning).

---

## G2: Source Template (GitHub Copilot) -- `resources/github-skills-templates/dev/x-dev-lifecycle.md`

### TASK-2: Mirror CLI generator section with GitHub path adjustments

**Description:** Apply the same CLI Documentation Generator section from TASK-1 to the GitHub Copilot version of the template. Adapt skill path references to use `.github/skills/` convention instead of `.claude/skills/`.

**File:** `resources/github-skills-templates/dev/x-dev-lifecycle.md`
**Section:** Documentation Phase, new subsection (same location as TASK-1)
**Change type:** Content addition (~30-50 lines), mirror of TASK-1
**Depends On:** TASK-1
**Parallel:** no (must verify TASK-1 content is finalized before mirroring)
**Estimated complexity:** simple

Platform-specific differences from TASK-1:

| Aspect | Claude Code (TASK-1) | GitHub Copilot (TASK-2) |
|--------|---------------------|------------------------|
| Frontmatter style | `allowed-tools`, `argument-hint` | `name`, `description` only |
| Global Output Policy | Present | Absent |
| KP path references | `skills/X/references/Y.md` | `.github/skills/X/SKILL.md` |

The CLI generator content itself is identical in logic and structure. Only skill path references need adjustment if any are referenced within the section.

---

## G3: Golden File Regeneration -- All 24 golden files

### TASK-3: Regenerate golden files via pipeline

**Description:** Run the generator pipeline for all 8 profiles and update the golden files to reflect the new CLI generator content in `x-dev-lifecycle/SKILL.md`. Since `x-dev-lifecycle` is a core skill, all profiles receive the updated template identically (the CLI dispatch logic is at AI level, not pipeline level).

**Files affected:** 24 golden files (8 profiles x 3 output targets)

**Claude Code golden files (8):**

| # | Path |
|---|------|
| 1 | `tests/golden/go-gin/.claude/skills/x-dev-lifecycle/SKILL.md` |
| 2 | `tests/golden/java-quarkus/.claude/skills/x-dev-lifecycle/SKILL.md` |
| 3 | `tests/golden/java-spring/.claude/skills/x-dev-lifecycle/SKILL.md` |
| 4 | `tests/golden/kotlin-ktor/.claude/skills/x-dev-lifecycle/SKILL.md` |
| 5 | `tests/golden/python-click-cli/.claude/skills/x-dev-lifecycle/SKILL.md` |
| 6 | `tests/golden/python-fastapi/.claude/skills/x-dev-lifecycle/SKILL.md` |
| 7 | `tests/golden/rust-axum/.claude/skills/x-dev-lifecycle/SKILL.md` |
| 8 | `tests/golden/typescript-nestjs/.claude/skills/x-dev-lifecycle/SKILL.md` |

**GitHub Copilot golden files (8):**

| # | Path |
|---|------|
| 9 | `tests/golden/go-gin/.github/skills/x-dev-lifecycle/SKILL.md` |
| 10 | `tests/golden/java-quarkus/.github/skills/x-dev-lifecycle/SKILL.md` |
| 11 | `tests/golden/java-spring/.github/skills/x-dev-lifecycle/SKILL.md` |
| 12 | `tests/golden/kotlin-ktor/.github/skills/x-dev-lifecycle/SKILL.md` |
| 13 | `tests/golden/python-click-cli/.github/skills/x-dev-lifecycle/SKILL.md` |
| 14 | `tests/golden/python-fastapi/.github/skills/x-dev-lifecycle/SKILL.md` |
| 15 | `tests/golden/rust-axum/.github/skills/x-dev-lifecycle/SKILL.md` |
| 16 | `tests/golden/typescript-nestjs/.github/skills/x-dev-lifecycle/SKILL.md` |

**Agents/Codex golden files (8):**

| # | Path |
|---|------|
| 17 | `tests/golden/go-gin/.agents/skills/x-dev-lifecycle/SKILL.md` |
| 18 | `tests/golden/java-quarkus/.agents/skills/x-dev-lifecycle/SKILL.md` |
| 19 | `tests/golden/java-spring/.agents/skills/x-dev-lifecycle/SKILL.md` |
| 20 | `tests/golden/kotlin-ktor/.agents/skills/x-dev-lifecycle/SKILL.md` |
| 21 | `tests/golden/python-click-cli/.agents/skills/x-dev-lifecycle/SKILL.md` |
| 22 | `tests/golden/python-fastapi/.agents/skills/x-dev-lifecycle/SKILL.md` |
| 23 | `tests/golden/rust-axum/.agents/skills/x-dev-lifecycle/SKILL.md` |
| 24 | `tests/golden/typescript-nestjs/.agents/skills/x-dev-lifecycle/SKILL.md` |

**Procedure:**
1. Run `npx vitest run` -- expect golden file test failures for `x-dev-lifecycle` across all profiles
2. Use the golden file update mechanism to regenerate (e.g., `UPDATE_GOLDEN=true npx vitest run` or equivalent)
3. Re-run `npx vitest run` to verify all golden files match byte-for-byte

**Depends On:** TASK-1, TASK-2
**Parallel:** no (requires both source templates to be finalized)
**Estimated complexity:** simple (automated regeneration, no manual edits)

---

## G4: Verification -- Compilation and Tests

### TASK-4: TypeScript compilation check

**Description:** Run TypeScript compilation to verify no accidental TS source file modifications were introduced.

**Command:** `npx tsc --noEmit`
**Expected result:** Clean compilation (no new TS source code was changed)
**Depends On:** TASK-1, TASK-2
**Parallel:** yes (can run in parallel with TASK-3)
**Estimated complexity:** simple

---

### TASK-5: Full test suite with coverage validation

**Description:** Run the full test suite to verify all golden file tests pass after regeneration, and coverage thresholds are maintained.

**Command:** `npx vitest run`
**Expected result:** All ~1,384 tests passing (including 24 regenerated golden files for `x-dev-lifecycle`)
**Coverage targets:** Line >= 95%, Branch >= 90% (expected unchanged at 99.6%/97.84% since no TS code was modified)
**Depends On:** TASK-3, TASK-4
**Parallel:** no (must run after golden file regeneration)
**Estimated complexity:** simple

---

### TASK-6: Dual copy parity validation

**Description:** Manually verify that the CLI generator content in the Claude Code template (TASK-1) and the GitHub Copilot template (TASK-2) are logically equivalent, differing only in platform-specific path conventions.

**Validation method:** Diff the two source templates and confirm:
- CLI generator section structure is identical
- Only path references differ (`.claude/skills/` vs `.github/skills/`)
- Skip behavior logic is identical
- Output format specification is identical

**Depends On:** TASK-1, TASK-2
**Parallel:** yes (can run in parallel with TASK-3)
**Estimated complexity:** simple

---

## Execution Order

```
TASK-1 (Claude Code source template -- CLI generator content)
   |
   v
TASK-2 (GitHub Copilot template -- mirror with path adjustments)
   |
   +---> TASK-4 (compile check)     -- parallel with TASK-3
   +---> TASK-6 (dual copy parity)  -- parallel with TASK-3
   |
   v
TASK-3 (regenerate 24 golden files)
   |
   v
TASK-5 (full test suite + coverage)
```

**Notes:**
- TASK-1 must be done first as it establishes the canonical content.
- TASK-2 depends on TASK-1 to mirror the content with platform adjustments.
- TASK-3 depends on both TASK-1 and TASK-2 because the pipeline reads both source templates.
- TASK-4 and TASK-6 can run in parallel with TASK-3 (independent verification).
- TASK-5 must run last as it validates the final state (golden files regenerated, compilation clean).

---

## Summary

| Group | Tasks | Files | Complexity |
|-------|-------|-------|------------|
| G1: Claude Code source template | TASK-1 | 1 file | medium |
| G2: GitHub Copilot template | TASK-2 | 1 file | simple |
| G3: Golden file regeneration | TASK-3 | 24 files | simple (automated) |
| G4: Verification | TASK-4, TASK-5, TASK-6 | 0 files (read-only) | simple |
| **Total** | **6 tasks** | **26 files** | |

---

## Risk Notes

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| story-0004-0005 not yet merged | Medium | Verify prerequisite is complete before starting TASK-1. If not, wait or coordinate on the documentation phase section structure. |
| Dual copy drift | Low | TASK-6 explicitly validates parity. Use diff to compare CLI generator sections between both templates. |
| Golden file count mismatch | Low | Confirmed 3 output targets (`.claude/`, `.github/`, `.agents/`) x 8 profiles = 24 golden files. All must be regenerated since `x-dev-lifecycle` is a core skill. |
| Content too long for template | Negligible | CLI generator section is ~30-50 lines. Template is already ~260 lines, well within reasonable bounds. |
