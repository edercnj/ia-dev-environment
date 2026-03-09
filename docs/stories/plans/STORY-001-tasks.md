# Task Breakdown: STORY-001 -- Scaffolding and Domain Models

**Story:** STORY-001 -- Scaffolding do Projeto e Modelos de Dominio
**Stack:** Python 3.9 + Click 8.1 (library architecture)
**Total Tasks:** 22
**Groups:** G1-G7 (parallelizable within each group; groups are sequential)

---

## Group Dependencies

```
G1 (Foundation) → G2 (Domain Models) → G3 (Factory Methods) → G4 (Entry Point)
                                                                      ↓
                                                              G5 (Unit Tests)
                                                                      ↓
                                                          G6 (Integration Tests)
                                                                      ↓
                                                          G7 (Final Verification)
```

G2 and G4 can run in parallel once G1 is complete.
G3 depends on G2.
G5 depends on G3 and G4.
G6 depends on G5.
G7 depends on G6.

---

## G1: Foundation (Package Structure and Build Config)

| Task ID | Description | File | Complexity |
|---------|-------------|------|------------|
| T-001 | Create `pyproject.toml` with project metadata, dependencies (`click>=8.1,<9.0`, `pyyaml>=6.0,<7.0`, `jinja2>=3.1,<4.0`), dev dependencies (`pytest>=7.0`, `pytest-cov>=4.0`), `[project.scripts]` entry point, pytest and coverage config. `requires-python = ">=3.9"`. Build backend: `setuptools`. | `pyproject.toml` | Low |
| T-002 | Create `ia_dev_env/__init__.py` with `__version__ = "0.1.0"` constant. | `ia_dev_env/__init__.py` | Low |
| T-003 | Create `ia_dev_env/assembler/__init__.py` as empty placeholder package for future assembler implementations (STORY-005+). | `ia_dev_env/assembler/__init__.py` | Low |
| T-004 | Create `tests/__init__.py` as empty test package init. | `tests/__init__.py` | Low |

**Parallelism:** T-001 through T-004 are independent and can all be created in parallel.

---

## G2: Domain Models (Dataclasses)

All dataclasses go in `ia_dev_env/models.py`. File must start with `from __future__ import annotations` for Python 3.9 forward-reference compatibility. Use `typing.List` and `typing.Optional` (not `list[X]` or `X | None` syntax at runtime). Use `field(default_factory=list)` for mutable defaults.

| Task ID | Description | File | Complexity |
|---------|-------------|------|------------|
| T-005 | Create `ProjectIdentity` dataclass with fields: `name: str`, `purpose: str`. | `ia_dev_env/models.py` | Low |
| T-006 | Create `ArchitectureConfig` dataclass with fields: `style: str`, `domain_driven: bool`, `event_driven: bool`. | `ia_dev_env/models.py` | Low |
| T-007 | Create `InterfaceConfig` dataclass with fields: `type: str`, `spec: str = ""`, `broker: str = ""`. | `ia_dev_env/models.py` | Low |
| T-008 | Create `LanguageConfig` dataclass with fields: `name: str`, `version: str`. | `ia_dev_env/models.py` | Low |
| T-009 | Create `FrameworkConfig` dataclass with fields: `name: str`, `version: str`, `build_tool: str`, `native_build: bool`. | `ia_dev_env/models.py` | Low |
| T-010 | Create `TechComponent` dataclass with fields: `type: str`, `version: str = ""`. | `ia_dev_env/models.py` | Low |
| T-011 | Create `DataConfig` dataclass with fields: `database: TechComponent`, `migration: str = "none"`, `cache: TechComponent`, `message_broker: TechComponent`. Use `field(default_factory=...)` for `TechComponent` defaults with `type="none"`. | `ia_dev_env/models.py` | Low |
| T-012 | Create `EncryptionConfig` dataclass with fields: `at_rest: bool = False`, `key_management: str = "none"`. | `ia_dev_env/models.py` | Low |
| T-013 | Create `SecurityConfig` dataclass with fields: `compliance: List[str]` (default_factory=list), `encryption: EncryptionConfig` (default_factory), `pentest_readiness: bool = False`. | `ia_dev_env/models.py` | Low |
| T-014 | Create `CloudConfig` dataclass with field: `provider: str = "none"`. | `ia_dev_env/models.py` | Low |
| T-015 | Create `InfraConfig` dataclass with fields: `container: str = "docker"`, `orchestrator: str = "none"`, `templating: str = "none"`, `iac: str = "none"`, `registry: str = "none"`, `api_gateway: str = "none"`, `service_mesh: str = "none"`. | `ia_dev_env/models.py` | Low |
| T-016 | Create `ObservabilityConfig` dataclass with fields: `standard: str = "none"`, `backend: str = "none"`. | `ia_dev_env/models.py` | Low |
| T-017 | Create `TestingConfig` dataclass with fields: `smoke_tests: bool = True`, `performance_tests: bool = False`, `contract_tests: bool = False`, `chaos_tests: bool = False`. | `ia_dev_env/models.py` | Low |
| T-018 | Create `DomainConfig` dataclass with field: `template: str = "none"`. Create `GitScope` dataclass with fields: `scope: str`, `area: str`. Create `ConventionsConfig` dataclass with fields: `code_language: str = "en"`, `commit_language: str = "en"`, `documentation_language: str = "en"`, `git_scopes: List[GitScope]` (default_factory=list). Create `AdaptiveModel` dataclass with fields: `junior: str = "haiku"`, `mid: str = "sonnet"`, `senior: str = "opus"`. Create `SkillsConfig` dataclass with field: `override: str = "auto"`. Create `AgentsConfig` dataclass with fields: `override: str = "auto"`, `adaptive_model: AdaptiveModel` (default_factory). Create `HooksConfig` dataclass with field: `post_compile: bool = False`. Create `SettingsConfig` dataclass with field: `auto_generate: bool = True`. | `ia_dev_env/models.py` | Medium |
| T-019 | Create `ProjectConfig` aggregate root dataclass with fields: `project: ProjectIdentity`, `architecture: ArchitectureConfig`, `interfaces: List[InterfaceConfig]`, `language: LanguageConfig`, `framework: FrameworkConfig`, `data: DataConfig` (default_factory), `infrastructure: InfraConfig` (default_factory), `security: SecurityConfig` (default_factory), `cloud: CloudConfig` (default_factory), `observability: ObservabilityConfig` (default_factory), `testing: TestingConfig` (default_factory), `domain: DomainConfig` (default_factory), `conventions: ConventionsConfig` (default_factory), `skills: SkillsConfig` (default_factory), `agents: AgentsConfig` (default_factory), `hooks: HooksConfig` (default_factory), `settings: SettingsConfig` (default_factory). | `ia_dev_env/models.py` | Medium |

**Parallelism:** T-005 through T-019 are all in the same file. They must be implemented sequentially within a single file write, but logically represent distinct model concerns. Simpler models (T-005 through T-017) can be written first, then compound models (T-018, T-019).

---

## G3: Factory Methods (`from_dict()`)

| Task ID | Description | File | Complexity |
|---------|-------------|------|------------|
| T-020 | Add `from_dict(cls, data: dict) -> T` classmethod to every dataclass. Each method uses `data.get(key, default)` for optional fields and `data[key]` for required fields. `ProjectConfig.from_dict()` orchestrates full deserialization by calling child `from_dict()` methods. `InterfaceConfig` must handle a list of dicts. `DataConfig.from_dict()` must construct nested `TechComponent` instances. `SecurityConfig.from_dict()` must construct nested `EncryptionConfig`. `AgentsConfig.from_dict()` must construct nested `AdaptiveModel`. `ConventionsConfig.from_dict()` must construct nested `GitScope` list. Wrap required-field `KeyError` with a descriptive error message. | `ia_dev_env/models.py` | High |

**Parallelism:** Single task, depends on all G2 tasks.

---

## G4: Entry Point (Click CLI)

| Task ID | Description | File | Complexity |
|---------|-------------|------|------------|
| T-021 | Create `ia_dev_env/__main__.py` with a minimal Click entry point. Import `click` and `ia_dev_env.__version__`. Define `@click.group()` or `@click.command()` decorated `main()` function with `--version` option (using `click.version_option`). Include `if __name__ == "__main__": main()` guard. Must satisfy `python -m ia_dev_env --help` returning exit code 0. | `ia_dev_env/__main__.py` | Low |

**Parallelism:** Independent of G2/G3. Can run in parallel with G2 once G1 is complete (only depends on `ia_dev_env/__init__.py` existing).

---

## G5: Unit Tests

| Task ID | Description | File | Complexity |
|---------|-------------|------|------------|
| T-022 | Create `tests/test_models.py` with comprehensive unit tests covering: (a) Direct instantiation of every dataclass with explicit arguments. (b) `from_dict()` with a full YAML-equivalent dict (all sections populated, interfaces with multiple items). (c) `from_dict()` with a minimal dict (only required fields: project, architecture, interfaces, language, framework) -- optional fields use defaults. (d) `from_dict()` error handling: missing required key raises descriptive error. (e) Default value verification for all optional fields. (f) Nested object construction (DataConfig.database is TechComponent, SecurityConfig.encryption is EncryptionConfig, etc.). Target: 100% line coverage on models.py, >= 90% branch coverage. | `ia_dev_env/tests/test_models.py` | High |

**Parallelism:** Single task. Depends on G3 (factory methods) and G4 (entry point exists for import validation).

---

## G6: Integration Tests

| Task ID | Description | File | Complexity |
|---------|-------------|------|------------|
| T-023 | Create `tests/test_integration.py` with integration tests: (a) Verify `pip install -e .` succeeds (subprocess call). (b) Verify `python -m ia_dev_env --help` returns exit code 0 and output contains usage info. (c) Verify `ia-dev-env --help` console script works (subprocess call). (d) Verify `ia-dev-env --version` outputs version string. | `ia_dev_env/tests/test_integration.py` | Medium |

**Parallelism:** Single task. Depends on G5 (unit tests passing first).

---

## G7: Final Verification

| Task ID | Description | File | Complexity |
|---------|-------------|------|------------|
| T-024 | Run full test suite with coverage: `pytest --cov=ia_dev_env --cov-report=html --cov-report=xml --cov-branch`. Verify: line coverage >= 95%, branch coverage >= 90%. Fix any gaps. No file creation -- validation only. | N/A (validation) | Medium |
| T-025 | Run linting/formatting checks. Verify no warnings. Ensure all imports are explicit (no wildcards). Verify `from __future__ import annotations` is present in `models.py`. Verify no mutable default arguments in dataclasses (all use `field(default_factory=...)`). No file creation -- validation only. | N/A (validation) | Low |

**Parallelism:** T-024 and T-025 can run in parallel.

---

## Summary

| Group | Tasks | Total Files | Can Parallelize With |
|-------|-------|-------------|---------------------|
| G1 | T-001 to T-004 | 4 | -- (first group) |
| G2 | T-005 to T-019 | 1 (models.py) | G4 |
| G3 | T-020 | 1 (models.py) | -- |
| G4 | T-021 | 1 (__main__.py) | G2 |
| G5 | T-022 | 1 (test_models.py) | -- |
| G6 | T-023 | 1 (test_integration.py) | -- |
| G7 | T-024 to T-025 | 0 (validation) | T-024 and T-025 parallel |

**Critical Path:** G1 -> G2 -> G3 -> G5 -> G6 -> G7

**Files Created (total: 8):**
1. `pyproject.toml`
2. `ia_dev_env/__init__.py`
3. `ia_dev_env/models.py`
4. `ia_dev_env/__main__.py`
5. `ia_dev_env/assembler/__init__.py`
6. `tests/__init__.py`
7. `tests/test_models.py`
8. `tests/test_integration.py`
