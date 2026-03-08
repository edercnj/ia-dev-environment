# Implementation Plan -- STORY-002: GithubMcpAssembler (copilot-mcp.json)

**Status:** PLANNED
**Date:** 2026-03-08

---

## 1. Affected Layers and Components

| Layer | Component | Impact |
|-------|-----------|--------|
| models | `ProjectConfig`, new `McpServerConfig`, `McpConfig` | New dataclasses and optional `mcp` field on `ProjectConfig` |
| assembler | `GithubMcpAssembler` (new) | New assembler generating `github/copilot-mcp.json` |
| assembler | `__init__.py` (pipeline orchestration) | Modified to register 10th assembler |
| config | `config.py` (`validate_config`, `load_config`) | No changes needed -- `mcp` is optional, `ProjectConfig.from_dict` handles it |
| CLI | `__main__.py` (`_classify_files`) | No changes needed -- files under `github/` already classified as "GitHub" |
| tests | `test_pipeline.py` | Updated assembler count from 9 to 10 |
| tests | `test_github_mcp_assembler.py` (new) | Unit tests for the new assembler |
| tests | `test_byte_for_byte.py` | Validates generated output against golden files |
| golden files | `tests/golden/java-quarkus/github/copilot-mcp.json` | New golden file for byte-for-byte validation |
| fixtures | `tests/fixtures/` | New or updated config YAML with `mcp` section |

## 2. New Classes/Interfaces Created

| Class / Module | Location | Responsibility |
|----------------|----------|----------------|
| `McpServerConfig` | `src/claude_setup/models.py` | Dataclass representing a single MCP server: `id`, `url`, `capabilities`, `env` |
| `McpConfig` | `src/claude_setup/models.py` | Dataclass wrapping a `List[McpServerConfig]` with `from_dict()` factory |
| `GithubMcpAssembler` | `src/claude_setup/assembler/github_mcp_assembler.py` | Generates `copilot-mcp.json` programmatically from `ProjectConfig.mcp` |
| `_build_copilot_mcp_dict()` | Same file (module-level function) | Builds the `{"mcpServers": {...}}` dict from `McpConfig` |

**Golden files (new):**

| File | Location |
|------|----------|
| `copilot-mcp.json` | `tests/golden/java-quarkus/github/copilot-mcp.json` |

### 2.1 McpServerConfig Dataclass

```python
@dataclass
class McpServerConfig:
    id: str                              # e.g. "firecrawl-mcp"
    url: str                             # e.g. "https://mcp.example.com/sse"
    capabilities: List[str] = field(default_factory=list)  # e.g. ["scrape", "crawl"]
    env: Dict[str, str] = field(default_factory=dict)      # e.g. {"API_KEY": "$FIRECRAWL_API_KEY"}

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> McpServerConfig:
        return cls(
            id=_require(data, "id", "McpServerConfig"),
            url=_require(data, "url", "McpServerConfig"),
            capabilities=data.get("capabilities", []),
            env=data.get("env", {}),
        )
```

### 2.2 McpConfig Dataclass

```python
@dataclass
class McpConfig:
    servers: List[McpServerConfig] = field(default_factory=list)

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> McpConfig:
        servers_raw = data.get("servers", [])
        return cls(
            servers=[McpServerConfig.from_dict(s) for s in servers_raw],
        )
```

### 2.3 Copilot MCP JSON Output Format

```json
{
  "mcpServers": {
    "firecrawl-mcp": {
      "url": "https://mcp.example.com/sse",
      "env": {
        "FIRECRAWL_API_KEY": "$FIRECRAWL_API_KEY"
      }
    }
  }
}
```

Note: `capabilities` from `McpServerConfig` is project metadata only -- the Copilot `copilot-mcp.json` format does not include a capabilities field per server. Capabilities are retained in `ProjectConfig` for parity tracking with `settings.json` but are not emitted in the JSON output.

### 2.4 GithubMcpAssembler Skeleton

```python
class GithubMcpAssembler:
    """Generates .github/copilot-mcp.json from ProjectConfig MCP data."""

    def assemble(
        self,
        config: ProjectConfig,
        output_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        if not config.mcp.servers:
            return []
        github_dir = output_dir / "github"
        github_dir.mkdir(parents=True, exist_ok=True)
        dest = github_dir / "copilot-mcp.json"
        mcp_dict = _build_copilot_mcp_dict(config.mcp)
        content = json.dumps(mcp_dict, indent=2) + "\n"
        dest.write_text(content, encoding="utf-8")
        return [dest]
```

Key design decisions:
- No `resources_dir` constructor parameter needed (programmatic generation, no templates)
- Returns empty list when `config.mcp.servers` is empty (no file generated)
- Uses `json.dumps` with `indent=2` + trailing newline (matches `SettingsAssembler` convention)

## 3. Existing Classes Modified

| Class / Module | Location | Change Description |
|----------------|----------|--------------------|
| `ProjectConfig` | `src/claude_setup/models.py` | Add optional `mcp: McpConfig` field with `default_factory=McpConfig` |
| `ProjectConfig.from_dict()` | `src/claude_setup/models.py` | Add `mcp=McpConfig.from_dict(data.get("mcp", {}))` |
| `_build_assemblers()` | `src/claude_setup/assembler/__init__.py` | Add `GithubMcpAssembler` as 10th entry |
| `__all__` | `src/claude_setup/assembler/__init__.py` | Add `GithubMcpAssembler` to exports |
| Import block | `src/claude_setup/assembler/__init__.py` | Add import of `GithubMcpAssembler` |
| `_execute_assemblers()` | `src/claude_setup/assembler/__init__.py` | No change -- `GithubMcpAssembler` follows the standard `assemble(config, output_dir, engine)` signature (no `resources_dir` needed) |
| `TestBuildAssemblers` | `tests/test_pipeline.py` | Update assembler count from 9 to 10; add ordering assertion |

### 3.1 ProjectConfig Change

Add after the `testing` field:

```python
mcp: McpConfig = field(
    default_factory=McpConfig,
)
```

Add in `from_dict()`:

```python
mcp=McpConfig.from_dict(
    data.get("mcp", {}),
),
```

This is fully backward-compatible: existing configs without `mcp` get an empty `McpConfig(servers=[])`.

### 3.2 Pipeline Registration

In `_build_assemblers()`, append:

```python
("GithubMcpAssembler", GithubMcpAssembler()),
```

Note: Unlike most assemblers, `GithubMcpAssembler` takes no constructor arguments (no `resources_dir`), similar in simplicity to `RulesAssembler`.

### 3.3 _execute_assemblers Dispatch

`GithubMcpAssembler` does NOT need `resources_dir` since it generates JSON programmatically. It follows the standard 3-arg signature `assemble(config, output_dir, engine)`, so no special-case dispatch is needed in `_execute_assemblers()`.

## 4. Dependency Direction Validation

```
GithubMcpAssembler
    imports: ProjectConfig, McpConfig (models -- domain layer)
    imports: TemplateEngine (engine -- unused but part of interface contract)
    imports: Path, List (stdlib)
    imports: json (stdlib)
    imports: logging (stdlib)
```

**Assessment: COMPLIANT.** The assembler depends only on domain models (`ProjectConfig`, `McpConfig`, `McpServerConfig`). It does not import any other assembler, framework code, or adapter. Dependencies point inward toward the domain.

```
McpServerConfig, McpConfig
    imports: dataclasses (stdlib)
    imports: typing (stdlib)
```

**Assessment: COMPLIANT.** Pure domain value objects with zero external dependencies.

```
Dependency graph:
  assembler/__init__.py → github_mcp_assembler.py → models.py
                                                      ↑
  config.py ────────────────────────────────────────────┘
```

No circular dependencies. No cross-assembler imports.

## 5. Integration Points

| Integration Point | Direction | Description |
|--------------------|-----------|-------------|
| Pipeline orchestration | Inbound | `_execute_assemblers()` calls `assemble()` on `GithubMcpAssembler` as the 10th step |
| `ProjectConfig.mcp` | Outbound (dependency) | Provides MCP server data: id, url, capabilities, env |
| File system | Outbound | Writes to `output_dir/github/copilot-mcp.json` |
| CLI classification | Downstream | `_classify_files()` counts files with `"github"` in path parts under "GitHub" category |
| Parity with SettingsAssembler | Conceptual only | Both consume `ProjectConfig.mcp` but there is NO code dependency between them |

### Data Flow

1. `claude-setup.yaml` is parsed; `mcp` section is deserialized into `McpConfig` within `ProjectConfig`
2. Pipeline calls `GithubMcpAssembler.assemble(config, output_dir, engine)`
3. Assembler checks `config.mcp.servers` -- returns empty list if none
4. For non-empty servers: builds `{"mcpServers": {id: {url, env}}}` dict
5. Writes JSON to `output_dir/github/copilot-mcp.json`
6. Returns list containing the generated `Path`

### Config YAML Example

```yaml
mcp:
  servers:
    - id: firecrawl-mcp
      url: "https://mcp.firecrawl.dev/sse"
      capabilities:
        - scrape
        - crawl
        - search
      env:
        FIRECRAWL_API_KEY: "$FIRECRAWL_API_KEY"
    - id: github-mcp
      url: "https://mcp.github.com/sse"
      capabilities:
        - issues
        - pull-requests
      env:
        GITHUB_TOKEN: "$GITHUB_TOKEN"
```

## 6. Configuration Changes

| Change | Location | Description |
|--------|----------|-------------|
| `McpServerConfig` dataclass | `src/claude_setup/models.py` | New domain value object |
| `McpConfig` dataclass | `src/claude_setup/models.py` | New domain value object wrapping server list |
| `ProjectConfig.mcp` field | `src/claude_setup/models.py` | New optional field (defaults to empty) |
| Assembler registration | `src/claude_setup/assembler/__init__.py` | Position 10 in `_build_assemblers()` |

No changes to `config.py` validation are needed because `mcp` is optional and `validate_config()` only checks `REQUIRED_SECTIONS`.

### Config YAML Schema Addition

```yaml
# Optional -- omit entirely if no MCP servers
mcp:
  servers:                    # List of MCP server configurations
    - id: string              # Required. Lowercase-hyphen identifier
      url: string             # Required. MCP server endpoint URL
      capabilities: [string]  # Optional. Capabilities offered
      env:                    # Optional. Environment variable references
        VAR_NAME: "$VAR_NAME" # Key=var name, Value="$VAR_NAME" (reference, not secret)
```

## 7. Risk Assessment

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| Empty `mcp` section generates no file | Low | Expected | `assemble()` returns `[]` when no servers; pipeline handles gracefully |
| Env values contain hardcoded secrets | High | Low | Convention enforces `$VAR_NAME` references; unit test validates no literal values without `$` prefix |
| Copilot MCP JSON schema changes | Medium | Low | Programmatic generation makes format changes easy; single function to update |
| `ProjectConfig` backward compatibility | Low | Low | `mcp` defaults to `McpConfig()` (empty servers list); existing configs unaffected |
| Golden file drift when MCP servers change | Medium | Medium | `test_byte_for_byte.py` catches immediately; golden files regenerated on model change |
| Pipeline ordering -- GithubMcpAssembler before GithubInstructionsAssembler | Low | Low | Both are independent; ordering does not matter but convention places MCP at position 10 (after instructions at 9) |
| `capabilities` field unused in output JSON | Low | Low | Retained in model for documentation/parity; explicitly excluded from JSON output with code comment |
| Constructor inconsistency (no `resources_dir`) | Low | Low | `GithubMcpAssembler()` takes no args like `RulesAssembler`; `_execute_assemblers` dispatch already handles both signatures |

## 8. Implementation Order

1. **models.py** -- Add `McpServerConfig`, `McpConfig`, and `ProjectConfig.mcp` field
2. **github_mcp_assembler.py** -- Create assembler with `assemble()` and `_build_copilot_mcp_dict()`
3. **assembler/__init__.py** -- Register as 10th assembler, update `__all__` and imports
4. **tests/fixtures/** -- Add or update a config YAML fixture with `mcp` section
5. **tests/golden/** -- Add `github/copilot-mcp.json` golden file
6. **test_github_mcp_assembler.py** -- Unit tests for assembler
7. **test_pipeline.py** -- Update assembler count and ordering assertions
8. **test_byte_for_byte.py** -- Verify golden file match (may need fixture update)

## 9. Test Strategy Summary

| Test Type | What | File |
|-----------|------|------|
| Unit | `McpServerConfig.from_dict()` with valid/missing fields | `test_models.py` or `test_github_mcp_assembler.py` |
| Unit | `McpConfig.from_dict()` with empty/populated servers | Same |
| Unit | `GithubMcpAssembler.assemble()` with servers -- generates valid JSON | `test_github_mcp_assembler.py` |
| Unit | `GithubMcpAssembler.assemble()` with no servers -- returns empty list | Same |
| Unit | `_build_copilot_mcp_dict()` output structure validation | Same |
| Unit | No hardcoded secrets in env values (all start with `$`) | Same |
| Unit | JSON output is parseable | Same |
| Contract | Byte-for-byte golden file match | `test_byte_for_byte.py` |
| Integration | Pipeline with 10 assemblers, correct ordering | `test_pipeline.py` |

## 10. Files Inventory (Complete)

### New Files

| File | Type |
|------|------|
| `src/claude_setup/assembler/github_mcp_assembler.py` | Production code |
| `tests/test_github_mcp_assembler.py` | Unit tests |
| `tests/golden/java-quarkus/github/copilot-mcp.json` | Golden file |

### Modified Files

| File | Change |
|------|--------|
| `src/claude_setup/models.py` | Add `McpServerConfig`, `McpConfig`, `ProjectConfig.mcp` |
| `src/claude_setup/assembler/__init__.py` | Register 10th assembler |
| `tests/test_pipeline.py` | Update count and ordering |
| `tests/fixtures/valid_v3_config.yaml` (or new fixture) | Add `mcp` section for testing |
