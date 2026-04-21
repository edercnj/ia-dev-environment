# Rule 14 — Project Scope Guard

## Rule

This project is a **CLI code generator**. Its sole purpose is:

1. Read a YAML configuration file describing a project's tech stack
2. Generate the `.claude/` directory structure with the appropriate skills, rules, agents, hooks, and settings
3. Copy and assemble resource files according to the user's configuration

## Allowed Code

| Layer | Allowed |
|-------|---------|
| `cli/` | CLI commands (`generate`, `validate`) and display utilities |
| `application/assembler/` | Assemblers that produce `.claude/` artifacts |
| `config/` | YAML configuration loading and context building |
| `domain/model/` | Data models representing project configuration |
| `domain/port/` | Input/output port interfaces for generation |
| `domain/service/` | Domain services orchestrating generation |
| `domain/stack/` | Stack resolution and validation |
| `template/` | Template engine integration |
| `infrastructure/adapter/` | Concrete implementations of output ports |
| `util/` | Path utilities, resource extraction |
| `exception/` | Core exception types |

## Forbidden

Adding Java code that does NOT serve the CLI generation pipeline:

- **Telemetry collection or analysis** — the generated `.claude/hooks/` scripts handle telemetry at runtime; the generator does not need Java telemetry classes
- **Release management** — version bumping, changelog generation, release orchestration belong in the generated skills, not in the generator itself
- **Parallelism analysis** — file footprint collision detection is a runtime concern of the orchestrating skills
- **Lifecycle auditing** — status reconciliation, lifecycle transitions are runtime concerns
- **CI validation gates** — telemetry marker linting, interactive gates auditing belong in CI scripts or generated skills
- **Checkpoint/resume for epic/story execution** — the generator runs to completion in one shot
- **Quality gate scoring** — story quality scoring is a runtime concern
- **Scope assessment** — story complexity classification is a runtime concern
- **Traceability analysis** — test-to-requirement correlation is a runtime concern
- **Schema versioning for execution state** — v1/v2 dispatch logic is a runtime concern

## Rationale

The project accumulated ~250 Java classes for runtime tools that are unrelated to code generation.
These classes inflated compile time, test suite duration, and maintenance burden.
The generated `.claude/` directory already contains skills and hooks that handle these runtime concerns — duplicating them in Java defeats the purpose of a generator.

## How to Evaluate New Code

Before adding a new package or class, ask:

1. Does this code run during `ia-dev-env generate` or `ia-dev-env validate`?
2. Does it read configuration and produce files in the `.claude/` output directory?
3. Would removing it break the generation pipeline?

If the answer to all three is NO, the code does not belong in this project.
