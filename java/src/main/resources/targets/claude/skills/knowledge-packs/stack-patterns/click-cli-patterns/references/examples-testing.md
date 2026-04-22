# Example: Testing

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
