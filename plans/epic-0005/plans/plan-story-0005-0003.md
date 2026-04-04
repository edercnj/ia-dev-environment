# Implementation Plan: SKILL.md Skeleton + Input Parsing

**Story:** story-0005-0003
**Epic:** epic-0005
**Date:** 2026-03-16

---

## 1. Affected Layers and Components

This story is exclusively a **template-layer** change. The new skill is a core skill template (a `.md` prompt file) that gets auto-discovered and copied by the existing pipeline. The primary areas of impact are:

| Layer | Component | Impact |
|-------|-----------|--------|
| **Resources (templates)** | `resources/skills-templates/core/x-dev-epic-implement/SKILL.md` | **NEW** -- the main deliverable |
| **Resources (GitHub templates)** | `resources/github-skills-templates/dev/x-dev-epic-implement.md` | **NEW** -- GitHub Copilot variant |
| **GitHub Skills Assembler** | `src/assembler/github-skills-assembler.ts` | **MODIFY** -- register skill in `SKILL_GROUPS.dev` array |
| **Golden Files (all 8 profiles)** | `tests/golden/{profile}/.claude/skills/x-dev-epic-implement/SKILL.md` | **NEW** -- golden output for byte-for-byte tests |
| **Golden Files (all 8 profiles)** | `tests/golden/{profile}/.github/skills/x-dev-epic-implement/SKILL.md` | **NEW** -- golden GitHub output |
| **Golden Files (all 8 profiles)** | `tests/golden/{profile}/.agents/skills/x-dev-epic-implement/SKILL.md` | **NEW** -- golden Codex output |
| **Golden Files (all 8 profiles)** | `tests/golden/{profile}/.claude/README.md` | **MODIFY** -- README regenerated with new skill count/table |

### Why No Assembler Code Changes for .claude/ Copy

The `SkillsAssembler.selectCoreSkills()` method (line 38-59 of `src/assembler/skills-assembler.ts`) **auto-discovers** all directories under `resources/skills-templates/core/`. Simply creating the directory `resources/skills-templates/core/x-dev-epic-implement/` with a `SKILL.md` inside is sufficient for the `.claude/skills/` pipeline to pick it up.

Similarly, the `CodexSkillsAssembler` (`.agents/skills/`) mirrors whatever `.claude/skills/` produces, so it also requires no code changes.

### Why GitHub Skills Assembler Needs a Code Change

The `GithubSkillsAssembler` does NOT auto-discover skills. It uses a hardcoded `SKILL_GROUPS` registry (line 24-58 of `src/assembler/github-skills-assembler.ts`). The new skill must be added to the `"dev"` group array.

---

## 2. New Files to Create

### 2.1 Core Skill Template (`.claude/` pipeline)

**Path:** `resources/skills-templates/core/x-dev-epic-implement/SKILL.md`

This is the main deliverable. Structure follows the `x-dev-lifecycle/SKILL.md` pattern:

```
---
name: x-dev-epic-implement
description: "<description>"
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill
argument-hint: "[EPIC-ID] [--phase N] [--story XXXX-YYYY] [--skip-review] [--dry-run] [--resume] [--parallel]"
---

## Global Output Policy
...

# Skill: Epic Implementation Orchestrator

## When to Use
...

## Input Parsing
  (epic ID + 6 flags)

## Prerequisites Check
  (5 checks + resume check)

## Phase 0 — Preparation
  (inline: parsing + prerequisites + branch)

## Phase 1 — Execution Loop
  (placeholder — story-0005-0005)

## Phase 2 — Consolidation
  (placeholder — story-0005-0011)

## Phase 3 — Verification
  (placeholder — story-0005-0011)
```

**Placeholder Tokens Used:**

The SKILL.md will contain `{{DOUBLE_BRACE}}` runtime markers that are **intentionally NOT resolved** during generation (per the convention established in `x-dev-lifecycle` line 412). These include:

| Token | Purpose | Resolved At |
|-------|---------|-------------|
| `{{PROJECT_NAME}}` | Project name in subagent prompts | Runtime (AI reads project config) |
| `{{COMPILE_COMMAND}}` | Compilation command | Runtime |
| `{{TEST_COMMAND}}` | Test execution command | Runtime |
| `{{COVERAGE_COMMAND}}` | Coverage reporting command | Runtime |

Note: The `{single_brace}` tokens (like `{project_name}`) ARE resolved by the `TemplateEngine.replacePlaceholders()` method during generation. But this SKILL.md should NOT use any `{single_brace}` tokens because its content is the same across all profiles (it's a prompt template, not a project-specific config). This matches the behavior of `x-dev-lifecycle/SKILL.md` where the golden files are identical across `typescript-nestjs` and `python-click-cli` profiles.

### 2.2 GitHub Copilot Skill Template

**Path:** `resources/github-skills-templates/dev/x-dev-epic-implement.md`

A condensed version of the `.claude/` SKILL.md following the GitHub skills format (see `x-dev-lifecycle.md` in the same directory for pattern). Uses YAML frontmatter with `name` and `description` (no `allowed-tools` or `argument-hint` -- GitHub format differs).

### 2.3 Golden Files (8 profiles x 3 outputs = 24 new files)

For each of the 8 profiles (`go-gin`, `java-quarkus`, `java-spring`, `kotlin-ktor`, `python-click-cli`, `python-fastapi`, `rust-axum`, `typescript-nestjs`):

| Golden file | Source |
|-------------|--------|
| `tests/golden/{profile}/.claude/skills/x-dev-epic-implement/SKILL.md` | Identical to the template (no `{single_brace}` placeholders used) |
| `tests/golden/{profile}/.github/skills/x-dev-epic-implement/SKILL.md` | Identical to the GitHub template |
| `tests/golden/{profile}/.agents/skills/x-dev-epic-implement/SKILL.md` | Identical to the `.claude/` version (Codex mirrors `.claude/`) |

**Total new golden files: 24** (8 profiles x 3 output targets)

Additionally, the `README.md` golden files for all 8 profiles will need regeneration because the new skill changes the skill count and skills table in the README.

---

## 3. Existing Files to Modify

### 3.1 GitHub Skills Assembler Registration

**File:** `src/assembler/github-skills-assembler.ts`
**Change:** Add `"x-dev-epic-implement"` to the `SKILL_GROUPS.dev` array.

Current (lines 29-33):
```typescript
"dev": [
    "x-dev-implement", "x-dev-lifecycle",
    "x-dev-architecture-plan", "x-dev-arch-update",
    "layer-templates", "x-dev-adr-automation",
],
```

After:
```typescript
"dev": [
    "x-dev-implement", "x-dev-lifecycle",
    "x-dev-epic-implement",
    "x-dev-architecture-plan", "x-dev-arch-update",
    "layer-templates", "x-dev-adr-automation",
],
```

### 3.2 Golden README Files (8 profiles)

**Files:** `tests/golden/{profile}/.claude/README.md` for all 8 profiles.

These will change because `ReadmeAssembler` dynamically scans `skills/` to build counts and tables. The new skill adds a row to the skills table and increments the skill count. The simplest approach is to **regenerate** golden files by running the pipeline and capturing the output.

---

## 4. Dependency Direction Validation

```
resources/skills-templates/core/x-dev-epic-implement/SKILL.md  (template data)
        |
        v
src/assembler/skills-assembler.ts   (auto-discovers core/ subdirs)
        |
        v
src/assembler/copy-helpers.ts       (copies + replaces {placeholders})
        |
        v
output/.claude/skills/x-dev-epic-implement/SKILL.md  (generated output)
        |
        v
src/assembler/codex-skills-assembler.ts  (mirrors .claude/ to .agents/)
        |
        v
output/.agents/skills/x-dev-epic-implement/SKILL.md
```

For GitHub:
```
resources/github-skills-templates/dev/x-dev-epic-implement.md  (template)
        |
        v
src/assembler/github-skills-assembler.ts  (reads SKILL_GROUPS registry)
        |
        v
output/.github/skills/x-dev-epic-implement/SKILL.md
```

All dependencies flow from resources -> assemblers -> output. No circular dependencies. The change respects the existing architecture:
- Templates are pure data (no code dependencies)
- Assemblers are the only code that reads templates
- Output is generated, never hand-edited

---

## 5. Integration Points

### 5.1 Pipeline Integration

The skill integrates at three points in the pipeline (see `src/assembler/pipeline.ts`):

| Step | Assembler | How |
|------|-----------|-----|
| Step 2 | `SkillsAssembler` (target: `claude`) | Auto-discovery -- no code change needed |
| Step 10 | `GithubSkillsAssembler` (target: `github`) | Requires registration in `SKILL_GROUPS.dev` |
| Step 19 | `CodexSkillsAssembler` (target: `codex-agents`) | Mirrors `.claude/` -- no code change needed |

Additionally, `ReadmeAssembler` (Step 22) dynamically scans the output to build the skills table, so the new skill will appear in the generated README automatically.

### 5.2 Golden File Integration

The `byte-for-byte.test.ts` integration test runs the pipeline for each profile and compares against golden files. New golden files must be added for the test to pass.

### 5.3 Skill Invocation Integration

At runtime, Claude Code discovers skills by scanning `.claude/skills/*/SKILL.md`. The new skill will automatically appear in the `/` command menu when the generated output is placed in a project.

---

## 6. Database Changes

N/A -- This is a CLI tool with no database.

---

## 7. API Changes

N/A -- No REST/gRPC/CLI command changes. The skill is a prompt file, not executable code.

However, the new skill introduces a **user-facing invocation contract**:

```
/x-dev-epic-implement [EPIC-ID] [--phase N] [--story XXXX-YYYY] [--skip-review] [--dry-run] [--resume] [--parallel]
```

This is documented in the SKILL.md's `argument-hint` frontmatter and the Input Parsing section.

---

## 8. Event Changes

N/A -- No event-driven components.

---

## 9. Configuration Changes

### 9.1 Auto-Discovery (No Config Needed)

For `.claude/` output: The `SkillsAssembler.selectCoreSkills()` method auto-discovers any directory under `resources/skills-templates/core/`. Creating the directory is sufficient.

### 9.2 GitHub Registration (Code Change)

For `.github/` output: The `SKILL_GROUPS` constant in `github-skills-assembler.ts` must be updated. This is the only "configuration" change required.

### 9.3 No Config Template Changes

No changes to `resources/config-templates/setup-config.*.yaml` files are needed. The skill is a core skill (always included), not a conditional skill gated by config.

---

## 10. Risk Assessment

### 10.1 Low Risk: Template Content

The SKILL.md is a prompt file with no executable logic. The main risk is incorrect YAML frontmatter or inconsistent placeholder conventions. Mitigated by following the established `x-dev-lifecycle` pattern exactly.

### 10.2 Medium Risk: Golden File Updates

**24 new golden files + 8 modified README golden files = 32 golden file changes across 8 profiles.**

This is the highest-risk area because:
- Each profile's golden files must match byte-for-byte
- README golden files change dynamically (skill counts, table entries)
- Missing or mismatched golden files cause the `byte-for-byte parity` test suite to fail

**Mitigation:** Run the pipeline for each profile, capture output, and use it to generate golden files. Verify with `npm test` before committing.

### 10.3 Low Risk: Assembler Registration

Adding one entry to the `SKILL_GROUPS.dev` array is a minimal, well-isolated change. Risk of breaking other skills is negligible because:
- Each skill is independently discovered and copied
- The array order does not affect functionality (skills are rendered independently)

### 10.4 Low Risk: Future Extensibility

The story explicitly creates Phase 1-3 as **placeholders** for subsequent stories (story-0005-0005 and story-0005-0011). Risk: placeholder format may not be compatible with how those stories expect to extend the file. Mitigated by using clear section headers and comment markers (e.g., `<!-- PHASE_1_CONTENT -->`) that future stories can locate and replace.

### 10.5 No Risk: Backward Compatibility

This is purely additive -- a new skill template. No existing skills, assemblers, or outputs are modified (except the README regeneration and the GitHub skills array, both of which are additive).

---

## Implementation Task Order

Following inner-to-outer layer ordering and TDD discipline:

| Task | Description | Dependencies |
|------|-------------|-------------|
| T1 | Create `resources/skills-templates/core/x-dev-epic-implement/SKILL.md` | None |
| T2 | Create `resources/github-skills-templates/dev/x-dev-epic-implement.md` | None |
| T3 | Add `"x-dev-epic-implement"` to `SKILL_GROUPS.dev` in `github-skills-assembler.ts` | None |
| T4 | Regenerate golden files (run pipeline, capture output for all 8 profiles) | T1, T2, T3 |
| T5 | Verify with `npm test` -- all 1384+ tests pass, byte-for-byte parity holds | T4 |
| T6 | Manual verification: inspect generated SKILL.md for correct frontmatter, parsing section, prerequisites, phase structure | T4 |

### Parallel Opportunities

- T1 and T2 can be done in parallel (independent templates)
- T3 can be done in parallel with T1/T2
- T4 depends on T1 + T2 + T3 all being complete
- T5 and T6 depend on T4

---

## File Manifest

### New Files (26)

| # | Path | Purpose |
|---|------|---------|
| 1 | `resources/skills-templates/core/x-dev-epic-implement/SKILL.md` | Core skill template |
| 2 | `resources/github-skills-templates/dev/x-dev-epic-implement.md` | GitHub Copilot skill template |
| 3-10 | `tests/golden/{profile}/.claude/skills/x-dev-epic-implement/SKILL.md` | Golden files (8 profiles) |
| 11-18 | `tests/golden/{profile}/.github/skills/x-dev-epic-implement/SKILL.md` | Golden files (8 profiles) |
| 19-26 | `tests/golden/{profile}/.agents/skills/x-dev-epic-implement/SKILL.md` | Golden files (8 profiles) |

### Modified Files (9)

| # | Path | Change |
|---|------|--------|
| 1 | `src/assembler/github-skills-assembler.ts` | Add skill to `SKILL_GROUPS.dev` |
| 2-9 | `tests/golden/{profile}/.claude/README.md` | Regenerated (new skill in table/count) |
