# Rule 23 — Model Selection Strategy

> **Related:** Rule 13 (Skill Invocation Protocol), Rule 22 (Skill Visibility).
> **Introduced by:** EPIC-0050 (Model Selection Enforcement & Token Optimization).

## Purpose

Models are an economic resource. Opus tokens cost ~2× Sonnet and ~5× Haiku, and every implicit default ("inherit from parent") cascades premium cost across an entire epic run (`x-epic-implement` → `x-story-implement` → `x-test-tdd` → `x-git-commit`). Model selection MUST be **explicit at the point of invocation** — via frontmatter, `Agent(model:)`, `Skill(model:)`, and agent metadata — so that deep-reasoning work lands in Opus, orchestration and review land in Sonnet, and utilities land in Haiku. Baseline before this rule: 84.4% Opus / 0.2% Sonnet / 5.8% Haiku. Target: ≤50% Opus / ≥35% Sonnet / ≥12% Haiku.

## Matrix

| Layer | Default Tier | Examples | Justification | Exception |
| :--- | :--- | :--- | :--- | :--- |
| Orchestrator | `sonnet` | `x-epic-implement`, `x-story-implement`, `x-release`, `x-review`, `x-epic-orchestrate`, `x-pr-fix-epic`, `x-task-implement`, `x-epic-decompose` | Dispatches other skills/agents; no deep design reasoning inline. | None — orchestrators always declare `model:` explicitly. |
| Deep Planner | `opus` | `x-arch-plan`, Architect subagent inside `x-story-plan` | Produces architecture plans, ADR content, trade-off analysis that benefits from strongest reasoning. | None. |
| Reviewer / Validator | `sonnet` | `x-review-qa`, `x-review-perf`, `x-review-pr`, `x-review-devops`, reviewer subagents | Applies checklists against code/plans; structured reasoning suffices. | Security-critical review MAY opt into `opus` per story. |
| Executor | `sonnet` | `x-task-implement`, `x-test-tdd`, TDD inner loop | TDD cycle is procedural; Sonnet preserves quality at lower cost. | `opus` only when story explicitly flags "deep-reasoning implementation". |
| Utility | `haiku` | `x-git-worktree`, `x-git-commit`, `x-code-format`, `x-code-lint` | Git ops, formatting, linting — zero design reasoning. | None. |
| Knowledge Pack (KP) | `haiku` | `architecture`, `coding-standards`, `testing`, `layer-templates`, `patterns`, `dockerfile` | Read-only reference consumed as context. No reasoning performed by the KP itself. | None. |

## Enforcement Points

Three technical contracts. Each MUST carry `model:` at the invocation site.

### 1. Frontmatter YAML on SKILL.md

```yaml
---
name: x-epic-implement
description: ...
model: sonnet
allowed-tools: [Skill, Agent, Bash]
---
```

Every skill classified as `Orchestrator`, `Deep Planner`, `Reviewer`, `Executor`, `Utility`, or `KP` in the Matrix MUST declare `model:` in the frontmatter. Absence is a CI audit violation (see Audit contract below). User-facing entry-point skills invoked directly by the human (see Exceptions) MAY omit the field — inheritance from the user session is allowed only in that case.

### 2. `Agent(...)` with explicit `model:`

```text
Agent(
  subagent_type: "general-purpose",
  model: "sonnet",
  description: "...",
  prompt: "..."
)
```

Every `Agent(subagent_type: "general-purpose", ...)` invocation inside a skill MUST pass `model:` explicitly. Implicit inheritance from the parent skill context is forbidden — subagents choose their own tier, independent of the parent. Specialized subagent types (`Explore`, `statusline-setup`, etc.) follow the same rule when their body targets orchestration work.

### 3. `Skill(...)` with explicit `model:`

```text
Skill(
  skill: "x-story-implement",
  model: "sonnet",
  args: "..."
)
```

Every `Skill(skill: "...", ...)` call inside an orchestrator MUST pass `model:` when the invoked skill's tier differs from the parent. Exception: user entry-points (top-level invocation by a human typing `/x-epic-implement` in chat) inherit — the human's session model is the tier for that outer skill, and only outer-skill callees need explicit `model:`.

## Agent Metadata Contract

Every file under `.claude/agents/*.md` MUST declare a deterministic `Recommended Model`:

```markdown
**Recommended Model:** Opus
```

Allowed values: `Opus`, `Sonnet`, `Haiku`. The value **`Adaptive` is forbidden** — it resolves to Opus in practice and is the mechanical cause of the 84.4% Opus baseline. Default matrix:

| Agent role | Recommended Model | Rationale |
| :--- | :--- | :--- |
| `architect` | Opus | Deep design reasoning. |
| `product-owner` | Sonnet | Validates against requirements; no deep design. (Changed from Opus.) |
| `qa`, `security`, `sre`, `performance`, `tech-lead`, `devops`, `devsecops`, `senior-engineer` | Sonnet | Structured review against checklists. |

## Haiku Eligibility Criteria

A skill is eligible for `model: haiku` if it satisfies at least one of:

- **(a) Utility without design reasoning** — git operations, formatting, linting, status-file mutations, branch manipulation. No code generation, no architectural choices, no test authoring.
- **(b) Read-only knowledge pack** — consumed as reference context by other skills; performs no execution logic of its own.

Initial eligibility list (10 skills): `x-git-worktree`, `x-git-commit`, `x-code-format`, `x-code-lint`, `architecture`, `coding-standards`, `testing`, `layer-templates`, `patterns`, `dockerfile`. Additions require a one-paragraph rationale in the skill's SKILL.md Integration Notes section.

## Audit Contract

CI MUST run `scripts/audit-model-selection.sh` on every PR. The script fails (exit code ≥ 1) when any of the following are detected within the declared enforcement matrix:

- **(a)** An orchestrator SKILL.md without `model:` in frontmatter.
- **(b)** An `Agent(subagent_type: "general-purpose", ...)` block inside an orchestrator that omits `model:`.
- **(c)** A `Skill(skill: "x-...", ...)` call inside an orchestrator that omits `model:` when the callee tier differs from the parent.
- **(d)** An agent file with `Recommended Model: Adaptive` or with no `Recommended Model` declaration at all.

Canonical check form (adapted from the Rule 13 audit pattern — zero matches = pass):

```bash
# (b) — Agent() without model: in orchestrator skills
grep -rnE "Agent\(\s*subagent_type:\s*\"general-purpose\"" \
    java/src/main/resources/targets/claude/skills/ \
    --include=SKILL.md \
  | while read -r hit; do
      file="${hit%%:*}"
      if grep -qE "model:\s*\"?(opus|sonnet|haiku)\"?" "$file"; then continue; fi
      echo "MISSING_MODEL $hit"
    done
```

Full script (all four checks) ships in STORY-0050-0009.

## Backward Compatibility

Model selection is **additive**:

- This rule introduces `model:` where it did not exist; it does NOT modify skills outside the declared matrix.
- Skills and agents outside the enforcement matrix continue to operate on implicit inheritance (Opus by default). Future epics MAY extend the matrix.
- CI audit covers only the declared targets. Expansion of the audit scope requires an explicit rule-update epic.
- Existing epics in flight before this rule was introduced (created before EPIC-0050 merges) are exempt. Their `execution-state.json` MAY omit `model:` references in planning artifacts without triggering the audit.

## Exceptions

- **User-invoked entry-point skills** (skills a human types as `/name` in chat — e.g., `/x-release`, `/x-epic-implement`, `/x-review`) MAY omit `model:` in frontmatter: the user's session tier applies to the outer skill, and only nested callees must declare. Audit script MUST skip these (list maintained in the script header).
- **Legacy skills (pre-EPIC-0050)** not in the enforcement matrix are ignored until they enter a future scope-expansion epic.
- **Internal skills (`x-internal-*`, Rule 22)** follow the same matrix as their public caller but MAY omit `model:` when their parent orchestrator's frontmatter guarantees the tier — declare `model:` only if the internal skill legitimately needs a different tier.
- **One-off experimental skills under `.claude/skills/experimental/`** are out of scope for this rule.
