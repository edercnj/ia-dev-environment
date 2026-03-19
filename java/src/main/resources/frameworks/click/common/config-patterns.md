# Global Behavior & Language Policy
- **Output Language**: English ONLY. (Mandatory for all responses and internal reasoning).
- **Token Optimization**: Eliminate all greetings, apologies, and conversational fluff. Start responses directly with technical information.
- **Priority**: Maintain 100% fidelity to the technical constraints defined in the original rules below.

# Click — Configuration Patterns
> Extends: `core/10-infrastructure-principles.md`

## Layered Configuration (defaults → file → env → CLI)

```python
from __future__ import annotations

import os
from pathlib import Path
from typing import Optional

import click
import yaml


def load_config(config_path: Optional[Path] = None) -> dict[str, object]:
    """Load config with layered precedence."""
    raw = _load_defaults()
    file_path = config_path or _find_config_file()
    if file_path is not None:
        raw.update(_load_yaml(file_path))
    raw = _apply_env_overrides(raw)
    return raw
```

## YAML Loading (Safe Only)

```python
from __future__ import annotations

from pathlib import Path

import click
import yaml


def _load_yaml(path: Path) -> dict[str, object]:
    with path.open("r", encoding="utf-8") as f:
        data = yaml.safe_load(f)
    if not isinstance(data, dict):
        raise click.ClickException(f"Config must be a YAML mapping: {path}")
    return data
```

**FORBIDDEN:** `yaml.load()` without `Loader` — arbitrary code execution risk.

## Config File Discovery

```python
from __future__ import annotations

from pathlib import Path
from typing import Optional

import click

CONFIG_FILENAME = "my-cli.yaml"


def _find_config_file() -> Optional[Path]:
    """Search CWD first, then platform-specific app config dir."""
    cwd_config = Path.cwd() / CONFIG_FILENAME
    if cwd_config.is_file():
        return cwd_config
    app_dir = Path(click.get_app_dir("my-cli"))
    global_config = app_dir / CONFIG_FILENAME
    if global_config.is_file():
        return global_config
    return None
```

## Dataclass Config

```python
from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path


@dataclass(frozen=True)
class CliConfig:
    project_root: Path
    default_template: str = "standard"
    author: str = ""
    license_type: str = "MIT"
```

## Environment Variable Overrides

Prefix all env vars with the tool name to avoid collisions:

```python
from __future__ import annotations

import os


def _apply_env_overrides(raw: dict[str, object]) -> dict[str, object]:
    env_author = os.environ.get("MY_CLI_AUTHOR")
    if env_author is not None:
        raw["author"] = env_author
    return raw
```
