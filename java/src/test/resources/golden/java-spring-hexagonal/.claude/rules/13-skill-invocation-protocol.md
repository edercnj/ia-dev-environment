# Rule 13 — Skill Invocation Protocol

> **Related:** Rule 07 defines the order of the pre-commit chain. This rule defines the *syntax* for skill-to-skill invocations across **any** chain, not just pre-commit.

## Scope

All skill templates under `skills/core/**/SKILL.md` and their `references/*.md` includes MUST follow one of the three **permitted delegation patterns** below when one skill invokes another. The bare-slash shorthand `/x-foo` — which is how users type commands in the Claude Code chat — is **forbidden** in delegation contexts because the LLM cannot reliably interpret it as a tool call and may silently fall through to inline execution.

## Permitted Patterns

### Pattern 1 — INLINE-SKILL (preferred for direct delegation)

Use when the orchestrator wants a synchronous call to another skill and will act on its return value directly.

```markdown
Invoke the `x-foo` skill via the Skill tool:

    Skill(skill: "x-foo", args: "--flag value --other thing")
```

**Requirement:** the calling skill's `allowed-tools` frontmatter MUST include `Skill`. Failure to do so causes the runtime to refuse the call even if the body syntax is correct.

### Pattern 2 — SUBAGENT-GENERAL (isolated general-purpose subagent)

Use when the orchestrator wants to isolate context — e.g., spawn a subagent for parallel work, to avoid polluting the current conversation window, or to launch multiple independent workers in one message. The subagent may:

- **(a) Invoke another skill** from its own context via `Skill(skill: "...", ...)`
- **(b) Produce an artifact** (read files, write an output file, return a structured summary) without calling any skill
- **(c) Perform complex analysis** and return a report

All three use the same call shape. The distinguishing factor is what goes INSIDE the `prompt:` argument.

**Required form:**

```markdown
Agent(
  subagent_type: "general-purpose",
  description: "<short 3-7 word summary of the subagent's job>",
  prompt: "<multi-line prompt: FIRST ACTION TaskCreate + task body + LAST ACTION TaskUpdate>"
)
```

**Example (a) — subagent invokes another skill:**

```markdown
Agent(
  subagent_type: "general-purpose",
  description: "Run x-foo for {args}",
  prompt: "FIRST ACTION: TaskCreate(...). Invoke x-foo via the Skill tool: Skill(skill: \"x-foo\", args: \"...\"). LAST ACTION: TaskUpdate(...)."
)
```

**Example (b) — subagent produces an artifact (no Skill call):**

```markdown
Agent(
  subagent_type: "general-purpose",
  description: "Plan implementation for story X",
  prompt: "FIRST ACTION: TaskCreate(...). You are a Senior Architect. Read context files. Produce implementation plan at plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md. LAST ACTION: TaskUpdate(...)."
)
```

**Requirement:** the parent's `allowed-tools` MUST include `Agent`. The subagent itself has access to all tools (Read, Write, Edit, Skill, TaskCreate, TaskUpdate, etc.) by default — the `general-purpose` type inherits the full toolset.

**Parallelism:** To launch multiple `general-purpose` subagents in parallel, emit all `Agent(...)` calls as SIBLING tool calls in the SAME assistant message. See `x-story-implement` Phases 1B-1F "Parallelism + tracking batching" section for the canonical parallel dispatch pattern (Batch A = all TaskCreate + Agent launches as siblings; Batch B = all TaskUpdate as siblings after results return).

### Pattern 3 — SUBAGENT-RESEARCH (no Skill call, pure exploration)

Use when the orchestrator needs investigation or research that does NOT require invoking another skill. The subagent reads, greps, and reports back in natural language.

```markdown
Delegate research to an explorer subagent:

    Agent(
      subagent_type: "Explore",
      description: "Find all references to pattern X",
      prompt: "Search the codebase for {pattern} and report file:line matches. Do not modify anything."
    )
```

The Explore subagent has read-only tools by design and cannot accidentally invoke another skill.

## Forbidden Pattern — Bare-Slash in Delegation Contexts

These forms are PROHIBITED in skill body text used for delegation:

```markdown
Invoke /x-foo with args
```

```markdown
/x-foo {STORY_ID}
/x-bar {STORY_ID}
```

```markdown
Each phase delegates to `/x-git-commit`: ...
```

They look like tool calls but are just text. The LLM must guess whether to literally execute them or read them as prose, and that ambiguity has caused silent delegation failures in this project. EPIC-0033 diagnosed **13 distinct logical routes (24 physical locations)** across 7 files where this pattern was in use; all were converted to one of the 3 permitted patterns above.

### Exception — User-Facing Sections

The bare-slash form IS permitted in sections addressed to the human user who types commands in chat:

- `## Triggers`
- `## Examples`
- Inline examples inside documentation tables, README files, and changelog entries

In these sections, `/x-foo` is legitimate because the user literally types it into the Claude Code input box. It is NOT a delegation instruction for the LLM.

## Audit Command

To verify compliance at any time:

```bash
grep -rnE "Invoke\s+\`?/?x-[a-z-]+" \
    java/src/main/resources/targets/claude/skills/core/ \
    --include=SKILL.md \
    --include="*.md" \
  | grep -v "## Triggers" \
  | grep -v "## Examples"
```

**Expected result:** 0 matches.

A secondary check for bullet-list slash calls (which `Invoke` does not catch):

```bash
grep -rnE "^\s*/x-[a-z-]+\s" \
    java/src/main/resources/targets/claude/skills/core/ \
  | grep -v "## Triggers" \
  | grep -v "## Examples"
```

**Expected result:** 0 matches.

## Rationale

Before EPIC-0033:

| Pattern (in `core/`) | Count |
|---|---|
| `Skill(skill: ...)` canonical | 3 files |
| `Invoke /x-foo` / bare-slash in delegation | 13 logical routes / 24 physical locations |

After EPIC-0033:

| Pattern (in `core/`) | Count |
|---|---|
| `Skill(skill: ...)` canonical | ≥ 13 |
| `Invoke /x-foo` / bare-slash in delegation | 0 |

Rule 13 was created as part of **STORY-0033-0001** (see `plans/epic-0033/`) to make the above invariant permanent. Any delegation added to a skill template after this epic MUST follow one of the 3 permitted patterns. CI audit (via the grep commands above) enforces the invariant going forward.

## Forbidden Additions

Do NOT add any of the following to a skill body:

- Markdown code blocks starting with `/x-foo TASK-...` in a delegation context
- Prose like "then run `/x-foo` to do X" — rewrite as "then invoke `x-foo` via the Skill tool: `Skill(skill: \"x-foo\", args: \"...\")`"
- Subagent prompts that tell the subagent to "run /x-foo" — rewrite as "invoke the `x-foo` skill via the Skill tool"
- Bullet lists of slash commands intended as parallel delegation — rewrite as a single block of parallel `Skill(...)` calls (Pattern 1, multiple calls in one message)

## Telemetry Markers (story-0040-0006)

Skills that participate in the implementation workflow (`x-epic-implement`, `x-story-implement`, `x-task-implement`, `x-task-plan`, and future siblings) MUST emit `phase.start` / `phase.end` telemetry markers around every numbered phase. Markers let operators analyse "which workflow phase is the bottleneck" without touching skill code — the signal is semantic, not just mechanical (passive hooks capture `tool.call` but not "which phase this call belongs to").

### When to Emit

| Situation | Emit? |
| :--- | :--- |
| Numbered phase of an implementation skill (Phase 1, Phase 2, ...) | Yes — one `phase.start` at entry + one `phase.end` at exit |
| TDD sub-phase of `x-task-implement` (Red / Green / Refactor) | Yes — one pair per sub-phase |
| Non-numbered prose sections (Integration Notes, Glossary) | No — captured passively by hooks |
| Skill with zero numbered phases | No markers (zero pairs is a valid state) |
| Phase aborts on error | Emit `phase.end` with `status=failed` |
| Phase skipped by a flag / condition | Emit `phase.end` with `status=skipped` |

### Canonical Shape

Each numbered phase receives one HTML comment + one Bash invocation at entry, and the mirrored pair at exit:

```markdown
## Phase N — <Phase Name>

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start <skill-name> Phase-N-<Name>`

... phase body ...

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end <skill-name> Phase-N-<Name> ok`
```

### Helper Contract (`telemetry-phase.sh`)

| Argument | Kind | Values |
| :--- | :--- | :--- |
| `$1` | required | `start` \| `end` \| `subagent-start` \| `subagent-end` |
| `$2` | required | skill identifier (kebab-case, e.g., `x-story-implement`) |
| `$3` | required | phase identifier for `start`/`end` (max 64 chars, e.g., `Phase-2-Implement`) OR role identifier for `subagent-start`/`subagent-end` (max 64 chars, e.g., `Architect`) |
| `$4` | required on `end` / `subagent-end` | `ok` \| `failed` \| `skipped` (defaults to `ok` if missing) |

Fail-open contract: invalid arguments, missing peer helpers, or `CLAUDE_TELEMETRY_DISABLED=1` cause the helper to log on stderr and exit 0 — skills never abort because telemetry broke.

### Subagent Markers (story-0040-0007)

Planning skills that dispatch parallel subagents (e.g., `x-story-plan` with its
5-agent Architect/QA/Security/TechLead/PO wave, or `x-epic-orchestrate` with
its per-story loop) MUST emit `subagent.start` / `subagent.end` markers around
each parallel dispatch so the `/x-telemetry-analyze` report can compute the
overlap window and flag slow agents that bottleneck the wave.

```markdown
<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-story-plan Architect`

... subagent dispatch ...

<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-story-plan Architect ok`
```

The role argument (position 3) is persisted under `metadata.role` of the
telemetry event. Degenerate planning skills (no parallel dispatch, e.g.,
`x-arch-plan`, `x-test-plan`, `x-epic-map`) MUST emit ZERO subagent markers —
this is validated by the `PlanningSmokeIT` acceptance test.

### CI Enforcement

`dev.iadev.ci.TelemetryMarkerLint` scans every SKILL.md for balance violations:

| Violation | Meaning |
| :--- | :--- |
| `DUPLICATE_START` | Two consecutive `phase.start` for the same `(skill, phase)` |
| `DUPLICATE_END` | Two consecutive `phase.end` for the same `(skill, phase)` |
| `DANGLING_END` | `phase.end` with no preceding `phase.start` |
| `UNCLOSED_START` | `phase.start` with no matching `phase.end` before EOF |

The linter runs as part of the CI build via the per-skill acceptance tests under `dev.iadev.skills.X*MarkersIT`. Any violation fails the build.

### Forbidden

- Emitting `phase.start` from inside a loop body — a loop iteration is NOT a phase; emit one pair around the loop, not one pair per iteration.
- Emitting a marker from a non-implementation skill (e.g., review, lint, format) — passive hooks already capture their activity.
- Hand-rolling the JSON payload instead of calling `telemetry-phase.sh` — bypasses the scrubber and fail-open contract.
- Using dotted case (`phase.1.plan`) instead of kebab case (`Phase-1-Plan`) in `$3` — consumers join on the exact string.
