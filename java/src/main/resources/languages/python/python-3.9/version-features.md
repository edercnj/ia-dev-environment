# Global Behavior & Language Policy
- **Output Language**: English ONLY. (Mandatory for all responses and internal reasoning).
- **Token Optimization**: Eliminate all greetings, apologies, and conversational fluff. Start responses directly with technical information.
- **Priority**: Maintain 100% fidelity to the technical constraints defined in the original rules below.

# Python 3.9 Version Features

## Dictionary Union Operators (PEP 584)

Merge and update dictionaries with `|` and `|=` operators.

```python
from __future__ import annotations

# Merge (creates new dict)
defaults = {"timeout": 30, "retries": 3, "verbose": False}
overrides = {"timeout": 60, "verbose": True}
config = defaults | overrides
# {"timeout": 60, "retries": 3, "verbose": True}

# Update in place
defaults |= overrides
```

## Type Hint Improvements (PEP 585)

Built-in collection types usable as generics — no need to import from `typing`.

```python
from __future__ import annotations

# CORRECT (3.9+) — built-in generics
def process_items(items: list[str]) -> dict[str, int]:
    return {item: len(item) for item in items}

def find_values(data: dict[str, list[int]]) -> set[int]:
    return {v for values in data.values() for v in values}

def get_pair() -> tuple[str, int]:
    return ("key", 42)

# LEGACY (3.8 and below) — typing imports
from typing import List, Dict, Set, Tuple
def process_items(items: List[str]) -> Dict[str, int]: ...
```

## String Methods

`removeprefix()` and `removesuffix()` replace fragile slicing.

```python
from __future__ import annotations

filename = "test_merchant_service.py"

# CORRECT (3.9+)
module_name = filename.removesuffix(".py")       # "test_merchant_service"
test_target = filename.removeprefix("test_")     # "merchant_service.py"

# LEGACY (3.8) — error-prone slicing
module_name = filename[:-3] if filename.endswith(".py") else filename
```

## `from __future__ import annotations` (PEP 563)

**MANDATORY** for Python 3.9 projects. Defers evaluation of all annotations to strings, enabling forward references and `X | Y` syntax in annotations without runtime errors.

```python
from __future__ import annotations  # MUST be first import

from typing import Optional

# With __future__.annotations, this is valid in 3.9 annotations:
def find_item(item_id: str) -> Item | None:
    ...

# BUT at runtime (isinstance, match, default values), use Optional:
def process(value: Optional[str] = None) -> str:
    if value is None:
        return "default"
    return value
```

**Rules:**
- MUST be the first import in every module
- Enables `X | Y` in **annotations only** (function signatures, variable annotations)
- Does NOT enable `X | Y` in runtime expressions (`isinstance()`, `match/case`, default values)
- For runtime union checks, use `isinstance(x, (TypeA, TypeB))`

## Compatibility Constraints (CRITICAL)

Features **NOT available** in Python 3.9. Code targeting 3.9 MUST use the alternatives.

| Feature | Available From | 3.9 Alternative |
|---------|---------------|-----------------|
| `X \| Y` union (runtime) | 3.10 | `Union[X, Y]` or `Optional[X]` from `typing` |
| `match/case` | 3.10 | `if/elif` chains |
| `ExceptionGroup` / `except*` | 3.11 | N/A (handle exceptions individually) |
| `tomllib` | 3.11 | `tomli` backport (`pip install tomli`) |
| `type X = ...` alias | 3.12 | `TypeAlias` from `typing` |
| `class Foo[T]:` generic syntax | 3.12 | `TypeVar` + `Generic[T]` from `typing` |
| `override` decorator | 3.12 | `typing_extensions.override` |
| F-string nested quotes/backslashes | 3.12 | Extract to variable before f-string |

### Runtime Union Alternative

```python
from __future__ import annotations

from typing import Optional, Union

# Annotations — X | Y is fine (deferred evaluation)
def find(item_id: str) -> Item | None:
    ...

# Runtime — MUST use typing forms
from typing import get_type_hints

ItemOrError = Union[Item, ErrorResult]  # runtime alias

def check_type(value: object) -> bool:
    return isinstance(value, (Item, ErrorResult))  # tuple form
```

### match/case Alternative

```python
from __future__ import annotations

# FORBIDDEN in 3.9
# match command:
#     case "init": ...
#     case "build": ...

# CORRECT — if/elif
def handle_command(command: str) -> None:
    if command == "init":
        run_init()
    elif command == "build":
        run_build()
    elif command == "test":
        run_test()
    else:
        raise ValueError(f"Unknown command: {command}")
```

### Generic Class Alternative

```python
from __future__ import annotations

from typing import Generic, TypeVar

T = TypeVar("T")

# FORBIDDEN in 3.9:
# class Repository[T]: ...

# CORRECT
class Repository(Generic[T]):
    def find_by_id(self, entity_id: str) -> Optional[T]:
        raise NotImplementedError

    def save(self, entity: T) -> T:
        raise NotImplementedError
```

## Recommended pyproject.toml (3.9)

```toml
[project]
requires-python = ">=3.9"

[tool.ruff]
target-version = "py39"
line-length = 120

[tool.ruff.lint]
select = ["E", "F", "I", "UP", "B", "SIM", "TCH"]

[tool.mypy]
python_version = "3.9"
strict = true
warn_return_any = true
warn_unused_configs = true
```
