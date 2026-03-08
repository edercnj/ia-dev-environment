# Implementation Plan: STORY-011 -- Src Layout Migration

## 1. Overview

Migrate the project from flat layout (`claude_setup/` at repo root) to the PyPA-recommended src layout (`src/claude_setup/`). Relocate non-Python assets from the current `src/` directory to `resources/` at the repo root.

### Current Structure

```
repo-root/
├── claude_setup/          # Python package (flat layout)
├── src/                   # Non-Python assets (templates, configs, setup.sh)
│   ├── agents-templates/
│   ├── cloud-providers/
│   ├── config-templates/
│   ├── core/
│   ├── core-rules/
│   ├── databases/
│   ├── docs/
│   ├── frameworks/
│   ├── hooks-templates/
│   ├── infrastructure/
│   ├── languages/
│   ├── patterns/
│   ├── protocols/
│   ├── readme-template.md
│   ├── security/
│   ├── settings-templates/
│   ├── setup.sh
│   ├── skills-templates/
│   ├── templates/
│   ├── tests/
│   └── claude_setup.egg-info/  (artifact -- delete)
├── tests/
├── scripts/
└── pyproject.toml
```

### Target Structure

```
repo-root/
├── src/
│   └── claude_setup/      # Python package (src layout)
│       ├── __init__.py
│       ├── __main__.py
│       ├── assembler/
│       ├── domain/
│       ├── config.py
│       ├── exceptions.py
│       ├── interactive.py
│       ├── models.py
│       ├── template_engine.py
│       ├── utils.py
│       └── verifier.py
├── resources/             # Non-Python assets (renamed from src/)
│   ├── agents-templates/
│   ├── cloud-providers/
│   ├── config-templates/
│   ├── ... (all asset directories)
│   └── setup.sh
├── tests/
├── scripts/
└── pyproject.toml
```

---

## 2. Affected Layers and Components

### 2.1 Files That Move

| Current Path | Target Path | Action |
|:---|:---|:---|
| `claude_setup/**` (31 .py files) | `src/claude_setup/**` | `git mv` |
| `src/agents-templates/` | `resources/agents-templates/` | `git mv` |
| `src/cloud-providers/` | `resources/cloud-providers/` | `git mv` |
| `src/config-templates/` | `resources/config-templates/` | `git mv` |
| `src/core/` | `resources/core/` | `git mv` |
| `src/core-rules/` | `resources/core-rules/` | `git mv` |
| `src/databases/` | `resources/databases/` | `git mv` |
| `src/docs/` | `resources/docs/` | `git mv` |
| `src/frameworks/` | `resources/frameworks/` | `git mv` |
| `src/hooks-templates/` | `resources/hooks-templates/` | `git mv` |
| `src/infrastructure/` | `resources/infrastructure/` | `git mv` |
| `src/languages/` | `resources/languages/` | `git mv` |
| `src/patterns/` | `resources/patterns/` | `git mv` |
| `src/protocols/` | `resources/protocols/` | `git mv` |
| `src/readme-template.md` | `resources/readme-template.md` | `git mv` |
| `src/security/` | `resources/security/` | `git mv` |
| `src/settings-templates/` | `resources/settings-templates/` | `git mv` |
| `src/setup.sh` | `resources/setup.sh` | `git mv` |
| `src/skills-templates/` | `resources/skills-templates/` | `git mv` |
| `src/templates/` | `resources/templates/` | `git mv` |
| `src/tests/` | `resources/tests/` | `git mv` |

### 2.2 Files to Delete

| Path | Reason |
|:---|:---|
| `src/claude_setup.egg-info/` | Build artifact in wrong location |
| `claude_setup.egg-info/` (if exists at root) | Stale egg-info from flat layout |

---

## 3. New Classes/Interfaces

None required. This is a structural migration only.

---

## 4. Existing Files Requiring Modification

### 4.1 Python Source Files (path references: `src/` to `resources/`)

| File | Line(s) | Current Reference | New Reference |
|:---|:---|:---|:---|
| `claude_setup/utils.py` | 74-81 | `find_src_dir()` returns `parent.parent / "src"` | Rename to `find_resources_dir()`, return `parent.parent / "resources"` |
| `claude_setup/__main__.py` | 15, 31, 38, 47, 81-88 | `find_src_dir`, `--src-dir` option | Rename to `find_resources_dir`, rename CLI option to `--resources-dir` |
| `claude_setup/assembler/rules_assembler.py` | 32 | `Path(__file__).resolve().parent.parent.parent / "src"` | Change to `... / "resources"` |
| `claude_setup/assembler/__init__.py` | 42-53, 56, 61-158 | `src_dir` parameter name throughout | Rename parameter to `resources_dir` (optional; can keep `src_dir` name internally if scope is limited) |

**Decision on parameter naming:** The `src_dir` parameter name is used pervasively in the assembler layer (50+ references). Two options:

- **Option A (Minimal diff):** Keep the internal parameter name `src_dir` in assemblers (it means "source of templates"), only rename the public-facing `find_src_dir()` -> `find_resources_dir()` and the CLI option. The parameter `src_dir` in assemblers will simply point to `resources/` instead of `src/`.
- **Option B (Full rename):** Rename `src_dir` to `resources_dir` everywhere.

**Recommendation: Option A.** The `src_dir` parameter in assemblers is an internal concept meaning "directory containing source templates." Renaming it across 50+ locations adds risk and churn with zero functional benefit. Only rename the user-facing function and CLI option.

### 4.2 Test Files (path references: `src/` to `resources/`)

| File | Line(s) | Change |
|:---|:---|:---|
| `tests/test_byte_for_byte.py` | 23, 25 | `PROJECT_ROOT / "src" / "config-templates"` -> `PROJECT_ROOT / "resources" / "config-templates"`; `SRC_DIR = PROJECT_ROOT / "src"` -> `PROJECT_ROOT / "resources"` |
| `tests/test_e2e_verification.py` | 24, 26 | Same pattern as above |
| `tests/test_verification_edge_cases.py` | 13 | `SRC_DIR = ... / "src"` -> `... / "resources"` |
| `tests/test_verification_performance.py` | 24, 26 | Same pattern as byte_for_byte |
| `tests/test_pipeline_integration.py` | 11, 31+ | Uses `find_src_dir()` -- rename import to `find_resources_dir` |
| `tests/test_cli_generate.py` | 47, 64, 101, 116, 130, 146, 161, 174, 187 | `@patch("claude_setup.__main__.find_src_dir")` -> `find_resources_dir`; mock returns and `--src-dir` option references |
| `tests/test_cli_init.py` | 26, 37, 50, 81, 97 | Same patch target rename |
| `tests/test_utils.py` | 172-198 | `TestFindSrcDir` class -> `TestFindResourcesDir`; update function calls and assertions (`result.name == "resources"`) |
| `tests/test_template_engine.py` | 83 | `test_init_nonexistent_src_dir` -- likely cosmetic, verify |

### 4.3 Scripts

| File | Line(s) | Change |
|:---|:---|:---|
| `scripts/generate_golden.py` | 11-12 | `SRC_DIR = PROJECT_ROOT / "src"` -> `PROJECT_ROOT / "resources"`; `CONFIG_DIR = SRC_DIR / "config-templates"` stays relative |

### 4.4 Configuration Files

| File | Section | Current | New |
|:---|:---|:---|:---|
| `pyproject.toml` | (new section) `[tool.setuptools.packages.find]` | absent | `where = ["src"]` |
| `pyproject.toml` | `[tool.coverage.run] source` | `["claude_setup"]` | `["src/claude_setup"]` |
| `pyproject.toml` | `[project.scripts]` | `claude_setup.__main__:main` | No change (setuptools resolves via `packages.find`) |

---

## 5. Dependency Direction Validation

No new dependencies introduced. The migration is purely structural:

- `src/claude_setup/domain/` still has zero external imports (standard library only).
- `src/claude_setup/assembler/` still depends on domain and models.
- `tests/` still imports from `claude_setup.*` (resolved via `pip install -e .`).
- No circular dependency risk.

---

## 6. Integration Points

### 6.1 pip install -e .

After migration, `pip install -e .` must be re-run. The editable install will resolve `claude_setup` from `src/claude_setup/` via `[tool.setuptools.packages.find] where = ["src"]`.

### 6.2 pytest

Tests import `from claude_setup.*` which resolves through the installed package, not direct filesystem access. This continues working after `pip install -e .`.

### 6.3 Coverage

`[tool.coverage.run] source` must change to `["src/claude_setup"]` so coverage correctly maps to source files in the new location.

### 6.4 CI/CD

Any CI scripts that reference `claude_setup/` directly (not via import) need updating. Grep CI configs for hardcoded paths.

---

## 7. Database Changes

N/A -- no database in this project.

---

## 8. API Changes

N/A -- CLI interface. One CLI option rename:

| Before | After | Impact |
|:---|:---|:---|
| `--src-dir` / `-s` | `--resources-dir` / `-s` | Users passing `--src-dir` must update. Low risk (advanced usage only). |

---

## 9. Event Changes

N/A -- no event-driven architecture.

---

## 10. Configuration Changes

### pyproject.toml Changes

```toml
# ADD new section:
[tool.setuptools.packages.find]
where = ["src"]

# MODIFY existing:
[tool.coverage.run]
source = ["src/claude_setup"]   # was: ["claude_setup"]
branch = true
```

### .gitignore

Verify `*.egg-info` pattern covers `src/claude_setup.egg-info/`. Add if needed:
```
src/*.egg-info/
```

---

## 11. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|:---|:---|:---|:---|
| Imports break after move | Medium | High | Run `pip install -e .` immediately after move; run full test suite |
| `find_src_dir()` callers missed | Low | High | Grep verified all 4 call sites; rename triggers import errors if missed |
| `rules_assembler.py` hardcoded path missed | Low | High | Line 32 explicitly hardcodes `parent.parent.parent / "src"` -- must update |
| Golden files invalidated | Low | Medium | Golden files are generated output, not source templates. They should NOT change. Verify with byte-for-byte tests. |
| Coverage config wrong | Low | Medium | Verify `pytest --cov` reports correctly after migration |
| CI/CD hardcoded paths | Low | Medium | Grep CI configs before merging |
| `--src-dir` CLI option rename breaks scripts | Low | Low | Document in changelog; option was for advanced usage only |

---

## 12. Execution Order

Execute in this exact sequence to minimize breakage windows:

### Phase 1: Prepare (no code changes)
1. Ensure all tests pass on current branch
2. Delete `src/claude_setup.egg-info/` (build artifact)

### Phase 2: Move assets (src/ -> resources/)
3. `mkdir resources/`
4. `git mv src/agents-templates resources/agents-templates` (repeat for all 20 asset dirs/files)
5. Verify: `ls resources/` shows all assets, `src/` is empty or gone

### Phase 3: Move Python package (claude_setup/ -> src/claude_setup/)
6. `git mv claude_setup/ src/claude_setup/`
7. Verify: `src/claude_setup/__init__.py` exists

### Phase 4: Update configuration
8. Update `pyproject.toml`: add `[tool.setuptools.packages.find]`, update coverage source
9. Run `pip install -e .` to register new package location

### Phase 5: Update path references in source code
10. `utils.py`: Rename `find_src_dir()` -> `find_resources_dir()`, update path to `"resources"`
11. `__main__.py`: Update import, rename CLI option `--src-dir` -> `--resources-dir`
12. `assembler/rules_assembler.py` line 32: Change `"src"` -> `"resources"`

### Phase 6: Update test files
13. Update all 9 test files with `src/` path references (see Section 4.2)
14. Update `scripts/generate_golden.py` paths

### Phase 7: Verify
15. `pip install -e .`
16. `claude-setup --help` (smoke test)
17. `pytest --cov` (full suite with coverage)
18. `python scripts/generate_golden.py` (regenerate golden files, verify no diff)
19. Verify coverage >= 95% line, >= 90% branch

---

## 13. Files Inventory (Complete List)

### Source files to modify (6 files)

1. `/Users/edercnj/workspaces/claude-environment/claude_setup/utils.py`
2. `/Users/edercnj/workspaces/claude-environment/claude_setup/__main__.py`
3. `/Users/edercnj/workspaces/claude-environment/claude_setup/assembler/rules_assembler.py`
4. `/Users/edercnj/workspaces/claude-environment/pyproject.toml`
5. `/Users/edercnj/workspaces/claude-environment/scripts/generate_golden.py`
6. `/Users/edercnj/workspaces/claude-environment/claude_setup/assembler/__init__.py` (no change needed if keeping `src_dir` param name)

### Test files to modify (9 files)

1. `/Users/edercnj/workspaces/claude-environment/tests/test_byte_for_byte.py`
2. `/Users/edercnj/workspaces/claude-environment/tests/test_e2e_verification.py`
3. `/Users/edercnj/workspaces/claude-environment/tests/test_verification_edge_cases.py`
4. `/Users/edercnj/workspaces/claude-environment/tests/test_verification_performance.py`
5. `/Users/edercnj/workspaces/claude-environment/tests/test_pipeline_integration.py`
6. `/Users/edercnj/workspaces/claude-environment/tests/test_cli_generate.py`
7. `/Users/edercnj/workspaces/claude-environment/tests/test_cli_init.py`
8. `/Users/edercnj/workspaces/claude-environment/tests/test_utils.py`
9. `/Users/edercnj/workspaces/claude-environment/tests/test_template_engine.py`

### Files to move (31 Python files + 20 asset directories/files)

- 31 Python files: `claude_setup/` -> `src/claude_setup/` (structure preserved)
- 20 asset items: `src/*` -> `resources/*` (structure preserved)

### Files to delete

- `src/claude_setup.egg-info/` (build artifact in wrong location)

---

## 14. Acceptance Checklist

- [ ] `claude_setup/` directory no longer exists at repo root
- [ ] `src/claude_setup/__init__.py` exists and is importable after `pip install -e .`
- [ ] Non-Python assets exist under `resources/` (not `src/`)
- [ ] `claude-setup --help` works after editable install
- [ ] `pytest --cov` passes with >= 95% line coverage, >= 90% branch
- [ ] `python scripts/generate_golden.py` produces identical output to pre-migration
- [ ] `python -c "import claude_setup"` from repo root fails without install (src layout protection)
- [ ] No references to old `src/` path remain in Python source or test files (verified via grep)
