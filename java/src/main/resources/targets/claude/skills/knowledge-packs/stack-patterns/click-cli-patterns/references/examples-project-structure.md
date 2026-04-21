# Example: Project Structure

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
