# ia-dev-environment

A **reusable, project-agnostic** generator for complete `.claude/` directories. Produces rules, skills, agents, hooks, settings, and documentation — everything a Claude Code project needs to enforce engineering standards from day one.

## Quick Start

### Prerequisites

- Python >= 3.9
- pip3

> **Note:** On macOS and some Linux distributions, the `pip` command may not exist or may point to a different Python version. Use `pip3` instead. Similarly, use `python3` instead of `python` if the latter is not available.

### Installation

```bash
# Clone the repository
git clone <repo-url>
cd ia-dev-environment

# Create and activate a virtual environment (recommended)
python3 -m venv .venv
source .venv/bin/activate    # macOS/Linux
# .venv\Scripts\activate     # Windows

# Install in editable mode (recommended for development)
pip3 install -e .

# Or install with dev dependencies (pytest, coverage)
pip3 install -e ".[dev]"
```

### Usage

#### Generate from a config file

```bash
# Use one of the bundled config profiles
ia-dev-env generate --config resources/config-templates/setup-config.python-fastapi.yaml --output-dir /path/to/your-project/.claude/

# Or use your own config file
ia-dev-env generate --config my-config.yaml --output-dir .claude/
```

#### Generate interactively

```bash
ia-dev-env generate --interactive --output-dir .claude/
```

#### Validate a config file (without generating output)

```bash
ia-dev-env validate --config my-config.yaml
```

#### Dry run (preview what would be generated)

```bash
ia-dev-env generate --config my-config.yaml --dry-run
```

#### Verbose output

```bash
ia-dev-env generate --config my-config.yaml --output-dir .claude/ --verbose
```

#### Custom resources directory

By default, the CLI auto-detects the `resources/` directory relative to the installed package. Override it with:

```bash
ia-dev-env generate --config my-config.yaml --resources-dir /path/to/resources --output-dir .claude/
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

### Bundled Config Profiles

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

## Development

### Project Structure

```
ia-dev-environment/
├── src/
│   └── ia_dev_env/            # Python package (src layout)
│       ├── __init__.py
│       ├── __main__.py        # CLI entry point (Click)
│       ├── config.py          # YAML config loading + v2→v3 migration
│       ├── models.py          # Dataclasses (ProjectConfig, PipelineResult, etc.)
│       ├── exceptions.py      # Custom exceptions
│       ├── interactive.py     # Interactive mode prompts
│       ├── template_engine.py # Jinja2 template engine
│       ├── utils.py           # Path resolution, logging utilities
│       ├── verifier.py        # Byte-for-byte output verification
│       ├── domain/            # Domain logic (stack resolution, validation)
│       └── assembler/         # Modular assemblers (rules, skills, agents, etc.)
├── resources/                 # Non-Python assets (templates, configs, rules)
│   ├── config-templates/      # YAML config profiles per stack
│   ├── core/                  # Core rule templates
│   ├── core-rules/            # Core rule definitions
│   ├── languages/             # Language-specific conventions
│   ├── frameworks/            # Framework-specific patterns
│   ├── patterns/              # Architecture patterns (22 files)
│   ├── protocols/             # Protocol conventions (8 files)
│   ├── security/              # Security rules + compliance frameworks
│   ├── databases/             # Database reference docs (22 files)
│   ├── cloud-providers/       # Cloud provider knowledge packs
│   ├── infrastructure/        # Infrastructure patterns
│   ├── templates/             # Domain templates
│   ├── agents-templates/      # Agent persona templates
│   ├── skills-templates/      # Skill templates
│   ├── hooks-templates/       # Hook script templates
│   ├── settings-templates/    # Permission fragment templates
│   └── readme-template.md     # README template
├── tests/                     # Test suite (923 tests)
│   ├── golden/                # Golden reference files (8 profiles)
│   ├── fixtures/              # Test fixtures
│   ├── assembler/             # Assembler unit tests
│   └── domain/                # Domain logic tests
├── scripts/
│   └── generate_golden.py     # Golden file regeneration script
├── pyproject.toml             # Build configuration
└── CHANGELOG.md
```

### Running Tests

```bash
# Run all tests
pytest

# Run with coverage report
pytest --cov

# Run with detailed coverage (show missing lines)
pytest --cov --cov-report=term-missing

# Run a specific test file
pytest tests/test_verifier.py

# Run tests matching a pattern
pytest -k "test_pipeline"

# Run with verbose output
pytest -v
```

### Coverage Targets

| Metric | Minimum | Current |
|--------|---------|---------|
| Line Coverage | >= 95% | 98.48% |
| Branch Coverage | >= 90% | 96.8% |

### Golden File Verification

Golden files are pre-generated reference outputs for all 8 config profiles. They ensure byte-for-byte compatibility after changes.

```bash
# Regenerate all golden files
python3 scripts/generate_golden.py

# Regenerate a specific profile
python3 scripts/generate_golden.py --profile python-fastapi

# Run byte-for-byte verification tests
pytest tests/test_byte_for_byte.py
```

### Build & Package

```bash
# Build the package
python3 -m build

# Install from built wheel
pip3 install dist/ia_dev_environment-0.1.0-py3-none-any.whl
```

## What's Generated

The generator produces a **complete `.claude/` directory** with 8 components:

```
.claude/
├── README.md               <- Auto-generated project guide
├── settings.json           <- Permissions and hooks (committed to git)
├── settings.local.json     <- Local overrides template (gitignored)
├── rules/                  <- Coding rules (<= 30 files, consolidated)
│   ├── 01-12-*.md          <- Core: universal engineering principles
│   ├── 13-protocol-conventions.md <- ALL protocols consolidated
│   ├── 14-architecture-patterns.md <- ALL patterns consolidated
│   ├── 15-security-principles.md <- Base + crypto + pentest consolidated
│   ├── 16-compliance-requirements.md <- ALL compliance consolidated (conditional)
│   ├── 20-25-*.md          <- Language: conventions + version features
│   ├── 30-{fw}-core.md     <- Framework: DI + config + web (consolidated)
│   ├── 31-{fw}-data.md     <- Framework: ORM + database (consolidated)
│   ├── 32-{fw}-operations.md <- Framework: testing + observability (consolidated)
│   └── 50-51-*.md          <- Domain: project identity + template
├── skills/                 <- Skills invocable via /command
│   ├── {core-skills}/      <- Always included (11 skills)
│   ├── {conditional}/      <- Feature-gated (up to 7 skills)
│   └── {knowledge-packs}/  <- Internal context packs (not user-invocable)
│       └── database-patterns/references/  <- DB + cache reference docs (auto-selected)
├── agents/                 <- AI personas used by skills
│   ├── {mandatory}.md      <- Always included (3 agents: architect, tech-lead, developer)
│   ├── {core-engineers}.md <- Always included (3 engineers: security, qa, performance)
│   └── {conditional}.md    <- Feature-gated (up to 4 engineers)
└── hooks/                  <- Automation scripts
    └── post-compile-check.sh  <- Auto-compile on file changes
```

| Component | Count | Description |
|-----------|-------|-------------|
| Rules | 12 core + 1 protocols + 1 patterns + 1-2 security + ~5 language + 3 framework + 2 domain | <= 30 consolidated rule files |
| Skills | 11 core + up to 9 conditional | `/command` invocable workflows |
| Knowledge Packs | Framework packs (10 frameworks) + infra packs (7 types) + database + cloud | Internal context for agents (not user-invocable) |
| Agents | 3 mandatory + 3 core + up to 4 conditional | AI personas for planning, implementation, review |
| Hooks | 1 (compiled languages) | Post-compile check on file changes |
| Settings | 2 files | Permissions (shared + local) |

## Architecture

### Four-Layer Rules

```
┌─────────────────────────────────────────────┐
│  CORE (Layer 1) — Files 01-11               │
│  Universal principles — no tech references  │
│  Clean code, SOLID, testing, git, arch,     │
│  API, security, observability, resilience,  │
│  infrastructure, database                    │
├─────────────────────────────────────────────┤
│  LANGUAGE (Layer 2) — Files 20-25           │
│  Language conventions + version features    │
│  e.g., java/common + java/java-21           │
├─────────────────────────────────────────────┤
│  FRAMEWORK (Layer 3) — Files 30-42          │
│  Framework implementation patterns          │
│  e.g., quarkus/common, spring-boot/common   │
├─────────────────────────────────────────────┤
│  DOMAIN (Layer 4) — Files 50+               │
│  Project-specific rules and business logic  │
│  Project identity + domain template         │
└─────────────────────────────────────────────┘
```

The 4-layer architecture separates **language** from **framework**, enabling:
- Multiple Java versions (11, 17, 21) independently
- Framework versioning (Quarkus 2.x vs 3.x, Spring Boot 2.7 vs 3.4)
- Content reuse (~40-50%) between frameworks on the same language

### Rule Numbering

| Range | Layer | Source |
|-------|-------|--------|
| 01-12 | Core | `core/` |
| 13 | Protocols | ALL protocols consolidated into single file |
| 14 | Patterns | ALL patterns consolidated into single file |
| 15 | Security | Base + crypto + pentest consolidated |
| 16 | Compliance | All compliance frameworks consolidated (conditional) |
| 20-22 | Language Common | `languages/{lang}/common/` |
| 24-25 | Language Version | `languages/{lang}/{lang}-{ver}/` |
| 30 | Framework Core | DI + config + web + resilience (consolidated) |
| 31 | Framework Data | ORM + database (consolidated) |
| 32 | Framework Operations | Testing + observability + native-build + infrastructure (consolidated) |
| 50 | Project Identity | Generated at setup time |
| 51 | Domain Template | `templates/` |

### Assembly Pipeline

The Python CLI implements a modular assembler pipeline:

```
ia-dev-env generate
├── Config Loading     <- YAML parsing + v2→v3 auto-migration
├── Stack Resolution   <- Language/framework compatibility validation
├── Rules Assembly     <- Core + language + framework + project identity + domain
├── Patterns Assembly  <- Select by architecture.style, consolidate
├── Protocols Assembly <- Select by interfaces, consolidate
├── Skills Assembly    <- Core + conditional (feature-gated) + knowledge packs
├── Agents Assembly    <- Mandatory + core engineers + conditional + developer
├── Hooks Assembly     <- Post-compile check (compiled languages only)
├── Settings Assembly  <- settings.json from permission fragments
├── README Assembly    <- Auto-generated project documentation
└── Verification       <- Cross-reference validation
```

### Rules Consolidation Strategy

Source files in the repository remain **modular** for maintainability (one concern per file). However, the generated `.claude/rules/` files are **consolidated** for context efficiency. The target is **30 or fewer rule files** for ANY project configuration.

Claude Code loads all rules into context for every conversation. More rules means less room for code, analysis, and conversation. The consolidation strategy keeps source files modular but merges them at generation time, giving you the best of both worlds.

#### Expected File Counts

```
Rules (loaded every conversation):
  MINIMAL PROJECT (library):          ~22 files, ~80KB
  TYPICAL MICROSERVICE:               ~25 files, ~120KB
  ENTERPRISE MICROSERVICE:            ~26 files, ~160KB

Knowledge packs (loaded on demand by skills/agents — NOT counted in rules budget):
  MAXIMUM CONFIG:                     6-8 packs, ~40-60KB additional (only when invoked)
```

#### Decision Matrix: Rule vs Knowledge Pack

| Criteria | Rule (`.claude/rules/`) | Knowledge Pack (`.claude/skills/`) |
|----------|------------------------|------------------------------------|
| Loaded when | Every conversation (system prompt) | Only when invoked by a skill/agent |
| Size target | Small, consolidated (<10KB per file) | Can be larger (reference docs, examples) |
| Use for | Coding standards, conventions, principles | Framework patterns, DB references, cloud mappings |
| Examples | SOLID, git workflow, security principles | Quarkus CDI patterns, PostgreSQL query optimization |
| Impact on context | Direct — reduces available tokens | Indirect — only loaded on demand |

## Supported Languages

| Language | Versions | Common Files | Version Files |
|----------|----------|--------------|---------------|
| Java | 11, 17, 21 | coding-conventions, testing-conventions, libraries | version-features |
| TypeScript | 5 | coding-conventions, testing-conventions, libraries | version-features |
| Python | 3.12 | coding-conventions, testing-conventions, libraries | version-features |
| Go | 1.22 | coding-conventions, testing-conventions, libraries | version-features |
| Kotlin | 2.0 | coding-conventions, testing-conventions, libraries | version-features |
| Rust | 2024 | coding-conventions, testing-conventions, libraries | version-features |
| C# | 12 | coding-conventions, testing-conventions, libraries | version-features |

## Supported Frameworks

| Framework | Language | Files | Knowledge Pack |
|-----------|----------|-------|----------------|
| Quarkus | Java | cdi, panache, resteasy, config, resilience, observability, testing, native-build, infrastructure, database | quarkus-patterns |
| Spring Boot | Java | di, jpa, web, config, resilience, observability, testing, native-build, infrastructure, database | spring-patterns |
| NestJS | TypeScript | di, prisma, web, config, testing | nestjs-patterns |
| Express | TypeScript | middleware, web, config, testing | express-patterns |
| FastAPI | Python | di, sqlalchemy, web, config, testing | fastapi-patterns |
| Django | Python | web, orm, config, testing | django-patterns |
| Gin | Go | middleware, web, config, testing | gin-patterns |
| Ktor | Kotlin | di, exposed, web, config, testing | ktor-patterns |
| Axum | Rust | web, config, testing | axum-patterns |
| dotnet | C# | di, ef, web, config, testing | dotnet-patterns |

### Compatibility Matrix

| Language | Compatible Frameworks |
|----------|----------------------|
| Java | quarkus, spring-boot |
| TypeScript | nestjs, express |
| Python | fastapi, django |
| Go | gin |
| Kotlin | ktor, spring-boot |
| Rust | axum |
| C# | dotnet |

## Supported Databases & Caches

The generator ships with **22 reference documents** in `resources/databases/` covering schema design, migration patterns, query optimization, and caching strategies.

### SQL Databases

| Database | Versions | Migration Tools | References |
|----------|----------|-----------------|------------|
| PostgreSQL | 14, 15, 16, 17 | Flyway, Liquibase | types-and-conventions, migration-patterns, query-optimization |
| Oracle | 19c, 21c, 23ai | Flyway, Liquibase | types-and-conventions, migration-patterns, query-optimization |
| MySQL/MariaDB | 8.0, 8.4, 9.x | Flyway, Liquibase | types-and-conventions, migration-patterns, query-optimization |

### NoSQL Databases

| Database | Versions | Migration Tools | References |
|----------|----------|-----------------|------------|
| MongoDB | 6.0, 7.0, 8.0 | Mongock, mongosh scripts | modeling-patterns, migration-patterns, query-optimization |
| Cassandra | 4.1, 5.0, DSE, ScyllaDB | CQL scripts, Cognitor | modeling-patterns, migration-patterns, query-optimization |

### Cache Systems

| Cache | Versions | Wire Protocol | References |
|-------|----------|---------------|------------|
| Redis | 7.0, 7.2, 7.4 | RESP3 | redis-patterns (data structures, Cluster vs Sentinel, Lua) |
| Dragonfly | 1.x | Redis-compatible | dragonfly-patterns (multi-thread, 25-40% less memory) |
| Memcached | 1.6 | Memcached text/binary | memcached-patterns (slab allocator, key-value only) |

## Configuration (YAML)

The v3 configuration structure (see bundled profiles in `resources/config-templates/` for full examples):

```yaml
# setup-config.yaml (v3)
project:
  name: "my-service"
  purpose: "Brief description"

architecture:
  style: microservice        # microservice | modular-monolith | monolith | library | serverless
  domain_driven: false       # true = DDD pattern (Anti-Corruption Layer)
  event_driven: false        # true = event patterns (Saga, Outbox, DLQ)

interfaces:
  - type: rest
    spec: openapi            # openapi | custom
  # - type: grpc
  #   spec: proto3
  # - type: event-consumer
  #   broker: kafka

language:
  name: java                 # java | typescript | python | go | kotlin | rust | csharp
  version: "21"

framework:
  name: quarkus
  version: "3.17"
  build_tool: maven          # maven | gradle | npm | pip | go-mod | cargo | dotnet
  native_build: true

data:
  database:
    type: postgresql         # postgresql | oracle | mysql | mongodb | cassandra | sqlite | none
    version: "17"
    migration: flyway
  cache:
    type: redis              # redis | dragonfly | memcached | none
    version: "7.4"
  message_broker:
    type: none               # kafka | rabbitmq | sqs | pulsar | nats | none

infrastructure:
  container: docker          # docker | podman | none
  orchestrator: kubernetes   # kubernetes | docker-compose | none
  templating: "kustomize"    # kustomize | helm | none
  iac: "terraform"           # terraform | crossplane | none
  registry: "ecr"            # ecr | acr | gcr | oci-registry | dockerhub | none
  api_gateway: "kong"        # kong | istio | aws-apigw | traefik | none
  service_mesh: "istio"      # istio | linkerd | none

observability:
  standard: opentelemetry
  backend: grafana-stack     # grafana-stack | elastic-stack | datadog | newrelic | custom

testing:
  smoke_tests: true
  performance_tests: true
  contract_tests: false
  chaos_tests: false

security:
  compliance:
    - "pci-dss"              # pci-dss | pci-ssf | lgpd | gdpr | hipaa | sox
  encryption:
    at_rest: "aes-256-gcm"
    in_transit: "tls-1.3"
    key_management: "vault"
  pentest_readiness: true

cloud:
  provider: "aws"            # aws | azure | gcp | oci | none

domain:
  template: "iso8583"        # iso8583 | open-banking | healthcare-fhir | ecommerce |
                             # saas-multitenant | telecom-tmf | insurance-acord | iot-telemetry

conventions:
  code_language: en
  commit_language: en
  documentation_language: pt-br
  git_scopes:
    - { scope: "auth", area: "Authentication module" }
```

### Migration Guide (v2 to v3)

The CLI auto-detects v2 configs and migrates them with a deprecation warning.

| v2 Field | v3 Field | Change |
|----------|----------|--------|
| `project.type` | _(removed)_ | Use `architecture.style` |
| `project.architecture` | _(removed)_ | Internal architecture (hexagonal) is always applied |
| _(new)_ | `architecture.style` | Required — defines deployment topology |
| `stack.protocols` (string array) | `interfaces` (object array) | Each entry is now `{type, spec?, broker?}` |
| `stack.database` | `data.database` | Moved under `data` section, added `version` |
| `stack.cache` | `data.cache` | Moved under `data` section, added `version` |

## Architecture Styles

| Style | Description | Patterns Included |
|-------|-------------|-------------------|
| `microservice` | Independent deployable service with own data store | All microservice, resilience, integration, data patterns |
| `modular-monolith` | Single deployment with strict module boundaries | Modular-monolith, selective resilience, data patterns |
| `monolith` | Traditional single deployment, shared DB | Data patterns, circuit-breaker |
| `library` | Reusable package/SDK, no runtime deployment | Minimal — repository (if DB), adapter |
| `serverless` | Function-based, event-triggered, managed infra | Resilience, integration patterns |

Cross-cutting flags:
- `domain_driven: true` — enables DDD pattern (Anti-Corruption Layer)
- `event_driven: true` — enables event patterns (Saga, Outbox, Event Store, DLQ, Event Sourcing)

## Security Configuration

### Base Security Rules (always included)

- **`application-security.md`** — OWASP Top 10, input validation, authentication/authorization patterns, session management, error handling, logging security events.
- **`cryptography.md`** — Encryption at rest and in transit, hashing algorithms, key management, secrets handling, certificate rotation.

### Compliance Frameworks

Selected via `security.compliance[]` in the YAML config.

| Framework | Config Value | What It Adds | Use When |
|-----------|-------------|--------------|----------|
| PCI-DSS | `pci-dss` | Cardholder data protection, audit trail, network segmentation | Processing credit card payments |
| PCI-SSF | `pci-ssf` | Secure software lifecycle, authentication controls | Building payment software |
| LGPD | `lgpd` | Brazilian data protection, consent management, data subject rights | Processing Brazilian personal data |
| GDPR | `gdpr` | EU data protection, privacy by design, right to be forgotten | Processing EU personal data |
| HIPAA | `hipaa` | PHI protection, minimum necessary standard, 6-year audit trail | Handling health information |
| SOX | `sox` | Change management, segregation of duties, financial audit trail | Financial reporting systems |

## Skills Catalog

### Core Skills (always included)

| Skill | Command | Description |
|-------|---------|-------------|
| x-dev-lifecycle | `/x-dev-lifecycle` | Orchestrates 8-phase feature implementation cycle |
| x-git-push | `/x-git-push` | Git operations: branch, commit, push, PR |
| x-lib-task-decomposer | `/x-lib-task-decomposer` | Decomposes plans into parallelizable tasks |
| x-lib-group-verifier | `/x-lib-group-verifier` | Build gate between task groups |
| x-dev-implement | `/x-dev-implement` | Implements feature following project conventions |
| x-test-run | `/x-test-run` | Runs tests with coverage reporting |
| x-ops-troubleshoot | `/x-ops-troubleshoot` | Diagnoses errors and build failures |
| x-review | `/x-review` | Parallel specialist review (Security, QA, Perf, etc.) |
| x-review-pr | `/x-review-pr` | Tech Lead holistic review (GO/NO-GO) |
| x-lib-audit-rules | `/x-lib-audit-rules` | Audits code compliance against rules |
| x-test-plan | `/x-test-plan` | Generates test plan before implementation |

### Conditional Skills (feature-gated)

| Skill | Command | Condition |
|-------|---------|-----------|
| x-review-api | `/x-review-api` | `rest` in protocols |
| instrument-otel | `/instrument-otel` | Always (observability always enabled) |
| setup-environment | `/setup-environment` | orchestrator != `none` |
| run-smoke-api | `/run-smoke-api` | smoke_tests + `rest` |
| run-smoke-socket | `/run-smoke-socket` | smoke_tests + `tcp-custom` |
| run-e2e | `/run-e2e` | Always available |
| run-perf-test | `/run-perf-test` | Always available |
| x-review-security | `/x-review-security` | Any item in `security.compliance[]` |
| x-review-gateway | `/x-review-gateway` | `infrastructure.api_gateway != none` |

## Agents Catalog

### Mandatory Agents (always included)

| Agent | Role | Model |
|-------|------|-------|
| Architect | Planner — creates implementation plans | Opus |
| Tech Lead | Approver — 40-point GO/NO-GO review | Adaptive |
| Developer | Implementer — writes production code | Adaptive |

### Core Engineers (always included)

| Agent | Checklist | Model |
|-------|-----------|-------|
| Security Engineer | 20-point security checklist | Adaptive |
| QA Engineer | 24-point quality checklist | Adaptive |
| Performance Engineer | 26-point performance checklist | Adaptive |

### Conditional Engineers (feature-gated)

| Agent | Condition |
|-------|-----------|
| Database Engineer | database != `none` OR cache != `none` |
| Observability Engineer | Always (observability always enabled) |
| DevOps Engineer | container or orchestrator != `none` |
| API Engineer | `rest` in protocols |

## Domain Templates

| Domain | Config Value | Use When |
|--------|-------------|----------|
| ISO 8583 | `iso8583` | Payment message authorizer/simulator |
| Open Banking | `open-banking` | PIX, BACEN APIs, Open Finance |
| Healthcare FHIR | `healthcare-fhir` | FHIR R4/R5, SMART on FHIR, HL7 |
| E-commerce | `ecommerce` | Catalog, cart, checkout, payments |
| SaaS Multi-tenant | `saas-multitenant` | Tenant isolation, billing, onboarding |
| Telecom TMF | `telecom-tmf` | TM Forum Open APIs, SID model |
| Insurance ACORD | `insurance-acord` | Policy lifecycle, claims, ACORD standards |
| IoT Telemetry | `iot-telemetry` | Device registry, MQTT, edge computing |

## Customization

After generating `.claude/`:

1. **Review `rules/50-project-identity.md`** — Update with your project specifics
2. **Fill in `rules/51-domain.md`** — Add your domain rules and business logic
3. **Customize git scopes** — Add domain-specific scopes to `rules/04-git-workflow.md`
4. **Review `settings.json`** — Verify permissions match your workflow
5. **Add local overrides** — Use `settings.local.json` for personal preferences

## License

MIT
