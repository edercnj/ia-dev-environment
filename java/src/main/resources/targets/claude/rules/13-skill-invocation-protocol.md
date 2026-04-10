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

**Parallelism:** To launch multiple `general-purpose` subagents in parallel, emit all `Agent(...)` calls as SIBLING tool calls in the SAME assistant message. See `x-dev-story-implement` Phases 1B-1F "Parallelism + tracking batching" section for the canonical parallel dispatch pattern (Batch A = all TaskCreate + Agent launches as siblings; Batch B = all TaskUpdate as siblings after results return).

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
