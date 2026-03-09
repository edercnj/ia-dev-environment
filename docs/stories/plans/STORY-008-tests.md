# Test Plan: STORY-008 -- Hooks, Settings, and README Assemblers

**Story:** STORY-008 -- Hooks, Settings, and README Assemblers
**Stack:** Python 3.9 + Click 8.1
**Framework:** pytest + pytest-cov
**Coverage Targets:** Line >= 95%, Branch >= 90%
**Naming Convention:** `test_{function}_{scenario}_{expected}`

---

## 1. Test File Structure

```
tests/
├── assembler/
│   ├── conftest.py                      # Existing fixtures (shared configs, config_factory)
│   ├── test_hooks_assembler.py          # Unit + Integration: HooksAssembler
│   ├── test_settings_assembler.py       # Unit + Integration: SettingsAssembler + merge_json_arrays
│   ├── test_readme_assembler.py         # Unit + Integration: ReadmeAssembler
│   ├── test_hooks_contract.py           # Contract: byte-for-byte comparison with bash output
│   ├── test_settings_contract.py        # Contract: byte-for-byte comparison with bash output
│   └── test_readme_contract.py          # Contract: byte-for-byte comparison with bash output
└── fixtures/
    └── assembler/
        ├── hooks-templates/             # Hook script templates for tests
        │   ├── java-maven/
        │   │   └── post-compile-check.sh
        │   ├── typescript/
        │   │   └── post-compile-check.sh
        │   └── go/
        │       └── post-compile-check.sh
        ├── settings-templates/          # Permission JSON fragments for tests
        │   ├── base.json
        │   ├── java-maven.json
        │   ├── python-pip.json
        │   ├── docker.json
        │   ├── kubernetes.json
        │   ├── docker-compose.json
        │   ├── database-psql.json
        │   ├── database-mysql.json
        │   ├── cache-redis.json
        │   └── testing-newman.json
        ├── readme-template.md           # README template with placeholders
        └── reference-output/            # Reference bash output for contract tests
            ├── hooks/
            ├── settings/
            └── readme/
```

---

## 2. Fixtures Required

### 2.1 New Fixtures for `conftest.py`

```python
@pytest.fixture
def compiled_lang_config(config_factory) -> ProjectConfig:
    """Java config -- compiled language with hook template key."""
    return config_factory(
        language={"name": "java", "version": "21"},
        framework={"name": "quarkus", "version": "3.17", "build_tool": "maven"},
    )


@pytest.fixture
def interpreted_lang_config() -> ProjectConfig:
    """Python config -- interpreted language, no hook template key."""
    # Same as minimal_cli_config
    ...


@pytest.fixture
def hooks_src_dir(tmp_path) -> Path:
    """Create minimal hooks template directory."""
    hooks_dir = tmp_path / "hooks-templates" / "java-maven"
    hooks_dir.mkdir(parents=True)
    script = hooks_dir / "post-compile-check.sh"
    script.write_text("#!/bin/bash\necho compile check\n", encoding="utf-8")
    return tmp_path / "hooks-templates"


@pytest.fixture
def settings_src_dir(tmp_path) -> Path:
    """Create minimal settings template directory with JSON fragments."""
    ...


@pytest.fixture
def readme_template_path(tmp_path) -> Path:
    """Copy readme-template.md to tmp_path."""
    ...
```

---

## 3. Unit Tests: `test_hooks_assembler.py`

### 3.1 HooksAssembler.assemble()

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_assemble_compiled_language_copies_hook_script` | Java config (compiled), hook template exists in src | `output_dir/hooks/post-compile-check.sh` exists |
| 2 | `test_assemble_compiled_language_hook_is_executable` | Java config (compiled), hook template exists | Hook file has executable permission (chmod +x) |
| 3 | `test_assemble_compiled_language_hook_content_matches_template` | Java config (compiled) | Output file content matches source template |
| 4 | `test_assemble_interpreted_language_skips_hooks` | Python config (interpreted, HOOK_TEMPLATE_KEY is empty) | `output_dir/hooks/` does not exist or is empty |
| 5 | `test_assemble_interpreted_language_returns_none_or_empty` | Python config (interpreted) | Returns `None` or empty path list |
| 6 | `test_assemble_missing_hook_template_skips_gracefully` | Java config but hook template file does not exist in src | No exception raised; returns None or logs warning |
| 7 | `test_assemble_creates_hooks_directory` | Java config, hooks dir does not pre-exist | `output_dir/hooks/` directory is created |
| 8 | `test_assemble_returns_path_to_hook_script` | Java config, template exists | Returns `Path` pointing to created hook file |

### 3.2 Hook Template Key Resolution

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 9 | `test_hook_template_key_java_maven_returns_java_maven` | Java + Maven config | Key is `"java-maven"` |
| 10 | `test_hook_template_key_java_gradle_returns_java_gradle` | Java + Gradle config | Key is `"java-gradle"` |
| 11 | `test_hook_template_key_kotlin_returns_kotlin` | Kotlin config | Key is `"kotlin"` |
| 12 | `test_hook_template_key_typescript_returns_typescript` | TypeScript config | Key is `"typescript"` |
| 13 | `test_hook_template_key_python_returns_empty` | Python config | Key is `""` (no hook) |
| 14 | `test_hook_template_key_go_returns_go` | Go config | Key is `"go"` |
| 15 | `test_hook_template_key_rust_returns_rust` | Rust config | Key is `"rust"` |

### 3.3 Parametrized Hook Template Key (Contract)

```python
@pytest.mark.parametrize(
    "language, build_tool, expected_key",
    [
        ("java", "maven", "java-maven"),
        ("java", "gradle", "java-gradle"),
        ("kotlin", "gradle", "kotlin"),
        ("typescript", "npm", "typescript"),
        ("python", "pip", ""),
        ("go", "go", "go"),
        ("rust", "cargo", "rust"),
    ],
    ids=lambda x: str(x),
)
def test_hook_template_key_language_mapping_matches_bash(
    language, build_tool, expected_key, config_factory
):
    ...
```

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 16 | `test_hook_template_key_language_mapping_matches_bash` | 7 parametrized language-to-key mappings | Correct key for each language |

---

## 4. Unit Tests: `test_settings_assembler.py`

### 4.1 `merge_json_arrays()`

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_merge_json_arrays_two_disjoint_arrays_returns_union` | `["a", "b"]` + `["c", "d"]` | `["a", "b", "c", "d"]` |
| 2 | `test_merge_json_arrays_empty_base_returns_overlay` | `[]` + `["a", "b"]` | `["a", "b"]` |
| 3 | `test_merge_json_arrays_empty_overlay_returns_base` | `["a", "b"]` + `[]` | `["a", "b"]` |
| 4 | `test_merge_json_arrays_both_empty_returns_empty` | `[]` + `[]` | `[]` |
| 5 | `test_merge_json_arrays_overlapping_items_concatenates` | `["a", "b"]` + `["b", "c"]` | `["a", "b", "b", "c"]` (dedup happens later) |
| 6 | `test_merge_json_arrays_preserves_order` | `["z", "a"]` + `["m", "b"]` | `["z", "a", "m", "b"]` |
| 7 | `test_merge_json_arrays_complex_permission_strings` | `["Bash(npm run *)"]` + `["Bash(docker *)"]` | Both present in result |

### 4.2 Permission Deduplication

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 8 | `test_deduplicate_permissions_removes_duplicates` | `["a", "b", "a", "c", "b"]` | `["a", "b", "c"]` |
| 9 | `test_deduplicate_permissions_preserves_first_occurrence_order` | `["c", "a", "b", "a"]` | `["c", "a", "b"]` |
| 10 | `test_deduplicate_permissions_empty_list_returns_empty` | `[]` | `[]` |
| 11 | `test_deduplicate_permissions_no_duplicates_unchanged` | `["a", "b", "c"]` | `["a", "b", "c"]` |

### 4.3 `SettingsAssembler.assemble()` -- Permission Collection

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 12 | `test_assemble_always_includes_base_permissions` | Any config, `base.json` exists | Base permissions present in output |
| 13 | `test_assemble_includes_language_permissions` | Python config, `python-pip.json` exists | Language permissions merged |
| 14 | `test_assemble_docker_container_includes_docker_permissions` | Config with `container: "docker"`, `docker.json` exists | Docker permissions merged |
| 15 | `test_assemble_podman_container_includes_docker_permissions` | Config with `container: "podman"` | Docker permissions merged (same file) |
| 16 | `test_assemble_no_container_excludes_docker_permissions` | Config with `container: "none"` | Docker permissions NOT in output |
| 17 | `test_assemble_kubernetes_orchestrator_includes_k8s_permissions` | Config with `orchestrator: "kubernetes"` | K8s permissions merged |
| 18 | `test_assemble_docker_compose_orchestrator_includes_dc_permissions` | Config with `orchestrator: "docker-compose"` | Docker Compose permissions merged |
| 19 | `test_assemble_no_orchestrator_excludes_k8s_and_dc_permissions` | Config with `orchestrator: "none"` | Neither K8s nor DC permissions |
| 20 | `test_assemble_postgresql_includes_psql_permissions` | Config with `database.name: "postgresql"` | `database-psql` permissions merged |
| 21 | `test_assemble_mysql_includes_mysql_permissions` | Config with `database.name: "mysql"` | `database-mysql` permissions merged |
| 22 | `test_assemble_no_database_excludes_db_permissions` | Config with `database.name: "none"` | No DB permissions |
| 23 | `test_assemble_redis_cache_includes_cache_permissions` | Config with `cache.name: "redis"` | `cache-redis` permissions merged |
| 24 | `test_assemble_no_cache_excludes_cache_permissions` | Config with `cache.name: "none"` | No cache permissions |
| 25 | `test_assemble_smoke_tests_true_includes_newman_permissions` | Config with `smoke_tests: True` | Newman permissions merged |
| 26 | `test_assemble_smoke_tests_false_excludes_newman_permissions` | Config with `smoke_tests: False` | Newman permissions NOT in output |

### 4.4 Parametrized Database Settings Key Mapping (Contract)

```python
@pytest.mark.parametrize(
    "db_type, expected_key",
    [
        ("postgresql", "database-psql"),
        ("mysql", "database-mysql"),
        ("oracle", "database-oracle"),
        ("mongodb", "database-mongodb"),
        ("cassandra", "database-cassandra"),
    ],
    ids=lambda x: str(x),
)
def test_db_settings_key_mapping_matches_bash(db_type, expected_key, config_factory):
    ...
```

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 27 | `test_db_settings_key_mapping_matches_bash` | 5 parametrized db-to-key mappings | Correct settings key for each DB |

### 4.5 `SettingsAssembler.assemble()` -- Hooks Section in settings.json

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 28 | `test_assemble_compiled_language_includes_hooks_section` | Java config (HOOK_TEMPLATE_KEY non-empty) | `settings.json` contains `"hooks"` key with `PostToolUse` |
| 29 | `test_assemble_hooks_section_matcher_is_write_or_edit` | Java config | Hooks matcher is `"Write\|Edit"` |
| 30 | `test_assemble_hooks_section_command_points_to_hook_script` | Java config | Command references `.claude/hooks/post-compile-check.sh` |
| 31 | `test_assemble_hooks_section_timeout_is_60` | Java config | Timeout is `60` |
| 32 | `test_assemble_interpreted_language_excludes_hooks_section` | Python config (HOOK_TEMPLATE_KEY empty) | `settings.json` has NO `"hooks"` key |

### 4.6 `SettingsAssembler.assemble()` -- Output Files

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 33 | `test_assemble_creates_settings_json` | Any valid config | `output_dir/settings.json` exists |
| 34 | `test_assemble_settings_json_is_valid_json` | Any valid config | `json.loads()` succeeds |
| 35 | `test_assemble_settings_json_has_permissions_allow_key` | Any valid config | `data["permissions"]["allow"]` exists and is a list |
| 36 | `test_assemble_creates_settings_local_json` | Any valid config | `output_dir/settings.local.json` exists |
| 37 | `test_assemble_settings_local_json_has_empty_allow` | Any valid config | `data["permissions"]["allow"]` is `[]` |
| 38 | `test_assemble_settings_local_json_is_valid_json` | Any valid config | `json.loads()` succeeds |
| 39 | `test_assemble_returns_path_to_settings_json` | Any valid config | Returns `Path` pointing to created settings.json |

### 4.7 Full-Featured Permission Merge (Integration)

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 40 | `test_assemble_full_featured_merges_all_permission_sources` | Full-featured Java config with DB, cache, docker, k8s, smoke tests | Output contains permissions from base + java-maven + docker + k8s + psql + redis + newman |
| 41 | `test_assemble_full_featured_permissions_are_deduplicated` | Full-featured config with overlapping permissions across sources | No duplicate entries in `permissions.allow` array |
| 42 | `test_assemble_full_featured_permissions_formatted_with_indentation` | Full-featured config | Each permission line has correct indentation (6 spaces) |

### 4.8 Edge Cases

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 43 | `test_assemble_missing_base_json_still_generates_settings` | `base.json` does not exist | settings.json generated with remaining sources |
| 44 | `test_assemble_missing_lang_json_still_generates_settings` | Language JSON does not exist | settings.json generated without language permissions |
| 45 | `test_assemble_all_permission_files_missing_generates_empty_allow` | No permission JSON files exist | `permissions.allow` is `[]` |
| 46 | `test_assemble_unknown_database_no_db_permissions` | Config with `database.name: "sqlite"` (no mapping) | No DB-specific permissions added |

---

## 5. Unit Tests: `test_readme_assembler.py`

### 5.1 `generate_readme()` -- Full Template

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_generate_readme_replaces_project_name_placeholder` | Config with `project.name: "my-service"` | Output contains `"my-service"`, no `"{{PROJECT_NAME}}"` |
| 2 | `test_generate_readme_replaces_rules_count_placeholder` | Output dir with 5 rule files | Output contains `"5"` at rules count position |
| 3 | `test_generate_readme_replaces_skills_count_placeholder` | Output dir with 3 skill SKILL.md files | Output contains `"3"` at skills count position |
| 4 | `test_generate_readme_replaces_agents_count_placeholder` | Output dir with 4 agent .md files | Output contains `"4"` at agents count position |
| 5 | `test_generate_readme_generates_rules_table` | Output dir with rules `01-identity.md`, `03-coding.md` | Table has rows for each rule with number and scope |
| 6 | `test_generate_readme_generates_skills_table` | Output dir with skills `coding-standards/SKILL.md` | Table has skill name and `/coding-standards` command |
| 7 | `test_generate_readme_generates_agents_table` | Output dir with agents `review.md`, `developer.md` | Table has rows for each agent |
| 8 | `test_generate_readme_generates_hooks_section_compiled_lang` | Java config (has hook template key) | Hooks section contains `PostToolUse`, file extension, compile command |
| 9 | `test_generate_readme_generates_hooks_section_no_hooks` | Python config (no hook template key) | Hooks section says "No hooks configured." |
| 10 | `test_generate_readme_generates_knowledge_packs_table` | Output dir with KP skills (user-invocable: false) | Knowledge packs table has entries |
| 11 | `test_generate_readme_generates_settings_section` | Any config | Settings section mentions `settings.json` and `settings.local.json` |
| 12 | `test_generate_readme_no_leftover_placeholders` | Full config with all sections populated | Output contains no `{{...}}` placeholders |

### 5.2 `generate_readme()` -- Counting Logic

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 13 | `test_generate_readme_rules_count_zero_when_no_rules_dir` | No `rules/` directory in output | Rules count is `0` |
| 14 | `test_generate_readme_skills_count_zero_when_no_skills_dir` | No `skills/` directory in output | Skills count is `0` |
| 15 | `test_generate_readme_agents_count_zero_when_no_agents_dir` | No `agents/` directory in output | Agents count is `0` |
| 16 | `test_generate_readme_rules_count_only_counts_md_files` | `rules/` has `.md` and `.txt` files | Only `.md` files counted |
| 17 | `test_generate_readme_skills_count_only_counts_skill_md` | `skills/` has dirs with and without `SKILL.md` | Only dirs with `SKILL.md` counted |
| 18 | `test_generate_readme_agents_count_only_counts_md_files` | `agents/` has `.md` and `.gitkeep` | Only `.md` files counted |

### 5.3 `generate_readme()` -- Rules Table Formatting

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 19 | `test_generate_readme_rules_table_extracts_number_from_filename` | Rule file `03-coding-standards.md` | Table row starts with `3` or `03` |
| 20 | `test_generate_readme_rules_table_extracts_scope_from_filename` | Rule file `04-architecture-summary.md` | Scope column contains `"architecture summary"` |
| 21 | `test_generate_readme_rules_table_has_header_row` | At least one rule file | Table starts with header `\| # \| File \| Scope \|` |

### 5.4 `generate_readme()` -- Skills Table Formatting

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 22 | `test_generate_readme_skills_table_shows_skill_name_bold` | Skill dir `coding-standards` | Row contains `**coding-standards**` |
| 23 | `test_generate_readme_skills_table_shows_slash_command` | Skill dir `coding-standards` | Row contains `/coding-standards` |
| 24 | `test_generate_readme_skills_table_extracts_description_from_skill_md` | SKILL.md has `description: "Enforce standards"` | Row contains description text |

### 5.5 `generate_readme()` -- Knowledge Packs Detection

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 25 | `test_generate_readme_kp_detected_by_user_invocable_false` | SKILL.md contains `user-invocable: false` | Skill listed in knowledge packs table |
| 26 | `test_generate_readme_kp_detected_by_knowledge_pack_header` | SKILL.md starts with `# Knowledge Pack` | Skill listed in knowledge packs table |
| 27 | `test_generate_readme_kp_not_detected_for_invocable_skill` | SKILL.md has no KP markers | Skill NOT in knowledge packs table |
| 28 | `test_generate_readme_kp_table_fallback_when_no_kps_found` | No skills match KP criteria | Knowledge packs section says "No knowledge packs configured." |

### 5.6 `generate_minimal_readme()`

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 29 | `test_generate_minimal_readme_contains_project_name` | Config with `project.name: "my-cli"` | Output contains `"my-cli"` |
| 30 | `test_generate_minimal_readme_contains_directory_structure` | Any config | Output contains `.claude/` structure block |
| 31 | `test_generate_minimal_readme_contains_architecture_style` | Config with `architecture.style: "library"` | Output contains `"library"` |
| 32 | `test_generate_minimal_readme_contains_interface_types` | Config with interfaces `["cli"]` | Output contains `"cli"` |
| 33 | `test_generate_minimal_readme_contains_tips_section` | Any config | Output contains "Rules are always active" |
| 34 | `test_generate_minimal_readme_no_placeholders_remain` | Any config | Output contains no `{{...}}` or `{...}` template markers |

### 5.7 `ReadmeAssembler.assemble()` -- Template Selection

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 35 | `test_assemble_uses_full_template_when_available` | README template exists at expected path | Output is full README (has Rules, Skills, Agents sections) |
| 36 | `test_assemble_falls_back_to_minimal_when_template_missing` | README template does not exist | Output is minimal README (shorter, no tables) |
| 37 | `test_assemble_creates_readme_md_in_output_dir` | Any valid config | `output_dir/README.md` exists |
| 38 | `test_assemble_returns_path_to_readme` | Any valid config | Returns `Path` pointing to created README.md |
| 39 | `test_assemble_output_is_valid_utf8` | Config with project name containing accented chars | Output file is valid UTF-8 |

### 5.8 Edge Cases

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 40 | `test_generate_readme_empty_rules_dir_produces_empty_table` | `rules/` directory exists but is empty | No table rows (or just header) |
| 41 | `test_generate_readme_empty_skills_dir_produces_empty_table` | `skills/` directory exists but is empty | No skill table rows |
| 42 | `test_generate_readme_empty_agents_dir_produces_empty_table` | `agents/` directory exists but is empty | No agent table rows |
| 43 | `test_generate_readme_project_name_with_special_chars_escaped` | Config with `project.name: "my-service (v2)"` | Name rendered correctly, no sed/regex issues |

---

## 6. Integration Tests

### 6.1 Full Pipeline: Hooks + Settings + README Together

| # | Test Name | File | Scenario | Expected |
|---|-----------|------|----------|----------|
| 1 | `test_full_pipeline_compiled_lang_all_files_generated` | `test_settings_assembler.py` | Java full-featured config, run all 3 assemblers | hooks/, settings.json, settings.local.json, README.md all exist |
| 2 | `test_full_pipeline_interpreted_lang_no_hooks_dir` | `test_settings_assembler.py` | Python minimal config, run all 3 assemblers | settings.json and README.md exist; no hooks/ directory |
| 3 | `test_settings_json_hooks_section_references_existing_hook_script` | `test_settings_assembler.py` | Run HooksAssembler then SettingsAssembler | settings.json hooks command path matches actual hook file |

### 6.2 Settings Assembler Multi-Source Merge (Integration)

| # | Test Name | File | Scenario | Expected |
|---|-----------|------|----------|----------|
| 4 | `test_assemble_merges_base_plus_lang_plus_docker` | `test_settings_assembler.py` | Config with docker container, create base.json + lang.json + docker.json | All three sets of permissions present |
| 5 | `test_assemble_merges_six_sources_deduplicates` | `test_settings_assembler.py` | Config with all features, create 6 JSON files with some overlapping entries | Deduplicated union in output |
| 6 | `test_assemble_merge_order_base_then_lang_then_infra_then_db` | `test_settings_assembler.py` | Multiple sources with unique entries | Base permissions appear first, then lang, then infra, then DB |

### 6.3 README Assembler with Pre-populated Output (Integration)

| # | Test Name | File | Scenario | Expected |
|---|-----------|------|----------|----------|
| 7 | `test_assemble_readme_with_prepopulated_rules_counts_correctly` | `test_readme_assembler.py` | Pre-create 5 rule files in output_dir/rules/ | README says "5 rules" |
| 8 | `test_assemble_readme_with_prepopulated_skills_counts_correctly` | `test_readme_assembler.py` | Pre-create 3 skill dirs with SKILL.md in output_dir/skills/ | README says "3 skills" |
| 9 | `test_assemble_readme_with_prepopulated_agents_counts_correctly` | `test_readme_assembler.py` | Pre-create 4 agent .md files in output_dir/agents/ | README says "4 agents" |

---

## 7. Contract Tests

Contract tests validate byte-for-byte compatibility with bash output (RULE-005). Require reference fixtures generated by the original bash script.

### 7.1 Approach

```python
@pytest.mark.contract
@pytest.mark.skipif(
    not REFERENCE_OUTPUT_DIR.exists(),
    reason="Reference bash output fixtures not available",
)
class TestHooksContract:
    ...
```

### 7.2 `test_hooks_contract.py`

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_hooks_output_matches_bash_for_java_quarkus` | Run assembler with Java-Quarkus config | `post-compile-check.sh` matches reference byte-for-byte |
| 2 | `test_hooks_output_python_no_hooks_matches_bash` | Run assembler with Python config | No hooks directory, matching bash behavior |

### 7.3 `test_settings_contract.py`

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 3 | `test_settings_json_matches_bash_for_java_quarkus` | Run assembler with Java-Quarkus full-featured config | `settings.json` matches reference byte-for-byte |
| 4 | `test_settings_json_matches_bash_for_python_cli` | Run assembler with Python CLI config | `settings.json` matches reference byte-for-byte |
| 5 | `test_settings_local_json_matches_bash` | Any config | `settings.local.json` matches reference byte-for-byte |
| 6 | `test_settings_permissions_order_matches_bash` | Full-featured config | Permission array order matches bash exactly |
| 7 | `test_settings_hooks_section_format_matches_bash` | Java config | Hooks JSON structure matches bash exactly (indentation, keys) |

### 7.4 `test_readme_contract.py`

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 8 | `test_readme_full_matches_bash_for_java_quarkus` | Run assembler with Java-Quarkus full-featured config | `README.md` matches reference byte-for-byte |
| 9 | `test_readme_minimal_matches_bash_for_python_cli` | Run assembler with Python CLI config (no template) | Minimal `README.md` matches reference byte-for-byte |
| 10 | `test_readme_output_file_exists_matches_bash` | Compare file presence | Same files generated as bash |

---

## 8. Edge Cases (Cross-cutting)

### 8.1 Missing Source Directories

| # | Test Name | File | Scenario | Expected |
|---|-----------|------|----------|----------|
| 1 | `test_hooks_assemble_missing_src_dir_no_exception` | `test_hooks_assembler.py` | `src_dir` does not exist | No exception; returns None or empty |
| 2 | `test_settings_assemble_missing_templates_dir_generates_empty` | `test_settings_assembler.py` | Settings templates dir missing | settings.json with empty `allow` array |
| 3 | `test_readme_assemble_missing_output_subdirs_counts_zero` | `test_readme_assembler.py` | No rules/, skills/, agents/ in output | All counts are 0 |

### 8.2 File Encoding

| # | Test Name | File | Scenario | Expected |
|---|-----------|------|----------|----------|
| 4 | `test_settings_json_output_is_utf8` | `test_settings_assembler.py` | Any config | Output file is valid UTF-8 |
| 5 | `test_readme_output_is_utf8` | `test_readme_assembler.py` | Any config | Output file is valid UTF-8 |

### 8.3 Idempotency

| # | Test Name | File | Scenario | Expected |
|---|-----------|------|----------|----------|
| 6 | `test_hooks_assemble_twice_same_result` | `test_hooks_assembler.py` | Run assemble twice with same config | Output is identical both times |
| 7 | `test_settings_assemble_twice_same_result` | `test_settings_assembler.py` | Run assemble twice with same config | Output is identical both times |
| 8 | `test_readme_assemble_twice_same_result` | `test_readme_assembler.py` | Run assemble twice with same config | Output is identical both times |

---

## 9. Mocking Strategy

| What | Mock? | Rationale |
|------|-------|-----------|
| Domain models (`ProjectConfig`, etc.) | NO | Use real objects -- never mock domain logic |
| `TemplateEngine` | NO | Use real engine with test templates (integration) |
| File system | NO | Use `tmp_path` fixture for real filesystem operations |
| `json.loads` / `json.dumps` | NO | Use real JSON parsing |
| `shutil.copy2` / `shutil.copytree` | NO | Use real copy against temp dirs |
| Permission JSON fragment files | NO | Create real files in `tmp_path` with test data |

---

## 10. Branch Coverage Targets

| Module | Key Branches | Strategy |
|--------|-------------|----------|
| `hooks.py :: assemble` | HOOK_TEMPLATE_KEY empty vs non-empty; template file exists vs missing | Tests 1-8 |
| `hooks.py :: _resolve_hook_key` | Each language case (7 branches) | Tests 9-16 |
| `settings.py :: assemble` | base exists/missing; lang exists/missing; docker/podman/none; k8s/dc/none; each DB type; cache none/present; smoke true/false; hook key empty/present | Tests 12-46 |
| `settings.py :: merge_json_arrays` | Empty/non-empty combinations | Tests 1-7 |
| `settings.py :: _deduplicate` | Empty; no dups; with dups | Tests 8-11 |
| `readme.py :: generate_readme` | Template exists/missing; each dir exists/empty/missing; KP detection (two markers); hooks present/absent | Tests 1-43 |
| `readme.py :: generate_minimal_readme` | All paths (single flow) | Tests 29-34 |

---

## 11. Estimated Coverage

| Module | Estimated Line Coverage | Estimated Branch Coverage |
|--------|------------------------|--------------------------|
| `assembler/hooks.py` | >= 98% | >= 95% |
| `assembler/settings.py` | >= 97% | >= 92% |
| `assembler/readme.py` | >= 96% | >= 91% |
| **Overall (STORY-008 modules)** | **>= 96%** | **>= 92%** |

---

## 12. Test Execution Configuration

### 12.1 `pyproject.toml` Markers

```toml
[tool.pytest.ini_options]
markers = [
    "contract: Contract tests for byte-for-byte bash compatibility",
]
```

### 12.2 Run Commands

```bash
# All STORY-008 tests
pytest tests/assembler/test_hooks_assembler.py tests/assembler/test_settings_assembler.py tests/assembler/test_readme_assembler.py --cov=ia_dev_env/assembler --cov-branch --cov-report=html

# Unit tests only
pytest tests/assembler/test_hooks_assembler.py tests/assembler/test_settings_assembler.py tests/assembler/test_readme_assembler.py -k "not contract"

# Contract tests only
pytest tests/assembler/test_hooks_contract.py tests/assembler/test_settings_contract.py tests/assembler/test_readme_contract.py -m contract

# Coverage check
pytest tests/assembler/test_hooks_assembler.py tests/assembler/test_settings_assembler.py tests/assembler/test_readme_assembler.py --cov=ia_dev_env/assembler --cov-branch --cov-fail-under=95
```

---

## 13. Acceptance Criteria Traceability

| Gherkin Scenario (from STORY-008) | Test(s) |
|-----------------------------------|---------|
| Gerar hooks.json valido | `test_hooks_assembler.py` tests 1-8 |
| Compiled language gets hook file | `test_hooks_assembler.py` tests 1-3, 7, 8 |
| Interpreted language (Python) skips hooks | `test_hooks_assembler.py` tests 4, 5 |
| Gerar settings.json com merge de arrays | `test_settings_assembler.py` tests 12-42 |
| merge_json_arrays correctness | `test_settings_assembler.py` tests 1-7 |
| Permission deduplication | `test_settings_assembler.py` tests 8-11, 41 |
| Hooks section in settings for compiled langs | `test_settings_assembler.py` tests 28-32 |
| No hooks section for Python | `test_settings_assembler.py` test 32 |
| settings.local.json generated | `test_settings_assembler.py` tests 36-38 |
| Gerar README completo | `test_readme_assembler.py` tests 1-12, 35 |
| Gerar README minimo | `test_readme_assembler.py` tests 29-34, 36 |
| Correct counts in README | `test_readme_assembler.py` tests 2-4, 13-18 |
| Output identico ao bash | Contract tests 1-10 |

---

## 14. Total Test Count Summary

| File | Tests |
|------|-------|
| `test_hooks_assembler.py` (unit + edge) | 16 + 3 = 19 |
| `test_settings_assembler.py` (unit + integration + edge) | 46 + 6 + 4 = 56 |
| `test_readme_assembler.py` (unit + integration + edge) | 43 + 3 + 5 = 51 |
| `test_hooks_contract.py` | 2 |
| `test_settings_contract.py` | 5 |
| `test_readme_contract.py` | 3 |
| **Total** | **136** |
