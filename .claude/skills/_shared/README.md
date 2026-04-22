# `_shared/` — Cross-Cutting Snippets (Source of Truth)

This directory holds Markdown snippets that are **referenced by more than one
skill** but do not belong to any single skill, any category folder
(`core/`, `conditional/`), or any knowledge pack (`knowledge-packs/`). It is a
peer directory to those three — not a child of them.

## Purpose

Eliminate physical duplication of canonical cross-skill text at the source of
truth, so that:

- A single edit here propagates to every referring skill on next regeneration.
- The 17 output profiles do not each carry N independent copies of the same
  paragraph / table / glossary.
- Reviewers can point a PR comment at one file when correcting a cross-cutting
  concern (e.g., pre-commit error handling row).

## What Belongs Here

Content that is:

- **Cross-cutting** — referenced by ≥ 2 skills in different subjects (e.g.,
  pre-commit error handling is shared by `x-git-commit`, `x-code-format`,
  `x-code-lint`).
- **Stable** — not a per-project or per-stack customization (for that, use
  `knowledge-packs/` or templates with `{{PLACEHOLDER}}` tokens).
- **Narrow in scope** — a single topic per file, readable in isolation.

## What Does NOT Belong Here

- `SKILL.md` files. `_shared/` is NOT a skill — nothing under `_shared/`
  produces an invocable slash command.
- Project-wide behavior policy (e.g., "English only" global output policy) —
  that lives in `CLAUDE.md` and is loaded once per conversation.
- Stack-specific content — keep it in `knowledge-packs/{language}-{framework}/`.

## Inclusion Strategy

Skills reference `_shared/` snippets via **Markdown relative links** (not
placeholder substitution). See
[`adr/ADR-0011-shared-snippets-inclusion-strategy.md`](../../../../../../../adr/ADR-0011-shared-snippets-inclusion-strategy.md)
for the decision, alternatives considered, and rationale.

Note: the story planning artifact refers to this ADR as "ADR-0006"; the actual
sequential slot `ADR-0006` was occupied by EPIC-0041 (file-conflict-aware
parallelism). The inclusion-strategy ADR was assigned the next free slot,
`ADR-0011`, at implementation time. The story contract (3 snippet files + one
ADR explaining the strategy) is satisfied.

Example reference from a `SKILL.md`:

```markdown
> **Pre-commit error matrix.** See
> [`_shared/error-handling-pre-commit.md`](../../../_shared/error-handling-pre-commit.md)
> for the canonical row set shared by `x-git-commit`, `x-code-format`, and
> `x-code-lint`.
```

The LLM follows the link on demand; the skill body stays compact.

## Initial Inventory

| File | Scope | Consumer Skills |
| :--- | :--- | :--- |
| [`error-handling-pre-commit.md`](./error-handling-pre-commit.md) | Error rows for the `format -> lint -> compile -> commit` chain | `x-git-commit`, `x-code-format`, `x-code-lint` |
| [`tdd-tags-glossary.md`](./tdd-tags-glossary.md) | Canonical RED / GREEN / REFACTOR tag set and commit-footer format | `x-test-tdd`, `x-task-implement`, `x-story-implement` |
| [`exit-codes-common.md`](./exit-codes-common.md) | Recurring `DEP_*` / `STATE_*` / `RULE_*` exit-code families | `x-release`, `x-epic-implement`, `x-story-implement` |

## Adding a New Snippet

1. Confirm the content is referenced by ≥ 2 skills in different subjects. If it
   is used by only one skill, keep it in that skill's `references/` folder.
2. Add the Markdown file under `_shared/` with a Level-2 heading as the first
   non-front-matter line (so the file is identifiable as a standalone snippet).
3. Add a row to the "Initial Inventory" table above listing the consumer skills.
4. Update every consumer `SKILL.md` (or `references/*.md`) to reference the new
   file via a relative Markdown link.
5. Run `mvn process-resources` and verify goldens are unchanged (link-based
   inclusion does not expand content at assembly time — consumer skill bodies
   shrink by the size of the removed duplicated text; goldens reflect that).

## Related

- `adr/ADR-0011-shared-snippets-inclusion-strategy.md` — decision record for
  the link-based inclusion strategy (Option (b) chosen over placeholder
  substitution (a) and symlinks (c)).
- `CLAUDE.md` — project-wide global output policy (for content that applies to
  the entire session, not just cross-skill sharing).
- `_TEMPLATE-SKILL.md` (under `java/src/main/resources/shared/templates/`) —
  authoring template for new skills; includes guidance on when to extract
  content to `_shared/` vs. keep it inline.
