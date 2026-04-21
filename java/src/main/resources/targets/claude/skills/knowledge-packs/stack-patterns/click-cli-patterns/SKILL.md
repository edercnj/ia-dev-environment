---
name: click-cli-patterns
description: "Click CLI patterns: command groups, options/arguments, Jinja2 templating, atomic file operations, pyproject.toml packaging, and CliRunner testing."
user-invocable: false
allowed-tools:
  - Read
  - Grep
  - Glob
---

# Pattern: Click CLI Patterns

## Purpose

Provides Click-based CLI implementation patterns for framework-less Python tools. Agents reference this pack when generating code for Python CLI applications using Click, Jinja2, and PyYAML. All examples target Python 3.9+ compatibility (`from __future__ import annotations` mandatory, no `match/case`, no `X | Y` at runtime).

## Supplements

Supplements `architecture` and `layer-templates` knowledge packs with Click CLI-specific conventions.

## Stack Compatibility

- **Python:** ≥ 3.9 (explicit `from __future__ import annotations`; no `match/case`; no runtime `X | Y`)
- **Click:** ≥ 8.1
- **Jinja2:** ≥ 3.1 (always `StrictUndefined`; `PackageLoader` for templates)
- **PyYAML:** ≥ 6.0 (`yaml.safe_load()` only)
- **Build:** `setuptools >= 68.0`, `setuptools-scm >= 8.0`
- **Tooling:** `ruff >= 0.4`, `mypy >= 1.10`, `pytest >= 7.4`, `pytest-cov >= 4.1`

## Patterns Index

| Pattern | Use case | File |
| :--- | :--- | :--- |
| Project Structure | Package layout, `pyproject.toml`, separation of concerns | [`references/examples-project-structure.md`](references/examples-project-structure.md) |
| Commands and Groups | Root groups, subcommands, option validation, prompting, confirmation | [`references/examples-commands-and-groups.md`](references/examples-commands-and-groups.md) |
| Configuration Loading | YAML config parsing with `yaml.safe_load`, `dataclass` configs, Click context injection | [`references/examples-configuration-loading.md`](references/examples-configuration-loading.md) |
| Jinja2 Templates | `PackageLoader`, `StrictUndefined`, autoescape by suffix, template context builders | [`references/examples-jinja2-templates.md`](references/examples-jinja2-templates.md) |
| Atomic File Operations | Tempfile + `os.replace()` pattern, directory atomicity, idempotent writes | [`references/examples-atomic-file-ops.md`](references/examples-atomic-file-ops.md) |
| CLI Output and Logging | `click.echo`/`click.secho`, stdout vs stderr, verbosity flag, structured logging integration | [`references/examples-output-and-logging.md`](references/examples-output-and-logging.md) |
| Testing | `CliRunner.invoke()`, `isolated_filesystem`, fixtures, parametrized commands, exit-code assertions | [`references/examples-testing.md`](references/examples-testing.md) |

## When to Open an Example File

Open a specific `references/examples-<pattern>.md` only when you are about to implement that pattern. The slim body above is enough to decide *which* pattern applies; the example file carries the complete copy-pasteable code.

## References

All pattern examples live under `references/examples-*.md` next to this SKILL.md. The naming convention is `examples-<slug>.md` where `<slug>` matches the third column of the Patterns Index above.

## Anti-Patterns (Click CLI-Specific)

- `sys.exit()` in command functions — use `raise click.ClickException(msg)` or `ctx.exit(code)` instead (allows CliRunner to capture exit codes)
- `print()` for output — use `click.echo()` / `click.secho()` (respects piping, encoding, and testing)
- Business logic inside `@click.command()` functions — extract to `core/` module; commands are thin wrappers
- `yaml.load()` without `Loader` — ALWAYS use `yaml.safe_load()` (security: arbitrary code execution risk)
- `jinja2.Undefined` (default) — ALWAYS use `jinja2.StrictUndefined` (silent variable errors cause corrupt output)
- String paths (`"/some/path"`) — use `pathlib.Path` everywhere; Click supports `path_type=Path`
- `os.makedirs()` + manual file write — use `atomic_write()` pattern (prevents partial file corruption)
- `click.echo()` for both machine and human output — separate: stdout for data, stderr for messages
- Hard-coded template strings in Python — move to `.j2` files with `PackageLoader`
- `@click.option("--flag", type=bool)` — use `is_flag=True` for boolean flags
- Mocking Click internals in tests — test through `CliRunner.invoke()` instead
- `match/case` for command dispatch — use `if/elif` (Python 3.9 compatibility)
- `X | Y` runtime union — use `Optional[X]` or `Union[X, Y]` from `typing`
