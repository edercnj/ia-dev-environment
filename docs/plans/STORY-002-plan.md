# Implementation Plan: STORY-002 -- Config Loading and Validation

**Story:** STORY-002 -- Carregamento e Validacao de Configuracao
**Phase:** 1 (Config Loading)
**Blocked By:** STORY-001 (models and scaffolding)
**Blocks:** STORY-009 (CLI orchestration)
**Architecture:** library (hexagonal-lite for a CLI tool)
**Stack:** Python 3.9 + Click 8.1

---

## 1. Affected Layers and Components

| Layer | Affected? | Rationale |
|-------|-----------|-----------|
| domain/model | MINOR | Add `ConfigValidationError` exception to models or a new exceptions module |
| domain/engine | YES | Core config loading, v2 detection, v2-to-v3 migration, validation logic |
| domain/port | NO | No ports needed -- config loading is a pure domain operation on dicts |
| application | NO | No use case orchestration yet; loading is invoked directly from CLI |
| adapter/inbound | YES | Interactive mode uses Click prompts (adapter concern), `__main__.py` gets `init` subcommand |
| adapter/outbound | NO | YAML file reading is stdlib (`pathlib.Path.read_text`) -- no adapter needed |
| config | NO | No framework configuration changes |
| tests | YES | Unit, contract, and integration tests for all config functions |

---

## 2. New Classes/Interfaces to Create

### 2.1 Domain -- Exceptions (`claude_setup/exceptions.py`)

| Class | Description |
|-------|-------------|
| `ConfigValidationError(Exception)` | Raised when required YAML sections are missing. Carries a `missing_fields: List[str]` attribute and a human-readable message listing all absent fields. |

**Rules:**
- Zero external dependencies (standard library only).
- Exception carries context values (the missing field names), per coding standards.
- Single class; avoids god-exception anti-pattern.

### 2.2 Domain -- Config Engine (`claude_setup/config.py`)

This is the core module. All functions operate on pure dicts and domain models. No Click or framework imports.

| Function | Signature | Description |
|----------|-----------|-------------|
| `load_config` | `(path: Path) -> ProjectConfig` | Reads YAML file, detects v2, migrates if needed, validates, returns model. |
| `detect_v2_format` | `(data: dict) -> bool` | Returns `True` if root-level `type` field matches v2 values (`api`, `cli`, `library`, `worker`, `fullstack`) OR if root-level `stack` key exists. |
| `migrate_v2_to_v3` | `(data: dict) -> dict` | Transforms a v2 dict into v3 structure. Maps `type` to `architecture.style` + `interfaces`, maps `stack` to `language` + `framework`. Returns a new dict (no mutation). |
| `validate_config` | `(data: dict) -> None` | Checks for required top-level sections: `project`, `architecture`, `interfaces`, `language`, `framework`. Raises `ConfigValidationError` listing all missing sections. |

**Internal constants (module-level):**

| Constant | Type | Description |
|----------|------|-------------|
| `V2_TYPE_MAPPING` | `Dict[str, Tuple[str, List[str]]]` | Maps v2 `type` values to `(architecture_style, interface_types)`. |
| `V2_STACK_MAPPING` | `Dict[str, Dict[str, str]]` | Maps v2 `stack` values to `{language_name, language_version, framework_name, framework_version}`. |
| `REQUIRED_SECTIONS` | `Tuple[str, ...]` | `("project", "architecture", "interfaces", "language", "framework")` |

**V2 Type Mapping (derived from `setup.sh` lines 435-464):**

```
"api"       -> ("microservice", ["rest"])
"cli"       -> ("library", ["cli"])
"library"   -> ("library", [])
"worker"    -> ("microservice", ["event-consumer"])
"fullstack" -> ("monolith", ["rest"])
```

**V2 Stack Mapping (derived from config templates):**

```
"java-quarkus"       -> language: java 21, framework: quarkus 3.17
"java-spring"        -> language: java 21, framework: spring-boot 3.4
"python-fastapi"     -> language: python 3.12, framework: fastapi 0.115
"python-click-cli"   -> language: python 3.9, framework: click 8.1
"go-gin"             -> language: go 1.23, framework: gin 1.10
"kotlin-ktor"        -> language: kotlin 2.1, framework: ktor 3.0
"typescript-nestjs"  -> language: typescript 5.7, framework: nestjs 10.4
"rust-axum"          -> language: rust 1.83, framework: axum 0.8
```

**Dependency validation:**
- Imports: `pathlib.Path`, `typing`, `yaml` (PyYAML -- already in `pyproject.toml` dependencies).
- Imports: `claude_setup.models.ProjectConfig` (domain model -- same layer).
- Imports: `claude_setup.exceptions.ConfigValidationError` (domain exception -- same layer).
- Does NOT import `click` or any framework code.
- PyYAML (`yaml.safe_load`) is a data-parsing library, not a framework. It is acceptable in the domain layer as a data format library analogous to `json` in stdlib.

### 2.3 Adapter Inbound -- Interactive Config (`claude_setup/interactive.py`)

This module lives in the adapter layer because it depends on Click (framework) for user interaction.

| Function | Signature | Description |
|----------|-----------|-------------|
| `run_interactive` | `() -> ProjectConfig` | Prompts user via Click for all required config fields. Returns a fully constructed `ProjectConfig`. |

**Internal helpers:**

| Function | Signature | Description |
|----------|-----------|-------------|
| `_prompt_select` | `(label: str, choices: List[str]) -> str` | Wraps `click.prompt` with `type=click.Choice(choices)`. |
| `_prompt_input` | `(label: str, default: str = "") -> str` | Wraps `click.prompt` for free-text input. |
| `_prompt_yesno` | `(label: str, default: bool = False) -> bool` | Wraps `click.confirm`. |

**Available choices for interactive prompts:**

| Field | Choices |
|-------|---------|
| Architecture style | `library`, `microservice`, `monolith` |
| Interface type | `rest`, `grpc`, `cli`, `event-consumer`, `event-producer` |
| Language | `java`, `python`, `go`, `kotlin`, `typescript`, `rust` |
| Framework (per language) | java: `quarkus`, `spring-boot`; python: `fastapi`, `click`, `django`, `flask`; go: `stdlib`, `gin`, `fiber`; kotlin: `ktor`; typescript: `nestjs`, `express`, `fastify`; rust: `axum`, `actix` |

**Dependency validation:**
- Imports `click` (framework -- allowed in adapter layer).
- Imports `claude_setup.models.*` (domain models -- adapter -> domain direction, correct).
- Does NOT import `claude_setup.config` (no cross-adapter dependency).

### 2.4 Tests

| File | Category | Coverage Target |
|------|----------|----------------|
| `tests/test_config.py` | Unit | `load_config`, `detect_v2_format`, `migrate_v2_to_v3`, `validate_config` |
| `tests/test_interactive.py` | Unit | `run_interactive` with mocked Click prompts |
| `tests/test_config_contract.py` | Contract | Parametrized: one row per v2 stack mapping, one row per v2 type mapping |

---

## 3. Existing Classes to Modify

| File | Change | Reason |
|------|--------|--------|
| `claude_setup/__main__.py` | Add `init` subcommand that accepts `--config PATH` option and calls `load_config()` or `run_interactive()` | Entry point for config loading flow |
| `claude_setup/models.py` | No changes required | `from_dict()` factories already handle dict-to-model conversion; `_require()` helper already raises `KeyError` with context |

**Note on `__main__.py` changes:**
The `init` subcommand is minimal in this story -- it loads config and echoes success. Full orchestration (calling assemblers) is deferred to STORY-009. The subcommand structure:

```python
@main.command()
@click.option("--config", "-c", type=click.Path(exists=True), default=None)
def init(config):
    if config:
        project_config = load_config(Path(config))
    else:
        project_config = run_interactive()
    click.echo(f"Loaded config for: {project_config.project.name}")
```

---

## 4. Dependency Direction Validation

```
claude_setup/exceptions.py   -> standard library only (DOMAIN)
claude_setup/config.py       -> yaml (data lib), claude_setup.models, claude_setup.exceptions (DOMAIN)
claude_setup/interactive.py  -> click (framework), claude_setup.models (ADAPTER -> DOMAIN, correct)
claude_setup/__main__.py     -> click, claude_setup.config, claude_setup.interactive (ADAPTER -> DOMAIN, correct)
tests/                       -> pytest, claude_setup.* (TEST -> all layers, correct)
```

**Validation checklist:**
- `config.py` (domain engine) has no Click imports. It uses `yaml` which is a data-format library, acceptable in domain.
- `interactive.py` (inbound adapter) depends on Click and domain models. Correct direction.
- `__main__.py` (inbound adapter) depends on both domain (`config.py`) and adapter (`interactive.py`). Both are inbound adapter; no outbound coupling.
- No circular dependencies exist.
- `config.py` does NOT import `interactive.py` (no adapter dependency from domain).

---

## 5. Integration Points

| Integration Point | Direction | Details |
|-------------------|-----------|---------|
| YAML file system | Inbound | `load_config()` reads file via `Path.read_text()` + `yaml.safe_load()` |
| `ProjectConfig.from_dict()` | Internal | `config.py` delegates final model construction to existing `from_dict()` factory from STORY-001 |
| Click CLI | Inbound | `__main__.py` `init` subcommand dispatches to `load_config()` or `run_interactive()` |
| Assemblers (future) | Outbound | `ProjectConfig` returned by this story is the input for STORY-005 through STORY-008 assemblers |
| STORY-009 orchestration | Downstream | STORY-009 will wire `init` subcommand to assembler pipeline |

**Contract guarantees:**
- `load_config(path)` always returns a valid `ProjectConfig` or raises `ConfigValidationError` / `KeyError`. Never returns `None`.
- `migrate_v2_to_v3(data)` returns a new dict; never mutates input.
- `run_interactive()` always returns a valid `ProjectConfig`. Never returns `None`.

---

## 6. Database Changes

**Not applicable.** This is a CLI tool with no database.

---

## 7. API Changes

**Not applicable.** No REST/gRPC interfaces.

**CLI contract additions for this story:**

```
claude-setup init --config path/to/config.yaml  -> Load config, print summary, exit 0
claude-setup init                                -> Interactive mode, prompt for all fields, exit 0
claude-setup init --config bad.yaml              -> ConfigValidationError, exit 1
```

---

## 8. Event Changes

**Not applicable.** `event_driven: false` in project config.

---

## 9. Configuration Changes

### 9.1 `pyproject.toml`

No new dependencies required. `pyyaml>=6.0,<7.0` is already declared from STORY-001.

### 9.2 Environment Variables

None required for this story.

### 9.3 Test Fixtures

New test fixture files needed in `tests/fixtures/`:

| File | Purpose |
|------|---------|
| `tests/fixtures/valid_v3_config.yaml` | Complete v3 config (copy of python-click-cli template) |
| `tests/fixtures/valid_v2_type_config.yaml` | v2 config with `type: api` at root level |
| `tests/fixtures/valid_v2_stack_config.yaml` | v2 config with `stack: java-quarkus` at root level |
| `tests/fixtures/missing_language_config.yaml` | v3 config missing `language` section (triggers validation error) |
| `tests/fixtures/minimal_v3_config.yaml` | v3 config with only required sections and defaults |

---

## 10. Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| PyYAML in domain layer violates hexagonal purity | Low | Medium | PyYAML is a data-format library (like `json` in stdlib). It has no framework coupling. Document this decision. If strict purity is needed later, extract a `ConfigReaderPort` interface. |
| v2 stack mapping values become stale | Medium | Low | Derive version numbers from actual config templates in `src/config-templates/`. Add a contract test that validates each mapping against the template files. |
| Interactive mode testing complexity | Medium | Medium | Use `click.testing.CliRunner` with `input` parameter to simulate user prompts. No real stdin needed. |
| `yaml.safe_load` returns `None` for empty files | Medium | High | Guard with explicit `None` check before validation. Raise `ConfigValidationError("Config file is empty")`. |
| v2 format with unknown type/stack value | Low | Medium | Default mapping for unknown types: `("microservice", ["rest"])` matching `setup.sh` default behavior (line 459). Log warning for unknown stack values and raise `ConfigValidationError`. |
| Python 3.9 compatibility | Low | High | No `match/case`, no `X | Y` union syntax, no `str.removeprefix()`. Use `if/elif` chains for v2 mappings. Use `Dict[str, str]` not `dict[str, str]`. |
| `_require()` in models raises `KeyError` but story expects `ConfigValidationError` | Medium | Medium | `validate_config()` runs BEFORE `from_dict()`. This catches missing top-level sections with the custom exception. Inner `_require()` KeyErrors from malformed sub-sections are acceptable as programming errors, not user-facing validation. |
| Click prompt interruption (Ctrl+C) during interactive mode | Low | Low | Click handles `Abort` exception natively. No special handling needed. |

---

## Implementation Order

Execute sub-tasks in this sequence:

1. **Create `claude_setup/exceptions.py`** -- `ConfigValidationError` with `missing_fields` attribute
2. **Create `claude_setup/config.py`** -- `V2_TYPE_MAPPING`, `V2_STACK_MAPPING`, `REQUIRED_SECTIONS`, `detect_v2_format()`, `migrate_v2_to_v3()`, `validate_config()`, `load_config()`
3. **Create `claude_setup/interactive.py`** -- `run_interactive()` with Click prompts
4. **Modify `claude_setup/__main__.py`** -- add `init` subcommand with `--config` option
5. **Create `tests/fixtures/`** -- all YAML fixture files
6. **Create `tests/test_config.py`** -- unit tests for config loading, detection, migration, validation
7. **Create `tests/test_config_contract.py`** -- parametrized contract tests for all v2 mappings
8. **Create `tests/test_interactive.py`** -- unit tests for interactive mode with mocked prompts
9. **Validate** -- `pytest --cov`, verify >= 95% line coverage, >= 90% branch coverage

---

## File Tree (Final State after STORY-002)

```
claude_setup/
    __init__.py              # (unchanged from STORY-001)
    __main__.py              # MODIFIED: add init subcommand
    models.py                # (unchanged from STORY-001)
    exceptions.py            # NEW: ConfigValidationError
    config.py                # NEW: load_config, detect_v2, migrate, validate
    interactive.py           # NEW: run_interactive with Click prompts
    assembler/
        __init__.py          # (unchanged from STORY-001)
tests/
    __init__.py
    conftest.py              # (may add config fixtures)
    test_models.py           # (unchanged from STORY-001)
    test_config.py           # NEW: unit tests for config module
    test_config_contract.py  # NEW: contract tests for v2 mappings
    test_interactive.py      # NEW: unit tests for interactive mode
    fixtures/                # NEW: YAML test fixtures
        valid_v3_config.yaml
        valid_v2_type_config.yaml
        valid_v2_stack_config.yaml
        missing_language_config.yaml
        minimal_v3_config.yaml
pyproject.toml               # (unchanged)
```

---

## Acceptance Criteria Traceability

| Gherkin Scenario | Validated By |
|------------------|-------------|
| Load valid v3 config -> `ProjectConfig` with correct fields | `test_config.py::test_load_config_valid_v3` |
| Auto-migrate v2 config with type field -> valid v3 `ProjectConfig` + warning | `test_config.py::test_load_config_v2_type_migration` |
| Missing required section -> `ConfigValidationError` with field name | `test_config.py::test_load_config_missing_required_section` |
| Interactive mode produces equivalent config to file-based loading | `test_interactive.py::test_run_interactive_produces_valid_config` |
| All v2 type values map correctly | `test_config_contract.py::test_v2_type_mapping[api]` through `[fullstack]` |
| All v2 stack values map correctly | `test_config_contract.py::test_v2_stack_mapping[java-quarkus]` through `[rust-axum]` |
| Empty YAML file raises validation error | `test_config.py::test_load_config_empty_file` |
| Coverage >= 95% line, >= 90% branch | `pytest --cov` with `fail_under=95` in pyproject.toml |
