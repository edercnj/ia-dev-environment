# Test Plan -- STORY-020: CI/CD, Packaging, Documentation

## Summary

STORY-020 is infrastructure/packaging work (CI workflow, npm packaging, Python removal, README rewrite).
It produces no new application code, so there are **no new unit or integration tests to write**.
Testing is entirely **verification-based**: checklists, automated shell commands, and CI pipeline validation.

- New test files: 0
- Existing test suite: 1,384 tests across 46 files (must stay green)
- Coverage: must remain >= 95% line, >= 90% branch (currently 99.6% / 97.84%)
- Verification categories: 6 (CI workflow, npm packaging, Python removal, resources integrity, README, regression)

---

## 1. Verification Checklist

### 1.1 CI Workflow Validity

| # | Check | Pass Criteria |
|---|-------|---------------|
| 1 | `.github/workflows/ci.yml` exists | File present after `.gitignore` negation |
| 2 | Workflow YAML is syntactically valid | Passes YAML lint without errors |
| 3 | Workflow defines 3 jobs: `lint`, `build-and-test`, `pack-verify` | All 3 job keys present |
| 4 | Node matrix includes 18, 20, 22 | `matrix.node-version` contains `[18, 20, 22]` |
| 5 | Coverage upload only on Node 22 | `if: matrix.node-version == 22` condition present |
| 6 | `pack-verify` depends on `build-and-test` | `needs: build-and-test` present |
| 7 | All 3 jobs pass on GitHub Actions | Green status after push |

### 1.2 npm Packaging

| # | Check | Pass Criteria |
|---|-------|---------------|
| 8 | `package.json` has `files: ["dist", "resources"]` | Field present with exact value |
| 9 | `package.json` has `prepublishOnly` script | Script runs `npm run build && npm run test` |
| 10 | `npm pack` generates `.tgz` file | `ia-dev-environment-*.tgz` exists after command |
| 11 | Tarball contains `dist/` | `tar tzf *.tgz \| grep 'package/dist/'` produces output |
| 12 | Tarball contains `resources/` | `tar tzf *.tgz \| grep 'package/resources/'` produces output |
| 13 | Tarball contains `package.json` | `tar tzf *.tgz \| grep 'package/package.json'` produces output |
| 14 | Tarball excludes `src/` | `tar tzf *.tgz \| grep 'package/src/'` produces no output |
| 15 | Tarball excludes `tests/` | `tar tzf *.tgz \| grep 'package/tests/'` produces no output |
| 16 | Tarball excludes `.claude/` | `tar tzf *.tgz \| grep '.claude/'` produces no output |
| 17 | `npx ia-dev-env --help` works from installed tarball | Usage text printed, exit code 0 |
| 18 | `npx ia-dev-env --version` works from installed tarball | Version `0.1.0` printed |
| 19 | `description` field updated | No longer contains "migration" |
| 20 | `keywords` field present | Contains at least `["cli", "ai"]` |

### 1.3 Python Removal

| # | Check | Pass Criteria |
|---|-------|---------------|
| 21 | No `.py` files in `src/` | `find src -name '*.py'` returns empty |
| 22 | No `.pyc` files in `src/` | `find src -name '*.pyc'` returns empty |
| 23 | No `__pycache__/` in `src/` | `find src -name '__pycache__'` returns empty |
| 24 | `src/ia_dev_env/` directory does not exist | `test -d src/ia_dev_env` returns exit code 1 |
| 25 | `pyproject.toml` does not exist | `test -f pyproject.toml` returns exit code 1 |
| 26 | `scripts/` directory does not exist | `test -d scripts` returns exit code 1 |
| 27 | No `.py` test files in `tests/` root | `find tests -maxdepth 1 -name '*.py'` returns empty |
| 28 | No `.py` test files in `tests/assembler/` | `test -d tests/assembler` returns exit code 1 OR dir has no `.py` files |
| 29 | No `.py` test files in `tests/domain/` | `test -d tests/domain` returns exit code 1 OR dir has no `.py` files |
| 30 | `.gitignore` has no Python-only entries | No `__pycache__`, `*.py[cod]`, `.venv`, `*.egg-info`, `.pytest_cache`, `*.whl` lines |
| 31 | `.gitignore` retains `coverage/` and `coverage.json` | These entries still present (shared with vitest) |
| 32 | `.gitignore` has `*.tgz` entry | npm pack artifacts ignored |

### 1.4 Resources Integrity (RULE-011)

| # | Check | Pass Criteria |
|---|-------|---------------|
| 33 | `resources/` directory exists | `test -d resources` returns exit code 0 |
| 34 | `resources/` has 25 entries | `ls resources/ \| wc -l` equals 25 |
| 35 | All 8 config templates present | `ls resources/config-templates/setup-config.*.yaml \| wc -l` equals 8 |
| 36 | `resources/readme-template.md` exists | `test -f resources/readme-template.md` returns 0 |
| 37 | No files modified in `resources/` | `git diff --name-only resources/` returns empty |

### 1.5 README / Documentation

| # | Check | Pass Criteria |
|---|-------|---------------|
| 38 | README contains `npm install` instructions | `grep -q 'npm install' README.md` succeeds |
| 39 | README contains `npx ia-dev-env` usage | `grep -q 'npx ia-dev-env' README.md` succeeds |
| 40 | README contains Node.js prerequisite | `grep -qi 'node' README.md` succeeds |
| 41 | README has no `python3` references | `grep -qi 'python3' README.md` fails (no matches) |
| 42 | README has no `pip3` references | `grep -qi 'pip3' README.md` fails |
| 43 | README has no `pytest` references | `grep -qi 'pytest' README.md` fails |
| 44 | README has no `pyproject.toml` references | `grep -qi 'pyproject.toml' README.md` fails |
| 45 | README has no `pip install -e` references | `grep -q 'pip install' README.md` fails |
| 46 | CHANGELOG.md has `[Unreleased]` entry for STORY-020 | Entry mentions Python removal and CI |

### 1.6 Regression

| # | Check | Pass Criteria |
|---|-------|---------------|
| 47 | All existing tests pass | `npm run test` exits 0, 1,384 tests passing |
| 48 | Coverage meets thresholds | `npm run test:coverage` exits 0 (thresholds enforced by vitest) |
| 49 | TypeScript compiles | `npm run lint` exits 0 (tsc --noEmit) |
| 50 | Build succeeds | `npm run build` exits 0, `dist/index.js` produced |
| 51 | Integration tests pass | `npm run test:integration` exits 0, all 8 profiles pass |
| 52 | Golden files intact | `ls tests/golden/ \| wc -l` equals 8 |
| 53 | Test fixtures intact | `ls tests/fixtures/` shows all expected directories |
| 54 | Test helpers intact | `test -f tests/helpers/integration-constants.ts` returns 0 |

---

## 2. Automated Verification Commands

Run these sequentially after all STORY-020 changes are applied. Each command must exit 0 (unless noted).

```bash
# --- Phase 1: Build & Test Regression ---

# 1. TypeScript compilation
npm run lint

# 2. Build
npm run build

# 3. Full test suite with coverage
npm run test:coverage

# 4. Integration tests specifically
npm run test:integration

# --- Phase 2: Python Removal Verification ---

# 5. No Python source files remain
test "$(find src -name '*.py' -o -name '*.pyc' 2>/dev/null | wc -l)" -eq 0

# 6. Python directories removed
! test -d src/ia_dev_env
! test -f pyproject.toml
! test -d scripts

# 7. No Python test files in tests/ root
test "$(find tests -maxdepth 1 -name '*.py' 2>/dev/null | wc -l)" -eq 0

# --- Phase 3: Resources Integrity (RULE-011) ---

# 8. Resources directory intact
test "$(ls resources/ | wc -l)" -eq 25
test -f resources/readme-template.md
test "$(ls resources/config-templates/setup-config.*.yaml | wc -l)" -eq 8

# 9. No resources modified in this story
git diff --name-only resources/ | wc -l  # Expect: 0

# --- Phase 4: npm Packaging ---

# 10. Pack
npm pack

# 11. Verify tarball contents
tar tzf ia-dev-environment-*.tgz | grep -q 'package/dist/'
tar tzf ia-dev-environment-*.tgz | grep -q 'package/resources/'
tar tzf ia-dev-environment-*.tgz | grep -q 'package/package.json'
! tar tzf ia-dev-environment-*.tgz | grep -q 'package/src/'
! tar tzf ia-dev-environment-*.tgz | grep -q 'package/tests/'

# 12. Install from tarball and smoke test
npm install -g ./ia-dev-environment-*.tgz
ia-dev-env --help
ia-dev-env --version

# 13. Generate command smoke test from packed CLI
TMPOUT=$(mktemp -d)
ia-dev-env generate \
  --config resources/config-templates/setup-config.python-fastapi.yaml \
  --output-dir "$TMPOUT"
test "$(ls "$TMPOUT" | wc -l)" -gt 0
rm -rf "$TMPOUT"

# 14. Cleanup
npm uninstall -g ia-dev-environment
rm -f ia-dev-environment-*.tgz

# --- Phase 5: CI Workflow ---

# 15. CI workflow exists
test -f .github/workflows/ci.yml

# 16. .gitignore allows workflows
grep -q '!.github/workflows/' .gitignore

# --- Phase 6: README Verification ---

# 17. Contains npm instructions, no Python references
grep -q 'npm install' README.md
grep -q 'npx ia-dev-env' README.md
! grep -qi 'python3' README.md
! grep -qi 'pip3' README.md
! grep -qi 'pytest' README.md
! grep -qi 'pyproject.toml' README.md
```

---

## 3. Manual Verification Steps

These require human judgment or external system access:

### 3.1 CI Pipeline (Post-Push)

1. Push branch to remote
2. Open GitHub Actions tab
3. Verify `CI` workflow triggers
4. Verify `lint` job passes
5. Verify `build-and-test` runs on Node 18, 20, and 22 (3 matrix entries)
6. Verify `pack-verify` job passes (runs after `build-and-test`)
7. Verify coverage artifact is uploaded (Node 22 only)
8. Verify total CI time < 5 minutes (per DoD)

### 3.2 README Quality

1. Open `README.md` in a Markdown previewer
2. Verify all sections render correctly (headers, tables, code blocks)
3. Verify Quick Start instructions are copy-pasteable
4. Verify CLI Reference section matches actual `--help` output
5. Verify 8 bundled profiles are listed
6. Verify Development section instructions work (`npm install` -> `npm run build` -> `npm test`)

### 3.3 Package Metadata

1. Run `npm pack --dry-run` and review included files list
2. Verify no sensitive files appear (no `.env`, no credentials, no `.claude/settings.local.json`)
3. Verify `package.json` description is end-user friendly (not migration-focused)

### 3.4 .gitignore Negation

1. Run `git status` after creating `.github/workflows/ci.yml`
2. Verify the workflow file appears as trackable (not ignored)
3. Verify other `.github/` contents (agents, skills, etc.) remain ignored
4. Test: `git check-ignore .github/workflows/ci.yml` should return empty (not ignored)
5. Test: `git check-ignore .github/copilot-instructions.md` should return the path (still ignored)

---

## 4. Regression Risk Areas

### 4.1 HIGH RISK: Accidental Resource Deletion

**What could go wrong:** While deleting `src/ia_dev_env/` (Python source), a developer might accidentally delete or modify files in `resources/` which is a sibling directory under the project root.

**Mitigation:**
- Check #33-37 explicitly validate resources integrity
- `git diff --name-only resources/` must return empty
- Integration tests (8 profiles) exercise all resource templates end-to-end
- The `files` field in `package.json` explicitly includes `resources`

### 4.2 MEDIUM RISK: .gitignore Breaks CI Workflow Tracking

**What could go wrong:** The `.github/` directory is gitignored. If the negation rule `!.github/workflows/` is malformed or missing, the CI workflow file cannot be committed.

**Mitigation:**
- Manual verification step 3.4 uses `git check-ignore` to confirm correct behavior
- Test both positive case (workflow tracked) and negative case (other `.github/` files still ignored)

### 4.3 MEDIUM RISK: Tarball Missing resources/ at Runtime

**What could go wrong:** The `files` field is set to `["dist", "resources"]` but `resources/` could be excluded if the field is misconfigured, or if `npm pack` resolves paths differently than expected.

**Mitigation:**
- Check #11-12 verify tarball contents
- Check #17 runs `ia-dev-env --help` from the installed tarball
- Smoke test (automated step 13) runs a full `generate` command from the installed CLI

### 4.4 LOW RISK: Python File Deletion Leaves Orphan Directories

**What could go wrong:** Deleting `.py` files but not empty parent directories (`tests/assembler/`, `tests/domain/`, `tests/__pycache__/`) leaves empty directories in the repo.

**Mitigation:**
- Checks #24, #28, #29 verify Python directories are fully removed
- `git clean -fd` or explicit `rm -rf` during implementation

### 4.5 LOW RISK: vitest Config Picks Up Stale Python Files

**What could go wrong:** The vitest include pattern `tests/**/*.test.ts` only matches `.test.ts` files, so Python `.py` files would be ignored by vitest. However, stale `__init__.py` or `conftest.py` files could confuse `find`-based tooling.

**Mitigation:**
- Checks #27-29 ensure no `.py` files remain in test directories
- This is cosmetic (vitest ignores them) but important for project hygiene

### 4.6 LOW RISK: Coverage Drop After Python Removal

**What could go wrong:** If the `coverage.include` pattern in `vitest.config.ts` somehow resolves differently after Python source removal. This is unlikely since coverage targets `src/**/*.ts` and Python files are `.py`.

**Mitigation:**
- Check #48 runs `npm run test:coverage` which enforces thresholds
- Current coverage (99.6% line / 97.84% branch) has wide margin above thresholds (95% / 90%)

---

## 5. Execution Order

Run verifications in this order to fail fast on the most critical checks:

1. **Regression first** (Phase 1): If existing tests break, stop immediately
2. **Python removal** (Phase 2): Verify clean removal before packaging
3. **Resources integrity** (Phase 3): Critical safety check (RULE-011)
4. **npm packaging** (Phase 4): Depends on clean build and resources
5. **CI workflow** (Phase 5): Can be validated independently
6. **Documentation** (Phase 6): Lowest risk, validate last
7. **Push and CI** (Manual 3.1): Final validation on real CI infrastructure
