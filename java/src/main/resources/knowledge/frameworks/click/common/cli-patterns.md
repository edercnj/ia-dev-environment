# Global Behavior & Language Policy
- **Output Language**: English ONLY. (Mandatory for all responses and internal reasoning).
- **Token Optimization**: Eliminate all greetings, apologies, and conversational fluff. Start responses directly with technical information.
- **Priority**: Maintain 100% fidelity to the technical constraints defined in the original rules below.

# Click — CLI Patterns
> Extends: `core/01-clean-code.md`

## Command Group Structure

```python
from __future__ import annotations

import click

@click.group()
@click.option("--verbose", "-v", is_flag=True, default=False)
@click.version_option(package_name="my-cli")
@click.pass_context
def main(ctx: click.Context, verbose: bool) -> None:
    """CLI tool description."""
    ctx.ensure_object(dict)
    ctx.obj["verbose"] = verbose
```

## Subcommand Registration

```python
from __future__ import annotations

from my_cli.commands.init_cmd import init
from my_cli.commands.build_cmd import build

main.add_command(init)
main.add_command(build)
```

## Option and Argument Patterns

```python
from __future__ import annotations

from pathlib import Path

import click

@click.command()
@click.argument("name")
@click.option("--output", "-o", type=click.Path(file_okay=False, path_type=Path), default=Path("."))
@click.option("--template", "-t", type=click.Choice(["minimal", "standard", "full"]), default="standard")
@click.option("--dry-run", is_flag=True, default=False)
@click.pass_context
def init(ctx: click.Context, name: str, output: Path, template: str, dry_run: bool) -> None:
    """Initialize a new project."""
    ...
```

## Error Handling

```python
from __future__ import annotations

import click

# CORRECT — Click-native exceptions
raise click.ClickException("Configuration file not found")
raise click.BadParameter("Must be a valid identifier", param_hint="'--name'")
raise click.UsageError("Cannot use --force with --dry-run")

# FORBIDDEN
import sys
sys.exit(1)  # Breaks CliRunner testing
```

## Context Object for Shared State

```python
from __future__ import annotations

import click

@click.pass_context
def subcommand(ctx: click.Context) -> None:
    verbose: bool = ctx.obj["verbose"]
    config = ctx.obj["config"]
```

## Separation of Concerns

| Layer | Can import Click | Responsibility |
|-------|-----------------|----------------|
| `cli.py` | YES | Group definition, command wiring |
| `commands/` | YES | Argument parsing, output formatting |
| `core/` | NO | Business logic, file I/O, rendering |

**Golden Rule:** `core/` never imports `click`. Commands are thin wrappers that delegate to `core/`.
