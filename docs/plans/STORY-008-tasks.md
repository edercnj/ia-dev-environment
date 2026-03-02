# STORY-008 Task Breakdown: Hooks, Settings, and README Assemblers

**Story:** STORY-008 -- Hooks, Settings, and README Assemblers
**Phase:** 3 (Assembly)
**Blocked By:** STORY-001 (models), STORY-004 (template engine)
**Blocks:** STORY-009 (CLI orchestration)

---

## Parallelism Groups

```
G1 (Domain Mappings) ──→ G2 (HooksAssembler) ──→ G3 (SettingsAssembler) ──→ G4 (ReadmeAssembler) ──→ G7 (Wire-up)
                     ──→ G2 (HooksAssembler)                                                      ──→ G7 (Wire-up)
                                                                                                   ──→ G5 (Unit Tests)
                                                                                                   ──→ G6 (Integration/Contract Tests)
```

G1 has no internal dependencies.
G2 depends on G1 (hook_template_key mapping).
G3 depends on G1 (settings_lang_key mapping) and G2 (hooks section references hooks output).
G4 depends on G1 (no direct code dep, but uses output_dir content from G2/G3).
G5 depends on G1-G4 (tests all assemblers).
G6 depends on G1-G4 (end-to-end assembly).
G7 depends on G2-G4 (module exports).

---

## G1 -- Domain Layer (Mappings)

### G1-T1: Add `HOOK_TEMPLATE_MAP` to domain

**File:** `claude_setup/domain/stack_mapping.py`
**Changes:**
- Add constant `HOOK_TEMPLATE_MAP: Dict[Tuple[str, str], str]` mapping `(language, build_tool)` to hook template directory key
- Mapping entries:

| (language, build_tool) | hook_template_key |
|------------------------|-------------------|
| `("java", "maven")` | `"java-maven"` |
| `("java", "gradle")` | `"java-gradle"` |
| `("kotlin", "gradle")` | `"kotlin"` |
| `("typescript", "npm")` | `"typescript"` |
| `("python", "pip")` | `""` (empty -- no hook template) |
| `("go", "go")` | `"go"` |
| `("rust", "cargo")` | `"rust"` |
| `("csharp", "dotnet")` | `"csharp"` |

- Add helper function `get_hook_template_key(language: str, build_tool: str) -> str` that returns the key from the map, defaulting to `""` if not found.

**Dependencies within group:** None
**Estimated lines:** ~25
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from claude_setup.domain.stack_mapping import get_hook_template_key; assert get_hook_template_key('java', 'maven') == 'java-maven'; assert get_hook_template_key('python', 'pip') == ''; print('OK')"`

---

### G1-T2: Add `SETTINGS_LANG_MAP` to domain

**File:** `claude_setup/domain/stack_mapping.py`
**Changes:**
- Add constant `SETTINGS_LANG_MAP: Dict[Tuple[str, str], str]` mapping `(language, build_tool)` to settings template filename key (without `.json` extension)
- Mapping entries:

| (language, build_tool) | settings_lang_key |
|------------------------|-------------------|
| `("java", "maven")` | `"java-maven"` |
| `("java", "gradle")` | `"java-gradle"` |
| `("kotlin", "gradle")` | `"kotlin"` |
| `("typescript", "npm")` | `"typescript-npm"` |
| `("python", "pip")` | `"python-pip"` |
| `("go", "go")` | `"go"` |
| `("rust", "cargo")` | `"rust-cargo"` |
| `("csharp", "dotnet")` | `"csharp-dotnet"` |

- Add helper function `get_settings_lang_key(language: str, build_tool: str) -> str` that returns the key from the map, defaulting to `""` if not found.

**Dependencies within group:** None (parallel with G1-T1)
**Estimated lines:** ~25
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from claude_setup.domain.stack_mapping import get_settings_lang_key; assert get_settings_lang_key('python', 'pip') == 'python-pip'; assert get_settings_lang_key('java', 'maven') == 'java-maven'; print('OK')"`

---

### G1-T3: Add `DATABASE_SETTINGS_MAP` to domain

**File:** `claude_setup/domain/stack_mapping.py`
**Changes:**
- Add constant `DATABASE_SETTINGS_MAP: Dict[str, str]` mapping database name to settings template filename key (without `.json` extension)
- Mapping entries:

| database name | settings_db_key |
|---------------|-----------------|
| `"postgresql"` | `"database-psql"` |
| `"mysql"` | `"database-mysql"` |
| `"oracle"` | `"database-oracle"` |
| `"mongodb"` | `"database-mongodb"` |
| `"cassandra"` | `"database-cassandra"` |

- Add constant `CACHE_SETTINGS_MAP: Dict[str, str]` mapping cache name to settings template filename key:

| cache name | settings_cache_key |
|------------|-------------------|
| `"redis"` | `"cache-redis"` |
| `"dragonfly"` | `"cache-dragonfly"` |
| `"memcached"` | `"cache-memcached"` |

- Add helper functions:
  - `get_database_settings_key(db_name: str) -> str` -- returns key or `""` if not found
  - `get_cache_settings_key(cache_name: str) -> str` -- returns key or `""` if not found

**Dependencies within group:** None (parallel with G1-T1, G1-T2)
**Estimated lines:** ~30
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from claude_setup.domain.stack_mapping import get_database_settings_key, get_cache_settings_key; assert get_database_settings_key('postgresql') == 'database-psql'; assert get_cache_settings_key('redis') == 'cache-redis'; print('OK')"`

---

## G2 -- HooksAssembler

### G2-T1: Create `HooksAssembler` class

**File:** `claude_setup/assembler/hooks_assembler.py` (new file)
**Class:** `HooksAssembler`
**Constructor:** `__init__(self, src_dir: Path)` -- stores reference to `src/` root directory
**Methods:**
- `assemble(self, config: ProjectConfig, output_dir: Path, engine: TemplateEngine) -> List[Path]`
  - Resolves `hook_template_key` via `get_hook_template_key(config.language.name, config.framework.build_tool)`
  - If key is empty string, returns `[]` (no hooks for this language)
  - Locates hook script at `self._src_dir / "hooks-templates" / {key} / "post-compile-check.sh"`
  - If script file does not exist, returns `[]`
  - Creates `output_dir / "hooks"` directory
  - Copies script to `output_dir / "hooks" / "post-compile-check.sh"`
  - Returns `[dest_path]`

**Dependencies:**
- `claude_setup.domain.stack_mapping.get_hook_template_key` (G1-T1)
- `claude_setup.models.ProjectConfig`

**Design notes:**
- Follows same pattern as `PatternsAssembler` (constructor takes `src_dir`)
- Uses `shutil.copy2` for file copy
- Each method <= 25 lines

**Estimated lines:** ~45
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from claude_setup.assembler.hooks_assembler import HooksAssembler; print('OK')"`

---

### G2-T2: Implement `_build_hooks_json` method

**File:** `claude_setup/assembler/hooks_assembler.py`
**Method:**
- `_build_hooks_json(self, hook_script_path: Path) -> dict`
  - Constructs the hooks.json structure referencing the hook script path
  - Structure follows Claude Code hooks format:
    ```json
    {
      "hooks": {
        "PostToolUse": [
          {
            "matcher": "Write|Edit",
            "hook": "/path/to/post-compile-check.sh"
          }
        ]
      }
    }
    ```
  - Returns the dict (caller writes to file)

- Update `assemble()` to also generate `output_dir / ".claude" / "hooks.json"` using `_build_hooks_json()` and `json.dumps()`

**Dependencies within group:** G2-T1
**Estimated lines:** ~25
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from claude_setup.assembler.hooks_assembler import HooksAssembler; print('OK')"`

---

## G3 -- SettingsAssembler

### G3-T1: Create `SettingsAssembler` class skeleton with `merge_json_arrays`

**File:** `claude_setup/assembler/settings_assembler.py` (new file)
**Class:** `SettingsAssembler`
**Constructor:** `__init__(self, src_dir: Path)` -- stores reference to `src/` root directory

**Static/class methods:**
- `merge_json_arrays(base: list, overlay: list) -> list`
  - Merges two JSON arrays (lists of strings representing permission entries)
  - Deduplicates: overlay items not already in base are appended
  - Preserves order: base items first, then new overlay items
  - Returns merged list

**Dependencies:**
- `claude_setup.models.ProjectConfig`

**Estimated lines:** ~30
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from claude_setup.assembler.settings_assembler import SettingsAssembler; r = SettingsAssembler.merge_json_arrays(['a','b'], ['b','c']); assert r == ['a','b','c']; print('OK')"`

---

### G3-T2: Implement `_collect_permissions` method

**File:** `claude_setup/assembler/settings_assembler.py`
**Method:**
- `_collect_permissions(self, config: ProjectConfig) -> list`
  - Reads and merges permission arrays from settings template JSON files in order:
    1. `base.json` (always)
    2. `{lang_key}.json` via `get_settings_lang_key()` (if key is non-empty)
    3. `docker.json` (if `config.infrastructure.container != "none"`)
    4. `kubernetes.json` (if `config.infrastructure.orchestrator == "kubernetes"`)
    5. `docker-compose.json` (if `config.infrastructure.container != "none"`)
    6. `database-{db_key}.json` via `get_database_settings_key()` (if key is non-empty)
    7. `cache-{cache_key}.json` via `get_cache_settings_key()` (if key is non-empty)
    8. `testing-newman.json` (if `config.testing.contract_tests`)
  - Each file is a JSON array of strings
  - Uses `merge_json_arrays()` to accumulate
  - Source directory: `self._src_dir / "settings-templates"`

**Dependencies within group:** G3-T1
**Dependencies on G1:** G1-T2 (settings_lang_key), G1-T3 (database/cache keys)
**Estimated lines:** ~40
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from claude_setup.assembler.settings_assembler import SettingsAssembler; print('OK')"`

---

### G3-T3: Implement `_build_settings_dict` and `assemble` methods

**File:** `claude_setup/assembler/settings_assembler.py`
**Methods:**
- `_build_settings_dict(self, config: ProjectConfig, permissions: list, has_hooks: bool) -> dict`
  - Builds the settings.json dict structure:
    ```json
    {
      "permissions": {
        "allow": [ ...permissions... ]
      }
    }
    ```
  - If `has_hooks` is True, adds hooks section referencing the hook script path
  - Returns the dict

- `_generate_settings_local(self, output_dir: Path) -> Path`
  - Generates `settings.local.json` template file with empty/placeholder structure
  - Returns path to generated file

- `assemble(self, config: ProjectConfig, output_dir: Path, engine: TemplateEngine) -> List[Path]`
  - Calls `_collect_permissions(config)` to gather all permissions
  - Determines `has_hooks` by checking if hook template key is non-empty via `get_hook_template_key()`
  - Calls `_build_settings_dict()` to construct the JSON structure
  - Writes `output_dir / "settings.json"` using `json.dumps(indent=2)`
  - Calls `_generate_settings_local()` for the local template
  - Returns list of generated paths

**Dependencies within group:** G3-T1, G3-T2
**Dependencies on G1:** G1-T1 (hook_template_key for has_hooks check)
**Estimated lines:** ~50
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from claude_setup.assembler.settings_assembler import SettingsAssembler; print('OK')"`

---

## G4 -- ReadmeAssembler

### G4-T1: Create `ReadmeAssembler` class skeleton

**File:** `claude_setup/assembler/readme_assembler.py` (new file)
**Class:** `ReadmeAssembler`
**Constructor:** `__init__(self, src_dir: Path)` -- stores reference to `src/` root directory

**Methods:**
- `_count_artifacts(self, output_dir: Path) -> dict`
  - Counts rules (`.md` files in `output_dir/rules/`)
  - Counts skills (directories in `output_dir/skills/` that contain `SKILL.md`)
  - Counts agents (`.md` files in `output_dir/agents/`)
  - Returns `{"rules": int, "skills": int, "agents": int}`

**Dependencies:** None (standalone utility)
**Estimated lines:** ~30
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from claude_setup.assembler.readme_assembler import ReadmeAssembler; print('OK')"`

---

### G4-T2: Implement `generate_readme` method

**File:** `claude_setup/assembler/readme_assembler.py`
**Method:**
- `generate_readme(self, config: ProjectConfig, engine: TemplateEngine, output_dir: Path) -> str`
  - Builds full README content from config and output_dir analysis
  - Sections generated:
    1. Title and project description
    2. Technology stack table (language, framework, build_tool, database, cache, container, orchestrator)
    3. Rules summary table (lists rules found in `output_dir/rules/`)
    4. Skills summary table (lists skills found in `output_dir/skills/`)
    5. Agents summary table (lists agents found in `output_dir/agents/`)
    6. Setup instructions section
    7. Quality gates section (coverage thresholds from config.testing)
  - Uses `_count_artifacts()` for counts
  - Returns the full README string

**Dependencies within group:** G4-T1
**Estimated lines:** ~80
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from claude_setup.assembler.readme_assembler import ReadmeAssembler; print('OK')"`

---

### G4-T3: Implement `generate_minimal_readme` and `assemble` methods

**File:** `claude_setup/assembler/readme_assembler.py`
**Methods:**
- `generate_minimal_readme(self, config: ProjectConfig) -> str`
  - Generates minimal README with only:
    1. Project name as title
    2. Project purpose as description
    3. Language and framework info
  - Returns the minimal README string

- `assemble(self, config: ProjectConfig, output_dir: Path, engine: TemplateEngine) -> List[Path]`
  - Calls `generate_readme(config, engine, output_dir)` to build full README
  - If `generate_readme` fails or returns empty, falls back to `generate_minimal_readme(config)`
  - Writes to `output_dir / "README.md"`
  - Returns `[dest_path]`

**Dependencies within group:** G4-T1, G4-T2
**Estimated lines:** ~40
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from claude_setup.assembler.readme_assembler import ReadmeAssembler; print('OK')"`

---

## G5 -- Unit Tests

### G5-T1: Unit tests for domain mappings (G1)

**File:** `tests/domain/test_stack_mapping_hooks_settings.py` (new file)
**Test cases:**

| Test Function | Scenario | Expected |
|---------------|----------|----------|
| `test_get_hook_template_key_java_maven` | `("java", "maven")` | `"java-maven"` |
| `test_get_hook_template_key_java_gradle` | `("java", "gradle")` | `"java-gradle"` |
| `test_get_hook_template_key_kotlin` | `("kotlin", "gradle")` | `"kotlin"` |
| `test_get_hook_template_key_typescript` | `("typescript", "npm")` | `"typescript"` |
| `test_get_hook_template_key_python_returns_empty` | `("python", "pip")` | `""` |
| `test_get_hook_template_key_go` | `("go", "go")` | `"go"` |
| `test_get_hook_template_key_rust` | `("rust", "cargo")` | `"rust"` |
| `test_get_hook_template_key_csharp` | `("csharp", "dotnet")` | `"csharp"` |
| `test_get_hook_template_key_unknown_returns_empty` | `("unknown", "unknown")` | `""` |
| `test_get_settings_lang_key_all_entries` | All 8 entries | Correct key for each |
| `test_get_settings_lang_key_unknown_returns_empty` | `("unknown", "x")` | `""` |
| `test_get_database_settings_key_postgresql` | `"postgresql"` | `"database-psql"` |
| `test_get_database_settings_key_all_entries` | All 5 databases | Correct key for each |
| `test_get_database_settings_key_unknown` | `"sqlite"` | `""` |
| `test_get_cache_settings_key_all_entries` | All 3 caches | Correct key for each |
| `test_get_cache_settings_key_unknown` | `"hazelcast"` | `""` |
| `test_get_cache_settings_key_none` | `"none"` | `""` |

**Dependencies:** G1-T1, G1-T2, G1-T3
**Estimated lines:** ~100
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -m pytest tests/domain/test_stack_mapping_hooks_settings.py -x -q`

---

### G5-T2: Unit tests for HooksAssembler

**File:** `tests/assembler/test_hooks_assembler.py` (new file)
**Test cases:**

| Test Function | Scenario | Expected |
|---------------|----------|----------|
| `test_assemble_java_maven_copies_hook_script` | java/maven config | `post-compile-check.sh` copied to `output_dir/hooks/` |
| `test_assemble_java_maven_returns_paths` | java/maven config | Returns list with generated paths |
| `test_assemble_python_returns_empty` | python/pip config | Returns `[]` (no hook template for python) |
| `test_assemble_typescript_copies_hook_script` | typescript/npm config | Hook script copied |
| `test_assemble_go_copies_hook_script` | go/go config | Hook script copied |
| `test_assemble_creates_hooks_directory` | java/maven config | `output_dir/hooks/` dir exists |
| `test_assemble_missing_src_dir_returns_empty` | nonexistent src_dir | Returns `[]` |
| `test_build_hooks_json_structure` | Valid hook script path | Dict with correct hooks JSON structure |
| `test_build_hooks_json_post_tool_use_matcher` | Valid hook script path | Matcher is `"Write\|Edit"` |
| `test_assemble_generates_hooks_json` | java/maven config | `hooks.json` written as valid JSON |

**Dependencies:** G2-T1, G2-T2
**Estimated lines:** ~120
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -m pytest tests/assembler/test_hooks_assembler.py -x -q`

---

### G5-T3: Unit tests for SettingsAssembler

**File:** `tests/assembler/test_settings_assembler.py` (new file)
**Test cases:**

| Test Function | Scenario | Expected |
|---------------|----------|----------|
| `test_merge_json_arrays_no_overlap` | `["a","b"]` + `["c","d"]` | `["a","b","c","d"]` |
| `test_merge_json_arrays_with_overlap` | `["a","b"]` + `["b","c"]` | `["a","b","c"]` |
| `test_merge_json_arrays_empty_base` | `[]` + `["a","b"]` | `["a","b"]` |
| `test_merge_json_arrays_empty_overlay` | `["a","b"]` + `[]` | `["a","b"]` |
| `test_merge_json_arrays_both_empty` | `[]` + `[]` | `[]` |
| `test_merge_json_arrays_identical` | `["a","b"]` + `["a","b"]` | `["a","b"]` |
| `test_collect_permissions_base_always_included` | Minimal config | Contains all base.json entries |
| `test_collect_permissions_includes_lang_key` | python/pip config | Contains python-pip.json entries |
| `test_collect_permissions_includes_docker` | container=docker | Contains docker.json entries |
| `test_collect_permissions_includes_kubernetes` | orchestrator=kubernetes | Contains kubernetes.json entries |
| `test_collect_permissions_includes_database` | database=postgresql | Contains database-psql.json entries |
| `test_collect_permissions_includes_cache` | cache=redis | Contains cache-redis.json entries |
| `test_collect_permissions_includes_newman` | contract_tests=True | Contains testing-newman.json entries |
| `test_collect_permissions_no_duplicates` | Full config | No duplicate entries in result |
| `test_collect_permissions_minimal_config` | Minimal (python, no db, no k8s) | Only base + python-pip entries |
| `test_build_settings_dict_has_permissions` | Any config | Dict has `permissions.allow` key |
| `test_build_settings_dict_with_hooks` | has_hooks=True | Dict contains hooks section |
| `test_build_settings_dict_without_hooks` | has_hooks=False | Dict does not contain hooks section |
| `test_assemble_writes_valid_json` | Full config | settings.json parseable as JSON |
| `test_assemble_writes_settings_local` | Any config | settings.local.json created |
| `test_assemble_returns_paths` | Any config | Returns list with both paths |

**Dependencies:** G3-T1, G3-T2, G3-T3
**Estimated lines:** ~200
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -m pytest tests/assembler/test_settings_assembler.py -x -q`

---

### G5-T4: Unit tests for ReadmeAssembler

**File:** `tests/assembler/test_readme_assembler.py` (new file)
**Test cases:**

| Test Function | Scenario | Expected |
|---------------|----------|----------|
| `test_count_artifacts_empty_dir` | Empty output_dir | `{"rules": 0, "skills": 0, "agents": 0}` |
| `test_count_artifacts_with_rules` | output_dir with 3 rule .md files | `rules == 3` |
| `test_count_artifacts_with_skills` | output_dir with 2 skill dirs each having SKILL.md | `skills == 2` |
| `test_count_artifacts_with_agents` | output_dir with 4 agent .md files | `agents == 4` |
| `test_generate_readme_contains_project_name` | Config with name "my-service" | `"# my-service"` in output |
| `test_generate_readme_contains_tech_stack_table` | Full config | `"\| Language \|"` in output |
| `test_generate_readme_contains_rules_section` | output_dir with rules | Rules section with count |
| `test_generate_readme_contains_skills_section` | output_dir with skills | Skills section with count |
| `test_generate_readme_contains_agents_section` | output_dir with agents | Agents section with count |
| `test_generate_readme_contains_quality_gates` | Config with coverage thresholds | Coverage values in output |
| `test_generate_minimal_readme_contains_name` | Config with name "my-tool" | `"# my-tool"` in output |
| `test_generate_minimal_readme_contains_purpose` | Config with purpose "Testing" | `"Testing"` in output |
| `test_generate_minimal_readme_minimal_sections` | Any config | No rules/skills/agents tables |
| `test_assemble_writes_readme` | Full config + populated output_dir | `README.md` created |
| `test_assemble_returns_path` | Any config | Returns list with README.md path |

**Dependencies:** G4-T1, G4-T2, G4-T3
**Estimated lines:** ~180
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -m pytest tests/assembler/test_readme_assembler.py -x -q`

---

## G6 -- Integration/Contract Tests

### G6-T1: Integration tests for end-to-end assembly

**File:** `tests/assembler/test_hooks_settings_readme_integration.py` (new file)
**Test cases:**

| Test Function | Scenario | Expected |
|---------------|----------|----------|
| `test_hooks_assemble_java_maven_full_flow` | java/maven config with real src dir | Hook script + hooks.json generated, script content matches source |
| `test_hooks_assemble_python_no_output` | python/pip config with real src dir | No hooks/ dir, no hooks.json |
| `test_settings_assemble_full_config_valid_json` | Full config (java, postgresql, redis, k8s) | settings.json valid JSON with all expected permission categories |
| `test_settings_assemble_minimal_config` | Minimal python/pip config | settings.json with only base + python-pip permissions |
| `test_settings_permissions_no_duplicates_full` | Full config end-to-end | Zero duplicate entries in `permissions.allow` |
| `test_readme_assemble_after_rules_skills_agents` | Pre-populate output_dir with rules/skills/agents then run readme | README counts match actual artifact counts |
| `test_readme_assemble_empty_output_dir` | Empty output_dir | README generated with zero counts (fallback graceful) |
| `test_all_three_assemblers_sequential` | Run hooks -> settings -> readme in order | All files generated, no conflicts |

**Dependencies:** G2, G3, G4 (all assemblers)
**Estimated lines:** ~150
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -m pytest tests/assembler/test_hooks_settings_readme_integration.py -x -q`

---

### G6-T2: Contract tests for byte-level comparison

**File:** `tests/assembler/test_hooks_settings_contract.py` (new file)
**Test cases:**

| Test Function | Scenario | Expected |
|---------------|----------|----------|
| `test_hook_script_byte_identical_java_maven` | Copy java-maven hook via assembler | Output matches `src/hooks-templates/java-maven/post-compile-check.sh` byte-for-byte |
| `test_hook_script_byte_identical_go` | Copy go hook via assembler | Output matches source byte-for-byte |
| `test_settings_base_permissions_present` | Generate settings for any config | All entries from base.json present in output |
| `test_settings_lang_permissions_present` | Generate settings for python/pip | All entries from python-pip.json present in output |
| `test_merge_json_arrays_idempotent` | Merge same overlay twice | Result unchanged after second merge |

**Dependencies:** G2, G3 (hooks + settings assemblers)
**Estimated lines:** ~80
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -m pytest tests/assembler/test_hooks_settings_contract.py -x -q`

---

## G7 -- Wire-up (Module Exports)

### G7-T1: Update `assembler/__init__.py` exports

**File:** `claude_setup/assembler/__init__.py`
**Changes:**
- Add import: `from claude_setup.assembler.hooks_assembler import HooksAssembler`
- Add import: `from claude_setup.assembler.settings_assembler import SettingsAssembler`
- Add import: `from claude_setup.assembler.readme_assembler import ReadmeAssembler`
- Add all three to `__all__` list

**Dependencies:** G2-T1, G3-T1, G4-T1 (all assembler classes must exist)
**Estimated lines changed:** ~6
**Check command:** `cd /Users/edercnj/workspaces/claude-environment && python -c "from claude_setup.assembler import HooksAssembler, SettingsAssembler, ReadmeAssembler; print('OK')"`

---

## Summary

| Group | Tasks | New Files | Modified Files | Total Est. Lines |
|-------|-------|-----------|----------------|-----------------|
| G1 | 3 | 0 | `claude_setup/domain/stack_mapping.py` | ~80 |
| G2 | 2 | `claude_setup/assembler/hooks_assembler.py` | 0 | ~70 |
| G3 | 3 | `claude_setup/assembler/settings_assembler.py` | 0 | ~120 |
| G4 | 3 | `claude_setup/assembler/readme_assembler.py` | 0 | ~150 |
| G5 | 4 | `tests/domain/test_stack_mapping_hooks_settings.py`, `tests/assembler/test_hooks_assembler.py`, `tests/assembler/test_settings_assembler.py`, `tests/assembler/test_readme_assembler.py` | 0 | ~600 |
| G6 | 2 | `tests/assembler/test_hooks_settings_readme_integration.py`, `tests/assembler/test_hooks_settings_contract.py` | 0 | ~230 |
| G7 | 1 | 0 | `claude_setup/assembler/__init__.py` | ~6 |
| **Total** | **18** | **7** | **2** | **~1256** |

## File Inventory

### New Files (7)

| File | Group | Purpose |
|------|-------|---------|
| `claude_setup/assembler/hooks_assembler.py` | G2 | HooksAssembler class |
| `claude_setup/assembler/settings_assembler.py` | G3 | SettingsAssembler class |
| `claude_setup/assembler/readme_assembler.py` | G4 | ReadmeAssembler class |
| `tests/domain/test_stack_mapping_hooks_settings.py` | G5 | Domain mapping unit tests |
| `tests/assembler/test_hooks_assembler.py` | G5 | Hooks assembler unit tests |
| `tests/assembler/test_settings_assembler.py` | G5 | Settings assembler unit tests |
| `tests/assembler/test_readme_assembler.py` | G5 | Readme assembler unit tests |
| `tests/assembler/test_hooks_settings_readme_integration.py` | G6 | Integration tests |
| `tests/assembler/test_hooks_settings_contract.py` | G6 | Contract/byte-level tests |

### Modified Files (2)

| File | Group | Changes |
|------|-------|---------|
| `claude_setup/domain/stack_mapping.py` | G1 | Add HOOK_TEMPLATE_MAP, SETTINGS_LANG_MAP, DATABASE_SETTINGS_MAP, CACHE_SETTINGS_MAP + helpers |
| `claude_setup/assembler/__init__.py` | G7 | Add imports and exports for 3 new assemblers |

## Full Validation Command

```bash
cd /Users/edercnj/workspaces/claude-environment && python -m pytest tests/ -x -q --tb=short && python -m pytest tests/ --cov=claude_setup --cov-report=term-missing --cov-fail-under=95
```
