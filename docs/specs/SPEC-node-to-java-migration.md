---
service: ia-dev-env
version: 2.0.0
runtime: Java 21 / Picocli 4.x
location: CLI
domain: Developer Tooling
---

# ia-dev-env Java — Migration from Node.js/TypeScript to Java 21

> **Source codebase:** `ia-dev-environment` (TypeScript 5 + Commander)
> **Target codebase:** `ia-dev-env-java` (Java 21 + Picocli + Pebble)

---

## 1. Visao Geral

### Objetivo

Migrate the entire `ia-dev-environment` CLI tool from Node.js/TypeScript to Java 21, preserving 100% functional parity with the existing TypeScript implementation. The tool generates `.claude/`, `.github/`, `.codex/`, and `.agents/` boilerplate for AI-assisted development environments. The Java version MUST produce byte-for-byte identical output to the TypeScript version for all 8 bundled profiles.

### Responsabilidades

- **CLI Interface (Picocli):** Parse commands (`generate`, `validate`), options (`--config`, `--interactive`, `--output-dir`, `--resources-dir`, `--verbose`, `--dry-run`, `--force`), and mutual exclusivity constraints
- **Config Loader (SnakeYAML):** Load YAML configuration files, validate required sections, map shorthand architecture types
- **Template Engine (Pebble):** Render Nunjucks/Jinja2-compatible templates with 25-field default context, placeholder replacement, and Python-style boolean formatting ("True"/"False")
- **Assembler Pipeline:** 23 sequential assemblers producing .claude/, .github/, docs/, .codex/, .agents/ artifacts
- **Domain Logic:** Stack resolution (commands, Docker images, health paths, ports), validation (language-framework compatibility, version requirements), knowledge pack routing, skill/agent selection via feature gates
- **Checkpoint System:** Epic execution state management with story status tracking, retry logic, block propagation, and JSON persistence
- **Implementation Map Parser:** Markdown-based dependency matrix parsing, DAG construction, cycle detection, phase computation, critical path analysis
- **Progress Reporting:** Execution metrics, ETA calculation, duration tracking, formatted output
- **Overwrite Detection:** Conflict detection for existing artifacts (.claude/, .github/, .codex/, .agents/, docs/)
- **Atomic Output:** Transactional file writing (temp dir → atomic rename/move)
- **Interactive Mode:** Guided project setup prompting (equivalent to Inquirer.js)
- **Dry-Run Mode:** Preview generation without writing files

### Stack Tecnologico

| Componente           | Tecnologia                                | Detalhes                                                                                      |
| :------------------- | :---------------------------------------- | :-------------------------------------------------------------------------------------------- |
| **Runtime**          | Java 21 (GraalVM-ready)                   | Stateless CLI. Distributed as fat JAR + optional native image via GraalVM                     |
| **CLI Framework**    | Picocli 4.7+                              | Subcommands, option parsing, mutual exclusivity, auto-help, auto-completion, ANSI colors      |
| **Template Engine**  | Pebble 3.2+                               | Jinja2-compatible syntax, custom filters for Python-style booleans, autoescape disabled       |
| **YAML Parser**      | SnakeYAML 2.x                             | YAML → Java object deserialization, type coercion                                             |
| **Build Tool**       | Maven 3.9+ (maven-wrapper)                | Multi-module optional; single module initially. Shade plugin for fat JAR                      |
| **Testing**          | JUnit 5 + AssertJ + Mockito               | Unit, integration, golden file tests. Coverage via JaCoCo (≥95% line, ≥90% branch)           |
| **Interactive UI**   | JLine 3.x                                 | Terminal prompts, checkbox/list selection (Inquirer.js equivalent)                             |
| **Logging**          | SLF4J + Logback                           | Structured logging, verbose toggle                                                            |
| **JSON**             | Jackson 2.x                               | Checkpoint state serialization/deserialization                                                |
| **File I/O**         | java.nio.file (NIO.2)                     | Atomic moves, temp directories, path normalization                                            |
| **Native Image**     | GraalVM Native Image (optional)           | AOT compilation for fast startup, reflection config for SnakeYAML/Jackson/Pebble              |

### Padroes Arquiteturais

- **Hexagonal Architecture (Ports & Adapters):** Domain logic isolated from CLI framework and I/O. Ports for template rendering, file system, config loading
- **Pipeline Pattern:** 23 assemblers executed sequentially, each implementing a common `Assembler` interface
- **Factory Pattern:** Model creation via static factory methods (`fromMap()`) for YAML deserialization
- **Strategy Pattern:** Conditional skill/agent/rule selection based on feature gates
- **Command Pattern:** CLI subcommands (generate, validate) as distinct command handlers
- **Template Method:** Each assembler follows the same `assemble()` contract with customized behavior

---

## 2. Regras de Negocio

### 2.1 Config Loading & Validation

**Required YAML Sections:**
- `project` (name, purpose)
- `architecture` (style, domainDriven, eventDriven)
- `interfaces` (type, spec, broker)
- `language` (name, version)
- `framework` (name, version, buildTool)

**Shorthand Mapping:**

| Shorthand | Architecture Style | Interface Type |
| :--- | :--- | :--- |
| `api` | microservice | rest |
| `cli` | library | cli |
| `library` | library | (none) |
| `worker` | microservice | event-consumer |
| `fullstack` | monolith | rest |

**Validations:**
- Language-framework compatibility (e.g., spring-boot requires java)
- Java 17+ required for Quarkus 3+ and Spring Boot 3+
- Python 3.10+ required for FastAPI, Click
- Valid architecture styles: microservice, monolith, library
- Valid interface types: rest, grpc, graphql, cli, event-consumer, event-producer, tcp, websocket

### 2.2 Stack Resolution

For each supported language-framework combination, resolve:

| Attribute | Resolution Source |
| :--- | :--- |
| `compileCmd` | LANGUAGE_COMMANDS mapping |
| `buildCmd` | LANGUAGE_COMMANDS mapping |
| `testCmd` | LANGUAGE_COMMANDS mapping |
| `coverageCmd` | LANGUAGE_COMMANDS mapping |
| `fileExtension` | LANGUAGE_COMMANDS mapping |
| `buildFile` | LANGUAGE_COMMANDS mapping |
| `packageManager` | LANGUAGE_COMMANDS mapping |
| `port` | FRAMEWORK_PORTS mapping |
| `healthPath` | FRAMEWORK_HEALTH_PATHS mapping |
| `dockerImage` | Language-specific base image template |

**Supported Stacks (8 profiles):**
1. java-quarkus (Java 21, Quarkus 3.17)
2. java-spring (Java 21, Spring Boot 3.4)
3. python-fastapi (Python 3.10+, FastAPI)
4. python-click-cli (Python 3.10+, Click)
5. go-gin (Go 1.21+, Gin)
6. kotlin-ktor (Kotlin 1.9+, Ktor)
7. typescript-nestjs (TypeScript 5, NestJS)
8. rust-axum (Rust 1.70+, Axum)

### 2.3 Assembler Pipeline (23 Assemblers)

**Execution Order (RULE-008):**

| # | Assembler | Output Directory | Description |
| :--- | :--- | :--- | :--- |
| 1 | RulesAssembler | .claude/rules/ | Core rules + conditional routing |
| 2 | SkillsAssembler | .claude/skills/ | Core, conditional, knowledge packs |
| 3 | AgentsAssembler | .claude/agents/ | Core, conditional, developer agents |
| 4 | PatternsAssembler | .claude/skills/patterns/ | Design patterns |
| 5 | ProtocolsAssembler | .claude/skills/protocols/ | Protocol specs |
| 6 | HooksAssembler | .claude/hooks/ | Automation hooks |
| 7 | SettingsAssembler | .claude/ | settings.json |
| 8 | GithubInstructionsAssembler | .github/instructions/ | Contextual instructions |
| 9 | GithubMcpAssembler | .github/ | MCP server config |
| 10 | GithubSkillsAssembler | .github/skills/ | GitHub Copilot skills |
| 11 | GithubAgentsAssembler | .github/agents/ | GitHub Copilot agents |
| 12 | GithubHooksAssembler | .github/hooks/ | GitHub hooks |
| 13 | GithubPromptsAssembler | .github/prompts/ | Prompt templates |
| 14 | DocsAssembler | docs/ | Documentation |
| 15 | GrpcDocsAssembler | docs/ | gRPC documentation |
| 16 | RunbookAssembler | (root) | RUNBOOK.md |
| 17 | CodexAgentsMdAssembler | (root) | AGENTS.md |
| 18 | CodexConfigAssembler | .codex/ | Codex config |
| 19 | CodexSkillsAssembler | .agents/ | Agent skills |
| 20 | DocsAdrAssembler | (root) | ADR documents |
| 21 | CicdAssembler | (root) | CI/CD pipelines |
| 22 | EpicReportAssembler | (root) | Epic execution reports |
| 23 | ReadmeAssembler | .claude/ | README.md |

### 2.4 Template Rendering Context

**25 Default Context Fields:**

| Field | Type | Source |
| :--- | :--- | :--- |
| `project_name` | String | config.project.name |
| `project_purpose` | String | config.project.purpose |
| `language_name` | String | config.language.name |
| `language_version` | String | config.language.version |
| `framework_name` | String | config.framework.name |
| `framework_version` | String | config.framework.version |
| `build_tool` | String | config.framework.buildTool |
| `architecture_style` | String | config.architecture.style |
| `domain_driven` | String (Python bool) | "True" / "False" |
| `event_driven` | String (Python bool) | "True" / "False" |
| `database_name` | String | config.data.database.name |
| `database_version` | String | config.data.database.version |
| `cache_name` | String | config.data.cache.name |
| `cache_version` | String | config.data.cache.version |
| `migration_tool` | String | config.data.migration.name |
| `interfaces_list` | String (comma-separated) | config.interfaces |
| `has_rest` | String (Python bool) | "True" / "False" |
| `has_grpc` | String (Python bool) | "True" / "False" |
| `has_graphql` | String (Python bool) | "True" / "False" |
| `has_events` | String (Python bool) | "True" / "False" |
| `native_build` | String (Python bool) | "True" / "False" |
| `smoke_tests` | String (Python bool) | "True" / "False" |
| `contract_tests` | String (Python bool) | "True" / "False" |
| `coverage_line` | Number | config.testing.coverageLine |
| `coverage_branch` | Number | config.testing.coverageBranch |

**CRITICAL:** Boolean values MUST be rendered as Python-style strings ("True"/"False") for backward compatibility with existing Jinja2/Nunjucks templates.

### 2.5 Feature Gates (Conditional Generation)

Skills, agents, and rules are conditionally included based on:
- Architecture style (microservice, monolith, library)
- Interface types (rest, grpc, graphql, events)
- Database presence
- Cache presence
- Security frameworks
- Observability configuration
- Infrastructure settings (container, orchestrator)

### 2.6 Checkpoint System (Epic Execution)

**Story Status Lifecycle:**
```
PENDING → IN_PROGRESS → SUCCESS
                      → FAILED → (retry) → IN_PROGRESS
                      → BLOCKED (dependency failed)
                      → PARTIAL
```

**Execution State Schema:**
- epicId, branch, startedAt, currentPhase, mode
- stories: Map<storyId, StoryEntry> with status, commitSha, phase, duration, retries, blockedBy, summary, findingsCount
- integrityGates: Map<gateId, IntegrityGateEntry> with status, timestamp, testCount, coverage, failedTests
- metrics: storiesCompleted, storiesTotal, estimatedRemainingMinutes, storiesFailed, storiesBlocked, elapsedMs, averageStoryDurationMs

### 2.7 Implementation Map Parser

**Input:** Markdown with dependency matrix table
**Output:** ParsedMap with DAG, phases, critical path

**Processing Pipeline:**
1. Parse markdown table → DependencyMatrixRow[]
2. Build DAG from rows → Map<storyId, DagNode>
3. Validate DAG (symmetry, cycles, roots) → errors or valid DAG
4. Compute phases from DAG → Map<phase, storyId[]>
5. Find critical path (longest dependency chain)
6. Support partial execution (phase, story, full modes)

### 2.8 Overwrite Detection

Check for existing directories before generation:
- `.claude/`
- `.github/`
- `.codex/`
- `.agents/`
- `docs/`

Unless `--force` flag is provided, abort with user-friendly error listing conflicts and suggesting `--force`.

### 2.9 Atomic Output

1. Write all files to a temporary directory
2. On success: atomically move temp → destination
3. On failure: keep temp directory for debugging, report error with temp path

---

## 3. Especificacoes de Plataforma

### 3.1 Path Safety

**Dangerous Path Rejection:**
- Home directory (`~`, `$HOME`)
- Root directory (`/`)
- System directories (`/usr`, `/etc`, `/var`, `/bin`, `/sbin`)
- Current directory without explicit flag

### 3.2 Resource Discovery

Auto-detect resources directory:
1. Check `--resources-dir` option
2. Check JAR internal resources (classpath)
3. Check relative to executable location
4. Fail with clear error if not found

### 3.3 Cross-Platform Compatibility

- Path separators: use `java.nio.file.Path` throughout (no hardcoded `/` or `\`)
- Line endings: generate LF (`\n`) on all platforms for consistency
- File encoding: UTF-8 for all files

### 3.4 GraalVM Native Image Support

- Reflection configuration for SnakeYAML, Jackson, Pebble
- Resource bundles registration
- Proxy configuration for template engine
- Build-time initialization where possible
- Native image testing in CI

---

## 4. Contratos de Dados

### 4.1 ProjectConfig (Root Aggregate)

| Campo | Tipo | Obrigatorio | Descricao |
| :--- | :--- | :--- | :--- |
| `project` | ProjectIdentity | Sim | Name and purpose |
| `architecture` | ArchitectureConfig | Sim | Style, DDD, event-driven flags |
| `interfaces` | List<InterfaceConfig> | Sim | Interface types (rest, grpc, cli) |
| `language` | LanguageConfig | Sim | Language name and version |
| `framework` | FrameworkConfig | Sim | Framework, version, build tool |
| `data` | DataConfig | Nao | Database, migration, cache |
| `security` | SecurityConfig | Nao | Security frameworks |
| `observability` | ObservabilityConfig | Nao | Observability tools |
| `infrastructure` | InfraConfig | Nao | Container, orchestrator, IaC |
| `testing` | TestingConfig | Nao | Test flags and coverage thresholds |
| `mcp` | McpConfig | Nao | MCP server configurations |

### 4.2 ProjectIdentity

| Campo | Tipo | Obrigatorio | Descricao |
| :--- | :--- | :--- | :--- |
| `name` | String | Sim | Project name (kebab-case) |
| `purpose` | String | Sim | One-line description |

### 4.3 ArchitectureConfig

| Campo | Tipo | Obrigatorio | Descricao |
| :--- | :--- | :--- | :--- |
| `style` | String | Sim | microservice, monolith, library |
| `domainDriven` | boolean | Nao | Enable DDD patterns (default: false) |
| `eventDriven` | boolean | Nao | Enable event patterns (default: false) |

### 4.4 InterfaceConfig

| Campo | Tipo | Obrigatorio | Descricao |
| :--- | :--- | :--- | :--- |
| `type` | String | Sim | rest, grpc, graphql, cli, event-consumer, etc. |
| `spec` | String | Nao | OpenAPI version, proto version |
| `broker` | String | Nao | Kafka, RabbitMQ (for event types) |

### 4.5 LanguageConfig

| Campo | Tipo | Obrigatorio | Descricao |
| :--- | :--- | :--- | :--- |
| `name` | String | Sim | java, python, go, kotlin, typescript, rust |
| `version` | String | Sim | Language version string |

### 4.6 FrameworkConfig

| Campo | Tipo | Obrigatorio | Descricao |
| :--- | :--- | :--- | :--- |
| `name` | String | Sim | Framework name |
| `version` | String | Nao | Framework version |
| `buildTool` | String | Nao | maven, gradle, npm, pip, cargo |
| `nativeBuild` | boolean | Nao | Native image support |

### 4.7 DataConfig

| Campo | Tipo | Obrigatorio | Descricao |
| :--- | :--- | :--- | :--- |
| `database` | TechComponent | Nao | Database name and version |
| `migration` | TechComponent | Nao | Migration tool name and version |
| `cache` | TechComponent | Nao | Cache name and version |

### 4.8 TechComponent

| Campo | Tipo | Obrigatorio | Descricao |
| :--- | :--- | :--- | :--- |
| `name` | String | Sim | Component name |
| `version` | String | Nao | Component version |

### 4.9 PipelineResult

| Campo | Tipo | Obrigatorio | Descricao |
| :--- | :--- | :--- | :--- |
| `success` | boolean | Sim | Pipeline completed successfully |
| `outputDir` | String | Sim | Output directory path |
| `filesGenerated` | List<String> | Sim | Generated file paths |
| `warnings` | List<String> | Sim | Non-fatal warnings |
| `durationMs` | long | Sim | Execution duration in milliseconds |

### 4.10 ExecutionState (Checkpoint)

| Campo | Tipo | Obrigatorio | Descricao |
| :--- | :--- | :--- | :--- |
| `epicId` | String | Sim | Epic identifier |
| `branch` | String | Sim | Git branch |
| `startedAt` | Instant | Sim | Execution start timestamp |
| `currentPhase` | int | Sim | Current execution phase |
| `mode` | ExecutionMode | Sim | parallel, skipReview |
| `stories` | Map<String, StoryEntry> | Sim | Story execution states |
| `integrityGates` | Map<String, IntegrityGateEntry> | Sim | Gate results |
| `metrics` | ExecutionMetrics | Sim | Aggregate metrics |

### 4.11 ParsedMap (Implementation Map)

| Campo | Tipo | Obrigatorio | Descricao |
| :--- | :--- | :--- | :--- |
| `stories` | Map<String, DagNode> | Sim | Story DAG nodes |
| `phases` | Map<Integer, List<String>> | Sim | Phase → story IDs |
| `criticalPath` | List<String> | Sim | Longest dependency chain |
| `totalPhases` | int | Sim | Total number of phases |
| `warnings` | List<String> | Sim | Parse warnings |

---

## 5. Jornadas (CLI Commands)

### 5.1 — Generate Command

**Command:** `ia-dev-env generate [options]`
**Protocol:** CLI (stdout/stderr)

#### Pre-condicoes
- Config file exists and is valid YAML (when `--config` provided)
- Output directory is writable
- Resources directory is discoverable

#### Options

| Option | Short | Required | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `--config <path>` | `-c` | Conditional | - | YAML config file path |
| `--interactive` | `-i` | Conditional | false | Interactive mode |
| `--output-dir <path>` | `-o` | No | "." | Output directory |
| `--resources-dir <path>` | `-s` | No | auto-detect | Resources directory |
| `--verbose` | `-v` | No | false | Enable verbose logging |
| `--dry-run` | - | No | false | Preview without writing |
| `--force` | `-f` | No | false | Overwrite existing artifacts |

**Mutual Exclusivity:** `--config` and `--interactive` cannot both be provided.

#### Flow

1. Parse CLI options
2. Load config (from file or interactive prompts)
3. Validate config (sections, compatibility, versions)
4. Check for existing artifacts (unless `--force`)
5. Build template context (25 fields)
6. Run 23 assemblers sequentially
7. Write output atomically (unless `--dry-run`)
8. Display summary table with file counts by category

#### Error Scenarios

| Scenario | Exit Code | Message |
| :--- | :--- | :--- |
| Config file not found | 1 | "Config file not found: <path>" |
| Invalid YAML syntax | 1 | "Failed to parse config: <details>" |
| Missing required section | 1 | "Missing required config section: <section>" |
| Invalid language-framework combo | 1 | "Framework <fw> requires language <lang>" |
| Existing artifacts without --force | 1 | "Existing artifacts found: <list>. Use --force to overwrite" |
| Output directory not writable | 1 | "Cannot write to output directory: <path>" |
| Template rendering error | 1 | "Template error in <file>: <details>" |

### 5.2 — Validate Command

**Command:** `ia-dev-env validate --config <path> [--verbose]`
**Protocol:** CLI (stdout/stderr)

#### Flow

1. Parse CLI options
2. Load YAML file
3. Validate all sections and compatibility rules
4. Report success or list all validation errors

#### Error Scenarios

| Scenario | Exit Code | Message |
| :--- | :--- | :--- |
| Config file not found | 1 | "Config file not found: <path>" |
| Validation errors | 1 | "Validation failed:\n- <error1>\n- <error2>" |
| Valid config | 0 | "Configuration is valid" |

---

## 6. Modulos e Pacotes (Java Package Structure)

### 6.1 Package Layout

```
com.iadevenv/
├── IaDevEnvApplication.java          # Main entry point
├── cli/                              # CLI layer (Picocli commands)
│   ├── GenerateCommand.java          # generate subcommand
│   ├── ValidateCommand.java          # validate subcommand
│   ├── CliDisplay.java               # Pipeline result formatting
│   └── InteractivePrompter.java      # JLine-based interactive mode
├── config/                           # Configuration loading
│   ├── ConfigLoader.java             # YAML → ProjectConfig
│   ├── StackMapping.java             # Language → command mappings
│   └── ConfigProfiles.java           # Bundled profile constants
├── model/                            # Domain models (17 classes)
│   ├── ProjectConfig.java            # Root aggregate
│   ├── ProjectIdentity.java
│   ├── ArchitectureConfig.java
│   ├── InterfaceConfig.java
│   ├── LanguageConfig.java
│   ├── FrameworkConfig.java
│   ├── DataConfig.java
│   ├── SecurityConfig.java
│   ├── ObservabilityConfig.java
│   ├── InfraConfig.java
│   ├── TestingConfig.java
│   ├── McpServerConfig.java
│   ├── McpConfig.java
│   ├── TechComponent.java
│   ├── PipelineResult.java
│   ├── FileDiff.java
│   └── ProjectFoundation.java
├── domain/                           # Business logic
│   ├── StackResolver.java            # Command, port, health path resolution
│   ├── StackValidator.java           # Language-framework validation
│   ├── VersionResolver.java          # Version parsing
│   ├── SkillRegistry.java            # Knowledge pack lists, infra rules
│   ├── CoreKpRouting.java            # Core KP routing logic
│   ├── PatternMapping.java           # Pattern → framework associations
│   ├── ProtocolMapping.java          # Protocol → spec associations
│   ├── StackPackMapping.java         # Framework → KP mapping
│   ├── failure/                      # Failure handling
│   │   ├── RetryEvaluator.java
│   │   └── BlockPropagator.java
│   ├── dryrun/                       # Dry-run planning
│   │   ├── DryRunPlan.java
│   │   ├── DryRunPlanner.java
│   │   └── DryRunFormatter.java
│   └── implmap/                      # Implementation map
│       ├── DependencyMatrixRow.java
│       ├── DagNode.java
│       ├── ParsedMap.java
│       ├── MarkdownParser.java
│       ├── DagBuilder.java
│       ├── DagValidator.java
│       ├── PhaseComputer.java
│       ├── CriticalPathFinder.java
│       ├── ExecutableStories.java
│       └── PartialExecution.java
├── assembler/                        # 23 assemblers + pipeline
│   ├── Assembler.java                # Interface
│   ├── AssemblerPipeline.java        # Sequential orchestrator
│   ├── RulesAssembler.java
│   ├── SkillsAssembler.java
│   ├── AgentsAssembler.java
│   ├── PatternsAssembler.java
│   ├── ProtocolsAssembler.java
│   ├── HooksAssembler.java
│   ├── SettingsAssembler.java
│   ├── GithubInstructionsAssembler.java
│   ├── GithubMcpAssembler.java
│   ├── GithubSkillsAssembler.java
│   ├── GithubAgentsAssembler.java
│   ├── GithubHooksAssembler.java
│   ├── GithubPromptsAssembler.java
│   ├── DocsAssembler.java
│   ├── GrpcDocsAssembler.java
│   ├── RunbookAssembler.java
│   ├── CodexAgentsMdAssembler.java
│   ├── CodexConfigAssembler.java
│   ├── CodexSkillsAssembler.java
│   ├── DocsAdrAssembler.java
│   ├── CicdAssembler.java
│   ├── EpicReportAssembler.java
│   ├── ReadmeAssembler.java
│   ├── conditions/                   # Feature gate evaluation
│   │   ├── ConditionEvaluator.java
│   │   ├── SkillsSelection.java
│   │   ├── AgentsSelection.java
│   │   └── RulesConditionals.java
│   └── helpers/                      # Shared assembler utilities
│       ├── CopyHelpers.java
│       ├── ReadmeTables.java
│       ├── ReadmeUtils.java
│       ├── Consolidator.java
│       └── Auditor.java
├── checkpoint/                       # Execution state management
│   ├── ExecutionState.java
│   ├── StoryEntry.java
│   ├── IntegrityGateEntry.java
│   ├── ExecutionMetrics.java
│   ├── StoryStatus.java              # Enum
│   ├── CheckpointEngine.java
│   ├── CheckpointValidation.java
│   └── ResumeHandler.java
├── progress/                         # Progress reporting
│   ├── MetricsCalculator.java
│   ├── ProgressFormatter.java
│   └── ProgressReporter.java
├── template/                         # Template engine
│   ├── TemplateEngine.java           # Pebble wrapper
│   ├── PythonBoolFilter.java         # "True"/"False" filter
│   └── ContextBuilder.java           # 25-field context builder
├── exception/                        # Custom exceptions
│   ├── CliException.java
│   ├── ConfigValidationException.java
│   ├── ConfigParseException.java
│   ├── PipelineException.java
│   ├── CheckpointValidationException.java
│   ├── CheckpointIOException.java
│   └── PartialExecutionException.java
└── util/                             # Utilities
    ├── PathUtils.java                # Path normalization, safety checks
    ├── AtomicOutput.java             # Transactional file writing
    ├── ResourceDiscovery.java        # Auto-detect resources directory
    └── OverwriteDetector.java        # Conflict detection
```

### 6.2 Module Dependencies

```
cli → config, model, assembler, template, util, exception
config → model, exception
domain → model, exception
assembler → domain, model, template, util, exception
checkpoint → model, exception
progress → checkpoint, model
template → model
util → exception
```

---

## 7. Dependencias

### Hard (Obrigatorias)

- **Java 21 JDK** — Runtime and compilation
- **Picocli 4.7+** — CLI framework (zero-dependency, annotation-based)
- **Pebble 3.2+** — Template engine (Jinja2 syntax compatibility)
- **SnakeYAML 2.x** — YAML config parsing
- **Jackson 2.x** — JSON serialization for checkpoint system
- **SLF4J 2.x + Logback** — Logging facade and implementation
- **JLine 3.x** — Interactive terminal prompts

### Dev Dependencies

- **JUnit 5.10+** — Test framework
- **AssertJ 3.x** — Fluent assertions
- **Mockito 5.x** — Mocking framework
- **JaCoCo 0.8.x** — Code coverage
- **Maven Shade Plugin** — Fat JAR packaging
- **Maven Surefire/Failsafe** — Test execution

### Optional

- **GraalVM Native Image** — AOT compilation for fast startup
- **Testcontainers** — Container-based integration testing (if needed)

---

## 8. Interfaces (Resumo de Endpoints)

### CLI Commands

| Command | Options | Description | Exit Codes |
| :--- | :--- | :--- | :--- |
| `ia-dev-env generate` | `-c, -i, -o, -s, -v, --dry-run, -f` | Generate boilerplate artifacts | 0 (success), 1 (error) |
| `ia-dev-env validate` | `-c, -v` | Validate config file | 0 (valid), 1 (invalid) |
| `ia-dev-env --help` | - | Display usage help | 0 |
| `ia-dev-env --version` | - | Display version | 0 |

---

## 9. Criterios de Aceitacao Globais

### 9.1 Byte-for-Byte Parity

The Java implementation MUST produce identical output to the TypeScript implementation for all 8 bundled profiles. Golden file tests from the TypeScript project MUST be reused as the acceptance criteria:
- `tests/golden/go-gin/`
- `tests/golden/java-quarkus/`
- `tests/golden/java-spring/`
- `tests/golden/kotlin-ktor/`
- `tests/golden/python-click-cli/`
- `tests/golden/python-fastapi/`
- `tests/golden/rust-axum/`
- `tests/golden/typescript-nestjs/`

### 9.2 Performance

- Startup time: < 500ms (JAR), < 50ms (native image)
- Generation time: < 2s for any profile (full pipeline)
- Memory usage: < 256MB peak during generation

### 9.3 Quality Gates

- Line coverage: ≥ 95%
- Branch coverage: ≥ 90%
- Zero compiler warnings
- All existing tests passing (ported from TypeScript)
- Golden file parity tests passing for all 8 profiles

### 9.4 Distribution

- Fat JAR (java -jar ia-dev-env.jar)
- Native image (./ia-dev-env) via GraalVM (optional, CI-built)
- Homebrew formula (optional)
- SDKMAN distribution (optional)

---

## 10. Riscos e Mitigacoes

| Risco | Probabilidade | Impacto | Mitigacao |
| :--- | :--- | :--- | :--- |
| Pebble template syntax divergence from Nunjucks/Jinja2 | Alta | Alto | Custom Pebble extensions, comprehensive template tests, consider Jinjava as alternative |
| Golden file parity failures due to whitespace/newline differences | Alta | Alto | Normalize line endings, use diff-based assertions with whitespace tolerance initially |
| SnakeYAML deserialization differences from js-yaml | Media | Alto | Unit tests for every model class with edge cases, custom deserializers if needed |
| GraalVM reflection issues with SnakeYAML/Jackson/Pebble | Media | Medio | Generate reflect-config.json early, test native image in CI |
| JLine interactive mode parity with Inquirer.js | Media | Medio | Focus on config-file mode first, interactive mode as separate story |
| Large resource bundle in JAR (templates, patterns, etc.) | Baixa | Medio | Efficient classpath resource loading, lazy loading |

---

## 11. Escopo Explicitamente Excluido

- **Python parity:** Only TypeScript→Java migration. Python reference is historical
- **New features:** No new features beyond what TypeScript version has
- **API server mode:** CLI only, no REST/gRPC server
- **Plugin system:** Not in initial scope
- **Multi-language templates:** Keep existing template format (Nunjucks/Jinja2 syntax)
- **Database/persistence:** No database needed (file-based only)
