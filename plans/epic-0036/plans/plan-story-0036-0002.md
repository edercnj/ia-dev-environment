# Implementation Plan — story-0036-0002

## 1. Goal

Reorganize `targets/claude/skills/core/**` and `conditional/**` into 10 category subfolders while keeping the generated `.claude/skills/` output flat and identical to the previous output.

## 2. Architectural Decisions

### Category-level traversal with SKILL.md marker

`selectCoreSkills()` and `copyConditionalSkill()` must walk subdirectories up to **one category level** (no deeper recursion). A directory is considered a "skill directory" iff it contains a `SKILL.md` file. This marker-based approach:

- Supports exactly one category level (`core/plan/x-story-epic/SKILL.md` works; `core/plan/sub/x-foo/SKILL.md` is NOT discovered)
- Preserves legacy flat layout for tests (`core/x-review/SKILL.md` still works)
- Preserves the `lib/` exception (stays at `core/lib/{name}` with `lib/` prefix; `lib/` children also require the `SKILL.md` marker)
- Ignores intermediate category directories that themselves have no `SKILL.md`

### Output path invariant

Output remains `.claude/skills/{name}/SKILL.md` — the category subfolder is ABSENT from output. The `name` is derived from the basename of the skill directory (consistent with today), not from `{category}/{name}`.

## 3. Implementation Steps

1. TDD: Add failing test that creates `core/plan/x-foo/SKILL.md` and expects `selectCoreSkills()` to return `[x-foo]`.
2. Update `SkillsAssembler.selectCoreSkills()` to recursively walk; keep `lib/` special-case.
3. Update `copyCoreSkill()` to search for the skill by name across subdirectories (resolve src path dynamically).
4. Update `copyConditionalSkill()` similarly (recursive search under `conditional/`).
5. Physical move of all skills in SoT per category mapping from skill-renames.md section 1.
6. Regenerate golden files via `mvn process-resources && GoldenFileRegenerator`.
7. Run `mvn clean verify`.

## 4. Risks

- Risk: category intermediate dirs trigger name collisions with skill dirs. Mitigated by SKILL.md marker rule.
- Risk: test `lib/x-lib-tool` still must produce `lib/x-lib-tool` output prefix. Legacy lib/ logic preserved.
- Risk: some conditional skills may have SKILL.md directly under `conditional/{name}/`. The recursive resolver handles both depths transparently.
