# x-internal-story-load-context — Full Protocol

> Depth reference for `x-internal-story-load-context`. The SKILL.md
> body is the normative contract; this document expands the
> workflow internals that orchestrators do not need in their
> working context but that implementers and auditors must be able
> to consult.

## 1. Argument-Parser Rejection Matrix

The parser is a tight single-file loop (`while (($#)); case "$1" in ...`)
to keep the SKILL.md within the SkillSizeLinter 500-line threshold
without delegating to `x-internal-args-normalize` (that skill is a
peer, not a dependency — see Rule 14).

| Input | Result | Exit |
| :--- | :--- | :--- |
| `--story-id story-0049-0011 --epic-id 0049` | accepted | 0 |
| `--story-id story-0049-0011` (epic-id missing) | `usage: --epic-id is required` | 64 |
| `--epic-id 0049` (story-id missing) | `usage: --story-id is required` | 64 |
| `--story-id STORY-0049-0011 --epic-id 0049` | accepted (ID normalised to lowercase) | 0 |
| `--story-id story-0049-0011 --epic-id 49` | accepted (zero-padded to `0049`) | 0 |
| `--story-id story-49-11 --epic-id 49` | `usage: --story-id must match story-NNNN-NNNN` | 64 |
| `--unknown-flag` | `usage: unknown flag --unknown-flag` | 64 |
| `--story-id='' --epic-id=0049` | `usage: --story-id must not be empty` | 64 |
| `--help` | print banner + usage; exit 0 | 0 |

The banner is intentionally terse (< 20 lines) so sourcing the
skill from a parent script that accidentally passes `--help` does
not flood the caller's stdout.

## 2. Section-Extraction Regex Catalogue

The skill reads the story markdown exactly once (one `awk` pass)
and emits four streams: Section 1 body, Section 7 body, Section 8
body, and the `**Status:**` header. The regex fragments below are
the authoritative source for each stream.

| Stream | Start regex | End regex |
| :--- | :--- | :--- |
| Section 1 | `^## 1\\. (Dependências|Dependencies)\\s*$` | `^## 2\\. ` |
| Section 7 | `^## 7\\. (Critérios de Aceite \\(Gherkin\\)|Acceptance Criteria \\(Gherkin\\))\\s*$` | `^## 8\\. ` |
| Section 8 | `^## 8\\. (Tasks|Tarefas)\\s*$` | `^## 9\\. ` or EOF |
| Status header | `^\\*\\*Status:\\*\\*\\s*(.+)$` | n/a (single line) |

Task detection inside Section 8:

```regex
^### TASK-([0-9]{4})-([0-9]{4})-([0-9]{3}):
```

Scenario detection inside Section 7:

```regex
^(Cenario|Scenario):
```

Dependency-row detection inside Section 1: the table has exactly
two columns; the parser strips leading/trailing `|`, splits on the
remaining `|`, trims each cell, and yields `Blocked By` cells
whose value is not `—` or an empty string.

## 3. Artifact-Classification Edge Cases

| Scenario | Classification |
| :--- | :--- |
| File exists, mtime > story mtime | `fresh` |
| File exists, mtime == story mtime (exact same second) | `fresh` (≥ comparison) |
| File exists, mtime < story mtime by 1 s | `stale` |
| File absent | `missing` |
| Symlink whose target exists and is newer than story | `fresh` (follows the symlink; `stat` dereferences by default) |
| Symlink to missing target | `missing` (the `-f` test fails) |
| Permissions prohibit read of artifact directory | treat all 7 as `missing`; log a single warning; continue |
| Filesystem clock skew (artifact mtime > `now` + 1 h) | `fresh` (skill trusts the filesystem; no clock validation) |
| Artifact name variant (e.g., `map-story-*` instead of canonical `task-implementation-map-story-*`) | accept legacy name as the 7th slot when the canonical is missing |

The classifier does **not** inspect artifact content — mtime is
sufficient to drive the orchestrator's reuse decision per the
Phase 0 idempotency contract (RULE-002).

## 4. execution-state.json Schema Contract

The skill consumes only two paths inside the state file; any other
schema evolution is safe:

| jq path | Expected type | Used for |
| :--- | :--- | :--- |
| `.stories` | `object` | map of `story-XXXX-YYYY` → story node |
| `.stories[<id>].status` | `string` | dependency-status lookup (Step 3) |

When either path is missing, the skill falls back to the dep
story's own markdown `**Status:**` header. When the `stories`
object is present but does not contain the looked-up id, the
status is treated as `UNKNOWN` and reported via exit 2.

Supported status strings (case-insensitive, trimmed):

| Success synonyms | Pending synonyms |
| :--- | :--- |
| `DONE`, `MERGED`, `COMPLETE`, `Concluída`, `Concluida`, `Done`, `Merged` | everything else, including `PENDING`, `IN_PROGRESS`, `BLOCKED`, `FAILED`, `UNKNOWN` |

Unknown synonyms raise exit 2 — the caller is expected to declare
terminal states explicitly. Silent success on an unrecognised
synonym would hide lifecycle drift (Rule 22 precedent).

## 5. Concurrency Contract

- The skill opens `<state_file>.lock` with a shared lock
  (`flock -s`) so it can run in parallel with other read-only
  consumers (e.g., `x-status-reconcile` in diagnose mode) and
  serialises only behind the exclusive writer
  (`x-internal-status-update`).
- The shared lock is released on process exit via file-descriptor
  closure; no `trap`-based cleanup is required.
- The skill holds the shared lock for the duration of Step 3 only
  (the `jq` status lookup). Step 4 (artifact classification) does
  not touch the state file and runs lock-free.

## 6. Performance Profile

Measured on the `plans/epic-0049/` fixture (22 stories, 7 current
planning artifacts for the pilot story):

| Step | Median time | Dominated by |
| :--- | :--- | :--- |
| 1 | 4 ms | argument parsing |
| 2 | 12 ms | `awk` single-pass extraction |
| 3 | 28 ms | `jq` lookup + lock acquisition |
| 4 | 18 ms | 7× `stat` + arithmetic |
| 5 | 6 ms | `grep -c` on Section 8 |
| 6 | 1 ms | arithmetic |
| 7 | 22 ms | `jq -n` envelope assembly |
| **Total** | **~90 ms** | well under the 500 ms DoD budget |

The skill is CPU-bound on `jq`; the dominant factor once `jq`
responds is filesystem latency on the 7× `stat` calls. Reducing
those to a single `find ... -printf '%T@ %p\n'` was evaluated and
rejected — `find`'s output ordering is not portable across macOS
BSD / GNU userland.

## 7. Failure Envelope Examples

Envelope for exit 2 (DEPENDENCY_NOT_DONE):

```text
Blocker story-0049-0001 is PENDING
```

Envelope for exit 1 (STORY_NOT_FOUND):

```text
Story file not found: plans/epic-0049/story-0049-9999.md
```

Envelope for exit 3 (EPIC_NOT_FOUND):

```text
Epic dir not found: plans/epic-9999
```

Envelope for exit 64 (usage error):

```text
usage: x-internal-story-load-context --story-id <id> --epic-id <id>
```

No JSON is written to stdout on any non-zero exit — callers
distinguish success from failure by exit code, not by parsing
stdout.

## 8. Why Not Delegate Parsing to x-internal-args-normalize?

A direct delegation would save ~30 lines in this SKILL.md but
would introduce a circular-dependency hazard during the
EPIC-0049 rollout: the argv-normalize skill is itself a caller
of no other internal skill, whereas this skill lives in the
planning category and would import a runtime-ops sibling. The
asymmetry is fine today (internal skills never import each
other), but the guarantee is enforced by Rule 22: if a future
audit tightens the invariant to "internal skills are mutually
independent", the inline parser here keeps us compliant without
a breaking refactor.

The inline parser also documents the parameter contract at the
point of use, which is the convention the three prior pilot
skills adopted — staying consistent with
`x-internal-status-update`, `x-internal-report-write`, and
`x-internal-args-normalize` themselves (each defines its own
parser inline).
