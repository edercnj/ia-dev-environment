# STORY-010 Task Breakdown: Tests and Verification End-to-End

**Story:** STORY-010 -- Tests and Verification End-to-End
**Phase:** 5 (Verification)
**Blocked By:** STORY-009
**Blocks:** --
**Architecture:** library (hexagonal-lite for a CLI tool)
**Stack:** Python 3.9 + Click 8.1 + pytest + pytest-cov

---

## Parallelism Groups

```
G1 (Foundation) ──> G2 (Core Logic) ──> G3 (CLI/Script) ──> G4 (Unit Tests)
   T1, T2             T3                  T4                   T5, T6
  (parallel)                                                  (parallel)
                                                                  |
                                                                  v
                                                G5 (Parametrized Tests) ──> G6 (Edge/Perf) ──> G7 (Integration)
                                                   T7                        T8, T9              T10
                                                                            (parallel)
```

G1 tasks have no internal dependencies (parallel).
G2 depends on G1 (VerificationResult and FileDiff must exist for verify_output).
G3 depends on G2 (golden file script calls verify_output and pipeline).
G4 depends on G1+G2 (unit tests exercise models and verify_output).
G5 depends on G3+G4 (parametrized tests need golden files generated).
G6 depends on G5 (edge cases and perf tests extend the parametrized suite).
G7 depends on G6 (full integration verification is the final gate).

---

## Config Profiles Inventory (8 profiles)

| # | Profile File | Short Name |
|---|-------------|------------|
| 1 | `setup-config.go-gin.yaml` | go-gin |
| 2 | `setup-config.java-quarkus.yaml` | java-quarkus |
| 3 | `setup-config.java-spring.yaml` | java-spring |
| 4 | `setup-config.kotlin-ktor.yaml` | kotlin-ktor |
| 5 | `setup-config.python-click-cli.yaml` | python-click-cli |
| 6 | `setup-config.python-fastapi.yaml` | python-fastapi |
| 7 | `setup-config.rust-axum.yaml` | rust-axum |
| 8 | `setup-config.typescript-nestjs.yaml` | typescript-nestjs |

> Note: Story mentions 7 profiles but 8 exist. All 8 must be covered.

---

## G1 -- Foundation (Parallel)

### T1: Add `VerificationResult` dataclass to `models.py`

| Attribute | Value |
|-----------|-------|
| **Task ID** | G1-T1 |
| **Title** | Add VerificationResult dataclass to models.py |
| **Layer** | domain/model |
| **Tier** | Junior |
| **Budget** | S (~15 lines) |
| **Group** | G1 (Foundation) |

**Files to modify:**
- `ia_dev_env/models.py` -- add `VerificationResult` dataclass after `PipelineResult`

**Dependencies:** None

**Description:**

Add a `VerificationResult` dataclass to `ia_dev_env/models.py`. This is a pure domain value object representing the outcome of a byte-for-byte output comparison. It must have zero framework dependencies (stdlib only).

**Dataclass definition:**

```python
@dataclass
class VerificationResult:
    """Outcome of a byte-for-byte output verification."""

    success: bool
    total_files: int
    mismatches: List[FileDiff]
    missing_files: List[Path]
    extra_files: List[Path]
```

All fields are required. No `from_dict` factory needed (constructed directly by `verify_output`).

**Acceptance Criteria:**
- [ ] `VerificationResult` dataclass exists in `ia_dev_env/models.py`
- [ ] Has all 5 fields: `success`, `total_files`, `mismatches`, `missing_files`, `extra_files`
- [ ] Uses only stdlib imports (already present: `dataclasses`, `pathlib.Path`, `typing.List`)
- [ ] Is importable: `from ia_dev_env.models import VerificationResult`
- [ ] Follows existing pattern (same style as `PipelineResult`)

**Checklist:**
- [ ] Add `VerificationResult` dataclass with type annotations
- [ ] Ensure `FileDiff` is defined before `VerificationResult` (forward reference)
- [ ] Verify no circular imports
- [ ] Verify `python -c "from ia_dev_env.models import VerificationResult; print('OK')"`

**Check command:**
```bash
cd /Users/edercnj/workspaces/claude-environment && python -c "from ia_dev_env.models import VerificationResult; r = VerificationResult(success=True, total_files=5, mismatches=[], missing_files=[], extra_files=[]); assert r.success; print('OK')"
```

---

### T2: Add `FileDiff` dataclass to `models.py`

| Attribute | Value |
|-----------|-------|
| **Task ID** | G1-T2 |
| **Title** | Add FileDiff dataclass to models.py |
| **Layer** | domain/model |
| **Tier** | Junior |
| **Budget** | S (~12 lines) |
| **Group** | G1 (Foundation) |

**Files to modify:**
- `ia_dev_env/models.py` -- add `FileDiff` dataclass before `VerificationResult`

**Dependencies:** None (parallel with G1-T1, but must be ordered before `VerificationResult` in the file)

**Description:**

Add a `FileDiff` dataclass to `ia_dev_env/models.py`. This value object represents a single file mismatch with unified diff content and size information.

**Dataclass definition:**

```python
@dataclass
class FileDiff:
    """A single file mismatch between Python output and reference."""

    path: Path
    diff: str
    python_size: int
    reference_size: int
```

**Acceptance Criteria:**
- [ ] `FileDiff` dataclass exists in `ia_dev_env/models.py`
- [ ] Has all 4 fields: `path`, `diff`, `python_size`, `reference_size`
- [ ] Uses only stdlib imports (`pathlib.Path`)
- [ ] Is importable: `from ia_dev_env.models import FileDiff`
- [ ] Positioned before `VerificationResult` in the file (since `VerificationResult` references it)

**Checklist:**
- [ ] Add `FileDiff` dataclass with type annotations
- [ ] Place it after `PipelineResult` and before `VerificationResult`
- [ ] Verify `python -c "from ia_dev_env.models import FileDiff; print('OK')"`

**Check command:**
```bash
cd /Users/edercnj/workspaces/claude-environment && python -c "from ia_dev_env.models import FileDiff; d = FileDiff(path=__import__('pathlib').Path('test.md'), diff='--- a\\n+++ b\\n', python_size=100, reference_size=100); assert d.python_size == 100; print('OK')"
```

---

## G2 -- Core Logic (depends on G1)

### T3: Implement `verify_output()` in `ia_dev_env/verifier.py`

| Attribute | Value |
|-----------|-------|
| **Task ID** | G2-T3 |
| **Title** | Implement verify_output() with recursive comparison and diff generation |
| **Layer** | domain/engine |
| **Tier** | Mid |
| **Budget** | L (~120 lines) |
| **Group** | G2 (Core Logic) |

**Files to create:**
- `ia_dev_env/verifier.py` -- new module with `verify_output()` and helpers

**Dependencies:** G1-T1 (VerificationResult), G1-T2 (FileDiff)

**Description:**

Create `ia_dev_env/verifier.py` implementing the byte-for-byte comparison engine. This module depends only on stdlib (`pathlib`, `difflib`, `os`). No framework imports.

#### Main function: `verify_output(python_dir: Path, reference_dir: Path) -> VerificationResult`

Algorithm:
1. Collect all relative file paths from both `python_dir` and `reference_dir` (recursive walk).
2. Compute set difference: `missing_files` = in reference but not in python; `extra_files` = in python but not in reference.
3. For common files: perform byte-for-byte comparison (`file.read_bytes()`).
4. For mismatched files: generate unified diff (text files via `difflib.unified_diff`; binary files produce a size-only diff message).
5. Build and return `VerificationResult`.

#### Helper functions:

```python
def _collect_relative_paths(base_dir: Path) -> List[Path]:
    """Walk directory recursively, return sorted relative paths."""

def _compare_files(
    python_file: Path,
    reference_file: Path,
    relative_path: Path,
) -> Optional[FileDiff]:
    """Compare two files byte-for-byte, return FileDiff if different."""

def _generate_text_diff(
    python_file: Path,
    reference_file: Path,
    relative_path: Path,
) -> str:
    """Generate unified diff string for text files."""
```

**Acceptance Criteria:**
- [ ] `verify_output()` function exists in `ia_dev_env/verifier.py`
- [ ] Recursively compares all files in both directories
- [ ] Byte-for-byte comparison using `read_bytes()`
- [ ] Detects missing files (in reference but not in python output)
- [ ] Detects extra files (in python output but not in reference)
- [ ] Generates unified diff for text file mismatches
- [ ] Handles binary files gracefully (size-only diff message)
- [ ] Returns `VerificationResult` with accurate `total_files` count
- [ ] `success` is `True` only when zero mismatches, zero missing, zero extra
- [ ] Module uses only stdlib imports
- [ ] Each function is <= 25 lines
- [ ] File is <= 250 lines total

**Checklist:**
- [ ] Create `ia_dev_env/verifier.py`
- [ ] Add `from __future__ import annotations` at top
- [ ] Implement `_collect_relative_paths(base_dir)`
- [ ] Implement `_compare_files(python_file, reference_file, relative_path)`
- [ ] Implement `_generate_text_diff(python_file, reference_file, relative_path)`
- [ ] Implement `verify_output(python_dir, reference_dir)`
- [ ] Verify `python -c "from ia_dev_env.verifier import verify_output; print('OK')"`

**Check command:**
```bash
cd /Users/edercnj/workspaces/claude-environment && python -c "from ia_dev_env.verifier import verify_output; print('OK')"
```

---

## G3 -- CLI/Script (depends on G2)

### T4: Create golden file generation script

| Attribute | Value |
|-----------|-------|
| **Task ID** | G3-T4 |
| **Title** | Create golden file generation script for all config profiles |
| **Layer** | scripts / test infrastructure |
| **Tier** | Mid |
| **Budget** | M (~80 lines) |
| **Group** | G3 (CLI/Script) |

**Files to create:**
- `scripts/generate_golden.py` -- standalone script to generate golden files
- `tests/golden/.gitkeep` -- placeholder for golden files directory

**Dependencies:** G2-T3 (verify_output for validation), STORY-009 (run_pipeline must work)

**Description:**

Create a script that generates reference golden files by running the Python pipeline for each config profile. Golden files are stored in `tests/golden/<profile-name>/` (e.g., `tests/golden/java-quarkus/`).

**Script behavior:**

```
python scripts/generate_golden.py [--profile <name>] [--all] [--verify]
```

1. `--all` (default): Generate golden files for all 8 config profiles.
2. `--profile <name>`: Generate for a single profile (e.g., `--profile java-quarkus`).
3. `--verify`: After generation, run `verify_output()` to self-check (output == golden).
4. For each profile:
   a. Load config from `src/config-templates/setup-config.<profile>.yaml`.
   b. Run `run_pipeline(config, src_dir, golden_dir/<profile>/)`.
   c. Print summary of files generated.

**Directory structure after generation:**

```
tests/golden/
  go-gin/
    rules/
    skills/
    ...
  java-quarkus/
    rules/
    skills/
    ...
  (one directory per profile)
```

**Acceptance Criteria:**
- [ ] Script exists at `scripts/generate_golden.py`
- [ ] Generates golden files for all 8 config profiles
- [ ] Supports `--profile` for single-profile generation
- [ ] Supports `--verify` for self-check after generation
- [ ] Golden files stored in `tests/golden/<profile>/`
- [ ] Script is runnable: `python scripts/generate_golden.py --all`
- [ ] Script is idempotent (running twice produces same output)
- [ ] Each function is <= 25 lines
- [ ] File is <= 250 lines total

**Checklist:**
- [ ] Create `scripts/` directory if needed
- [ ] Create `scripts/generate_golden.py`
- [ ] Create `tests/golden/.gitkeep`
- [ ] Implement profile discovery from `src/config-templates/`
- [ ] Implement per-profile golden file generation
- [ ] Add `--all`, `--profile`, and `--verify` CLI options (argparse, not click)
- [ ] Verify `python scripts/generate_golden.py --all`

**Check command:**
```bash
cd /Users/edercnj/workspaces/claude-environment && python scripts/generate_golden.py --all && ls tests/golden/
```

---

## G4 -- Unit Tests (Parallel, depends on G1+G2)

### T5: Unit tests for `FileDiff` and `VerificationResult` models

| Attribute | Value |
|-----------|-------|
| **Task ID** | G4-T5 |
| **Title** | Unit tests for FileDiff and VerificationResult dataclasses |
| **Layer** | tests |
| **Tier** | Junior |
| **Budget** | S (~60 lines) |
| **Group** | G4 (Unit Tests) |

**Files to modify:**
- `tests/test_models.py` -- add test classes for `FileDiff` and `VerificationResult`

**Dependencies:** G1-T1 (VerificationResult), G1-T2 (FileDiff)

**Description:**

Add unit tests for the new dataclasses to the existing `tests/test_models.py` file.

**Test cases:**

| Test Class | Test Method | Scenario | Expected |
|------------|-------------|----------|----------|
| `TestFileDiff` | `test_create_with_all_fields` | Construct with all 4 fields | All attributes accessible |
| `TestFileDiff` | `test_path_is_path_object` | Pass `Path` for `path` field | `isinstance(d.path, Path)` |
| `TestFileDiff` | `test_diff_stores_unified_diff_string` | Pass multiline diff string | `diff` field contains the string |
| `TestFileDiff` | `test_sizes_are_integers` | Pass int sizes | `python_size` and `reference_size` are ints |
| `TestVerificationResult` | `test_create_success_result` | `success=True`, empty lists | All fields correct |
| `TestVerificationResult` | `test_create_failure_result` | `success=False`, with mismatches | `mismatches` list non-empty |
| `TestVerificationResult` | `test_total_files_reflects_count` | `total_files=10` | Field stores 10 |
| `TestVerificationResult` | `test_missing_files_stores_paths` | Pass list of `Path` | `missing_files` accessible |
| `TestVerificationResult` | `test_extra_files_stores_paths` | Pass list of `Path` | `extra_files` accessible |

**Acceptance Criteria:**
- [ ] All 9 test cases pass
- [ ] Tests are added to existing `tests/test_models.py`
- [ ] Tests are independent (no execution order dependency)
- [ ] No mocking of domain logic

**Check command:**
```bash
cd /Users/edercnj/workspaces/claude-environment && python -m pytest tests/test_models.py -x -q --tb=short -k "FileDiff or VerificationResult"
```

---

### T6: Unit tests for `verify_output()` function

| Attribute | Value |
|-----------|-------|
| **Task ID** | G4-T6 |
| **Title** | Unit tests for verify_output with filesystem fixtures |
| **Layer** | tests |
| **Tier** | Mid |
| **Budget** | M (~150 lines) |
| **Group** | G4 (Unit Tests) |

**Files to create:**
- `tests/test_verifier.py` -- unit tests for all verifier functions

**Dependencies:** G2-T3 (verifier.py must exist)

**Description:**

Unit tests for `ia_dev_env/verifier.py`. Tests use `tmp_path` fixture to create filesystem structures for comparison.

**Test cases:**

| Test Class | Test Method | Scenario | Expected |
|------------|-------------|----------|----------|
| `TestVerifyOutput` | `test_identical_dirs_returns_success` | Two identical directory trees | `result.success is True`, zero mismatches |
| `TestVerifyOutput` | `test_identical_dirs_total_files_correct` | Two dirs with 3 files each | `result.total_files == 3` |
| `TestVerifyOutput` | `test_mismatch_detected_returns_failure` | One file differs by content | `result.success is False`, 1 mismatch |
| `TestVerifyOutput` | `test_mismatch_contains_diff_string` | One file differs | `result.mismatches[0].diff` contains unified diff |
| `TestVerifyOutput` | `test_mismatch_contains_file_sizes` | One file differs | `python_size` and `reference_size` populated |
| `TestVerifyOutput` | `test_missing_file_detected` | File in reference but not in python | `result.missing_files` contains path |
| `TestVerifyOutput` | `test_extra_file_detected` | File in python but not in reference | `result.extra_files` contains path |
| `TestVerifyOutput` | `test_nested_directories_compared` | Nested `sub/dir/file.txt` | Files at any depth compared |
| `TestVerifyOutput` | `test_empty_dirs_returns_success` | Both dirs empty | `result.success is True`, `total_files == 0` |
| `TestVerifyOutput` | `test_binary_file_mismatch_handled` | Binary files differ | Mismatch detected, diff message present |
| `TestVerifyOutput` | `test_whitespace_difference_detected` | Trailing whitespace differs | Detected as mismatch (byte-for-byte) |
| `TestCollectRelativePaths` | `test_returns_sorted_paths` | Dir with multiple files | Paths sorted alphabetically |
| `TestCollectRelativePaths` | `test_includes_nested_files` | Nested directory | Nested paths included |
| `TestCollectRelativePaths` | `test_empty_dir_returns_empty` | Empty directory | Empty list |

**Acceptance Criteria:**
- [ ] All 14 test cases pass
- [ ] Tests use `tmp_path` fixture (no manual temp dir management)
- [ ] Tests create real filesystem structures (files with known content)
- [ ] Tests are independent (no execution order dependency)
- [ ] Byte-for-byte precision verified (whitespace, line endings)
- [ ] No `sleep()` calls

**Check command:**
```bash
cd /Users/edercnj/workspaces/claude-environment && python -m pytest tests/test_verifier.py -x -q --tb=short
```

---

## G5 -- Parametrized Tests (depends on G3+G4)

### T7: Parametrized byte-for-byte tests across all config profiles

| Attribute | Value |
|-----------|-------|
| **Task ID** | G5-T7 |
| **Title** | Parametrized pytest tests for all 8 config profiles against golden files |
| **Layer** | tests |
| **Tier** | Senior |
| **Budget** | L (~120 lines) |
| **Group** | G5 (Parametrized Tests) |

**Files to create:**
- `tests/test_byte_for_byte.py` -- parametrized test suite

**Dependencies:** G3-T4 (golden files must exist), G4-T6 (verify_output tested)

**Description:**

Parametrized pytest suite that runs `run_pipeline()` for each config profile, then compares the output against pre-generated golden files using `verify_output()`.

**Parametrization:**

```python
CONFIG_PROFILES = [
    "go-gin",
    "java-quarkus",
    "java-spring",
    "kotlin-ktor",
    "python-click-cli",
    "python-fastapi",
    "rust-axum",
    "typescript-nestjs",
]

@pytest.fixture(params=CONFIG_PROFILES)
def profile_name(request):
    return request.param
```

**Test cases:**

| Test Class | Test Method | Scenario | Expected |
|------------|-------------|----------|----------|
| `TestByteForByte` | `test_pipeline_matches_golden_files` | Run pipeline for profile, verify vs golden | `result.success is True` |
| `TestByteForByte` | `test_no_missing_files` | Run pipeline for profile | `result.missing_files` is empty |
| `TestByteForByte` | `test_no_extra_files` | Run pipeline for profile | `result.extra_files` is empty |
| `TestByteForByte` | `test_pipeline_success_for_profile` | Run pipeline for profile | `pipeline_result.success is True` |
| `TestByteForByte` | `test_total_files_greater_than_zero` | Run pipeline for profile | `result.total_files > 0` |

**Test fixtures:**

```python
@pytest.fixture
def config_templates_dir():
    return Path(__file__).parent.parent / "src" / "config-templates"

@pytest.fixture
def golden_dir():
    return Path(__file__).parent / "golden"

@pytest.fixture
def src_dir():
    return Path(__file__).parent.parent / "src"
```

**Caching strategy:** Use a module-scoped fixture to cache pipeline output per profile (avoids re-running the pipeline for each test method within the same profile).

```python
@pytest.fixture(scope="module")
def pipeline_outputs():
    """Cache: profile_name -> (pipeline_result, output_dir_path)"""
    return {}
```

**Acceptance Criteria:**
- [ ] All 5 tests x 8 profiles = 40 test cases pass
- [ ] Tests are parametrized with `@pytest.fixture(params=CONFIG_PROFILES)`
- [ ] Each profile loads config from `src/config-templates/setup-config.<name>.yaml`
- [ ] Each profile compares against `tests/golden/<name>/`
- [ ] Pipeline output uses `tmp_path_factory` (no pollution of workspace)
- [ ] Test names include profile name in pytest output (e.g., `test_pipeline_matches_golden_files[java-quarkus]`)
- [ ] Golden files are a prerequisite (test skipped with clear message if missing)
- [ ] No `sleep()` calls

**Check command:**
```bash
cd /Users/edercnj/workspaces/claude-environment && python -m pytest tests/test_byte_for_byte.py -x -q --tb=short
```

---

## G6 -- Edge Cases and Performance (Parallel, depends on G5)

### T8: Edge case tests -- minimal config, optional fields, config v2

| Attribute | Value |
|-----------|-------|
| **Task ID** | G6-T8 |
| **Title** | Edge case tests for minimal config, optional fields, and v2 configs |
| **Layer** | tests |
| **Tier** | Mid |
| **Budget** | M (~100 lines) |
| **Group** | G6 (Edge Cases) |

**Files to create:**
- `tests/test_verification_edge_cases.py` -- edge case test suite

**Dependencies:** G5-T7 (parametrized suite established)

**Description:**

Tests for boundary conditions that the parametrized suite may not cover. These use synthetic configs rather than the standard profiles.

**Test cases:**

| Test Class | Test Method | Scenario | Expected |
|------------|-------------|----------|----------|
| `TestMinimalConfig` | `test_minimal_config_produces_output` | MINIMAL_PROJECT_DICT from conftest | Pipeline succeeds, files generated |
| `TestMinimalConfig` | `test_minimal_config_verifies_against_self` | Generate twice, compare | Identical output (idempotent) |
| `TestOptionalFields` | `test_config_with_no_database` | Config without data section | Pipeline succeeds |
| `TestOptionalFields` | `test_config_with_no_security` | Config without security section | Pipeline succeeds |
| `TestOptionalFields` | `test_config_with_no_infra` | Config without infrastructure section | Pipeline succeeds |
| `TestIdempotency` | `test_pipeline_is_idempotent` | Run pipeline twice for same config | `verify_output` returns success |
| `TestIdempotency` | `test_pipeline_byte_identical_across_runs` | Run pipeline twice | All files byte-identical |
| `TestEmptyReference` | `test_all_files_reported_as_extra` | Python output vs empty reference dir | `extra_files` contains all outputs |
| `TestEmptyOutput` | `test_all_files_reported_as_missing` | Empty output vs reference dir with files | `missing_files` contains all references |

**Acceptance Criteria:**
- [ ] All 9 test cases pass
- [ ] Tests use `tmp_path` for isolation
- [ ] Minimal config constructed from `MINIMAL_PROJECT_DICT` in conftest
- [ ] Idempotency verified via byte-for-byte comparison
- [ ] No reliance on golden files (these tests are self-contained)
- [ ] Tests are independent

**Check command:**
```bash
cd /Users/edercnj/workspaces/claude-environment && python -m pytest tests/test_verification_edge_cases.py -x -q --tb=short
```

---

### T9: Performance timing tests

| Attribute | Value |
|-----------|-------|
| **Task ID** | G6-T9 |
| **Title** | Performance timing tests -- < 5s per profile |
| **Layer** | tests |
| **Tier** | Mid |
| **Budget** | S (~60 lines) |
| **Group** | G6 (Performance) |

**Files to create:**
- `tests/test_verification_performance.py` -- performance test suite

**Dependencies:** G5-T7 (parametrized suite)

**Description:**

Performance tests that verify each config profile completes pipeline execution within the 5-second SLA defined in the story acceptance criteria.

**Test cases:**

| Test Class | Test Method | Scenario | Expected |
|------------|-------------|----------|----------|
| `TestPerformance` | `test_pipeline_under_five_seconds[profile]` | Run pipeline for each profile, measure wall time | `duration_ms < 5000` |
| `TestPerformance` | `test_verification_under_one_second[profile]` | Run verify_output for each profile | Verification completes in < 1000ms |
| `TestPerformance` | `test_total_suite_under_forty_seconds` | Sum of all profile durations | Total < 40s (5s x 8 profiles) |

**Parametrization:** Same `CONFIG_PROFILES` list as G5-T7.

**Timing approach:**
- Use `time.monotonic()` for wall-clock measurement (not `time.time()`)
- Also check `PipelineResult.duration_ms` field
- No `sleep()` -- measure actual execution time

**Acceptance Criteria:**
- [ ] All parametrized performance tests pass
- [ ] Each profile completes pipeline in < 5000ms
- [ ] Each profile completes verification in < 1000ms
- [ ] Total suite time < 40 seconds
- [ ] Uses `time.monotonic()` for timing
- [ ] No `sleep()` calls

**Check command:**
```bash
cd /Users/edercnj/workspaces/claude-environment && python -m pytest tests/test_verification_performance.py -x -q --tb=short -v
```

---

## G7 -- Integration (depends on G6)

### T10: Full integration verification -- end-to-end pipeline + verification

| Attribute | Value |
|-----------|-------|
| **Task ID** | G7-T10 |
| **Title** | Full E2E integration: pipeline + verify_output for all profiles |
| **Layer** | tests |
| **Tier** | Senior |
| **Budget** | M (~100 lines) |
| **Group** | G7 (Integration) |

**Files to create:**
- `tests/test_e2e_verification.py` -- full integration test suite

**Dependencies:** G6-T8, G6-T9 (all edge and perf tests green)

**Description:**

Final integration gate that exercises the complete flow: load config -> run pipeline -> verify against golden files -> assert zero differences. This test validates RULE-005 (byte-for-byte compatibility).

**Test cases:**

| Test Class | Test Method | Scenario | Expected |
|------------|-------------|----------|----------|
| `TestE2EVerification` | `test_full_flow_for_profile[profile]` | Load config, run pipeline, verify vs golden | `verification.success is True` |
| `TestE2EVerification` | `test_cli_generate_produces_verifiable_output[profile]` | Run via `CliRunner` with `generate -c`, then verify output | Verification succeeds |
| `TestE2EVerification` | `test_verification_result_summary_is_clean` | Run all profiles | Zero total mismatches across all profiles |
| `TestE2EVerification` | `test_golden_files_are_current` | Regenerate golden, compare with existing | No changes (golden files up to date) |

**Test flow for `test_full_flow_for_profile`:**

```python
def test_full_flow_for_profile(self, profile_name, tmp_path):
    # 1. Load config
    config_path = CONFIG_TEMPLATES / f"setup-config.{profile_name}.yaml"
    config = load_config(config_path)

    # 2. Run pipeline
    output_dir = tmp_path / "output"
    result = run_pipeline(config, SRC_DIR, output_dir)
    assert result.success

    # 3. Verify against golden
    golden_path = GOLDEN_DIR / profile_name
    verification = verify_output(output_dir, golden_path)
    assert verification.success, _format_failures(verification)
```

**Failure reporting helper:**

```python
def _format_failures(result: VerificationResult) -> str:
    """Format verification failures for readable pytest output."""
    lines = []
    for m in result.mismatches:
        lines.append(f"MISMATCH: {m.path} (python={m.python_size}B, ref={m.reference_size}B)")
        lines.append(m.diff[:500])  # Truncate long diffs
    for p in result.missing_files:
        lines.append(f"MISSING: {p}")
    for p in result.extra_files:
        lines.append(f"EXTRA: {p}")
    return "\n".join(lines)
```

**Acceptance Criteria:**
- [ ] All parametrized E2E tests pass for all 8 profiles
- [ ] CLI-based test uses `CliRunner` (not subprocess)
- [ ] Failure messages include file paths and truncated diffs
- [ ] Golden files existence is a prerequisite (skip with message if absent)
- [ ] Tests use `tmp_path` for output isolation
- [ ] Total E2E suite runs in < 60 seconds
- [ ] Zero byte-for-byte differences across all profiles (RULE-005)

**Check command:**
```bash
cd /Users/edercnj/workspaces/claude-environment && python -m pytest tests/test_e2e_verification.py -x -q --tb=long -v
```

---

## Summary

| Group | Tasks | New Files | Modified Files | Total Est. Lines |
|-------|-------|-----------|----------------|-----------------|
| G1 | 2 | 0 | `models.py` | ~30 |
| G2 | 1 | `verifier.py` | 0 | ~120 |
| G3 | 1 | `scripts/generate_golden.py`, `tests/golden/.gitkeep` | 0 | ~80 |
| G4 | 2 | `test_verifier.py` | `test_models.py` | ~210 |
| G5 | 1 | `test_byte_for_byte.py` | 0 | ~120 |
| G6 | 2 | `test_verification_edge_cases.py`, `test_verification_performance.py` | 0 | ~160 |
| G7 | 1 | `test_e2e_verification.py` | 0 | ~100 |
| **Total** | **10** | **6** | **2** | **~820** |

## Dependency Graph

```
G1-T1 (VerificationResult) ──┐
                               ├──> G2-T3 (verify_output) ──> G3-T4 (golden script) ──> G5-T7 (parametrized)
G1-T2 (FileDiff) ─────────────┤                          │                               │
                               │                          └──> G4-T6 (verifier tests) ────┤
                               └──> G4-T5 (model tests) ─────────────────────────────────┘
                                                                                           │
                                                                          G6-T8 (edge cases) ──┐
                                                                                                ├──> G7-T10 (E2E)
                                                                          G6-T9 (performance) ──┘
```

## File Inventory

### New Files (6)

| File | Group | Purpose |
|------|-------|---------|
| `ia_dev_env/verifier.py` | G2 | verify_output(), _collect_relative_paths(), _compare_files(), _generate_text_diff() |
| `scripts/generate_golden.py` | G3 | Golden file generation script for all 8 config profiles |
| `tests/test_verifier.py` | G4 | Unit tests for verify_output and helpers |
| `tests/test_byte_for_byte.py` | G5 | Parametrized byte-for-byte tests across all profiles |
| `tests/test_verification_edge_cases.py` | G6 | Edge case tests: minimal config, idempotency, empty dirs |
| `tests/test_verification_performance.py` | G6 | Performance timing tests: < 5s per profile |
| `tests/test_e2e_verification.py` | G7 | Full E2E integration verification |

### Modified Files (2)

| File | Group | Changes |
|------|-------|---------|
| `ia_dev_env/models.py` | G1 | Add `FileDiff` and `VerificationResult` dataclasses |
| `tests/test_models.py` | G4 | Add `TestFileDiff` and `TestVerificationResult` test classes |

### Generated Artifacts

| Path | Group | Description |
|------|-------|-------------|
| `tests/golden/<profile>/` | G3 | 8 directories of golden reference files (generated, not hand-written) |

## Full Validation Command

```bash
cd /Users/edercnj/workspaces/claude-environment && python scripts/generate_golden.py --all && python -m pytest tests/test_models.py tests/test_verifier.py tests/test_byte_for_byte.py tests/test_verification_edge_cases.py tests/test_verification_performance.py tests/test_e2e_verification.py -x -q --tb=short && python -m pytest tests/ --cov=ia_dev_env --cov-report=term-missing --cov-fail-under=95
```
