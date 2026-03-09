# Implementation Plan: STORY-007 -- Skills and Agents Assemblers

**Story:** STORY-007 -- Assemblers de Skills e Agents
**Phase:** 3 (Assembly)
**Blocked By:** STORY-001 (models), STORY-004 (template engine)
**Blocks:** STORY-009 (CLI orchestration)
**Architecture:** library (hexagonal-lite for a CLI tool)
**Stack:** Python 3.9 + Click 8.1

---

## 1. Affected Layers and Components

| Layer | Affected? | Rationale |
|-------|-----------|-----------|
| domain/model | YES | `InfraConfig` and `TestingConfig` need additional fields (templating, iac, registry, api_gateway, service_mesh, performance_tests) to match bash config variables. New `STACK_PACK_MAP` constant needed. |
| domain/engine | NO | Stack resolution logic (`resolver.py`) already exists; no changes needed there. |
| domain/port | NO | No ports needed -- assemblers are pure domain operations on filesystem + config. |
| application | NO | No use case orchestration yet; assemblers invoked directly from pipeline. |
| adapter/inbound | NO | CLI entrypoint unchanged; assemblers are called by the pipeline (STORY-009). |
| adapter/outbound | NO | File operations use stdlib `pathlib`/`shutil` -- no adapter needed. |
| assembler | YES | New `skills.py` and `agents.py` modules under `ia_dev_env/assembler/`. |
| config | MINOR | `config.py` may need to parse the new `InfraConfig` / `TestingConfig` fields from YAML. |
| tests | YES | Unit, contract, and integration tests for all assembler functions. |

---

## 2. New Classes/Interfaces to Create

### 2.1 Skills Assembler (`ia_dev_env/assembler/skills.py`)

| Function/Class | Description |
|----------------|-------------|
| `SkillsAssembler` | Stateless class that encapsulates all skills assembly logic. |
| `assemble(config, output_dir, src_dir, engine) -> List[Path]` | Main entry point. Copies core skills, conditional skills, and knowledge packs to `output_dir/skills/`. Returns list of created paths. |
| `select_core_skills(src_dir) -> List[str]` | Scans `src_dir/skills-templates/core/` directories (excluding `lib/`), plus `core/lib/` entries. Returns skill names. |
| `select_conditional_skills(config) -> List[str]` | Evaluates feature gates against `ProjectConfig` and returns list of conditional skill names to include. |
| `select_knowledge_packs(config) -> List[str]` | Evaluates config to determine which knowledge packs to include. Returns list of knowledge pack identifiers. |
| `_copy_core_skill(skill_name, src_dir, output_dir, engine) -> Path` | Copies a single core skill directory, runs placeholder replacement on SKILL.md. |
| `_copy_conditional_skill(skill_name, src_dir, output_dir, engine) -> Optional[Path]` | Copies a conditional skill if its source directory exists. |
| `_copy_knowledge_pack(pack_name, src_dir, output_dir, engine) -> Optional[Path]` | Copies SKILL.md (always overwrite) + merges non-existing items (preserves references/ populated earlier). |
| `_copy_stack_patterns(config, src_dir, output_dir, engine) -> Optional[Path]` | Resolves framework to stack pack name, copies the matching stack-patterns knowledge pack. |
| `_copy_infra_patterns(config, src_dir, output_dir) -> List[Path]` | Copies infra knowledge packs (k8s-deployment, k8s-kustomize/helm, dockerfile, container-registry, iac-terraform/crossplane) based on infrastructure config. |

**Conditional Skills Selection Rules:**

| Skill Name | Condition |
|------------|-----------|
| `x-review-api` | `"rest"` in interface types |
| `x-review-grpc` | `"grpc"` in interface types |
| `x-review-graphql` | `"graphql"` in interface types |
| `x-review-events` | `"event-consumer"` or `"event-producer"` in interface types |
| `instrument-otel` | `observability.tool != "none"` |
| `setup-environment` | `orchestrator != "none"` |
| `run-smoke-api` | `smoke_tests == True` AND `"rest"` in interface types |
| `run-smoke-socket` | `smoke_tests == True` AND `"tcp-custom"` in interface types |
| `run-e2e` | Always included |
| `run-perf-test` | `performance_tests == True` |
| `run-contract-tests` | `contract_tests == True` |
| `x-review-security` | `len(security.frameworks) > 0` |
| `x-review-gateway` | `api_gateway != "none"` |

**Knowledge Packs Selection Rules:**

| Pack | Condition |
|------|-----------|
| Core KPs (coding-standards, architecture, testing, security, compliance, api-design, observability, resilience, infrastructure, protocols, story-planning) | Always (SKILL.md only) |
| `layer-templates` | Always (full copy_knowledge_pack) |
| `database-patterns` | `database.name != "none"` OR `cache.name != "none"` |
| Stack patterns (e.g., `quarkus-patterns`) | Framework name maps to pack name via `STACK_PACK_MAP` |
| `k8s-deployment` | `orchestrator == "kubernetes"` |
| `k8s-kustomize` | `templating == "kustomize"` |
| `k8s-helm` | `templating == "helm"` |
| `dockerfile` | `container != "none"` |
| `container-registry` | `registry != "none"` |
| `iac-terraform` | `iac == "terraform"` |
| `iac-crossplane` | `iac == "crossplane"` |

**Stack Pack Mapping (constant `STACK_PACK_MAP`):**

| Framework | Pack Name |
|-----------|-----------|
| `quarkus` | `quarkus-patterns` |
| `spring-boot` | `spring-patterns` |
| `nestjs` | `nestjs-patterns` |
| `express` | `express-patterns` |
| `fastapi` | `fastapi-patterns` |
| `django` | `django-patterns` |
| `gin` | `gin-patterns` |
| `ktor` | `ktor-patterns` |
| `axum` | `axum-patterns` |
| `dotnet` | `dotnet-patterns` |
| `click` | `click-cli-patterns` |

**Rules:**
- Zero external dependencies (stdlib only: `pathlib`, `shutil`).
- Depends on `TemplateEngine.replace_placeholders()` for placeholder substitution.
- Depends on `ProjectConfig` for condition evaluation.
- Each function <= 25 lines per coding standards.
- All file operations are atomic (copy, then replace).

---

### 2.2 Agents Assembler (`ia_dev_env/assembler/agents.py`)

| Function/Class | Description |
|----------------|-------------|
| `AgentsAssembler` | Stateless class that encapsulates all agents assembly logic. |
| `assemble(config, output_dir, src_dir, engine) -> List[Path]` | Main entry point. Copies core agents, conditional agents, developer agent, and injects checklists. Returns list of created paths. |
| `select_core_agents(src_dir) -> List[str]` | Scans `src_dir/agents-templates/core/*.md`. Returns agent filenames. |
| `select_conditional_agents(config) -> List[str]` | Evaluates feature gates and returns list of conditional agent filenames to include. |
| `select_developer_agent(config) -> str` | Returns `"{language_name}-developer.md"` filename. |
| `_copy_core_agent(agent_file, src_dir, output_dir, engine) -> Path` | Copies a single core agent .md file, runs placeholder replacement. |
| `_copy_conditional_agent(agent_file, src_dir, output_dir, engine) -> Optional[Path]` | Copies a conditional agent if source file exists. |
| `_copy_developer_agent(config, src_dir, output_dir, engine) -> Optional[Path]` | Copies the language-specific developer agent. |
| `_inject_checklists(config, output_dir, src_dir, engine) -> None` | Injects conditional checklists into agent files (security, api, devops). |

**Conditional Agents Selection Rules:**

| Agent | Condition |
|-------|-----------|
| `database-engineer.md` | `database.name != "none"` |
| `observability-engineer.md` | `observability.tool != "none"` |
| `devops-engineer.md` | `container != "none"` OR `orchestrator != "none"` OR `iac != "none"` OR `service_mesh != "none"` |
| `api-engineer.md` | `"rest"` or `"grpc"` or `"graphql"` in interface types |
| `event-engineer.md` | `event_driven == True` OR `"event-consumer"` or `"event-producer"` in interface types |

**Developer Agent Mapping:**
- Language name maps directly: `python` -> `python-developer.md`, `java` -> `java-developer.md`, etc.

**Checklist Injection Rules:**

| Target Agent | Checklist | Condition |
|-------------|-----------|-----------|
| `security-engineer.md` | `pci-dss-security.md` | `"pci-dss"` in `security.frameworks` |
| `security-engineer.md` | `privacy-security.md` | `"lgpd"` or `"gdpr"` in `security.frameworks` |
| `security-engineer.md` | `hipaa-security.md` | `"hipaa"` in `security.frameworks` |
| `security-engineer.md` | `sox-security.md` | `"sox"` in `security.frameworks` |
| `api-engineer.md` | `grpc-api.md` | `"grpc"` in interface types |
| `api-engineer.md` | `graphql-api.md` | `"graphql"` in interface types |
| `api-engineer.md` | `websocket-api.md` | `"websocket"` in interface types |
| `devops-engineer.md` | `helm-devops.md` | `templating == "helm"` |
| `devops-engineer.md` | `iac-devops.md` | `iac != "none"` |
| `devops-engineer.md` | `mesh-devops.md` | `service_mesh != "none"` |
| `devops-engineer.md` | `registry-devops.md` | `registry != "none"` |

**Rules:**
- Same constraints as SkillsAssembler.
- Uses `TemplateEngine.inject_section()` for checklist injection (marker-based replacement).
- Uses `TemplateEngine.replace_placeholders()` for placeholder substitution in all copied files.

---

### 2.3 Helper: Interface Type Extractor (`ia_dev_env/assembler/conditions.py`)

| Function | Description |
|----------|-------------|
| `extract_interface_types(config) -> List[str]` | Returns `[iface.type for iface in config.interfaces]`. Shared by both assemblers. |
| `has_interface(config, iface_type) -> bool` | Checks if a specific interface type exists in config. |
| `has_any_interface(config, *types) -> bool` | Checks if any of the specified interface types exist. |

**Rules:**
- Pure functions, no side effects.
- Single responsibility: condition evaluation only.

---

## 3. Existing Classes to Modify

### 3.1 `InfraConfig` in `ia_dev_env/models.py`

**Current state:** Has `container`, `orchestrator`, and `observability` fields only.

**Required additions:**

| Field | Type | Default | Source (bash) |
|-------|------|---------|---------------|
| `templating` | `str` | `"kustomize"` | `TEMPLATING` |
| `iac` | `str` | `"none"` | `IAC` |
| `registry` | `str` | `"none"` | `REGISTRY` |
| `api_gateway` | `str` | `"none"` | `API_GATEWAY` |
| `service_mesh` | `str` | `"none"` | `SERVICE_MESH` |

Update `InfraConfig.from_dict()` to parse these from YAML `infrastructure:` section.

### 3.2 `TestingConfig` in `ia_dev_env/models.py`

**Current state:** Has `smoke_tests`, `contract_tests`, `coverage_line`, `coverage_branch`.

**Required addition:**

| Field | Type | Default | Source (bash) |
|-------|------|---------|---------------|
| `performance_tests` | `bool` | `True` | `PERFORMANCE_TESTS` |

Update `TestingConfig.from_dict()` to parse this field.

### 3.3 `_build_default_context()` in `ia_dev_env/template_engine.py`

Add new fields to the flat context map so placeholders for the new config fields work:
- `templating`, `iac`, `registry`, `api_gateway`, `service_mesh`
- `performance_tests`

### 3.4 `ia_dev_env/assembler/__init__.py`

Currently empty. Update to export `SkillsAssembler` and `AgentsAssembler`.

---

## 4. Dependency Direction Validation

```
assembler/skills.py  ──→  models.py (domain model)
assembler/skills.py  ──→  template_engine.py (engine)
assembler/agents.py  ──→  models.py (domain model)
assembler/agents.py  ──→  template_engine.py (engine)
assembler/conditions.py ──→ models.py (domain model)

template_engine.py ──→ models.py (domain model)
```

**Validation:**
- Assemblers depend on domain models and template engine -- correct direction (application -> domain).
- Assemblers do NOT depend on adapters, CLI, or framework -- correct.
- Domain models do NOT import assembler code -- correct.
- `conditions.py` is a pure utility depending only on domain models -- correct.
- No circular dependencies introduced.

---

## 5. Integration Points

| Integration | Source | Target | Mechanism |
|-------------|--------|--------|-----------|
| Skills assembly | Pipeline (STORY-009) | `SkillsAssembler.assemble()` | Direct function call |
| Agents assembly | Pipeline (STORY-009) | `AgentsAssembler.assemble()` | Direct function call |
| Placeholder replacement | Both assemblers | `TemplateEngine.replace_placeholders()` | Method call |
| Section injection | `AgentsAssembler` | `TemplateEngine.inject_section()` | Static method call |
| Config access | Both assemblers | `ProjectConfig` | Dataclass field access |
| Filesystem | Both assemblers | `pathlib.Path`, `shutil` | stdlib |

---

## 6. Database Changes

N/A -- No database in this project.

---

## 7. API Changes

N/A -- No API in this project. CLI interface unchanged.

---

## 8. Event Changes

N/A -- No events in this project.

---

## 9. Configuration Changes

### 9.1 YAML Config Schema Extensions

The YAML config file gains optional fields under `infrastructure:` and `testing:`:

```yaml
infrastructure:
  container: docker        # existing
  orchestrator: kubernetes # existing
  templating: kustomize    # NEW (default: "kustomize")
  iac: none                # NEW (default: "none")
  registry: none           # NEW (default: "none")
  api_gateway: none        # NEW (default: "none")
  service_mesh: none       # NEW (default: "none")
  observability:           # existing
    tool: none

testing:
  smoke_tests: true        # existing
  contract_tests: false    # existing
  performance_tests: true  # NEW (default: true)
  coverage_line: 95        # existing
  coverage_branch: 90      # existing
```

All new fields are optional with safe defaults, ensuring backward compatibility.

### 9.2 Source Templates Directory

Both assemblers expect source templates at:
- `src/skills-templates/` -- skills templates (core, conditional, knowledge-packs)
- `src/agents-templates/` -- agents templates (core, conditional, developers, checklists)

These directories already exist in the repository.

---

## 10. Risk Assessment

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Model field additions break existing tests | Medium | High | Update `conftest.py` fixtures, add defaults for all new fields, ensure backward-compatible `from_dict()` |
| Template placeholder context mismatch | Medium | Medium | Add new fields to `_build_default_context()` immediately alongside model changes |
| Knowledge pack merge logic (don't clobber existing references/) | High | Medium | Implement exact bash semantics: overwrite SKILL.md, skip-if-exists for other items. Unit test with pre-populated `references/` directory. |
| Infra-patterns conditional logic complexity | Medium | Low | Extract each condition into a named predicate function. Test each predicate independently. |
| Stack pack name resolution mismatch | Medium | Low | Use a constant `STACK_PACK_MAP` dict matching bash `get_stack_pack_name()` exactly. Contract-test against bash output. |
| Checklist injection marker names must match template content | High | Medium | Define marker constants (e.g., `PCI_DSS_CHECKLIST`) and verify they exist in agent templates. Test injection with real template files. |
| `performance_tests` default is `True` in bash but new in Python model | Low | Medium | Set default to `True` in `TestingConfig` to match bash behavior. Document the default. |
| File encoding issues on different platforms | Low | Low | Use explicit `encoding="utf-8"` on all file read/write operations. |

---

## Implementation Order

1. **Model changes** (`models.py`) -- Add new fields to `InfraConfig` and `TestingConfig`
2. **Template context** (`template_engine.py`) -- Add new fields to `_build_default_context()`
3. **Conditions helper** (`assembler/conditions.py`) -- Interface type extraction utilities
4. **Skills assembler** (`assembler/skills.py`) -- Core skills, conditional skills, knowledge packs
5. **Agents assembler** (`assembler/agents.py`) -- Core agents, conditional agents, developer agent, checklist injection
6. **Assembler exports** (`assembler/__init__.py`) -- Export public API
7. **Tests** -- Unit tests for conditions, skills selection, agents selection, copy operations, contract tests vs bash output
8. **Update existing test fixtures** -- Add new model fields to test conftest
