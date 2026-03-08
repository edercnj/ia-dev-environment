# Test Plan: STORY-009 -- CLI Pipeline and Orchestration

**Story:** STORY-009 -- CLI Pipeline e Orquestracao
**Date:** 2026-03-01
**Framework:** pytest + click.testing.CliRunner
**Coverage Target:** >= 95% Line, >= 90% Branch

---

## 1. Test File Structure

```
tests/
├── unit/
│   ├── __init__.py
│   ├── test_utils.py                  # atomic_output, setup_logging, find_src_dir
│   ├── test_pipeline_result.py        # PipelineResult dataclass
│   └── test_pipeline.py              # run_pipeline with mocked assemblers
├── integration/
│   ├── __init__.py
│   └── test_pipeline_integration.py   # Full pipeline with real assemblers
├── cli/
│   ├── __init__.py
│   ├── test_cli_generate.py           # generate command tests
│   └── test_cli_validate.py           # validate command tests
├── test_cli.py                        # Update: new command structure (help, version)
└── test_cli_init.py                   # Update: migrate to generate command
```

---

## 2. Unit Tests

### 2.1 `test_utils.py` -- `atomic_output()` Context Manager

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_atomic_output_success_copies_files_to_dest` | Create files in yielded temp dir, exit without error | Files appear in `dest_dir`, temp dir removed |
| 2 | `test_atomic_output_success_preserves_directory_structure` | Create nested dirs/files in temp dir | Nested structure intact in `dest_dir` |
| 3 | `test_atomic_output_failure_cleans_temp_dir` | Raise exception inside context | Temp dir removed, `dest_dir` unchanged |
| 4 | `test_atomic_output_failure_no_partial_files_in_dest` | Raise exception after creating files in temp | `dest_dir` has zero new files |
| 5 | `test_atomic_output_failure_reraises_exception` | Raise `RuntimeError` inside context | `RuntimeError` propagates to caller |
| 6 | `test_atomic_output_yields_path_object` | Enter context | Yielded value is a `Path` instance pointing to existing dir |
| 7 | `test_atomic_output_temp_dir_is_writable` | Enter context, write a file | File write succeeds without error |
| 8 | `test_atomic_output_nested_error_in_cleanup_does_not_swallow_original` | Raise error; simulate cleanup failure via monkeypatch | Original exception propagates |
| 9 | `test_atomic_output_dest_dir_created_if_not_exists` | Provide non-existent `dest_dir` | `dest_dir` created during atomic move |
| 10 | `test_atomic_output_existing_dest_dir_content_preserved` | `dest_dir` has pre-existing files, context succeeds | Pre-existing files remain alongside new files |

**Branch coverage targets:**
- Success path (no exception)
- Failure path (exception raised)
- Cleanup error path (exception during `shutil.rmtree`)

### 2.2 `test_utils.py` -- `setup_logging()`

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_setup_logging_verbose_true_sets_debug_level` | `verbose=True` | Root logger level is `DEBUG` |
| 2 | `test_setup_logging_verbose_false_sets_info_level` | `verbose=False` | Root logger level is `INFO` |
| 3 | `test_setup_logging_configures_handler` | Call `setup_logging` | At least one handler attached to root logger |

### 2.3 `test_utils.py` -- `find_src_dir()`

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_find_src_dir_existing_returns_path` | `src/` directory exists relative to package | Returns valid `Path` to `src/` |
| 2 | `test_find_src_dir_missing_raises_file_not_found` | `src/` directory does not exist (monkeypatch `Path.exists`) | Raises `FileNotFoundError` |
| 3 | `test_find_src_dir_returns_absolute_path` | Normal invocation | Returned path is absolute (`path.is_absolute()`) |

### 2.4 `test_pipeline_result.py` -- `PipelineResult` Dataclass

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_pipeline_result_init_stores_all_fields` | Construct with all fields | All attributes accessible and correct |
| 2 | `test_pipeline_result_success_true` | `success=True` | `result.success is True` |
| 3 | `test_pipeline_result_success_false` | `success=False` | `result.success is False` |
| 4 | `test_pipeline_result_empty_files_list` | `files_generated=[]` | Empty list stored |
| 5 | `test_pipeline_result_empty_warnings_list` | `warnings=[]` | Empty list stored |
| 6 | `test_pipeline_result_duration_ms_zero` | `duration_ms=0` | Value is `0` |
| 7 | `test_pipeline_result_output_dir_is_path` | `output_dir=Path("/tmp/out")` | Type is `Path` |

### 2.5 `test_pipeline.py` -- `run_pipeline()` Orchestration (Mocked Assemblers)

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_run_pipeline_success_returns_true` | All assemblers succeed (mocked) | `result.success is True` |
| 2 | `test_run_pipeline_success_returns_files_generated` | Assemblers produce files (mocked) | `result.files_generated` is non-empty |
| 3 | `test_run_pipeline_success_returns_positive_duration` | All assemblers succeed | `result.duration_ms > 0` |
| 4 | `test_run_pipeline_success_output_dir_matches` | Provide `output_dir` | `result.output_dir == output_dir` |
| 5 | `test_run_pipeline_calls_assemblers_in_order` | Mock all assemblers with side effects tracking call order | Call order matches: Rules, Skills, Agents, Patterns, Protocols, Hooks, Settings, Readme |
| 6 | `test_run_pipeline_readme_assembler_called_last` | Mock assemblers, track order | `ReadmeAssembler.assemble()` is the last call |
| 7 | `test_run_pipeline_partial_failure_returns_false` | One assembler raises exception | `result.success is False` |
| 8 | `test_run_pipeline_partial_failure_cleans_temp` | Assembler raises mid-pipeline | No partial files in `output_dir` |
| 9 | `test_run_pipeline_all_fail_returns_false` | All assemblers raise | `result.success is False` |
| 10 | `test_run_pipeline_all_fail_records_warnings` | All assemblers raise | `result.warnings` contains error info |
| 11 | `test_run_pipeline_dry_run_true_no_files_written` | `dry_run=True` | `output_dir` is empty or unchanged |
| 12 | `test_run_pipeline_dry_run_true_returns_success` | `dry_run=True` | `result.success is True` |
| 13 | `test_run_pipeline_dry_run_true_warnings_contain_dry_run_note` | `dry_run=True` | "dry run" or "Dry run" in warnings |
| 14 | `test_run_pipeline_creates_template_engine` | Mock `TemplateEngine` | `TemplateEngine` instantiated with `src_dir` and `config` |
| 15 | `test_run_pipeline_passes_config_to_assemblers` | Mock assemblers | Each `assemble()` receives the `config` |
| 16 | `test_run_pipeline_accumulates_warnings_from_assemblers` | Assemblers return warnings | `result.warnings` includes all assembler warnings |

---

## 3. Integration Tests

### 3.1 `test_pipeline_integration.py` -- Full Pipeline with Real Assemblers

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_pipeline_full_run_creates_rules_dir` | Run with valid config, real assemblers, real `src/` | `.claude/rules/` exists in output |
| 2 | `test_pipeline_full_run_creates_skills_dir` | Full run | `.claude/skills/` exists in output |
| 3 | `test_pipeline_full_run_creates_agents_dir` | Full run | `.claude/agents/` exists in output |
| 4 | `test_pipeline_full_run_creates_hooks_json` | Full run | `hooks.json` exists in output |
| 5 | `test_pipeline_full_run_creates_settings_json` | Full run | `settings.json` exists in output |
| 6 | `test_pipeline_full_run_creates_readme` | Full run | `README.md` exists in output |
| 7 | `test_pipeline_full_run_returns_success_true` | Full run | `result.success is True` |
| 8 | `test_pipeline_full_run_files_generated_non_empty` | Full run | `len(result.files_generated) > 0` |
| 9 | `test_pipeline_full_run_duration_positive` | Full run | `result.duration_ms > 0` |
| 10 | `test_pipeline_dry_run_no_output_files` | `dry_run=True`, real config | No files created in `output_dir` |
| 11 | `test_pipeline_dry_run_returns_success` | `dry_run=True` | `result.success is True` |
| 12 | `test_pipeline_atomic_cleanup_on_failure` | Monkeypatch one assembler to fail | Output dir clean, no partial files |

### 3.2 Interactive Mode Integration

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_interactive_mode_with_simulated_inputs_exits_zero` | CliRunner with `input=` simulating all prompts via `generate --interactive` | Exit code 0 |
| 2 | `test_interactive_mode_produces_valid_config` | Simulated inputs for java-quarkus equivalent | Output matches config-file based generation |

---

## 4. CLI Tests (Click CliRunner)

### 4.1 `test_cli_generate.py` -- `generate` Command

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_generate_with_config_valid_exits_zero` | `generate --config <valid.yaml>` | Exit code 0, success message |
| 2 | `test_generate_with_config_loads_project_name` | `generate --config <valid.yaml>` | Output contains project name |
| 3 | `test_generate_interactive_exits_zero` | `generate --interactive` with simulated input | Exit code 0 |
| 4 | `test_generate_no_config_no_interactive_exits_nonzero` | `generate` (no flags) | Exit code != 0, usage error |
| 5 | `test_generate_both_config_and_interactive_exits_nonzero` | `generate --config x --interactive` | Exit code != 0, usage error |
| 6 | `test_generate_dry_run_no_files_created` | `generate --config <valid.yaml> --dry-run` with `isolated_filesystem` | No output files, exit code 0 |
| 7 | `test_generate_dry_run_shows_plan` | `generate --config <valid.yaml> --dry-run` | Output mentions "dry run" or lists planned files |
| 8 | `test_generate_verbose_exits_zero` | `generate --config <valid.yaml> --verbose` | Exit code 0 |
| 9 | `test_generate_verbose_shows_debug_output` | `generate --config <valid.yaml> --verbose` | Additional debug-level info in output |
| 10 | `test_generate_output_dir_custom_creates_there` | `generate --config <valid.yaml> --output-dir ./custom` | Files appear in `./custom` |
| 11 | `test_generate_src_dir_custom_uses_provided` | `generate --config <valid.yaml> --src-dir <path>` | Pipeline uses the provided `src_dir` |
| 12 | `test_generate_nonexistent_config_exits_nonzero` | `generate --config /no/such/file.yaml` | Exit code 2 (Click path validation) |
| 13 | `test_generate_invalid_yaml_exits_one` | `generate --config <malformed.yaml>` | Exit code 1 with error message |
| 14 | `test_generate_help_shows_options` | `generate --help` | Output lists `--config`, `--interactive`, `--dry-run`, `--verbose`, `--output-dir`, `--src-dir` |

### 4.2 `test_cli_validate.py` -- `validate` Command

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_validate_valid_config_exits_zero` | `validate --config <valid.yaml>` | Exit code 0, "valid" message |
| 2 | `test_validate_invalid_config_exits_one` | `validate --config <invalid.yaml>` | Exit code 1, error details |
| 3 | `test_validate_missing_required_fields_shows_errors` | `validate --config <missing_language.yaml>` | Output lists missing fields |
| 4 | `test_validate_nonexistent_file_exits_two` | `validate --config /no/file.yaml` | Exit code 2 |
| 5 | `test_validate_no_files_generated` | `validate --config <valid.yaml>` with `isolated_filesystem` | No files created |
| 6 | `test_validate_verbose_exits_zero` | `validate --config <valid.yaml> --verbose` | Exit code 0 |
| 7 | `test_validate_help_shows_options` | `validate --help` | Output lists `--config`, `--verbose` |

### 4.3 `test_cli.py` -- Updated Top-Level CLI Tests

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_help_returns_zero` | `--help` | Exit code 0, shows usage |
| 2 | `test_version_returns_version` | `--version` | Exit code 0, shows version |
| 3 | `test_no_args_shows_help` | No args | Exit code 0, shows help |
| 4 | `test_help_lists_generate_command` | `--help` | Output contains "generate" |
| 5 | `test_help_lists_validate_command` | `--help` | Output contains "validate" |

---

## 5. Error Path Tests

| # | Test Name | File | Scenario | Expected |
|---|-----------|------|----------|----------|
| 1 | `test_generate_invalid_config_path_exits_nonzero` | `test_cli_generate.py` | `--config` points to nonexistent file | Exit code 2 |
| 2 | `test_run_pipeline_assembler_exception_cleans_up` | `test_pipeline.py` | Assembler raises `RuntimeError` mid-pipeline | Temp dir cleaned, `success=False` |
| 3 | `test_run_pipeline_assembler_exception_records_error_in_warnings` | `test_pipeline.py` | Assembler raises | Error message in `result.warnings` |
| 4 | `test_atomic_output_permission_error_on_dest_raises` | `test_utils.py` | `dest_dir` is read-only (monkeypatch `shutil.copytree` to raise `PermissionError`) | `PermissionError` propagates |
| 5 | `test_generate_malformed_yaml_exits_one` | `test_cli_generate.py` | Config file with invalid YAML syntax | Exit code 1, error message |
| 6 | `test_generate_yaml_missing_required_fields_exits_one` | `test_cli_generate.py` | Valid YAML but missing `project` section | Exit code 1, mentions missing field |
| 7 | `test_run_pipeline_template_engine_init_failure` | `test_pipeline.py` | `TemplateEngine.__init__` raises (monkeypatch) | `result.success is False`, temp cleaned |
| 8 | `test_atomic_output_keyboard_interrupt_cleans_up` | `test_utils.py` | `KeyboardInterrupt` raised inside context | Temp dir removed |

---

## 6. Boundary Tests

| # | Test Name | File | Scenario | Expected |
|---|-----------|------|----------|----------|
| 1 | `test_pipeline_result_empty_config_minimal_fields` | `test_pipeline_result.py` | Minimal `ProjectConfig` (only required fields) | `PipelineResult` created successfully |
| 2 | `test_run_pipeline_minimal_config_succeeds` | `test_pipeline.py` | `ProjectConfig` with only required fields | `result.success is True` |
| 3 | `test_atomic_output_very_long_dest_path` | `test_utils.py` | `dest_dir` with 200+ char path | Works or raises OS-level error cleanly |
| 4 | `test_pipeline_result_large_files_list` | `test_pipeline_result.py` | `files_generated` with 1000 entries | Stored correctly |
| 5 | `test_generate_config_with_all_optional_defaults` | `test_cli_generate.py` | Config with no optional sections (data, infra, security, testing) | Exit code 0, defaults applied |
| 6 | `test_run_pipeline_empty_src_dir` | `test_pipeline.py` | `src_dir` exists but is empty | Assemblers handle gracefully or fail with clear error |

---

## 7. Parametrized / Contract Tests

### 7.1 CLI Option Combinations

```python
@pytest.mark.parametrize(
    "args, expected_exit_code",
    [
        (["generate", "--config", "<valid>"], 0),
        (["generate", "--config", "<valid>", "--dry-run"], 0),
        (["generate", "--config", "<valid>", "--verbose"], 0),
        (["generate", "--config", "<valid>", "--dry-run", "--verbose"], 0),
        (["generate"], non_zero),
        (["validate", "--config", "<valid>"], 0),
        (["validate", "--config", "<invalid>"], 1),
    ],
)
def test_cli_option_combinations_exit_code(args, expected_exit_code):
    ...
```

### 7.2 `setup_logging` Levels

```python
@pytest.mark.parametrize(
    "verbose, expected_level",
    [
        (True, logging.DEBUG),
        (False, logging.INFO),
    ],
)
def test_setup_logging_level(verbose, expected_level):
    ...
```

---

## 8. Fixtures Required

### 8.1 New Fixtures (in `conftest.py` or `tests/fixtures/`)

| Fixture | Type | Description |
|---------|------|-------------|
| `tmp_output_dir` | `pytest.fixture` | Creates a temporary directory for pipeline output, cleaned up after test |
| `valid_config` | `pytest.fixture` | Returns a `ProjectConfig` built from `MINIMAL_PROJECT_DICT` |
| `full_config` | `pytest.fixture` | Returns a `ProjectConfig` built from `FULL_PROJECT_DICT` |
| `src_dir` | `pytest.fixture` | Returns the real `src/` directory path for integration tests |
| `mock_assemblers` | `pytest.fixture` | Returns dict of mocked assembler instances (all 8) |
| `malformed_yaml_path` | `pytest.fixture` | Creates a temp file with invalid YAML content |
| `incomplete_yaml_path` | `pytest.fixture` | Creates a temp file with valid YAML but missing required sections |

### 8.2 Existing Fixtures (reused from `tests/conftest.py`)

- `full_project_dict` -- deep copy of `FULL_PROJECT_DICT`
- `minimal_project_dict` -- deep copy of `MINIMAL_PROJECT_DICT`
- `create_project_config` -- factory fixture
- `valid_v3_path` -- path to valid V3 YAML fixture
- `missing_language_path` -- path to YAML missing `language` section

---

## 9. Mocking Strategy

| Component | Mock? | Rationale |
|-----------|-------|-----------|
| Assemblers (in unit tests) | YES | Isolate pipeline orchestration from assembler logic |
| `TemplateEngine` (in unit tests) | YES | Isolate pipeline from template rendering |
| `shutil.copytree` / `shutil.rmtree` | YES (error paths only) | Simulate filesystem errors |
| `load_config` (in CLI tests) | OPTIONAL | Can use real config files from `tests/fixtures/` |
| `run_interactive` (in CLI tests) | YES | Avoid actual terminal interaction |
| `atomic_output` (in pipeline unit tests) | NO | Test as real context manager; mock only for error simulation |
| Domain models (`ProjectConfig`, `PipelineResult`) | NEVER | Real domain objects per testing philosophy |

---

## 10. Coverage Estimation

| File | Lines (est.) | Line Coverage Target | Branch Coverage Target | Key Branches |
|------|-------------|---------------------|----------------------|--------------|
| `claude_setup/utils.py` | ~40 | 98% | 95% | `atomic_output`: success/failure; `find_src_dir`: exists/not-exists; `setup_logging`: verbose/not |
| `claude_setup/models.py` (PipelineResult) | ~10 | 100% | 100% | No branches (dataclass) |
| `claude_setup/exceptions.py` (PipelineError) | ~5 | 100% | 100% | No branches |
| `claude_setup/assembler/__init__.py` (run_pipeline) | ~60 | 96% | 92% | dry_run branch; per-assembler try/except; success/failure path; atomic_output success/failure |
| `claude_setup/__main__.py` | ~60 | 96% | 92% | generate: config vs interactive vs neither vs both; validate: valid/invalid; verbose flag; dry-run flag |

**Aggregate estimate:** ~175 new/modified lines, target 96% line coverage, 92% branch coverage.

---

## 11. Test Execution Order

Tests are independent and can run in any order. No shared mutable state between tests.

**Recommended development order (inner-to-outer):**

1. `tests/unit/test_pipeline_result.py` -- PipelineResult dataclass (simplest, no deps)
2. `tests/unit/test_utils.py` -- atomic_output, setup_logging, find_src_dir
3. `tests/unit/test_pipeline.py` -- run_pipeline with mocked assemblers
4. `tests/cli/test_cli_validate.py` -- validate command
5. `tests/cli/test_cli_generate.py` -- generate command
6. `tests/test_cli.py` -- update top-level CLI tests
7. `tests/integration/test_pipeline_integration.py` -- full pipeline integration
8. `tests/test_cli_init.py` -- update or remove (backward compat tests)

---

## 12. Test Count Summary

| Category | Count |
|----------|-------|
| Unit: `test_utils.py` | 16 |
| Unit: `test_pipeline_result.py` | 7 |
| Unit: `test_pipeline.py` | 16 |
| CLI: `test_cli_generate.py` | 14 |
| CLI: `test_cli_validate.py` | 7 |
| CLI: `test_cli.py` (updated) | 5 |
| Error paths (distributed) | 8 |
| Boundary (distributed) | 6 |
| Integration: `test_pipeline_integration.py` | 14 |
| **Total** | **~93** |

Note: Some error/boundary tests overlap with unit/CLI tests above (counted once in their primary category, listed separately for traceability).

**Unique test functions (deduplicated):** ~70-75

---

## 13. Acceptance Criteria Traceability

| Gherkin Scenario | Test(s) Covering It |
|-----------------|---------------------|
| Full generation with config file | `test_generate_with_config_valid_exits_zero`, `test_pipeline_full_run_*` |
| Interactive generation | `test_generate_interactive_exits_zero`, `test_interactive_mode_*` |
| Atomic output on failure | `test_atomic_output_failure_*`, `test_run_pipeline_partial_failure_cleans_temp`, `test_pipeline_atomic_cleanup_on_failure` |
| Dry run | `test_generate_dry_run_*`, `test_run_pipeline_dry_run_*`, `test_pipeline_dry_run_no_output_files` |
| Validate only | `test_validate_valid_config_exits_zero`, `test_validate_invalid_config_exits_one`, `test_validate_no_files_generated` |

---

## 14. Anti-Patterns to Avoid

- Do NOT mock domain models (`ProjectConfig`, `PipelineResult`) -- use real objects
- Do NOT use `time.sleep()` -- no async resources in this story
- Do NOT depend on test execution order
- Do NOT use `unittest.TestCase` -- use pytest functions/classes
- Do NOT share mutable state between tests -- each test gets fresh fixtures
- Do NOT use `assert True/False` without context -- use specific assertions
- Do NOT use wildcard imports in test files
