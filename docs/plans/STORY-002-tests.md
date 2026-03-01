# Test Plan: STORY-002 -- Config Loading and Validation

**Story:** STORY-002 -- Config Loading and Validation
**Stack:** Python 3.9 + Click 8.1
**Framework:** pytest + pytest-cov
**Coverage Targets:** Line >= 95%, Branch >= 90%
**Naming Convention:** `test_{function}_{scenario}_{expected}`

---

## 1. Test File Structure

```
tests/
├── conftest.py                  # Shared fixtures (YAML paths, config dicts)
├── test_config.py               # Unit tests: load_config, detect_v2_format, validate_config
├── test_migration.py            # Unit tests: migrate_v2_to_v3 (isolated)
├── test_config_contract.py      # Contract tests: parametrized v2->v3 mappings
├── test_interactive.py          # Unit tests: run_interactive with CliRunner
├── test_exceptions.py           # Unit tests: ConfigValidationError
└── fixtures/
    ├── valid_v3_config.yaml
    ├── valid_v3_java_quarkus.yaml
    ├── valid_v2_type_config.yaml
    ├── valid_v2_stack_config.yaml
    ├── valid_v2_combined_config.yaml
    ├── missing_language_config.yaml
    ├── missing_project_config.yaml
    ├── missing_multiple_sections.yaml
    ├── minimal_v3_config.yaml
    ├── empty_config.yaml
    └── malformed_yaml.yaml
```

---

## 2. Test Fixtures (YAML Files)

### 2.1 `tests/fixtures/valid_v3_config.yaml`

Complete v3 config matching the python-click-cli template. All required and optional sections present.

```yaml
project:
  name: "test-cli-tool"
  purpose: "Test CLI tool"
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
  native_build: false
```

### 2.2 `tests/fixtures/valid_v3_java_quarkus.yaml`

Complete v3 config matching the java-quarkus template. Used to verify complex configs with multiple interfaces.

```yaml
project:
  name: "test-quarkus-service"
  purpose: "Test Quarkus service"
architecture:
  style: microservice
  domain_driven: true
  event_driven: true
interfaces:
  - type: rest
    spec: openapi
  - type: grpc
    spec: proto3
language:
  name: java
  version: "21"
framework:
  name: quarkus
  version: "3.17"
  build_tool: maven
  native_build: true
```

### 2.3 `tests/fixtures/valid_v2_type_config.yaml`

Legacy v2 format with `type` at root level.

```yaml
type: api
stack: java-quarkus
project:
  name: "legacy-service"
  purpose: "Legacy service"
```

### 2.4 `tests/fixtures/valid_v2_stack_config.yaml`

Legacy v2 format with only `stack` at root level.

```yaml
stack: python-fastapi
project:
  name: "legacy-api"
  purpose: "Legacy API"
```

### 2.5 `tests/fixtures/valid_v2_combined_config.yaml`

v2 format with both `type` and `stack` present.

```yaml
type: cli
stack: python-click-cli
project:
  name: "legacy-cli"
  purpose: "Legacy CLI tool"
```

### 2.6 `tests/fixtures/missing_language_config.yaml`

v3 config missing the `language` section -- triggers `ConfigValidationError`.

```yaml
project:
  name: "incomplete-tool"
  purpose: "Missing language"
architecture:
  style: library
interfaces:
  - type: cli
framework:
  name: click
  version: "8.1"
```

### 2.7 `tests/fixtures/missing_project_config.yaml`

v3 config missing the `project` section.

```yaml
architecture:
  style: library
interfaces:
  - type: cli
language:
  name: python
  version: "3.9"
framework:
  name: click
  version: "8.1"
```

### 2.8 `tests/fixtures/missing_multiple_sections.yaml`

v3 config missing multiple required sections (`language`, `framework`, `interfaces`).

```yaml
project:
  name: "very-incomplete"
  purpose: "Missing many sections"
architecture:
  style: library
```

### 2.9 `tests/fixtures/minimal_v3_config.yaml`

v3 config with only required sections and no optional sections (no `data`, `infrastructure`, `security`, `testing`).

```yaml
project:
  name: "minimal-tool"
  purpose: "Minimal config"
architecture:
  style: library
interfaces:
  - type: cli
language:
  name: python
  version: "3.9"
framework:
  name: click
  version: "8.1"
```

### 2.10 `tests/fixtures/empty_config.yaml`

Empty file (zero bytes). Triggers `ConfigValidationError("Config file is empty")`.

### 2.11 `tests/fixtures/malformed_yaml.yaml`

Invalid YAML syntax.

```yaml
project:
  name: "bad-yaml
  purpose: [unclosed
```

---

## 3. Shared Fixtures (`conftest.py`)

```python
import pytest
from pathlib import Path

FIXTURES_DIR = Path(__file__).parent / "fixtures"


@pytest.fixture
def fixtures_dir() -> Path:
    return FIXTURES_DIR


@pytest.fixture
def valid_v3_path(fixtures_dir) -> Path:
    return fixtures_dir / "valid_v3_config.yaml"


@pytest.fixture
def valid_v2_type_path(fixtures_dir) -> Path:
    return fixtures_dir / "valid_v2_type_config.yaml"


@pytest.fixture
def valid_v2_stack_path(fixtures_dir) -> Path:
    return fixtures_dir / "valid_v2_stack_config.yaml"


@pytest.fixture
def valid_v3_dict() -> dict:
    """A complete v3 config as a Python dict."""
    return {
        "project": {"name": "test-tool", "purpose": "Testing"},
        "architecture": {"style": "library"},
        "interfaces": [{"type": "cli"}],
        "language": {"name": "python", "version": "3.9"},
        "framework": {"name": "click", "version": "8.1"},
    }


@pytest.fixture
def valid_v2_dict() -> dict:
    """A v2 config dict with type + stack at root level."""
    return {
        "type": "api",
        "stack": "java-quarkus",
        "project": {"name": "legacy", "purpose": "Legacy"},
    }
```

---

## 4. Unit Tests: `test_exceptions.py`

Tests for `ConfigValidationError` exception class.

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_config_validation_error_single_field_carries_field_name` | Instantiate with `missing_fields=["language"]` | `error.missing_fields == ["language"]` |
| 2 | `test_config_validation_error_multiple_fields_carries_all_fields` | Instantiate with `missing_fields=["language", "framework"]` | `error.missing_fields == ["language", "framework"]` |
| 3 | `test_config_validation_error_message_lists_missing_fields` | Instantiate with `missing_fields=["language"]` | `str(error)` contains `"language"` |
| 4 | `test_config_validation_error_inherits_from_exception` | Instantiate error | `isinstance(error, Exception)` is `True` |
| 5 | `test_config_validation_error_empty_fields_list_allowed` | Instantiate with `missing_fields=[]` | `error.missing_fields == []` |

---

## 5. Unit Tests: `test_config.py`

### 5.1 `detect_v2_format()`

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_detect_v2_format_with_type_key_returns_true` | Dict has `type: "api"` at root | Returns `True` |
| 2 | `test_detect_v2_format_with_stack_key_returns_true` | Dict has `stack: "java-quarkus"` at root | Returns `True` |
| 3 | `test_detect_v2_format_with_both_keys_returns_true` | Dict has both `type` and `stack` | Returns `True` |
| 4 | `test_detect_v2_format_v3_dict_returns_false` | Complete v3 dict (no `type`/`stack` at root) | Returns `False` |
| 5 | `test_detect_v2_format_empty_dict_returns_false` | Empty dict `{}` | Returns `False` |
| 6 | `test_detect_v2_format_interface_type_not_detected` | Dict has `interfaces: [{type: cli}]` but no root `type` | Returns `False` |

### 5.2 `validate_config()`

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 7 | `test_validate_config_complete_v3_raises_nothing` | Dict with all required sections | No exception raised |
| 8 | `test_validate_config_missing_language_raises_error` | Dict missing `language` | Raises `ConfigValidationError` with `"language"` in `missing_fields` |
| 9 | `test_validate_config_missing_project_raises_error` | Dict missing `project` | Raises `ConfigValidationError` with `"project"` in `missing_fields` |
| 10 | `test_validate_config_missing_architecture_raises_error` | Dict missing `architecture` | Raises `ConfigValidationError` with `"architecture"` in `missing_fields` |
| 11 | `test_validate_config_missing_interfaces_raises_error` | Dict missing `interfaces` | Raises `ConfigValidationError` with `"interfaces"` in `missing_fields` |
| 12 | `test_validate_config_missing_framework_raises_error` | Dict missing `framework` | Raises `ConfigValidationError` with `"framework"` in `missing_fields` |
| 13 | `test_validate_config_missing_multiple_sections_lists_all` | Dict missing `language`, `framework`, `interfaces` | Raises `ConfigValidationError` with all three in `missing_fields` |
| 14 | `test_validate_config_none_input_raises_error` | `None` as input | Raises `ConfigValidationError` with message about empty config |
| 15 | `test_validate_config_optional_sections_not_required` | Dict with only required sections (no `data`, `infrastructure`) | No exception raised |

### 5.3 `load_config()`

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 16 | `test_load_config_valid_v3_returns_project_config` | Path to `valid_v3_config.yaml` | Returns `ProjectConfig` with correct fields |
| 17 | `test_load_config_valid_v3_language_fields_correct` | Path to `valid_v3_config.yaml` | `config.language.name == "python"` and `config.language.version == "3.9"` |
| 18 | `test_load_config_valid_v3_framework_fields_correct` | Path to `valid_v3_config.yaml` | `config.framework.name == "click"` and `config.framework.version == "8.1"` |
| 19 | `test_load_config_valid_v3_project_fields_correct` | Path to `valid_v3_config.yaml` | `config.project.name == "test-cli-tool"` |
| 20 | `test_load_config_java_quarkus_multiple_interfaces` | Path to `valid_v3_java_quarkus.yaml` | `len(config.interfaces) == 2` and types match |
| 21 | `test_load_config_v2_auto_migrates_to_v3` | Path to `valid_v2_type_config.yaml` | Returns valid `ProjectConfig` (v3 structure) |
| 22 | `test_load_config_v2_emits_migration_warning` | Path to `valid_v2_type_config.yaml` | `click.echo` called with migration warning message |
| 23 | `test_load_config_missing_section_raises_validation_error` | Path to `missing_language_config.yaml` | Raises `ConfigValidationError` |
| 24 | `test_load_config_empty_file_raises_validation_error` | Path to `empty_config.yaml` | Raises `ConfigValidationError` with empty config message |
| 25 | `test_load_config_malformed_yaml_raises_yaml_error` | Path to `malformed_yaml.yaml` | Raises `yaml.YAMLError` |
| 26 | `test_load_config_nonexistent_file_raises_file_not_found` | Path to nonexistent file | Raises `FileNotFoundError` |
| 27 | `test_load_config_minimal_v3_uses_defaults` | Path to `minimal_v3_config.yaml` | `config.data.database.name == "none"` and `config.testing.smoke_tests is True` |
| 28 | `test_load_config_returns_project_config_type` | Path to valid config | `isinstance(result, ProjectConfig)` |

---

## 6. Unit Tests: `test_migration.py`

### 6.1 `migrate_v2_to_v3()`

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_migrate_v2_to_v3_api_type_maps_to_microservice` | `{"type": "api", "stack": "java-quarkus", "project": {...}}` | Result has `architecture.style == "microservice"` and `interfaces` contains `{"type": "rest"}` |
| 2 | `test_migrate_v2_to_v3_cli_type_maps_to_library` | `{"type": "cli", "stack": "python-click-cli", ...}` | Result has `architecture.style == "library"` and `interfaces` contains `{"type": "cli"}` |
| 3 | `test_migrate_v2_to_v3_library_type_maps_to_library` | `{"type": "library", "stack": "python-click-cli", ...}` | Result has `architecture.style == "library"` and `interfaces` is empty list |
| 4 | `test_migrate_v2_to_v3_worker_type_maps_to_microservice` | `{"type": "worker", "stack": "java-quarkus", ...}` | Result has `architecture.style == "microservice"` and `interfaces` contains `{"type": "event-consumer"}` |
| 5 | `test_migrate_v2_to_v3_fullstack_type_maps_to_monolith` | `{"type": "fullstack", "stack": "typescript-nestjs", ...}` | Result has `architecture.style == "monolith"` and `interfaces` contains `{"type": "rest"}` |
| 6 | `test_migrate_v2_to_v3_java_quarkus_stack_maps_correctly` | `{"stack": "java-quarkus", ...}` | `language: {name: "java", version: "21"}`, `framework: {name: "quarkus", version: "3.17"}` |
| 7 | `test_migrate_v2_to_v3_python_fastapi_stack_maps_correctly` | `{"stack": "python-fastapi", ...}` | `language: {name: "python", version: "3.12"}`, `framework: {name: "fastapi", version: "0.115"}` |
| 8 | `test_migrate_v2_to_v3_does_not_mutate_input` | Pass dict, check original unchanged | Input dict remains identical after call |
| 9 | `test_migrate_v2_to_v3_preserves_project_section` | v2 dict with `project: {name: "x", purpose: "y"}` | Result preserves `project.name` and `project.purpose` |
| 10 | `test_migrate_v2_to_v3_removes_type_and_stack_keys` | v2 dict with `type` and `stack` | Result dict has no `type` or `stack` keys at root |
| 11 | `test_migrate_v2_to_v3_result_passes_validation` | Any v2 dict with valid type+stack | `validate_config(result)` does not raise |
| 12 | `test_migrate_v2_to_v3_unknown_stack_raises_error` | `{"stack": "unknown-framework", ...}` | Raises `ConfigValidationError` |
| 13 | `test_migrate_v2_to_v3_unknown_type_uses_default` | `{"type": "unknown", "stack": "java-quarkus", ...}` | Uses default mapping `("microservice", ["rest"])` |

---

## 7. Contract Tests: `test_config_contract.py`

### 7.1 v2 Type Mapping (parametrized)

Each row validates one `V2_TYPE_MAPPING` entry.

```python
@pytest.mark.parametrize(
    "v2_type, expected_style, expected_interfaces",
    [
        ("api", "microservice", [{"type": "rest"}]),
        ("cli", "library", [{"type": "cli"}]),
        ("library", "library", []),
        ("worker", "microservice", [{"type": "event-consumer"}]),
        ("fullstack", "monolith", [{"type": "rest"}]),
    ],
    ids=["api", "cli", "library", "worker", "fullstack"],
)
def test_v2_type_mapping_produces_correct_architecture(
    v2_type, expected_style, expected_interfaces
):
    ...
```

| v2 `type` | Expected `architecture.style` | Expected `interfaces` |
|-----------|-------------------------------|----------------------|
| `api` | `microservice` | `[{type: rest}]` |
| `cli` | `library` | `[{type: cli}]` |
| `library` | `library` | `[]` |
| `worker` | `microservice` | `[{type: event-consumer}]` |
| `fullstack` | `monolith` | `[{type: rest}]` |

### 7.2 v2 Stack Mapping (parametrized)

Each row validates one `V2_STACK_MAPPING` entry.

```python
@pytest.mark.parametrize(
    "v2_stack, expected_lang, expected_lang_ver, expected_fw, expected_fw_ver",
    [
        ("java-quarkus", "java", "21", "quarkus", "3.17"),
        ("java-spring", "java", "21", "spring-boot", "3.4"),
        ("python-fastapi", "python", "3.12", "fastapi", "0.115"),
        ("python-click-cli", "python", "3.9", "click", "8.1"),
        ("go-gin", "go", "1.23", "gin", "1.10"),
        ("kotlin-ktor", "kotlin", "2.1", "ktor", "3.0"),
        ("typescript-nestjs", "typescript", "5.7", "nestjs", "10.4"),
        ("rust-axum", "rust", "1.83", "axum", "0.8"),
    ],
    ids=[
        "java-quarkus",
        "java-spring",
        "python-fastapi",
        "python-click-cli",
        "go-gin",
        "kotlin-ktor",
        "typescript-nestjs",
        "rust-axum",
    ],
)
def test_v2_stack_mapping_produces_correct_language_and_framework(
    v2_stack, expected_lang, expected_lang_ver, expected_fw, expected_fw_ver
):
    ...
```

| v2 `stack` | Language | Version | Framework | Version |
|-----------|----------|---------|-----------|---------|
| `java-quarkus` | java | 21 | quarkus | 3.17 |
| `java-spring` | java | 21 | spring-boot | 3.4 |
| `python-fastapi` | python | 3.12 | fastapi | 0.115 |
| `python-click-cli` | python | 3.9 | click | 8.1 |
| `go-gin` | go | 1.23 | gin | 1.10 |
| `kotlin-ktor` | kotlin | 2.1 | ktor | 3.0 |
| `typescript-nestjs` | typescript | 5.7 | nestjs | 10.4 |
| `rust-axum` | rust | 1.83 | axum | 0.8 |

### 7.3 Full Round-Trip Contract (parametrized)

Each row validates that a v2 dict, when passed through `migrate_v2_to_v3()` and then `ProjectConfig.from_dict()`, produces a valid `ProjectConfig` with correct field values.

```python
@pytest.mark.parametrize(
    "v2_type, v2_stack",
    [
        ("api", "java-quarkus"),
        ("api", "java-spring"),
        ("api", "python-fastapi"),
        ("cli", "python-click-cli"),
        ("api", "go-gin"),
        ("api", "kotlin-ktor"),
        ("api", "typescript-nestjs"),
        ("api", "rust-axum"),
    ],
    ids=[
        "api-java-quarkus",
        "api-java-spring",
        "api-python-fastapi",
        "cli-python-click-cli",
        "api-go-gin",
        "api-kotlin-ktor",
        "api-typescript-nestjs",
        "api-rust-axum",
    ],
)
def test_v2_full_roundtrip_produces_valid_project_config(v2_type, v2_stack):
    ...
```

---

## 8. Unit Tests: `test_interactive.py`

All tests use `click.testing.CliRunner` with simulated input strings.

### 8.1 `run_interactive()`

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_run_interactive_complete_input_returns_project_config` | Provide all required inputs via simulated stdin | Returns valid `ProjectConfig` |
| 2 | `test_run_interactive_python_click_produces_correct_language` | Select python + click | `config.language.name == "python"` |
| 3 | `test_run_interactive_java_quarkus_produces_correct_framework` | Select java + quarkus | `config.framework.name == "quarkus"` |
| 4 | `test_run_interactive_library_architecture_sets_style` | Select "library" architecture | `config.architecture.style == "library"` |
| 5 | `test_run_interactive_domain_driven_true_sets_flag` | Answer "yes" to domain_driven prompt | `config.architecture.domain_driven is True` |
| 6 | `test_run_interactive_domain_driven_false_sets_flag` | Answer "no" to domain_driven prompt | `config.architecture.domain_driven is False` |
| 7 | `test_run_interactive_custom_project_name_preserved` | Enter "my-custom-tool" as project name | `config.project.name == "my-custom-tool"` |
| 8 | `test_run_interactive_custom_purpose_preserved` | Enter custom purpose text | `config.project.purpose` matches input |
| 9 | `test_run_interactive_cli_interface_selected` | Select "cli" interface type | `config.interfaces[0].type == "cli"` |
| 10 | `test_run_interactive_result_type_is_project_config` | Complete interactive flow | `isinstance(result, ProjectConfig)` |

### 8.2 Interactive vs File Equivalence

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 11 | `test_run_interactive_equivalent_to_file_config` | Provide inputs matching `valid_v3_config.yaml` values | Interactive result matches `load_config()` result for same values |

### 8.3 Edge Cases

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 12 | `test_run_interactive_abort_raises_click_abort` | Send Ctrl+C (empty input / EOF) | Raises `click.Abort` or `SystemExit` |

---

## 9. Coverage Analysis

### 9.1 Branch Coverage Targets

| Module | Key Branches | Strategy |
|--------|-------------|----------|
| `config.py :: load_config` | v2 detected vs v3 path | Tests 16-28 cover both paths |
| `config.py :: detect_v2_format` | `type` present, `stack` present, neither present | Tests 1-6 cover all branches |
| `config.py :: validate_config` | Each missing section, `None` input, all present | Tests 7-15 cover all branches |
| `config.py :: migrate_v2_to_v3` | Each type mapping, each stack mapping, unknown values | Tests 1-13 + contract tests cover all branches |
| `exceptions.py` | Constructor branches | Tests 1-5 |
| `interactive.py :: run_interactive` | All prompt paths | Tests 1-12 |

### 9.2 Estimated Coverage

| Module | Estimated Line Coverage | Estimated Branch Coverage |
|--------|------------------------|--------------------------|
| `exceptions.py` | 100% | 100% |
| `config.py` | >= 97% | >= 92% |
| `interactive.py` | >= 95% | >= 90% |
| **Overall** | **>= 95%** | **>= 90%** |

---

## 10. Test Execution Configuration

### 10.1 `pyproject.toml` additions

```toml
[tool.pytest.ini_options]
testpaths = ["tests"]
addopts = "--strict-markers --tb=short"
markers = [
    "contract: Contract tests for v2-to-v3 mapping",
]

[tool.coverage.run]
source = ["claude_setup"]
branch = true

[tool.coverage.report]
fail_under = 95
show_missing = true
exclude_lines = [
    "pragma: no cover",
    "if __name__ == .__main__.",
]
```

### 10.2 Run Commands

```bash
# All tests with coverage
pytest --cov=claude_setup --cov-branch --cov-report=html --cov-report=xml

# Unit tests only
pytest tests/test_config.py tests/test_migration.py tests/test_exceptions.py

# Contract tests only
pytest tests/test_config_contract.py -m contract

# Interactive tests only
pytest tests/test_interactive.py

# Coverage check (fail if below threshold)
pytest --cov=claude_setup --cov-branch --cov-fail-under=95
```

---

## 11. Mocking Strategy

| What | Mock? | Rationale |
|------|-------|-----------|
| Domain models (`ProjectConfig`, etc.) | NO | Use real objects -- never mock domain logic |
| `yaml.safe_load` | NO | Use real YAML fixture files |
| File system (`Path.read_text`) | NO | Use real fixture files in `tests/fixtures/` |
| `click.echo` (migration warning) | YES | Verify warning message is emitted via `capsys` or mock |
| Click prompts in `run_interactive` | YES | Use `CliRunner` with `input` parameter (Click-native test support) |
| `V2_TYPE_MAPPING` / `V2_STACK_MAPPING` | NO | Test against real constants -- these are the contract |

---

## 12. Acceptance Criteria Traceability

| Gherkin Scenario (from STORY-002) | Test(s) |
|-----------------------------------|---------|
| Load valid v3 config -> ProjectConfig with correct fields | `test_config.py` tests 16-20, 27-28 |
| Auto-migrate v2 config -> valid v3 ProjectConfig + warning | `test_config.py` tests 21-22 |
| Missing required section -> ConfigValidationError with field name | `test_config.py` tests 8-13, 23 |
| Interactive mode produces equivalent config | `test_interactive.py` tests 1-11 |
| All v2 type values map correctly | `test_config_contract.py` section 7.1 (5 parametrized cases) |
| All v2 stack values map correctly | `test_config_contract.py` section 7.2 (8 parametrized cases) |
| Empty YAML raises validation error | `test_config.py` test 24 |
| Coverage >= 95% line, >= 90% branch | Section 10.2 run commands with `--cov-fail-under` |

---

## 13. Total Test Count Summary

| File | Tests |
|------|-------|
| `test_exceptions.py` | 5 |
| `test_config.py` (detect_v2 + validate + load) | 28 |
| `test_migration.py` | 13 |
| `test_config_contract.py` (type + stack + roundtrip) | 5 + 8 + 8 = 21 |
| `test_interactive.py` | 12 |
| **Total** | **79** |
