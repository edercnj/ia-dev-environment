# Task Decomposition -- STORY-020: CI/CD, Packaging, Documentation

**Status:** PENDING
**Date:** 2026-03-11
**Blocked By:** STORY-019 (integration tests parity -- complete)
**Blocks:** None (final story in epic)

---

## G1 -- npm Packaging Configuration

**Purpose:** Configure `package.json` for npm distribution. The `files` field controls the tarball contents (allowlist approach, safer than `.npmignore`). The `prepublishOnly` script gates publishing behind build + test. This is the foundation that G2 (CI) and G5 (verification) depend on.
**Dependencies:** None
**Compiles independently:** Yes
**Risk:** LOW

### T1.1 -- Add `files` field to package.json

- **File:** `package.json` (modify)
- **What to implement:**
  1. Add `"files": ["dist", "resources"]` after the `bin` field.
  2. This controls what `npm pack` includes in the tarball. Only compiled JS (`dist/`) and templates (`resources/`) are needed at runtime.
  3. Confirms RULE-011: `resources/` is explicitly included in the published package.
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### T1.2 -- Add `prepublishOnly` script

- **File:** `package.json` (modify)
- **What to implement:**
  1. Add `"prepublishOnly": "npm run build && npm run test"` to the `scripts` section.
  2. This ensures no broken package can be published -- build and test must pass first.
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### T1.3 -- Add `test:pack` script

- **File:** `package.json` (modify)
- **What to implement:**
  1. Add `"test:pack": "npm pack && npm install -g ./ia-dev-environment-*.tgz && ia-dev-env --help"` to the `scripts` section.
  2. This allows local verification of the packed CLI before pushing.
- **Dependencies on other tasks:** T1.1
- **Estimated complexity:** S

### T1.4 -- Update description and add metadata

- **File:** `package.json` (modify)
- **What to implement:**
  1. Change `description` from `"Project foundation for ia-dev-environment Node.js + TypeScript migration."` to `"CLI tool that generates .claude/ and .github/ boilerplate for AI-assisted development environments"`.
  2. Add `"keywords": ["cli", "ai", "claude", "copilot", "boilerplate", "generator"]` for npm discoverability.
  3. Add `"repository"` field with the actual git URL (or placeholder if URL is not yet known).
  4. **Do NOT change `version`** -- stays at `0.1.0` per the plan. Version bumping is a release concern.
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### Verification checkpoint G1

```bash
# Verify package.json is valid JSON
node -e "require('./package.json')"

# Build and pack
npm run build && npm pack

# Verify tarball contents include dist/ and resources/, exclude tests/ and src/
tar tzf ia-dev-environment-*.tgz | grep -c 'package/dist/'     # > 0
tar tzf ia-dev-environment-*.tgz | grep -c 'package/resources/' # > 0
tar tzf ia-dev-environment-*.tgz | grep -c 'package/tests/'    # 0
tar tzf ia-dev-environment-*.tgz | grep -c 'package/src/'      # 0

# Clean up tarball
rm -f ia-dev-environment-*.tgz
```

---

## G2 -- GitHub Actions CI Workflow

**Purpose:** Create the CI pipeline that runs lint, build, test, and coverage on every push/PR to main. Node.js 18/20/22 matrix ensures cross-version compatibility. A separate pack-verify job confirms the CLI works from a packed tarball. Requires `.gitignore` negation since `.github/` is currently gitignored.
**Dependencies:** G1 (packaging must be configured for pack-verify job)
**Compiles independently:** N/A (YAML file)
**Risk:** MEDIUM (`.gitignore` negation needed)

### T2.1 -- Update .gitignore for CI workflow tracking

- **File:** `.gitignore` (modify)
- **What to implement:**
  1. Add negation rule `!.github/workflows/` immediately after the `.github/` ignore line.
     ```
     # GitHub Copilot — generated output of the boilerplate
     .github/
     !.github/workflows/
     ```
  2. This allows CI workflow files to be tracked in git while the rest of `.github/` (generated boilerplate) stays ignored.
  3. Add `*.tgz` entry to ignore npm pack artifacts.
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### T2.2 -- Create CI workflow file

- **File:** `.github/workflows/ci.yml` (create)
- **What to implement:**
  1. **Trigger:** `push` to `main` and `pull_request` targeting `main`.
  2. **Job: `lint`** -- Ubuntu latest, Node 22, `npm ci`, `npm run lint`.
  3. **Job: `build-and-test`** -- Ubuntu latest, Node matrix [18, 20, 22], `npm ci`, `npm run build`, `npm run test:coverage`. Upload coverage artifact on Node 22 only.
  4. **Job: `pack-verify`** -- Depends on `build-and-test`. `npm ci`, `npm run build`, `npm pack`, install globally from tarball, run `ia-dev-env --help` and `ia-dev-env --version`.
  5. Use `actions/checkout@v4`, `actions/setup-node@v4` with `cache: npm`, `actions/upload-artifact@v4`.
  6. Full YAML content is specified in `STORY-020-plan.md` section 1.1.
- **Dependencies on other tasks:** T2.1 (`.gitignore` negation must be in place)
- **Estimated complexity:** M

### T2.3 -- Force-add workflow to git

- **What to do:**
  1. After creating the file and updating `.gitignore`, run `git add .github/workflows/ci.yml`.
  2. The negation rule in `.gitignore` should allow normal tracking. If git still ignores it, use `git add --force .github/workflows/ci.yml`.
  3. Verify with `git status` that the file appears as staged.
- **Dependencies on other tasks:** T2.1, T2.2
- **Estimated complexity:** S

### Verification checkpoint G2

```bash
# Verify workflow file exists and is valid YAML
node -e "const yaml = require('js-yaml'); const fs = require('fs'); yaml.load(fs.readFileSync('.github/workflows/ci.yml', 'utf8')); console.log('Valid YAML')"

# Verify .gitignore allows workflows
git check-ignore -v .github/workflows/ci.yml  # should NOT be ignored (exit code 1)

# Verify .gitignore still ignores other .github/ content
git check-ignore -v .github/copilot-instructions.md  # should be ignored

# Compilation still passes
npx tsc --noEmit
```

---

## G3 -- Python Code Removal

**Purpose:** Remove all Python source code, test files, build configuration, and scripts. This is the highest-risk group because it deletes ~25,000 lines across ~130 files. The critical safeguard is that `resources/` and `tests/fixtures/` must remain intact, and all Node.js tests must continue passing after deletion.
**Dependencies:** G1 and G2 should be committed first (so we have a clean rollback point)
**Compiles independently:** Yes (deletion only affects Python files; TypeScript compilation is unaffected)
**Risk:** HIGH

### T3.1 -- Delete Python source directory

- **Directory:** `src/ia_dev_env/` (delete entire tree)
- **What to delete:**
  1. All 39 `.py` files in `src/ia_dev_env/`, `src/ia_dev_env/assembler/`, `src/ia_dev_env/domain/`.
  2. All `__pycache__/` directories with ~39 `.pyc` files.
  3. Total: ~8,000 lines removed.
  4. **Verify:** `src/` still contains the TypeScript source (`src/*.ts`, `src/assembler/*.ts`, `src/domain/*.ts`).
- **Dependencies on other tasks:** None
- **Estimated complexity:** S (simple deletion)

### T3.2 -- Delete pyproject.toml

- **File:** `pyproject.toml` (delete)
- **What to delete:**
  1. Python build configuration file (36 lines).
  2. Replaced by `package.json` which was set up in STORY-001.
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### T3.3 -- Delete Python test files

- **Directories/files to delete:**
  1. `tests/__init__.py`
  2. `tests/conftest.py`
  3. All `tests/test_*.py` files (19 files, ~12,000 lines)
  4. `tests/assembler/__init__.py`
  5. `tests/assembler/conftest.py`
  6. All `tests/assembler/test_*.py` files (19 files, ~4,000 lines)
  7. `tests/domain/__init__.py`
  8. All `tests/domain/test_*.py` files (11 files, ~1,500 lines)
  9. Total: 56 files, ~17,157 lines removed.
- **What to KEEP:**
  1. `tests/fixtures/` -- Used by Node.js tests (YAML configs, templates, reference files).
  2. `tests/golden/` -- Used by Node.js byte-for-byte integration tests.
  3. `tests/helpers/` -- TypeScript test helpers.
  4. `tests/node/` -- TypeScript test files.
  5. `tests/fixtures/project-config.fixture.ts` -- TypeScript fixture.
- **Dependencies on other tasks:** None
- **Estimated complexity:** S (simple deletion, but must be precise about what to keep)

### T3.4 -- Delete scripts directory

- **Directory:** `scripts/` (delete entire directory)
- **What to delete:**
  1. `scripts/generate_golden.py` -- Golden file regeneration (replaced by Node.js test suite).
  2. `scripts/validate-github-structure.py` -- GitHub structure validation (no longer needed).
  3. Total: 2 files, ~500 lines.
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### T3.5 -- Clean .gitignore (remove Python-specific entries)

- **File:** `.gitignore` (modify)
- **What to implement:**
  1. Remove the `# Python` section header comment.
  2. Remove Python-only entries:
     - `__pycache__/`
     - `*.py[cod]`
     - `*.pyo`
     - `*.egg-info/`
     - `*.egg`
     - `.eggs/`
     - `.venv/`
     - `venv/`
     - `.coverage`
     - `.coverage.*`
     - `htmlcov/`
     - `.pytest_cache/`
     - `*.whl`
  3. **KEEP** `coverage/` and `coverage.json` -- also used by vitest v8 coverage reporter.
  4. **KEEP** `node_modules/`, `dist/`, `build/` -- these are in the Python section but are Node.js-relevant. Move them to a general section.
  5. Reorganize remaining entries under clean section headers (e.g., `# Build`, `# Dependencies`, `# Coverage`).
- **Dependencies on other tasks:** T3.1-T3.4 (remove Python code first, then clean gitignore)
- **Estimated complexity:** S

### Verification checkpoint G3

```bash
# Verify no Python files remain in src/
find src -name '*.py' | wc -l     # expect: 0

# Verify no Python test files remain
find tests -name '*.py' | wc -l   # expect: 0

# Verify pyproject.toml is gone
test -f pyproject.toml && echo "FAIL: still exists" || echo "OK: removed"

# Verify scripts/ is gone
test -d scripts && echo "FAIL: still exists" || echo "OK: removed"

# CRITICAL: Verify resources/ is intact (RULE-011)
ls resources/ | wc -l   # expect: 25+ items

# CRITICAL: Verify test fixtures are intact
ls tests/fixtures/ | wc -l   # expect: >10 items

# CRITICAL: Verify golden files are intact
ls tests/golden/ | wc -l     # expect: 8 directories

# TypeScript compilation still passes
npx tsc --noEmit

# All Node.js tests still pass
npm run test

# Coverage unchanged
npm run test:coverage
```

---

## G4 -- Documentation Update

**Purpose:** Rewrite README.md for the Node.js/TypeScript project and update CHANGELOG.md with the migration completion entry. All Python references must be removed. This is low-risk since it only affects documentation files.
**Dependencies:** G3 (Python removal must be complete so documentation reflects the actual state)
**Compiles independently:** N/A (documentation files)
**Risk:** LOW

### T4.1 -- Rewrite README.md

- **File:** `README.md` (full rewrite)
- **What to implement:**
  1. **Header + badges:** Project name, CI status badge (`[![CI](https://github.com/{owner}/{repo}/actions/workflows/ci.yml/badge.svg)](...)`) (placeholder URL if repo URL unknown).
  2. **Quick Start:** `npm install -g ia-dev-environment` or `npx ia-dev-env`.
  3. **Prerequisites:** Node.js >= 18.
  4. **CLI Reference:** `generate` and `validate` commands with all options (`--config`, `--output-dir`, `--interactive`, `--dry-run`, `--resources-dir`).
  5. **Bundled Profiles:** Table of 8 profiles (preserve from current README).
  6. **What's Generated:** Keep existing section describing `.claude/` and `.github/` output.
  7. **Configuration:** Keep YAML config examples (still accurate).
  8. **Development:** `npm install`, `npm run build`, `npm test`, `npm run test:coverage`, `npm run test:integration`, `npm run lint`.
  9. **Project Structure:** TypeScript `src/` layout (not Python).
  10. **Coverage Targets:** 95% line / 90% branch (current: 99.6% / 97.84%).
  11. **License:** MIT.
  12. **Key deletions:** All `pip3`, `python3`, `pytest`, `pip install -e`, `pyproject.toml` references. Remove "Node.js + TypeScript Foundation (Story 001)" transitional section. Remove Python project structure diagram.
- **Reference:** Full section-by-section spec in `STORY-020-plan.md` section 2.3.
- **Dependencies on other tasks:** T3.1-T3.5 (Python must be gone)
- **Estimated complexity:** L

### T4.2 -- Update CHANGELOG.md

- **File:** `CHANGELOG.md` (modify)
- **What to implement:**
  1. Add entries under `[Unreleased]` section:
     ```markdown
     ### Changed
     - **BREAKING: Python removal** -- Removed all Python source code (`src/ia_dev_env/`),
       Python tests (`tests/*.py`, `tests/assembler/*.py`, `tests/domain/*.py`),
       `pyproject.toml`, and Python scripts (`scripts/`). The project is now Node.js/TypeScript only.
     - **README rewrite** -- Updated all documentation to reflect Node.js/TypeScript CLI.
       Removed all Python references.

     ### Added
     - **GitHub Actions CI** -- Lint, build, test workflow with Node.js 18/20/22 matrix.
       Coverage upload on Node 22. Pack verification job.
     - **npm packaging** -- `files` field, `prepublishOnly` script, `test:pack` script.
     ```
  2. Insert these entries between the `## [Unreleased]` header and the existing `### Added` section.
- **Dependencies on other tasks:** T4.1 (README should be updated first)
- **Estimated complexity:** S

### Verification checkpoint G4

```bash
# Verify README has no Python references
grep -ic 'python3\|pip3\|pytest\|pyproject\|pip install' README.md   # expect: 0 (or only in profile table context)

# Verify README has npm installation
grep -c 'npm install' README.md   # expect: > 0

# Verify CHANGELOG has new entries
grep -c 'Python removal' CHANGELOG.md   # expect: 1
grep -c 'GitHub Actions CI' CHANGELOG.md # expect: 1
```

---

## G5 -- Final Verification

**Purpose:** End-to-end verification that all changes work together. Build, test, pack, install, and smoke-test the CLI. Verify resources integrity, tarball contents, and CI workflow validity. This is the final quality gate before the PR.
**Dependencies:** G1, G2, G3, G4 (all previous groups must be complete)
**Compiles independently:** N/A (verification only)
**Risk:** LOW (read-only verification)

### T5.1 -- Full test suite with coverage

- **Command:** `npm run test:coverage`
- **What to verify:**
  1. All 1,384 tests pass (no regressions from Python removal).
  2. Line coverage >= 95%.
  3. Branch coverage >= 90%.
  4. Zero compilation warnings: `npx tsc --noEmit`.
- **Dependencies on other tasks:** All G1-G4 tasks
- **Estimated complexity:** S

### T5.2 -- Build and lint verification

- **Commands:**
  ```bash
  npm run lint     # TypeScript type check -- 0 errors
  npm run build    # dist/index.js created
  ```
- **What to verify:**
  1. `dist/index.js` is created.
  2. Zero TypeScript errors.
- **Dependencies on other tasks:** T5.1
- **Estimated complexity:** S

### T5.3 -- Pack and install verification

- **Commands:**
  ```bash
  npm pack
  npm install -g ./ia-dev-environment-*.tgz
  ia-dev-env --help
  ia-dev-env --version
  ```
- **What to verify:**
  1. `npm pack` produces a `.tgz` file.
  2. Global install succeeds.
  3. `--help` prints usage information.
  4. `--version` prints the version number.
- **Dependencies on other tasks:** T5.2
- **Estimated complexity:** S

### T5.4 -- Tarball content verification

- **Commands:**
  ```bash
  tar tzf ia-dev-environment-*.tgz | grep 'dist/' | head -3      # dist/ files present
  tar tzf ia-dev-environment-*.tgz | grep 'resources/' | head -3  # resources/ files present
  tar tzf ia-dev-environment-*.tgz | grep 'tests/'                # empty (excluded)
  tar tzf ia-dev-environment-*.tgz | grep 'src/'                  # empty (excluded)
  ```
- **What to verify:**
  1. `dist/` is included in tarball.
  2. `resources/` is included in tarball (RULE-011).
  3. `tests/` is NOT included.
  4. `src/` is NOT included.
- **Dependencies on other tasks:** T5.3
- **Estimated complexity:** S

### T5.5 -- Smoke test: generate command

- **Command:**
  ```bash
  ia-dev-env generate --config resources/config-templates/setup-config.python-fastapi.yaml --output-dir /tmp/test-output-020
  ls /tmp/test-output-020 | head -10
  rm -rf /tmp/test-output-020
  ```
- **What to verify:**
  1. Generate command runs without error.
  2. Output directory contains generated files.
  3. Resources are correctly resolved at runtime from the installed package.
- **Dependencies on other tasks:** T5.3
- **Estimated complexity:** S

### T5.6 -- Resources integrity verification

- **Commands:**
  ```bash
  ls resources/ | wc -l                  # expect: 25+ items
  ls tests/golden/ | wc -l              # expect: 8 directories
  ls tests/fixtures/ | wc -l            # expect: >10 items
  tar tzf ia-dev-environment-*.tgz | grep resources | wc -l  # > 100 files
  ```
- **What to verify:**
  1. `resources/` directory structure is intact locally.
  2. `tests/golden/` has all 8 profile directories.
  3. `tests/fixtures/` has all fixture files.
  4. Tarball contains the full resources tree.
- **Dependencies on other tasks:** T5.4
- **Estimated complexity:** S

### T5.7 -- Deletion verification

- **Commands:**
  ```bash
  find src -name '*.py' | wc -l           # 0
  find tests -name '*.py' | wc -l         # 0
  test -f pyproject.toml; echo $?         # 1 (not found)
  test -d scripts; echo $?               # 1 (not found)
  ```
- **What to verify:**
  1. No Python files remain in `src/`.
  2. No Python test files remain in `tests/`.
  3. `pyproject.toml` is gone.
  4. `scripts/` directory is gone.
- **Dependencies on other tasks:** T5.1
- **Estimated complexity:** S

### T5.8 -- CI workflow validation

- **Command:**
  ```bash
  node -e "const yaml = require('js-yaml'); const fs = require('fs'); yaml.load(fs.readFileSync('.github/workflows/ci.yml', 'utf8')); console.log('Valid YAML')"
  ```
- **What to verify:**
  1. Workflow file is valid YAML.
  2. File is tracked by git (not ignored).
- **Dependencies on other tasks:** T5.1
- **Estimated complexity:** S

### T5.9 -- Clean up verification artifacts

- **Commands:**
  ```bash
  rm -f ia-dev-environment-*.tgz
  npm uninstall -g ia-dev-environment
  rm -rf /tmp/test-output-020
  ```
- **What to do:** Remove all temporary artifacts created during verification.
- **Dependencies on other tasks:** T5.3-T5.6
- **Estimated complexity:** S

### Verification checkpoint G5

```bash
# Full pre-merge checklist (one-liner summary)
npm run lint && npm run build && npm run test:coverage && echo "ALL CHECKS PASSED"
```

---

## Summary Table

| Group | Purpose | Files Created | Files Modified | Files Deleted | Tasks | Complexity |
|-------|---------|--------------|----------------|---------------|-------|------------|
| G1 | npm packaging | 0 | 1 (`package.json`) | 0 | 4 | S |
| G2 | CI workflow | 1 (`ci.yml`) | 1 (`.gitignore`) | 0 | 3 | M |
| G3 | Python removal | 0 | 1 (`.gitignore`) | ~133 files | 5 | S (but HIGH risk) |
| G4 | Documentation | 0 | 2 (`README.md`, `CHANGELOG.md`) | 0 | 2 | L |
| G5 | Verification | 0 | 0 | 0 | 9 | S |
| **Total** | | **1 new file** | **4 modified** | **~133 deleted** | **23 tasks** | |

## Dependency Graph

```
G1: NPM PACKAGING (package.json: files, prepublishOnly, description)
  |
  +----> G2: CI WORKFLOW (.gitignore negation, ci.yml)
           |
           +----> G3: PYTHON REMOVAL (delete src/ia_dev_env/, tests/*.py, pyproject.toml, scripts/)
                    |
                    +----> G4: DOCUMENTATION (README.md rewrite, CHANGELOG.md update)
                             |
                             +----> G5: FINAL VERIFICATION (full test suite, pack, smoke tests)
```

**Strictly sequential:** Each group depends on the previous one being committed. This is intentional:
- G2 needs G1's `files` field for the pack-verify job.
- G3 needs G1+G2 committed as a clean rollback point before the risky Python deletion.
- G4 needs G3 complete so documentation reflects the actual project state.
- G5 verifies all changes work together end-to-end.

## Key Risks and Mitigations

| Risk | Level | Mitigation |
|------|-------|------------|
| `resources/` accidentally deleted or modified | HIGH | Explicit `ls resources/` check in G3 and G5. `files` field in G1 ensures pack inclusion. |
| `.gitignore` negation doesn't work for `ci.yml` | MEDIUM | `git check-ignore -v` verification in G2. Fallback: `git add --force`. |
| Python test deletion removes Node.js fixtures | MEDIUM | Precise file-level deletion in T3.3. KEEP list explicitly documented. |
| Coverage drops after Python deletion | LOW | Python files are not counted by vitest. Verified in G5. |
| README rewrite misses Python references | LOW | `grep -i` verification in G4 checkpoint. |

## Commit Strategy

| Commit | Group | Message |
|--------|-------|---------|
| 1 | G1 | `feat(story-020): configure npm packaging (files, prepublishOnly, keywords)` |
| 2 | G2 | `ci(story-020): add GitHub Actions workflow with Node 18/20/22 matrix` |
| 3 | G3 | `refactor(story-020)!: remove Python source, tests, and build config` |
| 4 | G4 | `docs(story-020): rewrite README for Node.js/TypeScript, update CHANGELOG` |
