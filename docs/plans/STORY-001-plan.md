# Implementation Plan: STORY-001 -- Scaffolding and Domain Models

**Story:** STORY-001 -- Scaffolding do Projeto e Modelos de Dominio
**Phase:** 0 (Foundation)
**Blocks:** STORY-002, STORY-003, STORY-004, STORY-005, STORY-006, STORY-007, STORY-008
**Architecture:** library (hexagonal-lite for a CLI tool)
**Stack:** Python 3.9 + Click 8.1

---

## 1. Affected Layers and Components

| Layer | Affected? | Rationale |
|-------|-----------|-----------|
| domain/model | YES | All dataclasses representing YAML config sections |
| domain/engine | NO | No business logic in this story |
| domain/port | NO | No ports needed yet (no adapters) |
| application | NO | No use cases in this story |
| adapter/inbound | PARTIAL | Only the `__main__.py` Click entry point (minimal) |
| adapter/outbound | NO | No persistence or external I/O |
| config | NO | No framework configuration needed |
| assembler | SCAFFOLD ONLY | Empty subpackage with `__init__.py` |
| tests | YES | Unit tests for all models |

---

## 2. New Classes/Interfaces to Create

### 2.1 Project Packaging

| File | Purpose |
|------|---------|
| `pyproject.toml` | Project metadata, dependencies, entry points |
| `claude_setup/__init__.py` | Package init (version constant) |

### 2.2 Domain Models (`claude_setup/models.py`)

All classes use `@dataclass` with `from __future__ import annotations` for Python 3.9 forward-reference compatibility. Each dataclass includes a `from_dict(cls, data: dict)` class method factory.

| Class | Description | Key Fields |
|-------|-------------|------------|
| `ProjectIdentity` | Project name and purpose | `name: str`, `purpose: str` |
| `ArchitectureConfig` | Architecture decisions | `style: str`, `domain_driven: bool`, `event_driven: bool` |
| `InterfaceConfig` | Single interface definition | `type: str`, `spec: str = ""`, `broker: str = ""` |
| `LanguageConfig` | Language selection | `name: str`, `version: str` |
| `FrameworkConfig` | Framework and build tool | `name: str`, `version: str`, `build_tool: str`, `native_build: bool` |
| `TechComponent` | Reusable name+version pair | `type: str`, `version: str = ""` |
| `DataConfig` | All data layer settings | `database: TechComponent`, `cache: TechComponent`, `message_broker: TechComponent` |
| `EncryptionConfig` | Encryption settings | `at_rest: bool`, `key_management: str` |
| `SecurityConfig` | Security configuration | `compliance: List[str]`, `encryption: EncryptionConfig`, `pentest_readiness: bool` |
| `CloudConfig` | Cloud provider | `provider: str` |
| `InfraConfig` | Infrastructure settings | `container: str`, `orchestrator: str`, `templating: str`, `iac: str`, `registry: str`, `api_gateway: str`, `service_mesh: str` |
| `ObservabilityConfig` | Monitoring setup | `standard: str`, `backend: str` |
| `TestingConfig` | Test toggles | `smoke_tests: bool`, `performance_tests: bool`, `contract_tests: bool`, `chaos_tests: bool` |
| `DomainConfig` | Domain template | `template: str` |
| `GitScope` | Single git scope entry | `scope: str`, `area: str` |
| `ConventionsConfig` | Code conventions | `code_language: str`, `commit_language: str`, `documentation_language: str`, `git_scopes: List[GitScope]` |
| `AdaptiveModel` | Model tier mapping | `junior: str`, `mid: str`, `senior: str` |
| `SkillsConfig` | Skills override | `override: str` |
| `AgentsConfig` | Agents configuration | `override: str`, `adaptive_model: AdaptiveModel` |
| `HooksConfig` | Hooks toggles | `post_compile: bool` |
| `SettingsConfig` | Settings toggles | `auto_generate: bool` |
| `ProjectConfig` | **Aggregate root** | All above as fields; `from_dict()` orchestrates full deserialization |

### 2.3 Entry Point (`claude_setup/__main__.py`)

| Component | Description |
|-----------|-------------|
| `main()` | Click group or command with `--help` support, `--version` flag |

Minimal implementation -- just enough to satisfy `python -m claude_setup --help` returning exit code 0.

### 2.4 Scaffolding (Empty Packages)

| Path | Purpose |
|------|---------|
| `claude_setup/assembler/__init__.py` | Assembler subpackage placeholder |
| `tests/__init__.py` | Test package init |
| `tests/test_models.py` | Unit tests for all dataclasses |

---

## 3. Existing Classes to Modify

**None.** This is a greenfield story. The `claude_setup/` package does not exist yet.

---

## 4. Dependency Direction Validation

```
claude_setup/models.py       -> standard library only (dataclasses, typing)
claude_setup/__main__.py     -> click (framework), claude_setup.models (domain)
tests/                        -> pytest, claude_setup.models
```

**Validation:**
- `models.py` (domain layer) has ZERO external dependencies -- only `dataclasses`, `typing`, and `__future__`. This satisfies the hexagonal golden rule.
- `__main__.py` (inbound adapter) depends on Click and domain models. This is the correct direction: adapter -> domain.
- No circular dependencies exist.
- `from __future__ import annotations` enables Python 3.9 forward-reference support without `typing.Optional` unions.

---

## 5. Integration Points

| Integration Point | Direction | Details |
|-------------------|-----------|---------|
| YAML dict input | Inbound | `from_dict()` accepts raw `dict` from YAML parsing (STORY-002) |
| Assembler interface | Outbound | `ProjectConfig` will be passed to assemblers (STORY-005 through STORY-008) |
| CLI entry point | Inbound | Click command invocation (expanded in STORY-009) |

**Contract for downstream stories:**
- All assemblers receive `ProjectConfig` as their primary input.
- `ProjectConfig.from_dict(data)` is the single entry point for config deserialization.
- Models are immutable after construction (dataclass with frozen=False but no mutation methods).

---

## 6. Database Changes

**Not applicable.** This is a CLI tool with no database (`data.database.type: none`).

---

## 7. API Changes

**Not applicable.** No REST/gRPC interfaces. The only interface is CLI.

**CLI contract (minimal for this story):**

```
claude-setup --help       -> Usage info, exit code 0
claude-setup --version    -> Version string, exit code 0
python -m claude_setup --help  -> Same as above
```

---

## 8. Event Changes

**Not applicable.** `event_driven: false` in project config.

---

## 9. Configuration Changes

### 9.1 `pyproject.toml`

```toml
[build-system]
requires = ["setuptools>=68.0", "wheel"]
build-backend = "setuptools.backends._legacy:_Backend"

[project]
name = "claude-setup"
version = "0.1.0"
requires-python = ">=3.9"
dependencies = [
    "click>=8.1,<9.0",
    "pyyaml>=6.0,<7.0",
    "jinja2>=3.1,<4.0",
]

[project.optional-dependencies]
dev = [
    "pytest>=7.0",
    "pytest-cov>=4.0",
]

[project.scripts]
claude-setup = "claude_setup.__main__:main"

[tool.pytest.ini_options]
testpaths = ["tests"]

[tool.coverage.run]
source = ["claude_setup"]
branch = true

[tool.coverage.report]
fail_under = 95
show_missing = true
```

### 9.2 Environment Variables

None required for this story.

---

## 10. Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| YAML schema drift across config templates | Medium | High | Derive models from ALL 8 config templates, not just the Click one. Union of all fields across templates. |
| Python 3.9 compatibility issues | Low | High | Use `from __future__ import annotations` everywhere. Avoid `match/case`, `X \| Y` type unions, `str.removeprefix()`. Use `typing.List`, `typing.Optional` for runtime type hints if needed by any library. |
| `from_dict()` fragility with missing keys | Medium | Medium | Use `dict.get(key, default)` for all optional fields. Required fields raise `KeyError` with descriptive message wrapping. |
| Downstream story coupling to model shape | High | High | This is the foundation story. Any model change after Phase 0 forces changes in STORY-002 through STORY-009. Mitigate by covering ALL YAML sections now, including `cloud`, `testing`, `conventions`, `skills`, `agents`, `hooks`, `settings`. |
| `dataclass` default mutable gotcha | Medium | Medium | Never use mutable defaults (`list`, `dict`) directly. Use `field(default_factory=list)` from `dataclasses`. |
| Entry point naming collision | Low | Low | Verify `claude-setup` script name does not collide with existing packages on PyPI. |

---

## Implementation Order

Execute sub-tasks in this sequence:

1. **Create `pyproject.toml`** -- project metadata, dependencies, entry points
2. **Create `claude_setup/__init__.py`** -- package init with `__version__`
3. **Create `claude_setup/models.py`** -- all dataclasses with `from_dict()` factories
4. **Create `claude_setup/__main__.py`** -- minimal Click entry point
5. **Create `claude_setup/assembler/__init__.py`** -- empty assembler package
6. **Create `tests/__init__.py`** -- test package init
7. **Create `tests/test_models.py`** -- unit tests for all models
8. **Validate** -- `pip install -e .`, `claude-setup --help`, `pytest --cov`

---

## File Tree (Final State)

```
claude_setup/
    __init__.py              # __version__ = "0.1.0"
    __main__.py              # Click entry point: main()
    models.py                # All dataclasses + from_dict() factories
    assembler/
        __init__.py          # Empty (placeholder for STORY-005+)
tests/
    __init__.py
    conftest.py              # Shared fixtures
    test_models.py           # Unit tests for all models
    test_cli.py              # CLI tests with CliRunner
    test_integration.py      # Integration tests with subprocess
pyproject.toml               # Build config, deps, scripts
```

---

## Acceptance Criteria Traceability

| Gherkin Scenario | Validated By |
|------------------|-------------|
| Package installation (`pip install -e .`) | Sub-task 8: manual + CI validation |
| `ProjectConfig.from_dict(data)` with full YAML | `test_models.py`: test with Java-Quarkus full config dict |
| `ProjectConfig.from_dict(data)` with minimal YAML | `test_models.py`: test with only required fields |
| `python -m claude_setup --help` exits 0 | `test_models.py` or integration test: subprocess call |
| Coverage >= 95% line, >= 90% branch | `pytest --cov` with `fail_under=95` in pyproject.toml |
