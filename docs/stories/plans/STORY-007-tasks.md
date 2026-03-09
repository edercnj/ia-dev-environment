# STORY-007 Task Breakdown: Skills and Agents Assemblers

**Story:** STORY-007 -- Skills and Agents Assemblers
**Phase:** 3 (Assembly)
**Blocked By:** STORY-001 (models), STORY-004 (template engine)
**Blocks:** STORY-009 (CLI orchestration)

---

## Parallelism Groups

```
G1 (Foundation) ──→ G2 (Conditions) ──→ G3 (Skills Core) ──→ G4 (Skills Advanced) ──→ G6 (Exports) ──→ G7 (Tests)
                                    ──→ G5 (Agents)       ──→ G6 (Exports) ──→ G7 (Tests)
```

G1 has no internal dependencies.
G2 depends on G1 (model changes).
G3 and G5 depend on G2 (conditions helper).
G4 depends on G3 (skills core).
G6 depends on G3, G4, G5.
G7 depends on all previous groups.

---

## G1 -- Foundation (Model Changes)

### G1-T1: Add new fields to `InfraConfig`

**File:** `ia_dev_env/models.py` (lines 163-179)
**Function:** `InfraConfig` dataclass + `from_dict()`
**Changes:**
- Add fields: `templating: str = "kustomize"`, `iac: str = "none"`, `registry: str = "none"`, `api_gateway: str = "none"`, `service_mesh: str = "none"`
- Update `from_dict()` to parse all five new fields with defaults

**Estimated lines changed:** ~10
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from ia_dev_env.models import InfraConfig; c = InfraConfig(); assert c.templating == 'kustomize'; assert c.iac == 'none'; print('OK')"`

---

### G1-T2: Add `performance_tests` to `TestingConfig`

**File:** `ia_dev_env/models.py` (lines 182-196)
**Function:** `TestingConfig` dataclass + `from_dict()`
**Changes:**
- Add field: `performance_tests: bool = True`
- Update `from_dict()` to parse `performance_tests` with default `True`

**Estimated lines changed:** ~3
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from ia_dev_env.models import TestingConfig; t = TestingConfig(); assert t.performance_tests is True; print('OK')"`

---

### G1-T3: Update `_build_default_context()` in template_engine.py

**File:** `ia_dev_env/template_engine.py` (lines 15-34)
**Function:** `_build_default_context()`
**Changes:**
- Add keys: `templating`, `iac`, `registry`, `api_gateway`, `service_mesh` (from `config.infrastructure.*`)
- Add key: `performance_tests` (from `config.testing.performance_tests`)
- Add keys: `smoke_tests`, `contract_tests` (from `config.testing.*`) -- currently missing but needed for assemblers

**Estimated lines changed:** ~8
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from ia_dev_env.template_engine import _build_default_context; from ia_dev_env.models import ProjectConfig; c = ProjectConfig.from_dict({'project':{'name':'t','purpose':'t'},'architecture':{'style':'lib'},'interfaces':[{'type':'cli'}],'language':{'name':'py','version':'3.9'},'framework':{'name':'click','version':'8.1'}}); ctx = _build_default_context(c); assert 'templating' in ctx; assert 'performance_tests' in ctx; print('OK')"`

---

### G1-T4: Update existing test fixtures in conftest.py

**File:** `tests/conftest.py` (lines 44-62)
**Function:** `FULL_PROJECT_DICT` constant
**Changes:**
- Add `templating`, `iac`, `registry`, `api_gateway`, `service_mesh` under `infrastructure`
- Add `performance_tests` under `testing`

**Estimated lines changed:** ~6
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -m pytest tests/test_models.py -x -q 2>&1 | tail -5`

---

## G2 -- Conditions Helper

### G2-T1: Create `assembler/conditions.py`

**File:** `ia_dev_env/assembler/conditions.py` (new file)
**Functions:**
- `extract_interface_types(config: ProjectConfig) -> List[str]` -- returns `[iface.type for iface in config.interfaces]`
- `has_interface(config: ProjectConfig, iface_type: str) -> bool` -- checks if specific interface type exists
- `has_any_interface(config: ProjectConfig, *types: str) -> bool` -- checks if any of the specified types exist

**Rules:**
- Pure functions, no side effects
- Depends only on `ia_dev_env.models.ProjectConfig`
- Each function <= 25 lines

**Estimated lines:** ~30 (including imports, docstrings)
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from ia_dev_env.assembler.conditions import extract_interface_types, has_interface, has_any_interface; print('OK')"`

---

## G3 -- Skills Assembler Core

### G3-T1: Create `assembler/skills.py` with `SkillsAssembler` class skeleton

**File:** `ia_dev_env/assembler/skills.py` (new file)
**Class:** `SkillsAssembler`
**Methods to implement in this task:**
- `select_core_skills(src_dir: Path) -> List[str]` -- scans `src_dir/skills-templates/core/` directories (excluding `lib/`), plus `core/lib/` entries. Returns skill names.
- `select_conditional_skills(config: ProjectConfig) -> List[str]` -- evaluates feature gates against config. Returns conditional skill names matching the 13 rules from the plan.
- `select_knowledge_packs(config: ProjectConfig) -> List[str]` -- evaluates config to determine knowledge packs. Returns pack identifiers.

**Dependencies:**
- `ia_dev_env.models.ProjectConfig`
- `ia_dev_env.assembler.conditions` (extract_interface_types, has_interface, has_any_interface)

**Conditional Skills Selection Logic (13 rules):**

| Skill | Condition |
|-------|-----------|
| `x-review-api` | `has_interface(config, "rest")` |
| `x-review-grpc` | `has_interface(config, "grpc")` |
| `x-review-graphql` | `has_interface(config, "graphql")` |
| `x-review-events` | `has_any_interface(config, "event-consumer", "event-producer")` |
| `instrument-otel` | `config.infrastructure.observability.tool != "none"` |
| `setup-environment` | `config.infrastructure.orchestrator != "none"` |
| `run-smoke-api` | `config.testing.smoke_tests and has_interface(config, "rest")` |
| `run-smoke-socket` | `config.testing.smoke_tests and has_interface(config, "tcp-custom")` |
| `run-e2e` | Always |
| `run-perf-test` | `config.testing.performance_tests` |
| `run-contract-tests` | `config.testing.contract_tests` |
| `x-review-security` | `len(config.security.frameworks) > 0` |
| `x-review-gateway` | `config.infrastructure.api_gateway != "none"` |

**Knowledge Packs Selection Logic:**

| Pack | Condition |
|------|-----------|
| Core KPs (coding-standards, architecture, testing, security, compliance, api-design, observability, resilience, infrastructure, protocols, story-planning) | Always (SKILL.md only) |
| `layer-templates` | Always (full copy) |
| `database-patterns` | `config.data.database.name != "none" or config.data.cache.name != "none"` |

**Estimated lines:** ~120
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from ia_dev_env.assembler.skills import SkillsAssembler; print('OK')"`

---

### G3-T2: Implement `_copy_core_skill` and `_copy_conditional_skill`

**File:** `ia_dev_env/assembler/skills.py`
**Methods:**
- `_copy_core_skill(skill_name: str, src_dir: Path, output_dir: Path, engine: TemplateEngine) -> Path` -- copies a single core skill directory, runs `engine.replace_placeholders()` on SKILL.md
- `_copy_conditional_skill(skill_name: str, src_dir: Path, output_dir: Path, engine: TemplateEngine) -> Optional[Path]` -- copies a conditional skill if source directory exists, returns None if missing

**Rules:**
- Uses `shutil.copytree` for directory copy
- Runs `replace_placeholders()` on every `.md` file in the copied directory
- Each method <= 25 lines
- Explicit `encoding="utf-8"` on all file I/O

**Estimated lines:** ~40
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from ia_dev_env.assembler.skills import SkillsAssembler; print('OK')"`

---

### G3-T3: Implement `_copy_knowledge_pack`

**File:** `ia_dev_env/assembler/skills.py`
**Method:**
- `_copy_knowledge_pack(pack_name: str, src_dir: Path, output_dir: Path, engine: TemplateEngine) -> Optional[Path]` -- copies SKILL.md (always overwrite) + merges non-existing items (preserves `references/` already populated). Returns None if source does not exist.

**Semantics (match bash behavior):**
- Always overwrite `SKILL.md` with placeholder-replaced version
- For other files/dirs (e.g., `references/`): skip if already exists at destination
- Source path: `src_dir/skills-templates/knowledge-packs/{pack_name}/`
- Destination path: `output_dir/skills/{pack_name}/`

**Estimated lines:** ~25
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from ia_dev_env.assembler.skills import SkillsAssembler; print('OK')"`

---

## G4 -- Skills Assembler Advanced

### G4-T1: Add `STACK_PACK_MAP` constant and `_copy_stack_patterns`

**File:** `ia_dev_env/assembler/skills.py`
**Constant:** `STACK_PACK_MAP: Dict[str, str]`

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

**Method:**
- `_copy_stack_patterns(config: ProjectConfig, src_dir: Path, output_dir: Path, engine: TemplateEngine) -> Optional[Path]` -- resolves `config.framework.name` to pack name via `STACK_PACK_MAP`, copies matching stack-patterns knowledge pack from `src_dir/skills-templates/knowledge-packs/stack-patterns/{pack_name}/`. Returns None if framework not in map.

**Estimated lines:** ~30
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from ia_dev_env.assembler.skills import STACK_PACK_MAP; assert STACK_PACK_MAP['click'] == 'click-cli-patterns'; print('OK')"`

---

### G4-T2: Implement `_copy_infra_patterns`

**File:** `ia_dev_env/assembler/skills.py`
**Method:**
- `_copy_infra_patterns(config: ProjectConfig, src_dir: Path, output_dir: Path, engine: TemplateEngine) -> List[Path]` -- copies infra knowledge packs based on infrastructure config

**Infra Packs Selection Logic:**

| Pack | Condition | Source subdir |
|------|-----------|---------------|
| `k8s-deployment` | `config.infrastructure.orchestrator == "kubernetes"` | `infra-patterns/k8s-deployment/` |
| `k8s-kustomize` | `config.infrastructure.templating == "kustomize"` | `infra-patterns/k8s-kustomize/` |
| `k8s-helm` | `config.infrastructure.templating == "helm"` | `infra-patterns/k8s-helm/` |
| `dockerfile` | `config.infrastructure.container != "none"` | `infra-patterns/dockerfile/` |
| `container-registry` | `config.infrastructure.registry != "none"` | `infra-patterns/container-registry/` |
| `iac-terraform` | `config.infrastructure.iac == "terraform"` | `infra-patterns/iac-terraform/` |
| `iac-crossplane` | `config.infrastructure.iac == "crossplane"` | `infra-patterns/iac-crossplane/` |

**Estimated lines:** ~25
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from ia_dev_env.assembler.skills import SkillsAssembler; print('OK')"`

---

### G4-T3: Implement `assemble()` orchestrator method

**File:** `ia_dev_env/assembler/skills.py`
**Method:**
- `assemble(config: ProjectConfig, output_dir: Path, src_dir: Path, engine: TemplateEngine) -> List[Path]` -- main entry point

**Orchestration steps:**
1. Call `select_core_skills(src_dir)` and copy each via `_copy_core_skill()`
2. Call `select_conditional_skills(config)` and copy each via `_copy_conditional_skill()`
3. Call `select_knowledge_packs(config)` and copy each via `_copy_knowledge_pack()`
4. Call `_copy_stack_patterns(config, src_dir, output_dir, engine)`
5. Call `_copy_infra_patterns(config, src_dir, output_dir, engine)`
6. Return aggregated list of all created paths

**Estimated lines:** ~25
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from ia_dev_env.assembler.skills import SkillsAssembler; s = SkillsAssembler(); print('OK')"`

---

## G5 -- Agents Assembler

### G5-T1: Create `assembler/agents.py` with `AgentsAssembler` class + selection methods

**File:** `ia_dev_env/assembler/agents.py` (new file)
**Class:** `AgentsAssembler`
**Methods:**
- `select_core_agents(src_dir: Path) -> List[str]` -- scans `src_dir/agents-templates/core/*.md`. Returns agent filenames.
- `select_conditional_agents(config: ProjectConfig) -> List[str]` -- evaluates feature gates. Returns conditional agent filenames.
- `select_developer_agent(config: ProjectConfig) -> str` -- returns `"{language_name}-developer.md"` filename.

**Conditional Agents Selection Logic:**

| Agent | Condition |
|-------|-----------|
| `database-engineer.md` | `config.data.database.name != "none"` |
| `observability-engineer.md` | `config.infrastructure.observability.tool != "none"` |
| `devops-engineer.md` | `config.infrastructure.container != "none" or config.infrastructure.orchestrator != "none" or config.infrastructure.iac != "none" or config.infrastructure.service_mesh != "none"` |
| `api-engineer.md` | `has_any_interface(config, "rest", "grpc", "graphql")` |
| `event-engineer.md` | `config.architecture.event_driven or has_any_interface(config, "event-consumer", "event-producer")` |

**Dependencies:**
- `ia_dev_env.models.ProjectConfig`
- `ia_dev_env.assembler.conditions` (has_any_interface)

**Estimated lines:** ~70
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from ia_dev_env.assembler.agents import AgentsAssembler; print('OK')"`

---

### G5-T2: Implement copy methods

**File:** `ia_dev_env/assembler/agents.py`
**Methods:**
- `_copy_core_agent(agent_file: str, src_dir: Path, output_dir: Path, engine: TemplateEngine) -> Path` -- copies single core agent .md file with placeholder replacement
- `_copy_conditional_agent(agent_file: str, src_dir: Path, output_dir: Path, engine: TemplateEngine) -> Optional[Path]` -- copies conditional agent if source exists
- `_copy_developer_agent(config: ProjectConfig, src_dir: Path, output_dir: Path, engine: TemplateEngine) -> Optional[Path]` -- copies language-specific developer agent

**Source paths:**
- Core: `src_dir/agents-templates/core/{agent_file}`
- Conditional: `src_dir/agents-templates/conditional/{agent_file}`
- Developer: `src_dir/agents-templates/developers/{language}-developer.md`

**Destination:** `output_dir/agents/{agent_file}`

**Estimated lines:** ~50
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from ia_dev_env.assembler.agents import AgentsAssembler; print('OK')"`

---

### G5-T3: Implement checklist injection logic

**File:** `ia_dev_env/assembler/agents.py`
**Method:**
- `_inject_checklists(config: ProjectConfig, output_dir: Path, src_dir: Path, engine: TemplateEngine) -> None` -- injects conditional checklists into agent files

**Checklist Injection Rules:**

| Target Agent | Checklist File | Condition |
|-------------|----------------|-----------|
| `security-engineer.md` | `pci-dss-security.md` | `"pci-dss"` in `config.security.frameworks` |
| `security-engineer.md` | `privacy-security.md` | `"lgpd"` or `"gdpr"` in `config.security.frameworks` |
| `security-engineer.md` | `hipaa-security.md` | `"hipaa"` in `config.security.frameworks` |
| `security-engineer.md` | `sox-security.md` | `"sox"` in `config.security.frameworks` |
| `api-engineer.md` | `grpc-api.md` | `has_interface(config, "grpc")` |
| `api-engineer.md` | `graphql-api.md` | `has_interface(config, "graphql")` |
| `api-engineer.md` | `websocket-api.md` | `has_interface(config, "websocket")` |
| `devops-engineer.md` | `helm-devops.md` | `config.infrastructure.templating == "helm"` |
| `devops-engineer.md` | `iac-devops.md` | `config.infrastructure.iac != "none"` |
| `devops-engineer.md` | `mesh-devops.md` | `config.infrastructure.service_mesh != "none"` |
| `devops-engineer.md` | `registry-devops.md` | `config.infrastructure.registry != "none"` |

**Mechanism:** Uses `TemplateEngine.inject_section()` with marker-based replacement.
**Checklist source:** `src_dir/agents-templates/checklists/{checklist_file}`

**Estimated lines:** ~40
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from ia_dev_env.assembler.agents import AgentsAssembler; print('OK')"`

---

### G5-T4: Implement `assemble()` orchestrator method

**File:** `ia_dev_env/assembler/agents.py`
**Method:**
- `assemble(config: ProjectConfig, output_dir: Path, src_dir: Path, engine: TemplateEngine) -> List[Path]` -- main entry point

**Orchestration steps:**
1. Call `select_core_agents(src_dir)` and copy each via `_copy_core_agent()`
2. Call `select_conditional_agents(config)` and copy each via `_copy_conditional_agent()`
3. Call `select_developer_agent(config)` and copy via `_copy_developer_agent()`
4. Call `_inject_checklists(config, output_dir, src_dir, engine)`
5. Return aggregated list of all created paths

**Estimated lines:** ~25
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from ia_dev_env.assembler.agents import AgentsAssembler; a = AgentsAssembler(); print('OK')"`

---

## G6 -- Module Exports

### G6-T1: Update `assembler/__init__.py`

**File:** `ia_dev_env/assembler/__init__.py` (currently empty)
**Changes:**
- Import and export `SkillsAssembler` from `.skills`
- Import and export `AgentsAssembler` from `.agents`
- Import and export condition helpers from `.conditions`
- Define `__all__`

**Estimated lines:** ~15
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from ia_dev_env.assembler import SkillsAssembler, AgentsAssembler; print('OK')"`

---

## G7 -- Tests

### G7-T1: Unit tests for conditions

**File:** `tests/test_conditions.py` (new file)
**Test cases:**

| Test Function | Scenario | Expected |
|---------------|----------|----------|
| `test_extract_interface_types_returns_all_types` | Config with rest, grpc, event-consumer | `["rest", "grpc", "event-consumer"]` |
| `test_extract_interface_types_empty_interfaces` | Config with no interfaces raises or returns `[]` | Empty list or handles gracefully |
| `test_has_interface_true` | Config with rest, check "rest" | `True` |
| `test_has_interface_false` | Config with rest, check "grpc" | `False` |
| `test_has_any_interface_true` | Config with rest, check "grpc", "rest" | `True` |
| `test_has_any_interface_false` | Config with cli only, check "rest", "grpc" | `False` |
| `test_has_any_interface_no_args` | Config with rest, check with no types | `False` |

**Estimated lines:** ~60
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -m pytest tests/test_conditions.py -x -q`

---

### G7-T2: Unit tests for skills selection

**File:** `tests/test_skills_assembler.py` (new file)
**Test cases:**

| Test Function | Scenario | Expected |
|---------------|----------|----------|
| `test_select_core_skills_returns_all_core_dirs` | Valid src_dir with core skills | All core skill names (excluding `lib/`) plus lib entries |
| `test_select_conditional_skills_full_config` | Full config with rest, grpc, events, otel, k8s | All applicable conditional skills |
| `test_select_conditional_skills_minimal_config` | CLI-only minimal config | Only `run-e2e` (always included) |
| `test_select_conditional_skills_rest_smoke` | Config with rest + smoke_tests=True | Includes `run-smoke-api` |
| `test_select_conditional_skills_no_smoke` | Config with rest + smoke_tests=False | Excludes `run-smoke-api` |
| `test_select_conditional_skills_performance` | Config with performance_tests=True | Includes `run-perf-test` |
| `test_select_conditional_skills_contract` | Config with contract_tests=True | Includes `run-contract-tests` |
| `test_select_conditional_skills_security` | Config with security frameworks | Includes `x-review-security` |
| `test_select_conditional_skills_gateway` | Config with api_gateway != "none" | Includes `x-review-gateway` |
| `test_select_knowledge_packs_always_included` | Any config | Core KPs + `layer-templates` |
| `test_select_knowledge_packs_database` | Config with database | Includes `database-patterns` |
| `test_select_knowledge_packs_no_database` | Config without database | Excludes `database-patterns` |
| `test_stack_pack_map_all_entries` | Constant check | All 11 framework-to-pack mappings |
| `test_copy_core_skill_creates_dir` | Valid skill name + src | Directory created with SKILL.md |
| `test_copy_core_skill_replaces_placeholders` | Skill with `{language_name}` | Placeholder replaced |
| `test_copy_conditional_skill_exists` | Existing conditional skill | Path returned |
| `test_copy_conditional_skill_missing` | Non-existent skill | Returns None |
| `test_copy_knowledge_pack_overwrites_skill_md` | Pre-existing SKILL.md | SKILL.md overwritten |
| `test_copy_knowledge_pack_preserves_references` | Pre-existing references/ dir | references/ not clobbered |
| `test_copy_stack_patterns_known_framework` | Config with `click` | `click-cli-patterns` pack copied |
| `test_copy_stack_patterns_unknown_framework` | Config with unknown framework | Returns None |
| `test_copy_infra_patterns_kubernetes` | orchestrator=kubernetes | `k8s-deployment` copied |
| `test_copy_infra_patterns_helm` | templating=helm | `k8s-helm` copied |
| `test_copy_infra_patterns_docker` | container=docker | `dockerfile` copied |
| `test_copy_infra_patterns_none` | All infra = none | Empty list |

**Estimated lines:** ~250
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -m pytest tests/test_skills_assembler.py -x -q`

---

### G7-T3: Unit tests for agents selection

**File:** `tests/test_agents_assembler.py` (new file)
**Test cases:**

| Test Function | Scenario | Expected |
|---------------|----------|----------|
| `test_select_core_agents_returns_all_core` | Valid src_dir | All 6 core agent filenames |
| `test_select_conditional_agents_full_config` | Config with db, otel, docker, rest, events | All 5 conditional agents |
| `test_select_conditional_agents_minimal` | CLI-only, no db, no otel | Empty list |
| `test_select_conditional_agents_database_only` | Config with database | Only `database-engineer.md` |
| `test_select_conditional_agents_api` | Config with rest | Includes `api-engineer.md` |
| `test_select_conditional_agents_events` | Config with event-consumer | Includes `event-engineer.md` |
| `test_select_conditional_agents_devops_container` | container != "none" | Includes `devops-engineer.md` |
| `test_select_conditional_agents_devops_iac` | iac != "none" | Includes `devops-engineer.md` |
| `test_select_developer_agent` | language=python | `"python-developer.md"` |
| `test_select_developer_agent_java` | language=java | `"java-developer.md"` |
| `test_copy_core_agent_creates_file` | Valid agent file | File created with placeholders replaced |
| `test_copy_conditional_agent_exists` | Existing conditional agent | Path returned |
| `test_copy_conditional_agent_missing` | Non-existent agent | Returns None |
| `test_copy_developer_agent_exists` | python-developer.md exists | Path returned |
| `test_copy_developer_agent_missing` | unknown-developer.md | Returns None |
| `test_inject_checklists_security_pci` | security frameworks include pci-dss | pci-dss-security.md injected |
| `test_inject_checklists_security_privacy` | security frameworks include lgpd | privacy-security.md injected |
| `test_inject_checklists_api_grpc` | interfaces include grpc | grpc-api.md injected into api-engineer.md |
| `test_inject_checklists_devops_helm` | templating=helm | helm-devops.md injected into devops-engineer.md |
| `test_inject_checklists_devops_iac` | iac=terraform | iac-devops.md injected into devops-engineer.md |
| `test_inject_checklists_no_target_agent` | No api-engineer.md copied | No error, skip gracefully |

**Estimated lines:** ~250
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -m pytest tests/test_agents_assembler.py -x -q`

---

### G7-T4: Integration tests for full assembly

**File:** `tests/test_assembler_integration.py` (new file)
**Test cases:**

| Test Function | Scenario | Expected |
|---------------|----------|----------|
| `test_skills_assemble_full_config` | Full config (java-quarkus-like) with db, cache, k8s, otel | All core skills + conditional + KPs + stack-patterns + infra-patterns present in output dir |
| `test_skills_assemble_minimal_config` | CLI-only minimal config | Only core skills + `run-e2e` + core KPs + `layer-templates` + `click-cli-patterns` |
| `test_agents_assemble_full_config` | Full config with db, otel, docker, rest, grpc | All core agents + all conditional + python-developer + checklists injected |
| `test_agents_assemble_minimal_config` | CLI-only minimal config | Only core agents + python-developer |
| `test_placeholders_replaced_in_all_files` | Full assembly | No `{placeholder}` patterns remain in any output `.md` file |
| `test_knowledge_pack_preserves_references` | Pre-populate references/ then assemble | references/ content unchanged, SKILL.md updated |

**Estimated lines:** ~150
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -m pytest tests/test_assembler_integration.py -x -q`

---

### G7-T5: Update existing tests for model changes

**File:** `tests/test_models.py` (existing)
**Changes:**
- Add tests for `InfraConfig` new fields (defaults, from_dict parsing)
- Add tests for `TestingConfig.performance_tests` (default, from_dict parsing)
- Verify backward compatibility: existing dicts without new fields still parse correctly

**File:** `tests/test_template_engine.py` (existing)
**Changes:**
- Add test verifying `_build_default_context` includes new keys
- Add test verifying `replace_placeholders` handles new placeholder names

**Estimated lines changed:** ~40
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -m pytest tests/test_models.py tests/test_template_engine.py -x -q`

---

## Summary

| Group | Tasks | New Files | Modified Files | Total Est. Lines |
|-------|-------|-----------|----------------|-----------------|
| G1 | 4 | 0 | `models.py`, `template_engine.py`, `conftest.py` | ~27 |
| G2 | 1 | `assembler/conditions.py` | 0 | ~30 |
| G3 | 3 | `assembler/skills.py` | 0 | ~185 |
| G4 | 3 | 0 | `assembler/skills.py` | ~80 |
| G5 | 4 | `assembler/agents.py` | 0 | ~185 |
| G6 | 1 | 0 | `assembler/__init__.py` | ~15 |
| G7 | 5 | `test_conditions.py`, `test_skills_assembler.py`, `test_agents_assembler.py`, `test_assembler_integration.py` | `test_models.py`, `test_template_engine.py` | ~750 |
| **Total** | **21** | **5** | **6** | **~1272** |

## Full Validation Command

```bash
cd /Users/edercnj/workspaces/claude-environment && python -m pytest tests/ -x -q --tb=short && python -m pytest tests/ --cov=ia_dev_env --cov-report=term-missing --cov-fail-under=95
```
