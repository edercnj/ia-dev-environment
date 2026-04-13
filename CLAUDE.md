# .claude/ -- Usage Guide

This directory contains all Claude Code configuration for the **ia-dev-environment** project.
It includes coding rules, skills (slash commands), knowledge packs, agents, and hooks.

> **Note:** The `.claude/` directory is a **generated output** produced by `ia-dev-env`.
> Do not edit it manually -- regenerate instead.

> **CRITICAL — Source of Truth:**
> The source of truth for skills, knowledge packs, agents, rules, and templates is `java/src/main/resources/targets/claude/`.
> The directories `.claude/` and `src/test/resources/golden/` are generated outputs — NEVER edit them directly.

> The `CLAUDE.md` file at the project root provides an executive summary loaded automatically in EVERY conversation.

> **In progress — EPIC-0036 (Skill Taxonomy Refactor).**
> The source of truth for skills under `java/src/main/resources/targets/claude/skills/` is being reorganized into 10 category subfolders (`plan/`, `dev/`, `test/`, `review/`, `security/`, `code/`, `git/`, `pr/`, `ops/`, `jira/`), and ~19 skills will be renamed to a consistent `x-{subject}-{action}` scheme. The generated output `.claude/skills/` remains **flat** — user-facing invocation paths are preserved.
> - Decision record: [`adr/ADR-0003-skill-taxonomy-and-naming.md`](adr/ADR-0003-skill-taxonomy-and-naming.md)
> - Rename staging checklist: [`plans/epic-0036/skill-renames.md`](plans/epic-0036/skill-renames.md)
> - Current skill names are the renamed forms (e.g., `/x-epic-create`, `/x-task-implement`, `/x-test-e2e`). Do not use the old pre-rename names.

## Structure

```
CLAUDE.md                   <-- Executive summary (project root, loaded automatically)
.claude/
|-- README.md               <-- Usage guide
|-- settings.json           <-- Shared settings (committed to git)
|-- settings.local.json     <-- Local overrides (gitignored)
|-- hooks/                  <-- Automations (post-compile, etc.)
|-- rules/                  <-- Project rules (loaded into system prompt)
|-- skills/                 <-- Skills invocable via /command
|   +-- {knowledge-packs}/  <-- Knowledge packs (not invocable, referenced internally)
+-- agents/                 <-- AI personas (used by skills and lifecycle)
```

### settings.json vs settings.local.json

- **`settings.json`**: Team settings (permissions, hooks). Committed to git.
- **`settings.local.json`**: Local overrides. In `.gitignore`. Overrides `settings.json`.

---

## Rules

Rules are loaded automatically into the system prompt of EVERY conversation.
They define mandatory standards that Claude MUST follow when generating code.

| # | File | Scope |
|---|------|-------|
| 01 | `01-project-identity.md` | project identity |
| 02 | `02-domain.md` | domain |
| 03 | `03-coding-standards.md` | coding standards |
| 04 | `04-architecture-summary.md` | architecture summary |
| 05 | `05-quality-gates.md` | quality gates |
| 06 | `06-security-baseline.md` | security baseline |
| 07 | `07-operations-baseline.md` | operations baseline |
| 08 | `08-release-process.md` | release process |
| 09 | `09-branching-model.md` | branching model (Git Flow) |
| 13 | `13-skill-invocation-protocol.md` | skill invocation protocol (delegation syntax) |

**Total: 10 rules** (gaps at 10, 11, 12 reserved for conditional rules: `10-anti-patterns.*`, `11-security-pci`, `12-security-anti-patterns`)

### Numbering

- Gaps in numbering allow future insertion without renumbering existing rules.

---

## Skills (Slash Commands)

Skills are invoked by the user via `/name` in chat. They are lazy-loaded (only load when invoked).

A complete list of skills with descriptions is generated in `.claude/README.md` by the `ia-dev-env` generator.

### Usage Examples

```bash
# Run a specific skill
/skill-name argument

# Get help on available skills
# Type / in the chat to see the full list
```

---

## Knowledge Packs, Agents, Hooks

- **Knowledge Packs** (`user-invocable: false`): referenced internally by agents and skills; do not appear in the `/` menu.
- **Agents**: system prompts defining specialized personas; used by skills via Task tool, not invoked directly.
- **Hooks**: scripts executed on Claude Code events, configured in `settings.json` under `hooks`.

---

## Settings & Artifact Conventions

- `settings.json` (committed): team permissions and hooks.
- `settings.local.json` (gitignored): personal overrides.
- Rules: `NN-name.md` (numbered, no frontmatter).
- Skills: `skills/{name}/SKILL.md` with YAML frontmatter (name, description).
- Agents: `{name}.md`. Hooks: `.sh` / `.json`.

---

## Plan & Review Templates

Templates provide standardized output formats for planning and review artifacts produced by skills.
They contain `{{PLACEHOLDER}}` tokens resolved at runtime by the LLM, not during generation.
Content is copied verbatim by `PlanTemplatesAssembler` (RULE-003).

> **Fallback:** Templates are optional -- skills degrade gracefully without them.
> If a template is not found, skills use inline formatting as fallback and log a warning.

> **Location:** Templates are written to `.claude/templates/` only. Multi-target output (e.g., `.github/templates/`) was removed in EPIC-0034.

| Template | Produced By | Saved To | Pre-Check |
|----------|-------------|----------|-----------|
| `_TEMPLATE-IMPLEMENTATION-PLAN.md` | x-story-implement (Phase 1B) | `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md` | Yes |
| `_TEMPLATE-TEST-PLAN.md` | x-test-plan | `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md` | Yes |
| `_TEMPLATE-ARCHITECTURE-PLAN.md` | x-arch-plan | `plans/epic-XXXX/plans/arch-story-XXXX-YYYY.md` | Yes |
| `_TEMPLATE-TASK-BREAKDOWN.md` | x-lib-task-decomposer | `plans/epic-XXXX/plans/tasks-story-XXXX-YYYY.md` | Yes |
| `_TEMPLATE-SECURITY-ASSESSMENT.md` | x-story-implement (Phase 1E) | `plans/epic-XXXX/plans/security-story-XXXX-YYYY.md` | Yes |
| `_TEMPLATE-COMPLIANCE-ASSESSMENT.md` | x-story-implement (Phase 1F) | `plans/epic-XXXX/plans/compliance-story-XXXX-YYYY.md` | Yes |
| `_TEMPLATE-SPECIALIST-REVIEW.md` | x-review | `plans/epic-XXXX/plans/review-story-XXXX-YYYY.md` | No |
| `_TEMPLATE-TECH-LEAD-REVIEW.md` | x-review-pr | `plans/epic-XXXX/plans/techlead-review-story-XXXX-YYYY.md` | No |
| `_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md` | x-review | `plans/epic-XXXX/plans/review-dashboard-story-XXXX-YYYY.md` | No |
| `_TEMPLATE-REVIEW-REMEDIATION.md` | x-story-implement (Phase 5) | `plans/epic-XXXX/plans/remediation-story-XXXX-YYYY.md` | No |
| `_TEMPLATE-EPIC-EXECUTION-PLAN.md` | x-epic-implement | `plans/epic-XXXX/plans/execution-plan-epic-XXXX.md` | Yes |
| `_TEMPLATE-PHASE-COMPLETION-REPORT.md` | x-epic-implement | `plans/epic-XXXX/reports/phase-report-epic-XXXX.md` | No |

**Total: 12 plan & review templates** (copied to `.claude/templates/`)

---

## Generation Summary

| Component | Count |
|-----------|-------|
| Plan Templates (.claude) | 12 |

---

## Tips

- Rules are always active -- no invocation needed.
- Skills are lazy -- load when you type `/name`.
- Knowledge Packs do not appear in `/` -- used internally by agents.
- Hooks run automatically on events like post-compile.
- To add a skill / rule, create the file under the matching directory.
- The `.claude/` directory is generated -- run `ia-dev-env generate` to regenerate.

Generated by `ia-dev-env`.
