# x-internal-args-normalize — Full Protocol

Detail carve-out for [`../SKILL.md`](../SKILL.md) (ADR-0007). Normative
contract remains in SKILL.md; this file expands grammar rules, the canonical
Bash workflow, extended examples, the full error-handling matrix, and the
acceptance-test catalogue.

## §1 — Argv Grammar (Full)

The parser consumes standard Bash-style argv tokens:

- `--flag value` — two tokens; next token becomes the value unless it starts
  with `--`.
- `--flag=value` — single token; `=` splits name and value verbatim.
- `--flag` (boolean) — implicit `true`; never consumes the next token when
  the schema types it as boolean, even if the next token is a bare word.
- `--no-flag` — NOT a general negation form; only recognised when the schema
  explicitly declares `--no-flag`. (Boolean negation happens via
  `--flag=false`.)
- Positional arguments (no leading `--`) — preserved under `parsed._positional`
  as an ordered list. An orchestrator may inspect them but the schema does
  NOT declare positional expectations (single-responsibility: this skill is
  a flag parser).
- `--` terminator — every token after a bare `--` is treated as positional
  (standard POSIX convention).

Quoting: the caller passes `--argv` as a single string; the parser applies
`xargs -n 1`-style tokenisation honouring single/double quotes and backslash
escapes. Newlines inside the argv string are treated as whitespace.

Path-traversal guard (Rule 06): when `--schema` is passed as `@path`, the
resolved path MUST reside under `CLAUDE_PROJECT_DIR`. `..` sequences that
resolve above the project root trigger exit 1 (`INVALID_SCHEMA`) with the
offending path echoed.

## §2 — Canonical Workflow (Bash + jq)

### Step 1 — Argument parsing and validation

Parse the two mandatory skill flags (`--schema`, `--argv`). Reject any
other skill-level flag with exit 64. Resolve `@path` for `--schema` (reject
unreadable path as `INVALID_SCHEMA`).

### Step 2 — Schema validation

```bash
if ! echo "${schema_json}" | jq -e '
    (.flags | type == "array" and length > 0)
    and (.flags | all(.name | startswith("--")))
    and (.flags | all(.type as $t |
          ["boolean","string","integer","enum"] | any(. == $t)))
    and (.flags | all(select(.type == "enum") |
          .values | type == "array" and length > 0))
' >/dev/null 2>&1; then
  echo "Schema validation failed: flags contract violation" >&2
  exit 1
fi
```

Additionally reject:

- Duplicate `name` entries within `flags[]`.
- `mutuallyExclusive` entries that reference names absent from `flags[]`.
- `deprecated` values that are empty strings (must be non-empty when present).

### Step 3 — Tokenise argv

Tokenise the `--argv` string into a positional array using a single
`xargs`-based pipeline. Store tokens in `${tokens[@]}` and drop the
positional terminator `--` from the token list (positions after it are
captured under `parsed._positional`).

### Step 4 — Walk tokens against schema

For each token:

1. If it does NOT start with `--` (and we are not after `--`), append to
   `positional[]` and continue.
2. If it is `--flag=value`, split at the first `=`.
3. If it starts with `--`, look up `name` in the schema's `flags[]`.
   - Miss ⇒ exit 3 (`UNKNOWN_FLAG`) with the exact token.
   - Hit ⇒ coerce the value based on declared `type`:
     - `boolean` — implicit `true` when no `=` form; explicit `true`/`false`
       accepted; anything else fails coercion.
     - `string` — verbatim.
     - `integer` — `jq -r 'tonumber'` or reject.
     - `enum` — the raw string; later validated against `values[]`.
   - Record the seen flag (for deprecation and mutually-exclusive detection).

### Step 5 — Apply defaults and per-type validation

Walk `flags[]` in declaration order. For each flag not seen in argv:

- If `default` is present, apply it.
- Boolean flags without explicit `default` default to `false`.
- Flags with no `default` and no value from argv are OMITTED from `parsed`
  (never materialised as `null`).

For seen enum flags, reject values outside `values[]` with exit 4.

### Step 6 — Validate mutually-exclusive groups

For each group in `mutuallyExclusive[]`, count how many group members appear
in the seen-flags set. `count ≥ 2` ⇒ exit 2 with the offending pair listed
in lexical order (first two offenders suffice; the message is deterministic).

### Step 7 — Emit deprecation warnings

For each seen flag whose schema carries `deprecated`, append the exact
`deprecated` string to the `warnings[]` array. Warnings do NOT fail the
parse; exit remains 0 when warnings are the only finding.

### Step 8 — Emit response

```bash
printf '{"parsed":%s,"warnings":%s,"errors":%s}\n' \
  "${parsed_json}" "${warnings_json}" "${errors_json}"
```

All three fields are always present; `warnings` and `errors` default to `[]`.
Exit 0 on success, or the numbered exit code above on any failure (envelope
still emitted with `errors` populated).

## §3 — Extended Examples

### Example 1 — Defaults applied, argv empty

Schema:

```json
{"flags":[{"name":"--auto-merge-strategy","type":"enum",
           "values":["merge","squash"],"default":"merge"}]}
```

Stdout: `{"parsed":{"auto-merge-strategy":"merge"},"warnings":[],"errors":[]}`.
Exit 0.

### Example 2 — Happy path

Schema with `--parallel` (boolean) and `--auto-merge-strategy` (enum);
argv `"--parallel --auto-merge-strategy squash"`.

Stdout: `{"parsed":{"parallel":true,"auto-merge-strategy":"squash"},"warnings":[],"errors":[]}`.
Exit 0.

### Example 3 — Mutually exclusive

Schema with `mutuallyExclusive:[["--auto-merge","--no-merge"]]`; argv
`"--auto-merge --no-merge"`.

Stderr: `Flags --auto-merge, --no-merge are mutually exclusive`.
Stdout envelope carries `errors[]` populated. Exit 2.

### Example 4 — Deprecation warning

Schema `{"flags":[{"name":"--no-merge","type":"boolean","deprecated":"use --auto-merge-strategy=merge instead"}]}`,
argv `"--no-merge"`.

Stdout: `{"parsed":{"no-merge":true},"warnings":["use --auto-merge-strategy=merge instead"],"errors":[]}`.
Exit 0.

### Example 5 — Enum invalid value

Schema with `values:["merge","squash","rebase"]`, argv
`"--auto-merge-strategy invalid"`.

Stderr: `Invalid value 'invalid' for --auto-merge-strategy (allowed: merge,squash,rebase)`.
Exit 4.

### Example 6 — Unknown flag

Schema declares only `--parallel`; argv `"--parallel --typo"`.
Stderr: `Unknown flag '--typo'`. Exit 3.

### Example 7 — Invalid schema (missing `--`)

`--schema '{"flags":[{"name":"no-dash","type":"boolean"}]}'` — flag name
missing the required `--` prefix.
Stderr: `Schema validation failed: flags contract violation`. Exit 1.

### Example 8 — Positional arguments preserved

Schema `{"flags":[{"name":"--parallel","type":"boolean"}]}`, argv
`"EPIC-0049 --parallel extra"`.

Stdout: `{"parsed":{"parallel":true,"_positional":["EPIC-0049","extra"]},"warnings":[],"errors":[]}`.
Exit 0.

### Example 9 — `--flag=value` form

Schema `{"flags":[{"name":"--max-retries","type":"integer","default":3}]}`,
argv `"--max-retries=7"`.
Stdout: `{"parsed":{"max-retries":7},"warnings":[],"errors":[]}`. Exit 0.

### Example 10 — Boolean type coercion failure

Schema `{"flags":[{"name":"--parallel","type":"boolean"}]}`, argv
`"--parallel=maybe"`.
Stderr: `Schema validation failed: TYPE_COERCION_FAILED: --parallel expected boolean, got 'maybe'`.
Exit 1.

### Example 11 — Integer coercion failure

Schema `{"flags":[{"name":"--max-retries","type":"integer"}]}`, argv
`"--max-retries=abc"`.
Stderr: `Schema validation failed: TYPE_COERCION_FAILED: --max-retries expected integer, got 'abc'`.
Exit 1.

### Example 12 — Invalid schema (enum without values)

`--schema '{"flags":[{"name":"--mode","type":"enum"}]}'` — enum declared
without the required `values[]` array.
Stderr: `Schema validation failed: flags contract violation`. Exit 1.

## §4 — Full Error-Handling Matrix

| Scenario | Exit | Handling |
| :--- | :--- | :--- |
| Missing `--schema` or `--argv` | 64 | Print `usage:` banner on stderr |
| Unknown skill-level flag (e.g., `--foo`) | 64 | Same usage banner |
| `jq` absent on `PATH` | 127 | Abort before any parse |
| `--schema @path` points to unreadable file | 1 | `INVALID_SCHEMA` with path echoed |
| Schema JSON malformed | 1 | `INVALID_SCHEMA` with jq parse error detail |
| Schema has zero `flags[]` entries | 1 | `INVALID_SCHEMA` — empty schemas are rejected |
| `flags[].name` missing `--` prefix | 1 | `INVALID_SCHEMA` with the offending name |
| Duplicate `name` in `flags[]` | 1 | `INVALID_SCHEMA` with the duplicate name |
| `type=enum` without `values[]` | 1 | `INVALID_SCHEMA` |
| `type=enum` with empty `values[]` | 1 | `INVALID_SCHEMA` |
| `mutuallyExclusive` references unknown flag | 1 | `INVALID_SCHEMA` with the unknown name |
| `deprecated` value is empty string | 1 | `INVALID_SCHEMA` |
| Unknown `--flag` in argv | 3 | `UNKNOWN_FLAG` with exact token |
| enum value outside `values[]` | 4 | `INVALID_ENUM_VALUE` with allowed list |
| `mutuallyExclusive` group violated | 2 | First two offenders listed, lexical order |
| Integer coercion failure (`--n=abc`) | 1 | `TYPE_COERCION_FAILED` detail in message |
| Boolean coercion failure (`--flag=maybe`) | 1 | `TYPE_COERCION_FAILED` detail in message |
| Argv tokenisation fails (unmatched quote) | 1 | `INVALID_ARGV` detail; no partial parse |
| Path traversal attempt (`--schema @../foo`) | 1 | Rule 06 guard with offending path |

## §5 — Acceptance Test Catalogue

Story-0049-0007 ships acceptance test scenarios that every future
`x-internal-args-normalize` consumer MUST be able to rely on:

1. **Defaults applied** — empty argv, schema with defaults → `parsed`
   reflects every default, `warnings=[]`, `errors=[]`, exit 0.
2. **Happy path parse** — typical mix of boolean + enum flags parses to
   the expected map; exit 0.
3. **`--flag=value` form** — single-token equals syntax honoured for all
   types.
4. **Positional preservation** — bare tokens collected under
   `parsed._positional` in input order.
5. **Mutually exclusive violation** — two flags from one group ⇒ exit 2
   with deterministic offender ordering.
6. **Unknown flag** — undeclared `--typo` ⇒ exit 3 with the exact token.
7. **Enum invalid value** — value outside `values[]` ⇒ exit 4 with the
   allowed list in the message.
8. **Deprecation warning** — seen `deprecated` flag ⇒ `warnings[]` carries
   the schema string; exit remains 0.
9. **Invalid schema — missing `--`** — schema with bare flag name ⇒ exit 1.
10. **Invalid schema — enum without `values`** — enum flag missing the
    `values[]` array ⇒ exit 1.
11. **Integer coercion failure** — `--n=abc` against `type=integer` ⇒
    exit 1 with `TYPE_COERCION_FAILED`.
12. **Boolean coercion failure** — `--flag=maybe` against `type=boolean`
    ⇒ exit 1 with `TYPE_COERCION_FAILED`.

Goldens under
`src/test/resources/golden/internal/ops/x-internal-args-normalize/` lock
the SKILL.md rendering. Coverage requirement: ≥ 95% line / ≥ 90%
branch across the invoking Bash codepaths.
