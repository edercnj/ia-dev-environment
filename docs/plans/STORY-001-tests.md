# Test Plan: STORY-001 -- Scaffolding and Domain Models

**Story:** STORY-001 -- Scaffolding do Projeto e Modelos de Dominio
**Framework:** pytest + pytest-cov
**Language:** Python 3.9
**Coverage Target:** >= 95% line, >= 90% branch

---

## 1. Test Directory Structure

```
claude_setup/tests/
    __init__.py
    conftest.py                  # Shared fixtures and factory functions
    unit/
        __init__.py
        test_project_identity.py
        test_architecture_config.py
        test_interface_config.py
        test_language_config.py
        test_framework_config.py
        test_tech_component.py
        test_data_config.py
        test_encryption_config.py
        test_security_config.py
        test_cloud_config.py
        test_infra_config.py
        test_observability_config.py
        test_testing_config.py
        test_domain_config.py
        test_git_scope.py
        test_conventions_config.py
        test_adaptive_model.py
        test_skills_config.py
        test_agents_config.py
        test_hooks_config.py
        test_settings_config.py
        test_project_config.py
    contract/
        __init__.py
        test_from_dict_contracts.py
    integration/
        __init__.py
        test_entry_point.py
    fixtures/
        __init__.py
        model_fixtures.py
```

---

## 2. Test Fixtures (`conftest.py` and `fixtures/model_fixtures.py`)

### 2.1 Fixture: Full YAML-equivalent dict

```python
# fixtures/model_fixtures.py

FULL_PROJECT_DICT = {
    "project": {
        "name": "my-service",
        "purpose": "A sample service",
    },
    "architecture": {
        "style": "hexagonal",
        "domain_driven": True,
        "event_driven": False,
    },
    "interfaces": [
        {"type": "rest", "spec": "openapi-3.1"},
        {"type": "grpc", "spec": "proto3"},
        {"type": "event-consumer", "broker": "kafka"},
        {"type": "event-producer", "broker": "kafka"},
    ],
    "language": {
        "name": "python",
        "version": "3.9",
    },
    "framework": {
        "name": "click",
        "version": "8.1",
        "build_tool": "pip",
        "native_build": False,
    },
    "data": {
        "database": {"type": "postgresql", "version": "15"},
        "cache": {"type": "redis", "version": "7"},
        "message_broker": {"type": "kafka", "version": "3.5"},
    },
    "infrastructure": {
        "container": "docker",
        "orchestrator": "kubernetes",
        "templating": "helm",
        "iac": "terraform",
        "registry": "ghcr",
        "api_gateway": "kong",
        "service_mesh": "istio",
    },
    "security": {
        "compliance": ["owasp", "soc2"],
        "encryption": {
            "at_rest": True,
            "key_management": "vault",
        },
        "pentest_readiness": True,
    },
    "cloud": {
        "provider": "aws",
    },
    "observability": {
        "standard": "opentelemetry",
        "backend": "grafana",
    },
    "testing": {
        "smoke_tests": True,
        "performance_tests": True,
        "contract_tests": True,
        "chaos_tests": False,
    },
    "domain": {
        "template": "hexagonal-ddd",
    },
    "conventions": {
        "code_language": "english",
        "commit_language": "english",
        "documentation_language": "english",
        "git_scopes": [
            {"scope": "core", "area": "domain"},
            {"scope": "api", "area": "adapter"},
        ],
    },
    "skills": {
        "override": "none",
    },
    "agents": {
        "override": "none",
        "adaptive_model": {
            "junior": "haiku",
            "mid": "sonnet",
            "senior": "opus",
        },
    },
    "hooks": {
        "post_compile": True,
    },
    "settings": {
        "auto_generate": True,
    },
}

MINIMAL_PROJECT_DICT = {
    "project": {
        "name": "minimal-tool",
        "purpose": "Minimal CLI tool",
    },
    "architecture": {
        "style": "library",
        "domain_driven": False,
        "event_driven": False,
    },
    "interfaces": [
        {"type": "cli"},
    ],
    "language": {
        "name": "python",
        "version": "3.9",
    },
    "framework": {
        "name": "click",
        "version": "8.1",
        "build_tool": "pip",
        "native_build": False,
    },
}
```

### 2.2 Fixture: Factory functions (`conftest.py`)

```python
import pytest
from claude_setup.models import (
    ProjectIdentity, ArchitectureConfig, InterfaceConfig,
    LanguageConfig, FrameworkConfig, TechComponent, DataConfig,
    EncryptionConfig, SecurityConfig, CloudConfig, InfraConfig,
    ObservabilityConfig, TestingConfig, DomainConfig, GitScope,
    ConventionsConfig, AdaptiveModel, SkillsConfig, AgentsConfig,
    HooksConfig, SettingsConfig, ProjectConfig,
)
from claude_setup.tests.fixtures.model_fixtures import (
    FULL_PROJECT_DICT,
    MINIMAL_PROJECT_DICT,
)


@pytest.fixture
def full_project_dict():
    return FULL_PROJECT_DICT.copy()


@pytest.fixture
def minimal_project_dict():
    return MINIMAL_PROJECT_DICT.copy()


@pytest.fixture
def a_project_identity():
    return ProjectIdentity(name="test-tool", purpose="A test tool")


@pytest.fixture
def a_tech_component():
    return TechComponent(type="postgresql", version="15")
```

---

## 3. Unit Tests

### 3.1 Test File: `test_project_identity.py`

| Test Name | Scenario | Expected Behavior |
|-----------|----------|-------------------|
| `test_from_dict_complete_data_returns_populated_instance` | Dict with name and purpose | Both fields populated correctly |
| `test_from_dict_empty_purpose_returns_empty_string` | Dict with `purpose: ""` | Instance with empty purpose |
| `test_init_direct_construction_stores_attributes` | Direct `ProjectIdentity(name, purpose)` | Attributes accessible |
| `test_from_dict_missing_name_raises_key_error` | Dict without `name` key | Raises `KeyError` |

### 3.2 Test File: `test_architecture_config.py`

| Test Name | Scenario | Expected Behavior |
|-----------|----------|-------------------|
| `test_from_dict_complete_data_returns_populated_instance` | Full dict | All three fields populated |
| `test_from_dict_defaults_omitted_booleans_returns_false` | Dict with only `style` | `domain_driven` and `event_driven` default to `False` |
| `test_init_direct_construction_stores_attributes` | Direct construction | Attributes match |

### 3.3 Test File: `test_interface_config.py`

| Test Name | Scenario | Expected Behavior |
|-----------|----------|-------------------|
| `test_from_dict_rest_interface_returns_spec_populated` | `{"type": "rest", "spec": "openapi-3.1"}` | `spec` = `"openapi-3.1"`, `broker` = `""` |
| `test_from_dict_event_interface_returns_broker_populated` | `{"type": "event-consumer", "broker": "kafka"}` | `broker` = `"kafka"`, `spec` = `""` |
| `test_from_dict_minimal_type_only_returns_defaults` | `{"type": "cli"}` | `spec` = `""`, `broker` = `""` |
| `test_init_direct_construction_stores_attributes` | Direct construction | Attributes match |

### 3.4 Test File: `test_language_config.py`

| Test Name | Scenario | Expected Behavior |
|-----------|----------|-------------------|
| `test_from_dict_complete_data_returns_populated_instance` | `{"name": "python", "version": "3.9"}` | Both fields match |
| `test_from_dict_missing_version_raises_key_error` | Dict without `version` | Raises `KeyError` |

### 3.5 Test File: `test_framework_config.py`

| Test Name | Scenario | Expected Behavior |
|-----------|----------|-------------------|
| `test_from_dict_complete_data_returns_populated_instance` | Full dict | All four fields match |
| `test_from_dict_native_build_default_returns_false` | Dict without `native_build` | Defaults to `False` |

### 3.6 Test File: `test_tech_component.py`

| Test Name | Scenario | Expected Behavior |
|-----------|----------|-------------------|
| `test_from_dict_complete_data_returns_populated_instance` | `{"type": "postgresql", "version": "15"}` | Both fields match |
| `test_from_dict_version_omitted_returns_empty_string` | `{"type": "none"}` | `version` = `""` |
| `test_from_dict_empty_dict_returns_defaults` | `{}` | `type` = `""`, `version` = `""` or defaults per implementation |

### 3.7 Test File: `test_data_config.py`

| Test Name | Scenario | Expected Behavior |
|-----------|----------|-------------------|
| `test_from_dict_complete_data_returns_all_components` | Dict with database, cache, message_broker | All `TechComponent` instances populated |
| `test_from_dict_empty_dict_returns_default_components` | `{}` | All fields use default `TechComponent()` |
| `test_from_dict_partial_data_returns_mixed_defaults` | Dict with only `database` | `cache` and `message_broker` use defaults |

### 3.8 Test File: `test_encryption_config.py`

| Test Name | Scenario | Expected Behavior |
|-----------|----------|-------------------|
| `test_from_dict_complete_data_returns_populated_instance` | `{"at_rest": True, "key_management": "vault"}` | Both fields match |
| `test_from_dict_empty_dict_returns_defaults` | `{}` | `at_rest` = `False`, `key_management` = `""` |

### 3.9 Test File: `test_security_config.py`

| Test Name | Scenario | Expected Behavior |
|-----------|----------|-------------------|
| `test_from_dict_complete_data_returns_populated_instance` | Full dict | `compliance` list, `encryption`, `pentest_readiness` populated |
| `test_from_dict_empty_dict_returns_defaults` | `{}` | Empty list, default encryption, `False` pentest |
| `test_from_dict_empty_compliance_returns_empty_list` | `{"compliance": []}` | Empty compliance list |

### 3.10 Test File: `test_cloud_config.py`

| Test Name | Scenario | Expected Behavior |
|-----------|----------|-------------------|
| `test_from_dict_complete_data_returns_provider` | `{"provider": "aws"}` | `provider` = `"aws"` |
| `test_from_dict_empty_dict_returns_default_provider` | `{}` | `provider` = `""` or default |

### 3.11 Test File: `test_infra_config.py`

| Test Name | Scenario | Expected Behavior |
|-----------|----------|-------------------|
| `test_from_dict_complete_data_returns_all_fields` | Full dict with all 7 fields | All fields populated |
| `test_from_dict_empty_dict_returns_defaults` | `{}` | All fields default to `""` |
| `test_from_dict_partial_data_returns_mixed_defaults` | Dict with only `container` and `orchestrator` | Remaining fields default |

### 3.12 Test File: `test_observability_config.py`

| Test Name | Scenario | Expected Behavior |
|-----------|----------|-------------------|
| `test_from_dict_complete_data_returns_populated_instance` | `{"standard": "opentelemetry", "backend": "grafana"}` | Both fields match |
| `test_from_dict_empty_dict_returns_defaults` | `{}` | Both fields default to `""` |

### 3.13 Test File: `test_testing_config.py`

| Test Name | Scenario | Expected Behavior |
|-----------|----------|-------------------|
| `test_from_dict_complete_data_returns_all_booleans` | Full dict | All four booleans match |
| `test_from_dict_empty_dict_returns_false_defaults` | `{}` | All booleans `False` |

### 3.14 Test File: `test_domain_config.py`

| Test Name | Scenario | Expected Behavior |
|-----------|----------|-------------------|
| `test_from_dict_complete_data_returns_template` | `{"template": "hexagonal-ddd"}` | `template` populated |
| `test_from_dict_empty_dict_returns_default` | `{}` | `template` = `""` |

### 3.15 Test File: `test_git_scope.py`

| Test Name | Scenario | Expected Behavior |
|-----------|----------|-------------------|
| `test_from_dict_complete_data_returns_populated_instance` | `{"scope": "core", "area": "domain"}` | Both fields match |

### 3.16 Test File: `test_conventions_config.py`

| Test Name | Scenario | Expected Behavior |
|-----------|----------|-------------------|
| `test_from_dict_complete_data_returns_all_fields` | Full dict with git_scopes | All fields populated, git_scopes is list of `GitScope` |
| `test_from_dict_empty_dict_returns_defaults` | `{}` | Defaults for languages, empty git_scopes list |
| `test_from_dict_multiple_git_scopes_returns_list` | Dict with 3 scopes | List length = 3, each is `GitScope` |

### 3.17 Test File: `test_adaptive_model.py`

| Test Name | Scenario | Expected Behavior |
|-----------|----------|-------------------|
| `test_from_dict_complete_data_returns_all_tiers` | Full dict | junior, mid, senior populated |
| `test_from_dict_empty_dict_returns_defaults` | `{}` | All tiers default to `""` |

### 3.18 Test File: `test_skills_config.py`

| Test Name | Scenario | Expected Behavior |
|-----------|----------|-------------------|
| `test_from_dict_complete_data_returns_override` | `{"override": "custom"}` | `override` = `"custom"` |
| `test_from_dict_empty_dict_returns_default` | `{}` | `override` = `""` or `"none"` |

### 3.19 Test File: `test_agents_config.py`

| Test Name | Scenario | Expected Behavior |
|-----------|----------|-------------------|
| `test_from_dict_complete_data_returns_all_fields` | Full dict | `override` and `adaptive_model` populated |
| `test_from_dict_empty_dict_returns_defaults` | `{}` | Defaults applied |

### 3.20 Test File: `test_hooks_config.py`

| Test Name | Scenario | Expected Behavior |
|-----------|----------|-------------------|
| `test_from_dict_complete_data_returns_boolean` | `{"post_compile": True}` | `post_compile` = `True` |
| `test_from_dict_empty_dict_returns_false` | `{}` | `post_compile` = `False` |

### 3.21 Test File: `test_settings_config.py`

| Test Name | Scenario | Expected Behavior |
|-----------|----------|-------------------|
| `test_from_dict_complete_data_returns_boolean` | `{"auto_generate": True}` | `auto_generate` = `True` |
| `test_from_dict_empty_dict_returns_false` | `{}` | `auto_generate` = `False` |

### 3.22 Test File: `test_project_config.py`

| Test Name | Scenario | Expected Behavior |
|-----------|----------|-------------------|
| `test_from_dict_full_yaml_returns_fully_populated_config` | `FULL_PROJECT_DICT` | All fields populated, interfaces has 4 items |
| `test_from_dict_minimal_yaml_returns_defaults_for_optional` | `MINIMAL_PROJECT_DICT` | Required fields populated, optional fields use defaults |
| `test_from_dict_full_yaml_interfaces_count_returns_four` | `FULL_PROJECT_DICT` | `len(config.interfaces)` == 4 |
| `test_from_dict_full_yaml_interface_types_returns_correct_types` | `FULL_PROJECT_DICT` | Types are `["rest", "grpc", "event-consumer", "event-producer"]` |
| `test_from_dict_missing_required_project_raises_key_error` | Dict without `project` | Raises `KeyError` |
| `test_from_dict_missing_required_architecture_raises_key_error` | Dict without `architecture` | Raises `KeyError` |
| `test_from_dict_missing_required_interfaces_raises_key_error` | Dict without `interfaces` | Raises `KeyError` |
| `test_from_dict_missing_required_language_raises_key_error` | Dict without `language` | Raises `KeyError` |
| `test_from_dict_missing_required_framework_raises_key_error` | Dict without `framework` | Raises `KeyError` |
| `test_from_dict_empty_interfaces_list_returns_empty_list` | `{"interfaces": []}` + required fields | `config.interfaces` = `[]` |

---

## 4. Contract Tests (Parametrized)

### File: `test_from_dict_contracts.py`

Exhaustive parametrized tests covering `from_dict()` for every model class. One row per scenario.

### 4.1 `ProjectIdentity.from_dict` Contract

```python
@pytest.mark.parametrize(
    "input_dict, expected_name, expected_purpose, description",
    [
        ({"name": "my-tool", "purpose": "CLI tool"}, "my-tool", "CLI tool", "complete_data"),
        ({"name": "x", "purpose": ""}, "x", "", "empty_purpose"),
        ({"name": "a-b-c", "purpose": "multi-word purpose"}, "a-b-c", "multi-word purpose", "hyphenated_name"),
    ],
)
def test_project_identity_from_dict_parametrized(input_dict, expected_name, expected_purpose, description):
    ...
```

### 4.2 `ArchitectureConfig.from_dict` Contract

```python
@pytest.mark.parametrize(
    "input_dict, expected_style, expected_dd, expected_ed, description",
    [
        ({"style": "hexagonal", "domain_driven": True, "event_driven": True}, "hexagonal", True, True, "all_true"),
        ({"style": "library", "domain_driven": False, "event_driven": False}, "library", False, False, "all_false"),
        ({"style": "layered"}, "layered", False, False, "defaults_only"),
        ({"style": "microservice", "domain_driven": True}, "microservice", True, False, "partial_booleans"),
    ],
)
def test_architecture_config_from_dict_parametrized(input_dict, expected_style, expected_dd, expected_ed, description):
    ...
```

### 4.3 `InterfaceConfig.from_dict` Contract

```python
@pytest.mark.parametrize(
    "input_dict, expected_type, expected_spec, expected_broker, description",
    [
        ({"type": "rest", "spec": "openapi-3.1"}, "rest", "openapi-3.1", "", "rest_with_spec"),
        ({"type": "grpc", "spec": "proto3"}, "grpc", "proto3", "", "grpc_with_spec"),
        ({"type": "event-consumer", "broker": "kafka"}, "event-consumer", "", "kafka", "consumer_with_broker"),
        ({"type": "event-producer", "broker": "rabbitmq"}, "event-producer", "", "rabbitmq", "producer_with_broker"),
        ({"type": "cli"}, "cli", "", "", "cli_no_extras"),
        ({"type": "websocket", "spec": "ws", "broker": "redis"}, "websocket", "ws", "redis", "all_fields"),
    ],
)
def test_interface_config_from_dict_parametrized(input_dict, expected_type, expected_spec, expected_broker, description):
    ...
```

### 4.4 `FrameworkConfig.from_dict` Contract

```python
@pytest.mark.parametrize(
    "input_dict, expected_name, expected_version, expected_build, expected_native, description",
    [
        ({"name": "click", "version": "8.1", "build_tool": "pip", "native_build": False}, "click", "8.1", "pip", False, "click_pip"),
        ({"name": "quarkus", "version": "3.2", "build_tool": "maven", "native_build": True}, "quarkus", "3.2", "maven", True, "quarkus_native"),
        ({"name": "spring", "version": "3.1", "build_tool": "gradle"}, "spring", "3.1", "gradle", False, "no_native_default"),
    ],
)
def test_framework_config_from_dict_parametrized(input_dict, expected_name, expected_version, expected_build, expected_native, description):
    ...
```

### 4.5 `TechComponent.from_dict` Contract

```python
@pytest.mark.parametrize(
    "input_dict, expected_type, expected_version, description",
    [
        ({"type": "postgresql", "version": "15"}, "postgresql", "15", "postgres"),
        ({"type": "redis", "version": "7"}, "redis", "7", "redis"),
        ({"type": "none"}, "none", "", "none_no_version"),
        ({}, "", "", "empty_dict"),
    ],
)
def test_tech_component_from_dict_parametrized(input_dict, expected_type, expected_version, description):
    ...
```

### 4.6 `SecurityConfig.from_dict` Contract

```python
@pytest.mark.parametrize(
    "input_dict, expected_compliance_len, expected_pentest, description",
    [
        ({"compliance": ["owasp", "soc2"], "encryption": {"at_rest": True, "key_management": "vault"}, "pentest_readiness": True}, 2, True, "full_security"),
        ({"compliance": []}, 0, False, "empty_compliance"),
        ({}, 0, False, "empty_dict_defaults"),
        ({"pentest_readiness": True}, 0, True, "pentest_only"),
    ],
)
def test_security_config_from_dict_parametrized(input_dict, expected_compliance_len, expected_pentest, description):
    ...
```

### 4.7 `ProjectConfig.from_dict` Contract

```python
@pytest.mark.parametrize(
    "input_dict, expected_interface_count, expected_has_data, description",
    [
        (FULL_PROJECT_DICT, 4, True, "full_yaml_all_sections"),
        (MINIMAL_PROJECT_DICT, 1, False, "minimal_required_only"),
    ],
)
def test_project_config_from_dict_parametrized(input_dict, expected_interface_count, expected_has_data, description):
    ...
```

---

## 5. Integration Tests

### File: `test_entry_point.py`

| Test Name | Scenario | Expected Behavior |
|-----------|----------|-------------------|
| `test_pip_install_editable_succeeds` | Run `pip install -e .` | Exit code 0, no errors |
| `test_python_module_help_returns_zero_exit_code` | Run `python -m claude_setup --help` | Exit code 0, output contains usage info |
| `test_claude_setup_help_returns_zero_exit_code` | Run `claude-setup --help` | Exit code 0, output contains usage info |
| `test_claude_setup_version_returns_version_string` | Run `claude-setup --version` | Exit code 0, output contains version |

**Implementation approach:** Use `subprocess.run()` to invoke commands and assert on exit codes and stdout content.

```python
import subprocess

def test_python_module_help_returns_zero_exit_code():
    result = subprocess.run(
        ["python", "-m", "claude_setup", "--help"],
        capture_output=True,
        text=True,
        timeout=10,
    )
    assert result.returncode == 0
    assert "Usage" in result.stdout or "usage" in result.stdout
```

---

## 6. Coverage Targets

| Metric | Target | Enforcement |
|--------|--------|-------------|
| Line Coverage | >= 95% | `pytest-cov` with `fail_under=95` in `pyproject.toml` |
| Branch Coverage | >= 90% | `branch = true` in `[tool.coverage.run]` |
| Model Coverage | 100% | Every `from_dict()` path exercised (complete + partial + missing keys) |

### Coverage Command

```bash
pytest --cov=claude_setup --cov-branch --cov-report=html --cov-report=xml --cov-fail-under=95 claude_setup/tests/
```

---

## 7. Test Naming Convention

All test functions follow this pattern:

```
test_{method_under_test}_{scenario}_{expected_behavior}
```

Examples:
- `test_from_dict_complete_data_returns_populated_instance`
- `test_from_dict_empty_dict_returns_defaults`
- `test_from_dict_missing_name_raises_key_error`
- `test_python_module_help_returns_zero_exit_code`

---

## 8. Edge Cases to Cover

| Edge Case | Applies To | Test Strategy |
|-----------|------------|---------------|
| Empty dict `{}` | All models with optional fields | Verify defaults are applied |
| Missing required key | `ProjectIdentity.name`, `LanguageConfig.name`, `ProjectConfig.project` | Assert `KeyError` raised |
| Empty list `[]` for list fields | `InterfaceConfig` list, `SecurityConfig.compliance` | Verify empty list stored |
| Extra unknown keys in dict | All `from_dict()` methods | Verify unknown keys are ignored (no error) |
| `None` values in dict | Optional fields | Verify graceful handling or default applied |
| Nested `from_dict()` delegation | `ProjectConfig` -> `DataConfig` -> `TechComponent` | Full chain works end-to-end |
| Mutable default gotcha | `DataConfig`, `SecurityConfig`, `ConventionsConfig` | Two instances do not share mutable state (`field(default_factory=...)`) |

### 8.1 Mutable Default Isolation Test

```python
def test_data_config_default_factory_returns_independent_instances():
    # Arrange
    config_a = DataConfig()
    config_b = DataConfig()

    # Assert — instances must not share the same object references
    assert config_a is not config_b
    assert config_a.database is not config_b.database
```

---

## 9. Execution Order

1. **Unit tests first** -- fast, no external dependencies
2. **Contract tests second** -- parametrized, still pure Python
3. **Integration tests last** -- requires installed package, subprocess calls

### pytest markers (optional, recommended)

```python
# conftest.py
import pytest

def pytest_configure(config):
    config.addinivalue_line("markers", "unit: Unit tests")
    config.addinivalue_line("markers", "contract: Contract tests")
    config.addinivalue_line("markers", "integration: Integration tests")
```

### Run by category

```bash
pytest -m unit
pytest -m contract
pytest -m integration
pytest  # all
```

---

## 10. Summary

| Category | File Count | Test Count (approx) |
|----------|------------|---------------------|
| Unit | 21 files | ~65 tests |
| Contract | 1 file | ~25 parametrized rows |
| Integration | 1 file | ~4 tests |
| **Total** | **23 files** | **~94 tests** |

All tests use real domain objects (no mocking of domain logic). No external services or databases required. Full coverage of every dataclass, every `from_dict()` path, every default value, and every required-field validation.
