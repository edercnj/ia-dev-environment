# Test Plan: STORY-007 -- Skills and Agents Assemblers

**Story:** STORY-007 -- Skills and Agents Assemblers
**Stack:** Python 3.9 + Click 8.1
**Framework:** pytest + pytest-cov
**Coverage Targets:** Line >= 95%, Branch >= 90%
**Naming Convention:** `test_{function}_{scenario}_{expected}`

---

## 1. Test File Structure

```
tests/
├── conftest.py                          # Shared fixtures (updated with new model fields)
├── assembler/
│   ├── __init__.py
│   ├── conftest.py                      # Assembler-specific fixtures (configs, temp dirs)
│   ├── test_conditions.py               # Unit: extract_interface_types, has_interface, has_any_interface
│   ├── test_skills_selection.py         # Unit: select_conditional_skills, select_knowledge_packs, select_core_skills
│   ├── test_agents_selection.py         # Unit: select_conditional_agents, select_developer_agent, select_core_agents
│   ├── test_skills_assembly.py          # Integration: SkillsAssembler.assemble() with temp dirs
│   ├── test_agents_assembly.py          # Integration: AgentsAssembler.assemble() with temp dirs
│   ├── test_skills_contract.py          # Contract: byte-for-byte comparison with bash output
│   └── test_agents_contract.py          # Contract: byte-for-byte comparison with bash output
└── fixtures/
    └── assembler/
        ├── skills-templates/            # Minimal skill templates for integration tests
        │   ├── core/
        │   │   └── coding-standards/
        │   │       └── SKILL.md
        │   ├── conditional/
        │   │   ├── x-review-api/
        │   │   │   └── SKILL.md
        │   │   └── run-e2e/
        │   │       └── SKILL.md
        │   └── knowledge-packs/
        │       ├── layer-templates/
        │       │   ├── SKILL.md
        │       │   └── references/
        │       │       └── example.md
        │       └── database-patterns/
        │           └── SKILL.md
        ├── agents-templates/            # Minimal agent templates for integration tests
        │   ├── core/
        │   │   └── review.md
        │   ├── conditional/
        │   │   ├── database-engineer.md
        │   │   └── api-engineer.md
        │   ├── developers/
        │   │   ├── python-developer.md
        │   │   └── java-developer.md
        │   └── checklists/
        │       ├── pci-dss-security.md
        │       └── grpc-api.md
        └── reference-output/            # Reference bash output for contract tests (if available)
            ├── skills/
            └── agents/
```

---

## 2. Assembler-Specific Fixtures (`tests/assembler/conftest.py`)

```python
import copy
import shutil
from pathlib import Path
from typing import Callable

import pytest

from ia_dev_env.models import ProjectConfig

ASSEMBLER_FIXTURES = Path(__file__).parent.parent / "fixtures" / "assembler"


def _full_featured_dict():
    """Config dict with all features enabled."""
    return {
        "project": {"name": "full-service", "purpose": "Full featured service"},
        "architecture": {"style": "hexagonal", "domain_driven": True, "event_driven": True},
        "interfaces": [
            {"type": "rest", "spec": "openapi-3.1"},
            {"type": "grpc", "spec": "proto3"},
            {"type": "event-consumer", "broker": "kafka"},
        ],
        "language": {"name": "java", "version": "21"},
        "framework": {"name": "quarkus", "version": "3.17", "build_tool": "maven"},
        "data": {
            "database": {"name": "postgresql", "version": "15"},
            "cache": {"name": "redis", "version": "7"},
        },
        "infrastructure": {
            "container": "docker",
            "orchestrator": "kubernetes",
            "templating": "kustomize",
            "iac": "terraform",
            "registry": "ecr",
            "api_gateway": "kong",
            "service_mesh": "istio",
            "observability": {"tool": "opentelemetry", "metrics": "prometheus", "tracing": "jaeger"},
        },
        "security": {"frameworks": ["owasp", "pci-dss", "lgpd", "hipaa", "sox"]},
        "testing": {
            "smoke_tests": True,
            "contract_tests": True,
            "performance_tests": True,
            "coverage_line": 95,
            "coverage_branch": 90,
        },
    }


def _minimal_cli_dict():
    """Minimal CLI config with no optional features."""
    return {
        "project": {"name": "minimal-cli", "purpose": "Minimal CLI"},
        "architecture": {"style": "library"},
        "interfaces": [{"type": "cli"}],
        "language": {"name": "python", "version": "3.9"},
        "framework": {"name": "click", "version": "8.1"},
    }


@pytest.fixture
def full_featured_config():
    return ProjectConfig.from_dict(_full_featured_dict())


@pytest.fixture
def minimal_cli_config():
    return ProjectConfig.from_dict(_minimal_cli_dict())


@pytest.fixture
def config_factory() -> Callable:
    """Factory to build ProjectConfig with selective overrides."""
    def _create(**overrides):
        base = _minimal_cli_dict()
        for key, value in overrides.items():
            base[key] = value
        return ProjectConfig.from_dict(base)
    return _create


@pytest.fixture
def assembler_fixtures_dir():
    return ASSEMBLER_FIXTURES


@pytest.fixture
def skills_src_dir(assembler_fixtures_dir):
    return assembler_fixtures_dir / "skills-templates"


@pytest.fixture
def agents_src_dir(assembler_fixtures_dir):
    return assembler_fixtures_dir / "agents-templates"


@pytest.fixture
def output_dir(tmp_path):
    return tmp_path / "output"
```

---

## 3. Fixtures to Add to Root `conftest.py`

The `FULL_PROJECT_DICT` and `MINIMAL_PROJECT_DICT` in `tests/conftest.py` must be updated to include the new model fields added by STORY-007:

```python
# Add to FULL_PROJECT_DICT["infrastructure"]:
"templating": "kustomize",
"iac": "terraform",
"registry": "ecr",
"api_gateway": "kong",
"service_mesh": "istio",

# Add to FULL_PROJECT_DICT["testing"]:
"performance_tests": True,
```

---

## 4. Unit Tests: `test_conditions.py`

Tests for `ia_dev_env/assembler/conditions.py`.

### 4.1 `extract_interface_types()`

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_extract_interface_types_single_interface_returns_list_with_one_type` | Config with `[{type: "rest"}]` | `["rest"]` |
| 2 | `test_extract_interface_types_multiple_interfaces_returns_all_types` | Config with `[rest, grpc, event-consumer]` | `["rest", "grpc", "event-consumer"]` |
| 3 | `test_extract_interface_types_cli_only_returns_cli` | Config with `[{type: "cli"}]` | `["cli"]` |
| 4 | `test_extract_interface_types_empty_interfaces_returns_empty_list` | Config with `interfaces: []` | `[]` |
| 5 | `test_extract_interface_types_preserves_order` | Config with `[grpc, rest, cli]` | `["grpc", "rest", "cli"]` |

### 4.2 `has_interface()`

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 6 | `test_has_interface_rest_present_returns_true` | Config with `[rest, grpc]`, check `"rest"` | `True` |
| 7 | `test_has_interface_rest_absent_returns_false` | Config with `[cli]`, check `"rest"` | `False` |
| 8 | `test_has_interface_event_consumer_present_returns_true` | Config with `[event-consumer]`, check `"event-consumer"` | `True` |
| 9 | `test_has_interface_empty_interfaces_returns_false` | Config with `interfaces: []`, check `"rest"` | `False` |

### 4.3 `has_any_interface()`

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 10 | `test_has_any_interface_one_match_returns_true` | Config with `[rest]`, check `("rest", "grpc")` | `True` |
| 11 | `test_has_any_interface_no_match_returns_false` | Config with `[cli]`, check `("rest", "grpc")` | `False` |
| 12 | `test_has_any_interface_multiple_matches_returns_true` | Config with `[rest, grpc]`, check `("rest", "grpc")` | `True` |
| 13 | `test_has_any_interface_empty_types_returns_false` | Config with `[rest]`, check with no args | `False` |
| 14 | `test_has_any_interface_empty_interfaces_returns_false` | Config with `interfaces: []`, check `("rest",)` | `False` |

---

## 5. Unit Tests: `test_skills_selection.py`

### 5.1 `select_conditional_skills()` -- 13 Conditional Skill Gates

Each test validates a single conditional skill gate. The `config_factory` fixture is used to build configs with the exact conditions needed.

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_select_conditional_skills_rest_interface_includes_x_review_api` | Config with `interfaces: [{type: "rest"}]` | `"x-review-api"` in result |
| 2 | `test_select_conditional_skills_no_rest_excludes_x_review_api` | Config with `interfaces: [{type: "cli"}]` | `"x-review-api"` not in result |
| 3 | `test_select_conditional_skills_grpc_interface_includes_x_review_grpc` | Config with `interfaces: [{type: "grpc"}]` | `"x-review-grpc"` in result |
| 4 | `test_select_conditional_skills_no_grpc_excludes_x_review_grpc` | Config with `interfaces: [{type: "cli"}]` | `"x-review-grpc"` not in result |
| 5 | `test_select_conditional_skills_graphql_interface_includes_x_review_graphql` | Config with `interfaces: [{type: "graphql"}]` | `"x-review-graphql"` in result |
| 6 | `test_select_conditional_skills_event_consumer_includes_x_review_events` | Config with `interfaces: [{type: "event-consumer"}]` | `"x-review-events"` in result |
| 7 | `test_select_conditional_skills_event_producer_includes_x_review_events` | Config with `interfaces: [{type: "event-producer"}]` | `"x-review-events"` in result |
| 8 | `test_select_conditional_skills_no_events_excludes_x_review_events` | Config with `interfaces: [{type: "rest"}]` | `"x-review-events"` not in result |
| 9 | `test_select_conditional_skills_observability_enabled_includes_instrument_otel` | Config with `observability.tool: "opentelemetry"` | `"instrument-otel"` in result |
| 10 | `test_select_conditional_skills_observability_none_excludes_instrument_otel` | Config with `observability.tool: "none"` | `"instrument-otel"` not in result |
| 11 | `test_select_conditional_skills_orchestrator_enabled_includes_setup_environment` | Config with `orchestrator: "kubernetes"` | `"setup-environment"` in result |
| 12 | `test_select_conditional_skills_orchestrator_none_excludes_setup_environment` | Config with `orchestrator: "none"` | `"setup-environment"` not in result |
| 13 | `test_select_conditional_skills_smoke_tests_rest_includes_run_smoke_api` | Config with `smoke_tests: True` and `interfaces: [{type: "rest"}]` | `"run-smoke-api"` in result |
| 14 | `test_select_conditional_skills_smoke_tests_false_excludes_run_smoke_api` | Config with `smoke_tests: False` and `interfaces: [{type: "rest"}]` | `"run-smoke-api"` not in result |
| 15 | `test_select_conditional_skills_smoke_tests_no_rest_excludes_run_smoke_api` | Config with `smoke_tests: True` and `interfaces: [{type: "cli"}]` | `"run-smoke-api"` not in result |
| 16 | `test_select_conditional_skills_smoke_tests_tcp_includes_run_smoke_socket` | Config with `smoke_tests: True` and `interfaces: [{type: "tcp-custom"}]` | `"run-smoke-socket"` in result |
| 17 | `test_select_conditional_skills_smoke_tests_no_tcp_excludes_run_smoke_socket` | Config with `smoke_tests: True` and `interfaces: [{type: "rest"}]` | `"run-smoke-socket"` not in result |
| 18 | `test_select_conditional_skills_always_includes_run_e2e` | Any config (minimal CLI) | `"run-e2e"` in result |
| 19 | `test_select_conditional_skills_performance_tests_true_includes_run_perf_test` | Config with `performance_tests: True` | `"run-perf-test"` in result |
| 20 | `test_select_conditional_skills_performance_tests_false_excludes_run_perf_test` | Config with `performance_tests: False` | `"run-perf-test"` not in result |
| 21 | `test_select_conditional_skills_contract_tests_true_includes_run_contract_tests` | Config with `contract_tests: True` | `"run-contract-tests"` in result |
| 22 | `test_select_conditional_skills_contract_tests_false_excludes_run_contract_tests` | Config with `contract_tests: False` | `"run-contract-tests"` not in result |
| 23 | `test_select_conditional_skills_security_frameworks_present_includes_x_review_security` | Config with `security.frameworks: ["owasp"]` | `"x-review-security"` in result |
| 24 | `test_select_conditional_skills_security_frameworks_empty_excludes_x_review_security` | Config with `security.frameworks: []` | `"x-review-security"` not in result |
| 25 | `test_select_conditional_skills_api_gateway_enabled_includes_x_review_gateway` | Config with `api_gateway: "kong"` | `"x-review-gateway"` in result |
| 26 | `test_select_conditional_skills_api_gateway_none_excludes_x_review_gateway` | Config with `api_gateway: "none"` | `"x-review-gateway"` not in result |

### 5.2 `select_conditional_skills()` -- Parametrized Full-Featured Test

```python
@pytest.mark.parametrize(
    "skill_name",
    [
        "x-review-api",
        "x-review-grpc",
        "x-review-events",
        "instrument-otel",
        "setup-environment",
        "run-smoke-api",
        "run-e2e",
        "run-perf-test",
        "run-contract-tests",
        "x-review-security",
        "x-review-gateway",
    ],
    ids=lambda s: s,
)
def test_select_conditional_skills_full_featured_includes_all_applicable(
    full_featured_config, skill_name
):
    ...
```

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 27 | `test_select_conditional_skills_full_featured_includes_all_applicable` | Full featured config (11 parametrized cases) | Each skill is in result |

### 5.3 `select_conditional_skills()` -- Minimal Config

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 28 | `test_select_conditional_skills_minimal_cli_only_includes_always_on_skills` | Minimal CLI config | Only `"run-e2e"` (and `"run-perf-test"` if `performance_tests` defaults to `True`) |

### 5.4 `select_knowledge_packs()` -- Knowledge Pack Conditions

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 29 | `test_select_knowledge_packs_always_includes_core_packs` | Any config | Result contains `"coding-standards"`, `"architecture"`, `"testing"`, `"security"`, `"compliance"`, `"api-design"`, `"observability"`, `"resilience"`, `"infrastructure"`, `"protocols"`, `"story-planning"` |
| 30 | `test_select_knowledge_packs_always_includes_layer_templates` | Any config | `"layer-templates"` in result |
| 31 | `test_select_knowledge_packs_database_configured_includes_database_patterns` | Config with `database.name: "postgresql"` | `"database-patterns"` in result |
| 32 | `test_select_knowledge_packs_cache_configured_includes_database_patterns` | Config with `cache.name: "redis"` (no database) | `"database-patterns"` in result |
| 33 | `test_select_knowledge_packs_no_database_no_cache_excludes_database_patterns` | Config with `database.name: "none"` and `cache.name: "none"` | `"database-patterns"` not in result |
| 34 | `test_select_knowledge_packs_quarkus_framework_includes_quarkus_patterns` | Config with `framework.name: "quarkus"` | `"quarkus-patterns"` in result |
| 35 | `test_select_knowledge_packs_click_framework_includes_click_cli_patterns` | Config with `framework.name: "click"` | `"click-cli-patterns"` in result |
| 36 | `test_select_knowledge_packs_unknown_framework_excludes_stack_patterns` | Config with `framework.name: "unknown"` | No stack pattern pack in result |
| 37 | `test_select_knowledge_packs_kubernetes_includes_k8s_deployment` | Config with `orchestrator: "kubernetes"` | `"k8s-deployment"` in result |
| 38 | `test_select_knowledge_packs_no_kubernetes_excludes_k8s_deployment` | Config with `orchestrator: "none"` | `"k8s-deployment"` not in result |
| 39 | `test_select_knowledge_packs_kustomize_includes_k8s_kustomize` | Config with `templating: "kustomize"` | `"k8s-kustomize"` in result |
| 40 | `test_select_knowledge_packs_helm_includes_k8s_helm` | Config with `templating: "helm"` | `"k8s-helm"` in result |
| 41 | `test_select_knowledge_packs_docker_includes_dockerfile` | Config with `container: "docker"` | `"dockerfile"` in result |
| 42 | `test_select_knowledge_packs_no_container_excludes_dockerfile` | Config with `container: "none"` | `"dockerfile"` not in result |
| 43 | `test_select_knowledge_packs_registry_configured_includes_container_registry` | Config with `registry: "ecr"` | `"container-registry"` in result |
| 44 | `test_select_knowledge_packs_registry_none_excludes_container_registry` | Config with `registry: "none"` | `"container-registry"` not in result |
| 45 | `test_select_knowledge_packs_terraform_includes_iac_terraform` | Config with `iac: "terraform"` | `"iac-terraform"` in result |
| 46 | `test_select_knowledge_packs_crossplane_includes_iac_crossplane` | Config with `iac: "crossplane"` | `"iac-crossplane"` in result |
| 47 | `test_select_knowledge_packs_iac_none_excludes_iac_packs` | Config with `iac: "none"` | Neither `"iac-terraform"` nor `"iac-crossplane"` in result |

### 5.5 `select_knowledge_packs()` -- Parametrized Stack Pack Mapping (Contract)

```python
@pytest.mark.parametrize(
    "framework_name, expected_pack",
    [
        ("quarkus", "quarkus-patterns"),
        ("spring-boot", "spring-patterns"),
        ("nestjs", "nestjs-patterns"),
        ("express", "express-patterns"),
        ("fastapi", "fastapi-patterns"),
        ("django", "django-patterns"),
        ("gin", "gin-patterns"),
        ("ktor", "ktor-patterns"),
        ("axum", "axum-patterns"),
        ("dotnet", "dotnet-patterns"),
        ("click", "click-cli-patterns"),
    ],
    ids=lambda x: x[0] if isinstance(x, tuple) else x,
)
def test_select_knowledge_packs_stack_mapping_matches_bash(
    framework_name, expected_pack, config_factory
):
    ...
```

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 48 | `test_select_knowledge_packs_stack_mapping_matches_bash` | Each of 11 framework-to-pack mappings | Correct pack name in result |

### 5.6 `select_core_skills()`

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 49 | `test_select_core_skills_scans_core_directories` | src_dir with `core/coding-standards/` and `core/architecture/` | Returns `["coding-standards", "architecture"]` (sorted or unordered, verify membership) |
| 50 | `test_select_core_skills_excludes_lib_from_top_level` | src_dir with `core/lib/` and `core/coding-standards/` | `"lib"` not in top-level result |
| 51 | `test_select_core_skills_includes_lib_entries` | src_dir with `core/lib/some-util/` | `"lib/some-util"` in result or handled as lib entries |
| 52 | `test_select_core_skills_empty_core_dir_returns_empty` | src_dir with empty `core/` directory | Returns `[]` |

---

## 6. Unit Tests: `test_agents_selection.py`

### 6.1 `select_conditional_agents()` -- 5 Agent Gates

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_select_conditional_agents_database_configured_includes_database_engineer` | Config with `database.name: "postgresql"` | `"database-engineer.md"` in result |
| 2 | `test_select_conditional_agents_database_none_excludes_database_engineer` | Config with `database.name: "none"` | `"database-engineer.md"` not in result |
| 3 | `test_select_conditional_agents_observability_enabled_includes_observability_engineer` | Config with `observability.tool: "opentelemetry"` | `"observability-engineer.md"` in result |
| 4 | `test_select_conditional_agents_observability_none_excludes_observability_engineer` | Config with `observability.tool: "none"` | `"observability-engineer.md"` not in result |
| 5 | `test_select_conditional_agents_container_configured_includes_devops_engineer` | Config with `container: "docker"` (orchestrator/iac/mesh all "none") | `"devops-engineer.md"` in result |
| 6 | `test_select_conditional_agents_orchestrator_configured_includes_devops_engineer` | Config with `orchestrator: "kubernetes"` (container "none") | `"devops-engineer.md"` in result |
| 7 | `test_select_conditional_agents_iac_configured_includes_devops_engineer` | Config with `iac: "terraform"` (container/orchestrator "none") | `"devops-engineer.md"` in result |
| 8 | `test_select_conditional_agents_service_mesh_configured_includes_devops_engineer` | Config with `service_mesh: "istio"` (container/orchestrator/iac "none") | `"devops-engineer.md"` in result |
| 9 | `test_select_conditional_agents_no_infra_excludes_devops_engineer` | Config with all infra fields set to `"none"` | `"devops-engineer.md"` not in result |
| 10 | `test_select_conditional_agents_rest_interface_includes_api_engineer` | Config with `interfaces: [{type: "rest"}]` | `"api-engineer.md"` in result |
| 11 | `test_select_conditional_agents_grpc_interface_includes_api_engineer` | Config with `interfaces: [{type: "grpc"}]` | `"api-engineer.md"` in result |
| 12 | `test_select_conditional_agents_graphql_interface_includes_api_engineer` | Config with `interfaces: [{type: "graphql"}]` | `"api-engineer.md"` in result |
| 13 | `test_select_conditional_agents_cli_only_excludes_api_engineer` | Config with `interfaces: [{type: "cli"}]` | `"api-engineer.md"` not in result |
| 14 | `test_select_conditional_agents_event_driven_true_includes_event_engineer` | Config with `event_driven: True` | `"event-engineer.md"` in result |
| 15 | `test_select_conditional_agents_event_consumer_includes_event_engineer` | Config with `interfaces: [{type: "event-consumer"}]` (event_driven: False) | `"event-engineer.md"` in result |
| 16 | `test_select_conditional_agents_event_producer_includes_event_engineer` | Config with `interfaces: [{type: "event-producer"}]` | `"event-engineer.md"` in result |
| 17 | `test_select_conditional_agents_no_events_excludes_event_engineer` | Config with `interfaces: [{type: "cli"}]` and `event_driven: False` | `"event-engineer.md"` not in result |

### 6.2 `select_conditional_agents()` -- Full-Featured Test

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 18 | `test_select_conditional_agents_full_featured_includes_all_agents` | Full featured config | Result contains all 5 conditional agents |
| 19 | `test_select_conditional_agents_minimal_cli_excludes_all_conditional` | Minimal CLI config | No conditional agents in result (except possibly devops if container defaults to "docker") |

### 6.3 `select_developer_agent()`

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 20 | `test_select_developer_agent_python_returns_python_developer` | Config with `language.name: "python"` | `"python-developer.md"` |
| 21 | `test_select_developer_agent_java_returns_java_developer` | Config with `language.name: "java"` | `"java-developer.md"` |
| 22 | `test_select_developer_agent_typescript_returns_typescript_developer` | Config with `language.name: "typescript"` | `"typescript-developer.md"` |
| 23 | `test_select_developer_agent_go_returns_go_developer` | Config with `language.name: "go"` | `"go-developer.md"` |
| 24 | `test_select_developer_agent_kotlin_returns_kotlin_developer` | Config with `language.name: "kotlin"` | `"kotlin-developer.md"` |
| 25 | `test_select_developer_agent_rust_returns_rust_developer` | Config with `language.name: "rust"` | `"rust-developer.md"` |

### 6.4 `select_developer_agent()` -- Parametrized (Contract)

```python
@pytest.mark.parametrize(
    "language, expected_file",
    [
        ("python", "python-developer.md"),
        ("java", "java-developer.md"),
        ("typescript", "typescript-developer.md"),
        ("go", "go-developer.md"),
        ("kotlin", "kotlin-developer.md"),
        ("rust", "rust-developer.md"),
    ],
    ids=lambda x: x[0] if isinstance(x, tuple) else x,
)
def test_select_developer_agent_language_mapping_matches_bash(
    language, expected_file, config_factory
):
    ...
```

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 26 | `test_select_developer_agent_language_mapping_matches_bash` | 6 parametrized language-to-file mappings | Returns correct filename |

### 6.5 `select_core_agents()`

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 27 | `test_select_core_agents_scans_core_directory` | src_dir with `core/review.md`, `core/implement-story.md` | Returns both agent filenames |
| 28 | `test_select_core_agents_only_md_files` | src_dir with `core/review.md` and `core/.gitkeep` | Only `.md` files in result |
| 29 | `test_select_core_agents_empty_core_dir_returns_empty` | src_dir with empty `core/` directory | Returns `[]` |

---

## 7. Integration Tests: `test_skills_assembly.py`

Tests use `tmp_path` fixture to create temporary source and output directories with minimal template files.

### 7.1 `SkillsAssembler.assemble()`

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_assemble_creates_skills_directory` | Run assemble with valid config | `output_dir / "skills"` exists |
| 2 | `test_assemble_copies_core_skill_files` | src_dir has `core/coding-standards/SKILL.md` | `output_dir / "skills" / "coding-standards" / "SKILL.md"` exists |
| 3 | `test_assemble_replaces_placeholders_in_skill_md` | Template has `{language_name}`, config has `language.name: "python"` | Output file contains `"python"` not `"{language_name}"` |
| 4 | `test_assemble_copies_conditional_skill_when_condition_met` | Config with `interfaces: [{type: "rest"}]`, src has `conditional/x-review-api/SKILL.md` | `x-review-api/SKILL.md` exists in output |
| 5 | `test_assemble_skips_conditional_skill_when_condition_not_met` | Config with `interfaces: [{type: "cli"}]` | `x-review-api/` does not exist in output |
| 6 | `test_assemble_copies_knowledge_pack_skill_md` | src has `knowledge-packs/layer-templates/SKILL.md` | `output_dir / "skills" / "layer-templates" / "SKILL.md"` exists |
| 7 | `test_assemble_knowledge_pack_preserves_existing_references` | Pre-populate `output_dir/skills/layer-templates/references/existing.md`, run assemble | `existing.md` still present and unmodified |
| 8 | `test_assemble_knowledge_pack_overwrites_skill_md` | Pre-populate `output_dir/skills/layer-templates/SKILL.md` with old content, run assemble | SKILL.md contains new content from template |
| 9 | `test_assemble_returns_list_of_created_paths` | Run assemble | Returns `list[Path]` with at least one entry |
| 10 | `test_assemble_full_featured_creates_expected_skill_count` | Full featured config with all src templates | Returns list with expected number of paths |
| 11 | `test_assemble_stack_patterns_copies_correct_pack` | Config with `framework.name: "quarkus"`, src has `knowledge-packs/quarkus-patterns/` | `quarkus-patterns/SKILL.md` exists in output |
| 12 | `test_assemble_stack_patterns_unknown_framework_skips` | Config with `framework.name: "unknown"` | No stack pattern directory in output |

### 7.2 Infra Patterns Integration

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 13 | `test_assemble_k8s_deployment_copied_when_kubernetes` | Config with `orchestrator: "kubernetes"`, src has `knowledge-packs/k8s-deployment/` | `k8s-deployment/` exists in output |
| 14 | `test_assemble_dockerfile_copied_when_container_set` | Config with `container: "docker"`, src has `knowledge-packs/dockerfile/` | `dockerfile/` exists in output |
| 15 | `test_assemble_iac_terraform_copied_when_iac_terraform` | Config with `iac: "terraform"`, src has `knowledge-packs/iac-terraform/` | `iac-terraform/` exists in output |

---

## 8. Integration Tests: `test_agents_assembly.py`

### 8.1 `AgentsAssembler.assemble()`

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_assemble_creates_agents_directory` | Run assemble with valid config | `output_dir / "agents"` exists |
| 2 | `test_assemble_copies_core_agent_files` | src_dir has `core/review.md` | `output_dir / "agents" / "review.md"` exists |
| 3 | `test_assemble_replaces_placeholders_in_agent_md` | Template has `{project_name}`, config has `project.name: "my-service"` | Output file contains `"my-service"` not `"{project_name}"` |
| 4 | `test_assemble_copies_conditional_agent_when_condition_met` | Config with `database.name: "postgresql"`, src has `conditional/database-engineer.md` | `database-engineer.md` exists in output |
| 5 | `test_assemble_skips_conditional_agent_when_condition_not_met` | Config with `database.name: "none"` | `database-engineer.md` does not exist in output |
| 6 | `test_assemble_copies_developer_agent` | Config with `language.name: "python"`, src has `developers/python-developer.md` | `python-developer.md` exists in output |
| 7 | `test_assemble_developer_agent_has_placeholders_replaced` | Template has `{language_version}`, config has `language.version: "3.9"` | Output file contains `"3.9"` |
| 8 | `test_assemble_returns_list_of_created_paths` | Run assemble | Returns `list[Path]` with at least one entry |

### 8.2 Checklist Injection Integration

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 9 | `test_assemble_injects_pci_dss_checklist_into_security_engineer` | Config with `security.frameworks: ["pci-dss"]`, security-engineer.md has marker | Output security-engineer.md contains pci-dss checklist content |
| 10 | `test_assemble_injects_privacy_checklist_for_lgpd` | Config with `security.frameworks: ["lgpd"]` | Output security-engineer.md contains privacy checklist content |
| 11 | `test_assemble_injects_privacy_checklist_for_gdpr` | Config with `security.frameworks: ["gdpr"]` | Output security-engineer.md contains privacy checklist content |
| 12 | `test_assemble_injects_hipaa_checklist` | Config with `security.frameworks: ["hipaa"]` | Output security-engineer.md contains hipaa checklist content |
| 13 | `test_assemble_injects_sox_checklist` | Config with `security.frameworks: ["sox"]` | Output security-engineer.md contains sox checklist content |
| 14 | `test_assemble_injects_grpc_checklist_into_api_engineer` | Config with `interfaces: [{type: "grpc"}]` | Output api-engineer.md contains grpc checklist content |
| 15 | `test_assemble_injects_graphql_checklist_into_api_engineer` | Config with `interfaces: [{type: "graphql"}]` | Output api-engineer.md contains graphql checklist content |
| 16 | `test_assemble_injects_websocket_checklist_into_api_engineer` | Config with `interfaces: [{type: "websocket"}]` | Output api-engineer.md contains websocket checklist content |
| 17 | `test_assemble_injects_helm_checklist_into_devops_engineer` | Config with `templating: "helm"` | Output devops-engineer.md contains helm checklist content |
| 18 | `test_assemble_injects_iac_checklist_into_devops_engineer` | Config with `iac: "terraform"` | Output devops-engineer.md contains iac checklist content |
| 19 | `test_assemble_injects_mesh_checklist_into_devops_engineer` | Config with `service_mesh: "istio"` | Output devops-engineer.md contains mesh checklist content |
| 20 | `test_assemble_injects_registry_checklist_into_devops_engineer` | Config with `registry: "ecr"` | Output devops-engineer.md contains registry checklist content |
| 21 | `test_assemble_no_security_frameworks_no_security_checklists_injected` | Config with `security.frameworks: []` | security-engineer.md has no injected checklist content |

---

## 9. Contract Tests: `test_skills_contract.py` and `test_agents_contract.py`

Contract tests validate byte-for-byte compatibility with bash output (RULE-005). These tests require reference fixtures generated by the original bash script.

### 9.1 Approach

If reference output exists in `tests/fixtures/assembler/reference-output/`, run the Python assembler with the same config and compare file-by-file.

```python
@pytest.mark.contract
@pytest.mark.skipif(
    not REFERENCE_OUTPUT_DIR.exists(),
    reason="Reference bash output fixtures not available",
)
class TestSkillsContract:
    ...
```

### 9.2 `test_skills_contract.py`

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_skills_output_matches_bash_for_java_quarkus` | Run assembler with java-quarkus config | Each output file matches reference byte-for-byte |
| 2 | `test_skills_output_file_list_matches_bash` | Compare list of generated files | Same set of files as bash output |
| 3 | `test_skills_output_no_extra_files` | Check output has no files absent from reference | No extra files |

### 9.3 `test_agents_contract.py`

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 4 | `test_agents_output_matches_bash_for_java_quarkus` | Run assembler with java-quarkus config | Each output file matches reference byte-for-byte |
| 5 | `test_agents_output_file_list_matches_bash` | Compare list of generated files | Same set of files as bash output |
| 6 | `test_agents_output_no_extra_files` | Check output has no files absent from reference | No extra files |

---

## 10. Edge Cases

Distributed across the test files above, these edge cases must be covered:

### 10.1 Missing Source Directories

| # | Test Name | File | Scenario | Expected |
|---|-----------|------|----------|----------|
| 1 | `test_assemble_missing_src_dir_raises_error_or_returns_empty` | `test_skills_assembly.py` | `src_dir` does not exist | Raises `FileNotFoundError` or returns empty list (document actual behavior) |
| 2 | `test_assemble_missing_core_dir_returns_empty` | `test_skills_assembly.py` | `src_dir` exists but has no `core/` | Returns empty list for core skills |
| 3 | `test_copy_conditional_skill_missing_source_returns_none` | `test_skills_assembly.py` | Conditional skill dir does not exist in src | Returns `None`, does not raise |
| 4 | `test_copy_knowledge_pack_missing_source_returns_none` | `test_skills_assembly.py` | Knowledge pack dir does not exist in src | Returns `None`, does not raise |
| 5 | `test_copy_developer_agent_missing_source_returns_none` | `test_agents_assembly.py` | Developer agent file does not exist in src | Returns `None`, does not raise |

### 10.2 Empty Configurations

| # | Test Name | File | Scenario | Expected |
|---|-----------|------|----------|----------|
| 6 | `test_select_conditional_skills_empty_interfaces_list` | `test_skills_selection.py` | Config with `interfaces: []` | No interface-dependent skills selected |
| 7 | `test_select_conditional_agents_empty_interfaces_list` | `test_agents_selection.py` | Config with `interfaces: []` | No interface-dependent agents selected |
| 8 | `test_select_knowledge_packs_all_defaults` | `test_skills_selection.py` | Minimal config (all optional fields default) | Only core packs, layer-templates, and stack pack for framework |

### 10.3 No Interfaces / Unknown Values

| # | Test Name | File | Scenario | Expected |
|---|-----------|------|----------|----------|
| 9 | `test_has_interface_unknown_type_returns_false` | `test_conditions.py` | Check for `"unknown-protocol"` | Returns `False` |
| 10 | `test_select_developer_agent_unknown_language` | `test_agents_selection.py` | Config with `language.name: "cobol"` | Returns `"cobol-developer.md"` (maps directly) |
| 11 | `test_select_knowledge_packs_unknown_framework_no_stack_pack` | `test_skills_selection.py` | Config with `framework.name: "flask"` (not in STACK_PACK_MAP) | No stack-patterns pack in result |

### 10.4 File Encoding

| # | Test Name | File | Scenario | Expected |
|---|-----------|------|----------|----------|
| 12 | `test_assemble_writes_utf8_encoded_files` | `test_skills_assembly.py` | Template contains UTF-8 characters (accented chars) | Output file is valid UTF-8 |

---

## 11. Mocking Strategy

| What | Mock? | Rationale |
|------|-------|-----------|
| Domain models (`ProjectConfig`, etc.) | NO | Use real objects -- never mock domain logic |
| `TemplateEngine` | NO | Use real engine with test templates (integration) |
| `TemplateEngine` (unit selection tests) | N/A | Selection methods do not use engine |
| File system | NO | Use `tmp_path` fixture for real filesystem operations |
| `shutil.copytree` / `shutil.copy2` | NO | Use real copy operations against temp dirs |
| `STACK_PACK_MAP` constant | NO | Test against real constant -- this is the contract |

---

## 12. Branch Coverage Targets

| Module | Key Branches | Strategy |
|--------|-------------|----------|
| `conditions.py :: extract_interface_types` | Empty vs non-empty interfaces | Tests 1-5 |
| `conditions.py :: has_interface` | Found vs not found | Tests 6-9 |
| `conditions.py :: has_any_interface` | Any match vs none, empty args | Tests 10-14 |
| `skills.py :: select_conditional_skills` | Each of 13 conditions True/False | Tests 1-28 |
| `skills.py :: select_knowledge_packs` | Each pack condition True/False | Tests 29-48 |
| `skills.py :: assemble` | Core/conditional/knowledge paths | Integration tests 1-15 |
| `agents.py :: select_conditional_agents` | Each of 5 conditions (4 with OR) | Tests 1-19 |
| `agents.py :: select_developer_agent` | Language name mapping | Tests 20-26 |
| `agents.py :: assemble` | Core/conditional/developer/checklist paths | Integration tests 1-21 |
| `agents.py :: _inject_checklists` | Each of 11 injection conditions | Integration tests 9-21 |

---

## 13. Estimated Coverage

| Module | Estimated Line Coverage | Estimated Branch Coverage |
|--------|------------------------|--------------------------|
| `assembler/conditions.py` | 100% | 100% |
| `assembler/skills.py` | >= 97% | >= 92% |
| `assembler/agents.py` | >= 96% | >= 91% |
| **Overall (assembler)** | **>= 96%** | **>= 91%** |

---

## 14. Test Execution Configuration

### 14.1 `pyproject.toml` Markers

```toml
[tool.pytest.ini_options]
markers = [
    "contract: Contract tests for byte-for-byte bash compatibility",
]
```

### 14.2 Run Commands

```bash
# All assembler tests
pytest tests/assembler/ --cov=ia_dev_env/assembler --cov-branch --cov-report=html

# Unit tests only (selection logic)
pytest tests/assembler/test_conditions.py tests/assembler/test_skills_selection.py tests/assembler/test_agents_selection.py

# Integration tests only (file operations)
pytest tests/assembler/test_skills_assembly.py tests/assembler/test_agents_assembly.py

# Contract tests only
pytest tests/assembler/test_skills_contract.py tests/assembler/test_agents_contract.py -m contract

# Coverage check
pytest tests/assembler/ --cov=ia_dev_env/assembler --cov-branch --cov-fail-under=95
```

---

## 15. Acceptance Criteria Traceability

| Gherkin Scenario (from STORY-007) | Test(s) |
|-----------------------------------|---------|
| Select skills for java-quarkus full-featured | `test_skills_selection.py` test 27 (parametrized full-featured) |
| Exclude skills not applicable | `test_skills_selection.py` tests 2, 4, 8, 10, 12, 22, 24, 26 |
| Copy knowledge pack with placeholder substitution | `test_skills_assembly.py` tests 3, 6, 8 |
| Select agents based on interfaces | `test_agents_selection.py` tests 10-13, 18 |
| Output identical to bash | `test_skills_contract.py` tests 1-3, `test_agents_contract.py` tests 4-6 |
| Skills/agents not applicable are NOT included | All exclusion tests (negative cases) |

---

## 16. Total Test Count Summary

| File | Tests |
|------|-------|
| `test_conditions.py` | 14 |
| `test_skills_selection.py` (conditional + knowledge packs + core) | 52 + 4 = 56 |
| `test_agents_selection.py` (conditional + developer + core) | 29 + 3 = 32 |
| `test_skills_assembly.py` (integration + edge cases) | 15 + 5 = 20 |
| `test_agents_assembly.py` (integration + checklists + edge cases) | 21 + 2 = 23 |
| `test_skills_contract.py` | 3 |
| `test_agents_contract.py` | 3 |
| **Total** | **151** |
