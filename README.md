# ia-dev-environment

A CLI tool that generates complete `.claude/` and `.github/` boilerplate for AI-assisted development environments. Produces rules, skills, agents, hooks, settings, and documentation -- everything a Claude Code or GitHub Copilot project needs to enforce engineering standards from day one.

## Prerequisites

- Node.js >= 18

## Installation

```bash
# Install globally from npm
npm install -g ia-dev-environment

# Or run directly with npx
npx ia-dev-env generate --help
```

## Usage

### Generate from a config file

```bash
# Use one of the bundled config profiles
ia-dev-env generate --config resources/config-templates/setup-config.python-fastapi.yaml --output-dir /path/to/your-project/

# Or use your own config file
ia-dev-env generate --config my-config.yaml --output-dir .claude/
```

### Generate interactively

```bash
ia-dev-env generate --interactive --output-dir .claude/
```

### Validate a config file (without generating output)

```bash
ia-dev-env validate --config my-config.yaml
```

### Dry run (preview what would be generated)

```bash
ia-dev-env generate --config my-config.yaml --dry-run
```

### CLI Reference

```
ia-dev-env [OPTIONS] COMMAND [ARGS]...

Commands:
  generate    Generate project scaffolding from config or interactive mode
  validate    Validate a config file without generating output

Global Options:
  --version   Show the version and exit
  --help      Show this message and exit

Generate Options:
  -c, --config PATH         Path to YAML config file
  -i, --interactive         Run in interactive mode
  -o, --output-dir PATH     Output directory (default: .)
  -s, --resources-dir PATH  Resources templates directory (auto-detected)
  -v, --verbose             Enable verbose logging
  --dry-run                 Show what would be generated without writing
```

## Bundled Config Profiles

The repository includes 8 ready-to-use config profiles under `resources/config-templates/`:

| Profile | File | Stack |
|---------|------|-------|
| Go + Gin | `setup-config.go-gin.yaml` | Go 1.22, Gin, PostgreSQL |
| Java + Quarkus | `setup-config.java-quarkus.yaml` | Java 21, Quarkus 3.17, PostgreSQL |
| Java + Spring | `setup-config.java-spring.yaml` | Java 21, Spring Boot 3.4, PostgreSQL |
| Kotlin + Ktor | `setup-config.kotlin-ktor.yaml` | Kotlin 2.0, Ktor, PostgreSQL |
| Python + Click | `setup-config.python-click-cli.yaml` | Python 3.9, Click 8.1 |
| Python + FastAPI | `setup-config.python-fastapi.yaml` | Python 3.12, FastAPI, PostgreSQL |
| Rust + Axum | `setup-config.rust-axum.yaml` | Rust 2024, Axum, PostgreSQL |
| TypeScript + NestJS | `setup-config.typescript-nestjs.yaml` | TypeScript 5, NestJS, PostgreSQL |

## What's Generated

The generator produces a complete `.claude/` directory and `.github/` directory:

```
.claude/
├── README.md               <- Auto-generated project guide
├── settings.json           <- Permissions and hooks
├── settings.local.json     <- Local overrides template
├── rules/                  <- Coding rules (<=30 consolidated files)
├── skills/                 <- Skills invocable via /command
├── agents/                 <- AI personas for planning, implementation, review
└── hooks/                  <- Automation scripts

.github/
├── copilot-instructions.md <- Global Copilot instructions
├── instructions/           <- Contextual instructions
├── skills/                 <- Reusable Copilot skills
├── agents/                 <- Agent definitions
├── prompts/                <- Prompt templates
└── hooks/                  <- Event hooks
```

## Development

```bash
# Install dependencies
npm install

# Build
npm run build

# Run tests
npm test

# Run tests with coverage
npm run test:coverage

# Run integration tests only
npm run test:integration

# Type check
npm run lint
```

### Project Structure

```
ia-dev-environment/
├── src/                      # TypeScript source
│   ├── index.ts              # CLI entry point (Commander)
│   ├── cli.ts                # Command definitions
│   ├── config.ts             # YAML config loading + validation
│   ├── models.ts             # Type definitions
│   ├── template-engine.ts    # Nunjucks template engine
│   ├── assembler/            # Modular assemblers (rules, skills, agents, etc.)
│   └── domain/               # Domain logic (stack resolution, validation)
├── resources/                # Templates, configs, rules (runtime dependency)
├── tests/
│   ├── node/                 # TypeScript test files
│   ├── fixtures/             # Test fixtures
│   ├── golden/               # Golden reference files (8 profiles)
│   └── helpers/              # Shared test helpers
├── package.json
├── tsconfig.json
├── tsup.config.ts
└── vitest.config.ts
```

### Coverage Targets

| Metric | Minimum | Current |
|--------|---------|---------|
| Line Coverage | >= 95% | 99.6% |
| Branch Coverage | >= 90% | 97.84% |

## License

MIT
