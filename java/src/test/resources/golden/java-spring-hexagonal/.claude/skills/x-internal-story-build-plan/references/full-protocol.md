# x-internal-story-build-plan — Full Protocol

> Depth reference for `x-internal-story-build-plan`. The SKILL.md
> body is the normative contract; this document expands the
> workflow internals that orchestrators do not need in their
> working context but that implementers and auditors must be able
> to consult.

## 1. Argument-Parser Rejection Matrix

The parser is a tight single-file loop (`while (($#)); case "$1" in …`)
to keep the SKILL.md within the SkillSizeLinter 500-line threshold
without delegating to `x-internal-args-normalize` (peer, not a
dependency — same policy as `x-internal-story-load-context` §1).

| Input | Result | Exit |
| :--- | :--- | :--- |
| `--story-id story-0049-0012 --epic-id 0049` | accepted (defaults: `scope=STANDARD`, `skip-review=false`) | 0 |
| `--story-id story-0049-0012 --epic-id 0049 --scope SIMPLE` | accepted | 0 |
| `--story-id story-0049-0012 --epic-id 0049 --scope simple` | accepted (uppercased) | 0 |
| `--story-id story-0049-0012 --epic-id 0049 --scope FANCY` | `usage: --scope must be SIMPLE / STANDARD / COMPLEX` | 64 |
| `--story-id STORY-0049-0012 --epic-id 0049` | accepted (lowercased) | 0 |
| `--story-id story-0049-0012 --epic-id 49` | accepted (zero-padded to `0049`) | 0 |
| `--story-id story-49-12 --epic-id 49` | `usage: --story-id must match story-NNNN-NNNN` | 64 |
| `--story-id story-0049-0012` (epic-id missing) | `usage: --epic-id is required` | 64 |
| `--epic-id 0049` (story-id missing) | `usage: --story-id is required` | 64 |
| `--skip-review` (flag form) | accepted (treated as `true`) | 0 |
| `--skip-review=false` | accepted | 0 |
| `--unknown-flag` | `usage: unknown flag --unknown-flag` | 64 |
| `--help` | print banner + usage; exit 0 | 0 |

The banner is intentionally terse (< 20 lines) so sourcing the
skill from a parent script that accidentally passes `--help` does
not flood the caller's stdout.

## 2. Scope-Gate Decision Table

The `--scope` tier is the ONLY gate that changes the set of
dispatched subagents. Downstream behaviours (collision matrix,
approval gates, smoke thresholds) are the caller's concern and are
driven by the tier echoed in the response envelope, not re-derived
inside this skill.

| Tier | Steps 1B-1D dispatched | Step 1E (security) | Step 1F (compliance) | `skipped` field |
| :--- | :--- | :--- | :--- | :--- |
| `SIMPLE` | yes (3 siblings) | SKIP | SKIP | `["1E","1F"]` |
| `STANDARD` | yes (5 siblings) | yes | yes | `[]` |
| `COMPLEX` | yes (5 siblings) | yes | yes | `[]` |

The SIMPLE tier dispatches exactly 3 sibling agents in a single
assistant message (1B, 1C, 1D); STANDARD and COMPLEX dispatch all
5 (1B, 1C, 1D, 1E, 1F). The Rule-13 Pattern 2 contract requires the
dispatch to be a single assistant turn — partitioning SIMPLE into a
separate sub-message would serialise the remaining 3 and negate the
parallelism benefit.

When the caller is `x-story-implement` and the scope tier comes from
`x-internal-story-load-context` (Step 5 — task-count heuristic), the
contract is that SIMPLE = `taskCount ≤ 4`, STANDARD = `5-7`,
COMPLEX = `≥ 8`. The gate inside this skill trusts the tier it
receives and does not re-classify.

## 3. Subagent Prompt Catalogue

Every Agent invocation follows the `FIRST ACTION / body / LAST ACTION`
triad from Rule 13 Pattern 2 (SUBAGENT-GENERAL). The skills harness
at implementation time may not provide TaskCreate/TaskUpdate tools;
in that case the FIRST/LAST action slots degrade to a no-op comment
(the agent still emits the artifact path in its final turn).

### Step 1B — Senior Architect (implementation plan)

```
description: "Implementation plan for <story_id>"
prompt: |
  You are a Senior Architect. Read:
    - plans/epic-<epic_id>/<story_id>.md (story)
    - plans/epic-<epic_id>/plans/arch-story-<story_id>.md (arch plan, produced by Step 1A)
    - .claude/templates/_TEMPLATE-IMPLEMENTATION-PLAN.md (template)
    - .claude/rules/04-architecture-summary.md
  Produce the implementation plan at
    plans/epic-<epic_id>/plans/plan-story-<story_id>.md
  The plan MUST cover: (a) layer-by-layer breakdown (domain/application/
  adapter), (b) ports + contracts, (c) integration notes, (d) risks and
  mitigations, (e) a rollback plan.
  [If --skip-review: omit the inline peer-review section at the end.]
  Return the absolute path of the file you wrote.
```

### Step 1C — QA Engineer (test plan) — delegates to `x-test-plan`

```
description: "Test plan for <story_id>"
prompt: |
  You are a QA Engineer. Invoke x-test-plan via the Skill tool:
    Skill(skill: "x-test-plan", args: "--story-id <story_id> --epic-id <epic_id>")
  The called skill writes
    plans/epic-<epic_id>/plans/tests-story-<story_id>.md
  directly. Verify the file exists after the call; return its
  absolute path.
```

### Step 1D — Task Decomposer — delegates to `x-lib-task-decomposer`

```
description: "Task breakdown + map for <story_id>"
prompt: |
  You are a Task Decomposer. Invoke x-lib-task-decomposer via the
  Skill tool:
    Skill(skill: "x-lib-task-decomposer", args: "--story-id <story_id> --epic-id <epic_id>")
  The called skill writes TWO artifacts:
    plans/epic-<epic_id>/plans/tasks-story-<story_id>.md
    plans/epic-<epic_id>/plans/task-implementation-map-story-<story_id>.md
  Verify both exist; return both absolute paths as a JSON object
  {"tasks":"…","taskMap":"…"}.
```

### Step 1E — Security Engineer — delegates to `x-threat-model`

```
description: "Security assessment for <story_id>"
prompt: |
  You are a Security Engineer. Invoke x-threat-model via the Skill
  tool:
    Skill(skill: "x-threat-model", args: "--story-id <story_id> --epic-id <epic_id>")
  The called skill writes
    plans/epic-<epic_id>/plans/security-story-<story_id>.md
  directly. Verify the file exists after the call; return its
  absolute path.
```

### Step 1F — Compliance Engineer (compliance assessment, inline)

```
description: "Compliance assessment for <story_id>"
prompt: |
  You are a Compliance Engineer. Read:
    - plans/epic-<epic_id>/<story_id>.md (story)
    - plans/epic-<epic_id>/plans/arch-story-<story_id>.md
    - .claude/templates/_TEMPLATE-COMPLIANCE-ASSESSMENT.md
    - .claude/rules/12-security-anti-patterns.md
  Produce the compliance assessment at
    plans/epic-<epic_id>/plans/compliance-story-<story_id>.md
  covering: (a) data-classification, (b) retention policy, (c)
  regulatory scope (GDPR / LGPD / PCI-DSS as applicable), (d)
  audit-trail requirements, (e) rights-enforcement hooks.
  Return the absolute path of the file you wrote.
```

Prompt lengths are deliberately conservative (< 30 lines each) so
the 5 prompts fit comfortably in a single assistant turn alongside
the 5 `Agent(…)` tool calls.

## 4. Envelope-Assembly Edge Cases

### 4.1 `null` fields in SIMPLE

The response envelope MUST serialise `securityAssessment` and
`complianceAssessment` as JSON `null`, NOT as the string `"null"`
and NOT by omitting the keys. The `jq -nc` snippet in SKILL.md §
Workflow Step 2 uses an if-empty-then-null ternary to achieve this
deterministically.

### 4.2 `task-implementation-map` basename drift

Legacy stories (pre-EPIC-0038) used `map-story-XXXX-YYYY.md`
instead of the canonical `task-implementation-map-story-XXXX-YYYY.md`.
Step 1D's subagent writes the canonical name; if the called
`x-lib-task-decomposer` produces the legacy form, this skill
renames it before emitting the envelope. The rename is a safety
net, not a silent migration path — it logs a single WARNING line to
stderr: `legacy map name detected: renamed <old> → <new>`.

### 4.3 Concurrent re-invocation

Two callers invoking this skill for the same story at the same time
would step on each other's planning artifacts (both `jq -nc` would
run against partially-written files). The caller
(`x-story-implement` Phase 1) serialises invocations per-story via
the `feat/story-<id>-<slug>` branch check (one PR per story at any
time). This skill does NOT acquire its own lock — concurrent
invocation is a misuse and is out of scope.

### 4.4 Missing `--scope` argument

When `--scope` is not supplied, the default is `STANDARD`. The
envelope MUST still include `scope:"STANDARD"` and `skipped:[]`
explicitly so downstream consumers never have to distinguish
"unset" from "set to STANDARD".

## 5. Interaction with `x-internal-story-load-context`

This skill is the WRITE counterpart to the READ-only
`x-internal-story-load-context`. The canonical Phase 1 driver in
`x-story-implement` (story-0049-0019) looks like:

```bash
envelope=$(Skill x-internal-story-load-context …) || exit $?
planning_mode=$(echo "$envelope" | jq -r '.planningMode')
scope=$(echo "$envelope" | jq -r '.scope')

if [[ "$planning_mode" == "PRE_PLANNED" ]]; then
  # All 7 artifacts fresh — skip Phase 1 entirely.
  echo "$envelope" | jq '{archPlan: .artifacts.fresh[0], …}'
  exit 0
fi

# HYBRID or INLINE — regen missing / stale artifacts.
Skill x-internal-story-build-plan --story-id "$id" --epic-id "$epic" \
                                  --scope "$scope" || exit $?
```

The contract: the LOAD skill decides whether Phase 1 is needed; the
BUILD skill performs it when needed. Neither skill is aware of the
other at the tool level — the caller is the only coupling point.

## 6. Fail-Fast and Partial-Artifact Policy

The skill is fail-fast: the FIRST non-zero exit from any delegate
(`x-arch-plan` in Step 1A, or any of the 5 subagents in Steps 1B-1F)
terminates the run with the matching exit code (1 or 2).

Artifacts produced by successful steps before the failure remain on
disk. The caller MAY re-invoke with the same arguments — each
delegate is idempotent with respect to its artifact path
(`x-arch-plan` overwrites `arch-story-*.md`; `x-test-plan`
overwrites `tests-story-*.md`; etc.).

Rationale: rolling back partial artifacts on failure would require a
two-phase-commit protocol across 6 heterogeneous delegates. The
idempotent-overwrite contract is cheaper to implement and more
forgiving to the operator (who can inspect the partial output
before re-running).

## 7. Performance Budget

Wall-clock targets (measured on a 2024-era M-series workstation):

| Scope | Step 1A | Steps 1B-1F (parallel) | Total |
| :--- | :--- | :--- | :--- |
| SIMPLE | 30-60 s | 90-180 s (3 siblings) | ~2-4 min |
| STANDARD | 30-60 s | 120-240 s (5 siblings) | ~3-5 min |
| COMPLEX | 30-60 s | 150-300 s (5 siblings, larger artifacts) | ~4-6 min |

The 5-sibling dispatch gains ~2x over a sequential run (the wave
finishes with the slowest agent; sequential would sum all 5). The
gain shrinks for stories where one agent is ≫ slower than the
others (the wave degenerates to that agent's runtime).

## 8. Audit Surface

Acceptance tests that lock behaviour for this skill live under
`java/src/test/java/dev/iadev/skills/internal/plan/`. The
recommended shape (see `StoryBuildPlanSmokeTest` from
TASK-0049-0012-005) asserts:

1. SKILL.md exists at the expected path.
2. Frontmatter includes `visibility: internal`, `user-invocable: false`,
   `allowed-tools: Bash, Skill, Agent`.
3. The `> 🔒 **INTERNAL SKILL**` marker is present as the first
   non-frontmatter line block.
4. The file is ≤ 500 lines (SkillSizeLinter ERROR threshold).
5. `references/full-protocol.md` exists and is non-empty.

Goldens (if added) lock the SKILL.md rendering under
`src/test/resources/golden/internal/plan/x-internal-story-build-plan/`.

## 9. Changelog

| Version | Change | Story |
| :--- | :--- | :--- |
| 1.0 | Initial carve-out — replaces ~250 inline lines in `x-story-implement` Phase 1 | story-0049-0012 |
