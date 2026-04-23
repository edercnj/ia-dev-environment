# ADR-0013: Knowledge Packs live under `.claude/knowledge/`, not `.claude/skills/`

**Status:** Accepted
**Date:** 2026-04-23
**Deciders:** Platform Team
**Epic:** [EPIC-0051](../plans/epic-0051/epic-0051.md)

## Context

Knowledge Packs (KPs) — canonical reference documents for architecture, security, testing, coding standards, etc. — lived under `.claude/skills/{name}/SKILL.md`, side-by-side with invocable skills. This treated KPs as "fake skills":

- Claude Code only scans `.claude/skills/**/SKILL.md`, so putting KPs there satisfied the scan invariant but created semantic noise (~32 folders under `skills/` that are NOT skills).
- KPs carried a skill-only frontmatter (`user-invocable: false`, `allowed-tools`, `argument-hint`, `context-budget`) to satisfy the skill contract, despite not being skills.
- Fragile validation: every assembler handled the KP special case (`SkillsAssembler`, `SkillsCopyHelper`, `KnowledgePackSelection`).
- Documentation had to caveat "these are not really skills" in multiple places.

## Decision

Move all KPs to a dedicated sibling directory: `.claude/knowledge/`. Each KP becomes either:
- `knowledge/{name}.md` (simple KPs — no bundled references)
- `knowledge/{name}/index.md` + sibling reference files (complex KPs — formerly `skills/{name}/SKILL.md` + `skills/{name}/references/*.md`)

Frontmatter contract for files under `knowledge/`:
- **Allowed**: `name`, `description`, `tags`
- **Forbidden** (fail-fast on assembly): `user-invocable`, `allowed-tools`, `argument-hint`, `context-budget`

Implementation:
- New assembler `KnowledgeAssembler` mirrors `RulesAssembler` — copies verbatim with contract validation
- `SkillsAssembler` / `SkillsCopyHelper` / `KnowledgePackSelection` lose all KP handling (cleanup in story-0051-0006)
- Skills and rules that referenced `skills/{kp}/SKILL.md` now reference `knowledge/{kp}.md` or `knowledge/{kp}/index.md`

## Considered alternatives

### A. Keep the current layout, improve documentation
**Rejected.** Accretion of comments and docs can't fix a structural mismatch. The assemblers still special-case KPs; tests still fail on frontmatter validation for KPs that don't conform to either skill or pure-doc contract.

### B. Inline KPs into the skills that consume them
**Rejected.** A single KP is referenced by 3-10 skills. Inlining duplicates content and creates drift.

### C. Host KPs on an MCP resource server
**Rejected (overengineering).** Would require operating a sidecar service; existing Read tool already resolves filesystem paths fine. KPs are static documents, not dynamic resources.

### D. **Chosen:** `.claude/knowledge/` sibling directory (RULE-051-01 through RULE-051-07)

## Consequences

### Positive
- **Semantic clarity:** `.claude/skills/` contains only invocable skills; `.claude/knowledge/` contains references. Zero fake skills.
- **Simpler validation:** `KnowledgeAssembler` is ~150 lines, mirrors `RulesAssembler`, no special cases. `SkillsAssembler` gains no KP awareness.
- **Frontmatter contract explicit:** RULE-051-07 enforces schema; malformed source fails at build time.
- **Documentation alignment:** Docs can say "skills are invocable, knowledge packs are reference" without caveats.
- **Claude Code compatibility:** Reads still resolve via filesystem paths; no Claude Code contract violated.

### Negative
- **Big-bang migration:** RULE-051-05 — all consumers updated in one epic; branches outside the epic during the window will need rebase. Acceptable because EPIC-0051 is infra, not product work.
- **Generated projects need regeneration:** Users of older `ia-dev-env` output must regenerate to pick up the new layout. Documented in CHANGELOG as a MINOR bump (additive contract change; CLI API unchanged).

### Neutral
- Story-0051-0006 cleans up transient dual-output from `SkillsCopyHelper` introduced in story-0051-0002 to keep tests green during the retrofit window.

## Rollout

| Story | Scope | Status |
|---|---|---|
| 0051-0001 | `KnowledgeAssembler` + contract | Merged (PR #602) |
| 0051-0002 | Source-of-truth migration of 32 KPs | Merged (PR #604) |
| 0051-0003 | Retrofit ~21 skill consumers | Merged (PR #605) |
| 0051-0004 | Retrofit 9 rules | Merged (PR #606) |
| 0051-0005 | Goldens + end-to-end smoke | Merged (PR #607) |
| **0051-0006** | **ADR + SkillsCopyHelper cleanup + CHANGELOG** | **This ADR** |

## Compliance references

- RULE-051-01: single source of truth (`KnowledgeMigrationInvariantTest`)
- RULE-051-03: canonical path `knowledge/{kp}.md` (`SkillConsumerRetrofitInvariantTest`, `RuleKpRetrofitInvariantTest`)
- RULE-051-04: 32 KPs inventory (`plans/epic-0051/kp-inventory.txt`)
- RULE-051-05: big-bang migration (this epic)
- RULE-051-07: forbidden frontmatter (`KnowledgeAssembler.validateFrontmatter`)
- Rule 24: each story in 0051-0002 onwards produced specialist + TL reviews + verify envelope + completion report as mandatory evidence

## Audit command

```bash
# RULE-051-01 invariant (no dual source)
test ! -d java/src/main/resources/targets/claude/skills/knowledge-packs

# RULE-051-03 invariant (no old path references)
! grep -rq "skills/[a-z-]\+/SKILL\.md" \
    java/src/main/resources/targets/claude/skills/core/ \
    java/src/main/resources/targets/claude/rules/
```
