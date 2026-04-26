# .claude/ -- Usage Guide

This directory contains all Claude Code configuration for the **ia-dev-environment** project.
It includes coding rules, skills (slash commands), knowledge packs, agents, and hooks.

> **Note:** The `.claude/` directory is a **generated output** produced by `ia-dev-env`.
> Do not edit it manually -- regenerate instead.

> **CRITICAL — Source of Truth:**
> The source of truth for skills, knowledge packs, agents, rules, and templates is `java/src/main/resources/targets/claude/`.
> The directories `.claude/` and `src/test/resources/golden/` are generated outputs — NEVER edit them directly.

> The `CLAUDE.md` file at the project root provides an executive summary loaded automatically in EVERY conversation.

> **Concluded — EPIC-0041 (File-Conflict-Aware Parallelism Analysis).**
> Planning now emits a structured `## File Footprint` / `## Story File Footprint` block on every task/story plan (`write:` / `read:` / `regen:`). The new skill `/x-parallel-eval --scope=epic|story|task` consumes those footprints, produces a collision matrix (hard / regen / soft), and recommends demotions to serial when two plans touch the same hotspot. `x-epic-map` Step 8.5 annotates the Implementation Map with "Restrições de Paralelismo"; `x-epic-implement` Phase 0.5.0 and `x-story-implement` Phase 1.5 run the gate and **degrade waves to serial with a visible warning** when a collision is detected (`ExecutionState.parallelismDowngrades`). Hotspots catalogued in RULE-004 (`SettingsAssembler.java`, `HooksAssembler.java`, `CLAUDE.md`, `CHANGELOG.md`, `pom.xml`, `.gitignore`, `src/test/resources/golden/**`). Plans predating this epic are treated as "footprint unknown" — warn, do not block (RULE-006).
> - Decision record: [`adr/ADR-0006-file-conflict-aware-parallelism.md`](adr/ADR-0006-file-conflict-aware-parallelism.md)
> - Retroactive diff patches for epics 0036–0040: [`plans/epic-0041/migrations/`](plans/epic-0041/migrations/) (EPIC-0040 flagged HIGH — hard conflict on `telemetry-phase.sh`).
> - Skill inventory gained `/x-parallel-eval` (category `plan/`).

> **Concluded — EPIC-0045 (CI Watch no Fluxo de PR).**
> Delivered `x-pr-watch-ci` skill (CI polling + Copilot review detection, 8 stable exit codes — RULE-045-05), Rule 21 (CI-Watch, RULE-045-01) with fallback matrix and opt-out via `--no-ci-watch`, and retrofits to `x-story-implement` (Phase 2.2.8.5), `x-task-implement --worktree` (Step 4.5), and `x-release` (flag `--ci-watch`). `Epic0045SmokeTest` validates end-to-end contract. `PrWatchStatusClassifier` + `PrWatchExitCode` (zero-I/O, fully testable). All 6 stories merged to develop.
> - Story index: [`plans/epic-0045/`](plans/epic-0045/)

> **Concluded — EPIC-0055 (Task Hierarchy & Phase Gate Enforcement).**
> Introduces Rule 25 (hierarchical task tracking, 4-level depth via `›` separator: Epic › Story › Phase › Wave/Cycle), skill `x-internal-phase-gate` (internal, `haiku`, 4 modes: pre/post/wave/final), and ADR-0014. Phase gates block `## Phase N` transitions until child tasks are `completed` AND expected artifacts exist on disk. Operators can see `"EPIC-0065 › Phase 3 › story-0065-0001 (in_progress)"` during execution. 4-layer enforcement: normative (Rule 25 + CLAUDE.md), Stop hook (`verify-phase-gates.sh`), PreToolUse hook (`enforce-phase-sequence.sh`), CI audit (`audit-task-hierarchy.sh` + `audit-phase-gates.sh`). All 8 canonical orchestrators retrofitted: `x-task-implement`, `x-story-implement`, `x-epic-implement`, `x-release`, `x-epic-orchestrate`, `x-review`, `x-review-pr`, `x-pr-merge-train`. Backward compatible via Rule 19: `taskTracking.enabled` defaults `false` for legacy epics (gates become no-ops); 28 legacy execution-state.json files migrated. `Epic0055FoundationSmokeTest` validates end-to-end.
> - Rule: [`.claude/rules/25-task-hierarchy.md`](.claude/rules/25-task-hierarchy.md)
> - Skill (internal): `x-internal-phase-gate` (not user-invocable)
> - Decision record: [`adr/ADR-0014-task-hierarchy-and-phase-gates.md`](adr/ADR-0014-task-hierarchy-and-phase-gates.md)
> - Epic index: [`plans/epic-0055/`](plans/epic-0055/)

> **In progress — EPIC-0046 (Lifecycle Integrity Phase 2 — CI enforcement).**
> Story-0046-0007 ships `LifecycleIntegrityAuditTest` (Maven CI-blocking). The audit scans every `SKILL.md` under `java/src/main/resources/targets/claude/skills/` for three Rule 22 regressions: `ORPHAN_PHASE` (dotted sub-section documented but not referenced elsewhere), `WRITE_WITHOUT_COMMIT` (write to `plans/epic-*/reports/` with no `x-git-commit` in the next 20 lines), and `SKIP_IN_HAPPY_PATH` (`--skip-verification` / `--skip-status-sync` used outside `## Recovery` / `## Error Handling`). Baseline at `audits/lifecycle-integrity-baseline.txt` tolerates current TOC-style sub-sections; any NEW violation fails the build with `LIFECYCLE_AUDIT_REGRESSION`. Escape hatch: place `<!-- audit-exempt -->` on the line immediately before (or on) the intentional violation; keep usage rare (reviewed exceptions only). Standalone CLI: `java -cp target/test-classes:target/classes dev.iadev.adapter.inbound.cli.LifecycleAuditCli scan [--skills-root <path>] [--json]` (exit 0 / 11 / 2).
> - Story: [`plans/epic-0046/story-0046-0007.md`](plans/epic-0046/story-0046-0007.md)

> **In progress — EPIC-0043 (Interactive Gates Convention).**
> Standardizes interactive decision gates across orchestrating skills (`x-release`, `x-story-implement`, `x-epic-implement`, `x-review-pr`) with a fixed 3-option menu (PROCEED / FIX-PR / ABORT) as the default behavior. Menu is now default; `--non-interactive` replaces the patchwork of opt-in flags for CI/automation. FIX-PR slot invokes `x-pr-fix`/`x-pr-fix-epic` via Rule 13 INLINE-SKILL and loops back to the same menu. Guard-rail caps 3 consecutive fix attempts with `GATE_FIX_LOOP_EXCEEDED`. Rule 20 + ADR-0010 published in story-0043-0001; retrofits follow in stories 0043-0002 through 0043-0006.
> - Decision record: [`adr/ADR-0010-interactive-gates-convention.md`](adr/ADR-0010-interactive-gates-convention.md)
> - Story index: [`plans/epic-0043/`](plans/epic-0043/)

> **In progress — EPIC-0058 (Audit Scripts Lifecycle & Generation).**
> Formalizes the lifecycle of governance audit gates: creates Rule 26 "Audit Gate Lifecycle" + ADR-0015 (4-layer taxonomy: Hook/CI script/Java test/Workflow); creates 3 missing CI scripts referenced in Rules 19/21/22 (`audit-flow-version.sh`, `audit-epic-branches.sh`, `audit-skill-visibility.sh`); introduces `ScriptsAssembler` so generated projects inherit governance gates; regenerates golden files for 9 profiles; adds `audit.yml` CI workflow.
> - Rule: [`.claude/rules/26-audit-gate-lifecycle.md`](.claude/rules/26-audit-gate-lifecycle.md)
> - Decision record: [`adr/ADR-0015-audit-gate-lifecycle.md`](adr/ADR-0015-audit-gate-lifecycle.md)
> - Epic index: [`plans/epic-0058/`](plans/epic-0058/)

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
| 23 | `23-model-selection.md` | model selection strategy (Opus/Sonnet/Haiku tiers, enforcement points, CI audit contract) |
| 25 | `25-task-hierarchy.md` | task hierarchy (4-level) + phase gates contract (EPIC-0055) |
| 26 | `26-audit-gate-lifecycle.md` | audit gate taxonomy (4 layers) + naming, exit codes, self-check contract (EPIC-0058) |

**Total: 13 rules** (gaps at 10, 11, 12 reserved for conditional rules: `10-anti-patterns.*`, `11-security-pci`, `12-security-anti-patterns`; gap at 24 reserved for Rule 24 "Execution Integrity" tracked separately)

### Numbering

- Gaps in numbering allow future insertion without renumbering existing rules.

---

## Skills (Slash Commands)

Skills are invoked by the user via `/name` in chat. They are lazy-loaded (only load when invoked).

A complete list of skills with descriptions is generated in `.claude/README.md` by the `ia-dev-env` generator.

### Authoring a New Skill

- Start from `java/src/main/resources/shared/templates/_TEMPLATE-SKILL.md` (authoring template for SKILL.md files).
- The template includes a "## Telemetry (Optional)" section with plug-and-play helper calls (`telemetry-phase.sh start/end`, `subagent-start/end`, `mcp-start/end`). Copy-paste into numbered phases of the new skill to keep telemetry coverage close to 100% as the catalog grows (EPIC-0040).
- Canonical example: `x-story-implement` — review its phase markers for a working reference.
- See `.claude/rules/13-skill-invocation-protocol.md` for the markers contract.

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

## Telemetry

Every `ia-dev-env`-generated project ships with telemetry capture enabled by default. Skill executions, phase boundaries, subagent lifecycles, and tool calls are recorded as NDJSON under `plans/epic-*/telemetry/events.ndjson`, producing an auditable timeline of how long each part of an epic / story / task actually took. The design is documented in [`adr/ADR-0005-telemetry-architecture.md`](adr/ADR-0005-telemetry-architecture.md); the privacy contract is enforced by [Rule 20 — Telemetry Privacy](.claude/rules/20-telemetry-privacy.md) and the scrubber at `dev.iadev.telemetry.TelemetryScrubber`.

Capture happens through two cooperating layers:

- **Hook-based (automatic).** Five Bash entrypoint scripts under `.claude/hooks/` are registered in `settings.json` and fire on `SessionStart`, `PreToolUse`, `PostToolUse`, `SubagentStop`, and `Stop`. Additional helper scripts in `.claude/hooks/` (e.g., `telemetry-emit.sh`, `telemetry-lib.sh`, `telemetry-phase.sh`) are copied alongside them but are not registered as hook events. No per-skill code is required.
- **In-skill phase markers.** Implementation, planning, and creation skills call `telemetry-phase.sh start|end` around each numbered phase; the `_TEMPLATE-SKILL.md` authoring template includes a copy-paste-ready "Telemetry (Optional)" section.

Two skills consume the NDJSON:

```bash
# Point-in-time report for one or more epics (Mermaid Gantt + aggregates)
/x-telemetry-analyze --epic EPIC-0040

# Cross-epic P95 regression detector (top-10 slowest skills)
/x-telemetry-trend --last 5 --threshold-pct 20
```

Opt out globally with `CLAUDE_TELEMETRY_DISABLED=1`, or per-project by adding the nested YAML block below to the generator YAML (requires regeneration):

```yaml
telemetry:
  enabled: false
```

(The parser `ProjectConfig.parseTelemetryEnabled` expects the nested key; a flat `telemetryEnabled` line is ignored.)

EPIC-0040 shipped this stack — see the [CHANGELOG](CHANGELOG.md#380---2026-04-17) for the full release notes.

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
