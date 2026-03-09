# Implementation Plan: STORY-010 -- Tests and End-to-End Verification

**Story:** STORY-010 -- Tests and End-to-End Verification
**Phase:** 5 (Verification)
**Blocked By:** STORY-009
**Blocks:** --
**Architecture:** library (hexagonal-lite for a CLI tool)
**Stack:** Python 3.9 + Click 8.1

---

## 1. Affected Layers and Components

| Layer | Affected? | Rationale |
|-------|-----------|-----------|
| domain/model | YES | New `VerificationResult` and `FileDiff` dataclasses in `models.py`. |
| domain/engine | NO | No new business logic engines. |
| domain/port | NO | No ports needed -- verifier is a pure function on file trees. |
| application | YES | New `verify_output()` function in `ia_dev_env/verifier.py` -- orchestrates recursive byte-for-byte comparison. |
| adapter/inbound (CLI) | NO | No CLI changes needed. Verification is invoked from tests only. |
| adapter/outbound | NO | File operations use stdlib `pathlib` only. |
| assembler | NO | Existing assemblers are consumers of the verification, not modified. |
| tests | YES | Major: parametrized byte-for-byte tests, golden file generation, performance tests, edge case tests. |
| config | NO | No changes to config loading or YAML schema. |

---

## 2. New Classes/Interfaces to Create

### 2.1 VerificationResult Dataclass (`ia_dev_env/models.py` -- extend existing)

| Field | Type | Description |
|-------|------|-------------|
| `success` | `bool` | All files are identical between python output and reference |
| `total_files` | `int` | Total number of files compared |
| `mismatches` | `List[FileDiff]` | Files with byte-level differences |
| `missing_files` | `List[Path]` | Files present in reference but absent from python output |
| `extra_files` | `List[Path]` | Files present in python output but absent from reference |

```python
@dataclass
class VerificationResult:
    success: bool
    total_files: int
    mismatches: List[FileDiff]
    missing_files: List[Path]
    extra_files: List[Path]
```

Pure domain model. Zero framework dependencies. Located in `models.py` alongside `PipelineResult`.

### 2.2 FileDiff Dataclass (`ia_dev_env/models.py` -- extend existing)

| Field | Type | Description |
|-------|------|-------------|
| `path` | `Path` | Relative path of the file within the output tree |
| `diff` | `str` | Unified diff string showing the differences |
| `python_size` | `int` | Size in bytes of the python-generated file |
| `reference_size` | `int` | Size in bytes of the reference (golden) file |

```python
@dataclass
class FileDiff:
    path: Path
    diff: str
    python_size: int
    reference_size: int
```

Pure value object. Must be defined before `VerificationResult` in the file (forward reference).

### 2.3 Verifier Module (`ia_dev_env/verifier.py` -- new file)

Single public function:

| Function | Signature | Description |
|----------|-----------|-------------|
| `verify_output` | `(python_dir: Path, reference_dir: Path) -> VerificationResult` | Recursively compares two directory trees byte-for-byte. |

**Algorithm:**

1. Walk `reference_dir` recursively, collect all relative file paths into `reference_files: Set[Path]`.
2. Walk `python_dir` recursively, collect all relative file paths into `python_files: Set[Path]`.
3. Compute `missing_files = reference_files - python_files` (in reference, not in python).
4. Compute `extra_files = python_files - reference_files` (in python, not in reference).
5. Compute `common_files = reference_files & python_files`.
6. For each file in `common_files`:
   a. Read both files as bytes.
   b. If bytes differ, decode as UTF-8 (with fallback), compute unified diff, create `FileDiff`.
7. Compute `total_files = len(reference_files | python_files)`.
8. `success = len(mismatches) == 0 and len(missing_files) == 0 and len(extra_files) == 0`.
9. Return `VerificationResult`.

**Internal helpers (private):**

| Function | Signature | Description |
|----------|-----------|-------------|
| `_collect_files` | `(directory: Path) -> Set[Path]` | Recursively collect relative paths of all files. |
| `_compute_diff` | `(python_path: Path, reference_path: Path, relative: Path) -> Optional[FileDiff]` | Compare two files; return `FileDiff` if different, `None` if identical. |

**Constraints:**
- Max function length: 25 lines.
- Must handle binary files gracefully (diff shows `<binary files differ>`).
- Must sort results for deterministic output.
- Must validate that both input paths exist and are directories; raise `ValueError` with context if not.

### 2.4 Golden File Generation Script (`tests/generate_golden.py` -- new file)

A standalone script (not part of the library) that generates reference output for all 8 config profiles.

| Function | Signature | Description |
|----------|-----------|-------------|
| `generate_golden_files` | `(config_dir: Path, src_dir: Path, output_base: Path) -> None` | For each config profile, run the Python pipeline and store output in `tests/golden/<profile_name>/`. |
| `_profile_name_from_path` | `(config_path: Path) -> str` | Extract profile name from `setup-config.<name>.yaml`. |

**Behavior:**

1. Discover all `setup-config.*.yaml` files in `config_dir`.
2. For each config file:
   a. Load config via `load_config(path)`.
   b. Run `run_pipeline(config, src_dir, output_dir=golden_base/<profile_name>/)`.
   c. Log success/failure per profile.
3. Print summary: N profiles generated, M failures.

**Invocation methods:**
- `python -m tests.generate_golden` (direct execution)
- `pytest --generate-golden` (via conftest marker, skip-if-not-requested)

### 2.5 Test Fixtures (`tests/conftest.py` -- extend existing)

New fixtures to add:

| Fixture | Scope | Description |
|---------|-------|-------------|
| `golden_dir` | `session` | Returns `Path` to `tests/golden/` directory. |
| `src_dir` | `session` | Returns `Path` to `src/` directory (config-templates, skills, etc.). |
| `config_profiles` | `session` | Returns list of `(profile_name, config_path)` tuples for all 8 profiles. |
| `pipeline_output` | `function` | Factory fixture: given a profile name, runs the pipeline in a temp dir, returns the output path. |

### 2.6 Parametrized Test Module (`tests/test_verification.py` -- new file)

Test classes/functions:

| Test | Parametrized By | Description |
|------|----------------|-------------|
| `test_byte_for_byte_match` | 8 config profiles | Run pipeline, verify against golden files. Assert `result.success is True`. |
| `test_detect_mismatch` | N/A | Synthetic test: modify a golden file, verify mismatch detected. |
| `test_detect_missing_file` | N/A | Remove a file from python output, verify `missing_files` populated. |
| `test_detect_extra_file` | N/A | Add an extra file to python output, verify `extra_files` populated. |
| `test_empty_directories` | N/A | Both dirs empty: success=True, total_files=0. |
| `test_performance_per_profile` | 8 config profiles | Assert pipeline execution < 5s per profile. |

### 2.7 Verifier Unit Tests (`tests/test_verifier.py` -- new file)

| Test | Description |
|------|-------------|
| `test_verify_identical_trees` | Two identical directory trees -> success=True. |
| `test_verify_different_content` | One file differs -> success=False, mismatches has 1 entry with diff. |
| `test_verify_missing_file` | File in reference not in python -> missing_files populated. |
| `test_verify_extra_file` | File in python not in reference -> extra_files populated. |
| `test_verify_mixed_issues` | Missing + extra + mismatch simultaneously. |
| `test_verify_nested_directories` | Files in subdirectories compared correctly. |
| `test_verify_binary_files` | Binary file difference produces graceful diff message. |
| `test_verify_invalid_python_dir` | Non-existent python_dir raises ValueError. |
| `test_verify_invalid_reference_dir` | Non-existent reference_dir raises ValueError. |
| `test_verify_empty_vs_populated` | Empty python dir vs populated reference -> all missing. |
| `test_total_files_counts_union` | total_files = union of both file sets. |

---

## 3. Existing Classes to Modify

| File | Change | Reason |
|------|--------|--------|
| `ia_dev_env/models.py` | Add `FileDiff` and `VerificationResult` dataclasses | New domain models for verification output |
| `tests/conftest.py` | Add `golden_dir`, `src_dir`, `config_profiles`, `pipeline_output` fixtures | Shared fixtures for verification tests |

No other existing production code requires modification. The verifier is a new module that operates on directory trees produced by the existing pipeline.

---

## 4. Dependency Direction Validation

```
tests/test_verification.py (test code)
    --> ia_dev_env/verifier.py (application layer)
        --> ia_dev_env/models.py (VerificationResult, FileDiff)
        --> stdlib only (pathlib, difflib, os)
    --> ia_dev_env/assembler/__init__.py (run_pipeline)
    --> ia_dev_env/config.py (load_config)

tests/test_verifier.py (unit tests)
    --> ia_dev_env/verifier.py
        --> ia_dev_env/models.py
        --> stdlib only

tests/generate_golden.py (script)
    --> ia_dev_env/assembler/__init__.py (run_pipeline)
    --> ia_dev_env/config.py (load_config)
    --> ia_dev_env/models.py (ProjectConfig)

ia_dev_env/verifier.py (new module)
    --> ia_dev_env/models.py (FileDiff, VerificationResult)
    --> stdlib only (pathlib, difflib, os)

ia_dev_env/models.py (extended)
    --> stdlib only (dataclasses, pathlib)
```

All dependencies point inward:
- Tests depend on application layer (`verifier`) and domain (`models`)
- Application layer (`verifier.py`) depends only on domain models and stdlib
- Domain (`models.py`) depends only on stdlib
- No circular dependencies
- No domain -> adapter dependency
- `verifier.py` has zero framework dependencies (no Click, no YAML)

---

## 5. Integration Points

| Integration | Direction | Description |
|-------------|-----------|-------------|
| `run_pipeline()` | test -> assembler | Execute full pipeline to produce python output for comparison |
| `load_config()` | test -> config | Load config profiles from YAML for parametrized tests |
| `verify_output()` | test -> verifier | Compare python output against golden files |
| `VerificationResult` | verifier -> test | Return value asserted in tests |
| Golden files on disk | generate_golden -> tests/golden/ | Pre-generated reference output stored in repository |

**Test execution flow:**

```
pytest collects test_byte_for_byte_match[java-quarkus]
  --> load_config("src/config-templates/setup-config.java-quarkus.yaml")
  --> run_pipeline(config, src_dir, tmp_output_dir)
  --> verify_output(tmp_output_dir, "tests/golden/java-quarkus/")
  --> assert result.success is True
```

---

## 6. Database Changes

N/A -- This is a CLI tool with no database.

---

## 7. API Changes

N/A -- This is a CLI tool with no HTTP/gRPC API.

No CLI interface changes in this story. The verifier is invoked only from tests and the golden file generation script.

---

## 8. Event Changes

N/A -- No event-driven architecture.

---

## 9. Configuration Changes

No new config files or environment variables.

**New test infrastructure:**

| Path | Type | Description |
|------|------|-------------|
| `tests/golden/` | Directory | Root for golden file storage, one subdirectory per config profile |
| `tests/golden/<profile>/` | Directory | Reference output for a specific config profile (e.g., `java-quarkus`, `python-fastapi`) |
| `tests/generate_golden.py` | Script | Generates/regenerates golden files |

**Config profiles (8 total, all in `src/config-templates/`):**

1. `setup-config.java-quarkus.yaml`
2. `setup-config.java-spring.yaml`
3. `setup-config.go-gin.yaml`
4. `setup-config.kotlin-ktor.yaml`
5. `setup-config.python-fastapi.yaml`
6. `setup-config.rust-axum.yaml`
7. `setup-config.typescript-nestjs.yaml`
8. `setup-config.python-click-cli.yaml`

**Note:** The story mentions 7 profiles but 8 exist on disk. The plan covers all 8.

**pytest marker for golden generation:**

```python
# In conftest.py or pytest plugin
def pytest_addoption(parser):
    parser.addoption("--generate-golden", action="store_true", default=False)
```

---

## 10. Risk Assessment

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Golden files become stale after assembler changes | HIGH | HIGH | Golden files must be regenerated after any assembler modification. Add CI check that runs `generate_golden` and compares with committed golden files. Document regeneration in README. |
| Golden file size bloats the repository | MEDIUM | MEDIUM | Golden files are text (YAML, MD, JSON). Expected total size < 5MB for 8 profiles. If too large, use `.gitattributes` for LFS or generate on-the-fly in CI. |
| Platform-dependent line endings (CRLF vs LF) | MEDIUM | MEDIUM | Use binary mode reads (`rb`) for byte-for-byte comparison. Ensure golden files are committed with LF via `.gitattributes`. |
| Diff output for large files overwhelms test output | LOW | MEDIUM | Truncate diff output in `FileDiff.diff` to a configurable max length (e.g., 2000 chars). Show file size comparison as summary. |
| Pipeline non-determinism (timestamps, random IDs) | HIGH | LOW | Current assemblers produce deterministic output (no timestamps or random values). If future assemblers add non-deterministic content, the verifier will catch it as a regression. |
| Performance test flakiness (5s threshold) | MEDIUM | MEDIUM | Use a generous margin (5s is already generous for file generation). Run performance tests with `@pytest.mark.slow` marker so they can be excluded in rapid iteration. |
| `generate_golden.py` depends on working pipeline (STORY-009) | HIGH | LOW | STORY-009 is a hard dependency. Verify pipeline works for all 8 profiles before generating golden files. |
| Binary files in output tree | LOW | LOW | Verifier handles binary files gracefully with a `<binary files differ>` message instead of attempting text diff. |

---

## Implementation Order

Following the inner-to-outer rule:

1. **Domain models** -- Add `FileDiff` and `VerificationResult` dataclasses to `models.py`
2. **Verifier module** -- Create `ia_dev_env/verifier.py` with `verify_output()` function
3. **Unit tests for verifier** -- Create `tests/test_verifier.py` with synthetic directory comparisons
4. **Golden file generator** -- Create `tests/generate_golden.py` script
5. **Test fixtures** -- Extend `tests/conftest.py` with golden/pipeline fixtures
6. **Generate golden files** -- Run generator for all 8 profiles, commit to `tests/golden/`
7. **Parametrized verification tests** -- Create `tests/test_verification.py` with byte-for-byte tests
8. **Performance tests** -- Add timing assertions to parametrized tests
9. **Edge case tests** -- Empty dirs, minimal config, config v2 compatibility
10. **Coverage verification** -- Ensure project-wide coverage meets >= 95% line, >= 90% branch

---

## File Summary

### New Files

| File | Description |
|------|-------------|
| `ia_dev_env/verifier.py` | Byte-for-byte directory comparison: `verify_output()`, `_collect_files()`, `_compute_diff()` |
| `tests/test_verifier.py` | Unit tests for `verifier.py` with synthetic directory fixtures |
| `tests/test_verification.py` | Parametrized end-to-end tests: byte-for-byte match for all 8 config profiles |
| `tests/generate_golden.py` | Script to generate/regenerate golden reference files for all profiles |
| `tests/golden/` | Directory tree of reference output (one subdirectory per config profile) |

### Modified Files

| File | Change |
|------|--------|
| `ia_dev_env/models.py` | Add `FileDiff` and `VerificationResult` dataclasses (~25 lines) |
| `tests/conftest.py` | Add `golden_dir`, `src_dir`, `config_profiles`, `pipeline_output` fixtures (~40 lines) |

### Unchanged Files (verified)

| File | Reason |
|------|--------|
| `ia_dev_env/assembler/__init__.py` | `run_pipeline()` used as-is by tests |
| `ia_dev_env/__main__.py` | No CLI changes for verification story |
| `ia_dev_env/config.py` | `load_config()` used as-is by golden generator |
| `ia_dev_env/exceptions.py` | No new exceptions needed |
| `ia_dev_env/template_engine.py` | Used indirectly through pipeline, unchanged |
| `ia_dev_env/utils.py` | Used indirectly through pipeline, unchanged |
| All `ia_dev_env/assembler/*.py` | Assemblers are consumers, not modified |
| All `ia_dev_env/domain/*.py` | Domain logic unchanged |
| `pyproject.toml` | No new dependencies or entry points |
