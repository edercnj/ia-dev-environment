# STORY-005: Rules Assembly -- Task Decomposition

**Story:** STORY-005 -- Assembler de Regras (.claude/rules/)
**Decomposed from:** `docs/plans/STORY-005-plan.md`
**Architecture:** library (hexagonal-lite), Python 3.9, Click 8.1
**Total Tasks:** 17

---

## Parallelism Groups Overview

```
G1: FOUNDATION (Domain data mappings)           -- PARALLEL (3 tasks, no dependencies)
G2: DOMAIN LOGIC (Version resolution, routing)  -- PARALLEL (2 tasks, depends on G1)
G3: ASSEMBLER SUPPORT (Consolidator + Auditor)  -- PARALLEL (2 tasks, no cross-dependency)
G4: ASSEMBLER CORE (RulesAssembler class)       -- SEQUENTIAL (1 task, depends on G1+G2+G3)
G5: INTEGRATION (Package wiring)                -- SEQUENTIAL (1 task, depends on G4)
G6: UNIT TESTS (All modules)                    -- PARALLEL (6 tasks, depends on tested module)
G7: CONTRACT/INTEGRATION TESTS                  -- SEQUENTIAL (2 tasks, depends on G4+G6)
```

---

## G1: FOUNDATION -- Domain Data Mappings

These tasks create pure data structures and lookup functions with zero external dependencies. All are independent and can run in parallel.

### G1-T1: Stack Pack Mapping Dictionary

| Field | Value |
|-------|-------|
| **Task ID** | G1-T1 |
| **Module** | `claude_setup/domain/stack_pack_mapping.py` |
| **Layer** | domain |
| **Tier** | Junior |
| **Context Budget** | S |
| **Dependencies** | None |

**Description:**
Create a module containing the `FRAMEWORK_STACK_PACK` dictionary and the `get_stack_pack_name()` function. This is a pure data mapping from framework name (e.g., `"quarkus"`) to knowledge pack directory name (e.g., `"quarkus-patterns"`). Port from bash `get_stack_pack_name()` (setup.sh line 304-320).

**Implementation details:**
- `FRAMEWORK_STACK_PACK: Dict[str, str]` -- 11 entries (quarkus, spring-boot, nestjs, express, fastapi, django, gin, ktor, axum, dotnet, click)
- `get_stack_pack_name(framework: str) -> str` -- Returns pack name or empty string for unknown frameworks
- Use `from __future__ import annotations`
- Zero external dependencies (standard library only)
- Pure function, no side effects

**Acceptance Criteria:**
- [ ] All 11 framework-to-pack mappings are present and match bash source
- [ ] Unknown frameworks return empty string (not None, not exception)
- [ ] No imports outside standard library
- [ ] Module passes `mypy --strict` (if configured)
- [ ] Method body <= 25 lines

---

### G1-T2: Core KP Route Dataclasses

| Field | Value |
|-------|-------|
| **Task ID** | G1-T2 |
| **Module** | `claude_setup/domain/core_kp_routing.py` |
| **Layer** | domain |
| **Tier** | Junior |
| **Context Budget** | S |
| **Dependencies** | None |

**Description:**
Create frozen dataclasses `CoreKpRoute` and `ConditionalCoreKpRoute`, plus the static routing constants `CORE_TO_KP_MAPPING` and `CONDITIONAL_CORE_KP`. Port from bash Layer 1b logic (setup.sh lines 1267-1326).

**Implementation details:**
- `CoreKpRoute(frozen=True)`: fields `source_file`, `kp_name`, `dest_file` (all `str`)
- `ConditionalCoreKpRoute(CoreKpRoute)`: adds `condition_field` and `condition_exclude` (both `str`)
- `CORE_TO_KP_MAPPING: List[CoreKpRoute]` -- 11 unconditional routes
- `CONDITIONAL_CORE_KP: List[ConditionalCoreKpRoute]` -- 1 entry (cloud-native, excluded for library)
- Use `from __future__ import annotations`
- Zero external dependencies

**Acceptance Criteria:**
- [ ] All 11 unconditional routes match plan section 2.3 table exactly
- [ ] Conditional route for `12-cloud-native-principles.md` has `condition_field="architecture_style"` and `condition_exclude="library"`
- [ ] Dataclasses are frozen (immutable)
- [ ] No imports outside standard library

---

### G1-T3: Audit Result Dataclass

| Field | Value |
|-------|-------|
| **Task ID** | G1-T3 |
| **Module** | `claude_setup/assembler/auditor.py` (dataclass only) |
| **Layer** | domain |
| **Tier** | Junior |
| **Context Budget** | S |
| **Dependencies** | None |

**Description:**
Create the `AuditResult` frozen dataclass and the `audit_rules_context()` function. Port from bash `audit_rules_context()` (setup.sh line 1654).

**Implementation details:**
- `AuditResult(frozen=True)`: fields `total_files: int`, `total_bytes: int`, `file_sizes: List[Tuple[str, int]]` (sorted largest first), `warnings: List[str]`
- `audit_rules_context(rules_dir: Path) -> AuditResult` -- scans directory, counts files and sizes, checks thresholds
- Thresholds: max 10 files (warn), max 50KB total (warn)
- Read-only function; never modifies files
- Zero external dependencies

**Acceptance Criteria:**
- [ ] `AuditResult` is frozen dataclass with all 4 fields
- [ ] Warning generated when file count > 10
- [ ] Warning generated when total size > 51200 bytes (50KB)
- [ ] `file_sizes` sorted descending by size
- [ ] Function handles empty directory (0 files, 0 bytes, no warnings)
- [ ] Function handles non-existent directory gracefully

---

## G2: DOMAIN LOGIC -- Version Resolution and Routing

These tasks implement domain functions that depend on G1 data structures.

### G2-T1: Version Directory Resolver

| Field | Value |
|-------|-------|
| **Task ID** | G2-T1 |
| **Module** | `claude_setup/domain/version_resolver.py` |
| **Layer** | domain |
| **Tier** | Mid |
| **Context Budget** | M |
| **Dependencies** | None (standalone, but logically related to G1) |

**Description:**
Create `find_version_dir()` function that resolves version-specific directories with fallback. Port from bash `find_version_dir()` (setup.sh line 736-744).

**Implementation details:**
- `find_version_dir(base_dir: Path, name: str, version: str) -> Optional[Path]`
- Resolution order: (1) exact match `{name}-{version}`, (2) major version fallback `{name}-{major}.x`
- Major version extracted by splitting version on `.` and taking first element
- Returns `None` if neither directory exists (Python convention, not empty string)
- Uses `Path.is_dir()` for existence checks
- Zero external dependencies
- Use `from __future__ import annotations`

**Acceptance Criteria:**
- [ ] Returns exact match path when `{name}-{version}` directory exists
- [ ] Falls back to `{name}-{major}.x` when exact match missing
- [ ] Returns `None` when neither exists
- [ ] Handles versions with multiple dots (e.g., `3.9.1` -> major `3`)
- [ ] Handles single-digit versions (e.g., `21` -> major `21`)
- [ ] Pure function, no side effects beyond directory existence checks

---

### G2-T2: Conditional Route Filtering Logic

| Field | Value |
|-------|-------|
| **Task ID** | G2-T2 |
| **Module** | `claude_setup/domain/core_kp_routing.py` (add function) |
| **Layer** | domain |
| **Tier** | Mid |
| **Context Budget** | M |
| **Dependencies** | G1-T2 |

**Description:**
Add a `get_active_routes()` function to the `core_kp_routing` module that filters routes based on `ProjectConfig` values. This separates the static data (G1-T2) from the filtering logic.

**Implementation details:**
- `get_active_routes(config: ProjectConfig) -> List[CoreKpRoute]` -- returns combined list of unconditional routes + conditional routes whose conditions are met
- For each `ConditionalCoreKpRoute`: get the config value for `condition_field` via `getattr()`, exclude if value matches `condition_exclude`
- Uses `claude_setup.models.ProjectConfig` (read-only)

**Acceptance Criteria:**
- [ ] Returns all 11 unconditional routes for any config
- [ ] Includes cloud-native route when `architecture.style != "library"`
- [ ] Excludes cloud-native route when `architecture.style == "library"`
- [ ] Return type is `List[CoreKpRoute]` (not mixed with conditional subclass)
- [ ] Function is <= 25 lines

---

## G3: ASSEMBLER SUPPORT -- Consolidator

These tasks create support modules used by the main assembler. Independent of G1/G2.

### G3-T1: File Consolidation Functions

| Field | Value |
|-------|-------|
| **Task ID** | G3-T1 |
| **Module** | `claude_setup/assembler/consolidator.py` |
| **Layer** | domain |
| **Tier** | Mid |
| **Context Budget** | M |
| **Dependencies** | None |

**Description:**
Create `consolidate_files()` and `consolidate_framework_rules()` functions. Port from bash `consolidate_rules()` (line 1589) and `consolidate_framework_rules()` (line 1612).

**Implementation details:**
- `consolidate_files(output_path: Path, source_paths: List[Path]) -> None`
  - Merges multiple source files into single output with generated header and `---` separators
  - Creates parent directories if needed (`output_path.parent.mkdir(parents=True, exist_ok=True)`)
  - Skips missing source files silently
- `consolidate_framework_rules(framework: str, rules_dir: Path, source_dir: Path) -> List[Path]`
  - Groups framework files into 3 consolidated outputs:
    - Core (`30-{fw}-core.md`): files matching `*-cdi*`, `*-di*`, `*-config*`, `*-web*`, `*-resteasy*`, `*-middleware*`, `*-resilience*`
    - Data (`31-{fw}-data.md`): files matching `*-panache*`, `*-jpa*`, `*-prisma*`, `*-sqlalchemy*`, `*-exposed*`, `*-ef*`, `*-orm*`, `*-database*`
    - Operations (`32-{fw}-operations.md`): files matching `*-testing*`, `*-observability*`, `*-native-build*`, `*-infrastructure*`
  - Returns list of generated file paths
  - Pattern matching uses string `in` operator (e.g., `"-cdi" in filename`)
- Zero external dependencies

**Acceptance Criteria:**
- [ ] `consolidate_files` produces output with `---` separators between sources
- [ ] `consolidate_files` creates parent directories automatically
- [ ] `consolidate_files` skips missing source files without error
- [ ] `consolidate_framework_rules` produces exactly 3 files per framework
- [ ] File grouping patterns match all bash glob patterns from plan section 2.5
- [ ] Empty groups produce no output file (or empty file -- match bash behavior)
- [ ] All functions <= 25 lines each

---

## G4: ASSEMBLER CORE -- Main RulesAssembler Class

This is the central orchestration task. Depends on all previous groups.

### G4-T1: RulesAssembler -- Layer 1 (Core Rules) + Layer 4 (Project Identity & Domain)

| Field | Value |
|-------|-------|
| **Task ID** | G4-T1 |
| **Module** | `claude_setup/assembler/rules_assembler.py` |
| **Layer** | domain engine |
| **Tier** | Senior |
| **Context Budget** | L |
| **Dependencies** | G1-T1, G1-T2, G1-T3, G2-T1, G2-T2, G3-T1 |

**Description:**
Create the `RulesAssembler` class with all assembly layer methods. This is the most complex module in the story, orchestrating 4+ layers of file generation with conditional logic.

**Implementation details:**

**Public API:**
- `assemble(config: ProjectConfig, output_dir: Path, engine: TemplateEngine) -> List[Path]`
  - Orchestrates all layers, returns list of all generated file paths
  - Never returns None or empty list for valid config
  - All returned paths must exist on disk after completion

**Layer 1 -- Core Rules:**
- `_copy_core_rules(config, src_dir, rules_dir, engine) -> List[Path]`
  - Copy `src/core-rules/*.md` to `output_dir/rules/`
  - Replace placeholders using `engine.replace_placeholders()`

**Layer 1b -- Core to KP Routing:**
- `_route_core_to_knowledge_packs(config, src_dir, skills_dir) -> List[Path]`
  - Use `get_active_routes(config)` from `core_kp_routing`
  - Copy source files to `skills/{kp_name}/references/{dest_file}`

**Layer 2 -- Language Knowledge Packs:**
- `_copy_language_knowledge_packs(config, src_dir, skills_dir) -> List[Path]`
  - Use `find_version_dir()` for version-specific directories
  - Route testing files to `testing` KP, rest to `coding-standards` KP

**Layer 3 -- Framework Knowledge Packs:**
- `_copy_framework_knowledge_packs(config, src_dir, skills_dir) -> List[Path]`
  - Use `get_stack_pack_name()` for pack directory name
  - Use `find_version_dir()` for version resolution
  - Copy to `skills/{stack_pack_name}/references/`

**Layer 4 -- Project Identity & Domain:**
- `_generate_project_identity(config, rules_dir) -> Path`
  - Generate `01-project-identity.md` using template engine or formatted string
- `_copy_domain_template(config, src_dir, rules_dir) -> Path`
  - Copy/generate `02-domain.md` with placeholders replaced

**Conditional Layers:**
- `_copy_database_references(config, src_dir, skills_dir) -> List[Path]`
- `_copy_cache_references(config, src_dir, skills_dir) -> List[Path]`
- `_assemble_security_rules(config, src_dir, skills_dir) -> List[Path]`
- `_assemble_cloud_knowledge(config, src_dir, skills_dir) -> List[Path]`
- `_assemble_infrastructure_knowledge(config, src_dir, skills_dir) -> List[Path]`

**Constraints:**
- Receives `TemplateEngine` as parameter (dependency injection, no Jinja2 import)
- Each method <= 25 lines
- Class <= 250 lines (split helpers into private methods)
- Missing optional directories log warning and return empty list
- Uses `from __future__ import annotations`
- Python 3.9 compatible (no match/case, no `X | Y` unions)

**Acceptance Criteria:**
- [ ] `assemble()` orchestrates all layers and returns complete file list
- [ ] Layer 1: core rules copied with placeholder replacement
- [ ] Layer 1b: detailed core rules routed to correct KP directories
- [ ] Layer 2: language files routed (testing -> testing KP, rest -> coding-standards)
- [ ] Layer 3: framework files routed with correct stack pack name
- [ ] Layer 4: project identity generated with config values
- [ ] Layer 4: domain template copied with placeholders
- [ ] Conditional: database refs only when `data.database.name != "none"`
- [ ] Conditional: cache refs only when `data.cache.name != "none"`
- [ ] Conditional: security rules only when `security.frameworks` non-empty
- [ ] Conditional: cloud knowledge only when cloud provider configured
- [ ] Conditional: infrastructure knowledge for kubernetes/containers/IaC
- [ ] All returned paths exist on disk
- [ ] No method exceeds 25 lines
- [ ] Class does not exceed 250 lines
- [ ] No direct Jinja2 imports in this module

---

## G5: INTEGRATION -- Package Wiring

### G5-T1: Package Init Exports

| Field | Value |
|-------|-------|
| **Task ID** | G5-T1 |
| **Module** | `claude_setup/assembler/__init__.py` |
| **Layer** | config |
| **Tier** | Junior |
| **Context Budget** | S |
| **Dependencies** | G4-T1 |

**Description:**
Update the `claude_setup/assembler/__init__.py` to export `RulesAssembler` as the public API surface for the assembler package.

**Implementation details:**
- Add `from claude_setup.assembler.rules_assembler import RulesAssembler`
- Add `__all__ = ["RulesAssembler"]`

**Acceptance Criteria:**
- [ ] `from claude_setup.assembler import RulesAssembler` works
- [ ] No circular import issues
- [ ] `__all__` explicitly declares public API

---

## G6: UNIT TESTS

All test tasks can run in parallel (max 4 concurrent). Each depends on its corresponding implementation task.

### G6-T1: Tests for Stack Pack Mapping

| Field | Value |
|-------|-------|
| **Task ID** | G6-T1 |
| **Module** | `tests/domain/test_stack_pack_mapping.py` |
| **Layer** | test |
| **Tier** | Junior |
| **Context Budget** | S |
| **Dependencies** | G1-T1 |

**Description:**
Unit tests for `get_stack_pack_name()` and `FRAMEWORK_STACK_PACK` dictionary.

**Test scenarios:**
- Parameterized test covering all 11 framework-to-pack mappings
- Unknown framework returns empty string
- Empty string framework returns empty string
- Case sensitivity (frameworks are lowercase)

**Acceptance Criteria:**
- [ ] All 11 mappings tested via parameterized test
- [ ] Edge cases tested (unknown, empty, case)
- [ ] Test naming follows `[method]_[scenario]_[expected]` convention
- [ ] 100% line and branch coverage of the module

---

### G6-T2: Tests for Version Resolver

| Field | Value |
|-------|-------|
| **Task ID** | G6-T2 |
| **Module** | `tests/domain/test_version_resolver.py` |
| **Layer** | test |
| **Tier** | Mid |
| **Context Budget** | M |
| **Dependencies** | G2-T1 |

**Description:**
Unit tests for `find_version_dir()` using `tmp_path` pytest fixture for directory creation.

**Test scenarios:**
- Exact version match found (`python-3.9/` exists)
- Major version fallback (`python-3.x/` when `python-3.9/` missing)
- No match returns `None` (neither directory exists)
- Multi-dot version (`3.9.1` -> tries `name-3.9.1` then `name-3.x`)
- Single-segment version (`21` -> tries `name-21` then `name-21.x`)

**Acceptance Criteria:**
- [ ] All 5 scenarios pass
- [ ] Uses `tmp_path` for directory creation (no real filesystem dependencies)
- [ ] 100% line and branch coverage of the module

---

### G6-T3: Tests for Core KP Routing

| Field | Value |
|-------|-------|
| **Task ID** | G6-T3 |
| **Module** | `tests/domain/test_core_kp_routing.py` |
| **Layer** | test |
| **Tier** | Mid |
| **Context Budget** | M |
| **Dependencies** | G1-T2, G2-T2 |

**Description:**
Unit tests for routing constants and `get_active_routes()` filtering.

**Test scenarios:**
- `CORE_TO_KP_MAPPING` has exactly 11 entries
- `CONDITIONAL_CORE_KP` has exactly 1 entry
- Each route has non-empty `source_file`, `kp_name`, `dest_file`
- `get_active_routes()` with library config excludes cloud-native
- `get_active_routes()` with non-library config includes cloud-native
- All routes are `CoreKpRoute` instances (frozen dataclass)

**Acceptance Criteria:**
- [ ] Static mapping correctness validated
- [ ] Conditional filtering tested for both library and non-library configs
- [ ] 100% line and branch coverage of the module

---

### G6-T4: Tests for Auditor

| Field | Value |
|-------|-------|
| **Task ID** | G6-T4 |
| **Module** | `tests/assembler/test_auditor.py` |
| **Layer** | test |
| **Tier** | Mid |
| **Context Budget** | M |
| **Dependencies** | G1-T3 |

**Description:**
Unit tests for `audit_rules_context()` and `AuditResult` dataclass.

**Test scenarios:**
- Empty directory: 0 files, 0 bytes, no warnings
- Under thresholds: correct counts, no warnings
- Exceeds file count (>10 files): warning generated
- Exceeds total size (>50KB): warning generated
- Both thresholds exceeded: 2 warnings
- `file_sizes` sorted descending by size
- Non-existent directory handling

**Acceptance Criteria:**
- [ ] All 7 scenarios pass
- [ ] Uses `tmp_path` for test directory setup
- [ ] 100% line and branch coverage of the module

---

### G6-T5: Tests for Consolidator

| Field | Value |
|-------|-------|
| **Task ID** | G6-T5 |
| **Module** | `tests/assembler/test_consolidator.py` |
| **Layer** | test |
| **Tier** | Mid |
| **Context Budget** | M |
| **Dependencies** | G3-T1 |

**Description:**
Unit tests for `consolidate_files()` and `consolidate_framework_rules()`.

**Test scenarios for `consolidate_files`:**
- Merges 2 files with `---` separator
- Creates parent directories for output
- Skips missing source files
- Single file produces output without separator
- Empty source list produces no output (or empty file)

**Test scenarios for `consolidate_framework_rules`:**
- Framework with files in all 3 groups produces 3 output files
- CDI file routed to core group
- JPA file routed to data group
- Testing file routed to operations group
- File matching no group is excluded (or placed in default group -- match bash)
- Returns correct list of generated paths

**Acceptance Criteria:**
- [ ] All scenarios pass
- [ ] Parameterized test for framework file pattern matching
- [ ] Uses `tmp_path` for all file operations
- [ ] 100% line and branch coverage of the module

---

### G6-T6: Tests for RulesAssembler

| Field | Value |
|-------|-------|
| **Task ID** | G6-T6 |
| **Module** | `tests/assembler/test_rules_assembler.py` |
| **Layer** | test |
| **Tier** | Senior |
| **Context Budget** | L |
| **Dependencies** | G4-T1 |

**Description:**
Comprehensive unit tests for the `RulesAssembler` class covering all assembly layers and conditional logic. Uses `tmp_path` for output and minimal fixture directories for source files.

**Test scenarios:**

**Layer 1 -- Core Rules:**
- `test_copy_core_rules_replaces_placeholders` -- verifies placeholder substitution
- `test_copy_core_rules_copies_all_files` -- all core-rules files present in output

**Layer 1b -- Core to KP Routing:**
- `test_route_core_to_knowledge_packs` -- files appear in correct KP directories

**Layer 2 -- Language KPs:**
- `test_copy_language_kps_routes_testing_files` -- testing files go to testing KP
- `test_copy_language_kps_routes_standards_files` -- non-testing files go to coding-standards KP

**Layer 3 -- Framework KPs:**
- `test_copy_framework_kps_uses_stack_pack_name` -- correct directory name used

**Layer 4 -- Project Identity & Domain:**
- `test_generate_project_identity_contains_config_values` -- name, purpose, framework in output
- `test_copy_domain_template` -- domain template copied with replacements

**Conditional Logic:**
- `test_copy_database_references_for_postgresql` -- DB refs copied when DB configured
- `test_copy_database_references_skipped_for_none` -- skipped when no DB
- `test_copy_cache_references_for_redis` -- cache refs copied when cache configured
- `test_assemble_security_rules_when_configured` -- security files generated
- `test_assemble_no_security_skips_security_files` -- no security files when not configured
- `test_assemble_cloud_knowledge_skipped_for_none` -- no cloud files when no provider
- `test_assemble_infrastructure_for_kubernetes` -- infra files for k8s

**Full Pipeline:**
- `test_assemble_full_config_generates_all_core_rules` -- end-to-end with all sections
- `test_assemble_returns_existing_paths` -- all returned paths exist on disk

**Acceptance Criteria:**
- [ ] All scenarios pass
- [ ] Minimal fixture directories created in `tmp_path` (not dependent on real `src/` tree)
- [ ] `TemplateEngine` used via dependency injection (real instance with test fixtures, not mocked)
- [ ] No mocking of domain logic
- [ ] >= 95% line coverage, >= 90% branch coverage of `rules_assembler.py`

---

## G7: CONTRACT / INTEGRATION TESTS

### G7-T1: Test Package Init

| Field | Value |
|-------|-------|
| **Task ID** | G7-T1 |
| **Module** | `tests/assembler/__init__.py` |
| **Layer** | test |
| **Tier** | Junior |
| **Context Budget** | S |
| **Dependencies** | None |

**Description:**
Create the `tests/assembler/__init__.py` package init file to enable test discovery.

**Acceptance Criteria:**
- [ ] File exists (can be empty)
- [ ] `pytest tests/assembler/` discovers tests correctly

---

### G7-T2: Contract Tests -- Bash Output Comparison

| Field | Value |
|-------|-------|
| **Task ID** | G7-T2 |
| **Module** | `tests/test_rules_assembly_contract.py` |
| **Layer** | test |
| **Tier** | Senior |
| **Context Budget** | L |
| **Dependencies** | G4-T1, G6-T6 |

**Description:**
Contract tests comparing Python assembler output against pre-captured bash output for a reference configuration. Validates RULE-005 (byte-by-byte compatibility).

**Implementation details:**
- Use a reference `ProjectConfig` (e.g., java-quarkus with all sections enabled)
- Run `RulesAssembler.assemble()` against real `src/` tree
- Compare each generated file against pre-captured bash reference output
- Normalize line endings before comparison (cross-platform)
- Focus on content equivalence, not strict byte identity for whitespace

**Test scenarios:**
- `test_rules_output_matches_bash_reference` -- main contract test
- `test_rules_file_count_matches_bash` -- same number of output files
- `test_rules_directory_structure_matches_bash` -- same directory tree

**Acceptance Criteria:**
- [ ] Reference bash output captured and stored in `tests/fixtures/`
- [ ] Python output matches bash output for all generated files
- [ ] Line ending normalization applied
- [ ] Test uses real `src/` directory (not minimal fixtures)
- [ ] Clear diff output on failure showing exactly which file/line differs

---

## Dependency Graph

```
G1-T1 (stack_pack_mapping) ──────────────────────────────────────┐
G1-T2 (core_kp_routing dataclasses) ── G2-T2 (route filtering) ─┤
G1-T3 (auditor) ─────────────────────────────────────────────────┤
G2-T1 (version_resolver) ───────────────────────────────────────┤
G3-T1 (consolidator) ───────────────────────────────────────────┤
                                                                 ▼
                                                          G4-T1 (RulesAssembler)
                                                                 │
                                                                 ▼
                                                          G5-T1 (package init)
                                                                 │
                                                                 ▼
G6-T1 (test stack_pack) ◄── G1-T1                        G7-T1 (test __init__)
G6-T2 (test version_resolver) ◄── G2-T1
G6-T3 (test core_kp_routing) ◄── G1-T2, G2-T2
G6-T4 (test auditor) ◄── G1-T3
G6-T5 (test consolidator) ◄── G3-T1
G6-T6 (test rules_assembler) ◄── G4-T1
                                                                 │
                                                                 ▼
                                                          G7-T2 (contract tests)
```

## Parallelism Summary

| Phase | Tasks | Max Parallel | Estimated Effort |
|-------|-------|-------------|-----------------|
| Phase 1 | G1-T1, G1-T2, G1-T3, G2-T1, G3-T1 | 5 | Small (all S/M budget) |
| Phase 2 | G2-T2 | 1 | Small (depends on G1-T2) |
| Phase 3 | G4-T1 | 1 | Large (Senior, L budget) |
| Phase 4 | G5-T1, G7-T1 | 2 | Trivial |
| Phase 5 | G6-T1, G6-T2, G6-T3, G6-T4, G6-T5, G6-T6 | 4 (throttled) | Medium |
| Phase 6 | G7-T2 | 1 | Large (contract tests) |

## Review Assignments

| Reviewer | Tasks | Tier |
|----------|-------|------|
| Tech Lead | G4-T1, G6-T6, G7-T2 | Senior |
| QA | G6-T1 through G6-T6, G7-T2 | Senior (due to contract tests) |
| Security | N/A (no security-sensitive logic in this story) | -- |
