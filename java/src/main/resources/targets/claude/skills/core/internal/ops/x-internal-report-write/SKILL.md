---
name: x-internal-report-write
description: "Renders _TEMPLATE-*.md templates by substituting {{KEY}} placeholders (simple and nested, dot-path) and resolving {{#each}} loops against a structured JSON data payload, then writes the result atomically to an output path. Supports an --append mode with per-section deduplication keyed by `## ID: <value>` markers. Centralises phase reports, epic execution reports and planning reports so orchestrators (x-epic-implement, x-epic-orchestrate, x-story-implement) stop duplicating `Read template + inline Edit/Write` logic. Second skill in the x-internal-* convention (after x-internal-status-update pilot): internal visibility, non-user-invocable, subdir scoping under internal/ops/."
visibility: internal
user-invocable: false
allowed-tools: Bash
argument-hint: "--template <name> --output <path> --data <json-or-@path> [--append]"
category: internal-ops
context-budget: light
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

> 🔒 **INTERNAL SKILL**
> Esta skill é invocada apenas por outras skills (orquestradores).
> NÃO é destinada a invocação direta pelo usuário.
> Caller principal: x-epic-implement, x-epic-orchestrate, x-story-implement.
> Segunda skill da convenção `x-internal-*` (após x-internal-status-update,
> a story PILOTO 0049-0005): frontmatter `visibility: internal`, subdir
> `internal/ops/`, marker 🔒 e filtragem do menu `/help` via generator.

# Skill: x-internal-report-write

## Purpose

Render a template file under `.claude/templates/` (or an explicit path) by:

1. Reading the template body from disk.
2. Parsing a structured JSON payload (inline or `@path/to/data.json`).
3. Substituting `{{KEY}}` placeholders, including nested dot-paths
   (`{{stories.story-0049-0001.status}}`).
4. Expanding `{{#each <collection>}}…{{/each}}` blocks with block-local
   placeholder resolution against each array element.
5. Writing the rendered output to a target path — either overwriting,
   or appending with per-ID deduplication when `--append=true`.

This replaces the ad-hoc `Read template + Write output` pattern currently
inlined in `x-epic-implement`, `x-epic-orchestrate`, and `x-story-implement`
(documented in EPIC-0049 S9, S10, S15 analyses), centralising report
rendering so templates can evolve without editing N orchestrator skills.

## Convention Anchors (x-internal-*)

| Aspect | Value | Rationale |
| :--- | :--- | :--- |
| Path | `internal/ops/x-internal-report-write/` | `internal/` prefix scopes visibility; `ops/` aligns with sibling runtime-ops skills |
| Frontmatter `visibility` | `internal` | Generator filters these from `/help` menu |
| Frontmatter `user-invocable` | `false` | Declarative complement to `visibility: internal` |
| Body marker | `> 🔒 **INTERNAL SKILL**` block as first non-frontmatter content | Visible to humans browsing the repo; no parsing required |
| Allowed tools | `Bash` only | Minimal surface; renderer is a single shell pipeline built on `jq` |
| Naming | `x-internal-{subject}-{action}` | Mirrors Rule 04 skill taxonomy; `report-write` = subject+action |

Audit rule: Rule 22 (Lifecycle Integrity) validates every skill under
`internal/**` satisfies all 6 anchors above. Violations fail the
`LifecycleIntegrityAuditTest`.

## Triggers

Bare-slash form is intentionally omitted — this skill is never invoked
by a human typing `/x-internal-report-write` in chat. All invocations
follow Rule 13 INLINE-SKILL pattern from a calling orchestrator:

```markdown
Skill(skill: "x-internal-report-write",
      args: "--template _TEMPLATE-EPIC-EXECUTION-PLAN.md \
             --output plans/epic-0049/reports/exec-plan.md \
             --data @plans/epic-0049/reports/exec-plan.data.json")
```

## Parameters

| Parameter | Required | Default | Description |
| :--- | :--- | :--- | :--- |
| `--template <name>` | M | — | Template file name resolved under `.claude/templates/<name>`. An absolute path is accepted verbatim |
| `--output <path>` | M | — | Target path for the rendered file. Created with parent directories |
| `--data <json-or-@path>` | M | — | Structured payload: inline JSON string, or `@path/to/data.json` to read from disk |
| `--append` | O | `false` | When `true`, merge the rendered output into an existing file using `## ID: <value>` section markers for deduplication (see Step 6) |

## Response Contract

When successful, the skill writes a single-line JSON object to stdout:

| Field | Type | Always Present | Description |
| :--- | :--- | :--- | :--- |
| `outputPath` | `String` | yes | Absolute path of the written file |
| `bytesWritten` | `Integer` | yes | Final size of the output file in bytes |
| `placeholdersReplaced` | `Integer` | yes | Total count of `{{KEY}}` substitutions (loop expansions count each inner substitution once) |
| `entriesAppended` | `Integer\|Null` | only on `--append=true` | Number of new `## ID:` sections appended (existing sections updated in place do not increment this count) |

## Exit Codes

| Code | Name | Condition | Message Format |
| :--- | :--- | :--- | :--- |
| 0 | SUCCESS | Render and write completed | — |
| 1 | TEMPLATE_NOT_FOUND | Template file cannot be read | `Template '<name>' not found in .claude/templates/` |
| 2 | INVALID_JSON | `--data` payload fails to parse | `Invalid JSON in --data` |
| 3 | UNRESOLVED_PLACEHOLDER | Strict-mode placeholder has no value in data | `Placeholder '{{KEY}}' has no value (strict mode)` |
| 4 | WRITE_FAILED | Atomic `mv` of tmp file failed | `Failed to write <path>` |

## Placeholder Grammar

### Simple and nested

- `{{key}}` — top-level scalar lookup against the data object.
- `{{a.b.c}}` — dot-path into nested objects. Arrays are indexable via
  `{{stories.0.id}}` but this form is discouraged outside tests; use
  `{{#each}}` for list rendering.
- Missing keys in strict mode (default) trigger `UNRESOLVED_PLACEHOLDER`
  (exit 3). The error message includes the exact placeholder token.

### `{{#each <collection>}} … {{/each}}`

- The collection selector is a dot-path resolving to an array of objects
  (`stories`, `findings`, `tasks.pending`).
- Inside the block, placeholders are resolved against the current array
  element first; a miss falls back to the root data object. This lets
  templates interleave per-item fields (`{{id}}`, `{{status}}`) with
  outer context (`{{epicId}}`).
- Blocks MUST balance: every `{{#each}}` needs exactly one matching
  `{{/each}}` at the same nesting depth. Nested loops are allowed up to
  depth 3 (templates-in-the-wild never exceed depth 2).
- An empty collection renders the block body zero times — no error.

## Workflow

### Step 1 — Argument parsing and validation

Parse flags; reject unknown flags. Reject duplicate flags. Enforce:

- `--template`, `--output`, `--data` all mandatory.
- `--append` is boolean; absence defaults to `false`.

When `--template` is a bare name (no `/`), resolve against
`<CLAUDE_PROJECT_DIR>/.claude/templates/<name>`; an absolute or
relative path containing `/` is used verbatim (subject to traversal
guard against `..` above the project root per Rule 06).

### Step 2 — Template read

```bash
if [[ ! -r "${template_path}" ]]; then
  echo "Template '${template_name}' not found in .claude/templates/" >&2
  exit 1
fi
template_body=$(cat "${template_path}")
```

### Step 3 — Data payload parse

```bash
if [[ "${data_arg}" == @* ]]; then
  data_path="${data_arg#@}"
  [[ -r "${data_path}" ]] || { echo "Invalid JSON in --data" >&2; exit 2; }
  data_json=$(cat "${data_path}")
else
  data_json="${data_arg}"
fi
if ! echo "${data_json}" | jq -e '.' >/dev/null 2>&1; then
  echo "Invalid JSON in --data" >&2
  exit 2
fi
```

### Step 4 — `{{#each}}` expansion (inside-out)

Expand the deepest `{{#each}}` block first, substituting per-element
placeholders against the current array element (with fallback to root
data). Repeat until no `{{#each}}` tokens remain. A simple regex-based
state machine is sufficient because blocks are well-bracketed and
never straddle line continuations.

Implementation contract:

```bash
while echo "${body}" | grep -q '{{#each '; do
  body=$(expand_innermost_each "${body}" "${data_json}")
done
```

The helper `expand_innermost_each`:

1. Locates the innermost `{{#each <expr>}} … {{/each}}` match.
2. Resolves `<expr>` via `jq` against `${data_json}`; if the result is
   not an array, exit with a clear error (non-zero, message includes
   the expression).
3. For each element, substitutes block-local placeholders (`{{field}}`
   or `{{field.nested}}`), falling back to the root data.
4. Concatenates rendered elements in array order, replacing the whole
   `{{#each}}…{{/each}}` span in the body.

### Step 5 — Simple placeholder substitution (strict)

After all `{{#each}}` blocks are resolved, walk remaining
`{{<dot-path>}}` tokens and substitute each via `jq -r`. When the
lookup yields `null` or the path is absent, exit 3 with
`UNRESOLVED_PLACEHOLDER` and the exact token in the message. Count
every successful substitution into `placeholdersReplaced`.

### Step 6 — Atomic write (overwrite or --append merge)

**Overwrite mode (default):**

```bash
mkdir -p "$(dirname "${output_path}")"
tmp="${output_path}.tmp.$$"
printf '%s' "${rendered}" > "${tmp}"
if ! mv "${tmp}" "${output_path}"; then
  rm -f "${tmp}"
  echo "Failed to write ${output_path}" >&2
  exit 4
fi
```

**Append mode (`--append=true`):** the rendered body is interpreted as
a sequence of one or more sections, each beginning with a line of the
form `## ID: <value>` (leading `##` required; value is the trimmed
remainder). For every such section in the rendered body:

1. Scan the existing output file for an identical `## ID: <value>`
   marker.
2. If found, replace the existing section (up to the next `## ID:`
   marker or EOF) with the new section body. Do not increment
   `entriesAppended`.
3. If not found, append the new section at the end. Increment
   `entriesAppended`.
4. When the existing output file does not exist, behave as overwrite
   mode and count every rendered section as appended.

Both branches end with the same atomic `mv` tmp → final contract as
overwrite mode. The append merge is performed in-memory (a single
awk/jq pipeline), never line-by-line via interleaved writes.

### Step 7 — Emit response

```bash
bytes=$(wc -c < "${output_path}" | tr -d ' ')
if [[ "${append_mode}" == true ]]; then
  printf '{"outputPath":"%s","bytesWritten":%s,"placeholdersReplaced":%s,"entriesAppended":%s}\n' \
    "${output_path}" "${bytes}" "${replaced}" "${appended}"
else
  printf '{"outputPath":"%s","bytesWritten":%s,"placeholdersReplaced":%s,"entriesAppended":null}\n' \
    "${output_path}" "${bytes}" "${replaced}"
fi
```

Exit 0.

## Examples

### Example 1 — Simple render without loops

Template `.claude/templates/_TEMPLATE-EPIC-HEADER.md`:

```markdown
# Epic {{epicId}} — {{title}}

Status: {{status}}
```

Invocation:

```markdown
Skill(skill: "x-internal-report-write",
      args: "--template _TEMPLATE-EPIC-HEADER.md \
             --output plans/epic-0049/reports/header.md \
             --data '{\"epicId\":\"0049\",\"title\":\"Skill hygiene\",\"status\":\"IN_PROGRESS\"}'")
```

Output file contains `Epic 0049 — Skill hygiene` and `Status: IN_PROGRESS`.
Stdout:

```json
{"outputPath":"plans/epic-0049/reports/header.md","bytesWritten":52,"placeholdersReplaced":3,"entriesAppended":null}
```

Exit: 0.

### Example 2 — `{{#each}}` loop with per-item fields

Template fragment:

```markdown
{{#each stories}}
- {{id}} ({{status}})
{{/each}}
```

Data:

```json
{"stories":[{"id":"story-0049-0001","status":"DONE"},
            {"id":"story-0049-0002","status":"DONE"},
            {"id":"story-0049-0006","status":"IN_PROGRESS"}]}
```

Rendered body:

```markdown
- story-0049-0001 (DONE)
- story-0049-0002 (DONE)
- story-0049-0006 (IN_PROGRESS)
```

`placeholdersReplaced` = 6 (two per iteration).

### Example 3 — `--append` with update in place (no duplication)

Existing `plans/epic-0049/reports/status.md`:

```markdown
## ID: story-0049-0001
status: PENDING

## ID: story-0049-0002
status: PENDING
```

Invocation with data `{"id":"story-0049-0001","status":"DONE"}` and a
template producing the two-line `## ID: ... / status: ...` block:

```markdown
Skill(skill: "x-internal-report-write",
      args: "--template _TEMPLATE-STATUS-ENTRY.md \
             --output plans/epic-0049/reports/status.md \
             --data '{\"id\":\"story-0049-0001\",\"status\":\"DONE\"}' \
             --append true")
```

Final file:

```markdown
## ID: story-0049-0001
status: DONE

## ID: story-0049-0002
status: PENDING
```

Stdout:

```json
{"outputPath":"plans/epic-0049/reports/status.md","bytesWritten":74,"placeholdersReplaced":2,"entriesAppended":0}
```

Exit: 0. Note `entriesAppended=0` because the rendered ID already
existed and was updated in place.

### Example 4 — `--append` with new entries

Same output file as Example 3; data now introduces an unseen ID:

```json
{"id":"story-0049-0006","status":"IN_PROGRESS"}
```

Final file gains a third section at the end:

```markdown
## ID: story-0049-0001
status: DONE

## ID: story-0049-0002
status: PENDING

## ID: story-0049-0006
status: IN_PROGRESS
```

Stdout: `"entriesAppended":1`.

### Example 5 — Error: template not found

```markdown
Skill(skill: "x-internal-report-write",
      args: "--template _TEMPLATE-NONEXISTENT.md \
             --output /tmp/out.md \
             --data '{}'")
```

Stderr:

```
Template '_TEMPLATE-NONEXISTENT.md' not found in .claude/templates/
```

Exit: 1.

### Example 6 — Error: unresolved placeholder (strict mode)

Template contains `{{undefined_key}}`; data is `{"epicId":"0049"}`.

Stderr:

```
Placeholder '{{undefined_key}}' has no value (strict mode)
```

Exit: 3.

### Example 7 — Error: invalid JSON

`--data '{epicId:0049}'` (unquoted keys).

Stderr:

```
Invalid JSON in --data
```

Exit: 2.

## Outputs

| Artifact | Path | Description |
| :--- | :--- | :--- |
| Rendered report | `<--output>` | Markdown file written via tmp+rename; overwrite or merged-append |
| Response envelope | stdout | Single-line JSON (`outputPath` / `bytesWritten` / `placeholdersReplaced` / `entriesAppended`) |
| Temp file | `<--output>.tmp.$$` | Removed on successful rename; removed on failure |

## Error Handling

| Scenario | Action |
| :--- | :--- |
| Missing required flag | Print `usage:` banner to stderr; exit 64 (sysexits EX_USAGE) |
| `jq` absent on PATH | Exit 127 with `jq is required`; abort before any write |
| `--data @path` points to unreadable file | Exit 2 (`INVALID_JSON`) with the path echoed |
| `{{#each expr}}` resolves to non-array | Exit non-zero; message includes the expression and the resolved type |
| Unbalanced `{{#each}}` / `{{/each}}` | Exit non-zero; message identifies the dangling token |
| Strict-mode miss on any placeholder | Exit 3 (`UNRESOLVED_PLACEHOLDER`) with the full token text |
| `mv` fails mid-write | Delete tmp file; exit 4 (`WRITE_FAILED`); existing output file is left untouched |
| Output parent directory not writable | Exit 4 after cleaning up the tmp file |
| Path traversal attempt (`..` above project root) | Exit non-zero; message names the offending path (Rule 06) |

## Performance Contract

- Render < 200 ms for templates < 50 KB with ≤ 3 nested `{{#each}}`
  levels (DoD-local target of story-0049-0006).
- Memory usage bounded by template size + 2× data-payload size; the
  renderer streams rendered body to tmp file and never holds the final
  output file in memory.
- Idempotent under repeated invocations with identical template, data,
  and output path (same bytes, same sha256).

## Testing

Story-0049-0006 ships acceptance test scenarios that every future
`x-internal-report-write` consumer MUST be able to rely on:

1. **Simple render** — `{{epicId}}` substituted; `placeholdersReplaced=1`.
2. **Nested placeholder** — `{{stories.story-0049-0001.status}}` resolves.
3. **`{{#each}}` happy path** — array of three stories renders three lines.
4. **`{{#each}}` empty collection** — zero iterations, no error.
5. **`--append` update in place** — existing `## ID:` section replaced;
   `entriesAppended=0`; file sha256 changes; no duplicate section.
6. **`--append` new entry** — unseen ID appended at EOF;
   `entriesAppended=1`.
7. **TEMPLATE_NOT_FOUND** — absent template; exit 1; stderr matches.
8. **INVALID_JSON** — malformed `--data`; exit 2.
9. **UNRESOLVED_PLACEHOLDER** — strict miss; exit 3 with exact token.
10. **WRITE_FAILED** — output directory read-only; exit 4; tmp cleaned up.

Goldens under
`src/test/resources/golden/internal/ops/x-internal-report-write/` lock
the SKILL.md rendering. Coverage requirement: ≥ 95% line / ≥ 90%
branch across the invoking Bash codepaths.

## Generator Filter Contract

The `ia-dev-env` generator MUST exclude skills with
`visibility: internal` from:

1. The `.claude/README.md` skill-inventory table.
2. The `/help` menu listing surfaced by Claude Code.
3. User-facing autocomplete in the chat input.

Internal skills are still copied into `.claude/skills/` (flat layout)
so `Skill(skill: "x-internal-report-write")` invocations from other
skills resolve correctly. The invariant — set by the pilot story
(0049-0005) — holds here: **user cannot see it; orchestrators can
invoke it.**

## Telemetry

Internal skills DO NOT emit `phase.start` / `phase.end` markers —
telemetry is produced by the invoking orchestrator (the `phase` wrapping
the orchestrator's own step is the correct aggregation boundary).
Passive hooks still capture `tool.call` for the underlying `Bash`
invocation.

Reference: Rule 13 (Skill Invocation Protocol), Rule 22 (Lifecycle
Integrity Audit), ADR-0010 (Interactive Gates Convention — exempts
internal skills from the 3-option menu contract).

## Integration Notes

| Skill | Relationship | Context |
| :--- | :--- | :--- |
| `x-epic-implement` | caller | Phase-completion reports, epic execution plan rendering |
| `x-epic-orchestrate` | caller | Per-story planning report rendering |
| `x-story-implement` | caller | Story-level remediation / review dashboard rendering |
| `x-internal-status-update` | peer | Sibling `x-internal-*` skill; both scoped under `internal/ops/`; no shared runtime state |
| `x-status-reconcile` | consumer | Downstream reader of reports produced here; never mutates concurrently (Rule 22) |

Downstream stories that depend on this skill: story-0049-0009
(x-epic-implement consumes it), story-0049-0010 (x-epic-orchestrate
consumes it), story-0049-0015 (x-story-implement consumes it).
