---
name: x-internal-status-update
description: "Atomic read-modify-write of execution-state.json with flock-based concurrency, schema validation, and idempotency detection. Substitutes inline Edit-based mutations in orchestrator skills (x-epic-implement, x-story-implement, x-pr-fix-epic) that previously suffered race conditions in --parallel mode. PILOT skill for the x-internal-* convention: internal visibility, non-user-invocable, subdir scoping under internal/ops/."
visibility: internal
user-invocable: false
allowed-tools: Bash
argument-hint: "--file <path> --type <epic|story|task> --id <id> --field <name> --value <value> [--initialize] [--read-only]"
category: internal-ops
context-budget: medium
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

> 🔒 **INTERNAL SKILL**
> Esta skill é invocada apenas por outras skills (orquestradores).
> NÃO é destinada a invocação direta pelo usuário.
> Caller principal: x-epic-implement, x-story-implement, x-pr-fix-epic.
> Esta é a story PILOTO (story-0049-0005) da convenção `x-internal-*`:
> frontmatter `visibility: internal`, subdir `internal/ops/`, marker 🔒, e
> filtragem do menu `/help` via generator.

# Skill: x-internal-status-update

## Purpose

Perform atomic read-modify-write mutations of
`plans/epic-XXXX/execution-state.json` (the telemetry checkpoint file
consumed by every orchestrator skill). The operation:

1. Acquires a `flock`-based advisory lock with 30s timeout.
2. Reads the full JSON document.
3. Validates the resolved path exists in the schema.
4. Compares `previousValue` vs `newValue` — emits `noOp=true` when equal
   (idempotency per RULE-002).
5. Writes via tmp-file + rename (atomic on POSIX).
6. Releases the lock.

This replaces ad-hoc `Edit`-tool invocations from inside orchestrators,
which historically lost updates under `--parallel` execution (documented
in EPIC-0042 post-mortems).

## Convention Anchors (x-internal-* PILOT)

| Aspect | Value | Rationale |
| :--- | :--- | :--- |
| Path | `internal/ops/x-internal-status-update/` | `internal/` prefix scopes visibility; `ops/` aligns with sibling runtime-ops skills |
| Frontmatter `visibility` | `internal` | Generator filters these from `/help` menu |
| Frontmatter `user-invocable` | `false` | Declarative complement to `visibility: internal` |
| Body marker | `> 🔒 **INTERNAL SKILL**` block as first non-frontmatter content | Visible to humans browsing the repo; no parsing required |
| Allowed tools | `Bash` only | Minimal surface; all logic is a single shell pipeline |
| Naming | `x-internal-{subject}-{action}` | Mirrors Rule 04 skill taxonomy; `status-update` = subject+action |

Audit rule: Rule 22 (Lifecycle Integrity) validates every skill under
`internal/**` satisfies all 6 anchors above. Violations fail the
`LifecycleIntegrityAuditTest`.

## Triggers

Bare-slash form is intentionally omitted — this skill is never invoked
by a human typing `/x-internal-status-update` in chat. All invocations
follow Rule 13 INLINE-SKILL pattern from a calling orchestrator:

```markdown
Skill(skill: "x-internal-status-update",
      args: "--file plans/epic-0049/execution-state.json \
             --type story --id story-0049-0005 \
             --field status --value MERGED")
```

## Parameters

| Parameter | Required | Default | Description |
| :--- | :--- | :--- | :--- |
| `--file <path>` | O | `execution-state.json` in cwd | Target state file |
| `--type <epic\|story\|task>` | M | — | Scope of the node being mutated |
| `--id <id>` | M | — | Epic ID (`0049`), story ID (`story-0049-0005`), or task ID (`TASK-0049-0005-001`) |
| `--field <name>` | M | — | Schema field to update (e.g., `status`, `prNumber`, `commitSha`) |
| `--value <value>` | M | — | New value; coerced to the schema-declared type |
| `--initialize` | O | `false` | Create the file with an empty schema skeleton when absent |
| `--read-only` | O | `false` | Read and return the current value without acquiring the write lock |

## Response Contract

When successful, the skill writes a single-line JSON object to stdout:

| Field | Type | Always Present | Description |
| :--- | :--- | :--- | :--- |
| `previousValue` | `String\|Null` | yes | Value before write; null when field was absent |
| `newValue` | `String` | yes | Value after write (equal to previous on no-op) |
| `fileSha` | `String(64)` | yes | sha256 of the file after the write |
| `noOp` | `Boolean` | yes | `true` when `previousValue == newValue` |

## Exit Codes

| Code | Name | Condition | Message Format |
| :--- | :--- | :--- | :--- |
| 0 | SUCCESS | Write or no-op completed | — |
| 1 | FILE_NOT_FOUND | File missing and `--initialize=false` | `State file not found: <path>` |
| 2 | LOCK_TIMEOUT | `flock` timed out after 30s | `Lock timeout on <path>.lock` |
| 3 | INVALID_PATH | `--type/--id/--field` tuple does not resolve | `Path '<resolved-path>' not found in schema` |
| 4 | WRITE_FAILED | Atomic `mv` of tmp file failed | `Atomic write failed: <stderr>` |

## Workflow

### Step 1 — Argument parsing and validation

Parse flags; reject unknown flags; enforce mutual exclusivity of
`--read-only` with any write-implying flag combination. When
`--type=epic`, the resolved path is `<field>` at document root; when
`--type=story`, the path is `stories.<id>.<field>`; when `--type=task`,
the path is `stories.<parentStoryId>.tasks.<id>.<field>`. Parent story
ID is inferred from the task ID prefix (`TASK-0049-0005-001` ⇒
`story-0049-0005`).

### Step 2 — Lock acquisition

```bash
lock_file="${file}.lock"
exec {fd}>"${lock_file}"
if ! flock -w 30 "${fd}"; then
  echo "Lock timeout on ${lock_file}" >&2
  exit 2
fi
```

### Step 3 — File existence and initialization

- When the file is absent and `--initialize=false`: exit 1.
- When absent and `--initialize=true`: write an empty skeleton
  `{"version":1,"stories":{}}` via the same atomic tmp+rename contract
  before proceeding.

### Step 4 — Read and validate path

Parse JSON via `jq`; resolve the path per Step 1. When the path does
not exist, exit 3 with the resolved path in the message.

### Step 5 — Idempotency check

Compute `previousValue` at the resolved path. When
`previousValue == newValue`, emit the response JSON with `noOp=true`
and exit 0 without touching the file. The lock is still released via
`flock` descriptor closure.

### Step 6 — Atomic write

```bash
tmp="${file}.tmp.$$"
jq --arg v "${newValue}" "<path-expression>" "${file}" > "${tmp}"
if ! mv "${tmp}" "${file}"; then
  rm -f "${tmp}"
  echo "Atomic write failed: mv returned non-zero" >&2
  exit 4
fi
```

### Step 7 — Emit response and release lock

```bash
file_sha=$(shasum -a 256 "${file}" | cut -d' ' -f1)
printf '{"previousValue":%s,"newValue":"%s","fileSha":"%s","noOp":false}\n' \
  "${prev_json}" "${newValue}" "${file_sha}"
```

The `flock` descriptor is closed on process exit; no explicit unlock
is required.

### Step 8 — `--read-only` short-circuit

When `--read-only=true`, Steps 2, 6, and 7 are skipped. The skill
opens the file with a shared (`flock -s`) lock, reads the value,
emits the response with `noOp=true` and `newValue==previousValue`,
and exits 0.

## Examples

### Example 1 — Happy path: mark a story as MERGED

```bash
Skill(skill: "x-internal-status-update",
      args: "--file plans/epic-0049/execution-state.json \
             --type story --id story-0049-0005 \
             --field status --value MERGED")
```

Output:
```json
{"previousValue":"IN_PROGRESS","newValue":"MERGED","fileSha":"a1b2...","noOp":false}
```
Exit: 0.

### Example 2 — No-op: value already matches

```bash
Skill(skill: "x-internal-status-update",
      args: "--file plans/epic-0049/execution-state.json \
             --type story --id story-0049-0005 \
             --field status --value MERGED")
```

Output:
```json
{"previousValue":"MERGED","newValue":"MERGED","fileSha":"a1b2...","noOp":true}
```
Exit: 0.

### Example 3 — Initialize a fresh state file

```bash
Skill(skill: "x-internal-status-update",
      args: "--file plans/epic-0049/execution-state.json \
             --type epic --id 0049 \
             --field flowVersion --value 2 \
             --initialize")
```

Output:
```json
{"previousValue":null,"newValue":"2","fileSha":"c3d4...","noOp":false}
```
Exit: 0.

### Example 4 — Task-level update

```bash
Skill(skill: "x-internal-status-update",
      args: "--file plans/epic-0049/execution-state.json \
             --type task --id TASK-0049-0005-003 \
             --field prNumber --value 612")
```

Output:
```json
{"previousValue":null,"newValue":"612","fileSha":"e5f6...","noOp":false}
```
Exit: 0.

### Example 5 — Read-only query

```bash
Skill(skill: "x-internal-status-update",
      args: "--file plans/epic-0049/execution-state.json \
             --type story --id story-0049-0005 \
             --field status --value UNUSED \
             --read-only")
```

Output:
```json
{"previousValue":"MERGED","newValue":"MERGED","fileSha":"a1b2...","noOp":true}
```
Exit: 0. Note: `--value` is required by the argument schema but is
ignored under `--read-only`.

### Example 6 — Invalid path (schema rejection)

```bash
Skill(skill: "x-internal-status-update",
      args: "--file plans/epic-0049/execution-state.json \
             --type story --id unknown-story \
             --field status --value DONE")
```

Stderr:
```
Path 'stories.unknown-story.status' not found in schema
```
Exit: 3.

## Outputs

| Artifact | Path | Description |
| :--- | :--- | :--- |
| Updated state file | `<--file>` | JSON document mutated in place via atomic rename |
| Response envelope | stdout | Single-line JSON (`previousValue` / `newValue` / `fileSha` / `noOp`) |
| Lock file | `<--file>.lock` | Created empty on first invocation; retained for reuse |

## Error Handling

| Scenario | Action |
| :--- | :--- |
| Missing required flag | Print `usage:` banner to stderr; exit 64 (sysexits EX_USAGE) |
| `jq` absent on PATH | Exit 127 with `jq is required`; abort before lock |
| Concurrent invocation exceeds 30s wait | Exit 2 (`LOCK_TIMEOUT`) — caller retries with backoff |
| `mv` fails mid-write | Delete tmp file; exit 4 (`WRITE_FAILED`); state file is left untouched |
| `--initialize` collides with existing non-JSON file | Exit 4 — do not overwrite |
| Schema version mismatch | Log warning to stderr; proceed (non-blocking per RULE-006) |

## Concurrency Contract

- All readers and writers must acquire the same `<file>.lock` file
  descriptor. Exclusive (`flock -x`) for writes; shared (`flock -s`)
  for `--read-only`.
- Two concurrent invocations targeting **different fields** of the
  same file serialize through the lock; both updates are preserved.
- Two concurrent invocations targeting **the same field** serialize;
  the later caller observes the earlier caller's value as
  `previousValue` — this is the correct "last writer wins" semantic.
- Lock is released automatically on process exit (file descriptor
  closure). No `trap`-based cleanup required.

## Testing

The PILOT story (story-0049-0005) ships the following acceptance test
scenarios, which are the reference contract every future `x-internal-*`
skill MUST replicate in its own directory:

1. **Write happy path** — status transition PENDING → MERGED;
   assert `noOp=false` and fileSha changes.
2. **No-op detection** — same value twice; assert `noOp=true`, file
   mtime unchanged.
3. **Read-only** — assert no write occurs; shared lock only.
4. **Lock contention** — spawn 2 concurrent processes mutating
   distinct fields; both updates present in final file; JSON valid.
5. **FILE_NOT_FOUND** — absent file without `--initialize`; exit 1.
6. **INVALID_PATH** — unknown story ID; exit 3 with path in message.

Goldens under
`src/test/resources/golden/internal/ops/x-internal-status-update/`
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
| `x-epic-implement` | caller | Phase 2 (per-story status transitions) + Phase 4 (epic finalization) |
| `x-story-implement` | caller | Phase 2 (per-task status transitions) + Phase 3 (story finalization) |
| `x-pr-fix-epic` | caller | Records per-PR correction state when fanning out across an epic |
| `x-status-reconcile` | peer | Reads the same file to diagnose drift against markdown; never mutates concurrently with this skill (Rule 22) |
| `x-parallel-eval` | consumer | Reads the resulting state file to build the collision matrix |

Downstream stories that depend on this PILOT: story-0049-0013,
story-0049-0018, story-0049-0019.
