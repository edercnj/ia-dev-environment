# x-internal-story-report — Full Protocol

> Depth reference for `x-internal-story-report`. The SKILL.md body is
> the normative contract; this document expands the workflow
> internals that orchestrators do not need in their working context
> but that implementers and auditors must be able to consult.

## 1. Argument-Parser Rejection Matrix

The parser is a tight single-file loop (`while (($#)); case "$1" in …`)
to stay inside the SkillSizeLinter 500-line threshold without
delegating to `x-internal-args-normalize` (that skill is a peer, not
a dependency — same rationale as `x-internal-story-resume` §1).

| Input | Result | Exit |
| :--- | :--- | :--- |
| `--story-id story-0049-0001 --epic-id 0049 --output out.md` | accepted | 0 |
| `--story-id story-0049-0001 --epic-id 0049` (output missing) | `usage: --output is required` | 64 |
| `--story-id story-0049-0001 --output out.md` (epic-id missing) | `usage: --epic-id is required` | 64 |
| `--epic-id 0049 --output out.md` (story-id missing) | `usage: --story-id is required` | 64 |
| `--story-id=STORY-0049-0001 --epic-id=0049 --output=out.md` | accepted (ID normalised to lowercase) | 0 |
| `--story-id story-0049-0001 --epic-id 49 --output out.md` | accepted (zero-padded to `0049`) | 0 |
| `--story-id story-49-1 --epic-id 49 --output out.md` | `usage: --story-id must match story-NNNN-NNNN` | 64 |
| `--story-id story-0049-0001 --epic-id 0049 --output ''` | `usage: --output must not be empty` | 64 |
| `--story-id story-0049-0001 --epic-id 0049 --output ../../etc/passwd` | `output path unsafe (traversal)` | 64 |
| `--unknown-flag` | `usage: unknown flag --unknown-flag` | 64 |
| `--help` | print banner + usage; exit 0 | 0 |

The banner is intentionally terse (< 20 lines) so sourcing the skill
from a parent script that accidentally passes `--help` does not flood
the caller's stdout.

## 2. execution-state.json Schema Contract

The skill consumes the following paths inside the state file; any
other schema evolution is safe:

| jq path | Expected type | Used for |
| :--- | :--- | :--- |
| `.stories` | `object` | map of `story-XXXX-YYYY` → story node |
| `.stories[<id>]` | `object` | presence check — exit 1 when missing |
| `.stories[<id>].tasks` | `object` (ordered map) | tasks classification + `tasks` array for the template |
| `.stories[<id>].tasks[<taskId>].status` | `string` | DONE-synonym bucket |
| `.stories[<id>].tasks[<taskId>].commitSha` | `string \| null` | `commitsCount` deduplication and per-task table |
| `.stories[<id>].pr.number` | `integer \| null` | `summary.prNumber` |
| `.stories[<id>].pr.state` | `string \| null` | `summary.prState` after `ascii_upcase` |
| `.stories[<id>].verification.coverage.line` | `number \| null` | `summary.coverageLine` |
| `.stories[<id>].verification.coverage.branch` | `number \| null` | `summary.coverageBranch` |
| `.stories[<id>].reviews.findings` | `array \| null` | `findings` array for the template |

Supported status strings (case-insensitive, trimmed):

| DONE synonyms | Everything else |
| :--- | :--- |
| `DONE`, `MERGED`, `COMPLETE`, `Concluída`, `Concluida`, `Done`, `Merged` | `PENDING`, `IN_PROGRESS`, `PR_CREATED`, `PR_APPROVED`, `PR_MERGED`, `FAILED`, `BLOCKED`, `UNKNOWN`, any unknown status |

Note: `PR_MERGED` intentionally falls outside the DONE bucket — it
is a transient state between PR merge and the
`x-internal-status-update` transition to `DONE`. `tasksDone` is a
*post-transition* metric, matching the contract used by
`x-internal-story-resume`.

## 3. Render-Payload Schema

The payload assembled in Step 4 of the SKILL workflow matches the
following schema (TypeScript-flavoured):

```ts
interface RenderPayload {
  storyId: string;          // "story-XXXX-YYYY"
  epicId: string;           // "XXXX" (zero-padded)
  tasksCount: number;       // integer
  tasksDone: number;        // integer (0 <= tasksDone <= tasksCount)
  commitsCount: number;     // integer
  prNumber: number | null;
  prState: "MERGED" | "OPEN" | "CLOSED" | null;
  coverageLine: number | null;    // percentage 0..100
  coverageBranch: number | null;  // percentage 0..100
  tasks: Array<{
    id: string;             // "TASK-XXXX-YYYY-NNN"
    status: string;         // raw state-file value
    commitSha: string | null;
  }>;
  findings: Array<{
    severity: string;       // CRITICAL | HIGH | MEDIUM | LOW | INFO
    title: string;
    file: string | null;
  }>;
}
```

The template at `.claude/templates/_TEMPLATE-STORY-COMPLETION-REPORT.md`
MUST reference only fields present in this schema; adding new fields
is backward-compatible (unused payload keys are silently ignored by
`x-internal-report-write`), but removing a field is a breaking
change and requires a template version bump.

## 4. PR-State Normalisation Rules

Raw state-file values for `.stories[<id>].pr.state` may come from
three sources: GitHub API (`MERGED` / `OPEN` / `CLOSED`), the
`gh pr view --json state` CLI (same set), or manual edits (lowercase
variants). The skill normalises via `ascii_upcase` before emitting
`summary.prState`:

| Raw value | Emitted |
| :--- | :--- |
| `"MERGED"` | `"MERGED"` |
| `"merged"` | `"MERGED"` |
| `"Open"` | `"OPEN"` |
| `"CLOSED"` | `"CLOSED"` |
| `null` | `null` |
| `""` | `null` (treated as absent) |
| `"DRAFT"` | `"DRAFT"` (forward-compatible: GitHub draft PR state) |

Any other value passes through `ascii_upcase` unchanged — the skill
does NOT reject unknown states. Downstream consumers branch on the
three canonical values and fall back to rendering the raw string.

## 5. Coverage Field Edge Cases

When `.stories[<id>].verification.coverage` is present but partially
populated (e.g., `.line` present and `.branch` null), the skill emits
each field independently. This is intentional: some build tools
(notably `cargo tarpaulin` at low verbosity) report only line
coverage, and the template must handle each field's null-ness
separately.

| State block | `coverageLine` | `coverageBranch` |
| :--- | :--- | :--- |
| `{line: 96.4, branch: 92.1}` | `96.4` | `92.1` |
| `{line: 96.4}` | `96.4` | `null` |
| `{branch: 92.1}` | `null` | `92.1` |
| `{}` | `null` | `null` |
| absent | `null` | `null` |

## 6. Template-Version Compatibility Policy

The skill pins a single template name
(`_TEMPLATE-STORY-COMPLETION-REPORT.md`) and delegates version
management to the template file itself via a leading
`<!-- template-version: X.Y -->` HTML comment. When future stories
evolve the payload schema:

1. Additive-only change (new field appended) — no version bump
   required; the skill emits the new field, old templates ignore
   it, new templates reference it via `{{newField}}`.
2. Rename or remove a field — bump to the next major version in the
   template comment and update downstream consumers. The skill does
   NOT read the version comment; auditing is manual.
3. Structural change to an array element (e.g., renaming
   `tasks[].id` to `tasks[].taskId`) — breaking. Follow the same
   contract-evolution playbook used by `x-internal-report-write`
   template changes (see EPIC-0049 S6 references).

## 7. Performance Profile (measured on laptop-class)

Typical medium story (5 tasks, ~300-byte state node, empty findings
array):

| Step | Measured | Budget |
| :--- | :--- | :--- |
| Argument parse | 8 ms | 50 ms |
| State read (`jq .stories["…"]`) | 35 ms | 100 ms |
| Summary computation (5× jq pipes) | 85 ms | 200 ms |
| Payload assembly (single `jq -n`) | 25 ms | 50 ms |
| Template existence gate | < 1 ms | 10 ms |
| `x-internal-report-write` invocation | 180 ms | 250 ms |
| Envelope emission | 3 ms | 10 ms |
| **Total** | **≈ 340 ms** | **500 ms** |

Large stories (20+ tasks, 100+ findings) remain under 800 ms because
the jq pipeline is O(n) in task count and the payload is built in a
single pass. The `x-internal-report-write` step dominates latency;
optimisations there (e.g., template compilation caching) flow
through to this skill automatically.

## 8. Testing Matrix (Acceptance Tests)

Story-0049-0015 Section 7 scenarios with their concrete test
fixtures:

| # | Scenario | Fixture state snippet | Expected envelope field | Exit |
| :--- | :--- | :--- | :--- | :--- |
| 1 | Happy path | 5 DONE tasks, merged PR, coverage 96/92 | `tasksCount=5, tasksDone=5, prState="MERGED"` | 0 |
| 2 | Story in progress | 3 DONE + 2 PENDING, no PR yet | `tasksCount=5, tasksDone=3, prNumber=null` | 0 |
| 3 | STATE_NOT_FOUND | — | no envelope; stderr `State missing:` | 1 |
| 4 | TEMPLATE_MISSING | state OK, template deleted | no envelope; stderr `Template missing:` | 2 |
| 5 | Boundary — story sem PR | state with `.pr` absent | `prNumber=null, prState=null` | 0 |

All fixtures are built in-memory by the smoke test to keep the
test suite hermetic. Goldens lock only the SKILL.md rendering (EPIC-0049
convention — see
`src/test/resources/golden/internal/plan/x-internal-story-report/`).

## 9. Consumer Contract (story-0049-0019)

The downstream orchestrator consumer in story-0049-0019
(`x-story-implement` Phase 3 simplification) reads the envelope as
follows:

```bash
envelope=$(Skill x-internal-story-report \
  --story-id "${story_id}" \
  --epic-id "${epic_id}" \
  --output "${report_path}")

report_path=$(echo "${envelope}" | jq -r '.reportPath')
tasks_done=$(echo "${envelope}" | jq '.summary.tasksDone')
pr_state=$(echo "${envelope}" | jq -r '.summary.prState // "NONE"')
cov_line=$(echo "${envelope}" | jq '.summary.coverageLine // 0')
```

The consumer logs a one-line status based on the tuple
`(tasksDone, prState, cov_line)`. No other field is read; additive
extensions to the envelope are safe as long as this tuple remains
stable.

## 10. Forward-Compatibility Policy

New `summary` fields MAY be appended to the envelope at any time
without a version bump. Existing consumers ignore unknown keys by
construction (they access fields by name via `jq`). To REMOVE or
RENAME a `summary` field, follow this playbook:

1. Add the replacement field in release N.
2. Emit BOTH the old and new fields for at least one release cycle.
3. Update every known consumer (grep the repository for the field
   name).
4. Deprecate the old field (warning in changelog).
5. Remove the old field in release N+2 at the earliest.

This mirrors the deprecation contract used by
`x-internal-report-write` and avoids lockstep upgrades across the
`x-internal-*` family.
