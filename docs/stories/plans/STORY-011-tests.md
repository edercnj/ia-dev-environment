# Test Plan: STORY-011 -- Src Layout Migration

**Story:** STORY-011 -- Src Layout Migration (PyPA)
**Date:** 2026-03-02
**Framework:** pytest + pytest-cov
**Coverage Target:** >= 95% Line, >= 90% Branch
**Risk Profile:** HIGH -- migration story; primary risk is breaking 923 existing tests

---

## 1. Test Phases

This migration has no new feature code. The test strategy is structured in
sequential phases to guarantee zero regression.

| Phase | Gate | Blocks |
|-------|------|--------|
| 0. Pre-migration baseline | All 923 tests green, coverage thresholds met | Phase 1 |
| 1. Post-migration smoke | `pip install -e .` + `ia-dev-env --help` | Phase 2 |
| 2. Full regression | All 923 tests green again | Phase 3 |
| 3. New migration-specific tests | Import protection + resources path | Phase 4 |
| 4. Golden file comparison | Byte-for-byte output unchanged | Phase 5 |
| 5. Coverage verification | Thresholds still met with new source path | Done |

---

## 2. Phase 0 -- Pre-Migration Baseline

**Purpose:** Record the "known good" state before any file moves.

### 2.1 Steps (manual, run before migration commits)

```bash
# Record test count and status
pytest --tb=short -q 2>&1 | tee baseline-results.txt

# Record coverage
pytest --cov=ia_dev_env --cov-report=term-missing --cov-branch \
       -q 2>&1 | tee baseline-coverage.txt

# Generate golden files (if not already present)
python scripts/generate_golden.py --all
```

### 2.2 Acceptance Criteria

| Check | Expected |
|-------|----------|
| Total tests | 923 passed |
| Failures | 0 |
| Line coverage | >= 95% |
| Branch coverage | >= 90% |
| `tests/golden/` directory | Populated for all 8 profiles |

---

## 3. Phase 1 -- Post-Migration Smoke Tests

**Purpose:** Verify the package is installable and the entry point works after
the directory restructure.

### 3.1 Test File: `tests/test_src_layout_smoke.py`

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_pip_install_editable_succeeds` | Run `pip install -e .` in subprocess | Exit code 0, no errors |
| 2 | `test_cli_help_returns_zero` | Run `ia-dev-env --help` in subprocess | Exit code 0, output contains "Usage" |
| 3 | `test_import_ia_dev_env_succeeds` | `import ia_dev_env` after install | No `ModuleNotFoundError` |
| 4 | `test_import_submodules_succeeds` | Import `ia_dev_env.config`, `ia_dev_env.assembler`, `ia_dev_env.domain` | All resolve without error |
| 5 | `test_package_location_is_under_src` | Check `ia_dev_env.__file__` | Path contains `src/ia_dev_env` |

### 3.2 Implementation Notes

```python
import subprocess
import sys

import pytest


class TestSrcLayoutSmoke:
    """Smoke tests verifying the package installs from src layout."""

    def test_pip_install_editable_succeeds(self):
        result = subprocess.run(
            [sys.executable, "-m", "pip", "install", "-e", "."],
            capture_output=True,
            text=True,
            cwd=PROJECT_ROOT,
            timeout=120,
        )
        assert result.returncode == 0

    def test_cli_help_returns_zero(self):
        result = subprocess.run(
            ["ia-dev-env", "--help"],
            capture_output=True,
            text=True,
            timeout=30,
        )
        assert result.returncode == 0
        assert "Usage" in result.stdout

    def test_package_location_is_under_src(self):
        import ia_dev_env
        pkg_path = Path(ia_dev_env.__file__).resolve()
        assert "src/ia_dev_env" in str(pkg_path)
```

---

## 4. Phase 2 -- Full Regression (Existing 923 Tests)

**Purpose:** Confirm every existing test still passes unchanged.

### 4.1 Steps

```bash
pip install -e ".[dev]"
pytest --tb=short -q
```

### 4.2 Acceptance Criteria

| Check | Expected |
|-------|----------|
| Total tests passed | Same count as Phase 0 baseline (923) |
| Failures | 0 |
| Errors | 0 |
| Skipped | Same count as Phase 0 baseline |

### 4.3 Known Areas Requiring Attention

These files reference `src/` paths and must be updated before regression passes:

| File | Current Reference | Post-Migration Reference |
|------|-------------------|--------------------------|
| `tests/test_byte_for_byte.py` line 23 | `PROJECT_ROOT / "src" / "config-templates"` | `PROJECT_ROOT / "resources" / "config-templates"` |
| `tests/test_byte_for_byte.py` line 25 | `PROJECT_ROOT / "src"` | `PROJECT_ROOT / "resources"` |
| `ia_dev_env/utils.py` | `src/` path references | `resources/` path references |
| `ia_dev_env/domain/stack_mapping.py` | `src/` path references | `resources/` path references |
| `tests/conftest.py` | `FIXTURES_DIR` (relative to `__file__`) | No change needed (relative) |

---

## 5. Phase 3 -- New Migration-Specific Tests

### 5.1 Test File: `tests/test_import_protection.py`

**Purpose:** Verify the src layout import protection -- `import ia_dev_env`
MUST fail when invoked from the project root without `pip install`.

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_import_fails_without_install` | Run `python -c "import ia_dev_env"` from project root in a clean venv | `ModuleNotFoundError` (exit code 1) |
| 2 | `test_import_succeeds_after_install` | Run same command after `pip install -e .` | Exit code 0 |
| 3 | `test_no_ia_dev_env_dir_in_project_root` | Check filesystem | `PROJECT_ROOT / "ia_dev_env"` does not exist |
| 4 | `test_package_exists_under_src` | Check filesystem | `PROJECT_ROOT / "src" / "ia_dev_env" / "__init__.py"` exists |

#### 5.1.1 Implementation Notes

```python
import subprocess
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent


class TestImportProtection:
    """Verify src layout prevents accidental imports."""

    def test_no_ia_dev_env_dir_in_project_root(self):
        flat_layout_dir = PROJECT_ROOT / "ia_dev_env"
        assert not flat_layout_dir.exists(), (
            "ia_dev_env/ must not exist at project root "
            "after src layout migration"
        )

    def test_package_exists_under_src(self):
        src_init = (
            PROJECT_ROOT / "src" / "ia_dev_env" / "__init__.py"
        )
        assert src_init.is_file()

    def test_import_fails_without_install(self, tmp_path):
        """Spawn a subprocess with project root on cwd but NOT on sys.path."""
        result = subprocess.run(
            [
                sys.executable, "-c",
                "import sys; "
                "sys.path = [p for p in sys.path "
                "if 'ia_dev_env' not in p and 'src' not in p]; "
                "import ia_dev_env",
            ],
            capture_output=True,
            text=True,
            cwd=str(PROJECT_ROOT),
            timeout=30,
        )
        assert result.returncode != 0
        assert "ModuleNotFoundError" in result.stderr
```

### 5.2 Test File: `tests/test_resources_path.py`

**Purpose:** Verify that all `resources/` paths resolve correctly after the
rename from `src/`.

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `test_resources_dir_exists` | Check `PROJECT_ROOT / "resources"` | Directory exists |
| 2 | `test_old_src_dir_has_no_assets` | Check `PROJECT_ROOT / "src"` contents | Only `ia_dev_env/` package, no asset dirs |
| 3 | `test_config_templates_in_resources` | Check `resources/config-templates/` | Directory exists with 8 profile YAML files |
| 4 | `test_core_rules_in_resources` | Check `resources/core-rules/` | Directory exists and is non-empty |
| 5 | `test_skills_templates_in_resources` | Check `resources/skills-templates/` | Directory exists and is non-empty |
| 6 | `test_agents_templates_in_resources` | Check `resources/agents-templates/` | Directory exists and is non-empty |
| 7 | `test_setup_sh_in_resources` | Check `resources/setup.sh` | File exists and is executable |
| 8 | `test_no_orphan_asset_dirs_in_src` | List `src/` subdirs | Only `ia_dev_env` present |
| 9 | `test_pipeline_resolves_resources_path` | Load config + run pipeline with `resources/` as src_dir | Pipeline completes without `FileNotFoundError` |
| 10 | `test_all_config_profiles_loadable` | Load each of the 8 profile YAMLs from `resources/config-templates/` | All 8 load successfully |

#### 5.2.1 Implementation Notes

```python
from pathlib import Path

import pytest

from ia_dev_env.config import load_config

PROJECT_ROOT = Path(__file__).resolve().parent.parent
RESOURCES_DIR = PROJECT_ROOT / "resources"
SRC_DIR = PROJECT_ROOT / "src"

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


class TestResourcesPath:
    """Verify resources/ directory structure after migration."""

    def test_resources_dir_exists(self):
        assert RESOURCES_DIR.is_dir()

    def test_old_src_dir_has_no_assets(self):
        if SRC_DIR.is_dir():
            children = [d.name for d in SRC_DIR.iterdir()]
            assert children == ["ia_dev_env"], (
                f"src/ should only contain ia_dev_env/, "
                f"found: {children}"
            )

    @pytest.mark.parametrize("subdir", [
        "config-templates",
        "core-rules",
        "skills-templates",
        "agents-templates",
    ])
    def test_expected_subdirs_in_resources(self, subdir):
        path = RESOURCES_DIR / subdir
        assert path.is_dir(), f"Missing: resources/{subdir}/"

    @pytest.mark.parametrize("profile", CONFIG_PROFILES)
    def test_all_config_profiles_loadable(self, profile):
        config_path = (
            RESOURCES_DIR
            / "config-templates"
            / f"setup-config.{profile}.yaml"
        )
        config = load_config(config_path)
        assert config.project.name is not None
```

---

## 6. Phase 4 -- Golden File / Byte-for-Byte Verification

**Purpose:** Confirm the migration produces identical output to pre-migration.

### 6.1 Existing Test: `tests/test_byte_for_byte.py`

This test already exists and is parametrized across all 8 config profiles.
After migration, only the path constants need updating:

| Constant | Before | After |
|----------|--------|-------|
| `CONFIG_TEMPLATES_DIR` | `PROJECT_ROOT / "src" / "config-templates"` | `PROJECT_ROOT / "resources" / "config-templates"` |
| `SRC_DIR` | `PROJECT_ROOT / "src"` | `PROJECT_ROOT / "resources"` |

### 6.2 Verification Steps

```bash
# Re-generate golden files from migrated layout
python scripts/generate_golden.py --all

# Diff against baseline golden files captured in Phase 0
diff -r tests/golden-baseline/ tests/golden/

# Run byte-for-byte tests
pytest tests/test_byte_for_byte.py -v
```

### 6.3 Acceptance Criteria

| Check | Expected |
|-------|----------|
| `diff` output | No differences |
| `test_byte_for_byte.py` | All 8 profiles pass |
| File count per profile | Identical to baseline |

---

## 7. Phase 5 -- Coverage Verification

**Purpose:** Confirm `pyproject.toml` coverage configuration works with
the new source path.

### 7.1 pyproject.toml Changes Required

```toml
# BEFORE
[tool.coverage.run]
source = ["ia_dev_env"]

# AFTER
[tool.coverage.run]
source = ["src/ia_dev_env"]
```

Additionally, `[tool.setuptools.packages.find]` must be added:

```toml
[tool.setuptools.packages.find]
where = ["src"]
```

### 7.2 Verification Steps

```bash
pytest --cov=src/ia_dev_env --cov-report=term-missing --cov-branch -q
```

### 7.3 Acceptance Criteria

| Metric | Minimum | Notes |
|--------|---------|-------|
| Line coverage | >= 95% | `fail_under = 95` in pyproject.toml |
| Branch coverage | >= 90% | Verified via `--cov-branch` |
| Coverage source | `src/ia_dev_env` | Must not be `ia_dev_env` (flat layout) |
| Uncovered lines | Same or fewer than Phase 0 baseline | Migration must not reduce coverage |

---

## 8. Test File Summary

| File | Phase | New/Modified | Test Count |
|------|-------|-------------|------------|
| (all existing test files) | 2 | Unmodified (path constants may change) | 923 |
| `tests/test_src_layout_smoke.py` | 1 | New | 5 |
| `tests/test_import_protection.py` | 3 | New | 4 |
| `tests/test_resources_path.py` | 3 | New | ~14 (parametrized) |
| `tests/test_byte_for_byte.py` | 4 | Modified (paths only) | 8 |

**Total new tests:** ~23
**Total after migration:** ~946

---

## 9. Risk Matrix

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Existing tests break due to import path changes | Medium | HIGH | Phase 2 runs full regression; `pip install -e .` resolves imports |
| `conftest.py` fixtures break | Low | HIGH | `FIXTURES_DIR` uses `__file__`-relative path (no change needed) |
| Coverage drops below threshold | Medium | Medium | Update `[tool.coverage.run] source` to `src/ia_dev_env` |
| Golden file paths break | High | Medium | Update `SRC_DIR` and `CONFIG_TEMPLATES_DIR` constants |
| `resources/` path not found at runtime | Medium | HIGH | `test_resources_path.py` validates all expected subdirs |
| CI breaks due to missing `pip install -e .` step | Low | HIGH | Smoke tests in Phase 1 catch this immediately |

---

## 10. Execution Checklist

- [ ] Phase 0: Run baseline, save `baseline-results.txt` and `baseline-coverage.txt`
- [ ] Phase 0: Generate and archive golden files as `tests/golden-baseline/`
- [ ] Phase 1: After migration, run smoke tests (`test_src_layout_smoke.py`)
- [ ] Phase 2: Run full `pytest` -- all 923 tests green
- [ ] Phase 3: Add and run `test_import_protection.py` and `test_resources_path.py`
- [ ] Phase 4: Run `test_byte_for_byte.py` -- all 8 profiles pass
- [ ] Phase 5: Run coverage check -- >= 95% line, >= 90% branch
- [ ] Final: Compare test count against baseline (should be baseline + ~23)
