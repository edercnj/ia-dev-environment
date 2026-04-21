# Example: Configuration Loading

### Dataclass Config

```python
from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional

import yaml


@dataclass(frozen=True)
class TemplateConfig:
    name: str
    source_dir: Path
    variables: dict[str, str] = field(default_factory=dict)


@dataclass(frozen=True)
class CliConfig:
    project_root: Path
    templates: list[TemplateConfig] = field(default_factory=list)
    default_template: str = "standard"
    author: str = ""
    license_type: str = "MIT"
```

### Loading with Layered Config

```python
from __future__ import annotations

import os
from pathlib import Path
from typing import Optional

import click
import yaml

from my_cli.core.config import CliConfig, TemplateConfig

CONFIG_FILENAME = "my-cli.yaml"


def load_config(config_path: Optional[Path] = None) -> CliConfig:
    """Load config: defaults → file → env → CLI args."""
    raw = _load_defaults()
    file_path = config_path or _find_config_file()
    if file_path is not None:
        raw = _merge(raw, _load_yaml(file_path))
    raw = _apply_env_overrides(raw)
    return _parse_config(raw)


def _load_defaults() -> dict[str, object]:
    return {
        "default_template": "standard",
        "author": "",
        "license_type": "MIT",
        "templates": [],
    }


def _find_config_file() -> Optional[Path]:
    """Search CWD, then app config dir."""
    cwd_config = Path.cwd() / CONFIG_FILENAME
    if cwd_config.is_file():
        return cwd_config
    app_dir = Path(click.get_app_dir("my-cli"))
    global_config = app_dir / CONFIG_FILENAME
    if global_config.is_file():
        return global_config
    return None


def _load_yaml(path: Path) -> dict[str, object]:
    with path.open("r", encoding="utf-8") as f:
        data = yaml.safe_load(f)
    if not isinstance(data, dict):
        raise click.ClickException(
            f"Config must be a YAML mapping: {path}"
        )
    return data


def _apply_env_overrides(
    raw: dict[str, object],
) -> dict[str, object]:
    env_author = os.environ.get("MY_CLI_AUTHOR")
    if env_author is not None:
        raw["author"] = env_author
    env_license = os.environ.get("MY_CLI_LICENSE")
    if env_license is not None:
        raw["license_type"] = env_license
    return raw


def _merge(
    base: dict[str, object],
    override: dict[str, object],
) -> dict[str, object]:
    result = {**base}
    result.update(override)
    return result


def _parse_config(raw: dict[str, object]) -> CliConfig:
    templates = [
        TemplateConfig(
            name=t["name"],
            source_dir=Path(t["source_dir"]),
            variables=t.get("variables", {}),
        )
        for t in raw.get("templates", [])
    ]
    return CliConfig(
        project_root=Path.cwd(),
        templates=templates,
        default_template=str(raw.get("default_template", "standard")),
        author=str(raw.get("author", "")),
        license_type=str(raw.get("license_type", "MIT")),
    )
```

### Config YAML Example

```yaml
# my-cli.yaml
default_template: standard
author: "Dev Team"
license_type: MIT
templates:
  - name: standard
    source_dir: ./templates/standard
    variables:
      python_version: "3.9"
      use_docker: "true"
```
