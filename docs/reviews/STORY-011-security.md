```
ENGINEER: Security
STORY: STORY-011
SCORE: 8/8 (effective max 8/20 after N/A exclusions)
NA_COUNT: 6
STATUS: Approved
---
PASSED:
- [1] Input validation (2/2) — CLI `--resources-dir` uses `click.Path(exists=True)` for filesystem validation; `_validate_dest_path` rejects symlinks and dangerous paths; `_reject_dangerous_path` protects system directories. No new user-input surface introduced; existing path traversal protections in `utils.py` remain intact.
- [5] Sensitive data masking (2/2) — No secrets, credentials, API keys, or PII found in moved files. `coverage.json` contains only code coverage metrics (line/branch counts), no sensitive data. All moved resources are markdown templates, YAML configs, and shell scripts with no embedded secrets.
- [6] Error handling — no stack traces (2/2) — Error messages in `find_resources_dir()` expose only the computed path ("Resources directory not found: {resources}"), which is safe diagnostic info. All exceptions are caught at CLI boundary and wrapped via `click.ClickException`, preventing raw stack traces from leaking to end users.
- [8] Dependency vulnerabilities (2/2) — No new dependencies added. `pyproject.toml` changes are limited to `[tool.setuptools.packages.find]` adding `where = ["src"]` and updating `[tool.coverage.run]` source path. No floating version ranges introduced.

FAILED:
(none)

PARTIAL:
(none)

N/A:
- [2] Output encoding — Reason: CLI tool that generates markdown/YAML files from templates. No HTML rendering, no browser output, no user-facing web content. Output encoding context (XSS, injection) does not apply.
- [3] Authentication checks — Reason: Local CLI tool with no authentication mechanism. No network services, no user sessions, no tokens.
- [4] Authorization checks — Reason: Local CLI tool operating on local filesystem. No RBAC/ABAC, no multi-user access control.
- [7] Cryptography usage — Reason: No cryptographic operations in this change. No encryption, hashing, signing, or key management involved.
- [9] CORS/CSP headers — Reason: CLI tool with no HTTP server component. No web responses, no security headers applicable.
- [10] Audit logging — Reason: Local CLI tool with no audit trail requirements. Standard Python logging used for debug/info output only.
---

## Detailed Analysis

### Path Resolution Review

The migration introduces two path resolution changes that were verified for traversal safety:

1. **`find_resources_dir()` in `src/claude_setup/utils.py:74-81`**: Changed from `parent.parent / "src"` to `parent.parent.parent / "resources"`. The extra `.parent` traversal is correct because the package moved one level deeper (`src/claude_setup/` vs `claude_setup/`). The function uses `Path.resolve()` on `__file__`, eliminating symlink-based traversal. Returns only if `.is_dir()` succeeds.

2. **`RulesAssembler.assemble()` in `src/claude_setup/assembler/rules_assembler.py:32`**: Changed from `parent.parent.parent / "src"` to `parent.parent.parent.parent / "resources"`. Same pattern — adds one `.parent` to account for the new `src/` prefix. This is an internal path computed from the installed package location, not from user input.

3. **CLI `--resources-dir` option in `src/claude_setup/__main__.py:31`**: Uses `click.Path(exists=True)` which validates the path exists before passing to the application. The value is used directly as a `Path()` constructor argument without further sanitization, which is acceptable because Click already validates existence. The downstream `atomic_output()` applies symlink rejection and protected-path checks on the output directory.

### coverage.json Assessment

The `coverage.json` file committed at the project root contains only code coverage metrics (line numbers, branch counts, percentage summaries). It references internal module paths (`claude_setup/__init__.py`, etc.) but contains no secrets, credentials, or sensitive data. Note: this file appears to reference the old `claude_setup/` paths (pre-migration), suggesting it may have been generated before the layout change. This is not a security concern but may be a staleness issue.

### Moved Files Assessment

All 300+ file renames are pure directory moves (`src/` to `resources/`, `claude_setup/` to `src/claude_setup/`). Content diffs are limited to variable renames (`src_dir` to `resources_dir`) and path adjustments. No new imports that bypass security boundaries were introduced. No new external dependencies added.
```
