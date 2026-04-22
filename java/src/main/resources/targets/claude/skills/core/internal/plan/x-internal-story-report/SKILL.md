---
name: x-internal-story-report
description: "Generates the final consolidated story-completion report by reading plans/epic-XXXX/execution-state.json, collecting per-task status and commitSha, PR metadata (prNumber, prState), coverage delta, and review findings, then rendering the output via x-internal-report-write with _TEMPLATE-STORY-COMPLETION-REPORT.md to the caller-specified --output path. Eighth skill in the x-internal-* convention and the fifth under internal/plan/ (after x-internal-story-load-context, x-internal-story-build-plan, x-internal-story-verify, and x-internal-story-resume). Read-only against state; writes only to --output via x-internal-report-write."
visibility: internal
user-invocable: false
allowed-tools: Bash, Skill
argument-hint: "--story-id <story-XXXX-YYYY> --epic-id <XXXX> --output <path>"
category: internal-plan
context-budget: light
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

> 🔒 **INTERNAL SKILL**
> Esta skill é invocada apenas por outras skills (orquestradores).
> NÃO é destinada a invocação direta pelo usuário.
> Caller principal: x-story-implement (Phase 3 carve-out — final report).
> Oitava skill da convenção `x-internal-*` (após x-internal-status-update
> pilot 0049-0005, x-internal-report-write 0049-0006,
> x-internal-args-normalize 0049-0007, x-internal-story-load-context
> 0049-0011, x-internal-story-build-plan 0049-0012,
> x-internal-story-verify 0049-0014, e x-internal-story-resume
> 0049-0013). Quinta skill em `internal/plan/`: o subdir `plan/` agrupa
> orquestração de planejamento e verificação da story; esta skill fecha
> a quíntupla `load → build → verify → resume → report` com o report
> final consolidado. Difere de `internal/ops/`, cujas sibling skills
> mutam estado compartilhado (`execution-state`, append-markers); esta
> skill é **single-writer** para o seu próprio `--output` e delega o
> render a `x-internal-report-write`.

# Skill: x-internal-story-report

## Purpose

Carve out the "final story report" block that `x-story-implement`
currently inlines at the end of Phase 3 into a single,
single-responsibility skill. The orchestrator passes three flags
(`--story-id`, `--epic-id`, `--output`) and consumes a compact
`{reportPath, summary}` envelope; the skill handles data collection,
template resolution, and rendering delegation.

Responsibilities (single):

1. Locate and read `plans/epic-XXXX/execution-state.json`; exit 1
   (`STATE_NOT_FOUND`) when absent.
2. Extract the story node (`.stories.<story-id>`) and its tasks map.
3. Compute the `summary` object:
   - `tasksCount` — total number of tasks under `.stories.<id>.tasks`.
   - `tasksDone` — count of tasks whose status matches the DONE
     synonym set (see Step 2 below).
   - `commitsCount` — count of distinct `commitSha` values observed
     across all tasks (null and empty strings excluded).
   - `prNumber` — the story-level PR number if present; otherwise
     `null`.
   - `prState` — normalised PR state (`MERGED`, `OPEN`, `CLOSED`);
     `null` when no PR exists.
   - `coverageLine` / `coverageBranch` — percentages from
     `.stories.<id>.verification.coverage` when that block exists;
     `null` when the verification gate has not run.
4. Build a structured JSON data payload containing the scalar summary
   plus per-task arrays (`tasks`) and findings arrays (`findings`)
   sourced from `.stories.<id>.reviews` when available.
5. Resolve the template at
   `.claude/templates/_TEMPLATE-STORY-COMPLETION-REPORT.md`; exit 2
   (`TEMPLATE_MISSING`) when absent.
6. Invoke `Skill(skill: "x-internal-report-write", …)` with the
   resolved template, the built payload, and the forwarded `--output`
   path. On render success, forward `outputPath` to the caller.
7. Emit the response envelope
   `{reportPath, summary}` on stdout as a single JSON line.

Non-responsibilities (explicit):

- The skill does NOT mutate `execution-state.json`; it is read-only
  against the state file (delegation to `x-internal-status-update
  --read-only` is not required because a single best-effort read is
  sufficient — downstream `x-internal-report-write` owns the atomic
  write of the report file).
- The skill does NOT run tests, coverage, or review dispatch — those
  values must already be present in the state file (written by
  `x-internal-story-verify`, `x-review`, and `x-review-pr`
  upstream).
- The skill does NOT create branches, PRs, or mutate git state.
- The skill does NOT emit telemetry markers — the caller
  (`x-story-implement` Phase 3) owns the phase boundary.
- The skill does NOT implement its own render engine — it delegates
  100% of placeholder substitution and `{{#each}}` expansion to
  `x-internal-report-write`.

## Convention Anchors (x-internal-*)

| Aspect | Value | Rationale |
| :--- | :--- | :--- |
| Path | `internal/plan/x-internal-story-report/` | `internal/` prefix scopes visibility; `plan/` co-locates with the other story-lifecycle carve-outs (`x-internal-story-load-context`, `x-internal-story-build-plan`, `x-internal-story-verify`, `x-internal-story-resume`) |
| Frontmatter `visibility` | `internal` | Generator filters these from `/help` menu |
| Frontmatter `user-invocable` | `false` | Declarative complement to `visibility: internal` |
| Body marker | `> 🔒 **INTERNAL SKILL**` block as first non-frontmatter content | Visible to humans browsing the repo; no parsing required |
| Allowed tools | `Bash, Skill` | `Bash` reads the state file and builds the JSON payload with `jq`; `Skill` dispatches `x-internal-report-write` for rendering. No direct `Write` / `Edit` surface |
| Naming | `x-internal-{subject}-{action}` | Mirrors Rule 04 skill taxonomy; `story-report` = subject+action |

Audit rule: Rule 22 (Lifecycle Integrity) validates every skill under
`internal/**` satisfies all 6 anchors above. Violations fail
`LifecycleIntegrityAuditTest`.

## Triggers

Bare-slash form is intentionally omitted — this skill is never
invoked by a human typing `/x-internal-story-report` in chat. All
invocations follow Rule 13 INLINE-SKILL pattern from a calling
orchestrator:

```markdown
Skill(skill: "x-internal-story-report",
      args: "--story-id story-0049-0001 --epic-id 0049 --output plans/epic-0049/reports/story-0049-0001-report.md")
```

## Parameters

| Parameter | Required | Default | Description |
| :--- | :--- | :--- | :--- |
| `--story-id <id>` | M | — | Story identifier (`story-XXXX-YYYY` canonical form; lowercase-normalised) |
| `--epic-id <id>` | M | — | 4-digit epic identifier (`XXXX`) — used to resolve `plans/epic-XXXX/` (zero-padded) |
| `--output <path>` | M | — | Target path for the rendered report; forwarded verbatim to `x-internal-report-write --output` |

All three flags support both `--key value` and `--key=value` forms.
Unknown or missing required flags exit with `64` (sysexits
`EX_USAGE`). `--story-id` is validated against
`^story-[0-9]{4}-[0-9]{4}$`; `--epic-id` is zero-padded to 4 digits
and validated against `^[0-9]{4}$`.

## Response Contract

On success the skill writes a single-line JSON object to stdout:

| Field | Type | Always Present | Description |
| :--- | :--- | :--- | :--- |
| `reportPath` | `String` | yes | Absolute path of the written report (echoed from `x-internal-report-write`) |
| `summary` | `Object` | yes | Compact summary used by the orchestrator for final logging (see sub-schema below) |

### `summary` sub-schema

| Field | Type | Always Present | Description |
| :--- | :--- | :--- | :--- |
| `tasksCount` | `Integer` | yes | Total tasks under `.stories.<id>.tasks` (0 when map is empty) |
| `tasksDone` | `Integer` | yes | Subset of `tasksCount` in the DONE synonym set |
| `commitsCount` | `Integer` | yes | Count of distinct non-empty `commitSha` values across tasks |
| `prNumber` | `Integer \| Null` | yes | Story-level PR number; `null` when no PR exists |
| `prState` | `String \| Null` | yes | `MERGED`, `OPEN`, or `CLOSED`; `null` when `prNumber=null` |
| `coverageLine` | `Number \| Null` | yes | Line-coverage percentage; `null` when verification has not run |
| `coverageBranch` | `Number \| Null` | yes | Branch-coverage percentage; `null` when verification has not run |

### Example envelope (happy path)

```json
{"reportPath":"/repo/plans/epic-0049/reports/story-0049-0001-report.md","summary":{"tasksCount":5,"tasksDone":5,"commitsCount":5,"prNumber":42,"prState":"MERGED","coverageLine":96.4,"coverageBranch":92.1}}
```

### Example envelope (story without PR — boundary)

```json
{"reportPath":"/repo/plans/epic-0049/reports/story-0049-0001-report.md","summary":{"tasksCount":4,"tasksDone":2,"commitsCount":2,"prNumber":null,"prState":null,"coverageLine":null,"coverageBranch":null}}
```

## Exit Codes

| Code | Name | Condition | Message Format |
| :--- | :--- | :--- | :--- |
| 0 | SUCCESS | Report rendered and response envelope emitted | — |
| 1 | STATE_NOT_FOUND | `plans/epic-XXXX/execution-state.json` missing or unreadable | `State missing: plans/epic-XXXX/execution-state.json` |
| 2 | TEMPLATE_MISSING | `.claude/templates/_TEMPLATE-STORY-COMPLETION-REPORT.md` absent | `Template missing: _TEMPLATE-STORY-COMPLETION-REPORT.md` |
| 64 | EX_USAGE | Unknown or malformed flag | `usage: --story-id <id> --epic-id <id> --output <path>` |
| 127 | DEPENDENCY_MISSING | `jq` absent on `PATH` | `dependency missing: jq` |

Render-time failures (e.g., `UNRESOLVED_PLACEHOLDER`) propagate the
`x-internal-report-write` exit code unchanged so the caller branches
on the original semantic. No envelope is emitted on any non-zero
exit.

## Workflow

### Step 1 — Argument parsing and path resolution

Parse the three flags via a tight `while (($#)); case "$1" in …` loop
to stay within the SkillSizeLinter 500-line budget without delegating
to `x-internal-args-normalize` (peer skill; same rationale as
`x-internal-story-resume` §1 of full-protocol).

```bash
epic_dir="plans/epic-${epic_id}"
state_file="${epic_dir}/execution-state.json"
template_name="_TEMPLATE-STORY-COMPLETION-REPORT.md"
template_path="${CLAUDE_PROJECT_DIR}/.claude/templates/${template_name}"
```

Validate `--output` is not empty and does not contain `..` above the
project root (Rule 06 path traversal guard). Reject unknown flags
with exit `64`.

### Step 2 — Read execution-state.json (single best-effort read)

```bash
if [[ ! -r "${state_file}" ]]; then
  echo "State missing: ${state_file}" >&2
  exit 1
fi
```

Parse the story node:

```bash
story_json=$(jq --arg sid "${story_id}" '.stories[$sid] // null' "${state_file}")
if [[ "${story_json}" == "null" ]]; then
  echo "Story ${story_id} not present in state file" >&2
  exit 1
fi
```

A story absent from the state file degrades to exit 1 (same as
missing state) — consumers distinguish via the stderr message, not
the exit code.

### Step 3 — Compute summary fields

Tasks classification uses the same DONE synonym set as
`x-internal-story-resume` (see references §2):

```text
DONE synonyms: DONE, MERGED, COMPLETE, Concluída, Concluida, Done, Merged
```

```bash
tasks_count=$(echo "${story_json}" | jq '.tasks // {} | length')
tasks_done=$(echo "${story_json}" | jq '
  [ .tasks // {} | to_entries[] |
    select(.value.status as $s |
      ["DONE","MERGED","COMPLETE","Concluída","Concluida","Done","Merged"] |
      index($s)) ] | length')
commits_count=$(echo "${story_json}" | jq '
  [ .tasks // {} | to_entries[] | .value.commitSha |
    select(. != null and . != "") ] | unique | length')
pr_number=$(echo "${story_json}" | jq '.pr.number // null')
pr_state=$(echo "${story_json}" | jq '
  if .pr.state then (.pr.state | ascii_upcase) else null end')
coverage_line=$(echo "${story_json}" | jq '.verification.coverage.line // null')
coverage_branch=$(echo "${story_json}" | jq '.verification.coverage.branch // null')
```

### Step 4 — Build the render payload

Assemble the payload as a single `jq -n` expression so the
`x-internal-report-write` invocation receives canonical JSON:

```bash
data_json=$(jq -nc \
  --arg storyId "${story_id}" \
  --arg epicId "${epic_id}" \
  --argjson tasksCount "${tasks_count}" \
  --argjson tasksDone "${tasks_done}" \
  --argjson commitsCount "${commits_count}" \
  --argjson prNumber "${pr_number}" \
  --argjson prState "$(jq -nc --arg s "${pr_state}" 'if $s=="null" or $s=="" then null else $s end')" \
  --argjson coverageLine "${coverage_line}" \
  --argjson coverageBranch "${coverage_branch}" \
  --argjson tasks "$(echo "${story_json}" | jq '[.tasks // {} | to_entries[] | {id: .key, status: .value.status, commitSha: (.value.commitSha // null)}]')" \
  --argjson findings "$(echo "${story_json}" | jq '[.reviews.findings // [] | .[] | {severity: .severity, title: .title, file: (.file // null)}]')" \
  '{storyId:$storyId, epicId:$epicId,
    tasksCount:$tasksCount, tasksDone:$tasksDone,
    commitsCount:$commitsCount,
    prNumber:$prNumber, prState:$prState,
    coverageLine:$coverageLine, coverageBranch:$coverageBranch,
    tasks:$tasks, findings:$findings}')
```

### Step 5 — Template existence gate

```bash
if [[ ! -r "${template_path}" ]]; then
  echo "Template missing: ${template_name}" >&2
  exit 2
fi
```

The pre-check is intentionally local (not delegated to
`x-internal-report-write`) so the orchestrator gets a distinct
`TEMPLATE_MISSING` exit code (2) rather than the renderer's
`TEMPLATE_NOT_FOUND` exit code (1 of that skill). This keeps the
two skills' exit-code spaces non-overlapping.

### Step 6 — Delegate to x-internal-report-write

```markdown
Skill(skill: "x-internal-report-write",
      args: "--template _TEMPLATE-STORY-COMPLETION-REPORT.md \
             --output ${output_path} \
             --data ${data_json}")
```

The renderer handles all placeholder substitution and `{{#each}}`
loops. Its stdout is a single JSON line matching the
`x-internal-report-write` response contract
(`{outputPath, bytesWritten, placeholdersReplaced, entriesAppended}`);
this skill reads `outputPath` and discards the rest.

### Step 7 — Emit response envelope

```bash
jq -nc \
  --arg reportPath "${report_path}" \
  --argjson summary "${summary_json}" \
  '{reportPath:$reportPath, summary:$summary}'
```

Exit 0.

## Examples

### Example 1 — Happy path: fully-done story with merged PR

State snippet for `story-0049-0001`:

```json
{
  "stories": {
    "story-0049-0001": {
      "tasks": {
        "TASK-0049-0001-001": {"status":"DONE","commitSha":"abc123"},
        "TASK-0049-0001-002": {"status":"DONE","commitSha":"def456"},
        "TASK-0049-0001-003": {"status":"DONE","commitSha":"ghi789"},
        "TASK-0049-0001-004": {"status":"DONE","commitSha":"jkl012"},
        "TASK-0049-0001-005": {"status":"DONE","commitSha":"mno345"}
      },
      "pr": {"number": 42, "state": "MERGED"},
      "verification": {"coverage": {"line": 96.4, "branch": 92.1}},
      "reviews": {"findings": []}
    }
  }
}
```

Invocation:

```markdown
Skill(skill: "x-internal-story-report",
      args: "--story-id story-0049-0001 --epic-id 0049 --output plans/epic-0049/reports/story-0049-0001-report.md")
```

Envelope:

```json
{"reportPath":"/repo/plans/epic-0049/reports/story-0049-0001-report.md","summary":{"tasksCount":5,"tasksDone":5,"commitsCount":5,"prNumber":42,"prState":"MERGED","coverageLine":96.4,"coverageBranch":92.1}}
```

Exit: 0.

### Example 2 — Story in progress (3 DONE, 2 PENDING)

The report is still generated; `summary.tasksDone=3` while
`summary.tasksCount=5`. The rendered report includes a per-task
status table via the template's `{{#each tasks}}` block. Exit: 0.

### Example 3 — Error: STATE_NOT_FOUND

```bash
Skill(skill: "x-internal-story-report",
      args: "--story-id story-0049-0099 --epic-id 0099 --output /tmp/out.md")
```

Stderr:

```
State missing: plans/epic-0099/execution-state.json
```

Exit: 1. No envelope emitted.

### Example 4 — Error: TEMPLATE_MISSING

The state file exists but `_TEMPLATE-STORY-COMPLETION-REPORT.md` is
not present under `.claude/templates/`. Stderr:

```
Template missing: _TEMPLATE-STORY-COMPLETION-REPORT.md
```

Exit: 2.

### Example 5 — Boundary: story without PR

State snippet: `"pr"` key is absent or `null`.

Envelope:

```json
{"reportPath":"…","summary":{"tasksCount":4,"tasksDone":2,"commitsCount":2,"prNumber":null,"prState":null,"coverageLine":null,"coverageBranch":null}}
```

The rendered report omits the "Pull Request" section because the
template guards that block on `{{prNumber}}`. Exit: 0.

## Outputs

| Artifact | Path | Description |
| :--- | :--- | :--- |
| Rendered report | `<--output>` | Markdown file written via `x-internal-report-write` (atomic tmp+rename) |
| Response envelope | stdout | Single-line JSON (`reportPath` / `summary`) |
| Error diagnostic | stderr | Single line, non-empty only on exit ≠ 0 |

No file is created by this skill directly — every disk mutation is
delegated to `x-internal-report-write`, which provides the atomic
write contract.

## Error Handling

| Scenario | Action |
| :--- | :--- |
| Missing required flag | Print `usage:` banner to stderr; exit 64 |
| Unknown flag | Print `unknown flag <name>; usage:`; exit 64 |
| `--output` empty or traversal-unsafe | Print `output path unsafe`; exit 64 |
| `jq` absent on PATH | Print `dependency missing: jq`; exit 127 |
| `execution-state.json` missing | Exit 1 (`STATE_NOT_FOUND`); no envelope |
| Story node absent from state | Exit 1; stderr distinguishes via message |
| `.tasks` key absent | Treat as empty map (`tasksCount=0`); exit 0 |
| `.pr` key absent | Emit `prNumber=null, prState=null`; exit 0 |
| `.verification.coverage` absent | Emit `coverageLine=null, coverageBranch=null`; exit 0 |
| `.reviews.findings` absent | Emit `findings:[]`; exit 0 |
| Template file absent | Exit 2 (`TEMPLATE_MISSING`); no envelope |
| `x-internal-report-write` UNRESOLVED_PLACEHOLDER (exit 3) | Propagate exit code 3 unchanged; stderr already populated by renderer |
| `x-internal-report-write` WRITE_FAILED (exit 4) | Propagate exit code 4 unchanged |

## Performance Contract

- Target: < 500 ms end-to-end for a typical story (5–10 tasks) on a
  laptop-class machine. Budget breakdown:
  - Argument parse + state read: < 50 ms
  - Summary JSON computation (jq): < 100 ms
  - Payload assembly: < 50 ms
  - Template resolution: < 10 ms
  - `x-internal-report-write` invocation: ~200 ms (its own contract)
  - Envelope emission: < 10 ms
- Memory bounded by state-file size + 2× payload size; the skill
  never loads the rendered report into memory.
- Idempotent under repeated invocations with identical state file
  and `--output` path (the renderer's atomic tmp+rename contract).

## Testing

Story-0049-0015 ships the following acceptance test scenarios,
mirroring Section 7 of the story file:

1. **Happy path** — state with 5 DONE tasks and merged PR renders
   the full report; `summary.tasksCount=5`, `summary.prState=MERGED`.
2. **Story in progress** — 3 DONE + 2 PENDING renders a report with
   per-task status; `summary.tasksDone=3`, `summary.tasksCount=5`.
3. **Error — STATE_NOT_FOUND** — missing state file → exit 1, no
   envelope, stderr matches.
4. **Error — TEMPLATE_MISSING** — template absent → exit 2, no
   envelope, stderr matches.
5. **Boundary — story sem PR** — `.pr` absent → `prNumber=null`,
   `prState=null`, rendered report omits the "Pull Request" section.

Goldens live under
`src/test/resources/golden/internal/plan/x-internal-story-report/`
and lock the SKILL.md rendering (per EPIC-0049 convention: generator
copies internal skills into `.claude/skills/` flat layout but filters
them from `/help` — goldens validate the source, not the flattened
output).

Coverage requirement: ≥ 95% line / ≥ 90% branch across the invoking
Bash codepaths (DoD Global).

## Generator Filter Contract

The `ia-dev-env` generator MUST exclude skills with
`visibility: internal` from:

1. The `.claude/README.md` skill-inventory table.
2. The `/help` menu listing surfaced by Claude Code.
3. User-facing autocomplete in the chat input.

Internal skills are still copied into `.claude/skills/` (flat layout)
so `Skill(skill: "x-internal-story-report")` invocations from other
skills resolve correctly. The invariant: **user cannot see it;
orchestrators can invoke it.**

## Telemetry

Internal skills DO NOT emit `phase.start` / `phase.end` markers —
telemetry is produced by the invoking orchestrator (the `phase`
wrapping the orchestrator's own step — Phase 3 in this case — is
the correct aggregation boundary). Passive hooks still capture
`tool.call` for the underlying `Bash` invocations (`jq`) and the
nested `Skill(x-internal-report-write)` dispatch.

Reference: Rule 13 (Skill Invocation Protocol), Rule 22 (Lifecycle
Integrity Audit), ADR-0010 (Interactive Gates Convention — exempts
internal skills from the 3-option menu contract).

## Integration Notes

| Skill | Relationship | Context |
| :--- | :--- | :--- |
| `x-story-implement` | caller (primary) | Phase 3 final-report carve-out: orchestrator passes `--story-id --epic-id --output` and consumes `{reportPath, summary}` |
| `x-internal-report-write` | downstream (delegated render) | Resolves `{{KEY}}` placeholders and `{{#each tasks}} / {{#each findings}}` loops against the payload built in Step 4 |
| `x-internal-status-update` | peer (read-only consumer) | Both read `execution-state.json`; this skill does NOT write to it |
| `x-internal-story-verify` | upstream producer | Writes `.verification.coverage` consumed in Step 3 |
| `x-internal-story-resume` | peer | Shares the DONE synonym set and task-classification logic; no runtime coupling |
| `x-review` / `x-review-pr` | upstream producers | Write `.reviews.findings` consumed in Step 4 |
| `x-status-reconcile` | downstream reader | May read the rendered report for epic-wide status reconciliation (never mutates concurrently) |

Downstream story that depends on this skill: story-0049-0019
(orchestrator consumes the envelope and deletes the inline Phase 3
final-report block).

Full workflow detail (argument-parser rejection matrix, summary
computation edge cases, render-payload schema contract with the
template, PR-state normalisation rules, performance profiling on
large stories, and the forward-compatibility policy for new
`summary` fields) lives in
[`references/full-protocol.md`](references/full-protocol.md) per
ADR-0011.
