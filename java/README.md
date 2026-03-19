# ia-dev-env (Java 2.0.0)

CLI tool that generates `.claude/` and `.github/` boilerplate for AI-assisted development environments.

## Overview

**ia-dev-env** generates comprehensive development environment configurations for AI coding assistants.
Given a YAML configuration file describing your project's technology stack, it produces:

- `.claude/` -- Rules, skills, agents, hooks, and settings for Claude Code
- `.github/` -- Instructions, skills, agents, prompts, and hooks for GitHub Copilot
- `.codex/` -- Configuration for OpenAI Codex
- `.agents/` -- Shared agent definitions
- `CLAUDE.md` -- Executive summary loaded in every Claude Code conversation
- `AGENTS.md` -- Root-level agent reference
- `docs/` -- Templates for stories, epics, ADRs, and threat models
- CI/CD -- Dockerfiles, docker-compose, Kubernetes manifests, CI workflows

Version 2.0.0 is a complete rewrite from Node.js/TypeScript to Java 21,
achieving byte-for-byte output parity with the original for all 8 bundled profiles.

## Prerequisites

- **Java 21** or later (LTS recommended)
- **Maven 3.9+** (only for building from source)

## Installation

### Option 1: Download the JAR

Download `ia-dev-env-2.0.0-SNAPSHOT.jar` from the releases page and run directly:

```bash
java -jar ia-dev-env-2.0.0-SNAPSHOT.jar --help
```

### Option 2: Use the wrapper script

The wrapper script auto-detects Java 21 and provides clear error messages:

```bash
./bin/ia-dev-env --help
```

### Option 3: Build from source

```bash
git clone https://github.com/edercnj/ia-dev-environment.git
cd ia-dev-environment/java
mvn clean package
java -jar target/ia-dev-env-2.0.0-SNAPSHOT.jar --help
```

## Quick Start

Generate boilerplate using a bundled stack profile:

```bash
# Using a bundled profile
java -jar ia-dev-env-2.0.0-SNAPSHOT.jar generate \
  --stack java-quarkus \
  --output my-project/

# Using a custom YAML config
java -jar ia-dev-env-2.0.0-SNAPSHOT.jar generate \
  --config my-config.yaml \
  --output my-project/

# Validate a config before generating
java -jar ia-dev-env-2.0.0-SNAPSHOT.jar validate \
  --config my-config.yaml
```

## Configuration

Configuration is provided via a YAML file. The minimal structure:

```yaml
project:
  name: "my-api"
  purpose: "REST API service for order management"

architecture:
  style: microservice

interfaces:
  - type: rest

language:
  name: java
  version: "21"

framework:
  name: quarkus
  version: "3.17"
  build_tool: maven
```

### Required Sections

| Section | Description |
|---------|-------------|
| `project.name` | Project name (used in generated files) |
| `project.purpose` | Brief description of the project |
| `architecture.style` | Architecture style (microservice, monolith, library, etc.) |
| `interfaces` | List of interface types (rest, grpc, cli, etc.) |
| `language.name` | Programming language |
| `language.version` | Language version |
| `framework.name` | Framework name |
| `framework.version` | Framework version |
| `framework.build_tool` | Build tool (maven, gradle, npm, cargo, etc.) |

### Optional Sections

| Section | Description |
|---------|-------------|
| `database` | Database configuration (type, migration tool) |
| `cache` | Cache configuration (redis, memcached, etc.) |
| `message_broker` | Message broker (kafka, rabbitmq, etc.) |
| `observability` | Observability stack (metrics, tracing, logging) |
| `security` | Security configuration |
| `cloud_provider` | Cloud provider specifics |
| `container` | Container runtime (docker) |
| `orchestrator` | Container orchestrator (kubernetes) |
| `domain` | Domain model, business rules, glossary |

## Bundled Profiles

8 ready-to-use profiles are included:

| Profile | Language | Framework | Architecture |
|---------|----------|-----------|--------------|
| `go-gin` | Go 1.23 | Gin 1.10 | microservice |
| `java-quarkus` | Java 21 | Quarkus 3.17 | microservice |
| `java-spring` | Java 21 | Spring Boot 3.4 | microservice |
| `kotlin-ktor` | Kotlin 2.1 | Ktor 3.0 | microservice |
| `python-click-cli` | Python 3.13 | Click 8.1 | library |
| `python-fastapi` | Python 3.13 | FastAPI 0.115 | microservice |
| `rust-axum` | Rust 1.83 | Axum 0.8 | microservice |
| `typescript-nestjs` | TypeScript 5 | NestJS 10.4 | microservice |

Usage:

```bash
java -jar ia-dev-env-2.0.0-SNAPSHOT.jar generate \
  --stack java-quarkus --output my-project/
```

## Advanced Usage

### Dry-run mode

Preview what files would be generated without writing anything:

```bash
java -jar ia-dev-env-2.0.0-SNAPSHOT.jar generate \
  --config my-config.yaml --dry-run
```

### Force overwrite

Overwrite existing artifact directories without prompting:

```bash
java -jar ia-dev-env-2.0.0-SNAPSHOT.jar generate \
  --config my-config.yaml --output my-project/ --force
```

### Verbose output

Show detailed pipeline execution information:

```bash
java -jar ia-dev-env-2.0.0-SNAPSHOT.jar generate \
  --config my-config.yaml --output my-project/ --verbose
```

### Interactive mode

Guided configuration via terminal prompts:

```bash
java -jar ia-dev-env-2.0.0-SNAPSHOT.jar generate \
  --interactive
```

### JVM options

Pass extra JVM options via the wrapper script:

```bash
IA_DEV_ENV_JAVA_OPTS="-Xmx512m" ./bin/ia-dev-env generate \
  --stack java-quarkus --output my-project/
```

## Building from Source

### Prerequisites

- Java 21+ (JDK)
- Maven 3.9+

### Build

```bash
# Build fat JAR (skip tests)
mvn clean package -DskipTests

# Build with unit tests
mvn clean package

# Build with all tests (unit + integration + golden file)
mvn clean verify -P all-tests

# Run unit tests only
mvn test

# Run integration tests only
mvn verify -P integration-tests
```

### Test Coverage

```bash
# Generate JaCoCo coverage report
mvn verify -P all-tests

# View report
open target/site/jacoco/index.html
```

Coverage thresholds (enforced by JaCoCo):
- Line coverage: >= 95%
- Branch coverage: >= 90%

### Native Image (optional)

Build a native executable using GraalVM:

```bash
mvn package -P native -DskipTests
./target/ia-dev-env --help
```

Requires GraalVM 21+ with `native-image` installed.

## Contributing

### Code Style

- Java 21 with modern idioms (records, sealed interfaces, pattern matching)
- Method length <= 25 lines
- Class length <= 250 lines
- Parameters per function <= 4
- Line width <= 120 characters

### Commit Convention

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(assembler): add CiCd assembler for workflow generation
fix(template): correct Pebble whitespace handling
test(golden): add byte-for-byte tests for go-gin profile
docs(readme): update installation instructions
refactor(config): extract validation into StackValidator
```

### Pull Request Process

1. Create a feature branch from `main`
2. Write tests first (TDD: Red-Green-Refactor)
3. Ensure all tests pass: `mvn verify -P all-tests`
4. Ensure coverage thresholds are met
5. Submit PR with clear description

### Project Structure

```
java/
├── pom.xml
├── bin/
│   └── ia-dev-env          # Wrapper script
├── src/
│   ├── main/
│   │   ├── java/dev/iadev/
│   │   │   ├── assembler/  # 23 assemblers + pipeline
│   │   │   ├── cli/        # Picocli commands
│   │   │   ├── config/     # Config loading + profiles
│   │   │   ├── domain/     # Stack resolution + validation
│   │   │   ├── exception/  # Custom exceptions
│   │   │   ├── model/      # Data classes (records)
│   │   │   ├── template/   # Pebble template engine
│   │   │   └── util/       # Path security, file utils
│   │   └── resources/
│   │       ├── config-templates/  # 9 bundled profiles
│   │       ├── templates/         # Document templates
│   │       └── ...                # Pebble templates
│   └── test/
│       ├── java/dev/iadev/        # Test classes
│       └── resources/golden/      # Golden file fixtures
└── target/
    └── ia-dev-env-2.0.0-SNAPSHOT.jar  # Fat JAR
```
