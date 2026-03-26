# Global Behavior & Language Policy
- **Output Language**: English ONLY. (Mandatory for all responses and internal reasoning).
- **Token Optimization**: Eliminate all greetings, apologies, and conversational fluff. Start responses directly with technical information.
- **Priority**: Maintain 100% fidelity to the technical constraints defined in the original rules below.

# Click — Testing Patterns
> Extends: `core/03-testing-philosophy.md`

## CliRunner

```python
from __future__ import annotations

from click.testing import CliRunner

from my_cli.cli import main


def test_main_version_flag_prints_version() -> None:
    runner = CliRunner()
    result = runner.invoke(main, ["--version"])

    assert result.exit_code == 0
    assert "my-cli" in result.output
```

## Isolated Filesystem

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
        assert (Path(tmp_dir) / "my-project").is_dir()
```

## Testing with Input (Prompts)

```python
from __future__ import annotations

from click.testing import CliRunner

from my_cli.cli import main


def test_init_prompts_for_author() -> None:
    runner = CliRunner()
    result = runner.invoke(main, ["init", "demo"], input="Jane Doe\nMIT\n")

    assert result.exit_code == 0
    assert "Author name" in result.output
```

## Stderr Separation

```python
from __future__ import annotations

import pytest
from click.testing import CliRunner


@pytest.fixture
def cli_runner() -> CliRunner:
    return CliRunner(mix_stderr=False)
```

With `mix_stderr=False`, use `result.output` for stdout and `result.stderr` for stderr.

## Parametrized CLI Tests

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
def test_init_template_creates_expected_files(template: str, expected_file: str) -> None:
    runner = CliRunner()
    with runner.isolated_filesystem():
        result = runner.invoke(main, ["init", "demo", "--template", template])

        assert result.exit_code == 0
        assert Path("demo", expected_file).is_file()
```

## Test Core Logic Without Click

Business logic in `core/` should be tested directly without CliRunner:

```python
from __future__ import annotations

from pathlib import Path

import pytest

from my_cli.core.renderer import TemplateRenderer, create_jinja_env


@pytest.fixture
def renderer(tmp_path: Path) -> TemplateRenderer:
    template_dir = tmp_path / "templates"
    template_dir.mkdir()
    (template_dir / "test.txt.j2").write_text("Hello {{ name }}!")
    return TemplateRenderer(create_jinja_env(template_dir))


def test_render_substitutes_variables(renderer: TemplateRenderer) -> None:
    result = renderer.render("test.txt.j2", {"name": "World"})
    assert result == "Hello World!"
```

## FORBIDDEN

- Mocking Click internals (`click.Context`, `click.Parameter`) — test through `CliRunner.invoke()`
- `sys.exit()` in commands — prevents CliRunner from capturing exit codes
- Tests without `exit_code` assertion — always verify the command succeeded or failed as expected
