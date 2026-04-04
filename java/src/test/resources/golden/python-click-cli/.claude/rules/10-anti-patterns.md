# Rule 10 — Anti-Patterns (python + click)

> Language-specific anti-patterns with incorrect and correct code examples.
> Each entry references the rule or knowledge pack it violates.

## Anti-Patterns

### ANTI-001: God Command (CRITICAL)
**Category:** SERVICE_LAYER
**Rule violated:** `03-coding-standards.md#solid` (SRP)

**Incorrect code:**
```python
# Single command with multiple responsibilities
@click.command()
@click.argument("input_file")
def process(input_file):
    # Reads, validates, transforms, writes — all in one function
    data = open(input_file).read()
    parsed = json.loads(data)
    validated = {k: v for k, v in parsed.items() if v is not None}
    result = transform(validated)
    with open("output.json", "w") as f:
        json.dump(result, f)
    send_email("admin@co.com", "Done processing")
```

**Correct code:**
```python
# Command delegates to focused services
@click.command()
@click.argument("input_file", type=click.Path(exists=True))
def process(input_file: str) -> None:
    reader = FileReader()
    validator = DataValidator()
    writer = FileWriter()

    data = reader.read(Path(input_file))
    validated = validator.validate(data)
    writer.write(Path("output.json"), validated)
```

### ANTI-002: Bare Exception Handling (HIGH)
**Category:** ERROR_HANDLING
**Rule violated:** `03-coding-standards.md#error-handling`

**Incorrect code:**
```python
# Catches everything, swallows all errors
@click.command()
def run():
    try:
        do_work()
    except:  # bare except — catches SystemExit too
        pass  # silently swallowed
```

**Correct code:**
```python
# Specific exception with user-friendly message
@click.command()
def run():
    try:
        do_work()
    except FileNotFoundError as e:
        raise click.ClickException(
            f"Input file not found: {e.filename}"
        ) from e
    except ValidationError as e:
        raise click.ClickException(
            f"Validation failed: {e}"
        ) from e
```

### ANTI-003: Hardcoded Paths (HIGH)
**Category:** SECURITY
**Rule violated:** `06-security-baseline.md` (path handling)

**Incorrect code:**
```python
# Hardcoded absolute path — not portable
def load_config():
    with open("/home/user/.config/app/config.yaml") as f:
        return yaml.safe_load(f)
```

**Correct code:**
```python
# Configurable path with safe defaults
def load_config(config_path: Path | None = None) -> dict:
    if config_path is None:
        config_path = Path(
            click.get_app_dir("my-cli-tool")
        ) / "config.yaml"
    if not config_path.exists():
        return {}
    with config_path.open() as f:
        return yaml.safe_load(f) or {}
```

### ANTI-004: Global Mutable State (HIGH)
**Category:** SERVICE_LAYER
**Rule violated:** `03-coding-standards.md#forbidden`

**Incorrect code:**
```python
# Module-level mutable state — shared across invocations
_results: list[str] = []

@click.command()
def analyze():
    _results.append(run_analysis())  # accumulates across calls
    click.echo(f"Total: {len(_results)}")
```

**Correct code:**
```python
# State scoped to invocation via Click context
@click.command()
@click.pass_context
def analyze(ctx: click.Context):
    results = ctx.ensure_object(list)
    results.append(run_analysis())
    click.echo(f"Total: {len(results)}")
```

### ANTI-005: Unsafe YAML Loading (CRITICAL)
**Category:** SECURITY
**Rule violated:** `06-security-baseline.md` (deserialization)

**Incorrect code:**
```python
# yaml.load without SafeLoader — arbitrary code execution
def load_config(path: str) -> dict:
    with open(path) as f:
        return yaml.load(f)  # unsafe: allows arbitrary Python
```

**Correct code:**
```python
# Always use safe_load or explicit SafeLoader
def load_config(path: Path) -> dict:
    with path.open() as f:
        return yaml.safe_load(f) or {}
```
