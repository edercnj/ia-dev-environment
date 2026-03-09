# Task Decomposition -- STORY-002: GithubMcpAssembler (copilot-mcp.json)

**Status:** PLANNED
**Date:** 2026-03-08

---

## G1: Domain Model -- `McpServerConfig`, `McpConfig`, `ProjectConfig.mcp`

**Dependencies:** None

Add two new dataclasses to the domain model and wire them into `ProjectConfig`
as an optional field. Backward-compatible: configs without `mcp` get
`McpConfig(servers=[])`.

| Task | Files Affected | Notes |
|------|---------------|-------|
| T1.1 Add `McpServerConfig` dataclass | `src/ia_dev_env/models.py` | Fields: `id` (required), `url` (required), `capabilities` (list, default `[]`), `env` (dict, default `{}`). Uses `_require()` for mandatory fields. |
| T1.2 Add `McpConfig` dataclass | `src/ia_dev_env/models.py` | Field: `servers: List[McpServerConfig]` (default `[]`). `from_dict()` iterates `data.get("servers", [])`. |
| T1.3 Add `mcp` field to `ProjectConfig` | `src/ia_dev_env/models.py` | `mcp: McpConfig = field(default_factory=McpConfig)` after `testing` field. |
| T1.4 Update `ProjectConfig.from_dict()` | `src/ia_dev_env/models.py` | Add `mcp=McpConfig.from_dict(data.get("mcp", {}))` to the constructor call. |

---

## G2: Config Templates -- Add `mcp_servers` Section to 8 YAML Templates

**Dependencies:** G1 (model must define the expected YAML structure)

Add the optional `mcp` section to all 8 config YAML templates so users see
the available configuration. The section is commented or set to empty by default.

| Task | Files Affected | Notes |
|------|---------------|-------|
| T2.1 Add `mcp` section to `setup-config.java-quarkus.yaml` | `resources/config-templates/setup-config.java-quarkus.yaml` | New section 18. Contains example servers (firecrawl-mcp, github-mcp) with `id`, `url`, `capabilities`, `env`. |
| T2.2 Add `mcp` section to `setup-config.java-spring.yaml` | `resources/config-templates/setup-config.java-spring.yaml` | Same structure as T2.1. |
| T2.3 Add `mcp` section to `setup-config.python-fastapi.yaml` | `resources/config-templates/setup-config.python-fastapi.yaml` | Same structure as T2.1. |
| T2.4 Add `mcp` section to `setup-config.python-click-cli.yaml` | `resources/config-templates/setup-config.python-click-cli.yaml` | Same structure as T2.1. |
| T2.5 Add `mcp` section to `setup-config.typescript-nestjs.yaml` | `resources/config-templates/setup-config.typescript-nestjs.yaml` | Same structure as T2.1. |
| T2.6 Add `mcp` section to `setup-config.kotlin-ktor.yaml` | `resources/config-templates/setup-config.kotlin-ktor.yaml` | Same structure as T2.1. |
| T2.7 Add `mcp` section to `setup-config.rust-axum.yaml` | `resources/config-templates/setup-config.rust-axum.yaml` | Same structure as T2.1. |
| T2.8 Add `mcp` section to `setup-config.go-gin.yaml` | `resources/config-templates/setup-config.go-gin.yaml` | Same structure as T2.1. |

### YAML Section Format

```yaml
# ═══════════════════════════════════════════════════
# SECTION 18: MCP SERVERS
# ═══════════════════════════════════════════════════
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

---

## G3: Assembler -- `GithubMcpAssembler`

**Dependencies:** G1 (domain model must expose `config.mcp`)

Create the assembler that reads `ProjectConfig.mcp` and generates
`output_dir/github/copilot-mcp.json`. Programmatic generation (no templates),
similar pattern to `SettingsAssembler`.

| Task | Files Affected | Notes |
|------|---------------|-------|
| T3.1 Create `GithubMcpAssembler` class | `src/ia_dev_env/assembler/github_mcp_assembler.py` | New file. No constructor args (no `resources_dir` needed). |
| T3.2 Implement `assemble()` method | Same file | Signature: `assemble(config, output_dir, engine) -> List[Path]`. Returns `[]` when `config.mcp.servers` is empty. |
| T3.3 Implement `_build_copilot_mcp_dict()` | Same file (module-level function) | Builds `{"mcpServers": {server.id: {"url": server.url, "env": server.env}}}`. `capabilities` excluded from JSON output (Copilot format does not support it). |
| T3.4 Write JSON with `json.dumps(indent=2)` + trailing newline | Same file | Matches `SettingsAssembler` convention for consistent formatting. |

### Key Design Decisions

- No `resources_dir` in constructor -- JSON is built programmatically, not from templates.
- `capabilities` stored in model for documentation/parity but NOT emitted in JSON output
  (Copilot `copilot-mcp.json` schema does not include capabilities per server).
- Empty servers list produces no file (returns `[]`), not an empty JSON object.

---

## G4: Pipeline Integration -- Register 10th Assembler

**Dependencies:** G3 (assembler class must exist)

Register `GithubMcpAssembler` in the pipeline as the 10th assembler.

| Task | Files Affected | Notes |
|------|---------------|-------|
| T4.1 Add import of `GithubMcpAssembler` | `src/ia_dev_env/assembler/__init__.py` | `from ia_dev_env.assembler.github_mcp_assembler import GithubMcpAssembler` |
| T4.2 Add entry to `_build_assemblers()` | `src/ia_dev_env/assembler/__init__.py` | Append as 10th tuple: `("GithubMcpAssembler", GithubMcpAssembler())`. Placed after `GithubInstructionsAssembler`. |
| T4.3 Add `"GithubMcpAssembler"` to `__all__` | `src/ia_dev_env/assembler/__init__.py` | Alphabetical insertion in the exports list. |
| T4.4 Verify `_execute_assemblers()` dispatch | `src/ia_dev_env/assembler/__init__.py` | No change needed. `GithubMcpAssembler` uses the standard 3-arg signature `assemble(config, output_dir, engine)`. Not in `ASSEMBLERS_WITH_RESOURCES_DIR`. |
| T4.5 Verify CLI classification | `src/ia_dev_env/__main__.py` | No change needed. `_classify_files()` already classifies paths containing `"github"` as "GitHub" category. |

---

## G5: Golden Files -- Add `copilot-mcp.json` for All 8 Profiles

**Dependencies:** G2 (config templates with `mcp` section), G3 (assembler must produce correct output)

Generate and commit golden files for byte-for-byte validation. Each profile
gets a `github/copilot-mcp.json` golden file. The content depends on whether
the profile's config includes MCP servers.

| Task | Files Affected | Notes |
|------|---------------|-------|
| T5.1 Create golden file for `java-quarkus` | `tests/golden/java-quarkus/github/copilot-mcp.json` | New file. Generated from java-quarkus config with MCP servers. |
| T5.2 Create golden file for `java-spring` | `tests/golden/java-spring/github/copilot-mcp.json` | New file. |
| T5.3 Create golden file for `python-fastapi` | `tests/golden/python-fastapi/github/copilot-mcp.json` | New file. |
| T5.4 Create golden file for `python-click-cli` | `tests/golden/python-click-cli/github/copilot-mcp.json` | New file. |
| T5.5 Create golden file for `typescript-nestjs` | `tests/golden/typescript-nestjs/github/copilot-mcp.json` | New file. |
| T5.6 Create golden file for `kotlin-ktor` | `tests/golden/kotlin-ktor/github/copilot-mcp.json` | New file. |
| T5.7 Create golden file for `rust-axum` | `tests/golden/rust-axum/github/copilot-mcp.json` | New file. |
| T5.8 Create golden file for `go-gin` | `tests/golden/go-gin/github/copilot-mcp.json` | New file. |

### Golden File Format (Expected)

```json
{
  "mcpServers": {
    "firecrawl-mcp": {
      "url": "https://mcp.firecrawl.dev/sse",
      "env": {
        "FIRECRAWL_API_KEY": "$FIRECRAWL_API_KEY"
      }
    },
    "github-mcp": {
      "url": "https://mcp.github.com/sse",
      "env": {
        "GITHUB_TOKEN": "$GITHUB_TOKEN"
      }
    }
  }
}
```

---

## G6: Tests -- Unit, Pipeline, and Byte-for-Byte

**Dependencies:** G1, G3, G4, G5 (model, assembler, pipeline registration, golden files)

### Unit Tests (new file)

| Task | Files Affected | Notes |
|------|---------------|-------|
| T6.1 Test `McpServerConfig.from_dict()` with valid data | `tests/test_models.py` or `tests/assembler/test_github_mcp_assembler.py` | Required fields: `id`, `url`. Optional: `capabilities`, `env`. |
| T6.2 Test `McpServerConfig.from_dict()` missing required field | Same file | Expects `KeyError` when `id` or `url` missing. |
| T6.3 Test `McpConfig.from_dict()` with empty servers | Same file | Returns `McpConfig(servers=[])`. |
| T6.4 Test `McpConfig.from_dict()` with populated servers | Same file | Returns correct count of `McpServerConfig` instances. |
| T6.5 Test `assemble()` with servers -- generates valid JSON | `tests/assembler/test_github_mcp_assembler.py` | New file. Validates file created at `github/copilot-mcp.json`, content is parseable JSON. |
| T6.6 Test `assemble()` with no servers -- returns empty list | Same file | `config.mcp.servers = []` produces no file, returns `[]`. |
| T6.7 Test `_build_copilot_mcp_dict()` output structure | Same file | Validates `{"mcpServers": {id: {"url": ..., "env": ...}}}` structure. |
| T6.8 Test `capabilities` excluded from JSON output | Same file | Server with capabilities still produces JSON without capabilities field. |
| T6.9 Test no hardcoded secrets in env values | Same file | All env values must start with `$`. |
| T6.10 Test JSON output is parseable | Same file | `json.loads()` succeeds on generated content. |
| T6.11 Test trailing newline in output | Same file | Content ends with `\n`. |

### Pipeline Tests (update existing)

| Task | Files Affected | Notes |
|------|---------------|-------|
| T6.12 Update assembler count from 9 to 10 | `tests/test_pipeline.py` | `test_returns_nine_assemblers` renamed/updated to `test_returns_ten_assemblers`. |
| T6.13 Update last assembler assertion | `tests/test_pipeline.py` | Last assembler is now `GithubMcpAssembler`, not `GithubInstructionsAssembler`. |
| T6.14 Add ordering assertion for position 10 | `tests/test_pipeline.py` | `GithubMcpAssembler` at index 9 (0-based). |

### Byte-for-Byte Tests (verify existing coverage)

| Task | Files Affected | Notes |
|------|---------------|-------|
| T6.15 Verify `test_byte_for_byte.py` picks up new golden files | `tests/test_byte_for_byte.py` | No code change expected -- parametrized test auto-discovers files under `tests/golden/`. Verify `copilot-mcp.json` is included. |

---

## G7: Documentation -- Story Status Update

**Dependencies:** G6 (all tests must pass)

| Task | Files Affected | Notes |
|------|---------------|-------|
| T7.1 Mark STORY-002 as IMPLEMENTED | `docs/stories/github-structure/STORY-002.md` | Update sub-task checkboxes and DoD checkboxes. |
| T7.2 Update plan status | `docs/plans/STORY-002-plan.md` | Change status from PLANNED to IMPLEMENTED. |
| T7.3 Update task decomposition status | `docs/plans/STORY-002-tasks.md` | This file. Change status to IMPLEMENTED. |

---

## Dependency Graph

```
G1 (Domain Model)
 ├─> G2 (Config Templates) -- independent of G3
 └─> G3 (Assembler)
      └─> G4 (Pipeline Integration)
           └─> G5 (Golden Files) ← also depends on G2
                └─> G6 (Tests) ← also depends on G1, G3, G4
                     └─> G7 (Documentation)
```

**Parallelization:** G2 and G3 can run in parallel after G1 completes.

---

## File Summary

| File | Group(s) | Action |
|------|----------|--------|
| `src/ia_dev_env/models.py` | G1 | Modified (add `McpServerConfig`, `McpConfig`, `ProjectConfig.mcp`) |
| `resources/config-templates/setup-config.java-quarkus.yaml` | G2 | Modified (add `mcp` section) |
| `resources/config-templates/setup-config.java-spring.yaml` | G2 | Modified (add `mcp` section) |
| `resources/config-templates/setup-config.python-fastapi.yaml` | G2 | Modified (add `mcp` section) |
| `resources/config-templates/setup-config.python-click-cli.yaml` | G2 | Modified (add `mcp` section) |
| `resources/config-templates/setup-config.typescript-nestjs.yaml` | G2 | Modified (add `mcp` section) |
| `resources/config-templates/setup-config.kotlin-ktor.yaml` | G2 | Modified (add `mcp` section) |
| `resources/config-templates/setup-config.rust-axum.yaml` | G2 | Modified (add `mcp` section) |
| `resources/config-templates/setup-config.go-gin.yaml` | G2 | Modified (add `mcp` section) |
| `src/ia_dev_env/assembler/github_mcp_assembler.py` | G3 | Created |
| `src/ia_dev_env/assembler/__init__.py` | G4 | Modified (import, `_build_assemblers`, `__all__`) |
| `src/ia_dev_env/__main__.py` | G4 | Verified (no change) |
| `tests/golden/java-quarkus/github/copilot-mcp.json` | G5 | Created |
| `tests/golden/java-spring/github/copilot-mcp.json` | G5 | Created |
| `tests/golden/python-fastapi/github/copilot-mcp.json` | G5 | Created |
| `tests/golden/python-click-cli/github/copilot-mcp.json` | G5 | Created |
| `tests/golden/typescript-nestjs/github/copilot-mcp.json` | G5 | Created |
| `tests/golden/kotlin-ktor/github/copilot-mcp.json` | G5 | Created |
| `tests/golden/rust-axum/github/copilot-mcp.json` | G5 | Created |
| `tests/golden/go-gin/github/copilot-mcp.json` | G5 | Created |
| `tests/test_models.py` | G6 | Modified (add `McpServerConfig`/`McpConfig` tests) |
| `tests/assembler/test_github_mcp_assembler.py` | G6 | Created |
| `tests/test_pipeline.py` | G6 | Modified (count 9->10, ordering, last assembler) |
| `tests/test_byte_for_byte.py` | G6 | Verified (auto-discovers new golden files) |
| `docs/stories/github-structure/STORY-002.md` | G7 | Updated |
| `docs/plans/STORY-002-plan.md` | G7 | Updated |
| `docs/plans/STORY-002-tasks.md` | G7 | This file |

---

## Totals

- **New files:** 10 (1 assembler, 1 test file, 8 golden files)
- **Modified files:** 12 (1 model, 8 config templates, 1 pipeline init, 2 test files)
- **Verified (no change):** 3 (`__main__.py`, `test_byte_for_byte.py`, `config.py`)
- **Documentation:** 3 (story, plan, tasks)
