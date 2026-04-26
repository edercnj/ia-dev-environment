# Rule 22 — Skill Visibility

> **Related:** Rule 13 (Skill Invocation Protocol), Rule 21 (Epic Branch Model).
> **Introduced by:** EPIC-0049 (Refatoração do Fluxo de Épico) — RULE-006 (Convenção `x-internal-*`).

## Purpose

Skills in this repository fall into two visibility classes:

| Class | Prefix | User-Invocable? | Shows in `/help`? |
| :--- | :--- | :--- | :--- |
| **Public** | `x-{subject}-{action}` (e.g., `x-pr-create`, `x-epic-implement`) | Yes | Yes |
| **Internal** | `x-internal-{subject}-{action}` (e.g., `x-internal-status-update`) | **No** | **No** |

Internal skills are implementation details extracted from orchestrators to reduce SKILL.md size, isolate concerns, and enable reuse. They are invoked **only** by other skills via the Skill tool — never typed by a user in chat. Rule 22 makes that boundary explicit, visible, and mechanically enforceable.

## Naming Convention

- Public skills: `x-{subject}-{action}` — 2-to-3-token kebab-case name (e.g., `x-epic-create`, `x-task-implement`, `x-pr-watch-ci`).
- Internal skills: `x-internal-{subject}-{action}` — MUST carry the `x-internal-` prefix exactly. No exceptions.
- Subdir convention (source of truth): `java/src/main/resources/targets/claude/skills/internal/{group}/x-internal-{name}/SKILL.md` where `{group}` is one of `epic`, `story`, `task`, `git`, `pr`, `report`, `state`.
- Generated output: **flat** under `.claude/skills/` — the subdir structure exists only in source-of-truth to aid navigation.

## Frontmatter Contract

Every internal skill's SKILL.md MUST start with:

```yaml
---
name: x-internal-{subject}-{action}
description: <one-line description>
visibility: internal
user-invocable: false
allowed-tools: [...]
---
```

Both `visibility: internal` AND `user-invocable: false` are required. The generator uses these two fields independently:

- `visibility: internal` — filters the skill from the `/help` output and from the user-facing slash-command menu.
- `user-invocable: false` — additionally blocks direct user invocation at the runtime layer (the CLI refuses to dispatch).

Public skills MUST NOT set these fields (or MUST set `visibility: public` explicitly if the project requires it).

## Body Marker

Every internal skill's SKILL.md body MUST open with the following visible block, placed between the frontmatter and the first prose section:

```markdown
> 🔒 **INTERNAL SKILL** — Invoked only by other skills via the Skill tool. Not user-invocable.
```

This makes the visibility class obvious when a human opens the file, independent of frontmatter parsing.

## Forbidden

- Documenting an `x-internal-*` skill as a user command (`## Triggers` section listing `/x-internal-foo ...`).
- Prose in any user-facing file (README, CHANGELOG, user-facing docs, `## Examples` in public SKILL.md) that instructs the user to "run `/x-internal-foo`".
- Calling an internal skill from a non-skill context (e.g., CI scripts). Internal skills are only composable from other skills.
- Renaming a public skill to `x-internal-*` without a deprecation announcement (breaks user muscle memory).
- Renaming an `x-internal-*` skill to a public name without adding it to the `/help` inventory.

## Permitted

- A public skill's INLINE-SKILL delegation (Rule 13 Pattern 1) invoking an internal skill:
  ```markdown
  Invoke the `x-internal-status-update` skill via the Skill tool:

      Skill(skill: "x-internal-status-update", args: "--key executionState --value ...")
  ```
- An internal skill invoking another internal skill via the same pattern.
- `## Integration Notes` in a public SKILL.md listing internal skills it depends on (documents the call graph for maintainers).

## Audit Script

CI script `scripts/audit-skill-visibility.sh` scans every SKILL.md under `java/src/main/resources/targets/claude/skills/` and verifies:

1. **Prefix/frontmatter consistency.** If the directory name contains `x-internal-`, the frontmatter MUST have `visibility: internal` + `user-invocable: false`. Conversely, `visibility: internal` MUST only appear under an `x-internal-*` path.
2. **Body marker present.** Internal skills MUST contain the `🔒 **INTERNAL SKILL**` marker in the first 10 lines of body content.
3. **No user-facing trigger.** Internal skills' `## Triggers` section (if present) MUST NOT list bare-slash commands — the section exists only for documentation, never for user dispatch.
4. **No cross-reference in user-facing docs.** README.md, CHANGELOG.md, and any file under `docs/` MUST NOT contain a `/x-internal-` slash in prose (exception: audit/migration docs explicitly describing the convention).

Any violation exits the script with code `SKILL_VISIBILITY_VIOLATION` (exit 22) and fails the CI build.

## Migration Path

When extracting an internal skill from an existing public orchestrator:

1. Pick the name: `x-internal-{subject}-{action}` matching the extracted concern.
2. Create `java/src/main/resources/targets/claude/skills/internal/{group}/x-internal-{name}/SKILL.md` with the required frontmatter + body marker.
3. Replace the inline code in the orchestrator with an `INLINE-SKILL` invocation (Rule 13 Pattern 1).
4. Regenerate `.claude/skills/` via `mvn process-resources && java ... GoldenFileRegenerator`.
5. Verify the audit script passes: `scripts/audit-skill-visibility.sh`.

Typical candidates for internal extraction: shared subroutines used by ≥ 2 public orchestrators; size-reduction targets (> 150 lines of incidental code in a SKILL.md); testability isolation.

## Rationale

Before EPIC-0049, `x-epic-implement` (~1100 lines) and `x-story-implement` (~900 lines) carried inline Bash for branch creation, status updates, plan construction, integrity gates, and report rendering. Every user-initiated `/x-story-implement` re-injected the full body into the LLM context, inflating token cost. Rule 22 codifies the extraction pattern so future size-reduction work has a stable convention to follow.

---

> **Catalogado em:** [`docs/audit-gates-catalog.md`](../../docs/audit-gates-catalog.md)

