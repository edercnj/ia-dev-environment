---
name: x-internal-args-normalize
description: "Parses an argv string against a declarative JSON schema with typed flags (boolean, string, integer, enum), defaults, mutually-exclusive groups, and deprecation warnings, then emits a normalized `{parsed, warnings, errors}` envelope on stdout. Replaces ~150 lines of inline argv parsing inlined inside `x-epic-implement`, `x-story-implement`, and `x-epic-orchestrate`, giving every orchestrator identical flag-validation syntax and error messages. Third skill in the x-internal-* convention (after x-internal-status-update pilot and x-internal-report-write): internal visibility, non-user-invocable, subdir scoping under internal/ops/."
visibility: internal
user-invocable: false
allowed-tools: Bash
argument-hint: "--schema <json-or-@path> --argv <string>"
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
> Caller principal: x-epic-implement, x-story-implement, x-epic-orchestrate.
> Terceira skill da convenção `x-internal-*` (após x-internal-status-update,
> a story PILOTO 0049-0005, e x-internal-report-write, story 0049-0006):
> frontmatter `visibility: internal`, subdir `internal/ops/`, marker 🔒 e
> filtragem do menu `/help` via generator.

# Skill: x-internal-args-normalize

## Purpose

Parse a Bash-style argv string against a declarative JSON schema and produce
a normalized envelope describing:

1. `parsed` — map of flag name → typed value (after defaults applied).
2. `warnings` — list of deprecation notices emitted for seen `deprecated` flags.
3. `errors` — list of validation errors (populated only on non-zero exit).

This removes the ~150 lines of ad-hoc argv parsing currently duplicated inside
`x-epic-implement`, `x-story-implement`, and `x-epic-orchestrate` (catalogued
in EPIC-0049 story-0049-0007 dependency matrix), so future orchestrators can
declare their flag surface once — as a schema — and reuse the same parser,
the same error codes, and the same deprecation-warning policy.

## Convention Anchors (x-internal-*)

| Aspect | Value | Rationale |
| :--- | :--- | :--- |
| Path | `internal/ops/x-internal-args-normalize/` | `internal/` prefix scopes visibility; `ops/` aligns with sibling runtime-ops skills |
| Frontmatter `visibility` | `internal` | Generator filters these from `/help` menu |
| Frontmatter `user-invocable` | `false` | Declarative complement to `visibility: internal` |
| Body marker | `> 🔒 **INTERNAL SKILL**` block as first non-frontmatter content | Visible to humans browsing the repo; no parsing required |
| Allowed tools | `Bash` only | Minimal surface; parser is a single shell pipeline built on `jq` |
| Naming | `x-internal-{subject}-{action}` | Mirrors Rule 04 skill taxonomy; `args-normalize` = subject+action |

Audit rule: Rule 22 (Lifecycle Integrity) validates every skill under
`internal/**` satisfies all 6 anchors above. Violations fail the
`LifecycleIntegrityAuditTest`.

## Triggers

Bare-slash form is intentionally omitted — this skill is never invoked
by a human typing `/x-internal-args-normalize` in chat. All invocations
follow Rule 13 INLINE-SKILL pattern from a calling orchestrator:

```markdown
Skill(skill: "x-internal-args-normalize",
      args: "--schema @.claude/schemas/x-epic-implement.args.json \
             --argv \"EPIC-0049 --parallel --auto-merge-strategy squash\"")
```

## Parameters

| Parameter | Required | Default | Description |
| :--- | :--- | :--- | :--- |
| `--schema <json-or-@path>` | M | — | Declarative schema describing flags, defaults, mutually-exclusive groups, and deprecations. Inline JSON string OR `@path/to/schema.json` read from disk |
| `--argv <string>` | M | — | Bash-quoted argv string exactly as the caller received it (positional tokens preserved; `--flag value` and `--flag=value` both honoured) |

## Schema Contract

The schema is a JSON object with two top-level keys: `flags` (required array)
and `mutuallyExclusive` (optional array of arrays of flag names).

```json
{
  "flags": [
    {"name": "--parallel", "type": "boolean", "default": false},
    {"name": "--story-id", "type": "string"},
    {"name": "--max-retries", "type": "integer", "default": 3},
    {"name": "--auto-merge-strategy", "type": "enum",
     "values": ["merge", "squash", "rebase"], "default": "merge"},
    {"name": "--no-merge", "type": "boolean",
     "deprecated": "use --auto-merge-strategy=<strategy> instead"}
  ],
  "mutuallyExclusive": [
    ["--auto-merge", "--no-merge", "--interactive-merge"]
  ]
}
```

### Per-flag fields

| Field | Required | Description |
| :--- | :--- | :--- |
| `name` | M | Flag token including leading `--` (double-dash mandatory). Unique across `flags[]` |
| `type` | M | One of `boolean`, `string`, `integer`, `enum` |
| `values` | M if `type=enum` | Non-empty array of allowed string values |
| `default` | O | Typed default applied when the flag is absent from argv. For `boolean`, default is `false` unless set explicitly |
| `deprecated` | O | Non-empty string to emit as a warning when the flag appears in argv. Flag is still parsed and returned in `parsed` |

### `mutuallyExclusive`

Each inner array lists flag names; at most one may be present in argv.
Violation triggers exit 2 (`MUTUALLY_EXCLUSIVE`). Empty or absent outer
array disables the check. Names MUST match `flags[].name` exactly;
references to unknown flags are a schema error.

## Response Contract

On success (exit 0), the skill writes a single-line JSON object to stdout:

| Field | Type | Always Present | Description |
| :--- | :--- | :--- | :--- |
| `parsed` | `Object` | yes | Map of flag-name (no leading `--`, kebab preserved) to typed value. Includes every schema-declared flag (explicit or defaulted); omits flags with no default that were absent from argv |
| `warnings` | `List<String>` | yes | Deprecation messages emitted for each `deprecated` flag seen in argv (message = the schema's `deprecated` string verbatim). Empty array if none |
| `errors` | `List<String>` | yes | Validation errors. Empty array on success. Populated and echoed to stderr on non-zero exit |

On non-zero exit, stderr carries the same human-readable error messages;
stdout still emits a best-effort envelope with `errors` populated and
`parsed` containing whatever was resolvable before the failure.

## Exit Codes

| Code | Name | Condition | Message Format |
| :--- | :--- | :--- | :--- |
| 0 | SUCCESS | Parse completed; all validations passed | — |
| 1 | INVALID_SCHEMA | Schema JSON is malformed or violates contract | `Schema validation failed: <detail>` |
| 2 | MUTUALLY_EXCLUSIVE | Two or more flags from one `mutuallyExclusive` group seen | `Flags <A>, <B> are mutually exclusive` |
| 3 | UNKNOWN_FLAG | argv contains a `--flag` not declared in `flags[]` | `Unknown flag '<--xxx>'` |
| 4 | INVALID_ENUM_VALUE | enum flag value outside `values[]` | `Invalid value '<v>' for <--flag> (allowed: <v1,v2,...>)` |
| 64 | USAGE | Missing `--schema` or `--argv`; unknown skill-level flag | Usage banner on stderr |
| 127 | MISSING_DEPENDENCY | `jq` absent on `PATH` | `jq is required` |

Integer parsing failures (non-numeric token for `type=integer`) and boolean
parsing failures (value for `type=boolean` is neither `true`, `false`, `1`,
`0`, nor absent) are reported under exit 1 with `INVALID_SCHEMA` subcategory
`TYPE_COERCION_FAILED` in the message — the schema declared the type, so
malformed argv violates the schema contract from the parser's perspective.

## Argv Grammar (Summary)

The parser consumes standard Bash-style argv tokens: `--flag value`,
`--flag=value`, implicit `true` for booleans, positional tokens (preserved
under `parsed._positional`), and the POSIX `--` terminator. Full grammar,
quoting rules, and boolean-negation semantics in
[`references/full-protocol.md`](references/full-protocol.md) §1.

## Workflow (High-Level)

| Step | Summary |
| :--- | :--- |
| 1 | Parse skill-level flags (`--schema`, `--argv`). Reject unknowns with exit 64 |
| 2 | Validate schema JSON shape via `jq` (flags array, types, enum `values[]`, unique names, mutex references) |
| 3 | Tokenise argv into positional array (quote-aware via `xargs`) |
| 4 | Walk tokens: `--flag[=value]` matched against schema, else `UNKNOWN_FLAG` |
| 5 | Apply defaults; validate enum values |
| 6 | Check mutually-exclusive groups |
| 7 | Emit deprecation warnings for seen `deprecated` flags |
| 8 | Print `{parsed, warnings, errors}` envelope on stdout; exit with numbered code |

Per-step details, jq pipelines, and the canonical Bash implementation
skeleton live in [`references/full-protocol.md`](references/full-protocol.md) §2.

## Examples (Minimal)

### Defaults applied (empty argv)

Schema `{"flags":[{"name":"--auto-merge-strategy","type":"enum",
"values":["merge","squash"],"default":"merge"}]}`, argv `""`.

Stdout: `{"parsed":{"auto-merge-strategy":"merge"},"warnings":[],"errors":[]}`.
Exit 0.

### Happy path parse

Schema with `--parallel` (boolean) and `--auto-merge-strategy` (enum),
argv `"--parallel --auto-merge-strategy squash"`.

Stdout: `{"parsed":{"parallel":true,"auto-merge-strategy":"squash"},"warnings":[],"errors":[]}`.
Exit 0.

### Mutually-exclusive violation

argv `"--auto-merge --no-merge"` with mutex group.
Stderr: `Flags --auto-merge, --no-merge are mutually exclusive`. Exit 2.

### Deprecation warning

argv `"--no-merge"` against schema with `"deprecated":"use --auto-merge-strategy=merge instead"`.
`warnings[]` carries the schema string verbatim; exit 0.

### Enum invalid value

argv `"--auto-merge-strategy invalid"` against enum `["merge","squash","rebase"]`.
Stderr: `Invalid value 'invalid' for --auto-merge-strategy (allowed: merge,squash,rebase)`. Exit 4.

Six more examples — `--flag=value` form, positional preservation, unknown
flag, invalid schema (missing `--`), boolean coercion failure, integer
coercion failure — in [`references/full-protocol.md`](references/full-protocol.md) §3.

## Outputs

| Artifact | Path | Description |
| :--- | :--- | :--- |
| Response envelope | stdout | Single-line JSON with `parsed`, `warnings`, `errors` |
| Human-readable errors | stderr | One line per error; same text that populates `errors[]` |

This skill writes NO files. The `parsed` envelope is the sole side effect,
consumed by the invoking orchestrator.

## Error Handling (Quick Reference)

| Scenario | Action |
| :--- | :--- |
| Missing `--schema` or `--argv` | Usage banner on stderr; exit 64 |
| `jq` absent on `PATH` | Exit 127 with `jq is required` |
| `--schema @path` unreadable | Exit 1 (`INVALID_SCHEMA`) with the path echoed |
| Schema has zero `flags[]` entries | Exit 1 (`INVALID_SCHEMA`) |
| `flags[].name` missing `--` prefix | Exit 1 with the offending name |
| `type=enum` without `values[]` | Exit 1 |
| `mutuallyExclusive` references unknown flag | Exit 1 with the unknown name |
| Unknown `--flag` in argv | Exit 3 (`UNKNOWN_FLAG`) |
| enum value outside `values[]` | Exit 4 (`INVALID_ENUM_VALUE`) |
| `mutuallyExclusive` group violated | Exit 2 (`MUTUALLY_EXCLUSIVE`) |
| Integer / boolean coercion failure | Exit 1 with `TYPE_COERCION_FAILED` detail |

Complete error-handling matrix, including argv tokenisation failures and
path-traversal guards, in [`references/full-protocol.md`](references/full-protocol.md) §4.

## Performance Contract

- Parse < 50 ms for schemas with ≤ 30 flags and argv ≤ 20 tokens (DoD-local
  target of story-0049-0007).
- Memory usage bounded by schema size + argv size; the parser builds the
  response envelope in-memory via `jq` streaming.
- Deterministic output: identical schema + argv produces byte-identical
  stdout and exit code across runs.
- Idempotent under repeated invocations; no shared state between calls.

## Testing

Story-0049-0007 ships 12 acceptance test scenarios covering defaults,
happy-path parse, `--flag=value` form, positional preservation, mutex
violations, unknown flag, enum invalid value, deprecation warning, and
four invalid-schema / coercion cases. Full matrix in
[`references/full-protocol.md`](references/full-protocol.md) §5.

Goldens under
`src/test/resources/golden/internal/ops/x-internal-args-normalize/` lock
the SKILL.md rendering. Coverage requirement: ≥ 95% line / ≥ 90%
branch across the invoking Bash codepaths.

## Generator Filter Contract

The `ia-dev-env` generator MUST exclude skills with
`visibility: internal` from:

1. The `.claude/README.md` skill-inventory table.
2. The `/help` menu listing surfaced by Claude Code.
3. User-facing autocomplete in the chat input.

Internal skills are still copied into `.claude/skills/` (flat layout)
so `Skill(skill: "x-internal-args-normalize")` invocations from other
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
| `x-epic-implement` | caller | Replaces inline argv parsing in Phase 0 (~150 LOC — story-0049-0018) |
| `x-story-implement` | caller | Replaces inline argv parsing in Phase 0 (~150 LOC — story-0049-0019) |
| `x-epic-orchestrate` | caller | Shares the same schema convention; parses epic-level flags |
| `x-internal-status-update` | peer | Pilot sibling `x-internal-*` skill; both scoped under `internal/ops/` |
| `x-internal-report-write` | peer | Second sibling `x-internal-*` skill; no shared runtime state |

Downstream stories that depend on this skill: story-0049-0018
(x-epic-implement consumes it), story-0049-0019 (x-story-implement
consumes it).
