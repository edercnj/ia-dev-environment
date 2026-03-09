# Implementation Plan: STORY-005 -- Rules Assembly

**Story:** STORY-005 -- Assembler de Regras (.claude/rules/)
**Phase:** 2 (Assembly)
**Blocked By:** STORY-001 (models), STORY-004 (template engine)
**Blocks:** STORY-009 (CLI orchestration)
**Architecture:** library (hexagonal-lite for a CLI tool)
**Stack:** Python 3.9 + Click 8.1

---

## 1. Affected Layers and Components

| Layer | Affected? | Rationale |
|-------|-----------|-----------|
| domain/model | NO | `ProjectConfig`, `ResolvedStack` already exist from STORY-001/003 |
| domain/engine | YES | `RulesAssembler` contains business logic for rule file generation, consolidation, and auditing |
| domain/port | NO | No ports needed -- file system operations use stdlib `pathlib.Path` (same rationale as config loading) |
| application | NO | No use case orchestration yet; assembler is invoked directly from pipeline (STORY-009) |
| adapter/inbound | NO | No CLI changes in this story; wiring deferred to STORY-009 |
| adapter/outbound | NO | File I/O uses stdlib `pathlib.Path` -- no adapter needed |
| config | NO | No framework configuration changes |
| tests | YES | Unit, contract, and integration tests for all assembly functions |

---

## 2. New Classes/Interfaces to Create

### 2.1 Domain -- Stack Pack Mapping (`ia_dev_env/domain/stack_pack_mapping.py`)

Pure data mapping from framework name to knowledge pack directory name. Ports the bash `get_stack_pack_name()` function.

| Constant | Type | Description |
|----------|------|-------------|
| `FRAMEWORK_STACK_PACK` | `Dict[str, str]` | Maps framework name to stack pack name (e.g., `"quarkus"` -> `"quarkus-patterns"`, `"click"` -> `"click-cli-patterns"`) |

| Function | Signature | Description |
|----------|-----------|-------------|
| `get_stack_pack_name` | `(framework: str) -> str` | Returns the stack pack directory name for a framework. Returns empty string for unknown frameworks. |

**Mapping (from `setup.sh` line 304-320):**

```
"quarkus"     -> "quarkus-patterns"
"spring-boot" -> "spring-patterns"
"nestjs"      -> "nestjs-patterns"
"express"     -> "express-patterns"
"fastapi"     -> "fastapi-patterns"
"django"      -> "django-patterns"
"gin"         -> "gin-patterns"
"ktor"        -> "ktor-patterns"
"axum"        -> "axum-patterns"
"dotnet"      -> "dotnet-patterns"
"click"       -> "click-cli-patterns"
```

**Rules:**
- Zero external dependencies (standard library only).
- Pure function with no side effects.

### 2.2 Domain -- Version Directory Resolution (`ia_dev_env/domain/version_resolver.py`)

Ports the bash `find_version_dir()` function (line 736-744).

| Function | Signature | Description |
|----------|-----------|-------------|
| `find_version_dir` | `(base_dir: Path, name: str, version: str) -> Optional[Path]` | Finds the version-specific directory. Tries exact match (`{name}-{version}`), then major version with `.x` suffix (`{name}-{major}.x`). Returns `None` if not found. |

**Rules:**
- Zero external dependencies (standard library only).
- Returns `None` instead of empty string (Python convention, not bash).
- Pure function with no side effects.

### 2.3 Domain -- Core Rules KP Routing Map (`ia_dev_env/domain/core_kp_routing.py`)

Defines the static mapping of core detailed rules to knowledge pack destinations. Ports the bash Layer 1b logic (lines 1267-1326).

| Constant | Type | Description |
|----------|------|-------------|
| `CORE_TO_KP_MAPPING` | `List[CoreKpRoute]` | Ordered list of (source_filename, kp_name, subfolder, dest_filename) tuples |
| `CONDITIONAL_CORE_KP` | `List[ConditionalCoreKpRoute]` | Routes that depend on config values (e.g., cloud-native only for non-library) |

```python
@dataclass(frozen=True)
class CoreKpRoute:
    source_file: str        # e.g., "01-clean-code.md"
    kp_name: str            # e.g., "coding-standards"
    dest_file: str          # e.g., "clean-code.md"

@dataclass(frozen=True)
class ConditionalCoreKpRoute(CoreKpRoute):
    condition_field: str    # e.g., "architecture_style"
    condition_exclude: str  # e.g., "library"
```

**Static mapping (from `setup.sh` lines 1271-1326):**

| Source | KP Name | Dest File |
|--------|---------|-----------|
| `01-clean-code.md` | `coding-standards` | `clean-code.md` |
| `02-solid-principles.md` | `coding-standards` | `solid-principles.md` |
| `03-testing-philosophy.md` | `testing` | `testing-philosophy.md` |
| `05-architecture-principles.md` | `architecture` | `architecture-principles.md` |
| `06-api-design-principles.md` | `api-design` | `api-design-principles.md` |
| `07-security-principles.md` | `security` | `security-principles.md` |
| `08-observability-principles.md` | `observability` | `observability-principles.md` |
| `09-resilience-principles.md` | `resilience` | `resilience-principles.md` |
| `10-infrastructure-principles.md` | `infrastructure` | `infrastructure-principles.md` |
| `11-database-principles.md` | `database-patterns` | `database-principles.md` |
| `12-cloud-native-principles.md` | `infrastructure` | `cloud-native-principles.md` (**conditional**: only if `architecture_style != "library"`) |
| `13-story-decomposition.md` | `story-planning` | `story-decomposition.md` |

### 2.4 Domain -- Rules Assembler (`ia_dev_env/assembler/rules_assembler.py`)

Main assembler class. Ports the bash `assemble_rules()` function and its related helpers.

| Method | Signature | Description |
|--------|-----------|-------------|
| `assemble` | `(config: ProjectConfig, output_dir: Path, engine: TemplateEngine) -> List[Path]` | Orchestrates all layers and returns list of generated file paths |
| `_copy_core_rules` | `(config: ProjectConfig, src_dir: Path, rules_dir: Path, engine: TemplateEngine) -> List[Path]` | Layer 1: Copy and process `src/core-rules/*.md` |
| `_route_core_to_knowledge_packs` | `(config: ProjectConfig, src_dir: Path, skills_dir: Path) -> List[Path]` | Layer 1b: Route `src/core/*.md` to KP destinations |
| `_copy_language_knowledge_packs` | `(config: ProjectConfig, src_dir: Path, skills_dir: Path) -> List[Path]` | Layer 2: Route language files to coding-standards and testing KPs |
| `_copy_framework_knowledge_packs` | `(config: ProjectConfig, src_dir: Path, skills_dir: Path) -> List[Path]` | Layer 3: Route framework files to stack-patterns KP |
| `_generate_project_identity` | `(config: ProjectConfig, rules_dir: Path) -> Path` | Layer 4: Generate `01-project-identity.md` with dynamic values |
| `_copy_domain_template` | `(config: ProjectConfig, src_dir: Path, rules_dir: Path) -> Path` | Layer 4: Copy/generate `02-domain.md` |

| Method | Signature | Description |
|--------|-----------|-------------|
| `_copy_database_references` | `(config: ProjectConfig, src_dir: Path, skills_dir: Path) -> List[Path]` | Conditional: copy DB refs to `database-patterns` KP |
| `_copy_cache_references` | `(config: ProjectConfig, src_dir: Path, skills_dir: Path) -> List[Path]` | Conditional: copy cache refs to `database-patterns` KP |
| `_assemble_security_rules` | `(config: ProjectConfig, src_dir: Path, skills_dir: Path) -> List[Path]` | Conditional: copy security files to security/compliance KPs |
| `_assemble_cloud_knowledge` | `(config: ProjectConfig, src_dir: Path, skills_dir: Path) -> List[Path]` | Conditional: copy cloud provider file to KPs |
| `_assemble_infrastructure_knowledge` | `(config: ProjectConfig, src_dir: Path, skills_dir: Path) -> List[Path]` | Conditional: copy infra files (k8s, containers, IaC) to KPs |

### 2.5 Domain -- Rules Consolidator (`ia_dev_env/assembler/consolidator.py`)

Ports `consolidate_rules()` (line 1589) and `consolidate_framework_rules()` (line 1612).

| Function | Signature | Description |
|----------|-----------|-------------|
| `consolidate_files` | `(output_path: Path, source_paths: List[Path]) -> None` | Merges multiple source files into a single output with generated header and `---` separators |
| `consolidate_framework_rules` | `(framework: str, rules_dir: Path, source_dir: Path) -> List[Path]` | Groups framework files into 3 consolidated outputs: `30-{fw}-core.md`, `31-{fw}-data.md`, `32-{fw}-operations.md` |

**Framework file grouping (from `setup.sh` lines 1617-1650):**

| Group | File number | Glob patterns |
|-------|-------------|---------------|
| Core | `30-{fw}-core.md` | `*-cdi*`, `*-di*`, `*-config*`, `*-web*`, `*-resteasy*`, `*-middleware*`, `*-resilience*` |
| Data | `31-{fw}-data.md` | `*-panache*`, `*-jpa*`, `*-prisma*`, `*-sqlalchemy*`, `*-exposed*`, `*-ef*`, `*-orm*`, `*-database*` |
| Operations | `32-{fw}-operations.md` | `*-testing*`, `*-observability*`, `*-native-build*`, `*-infrastructure*` |

### 2.6 Domain -- Rules Auditor (`ia_dev_env/assembler/auditor.py`)

Ports `audit_rules_context()` (line 1654).

| Function | Signature | Description |
|----------|-----------|-------------|
| `audit_rules_context` | `(rules_dir: Path) -> AuditResult` | Counts rule files and total size, checks against thresholds |

```python
@dataclass(frozen=True)
class AuditResult:
    total_files: int
    total_bytes: int
    file_sizes: List[Tuple[str, int]]  # (filename, bytes) sorted largest first
    warnings: List[str]
```

**Thresholds (from `setup.sh` lines 1671-1682):**
- Max files: 10 (warn if exceeded)
- Max total size: 50KB (warn if exceeded)

### 2.7 Tests

| File | Category | Coverage Target |
|------|----------|----------------|
| `tests/assembler/__init__.py` | Package | -- |
| `tests/assembler/test_rules_assembler.py` | Unit | All assembly layers, conditional inclusion/exclusion |
| `tests/assembler/test_consolidator.py` | Unit | File merging, framework grouping |
| `tests/assembler/test_auditor.py` | Unit | Threshold checks, file counting |
| `tests/domain/test_stack_pack_mapping.py` | Unit | All framework-to-pack-name mappings |
| `tests/domain/test_version_resolver.py` | Unit | Exact match, major version fallback, not found |
| `tests/domain/test_core_kp_routing.py` | Unit | Static mapping correctness, conditional filtering |
| `tests/test_rules_assembly_contract.py` | Contract | Byte-by-byte comparison with bash output for reference config |

---

## 3. Existing Classes to Modify

| File | Change | Reason |
|------|--------|--------|
| `ia_dev_env/assembler/__init__.py` | Add public imports for `RulesAssembler` | Package API surface |
| `ia_dev_env/template_engine.py` | No changes required | `replace_placeholders()`, `concat_files()`, `render_template()` already provide needed functionality |
| `ia_dev_env/models.py` | No changes required | `ProjectConfig` and all sub-models already exist |
| `ia_dev_env/domain/stack_mapping.py` | No changes required | Read-only dependency for framework validation |

---

## 4. Dependency Direction Validation

```
ia_dev_env/domain/stack_pack_mapping.py   -> standard library only                          (DOMAIN)
ia_dev_env/domain/version_resolver.py     -> standard library only                          (DOMAIN)
ia_dev_env/domain/core_kp_routing.py      -> standard library only                          (DOMAIN)
ia_dev_env/assembler/rules_assembler.py   -> ia_dev_env.models                            (DOMAIN -> DOMAIN)
                                            -> ia_dev_env.template_engine                    (DOMAIN -> DOMAIN)
                                            -> ia_dev_env.domain.stack_pack_mapping          (DOMAIN -> DOMAIN)
                                            -> ia_dev_env.domain.version_resolver            (DOMAIN -> DOMAIN)
                                            -> ia_dev_env.domain.core_kp_routing             (DOMAIN -> DOMAIN)
                                            -> ia_dev_env.assembler.consolidator             (DOMAIN -> DOMAIN)
                                            -> ia_dev_env.assembler.auditor                  (DOMAIN -> DOMAIN)
ia_dev_env/assembler/consolidator.py      -> standard library only                          (DOMAIN)
ia_dev_env/assembler/auditor.py           -> standard library only                          (DOMAIN)
tests/                                      -> pytest, ia_dev_env.*                         (TEST -> all layers)
```

**Validation checklist:**
- All new modules depend only on standard library and domain-layer modules. No Click, Jinja2, or framework imports in domain modules.
- `template_engine.py` is a domain-level utility (wraps Jinja2 for template rendering). It was designed to be passed as a dependency, not imported by domain code directly. The assembler receives it as an argument, not as an import-time dependency.
- `rules_assembler.py` receives `TemplateEngine` as a parameter in `assemble()`, following dependency injection. It does not import Jinja2 directly.
- No circular dependencies exist.
- No adapter-to-adapter coupling.

**Note on TemplateEngine:** The `TemplateEngine` class imports Jinja2, which is a template library (similar to how `config.py` uses PyYAML). The assembler receives the engine instance via method parameter injection, keeping the assembler module free of Jinja2 imports. This is consistent with the STORY-004 design decision.

---

## 5. Integration Points

| Integration Point | Direction | Details |
|-------------------|-----------|---------|
| `src/core-rules/*.md` | Inbound (file read) | Layer 1: Read core rule markdown files from source tree |
| `src/core/*.md` | Inbound (file read) | Layer 1b: Read detailed core rules for KP routing |
| `src/languages/{lang}/` | Inbound (file read) | Layer 2: Read language-specific knowledge pack files |
| `src/frameworks/{fw}/` | Inbound (file read) | Layer 3: Read framework-specific knowledge pack files |
| `src/databases/` | Inbound (file read) | Conditional: Read database reference files |
| `src/security/` | Inbound (file read) | Conditional: Read security knowledge files |
| `src/cloud-providers/` | Inbound (file read) | Conditional: Read cloud provider files |
| `src/infrastructure/` | Inbound (file read) | Conditional: Read infrastructure files |
| `src/templates/` | Inbound (file read) | Domain template, project identity template |
| `TemplateEngine` (STORY-004) | Internal | Passed as argument for placeholder replacement and template rendering |
| `ProjectConfig` (STORY-001) | Internal | Read-only access to project configuration values |
| Output directory | Outbound (file write) | Write generated rules to `output_dir/rules/` and `output_dir/skills/` |
| STORY-009 orchestration | Downstream | `RulesAssembler.assemble()` is called by the pipeline orchestrator |

**Contract guarantees:**
- `assemble(config, output_dir, engine)` always returns a non-empty `List[Path]` of generated files, or raises an exception. Never returns `None` or empty list for valid config.
- All returned paths exist on disk after `assemble()` completes.
- `consolidate_files()` creates parent directories if needed.
- `audit_rules_context()` is read-only; never modifies files.

---

## 6. Database Changes

**Not applicable.** This is a CLI tool with no database.

---

## 7. API Changes

**Not applicable.** No REST/gRPC interfaces. No CLI changes in this story (deferred to STORY-009).

---

## 8. Event Changes

**Not applicable.** `event_driven: false` in project config.

---

## 9. Configuration Changes

### 9.1 `pyproject.toml`

No new dependencies required. `jinja2` and `pyyaml` are already declared from STORY-001/004.

### 9.2 Environment Variables

None required for this story.

### 9.3 Test Fixtures

New test fixture files and directories needed:

| Path | Purpose |
|------|---------|
| `tests/assembler/__init__.py` | Test package init |
| `tests/fixtures/src/core-rules/` | Minimal core rule files for testing |
| `tests/fixtures/src/core/` | Minimal detailed core rule files for testing |
| `tests/fixtures/src/languages/python/common/` | Language common files for testing |
| `tests/fixtures/src/languages/python/python-3.9/` | Language version files for testing |
| `tests/fixtures/src/frameworks/click/common/` | Framework common files for testing |
| `tests/fixtures/src/templates/domain-template.md` | Domain template for testing |
| `tests/fixtures/src/databases/` | Database reference files for testing |
| `tests/fixtures/src/security/` | Security files for testing |

**Strategy:** Tests should use `tmp_path` (pytest fixture) for output directories. For source directories, use either real `src/` paths (contract tests) or minimal fixture copies (unit tests) to keep tests fast and isolated.

---

## 10. Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Byte-by-byte compatibility with bash output | High | High | Use contract tests comparing Python output against pre-captured bash output for a reference config. Focus on content equivalence (normalize line endings) rather than strict byte identity on all platforms. |
| Large number of file I/O operations in tests | Medium | Medium | Use `tmp_path` for all output. Use real `src/` tree for contract tests only. Unit tests use minimal fixtures. |
| Framework file grouping patterns may not match all edge cases | Medium | Medium | The glob patterns for `consolidate_framework_rules` are explicit pattern lists from the bash. Port them as string contains checks (`"-cdi" in filename`). Add parameterized tests for each pattern group. |
| `find_version_dir` path resolution differences between bash and Python | Low | Medium | Bash uses shell globbing; Python uses `Path.is_dir()`. Behavior is equivalent for the directory existence check pattern used. |
| Missing source directories (e.g., language pack not found) | Medium | Low | Match bash behavior: log warning and continue. Never fail the entire assembly for missing optional content. Return empty list for that layer. |
| `TemplateEngine` dependency on `src_dir` for Jinja2 loader | Medium | Medium | The engine is pre-configured with `src_dir` as the template search root. The assembler uses `engine.replace_placeholders()` for simple substitution and `engine.render_template()` for Jinja2 templates. Both are already tested in STORY-004. |
| Python 3.9 compatibility | Low | High | No `match/case`, no `X \| Y` union syntax. Use `if/elif` chains. Use `Dict[str, str]` not `dict[str, str]`. Use `from __future__ import annotations` in all modules. |
| Assembler method count exceeds 25-line limit | Medium | Medium | Split into focused private methods. Each layer is a separate private method. Consolidation and auditing are in separate modules. The `assemble()` orchestrator method delegates to layer methods, keeping each under 25 lines. |
| `generate_project_identity` uses heredoc in bash | Low | Low | Python equivalent: use Jinja2 template or f-string. Prefer Jinja2 template via `engine.render_template()` to match the template engine pattern. If a template file exists at `src/templates/project-identity-template.md`, use it. Otherwise, generate with formatted string. |

---

## Implementation Order

Execute sub-tasks in this sequence:

1. **Create `ia_dev_env/domain/stack_pack_mapping.py`** -- `FRAMEWORK_STACK_PACK` dict, `get_stack_pack_name()` function
2. **Create `ia_dev_env/domain/version_resolver.py`** -- `find_version_dir()` function
3. **Create `ia_dev_env/domain/core_kp_routing.py`** -- `CoreKpRoute`, `ConditionalCoreKpRoute`, routing constants
4. **Create `ia_dev_env/assembler/auditor.py`** -- `AuditResult` dataclass, `audit_rules_context()` function
5. **Create `ia_dev_env/assembler/consolidator.py`** -- `consolidate_files()`, `consolidate_framework_rules()` functions
6. **Create `ia_dev_env/assembler/rules_assembler.py`** -- `RulesAssembler` class with all layer methods
7. **Update `ia_dev_env/assembler/__init__.py`** -- export `RulesAssembler`
8. **Create `tests/domain/test_stack_pack_mapping.py`** -- unit tests for all framework mappings
9. **Create `tests/domain/test_version_resolver.py`** -- unit tests for version dir resolution
10. **Create `tests/domain/test_core_kp_routing.py`** -- unit tests for routing map
11. **Create `tests/assembler/__init__.py`** and test files -- unit tests for assembler, consolidator, auditor
12. **Create `tests/test_rules_assembly_contract.py`** -- contract tests comparing with bash reference output
13. **Validate** -- `pytest --cov`, verify >= 95% line coverage, >= 90% branch coverage

---

## File Tree (Final State after STORY-005)

```
ia_dev_env/
    __init__.py                          # (unchanged)
    __main__.py                          # (unchanged)
    models.py                            # (unchanged)
    exceptions.py                        # (unchanged)
    config.py                            # (unchanged)
    interactive.py                       # (unchanged)
    template_engine.py                   # (unchanged)
    domain/
        __init__.py                      # (unchanged)
        resolved_stack.py                # (unchanged)
        resolver.py                      # (unchanged)
        stack_mapping.py                 # (unchanged)
        validator.py                     # (unchanged)
        stack_pack_mapping.py            # NEW: framework -> KP name mapping
        version_resolver.py              # NEW: find_version_dir()
        core_kp_routing.py              # NEW: core rule -> KP routing map
    assembler/
        __init__.py                      # MODIFIED: export RulesAssembler
        rules_assembler.py              # NEW: main assembler class
        consolidator.py                  # NEW: file consolidation logic
        auditor.py                       # NEW: rules context auditing
tests/
    __init__.py
    conftest.py
    domain/
        __init__.py
        test_resolved_stack.py           # (unchanged)
        test_resolver.py                 # (unchanged)
        test_stack_mapping.py            # (unchanged)
        test_validator.py                # (unchanged)
        test_stack_pack_mapping.py       # NEW
        test_version_resolver.py         # NEW
        test_core_kp_routing.py          # NEW
    assembler/
        __init__.py                      # NEW
        test_rules_assembler.py          # NEW
        test_consolidator.py             # NEW
        test_auditor.py                  # NEW
    test_rules_assembly_contract.py      # NEW: byte-by-byte contract tests
```

---

## Acceptance Criteria Traceability

| Gherkin Scenario | Validated By |
|------------------|-------------|
| Generate rules for java-quarkus with all sections | `test_rules_assembler.py::test_assemble_full_config_generates_all_core_rules` |
| Exclude security rules when not configured | `test_rules_assembler.py::test_assemble_no_security_skips_security_files` |
| Consolidate language knowledge packs | `test_consolidator.py::test_consolidate_files_merges_without_duplication` |
| Output identical to bash | `test_rules_assembly_contract.py::test_rules_output_matches_bash_reference` |
| Layer 1: core rules copied with placeholder replacement | `test_rules_assembler.py::test_copy_core_rules_replaces_placeholders` |
| Layer 1b: core detailed rules routed to KPs | `test_rules_assembler.py::test_route_core_to_knowledge_packs` |
| Layer 2: language KPs routed correctly (testing to testing KP, rest to coding-standards) | `test_rules_assembler.py::test_copy_language_kps_routes_testing_files` |
| Layer 3: framework KPs routed to stack-patterns with correct pack name | `test_rules_assembler.py::test_copy_framework_kps_uses_stack_pack_name` |
| Layer 4: project identity generated with config values | `test_rules_assembler.py::test_generate_project_identity_contains_config_values` |
| Conditional database references | `test_rules_assembler.py::test_copy_database_references_for_postgresql` |
| Conditional cache references | `test_rules_assembler.py::test_copy_cache_references_for_redis` |
| Cloud knowledge skipped when no provider | `test_rules_assembler.py::test_assemble_cloud_knowledge_skipped_for_none` |
| Infrastructure knowledge for kubernetes | `test_rules_assembler.py::test_assemble_infrastructure_for_kubernetes` |
| Framework consolidation into 3 groups | `test_consolidator.py::test_consolidate_framework_rules_produces_three_files` |
| Audit warns on file count exceeding 10 | `test_auditor.py::test_audit_warns_on_excessive_file_count` |
| Audit warns on total size exceeding 50KB | `test_auditor.py::test_audit_warns_on_excessive_total_size` |
| Coverage >= 95% line, >= 90% branch | `pytest --cov` with `fail_under=95` in pyproject.toml |

---

## Bash-to-Python Porting Reference

### Function Mapping

| Bash Function (setup.sh) | Python Equivalent | Module |
|---------------------------|-------------------|--------|
| `assemble_rules()` (line 1245) | `RulesAssembler.assemble()` | `assembler/rules_assembler.py` |
| `generate_project_identity()` (line 1444) | `RulesAssembler._generate_project_identity()` | `assembler/rules_assembler.py` |
| `copy_database_references()` (line 1507) | `RulesAssembler._copy_database_references()` | `assembler/rules_assembler.py` |
| `copy_cache_references()` (line 1542) | `RulesAssembler._copy_cache_references()` | `assembler/rules_assembler.py` |
| `consolidate_rules()` (line 1589) | `consolidate_files()` | `assembler/consolidator.py` |
| `consolidate_framework_rules()` (line 1612) | `consolidate_framework_rules()` | `assembler/consolidator.py` |
| `audit_rules_context()` (line 1655) | `audit_rules_context()` | `assembler/auditor.py` |
| `assemble_security_rules()` (line 1697) | `RulesAssembler._assemble_security_rules()` | `assembler/rules_assembler.py` |
| `assemble_cloud_knowledge()` (line 1739) | `RulesAssembler._assemble_cloud_knowledge()` | `assembler/rules_assembler.py` |
| `assemble_infrastructure_knowledge()` (line 1758) | `RulesAssembler._assemble_infrastructure_knowledge()` | `assembler/rules_assembler.py` |
| `get_stack_pack_name()` (line 304) | `get_stack_pack_name()` | `domain/stack_pack_mapping.py` |
| `find_version_dir()` (line 736) | `find_version_dir()` | `domain/version_resolver.py` |
| `replace_placeholders()` | `TemplateEngine.replace_placeholders()` | `template_engine.py` (STORY-004, existing) |

### Source Directory Mapping

| Bash Variable | Python Equivalent (relative to `src_dir: Path`) |
|---------------|--------------------------------------------------|
| `SCRIPT_DIR` / `CORE_DIR` | `src_dir / "core"` |
| `core_rules_dir` | `src_dir / "core-rules"` |
| `LANGUAGES_DIR` | `src_dir / "languages"` |
| `FRAMEWORKS_DIR` | `src_dir / "frameworks"` |
| `DATABASES_DIR` | `src_dir / "databases"` |
| `TEMPLATES_DIR` | `src_dir / "templates"` |
| `SECURITY_DIR` | `src_dir / "security"` |
| `CLOUD_PROVIDERS_DIR` | `src_dir / "cloud-providers"` |
| `INFRASTRUCTURE_DIR` | `src_dir / "infrastructure"` |

### Output Directory Mapping

| Bash Path | Python Equivalent (relative to `output_dir: Path`) |
|-----------|-----------------------------------------------------|
| `${OUTPUT_DIR}/rules/` | `output_dir / "rules"` |
| `${OUTPUT_DIR}/skills/` | `output_dir / "skills"` |
| `${OUTPUT_DIR}/skills/{kp_name}/references/` | `output_dir / "skills" / kp_name / "references"` |
