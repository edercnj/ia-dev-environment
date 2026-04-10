# x-task-plan

> Generates a detailed implementation plan for an individual task with per-task TDD cycle mapping (TPP order), file impact analysis by architecture layer, security checklist by task type, and integration points. Reads the task definition from story Section 8 and produces a self-contained execution guide.

| | |
|---|---|
| **Category** | Planning |
| **Invocation** | `/x-task-plan [STORY-ID] --task [TASK-ID] [--force]` |
| **Reads** | testing, architecture, security, coding-standards |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Produces a self-contained implementation plan for a single task extracted from a story's Section 8 task definitions. The plan maps TDD cycles in strict TPP order (degenerate first, complex last), identifies all affected files organized by architecture layer, generates a security checklist adapted to the task type (endpoint, persistence, domain, config, integration), and includes dependencies and definition of done criteria.

## Usage

```
/x-task-plan story-0029-0001 --task TASK-0029-0001-001
/x-task-plan story-0029-0001 --task TASK-0029-0001-001 --force
```

## Flags

| Flag | Default | Effect |
|------|---------|--------|
| `--task` | (required) | Task identifier in TASK-XXXX-YYYY-NNN format |
| `--force` | off | Regenerate plan even if a fresh one exists (bypass staleness check) |

## Workflow

1. Parse story ID and task ID, validate formats
2. Resolve epic directory and paths
3. Check staleness (idempotency pre-check)
4. Read story Section 8 and extract the specific task definition
5. Map TDD cycles in TPP order (minimum 3 cycles, degenerate first)
6. Analyze affected files organized by architecture layer
7. Generate security checklist based on task type
8. Write plan to `plans/epic-XXXX/plans/task-plan-XXXX-YYYY-NNN.md`

## Outputs

| Artifact | Path |
|----------|------|
| Task plan | `plans/epic-XXXX/plans/task-plan-XXXX-YYYY-NNN.md` |

## Plan Sections

| Section | Content |
|---------|---------|
| Header | Task ID, Story ID, Epic ID, Layer, Type, TDD Cycles count, Effort, Date |
| Objective | What the task accomplishes (from Section 8 title + acceptance criteria) |
| Implementation Guide | Target class/method, design pattern, step-by-step in layer order |
| TDD Cycles | RED/GREEN/REFACTOR cycles in TPP order with commit messages |
| Affected Files | Table of files by layer (domain -> port -> adapter -> application -> config) |
| Security Checklist | Items adapted to task type with severity and CWE references |
| Dependencies | Task and cross-story dependencies with rationale |
| Definition of Done | Completion criteria including TDD, tests, compilation, security |

## Security Checklist Adapts to Task Type

| Task Type | Focus Areas |
|-----------|-------------|
| Endpoint / API | Input validation, output encoding, auth, rate limiting |
| Persistence / DB | SQL injection, parameterized queries, encryption, audit |
| Domain Logic | Business rule bypass, state manipulation, privilege escalation |
| Config | Hardcoded secrets, secure defaults, externalization |
| Integration | TLS validation, timeouts, retry, circuit breaker |

## See Also

- [x-story-plan](../x-story-plan/) -- Multi-agent story planning (generates task breakdown consumed by this skill)
- [x-dev-story-implement](../x-dev-story-implement/) -- Full lifecycle orchestrator (reads task plans in PRE_PLANNED mode)
- [x-dev-implement](../x-dev-implement/) -- Implementation skill (uses task plans as execution guides)
- [x-test-plan](../x-test-plan/) -- Story-level test planning (complementary to per-task TDD cycles)
