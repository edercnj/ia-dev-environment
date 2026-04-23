---
name: x-internal-story-build-plan
description: "Orchestrates parallel story-planning (Phase 1 carve-out of x-story-implement): invokes x-arch-plan (Step 1A) and then dispatches 5 sibling Agent subagents in ONE assistant message for implementation plan, test plan, task breakdown, security assessment, and compliance assessment (Steps 1B-1F). Applies the Rule 13 SUBAGENT-GENERAL pattern as the canonical parallel-planning gateway. Scope-aware: SIMPLE skips 1E/1F. Returns a consolidated envelope of artifact paths to the calling orchestrator. Fifth skill in the x-internal-* convention and the second under internal/plan/ (after x-internal-story-load-context)."
visibility: internal
user-invocable: false
allowed-tools: Bash, Skill, Agent
argument-hint: "--story-id <story-XXXX-YYYY> --epic-id <XXXX> [--scope <SIMPLE|STANDARD|COMPLEX>] [--skip-review]"
category: internal-plan
context-budget: medium
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

> 🔒 **INTERNAL SKILL**
> Esta skill é invocada apenas por outras skills (orquestradores).
> NÃO é destinada a invocação direta pelo usuário.
> Caller principal: x-story-implement (Phase 1 carve-out).
> Quinta skill da convenção `x-internal-*` (após x-internal-status-update
> pilot 0049-0005, x-internal-report-write 0049-0006,
> x-internal-args-normalize 0049-0007, e x-internal-story-load-context
> 0049-0011). Segunda skill na subdir `internal/plan/` (co-habita com
> x-internal-story-load-context). A subdir `plan/` agrupa as skills que
> orquestram lógica de planejamento; difere de `internal/ops/`, cujas
> sibling skills mutam estado (execution-state.json, reports).

# Skill: x-internal-story-build-plan

## Purpose

Carve out Phase 1 (Architecture Planning + Parallel Planning) of
`x-story-implement` into a single orchestration skill. The ~250
inline lines currently inside `x-story-implement` Phase 1 become a
single `Skill(skill: "x-internal-story-build-plan", …)` invocation;
the orchestrator shrinks to a read-the-envelope consumer.

Responsibilities (single):

1. Invoke `x-arch-plan` for the story (Step 1A).
2. Dispatch 5 sibling `Agent(general-purpose, …)` subagents in ONE
   assistant message for Steps 1B (implementation plan), 1C (test
   plan), 1D (task breakdown), 1E (security assessment), and 1F
   (compliance assessment).
3. Gate Steps 1E and 1F behind `--scope` (SIMPLE → skip 1E/1F).
4. Collect artifact paths from every subagent, assemble a
   consolidated response envelope, and emit it on stdout.
5. Translate individual subagent or `x-arch-plan` failures into the
   stable exit-code catalogue below.

Non-responsibilities (explicit):

- The skill does NOT read, re-parse, or classify the story file —
  that is `x-internal-story-load-context`'s contract (story-0049-0011).
- The skill does NOT mutate `execution-state.json`, story
  `**Status:**` headers, or IMPLEMENTATION-MAP — those belong to
  `x-internal-status-update` (story-0049-0005).
- The skill does NOT run the parallelism-collision gate — Phase 1.5
  remains the orchestrator's responsibility via `x-parallel-eval`
  (EPIC-0041).
- The skill does NOT perform pre-check / freshness classification of
  the 7 planning artifacts — that runs upstream inside
  `x-internal-story-load-context` (Step 4). Callers only invoke this
  skill when `planningMode ∈ {HYBRID, INLINE}` and a regen is
  actually required.

## Convention Anchors (x-internal-*)

| Aspect | Value | Rationale |
| :--- | :--- | :--- |
| Path | `internal/plan/x-internal-story-build-plan/` | `internal/` prefix scopes visibility; `plan/` co-locates with the sibling read-only carve-out (x-internal-story-load-context) |
| Frontmatter `visibility` | `internal` | Generator filters these from `/help` menu |
| Frontmatter `user-invocable` | `false` | Declarative complement to `visibility: internal` |
| Body marker | `> 🔒 **INTERNAL SKILL**` block as first non-frontmatter content | Visible to humans browsing the repo; no parsing required |
| Allowed tools | `Bash, Skill, Agent` | Minimal for an orchestrator: `Skill` for Step 1A (x-arch-plan), `Agent` for Steps 1B-1F, `Bash` for argv parsing and the envelope `jq -nc` assembly |
| Naming | `x-internal-{subject}-{action}` | Mirrors Rule 04 skill taxonomy; `story-build-plan` = subject+action |

Audit rule: Rule 22 (Lifecycle Integrity) validates every skill under
`internal/**` satisfies all 6 anchors above. Violations fail
`LifecycleIntegrityAuditTest`.

## Triggers

Bare-slash form is intentionally omitted — this skill is never
invoked by a human typing `/x-internal-story-build-plan` in chat.
All invocations follow Rule 13 INLINE-SKILL pattern from a calling
orchestrator:

```markdown
Skill(skill: "x-internal-story-build-plan",
      args: "--story-id story-0049-0012 --epic-id 0049 --scope STANDARD")
```

## Parameters

| Parameter | Required | Default | Description |
| :--- | :--- | :--- | :--- |
| `--story-id <id>` | M | — | Story identifier (`story-XXXX-YYYY` canonical form) |
| `--epic-id <id>` | M | — | 4-digit epic identifier (`XXXX`) — used to resolve `plans/epic-XXXX/` |
| `--scope <tier>` | O | `STANDARD` | `SIMPLE` / `STANDARD` / `COMPLEX`; `SIMPLE` skips Steps 1E (security) and 1F (compliance) |
| `--skip-review` | O | `false` | When `true`, signals downstream subagents to omit inline peer-review passes in the generated artifacts (advisory only — does not affect artifact count) |

All four argument forms (`--key value`, `--key=value`, empty, and
unknown-flag rejection) follow the Rule-14 rejection matrix: reject
unknown flags and missing required flags with exit `64` (sysexits
`EX_USAGE`). The `--story-id` value is normalised to lowercase and
validated against `^story-[0-9]{4}-[0-9]{4}$`; `--epic-id` is
zero-padded to 4 digits.

## Response Contract

On success the skill writes a single-line JSON object to stdout:

| Field | Type | Always Present | Description |
| :--- | :--- | :--- | :--- |
| `archPlan` | `String` (path) | yes | Path of the architecture plan (`plans/epic-XXXX/plans/arch-story-XXXX-YYYY.md`) |
| `implPlan` | `String` (path) | yes | Path of the implementation plan (`plan-story-XXXX-YYYY.md`) |
| `testPlan` | `String` (path) | yes | Path of the test plan (`tests-story-XXXX-YYYY.md`) |
| `taskBreakdown` | `String` (path) | yes | Path of the task breakdown (`tasks-story-XXXX-YYYY.md`) |
| `securityAssessment` | `String` (path) \| `null` | no — null when `scope=SIMPLE` | Path of the security assessment (`security-story-XXXX-YYYY.md`) |
| `complianceAssessment` | `String` (path) \| `null` | no — null when `scope=SIMPLE` | Path of the compliance assessment (`compliance-story-XXXX-YYYY.md`) |
| `taskMap` | `String` (path) | yes | Path of the task-implementation map (`task-implementation-map-story-XXXX-YYYY.md`) |
| `scope` | `String` | yes | Echo of the resolved scope tier — lets the caller verify the gate applied |
| `skipped` | `Array<String>` | yes | Names of gated steps skipped (e.g., `["1E", "1F"]` in SIMPLE; `[]` otherwise) |

The six artifact basenames (all under `plans/epic-XXXX/plans/`):

1. `arch-story-XXXX-YYYY.md` (Step 1A — `x-arch-plan`)
2. `plan-story-XXXX-YYYY.md` (Step 1B — Senior Architect subagent)
3. `tests-story-XXXX-YYYY.md` (Step 1C — `x-test-plan` subagent)
4. `tasks-story-XXXX-YYYY.md` (Step 1D — `x-lib-task-decomposer` subagent)
5. `security-story-XXXX-YYYY.md` (Step 1E — `x-threat-model` subagent; omitted in SIMPLE)
6. `compliance-story-XXXX-YYYY.md` (Step 1F — Compliance Engineer subagent; omitted in SIMPLE)

The task-implementation-map (`task-implementation-map-story-XXXX-YYYY.md`)
is produced by the Step 1D subagent as part of the task-decomposition
artifact (EPIC-0038 v2 contract).

## Exit Codes

| Code | Name | Condition | Message Format |
| :--- | :--- | :--- | :--- |
| 0 | SUCCESS | All required artifacts produced | — |
| 1 | ARCH_PLAN_FAILED | `x-arch-plan` (Step 1A) returned non-zero or failed to produce `arch-story-*.md` | `Architecture plan failed: <detail>` |
| 2 | SUBAGENT_FAILED | Any of Steps 1B-1F (subagent dispatch) failed or did not produce its artifact | `Subagent <name> failed: <detail>` |
| 64 | EX_USAGE | Unknown flag, missing required flag, or malformed `--story-id` / `--epic-id` | `usage: <detail>` |

No partial-success state: if any required step fails, the skill
exits non-zero. Optional steps (1E / 1F in SIMPLE) are not errors
when skipped by the gate; they ARE errors when dispatched and
failed.

## Workflow

### Step 1 — Argument parsing and path resolution

Parse arguments with the Rule-14 single-file `while (($#))` loop;
reject unknown flags / missing required flags / empty values with
exit `64`. Normalise `--story-id` to lowercase and verify against
`^story-[0-9]{4}-[0-9]{4}$`; zero-pad `--epic-id` to 4 digits.
Derive:

```bash
epic_dir="plans/epic-${epic_id}"
story_file="${epic_dir}/${story_id}.md"
plans_dir="${epic_dir}/plans"
```

When `${epic_dir}` is not a directory, exit 64 with
`usage: epic dir not found: plans/epic-<id>`. The skill does NOT
re-validate the story file existence — that is the caller's
responsibility (`x-internal-story-load-context` runs upstream).

### Step 1A — Architecture plan (sequential)

Invoke `x-arch-plan` via the Rule 13 INLINE-SKILL pattern:

```markdown
Skill(skill: "x-arch-plan",
      args: "--story-id ${story_id} --epic-id ${epic_id}")
```

Expected output: `${plans_dir}/arch-story-${story_id}.md`.

On any failure (non-zero exit from `x-arch-plan`, or the expected
file does not exist after invocation), emit to stderr:
`Architecture plan failed: <underlying message>` and exit `1`.

Step 1A MUST complete before Step 1B-1F dispatch: later steps read
architectural context (ports, layers, dependency direction) from
the arch plan.

### Steps 1B-1F — Parallel subagent dispatch (one assistant message)

Dispatch exactly 5 sibling `Agent(general-purpose, …)` tool calls
in ONE assistant message — the canonical Rule 13 Pattern 2
(SUBAGENT-GENERAL) parallel-launch shape. When `--scope=SIMPLE`,
dispatch only Steps 1B / 1C / 1D (3 siblings) and record `1E` and
`1F` in `skipped`.

Each subagent prompt follows the structure:

```markdown
FIRST ACTION: <no-op — TaskCreate/TaskUpdate unavailable in this harness>.
You are a <role>. Read context files:
- plans/epic-${epic_id}/${story_id}.md (story)
- plans/epic-${epic_id}/plans/arch-story-${story_id}.md (arch plan)
- <role-specific templates under .claude/templates/>.
Produce <artifact> at ${plans_dir}/<basename>.
LAST ACTION: return the absolute path of the artifact you produced.
```

Per-step role and artifact mapping:

| Step | Role | Skill delegate (if any) | Artifact basename |
| :--- | :--- | :--- | :--- |
| 1B | Senior Architect | — (inline prompt) | `plan-story-${story_id}.md` |
| 1C | QA Engineer | `x-test-plan` via `Skill(…)` inside subagent | `tests-story-${story_id}.md` |
| 1D | Task Decomposer | `x-lib-task-decomposer` via `Skill(…)` inside subagent | `tasks-story-${story_id}.md` + `task-implementation-map-story-${story_id}.md` |
| 1E | Security Engineer | `x-threat-model` via `Skill(…)` inside subagent | `security-story-${story_id}.md` |
| 1F | Compliance Engineer | — (inline prompt) | `compliance-story-${story_id}.md` |

The `--skip-review` flag propagates into the subagent prompt when
set: a single sentence instructing the subagent to omit the inline
peer-review pass customarily appended to its artifact.

### Step 2 — Collect results and assemble envelope

After all subagents return, validate that every expected artifact
exists on disk:

```bash
for basename in arch plan tests tasks; do
  path="${plans_dir}/${basename}-story-${story_id}.md"
  [[ -f "${path}" ]] || { err "Subagent ${basename} did not produce ${path}"; exit 2; }
done
```

Repeat for `security-` / `compliance-` ONLY when `scope != SIMPLE`.

Assemble the response envelope via `jq -nc` so the shape is
authoritative:

```bash
jq -nc \
  --arg archPlan "${plans_dir}/arch-story-${story_id}.md" \
  --arg implPlan "${plans_dir}/plan-story-${story_id}.md" \
  --arg testPlan "${plans_dir}/tests-story-${story_id}.md" \
  --arg taskBreakdown "${plans_dir}/tasks-story-${story_id}.md" \
  --arg taskMap "${plans_dir}/task-implementation-map-story-${story_id}.md" \
  --arg scope "${scope}" \
  --argjson skipped "${skipped_json}" \
  --arg securityAssessment "${security_path_or_null}" \
  --arg complianceAssessment "${compliance_path_or_null}" \
  '{archPlan:$archPlan, implPlan:$implPlan, testPlan:$testPlan,
    taskBreakdown:$taskBreakdown, taskMap:$taskMap,
    securityAssessment: (if $securityAssessment == "" then null
                         else $securityAssessment end),
    complianceAssessment: (if $complianceAssessment == "" then null
                           else $complianceAssessment end),
    scope:$scope, skipped:$skipped}'
```

Emit on stdout as a single line terminated by `\n`. Exit 0.

## Examples

### Example 1 — Happy path: STANDARD scope (6 artifacts)

```bash
Skill(skill: "x-internal-story-build-plan",
      args: "--story-id story-0049-0012 --epic-id 0049 --scope STANDARD")
```

Output:
```json
{"archPlan":"plans/epic-0049/plans/arch-story-0049-0012.md","implPlan":"plans/epic-0049/plans/plan-story-0049-0012.md","testPlan":"plans/epic-0049/plans/tests-story-0049-0012.md","taskBreakdown":"plans/epic-0049/plans/tasks-story-0049-0012.md","taskMap":"plans/epic-0049/plans/task-implementation-map-story-0049-0012.md","securityAssessment":"plans/epic-0049/plans/security-story-0049-0012.md","complianceAssessment":"plans/epic-0049/plans/compliance-story-0049-0012.md","scope":"STANDARD","skipped":[]}
```
Exit: 0.

### Example 2 — SIMPLE scope skips 1E / 1F

```bash
Skill(skill: "x-internal-story-build-plan",
      args: "--story-id story-0049-0001 --epic-id 0049 --scope SIMPLE")
```

Output (truncated):
```json
{"archPlan":"…","implPlan":"…","testPlan":"…","taskBreakdown":"…","taskMap":"…","securityAssessment":null,"complianceAssessment":null,"scope":"SIMPLE","skipped":["1E","1F"]}
```
Exit: 0.

### Example 3 — COMPLEX scope (same artifact set as STANDARD)

COMPLEX currently produces the same 6 artifacts as STANDARD; the
tier is echoed in `scope` for downstream decisions (e.g., post-Phase-2
stakeholder review gate in `x-story-implement`) but does not change
the planning surface.

### Example 4 — Architecture plan fails

```bash
Skill(skill: "x-internal-story-build-plan",
      args: "--story-id story-0049-0099 --epic-id 0049 --scope STANDARD")
```

Stderr:
```
Architecture plan failed: x-arch-plan exit 3 (story not found)
```
Exit: 1.

### Example 5 — Subagent failure

Step 1B (Senior Architect) fails to produce `plan-story-…md`.

Stderr:
```
Subagent 1B failed: plan-story-0049-0012.md not produced
```
Exit: 2.

## Outputs

| Artifact | Path | Description |
| :--- | :--- | :--- |
| Response envelope | stdout | Single-line JSON matching the Response Contract |
| Error diagnostic | stderr | Single line, non-empty only on exit ≠ 0 |
| Planning artifacts | `plans/epic-${epic_id}/plans/*.md` | Written by `x-arch-plan` + Step 1B-1F subagents |

This skill DOES create files — unlike `x-internal-story-load-context`,
which is strictly read-only. The `allowed-tools` frontmatter
includes `Skill` and `Agent` to reflect this: the writes happen
inside the dispatched subagents / the invoked `x-arch-plan`, not
directly from this skill's body.

## Error Handling

| Scenario | Action |
| :--- | :--- |
| Missing required flag | Print `usage:` banner to stderr; exit 64 |
| Unknown flag | Print `usage: unknown flag …` to stderr; exit 64 |
| Malformed `--story-id` | `usage: --story-id must match story-NNNN-NNNN`; exit 64 |
| Missing epic dir | `usage: epic dir not found: plans/epic-<id>`; exit 64 |
| `x-arch-plan` non-zero | `Architecture plan failed: <detail>`; exit 1 |
| Expected `arch-story-*.md` absent after Step 1A | `Architecture plan failed: arch-story-*.md not produced`; exit 1 |
| Any subagent returns error | `Subagent <step> failed: <detail>`; exit 2 |
| Any subagent returns success but required artifact absent | `Subagent <step> failed: <basename> not produced`; exit 2 |
| `jq` absent on PATH | Exit 127 with `jq is required`; abort before dispatch |
| `--scope` value unrecognised | `usage: --scope must be SIMPLE / STANDARD / COMPLEX`; exit 64 |

The skill is fail-fast: the first error aborts the run. Partial
artifacts produced by successful subagents remain on disk (the
orchestrator may choose to re-invoke with the same arguments —
`x-arch-plan` and each subagent are idempotent with respect to their
artifact path).

## Performance Contract

Target: ~3-5 minutes for STANDARD scope on a typical story. The
dominant cost is the wall-clock of the slowest subagent (Step 1B-1F
run in parallel, so the total ≈ max(individual)). Step 1A is
strictly sequential and typically ≤ 60 seconds.

SIMPLE-scope runs save ~0-30 seconds by skipping 1E and 1F (those
subagents are typically I/O-bound on template reads, not model
inference).

## Testing

Acceptance scenarios (mirroring Section 7 of story-0049-0012):

1. **Happy path — STANDARD.** All 6 artifacts produced; envelope
   echoes `scope=STANDARD`, `skipped=[]`.
2. **Degenerate — SIMPLE.** 4 artifacts produced; envelope reports
   `securityAssessment=null`, `complianceAssessment=null`,
   `skipped=["1E","1F"]`.
3. **Boundary — COMPLEX.** Same 6 artifacts as STANDARD; envelope
   echoes `scope=COMPLEX`.
4. **Error — `x-arch-plan` fails.** Exit 1; stderr starts with
   `Architecture plan failed:`.
5. **Error — subagent failure.** Exit 2; stderr identifies which
   step (`1B` / `1C` / `1D` / `1E` / `1F`).

Coverage requirement: ≥ 95% line / ≥ 90% branch across the
invoking Bash codepaths and the dispatch / envelope-assembly
logic. Goldens (if added) lock the SKILL.md rendering under
`src/test/resources/golden/internal/plan/x-internal-story-build-plan/`
per the sibling pattern.

## Generator Filter Contract

The `ia-dev-env` generator MUST exclude skills with
`visibility: internal` from:

1. The `.claude/README.md` skill-inventory table.
2. The `/help` menu listing surfaced by Claude Code.
3. User-facing autocomplete in the chat input.

Internal skills are still copied into `.claude/skills/` (flat
layout) so `Skill(skill: "x-internal-…")` invocations from other
skills resolve correctly. The invariant: **user cannot see them;
orchestrators can invoke them.**

## Telemetry

Internal skills DO NOT emit `phase.start` / `phase.end` markers —
telemetry is produced by the invoking orchestrator (the `phase`
wrapping the orchestrator's own step is the correct aggregation
boundary). Passive hooks still capture `tool.call` for each
underlying `Skill` / `Agent` / `Bash` invocation made from inside
the skill body.

Subagent `subagent.start` / `subagent.end` markers around Steps
1B-1F are emitted by the caller (`x-story-implement` Phase 1)
because that orchestrator owns the enclosing phase. Re-emitting
from inside this skill would double-count the wave.

Reference: Rule 13 (Skill Invocation Protocol), Rule 22 (Lifecycle
Integrity Audit), ADR-0010 (Interactive Gates Convention — exempts
internal skills from the 3-option menu contract).

## Integration Notes

| Skill | Relationship | Context |
| :--- | :--- | :--- |
| `x-story-implement` | caller (primary) | Phase 1 carve-out: ~250 inline lines collapse to one `Skill(skill: "x-internal-story-build-plan", …)` invocation in story-0049-0019 |
| `x-internal-story-load-context` | upstream peer | Caller runs this sibling first to decide whether Phase 1 needs regen (`planningMode != PRE_PLANNED`); only then invokes `x-internal-story-build-plan` |
| `x-arch-plan` | delegate (Step 1A) | Sequential precondition; produces `arch-story-*.md` consumed by Steps 1B-1F |
| `x-test-plan` | delegate (Step 1C) | Invoked from inside the QA-Engineer subagent |
| `x-lib-task-decomposer` | delegate (Step 1D) | Invoked from inside the Task-Decomposer subagent; produces both the tasks file and the task-implementation-map |
| `x-threat-model` | delegate (Step 1E) | Invoked from inside the Security-Engineer subagent; skipped in SIMPLE |
| `x-parallel-eval` | consumer (downstream) | Phase 1.5 of the caller reads the `task-implementation-map` produced here to compute the collision matrix (EPIC-0041) |
| `x-internal-status-update` | peer | Separate concern (mutates state); never invoked from this skill |

Downstream stories that depend on this carve-out:
story-0049-0019 (orchestrator consumes the envelope and deletes the
inline Phase 1 block).

Full workflow detail (subagent prompt catalogue per step, scope-gate
decision table, envelope-assembly edge cases, and the interaction
with `x-internal-story-load-context`'s upstream freshness result)
lives in [`references/full-protocol.md`](references/full-protocol.md)
per ADR-0011.
