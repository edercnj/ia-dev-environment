# STORY-011 Task Breakdown: Src Layout Migration (PyPA)

**Story:** STORY-011 -- Src Layout Migration
**Phase:** 6 (Structural)
**Blocked By:** STORY-010
**Blocks:** --
**Architecture:** library (hexagonal-lite for a CLI tool)
**Stack:** Python 3.9 + Click 8.1 + setuptools + pytest + pytest-cov

---

## Parallelism Groups

```
G1 (Foundation) ──> G2 (Core Move) ──> G3 (Config) ──> G4 (Path Fixes)
   T1, T2             T3                 T4              T5, T6, T7
  (parallel)                                            (parallel)
                                                            |
                                                            v
                                          G5 (Test Fixes) ──> G6 (Install+Verify) ──> G7 (Cleanup)
                                             T8, T9             T10, T11                T12, T13
                                            (parallel)         (sequential)            (parallel)
```

G1 tasks have no internal dependencies (parallel).
G2 depends on G1 (assets must be relocated before Python package moves into `src/`).
G3 depends on G2 (pyproject.toml needs the package already in `src/ia_dev_env/`).
G4 depends on G2 (path references must point to `resources/` after the move).
G5 depends on G4 (test path fixes require production code paths to be final).
G6 depends on G3+G5 (install and test run require all config and paths updated).
G7 depends on G6 (cleanup only after verification passes).

---

## Current File Inventory

### Production Python files (33 files in `ia_dev_env/`)

```
ia_dev_env/__init__.py
ia_dev_env/__main__.py
ia_dev_env/config.py
ia_dev_env/exceptions.py
ia_dev_env/interactive.py
ia_dev_env/models.py
ia_dev_env/template_engine.py
ia_dev_env/utils.py
ia_dev_env/verifier.py
ia_dev_env/assembler/__init__.py
ia_dev_env/assembler/agents.py
ia_dev_env/assembler/auditor.py
ia_dev_env/assembler/conditions.py
ia_dev_env/assembler/consolidator.py
ia_dev_env/assembler/copy_helpers.py
ia_dev_env/assembler/hooks_assembler.py
ia_dev_env/assembler/patterns_assembler.py
ia_dev_env/assembler/protocols_assembler.py
ia_dev_env/assembler/readme_assembler.py
ia_dev_env/assembler/rules_assembler.py
ia_dev_env/assembler/settings_assembler.py
ia_dev_env/assembler/skills.py
ia_dev_env/domain/__init__.py
ia_dev_env/domain/core_kp_routing.py
ia_dev_env/domain/pattern_mapping.py
ia_dev_env/domain/protocol_mapping.py
ia_dev_env/domain/resolved_stack.py
ia_dev_env/domain/resolver.py
ia_dev_env/domain/skill_registry.py
ia_dev_env/domain/stack_mapping.py
ia_dev_env/domain/stack_pack_mapping.py
ia_dev_env/domain/validator.py
ia_dev_env/domain/version_resolver.py
```

### Non-Python assets (current `src/` directory)

```
src/agents-templates/
src/cloud-providers/
src/config-templates/
src/core/
src/core-rules/
src/databases/
src/docs/
src/frameworks/
src/hooks-templates/
src/infrastructure/
src/languages/
src/patterns/
src/protocols/
src/readme-template.md
src/security/
src/settings-templates/
src/setup.sh
src/skills-templates/
src/templates/
src/tests/
src/ia_dev_env.egg-info/  (to be deleted)
src/.claude/                 (to be deleted or kept)
src/.editorconfig            (to be deleted or kept)
src/.shellcheckrc            (to be deleted or kept)
```

### Test files referencing `src/` or `src_dir` (23 files)

```
tests/test_byte_for_byte.py
tests/test_e2e_verification.py
tests/test_verification_edge_cases.py
tests/test_verification_performance.py
tests/test_pipeline_integration.py
tests/test_pipeline.py
tests/test_utils.py
tests/test_cli_generate.py
tests/test_cli_init.py
tests/test_template_engine.py
tests/test_patterns_protocols_contract.py
tests/assembler/test_settings_assembler.py
tests/assembler/test_hooks_assembler.py
tests/assembler/test_readme_assembler.py
tests/assembler/test_copy_helpers.py
tests/assembler/test_patterns_assembler.py
tests/assembler/test_protocols_assembler.py
tests/assembler/test_rules_assembler.py
tests/assembler/test_skills_assembly.py
tests/assembler/test_agents_assembly.py
tests/assembler/test_consolidator.py
tests/domain/test_pattern_mapping.py
tests/domain/test_protocol_mapping.py
```

### Other files to update

```
pyproject.toml
scripts/generate_golden.py
```

---

## G1 -- Foundation: Move non-Python assets from `src/` to `resources/` (Parallel)

### T1: Create `resources/` directory and move asset directories

| Attribute | Value |
|-----------|-------|
| **Task ID** | G1-T1 |
| **Title** | Move non-Python asset directories from `src/` to `resources/` |
| **Layer** | filesystem |
| **Depends on** | -- |
| **Blocks** | G2-T3 |

**What to do:**

1. Create `resources/` at project root
2. Move the following from `src/` to `resources/`:
   - `agents-templates/`
   - `cloud-providers/`
   - `config-templates/`
   - `core/`
   - `core-rules/`
   - `databases/`
   - `docs/`
   - `frameworks/`
   - `hooks-templates/`
   - `infrastructure/`
   - `languages/`
   - `patterns/`
   - `protocols/`
   - `readme-template.md`
   - `security/`
   - `settings-templates/`
   - `setup.sh`
   - `skills-templates/`
   - `templates/`
   - `tests/`

**Commands:**

```bash
mkdir -p resources
# For each asset directory/file:
git mv src/agents-templates resources/agents-templates
git mv src/cloud-providers resources/cloud-providers
git mv src/config-templates resources/config-templates
git mv src/core resources/core
git mv src/core-rules resources/core-rules
git mv src/databases resources/databases
git mv src/docs resources/docs
git mv src/frameworks resources/frameworks
git mv src/hooks-templates resources/hooks-templates
git mv src/infrastructure resources/infrastructure
git mv src/languages resources/languages
git mv src/patterns resources/patterns
git mv src/protocols resources/protocols
git mv src/readme-template.md resources/readme-template.md
git mv src/security resources/security
git mv src/settings-templates resources/settings-templates
git mv src/setup.sh resources/setup.sh
git mv src/skills-templates resources/skills-templates
git mv src/templates resources/templates
git mv src/tests resources/tests
```

**Verify:**

```bash
ls resources/  # all 20 items present
ls src/        # should only have dotfiles, egg-info, .DS_Store left
```

---

### T2: Remove `ia_dev_env.egg-info/` and other artifacts from `src/`

| Attribute | Value |
|-----------|-------|
| **Task ID** | G1-T2 |
| **Title** | Remove egg-info and leftover artifacts from `src/` |
| **Layer** | filesystem |
| **Depends on** | -- |
| **Blocks** | G2-T3 |

**What to do:**

1. Delete `src/ia_dev_env.egg-info/` (build artifact, should not be tracked)
2. Decide on `src/.claude/`, `src/.editorconfig`, `src/.shellcheckrc`:
   - If they belong to the resources context, move to `resources/`
   - If they are project-level dotfiles duplicated by mistake, delete them
3. Delete `src/.DS_Store`

**Commands:**

```bash
rm -rf src/ia_dev_env.egg-info
rm -f src/.DS_Store
# Move or remove dotfiles as appropriate:
git mv src/.editorconfig resources/.editorconfig 2>/dev/null || true
git mv src/.shellcheckrc resources/.shellcheckrc 2>/dev/null || true
# If src/.claude/ contains resource-related content:
git mv src/.claude resources/.claude 2>/dev/null || true
```

**Verify:**

```bash
ls -la src/  # should be empty (or only __pycache__ which git ignores)
```

---

## G2 -- Core Move: Move `ia_dev_env/` to `src/ia_dev_env/` (Sequential)

### T3: Move Python package to src layout

| Attribute | Value |
|-----------|-------|
| **Task ID** | G2-T3 |
| **Title** | Move `ia_dev_env/` directory to `src/ia_dev_env/` |
| **Layer** | filesystem |
| **Depends on** | G1-T1, G1-T2 |
| **Blocks** | G3-T4, G4-T5, G4-T6, G4-T7 |

**What to do:**

1. Ensure `src/` is empty (all assets moved in G1)
2. Move `ia_dev_env/` into `src/ia_dev_env/`
3. The internal structure remains unchanged:
   - `src/ia_dev_env/__init__.py`
   - `src/ia_dev_env/__main__.py`
   - `src/ia_dev_env/assembler/`
   - `src/ia_dev_env/domain/`
   - etc.

**Commands:**

```bash
# src/ should be empty at this point (or removed entirely)
# If src/ still exists but is empty:
git mv ia_dev_env src/ia_dev_env

# If src/ was fully removed:
mkdir -p src
git mv ia_dev_env src/ia_dev_env
```

**Verify:**

```bash
ls src/ia_dev_env/__init__.py          # must exist
ls src/ia_dev_env/__main__.py          # must exist
ls src/ia_dev_env/assembler/__init__.py # must exist
ls src/ia_dev_env/domain/__init__.py    # must exist
find src/ia_dev_env -name "*.py" | wc -l  # should be 33
test ! -d ia_dev_env && echo "OK: old dir removed"
```

---

## G3 -- Config: Update pyproject.toml (Sequential)

### T4: Update pyproject.toml for src layout

| Attribute | Value |
|-----------|-------|
| **Task ID** | G3-T4 |
| **Title** | Update pyproject.toml with src layout configuration |
| **Layer** | config |
| **Depends on** | G2-T3 |
| **Blocks** | G6-T10 |

**What to do:**

1. Add `[tool.setuptools.packages.find]` section with `where = ["src"]`
2. Update `[tool.coverage.run]` source from `["ia_dev_env"]` to `["src/ia_dev_env"]`
3. Entry point `ia-dev-env = "ia_dev_env.__main__:main"` stays the same (setuptools resolves via `packages.find`)

**Files to modify:**

- `pyproject.toml`

**Before:**

```toml
[tool.coverage.run]
source = ["ia_dev_env"]
branch = true
```

**After:**

```toml
[tool.setuptools.packages.find]
where = ["src"]

[tool.coverage.run]
source = ["src/ia_dev_env"]
branch = true
```

**Verify:**

```bash
python -c "
import tomllib
with open('pyproject.toml', 'rb') as f:
    d = tomllib.load(f)
assert d['tool']['setuptools']['packages']['find']['where'] == ['src']
assert d['tool']['coverage']['run']['source'] == ['src/ia_dev_env']
assert d['project']['scripts']['ia-dev-env'] == 'ia_dev_env.__main__:main'
print('pyproject.toml OK')
"
```

---

## G4 -- Path Fixes: Update all path references in production code (Parallel)

### T5: Rename `find_src_dir` to `find_resources_dir` in `utils.py`

| Attribute | Value |
|-----------|-------|
| **Task ID** | G4-T5 |
| **Title** | Rename `find_src_dir` to `find_resources_dir` in utils.py |
| **Layer** | application |
| **Depends on** | G2-T3 |
| **Blocks** | G5-T8 |

**What to do:**

1. In `src/ia_dev_env/utils.py`:
   - Rename function `find_src_dir()` to `find_resources_dir()`
   - Update the docstring from "Locate the src/ directory" to "Locate the resources/ directory"
   - Change the path resolution from `parent.parent / "src"` to `parent.parent.parent / "resources"`
     (After the move, `__file__` is at `src/ia_dev_env/utils.py`, so `.parent.parent.parent` gets to project root)
   - Update error message accordingly

**Before:**

```python
def find_src_dir() -> Path:
    """Locate the src/ directory relative to the package."""
    src = Path(__file__).resolve().parent.parent / "src"
    if not src.is_dir():
        raise FileNotFoundError(
            f"Source directory not found: {src}"
        )
    return src
```

**After:**

```python
def find_resources_dir() -> Path:
    """Locate the resources/ directory relative to the package."""
    resources = Path(__file__).resolve().parent.parent.parent / "resources"
    if not resources.is_dir():
        raise FileNotFoundError(
            f"Resources directory not found: {resources}"
        )
    return resources
```

**Verify:**

```bash
grep -n "find_resources_dir" src/ia_dev_env/utils.py
grep -c "find_src_dir" src/ia_dev_env/utils.py  # should be 0
```

---

### T6: Update `__main__.py` to use `find_resources_dir`

| Attribute | Value |
|-----------|-------|
| **Task ID** | G4-T6 |
| **Title** | Update __main__.py imports and references from src_dir to resources_dir |
| **Layer** | adapter/inbound |
| **Depends on** | G2-T3 |
| **Blocks** | G5-T8 |

**What to do:**

1. In `src/ia_dev_env/__main__.py`:
   - Change import from `find_src_dir` to `find_resources_dir`
   - Rename CLI option `--src-dir` to `--resources-dir` (or keep `--src-dir` as deprecated alias -- decision needed)
   - Rename parameter `src_dir` to `resources_dir` throughout
   - Rename `_resolve_src_dir` to `_resolve_resources_dir`
   - Update all internal references

**Decision point:** The CLI option `--src-dir` / `-s` is a public API. Options:
- **(A) Rename to `--resources-dir`** -- clean but breaking change
- **(B) Keep `--src-dir` as name but resolve to resources/** -- confusing
- **(C) Add `--resources-dir`, deprecate `--src-dir`** -- safest

Recommendation: **(A)** since this is pre-1.0 and the option semantics change.

**Files to modify:**

- `src/ia_dev_env/__main__.py`

**Key changes:**

```python
# Import
from ia_dev_env.utils import find_resources_dir, setup_logging

# CLI option
@click.option("--resources-dir", "-s", type=click.Path(exists=True), default=None, help="Resources templates directory.")

# Parameter
def generate(..., resources_dir: Optional[str], ...):
    resolved_resources = _resolve_resources_dir(resources_dir)

# Internal function
def _resolve_resources_dir(resources_dir: Optional[str]) -> Path:
    if resources_dir:
        return Path(resources_dir)
    try:
        return find_resources_dir()
    except FileNotFoundError as exc:
        raise click.ClickException(str(exc)) from exc
```

**Verify:**

```bash
grep -c "find_src_dir\|src_dir" src/ia_dev_env/__main__.py  # should be 0
grep -c "find_resources_dir\|resources_dir" src/ia_dev_env/__main__.py  # should be > 0
```

---

### T7: Update all assembler and domain path references from `src/` to `resources/`

| Attribute | Value |
|-----------|-------|
| **Task ID** | G4-T7 |
| **Title** | Update assembler and domain code: src_dir references become resources_dir |
| **Layer** | application + domain |
| **Depends on** | G2-T3 |
| **Blocks** | G5-T8 |

**What to do:**

This is the largest task. All assembler and domain files that accept a `src_dir: Path` parameter need renaming. The parameter currently receives a `Path` pointing to what was `src/` (the assets directory). After migration, it points to `resources/`.

**Semantic rename:** `src_dir` -> `resources_dir` in all function signatures and call sites.

**Files to modify (14 files):**

| File | Changes |
|------|---------|
| `src/ia_dev_env/assembler/__init__.py` | Rename `src_dir` param in `_build_assemblers`, `_assemble_all`, `_run_in_temp`, `_run_dry`, `_run_real`, `run_pipeline`; rename `ASSEMBLERS_WITH_SRC_DIR` constant |
| `src/ia_dev_env/assembler/agents.py` | Rename `src_dir` param in all methods (~20 occurrences) |
| `src/ia_dev_env/assembler/hooks_assembler.py` | Rename `self._src_dir` to `self._resources_dir`, constructor param |
| `src/ia_dev_env/assembler/patterns_assembler.py` | Rename `self._src_dir` to `self._resources_dir`, constructor param |
| `src/ia_dev_env/assembler/protocols_assembler.py` | Rename `self._src_dir` to `self._resources_dir`, constructor param |
| `src/ia_dev_env/assembler/readme_assembler.py` | Rename `self._src_dir` to `self._resources_dir`, constructor param |
| `src/ia_dev_env/assembler/rules_assembler.py` | Rename `src_dir` in all methods + hardcoded path at line 32: `parent.parent.parent / "src"` -> `parent.parent.parent.parent / "resources"` |
| `src/ia_dev_env/assembler/settings_assembler.py` | Rename `self._src_dir` to `self._resources_dir`, constructor param |
| `src/ia_dev_env/assembler/skills.py` | Rename `src_dir` param in all methods (~20 occurrences) |
| `src/ia_dev_env/assembler/copy_helpers.py` | Rename `src_dir` if present |
| `src/ia_dev_env/assembler/consolidator.py` | Check for `src_dir` references |
| `src/ia_dev_env/domain/pattern_mapping.py` | Rename `src_dir` param |
| `src/ia_dev_env/domain/protocol_mapping.py` | Rename `src_dir` param |
| `src/ia_dev_env/domain/validator.py` | Rename `src_dir` param and error message |
| `src/ia_dev_env/template_engine.py` | Rename `src_dir` param in constructor |

**Critical path change in `rules_assembler.py` (line 32):**

```python
# Before (from src/ia_dev_env/assembler/rules_assembler.py):
src_dir = Path(__file__).resolve().parent.parent.parent / "src"

# After:
resources_dir = Path(__file__).resolve().parent.parent.parent.parent / "resources"
# Because __file__ is now at src/ia_dev_env/assembler/rules_assembler.py
# .parent = src/ia_dev_env/assembler/
# .parent.parent = src/ia_dev_env/
# .parent.parent.parent = src/
# .parent.parent.parent.parent = project_root/
```

**Verify:**

```bash
# No remaining references to src_dir in production code (excluding the src/ directory name itself)
grep -rn "src_dir" src/ia_dev_env/ | grep -v "__pycache__"  # should be 0
grep -rn "find_src_dir" src/ia_dev_env/  # should be 0

# Confirm resources_dir is used
grep -rn "resources_dir" src/ia_dev_env/ | grep -v "__pycache__" | wc -l  # should be > 50
```

---

## G5 -- Test Fixes: Update all path references in test files (Parallel)

### T8: Update test files that reference `SRC_DIR` or `src_dir` with real project paths

| Attribute | Value |
|-----------|-------|
| **Task ID** | G5-T8 |
| **Title** | Update integration/E2E test files: `SRC_DIR` -> `RESOURCES_DIR` |
| **Layer** | test |
| **Depends on** | G4-T5, G4-T6, G4-T7 |
| **Blocks** | G6-T10 |

**What to do:**

Update all test files that define `SRC_DIR = PROJECT_ROOT / "src"` or similar to point to `resources/`.

**Files to modify (7 files with real project path references):**

| File | Change |
|------|--------|
| `tests/test_byte_for_byte.py` | `SRC_DIR = PROJECT_ROOT / "src"` -> `RESOURCES_DIR = PROJECT_ROOT / "resources"` + update `CONFIG_TEMPLATES_DIR` |
| `tests/test_e2e_verification.py` | Same pattern |
| `tests/test_verification_edge_cases.py` | `SRC_DIR = Path(__file__).resolve().parent.parent / "src"` -> `RESOURCES_DIR = ... / "resources"` |
| `tests/test_verification_performance.py` | Same pattern (check for SRC_DIR usage) |
| `tests/test_pipeline_integration.py` | Same pattern |
| `tests/test_utils.py` | Test for `find_resources_dir` instead of `find_src_dir` |
| `tests/test_cli_generate.py` | Mock/reference updates for `find_resources_dir` |

**Pattern for each file:**

```python
# Before:
SRC_DIR = PROJECT_ROOT / "src"
CONFIG_TEMPLATES_DIR = PROJECT_ROOT / "src" / "config-templates"

# After:
RESOURCES_DIR = PROJECT_ROOT / "resources"
CONFIG_TEMPLATES_DIR = PROJECT_ROOT / "resources" / "config-templates"
```

**Verify:**

```bash
grep -rn "SRC_DIR\|find_src_dir" tests/ | grep -v "__pycache__"  # should be 0
grep -rn "RESOURCES_DIR\|find_resources_dir" tests/ | grep -v "__pycache__" | wc -l  # should be > 0
```

---

### T9: Update test files that use `tmp_path / "src"` for mock fixture trees

| Attribute | Value |
|-----------|-------|
| **Task ID** | G5-T9 |
| **Title** | Update unit test files: mock `src` directory trees -> `resources` |
| **Layer** | test |
| **Depends on** | G4-T5, G4-T6, G4-T7 |
| **Blocks** | G6-T10 |

**What to do:**

Many unit tests create mock directory structures using `tmp_path / "src"` to simulate the source assets directory. These must change to `tmp_path / "resources"` (or keep as any name since they are passed as a parameter -- verify each case).

**Important distinction:** If the test passes a path as `resources_dir` parameter, the directory name does not matter. Only tests that rely on auto-detection (`find_resources_dir`) or hardcoded `"src"` strings in fixtures need updating.

**Files to audit and update (16 files with mock src trees):**

| File | Action |
|------|--------|
| `tests/test_pipeline.py` | Rename `src = tmp_path / "src"` variable if used as param name; functionally should still work since it is just a local var name but rename for clarity |
| `tests/test_template_engine.py` | Check `src_dir` mock references |
| `tests/test_patterns_protocols_contract.py` | Update `src_dir` variable names |
| `tests/test_cli_init.py` | Check for `src_dir` mock references |
| `tests/assembler/test_settings_assembler.py` | Rename constructor param from `src_dir` |
| `tests/assembler/test_hooks_assembler.py` | Rename constructor param from `src_dir` |
| `tests/assembler/test_readme_assembler.py` | Rename constructor param from `src_dir` |
| `tests/assembler/test_copy_helpers.py` | Rename param references |
| `tests/assembler/test_patterns_assembler.py` | Rename constructor param from `src_dir` |
| `tests/assembler/test_protocols_assembler.py` | Rename constructor param from `src_dir` |
| `tests/assembler/test_rules_assembler.py` | Rename param references + hardcoded path mock |
| `tests/assembler/test_skills_assembly.py` | Rename param references |
| `tests/assembler/test_agents_assembly.py` | Rename param references |
| `tests/assembler/test_consolidator.py` | Rename param references |
| `tests/domain/test_pattern_mapping.py` | Rename `src_dir` param |
| `tests/domain/test_protocol_mapping.py` | Rename `src_dir` param |

**Verify:**

```bash
# No remaining src_dir variable names in tests (excluding __pycache__)
grep -rn "src_dir" tests/ | grep -v "__pycache__" | grep -v "# legacy"  # should be 0
```

---

## G6 -- Install + Verify (Sequential)

### T10: Reinstall package in editable mode and run full test suite

| Attribute | Value |
|-----------|-------|
| **Task ID** | G6-T10 |
| **Title** | pip install -e . and run pytest with coverage |
| **Layer** | verification |
| **Depends on** | G3-T4, G5-T8, G5-T9 |
| **Blocks** | G7-T12 |

**What to do:**

1. Remove any existing installation artifacts
2. Reinstall in editable mode
3. Verify import works
4. Run full test suite with coverage
5. Verify `ia-dev-env --help` works

**Commands:**

```bash
# Step 1: Clean install
pip uninstall ia-dev-env -y 2>/dev/null || true
rm -rf src/ia_dev_env.egg-info ia_dev_env.egg-info *.egg-info
pip install -e ".[dev]"

# Step 2: Verify import
python -c "import ia_dev_env; print(ia_dev_env.__version__)"

# Step 3: Verify import protection (should fail when NOT installed)
# This test must be run in a clean venv without pip install -e .
# For now, verify the positive case:
python -c "from ia_dev_env.assembler import run_pipeline; print('OK')"

# Step 4: Run tests
pytest tests/ -v --tb=short --cov=ia_dev_env --cov-report=term-missing --cov-branch

# Step 5: Verify CLI
ia-dev-env --help
ia-dev-env generate --help
ia-dev-env validate --help
```

**Acceptance criteria:**

- [ ] `pip install -e .` completes without errors
- [ ] `import ia_dev_env` works
- [ ] `ia-dev-env --help` shows usage
- [ ] All tests pass (923+ tests)
- [ ] Line coverage >= 95%
- [ ] Branch coverage >= 90%

---

### T11: Verify golden file output is byte-for-byte identical

| Attribute | Value |
|-----------|-------|
| **Task ID** | G6-T11 |
| **Title** | Verify golden file byte-for-byte output after migration |
| **Layer** | verification |
| **Depends on** | G6-T10 |
| **Blocks** | G7-T12 |

**What to do:**

1. Regenerate golden files using the migrated code
2. Compare output against pre-migration golden reference
3. Ensure byte-for-byte identical output (RULE-005 compliance)

**Commands:**

```bash
# Regenerate golden files
python scripts/generate_golden.py

# Run byte-for-byte tests
pytest tests/test_byte_for_byte.py -v

# Run E2E verification tests
pytest tests/test_e2e_verification.py -v
```

**Note:** The `scripts/generate_golden.py` file also references `SRC_DIR = PROJECT_ROOT / "src"` and must be updated to `RESOURCES_DIR = PROJECT_ROOT / "resources"` as part of G5-T8 or here.

**Verify:**

```bash
pytest tests/test_byte_for_byte.py tests/test_e2e_verification.py -v  # all pass
```

---

## G7 -- Cleanup (Parallel)

### T12: Remove old artifacts and verify no orphan files

| Attribute | Value |
|-----------|-------|
| **Task ID** | G7-T12 |
| **Title** | Remove old egg-info, verify no orphan files |
| **Layer** | filesystem |
| **Depends on** | G6-T10, G6-T11 |
| **Blocks** | -- |

**What to do:**

1. Verify `ia_dev_env/` does NOT exist at project root
2. Verify `src/` contains ONLY `ia_dev_env/` (no leftover assets)
3. Remove any stale `__pycache__` directories in old locations
4. Remove `ia_dev_env.egg-info` from project root if present
5. Verify `.gitignore` includes egg-info patterns

**Commands:**

```bash
# Verify no orphan directories
test ! -d ia_dev_env && echo "OK: no ia_dev_env/ at root"
test ! -f src/setup.sh && echo "OK: no assets in src/"
test ! -d src/agents-templates && echo "OK: no asset dirs in src/"

# Clean pycache
find . -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true

# Remove egg-info at root
rm -rf ia_dev_env.egg-info

# Verify src/ only has ia_dev_env/
ls src/  # should show only: ia_dev_env/

# Verify resources/ has all assets
ls resources/ | wc -l  # should be ~20 items
```

---

### T13: Update documentation and scripts

| Attribute | Value |
|-----------|-------|
| **Task ID** | G7-T13 |
| **Title** | Update README, scripts, and docs with new directory structure |
| **Layer** | documentation |
| **Depends on** | G6-T10, G6-T11 |
| **Blocks** | -- |

**What to do:**

1. Update `README.md` directory structure section to reflect:
   - `src/ia_dev_env/` (Python package)
   - `resources/` (templates, configs, scripts)
2. Update `scripts/generate_golden.py`:
   - `SRC_DIR = PROJECT_ROOT / "src"` -> `RESOURCES_DIR = PROJECT_ROOT / "resources"`
   - Update all references
3. Review `.claude/rules/*.md` for any references to `src/` that should now say `resources/`
4. Update the STORY-011 doc itself to mark DoD items as complete

**Files to modify:**

- `README.md`
- `scripts/generate_golden.py`
- `.claude/rules/04-architecture-summary.md` (if it references `src/` as assets)

**Verify:**

```bash
# No stale references to "src/" meaning the assets directory (not the Python src layout)
grep -rn '"src/' README.md scripts/ .claude/rules/ | grep -v "src/ia_dev_env" | grep -v "__pycache__"
# Should be 0 or only legitimate references to src layout
```

---

## Risk Register

| Risk | Impact | Mitigation |
|------|--------|------------|
| `rules_assembler.py` hardcoded path breaks after move | All rule assembly fails | T7 explicitly addresses line 32 path calculation; verify depth change |
| `find_resources_dir` path depth wrong | CLI cannot locate templates | T5 carefully counts `.parent` hops; unit test in T8 validates |
| `pip install -e .` caches stale paths | Tests import old code | T10 does clean uninstall before reinstall |
| Golden files differ after migration | RULE-005 violation | T11 runs byte-for-byte comparison |
| Test fixtures use hardcoded `"src"` strings | Mock tests fail | T9 audits all 16 test files with mock trees |

---

## Summary Table

| Group | Task | Title | Depends | Est. Files |
|-------|------|-------|---------|-----------|
| G1 | T1 | Move asset dirs to `resources/` | -- | 20 dirs |
| G1 | T2 | Remove egg-info and artifacts | -- | 3-5 items |
| G2 | T3 | Move `ia_dev_env/` to `src/ia_dev_env/` | T1, T2 | 33 files |
| G3 | T4 | Update `pyproject.toml` | T3 | 1 file |
| G4 | T5 | Rename `find_src_dir` in `utils.py` | T3 | 1 file |
| G4 | T6 | Update `__main__.py` | T3 | 1 file |
| G4 | T7 | Update assembler + domain path refs | T3 | 14 files |
| G5 | T8 | Update integration/E2E test paths | T5-T7 | 7 files |
| G5 | T9 | Update unit test mock trees | T5-T7 | 16 files |
| G6 | T10 | pip install + run tests | T4, T8, T9 | 0 (commands only) |
| G6 | T11 | Verify golden file output | T10 | 0 (commands only) |
| G7 | T12 | Remove orphans, clean artifacts | T10, T11 | 0 (commands only) |
| G7 | T13 | Update docs and scripts | T10, T11 | 3 files |

**Total estimated files to modify:** ~43 production + test files, ~20 directory moves
