# Security Review Report

```
ENGINEER: Security
STORY: STORY-009
SCORE: 10/12 (12 = effective max after N/A exclusions)
NA_COUNT: 3
STATUS: Request Changes
---
PASSED:
- [SEC-02] Output encoding — no injection vectors in output (2/2)
- [SEC-05] Sensitive data masking — no secrets/credentials in logs or output (2/2)
- [SEC-08] Dependency vulnerabilities — no new risky deps (2/2)
- [SEC-10] Audit logging — operations logged appropriately via Python logging module (2/2)

FAILED:
- [SEC-01] Input validation — CLI inputs validated, paths sanitized (0/2) — claude_setup/utils.py:22-24 — Fix: `atomic_output` calls `shutil.rmtree(dest_dir)` on user-supplied `--output-dir` without resolving symlinks or verifying the path is safe. A symlink at `dest_dir` pointing to a sensitive directory (e.g., `/etc`, `$HOME`) would cause that directory to be deleted. Add `dest_dir = dest_dir.resolve()` and verify the resolved path is not a symlink or outside the expected working tree before calling `rmtree`. Similarly, `--output-dir` accepts any path without validation — a path like `../../` could write outside the intended scope. [CRITICAL]

PARTIAL:
- [SEC-06] Error handling — no stack traces leaked to users, errors have context (1/2) — claude_setup/assembler/__init__.py:84 — Improvement: The bare `except Exception as exc` in `_execute_assemblers` catches all exceptions and wraps them in `PipelineError`. While this prevents stack trace leakage to the user, the original exception's full message (which could contain internal paths or system details) is passed through via `str(exc)`. Consider sanitizing or truncating the inner exception message before including it in user-facing output. [LOW]

N/A:
- [SEC-03] Authentication checks — Reason: CLI tool with no authentication requirement
- [SEC-04] Authorization checks — Reason: CLI tool with no authorization requirement
- [SEC-07] Cryptography usage — Reason: No cryptographic operations in the changed code
- [SEC-09] CORS/CSP headers — Reason: CLI tool, no HTTP server
```

## Detailed Findings

### CRITICAL: Path Traversal / Unsafe `shutil.rmtree` on User-Supplied Path

**File:** `/Users/edercnj/workspaces/claude-environment/claude_setup/utils.py`, lines 22-24

```python
def atomic_output(dest_dir: Path) -> Generator[Path, None, None]:
    temp_dir = Path(tempfile.mkdtemp(prefix="claude-setup-"))
    try:
        yield temp_dir
        if dest_dir.exists():
            shutil.rmtree(str(dest_dir))       # <-- DANGEROUS
        shutil.copytree(str(temp_dir), str(dest_dir))
    finally:
        if temp_dir.exists():
            shutil.rmtree(str(temp_dir))
```

The `dest_dir` parameter originates from the `--output-dir` CLI option (default: `.`). The following attack vectors exist:

1. **Symlink attack:** If `dest_dir` is a symlink pointing to a critical directory (e.g., `$HOME`, `/etc`), `shutil.rmtree` follows the symlink and destroys the target directory.
2. **Path traversal:** A value like `--output-dir ../../../important-data` would delete and overwrite an arbitrary directory.
3. **Default `.` is current working directory:** With the default value of `.`, the tool would `rmtree` the current working directory, which could be destructive if invoked from the wrong location.

**Recommended fix:**

```python
@contextmanager
def atomic_output(dest_dir: Path) -> Generator[Path, None, None]:
    dest_dir = dest_dir.resolve()
    if dest_dir.is_symlink():
        raise ValueError(f"Output directory must not be a symlink: {dest_dir}")
    temp_dir = Path(tempfile.mkdtemp(prefix="claude-setup-"))
    try:
        yield temp_dir
        if dest_dir.exists():
            shutil.rmtree(str(dest_dir))
        shutil.copytree(str(temp_dir), str(dest_dir))
    finally:
        if temp_dir.exists():
            shutil.rmtree(str(temp_dir))
```

Additionally, consider adding a safety check in `__main__.py` to ensure the resolved output directory is not a system path or the filesystem root.

### LOW: Exception Message Passthrough

**File:** `/Users/edercnj/workspaces/claude-environment/claude_setup/assembler/__init__.py`, line 84-85

```python
except Exception as exc:
    raise PipelineError(name, str(exc)) from exc
```

The raw exception message is forwarded to the user. While exceptions in this context are unlikely to contain sensitive data, internal file paths from `FileNotFoundError` or `PermissionError` could reveal directory structure. Consider logging the full exception at DEBUG level and providing a generic user-facing message.

### Positive Observations

- **No secrets or credentials** are logged or included in output.
- **Temporary directories** use `tempfile.mkdtemp` with a prefix, properly cleaned in `finally` blocks.
- **JSON parsing** in `_read_json_array` gracefully handles malformed input with a warning log and empty return.
- **Dependencies** (click, pyyaml, jinja2) are well-maintained, widely-used libraries with no known critical vulnerabilities.
- **Logging** uses the standard `logging` module with appropriate levels (info for operations, warning for missing templates).
- **Hook file permissions** set execute bit correctly via `dest.chmod(dest.stat().st_mode | 0o111)` without granting world-write.
- **Error handling** in the CLI layer properly converts domain exceptions to `click.ClickException`, preventing stack traces from reaching users.
