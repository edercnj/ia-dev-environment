---
status: Accepted
date: 2026-04-21
deciders:
  - Eder Celeste Nunes Junior (edercnj)
  - EPIC-0047 planning round (Architect, QA, Security, Tech Lead, PO)
story-ref: "story-0047-0001"
---

# ADR-0011: Shared Snippets — Inclusion Strategy for `_shared/`

> **Numbering note.** Story-0047-0001 (planning artifacts dated 2026-04-21)
> refers to this decision record as "ADR-0006" because the planning pass did
> not check the actual ADR index at authoring time. When implementation
> started, slot ADR-0006 was already occupied by the EPIC-0041 decision
> (file-conflict-aware parallelism). The inclusion-strategy ADR is therefore
> written into the next free slot, ADR-0011. The acceptance criteria of
> story-0047-0001 ("a formal decision record exists, is Accepted, and is
> linked from `_shared/README.md`") are met by this file.

## Status

Accepted | 2026-04-21

## Context

EPIC-0047 (Skill Body Compression Framework) aims to reduce the total line
count of the skill catalog from ~50,191 lines (v3.9.0 baseline, re-measured
2026-04-21) to ≤ 30,115 lines. One lever is **deduplicating cross-cutting
text** that currently lives verbatim in multiple `SKILL.md` files:

- A pre-commit error matrix duplicated in `x-git-commit`, `x-code-format`,
  and `x-code-lint` (~10 rows × 3 files).
- A TDD-tag glossary duplicated in `x-test-tdd`, `x-task-implement`, and
  `x-story-implement`.
- `DEP_*` / `STATE_*` / `RULE_*` exit-code families duplicated in `x-release`,
  `x-epic-implement`, and `x-story-implement`.

Story-0047-0001 introduces a new peer directory under the source of truth:
`java/src/main/resources/targets/claude/skills/_shared/` (sibling of `core/`,
`conditional/`, `knowledge-packs/`). The physical files exist; the open
question — which this ADR resolves — is **how consumer skills reference
them**.

Three options were considered.

### Option (a) — Placeholder Substitution at Assembly Time

Syntax in source `SKILL.md`:

```
{{INCLUDE:_shared/error-handling-pre-commit.md}}
```

Behavior: a new `SnippetIncluder` helper (called from
`SkillsAssembler.copyCoreSkill` or `CopyHelpers.replacePlaceholdersInDir`)
resolves the placeholder by reading `_shared/…` and inlining its content
verbatim into the output `SKILL.md` under `.claude/skills/…`. The output
goldens contain the fully-expanded body.

### Option (b) — Markdown Relative Links

Syntax in source `SKILL.md`:

```markdown
> See [`_shared/error-handling-pre-commit.md`](../../../_shared/error-handling-pre-commit.md)
> for the canonical pre-commit error matrix.
```

Behavior: **no assembler change**. The link is copied verbatim into the
output `.claude/skills/…/SKILL.md`. The LLM follows the link on demand when
the referenced content is actually needed; otherwise the link costs ~1 line.

### Option (c) — Symlinks

Behavior: each consumer skill carries a symlink under its `references/`
folder pointing to the `_shared/` file. Git would have to track the symlink
(platform-specific semantics on Windows vs. POSIX).

## Decision

**Adopt Option (b) — Markdown relative links.**

Consumer skills reference `_shared/*.md` via Markdown link syntax. No
placeholder resolver is added to the assembler. No symlinks are created.

## Rationale (Why Option (b) Wins)

1. **Alignment with the epic's thesis.** EPIC-0047 exists because **every
   `Skill()` invocation re-injects the full `SKILL.md` body** into the LLM
   context window (project memory `feedback_skill_body_token_cost.md`).
   Option (a) expands the body at assembly time → each invocation pays the
   cost of the inlined snippet N times per session. Option (b) keeps the
   body compact; the LLM fetches the linked snippet only when it needs to.
   **Option (a) actively contradicts the epic goal.**
2. **Zero assembler risk.** Option (b) ships with no Java change. No new
   `SnippetIncluder` class, no new path-traversal guard, no new unit tests,
   no risk of golden drift outside the intended cluster. The scope
   compression story ships faster and more safely.
3. **Reviewer UX (source).** A reviewer reading `x-git-commit/SKILL.md` in
   the source tree sees `[Pre-commit error matrix](…_shared/…)` — an
   unambiguous cross-reference. Option (a)'s `{{INCLUDE:…}}` token requires
   the reviewer to mentally expand the placeholder or run the assembler
   locally.
4. **Reader UX (output).** After generation, the `.claude/skills/…/SKILL.md`
   under Option (b) still shows a clickable Markdown link (IDEs and GitHub
   render it as a hyperlink). Under Option (a) the output silently carries
   duplicated content — the reader cannot tell it came from `_shared/`.
5. **Golden byte-stability.** Option (a) requires placeholder resolution to
   happen before the byte-diff capture; any change in resolution order
   (e.g., whitespace normalization) ripples through 17 profile goldens.
   Option (b) keeps resolution out of the pipeline → goldens only shift for
   the textual delta (link line vs. removed duplicated block).
6. **No new security surface.** Option (a) must normalize the placeholder
   path and reject traversal outside `_shared/` (Rule 06 Secure Defaults).
   Option (b) has no such surface because no path is resolved at assembly
   time — the Markdown link is static text.
7. **Consistent with existing patterns.** The project already uses Markdown
   cross-references extensively (ADR-to-ADR, SKILL-to-KP, skill-to-rule).
   A new placeholder syntax would be an unfamiliar eighth pattern.

## Example — Applied to the Pre-Commit Cluster

Before (duplicated in three `SKILL.md` files):

```markdown
## Error Handling

| Step | Error Condition | Exit Code | Behavior |
| :--- | :--- | :--- | :--- |
| format | Formatter binary not installed | FORMAT_TOOL_MISSING | Abort ... |
| ... (10 more rows) |
```

After (link, no duplication):

```markdown
## Error Handling

> **Pre-commit error matrix.** See
> [`_shared/error-handling-pre-commit.md`](../../../_shared/error-handling-pre-commit.md)
> for the canonical row set. Format / lint / compile / commit all share
> this matrix — a fix there propagates to every consumer skill.
```

The compressed form shrinks the consumer `SKILL.md` by ~10-15 lines per
skill for this particular snippet; multiplied across 17 profiles and 3
consumer skills, ~450+ lines leave the corpus while the source of truth
gains 1 file (~40 lines) and each consumer gains ~3 lines of link prose.
**Net: > 400 lines removed from the distributed corpus.**

## Consequences

### Positive

- Consumer skills are immediately smaller → lower re-injection cost per
  `Skill()` call (the direct epic objective).
- Zero assembler change → zero risk of a pipeline regression.
- Single source of truth (`_shared/*.md`) for cross-cutting content; edits
  propagate by regenerating (which already happens on every profile build).
- Works uniformly across all 17 profiles — no profile-specific logic.
- Reviewers can comment on one file when cross-cutting text needs changing.

### Negative

- The LLM must actively choose to follow a link when it needs the content.
  If it "forgets" to read the linked snippet, guidance may be lost.
  Mitigation: link prose is explicit ("See … for the canonical row set");
  the LLM is trained to follow Markdown links in skills.
- A reader skimming the output `SKILL.md` sees a link, not the content.
  Mitigation: this is by design for body compactness. The link is still
  one click away.
- Golden regeneration still runs for every pilot: the consumer `SKILL.md`
  shrinks by the duplicated block size, so goldens change. Expected and
  intentional.

### Neutral

- No new Java code is added, so no new coverage gate. Verification of the
  cluster is via golden byte-diff + a link-existence sanity test.
- `_shared/` is a peer of `core/` / `conditional/` / `knowledge-packs/`
  at the source-of-truth level; the assembler already walks that tree for
  reading purposes (no change required), but it does NOT copy `_shared/`
  to the output `.claude/skills/` — the output remains flat, user-invocable
  skills only. This ADR does NOT require copying `_shared/` to the output;
  see story-0047-0001 Section 5.2 for where consumer skills' relative
  links resolve to (inside the source-of-truth tree; assembler copies only
  per-skill directories).

## Alternatives Considered and Rejected

### Rejected: Option (a) — Placeholder Substitution

- Inlines duplicated text at assembly time → each re-injected `SKILL.md`
  pays the snippet cost on every invocation. **Directly contradicts
  EPIC-0047's goal of body compression.**
- Introduces a new assembler surface (`SnippetIncluder`) requiring
  path-traversal defense (Rule 06), unit tests (≥ 95% line / 90% branch),
  and potential golden regeneration orchestration changes.
- Changes the placeholder-resolution pipeline → risk of ordering bugs with
  the existing `{{PROJECT_NAME}}` / `{{LANGUAGE}}` substitution pass.

### Rejected: Option (c) — Symlinks

- Symlink semantics differ between POSIX and Windows (developer OS is
  cross-platform).
- Git tracks symlinks as a special blob type; some corporate Git hosts
  forbid symlinks in pushed commits.
- Does not compress anything — the referenced file is still copied verbatim
  to the consumer's `references/` folder at generation time (or the symlink
  dangles in the output). Either way, no win over Option (b).

## Related ADRs

- `ADR-0001-intentional-architectural-deviations-for-cli-tool.md` — the
  hexagonal-purity carve-outs for CLI-tool code; relevant because Option (a)
  would have added a new assembler component under `application/assembler/`.
- `ADR-0006-file-conflict-aware-parallelism.md` — occupies the slot the
  story originally referenced for this decision (see numbering note above).

## Story Reference

- `plans/epic-0047/story-0047-0001.md` — story that authorized this ADR.
- `plans/epic-0047/plans/tasks-story-0047-0001.md` — TASK-0047-0001-002 is
  the delivery vehicle for this ADR; TASK-0047-0001-004 is the pilot
  application to the pre-commit cluster.
- `plans/epic-0047/epic-0047.md` — epic rationale (baseline 50,191 lines;
  target ≤ 30,115 lines).
