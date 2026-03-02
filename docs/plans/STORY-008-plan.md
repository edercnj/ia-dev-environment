# Implementation Plan: STORY-008 -- Hooks, Settings, and README Assemblers

**Story:** STORY-008 -- Assemblers de Hooks, Settings e README
**Phase:** 3 (Assembly)
**Blocked By:** STORY-001 (models), STORY-004 (template engine)
**Blocks:** STORY-009 (CLI orchestration)
**Architecture:** library (hexagonal-lite for a CLI tool)
**Stack:** Python 3.9 + Click 8.1

---

## 1. Affected Layers and Components

| Layer | Affected? | Rationale |
|-------|-----------|-----------|
| domain/model | YES | New `ResolvedStack` fields may need `hook_template_key` and `settings_lang_key`. Alternatively, new domain mapping constants in `stack_mapping.py`. |
| domain/engine | NO | No new business logic engines required. |
| domain/port | NO | No ports needed -- assemblers are pure domain operations on filesystem + config. |
| application | NO | No use case orchestration; assemblers invoked directly from pipeline. |
| adapter/inbound | NO | CLI entrypoint unchanged; assemblers called by the pipeline (STORY-009). |
| adapter/outbound | NO | File operations use stdlib `pathlib`/`shutil`/`json` -- no adapter needed. |
| assembler | YES | New `hooks_assembler.py`, `settings_assembler.py`, and `readme_assembler.py` modules under `claude_setup/assembler/`. |
| config | NO | No new config parsing needed. |
| tests | YES | Unit, contract, and integration tests for all three assemblers. |

---

## 2. New Classes/Interfaces to Create

### 2.1 Domain Mappings (`claude_setup/domain/stack_mapping.py` -- extend existing)

Two new constant dictionaries to add to the existing `stack_mapping.py` module:

| Constant | Type | Description |
|----------|------|-------------|
| `HOOK_TEMPLATE_KEY_MAP` | `Dict[Tuple[str, str], str]` | Maps `(language, build_tool)` to hook template directory name. Empty string means no hook (interpreted languages). |
| `SETTINGS_LANG_KEY_MAP` | `Dict[Tuple[str, str], str]` | Maps `(language, build_tool)` to settings template JSON file basename. |

**HOOK_TEMPLATE_KEY_MAP values (from bash reference):**

| (language, build_tool) | hook_template_key |
|------------------------|-------------------|
| (java, maven) | `"java-maven"` |
| (java, gradle) | `"java-gradle"` |
| (kotlin, gradle) | `"kotlin"` |
| (typescript, npm) | `"typescript"` |
| (python, pip) | `""` (no hook) |
| (go, go) | `"go"` |
| (rust, cargo) | `"rust"` |
| (csharp, dotnet) | `"csharp"` |

**SETTINGS_LANG_KEY_MAP values (from bash reference):**

| (language, build_tool) | settings_lang_key |
|------------------------|-------------------|
| (java, maven) | `"java-maven"` |
| (java, gradle) | `"java-gradle"` |
| (kotlin, gradle) | `"java-gradle"` |
| (typescript, npm) | `"typescript-npm"` |
| (python, pip) | `"python-pip"` |
| (go, go) | `"go"` |
| (rust, cargo) | `"rust-cargo"` |
| (csharp, dotnet) | `"csharp-dotnet"` |

Two pure functions:

| Function | Signature | Description |
|----------|-----------|-------------|
| `get_hook_template_key(language, build_tool)` | `(str, str) -> str` | Looks up `HOOK_TEMPLATE_KEY_MAP`, returns empty string if not found or if the language is interpreted. |
| `get_settings_lang_key(language, build_tool)` | `(str, str) -> str` | Looks up `SETTINGS_LANG_KEY_MAP`, returns empty string if not found. |

### 2.2 Database Settings Key Mapping (`claude_setup/domain/stack_mapping.py` -- extend existing)

| Constant | Type | Description |
|----------|------|-------------|
| `DB_SETTINGS_KEY_MAP` | `Dict[str, str]` | Maps database name to settings template key. |
| `CACHE_SETTINGS_KEY_MAP` | `Dict[str, str]` | Maps cache name to settings template key. |

**DB_SETTINGS_KEY_MAP values:**

| db_name | settings key |
|---------|-------------|
| postgresql | `"database-psql"` |
| mysql | `"database-mysql"` |
| oracle | `"database-oracle"` |
| mongodb | `"database-mongodb"` |
| cassandra | `"database-cassandra"` |

**CACHE_SETTINGS_KEY_MAP values:**

| cache_name | settings key |
|------------|-------------|
| redis | `"cache-redis"` |
| dragonfly | `"cache-dragonfly"` |
| memcached | `"cache-memcached"` |

### 2.3 Hooks Assembler (`claude_setup/assembler/hooks_assembler.py`)

| Class/Function | Description |
|----------------|-------------|
| `HooksAssembler` | Stateless class that copies post-compile-check.sh for compiled languages. |
| `assemble(config, output_dir, engine) -> Path` | Main entry point. Returns path to hooks dir or empty path if no hooks needed. |

**Algorithm:**

1. Derive `hook_template_key` from `get_hook_template_key(config.language.name, config.framework.build_tool)`.
2. If empty string, skip (interpreted language -- no hooks needed). Return empty list.
3. Look for `src/hooks-templates/{hook_template_key}/post-compile-check.sh`.
4. If not found, log warning, return empty list.
5. Create `output_dir/hooks/` directory.
6. Copy `post-compile-check.sh` to `output_dir/hooks/post-compile-check.sh`.
7. Set executable permission (`chmod +x`).
8. Return `[Path to hooks dir]`.

**Signature:** `assemble(config: ProjectConfig, output_dir: Path, engine: TemplateEngine) -> List[Path]`

> Note: `engine` parameter included for interface consistency with other assemblers, even though hooks assembler does not perform template rendering.

### 2.4 Settings Assembler (`claude_setup/assembler/settings_assembler.py`)

| Class/Function | Description |
|----------------|-------------|
| `SettingsAssembler` | Stateless class that generates `settings.json` and `settings.local.json`. |
| `assemble(config, output_dir, engine) -> List[Path]` | Main entry point. Returns paths to generated files. |
| `merge_json_arrays(base, overlay)` | Static method. Merges two JSON arrays (lists). Returns combined list. |
| `_collect_permissions(config, src_dir) -> List[str]` | Collects permission strings from all applicable JSON fragment files. |
| `_deduplicate_preserving_order(items)` | Deduplicates a list while preserving insertion order. |
| `_build_hooks_json(hook_template_key) -> dict` | Builds the hooks section dict for compiled languages. Returns empty dict if no hook. |
| `_format_settings(permissions, hooks_section) -> str` | Formats the final settings.json content string. |

**Algorithm for `assemble`:**

1. Resolve `src_dir` (same pattern as `RulesAssembler`).
2. Collect all permission fragments:
   a. Always: `base.json`
   b. Language: `{settings_lang_key}.json` (from `get_settings_lang_key`)
   c. Docker: `docker.json` if `container in ("docker", "podman")`
   d. Kubernetes: `kubernetes.json` if `orchestrator == "kubernetes"`
   e. Docker Compose: `docker-compose.json` if `orchestrator == "docker-compose"`
   f. Database: `{db_settings_key}.json` (from `DB_SETTINGS_KEY_MAP`)
   g. Cache: `{cache_settings_key}.json` (from `CACHE_SETTINGS_KEY_MAP`)
   h. Newman: `testing-newman.json` if `smoke_tests == True`
3. Merge all arrays via `merge_json_arrays`.
4. Deduplicate while preserving order.
5. Build hooks JSON section if `hook_template_key` is non-empty.
6. Write `settings.json` with formatted JSON.
7. Write `settings.local.json` (static template).
8. Return list of generated paths.

**`merge_json_arrays` contract:**

- Input: two `list` values (JSON arrays).
- Output: concatenated `list`.
- Pure function, no side effects.

**Hooks JSON structure (for compiled languages):**

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Write|Edit",
        "hooks": [
          {
            "type": "command",
            "command": "\"$CLAUDE_PROJECT_DIR\"/.claude/hooks/post-compile-check.sh",
            "timeout": 60,
            "statusMessage": "Checking compilation..."
          }
        ]
      }
    ]
  }
}
```

### 2.5 README Assembler (`claude_setup/assembler/readme_assembler.py`)

| Class/Function | Description |
|----------------|-------------|
| `ReadmeAssembler` | Stateless class that generates README.md from template or minimal fallback. |
| `assemble(config, output_dir, engine) -> List[Path]` | Main entry point. Returns path to README.md. |
| `generate_readme(config, output_dir, engine) -> str` | Full README from template with placeholder replacement. |
| `generate_minimal_readme(config) -> str` | Minimal README fallback (no template needed). |
| `_count_rules(output_dir) -> int` | Count .md files in `output_dir/rules/`. |
| `_count_skills(output_dir) -> int` | Count SKILL.md files in `output_dir/skills/`. |
| `_count_agents(output_dir) -> int` | Count .md files in `output_dir/agents/`. |
| `_build_rules_table(output_dir) -> str` | Generate markdown table of rules. |
| `_build_skills_table(output_dir) -> str` | Generate markdown table of skills. |
| `_build_agents_table(output_dir) -> str` | Generate markdown table of agents. |
| `_build_hooks_section(config) -> str` | Generate hooks documentation section. |
| `_build_settings_section() -> str` | Generate settings documentation section. |
| `_build_knowledge_packs_table(output_dir) -> str` | Generate knowledge packs table. |

**Algorithm for `assemble`:**

1. Resolve `src_dir` and find `src/readme-template.md`.
2. If template exists: call `generate_readme()`.
3. If template missing: call `generate_minimal_readme()`.
4. Write result to `output_dir/README.md`.
5. Return `[Path to README.md]`.

**Placeholder replacements in README template:**

| Placeholder | Source |
|-------------|--------|
| `{{PROJECT_NAME}}` | `config.project.name` |
| `{{RULES_COUNT}}` | Count of .md files in `output_dir/rules/` |
| `{{SKILLS_COUNT}}` | Count of SKILL.md files in `output_dir/skills/` |
| `{{AGENTS_COUNT}}` | Count of .md files in `output_dir/agents/` |
| `{{RULES_TABLE}}` | Generated markdown table |
| `{{SKILLS_TABLE}}` | Generated markdown table |
| `{{AGENTS_TABLE}}` | Generated markdown table |
| `{{HOOKS_SECTION}}` | Generated hooks description or "No hooks configured." |
| `{{KNOWLEDGE_PACKS_TABLE}}` | Generated knowledge packs table |
| `{{SETTINGS_SECTION}}` | Static settings documentation section |

**Minimal README template variables:**

- `PROJECT_NAME`: `config.project.name`
- `ARCH_STYLE`: `config.architecture.style`
- `INTERFACE_TYPES`: joined interface types from `config.interfaces`

---

## 3. Existing Classes to Modify

| File | Change | Reason |
|------|--------|--------|
| `claude_setup/domain/stack_mapping.py` | Add `HOOK_TEMPLATE_KEY_MAP`, `SETTINGS_LANG_KEY_MAP`, `DB_SETTINGS_KEY_MAP`, `CACHE_SETTINGS_KEY_MAP` constants and lookup functions | Domain mapping needed by hooks and settings assemblers |
| `claude_setup/assembler/__init__.py` | Add exports for `HooksAssembler`, `SettingsAssembler`, `ReadmeAssembler` | Maintain consistent module exports |

---

## 4. Dependency Direction Validation

```
assembler/hooks_assembler.py    --> domain/stack_mapping.py  (get_hook_template_key)
                                --> models.py                (ProjectConfig)
                                --> template_engine.py       (TemplateEngine -- interface consistency)

assembler/settings_assembler.py --> domain/stack_mapping.py  (get_settings_lang_key, DB/CACHE maps)
                                --> models.py                (ProjectConfig)
                                --> template_engine.py       (TemplateEngine -- interface consistency)

assembler/readme_assembler.py   --> domain/stack_mapping.py  (get_hook_template_key -- for hooks section)
                                --> models.py                (ProjectConfig)
                                --> template_engine.py       (TemplateEngine -- placeholder replacement)
```

All dependencies point inward (assembler -> domain, assembler -> models). No domain -> assembler dependency. No circular references. The `assembler` layer is at the same level as existing assemblers (`rules_assembler.py`, `skills.py`, `agents.py`).

---

## 5. Integration Points

| Integration | Direction | Description |
|-------------|-----------|-------------|
| Pipeline (STORY-009) | Caller | Pipeline calls `HooksAssembler.assemble()`, `SettingsAssembler.assemble()`, `ReadmeAssembler.assemble()` in sequence. |
| `RulesAssembler` | Precedes | Must run before README assembler so rules can be counted and listed. |
| `SkillsAssembler` | Precedes | Must run before README assembler so skills can be counted and listed. |
| `AgentsAssembler` | Precedes | Must run before README assembler so agents can be counted and listed. |
| Hooks templates (`src/hooks-templates/`) | Read | HooksAssembler reads shell scripts from source templates. |
| Settings templates (`src/settings-templates/`) | Read | SettingsAssembler reads JSON fragment files from source templates. |
| README template (`src/readme-template.md`) | Read | ReadmeAssembler reads the README template. |

**Assembly execution order (for STORY-009):**

1. `RulesAssembler.assemble()` -- generates `rules/`
2. `SkillsAssembler.assemble()` -- generates `skills/`
3. `AgentsAssembler.assemble()` -- generates `agents/`
4. `PatternsAssembler.assemble()` -- generates `patterns/`
5. `ProtocolsAssembler.assemble()` -- generates `protocols/`
6. `HooksAssembler.assemble()` -- generates `hooks/`
7. `SettingsAssembler.assemble()` -- generates `settings.json`, `settings.local.json`
8. `ReadmeAssembler.assemble()` -- generates `README.md` (must be LAST since it counts all other artifacts)

---

## 6. Database Changes

N/A -- This is a CLI tool with no database.

---

## 7. API Changes

N/A -- This is a CLI tool with no API. The assembler public methods follow the established pattern:

```python
def assemble(config: ProjectConfig, output_dir: Path, engine: TemplateEngine) -> List[Path]
```

---

## 8. Event Changes

N/A -- No event-driven architecture.

---

## 9. Configuration Changes

No new configuration needed. The assemblers derive all values from the existing `ProjectConfig` model and the new domain mapping constants.

**Source template directories used (already existing in `src/`):**

| Directory | Used By |
|-----------|---------|
| `src/hooks-templates/{key}/` | HooksAssembler |
| `src/settings-templates/` | SettingsAssembler |
| `src/readme-template.md` | ReadmeAssembler |

---

## 10. Risk Assessment

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Byte-for-byte mismatch with bash output in `settings.json` formatting | HIGH | MEDIUM | JSON formatting must replicate exact indentation (6-space indent for permissions array items). Contract tests compare against bash reference output. |
| `merge_json_arrays` ordering differs from bash | MEDIUM | LOW | Preserve insertion order. Deduplication uses `seen` set with list for order. Identical to bash Python snippet. |
| README placeholder replacement edge cases (special chars in project name) | MEDIUM | LOW | Use direct string replacement (not regex). Match bash behavior exactly. |
| Missing hook template for new language combinations | LOW | LOW | Graceful degradation: log warning and skip hooks. |
| `settings.local.json` format mismatch | LOW | LOW | Static content, trivially verifiable. |
| Minimal README missing template variables available in bash | MEDIUM | LOW | Ensure all variables (`ARCH_STYLE`, `INTERFACE_TYPES`) are accessible from `ProjectConfig`. |

---

## Implementation Order

Following the inner-to-outer rule:

1. **Domain mappings** -- Add constants and lookup functions to `stack_mapping.py`
2. **HooksAssembler** -- Simplest assembler (copy + chmod)
3. **SettingsAssembler** -- JSON merge logic + hooks section generation
4. **ReadmeAssembler** -- Template replacement + table generation + minimal fallback
5. **Update `__init__.py`** -- Export new assemblers
6. **Unit tests** -- Per assembler, per function
7. **Contract tests** -- Byte-for-byte comparison with bash reference output

---

## File Summary

### New Files

| File | Description |
|------|-------------|
| `claude_setup/assembler/hooks_assembler.py` | HooksAssembler class |
| `claude_setup/assembler/settings_assembler.py` | SettingsAssembler class with merge_json_arrays |
| `claude_setup/assembler/readme_assembler.py` | ReadmeAssembler class with full and minimal generation |
| `tests/assembler/test_hooks_assembler.py` | Unit tests for HooksAssembler |
| `tests/assembler/test_settings_assembler.py` | Unit tests for SettingsAssembler |
| `tests/assembler/test_readme_assembler.py` | Unit tests for ReadmeAssembler |
| `tests/assembler/test_stack_mapping_hooks_settings.py` | Unit tests for new domain mappings |

### Modified Files

| File | Change |
|------|--------|
| `claude_setup/domain/stack_mapping.py` | Add HOOK_TEMPLATE_KEY_MAP, SETTINGS_LANG_KEY_MAP, DB_SETTINGS_KEY_MAP, CACHE_SETTINGS_KEY_MAP + lookup functions |
| `claude_setup/assembler/__init__.py` | Add HooksAssembler, SettingsAssembler, ReadmeAssembler exports |
