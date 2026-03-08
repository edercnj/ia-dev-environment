# Test Plan -- STORY-001: Global and Contextual Copilot Instructions

**Status:** DOCUMENTED
**Date:** 2026-03-08
**Assembler Under Test:** `GithubInstructionsAssembler`
**Source:** `src/claude_setup/assembler/github_instructions_assembler.py`

---

## 1. Unit Test Scenarios

### 1.1 `_build_copilot_instructions(config)` -- Global File Generation

| # | Scenario | Input | Expected Output | Acceptance Criteria |
|---|----------|-------|-----------------|---------------------|
| U-01 | Standard config produces valid global file | `ProjectConfig` with java/quarkus/microservice | Markdown with Identity, Technology Stack, Constraints, Contextual Instructions sections | AC: STORY-001 Gherkin scenario 1 |
| U-02 | Project name appears in heading and Identity section | `config.project.name = "my-quarkus-service"` | `# Project Identity -- my-quarkus-service` and `- **Name:** my-quarkus-service` | AC: contains project name |
| U-03 | Stack fields extracted correctly | java 21, quarkus 3.17 | `- **Language:** java 21`, `- **Framework:** quarkus 3.17` | AC: contains stack info |
| U-04 | Interfaces formatted with uppercase for REST/GRPC | `[{type: rest}, {type: grpc}, {type: event-consumer}]` | `REST, GRPC, event-consumer` | Formatting rule in assembler |
| U-05 | Empty interfaces list produces "none" | `interfaces = []` | `- **Interfaces:** none` | Fallback behavior |
| U-06 | Framework version omitted when None | `config.framework.version = None` | `- **Framework:** quarkus` (no trailing space) | Conditional logic |
| U-07 | Boolean fields rendered lowercase | `domain_driven=True, event_driven=False` | `true`, `false` (not `True`, `False`) | Python str() vs lower() |
| U-08 | Technology Stack table capitalizes values | `style=microservice, language=java` | `Microservice`, `Java 21` | `.capitalize()` logic |
| U-09 | Constraints section is static/hardcoded | Any config | Three constraint bullet points present | Static content check |
| U-10 | Contextual Instructions section lists all 4 files | Any config | Four `instructions/*.instructions.md` references | Cross-reference integrity |
| U-11 | Output ends with trailing newline | Any config | Last char is `\n` | File format convention |
| U-12 | No YAML frontmatter present | Any config | File does NOT start with `---` | Copilot global file convention |

### 1.2 `_generate_global(config, github_dir)` -- File Writing

| # | Scenario | Expected Behavior |
|---|----------|-------------------|
| U-13 | Writes `copilot-instructions.md` to `github_dir` | File exists at `github_dir/copilot-instructions.md` |
| U-14 | Returns the Path of the written file | Return value == `github_dir / "copilot-instructions.md"` |
| U-15 | File is UTF-8 encoded | Written with `encoding="utf-8"` |

### 1.3 `_generate_contextual(engine, instructions_dir)` -- Contextual File Generation

| # | Scenario | Expected Behavior |
|---|----------|-------------------|
| U-16 | Generates 4 `.instructions.md` files | `domain.instructions.md`, `coding-standards.instructions.md`, `architecture.instructions.md`, `quality-gates.instructions.md` |
| U-17 | Each file uses `.instructions.md` extension | All generated filenames end with `.instructions.md` |
| U-18 | Template placeholders are replaced | `engine.replace_placeholders()` called for each template content |
| U-19 | Returns list of 4 Paths | Return value length == 4 |
| U-20 | Files written to `instructions_dir` subdirectory | All paths are children of `instructions_dir` |

### 1.4 `assemble(config, output_dir, engine)` -- Orchestration

| # | Scenario | Expected Behavior |
|---|----------|-------------------|
| U-21 | Creates `github/` and `github/instructions/` directories | Both directories exist after call |
| U-22 | Returns list of 5 Paths (1 global + 4 contextual) | `len(result) == 5` |
| U-23 | First element is global file | `result[0]` ends with `copilot-instructions.md` |
| U-24 | Remaining 4 elements are contextual files | `result[1:]` all end with `.instructions.md` |
| U-25 | Idempotent on repeated calls | Second call produces same output without errors |

---

## 2. Integration Test Scenarios

### 2.1 Pipeline Execution with `GithubInstructionsAssembler`

| # | Scenario | Test File | Validation |
|---|----------|-----------|------------|
| I-01 | Pipeline includes 9 assemblers | `test_pipeline.py::TestBuildAssemblers::test_returns_nine_assemblers` | `len(assemblers) == 9` |
| I-02 | GithubInstructionsAssembler is last (9th) | `test_pipeline.py::TestBuildAssemblers::test_last_assembler_is_github_instructions` | `assemblers[-1] name == "GithubInstructionsAssembler"` |
| I-03 | Full pipeline produces github/ output | `test_byte_for_byte.py::TestByteForByte::test_pipeline_success_for_profile` | `result.success is True` for all 8 config profiles |
| I-04 | Pipeline failure on assembler error wraps in PipelineError | `test_pipeline.py::TestExecuteAssemblers::test_wraps_exception_in_pipeline_error` | `PipelineError` raised with assembler name |
| I-05 | Pipeline generates github files for all profiles | `test_byte_for_byte.py` parametrized over 8 profiles | `github/copilot-instructions.md` present in output for each profile |
| I-06 | CLI classifies github files under "GitHub" category | Manual / functional | Files with `github` in path classified correctly by `_classify_files()` |

### 2.2 Template Engine Integration

| # | Scenario | Validation |
|---|----------|------------|
| I-07 | Placeholder `{project_name}` replaced in domain template | Output `domain.instructions.md` contains actual project name, not `{project_name}` |
| I-08 | Placeholder `{language_name}` replaced in coding-standards template | Output contains actual language name |
| I-09 | Placeholder `{coverage_line}` replaced in quality-gates template | Output contains actual coverage threshold value |
| I-10 | Unknown placeholders left as-is | If template contains `{unknown_placeholder}`, it remains in output (visible for debugging) |

---

## 3. Golden File / Byte-for-Byte Tests

### 3.1 Test Infrastructure

- **Test file:** `tests/test_byte_for_byte.py`
- **Golden directory:** `tests/golden/{profile}/github/`
- **Profiles tested:** 8 profiles (go-gin, java-quarkus, java-spring, kotlin-ktor, python-click-cli, python-fastapi, rust-axum, typescript-nestjs)
- **Regeneration:** `python scripts/generate_golden.py --all`

### 3.2 Golden File Scenarios

| # | Scenario | Test Method | Validation |
|---|----------|-------------|------------|
| G-01 | Generated output matches golden files byte-for-byte | `test_pipeline_matches_golden_files` | `verify_output()` reports no mismatches |
| G-02 | No missing files vs golden | `test_no_missing_files` | `result.missing_files == []` |
| G-03 | No extra files vs golden | `test_no_extra_files` | `result.extra_files == []` |
| G-04 | Total generated file count > 0 | `test_total_files_greater_than_zero` | `result.total_files > 0` |
| G-05 | Pipeline succeeds for all profiles | `test_pipeline_success_for_profile` | `result.success is True` |

### 3.3 Golden Files for GitHub Instructions

| Golden File | Profile | Path |
|-------------|---------|------|
| `copilot-instructions.md` | java-quarkus | `tests/golden/java-quarkus/github/copilot-instructions.md` |
| `domain.instructions.md` | java-quarkus | `tests/golden/java-quarkus/github/instructions/domain.instructions.md` |
| `coding-standards.instructions.md` | java-quarkus | `tests/golden/java-quarkus/github/instructions/coding-standards.instructions.md` |
| `architecture.instructions.md` | java-quarkus | `tests/golden/java-quarkus/github/instructions/architecture.instructions.md` |
| `quality-gates.instructions.md` | java-quarkus | `tests/golden/java-quarkus/github/instructions/quality-gates.instructions.md` |

Each of the 8 config profiles should have equivalent golden files under `tests/golden/{profile}/github/`.

---

## 4. Edge Cases

### 4.1 Missing Templates

| # | Scenario | Expected Behavior | Severity |
|---|----------|-------------------|----------|
| E-01 | Templates directory does not exist | `_generate_contextual()` logs warning, returns empty list | Low -- graceful degradation |
| E-02 | Individual template file missing | Loop skips missing template with warning log, generates remaining templates | Low -- partial output |
| E-03 | All 4 template files missing but directory exists | Returns empty list from contextual generation; global file still generated | Low |
| E-04 | `resources_dir` path is invalid | `_generate_contextual()` logs warning on missing templates_dir, returns `[]` | Low |

### 4.2 Empty or Unusual Config Values

| # | Scenario | Expected Behavior |
|---|----------|-------------------|
| E-05 | `config.project.name` is empty string | Heading becomes `# Project Identity -- ` (renders but visually empty) |
| E-06 | `config.interfaces` is empty list | Interfaces field shows `none` |
| E-07 | `config.framework.version` is None | Framework line omits version suffix (no trailing space) |
| E-08 | `config.framework.version` is empty string | Framework line includes empty string (potential trailing space -- verify behavior) |
| E-09 | Interface type is mixed case (e.g., `Rest`) | Only exact match `rest`/`grpc` triggers uppercase; `Rest` passes through as-is |

### 4.3 File System Edge Cases

| # | Scenario | Expected Behavior |
|---|----------|-------------------|
| E-10 | Output directory does not exist | `mkdir(parents=True, exist_ok=True)` creates it |
| E-11 | Output directory already exists with stale files | Existing files overwritten; no cleanup of old files |
| E-12 | Read-only output directory | `PermissionError` propagated (assembler does not catch it; pipeline wraps in `PipelineError`) |
| E-13 | Template file with non-UTF-8 encoding | `UnicodeDecodeError` propagated |

### 4.4 Content Integrity Edge Cases

| # | Scenario | Expected Behavior |
|---|----------|-------------------|
| E-14 | Template contains no placeholders | Content written as-is (no replacement needed) |
| E-15 | Template contains unknown placeholders | `TemplateEngine.replace_placeholders()` leaves them as-is in output |
| E-16 | Template is empty file | Empty `.instructions.md` file generated |

---

## 5. Coverage Assessment

### 5.1 Current Test Coverage

| Area | Covered By | Coverage Level |
|------|-----------|----------------|
| `_build_copilot_instructions()` | Golden file tests (byte-for-byte comparison across 8 profiles) | High -- validates exact output for each config profile |
| `_generate_global()` | Golden file tests + pipeline integration | High -- file presence and content validated |
| `_generate_contextual()` | Golden file tests + pipeline integration | High -- 4 files validated per profile |
| `assemble()` orchestration | Pipeline tests (9 assemblers, ordering) + golden file tests | High |
| Missing templates directory | **NOT directly tested** -- relies on runtime behavior | Gap |
| Missing individual template | **NOT directly tested** | Gap |
| Empty interfaces list | **NOT directly tested** | Gap |
| Framework version None | Depends on whether any profile config omits version | Partial |
| Error propagation | `test_pipeline.py::TestExecuteAssemblers::test_wraps_exception_in_pipeline_error` | Covered |

### 5.2 Identified Gaps

| Gap ID | Description | Risk | Recommendation |
|--------|-------------|------|----------------|
| GAP-01 | No dedicated unit test file for `GithubInstructionsAssembler` | Medium | Create `tests/test_github_instructions_assembler.py` with isolated unit tests for `_build_copilot_instructions()`, `_generate_global()`, `_generate_contextual()` |
| GAP-02 | Missing templates directory path not tested | Low | Add unit test that instantiates assembler with nonexistent `resources_dir` and verifies `_generate_contextual()` returns `[]` and logs warning |
| GAP-03 | Individual missing template not tested | Low | Add unit test with partial template set (e.g., only 2 of 4 templates present) and verify only those 2 are generated |
| GAP-04 | Empty interfaces edge case not tested | Low | Add unit test with `config.interfaces = []` and assert output contains `none` |
| GAP-05 | Framework version None edge case not tested | Low | Add unit test with `config.framework.version = None` and verify no trailing space |
| GAP-06 | File extension convention not explicitly asserted | Low | Add assertion in unit tests that all contextual output filenames match `*.instructions.md` pattern |
| GAP-07 | Cross-reference links between `.github/` and `.claude/` not validated | Medium | Add test that parses generated contextual files and verifies relative path references to `.claude/` are syntactically correct |

### 5.3 Coverage Summary

- **Estimated Line Coverage:** ~90% (via golden file tests exercising the full pipeline path)
- **Estimated Branch Coverage:** ~75% (missing template directory branch and individual missing template branch not exercised)
- **Target:** Line >= 95%, Branch >= 90% (per Rule 05)
- **Action Required:** Close GAP-01 through GAP-03 to meet branch coverage threshold. GAP-01 (dedicated unit test file) is the highest priority item as it would cover most remaining branches.

---

## 6. Test Execution

### Run all tests
```bash
pytest tests/test_byte_for_byte.py tests/test_pipeline.py -v
```

### Run only golden file tests for java-quarkus
```bash
pytest tests/test_byte_for_byte.py -k "java_quarkus" -v
```

### Regenerate golden files after changes
```bash
python scripts/generate_golden.py --all
```

### Run with coverage report
```bash
pytest tests/ --cov=claude_setup.assembler.github_instructions_assembler --cov-report=term-missing
```
