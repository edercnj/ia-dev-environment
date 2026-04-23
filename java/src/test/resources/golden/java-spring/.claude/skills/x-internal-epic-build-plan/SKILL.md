---
name: x-internal-epic-build-plan
description: "Builds the canonical ExecutionPlan for an epic (Phase 0/0.5 carve-out of x-epic-implement): loads epic-XXXX.md, IMPLEMENTATION-MAP.md, and every story-*.md; constructs the inter-story dependency DAG; runs Kahn's algorithm with cycle detection; optionally computes a file-overlap matrix (mode=parallel) and the critical path; then renders plans/epic-XXXX/epic-execution-plan.md via x-internal-report-write using the _TEMPLATE-EPIC-EXECUTION-PLAN.md template. Emits a stable JSON envelope on stdout for orchestrator consumption. Sixth skill in the x-internal-* convention and the third under internal/plan/ (after x-internal-story-load-context and x-internal-story-build-plan)."
visibility: internal
user-invocable: false
allowed-tools: Bash, Skill
argument-hint: "--epic-id <XXXX> --mode <sequential|parallel> --output <path> [--strict-overlap]"
category: internal-plan
context-budget: heavy
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

> 🔒 **INTERNAL SKILL**
> Esta skill é invocada apenas por outras skills (orquestradores).
> NÃO é destinada a invocação direta pelo usuário.
> Caller principal: `x-epic-implement` (Phase 0 / 0.5 carve-out).
> Sexta skill da convenção `x-internal-*` (após x-internal-status-update
> pilot 0049-0005, x-internal-report-write 0049-0006,
> x-internal-args-normalize 0049-0007, x-internal-story-load-context
> 0049-0011, e x-internal-story-build-plan 0049-0012). Terceira skill
> na subdir `internal/plan/`. A subdir `plan/` agrupa as skills que
> orquestram lógica de planejamento (read-only computation + render);
> difere de `internal/ops/`, cujas sibling skills mutam estado
> (execution-state.json, reports).

# Skill: x-internal-epic-build-plan

## Purpose

Carve out the Phase 0 / 0.5 pre-flight of `x-epic-implement` — the
~250 inline lines that load epic metadata, stories, and dependency
matrices, compute the phase ordering via Kahn's algorithm, optionally
analyse file overlaps, and compute the critical path — into a single
invocable skill with a stable JSON envelope. The orchestrator shrinks
to a read-the-envelope consumer that then dispatches waves.

Responsibilities (single):

1. Load `plans/epic-${epic_id}/epic-${epic_id}.md` and parse the story
   table.
2. Load `plans/epic-${epic_id}/IMPLEMENTATION-MAP.md` and extract the
   inter-story dependency matrix.
3. For every `story-${epic_id}-NNNN.md`, cross-validate declared
   dependencies against the matrix and detect missing story files.
4. Build the dependency DAG, run Kahn's algorithm, and detect cycles
   (exit `3` with `CYCLIC_DEPENDENCY` on detection).
5. When `--mode parallel`: compute the file-overlap matrix across
   every pair of stories co-scheduled in the same Kahn phase.
   Advisory by default; `--strict-overlap` escalates any hard
   collision to a non-zero warning in the envelope (NOT a fatal
   error — Phase 1.5 parallelism gate remains the orchestrator's
   abort point via `x-parallel-eval`).
6. Compute the critical path (longest chain of dependencies measured
   in story count).
7. Render `${output}` via `x-internal-report-write` using the
   template `_TEMPLATE-EPIC-EXECUTION-PLAN.md`.
8. Emit a stable JSON envelope on stdout; translate every error
   category to the exit-code catalogue below.

Non-responsibilities (explicit):

- The skill does NOT mutate `execution-state.json`, `**Status:**`
  headers, or IMPLEMENTATION-MAP — those belong to
  `x-internal-status-update` (story-0049-0005).
- The skill does NOT run the Phase 1.5 collision gate — that is
  `x-parallel-eval`'s exclusive contract (EPIC-0041).
- The skill does NOT dispatch story implementation waves — that is
  `x-epic-implement`'s contract, downstream of this skill.
- The skill does NOT re-read individual stories' planning artifacts
  (arch/impl/test/task plans) — those belong to
  `x-internal-story-load-context` (per-story, called inside
  `x-story-implement` Phase 0).
- The skill does NOT write the story `**Status:**` header or trigger
  Jira transitions — those are status-finalize concerns.

## Convention Anchors (x-internal-*)

| Aspect | Value | Rationale |
| :--- | :--- | :--- |
| Path | `internal/plan/x-internal-epic-build-plan/` | `internal/` prefix scopes visibility; `plan/` co-locates with the sibling read-and-compute carve-outs (x-internal-story-load-context, x-internal-story-build-plan) |
| Frontmatter `visibility` | `internal` | Generator filters these from `/help` menu |
| Frontmatter `user-invocable` | `false` | Declarative complement to `visibility: internal` |
| Body marker | `> 🔒 **INTERNAL SKILL**` block as first non-frontmatter content | Visible to humans browsing the repo; no parsing required |
| Allowed tools | `Bash, Skill` | Minimal: `Bash` for parsing + DAG + envelope assembly, `Skill` for the single downstream `x-internal-report-write` invocation. No `Agent` — planning is deterministic computation, not subagent dispatch |
| Naming | `x-internal-{subject}-{action}` | `epic-build-plan` = subject+action per Rule 04 skill taxonomy |

Audit rule: Rule 22 (Lifecycle Integrity) validates every skill under
`internal/**` satisfies all 6 anchors above. Violations fail
`LifecycleIntegrityAuditTest`.

## Triggers

Bare-slash form is intentionally omitted — this skill is never
invoked by a human typing `/x-internal-epic-build-plan` in chat.
All invocations follow the Rule 13 INLINE-SKILL pattern from a
calling orchestrator:

```markdown
Skill(skill: "x-internal-epic-build-plan",
      args: "--epic-id 0049 --mode sequential --output plans/epic-0049/epic-execution-plan.md")
```

## Parameters

| Parameter | Required | Default | Description |
| :--- | :--- | :--- | :--- |
| `--epic-id <id>` | M | — | 4-digit epic identifier (`XXXX`); zero-padded to 4 digits before use |
| `--mode <tier>` | M | — | `sequential` or `parallel`; case-insensitive; controls whether the overlap matrix is computed |
| `--output <path>` | M | — | Absolute or repo-relative path where the rendered markdown is written (typically `plans/epic-XXXX/epic-execution-plan.md`); must be writable |
| `--strict-overlap` | O | `false` | When `true` in parallel mode, annotates hard collisions in the envelope with `overlapSeverity: "hard"`; does NOT abort the run — the caller's Phase 1.5 gate decides |

All four argument forms (`--key value`, `--key=value`, empty, and
unknown-flag rejection) follow the Rule-14 rejection matrix: reject
unknown flags and missing required flags with exit `64` (sysexits
`EX_USAGE`). The `--epic-id` value is normalised to 4 digits
(zero-padded); `--mode` is normalised to lowercase.

## Response Contract

On success the skill writes a single-line JSON object to stdout:

| Field | Type | Always Present | Description |
| :--- | :--- | :--- | :--- |
| `phases` | `Array<Phase>` | yes | Ordered list of Kahn phases, each `{ "index": <int>, "stories": [<storyId>,…] }` |
| `overlapMatrix` | `Object<storyId, Array<storyId>>` \| `null` | no — `null` when `mode=sequential` | Map from each story to the list of co-scheduled peers touching the same files |
| `overlapSeverity` | `String` \| `null` | no — `null` when `mode=sequential` | One of `"none"` / `"soft"` / `"regen"` / `"hard"`; mirrors `x-parallel-eval` severity vocabulary |
| `criticalPath` | `Array<storyId>` | yes | Longest dependency chain in the DAG (length = N stories); ties broken by lexicographic storyId |
| `planPath` | `String` | yes | Echo of `--output` after normalisation — also the path of the rendered markdown |
| `mode` | `String` | yes | Echo of the resolved mode (`sequential` / `parallel`) |
| `epicId` | `String` | yes | Echo of the 4-digit epic id |
| `storyCount` | `Integer` | yes | Count of distinct stories discovered under `plans/epic-${epic_id}/story-*.md` |
| `strictOverlap` | `Boolean` | yes | Echo of `--strict-overlap` |

The envelope shape is authoritative; callers must NOT assume field
order. `overlapMatrix` and `overlapSeverity` are `null` (not absent)
in sequential mode so consumers can use a single deserialisation
schema.

## Exit Codes

| Code | Name | Condition | Message Format |
| :--- | :--- | :--- | :--- |
| 0 | SUCCESS | All steps succeeded; envelope emitted | — |
| 1 | EPIC_NOT_FOUND | `plans/epic-${epic_id}/` directory does not exist | `Epic dir not found: plans/epic-<id>` |
| 2 | MAP_NOT_FOUND | `plans/epic-${epic_id}/IMPLEMENTATION-MAP.md` is missing | `IMPLEMENTATION-MAP.md missing under plans/epic-<id>` |
| 3 | CYCLIC_DEPENDENCY | Kahn's algorithm detected a cycle in the DAG | `Cycle detected: <storyA> -> <storyB> -> … -> <storyA>` |
| 4 | STORY_FILE_MISSING | Dependency matrix references a story whose `.md` file is absent | `Story file missing: <storyId>.md` |
| 5 | REPORT_WRITE_FAILED | Downstream `x-internal-report-write` returned non-zero or did not produce `${output}` | `Report write failed: <detail>` |
| 64 | EX_USAGE | Unknown flag, missing required flag, malformed `--epic-id` / `--mode`, or unwritable `--output` | `usage: <detail>` |

No partial-success state: the first non-recoverable error aborts the
run and yields a non-zero exit. The `--strict-overlap` flag does NOT
produce a non-zero exit; it only escalates `overlapSeverity` in the
envelope.

## Workflow

### Step 1 — Argument parsing and path resolution

Parse arguments with the Rule-14 single-file `while (($#))` loop;
reject unknown flags / missing required flags / empty values with
exit `64`. Normalise `--epic-id` to 4 digits (zero-padded);
normalise `--mode` to lowercase and verify it matches
`^(sequential|parallel)$`. Verify `--output`'s parent directory
exists and is writable; reject with exit `64` if not.

Derive:

```bash
epic_dir="plans/epic-${epic_id}"
epic_file="${epic_dir}/epic-${epic_id}.md"
map_file="${epic_dir}/IMPLEMENTATION-MAP.md"
```

Validate in order:

1. `[[ -d "${epic_dir}" ]]` — else exit `1` with `EPIC_NOT_FOUND`.
2. `[[ -f "${map_file}" ]]` — else exit `2` with `MAP_NOT_FOUND`.
3. `[[ -f "${epic_file}" ]]` — else exit `1` with `EPIC_NOT_FOUND`
   (conceptually the same failure class — epic-dir without its epic
   file is unusable; folded into code `1` for a flatter contract).

### Step 2 — Load and parse artifacts

Parse the epic file's story table (Markdown `| storyId | …` rows)
into a `Set<storyId>` of DECLARED stories. Parse
`IMPLEMENTATION-MAP.md` dependency section into a
`Map<storyId, List<storyId>>` of EDGES (each key's list is its
`blockedBy` set). Enumerate `plans/epic-${epic_id}/story-*.md` to
a `Set<storyId>` of ON_DISK stories.

Cross-validation:

- For every `storyId` in EDGES.keys ∪ EDGES.values: if
  `storyId ∉ ON_DISK`, exit `4` with `STORY_FILE_MISSING`.
- Stories in ON_DISK but absent from EDGES are treated as zero-dep
  roots (no error — the map is the authority for edges, but its
  absence implies "no declared dependency").

### Step 3 — Kahn's algorithm + cycle detection

Classic Kahn: compute `inDegree` per story; seed queue with all
`inDegree == 0` roots (sorted lexicographically for determinism);
emit them as `phase[0].stories`; decrement neighbour in-degrees;
stories reaching zero enter `phase[k+1]`.

If after the loop `|emitted| < |stories|`, there is a cycle. Walk
from any unvisited node with `inDegree > 0`, following outgoing
edges until a node is revisited — that traversal is the minimal
cycle chain. Emit:

```
Cycle detected: <story1> -> <story2> -> … -> <story1>
```

and exit `3`.

### Step 4 — Overlap matrix (parallel mode only)

For `--mode parallel`, compute for every pair `(storyA, storyB)` in
the SAME Kahn phase:

```
filesA = union of write+regen footprint declared in story-A's plans
filesB = same for story-B
overlap = filesA ∩ filesB
```

The footprint source is the `## File Footprint` block introduced
by EPIC-0041 in each plan file (task / story level). When the block
is absent (legacy stories predating EPIC-0041), mark the story as
`footprint-unknown` and include it in the matrix with an empty peer
list + an advisory in the envelope's `warnings` field. Never abort
— RULE-006 (EPIC-0041) dictates "warn, do not block" for
footprint-unknown stories.

Classify each non-empty overlap by the RULE-004 hotspot table:

- `hard` — overlap contains any hotspot file (`SettingsAssembler.java`,
  `HooksAssembler.java`, `CLAUDE.md`, `CHANGELOG.md`, `pom.xml`,
  `.gitignore`, `src/test/resources/golden/**`)
- `regen` — overlap contains only generator-touched files
- `soft` — overlap contains only hand-edited files outside hotspots
- `none` — overlap is empty

`overlapSeverity` at the envelope level is the MAX across all pairs
(`hard > regen > soft > none`). `--strict-overlap` does NOT change
the severity function — it is already maximal; the flag is echoed so
the caller can decide its own threshold.

### Step 5 — Critical path

Longest-path in the DAG by topological order. For every story,
`longest[story] = max(longest[dep]) + 1` iterating in Kahn order.
The critical path is the chain ending at the story with maximum
`longest[story]`, unwound via back-pointers. Ties broken by
lexicographic storyId.

### Step 6 — Render markdown via x-internal-report-write

Invoke the downstream via the Rule 13 INLINE-SKILL pattern:

```markdown
Skill(skill: "x-internal-report-write",
      args: "--template _TEMPLATE-EPIC-EXECUTION-PLAN.md --output ${output} --data-stdin")
```

Pipe the JSON envelope (augmented with a `renderedAt` ISO-8601
timestamp) to the child's stdin. The child is responsible for
placeholder substitution; this skill passes the data as-is.

On non-zero exit or absent `${output}`, emit:
`Report write failed: <child stderr line>` and exit `5`.

### Step 7 — Emit the envelope

Assemble via `jq -nc`:

```bash
jq -nc \
  --arg epicId   "${epic_id}" \
  --arg mode     "${mode}" \
  --arg planPath "${output}" \
  --argjson phases        "${phases_json}" \
  --argjson overlapMatrix "${overlap_json_or_null}" \
  --arg     overlapSeverity "${severity_or_null}" \
  --argjson criticalPath  "${critical_json}" \
  --argjson storyCount    "${story_count}" \
  --argjson strictOverlap "${strict_bool}" \
  '{ epicId:$epicId, mode:$mode, phases:$phases,
     overlapMatrix: (if $overlapMatrix == null then null
                     else $overlapMatrix end),
     overlapSeverity: (if $overlapSeverity == "" then null
                       else $overlapSeverity end),
     criticalPath:$criticalPath, planPath:$planPath,
     storyCount:$storyCount, strictOverlap:$strictOverlap }'
```

Emit on stdout as a single line terminated by `\n`. Exit `0`.

## Examples

### Example 1 — Happy path: sequential mode

```bash
Skill(skill: "x-internal-epic-build-plan",
      args: "--epic-id 0049 --mode sequential --output plans/epic-0049/epic-execution-plan.md")
```

Output:
```json
{"epicId":"0049","mode":"sequential","phases":[{"index":0,"stories":["story-0049-0001","story-0049-0002","story-0049-0003","story-0049-0004"]},{"index":1,"stories":["story-0049-0005","story-0049-0006","story-0049-0007","story-0049-0008"]},{"index":2,"stories":["story-0049-0009","story-0049-0010","story-0049-0011","story-0049-0012","story-0049-0013","story-0049-0014","story-0049-0015","story-0049-0016","story-0049-0017"]},{"index":3,"stories":["story-0049-0018","story-0049-0019"]},{"index":4,"stories":["story-0049-0020","story-0049-0021","story-0049-0022"]}],"overlapMatrix":null,"overlapSeverity":null,"criticalPath":["story-0049-0001","story-0049-0005","story-0049-0018","story-0049-0020"],"planPath":"plans/epic-0049/epic-execution-plan.md","storyCount":22,"strictOverlap":false}
```
Exit: 0.

### Example 2 — Parallel mode with overlap

```bash
Skill(skill: "x-internal-epic-build-plan",
      args: "--epic-id 0049 --mode parallel --output plans/epic-0049/epic-execution-plan.md")
```

Output (truncated):
```json
{"epicId":"0049","mode":"parallel","phases":[…],"overlapMatrix":{"story-0049-0006":["story-0049-0007"],"story-0049-0007":["story-0049-0006"]},"overlapSeverity":"regen","criticalPath":[…],"planPath":"…","storyCount":22,"strictOverlap":false}
```
Exit: 0.

### Example 3 — Cyclic dependency

Synthetic epic where `story-A` blockedBy `story-B` and
`story-B` blockedBy `story-A`.

Stderr:
```
Cycle detected: story-0099-0001 -> story-0099-0002 -> story-0099-0001
```
Exit: 3.

### Example 4 — Epic directory missing

```bash
Skill(skill: "x-internal-epic-build-plan",
      args: "--epic-id 9999 --mode sequential --output /tmp/out.md")
```

Stderr:
```
Epic dir not found: plans/epic-9999
```
Exit: 1.

### Example 5 — Story file missing

The IMPLEMENTATION-MAP references `story-0049-0099` but no
`story-0049-0099.md` exists on disk.

Stderr:
```
Story file missing: story-0049-0099.md
```
Exit: 4.

### Example 6 — Boundary: epic with 1 story, zero deps

```bash
Skill(skill: "x-internal-epic-build-plan",
      args: "--epic-id 0050 --mode sequential --output plans/epic-0050/epic-execution-plan.md")
```

Output:
```json
{"epicId":"0050","mode":"sequential","phases":[{"index":0,"stories":["story-0050-0001"]}],"overlapMatrix":null,"overlapSeverity":null,"criticalPath":["story-0050-0001"],"planPath":"plans/epic-0050/epic-execution-plan.md","storyCount":1,"strictOverlap":false}
```
Exit: 0.

## Outputs

| Artifact | Path | Description |
| :--- | :--- | :--- |
| Response envelope | stdout | Single-line JSON matching the Response Contract |
| Error diagnostic | stderr | Single line, non-empty only on exit ≠ 0 |
| Execution plan markdown | `${output}` (typically `plans/epic-XXXX/epic-execution-plan.md`) | Rendered by `x-internal-report-write` using the `_TEMPLATE-EPIC-EXECUTION-PLAN.md` template |

The skill DOES create files — specifically the `${output}` markdown —
but only via the `x-internal-report-write` delegation. The
`allowed-tools` frontmatter therefore does not include `Write`: the
write happens inside the child skill's own adapter, not directly
from this skill's body.

## Error Handling

| Scenario | Action |
| :--- | :--- |
| Missing required flag | Print `usage:` banner to stderr; exit 64 |
| Unknown flag | Print `usage: unknown flag …` to stderr; exit 64 |
| Malformed `--epic-id` | `usage: --epic-id must be a 4-digit integer`; exit 64 |
| `--mode` unrecognised | `usage: --mode must be sequential / parallel`; exit 64 |
| `--output` path unwritable | `usage: --output path not writable`; exit 64 |
| Missing epic dir | `Epic dir not found: plans/epic-<id>`; exit 1 |
| Missing epic file | `Epic file not found: plans/epic-<id>/epic-<id>.md`; exit 1 |
| Missing IMPLEMENTATION-MAP | `IMPLEMENTATION-MAP.md missing under plans/epic-<id>`; exit 2 |
| Cycle detected in DAG | `Cycle detected: <chain>`; exit 3 |
| Story-file referenced by map but absent | `Story file missing: <storyId>.md`; exit 4 |
| `x-internal-report-write` non-zero | `Report write failed: <child stderr line>`; exit 5 |
| `x-internal-report-write` success but `${output}` absent | `Report write failed: expected output not produced`; exit 5 |
| `jq` absent on PATH | Exit 127 with `jq is required`; abort before any file read |
| Story has no `## File Footprint` block (parallel mode) | Emit advisory in envelope `warnings`; include story in matrix with empty peer list; do NOT abort |

The skill is fail-fast on structural errors (exit 1–4); reporting
failures (exit 5) preserve the computed envelope on stderr so the
caller can choose to retry just the render step. The `warnings`
advisory for footprint-unknown stories is non-fatal by design
(EPIC-0041 RULE-006).

## Performance Contract

Target: plan computation strictly under 5 seconds for epics up to
30 stories on a typical developer machine — this is the Global DoD
threshold from story-0049-0009 §4. The dominant cost is the
file-system scan of `story-*.md` + footprint-block parsing; DAG
processing is O(V + E) and negligible.

The render step (`x-internal-report-write`) is measured separately
and is typically 0.5–2 seconds for a 22-story epic; it is NOT
counted against the 5-second DoD because the DoD bounds plan
computation, not the physical write.

## Testing

Acceptance scenarios (mirroring Section 7 of story-0049-0009):

1. **Happy path — sequential.** Epic-0049 with 22 stories; 5 phases
   emitted; critical path length ≥ 4; markdown produced at
   `--output`.
2. **Boundary — single story.** Epic with only
   `story-XXXX-0001`; 1 phase with 1 story; critical path length 1.
3. **Error — cyclic dependency.** Synthetic epic A→B, B→A; exit 3;
   stderr contains `CYCLIC_DEPENDENCY`.
4. **Error — epic dir missing.** `--epic-id 9999`; exit 1.
5. **Error — map missing.** Epic dir exists without
   `IMPLEMENTATION-MAP.md`; exit 2.
6. **Error — story file missing.** Map references
   `story-XXXX-0099.md` but file absent; exit 4.
7. **Parallel — overlap computed.** Two stories in the same phase
   touching the same file; `overlapMatrix` populated;
   `overlapSeverity` matches RULE-004 classification.

Coverage requirement: ≥ 95% line / ≥ 90% branch across the
parser, DAG, overlap matrix, and envelope-assembly logic. Goldens
(if added) lock the SKILL.md rendering under
`src/test/resources/golden/internal/plan/x-internal-epic-build-plan/`
per the sibling pattern (story-0049-0012, story-0049-0014).

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
underlying `Skill` / `Bash` invocation made from inside the skill
body.

Reference: Rule 13 (Skill Invocation Protocol), Rule 22 (Lifecycle
Integrity Audit), ADR-0010 (Interactive Gates Convention — exempts
internal skills from the 3-option menu contract), ADR-0006
(File-Conflict-Aware Parallelism — defines RULE-004 hotspot table
and RULE-006 footprint-unknown advisory).

## Integration Notes

| Skill | Relationship | Context |
| :--- | :--- | :--- |
| `x-epic-implement` | caller (primary) | Phase 0 / 0.5 carve-out: ~250 inline lines collapse to one `Skill(skill: "x-internal-epic-build-plan", …)` invocation in story-0049-0018 |
| `x-internal-report-write` | delegate (Step 6) | Renders `epic-execution-plan.md` using `_TEMPLATE-EPIC-EXECUTION-PLAN.md`; defined in story-0049-0006 (MERGED) |
| `x-parallel-eval` | downstream peer | Phase 1.5 of the caller consumes the `overlapMatrix` to compute per-wave collision classification (EPIC-0041) |
| `x-internal-status-update` | peer | Separate concern (mutates state); never invoked from this skill |
| `x-internal-story-load-context` | peer | Sibling read-only carve-out at the story level; never invoked from this skill |
| `x-internal-story-build-plan` | peer | Sibling story-level planning carve-out; never invoked from this skill |

Downstream stories that depend on this carve-out:
story-0049-0018 (`x-epic-implement` refactor consumes the envelope
and deletes the inline Phase 0 / 0.5 blocks).

Full workflow detail (parser grammar for the IMPLEMENTATION-MAP
dependency table, Kahn-algorithm pseudocode, footprint-block regex,
overlap-severity decision table, and the `x-internal-report-write`
stdin schema) lives in
[`references/full-protocol.md`](references/full-protocol.md)
per ADR-0011.
