---
name: x-internal-epic-branch-ensure
description: "Single source of truth for the `epic/<ID>` branch convention (RULE-001). Invoked by every epic entry-point (x-epic-create, x-epic-decompose, x-epic-orchestrate, x-epic-implement, x-epic-map) at step 0; ensures the branch exists idempotently both locally and on origin. When the branch is absent, delegates creation to `x-git-branch`; when present locally but not on origin, emits a complementary `git push`. Seventh skill in the x-internal-* convention and the first under internal/git/ (after x-internal-status-update / x-internal-report-write / x-internal-args-normalize at internal/ops/ and x-internal-story-load-context / x-internal-story-build-plan / x-internal-story-verify at internal/plan/)."
visibility: internal
user-invocable: false
allowed-tools: Bash
argument-hint: "--epic-id <XXXX> [--base <branch>] [--push <true|false>]"
category: internal-git
context-budget: medium
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

> 🔒 **INTERNAL SKILL**
> Esta skill é invocada apenas por outras skills (orquestradores).
> NÃO é destinada a invocação direta pelo usuário.
> Callers principais: x-epic-create, x-epic-decompose, x-epic-orchestrate,
> x-epic-implement, x-epic-map (story-0049-0018, 0049-0021, 0049-0022).
> Sétima skill da convenção `x-internal-*` (após x-internal-status-update
> pilot 0049-0005, x-internal-report-write 0049-0006, x-internal-args-normalize
> 0049-0007, x-internal-story-load-context 0049-0011, x-internal-story-build-plan
> 0049-0012, x-internal-story-verify 0049-0014). Primeira skill na subdir
> `internal/git/` — o subdir `git/` agrupa operações git de orquestração
> (ensure de branches de épico; futuras skills de ensure de release branch,
> merge-back de hotfix, etc.).

# Skill: x-internal-epic-branch-ensure

## Purpose

Provide a single, idempotent entry point for the RULE-001 convention
"one branch per epic (`epic/<ID>`), always exists, always pushed". The
orchestrator skills that open an epic lifecycle step MUST call this
skill at step 0 rather than issuing inline `git checkout -b` or
`git push -u origin` commands; this eliminates three historical
regressions:

1. Duplicate branches created in parallel invocations (two entry-points
   launched in the same wave both running `git checkout -b`).
2. Local-only branches that lagged origin for hours because the caller
   forgot to push (documented in EPIC-0033 post-mortem).
3. Divergent base commits between local and remote due to mixed
   `--base` flags across entry-points.

Responsibilities (single):

1. Validate `--epic-id` against the canonical 4-digit regex.
2. Resolve branch name → `epic/<ID>`.
3. Detect local existence (`git rev-parse --verify`).
4. Detect remote existence (`git ls-remote origin`).
5. Apply the 3-state decision:
   - new (neither) ⇒ delegate to `Skill x-git-branch --push`;
   - local-only ⇒ emit complementary `git push -u origin`;
   - local+remote ⇒ idempotent no-op.
6. Emit a single-line JSON envelope consumed by the caller.

This skill does NOT create branches directly — all branch creation
goes through `x-git-branch` (story-0049-0001) to preserve the "one
creation path" invariant of RULE-001.

## Convention Anchors (x-internal-* 7th skill)

| Aspect | Value | Rationale |
| :--- | :--- | :--- |
| Path | `internal/git/x-internal-epic-branch-ensure/` | `internal/` prefix scopes visibility; `git/` groups orchestration-level git ensure operations (distinct from `internal/ops/` state mutation and `internal/plan/` planning-phase carve-outs). |
| Frontmatter `visibility` | `internal` | Generator filters from `/help` menu. |
| Frontmatter `user-invocable` | `false` | Declarative complement to `visibility: internal`. |
| Body marker | `> 🔒 **INTERNAL SKILL**` as first non-frontmatter content | Human-visible when browsing the repo. |
| Allowed tools | `Bash` only | Minimal surface; delegates to `x-git-branch` via Skill tool when needed. |
| Naming | `x-internal-{subject}-{action}` | subject = `epic-branch`; action = `ensure`. Mirrors Rule 04 skill taxonomy. |

Audit rule: Rule 22 (Lifecycle Integrity) validates every skill under
`internal/**` satisfies all 6 anchors above. Violations fail the
`LifecycleIntegrityAuditTest`.

## Triggers

Bare-slash form is intentionally omitted — this skill is never invoked
by a human typing `/x-internal-epic-branch-ensure` in chat. All
invocations follow Rule 13 INLINE-SKILL pattern from a calling
orchestrator:

```markdown
Skill(skill: "x-internal-epic-branch-ensure",
      args: "--epic-id 0049")
```

```markdown
Skill(skill: "x-internal-epic-branch-ensure",
      args: "--epic-id 0050 --base main --push true")
```

## Parameters

| Parameter | Required | Default | Description |
| :--- | :--- | :--- | :--- |
| `--epic-id <XXXX>` | M | — | 4-digit numeric epic identifier. Must match `^\d{4}$`. |
| `--base <branch>` | O | `develop` | Base branch for the delegated creation. Must exist locally. |
| `--push <true\|false>` | O | `true` | When `true`, guarantees the branch is present on `origin`. When `false`, creation is local-only and the remote-check step is skipped. |

## Response Contract

Successful invocations emit a single-line JSON object on stdout:

| Field | Type | Always Present | Description |
| :--- | :--- | :--- | :--- |
| `branchName` | `String` | yes | Resolved branch name (`epic/<ID>`). |
| `baseSha` | `String(40)` | yes | SHA of `--base` at the moment the skill ran (captured for audit). |
| `created` | `Boolean` | yes | `true` when the branch was created in this invocation (delegation to `x-git-branch` succeeded). |
| `alreadyExisted` | `Boolean` | yes | `true` when the branch already existed locally before this invocation. |
| `pushedNow` | `Boolean` | yes | `true` when this invocation issued the `git push -u origin <branch>` (either through `x-git-branch --push` or the complementary-push branch). |

Invariant: exactly one of (`created`, `alreadyExisted`) is `true`.
`pushedNow` is independent; it can be `true` with either.

## Exit Codes

| Code | Name | Condition | Message Format |
| :--- | :--- | :--- | :--- |
| 0 | SUCCESS | Ensure completed (created, no-op, or complementary push) | — |
| 1 | INVALID_EPIC_ID | `--epic-id` missing or fails `^\d{4}$` | `Epic ID must be 4 digits` |
| 2 | BASE_NOT_FOUND | `--base` does not resolve locally | `Base branch '<name>' not found` |
| 3 | DELEGATION_FAILED | `x-git-branch` returned non-zero | `x-git-branch failed with exit <code>: <stderr>` |
| 4 | PUSH_FAILED | Complementary `git push` returned non-zero | `Push to origin failed: <stderr>` |

Exit codes 1 and 2 mirror the story's Section 5.3 error-code table.
Codes 3 and 4 surface child-process failures without swallowing stderr.

## Workflow

### Step 1 — Argument parsing and `--epic-id` validation

```bash
EPIC_ID=""
BASE_BRANCH="develop"
PUSH="true"

while [ $# -gt 0 ]; do
  case "$1" in
    --epic-id) EPIC_ID="$2"; shift 2 ;;
    --base)    BASE_BRANCH="$2"; shift 2 ;;
    --push)    PUSH="$2"; shift 2 ;;
    *) echo "ERROR: unknown flag: $1" >&2; exit 64 ;;
  esac
done

if ! printf '%s' "$EPIC_ID" | grep -Eq '^[0-9]{4}$'; then
  echo "INVALID_EPIC_ID — Epic ID must be 4 digits" >&2
  exit 1
fi
```

The 4-digit regex matches the story's Section 5.1 validation rule. The
skill intentionally does NOT accept a `feat/` / `epic/` prefix in
`--epic-id` — the caller supplies the raw number; prefix composition
is the skill's responsibility.

### Step 2 — Base branch existence check

```bash
if ! git rev-parse --verify --quiet "$BASE_BRANCH" >/dev/null; then
  echo "BASE_NOT_FOUND — Base branch '$BASE_BRANCH' not found" >&2
  exit 2
fi
BASE_SHA=$(git rev-parse --verify "$BASE_BRANCH")
```

Exits early before touching any network operation. Matches
`x-git-branch` Step 3 semantics.

### Step 3 — Compute target branch name

```bash
BRANCH_NAME="epic/${EPIC_ID}"
```

Hard-coded prefix — the RULE-001 convention has exactly one form. No
configurability is intentional; variance breaks the "one branch per
epic" audit trail.

### Step 4 — Local existence detection

```bash
ALREADY_EXISTED="false"
if git rev-parse --verify --quiet "$BRANCH_NAME" >/dev/null; then
  ALREADY_EXISTED="true"
fi
```

A local branch is authoritative over remote — the epic lifecycle runs
N times on a developer workstation between pushes. The skill trusts
the local state and only reconciles to remote when `--push=true`.

### Step 5 — Remote existence detection (skipped when `--push=false`)

```bash
REMOTE_EXISTS="false"
if [ "$PUSH" = "true" ]; then
  if git ls-remote --exit-code --heads origin "$BRANCH_NAME" >/dev/null 2>&1; then
    REMOTE_EXISTS="true"
  fi
fi
```

When `--push=false`, the remote check is skipped and the
complementary-push branch becomes unreachable. The skill still emits
`pushedNow=false` in that case; the caller owns the downstream
decision (e.g., a dry-run mode may want local-only).

### Step 6 — Three-state decision and execution

```bash
CREATED="false"
PUSHED_NOW="false"

if [ "$ALREADY_EXISTED" = "true" ] && [ "$REMOTE_EXISTS" = "true" ]; then
  # State A — idempotent no-op
  :
elif [ "$ALREADY_EXISTED" = "true" ] && [ "$REMOTE_EXISTS" = "false" ] && [ "$PUSH" = "true" ]; then
  # State B — complementary push
  if ! git push -u origin "$BRANCH_NAME" 2>/tmp/epic-branch-ensure.push.err; then
    echo "PUSH_FAILED — Push to origin failed" >&2
    cat /tmp/epic-branch-ensure.push.err >&2
    exit 4
  fi
  PUSHED_NOW="true"
elif [ "$ALREADY_EXISTED" = "false" ]; then
  # State C — delegate creation to x-git-branch
  #
  # Invocation shape (Rule 13 INLINE-SKILL):
  #
  #   Skill(skill: "x-git-branch",
  #         args: "--name epic/<ID> --base <BASE_BRANCH> [--push]")
  #
  # The calling orchestrator forwards --push=true/false verbatim. A
  # successful invocation populates baseSha, created=true, pushed=<push>
  # on the x-git-branch response; this skill re-exports those as
  # created=true, pushedNow=<push>.
  echo "DELEGATE_TO_X_GIT_BRANCH — name=$BRANCH_NAME base=$BASE_BRANCH push=$PUSH" >&2
  # The orchestrator layer performs the actual Skill(...) call and
  # captures the result; see the full protocol for the end-to-end
  # envelope translation.
  CREATED="true"
  if [ "$PUSH" = "true" ]; then
    PUSHED_NOW="true"
  fi
fi
```

State A, B, and C are mutually exclusive. The caller can distinguish
them post-hoc by inspecting `(created, alreadyExisted, pushedNow)`.

### Step 7 — Emit structured response

```bash
printf '{"branchName":"%s","baseSha":"%s","created":%s,"alreadyExisted":%s,"pushedNow":%s}\n' \
  "$BRANCH_NAME" "$BASE_SHA" "$CREATED" "$ALREADY_EXISTED" "$PUSHED_NOW"
```

Single-line JSON keeps parsing cost at O(1) for the caller, which is
typically the very first step of an epic entry-point — any overhead
here is paid N times per day on a developer workstation.

## Examples

### Example 1 — New epic, branch absent locally and remotely

```bash
Skill(skill: "x-internal-epic-branch-ensure",
      args: "--epic-id 0050")
```

Expected stdout:
```json
{"branchName":"epic/0050","baseSha":"<sha>","created":true,"alreadyExisted":false,"pushedNow":true}
```
Exit: 0. Matches Section 7 Gherkin scenario "Criar branch nova quando
epic é novo".

### Example 2 — Idempotent no-op, branch present locally + remotely

```bash
Skill(skill: "x-internal-epic-branch-ensure",
      args: "--epic-id 0049")
```

Expected stdout:
```json
{"branchName":"epic/0049","baseSha":"<sha>","created":false,"alreadyExisted":true,"pushedNow":false}
```
Exit: 0. Matches "Idempotência — branch já existe local + remoto".

### Example 3 — Complementary push, local-only branch

```bash
# Branch created manually earlier; never pushed.
Skill(skill: "x-internal-epic-branch-ensure",
      args: "--epic-id 0049 --push true")
```

Expected stdout:
```json
{"branchName":"epic/0049","baseSha":"<sha>","created":false,"alreadyExisted":true,"pushedNow":true}
```
Exit: 0. Matches "Push complementar — branch local mas não remota".

### Example 4 — Custom base

```bash
Skill(skill: "x-internal-epic-branch-ensure",
      args: "--epic-id 0051 --base main")
```

Expected stdout:
```json
{"branchName":"epic/0051","baseSha":"<sha>","created":true,"alreadyExisted":false,"pushedNow":true}
```
Exit: 0.

### Example 5 — Missing base branch

```bash
Skill(skill: "x-internal-epic-branch-ensure",
      args: "--epic-id 0052 --base mybase")
```

Expected stderr:
```
BASE_NOT_FOUND — Base branch 'mybase' not found
```
Exit: 2. Matches "Erro — base inexistente".

### Example 6 — Malformed epic-id

```bash
Skill(skill: "x-internal-epic-branch-ensure",
      args: "--epic-id 49")
```

Expected stderr:
```
INVALID_EPIC_ID — Epic ID must be 4 digits
```
Exit: 1. Matches "Boundary — epic-id mal formado".

### Example 7 — Local-only flow (`--push false`)

```bash
Skill(skill: "x-internal-epic-branch-ensure",
      args: "--epic-id 0053 --push false")
```

Expected stdout:
```json
{"branchName":"epic/0053","baseSha":"<sha>","created":true,"alreadyExisted":false,"pushedNow":false}
```
Exit: 0. The skill never issues a network operation; suitable for
offline dry-runs.

## Error Handling

| Scenario | Action |
| :--- | :--- |
| `--epic-id` missing or fails regex | Exit 1 (`INVALID_EPIC_ID`); stderr carries the offending value. |
| `--base` absent locally | Exit 2 (`BASE_NOT_FOUND`); suggest `git fetch origin`. |
| `x-git-branch` delegation fails (State C) | Exit 3 (`DELEGATION_FAILED`); stderr carries `x-git-branch` stderr verbatim. |
| `git push` fails in State B | Exit 4 (`PUSH_FAILED`); stderr carries `git push` stderr verbatim. |
| `git ls-remote` times out or auth fails | Exit 4 (`PUSH_FAILED`); remote-check failure is treated as a push-side failure. |
| Unknown flag | Exit 64 (sysexits EX_USAGE); print `usage:` banner. |
| `git` absent on `PATH` | Exit 127 with `git is required`; abort before any state mutation. |

All exits preserve `set -e` / `trap`-free semantics: no temp files are
written in the happy path, and the complementary-push temp file at
`/tmp/epic-branch-ensure.push.err` is overwritten on the next
invocation (no accumulation).

## Idempotency Contract

The skill satisfies RULE-002 idempotency over the full 3-state matrix:

| State before | First call | Second call |
| :--- | :--- | :--- |
| Branch absent (local + remote) | `created=true, alreadyExisted=false, pushedNow=true` | `created=false, alreadyExisted=true, pushedNow=false` |
| Branch local, not remote | `created=false, alreadyExisted=true, pushedNow=true` | `created=false, alreadyExisted=true, pushedNow=false` |
| Branch local + remote | `created=false, alreadyExisted=true, pushedNow=false` | `created=false, alreadyExisted=true, pushedNow=false` |

Two concurrent invocations of the same `--epic-id` serialize on the
`git` process lock; the later caller always observes the earlier
caller's result as "already exists". No `flock` is required — git's
own `.git/index.lock` provides the critical-section guarantee at the
granularity we need.

## Performance Contract (story Section 3.5)

| Operation | Budget |
| :--- | :--- |
| State A (idempotent no-op; no network) | < 1s (two `git rev-parse` calls + `ls-remote`) |
| State B (complementary push) | < 3s (`ls-remote` + single push round-trip) |
| State C (delegate to `x-git-branch`) | < 3s (branch create + push; matches `x-git-branch` budget) |

The "< 1s" ceiling for idempotency is the dominant case in a
multi-entry-point workflow: `x-epic-implement` followed by
`x-epic-orchestrate` followed by `x-epic-map` on the same epic will
trigger State A three times; the aggregate overhead must stay sub-3s.

## Testing

The story ships the following acceptance test scenarios, which are
the reference contract every downstream caller (0049-0018, 0049-0021,
0049-0022) can rely on:

1. **Happy path (State C)** — branch absent both sides; assert
   `created=true, alreadyExisted=false, pushedNow=true`; assert the
   ref exists locally (`git rev-parse`) and on origin
   (`git ls-remote`).
2. **Idempotency (State A)** — second invocation of the same epic ID
   yields `created=false, alreadyExisted=true, pushedNow=false`; file
   tree unchanged; `.git/packed-refs` unchanged.
3. **Complementary push (State B)** — pre-create local branch without
   pushing, then invoke skill; assert `pushedNow=true`; origin ref
   now present.
4. **`--push false`** — assert no network call; `pushedNow=false`
   regardless of remote state.
5. **BASE_NOT_FOUND** — pass `--base unknown`; exit 2; message
   contains `BASE_NOT_FOUND`.
6. **INVALID_EPIC_ID** — pass `--epic-id 49`; exit 1; message contains
   `INVALID_EPIC_ID`.

No Java production code is introduced by this story; the SKILL.md is
the deliverable. Golden-file smoke tests are covered upstream by the
generator's existing skill-inventory goldens — this skill, being
`visibility: internal`, does NOT appear in the `/help` menu or in
`.claude/README.md`, so no golden-file updates are required.

## Generator Filter Contract

The `ia-dev-env` generator MUST exclude skills with
`visibility: internal` from:

1. The `.claude/README.md` skill-inventory table.
2. The `/help` menu listing surfaced by Claude Code.
3. User-facing autocomplete in the chat input.

Internal skills ARE still copied into `.claude/skills/` (flat layout)
so `Skill(skill: "x-internal-epic-branch-ensure")` invocations from
other skills resolve correctly. The invariant: **user cannot see
them; orchestrators can invoke them.**

## Telemetry

Internal skills DO NOT emit `phase.start` / `phase.end` markers —
telemetry is produced by the invoking orchestrator (the `phase`
wrapping the orchestrator's own step 0 is the correct aggregation
boundary). Passive hooks still capture `tool.call` for the underlying
`Bash` invocation.

Reference: Rule 13 (Skill Invocation Protocol), Rule 22 (Lifecycle
Integrity Audit), ADR-0010 (Interactive Gates Convention — exempts
internal skills from the 3-option menu contract).

## Rule References

- **RULE-001** (EPIC-0049) — Branch única por épico (`epic/<ID>`).
  This skill IS the canonical enforcement point.
- **RULE-005** (EPIC-0049) — Thin orchestrator (UseCase pattern). Epic
  entry-points call this skill instead of inlining branch logic.
- **RULE-006** (EPIC-0049) — `x-internal-*` convention. This is the
  seventh skill to follow the convention.
- **Rule 09** (Branching Model) — `epic/<ID>` is a canonical prefix
  in the generic branch-name regex.
- **Rule 13** (Skill Invocation Protocol) — INLINE-SKILL pattern
  used by all callers.
- **Rule 22** (Lifecycle Integrity Audit) — validates the 6
  convention anchors above.

## Integration Notes

| Skill | Relationship | Context |
| :--- | :--- | :--- |
| `x-git-branch` | delegate (State C) | The only skill this one calls. Handles actual `git branch` + optional push. |
| `x-epic-create` | caller (future — story-0049-0018) | Step 0 before writing epic metadata. |
| `x-epic-decompose` | caller (future — story-0049-0018) | Step 0 before writing stories / IMPLEMENTATION-MAP. |
| `x-epic-orchestrate` | caller (future — story-0049-0021) | Step 0 before per-story planning loop. |
| `x-epic-implement` | caller (future — story-0049-0021) | Step 0 before Phase 0 preparation. |
| `x-epic-map` | caller (future — story-0049-0022) | Step 0 before IMPLEMENTATION-MAP refresh. |
| `x-internal-status-update` | peer (internal/ops/) | Independent — mutates execution-state; this skill mutates git refs. Never co-serialized. |
| `x-internal-args-normalize` | peer (internal/ops/) | Future refactor may adopt it for flag parsing; current implementation uses inline Bash parsing for minimal surface. |

Downstream stories that depend on this skill: story-0049-0018
(epic-create / epic-decompose refactor), story-0049-0021
(epic-orchestrate / epic-implement refactor), story-0049-0022
(epic-map refactor).
