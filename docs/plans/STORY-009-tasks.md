# STORY-009 Task Breakdown: CLI Pipeline and Orchestration

**Story:** STORY-009 -- CLI Pipeline and Orchestration
**Phase:** 4 (Integration)
**Blocked By:** STORY-002, STORY-003, STORY-004, STORY-005, STORY-006, STORY-007, STORY-008
**Blocks:** STORY-010
**Architecture:** library (hexagonal-lite for a CLI tool)
**Stack:** Python 3.9 + Click 8.1

---

## Parallelism Groups

```
G1 (Foundation) ──→ G2 (Core Logic) ──→ G3 (CLI Layer) ──→ G4 (Tests)
   T1, T2             T3, T4              T5                 T6, T7, T8, T9
  (parallel)         (parallel)                              (parallel)
```

G1 tasks have no internal dependencies (parallel).
G2 depends on G1 (PipelineResult and PipelineError must exist for pipeline and utils).
G3 depends on G2 (CLI commands call run_pipeline and utils).
G4 depends on G3 (tests exercise all layers).

---

## G1 -- Foundation (Parallel)

### T1: Add `PipelineResult` dataclass to `models.py`

| Attribute | Value |
|-----------|-------|
| **Task ID** | G1-T1 |
| **Title** | Add PipelineResult dataclass to models.py |
| **Layer** | domain/model |
| **Tier** | Junior |
| **Budget** | S (~20 lines) |
| **Group** | G1 (Foundation) |

**Files to modify:**
- `claude_setup/models.py` -- add `PipelineResult` dataclass after `ProjectConfig`

**Dependencies:** None

**Description:**

Add a new `PipelineResult` dataclass to `claude_setup/models.py`. This is a pure domain value object representing the outcome of a pipeline execution. It must have zero framework dependencies (stdlib only: `dataclasses`, `pathlib`, `typing`).

**Dataclass definition:**

```python
@dataclass
class PipelineResult:
    success: bool
    output_dir: Path
    files_generated: List[Path]
    warnings: List[str]
    duration_ms: int
```

All fields are required. No `from_dict` factory needed (constructed directly by `run_pipeline`).

**Acceptance Criteria:**
- [ ] `PipelineResult` dataclass exists in `claude_setup/models.py`
- [ ] Has all 5 fields: `success`, `output_dir`, `files_generated`, `warnings`, `duration_ms`
- [ ] Uses only stdlib imports (`dataclasses`, `pathlib.Path`, `typing.List`)
- [ ] Is importable: `from claude_setup.models import PipelineResult`
- [ ] Follows existing pattern (same style as `ProjectConfig` and other dataclasses in the file)

**Checklist:**
- [ ] Add `from pathlib import Path` if not already imported (it is NOT currently imported in models.py)
- [ ] Add `PipelineResult` dataclass with type annotations
- [ ] Verify no circular imports
- [ ] Verify `python -c "from claude_setup.models import PipelineResult; print('OK')"`

**Check command:**
```bash
cd /Users/edercnj/workspaces/claude-environment && python -c "from claude_setup.models import PipelineResult; r = PipelineResult(success=True, output_dir=__import__('pathlib').Path('.'), files_generated=[], warnings=[], duration_ms=100); assert r.success; print('OK')"
```

---

### T2: Add `PipelineError` to `exceptions.py`

| Attribute | Value |
|-----------|-------|
| **Task ID** | G1-T2 |
| **Title** | Add PipelineError exception to exceptions.py |
| **Layer** | domain/model |
| **Tier** | Junior |
| **Budget** | S (~15 lines) |
| **Group** | G1 (Foundation) |

**Files to modify:**
- `claude_setup/exceptions.py` -- add `PipelineError` exception class

**Dependencies:** None (parallel with G1-T1)

**Description:**

Add a new `PipelineError` exception class to `claude_setup/exceptions.py`. This exception is raised when the pipeline orchestration encounters a fatal error that prevents completion. It should carry context about which assembler failed and what the error was.

**Exception definition:**

```python
class PipelineError(Exception):
    """Raised when the assembly pipeline fails fatally."""

    def __init__(self, assembler_name: str, reason: str) -> None:
        self.assembler_name = assembler_name
        self.reason = reason
        super().__init__(
            f"Pipeline failed at '{assembler_name}': {reason}"
        )
```

**Acceptance Criteria:**
- [ ] `PipelineError` exception exists in `claude_setup/exceptions.py`
- [ ] Has `assembler_name` and `reason` attributes for context
- [ ] Error message includes both assembler name and reason
- [ ] Is importable: `from claude_setup.exceptions import PipelineError`
- [ ] Follows existing pattern (same style as `ConfigValidationError`)

**Checklist:**
- [ ] Add `PipelineError` class with `assembler_name` and `reason` attributes
- [ ] Include descriptive `__init__` with formatted message
- [ ] Verify no circular imports
- [ ] Verify `python -c "from claude_setup.exceptions import PipelineError; print('OK')"`

**Check command:**
```bash
cd /Users/edercnj/workspaces/claude-environment && python -c "from claude_setup.exceptions import PipelineError; e = PipelineError('RulesAssembler', 'src not found'); assert 'RulesAssembler' in str(e); assert e.assembler_name == 'RulesAssembler'; print('OK')"
```

---

## G2 -- Core Logic (Parallel, depends on G1)

### T3: Implement `utils.py` -- `atomic_output()`, `setup_logging()`, `find_src_dir()`

| Attribute | Value |
|-----------|-------|
| **Task ID** | G2-T3 |
| **Title** | Create utils.py with atomic_output, setup_logging, find_src_dir |
| **Layer** | utils (infrastructure support) |
| **Tier** | Mid |
| **Budget** | M (~80 lines) |
| **Group** | G2 (Core Logic) |

**Files to create:**
- `claude_setup/utils.py` -- new module with three utility functions

**Dependencies:** G1-T1, G1-T2 (uses no domain types directly, but logically sequenced after foundation)

**Description:**

Create `claude_setup/utils.py` with three functions. This module depends only on stdlib (`tempfile`, `shutil`, `pathlib`, `logging`, `contextlib`). No framework or domain imports.

#### 3.1 `atomic_output(dest_dir: Path) -> ContextManager[Path]`

Context manager implementing RULE-003 (atomic output):

```python
@contextmanager
def atomic_output(dest_dir: Path) -> Generator[Path, None, None]:
    temp_dir = Path(tempfile.mkdtemp(prefix="claude-setup-"))
    try:
        yield temp_dir
        if dest_dir.exists():
            shutil.rmtree(str(dest_dir))
        shutil.copytree(str(temp_dir), str(dest_dir))
    finally:
        if temp_dir.exists():
            shutil.rmtree(str(temp_dir))
```

Contract:
- Creates `tempfile.mkdtemp()` directory with prefix `"claude-setup-"`.
- Yields the temp `Path`.
- On `__exit__` with no exception: copies temp contents to `dest_dir` via `shutil.copytree`, then removes temp.
- On `__exit__` with exception: removes temp dir via `shutil.rmtree`, re-raises exception.
- Guarantees: no partial files in `dest_dir` on failure.

#### 3.2 `setup_logging(verbose: bool) -> None`

```python
def setup_logging(verbose: bool) -> None:
    level = logging.DEBUG if verbose else logging.INFO
    logging.basicConfig(
        level=level,
        format="%(levelname)s: %(message)s",
    )
```

#### 3.3 `find_src_dir() -> Path`

```python
def find_src_dir() -> Path:
    src = Path(__file__).resolve().parent.parent / "src"
    if not src.is_dir():
        raise FileNotFoundError(
            f"Source directory not found: {src}"
        )
    return src
```

Resolves `Path(__file__).resolve().parent.parent / "src"` (i.e., two levels up from `claude_setup/utils.py` to repo root, then into `src/`). Raises `FileNotFoundError` if directory does not exist.

**Acceptance Criteria:**
- [ ] `claude_setup/utils.py` exists with all three functions
- [ ] `atomic_output` creates temp dir, yields it, copies to dest on success, cleans up on failure
- [ ] `atomic_output` removes temp dir in ALL cases (success and failure)
- [ ] `atomic_output` removes existing `dest_dir` before copying (to avoid merge with old files)
- [ ] `setup_logging` sets DEBUG when verbose=True, INFO when verbose=False
- [ ] `find_src_dir` returns the correct `src/` path relative to the package
- [ ] `find_src_dir` raises `FileNotFoundError` when `src/` does not exist
- [ ] Module uses only stdlib imports (zero framework dependencies)
- [ ] Each function is <= 25 lines
- [ ] All importable: `from claude_setup.utils import atomic_output, setup_logging, find_src_dir`

**Checklist:**
- [ ] Create `claude_setup/utils.py`
- [ ] Implement `atomic_output` context manager with `tempfile.mkdtemp` and `shutil.copytree`
- [ ] Implement `setup_logging` with `logging.basicConfig`
- [ ] Implement `find_src_dir` with `Path(__file__)` resolution
- [ ] Add `from __future__ import annotations` at top
- [ ] Verify no circular imports
- [ ] Verify all three functions are importable

**Check command:**
```bash
cd /Users/edercnj/workspaces/claude-environment && python -c "from claude_setup.utils import atomic_output, setup_logging, find_src_dir; print(find_src_dir()); print('OK')"
```

---

### T4: Implement `run_pipeline()` in `assembler/__init__.py`

| Attribute | Value |
|-----------|-------|
| **Task ID** | G2-T4 |
| **Title** | Implement run_pipeline() orchestration in assembler/__init__.py |
| **Layer** | application (co-located in assembler per project convention) |
| **Tier** | Senior |
| **Budget** | L (~120 lines) |
| **Group** | G2 (Core Logic) |

**Files to modify:**
- `claude_setup/assembler/__init__.py` -- add `run_pipeline()` function, update imports and `__all__`

**Dependencies:** G1-T1 (PipelineResult), G1-T2 (PipelineError), G2-T3 (atomic_output)

**Description:**

Add the `run_pipeline()` function to `claude_setup/assembler/__init__.py`. This is the main orchestration entry point that calls all assemblers in sequence with atomic output.

**Function signature:**

```python
def run_pipeline(
    config: ProjectConfig,
    src_dir: Path,
    output_dir: Path,
    dry_run: bool = False,
) -> PipelineResult:
```

**Algorithm:**

1. Record start time via `time.monotonic()`.
2. Initialize `warnings: List[str] = []` and `files_generated: List[Path] = []`.
3. If `dry_run=True`:
   - Use a temporary directory (via `atomic_output` to a throwaway path or `tempfile.mkdtemp`).
   - Run the full pipeline in the temp dir to get an accurate `files_generated` list.
   - Discard the temp dir (do NOT copy to `output_dir`).
   - Add `"Dry run -- no files written"` to warnings.
   - Return `PipelineResult(success=True, ...)` with the collected file list.
4. Otherwise, enter `atomic_output(output_dir)` context manager:
   a. Create `TemplateEngine(src_dir, config)`.
   b. Call `_execute_assemblers(config, src_dir, temp_dir, engine)` which returns `(files_generated, warnings)`.
   c. On success, atomic move from temp to output.
   d. On exception from any assembler, wrap in `PipelineError` and let `atomic_output` handle cleanup.
5. Record end time, compute `duration_ms = int((end - start) * 1000)`.
6. Return `PipelineResult`.

**Assembler execution order (critical -- ReadmeAssembler must be last):**

```python
def _build_assemblers(src_dir: Path) -> list:
    return [
        ("RulesAssembler", RulesAssembler()),
        ("SkillsAssembler", SkillsAssembler()),
        ("AgentsAssembler", AgentsAssembler()),
        ("PatternsAssembler", PatternsAssembler(src_dir)),
        ("ProtocolsAssembler", ProtocolsAssembler(src_dir)),
        ("HooksAssembler", HooksAssembler(src_dir)),
        ("SettingsAssembler", SettingsAssembler(src_dir)),
        ("ReadmeAssembler", ReadmeAssembler(src_dir)),
    ]
```

**Helper function `_execute_assemblers`:**

```python
def _execute_assemblers(
    config: ProjectConfig,
    src_dir: Path,
    output_dir: Path,
    engine: TemplateEngine,
) -> Tuple[List[Path], List[str]]:
    files: List[Path] = []
    warnings: List[str] = []
    for name, assembler in _build_assemblers(src_dir):
        try:
            result = assembler.assemble(config, output_dir, engine)
            files.extend(result)
        except Exception as exc:
            raise PipelineError(name, str(exc)) from exc
    return files, warnings
```

**Updates to `__init__.py`:**

- Add new imports: `PipelineResult`, `PipelineError`, `TemplateEngine`, `atomic_output`, `time`, `tempfile`, `Path`
- Add `run_pipeline` and `PipelineResult` to `__all__`
- Keep existing assembler re-exports

**Acceptance Criteria:**
- [ ] `run_pipeline()` function exists in `claude_setup/assembler/__init__.py`
- [ ] Calls all 8 assemblers in the correct order (ReadmeAssembler last)
- [ ] Uses `atomic_output` context manager for non-dry-run execution
- [ ] Handles `dry_run=True` by running in temp dir and discarding output
- [ ] Wraps assembler exceptions in `PipelineError` with assembler name context
- [ ] Returns `PipelineResult` with accurate `files_generated`, `warnings`, `duration_ms`
- [ ] `duration_ms` computed from `time.monotonic()` difference
- [ ] Each function/method is <= 25 lines
- [ ] Importable: `from claude_setup.assembler import run_pipeline`
- [ ] No circular imports
- [ ] Handles both assembler constructor patterns (no-arg and src_dir-arg)

**Checklist:**
- [ ] Add imports for `time`, `tempfile`, `Path`, `PipelineResult`, `PipelineError`, `TemplateEngine`, `atomic_output`
- [ ] Implement `_build_assemblers(src_dir)` helper returning ordered list of `(name, assembler)` tuples
- [ ] Implement `_execute_assemblers(config, src_dir, output_dir, engine)` helper
- [ ] Implement `run_pipeline(config, src_dir, output_dir, dry_run)` main function
- [ ] Implement dry-run path using temp dir
- [ ] Add `run_pipeline` and `PipelineResult` to `__all__`
- [ ] Verify total file length stays <= 250 lines (extract helpers if needed)
- [ ] Verify `python -c "from claude_setup.assembler import run_pipeline; print('OK')"`

**Check command:**
```bash
cd /Users/edercnj/workspaces/claude-environment && python -c "from claude_setup.assembler import run_pipeline, PipelineResult; print('OK')"
```

---

## G3 -- CLI Layer (depends on G2)

### T5: Implement CLI commands in `__main__.py` -- `generate`, `validate`

| Attribute | Value |
|-----------|-------|
| **Task ID** | G3-T5 |
| **Title** | Rewrite __main__.py with generate and validate CLI commands |
| **Layer** | adapter/inbound (CLI) |
| **Tier** | Mid |
| **Budget** | M (~100 lines) |
| **Group** | G3 (CLI Layer) |

**Files to modify:**
- `claude_setup/__main__.py` -- replace `init` command with `generate` and `validate` commands

**Dependencies:** G2-T3 (utils), G2-T4 (run_pipeline)

**Description:**

Rewrite `claude_setup/__main__.py` to replace the current `init` command with two new subcommands: `generate` and `validate`. The existing `main` Click group remains as the entry point.

**New command structure:**

```
claude-setup generate --config <path> [--output-dir DIR] [--src-dir DIR] [--verbose] [--dry-run]
claude-setup generate --interactive [--output-dir DIR] [--src-dir DIR] [--verbose] [--dry-run]
claude-setup validate --config <path> [--verbose]
```

#### `generate` command:

```python
@main.command()
@click.option("--config", "-c", type=click.Path(exists=True), default=None, help="Path to YAML config file.")
@click.option("--interactive", "-i", is_flag=True, default=False, help="Run in interactive mode.")
@click.option("--output-dir", "-o", type=click.Path(), default=".", help="Output directory.")
@click.option("--src-dir", "-s", type=click.Path(exists=True), default=None, help="Source templates directory.")
@click.option("--verbose", "-v", is_flag=True, default=False, help="Enable verbose logging.")
@click.option("--dry-run", is_flag=True, default=False, help="Show what would be generated without writing.")
def generate(config, interactive, output_dir, src_dir, verbose, dry_run):
```

Logic:
1. Validate mutually exclusive options: `--config` and `--interactive` cannot both be specified. Neither can be omitted.
2. If `--config`: call `load_config(Path(config))`.
3. If `--interactive`: call `run_interactive()`.
4. Resolve `src_dir`: from `--src-dir` option or `find_src_dir()`.
5. Resolve `output_dir`: from `--output-dir` option (default `.`).
6. If `--verbose`: call `setup_logging(verbose=True)`.
7. Call `run_pipeline(project_config, src_dir, output_dir, dry_run=dry_run)`.
8. Display `PipelineResult` summary (files count, warnings, duration).
9. Exit with code 0 on `result.success`, 1 otherwise.

#### `validate` command:

```python
@main.command()
@click.option("--config", "-c", type=click.Path(exists=True), required=True, help="Path to YAML config file.")
@click.option("--verbose", "-v", is_flag=True, default=False, help="Enable verbose logging.")
def validate(config, verbose):
```

Logic:
1. Call `load_config(Path(config))`.
2. Call `validate_stack(project_config)` from `domain/validator.py`.
3. Display validation results.
4. Exit with code 0 if no errors, 1 if errors.

#### Result display helper:

Extract a `_display_result(result: PipelineResult)` function to format and echo the pipeline result summary:
- Number of files generated
- Warnings (if any)
- Duration in milliseconds
- Success/failure status

#### Error handling:

- Catch `ConfigValidationError` and display as `click.ClickException`.
- Catch `PipelineError` and display as `click.ClickException`.
- Catch `FileNotFoundError` (from `find_src_dir`) and display as `click.ClickException`.

**Backward compatibility note:** The `init` command is removed. Pre-1.0 tool, no external consumers. Existing tests in `test_cli_init.py` will need updating (handled in G4).

**Acceptance Criteria:**
- [ ] `generate` command exists with all 6 options: `--config`, `--interactive`, `--output-dir`, `--src-dir`, `--verbose`, `--dry-run`
- [ ] `validate` command exists with `--config` (required) and `--verbose`
- [ ] `--config` and `--interactive` are mutually exclusive in `generate`
- [ ] `generate --config` loads YAML and calls `run_pipeline`
- [ ] `generate --interactive` collects inputs and calls `run_pipeline`
- [ ] `generate --dry-run` shows plan without writing files
- [ ] `validate --config` runs validation and displays results
- [ ] Exit code 0 on success, 1 on failure for both commands
- [ ] Error messages displayed via `click.ClickException` (not raw tracebacks)
- [ ] `--help` works for both commands
- [ ] `main` group still supports `--version` and `--help`
- [ ] Each function is <= 25 lines (extract helpers as needed)
- [ ] File is <= 250 lines total

**Checklist:**
- [ ] Remove `init` command
- [ ] Add `generate` command with all options
- [ ] Add `validate` command with all options
- [ ] Implement mutual exclusion for `--config` / `--interactive`
- [ ] Add `_display_result` helper for PipelineResult output
- [ ] Import `run_pipeline`, `find_src_dir`, `setup_logging`, `validate_stack`, `PipelineError`
- [ ] Handle all exceptions with `click.ClickException`
- [ ] Verify `python -m claude_setup generate --help` works
- [ ] Verify `python -m claude_setup validate --help` works

**Check command:**
```bash
cd /Users/edercnj/workspaces/claude-environment && python -m claude_setup --help && python -m claude_setup generate --help && python -m claude_setup validate --help
```

---

## G4 -- Tests (Parallel, depends on G3)

### T6: Unit tests for `utils.py`

| Attribute | Value |
|-----------|-------|
| **Task ID** | G4-T6 |
| **Title** | Unit tests for atomic_output, setup_logging, find_src_dir |
| **Layer** | tests |
| **Tier** | Mid |
| **Budget** | M (~120 lines) |
| **Group** | G4 (Tests) |

**Files to create:**
- `tests/test_utils.py` -- unit tests for all three utility functions

**Dependencies:** G2-T3 (utils.py must exist)

**Description:**

Unit tests for `claude_setup/utils.py`. Each function gets its own test class. Tests use `tmp_path` fixture for filesystem operations.

**Test cases:**

| Test Class | Test Method | Scenario | Expected |
|------------|-------------|----------|----------|
| `TestAtomicOutput` | `test_success_copies_to_dest` | Write file in temp, exit normally | File exists in dest_dir |
| `TestAtomicOutput` | `test_success_removes_temp_dir` | Exit normally | Temp dir does not exist after exit |
| `TestAtomicOutput` | `test_failure_cleans_temp_dir` | Raise exception inside context | Temp dir removed, dest_dir empty/absent |
| `TestAtomicOutput` | `test_failure_does_not_write_dest` | Raise exception inside context | dest_dir has no partial files |
| `TestAtomicOutput` | `test_failure_reraises_exception` | Raise ValueError inside context | ValueError propagates |
| `TestAtomicOutput` | `test_replaces_existing_dest_dir` | dest_dir has old files, new run writes different files | dest_dir contains only new files |
| `TestAtomicOutput` | `test_creates_dest_parent_dirs` | dest_dir parent does not exist | Parent dirs created automatically |
| `TestSetupLogging` | `test_verbose_sets_debug_level` | `verbose=True` | Root logger level is DEBUG |
| `TestSetupLogging` | `test_non_verbose_sets_info_level` | `verbose=False` | Root logger level is INFO |
| `TestFindSrcDir` | `test_returns_existing_src_dir` | Running from installed package | Returns a Path that exists |
| `TestFindSrcDir` | `test_returns_path_ending_in_src` | Normal run | Path ends with `src` |
| `TestFindSrcDir` | `test_raises_when_src_missing` | Monkeypatch `__file__` to nonexistent location | Raises `FileNotFoundError` |

**Acceptance Criteria:**
- [ ] All 12 test cases pass
- [ ] Tests use `tmp_path` fixture (no manual temp dir management)
- [ ] Tests are independent (no execution order dependency)
- [ ] Test class names follow pattern: `Test{FunctionName}`
- [ ] Test method names follow pattern: `test_{scenario}_{expected}`
- [ ] No mocking of domain logic
- [ ] No `sleep()` calls

**Checklist:**
- [ ] Create `tests/test_utils.py`
- [ ] Implement `TestAtomicOutput` with 7 test methods
- [ ] Implement `TestSetupLogging` with 2 test methods
- [ ] Implement `TestFindSrcDir` with 3 test methods
- [ ] Run `python -m pytest tests/test_utils.py -x -q`

**Check command:**
```bash
cd /Users/edercnj/workspaces/claude-environment && python -m pytest tests/test_utils.py -x -q --tb=short
```

---

### T7: Unit tests for `run_pipeline()`

| Attribute | Value |
|-----------|-------|
| **Task ID** | G4-T7 |
| **Title** | Unit tests for run_pipeline with mocked assemblers |
| **Layer** | tests |
| **Tier** | Mid |
| **Budget** | M (~150 lines) |
| **Group** | G4 (Tests) |

**Files to create:**
- `tests/test_pipeline.py` -- unit tests for `run_pipeline` function

**Dependencies:** G2-T4 (run_pipeline must exist)

**Description:**

Unit tests for `run_pipeline()` in `claude_setup/assembler/__init__.py`. These tests mock individual assemblers to isolate the orchestration logic. The `ProjectConfig` can be constructed from test fixtures.

**Test cases:**

| Test Class | Test Method | Scenario | Expected |
|------------|-------------|----------|----------|
| `TestRunPipeline` | `test_returns_pipeline_result` | Valid config, mocked assemblers | Returns `PipelineResult` instance |
| `TestRunPipeline` | `test_success_flag_true_on_completion` | All assemblers succeed | `result.success is True` |
| `TestRunPipeline` | `test_files_generated_contains_all_assembler_outputs` | All assemblers return paths | `result.files_generated` contains all paths |
| `TestRunPipeline` | `test_duration_ms_is_positive` | Any run | `result.duration_ms > 0` |
| `TestRunPipeline` | `test_output_dir_matches_requested` | Specify output_dir | `result.output_dir` matches |
| `TestRunPipeline` | `test_assembler_failure_raises_pipeline_error` | One assembler raises exception | `PipelineError` raised with assembler name |
| `TestRunPipeline` | `test_assembler_failure_cleans_up_temp` | One assembler raises | No temp dir left behind |
| `TestRunPipeline` | `test_dry_run_does_not_write_to_output` | `dry_run=True` | `output_dir` does not exist or is empty |
| `TestRunPipeline` | `test_dry_run_returns_success` | `dry_run=True` | `result.success is True` |
| `TestRunPipeline` | `test_dry_run_includes_warning` | `dry_run=True` | `"Dry run"` in `result.warnings` |
| `TestRunPipeline` | `test_assembler_order_readme_last` | Mock assemblers, track call order | ReadmeAssembler called after all others |
| `TestBuildAssemblers` | `test_returns_eight_assemblers` | Call `_build_assemblers` | Returns list of 8 tuples |
| `TestBuildAssemblers` | `test_last_assembler_is_readme` | Call `_build_assemblers` | Last tuple name is `"ReadmeAssembler"` |

**Mocking strategy:**
- Mock individual assembler classes' `assemble` method to return empty lists or predefined paths
- Use `monkeypatch` or `unittest.mock.patch` to replace assembler constructors
- Use `tmp_path` for output_dir and a test fixture for src_dir

**Acceptance Criteria:**
- [ ] All 13 test cases pass
- [ ] Tests mock assemblers (not domain logic)
- [ ] Tests verify orchestration behavior (order, error handling, dry-run)
- [ ] Tests use `tmp_path` fixture
- [ ] No execution order dependency between tests
- [ ] `PipelineError` correctly wraps assembler exceptions

**Checklist:**
- [ ] Create `tests/test_pipeline.py`
- [ ] Create test fixtures for `ProjectConfig` (reuse from `conftest.py` if available)
- [ ] Implement `TestRunPipeline` with 11 test methods
- [ ] Implement `TestBuildAssemblers` with 2 test methods
- [ ] Run `python -m pytest tests/test_pipeline.py -x -q`

**Check command:**
```bash
cd /Users/edercnj/workspaces/claude-environment && python -m pytest tests/test_pipeline.py -x -q --tb=short
```

---

### T8: CLI integration tests with `CliRunner`

| Attribute | Value |
|-----------|-------|
| **Task ID** | G4-T8 |
| **Title** | CLI integration tests for generate and validate commands |
| **Layer** | tests |
| **Tier** | Mid |
| **Budget** | M (~180 lines) |
| **Group** | G4 (Tests) |

**Files to create:**
- `tests/test_cli_generate.py` -- CLI tests for `generate` command
- `tests/test_cli_validate.py` -- CLI tests for `validate` command

**Files to modify:**
- `tests/test_cli.py` -- update tests for new command structure (help output may reference `generate`/`validate` instead of `init`)
- `tests/test_cli_init.py` -- replace `init` tests with `generate` equivalent tests

**Dependencies:** G3-T5 (__main__.py must be rewritten)

**Description:**

CLI tests using `click.testing.CliRunner`. These tests exercise the full CLI layer including option parsing, error handling, and output formatting. Pipeline execution can be mocked to isolate CLI behavior.

#### `tests/test_cli_generate.py` test cases:

| Test Class | Test Method | Scenario | Expected |
|------------|-------------|----------|----------|
| `TestGenerateCommand` | `test_generate_help_exits_zero` | `generate --help` | exit_code == 0, shows options |
| `TestGenerateCommand` | `test_generate_config_valid_exits_zero` | `generate -c valid.yaml` (mock pipeline) | exit_code == 0 |
| `TestGenerateCommand` | `test_generate_config_missing_file_exits_two` | `generate -c /no/such/file.yaml` | exit_code == 2 |
| `TestGenerateCommand` | `test_generate_interactive_exits_zero` | `generate --interactive` with input (mock pipeline) | exit_code == 0 |
| `TestGenerateCommand` | `test_generate_both_config_and_interactive_exits_error` | `generate -c file.yaml --interactive` | exit_code != 0, error message |
| `TestGenerateCommand` | `test_generate_neither_config_nor_interactive_exits_error` | `generate` (no options) | exit_code != 0, error message |
| `TestGenerateCommand` | `test_generate_dry_run_shows_plan` | `generate -c valid.yaml --dry-run` (mock pipeline) | exit_code == 0, "dry run" in output |
| `TestGenerateCommand` | `test_generate_verbose_enables_logging` | `generate -c valid.yaml --verbose` (mock pipeline) | exit_code == 0 |
| `TestGenerateCommand` | `test_generate_output_dir_option` | `generate -c valid.yaml --output-dir /tmp/test` (mock pipeline) | exit_code == 0 |
| `TestGenerateCommand` | `test_generate_pipeline_error_exits_one` | `generate -c valid.yaml` (mock pipeline raises) | exit_code == 1 |

#### `tests/test_cli_validate.py` test cases:

| Test Class | Test Method | Scenario | Expected |
|------------|-------------|----------|----------|
| `TestValidateCommand` | `test_validate_help_exits_zero` | `validate --help` | exit_code == 0, shows options |
| `TestValidateCommand` | `test_validate_valid_config_exits_zero` | `validate -c valid.yaml` | exit_code == 0, "valid" or "no errors" |
| `TestValidateCommand` | `test_validate_invalid_config_exits_one` | `validate -c invalid.yaml` | exit_code == 1, error messages |
| `TestValidateCommand` | `test_validate_missing_file_exits_two` | `validate -c /no/file.yaml` | exit_code == 2 |
| `TestValidateCommand` | `test_validate_missing_config_option_exits_error` | `validate` (no --config) | exit_code == 2 (missing required) |

#### Updates to existing test files:

**`tests/test_cli.py`:** Update assertions if help text changed (e.g., `init` no longer listed, `generate`/`validate` listed). The `test_help_returns_zero`, `test_version_returns_version`, and `test_no_args_shows_help` tests should still pass with updated assertions.

**`tests/test_cli_init.py`:** Replace all `init` invocations with `generate` equivalents. The `TestInitWithConfigFile` class becomes `TestGenerateWithConfigFile` and `TestInitInteractive` becomes `TestGenerateInteractive`. Adapt option names (`init -c` becomes `generate -c`, `init` interactive becomes `generate --interactive`). Note: the `generate` command needs a mocked `run_pipeline` to avoid actually running assemblers. Alternatively, these can be removed if `test_cli_generate.py` covers the same scenarios.

**Acceptance Criteria:**
- [ ] `tests/test_cli_generate.py` has 10 test cases, all passing
- [ ] `tests/test_cli_validate.py` has 5 test cases, all passing
- [ ] `tests/test_cli.py` updated and passing (help/version/no-args)
- [ ] `tests/test_cli_init.py` updated or removed (no references to `init` command)
- [ ] All tests use `CliRunner` from `click.testing`
- [ ] Pipeline calls are mocked where appropriate to isolate CLI testing
- [ ] No `sleep()` calls
- [ ] Tests are independent (no execution order dependency)

**Checklist:**
- [ ] Create `tests/test_cli_generate.py` with `TestGenerateCommand` class
- [ ] Create `tests/test_cli_validate.py` with `TestValidateCommand` class
- [ ] Update `tests/test_cli.py` assertions for new command structure
- [ ] Update or remove `tests/test_cli_init.py`
- [ ] Create test fixture YAML files if not already available in `tests/conftest.py`
- [ ] Run all CLI tests: `python -m pytest tests/test_cli*.py -x -q`

**Check command:**
```bash
cd /Users/edercnj/workspaces/claude-environment && python -m pytest tests/test_cli_generate.py tests/test_cli_validate.py tests/test_cli.py -x -q --tb=short
```

---

### T9: E2E pipeline integration tests

| Attribute | Value |
|-----------|-------|
| **Task ID** | G4-T9 |
| **Title** | End-to-end pipeline integration tests with real assemblers |
| **Layer** | tests |
| **Tier** | Mid |
| **Budget** | M (~120 lines) |
| **Group** | G4 (Tests) |

**Files to create:**
- `tests/test_pipeline_integration.py` -- integration tests running `run_pipeline` with real assemblers and real `src/` directory

**Dependencies:** G2-T4 (run_pipeline), G3-T5 (CLI, indirectly)

**Description:**

End-to-end integration tests that call `run_pipeline()` with real `ProjectConfig`, the actual `src/` directory, and real assemblers. These tests verify the full pipeline produces correct output without mocking.

**Test cases:**

| Test Class | Test Method | Scenario | Expected |
|------------|-------------|----------|----------|
| `TestPipelineIntegration` | `test_pipeline_success_with_valid_config` | Full config (python/click) | `result.success is True` |
| `TestPipelineIntegration` | `test_pipeline_generates_rules_directory` | Full config | `output_dir/rules/` exists with `.md` files |
| `TestPipelineIntegration` | `test_pipeline_generates_skills_directory` | Full config | `output_dir/skills/` exists |
| `TestPipelineIntegration` | `test_pipeline_generates_settings_json` | Full config | `output_dir/settings.json` exists and is valid JSON |
| `TestPipelineIntegration` | `test_pipeline_generates_readme` | Full config | `output_dir/README.md` exists |
| `TestPipelineIntegration` | `test_pipeline_files_generated_not_empty` | Full config | `len(result.files_generated) > 0` |
| `TestPipelineIntegration` | `test_pipeline_duration_ms_positive` | Full config | `result.duration_ms > 0` |
| `TestPipelineIntegration` | `test_pipeline_atomic_cleanup_on_failure` | Config with broken src_dir | No partial files in output_dir |
| `TestPipelineIntegration` | `test_pipeline_dry_run_no_output` | Full config + `dry_run=True` | `output_dir` is empty, result.success is True |
| `TestPipelineIntegration` | `test_pipeline_dry_run_lists_files` | Full config + `dry_run=True` | `result.files_generated` is non-empty |

**Test setup:**
- Use `tmp_path` for `output_dir`
- Use `find_src_dir()` or hardcoded `Path` to the real `src/` directory
- Construct `ProjectConfig` matching the project identity (python 3.9, click 8.1, library)
- Reuse test fixture from `tests/conftest.py` if available

**Acceptance Criteria:**
- [ ] All 10 test cases pass
- [ ] Tests use real `src/` directory and real assemblers (no mocking)
- [ ] Tests use `tmp_path` for output to avoid polluting the workspace
- [ ] Atomic output verified: failure leaves no partial files
- [ ] Dry-run verified: no files written to output_dir
- [ ] Tests are independent (each creates fresh output_dir via `tmp_path`)
- [ ] Total test execution time < 5 seconds

**Checklist:**
- [ ] Create `tests/test_pipeline_integration.py`
- [ ] Set up `ProjectConfig` fixture matching python/click/library profile
- [ ] Implement `TestPipelineIntegration` with 10 test methods
- [ ] Verify tests pass with real `src/` directory
- [ ] Run `python -m pytest tests/test_pipeline_integration.py -x -q`

**Check command:**
```bash
cd /Users/edercnj/workspaces/claude-environment && python -m pytest tests/test_pipeline_integration.py -x -q --tb=short
```

---

## Summary

| Group | Tasks | New Files | Modified Files | Total Est. Lines |
|-------|-------|-----------|----------------|-----------------|
| G1 | 2 | 0 | `models.py`, `exceptions.py` | ~35 |
| G2 | 2 | `utils.py` | `assembler/__init__.py` | ~200 |
| G3 | 1 | 0 | `__main__.py` | ~100 |
| G4 | 4 | `test_utils.py`, `test_pipeline.py`, `test_cli_generate.py`, `test_cli_validate.py`, `test_pipeline_integration.py` | `test_cli.py`, `test_cli_init.py` | ~570 |
| **Total** | **9** | **6** | **5** | **~905** |

## Dependency Graph

```
G1-T1 (PipelineResult) ──┐
                          ├──→ G2-T4 (run_pipeline) ──→ G3-T5 (CLI) ──→ G4-T7 (pipeline tests)
G1-T2 (PipelineError) ───┤                                           ──→ G4-T8 (CLI tests)
                          │                                           ──→ G4-T9 (E2E tests)
                          ├──→ G2-T3 (utils) ──────────────────────────→ G4-T6 (utils tests)
                          │
                          └──→ (G2-T3 and G2-T4 are parallel within G2)
```

## File Inventory

### New Files (6)

| File | Group | Purpose |
|------|-------|---------|
| `claude_setup/utils.py` | G2 | atomic_output, setup_logging, find_src_dir |
| `tests/test_utils.py` | G4 | Unit tests for utils module |
| `tests/test_pipeline.py` | G4 | Unit tests for run_pipeline |
| `tests/test_cli_generate.py` | G4 | CLI tests for generate command |
| `tests/test_cli_validate.py` | G4 | CLI tests for validate command |
| `tests/test_pipeline_integration.py` | G4 | E2E integration tests |

### Modified Files (5)

| File | Group | Changes |
|------|-------|---------|
| `claude_setup/models.py` | G1 | Add PipelineResult dataclass |
| `claude_setup/exceptions.py` | G1 | Add PipelineError exception |
| `claude_setup/assembler/__init__.py` | G2 | Add run_pipeline(), _build_assemblers(), _execute_assemblers(), update __all__ |
| `claude_setup/__main__.py` | G3 | Replace init with generate and validate commands |
| `tests/test_cli.py` | G4 | Update assertions for new CLI structure |

### Removed/Replaced Files

| File | Group | Action |
|------|-------|--------|
| `tests/test_cli_init.py` | G4 | Replace with `test_cli_generate.py` or update to test `generate` command |

## Full Validation Command

```bash
cd /Users/edercnj/workspaces/claude-environment && python -m pytest tests/ -x -q --tb=short && python -m pytest tests/ --cov=claude_setup --cov-report=term-missing --cov-fail-under=95
```
