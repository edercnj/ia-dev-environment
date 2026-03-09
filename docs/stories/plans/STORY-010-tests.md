# Test Plan: STORY-010 -- Tests and Verification End-to-End

**Story:** STORY-010 -- Tests and Verification End-to-End
**Date:** 2026-03-01
**Framework:** pytest + pytest-cov
**Coverage Target:** >= 95% Line, >= 90% Branch

---

## 1. Test File Structure

```
tests/
├── unit/
│   ├── __init__.py
│   ├── test_verification_result.py     # VerificationResult dataclass
│   ├── test_file_diff.py              # FileDiff dataclass
│   └── test_verifier.py              # verify_output() function
├── integration/
│   ├── __init__.py
│   └── test_golden_verification.py    # Parametrized golden file comparison
├── performance/
│   ├── __init__.py
│   └── test_pipeline_perf.py         # Per-profile timing assertions
├── edge/
│   ├── __init__.py
│   └── test_verifier_edge.py         # Edge cases for verifier
└── conftest.py                        # Shared fixtures (extend existing)
```

---

## 2. Unit Tests

### 2.1 `test_verification_result.py` -- VerificationResult Dataclass

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_init_stores_all_fields` | Construct with all fields | All attributes accessible and correct |
| 2 | `test_success_true_when_no_mismatches` | `success=True, mismatches=[], missing_files=[], extra_files=[]` | `result.success is True` |
| 3 | `test_success_false_when_mismatches_exist` | `success=False, mismatches=[FileDiff(...)]` | `result.success is False` |
| 4 | `test_success_false_when_missing_files_exist` | `success=False, missing_files=[Path("a.txt")]` | `result.success is False` |
| 5 | `test_success_false_when_extra_files_exist` | `success=False, extra_files=[Path("b.txt")]` | `result.success is False` |
| 6 | `test_total_files_zero` | `total_files=0` | Value is `0` |
| 7 | `test_total_files_positive` | `total_files=42` | Value is `42` |
| 8 | `test_mismatches_is_list_of_file_diff` | Populate with `FileDiff` instances | Type check passes |
| 9 | `test_missing_files_is_list_of_path` | Populate with `Path` instances | Type check passes |
| 10 | `test_extra_files_is_list_of_path` | Populate with `Path` instances | Type check passes |
| 11 | `test_empty_result_all_lists_empty` | Default empty lists | All list fields are `[]` |
| 12 | `test_equality_two_identical_results` | Two identical instances | `result1 == result2` |

### 2.2 `test_file_diff.py` -- FileDiff Dataclass

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_init_stores_all_fields` | Construct with path, diff, python_size, reference_size | All attributes accessible |
| 2 | `test_path_is_path_object` | `path=Path("rules/01.md")` | `isinstance(result.path, Path)` |
| 3 | `test_diff_contains_unified_diff` | `diff="--- a\n+++ b\n@@ ...\n-old\n+new"` | String stored correctly |
| 4 | `test_python_size_stores_int` | `python_size=1024` | Value is `1024` |
| 5 | `test_reference_size_stores_int` | `reference_size=1020` | Value is `1020` |
| 6 | `test_diff_empty_string` | `diff=""` | Empty string stored |
| 7 | `test_sizes_zero` | Both sizes `0` | Both are `0` |
| 8 | `test_equality_two_identical_diffs` | Two identical instances | `diff1 == diff2` |

### 2.3 `test_verifier.py` -- `verify_output()` Function

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_identical_dirs_returns_success_true` | Two dirs with identical files | `result.success is True`, `mismatches == []` |
| 2 | `test_identical_dirs_total_files_correct` | Two dirs with 3 identical files | `result.total_files == 3` |
| 3 | `test_content_mismatch_returns_success_false` | One file differs by 1 byte | `result.success is False` |
| 4 | `test_content_mismatch_reports_file_in_mismatches` | One file differs | `len(result.mismatches) == 1`, path matches |
| 5 | `test_content_mismatch_includes_unified_diff` | File content differs | `result.mismatches[0].diff` contains `---` and `+++` |
| 6 | `test_content_mismatch_includes_sizes` | Files differ (100 vs 105 bytes) | `python_size == 100`, `reference_size == 105` |
| 7 | `test_missing_file_detected` | Reference has file not in python_dir | `result.missing_files` contains the path |
| 8 | `test_missing_file_sets_success_false` | Missing file present | `result.success is False` |
| 9 | `test_extra_file_detected` | Python dir has file not in reference | `result.extra_files` contains the path |
| 10 | `test_extra_file_sets_success_false` | Extra file present | `result.success is False` |
| 11 | `test_multiple_mismatches_all_reported` | 3 files differ | `len(result.mismatches) == 3` |
| 12 | `test_missing_and_extra_combined` | 1 missing + 1 extra | Both lists populated, `success is False` |
| 13 | `test_nested_directory_comparison` | Identical nested structure `a/b/c.txt` | `result.success is True` |
| 14 | `test_nested_directory_missing_file` | Reference has `a/b/c.txt`, python lacks `a/b/c.txt` | `missing_files` contains `Path("a/b/c.txt")` |
| 15 | `test_nested_directory_content_diff` | `a/b/c.txt` differs | `mismatches[0].path == Path("a/b/c.txt")` |
| 16 | `test_whitespace_only_difference_detected` | Trailing whitespace difference | `success is False`, mismatch reported |
| 17 | `test_newline_difference_detected` | LF vs CRLF or trailing newline diff | `success is False` |
| 18 | `test_paths_are_relative_in_results` | Compare dirs with nested files | All paths in results are relative (no absolute) |
| 19 | `test_returns_verification_result_type` | Any valid comparison | `isinstance(result, VerificationResult)` |
| 20 | `test_both_dirs_empty_returns_success` | Both dirs empty | `success is True`, `total_files == 0` |

**Branch coverage targets:**
- Identical files path
- Content mismatch path (with diff generation)
- Missing files path
- Extra files path
- Nested directory recursion
- Empty directories path
- Binary file handling path

---

## 3. Parametrized Tests -- Config Profile Verification

### 3.1 `test_golden_verification.py` -- Byte-for-Byte Across All Profiles

**Config profiles (8 total):**

| Profile | Config File |
|---------|-------------|
| `java-quarkus` | `setup-config.java-quarkus.yaml` |
| `java-spring` | `setup-config.java-spring.yaml` |
| `go-gin` | `setup-config.go-gin.yaml` |
| `kotlin-ktor` | `setup-config.kotlin-ktor.yaml` |
| `python-fastapi` | `setup-config.python-fastapi.yaml` |
| `rust-axum` | `setup-config.rust-axum.yaml` |
| `typescript-nestjs` | `setup-config.typescript-nestjs.yaml` |
| `python-click-cli` | `setup-config.python-click-cli.yaml` |

```python
CONFIG_PROFILES = [
    "java-quarkus",
    "java-spring",
    "go-gin",
    "kotlin-ktor",
    "python-fastapi",
    "rust-axum",
    "typescript-nestjs",
    "python-click-cli",
]

@pytest.mark.parametrize("profile", CONFIG_PROFILES)
def test_byte_for_byte_matches_golden(profile, tmp_path, src_dir):
    ...
```

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_byte_for_byte_matches_golden[java-quarkus]` | Run pipeline, compare to golden | `result.success is True` |
| 2 | `test_byte_for_byte_matches_golden[java-spring]` | Run pipeline, compare to golden | `result.success is True` |
| 3 | `test_byte_for_byte_matches_golden[go-gin]` | Run pipeline, compare to golden | `result.success is True` |
| 4 | `test_byte_for_byte_matches_golden[kotlin-ktor]` | Run pipeline, compare to golden | `result.success is True` |
| 5 | `test_byte_for_byte_matches_golden[python-fastapi]` | Run pipeline, compare to golden | `result.success is True` |
| 6 | `test_byte_for_byte_matches_golden[rust-axum]` | Run pipeline, compare to golden | `result.success is True` |
| 7 | `test_byte_for_byte_matches_golden[typescript-nestjs]` | Run pipeline, compare to golden | `result.success is True` |
| 8 | `test_byte_for_byte_matches_golden[python-click-cli]` | Run pipeline, compare to golden | `result.success is True` |
| 9 | `test_no_missing_files[<profile>]` | Per profile | `result.missing_files == []` |
| 10 | `test_no_extra_files[<profile>]` | Per profile | `result.extra_files == []` |
| 11 | `test_total_files_matches_golden[<profile>]` | Per profile | `result.total_files` matches golden file count |

### 3.2 Golden File Generation Verification

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_golden_dir_exists_for_profile[<profile>]` | Check `tests/golden/<profile>/` exists | Directory exists and is non-empty |
| 2 | `test_golden_files_are_not_empty[<profile>]` | Check each golden file has content | All files have `size > 0` |
| 3 | `test_golden_dir_has_expected_structure[<profile>]` | Check `.claude/` subdir exists in golden | `.claude/rules/` and `.claude/` dirs present |

---

## 4. Performance Tests

### 4.1 `test_pipeline_perf.py` -- Execution Timing

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_pipeline_execution_under_5s[java-quarkus]` | Time full pipeline execution | `duration_ms < 5000` |
| 2 | `test_pipeline_execution_under_5s[java-spring]` | Time full pipeline execution | `duration_ms < 5000` |
| 3 | `test_pipeline_execution_under_5s[go-gin]` | Time full pipeline execution | `duration_ms < 5000` |
| 4 | `test_pipeline_execution_under_5s[kotlin-ktor]` | Time full pipeline execution | `duration_ms < 5000` |
| 5 | `test_pipeline_execution_under_5s[python-fastapi]` | Time full pipeline execution | `duration_ms < 5000` |
| 6 | `test_pipeline_execution_under_5s[rust-axum]` | Time full pipeline execution | `duration_ms < 5000` |
| 7 | `test_pipeline_execution_under_5s[typescript-nestjs]` | Time full pipeline execution | `duration_ms < 5000` |
| 8 | `test_pipeline_execution_under_5s[python-click-cli]` | Time full pipeline execution | `duration_ms < 5000` |
| 9 | `test_total_suite_execution_under_40s` | Time all 8 profiles sequentially | Total < 40s (8 x 5s) |
| 10 | `test_verification_overhead_under_500ms[<profile>]` | Time `verify_output()` alone | `< 500ms` |

```python
PERF_THRESHOLD_MS = 5000

@pytest.mark.parametrize("profile", CONFIG_PROFILES)
def test_pipeline_execution_under_5s(profile, tmp_path, src_dir):
    start = time.monotonic()
    result = run_pipeline(config, src_dir, tmp_path / profile)
    elapsed_ms = (time.monotonic() - start) * 1000
    assert elapsed_ms < PERF_THRESHOLD_MS
    assert result.success is True
```

---

## 5. Edge Case Tests

### 5.1 `test_verifier_edge.py` -- Edge Cases for verify_output()

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_empty_python_dir_all_files_missing` | Python dir empty, reference has 5 files | `missing_files` has 5 entries, `success is False` |
| 2 | `test_empty_reference_dir_all_files_extra` | Reference empty, python dir has 5 files | `extra_files` has 5 entries, `success is False` |
| 3 | `test_both_dirs_empty_success` | Both dirs empty | `success is True`, `total_files == 0` |
| 4 | `test_single_file_identical` | One file in each dir, identical content | `success is True`, `total_files == 1` |
| 5 | `test_single_file_differs` | One file in each dir, content differs | `success is False`, 1 mismatch |
| 6 | `test_binary_file_identical` | Binary file (bytes 0x00-0xFF) in both dirs | `success is True` |
| 7 | `test_binary_file_differs` | Binary file differs by 1 byte | `success is False`, mismatch reported |
| 8 | `test_large_file_comparison` | 1MB identical files | `success is True`, completes in < 1s |
| 9 | `test_deeply_nested_structure` | Files at depth 10 (`a/b/c/d/e/f/g/h/i/j/file.txt`) | Comparison traverses full depth |
| 10 | `test_symlink_in_directory_handled` | Symlink in python_dir pointing to file | Handled gracefully (follows or skips) |
| 11 | `test_hidden_files_included` | `.hidden` files in both dirs | Compared like regular files |
| 12 | `test_unicode_filename_handled` | File named with unicode chars | Comparison works correctly |
| 13 | `test_empty_file_vs_nonempty` | Python has 0-byte file, reference has content | `success is False`, mismatch with sizes 0 vs N |
| 14 | `test_identical_empty_files` | Both dirs have same 0-byte file | `success is True` |
| 15 | `test_permission_preserved_files_compared` | Files with restricted permissions (readable) | Comparison succeeds |
| 16 | `test_nonexistent_python_dir_raises` | `python_dir` does not exist | Raises `FileNotFoundError` or equivalent |
| 17 | `test_nonexistent_reference_dir_raises` | `reference_dir` does not exist | Raises `FileNotFoundError` or equivalent |
| 18 | `test_file_in_python_dir_in_reference_is_dir` | `python_dir/x` is a file, `reference_dir/x` is a directory | Handled gracefully (reported as mismatch or error) |

---

## 6. Fixtures Required

### 6.1 New Fixtures

| Fixture | Type | Description |
|---------|------|-------------|
| `golden_dir` | `pytest.fixture` | Returns `Path("tests/golden/")` base directory |
| `golden_profile_dir` | `pytest.fixture` (parametrized) | Returns `tests/golden/<profile>/` for current profile |
| `src_dir` | `pytest.fixture` | Returns the real `src/` directory path |
| `config_for_profile` | `pytest.fixture` | Factory: loads and parses config YAML for a given profile name |
| `identical_dirs` | `pytest.fixture` | Creates two tmp dirs with identical file trees |
| `mismatched_dirs` | `pytest.fixture` | Creates two tmp dirs with known content differences |
| `missing_file_dirs` | `pytest.fixture` | Creates reference dir with extra file not in python dir |
| `extra_file_dirs` | `pytest.fixture` | Creates python dir with extra file not in reference dir |
| `binary_file_dirs` | `pytest.fixture` | Creates dirs with identical/different binary files |
| `nested_dirs` | `pytest.fixture` | Creates dirs with deeply nested identical structure |

### 6.2 Existing Fixtures (reused from `tests/conftest.py`)

- `full_project_dict` -- deep copy of `FULL_PROJECT_DICT`
- `minimal_project_dict` -- deep copy of `MINIMAL_PROJECT_DICT`
- `create_project_config` -- factory fixture
- `fixtures_dir` -- path to `tests/fixtures/`
- `valid_v3_path` -- path to valid V3 YAML fixture

### 6.3 Fixture Helpers

```python
def create_file_tree(base: Path, files: dict) -> None:
    """Create a file tree from a dict of {relative_path: content}."""
    for rel_path, content in files.items():
        full_path = base / rel_path
        full_path.parent.mkdir(parents=True, exist_ok=True)
        if isinstance(content, bytes):
            full_path.write_bytes(content)
        else:
            full_path.write_text(content, encoding="utf-8")
```

---

## 7. Mocking Strategy

| Component | Mock? | Rationale |
|-----------|-------|-----------|
| `VerificationResult` / `FileDiff` | NEVER | Real domain dataclasses -- test actual behavior |
| `verify_output()` (in golden tests) | NO | Must use real verifier for byte-for-byte guarantee |
| `run_pipeline()` (in perf tests) | NO | Must time real execution |
| `run_pipeline()` (in verifier unit tests) | YES | Isolate verifier from pipeline; use pre-built dirs |
| File system (in unit tests) | NO | Use `tmp_path` with real files for accurate comparison |
| `difflib.unified_diff` | NO | Part of verifier's core logic, do not mock |
| `os.walk` / `Path.rglob` | NO (normal) / YES (error paths) | Mock only to simulate permission errors |

---

## 8. Coverage Estimation

| File | Lines (est.) | Line Coverage Target | Branch Coverage Target | Key Branches |
|------|-------------|---------------------|----------------------|--------------|
| `ia_dev_env/verifier.py` | ~80 | 98% | 95% | match/mismatch, missing/extra, binary/text, empty dirs, nested recursion |
| `ia_dev_env/models.py` (VerificationResult, FileDiff) | ~25 | 100% | 100% | No branches (dataclasses) |
| Golden generation script | ~30 | 95% | 90% | Per-profile generation, error handling |

**Aggregate estimate:** ~135 new lines of production code, target 97% line coverage, 93% branch coverage.

---

## 9. Test Execution Order

Tests are independent and can run in any order. No shared mutable state.

**Recommended development order (inner-to-outer):**

1. `tests/unit/test_file_diff.py` -- FileDiff dataclass (simplest, no deps)
2. `tests/unit/test_verification_result.py` -- VerificationResult dataclass
3. `tests/unit/test_verifier.py` -- verify_output() core logic
4. `tests/edge/test_verifier_edge.py` -- edge cases
5. `tests/integration/test_golden_verification.py` -- parametrized golden comparison
6. `tests/performance/test_pipeline_perf.py` -- timing assertions

---

## 10. Test Count Summary

| Category | Count |
|----------|-------|
| Unit: `test_verification_result.py` | 12 |
| Unit: `test_file_diff.py` | 8 |
| Unit: `test_verifier.py` | 20 |
| Integration: `test_golden_verification.py` (parametrized x8 profiles) | 27 |
| Performance: `test_pipeline_perf.py` (parametrized x8 profiles) | 18 |
| Edge: `test_verifier_edge.py` | 18 |
| **Total** | **~103** |

Note: Parametrized tests count as one test definition but expand to N executions (one per profile). The count above reflects unique test functions; actual pytest node count will be higher (~140+) due to parametrization across 8 profiles.

---

## 11. Acceptance Criteria Traceability

| Gherkin Scenario (from STORY-010) | Test(s) Covering It |
|----------------------------------|---------------------|
| Successful verification for java-quarkus | `test_byte_for_byte_matches_golden[java-quarkus]` |
| Detect content difference in file | `test_content_mismatch_returns_success_false`, `test_content_mismatch_includes_unified_diff` |
| Detect missing file | `test_missing_file_detected`, `test_missing_file_sets_success_false` |
| Parametrized tests for all profiles | `test_byte_for_byte_matches_golden[<all 8>]` |
| Performance < 5s per profile | `test_pipeline_execution_under_5s[<all 8>]` |

---

## 12. Golden File Management

### 12.1 Generation

```bash
# Generate golden files for all profiles
make golden

# Or via pytest marker
pytest --generate-golden

# Or manually per profile
python -m ia_dev_env.golden_gen --profile java-quarkus --output tests/golden/java-quarkus/
```

### 12.2 Storage Convention

```
tests/golden/
├── java-quarkus/
│   └── .claude/
│       ├── rules/
│       ├── skills/
│       ├── agents/
│       ├── hooks.json
│       ├── settings.json
│       └── README.md
├── java-spring/
│   └── .claude/ ...
├── go-gin/
│   └── .claude/ ...
├── kotlin-ktor/
│   └── .claude/ ...
├── python-fastapi/
│   └── .claude/ ...
├── rust-axum/
│   └── .claude/ ...
├── typescript-nestjs/
│   └── .claude/ ...
└── python-click-cli/
    └── .claude/ ...
```

### 12.3 Regeneration Rules

- Golden files MUST be regenerated when `setup.sh` changes
- Golden files MUST be committed to version control
- CI must fail if golden files are stale (compare against fresh generation)

---

## 13. pytest Markers

```python
# conftest.py or pyproject.toml
markers = [
    "golden: golden file comparison tests (requires generated golden files)",
    "perf: performance timing tests",
    "edge: edge case tests",
]
```

```bash
# Run only golden tests
pytest -m golden

# Run only performance tests
pytest -m perf

# Run everything except performance
pytest -m "not perf"
```

---

## 14. Anti-Patterns to Avoid

- Do NOT mock `VerificationResult` or `FileDiff` -- use real dataclass instances
- Do NOT mock file I/O in verifier unit tests -- use `tmp_path` with real files
- Do NOT hardcode golden file content in tests -- always read from `tests/golden/`
- Do NOT use `time.sleep()` for timing -- use `time.monotonic()` for measurement
- Do NOT depend on test execution order
- Do NOT share mutable state between tests
- Do NOT skip golden tests when golden files are missing -- fail explicitly with clear message
- Do NOT compare binary files using text diff -- detect binary and report size difference only
