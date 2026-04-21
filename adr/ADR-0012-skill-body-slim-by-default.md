---
status: Accepted
date: 2026-04-21
deciders:
  - Eder Celeste Nunes Junior (edercnj)
  - EPIC-0047 planning round (Architect, QA, Security, Tech Lead, PO)
story-ref: "story-0047-0002"
---

# ADR-0012: Skill Body Slim-by-Default (Flipped Orientation)

> **Numbering note.** Story-0047-0002 (planning artifacts dated 2026-04-21)
> refers to this decision record as "ADR-0007" because the planning pass did
> not check the actual ADR index at authoring time. When implementation
> started, slot ADR-0007 was already occupied by the console-progress-reporter
> stdout-contract decision. The flipped-orientation ADR is therefore written
> into the next free slot, ADR-0012 (after ADR-0011, which story-0047-0001
> similarly had to renumber from its originally-planned ADR-0006). The
> acceptance criteria of story-0047-0002 ("ADR-0007 mergeada como Accepted"
> per §4 DoD) are met by this file; the story §3.2 path reference is updated
> in the same commit.

## Status

Accepted | 2026-04-21

## Context

EPIC-0047 (Skill Body Compression Framework) set an aspirational target of
reducing the total `SKILL.md` corpus from ~50,191 lines (v3.9.0 baseline) to
≤ 30,115 lines. The runtime load characteristic that motivates compression is
well-understood by the team and documented in project memory: every time the
Claude Code harness invokes a skill via the Skill tool, the full `SKILL.md`
body is re-injected into the conversation context. For orchestrator skills
that invoke N sub-skills in a single story implementation, this multiplies
cost by N × body-size.

Story-0030-0006 (merged in v2.0.0) introduced a coping mechanism called
**`## Slim Mode`**: skill authors appended a "## Slim Mode" section to the
end of a verbose `SKILL.md` file, containing a condensed re-statement of the
protocol that sub-agents were instructed to read "only this section" when
invoked in a context-pressured run. The pattern was invalid at the
architecture level:

1. The Claude Code runtime loads `SKILL.md` as a single unit. There is no
   API, directive, or convention that lets the harness load **only a
   specific section** of a markdown file.
2. The `## Slim Mode` text was consultative, not enforced. Sub-agents had
   no mechanical way to honor "only read this section" — the whole file was
   already in context.
3. The pattern was append-only, meaning the verbose body still shipped in
   full on every invocation. Instead of reducing footprint, it **increased**
   it (body + slim re-statement).
4. In practice, several skills accumulated a `## Slim Mode` section that
   drifted out of sync with the actual body, producing contradictory
   instructions. This regressed silently because there is no linter that
   cross-checks the two halves.

Bucket A item A5 of the `mellow-mixing-rainbow.md` plan (PR #534) deleted the
five dead `## Slim Mode` sections from `x-test-tdd`, `x-story-implement`,
`x-git-commit`, `x-code-format`, and `x-code-lint`. That cleanup removed the
contradiction but did not solve the underlying problem: the verbose body is
still re-injected on every invocation.

This ADR records the architectural decision that replaces `## Slim Mode`.

## Decision

**SKILL.md is the minimum viable behavioral contract (default-slim). The
verbose protocol detail lives in `references/full-protocol.md` and is
loaded on-demand.**

Concretely:

1. Every `SKILL.md` under `java/src/main/resources/targets/claude/skills/core/**/`
   MUST contain only the five canonical slim sections:
   - `## Triggers` — 1–3 lines describing when to invoke.
   - `## Parameters` — table of args + flags.
   - `## Output Contract` — what the skill produces (state-file path, exit
     code, file modification).
   - `## Error Envelope` — table of error codes with links to
     `_shared/exit-codes-common.md` where applicable (ADR-0011).
   - `## Full Protocol` — one sentence plus a link to
     `references/full-protocol.md`.
2. Anything that does NOT fit those five categories — multi-step workflows,
   examples over 10 lines, expanded JSON schemas, large Mermaid diagrams,
   historical rationale — moves to a sibling `references/full-protocol.md`.
3. The runtime reads `SKILL.md` on every invocation; it reads
   `references/full-protocol.md` only when the slim contract is insufficient
   for the task at hand. That conditional read costs one extra `Read` tool
   call — an explicit, observable cost.
4. The five skills that previously carried `## Slim Mode` — `x-test-tdd`,
   `x-story-implement`, `x-git-commit`, `x-code-format`, `x-code-lint` —
   are the **pilot**. Future skills migrate to this pattern as they enter
   maintenance (no force-migration of the corpus).
5. Decision criterion for "minimum viable": the slim `SKILL.md` MUST be
   executable standalone for the happy path. Atypical cases may require
   reading `references/full-protocol.md`; that is acceptable and documented
   in §Consequences below.

## Consequences

### Positive

- Orchestrator skills that invoke N sub-skills pay the body cost once
  per invocation instead of once-per-verbose-body. For the pre-commit chain
  (invoked by every TDD cycle), this is a measurable reduction on every
  story run.
- The slim contract is audit-friendly: reviewers can read a 200-line file
  and see the complete behavioral surface in one viewport, without hunting
  for edge-case detail buried in workflow prose.
- Dead text ("load only this section") is removed from the source of truth
  permanently.
- The pattern is consistent with ADR-0011 (shared snippets via
  `_shared/`): both ADRs reduce footprint by moving text out of the skill
  body, but attack different axes. ADR-0011 deduplicates cross-cutting
  text; ADR-0012 demotes skill-local detail. They compose cleanly.

### Negative

- Reviewers who want the full protocol now open two files (SKILL.md +
  references/full-protocol.md) instead of one. This is a mild ergonomic
  tax on human reviewers; IDE "jump to link" mitigates it somewhat.
- The LLM at runtime may occasionally need to read
  `references/full-protocol.md` for an edge case the slim body does not
  cover. Each such read is an additional `Read` tool call. Empirically, a
  happy-path invocation reads SKILL.md once and never touches
  `references/`; worst case for an atypical invocation is two reads. Net
  cost is still lower than re-injecting the verbose body on every
  invocation, but the savings asymmetry (cheap happy path, slightly more
  expensive edge path) is real and acknowledged.
- Source-of-truth discipline now spans two files per skill instead of one.
  Edits that conceptually belong to both (e.g., a new parameter whose
  mechanics require a full-protocol section update) require two-file
  patches.

### Neutral

- No change to the generated `.claude/skills/` output tree beyond the file
  that already existed: `references/` siblings are copied verbatim by the
  existing `SkillsAssembler` path — no new assembler code is required.
- The SkillSizeLinter introduced by story-0047-0003 already enforces the
  invariant "SKILL.md > 500 lines → `references/` sibling required". This
  ADR tightens the target (200–250 lines) but the underlying linter is
  unchanged. The baseline file `audits/skill-size-baseline.txt` contains
  entries for skills that have not yet migrated; entries are REMOVED from
  that file as each skill adopts the slim contract, not added.

## Alternatives Considered

### Alternative A — Keep the status quo (do nothing)

Leave all `SKILL.md` files at their current verbose size; rely on operator
discipline to invoke fewer orchestrators.

**Rejected.** The cost is real and multiplicative (N sub-skills × verbose
body each story). Story-0030-0006 already tried this route and discovered
the cost was high enough to motivate `## Slim Mode` in the first place.
Removing `## Slim Mode` without a replacement pattern regresses the
corpus to the pre-0030-0006 state.

### Alternative B — Restore `## Slim Mode` (append-only slim section)

Re-introduce the `## Slim Mode` section and document an operator
convention that sub-agents read "only that section".

**Rejected.** Architecturally broken for the reasons stated in §Context:
the runtime loads the whole file, the directive is consultative, and the
body grows instead of shrinking. The team has direct empirical evidence
(the five dead `## Slim Mode` sections deleted by Bucket A A5) that
authors cannot maintain two halves of the same document in sync.

### Alternative C — Split each skill into two peer skills

Have `/x-git-commit` (happy path) and `/x-git-commit-full` (complete
protocol) as separate invocable skills.

**Rejected.** Explodes the `/` command surface (roughly doubles it),
forces operators to know which variant to invoke, and creates a new
failure mode where a sub-skill chain calls the wrong variant. The
reference-link pattern keeps the invocation surface stable and defers the
"full vs slim" decision to runtime (inside the slim skill's own body, if
an edge case is detected).

## Migration Path

- The five pilot skills named in §Decision migrate in this story
  (story-0047-0002) via per-task atomic commits, one skill per task
  (TASK-0047-0002-002 through 006).
- Future skills migrate opportunistically: when a `SKILL.md` that is > 250
  lines receives a substantive edit, the author carves it to the
  slim+references shape as part of that edit. The SkillSizeLinter surfaces
  offenders at CI time.
- No force-migration deadline is set. The `audits/skill-size-baseline.txt`
  file shrinks organically as stories migrate skills; when it reaches
  empty, the brownfield phase is complete.

## Related ADRs

- [ADR-0011 — Shared Snippets Inclusion Strategy](ADR-0011-shared-snippets-inclusion-strategy.md)
  (complementary decompositionn axis: cross-cutting text via link-based
  include; ADR-0012 targets skill-local detail via references/full-protocol.md).
- ADR-0003 — Skill Taxonomy and Naming (governs where skill directories
  live and how they are named; flipped orientation inherits that layout).

## Story Reference

- story-0047-0002 — "Retirar pattern Slim Mode + ADR flipped orientation"
  (final story of EPIC-0047 Phase 1).
