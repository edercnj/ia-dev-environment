# Implementation Plan: story-0004-0009 -- CLI Documentation Generator

**Story:** Gerador de Documentacao CLI
**Blocked By:** story-0004-0005 (Documentation Phase in `x-dev-lifecycle`)
**Architecture:** Library / CLI tool (TypeScript 5 + Commander)

---

## 0. Context Summary

This story adds a **CLI documentation generator** to the `x-dev-lifecycle` skill's documentation phase (story-0004-0005). The generator is invoked when the project identity `interfaces` field contains `"cli"`. It produces instructions (as part of the lifecycle skill's Markdown template) for the AI agent to scan CLI command definitions and generate `docs/api/cli-reference.md`.

**Key insight:** This project is a **code generator** (CLI tool that generates `.claude/` and `.github/` boilerplate). The "CLI doc generator" is NOT runtime code that parses Commander.js ASTs -- it is a **template/prompt section** added to the `x-dev-lifecycle` SKILL.md that instructs the AI agent to analyze the target project's CLI commands and produce documentation. The generator is a content addition to the lifecycle skill template, dispatched by the documentation phase's interface-aware mechanism.

---

## 1. Affected Layers and Components

| Layer | Component | Impact |
|-------|-----------|--------|
| Resources (source of truth) | `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` | **Modify** -- Add CLI doc generator section to documentation phase |
| Resources (GitHub copy) | `resources/github-skills-templates/dev/x-dev-lifecycle.md` | **Modify** -- Mirror changes (RULE-001 dual copy) |
| Golden files | `tests/golden/*/` (8 profiles) | **Update** -- All profiles include x-dev-lifecycle; golden files must reflect new content |
| Tests | `tests/node/integration/byte-for-byte.test.ts` | **Verify** -- Existing golden file tests will validate the output |

**Not affected:**
- `src/` application code -- No TypeScript source changes needed. The SkillsAssembler already copies `x-dev-lifecycle/SKILL.md` as a core skill. No new assembler logic, no new conditions.
- `src/assembler/conditions.ts` -- The `hasInterface(config, "cli")` utility already exists but is not needed here. The interface dispatch logic lives _inside_ the SKILL.md content (AI-level instructions), not in the generator pipeline.
- `src/models.ts` -- No model changes.

---

## 2. New Classes/Interfaces to Create

**None.** This story is purely a template content change. The existing pipeline already copies `x-dev-lifecycle/SKILL.md` to all output profiles via `SkillsAssembler.assembleCore()`.

---

## 3. Existing Files to Modify

### 3.1 `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`

**Change type:** Content addition (new documentation phase with CLI generator dispatch)

**Prerequisite:** story-0004-0005 must first add the documentation phase skeleton (Phase 3 -- Documentation) with interface dispatch mechanism, phase renumbering (Phase 3->4, 4->5, etc.), and total phase count update (8->9 phases, 0-8). This plan assumes story-0004-0005 is already implemented.

**What to add (within the Documentation Phase section created by story-0004-0005):**

A CLI documentation generator subsection that instructs the AI subagent to:

1. **Detect CLI interface** -- Check if `interfaces` contains `"cli"`
2. **Scan command definitions** -- Analyze the target project's CLI framework (Commander.js, Click, Cobra, Clap, etc.) for:
   - Top-level commands and subcommands (hierarchy)
   - Flags/options (`--flag`) with type, default value, required/optional
   - Positional arguments with type, required/optional
   - Command descriptions
3. **Generate `docs/api/cli-reference.md`** with:
   - `# CLI Reference` -- title
   - `## Quick Start` -- 2+ usage examples in code blocks
   - `## Global Flags` -- flags applicable to all commands
   - `## Command: {name}` -- one section per top-level command with:
     - Usage line in code block
     - Flags table (Flag, Type, Default, Description)
     - Arguments table (Argument, Type, Required, Description)
     - At least 1 example per command
   - `### Subcommand: {parent} {child}` -- nested sections for subcommands
   - `## Exit Codes` -- table with Code and Meaning columns
4. **Skip silently** if `interfaces` does not contain `"cli"` (RULE-004)

### 3.2 `resources/github-skills-templates/dev/x-dev-lifecycle.md`

**Change type:** Mirror of 3.1 changes (RULE-001 dual copy consistency)

Identical CLI generator content, adapted for GitHub Copilot context:
- Skill references use `.github/skills/` paths instead of `.claude/skills/` (already the convention in this file)

### 3.3 Golden Files (8 profiles)

All 8 golden file profiles contain a copy of `x-dev-lifecycle/SKILL.md`:

| Profile | Path | Has CLI? |
|---------|------|----------|
| `go-gin` | `tests/golden/go-gin/.claude/skills/x-dev-lifecycle/SKILL.md` | No (rest, grpc) |
| `java-quarkus` | `tests/golden/java-quarkus/.claude/skills/x-dev-lifecycle/SKILL.md` | No (rest, grpc) |
| `java-spring` | `tests/golden/java-spring/.claude/skills/x-dev-lifecycle/SKILL.md` | No (rest, grpc) |
| `kotlin-ktor` | `tests/golden/kotlin-ktor/.claude/skills/x-dev-lifecycle/SKILL.md` | No (rest, grpc) |
| `python-click-cli` | `tests/golden/python-click-cli/.claude/skills/x-dev-lifecycle/SKILL.md` | **Yes (cli)** |
| `python-fastapi` | `tests/golden/python-fastapi/.claude/skills/x-dev-lifecycle/SKILL.md` | No (rest) |
| `rust-axum` | `tests/golden/rust-axum/.claude/skills/x-dev-lifecycle/SKILL.md` | No (rest, grpc) |
| `typescript-nestjs` | `tests/golden/typescript-nestjs/.claude/skills/x-dev-lifecycle/SKILL.md` | No (rest) |

All profiles must be regenerated because `x-dev-lifecycle/SKILL.md` is a **core skill** (not conditional) -- it is copied identically to every profile. The template content is the same regardless of whether the project has CLI interface; the AI-level dispatch logic inside the template handles the conditional behavior.

Additionally, GitHub skill copies and Codex/agents copies of x-dev-lifecycle must also be updated:
- `tests/golden/*/.github/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/*/.agents/skills/x-dev-lifecycle/SKILL.md`

---

## 4. Dependency Direction Validation

This story has no impact on dependency direction. The changes are confined to:
- **Resource template files** (Markdown content, source of truth)
- **Golden test fixtures** (regenerated from the pipeline)

No new imports, no new module dependencies, no circular references introduced.

The existing data flow is preserved:
```
resources/skills-templates/core/x-dev-lifecycle/SKILL.md
    --> SkillsAssembler.assembleCore() (copies to .claude/skills/)
    --> GithubSkillsAssembler (copies github template to .github/skills/)
    --> CodexSkillsAssembler (copies to .agents/skills/)
```

---

## 5. Integration Points

### 5.1 With story-0004-0005 (Documentation Phase)

The CLI generator section integrates into the documentation phase infrastructure created by story-0004-0005. Specifically:

- The documentation phase defines the dispatch mechanism (read `interfaces`, invoke generators)
- This story adds the CLI generator as one of the dispatched generators
- The dispatch section should reference CLI alongside REST (story-0004-0007), gRPC (story-0004-0008), and Event-Driven (story-0004-0010)

### 5.2 With existing pipeline

No pipeline integration needed. The `SkillsAssembler` already copies `x-dev-lifecycle/SKILL.md` as a core skill to all output directories. The content change propagates automatically through the existing pipeline.

### 5.3 With golden file tests

The `byte-for-byte.test.ts` integration test compares pipeline output against golden files. After modifying the template, golden files must be regenerated to match.

---

## 6. Database Changes

**None.** This is a CLI code generator tool with no database.

---

## 7. API Changes

**None.** No programmatic API changes. The change is purely to the content of generated Markdown skill templates.

---

## 8. Event Changes

**None.** The project is not event-driven.

---

## 9. Configuration Changes

**None.** No changes to:
- `setup-config.*.yaml` templates
- `src/models.ts` (ProjectConfig)
- `src/assembler/conditions.ts` (interface detection)
- `src/assembler/skills-selection.ts` (skill selection logic)

The CLI interface type `"cli"` is already supported in the configuration schema (used by `python-click-cli` and `typescript-commander-cli` profiles).

---

## 10. Risk Assessment

### Low Risk

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Story-0004-0005 not yet implemented | Medium | High (blocker) | Verify story-0004-0005 is merged before starting. If not, coordinate with the documentation phase implementation to define the exact dispatch section structure. |
| Golden file regeneration scope | Low | Medium | All 8 profiles x 3 output targets (`.claude/`, `.github/`, `.agents/`) need regeneration. Use `npm run test -- --update` or equivalent to regenerate. |
| Dual copy drift | Low | Medium | Apply changes to both `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` AND `resources/github-skills-templates/dev/x-dev-lifecycle.md` simultaneously. Use diff to verify parity. |

### Negligible Risk

| Risk | Note |
|------|------|
| Backward compatibility | Additive change only. Existing generated files get more content; no existing content removed or restructured (beyond story-0004-0005's phase renumbering). |
| Test coverage impact | No new TypeScript code, so no coverage impact. Golden file tests cover the content changes. |
| Performance impact | No runtime performance impact. Template files are slightly larger but negligibly so. |

---

## 11. Implementation Order

1. **Verify prerequisite:** Confirm story-0004-0005 (Documentation Phase) is implemented and the phase structure exists in `x-dev-lifecycle/SKILL.md`.
2. **Modify source-of-truth template:** Add CLI generator section to `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` within the documentation phase.
3. **Mirror to GitHub copy:** Apply equivalent changes to `resources/github-skills-templates/dev/x-dev-lifecycle.md` (RULE-001).
4. **Regenerate golden files:** Run the pipeline for all 8 profiles and update golden files (`.claude/`, `.github/`, `.agents/` copies).
5. **Run tests:** Execute `npm test` to verify byte-for-byte golden file match and coverage thresholds.
6. **Verify skip behavior:** Confirm that the CLI generator content is present in all profiles (since it is a core skill with AI-level conditional dispatch, not pipeline-level filtering).

---

## 12. Detailed Content Structure for CLI Generator Section

The CLI generator section (added to the documentation phase in x-dev-lifecycle SKILL.md) should follow this structure:

```markdown
### CLI Documentation Generator (interface: cli)

> Invoked when project identity `interfaces` contains `"cli"`.
> Output: `docs/api/cli-reference.md`

Launch subagent or inline:

> **Scan** the project's CLI command definitions ({{FRAMEWORK}}-specific patterns).
> For Commander.js: scan `.command()`, `.option()`, `.argument()` chains.
> For Click: scan `@click.command()`, `@click.option()`, `@click.argument()` decorators.
> For Cobra: scan `cobra.Command{}` structs.
> For Clap: scan `#[derive(Parser)]` and `#[arg()]` attributes.
>
> **Generate** `docs/api/cli-reference.md` with:
>
> 1. `# CLI Reference` — title with project name
> 2. `## Quick Start` — at least 2 basic usage examples in code blocks
> 3. `## Global Flags` — table of flags applicable to all commands
> 4. `## Command: {name}` — one section per top-level command:
>    - Usage line: `$ {tool-name} {command} [flags] [args]`
>    - Flags table: | Flag | Type | Default | Required | Description |
>    - Arguments table: | Argument | Type | Required | Description |
>    - At least 1 example in code block
> 5. `### Subcommand: {parent} {child}` — nested sections with same structure
> 6. `## Exit Codes` — table: | Code | Meaning |
>
> If `interfaces` does NOT contain `"cli"`: skip silently (no output, no warning).
```

---

## 13. Files Changed Summary

| File | Action | Lines (est.) |
|------|--------|-------------|
| `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` | Modify | +30-50 |
| `resources/github-skills-templates/dev/x-dev-lifecycle.md` | Modify | +30-50 |
| `tests/golden/*/.claude/skills/x-dev-lifecycle/SKILL.md` (x8) | Regenerate | auto |
| `tests/golden/*/.github/skills/x-dev-lifecycle/SKILL.md` (x8) | Regenerate | auto |
| `tests/golden/*/.agents/skills/x-dev-lifecycle/SKILL.md` (x8) | Regenerate | auto |

**Total new TypeScript code:** 0 lines
**Total template content added:** ~60-100 lines (across 2 source files)
**Total golden files updated:** ~24 files (8 profiles x 3 output targets)
