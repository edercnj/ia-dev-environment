# Test Plan -- STORY-002: GithubMcpAssembler (copilot-mcp.json)

**Status:** DOCUMENTED
**Date:** 2026-03-08
**Assembler Under Test:** `GithubMcpAssembler`
**Source:** `src/ia_dev_env/assembler/github_mcp_assembler.py`

---

## 1. Unit Test Scenarios

### 1.1 `McpServerConfig.from_dict()` -- Model Deserialization

| # | Scenario | Input | Expected Output | Acceptance Criteria |
|---|----------|-------|-----------------|---------------------|
| U-01 | Full data produces valid config | `{"id": "firecrawl-mcp", "url": "https://mcp.firecrawl.dev/sse", "capabilities": ["scrape"], "env": {"API_KEY": "$API_KEY"}}` | `McpServerConfig` with all fields populated | All attributes match input |
| U-02 | Minimal data (no env, no capabilities) | `{"id": "simple-mcp", "url": "https://mcp.example.com/sse"}` | `McpServerConfig` with `env={}`, `capabilities=[]` | Defaults applied correctly |
| U-03 | Missing `id` raises KeyError | `{"url": "https://example.com"}` | `KeyError` with message containing `'id'` | Required field enforcement |
| U-04 | Missing `url` raises KeyError | `{"id": "test"}` | `KeyError` with message containing `'url'` | Required field enforcement |
| U-05 | Empty env dict preserved | `{"id": "x", "url": "http://x", "env": {}}` | `McpServerConfig` with `env == {}` | Empty dict not converted to None |
| U-06 | Empty capabilities list preserved | `{"id": "x", "url": "http://x", "capabilities": []}` | `McpServerConfig` with `capabilities == []` | Empty list not converted to None |

### 1.2 `McpConfig.from_dict()` -- Config Wrapper

| # | Scenario | Input | Expected Output | Acceptance Criteria |
|---|----------|-------|-----------------|---------------------|
| U-07 | Populated servers list | `{"servers": [{"id": "a", "url": "http://a"}, {"id": "b", "url": "http://b"}]}` | `McpConfig` with 2 `McpServerConfig` entries | `len(config.servers) == 2` |
| U-08 | Empty servers list | `{"servers": []}` | `McpConfig` with `servers == []` | Empty list handled |
| U-09 | Missing servers key defaults to empty | `{}` | `McpConfig` with `servers == []` | Default factory applied |
| U-10 | Default constructor produces empty servers | `McpConfig()` | `servers == []` | `default_factory=list` works |

### 1.3 `ProjectConfig.mcp` -- Backward Compatibility

| # | Scenario | Input | Expected Output | Acceptance Criteria |
|---|----------|-------|-----------------|---------------------|
| U-11 | Config without `mcp` section | `MINIMAL_PROJECT_DICT` (no `mcp` key) | `config.mcp.servers == []` | Backward compatible; no error |
| U-12 | Config with `mcp` section populated | Dict with `mcp: {servers: [...]}` | `config.mcp.servers` contains deserialized servers | Forward compatible |

### 1.4 `_build_copilot_mcp_dict()` -- JSON Structure Builder

| # | Scenario | Input | Expected Output | Acceptance Criteria |
|---|----------|-------|-----------------|---------------------|
| U-13 | Single server produces correct structure | 1 server: `id="firecrawl-mcp"`, `url="https://mcp.firecrawl.dev/sse"`, `env={"FIRECRAWL_API_KEY": "$FIRECRAWL_API_KEY"}` | `{"mcpServers": {"firecrawl-mcp": {"url": "https://mcp.firecrawl.dev/sse", "env": {"FIRECRAWL_API_KEY": "$FIRECRAWL_API_KEY"}}}}` | Dict structure matches Copilot format |
| U-14 | Multiple servers all present | 3 servers with distinct ids | `mcpServers` dict contains all 3 keys | No servers lost |
| U-15 | Server id used as JSON key | `id="my-server"` | `result["mcpServers"]["my-server"]` exists | Id maps to dict key |
| U-16 | Capabilities included in JSON output when present | Server with `capabilities=["scrape"]` | Output dict contains `"capabilities": ["scrape"]` for that server | Capabilities propagated to Copilot config |
| U-17 | Empty env dict omitted from output | Server with `env={}` | Output dict for that server has no `env` key | Empty env not serialized (field omitted) |
| U-18 | Env values reference variables with $ prefix | `env={"API_KEY": "$API_KEY"}` | All env values start with `$` | Security convention enforced |

### 1.5 `GithubMcpAssembler.assemble()` -- Orchestration

| # | Scenario | Input | Expected Output | Acceptance Criteria |
|---|----------|-------|-----------------|---------------------|
| U-19 | Servers configured -- generates JSON file | Config with 1+ servers | Returns `[Path("github/copilot-mcp.json")]` | File exists, return list has 1 element |
| U-20 | No servers configured -- returns empty list | Config with `mcp.servers == []` | Returns `[]` | No file generated |
| U-21 | Output path is `github/copilot-mcp.json` | Any config with servers | Generated file at `output_dir / "github" / "copilot-mcp.json"` | Path convention |
| U-22 | Creates `github/` directory if missing | `output_dir` with no `github/` subdir | `github/` directory created | `mkdir(parents=True, exist_ok=True)` |
| U-23 | JSON output is parseable | Config with servers | `json.loads(file_content)` succeeds | Valid JSON |
| U-24 | JSON uses 2-space indentation | Any config with servers | Output matches `json.dumps(..., indent=2)` format | Consistent with `SettingsAssembler` |
| U-25 | File ends with trailing newline | Any config with servers | Last char is `\n` | File format convention |
| U-26 | File is UTF-8 encoded | Any config with servers | File readable with `encoding="utf-8"` | Encoding convention |
| U-27 | Idempotent on repeated calls | Same config, same output_dir | Second call produces identical output | No side effects |

---

## 2. Security Validation

### 2.1 No Hardcoded Secrets

| # | Scenario | Validation | Acceptance Criteria |
|---|----------|------------|---------------------|
| S-01 | All env values use `$` prefix convention | Parse generated JSON, iterate env values | Every value in every server's `env` dict starts with `$` |
| S-02 | No literal API keys in output | Scan output for patterns like `sk-`, `ghp_`, `xoxb-` | No matches found |
| S-03 | Input config enforces `$` convention | `McpServerConfig` with `env={"KEY": "literal-secret"}` | Test documents that this value passes through as-is (assembler does not validate; convention is the guard) |

---

## 3. Pipeline Integration Test Scenarios

### 3.1 Pipeline Registration

| # | Scenario | Test File | Validation |
|---|----------|-----------|------------|
| I-01 | Pipeline includes 10 assemblers | `test_pipeline.py::TestBuildAssemblers::test_returns_ten_assemblers` | `len(assemblers) == 10` |
| I-02 | GithubMcpAssembler is 10th (last) | `test_pipeline.py::TestBuildAssemblers::test_last_assembler_is_github_mcp` | `assemblers[-1] name == "GithubMcpAssembler"` |
| I-03 | GithubMcpAssembler follows GithubInstructionsAssembler | `test_pipeline.py::TestBuildAssemblers::test_mcp_after_instructions` | `assemblers[8] == "GithubInstructionsAssembler"`, `assemblers[9] == "GithubMcpAssembler"` |
| I-04 | Full pipeline succeeds with MCP config | `test_byte_for_byte.py` parametrized | `result.success is True` for profiles with MCP configured |
| I-05 | Full pipeline succeeds without MCP config | `test_byte_for_byte.py` parametrized | `result.success is True` for profiles without MCP section |
| I-06 | Pipeline failure wraps in PipelineError | `test_pipeline.py::TestExecuteAssemblers::test_wraps_exception_in_pipeline_error` | `PipelineError` raised with assembler name |

### 3.2 CLI Classification

| # | Scenario | Validation |
|---|----------|------------|
| I-07 | `copilot-mcp.json` classified under "GitHub" category | `_classify_files()` groups files with `github` in path under "GitHub" |

---

## 4. Golden File / Byte-for-Byte Tests

### 4.1 Test Infrastructure

- **Test file:** `tests/test_byte_for_byte.py`
- **Golden directory:** `tests/golden/{profile}/github/`
- **Profiles tested:** 8 profiles (go-gin, java-quarkus, java-spring, kotlin-ktor, python-click-cli, python-fastapi, rust-axum, typescript-nestjs)
- **Regeneration:** `python scripts/generate_golden.py --all`

### 4.2 Golden File Scenarios

| # | Scenario | Test Method | Validation |
|---|----------|-------------|------------|
| G-01 | Generated `copilot-mcp.json` matches golden file byte-for-byte | `test_pipeline_matches_golden_files` | `verify_output()` reports no mismatches |
| G-02 | No missing files vs golden (including `copilot-mcp.json`) | `test_no_missing_files` | `result.missing_files == []` |
| G-03 | No extra files vs golden | `test_no_extra_files` | `result.extra_files == []` |
| G-04 | Pipeline succeeds for all profiles | `test_pipeline_success_for_profile` | `result.success is True` |

### 4.3 Golden Files for MCP

| Golden File | Profile | Path |
|-------------|---------|------|
| `copilot-mcp.json` | java-quarkus | `tests/golden/java-quarkus/github/copilot-mcp.json` |

Only profiles whose config YAML includes an `mcp` section will have this golden file. Profiles without `mcp` should NOT have a `copilot-mcp.json` golden file.

---

## 5. Edge Cases

### 5.1 Empty or Missing Config Values

| # | Scenario | Expected Behavior | Severity |
|---|----------|-------------------|----------|
| E-01 | Config YAML has no `mcp` section | `ProjectConfig.mcp` defaults to `McpConfig(servers=[])` -- assembler returns `[]`, no file generated | Low -- backward compatibility |
| E-02 | `mcp` section present but `servers` key missing | `McpConfig.from_dict({})` produces empty servers list -- no file generated | Low |
| E-03 | `mcp.servers` is empty list | Assembler returns `[]` -- no file generated | Low |
| E-04 | Server with empty `env` dict | `env: {}` serialized as `"env": {}` in JSON output | Low |
| E-05 | Server with no `capabilities` field | `capabilities` defaults to `[]` -- not emitted in JSON (by design) | Low |

### 5.2 Env Value Patterns

| # | Scenario | Expected Behavior | Severity |
|---|----------|-------------------|----------|
| E-06 | Env value is `$VAR_NAME` (correct convention) | Passes through as-is to JSON output | Low |
| E-07 | Env value is literal string without `$` prefix | Passes through as-is (assembler does not validate) -- test documents this behavior | Medium -- convention violation |
| E-08 | Env value is empty string | Serialized as `""` in JSON | Low |
| E-09 | Env key contains special characters | Serialized as-is (JSON handles it) | Low |

### 5.3 File System Edge Cases

| # | Scenario | Expected Behavior | Severity |
|---|----------|-------------------|----------|
| E-10 | Output `github/` directory does not exist | `mkdir(parents=True, exist_ok=True)` creates it | Low |
| E-11 | Output `github/` directory already exists | `exist_ok=True` prevents error; file overwritten | Low |
| E-12 | Read-only output directory | `PermissionError` propagated; pipeline wraps in `PipelineError` | Low |

### 5.4 JSON Serialization Edge Cases

| # | Scenario | Expected Behavior | Severity |
|---|----------|-------------------|----------|
| E-13 | Server URL contains query parameters | URL serialized as-is in JSON | Low |
| E-14 | Server id contains unicode characters | `json.dumps` with default settings handles it | Low |
| E-15 | Multiple servers with duplicate ids | Last one wins (dict key override) -- test documents this behavior | Medium -- potential data loss |

---

## 6. Coverage Assessment

### 6.1 Test Coverage Map

| Area | Covered By | Coverage Level |
|------|-----------|----------------|
| `McpServerConfig.from_dict()` | Unit tests U-01 through U-06 | High -- all branches (full data, minimal data, missing required fields) |
| `McpConfig.from_dict()` | Unit tests U-07 through U-10 | High -- all branches (populated, empty, missing key, default constructor) |
| `ProjectConfig.mcp` backward compat | Unit tests U-11, U-12 | High -- both with and without `mcp` section |
| `_build_copilot_mcp_dict()` | Unit tests U-13 through U-18 | High -- structure, multiple servers, capabilities exclusion, env handling |
| `GithubMcpAssembler.assemble()` | Unit tests U-19 through U-27 | High -- all branches (with servers, without servers, file creation, JSON format) |
| Pipeline registration | Integration tests I-01 through I-06 | High -- count, ordering, error handling |
| Golden file match | Golden file tests G-01 through G-04 | High -- byte-for-byte validation |
| Security (env values) | Security tests S-01 through S-03 | Medium -- convention-based, not runtime-enforced |

### 6.2 Branch Coverage Analysis

| Branch | Test IDs Covering It |
|--------|---------------------|
| `if not config.mcp.servers: return []` (true branch) | U-20, E-01, E-02, E-03 |
| `if not config.mcp.servers: return []` (false branch) | U-19, U-21 through U-27 |
| `data.get("servers", [])` with key present | U-07 |
| `data.get("servers", [])` with key absent | U-09 |
| `data.get("env", {})` with key present | U-01, U-05 |
| `data.get("env", {})` with key absent | U-02 |
| `data.get("capabilities", [])` with key present | U-01, U-06 |
| `data.get("capabilities", [])` with key absent | U-02 |

### 6.3 Coverage Summary

- **Estimated Line Coverage:** >= 95% (all code paths exercised by unit + golden tests)
- **Estimated Branch Coverage:** >= 90% (all conditional branches explicitly tested)
- **Target:** Line >= 95%, Branch >= 90% (per Rule 05)
- **Assessment:** Plan meets coverage thresholds when fully implemented.

---

## 7. Test Execution

### Run unit tests for GithubMcpAssembler
```bash
pytest tests/assembler/test_github_mcp_assembler.py -v
```

### Run pipeline integration tests
```bash
pytest tests/test_pipeline.py -v
```

### Run golden file tests
```bash
pytest tests/test_byte_for_byte.py -v
```

### Run all STORY-002 related tests
```bash
pytest tests/assembler/test_github_mcp_assembler.py tests/test_pipeline.py tests/test_byte_for_byte.py -v
```

### Regenerate golden files after changes
```bash
python scripts/generate_golden.py --all
```

### Run with coverage report
```bash
pytest tests/ --cov=ia_dev_env.assembler.github_mcp_assembler --cov=ia_dev_env.models --cov-report=term-missing
```

---

## 8. Test File Inventory

### New Test Files

| File | Type | Covers |
|------|------|--------|
| `tests/assembler/test_github_mcp_assembler.py` | Unit tests | U-13 through U-27, S-01 through S-03 |
| `tests/test_models.py` (or added to existing) | Unit tests | U-01 through U-12 |

### Modified Test Files

| File | Change |
|------|--------|
| `tests/test_pipeline.py` | Update assembler count from 9 to 10; add ordering assertions for position 10 |
| `tests/test_byte_for_byte.py` | Validates new `copilot-mcp.json` golden file (no code changes needed if golden files regenerated) |
| `tests/conftest.py` | Optionally add `FULL_PROJECT_DICT` variant with `mcp` section |

### New Fixtures / Golden Files

| File | Purpose |
|------|---------|
| `tests/golden/java-quarkus/github/copilot-mcp.json` | Golden file for byte-for-byte validation |
| Config fixture with `mcp` section | Input data for integration tests |
