---
name: x-internal-story-load-context
description: "Loads a story file, validates predecessor dependencies against execution-state.json, executes artifact pre-checks (mtime-based staleness detection across the 7 planning artifacts), classifies scope (SIMPLE / STANDARD / COMPLEX) from task-count + Gherkin-scenario heuristics, and detects planning mode (PRE_PLANNED / HYBRID / INLINE). Replaces ~140 lines of inline Phase 0 logic previously duplicated inside x-story-implement. Fourth skill in the x-internal-* convention (after x-internal-status-update pilot, x-internal-report-write, and x-internal-args-normalize): internal visibility, non-user-invocable, read-only, subdir scoping under internal/plan/."
visibility: internal
user-invocable: false
allowed-tools: Bash
argument-hint: "--story-id <story-XXXX-YYYY> --epic-id <XXXX>"
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
> Caller principal: x-story-implement (Phase 0 carve-out).
> Quarta skill da convenção `x-internal-*` (após x-internal-status-update
> pilot 0049-0005, x-internal-report-write 0049-0006, e
> x-internal-args-normalize 0049-0007). Primeira skill na subdir
> `internal/plan/` (as três anteriores vivem em `internal/ops/`): o subdir
> reflete categoria funcional — esta skill orquestra lógica de planejamento
> (load+classify), não runtime ops (mutate/write).

# Skill: x-internal-story-load-context

## Purpose

Carve out Phase 0 of `x-story-implement` into a single-responsibility,
read-only Bash pipeline. The skill:

1. Resolves and reads the story file under
   `plans/epic-XXXX/story-XXXX-YYYY.md`.
2. Parses Section 1 (Dependencies) and validates each predecessor story
   reports status `DONE` (or an accepted synonym: `MERGED` / `COMPLETE`)
   inside `plans/epic-XXXX/execution-state.json`.
3. Globs the 7 planning artifacts under `plans/epic-XXXX/plans/` and
   classifies each as `fresh` (artifact mtime ≥ story mtime),
   `stale` (artifact mtime < story mtime), or `missing`.
4. Assesses scope from Section 8 task count + Section 7 Gherkin
   scenario count: `SIMPLE` (≤ 4 tasks), `STANDARD` (5–7 tasks),
   `COMPLEX` (≥ 8 tasks).
5. Derives planning mode from artifact classification:
   `PRE_PLANNED` (all 7 fresh), `HYBRID` (some fresh, some stale or
   missing), `INLINE` (zero fresh).

The output is a single-line JSON envelope consumed by
`x-story-implement` to drive the subsequent Phase 0.5 / Phase 1 /
Phase 1.5 branching. The skill **never** mutates the filesystem.

## Convention Anchors (x-internal-*)

| Aspect | Value | Rationale |
| :--- | :--- | :--- |
| Path | `internal/plan/x-internal-story-load-context/` | `internal/` prefix scopes visibility; `plan/` aligns with sibling planning-category skills (x-story-plan, x-epic-map) and separates from `internal/ops/` siblings that mutate state |
| Frontmatter `visibility` | `internal` | Generator filters these from `/help` menu |
| Frontmatter `user-invocable` | `false` | Declarative complement to `visibility: internal` |
| Body marker | `> 🔒 **INTERNAL SKILL**` block as first non-frontmatter content | Visible to humans browsing the repo; no parsing required |
| Allowed tools | `Bash` only | Minimal surface; all logic is a read-only shell pipeline built on `jq` + `stat` |
| Naming | `x-internal-{subject}-{action}` | Mirrors Rule 04 skill taxonomy; `story-load-context` = subject+action |

Audit rule: Rule 22 (Lifecycle Integrity) validates every skill under
`internal/**` satisfies all 6 anchors above. Violations fail the
`LifecycleIntegrityAuditTest`.

## Triggers

Bare-slash form is intentionally omitted — this skill is never invoked
by a human typing `/x-internal-story-load-context` in chat. All
invocations follow Rule 13 INLINE-SKILL pattern from a calling
orchestrator:

```markdown
Skill(skill: "x-internal-story-load-context",
      args: "--story-id story-0049-0011 --epic-id 0049")
```

## Parameters

| Parameter | Required | Default | Description |
| :--- | :--- | :--- | :--- |
| `--story-id <id>` | M | — | Story identifier (`story-XXXX-YYYY` canonical form) |
| `--epic-id <id>` | M | — | 4-digit epic identifier (`XXXX`) — used to resolve `plans/epic-XXXX/` |

## Response Contract

When successful, the skill writes a single-line JSON object to stdout:

| Field | Type | Always Present | Description |
| :--- | :--- | :--- | :--- |
| `storyFile` | `String` | yes | Absolute path to the resolved story markdown |
| `storyMtime` | `Number` | yes | Unix epoch seconds of the story file's last modification |
| `dependencies` | `Array<{id:String, status:String}>` | yes | One entry per Section 1 blocker (empty array when `Blocked By: —`) |
| `scope` | `String` | yes | `SIMPLE` / `STANDARD` / `COMPLEX` |
| `planningMode` | `String` | yes | `PRE_PLANNED` / `HYBRID` / `INLINE` |
| `artifacts` | `{fresh:Array<String>, stale:Array<String>, missing:Array<String>}` | yes | Classification of the 7 planning artifact paths (relative to `plans/epic-XXXX/plans/`) |
| `taskCount` | `Number` | yes | Count of `### TASK-XXXX-YYYY-NNN:` headers under Section 8 |
| `scenarioCount` | `Number` | yes | Count of `Cenario:` / `Scenario:` markers under Section 7 |

The seven planning-artifact basenames scanned under
`plans/epic-XXXX/plans/` are:

1. `plan-story-XXXX-YYYY.md` (implementation plan)
2. `arch-story-XXXX-YYYY.md` (architecture plan)
3. `tests-story-XXXX-YYYY.md` (test plan)
4. `tasks-story-XXXX-YYYY.md` (task breakdown)
5. `security-story-XXXX-YYYY.md` (security assessment)
6. `compliance-story-XXXX-YYYY.md` (compliance assessment)
7. `task-implementation-map-story-XXXX-YYYY.md` (EPIC-0038 map)

## Exit Codes

| Code | Name | Condition | Message Format |
| :--- | :--- | :--- | :--- |
| 0 | SUCCESS | Context loaded and emitted | — |
| 1 | STORY_NOT_FOUND | `plans/epic-XXXX/story-XXXX-YYYY.md` absent | `Story file not found: <path>` |
| 2 | DEPENDENCY_NOT_DONE | Any Section 1 blocker reports non-`DONE` status | `Blocker <id> is <status>` |
| 3 | EPIC_NOT_FOUND | `plans/epic-XXXX/` absent | `Epic dir not found: plans/epic-<id>` |

## Workflow

### Step 1 — Argument parsing and path resolution

Parse `--story-id` and `--epic-id`; reject unknown flags and missing
required flags with exit 64 (sysexits `EX_USAGE`). Derive:

```bash
epic_dir="plans/epic-${epic_id}"
story_file="${epic_dir}/${story_id}.md"
state_file="${epic_dir}/execution-state.json"
plans_dir="${epic_dir}/plans"
```

When `${epic_dir}` is not a directory, exit 3 (`EPIC_NOT_FOUND`).
When `${story_file}` is not a regular file, exit 1 (`STORY_NOT_FOUND`).

### Step 2 — Read story mtime and section markers

```bash
story_mtime=$(stat -f '%m' "${story_file}" 2>/dev/null \
            || stat -c '%Y' "${story_file}")
```

Extract Section 1 (Dependencies) between the `## 1. Dependências` /
`## 1. Dependencies` header and the next `## ` header; extract
Section 7 (Acceptance Criteria) and Section 8 (Tasks) likewise. The
parser is tolerant: accepts both Portuguese (`Dependências`, `Cenario`)
and English (`Dependencies`, `Scenario`) section titles since current
stories under `plans/epic-0049/` are authored in Portuguese.

### Step 3 — Parse dependencies from Section 1

Section 1 is a markdown table with columns `Blocked By | Blocks`. The
`Blocked By` cell is either `—` (empty) or a comma-separated list of
story IDs. Collect IDs; for each, resolve its status via
`execution-state.json`:

```bash
status=$(jq -r ".stories[\"${dep_id}\"].status // \"UNKNOWN\"" \
         "${state_file}")
```

When `state_file` is absent, fall back to parsing the dep story's own
`**Status:**` header inside its markdown file (supports epics where
execution-state.json has not yet been initialized). Accepted synonyms
for success: `DONE`, `MERGED`, `COMPLETE`, `Concluída`, `Concluida`.

On any blocker with non-DONE status, exit 2 (`DEPENDENCY_NOT_DONE`)
with the offending ID and status in the error message.

### Step 4 — Artifact freshness classification

For each of the 7 planning-artifact basenames:

```bash
artifact="${plans_dir}/${name}"
if [[ ! -f "${artifact}" ]]; then
  missing+=("${name}")
elif [[ $(stat -f '%m' "${artifact}" 2>/dev/null \
          || stat -c '%Y' "${artifact}") -ge ${story_mtime} ]]; then
  fresh+=("${name}")
else
  stale+=("${name}")
fi
```

Artifacts not present in the plans directory tree (common in early
stories) populate `missing`. Artifacts with mtime strictly less than
the story's mtime populate `stale`. The rest populate `fresh`.

### Step 5 — Scope assessment

Count Section 8 tasks via `grep -cE '^### TASK-[0-9]{4}-[0-9]{4}-[0-9]{3}:'`.
Classification:

| Task count | Scope |
| :--- | :--- |
| ≤ 4 | `SIMPLE` |
| 5–7 | `STANDARD` |
| ≥ 8 | `COMPLEX` |

Scenario count is computed for telemetry (Section 7 `grep -cE '^(Cenario|Scenario):'`) but does not influence the scope label in this story; reserved for future tuning per story-0049-0011 §3.2.

### Step 6 — Planning mode detection

| Fresh artifact count | Planning mode |
| :--- | :--- |
| 7 (all fresh) | `PRE_PLANNED` |
| 1–6 (mixed) | `HYBRID` |
| 0 | `INLINE` |

### Step 7 — Emit response envelope

Compose the response using `jq -n` so the shape is authoritative and
the stdout stream is a single line terminated by `\n`:

```bash
jq -nc \
  --arg storyFile "${story_file}" \
  --argjson storyMtime "${story_mtime}" \
  --argjson dependencies "${deps_json}" \
  --arg scope "${scope}" \
  --arg planningMode "${mode}" \
  --argjson artifacts "${artifacts_json}" \
  --argjson taskCount "${task_count}" \
  --argjson scenarioCount "${scenario_count}" \
  '{storyFile:$storyFile, storyMtime:$storyMtime,
    dependencies:$dependencies, scope:$scope,
    planningMode:$planningMode, artifacts:$artifacts,
    taskCount:$taskCount, scenarioCount:$scenarioCount}'
```

Exit 0.

## Examples

### Example 1 — Happy path: simple story, no dependencies, no planning yet

```bash
Skill(skill: "x-internal-story-load-context",
      args: "--story-id story-0049-0001 --epic-id 0049")
```

Output:
```json
{"storyFile":"plans/epic-0049/story-0049-0001.md","storyMtime":1745343600,"dependencies":[],"scope":"SIMPLE","planningMode":"INLINE","artifacts":{"fresh":[],"stale":[],"missing":["plan-story-0049-0001.md","arch-story-0049-0001.md","tests-story-0049-0001.md","tasks-story-0049-0001.md","security-story-0049-0001.md","compliance-story-0049-0001.md","task-implementation-map-story-0049-0001.md"]},"taskCount":3,"scenarioCount":4}
```
Exit: 0.

### Example 2 — Dependency not DONE

```bash
Skill(skill: "x-internal-story-load-context",
      args: "--story-id story-0049-0008 --epic-id 0049")
```

Stderr:
```
Blocker story-0049-0001 is PENDING
```
Exit: 2.

### Example 3 — Planning mode PRE_PLANNED

Given all 7 artifacts under `plans/epic-0049/plans/` with mtime ≥ story
mtime, output includes `"planningMode":"PRE_PLANNED"` and every
artifact in `artifacts.fresh`.

Exit: 0.

### Example 4 — Boundary: COMPLEX scope (10 tasks)

Output contains `"scope":"COMPLEX"` and `"taskCount":10`. Exit: 0.

### Example 5 — Missing story file

```bash
Skill(skill: "x-internal-story-load-context",
      args: "--story-id story-9999-0001 --epic-id 9999")
```

Stderr:
```
Epic dir not found: plans/epic-9999
```
Exit: 3.

When the epic directory exists but the story file does not:

Stderr:
```
Story file not found: plans/epic-0049/story-0049-9999.md
```
Exit: 1.

## Outputs

| Artifact | Path | Description |
| :--- | :--- | :--- |
| Response envelope | stdout | Single-line JSON matching the Response Contract |
| Error diagnostic | stderr | Single line, non-empty only on exit ≠ 0 |

No file is created or modified. The skill is **strictly read-only**
(enforced by convention and by the `allowed-tools: Bash` frontmatter —
no `Write` / `Edit` tool is available).

## Error Handling

| Scenario | Action |
| :--- | :--- |
| Missing required flag | Print `usage:` banner to stderr; exit 64 (sysexits EX_USAGE) |
| `jq` absent on PATH | Exit 127 with `jq is required`; abort before any stat |
| `stat` flavour mismatch (GNU vs BSD) | Try `stat -f %m` (BSD) first, fall back to `stat -c %Y` (GNU) |
| `execution-state.json` absent | Fall back to parsing each dep story's own `**Status:**` header |
| Dep story file absent AND state file absent | Treat dep status as `UNKNOWN`; exit 2 |
| Section 1 table unparseable (malformed markdown) | Treat dependencies as empty; log warning to stderr; continue |
| Unknown section-title language | Try both `Dependências` / `Dependencies`, `Cenario` / `Scenario`; stop at first hit |
| `task-implementation-map-*` file name variant (legacy `map-story-*`) | Also accept `map-story-XXXX-YYYY.md` as the 7th artifact when the canonical name is missing |

## Performance Contract

Target: < 500 ms for a typical epic with 20 stories in
`execution-state.json` and all 7 planning artifacts present. The
implementation reads at most 9 files (1 story, 1 state, 7 artifacts
via `stat` only — no body read) and spawns a single `jq` invocation
for JSON assembly. No network I/O; no recursive traversal beyond
`plans/epic-XXXX/plans/`.

## Testing

The story ships the following acceptance test scenarios, mirroring
Section 7 of story-0049-0011:

1. **Degenerate — no dependencies.** `story-0049-0001` reports
   `dependencies=[]` and valid `scope` / `planningMode`.
2. **Dependency not DONE.** Ancillary story depends on a blocker with
   `PENDING` status → exit 2, message `"Blocker <id> is PENDING"`.
3. **Fresh artifacts → PRE_PLANNED.** Fabricate 7 artifacts with
   mtime > story; assert `planningMode=PRE_PLANNED`.
4. **Missing story file.** `--story-id story-9999-0001` with epic
   dir absent → exit 3; epic dir present but story missing → exit 1.
5. **Boundary — COMPLEX scope.** Story with 10 tasks → `scope=COMPLEX`.

Goldens under
`src/test/resources/golden/internal/plan/x-internal-story-load-context/`
lock the SKILL.md rendering. Coverage requirement: ≥ 95% line /
≥ 90% branch across the invoking Bash codepaths.

## Generator Filter Contract

The `ia-dev-env` generator MUST exclude skills with
`visibility: internal` from:

1. The `.claude/README.md` skill-inventory table.
2. The `/help` menu listing surfaced by Claude Code.
3. User-facing autocomplete in the chat input.

Internal skills are still copied into `.claude/skills/` (flat layout)
so `Skill(skill: "x-internal-...")` invocations from other skills
resolve correctly. The invariant: **user cannot see them; orchestrators
can invoke them.**

## Telemetry

Internal skills DO NOT emit `phase.start` / `phase.end` markers —
telemetry is produced by the invoking orchestrator (the `phase`
wrapping the orchestrator's own step is the correct aggregation
boundary). Passive hooks still capture `tool.call` for the underlying
`Bash` invocation.

Reference: Rule 13 (Skill Invocation Protocol), Rule 22 (Lifecycle
Integrity Audit), ADR-0010 (Interactive Gates Convention — exempts
internal skills from the 3-option menu contract).

## Integration Notes

| Skill | Relationship | Context |
| :--- | :--- | :--- |
| `x-story-implement` | caller (primary) | Phase 0 carve-out: the skill's stdout envelope replaces ~140 inline lines previously executed by the orchestrator |
| `x-epic-implement` | caller (indirect, via story-0049-0019 downstream) | Consumes the same envelope when iterating stories at the epic scope |
| `x-internal-status-update` | peer | Sibling `x-internal-*` skill; this skill *reads* `execution-state.json`, the other *mutates* it; both acquire the same `<file>.lock` when their lifetimes overlap (this skill uses `flock -s` shared lock) |
| `x-internal-args-normalize` | delegate (optional) | Future refactor: the argument parser in Step 1 can be replaced by a call to `x-internal-args-normalize` once its schema-driven mode covers read-only skills |
| `x-status-reconcile` | consumer (peer) | Reads the same story `**Status:**` header this skill inspects; no shared mutation |

Downstream stories that depend on this carve-out:
story-0049-0019 (orchestrator consumes the envelope and deletes the
inline Phase 0 block).

Full workflow detail (argument-parser rejection matrix, section-extraction
regex catalogue, artifact-classification edge cases, and the
execution-state.json schema contract) lives in
[`references/full-protocol.md`](references/full-protocol.md) per
ADR-0011.
