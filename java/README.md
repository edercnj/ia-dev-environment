# ia-dev-env (Java 2.0.0)

CLI tool that generates `.claude/` and `.github/` boilerplate for AI-assisted development environments.

## Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [CLI Commands](#cli-commands)
- [Configuration Reference](#configuration-reference)
- [Bundled Profiles](#bundled-profiles)
- [Configuration Examples](#configuration-examples)
- [Generated Output](#generated-output)
- [Advanced Usage](#advanced-usage)
- [Environment Variables](#environment-variables)
- [Building from Source](#building-from-source)
- [Contributing](#contributing)
- [Project Structure](#project-structure)

---

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
achieving byte-for-byte output parity with the original for all bundled profiles.

---

## Prerequisites

- **Java 21** or later (LTS recommended)
- **Maven 3.9+** (only for building from source)

---

## Installation

### Option 1: Install script (recommended)

The install script builds (or copies) the fat JAR, installs it to your system, and
creates the `ia-dev-env` command globally.

**macOS / Linux:**

```bash
git clone https://github.com/edercnj/ia-dev-environment.git
cd ia-dev-environment/java
bash install.sh
```

**Windows (PowerShell):**

```powershell
git clone https://github.com/edercnj/ia-dev-environment.git
cd ia-dev-environment\java
.\install.ps1
```

After installation, run from anywhere:

```bash
ia-dev-env --version
ia-dev-env generate --stack java-quarkus --output my-project/
```

#### Install script options (macOS / Linux)

| Flag | Description |
|------|-------------|
| `--system` | Install to `/usr/local/` instead of `~/.local/` (requires sudo) |
| `--prefix=DIR` | Custom install directory |
| `--jar=PATH` | Use a pre-built JAR instead of building from source |
| `--skip-build` | Skip Maven build (expects JAR already in `target/`) |
| `--uninstall` | Remove ia-dev-env installation |
| `--help` | Show usage |

#### Install script options (Windows)

| Parameter | Description |
|-----------|-------------|
| `-Prefix` | Custom install directory |
| `-JarPath` | Use a pre-built JAR instead of building from source |
| `-SkipBuild` | Skip Maven build |
| `-Uninstall` | Remove ia-dev-env installation |

#### Install locations

| OS | JAR location | Wrapper location | Requires sudo? |
|----|-------------|-----------------|----------------|
| **macOS** | `~/.local/share/ia-dev-env/ia-dev-env.jar` | `~/.local/bin/ia-dev-env` | No |
| **Linux** | `~/.local/share/ia-dev-env/ia-dev-env.jar` | `~/.local/bin/ia-dev-env` | No |
| **macOS/Linux** (`--system`) | `/usr/local/share/ia-dev-env/ia-dev-env.jar` | `/usr/local/bin/ia-dev-env` | Yes |
| **Windows** | `%LOCALAPPDATA%\Programs\ia-dev-env\ia-dev-env.jar` | `%LOCALAPPDATA%\Programs\ia-dev-env\ia-dev-env.cmd` | No |

#### Uninstall

```bash
# macOS / Linux
bash install.sh --uninstall

# macOS / Linux (system install)
bash install.sh --system --uninstall

# Windows
.\install.ps1 -Uninstall
```

### Option 2: Download the JAR

Download `ia-dev-env-2.0.0-SNAPSHOT.jar` from the releases page and run directly:

```bash
java -jar ia-dev-env-2.0.0-SNAPSHOT.jar --help
```

### Option 3: Use the wrapper script (development)

The wrapper script auto-detects Java 21 and provides clear error messages:

```bash
./bin/ia-dev-env --help
```

### Option 4: Build from source

```bash
git clone https://github.com/edercnj/ia-dev-environment.git
cd ia-dev-environment/java
mvn clean package
java -jar target/ia-dev-env-2.0.0-SNAPSHOT.jar --help
```

---

## Quick Start

```bash
# Generate from a bundled profile
ia-dev-env generate --stack java-quarkus --output my-project/

# Generate from a custom YAML config
ia-dev-env generate --config my-config.yaml --output my-project/

# Preview without writing files
ia-dev-env generate --stack go-gin --output my-project/ --dry-run

# Validate a config file
ia-dev-env validate --config my-config.yaml

# Interactive mode (guided prompts)
ia-dev-env generate --interactive
```

---

## CLI Commands

### `ia-dev-env generate`

Generate AI dev environment boilerplate from a configuration.

```
ia-dev-env generate [OPTIONS]
```

| Option | Short | Description |
|--------|-------|-------------|
| `--config <path>` | `-c` | Path to YAML configuration file |
| `--stack <name>` | `-s` | Use a bundled stack profile by name |
| `--interactive` | `-i` | Enable interactive guided mode |
| `--output <dir>` | `-o` | Output directory (default: current directory) |
| `--dry-run` | | Simulate without writing files |
| `--force` | `-f` | Overwrite existing files without prompting |
| `--verbose` | `-v` | Show detailed pipeline execution info |

> One of `--config`, `--stack`, or `--interactive` is required. They are mutually exclusive.

### `ia-dev-env validate`

Validate a YAML configuration file without generating output.

```
ia-dev-env validate [OPTIONS]
```

| Option | Short | Description |
|--------|-------|-------------|
| `--config <path>` | `-c` | Path to YAML configuration file (required) |
| `--verbose` | `-v` | Show detailed validation info |

### Global Options

| Option | Description |
|--------|-------------|
| `--help` / `-h` | Show help message |
| `--version` | Show version (2.0.0) |

---

## Configuration Reference

Configuration is provided via a YAML file with up to 18 sections. Only sections 1-5 are
required; all others have sensible defaults.

### Section 1: Project Identity (required)

```yaml
project:
  name: "my-service"           # Project name in kebab-case
  purpose: "Brief description" # One-line description of the project
```

### Section 2: Architecture (required)

```yaml
architecture:
  style: microservice          # Architecture style
  domain_driven: true          # Enable DDD patterns (default: false)
  event_driven: true           # Enable event-driven patterns (default: false)
```

| Field | Values |
|-------|--------|
| `style` | `microservice`, `modular-monolith`, `monolith`, `library`, `serverless` |
| `domain_driven` | `true`, `false` |
| `event_driven` | `true`, `false` |

### Section 3: Interfaces (required)

```yaml
interfaces:
  - type: rest
    spec: openapi              # API specification (optional)
  - type: grpc
    spec: proto3
  - type: event-consumer
    broker: kafka              # Message broker (for event types)
  - type: event-producer
    broker: kafka
```

| Field | Values |
|-------|--------|
| `type` | `rest`, `grpc`, `graphql`, `websocket`, `tcp-custom`, `cli`, `event-consumer`, `event-producer`, `scheduled` |
| `spec` | `openapi`, `openapi-3.1`, `proto3`, `graphql`, `code-first`, `websocket`, `tcp-custom`, `kafka` |
| `broker` | `kafka` (used with `event-consumer` and `event-producer`) |

### Section 4: Language (required)

```yaml
language:
  name: java
  version: "21"
```

| Field | Values |
|-------|--------|
| `name` | `java`, `kotlin`, `python`, `go`, `typescript`, `rust` |
| `version` | Version string (e.g., `"21"`, `"3.12"`, `"1.22"`, `"2.0"`, `"5"`, `"2024"`) |

### Section 5: Framework (required)

```yaml
framework:
  name: quarkus
  version: "3.17"
  build_tool: maven            # Build tool (optional, has defaults)
  native_build: true           # GraalVM native image (optional, default: false)
```

| Field | Values |
|-------|--------|
| `name` | `spring-boot`, `quarkus`, `picocli`, `fastapi`, `click`, `django`, `flask`, `gin`, `fiber`, `stdlib`, `ktor`, `nestjs`, `express`, `fastify`, `commander`, `axum`, `actix-web`, `aspnet` |
| `build_tool` | `maven`, `gradle`, `npm`, `pip`, `go-mod`, `cargo`, `dotnet` |
| `native_build` | `true`, `false` (only supported for `quarkus` and `spring-boot`) |

**Language-Framework compatibility:**

| Language | Supported Frameworks | Default Build Tool |
|----------|---------------------|--------------------|
| Java | `spring-boot`, `quarkus`, `picocli` | `maven` |
| Kotlin | `ktor` | `gradle` |
| Python | `fastapi`, `click`, `django`, `flask` | `pip` |
| Go | `gin`, `fiber`, `stdlib` | `go-mod` |
| TypeScript | `nestjs`, `express`, `fastify`, `commander` | `npm` |
| Rust | `axum`, `actix-web` | `cargo` |

### Section 6: Data (optional)

```yaml
data:
  database:
    type: postgresql
    version: "17"
    migration: flyway

  cache:
    type: redis
    version: "7.4"

  message_broker:
    type: kafka
    version: "3.7"
```

| Field | Values |
|-------|--------|
| `database.type` | `postgresql`, `mysql`, `oracle`, `mongodb`, `cassandra`, `none` |
| `database.migration` | `flyway`, `liquibase`, `alembic`, `prisma`, `none` |
| `cache.type` | `redis`, `dragonfly`, `memcached`, `none` |
| `message_broker.type` | `kafka`, `none` |

### Section 7: Security (optional)

```yaml
security:
  compliance:
    - lgpd
  encryption:
    at_rest: true
    key_management: vault
  pentest_readiness: true
```

| Field | Values |
|-------|--------|
| `compliance` | List of: `lgpd`, `gdpr`, `hipaa`, `pci-dss`, `sox` |
| `encryption.at_rest` | `true`, `false` |
| `encryption.key_management` | `vault`, `none` |
| `pentest_readiness` | `true`, `false` |

### Section 8: Cloud Provider (optional)

```yaml
cloud:
  provider: none
```

| Field | Values |
|-------|--------|
| `provider` | `aws`, `azure`, `gcp`, `oci`, `none` |

### Section 9: Infrastructure (optional)

```yaml
infrastructure:
  container: docker
  orchestrator: kubernetes
  templating: kustomize
  iac: terraform
  registry: ghcr
  api_gateway: kong
  service_mesh: istio
```

| Field | Values |
|-------|--------|
| `container` | `docker`, `podman`, `none` |
| `orchestrator` | `kubernetes`, `docker-swarm`, `nomad`, `ecs`, `none` |
| `templating` | `kustomize`, `helm`, `none` |
| `iac` | `terraform`, `pulumi`, `cloudformation`, `bicep`, `none` |
| `registry` | `ecr`, `acr`, `gar`, `ghcr`, `dockerhub`, `quay`, `none` |
| `api_gateway` | `kong`, `nginx`, `traefik`, `apigee`, `none` |
| `service_mesh` | `istio`, `linkerd`, `consul`, `none` |

### Section 10: Observability (optional)

```yaml
observability:
  standard: opentelemetry
  backend: grafana-stack
```

| Field | Values |
|-------|--------|
| `standard` | `opentelemetry`, `none` |
| `backend` | `grafana-stack`, `none` |

### Section 11: Testing (optional)

```yaml
testing:
  smoke_tests: true
  performance_tests: true
  contract_tests: true
  chaos_tests: true
```

| Field | Default | Description |
|-------|---------|-------------|
| `smoke_tests` | `true` | Black-box tests against running environment |
| `performance_tests` | `true` | Latency SLAs, throughput, resource usage |
| `contract_tests` | `false` | Consumer-driven contract tests (e.g., Pact) |
| `chaos_tests` | `false` | Fault injection and resilience tests |

### Section 12: Domain Template (optional)

```yaml
domain:
  template: none
```

| Field | Values |
|-------|--------|
| `template` | `none`, or custom domain template name (e.g., `iso8583`, `open-banking`, `healthcare-fhir`) |

### Section 13: Conventions (optional)

```yaml
conventions:
  code_language: en
  commit_language: en
  documentation_language: pt-br
  git_scopes:
    - { scope: "domain", area: "Core domain logic" }
    - { scope: "infra", area: "Infrastructure and adapters" }
    - { scope: "api", area: "REST/gRPC API layer" }
    - { scope: "events", area: "Event producers and consumers" }
    - { scope: "config", area: "Configuration and properties" }
```

| Field | Description |
|-------|-------------|
| `code_language` | Language for code identifiers (e.g., `en`) |
| `commit_language` | Language for commit messages (e.g., `en`) |
| `documentation_language` | Language for generated docs (e.g., `en`, `pt-br`) |
| `git_scopes` | List of Conventional Commits scopes with descriptions |

### Section 14: Skills (optional)

```yaml
skills:
  override: auto               # auto-generate skills based on stack
```

### Section 15: Agents (optional)

```yaml
agents:
  override: auto               # auto-generate agents based on stack
  adaptive_model:
    junior: "haiku"            # Fast model for simple tasks
    mid: "sonnet"              # Balanced model
    senior: "opus"             # Most capable model
```

### Section 16: Hooks (optional)

```yaml
hooks:
  post_compile: true           # Run compiler check after file edits
```

| Field | Description |
|-------|-------------|
| `post_compile` | `true` for compiled languages (Java, Go, Rust, Kotlin), `false` for interpreted (Python, TypeScript with tsx) |

### Section 17: Settings (optional)

```yaml
settings:
  auto_generate: true          # Auto-generate settings.json
```

### Section 18: MCP Servers (optional)

```yaml
mcp:
  servers:
    - id: "firecrawl-mcp"
      url: "https://mcp.firecrawl.dev"
      capabilities:
        - scrape
        - crawl
        - search
      env:
        FIRECRAWL_API_KEY: "$FIRECRAWL_API_KEY"
```

| Field | Description |
|-------|-------------|
| `servers[].id` | Unique server identifier |
| `servers[].url` | Server URL |
| `servers[].capabilities` | List of capabilities (e.g., `scrape`, `crawl`, `search`) |
| `servers[].env` | Environment variables map (key-value pairs) |

---

## Bundled Profiles

10 ready-to-use profiles are included. Use them with `--stack <name>`:

### Microservice Profiles (7)

| Profile | Language | Framework | Build Tool | Interfaces | DDD |
|---------|----------|-----------|------------|------------|-----|
| `java-quarkus` | Java 21 | Quarkus 3.17 | Maven | REST, gRPC, Kafka | Yes |
| `java-spring` | Java 21 | Spring Boot 3.x | Gradle | REST, gRPC, Kafka | Yes |
| `kotlin-ktor` | Kotlin 2.0 | Ktor | Gradle | REST, WebSocket, Kafka | Yes |
| `typescript-nestjs` | TypeScript 5 | NestJS | npm | REST, GraphQL, WebSocket, Kafka | Yes |
| `go-gin` | Go 1.22 | Gin | go-mod | REST, gRPC, Kafka | No |
| `rust-axum` | Rust 2024 | Axum | Cargo | REST, gRPC, Kafka | No |
| `python-fastapi` | Python 3.12 | FastAPI | pip | REST, WebSocket, Kafka | Yes |

All microservice profiles include: PostgreSQL 17, Redis 7.4, Kafka 3.7, Docker, Kubernetes,
Kustomize, Terraform, Kong API Gateway, OpenTelemetry + Grafana Stack, LGPD compliance, and Vault encryption.

### CLI Tool Profiles (3)

| Profile | Language | Framework | Build Tool | Notes |
|---------|----------|-----------|------------|-------|
| `java-picocli-cli` | Java 21 | Picocli 4.7 | Maven | DDD enabled, post-compile hook |
| `python-click-cli` | Python 3.9 | Click 8.1 | pip | Max compatibility target |
| `typescript-commander-cli` | TypeScript 5 | Commander | npm | Node.js runtime |

CLI profiles use `library` architecture, no database/cache/broker, and minimal infrastructure (Docker only).

### Usage

```bash
# Java microservice with Quarkus
ia-dev-env generate --stack java-quarkus --output my-quarkus-api/

# Java microservice with Spring Boot
ia-dev-env generate --stack java-spring --output my-spring-api/

# Kotlin microservice with Ktor
ia-dev-env generate --stack kotlin-ktor --output my-ktor-api/

# TypeScript microservice with NestJS
ia-dev-env generate --stack typescript-nestjs --output my-nestjs-api/

# Go microservice with Gin
ia-dev-env generate --stack go-gin --output my-go-api/

# Rust microservice with Axum
ia-dev-env generate --stack rust-axum --output my-rust-api/

# Python microservice with FastAPI
ia-dev-env generate --stack python-fastapi --output my-fastapi-api/

# Java CLI tool with Picocli
ia-dev-env generate --stack java-picocli-cli --output my-java-cli/

# Python CLI tool with Click
ia-dev-env generate --stack python-click-cli --output my-python-cli/

# TypeScript CLI tool with Commander
ia-dev-env generate --stack typescript-commander-cli --output my-ts-cli/
```

---

## Configuration Examples

### Minimal Configuration (5 required sections)

```yaml
project:
  name: "my-api"
  purpose: "REST API for order management"

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

### Full-Featured Java Microservice

```yaml
project:
  name: "order-service"
  purpose: "Manages order lifecycle from creation to fulfillment"

architecture:
  style: microservice
  domain_driven: true
  event_driven: true

interfaces:
  - type: rest
    spec: openapi
  - type: grpc
    spec: proto3
  - type: event-consumer
    broker: kafka
  - type: event-producer
    broker: kafka

language:
  name: java
  version: "21"

framework:
  name: quarkus
  version: "3.17"
  build_tool: maven
  native_build: true

data:
  database:
    type: postgresql
    version: "17"
    migration: flyway
  cache:
    type: redis
    version: "7.4"
  message_broker:
    type: kafka
    version: "3.7"

security:
  compliance:
    - lgpd
  encryption:
    at_rest: true
    key_management: vault
  pentest_readiness: true

cloud:
  provider: aws

infrastructure:
  container: docker
  orchestrator: kubernetes
  templating: kustomize
  iac: terraform
  registry: ecr
  api_gateway: kong
  service_mesh: istio

observability:
  standard: opentelemetry
  backend: grafana-stack

testing:
  smoke_tests: true
  performance_tests: true
  contract_tests: true
  chaos_tests: true

domain:
  template: none

conventions:
  code_language: en
  commit_language: en
  documentation_language: pt-br
  git_scopes:
    - { scope: "domain", area: "Core domain logic" }
    - { scope: "infra", area: "Infrastructure and adapters" }
    - { scope: "api", area: "REST/gRPC API layer" }
    - { scope: "events", area: "Event producers and consumers" }
    - { scope: "config", area: "Configuration and properties" }

skills:
  override: auto
agents:
  override: auto
  adaptive_model:
    junior: "haiku"
    mid: "sonnet"
    senior: "opus"
hooks:
  post_compile: true
settings:
  auto_generate: true

mcp:
  servers:
    - id: "firecrawl-mcp"
      url: "https://mcp.firecrawl.dev"
      capabilities:
        - scrape
        - crawl
        - search
      env:
        FIRECRAWL_API_KEY: "$FIRECRAWL_API_KEY"
```

### Python FastAPI with DDD

```yaml
project:
  name: "payment-gateway"
  purpose: "Payment processing and reconciliation service"

architecture:
  style: microservice
  domain_driven: true
  event_driven: true

interfaces:
  - type: rest
    spec: openapi
  - type: websocket
  - type: event-consumer
    broker: kafka
  - type: event-producer
    broker: kafka

language:
  name: python
  version: "3.12"

framework:
  name: fastapi
  version: ""
  build_tool: pip
  native_build: false

data:
  database:
    type: postgresql
    version: "17"
    migration: alembic
  cache:
    type: redis
    version: "7.4"
  message_broker:
    type: kafka
    version: "3.7"

security:
  compliance:
    - lgpd
    - pci-dss
  encryption:
    at_rest: true
    key_management: vault
  pentest_readiness: true

cloud:
  provider: none

infrastructure:
  container: docker
  orchestrator: kubernetes
  templating: helm
  iac: terraform
  registry: ghcr
  api_gateway: kong
  service_mesh: none

observability:
  standard: opentelemetry
  backend: grafana-stack

testing:
  smoke_tests: true
  performance_tests: true
  contract_tests: true
  chaos_tests: false

conventions:
  code_language: en
  commit_language: en
  documentation_language: en
  git_scopes:
    - { scope: "domain", area: "Core domain logic" }
    - { scope: "api", area: "REST/WebSocket API layer" }
    - { scope: "events", area: "Event producers and consumers" }
    - { scope: "config", area: "Configuration and settings" }
```

### TypeScript NestJS with GraphQL

```yaml
project:
  name: "catalog-service"
  purpose: "Product catalog with full-text search and recommendations"

architecture:
  style: microservice
  domain_driven: true
  event_driven: true

interfaces:
  - type: rest
    spec: openapi
  - type: graphql
    spec: code-first
  - type: websocket
  - type: event-consumer
    broker: kafka
  - type: event-producer
    broker: kafka

language:
  name: typescript
  version: "5"

framework:
  name: nestjs
  version: ""
  build_tool: npm

data:
  database:
    type: postgresql
    version: "17"
    migration: prisma
  cache:
    type: redis
    version: "7.4"
  message_broker:
    type: kafka
    version: "3.7"

security:
  compliance:
    - gdpr
  encryption:
    at_rest: true
    key_management: vault
  pentest_readiness: true

infrastructure:
  container: docker
  orchestrator: kubernetes
  templating: kustomize
  iac: terraform
  registry: ghcr
  api_gateway: kong
  service_mesh: none

observability:
  standard: opentelemetry
  backend: grafana-stack

testing:
  smoke_tests: true
  performance_tests: true
  contract_tests: true
  chaos_tests: false
```

### Go Microservice (Composition-Based)

```yaml
project:
  name: "ingestion-service"
  purpose: "High-throughput data ingestion pipeline"

architecture:
  style: microservice
  domain_driven: false         # Go uses packages for domain isolation
  event_driven: true

interfaces:
  - type: rest
    spec: openapi
  - type: grpc
    spec: proto3
  - type: event-consumer
    broker: kafka
  - type: event-producer
    broker: kafka

language:
  name: go
  version: "1.22"

framework:
  name: gin
  version: ""
  build_tool: go-mod

data:
  database:
    type: postgresql
    version: "17"
    migration: none            # Go uses golang-migrate or goose
  cache:
    type: redis
    version: "7.4"
  message_broker:
    type: kafka
    version: "3.7"

infrastructure:
  container: docker
  orchestrator: kubernetes
  templating: kustomize
  iac: terraform
  api_gateway: kong

observability:
  standard: opentelemetry
  backend: grafana-stack

testing:
  smoke_tests: true
  performance_tests: true
  contract_tests: false
  chaos_tests: true
```

### Rust High-Performance Service

```yaml
project:
  name: "matching-engine"
  purpose: "Low-latency order matching engine"

architecture:
  style: microservice
  domain_driven: false         # Rust uses traits + modules for boundaries
  event_driven: true

interfaces:
  - type: rest
    spec: openapi
  - type: grpc
    spec: proto3
  - type: event-consumer
    broker: kafka
  - type: event-producer
    broker: kafka

language:
  name: rust
  version: "2024"

framework:
  name: axum
  version: ""
  build_tool: cargo

data:
  database:
    type: postgresql
    version: "17"
    migration: none            # Rust uses sqlx or diesel migrations
  cache:
    type: redis
    version: "7.4"
  message_broker:
    type: kafka
    version: "3.7"

infrastructure:
  container: docker
  orchestrator: kubernetes
  templating: kustomize
  iac: terraform

observability:
  standard: opentelemetry
  backend: grafana-stack

testing:
  smoke_tests: true
  performance_tests: true
  contract_tests: false
  chaos_tests: true
```

### CLI Tool (Minimal)

```yaml
project:
  name: "my-cli"
  purpose: "Developer productivity CLI tool"

architecture:
  style: library
  domain_driven: false
  event_driven: false

interfaces:
  - type: cli

language:
  name: python
  version: "3.9"

framework:
  name: click
  version: "8.1"
  build_tool: pip

data:
  database:
    type: none
  cache:
    type: none
  message_broker:
    type: none

security:
  compliance: []
  encryption:
    at_rest: false
    key_management: none
  pentest_readiness: false

infrastructure:
  container: docker
  orchestrator: none
  templating: none
  iac: none
  registry: none
  api_gateway: none
  service_mesh: none

observability:
  standard: none
  backend: none

testing:
  smoke_tests: true
  performance_tests: false
  contract_tests: false
  chaos_tests: false

hooks:
  post_compile: false

mcp:
  servers: []
```

### Java CLI Tool with Picocli

```yaml
project:
  name: "data-migrator"
  purpose: "Database migration and schema management CLI tool"

architecture:
  style: library
  domain_driven: true
  event_driven: false

interfaces:
  - type: cli

language:
  name: java
  version: "21"

framework:
  name: picocli
  version: "4.7"
  build_tool: maven
  native_build: false          # Set true for GraalVM native image

data:
  database:
    type: none
  cache:
    type: none
  message_broker:
    type: none

infrastructure:
  container: docker
  orchestrator: none

testing:
  smoke_tests: true

hooks:
  post_compile: true           # Java needs compilation check
```

### Spring Boot with Gradle

```yaml
project:
  name: "notification-service"
  purpose: "Multi-channel notification delivery service"

architecture:
  style: microservice
  domain_driven: true
  event_driven: true

interfaces:
  - type: rest
    spec: openapi
  - type: grpc
    spec: proto3
  - type: event-consumer
    broker: kafka
  - type: event-producer
    broker: kafka

language:
  name: java
  version: "21"

framework:
  name: spring-boot
  version: "3.x"
  build_tool: gradle
  native_build: true

data:
  database:
    type: postgresql
    version: "17"
    migration: flyway
  cache:
    type: redis
    version: "7.4"
  message_broker:
    type: kafka
    version: "3.7"

security:
  compliance:
    - lgpd
    - hipaa
  encryption:
    at_rest: true
    key_management: vault
  pentest_readiness: true

cloud:
  provider: azure

infrastructure:
  container: docker
  orchestrator: kubernetes
  templating: helm
  iac: terraform
  registry: acr
  api_gateway: traefik
  service_mesh: linkerd

observability:
  standard: opentelemetry
  backend: grafana-stack

testing:
  smoke_tests: true
  performance_tests: true
  contract_tests: true
  chaos_tests: true
```

### Kotlin Ktor with WebSocket

```yaml
project:
  name: "realtime-feed"
  purpose: "Real-time data streaming and WebSocket feed service"

architecture:
  style: microservice
  domain_driven: true
  event_driven: true

interfaces:
  - type: rest
    spec: openapi
  - type: websocket
  - type: event-consumer
    broker: kafka
  - type: event-producer
    broker: kafka

language:
  name: kotlin
  version: "2.0"

framework:
  name: ktor
  version: ""
  build_tool: gradle

data:
  database:
    type: postgresql
    version: "17"
    migration: flyway
  cache:
    type: redis
    version: "7.4"
  message_broker:
    type: kafka
    version: "3.7"

infrastructure:
  container: docker
  orchestrator: kubernetes
  templating: kustomize
  iac: terraform
  api_gateway: kong

observability:
  standard: opentelemetry
  backend: grafana-stack

testing:
  smoke_tests: true
  performance_tests: true
  contract_tests: true
  chaos_tests: false
```

---

## Generated Output

The tool generates the following directory structure in the output directory:

```
output/
├── .claude/
│   ├── rules/                    # Coding standards, architecture rules
│   │   ├── 01-project-identity.md
│   │   ├── 02-domain.md
│   │   ├── 03-coding-standards.md
│   │   ├── 04-architecture-summary.md
│   │   └── 05-quality-gates.md
│   ├── skills/                   # Slash commands (lazy-loaded)
│   │   ├── coding-standards/
│   │   ├── architecture/
│   │   ├── testing/
│   │   ├── security/
│   │   ├── api-design/
│   │   ├── observability/
│   │   ├── x-dev-implement/
│   │   ├── x-review/
│   │   └── ...
│   ├── agents/                   # AI personas (architect, qa, security, etc.)
│   ├── hooks/                    # Post-compile automation
│   └── settings.json             # Permissions and hook configuration
├── .github/
│   ├── copilot-instructions.md   # Global Copilot instructions
│   ├── instructions/             # Contextual instructions
│   ├── skills/                   # Copilot skills
│   ├── agents/                   # Copilot agent definitions
│   ├── prompts/                  # Prompt templates
│   └── hooks/                    # Event hooks
├── .codex/                       # OpenAI Codex configuration
├── .agents/                      # Shared agent definitions
├── CLAUDE.md                     # Executive summary for Claude Code
├── AGENTS.md                     # Agent reference
├── docs/
│   ├── stories/                  # Story templates
│   ├── epics/                    # Epic templates
│   ├── adrs/                     # Architecture Decision Records
│   └── threat-models/            # Threat model templates
├── Dockerfile                    # Multi-stage Docker build
├── docker-compose.yaml           # Local development stack
└── k8s/                          # Kubernetes manifests
    ├── base/
    └── overlays/
```

> The exact files generated depend on the configuration. CLI profiles generate fewer
> infrastructure files. Microservice profiles include the full stack.

---

## Advanced Usage

### Dry-run mode

Preview what files would be generated without writing anything:

```bash
ia-dev-env generate --config my-config.yaml --dry-run
```

### Force overwrite

Overwrite existing artifact directories without prompting:

```bash
ia-dev-env generate --config my-config.yaml --output my-project/ --force
```

### Verbose output

Show detailed pipeline execution information:

```bash
ia-dev-env generate --config my-config.yaml --output my-project/ --verbose
```

### Interactive mode

Guided configuration via terminal prompts:

```bash
ia-dev-env generate --interactive
```

### Validate before generating

Check your config for errors without producing output:

```bash
ia-dev-env validate --config my-config.yaml
ia-dev-env validate --config my-config.yaml --verbose
```

---

## Environment Variables

| Variable | Description |
|----------|-------------|
| `JAVA_HOME` | Path to Java installation (auto-detected if on PATH) |
| `IA_DEV_ENV_JAVA_OPTS` | Extra JVM options (e.g., `-Xmx512m`) |

Example:

```bash
IA_DEV_ENV_JAVA_OPTS="-Xmx512m" ia-dev-env generate \
  --stack java-quarkus --output my-project/
```

---

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

---

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

---

## Project Structure

```
java/
├── pom.xml
├── install.sh                 # Install script (macOS/Linux)
├── install.ps1                # Install script (Windows)
├── bin/
│   └── ia-dev-env             # Dev wrapper script
├── src/
│   ├── main/
│   │   ├── java/dev/iadev/
│   │   │   ├── assembler/     # 23 assemblers + pipeline
│   │   │   ├── cli/           # Picocli commands
│   │   │   ├── config/        # Config loading + profiles
│   │   │   ├── domain/        # Stack resolution + validation
│   │   │   ├── exception/     # Custom exceptions
│   │   │   ├── model/         # Data classes (records)
│   │   │   ├── template/      # Pebble template engine
│   │   │   └── util/          # Path security, file utils
│   │   └── resources/
│   │       ├── targets/           # Target-specific (claude, github-copilot, codex)
│   │       ├── knowledge/         # Shared knowledge base
│   │       ├── shared/            # Cross-cutting templates (config, cicd, docs)
│   │       └── readme-template.md # README generation template
│   └── test/
│       ├── java/dev/iadev/        # Test classes
│       └── resources/golden/      # Golden file fixtures
└── target/
    └── ia-dev-env-2.0.0-SNAPSHOT.jar  # Fat JAR (~7.7 MB)
```
