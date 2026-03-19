# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
- 9 bundled stack profiles:
  - go-gin, java-quarkus, java-spring, kotlin-ktor
  - python-click-cli, python-fastapi, rust-axum
  - typescript-nestjs, typescript-commander-cli
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
