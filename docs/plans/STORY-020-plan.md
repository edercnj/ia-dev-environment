# STORY-020 Implementation Plan: CI/CD, Packaging, Documentation

## Status: PLANNING
**Branch:** `feat/STORY-020-cicd-packaging`
**Predecessor:** STORY-019 (integration tests parity -- complete)

---

## 1. Files to Create

### 1.1 GitHub Actions CI Workflow

**File:** `.github/workflows/ci.yml`

> Note: `.github/` is in `.gitignore` (because it is a generated output of the boilerplate).
> The CI workflow is NOT generated output -- it is project infrastructure.
> **Action required:** Either (a) add a negation rule `!.github/workflows/` in `.gitignore`, or
> (b) use `git add --force .github/workflows/ci.yml`.
> Option (a) is cleaner: it communicates intent that workflows are hand-maintained, while the
> rest of `.github/` remains ignored.

```yaml
name: CI
on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 22
          cache: npm
      - run: npm ci
      - run: npm run lint

  build-and-test:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        node-version: [18, 20, 22]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: ${{ matrix.node-version }}
          cache: npm
      - run: npm ci
      - run: npm run build
      - run: npm run test:coverage
      - name: Upload coverage (Node 22 only)
        if: matrix.node-version == 22
        uses: actions/upload-artifact@v4
        with:
          name: coverage-report
          path: coverage/

  pack-verify:
    needs: build-and-test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 22
          cache: npm
      - run: npm ci
      - run: npm run build
      - run: npm pack
      - name: Verify packed CLI works
        run: |
          npm install -g ./ia-dev-environment-*.tgz
          ia-dev-env --help
          ia-dev-env --version
```

### 1.2 .npmignore (Optional -- Alternative: use `files` field)

Not creating `.npmignore`. The `files` field in `package.json` is the preferred allowlist approach
and is already partially in place (see section 2.1). The `files` field is a positive list, which
is safer than `.npmignore` (a negative list that can accidentally include secrets or test files).

---

## 2. Files to Modify

### 2.1 package.json

**Changes:**

| Field | Current | New | Rationale |
|-------|---------|-----|-----------|
| `description` | "Project foundation for ia-dev-environment Node.js + TypeScript migration." | "CLI tool that generates .claude/ and .github/ boilerplate for AI-assisted development environments" | Reflects final product, not migration status |
| `files` | _(missing)_ | `["dist", "resources"]` | Controls what goes into the npm tarball. Only `dist/` (compiled JS) and `resources/` (templates) are needed at runtime |
| `scripts.prepublishOnly` | _(missing)_ | `"npm run build && npm run test"` | Gate: prevents publishing broken packages |
| `scripts.test:pack` | _(missing)_ | `"npm pack && npm install -g ./ia-dev-environment-*.tgz && ia-dev-env --help"` | Local verification of packed CLI |
| `keywords` | _(missing)_ | `["cli", "ai", "claude", "copilot", "boilerplate", "generator"]` | npm discoverability |
| `repository` | _(missing)_ | `{ "type": "git", "url": "..." }` | npm metadata (fill with actual repo URL) |

**Not changed:** `version` stays at `0.1.0`. Version bumping is a release concern, not a CI story.

### 2.2 .gitignore

**Changes:**

1. **Add negation for CI workflow:**
   ```
   # GitHub Copilot — generated output of the boilerplate
   .github/
   !.github/workflows/
   ```

2. **Remove Python-specific entries** (after Python removal is complete):
   Remove or comment out these lines:
   ```
   # Python                    <-- REMOVE entire section
   __pycache__/
   *.py[cod]
   *.pyo
   *.egg-info/
   *.egg
   .eggs/
   .venv/
   venv/
   .coverage
   .coverage.*
   coverage/          <-- KEEP (also used by vitest lcov output)
   coverage.json      <-- KEEP (also used by vitest)
   htmlcov/
   .pytest_cache/
   *.whl
   ```
   **Retained entries:** `coverage/`, `coverage.json` (used by vitest v8 coverage reporter).
   **Removed entries:** All Python-only artifacts (`__pycache__`, `*.py[cod]`, `*.egg*`, `.venv`, `venv`, `.coverage`, `.coverage.*`, `htmlcov`, `.pytest_cache`, `*.whl`).

3. **Add npm pack artifact:**
   ```
   *.tgz
   ```

### 2.3 README.md

**Full rewrite.** The current README is 654 lines documenting the Python CLI. Replace with
a Node.js/TypeScript-focused README covering:

| Section | Content |
|---------|---------|
| Header + badges | Project name, CI status badge, npm version (future), license |
| Quick Start | `npm install -g ia-dev-environment` or `npx ia-dev-env` |
| Prerequisites | Node.js >= 18 |
| CLI Reference | `generate`, `validate` commands with all options |
| Bundled Profiles | Table of 8 profiles (keep existing table) |
| Development | `npm install`, `npm run build`, `npm test`, `npm run test:coverage` |
| Project Structure | TypeScript `src/` layout (not Python) |
| Golden File Verification | `npm run test:integration` (not `python3 scripts/generate_golden.py`) |
| Coverage Targets | 95% line / 90% branch (current: 99.6% / 97.92%) |
| What's Generated | Keep existing section (still accurate) |
| Architecture | Update "Assembly Pipeline" to reference TypeScript, remove "Python CLI" references |
| Configuration | Keep YAML examples (unchanged) |
| License | MIT |

**Key deletions from README:**
- All `pip3`, `python3`, `pytest`, `pip install -e` references
- "Node.js + TypeScript Foundation (Story 001)" transitional section
- Python project structure diagram
- Python test commands
- `pyproject.toml` references

### 2.4 CHANGELOG.md

**Add entry under `[Unreleased]`:**

```markdown
### Changed
- **BREAKING: Python removal** — Removed all Python source code (`src/ia_dev_env/`),
  Python tests (`tests/*.py`, `tests/assembler/*.py`, `tests/domain/*.py`),
  `pyproject.toml`, and Python scripts (`scripts/`). The project is now Node.js/TypeScript only.
- **README rewrite** — Updated all documentation to reflect Node.js/TypeScript CLI.
  Removed all Python references.

### Added
- **GitHub Actions CI** — Lint, build, test workflow with Node.js 18/20/22 matrix.
  Coverage upload on Node 22. Pack verification job.
- **npm packaging** — `files` field, `prepublishOnly` script, `test:pack` script.
```

---

## 3. Files/Directories to Delete

### 3.1 Python Source Code

**Directory:** `src/ia_dev_env/` (entire tree)

| Path | Files | Lines |
|------|-------|-------|
| `src/ia_dev_env/*.py` | 9 files | ~2,500 |
| `src/ia_dev_env/assembler/*.py` | 17 files | ~3,500 |
| `src/ia_dev_env/domain/*.py` | 11 files | ~2,000 |
| `src/ia_dev_env/**/__pycache__/` | ~39 .pyc files | (binary) |
| **Total** | **37 .py + 39 .pyc** | **~8,000** |

### 3.2 Python Build Configuration

| File | Rationale |
|------|-----------|
| `pyproject.toml` | Python build config, replaced by `package.json` |

### 3.3 Python Test Files

| Path | Files | Lines |
|------|-------|-------|
| `tests/__init__.py` | 1 | ~0 |
| `tests/conftest.py` | 1 | ~100 |
| `tests/test_*.py` | 19 files | ~12,000 |
| `tests/assembler/__init__.py` | 1 | ~0 |
| `tests/assembler/conftest.py` | 1 | ~200 |
| `tests/assembler/test_*.py` | 19 files | ~4,000 |
| `tests/domain/__init__.py` | 1 | ~0 |
| `tests/domain/test_*.py` | 11 files | ~1,500 |
| **Total** | **54 files** | **~17,157** |

### 3.4 Python Scripts

| File | Rationale |
|------|-----------|
| `scripts/generate_golden.py` | Golden file regeneration -- replaced by Node.js test suite |
| `scripts/validate-github-structure.py` | GitHub structure validation -- no longer needed |

**Delete entire `scripts/` directory** (only contains these 2 Python files).

### 3.5 Virtual Environment

| Path | Rationale |
|------|-----------|
| `.venv/` | Python virtual environment -- local only, already in `.gitignore` |

This is already gitignored, so no git action needed. Local cleanup only.

### 3.6 Summary: Deletion Count

| Category | Files | Lines Removed |
|----------|-------|---------------|
| Python source (`src/ia_dev_env/`) | 37 .py + 39 .pyc | ~8,000 |
| Python build config | 1 | 36 |
| Python tests | 54 | ~17,157 |
| Python scripts | 2 | ~500 |
| **Total** | **133 files** | **~25,693** |

---

## 4. Risk Assessment

### 4.1 CRITICAL: Resources Must Stay Intact (RULE-011)

**Risk Level: MEDIUM**
**Mitigation: Explicit verification step**

The `resources/` directory is a **runtime dependency** -- it contains templates, config profiles,
and reference documents that the CLI copies into generated output. It is NOT Python-specific.

| Check | Command | Expected |
|-------|---------|----------|
| Resources exist after deletion | `ls resources/` | 25 subdirectories + `readme-template.md` |
| No Python imports of resources | N/A | Resources are loaded by path, not imported |
| `npm pack` includes resources | `tar tzf ia-dev-environment-*.tgz \| grep resources \| head -5` | Files listed |
| Integration tests pass | `npm run test:integration` | All 8 profiles generate correctly |

**Key safeguard:** The `files` field in `package.json` explicitly lists `["dist", "resources"]`.
The `tsup` build bundles `src/` into `dist/`. Resources are copied as-is.

### 4.2 Test Fixtures Shared Between Python and Node

**Risk Level: LOW**

The `tests/fixtures/` directory contains YAML configs, markdown templates, and reference files
used by **both** Python and Node.js test suites. After Python test deletion:

- `tests/fixtures/*.yaml` -- Used by Node tests (`tests/node/`) -- KEEP
- `tests/fixtures/templates/` -- Used by Node template-engine tests -- KEEP
- `tests/fixtures/reference/` -- Used by Node tests -- KEEP
- `tests/fixtures/integration/` -- Used by Node integration tests -- KEEP
- `tests/fixtures/src/` -- Used by Node assembler tests -- KEEP
- `tests/fixtures/project-config.fixture.ts` -- TypeScript fixture -- KEEP

**Action:** Keep entire `tests/fixtures/` directory. Only delete `.py` files.

### 4.3 Golden Files

**Risk Level: LOW**

The `tests/golden/` directory (8 profile subdirectories) is used by Node.js integration tests
(`tests/node/integration/byte-for-byte.test.ts`). These are NOT Python-generated anymore
(they were regenerated during STORY-019).

**Action:** Keep `tests/golden/` intact. Delete only `scripts/generate_golden.py`.

### 4.4 CI Workflow in .gitignore

**Risk Level: MEDIUM**
**Mitigation: .gitignore negation rule**

`.github/` is currently gitignored because it is generated boilerplate output.
The CI workflow (`ci.yml`) is NOT generated -- it is hand-maintained project infrastructure.

**Action:** Add negation rule `!.github/workflows/` to `.gitignore` before creating the workflow.
This allows workflows to be tracked while keeping the rest of `.github/` ignored.

### 4.5 Node 18 Compatibility

**Risk Level: LOW**

The project already targets Node 18 (`engines: ">=18"`, `tsup target: "node18"`).
The CI matrix tests 18, 20, 22. STORY-019 already fixed Node 18 compat issues
(commit `b962b5d`).

### 4.6 vitest Coverage on CI

**Risk Level: LOW**

Coverage thresholds are configured in `vitest.config.ts` (lines: 95, branches: 90).
The `test:coverage` script already enforces these. CI will fail if coverage drops.

Note: vitest uses `pool: "forks"` with `maxForks: 3` to prevent OOM. GitHub Actions
runners typically have 7GB RAM -- sufficient for 3 forked workers.

---

## 5. Implementation Order

Execute in this sequence to minimize risk:

### Phase 1: Packaging (Low Risk)
1. Update `package.json` (`files`, `description`, `prepublishOnly`, `keywords`)
2. Run `npm pack` locally and verify tarball contents
3. Verify `npx ia-dev-env --help` works from packed tarball

### Phase 2: CI Workflow (Medium Risk)
4. Update `.gitignore` -- add `!.github/workflows/` negation and `*.tgz`
5. Create `.github/workflows/ci.yml`
6. Verify workflow syntax with `act` or by push to remote

### Phase 3: Python Removal (Highest Risk)
7. Delete `src/ia_dev_env/` (Python source)
8. Delete `pyproject.toml`
9. Delete Python test files (`tests/*.py`, `tests/assembler/*.py`, `tests/domain/*.py`)
10. Delete `scripts/` directory
11. Clean `.gitignore` (remove Python-specific entries)
12. Run `npm run test:coverage` -- verify 1,384 tests still pass, coverage unchanged
13. Run `npm run build` -- verify TypeScript compilation unaffected
14. Run `npm run lint` -- verify no broken imports

### Phase 4: Documentation (Low Risk)
15. Rewrite `README.md`
16. Update `CHANGELOG.md`

### Phase 5: Final Verification
17. Full test suite: `npm run test:coverage`
18. Build: `npm run build`
19. Pack and install: `npm pack && npm install -g ./ia-dev-environment-*.tgz`
20. Smoke test: `ia-dev-env --help`, `ia-dev-env --version`
21. Smoke test: `ia-dev-env generate --config resources/config-templates/setup-config.python-fastapi.yaml --output-dir /tmp/test-output`
22. Verify `resources/` presence in tarball: `tar tzf ia-dev-environment-*.tgz | grep resources | wc -l`

---

## 6. Verification Steps

### 6.1 Pre-Merge Checklist

| # | Check | Command | Expected |
|---|-------|---------|----------|
| 1 | TypeScript compiles | `npm run lint` | 0 errors |
| 2 | Build succeeds | `npm run build` | `dist/index.js` created |
| 3 | All tests pass | `npm run test` | 1,384 passing |
| 4 | Coverage meets thresholds | `npm run test:coverage` | >= 95% line, >= 90% branch |
| 5 | No Python files in `src/` | `find src -name '*.py' \| wc -l` | 0 |
| 6 | No Python test files | `find tests -name '*.py' \| wc -l` | 0 |
| 7 | `pyproject.toml` gone | `test -f pyproject.toml` | Exit code 1 (not found) |
| 8 | `scripts/` gone | `test -d scripts` | Exit code 1 (not found) |
| 9 | `resources/` intact | `ls resources/ \| wc -l` | 25 (dirs + readme-template.md) |
| 10 | Pack includes `dist/` | `tar tzf *.tgz \| grep 'dist/' \| head -3` | Files listed |
| 11 | Pack includes `resources/` | `tar tzf *.tgz \| grep 'resources/' \| head -3` | Files listed |
| 12 | Pack excludes tests | `tar tzf *.tgz \| grep 'tests/'` | Empty (no output) |
| 13 | Pack excludes `src/` | `tar tzf *.tgz \| grep 'src/'` | Empty (no output) |
| 14 | CLI works from pack | `npm install -g ./ia-dev-environment-*.tgz && ia-dev-env --help` | Usage printed |
| 15 | README has no Python refs | `grep -i 'python3\|pip3\|pytest\|pyproject' README.md` | Empty |
| 16 | CI workflow valid YAML | `npx yaml-lint .github/workflows/ci.yml` (or similar) | Valid |
| 17 | Golden files intact | `ls tests/golden/ \| wc -l` | 8 |
| 18 | Test fixtures intact | `ls tests/fixtures/ \| wc -l` | >10 |

### 6.2 Post-Push CI Verification

After pushing to remote, verify all 3 CI jobs pass:
1. `lint` -- TypeScript type check
2. `build-and-test` -- Node 18, 20, 22 matrix
3. `pack-verify` -- CLI works from packed tarball

---

## 7. Out of Scope

| Item | Rationale |
|------|-----------|
| npm publish | This story sets up packaging; actual publishing is a release workflow |
| Version bump | Stays at 0.1.0; version management is a separate concern |
| Codecov/Coveralls integration | Coverage is uploaded as artifact; third-party integration is future work |
| Docker image | Not required by story requirements |
| Release workflow | Separate from CI; would include version tagging and npm publish |
| `.venv/` deletion | Already gitignored; local cleanup is developer responsibility |
