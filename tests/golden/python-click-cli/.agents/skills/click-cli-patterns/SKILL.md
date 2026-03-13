---
name: click-cli-patterns
description: "Click CLI patterns: command groups, options/arguments, Jinja2 templating, atomic file operations, pyproject.toml packaging, CLI testing with CliRunner, structured logging for CLI."
allowed-tools:
  - Read
  - Grep
  - Glob
---

# Knowledge Pack: Click CLI Patterns

## Purpose

Provides Click-based CLI implementation patterns for framework-less Python tools. Agents reference this pack when generating code for Python CLI applications using Click, Jinja2, and PyYAML. All examples target Python 3.9+ compatibility (`from __future__ import annotations` mandatory, no `match/case`, no `X | Y` at runtime).

---

## 1. Project Structure

### Package Layout

```
my_cli/
├── pyproject.toml
├── src/
│   └── my_cli/
│       ├── __init__.py
│       ├── __main__.py          # python -m my_cli entrypoint
│       ├── cli.py               # Click groups and commands (thin layer)
│       ├── commands/             # One module per command group
│       │   ├── __init__.py
│       │   ├── init_cmd.py
│       │   ├── build_cmd.py
│       │   └── validate_cmd.py
│       ├── core/                 # Business logic (no Click dependency)
│       │   ├── __init__.py
│       │   ├── config.py         # Configuration loading
│       │   ├── renderer.py       # Jinja2 template rendering
│       │   ├── file_ops.py       # Atomic file operations
│       │   └── validator.py      # Validation rules
│       └── templates/            # Jinja2 templates (package data)
│           ├── dockerfile.j2
│           └── config.yaml.j2
├── tests/
│   ├── conftest.py
│   ├── test_cli.py
│   ├── commands/
│   │   └── test_init_cmd.py
│   └── core/
│       ├── test_config.py
│       └── test_renderer.py
└── README.md
```

### `__main__.py`

```python
from __future__ import annotations

from my_cli.cli import main

if __name__ == "__main__":
    main()
```

### `pyproject.toml` (Complete)

```toml
[build-system]
requires = ["setuptools>=68.0", "setuptools-scm>=8.0"]
build-backend = "setuptools.backends._legacy:_Backend"

[project]
name = "my-cli"
version = "1.0.0"
requires-python = ">=3.9"
dependencies = [
    "click>=8.1",
    "jinja2>=3.1",
    "pyyaml>=6.0",
]

[project.optional-dependencies]
dev = [
    "pytest>=7.4",
    "pytest-cov>=4.1",
    "ruff>=0.4",
    "mypy>=1.10",
]

[project.scripts]
my-cli = "my_cli.cli:main"

[tool.setuptools.packages.find]
where = ["src"]

[tool.setuptools.package-data]
my_cli = ["templates/*.j2"]

[tool.ruff]
target-version = "py39"
line-length = 120

[tool.ruff.lint]
select = ["E", "F", "I", "UP", "B", "SIM", "TCH"]

[tool.mypy]
python_version = "3.9"
strict = true

[tool.pytest.ini_options]
testpaths = ["tests"]
filterwarnings = ["error", "ignore::DeprecationWarning"]
```

### Separation of Concerns

| Layer | Responsibility | Click Dependency |
|-------|---------------|-----------------|
| `cli.py` | Define groups, wire commands | YES |
| `commands/` | Parse CLI args, call core, format output | YES |
| `core/` | Business logic, file I/O, rendering | NO |
| `templates/` | Jinja2 template files | NO |

**Golden Rule:** `core/` NEVER imports from `click`. All Click-specific code stays in `cli.py` and `commands/`.

---

## 2. Click Commands and Groups

### Root Group

```python
from __future__ import annotations

from pathlib import Path
from typing import Optional

import click

from my_cli.core.config import CliConfig, load_config


@click.group()
@click.option(
    "--config",
    "-c",
    "config_path",
    type=click.Path(exists=True, dir_okay=False, path_type=Path),
    default=None,
    help="Path to configuration file.",
)
@click.option("--verbose", "-v", is_flag=True, default=False, help="Enable verbose output.")
@click.version_option(package_name="my-cli")
@click.pass_context
def main(ctx: click.Context, config_path: Optional[Path], verbose: bool) -> None:
    """My CLI tool — generates project scaffolding."""
    ctx.ensure_object(dict)
    ctx.obj["verbose"] = verbose
    ctx.obj["config"] = load_config(config_path)
```

### Subcommand

```python
from __future__ import annotations

from pathlib import Path

import click


@click.command()
@click.argument("project_name")
@click.option(
    "--output-dir",
    "-o",
    type=click.Path(file_okay=False, path_type=Path),
    default=Path("."),
    help="Output directory.",
)
@click.option(
    "--template",
    "-t",
    type=click.Choice(["minimal", "standard", "full"], case_sensitive=False),
    default="standard",
    help="Project template to use.",
)
@click.option("--dry-run", is_flag=True, default=False, help="Show what would be created.")
@click.pass_context
def init(
    ctx: click.Context,
    project_name: str,
    output_dir: Path,
    template: str,
    dry_run: bool,
) -> None:
    """Initialize a new project from template."""
    config: CliConfig = ctx.obj["config"]
    verbose: bool = ctx.obj["verbose"]

    target = output_dir / project_name
    if target.exists() and not dry_run:
        raise click.ClickException(f"Directory already exists: {target}")

    result = scaffold_project(
        name=project_name,
        target=target,
        template_name=template,
        config=config,
        dry_run=dry_run,
    )

    if verbose:
        for file_path in result.created_files:
            click.echo(f"  Created: {file_path}")
    click.secho(f"Project 'my-cli-tool' initialized.", fg="green")
```

### Registering Commands

```python
from __future__ import annotations

from my_cli.commands.init_cmd import init
from my_cli.commands.build_cmd import build
from my_cli.commands.validate_cmd import validate

main.add_command(init)
main.add_command(build)
main.add_command(validate)
```

### Option Validation (Callbacks)

```python
from __future__ import annotations

import re

import click


def validate_project_name(
    ctx: click.Context,
    param: click.Parameter,
    value: str,
) -> str:
    if not re.match(r"^[a-z][a-z0-9_-]*$", value):
        raise click.BadParameter(
            f"Must start with lowercase letter, "
            f"contain only [a-z0-9_-]: {value}"
        )
    return value


@click.command()
@click.argument("name", callback=validate_project_name)
def init(name: str) -> None:
    """Initialize project."""
    ...
```

### Prompting for Missing Values

```python
from __future__ import annotations

import click


@click.command()
@click.option(
    "--author",
    prompt="Author name",
    help="Author name for project metadata.",
)
@click.option(
    "--license",
    "license_type",
    type=click.Choice(["MIT", "Apache-2.0", "GPL-3.0"]),
    prompt="License type",
    default="MIT",
    help="License type.",
)
def init(author: str, license_type: str) -> None:
    """Initialize project with author info."""
    ...
```

### Confirmation

```python
from __future__ import annotations

import click


@click.command()
@click.argument("target")
@click.option("--force", is_flag=True, default=False, help="Skip confirmation.")
def clean(target: str, force: bool) -> None:
    """Remove generated files."""
    if not force:
        click.confirm(f"Delete all files in '{target}'?", abort=True)
    perform_clean(target)
```

---

## 3. Configuration Loading

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

---

## 4. Jinja2 Template Engine

### Environment Setup

```python
from __future__ import annotations

from pathlib import Path

import jinja2


def create_jinja_env(template_dir: Path) -> jinja2.Environment:
    """Create a strict Jinja2 environment."""
    return jinja2.Environment(
        loader=jinja2.FileSystemLoader(str(template_dir)),
        undefined=jinja2.StrictUndefined,
        keep_trailing_newline=True,
        trim_blocks=True,
        lstrip_blocks=True,
        autoescape=False,
    )
```

### Package Resource Loader

```python
from __future__ import annotations

import jinja2


def create_package_jinja_env() -> jinja2.Environment:
    """Load templates bundled inside the package."""
    return jinja2.Environment(
        loader=jinja2.PackageLoader("my_cli", "templates"),
        undefined=jinja2.StrictUndefined,
        keep_trailing_newline=True,
        trim_blocks=True,
        lstrip_blocks=True,
        autoescape=False,
    )
```

### Chained Loader (Package + User Override)

```python
from __future__ import annotations

from pathlib import Path

import jinja2


def create_chained_env(
    user_template_dir: Path,
) -> jinja2.Environment:
    """User templates override package defaults."""
    return jinja2.Environment(
        loader=jinja2.ChoiceLoader([
            jinja2.FileSystemLoader(str(user_template_dir)),
            jinja2.PackageLoader("my_cli", "templates"),
        ]),
        undefined=jinja2.StrictUndefined,
        keep_trailing_newline=True,
        trim_blocks=True,
        lstrip_blocks=True,
        autoescape=False,
    )
```

### Custom Filters

```python
from __future__ import annotations

import re

import jinja2


def snake_case(value: str) -> str:
    s1 = re.sub(r"([A-Z]+)([A-Z][a-z])", r"\1_\2", value)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", s1).lower()


def kebab_case(value: str) -> str:
    return snake_case(value).replace("_", "-")


def register_filters(env: jinja2.Environment) -> None:
    env.filters["snake_case"] = snake_case
    env.filters["kebab_case"] = kebab_case
```

### Rendering to String

```python
from __future__ import annotations

from pathlib import Path
from typing import Optional

import jinja2


class TemplateRenderer:
    def __init__(self, env: jinja2.Environment) -> None:
        self._env = env

    def render(
        self,
        template_name: str,
        context: dict[str, object],
    ) -> str:
        template = self._env.get_template(template_name)
        return template.render(context)

    def render_string(
        self,
        source: str,
        context: dict[str, object],
    ) -> str:
        template = self._env.from_string(source)
        return template.render(context)
```

### Template Example (`config.yaml.j2`)

```jinja2
# Generated by my-cli — do not edit manually
project:
  name: {{ project_name }}
  version: {{ version | default("0.1.0") }}
{% if author %}
  author: {{ author }}
{% endif %}

dependencies:
{% for dep in dependencies %}
  - {{ dep }}
{% endfor %}
```

### Whitespace Control

```jinja2
{# trim_blocks + lstrip_blocks handle most cases #}
{# Use minus sign for fine control: #}

{% for item in items -%}
  {{ item }}
{%- endfor %}

{# Produces: item1item2item3 (no whitespace) #}

{# Prefer block-level control over inline minus signs #}
```

### Error Handling

```python
from __future__ import annotations

import click
import jinja2


def safe_render(
    renderer: TemplateRenderer,
    template_name: str,
    context: dict[str, object],
) -> str:
    try:
        return renderer.render(template_name, context)
    except jinja2.UndefinedError as e:
        raise click.ClickException(
            f"Missing template variable in "
            f"'{template_name}': {e}"
        ) from e
    except jinja2.TemplateSyntaxError as e:
        raise click.ClickException(
            f"Template syntax error in "
            f"'{e.filename}' line {e.lineno}: {e.message}"
        ) from e
    except jinja2.TemplateNotFound as e:
        raise click.ClickException(
            f"Template not found: {e.name}"
        ) from e
```

---

## 5. Atomic File Operations

### Atomic Write (tempfile + os.replace)

```python
from __future__ import annotations

import os
import tempfile
from pathlib import Path


def atomic_write(target: Path, content: str) -> None:
    """Write file atomically — never leaves partial content."""
    target.parent.mkdir(parents=True, exist_ok=True)
    fd, tmp_path = tempfile.mkstemp(
        dir=target.parent,
        suffix=".tmp",
    )
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as f:
            f.write(content)
        os.replace(tmp_path, target)
    except BaseException:
        os.unlink(tmp_path)
        raise
```

### Context Manager for Atomic Writes

```python
from __future__ import annotations

import os
import tempfile
from contextlib import contextmanager
from pathlib import Path
from typing import Generator, TextIO


@contextmanager
def atomic_open(
    target: Path,
    mode: str = "w",
) -> Generator[TextIO, None, None]:
    """Context manager for atomic file writes."""
    target.parent.mkdir(parents=True, exist_ok=True)
    fd, tmp_path = tempfile.mkstemp(
        dir=target.parent,
        suffix=".tmp",
    )
    try:
        with os.fdopen(fd, mode, encoding="utf-8") as f:
            yield f
        os.replace(tmp_path, target)
    except BaseException:
        os.unlink(tmp_path)
        raise
```

### Directory Scaffolding

```python
from __future__ import annotations

import shutil
from dataclasses import dataclass, field
from pathlib import Path


@dataclass
class ScaffoldResult:
    created_files: list[Path] = field(default_factory=list)
    created_dirs: list[Path] = field(default_factory=list)


def scaffold_directory(
    target: Path,
    files: dict[Path, str],
    dry_run: bool = False,
) -> ScaffoldResult:
    """Create directory tree with rendered files."""
    result = ScaffoldResult()

    for relative_path, content in files.items():
        full_path = target / relative_path
        result.created_dirs.append(full_path.parent)
        result.created_files.append(full_path)

        if not dry_run:
            atomic_write(full_path, content)

    return result
```

### Safe Copy with Backup

```python
from __future__ import annotations

import shutil
from pathlib import Path


def safe_copy_tree(
    src: Path,
    dst: Path,
    backup: bool = True,
) -> None:
    """Copy directory tree with optional backup."""
    if dst.exists() and backup:
        backup_path = dst.with_suffix(".bak")
        if backup_path.exists():
            shutil.rmtree(backup_path)
        shutil.copytree(dst, backup_path)
    shutil.copytree(src, dst, dirs_exist_ok=True)
```

### Dry-Run Pattern

```python
from __future__ import annotations

from pathlib import Path

import click


def write_file(
    target: Path,
    content: str,
    dry_run: bool = False,
) -> None:
    """Write file or report what would be written."""
    if dry_run:
        click.echo(f"  [DRY RUN] Would create: {target}")
        return
    atomic_write(target, content)
```

### Path Utilities

```python
from __future__ import annotations

from pathlib import Path


def ensure_parent_dirs(path: Path) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    return path


def relative_to_cwd(path: Path) -> Path:
    try:
        return path.relative_to(Path.cwd())
    except ValueError:
        return path
```

---

## 6. CLI Output and Logging

### Standard Output with Click

```python
from __future__ import annotations

import click


def echo_info(message: str) -> None:
    click.echo(message)


def echo_success(message: str) -> None:
    click.secho(message, fg="green")


def echo_warning(message: str) -> None:
    click.secho(f"Warning: {message}", fg="yellow", err=True)


def echo_error(message: str) -> None:
    click.secho(f"Error: {message}", fg="red", err=True)
```

### Verbosity Levels

```python
from __future__ import annotations

from dataclasses import dataclass

import click


@dataclass
class Output:
    verbose: bool = False

    def info(self, message: str) -> None:
        click.echo(message)

    def detail(self, message: str) -> None:
        if self.verbose:
            click.echo(f"  {message}")

    def success(self, message: str) -> None:
        click.secho(message, fg="green")

    def warning(self, message: str) -> None:
        click.secho(f"Warning: {message}", fg="yellow", err=True)

    def error(self, message: str) -> None:
        click.secho(f"Error: {message}", fg="red", err=True)
```

### Progress Bar

```python
from __future__ import annotations

from pathlib import Path
from typing import Sequence

import click


def render_files(
    files: Sequence[tuple[Path, str]],
    output: Output,
) -> None:
    with click.progressbar(
        files,
        label="Generating files",
        show_pos=True,
    ) as progress:
        for file_path, content in progress:
            atomic_write(file_path, content)
            output.detail(f"Created: {file_path}")
```

### Stdout vs Stderr Separation

```
stdout → machine-parseable output (file paths, JSON, rendered content)
stderr → human-readable messages (progress, warnings, errors)
```

```python
from __future__ import annotations

from pathlib import Path

import click


def list_generated_files(files: list[Path]) -> None:
    """Machine output to stdout, status to stderr."""
    click.echo(
        f"Generated {len(files)} files:", err=True,
    )
    for f in files:
        click.echo(str(f))  # stdout — pipeable
```

### Structured Logging (for long-running CLIs)

```python
from __future__ import annotations

import logging
import sys


def configure_logging(verbose: bool = False) -> None:
    level = logging.DEBUG if verbose else logging.INFO
    handler = logging.StreamHandler(sys.stderr)
    handler.setFormatter(
        logging.Formatter(
            "%(asctime)s [%(levelname)s] %(name)s: %(message)s",
            datefmt="%Y-%m-%d %H:%M:%S",
        )
    )
    root = logging.getLogger("my_cli")
    root.setLevel(level)
    root.addHandler(handler)
```

---

## 7. Testing

### CliRunner Basics

```python
from __future__ import annotations

from click.testing import CliRunner

from my_cli.cli import main


def test_main_version_flag_prints_version() -> None:
    runner = CliRunner()
    result = runner.invoke(main, ["--version"])

    assert result.exit_code == 0
    assert "my-cli" in result.output


def test_main_no_args_shows_help() -> None:
    runner = CliRunner()
    result = runner.invoke(main, [])

    assert result.exit_code == 0
    assert "Usage:" in result.output
```

### Testing with Isolated Filesystem

```python
from __future__ import annotations

from pathlib import Path

from click.testing import CliRunner

from my_cli.cli import main


def test_init_creates_project_directory() -> None:
    runner = CliRunner()
    with runner.isolated_filesystem() as tmp_dir:
        result = runner.invoke(main, ["init", "my-project"])

        assert result.exit_code == 0
        project = Path(tmp_dir) / "my-project"
        assert project.is_dir()
        assert (project / "pyproject.toml").is_file()
```

### Testing Exit Codes and Error Output

```python
from __future__ import annotations

from pathlib import Path

from click.testing import CliRunner

from my_cli.cli import main


def test_init_existing_dir_fails_with_error() -> None:
    runner = CliRunner()
    with runner.isolated_filesystem():
        Path("my-project").mkdir()

        result = runner.invoke(main, ["init", "my-project"])

        assert result.exit_code != 0
        assert "already exists" in result.output


def test_init_invalid_name_shows_bad_parameter() -> None:
    runner = CliRunner()
    result = runner.invoke(main, ["init", "123Invalid"])

    assert result.exit_code != 0
    assert "Must start with lowercase" in result.output
```

### Testing with Config File

```python
from __future__ import annotations

from pathlib import Path

from click.testing import CliRunner

from my_cli.cli import main

SAMPLE_CONFIG = """\
default_template: minimal
author: Test Author
"""


def test_init_with_config_uses_author() -> None:
    runner = CliRunner()
    with runner.isolated_filesystem():
        Path("my-cli.yaml").write_text(SAMPLE_CONFIG)

        result = runner.invoke(
            main, ["--config", "my-cli.yaml", "init", "demo"]
        )

        assert result.exit_code == 0
        generated = Path("demo") / "pyproject.toml"
        assert "Test Author" in generated.read_text()
```

### Testing Prompts

```python
from __future__ import annotations

from click.testing import CliRunner

from my_cli.cli import main


def test_init_prompts_for_author_when_missing() -> None:
    runner = CliRunner()
    result = runner.invoke(
        main,
        ["init", "demo"],
        input="Jane Doe\nMIT\n",
    )

    assert result.exit_code == 0
    assert "Author name" in result.output
```

### Parametrized CLI Tests

```python
from __future__ import annotations

import pytest
from click.testing import CliRunner

from my_cli.cli import main


@pytest.mark.parametrize(
    "template, expected_file",
    [
        ("minimal", "setup.py"),
        ("standard", "pyproject.toml"),
        ("full", "Dockerfile"),
    ],
)
def test_init_template_creates_expected_files(
    template: str,
    expected_file: str,
) -> None:
    runner = CliRunner()
    with runner.isolated_filesystem():
        result = runner.invoke(
            main, ["init", "demo", "--template", template]
        )

        assert result.exit_code == 0
        assert (Path("demo") / expected_file).is_file()
```

### Testing Core Logic (No Click)

```python
from __future__ import annotations

from pathlib import Path

import pytest

from my_cli.core.renderer import TemplateRenderer, create_jinja_env


@pytest.fixture
def renderer(tmp_path: Path) -> TemplateRenderer:
    template_dir = tmp_path / "templates"
    template_dir.mkdir()
    (template_dir / "test.txt.j2").write_text(
        "Hello {{ name }}!"
    )
    env = create_jinja_env(template_dir)
    return TemplateRenderer(env)


def test_render_substitutes_variables(
    renderer: TemplateRenderer,
) -> None:
    result = renderer.render(
        "test.txt.j2", {"name": "World"}
    )
    assert result == "Hello World!"


def test_render_missing_var_raises_undefined(
    renderer: TemplateRenderer,
) -> None:
    with pytest.raises(Exception, match="'name'"):
        renderer.render("test.txt.j2", {})
```

### Mocking External Dependencies

```python
from __future__ import annotations

from pathlib import Path
from unittest.mock import patch

from click.testing import CliRunner

from my_cli.cli import main


def test_init_dry_run_does_not_write_files() -> None:
    runner = CliRunner()
    with runner.isolated_filesystem():
        with patch(
            "my_cli.core.file_ops.atomic_write"
        ) as mock_write:
            result = runner.invoke(
                main, ["init", "demo", "--dry-run"]
            )

            assert result.exit_code == 0
            mock_write.assert_not_called()
            assert "[DRY RUN]" in result.output
```

### conftest.py

```python
from __future__ import annotations

from pathlib import Path

import pytest
from click.testing import CliRunner


@pytest.fixture
def cli_runner() -> CliRunner:
    return CliRunner(mix_stderr=False)


@pytest.fixture
def sample_config(tmp_path: Path) -> Path:
    config = tmp_path / "my-cli.yaml"
    config.write_text(
        "default_template: minimal\nauthor: Tester\n"
    )
    return config
```

---

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
