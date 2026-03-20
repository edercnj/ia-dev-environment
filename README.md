# ia-dev-environment

A CLI tool that generates complete `.claude/`, `.github/`, `.codex/`, and `.agents/` boilerplate for AI-assisted development environments. Produces rules, skills, agents, hooks, settings, and documentation -- everything a Claude Code, GitHub Copilot, or OpenAI Codex project needs to enforce engineering standards from day one.

## Prerequisites

- Java 21 or later

## Installation

### From JAR (recommended)

```bash
# Clone and build
git clone https://github.com/edercnj/ia-dev-environment.git
cd ia-dev-environment/java
mvn clean package -DskipTests

# Run
java -jar target/ia-dev-env-2.0.0-SNAPSHOT.jar --help
```

### Using the wrapper script

```bash
# Make the wrapper executable
chmod +x bin/ia-dev-env

# Run (auto-detects Java 21)
./bin/ia-dev-env --help
```

### GraalVM native image (optional)

```bash
cd java
mvn clean package -Pnative -DskipTests
./target/ia-dev-env --help
```

## Usage

### Generate from a config file

```bash
# Use a bundled stack profile
java -jar java/target/ia-dev-env-2.0.0-SNAPSHOT.jar generate --stack java-spring --output ./my-project

# Use your own config file
java -jar java/target/ia-dev-env-2.0.0-SNAPSHOT.jar generate --config my-config.yaml --output ./my-project
```

### Generate interactively

```bash
java -jar java/target/ia-dev-env-2.0.0-SNAPSHOT.jar generate --interactive --output ./my-project
```

### Validate a config file

```bash
java -jar java/target/ia-dev-env-2.0.0-SNAPSHOT.jar validate --config my-config.yaml --verbose
```

### Dry run (preview without writing)

```bash
java -jar java/target/ia-dev-env-2.0.0-SNAPSHOT.jar generate --config my-config.yaml --dry-run
```

### CLI Reference

```
ia-dev-env [OPTIONS] COMMAND [ARGS]...

Commands:
  generate    Generate AI dev environment boilerplate from config or interactive mode
  validate    Validate a YAML configuration file

Global Options:
  -V, --version   Print version information and exit
  -h, --help      Show this help message and exit

Generate Options:
  -c, --config <path>    Path to YAML config file
  -i, --interactive      Run in interactive mode (mutually exclusive with --config)
  -s, --stack <name>     Use a bundled stack profile
  -o, --output <dir>     Output directory (default: .)
  -v, --verbose          Enable verbose logging
  -f, --force            Overwrite existing files without prompting
  --dry-run              Show what would be generated without writing

Validate Options:
  -c, --config <path>    Path to YAML config file (required)
  -v, --verbose          Enable verbose output with per-category results
```

## Bundled Stack Profiles

8 ready-to-use profiles available via `--stack <name>`:

| Profile | Stack |
|---------|-------|
| `go-gin` | Go 1.22, Gin, PostgreSQL |
| `java-quarkus` | Java 21, Quarkus 3.17, PostgreSQL |
| `java-spring` | Java 21, Spring Boot 3.4, PostgreSQL |
| `kotlin-ktor` | Kotlin 2.0, Ktor, PostgreSQL |
| `python-click-cli` | Python 3.9, Click 8.1 |
| `python-fastapi` | Python 3.12, FastAPI, PostgreSQL |
| `rust-axum` | Rust 2024, Axum, PostgreSQL |
| `typescript-nestjs` | TypeScript 5, NestJS, PostgreSQL |

## What's Generated

The generator produces boilerplate for multiple AI coding assistants:

```
.claude/                          # Claude Code configuration
├── README.md                     # Auto-generated project guide
├── settings.json                 # Permissions and hooks
├── settings.local.json           # Local overrides template
├── rules/                        # Coding rules (loaded into system prompt)
├── skills/                       # Skills invocable via /command
│   └── {knowledge-packs}/        # Internal context for agents
├── agents/                       # AI personas (architect, tech-lead, etc.)
└── hooks/                        # Automation scripts (post-compile, etc.)

.github/                          # GitHub Copilot configuration
├── copilot-instructions.md       # Global Copilot instructions
├── instructions/                 # Contextual instructions
├── skills/                       # Reusable Copilot skills
├── agents/                       # Agent definitions (*.agent.md)
├── prompts/                      # Prompt templates
└── hooks/                        # Event hooks

.codex/                           # OpenAI Codex configuration
├── config.toml                   # Model, approval, sandbox settings
└── (AGENTS.md at project root)   # Consolidated agent instructions

.agents/                          # Shared skills (cross-platform)
└── skills/                       # Mirrored skills for Codex agents

docs/                             # Documentation templates
├── architecture/                 # Service architecture doc
├── adr/                          # ADR template and index
└── runbook/                      # Deploy runbook template
```

Plus CI/CD artifacts: `Dockerfile`, `docker-compose.yml`, `.github/workflows/ci.yml`, and Kubernetes manifests (when applicable).

## Development

```bash
cd java

# Build
mvn clean package

# Run tests
mvn test

# Run tests with coverage report
mvn verify

# Run only unit tests
mvn test -Punit-tests

# Run integration tests (golden file parity)
mvn verify -Pintegration-tests

# Run all tests
mvn verify -Pall-tests
```

### Project Structure

```
ia-dev-environment/
├── .claude/                      # Claude Code project config (generated)
├── .github/                      # GitHub Copilot config (generated)
├── .agents/                      # Shared agent skills (generated)
├── .codex/                       # Codex config (generated)
├── docs/                         # Stories, specs, epics
│   ├── specs/                    # System specifications
│   └── stories/                  # Epic stories and implementation maps
├── java/                         # Java 21 source (single codebase)
│   ├── pom.xml                   # Maven build (JUnit 5, JaCoCo, Picocli, Pebble)
│   ├── bin/                      # Wrapper script with Java 21 detection
│   ├── src/main/java/dev/iadev/
│   │   ├── cli/                  # Picocli commands (generate, validate)
│   │   ├── config/               # YAML loading, profiles, context builder
│   │   ├── model/                # 17 immutable data records
│   │   ├── domain/               # Stack resolution, DAG, implementation map
│   │   ├── assembler/            # 23 assemblers (rules, skills, agents, ...)
│   │   ├── template/             # Pebble engine with Python-bool filter
│   │   ├── checkpoint/           # Execution state management
│   │   ├── progress/             # Metrics and reporting
│   │   ├── exception/            # 7 context-rich exceptions
│   │   └── util/                 # I/O, path safety, resource discovery
│   ├── src/main/resources/       # ~464 template files on classpath
│   │   ├── config-templates/     # 8 bundled stack profiles (YAML)
│   │   └── templates/            # Pebble/Nunjucks templates
│   └── src/test/
│       ├── java/                 # 1959 tests (unit + integration + golden)
│       └── resources/golden/     # Golden files for 8 profiles
├── CLAUDE.md                     # Executive summary (auto-loaded)
├── AGENTS.md                     # Codex agent instructions
└── README.md                     # This file
```

### Coverage

| Metric | Minimum | Current |
|--------|---------|---------|
| Line Coverage | >= 95% | 95.23% |
| Branch Coverage | >= 90% | 91.12% |

Enforced by JaCoCo in `mvn verify`. Build fails if thresholds are not met.

## License

MIT
