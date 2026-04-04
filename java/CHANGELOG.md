# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.1.0] - 2026-04-04

### Added

- Hexagonal Architecture (Ports & Adapters) with enforced layer boundaries
- Domain model layer (`domain/model/`) with 19 immutable records
- Input port interfaces (`domain/port/input/`): GenerateEnvironmentUseCase,
  ValidateConfigUseCase, ListStackProfilesUseCase
- Output port interfaces (`domain/port/output/`): StackProfileRepository,
  TemplateRenderer, FileSystemWriter, CheckpointStore, ProgressReporter
- Domain services (`domain/service/`) implementing input ports
- Infrastructure adapters for CLI input and 5 output concerns
- Composition root (`ApplicationFactory`) wiring all dependencies
- ArchUnit enforcement: 8 rules active with zero violations
- Domain-level `ConfigValidationException` for domain isolation
- ADR-001: Hexagonal Architecture Migration decision record

### Changed

- Package structure: flat -> hexagonal (domain, application, infrastructure)
- Domain model no longer depends on infrastructure exception package (RULE-001)
- Service architecture documentation updated with hexagonal structure

## [2.0.0] - 2026-03-19

### Added

- Complete rewrite from Node.js/TypeScript to Java 21
- CLI via Picocli with `generate` and `validate` subcommands
- Interactive mode with JLine terminal for guided configuration
- Template engine using Pebble with Jinja2/Nunjucks template compatibility
- 23 assemblers covering all output categories:
  - Core rules, skills, agents, hooks, settings
  - GitHub instructions, skills, agents, prompts, hooks
  - Codex configuration and agents
  - CI/CD workflows, Dockerfiles, docker-compose, Kubernetes manifests
  - Documentation templates (stories, epics, ADRs, threat models)
- 8 bundled stack profiles:
  - go-gin, java-quarkus, java-spring, kotlin-ktor
  - python-click-cli, python-fastapi, rust-axum, typescript-nestjs
- Stack validation with language-framework compatibility checks
- Path security: rejects dangerous output paths (home dir, root, etc.)
- Overwrite detection: warns when output directories already exist
- Dry-run mode: preview generated files without writing
- Force mode: overwrite existing artifacts
- Verbose mode: detailed pipeline execution output
- Fat JAR packaging via maven-shade-plugin
- GraalVM native image support via native-maven-plugin
- Wrapper script (`bin/ia-dev-env`) with Java 21 version detection
- Configuration loading from YAML files with cross-field validation
- Byte-for-byte output parity with TypeScript version for all 8 profiles
- Comprehensive test suite: 1900+ tests
- Test coverage: >= 95% line, >= 90% branch (enforced by JaCoCo)
- Golden file tests for all 8 profiles ensuring output stability

### Changed

- Runtime: Node.js/TypeScript -> Java 21
- Template engine: Nunjucks -> Pebble (with compatibility layer)
- CLI framework: Commander.js -> Picocli
- YAML parsing: js-yaml -> SnakeYAML
- Package structure: flat modules -> layered architecture
  (cli, config, domain, assembler, template, model, util, exception)

### Migration Notes

- Java 21 or later is now required
- The YAML configuration format is fully backward compatible
- All generated output is byte-for-byte identical to the TypeScript version
- The `--interactive` flag replaces the previous interactive wizard
- Environment variable `IA_DEV_ENV_JAVA_OPTS` can pass JVM options
