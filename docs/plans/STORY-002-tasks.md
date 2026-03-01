# STORY-002 Task Breakdown: Config Loading and Validation

**Story:** STORY-002 -- Config Loading and Validation
**Stack:** Python 3.9 + Click 8.1
**Blocked By:** STORY-001 (models and scaffolding)

---

## Group Overview

| Group | Name | Parallelizable With | Description |
|-------|------|---------------------|-------------|
| G1 | Foundation | -- | Exception class; zero dependencies beyond stdlib |
| G2 | Domain Logic | After G1 | Core config engine: load, detect, migrate, validate |
| G3 | Interactive Mode | After G1 | Click-based interactive prompts (adapter layer) |
| G4 | CLI Integration | After G2, G3 | Wire `init` subcommand into `__main__.py` |
| G5 | Test Fixtures | After G1 | YAML fixture files for all test scenarios |
| G6 | Unit + Contract Tests | After G2, G3, G5 | Tests for config.py, interactive.py, contract tests |
| G7 | Integration Tests | After G4, G6 | End-to-end CLI tests + coverage verification |

**Parallelism notes:**
- G2 and G3 can run in parallel (both depend only on G1).
- G5 can run in parallel with G2 and G3 (no code dependencies, only needs G1 for context).
- G6 requires G2, G3, and G5 to be complete.
- G7 requires G4 and G6 to be complete.

---

## G1: Foundation

### T-001: Create `ConfigValidationError` exception

| Field | Value |
|-------|-------|
| **ID** | T-001 |
| **Group** | G1 |
| **File** | `claude_setup/exceptions.py` (NEW) |
| **Action** | Create |
| **Complexity** | Low |
| **Blocked By** | -- |
| **Blocks** | T-002, T-003, T-004, T-005, T-006, T-007 |

**Description:**
Create `claude_setup/exceptions.py` with a single exception class `ConfigValidationError(Exception)`.

**Requirements:**
- Carries `missing_fields: List[str]` attribute with names of all absent required sections.
- Human-readable `str` representation listing missing fields.
- Zero external dependencies (standard library only -- domain layer).
- Exception carries context values per coding standards (the field names that caused the error).

**Acceptance:**
```python
class ConfigValidationError(Exception):
    def __init__(self, missing_fields: List[str]) -> None:
        self.missing_fields = missing_fields
        super().__init__(
            f"Missing required config sections: {', '.join(missing_fields)}"
        )
```

---

## G2: Domain Logic

### T-002: Create `V2_TYPE_MAPPING` and `V2_STACK_MAPPING` constants

| Field | Value |
|-------|-------|
| **ID** | T-002 |
| **Group** | G2 |
| **File** | `claude_setup/config.py` (NEW) |
| **Action** | Create |
| **Complexity** | Medium |
| **Blocked By** | T-001 |
| **Blocks** | T-003, T-004 |

**Description:**
Create `claude_setup/config.py` with module-level constants for v2-to-v3 migration mappings.

**Constants to define:**

`REQUIRED_SECTIONS: Tuple[str, ...] = ("project", "architecture", "interfaces", "language", "framework")`

`V2_TYPE_MAPPING: Dict[str, Tuple[str, List[str]]]`:
```
"api"       -> ("microservice", ["rest"])
"cli"       -> ("library", ["cli"])
"library"   -> ("library", [])
"worker"    -> ("microservice", ["event-consumer"])
"fullstack" -> ("monolith", ["rest"])
```

`V2_STACK_MAPPING: Dict[str, Dict[str, str]]`:
```
"java-quarkus"      -> {language_name: "java", language_version: "21", framework_name: "quarkus", framework_version: "3.17"}
"java-spring"       -> {language_name: "java", language_version: "21", framework_name: "spring-boot", framework_version: "3.4"}
"python-fastapi"    -> {language_name: "python", language_version: "3.12", framework_name: "fastapi", framework_version: "0.115"}
"python-click-cli"  -> {language_name: "python", language_version: "3.9", framework_name: "click", framework_version: "8.1"}
"go-gin"            -> {language_name: "go", language_version: "1.23", framework_name: "gin", framework_version: "1.10"}
"kotlin-ktor"       -> {language_name: "kotlin", language_version: "2.1", framework_name: "ktor", framework_version: "3.0"}
"typescript-nestjs" -> {language_name: "typescript", language_version: "5.7", framework_name: "nestjs", framework_version: "10.4"}
"rust-axum"         -> {language_name: "rust", language_version: "1.83", framework_name: "axum", framework_version: "0.8"}
```

**Rules:**
- Use `Dict[str, ...]` not `dict[str, ...]` (Python 3.9 compat).
- Use `from __future__ import annotations` at top.
- Module imports: `pathlib.Path`, `typing`, `yaml`.
- Domain imports: `claude_setup.models.ProjectConfig`, `claude_setup.exceptions.ConfigValidationError`.
- Does NOT import `click`.

---

### T-003: Implement `detect_v2_format` and `migrate_v2_to_v3`

| Field | Value |
|-------|-------|
| **ID** | T-003 |
| **Group** | G2 |
| **File** | `claude_setup/config.py` |
| **Action** | Add functions |
| **Complexity** | Medium |
| **Blocked By** | T-002 |
| **Blocks** | T-005 |

**Description:**
Implement the v2 detection and migration functions in `config.py`.

**`detect_v2_format(data: dict) -> bool`:**
- Returns `True` if root-level `type` key exists with a value in `V2_TYPE_MAPPING`.
- OR if root-level `stack` key exists with a value in `V2_STACK_MAPPING`.
- Returns `False` otherwise.

**`migrate_v2_to_v3(data: dict) -> dict`:**
- Creates a NEW dict (never mutates input).
- Maps `type` -> `architecture.style` + `interfaces` list using `V2_TYPE_MAPPING`.
- Maps `stack` -> `language` + `framework` sections using `V2_STACK_MAPPING`.
- Preserves `project` section from input (copies `name`, `purpose` if present).
- Emits warning via `click.echo` -- CORRECTION: per plan, `config.py` must NOT import click. Instead, use `warnings.warn()` from stdlib or return a tuple `(dict, List[str])` with warning messages. Decision: use `warnings.warn()` to keep domain pure.
- Raises `ConfigValidationError` if `stack` value is unknown.
- For unknown `type` values, defaults to `("microservice", ["rest"])` per setup.sh behavior.

**Rules:**
- No mutation of input dict.
- Function length <= 25 lines each.
- No click imports in this module.

---

### T-004: Implement `validate_config` function

| Field | Value |
|-------|-------|
| **ID** | T-004 |
| **Group** | G2 |
| **File** | `claude_setup/config.py` |
| **Action** | Add function |
| **Complexity** | Low |
| **Blocked By** | T-002 |
| **Blocks** | T-005 |

**Description:**
Implement validation of required top-level sections.

**`validate_config(data: dict) -> None`:**
- Checks that `data` is not `None` (empty YAML file guard). If `None`, raises `ConfigValidationError(list(REQUIRED_SECTIONS))`.
- Checks for presence of all keys in `REQUIRED_SECTIONS`.
- Collects ALL missing keys (not fail-fast on first).
- Raises `ConfigValidationError(missing_fields)` if any are missing.
- Returns `None` if all sections present.

**Rules:**
- Single-pass validation: collect all missing fields, report once.
- Exception carries full list of missing field names.

---

### T-005: Implement `load_config` function

| Field | Value |
|-------|-------|
| **ID** | T-005 |
| **Group** | G2 |
| **File** | `claude_setup/config.py` |
| **Action** | Add function |
| **Complexity** | Medium |
| **Blocked By** | T-003, T-004 |
| **Blocks** | T-008 |

**Description:**
Implement the main entry point for config loading.

**`load_config(path: Path) -> ProjectConfig`:**
1. Read file: `path.read_text(encoding="utf-8")`.
2. Parse YAML: `yaml.safe_load(content)`.
3. Guard `None` result (empty file): raise `ConfigValidationError`.
4. Detect v2: call `detect_v2_format(data)`.
5. If v2: call `migrate_v2_to_v3(data)` to get v3 dict.
6. Validate: call `validate_config(data)`.
7. Construct model: `ProjectConfig.from_dict(data)`.
8. Return `ProjectConfig`.

**Contract:**
- Always returns a valid `ProjectConfig` or raises an exception. Never returns `None`.
- Raises `ConfigValidationError` for missing sections.
- Raises `FileNotFoundError` if path does not exist (stdlib behavior from `Path.read_text`).
- Raises `yaml.YAMLError` for malformed YAML (PyYAML behavior).

---

## G3: Interactive Mode

### T-006: Implement `run_interactive` with Click prompts

| Field | Value |
|-------|-------|
| **ID** | T-006 |
| **Group** | G3 |
| **File** | `claude_setup/interactive.py` (NEW) |
| **Action** | Create |
| **Complexity** | High |
| **Blocked By** | T-001 |
| **Blocks** | T-008 |

**Description:**
Create `claude_setup/interactive.py` with the interactive config collection flow.

**Functions:**

`_prompt_select(label: str, choices: List[str]) -> str`:
- Wraps `click.prompt` with `type=click.Choice(choices)`.

`_prompt_input(label: str, default: str = "") -> str`:
- Wraps `click.prompt` for free-text input.

`_prompt_yesno(label: str, default: bool = False) -> bool`:
- Wraps `click.confirm`.

`run_interactive() -> ProjectConfig`:
- Prompts for project name (text input).
- Prompts for project purpose (text input).
- Prompts for architecture style: `library`, `microservice`, `monolith`.
- Prompts for domain_driven (yes/no, default no).
- Prompts for event_driven (yes/no, default no).
- Prompts for interface type: `rest`, `grpc`, `graphql`, `cli`, `event-consumer`, `tcp`, `websocket`.
- Prompts for language: `java`, `python`, `go`, `kotlin`, `typescript`, `rust`.
- Prompts for framework (choices depend on selected language):
  - java: `quarkus`, `spring-boot`
  - python: `fastapi`, `click`, `django`, `flask`
  - go: `stdlib`, `gin`, `fiber`
  - kotlin: `ktor`
  - typescript: `nestjs`, `express`, `fastify`
  - rust: `axum`, `actix`
- Prompts for language version (text input, with sensible default based on language).
- Prompts for framework version (text input, with sensible default based on framework).
- Prompts for build tool (text input, default based on language).
- Prompts for native build (yes/no, default no).
- Constructs and returns `ProjectConfig`.

**Available choices constant:**

`FRAMEWORK_CHOICES: Dict[str, List[str]]` mapping language -> list of frameworks.

**Rules:**
- Imports `click` (framework -- allowed in adapter layer).
- Imports `claude_setup.models.*` (adapter -> domain, correct direction).
- Does NOT import `claude_setup.config` (no cross-adapter dependency).
- Function length <= 25 lines: split `run_interactive` into helper calls if needed.
- Always returns valid `ProjectConfig`, never `None`.

---

## G4: CLI Integration

### T-007: Wire `init` subcommand into `__main__.py`

| Field | Value |
|-------|-------|
| **ID** | T-007 |
| **Group** | G4 |
| **File** | `claude_setup/__main__.py` (MODIFY) |
| **Action** | Modify |
| **Complexity** | Low |

**Blocked By:** T-005, T-006
**Blocks:** T-012

**Description:**
Add the `init` subcommand to the existing Click group in `__main__.py`.

**Changes:**
- Add imports: `from pathlib import Path`, `from claude_setup.config import load_config`, `from claude_setup.interactive import run_interactive`.
- Add `init` command decorated with `@main.command()`.
- Add `--config` / `-c` option: `click.option("--config", "-c", type=click.Path(exists=True), default=None)`.
- Logic: if `config` provided, call `load_config(Path(config))`; else call `run_interactive()`.
- Echo success: `click.echo(f"Loaded config for: {project_config.project.name}")`.
- Catch `ConfigValidationError` and exit with code 1 and error message.

**Minimal implementation (full orchestration deferred to STORY-009):**
```python
@main.command()
@click.option("--config", "-c", type=click.Path(exists=True), default=None)
def init(config):
    """Initialize project from config file or interactive mode."""
    if config:
        project_config = load_config(Path(config))
    else:
        project_config = run_interactive()
    click.echo(f"Loaded config for: {project_config.project.name}")
```

---

## G5: Test Fixtures

### T-008: Create YAML fixture files

| Field | Value |
|-------|-------|
| **ID** | T-008 |
| **Group** | G5 |
| **File** | `tests/fixtures/*.yaml` (NEW, 5 files) |
| **Action** | Create |
| **Complexity** | Medium |
| **Blocked By** | T-001 |
| **Blocks** | T-009, T-010 |

**Description:**
Create the `tests/fixtures/` directory and the following YAML fixture files:

**1. `tests/fixtures/valid_v3_config.yaml`:**
Complete v3 config matching the python-click-cli template. All required sections present (`project`, `architecture`, `interfaces`, `language`, `framework`), plus optional sections (`data`, `infrastructure`, `security`, `testing`).

**2. `tests/fixtures/valid_v2_type_config.yaml`:**
v2-format config with `type: api` and `stack: java-quarkus` at root level, plus `project.name` and `project.purpose`. No `architecture`, `interfaces`, `language`, `framework` sections (those come from migration).

**3. `tests/fixtures/valid_v2_stack_config.yaml`:**
v2-format config with `type: cli` and `stack: python-click-cli` at root level.

**4. `tests/fixtures/missing_language_config.yaml`:**
v3-format config with `language` section intentionally removed. All other required sections present. Used to trigger `ConfigValidationError`.

**5. `tests/fixtures/minimal_v3_config.yaml`:**
v3 config with ONLY the five required sections and minimal field values. No optional sections. Tests that defaults are applied correctly.

---

## G6: Unit + Contract Tests

### T-009: Create unit tests for config module

| Field | Value |
|-------|-------|
| **ID** | T-009 |
| **Group** | G6 |
| **File** | `tests/test_config.py` (NEW) |
| **Action** | Create |
| **Complexity** | High |
| **Blocked By** | T-005, T-008 |
| **Blocks** | T-012 |

**Description:**
Create comprehensive unit tests for all functions in `claude_setup/config.py`.

**Test cases:**

| Test Name | Target | Scenario |
|-----------|--------|----------|
| `test_load_config_valid_v3` | `load_config` | Load `valid_v3_config.yaml`, assert `ProjectConfig` fields match |
| `test_load_config_minimal_v3` | `load_config` | Load `minimal_v3_config.yaml`, assert defaults applied |
| `test_load_config_v2_type_migration` | `load_config` | Load `valid_v2_type_config.yaml`, assert v3 model correct + warning emitted |
| `test_load_config_v2_stack_migration` | `load_config` | Load `valid_v2_stack_config.yaml`, assert v3 model correct |
| `test_load_config_missing_required_section` | `load_config` | Load `missing_language_config.yaml`, assert `ConfigValidationError` raised with `"language"` in `missing_fields` |
| `test_load_config_empty_file` | `load_config` | Load empty YAML file (via tmp_path), assert `ConfigValidationError` raised |
| `test_load_config_file_not_found` | `load_config` | Pass non-existent path, assert `FileNotFoundError` raised |
| `test_detect_v2_format_with_type` | `detect_v2_format` | Dict with `type: "api"` -> `True` |
| `test_detect_v2_format_with_stack` | `detect_v2_format` | Dict with `stack: "java-quarkus"` -> `True` |
| `test_detect_v2_format_v3_dict` | `detect_v2_format` | Dict with v3 sections -> `False` |
| `test_validate_config_all_present` | `validate_config` | Dict with all required sections -> no exception |
| `test_validate_config_multiple_missing` | `validate_config` | Dict missing 3 sections -> `ConfigValidationError` with 3 fields |
| `test_validate_config_none_input` | `validate_config` | `None` input -> `ConfigValidationError` |
| `test_migrate_v2_to_v3_does_not_mutate` | `migrate_v2_to_v3` | Verify input dict is unchanged after migration |
| `test_migrate_v2_to_v3_unknown_type_defaults` | `migrate_v2_to_v3` | Unknown type value defaults to `("microservice", ["rest"])` |

**Naming convention:** `[function_under_test]_[scenario]_[expected_behavior]` (implicit in names above).

---

### T-010: Create contract tests for v2 mappings

| Field | Value |
|-------|-------|
| **ID** | T-010 |
| **Group** | G6 |
| **File** | `tests/test_config_contract.py` (NEW) |
| **Action** | Create |
| **Complexity** | Medium |
| **Blocked By** | T-003, T-008 |
| **Blocks** | T-012 |

**Description:**
Create parametrized contract tests that validate every v2 mapping produces the correct v3 output.

**Test cases:**

**`test_v2_type_mapping` (parametrized, 5 rows):**
One row per v2 type value (`api`, `cli`, `library`, `worker`, `fullstack`). Each row asserts:
- `architecture.style` matches expected value.
- `interfaces` list matches expected types.

**`test_v2_stack_mapping` (parametrized, 8 rows):**
One row per v2 stack value. Each row provides a complete v2 dict with `type` + `stack` + `project`, runs `migrate_v2_to_v3`, then asserts:
- `language.name` matches.
- `language.version` matches.
- `framework.name` matches.
- `framework.version` matches.

**`test_roundtrip_v2_to_model` (parametrized, 8 rows):**
For each v2 stack, migrate to v3, then construct `ProjectConfig.from_dict()`. Assert no exception and model fields match expected values. Validates that migration output is consumable by the model layer.

---

### T-011: Create unit tests for interactive mode

| Field | Value |
|-------|-------|
| **ID** | T-011 |
| **Group** | G6 |
| **File** | `tests/test_interactive.py` (NEW) |
| **Action** | Create |
| **Complexity** | Medium |
| **Blocked By** | T-006 |
| **Blocks** | T-012 |

**Description:**
Create unit tests for `run_interactive()` using `click.testing.CliRunner` with simulated input.

**Test cases:**

| Test Name | Scenario | Expected |
|-----------|----------|----------|
| `test_run_interactive_produces_valid_config` | Provide all answers simulating python-click-cli choices | Returns `ProjectConfig` with correct field values |
| `test_run_interactive_java_quarkus_choices` | Provide answers for java-quarkus stack | `language.name == "java"`, `framework.name == "quarkus"` |
| `test_run_interactive_config_equivalent_to_file` | Compare interactive output to `valid_v3_config.yaml` loaded via `load_config` | Both `ProjectConfig` instances have equivalent field values |
| `test_prompt_select_returns_valid_choice` | Unit test `_prompt_select` with mocked click | Returns chosen value |
| `test_prompt_yesno_default_false` | Unit test `_prompt_yesno` with default | Returns `False` when user accepts default |

**Testing strategy:**
- Use `click.testing.CliRunner` with `input` parameter to simulate stdin.
- Mock `click.prompt` and `click.confirm` for isolated helper tests.
- No real stdin interaction in tests.

---

## G7: Integration Tests + Final Verification

### T-012: Create integration tests for CLI `init` subcommand

| Field | Value |
|-------|-------|
| **ID** | T-012 |
| **Group** | G7 |
| **File** | `tests/test_integration.py` (MODIFY) or `tests/test_cli_init.py` (NEW) |
| **Action** | Create or Modify |
| **Complexity** | Medium |
| **Blocked By** | T-007, T-009, T-010, T-011 |
| **Blocks** | T-013 |

**Description:**
Integration tests exercising the full CLI flow through `CliRunner`.

**Test cases:**

| Test Name | Scenario | Expected |
|-----------|----------|----------|
| `test_init_with_valid_config_file` | `claude-setup init --config valid_v3_config.yaml` | Exit code 0, output contains project name |
| `test_init_with_v2_config_file` | `claude-setup init --config valid_v2_type_config.yaml` | Exit code 0, migration warning emitted, output contains project name |
| `test_init_with_invalid_config_file` | `claude-setup init --config missing_language_config.yaml` | Exit code 1, error message mentions missing section |
| `test_init_with_nonexistent_file` | `claude-setup init --config nonexistent.yaml` | Exit code 2 (Click file validation), error message |
| `test_init_interactive_mode` | `claude-setup init` with simulated stdin | Exit code 0, output contains project name |

---

### T-013: Run full test suite and verify coverage thresholds

| Field | Value |
|-------|-------|
| **ID** | T-013 |
| **Group** | G7 |
| **File** | -- (verification step) |
| **Action** | Verify |
| **Complexity** | Low |
| **Blocked By** | T-012 |
| **Blocks** | -- |

**Description:**
Run the complete test suite with coverage measurement and verify thresholds are met.

**Commands:**
```bash
pytest --cov=claude_setup --cov-report=html --cov-report=xml --cov-report=term-missing --cov-fail-under=95
```

**Verification checklist:**
- [ ] All tests pass (zero failures).
- [ ] Line coverage >= 95%.
- [ ] Branch coverage >= 90%.
- [ ] Zero linter warnings.
- [ ] `claude-setup init --config tests/fixtures/valid_v3_config.yaml` runs successfully.
- [ ] `claude-setup init` (interactive) runs successfully with manual input.
- [ ] All Gherkin scenarios from STORY-002 are covered by at least one test.

---

## Task Dependency Graph

```
T-001 (exceptions.py)
  ├── T-002 (constants in config.py)
  │     ├── T-003 (detect + migrate)
  │     │     └── T-005 (load_config) ──┐
  │     └── T-004 (validate_config) ────┘
  │                                      ├── T-007 (__main__.py init)
  ├── T-006 (interactive.py) ───────────┘        │
  └── T-008 (YAML fixtures)                      │
        ├── T-009 (test_config.py) ───────────────┤
        └── T-010 (test_config_contract.py) ──────┤
                                                   │
  T-011 (test_interactive.py) ────────────────────┤
                                                   │
                                          T-012 (integration tests)
                                                   │
                                          T-013 (coverage verification)
```

**Parallel execution windows:**
1. **Window 1:** T-001 alone.
2. **Window 2:** T-002 + T-006 + T-008 in parallel.
3. **Window 3:** T-003 + T-004 in parallel (within config.py, after T-002).
4. **Window 4:** T-005 + T-011 in parallel.
5. **Window 5:** T-007 + T-009 + T-010 in parallel.
6. **Window 6:** T-012.
7. **Window 7:** T-013.

---

## Summary

| ID | Description | File | Group | Complexity | Blocked By |
|----|-------------|------|-------|------------|------------|
| T-001 | Create `ConfigValidationError` exception | `claude_setup/exceptions.py` | G1 | Low | -- |
| T-002 | Create mapping constants + module scaffold | `claude_setup/config.py` | G2 | Medium | T-001 |
| T-003 | Implement `detect_v2_format` + `migrate_v2_to_v3` | `claude_setup/config.py` | G2 | Medium | T-002 |
| T-004 | Implement `validate_config` | `claude_setup/config.py` | G2 | Low | T-002 |
| T-005 | Implement `load_config` | `claude_setup/config.py` | G2 | Medium | T-003, T-004 |
| T-006 | Implement `run_interactive` with Click prompts | `claude_setup/interactive.py` | G3 | High | T-001 |
| T-007 | Wire `init` subcommand into `__main__.py` | `claude_setup/__main__.py` | G4 | Low | T-005, T-006 |
| T-008 | Create YAML fixture files (5 files) | `tests/fixtures/*.yaml` | G5 | Medium | T-001 |
| T-009 | Unit tests for config module | `tests/test_config.py` | G6 | High | T-005, T-008 |
| T-010 | Contract tests for v2 mappings | `tests/test_config_contract.py` | G6 | Medium | T-003, T-008 |
| T-011 | Unit tests for interactive mode | `tests/test_interactive.py` | G6 | Medium | T-006 |
| T-012 | Integration tests for CLI `init` | `tests/test_cli_init.py` | G7 | Medium | T-007, T-009, T-010, T-011 |
| T-013 | Coverage verification | -- | G7 | Low | T-012 |
