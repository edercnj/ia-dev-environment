# Task Breakdown -- story-0004-0011: CI/CD Artifact Generation

## Summary

This story adds a new `CicdAssembler` to the generation pipeline that produces CI/CD artifacts
(GitHub Actions workflow, Dockerfile, Docker Compose, Kubernetes manifests, smoke test config,
deploy runbook) conditionally based on `container`, `orchestrator`, and `smokeTests` fields in
the project configuration.

**Decomposition Mode:** Layer-based (G1-G6). Tasks grouped by: template creation, assembler
implementation, pipeline integration, golden file updates, test implementation, and verification.

**Total Tasks:** 19
**Estimated Effort:** Large

**Key Design Decisions:**
- Single `CicdAssembler` class with private `generate*` methods per artifact type
- Returns `AssembleResult` (files + warnings) to report skipped artifacts
- Uses `TemplateEngine.renderTemplate()` for Nunjucks (`.njk`) templates
- Pipeline target: `"root"` (artifacts span `.github/workflows/`, root `Dockerfile`, `k8s/`, `docs/runbook/`, `tests/smoke/`)
- Registered before `ReadmeAssembler` in pipeline so `.github/workflows/ci.yml` is counted

---

## G1: TEMPLATE CREATION (Resources)

> **Parallel within group:** Yes -- all TASK-1 through TASK-6 can run in parallel.
> **Dependencies:** None

### TASK-1: Create GitHub Actions CI workflow template

- **Tier:** Mid
- **Budget:** M
- **Group:** G1
- **Parallel:** yes (with TASK-2 through TASK-6)
- **Depends On:** none
- **TDD:** N/A (template file, no TypeScript)

**File to create:** `resources/cicd-templates/ci-workflow/ci.yml.njk`

**Scope:**
1. Create Nunjucks template for `.github/workflows/ci.yml`
2. Template variables from `buildDefaultContext()`: `project_name`, `language_name`, `language_version`, `build_tool`
3. Extra context from `LANGUAGE_COMMANDS`: `compile_cmd`, `build_cmd`, `test_cmd`, `coverage_cmd`
4. Jobs: build, test, lint
5. Stack-aware setup (Node.js for TS, JDK for Java/Kotlin, Python for Python, Go for Go, Rust for Rust)
6. Use `{% if %}` blocks for stack-specific steps (e.g., Gradle cache for Java/Kotlin, npm cache for TS)

**Acceptance Criteria:**
- Template renders without errors for all 8 stack combinations
- Generated YAML is valid GitHub Actions syntax
- Contains build, test, and lint jobs

---

### TASK-2: Create Dockerfile templates (per stack)

- **Tier:** Mid
- **Budget:** L
- **Group:** G1
- **Parallel:** yes (with TASK-1, TASK-3 through TASK-6)
- **Depends On:** none
- **TDD:** N/A (template files, no TypeScript)

**Files to create:**
- `resources/cicd-templates/dockerfile/Dockerfile.java-maven.njk`
- `resources/cicd-templates/dockerfile/Dockerfile.java-gradle.njk`
- `resources/cicd-templates/dockerfile/Dockerfile.kotlin-gradle.njk`
- `resources/cicd-templates/dockerfile/Dockerfile.typescript-npm.njk`
- `resources/cicd-templates/dockerfile/Dockerfile.python-pip.njk`
- `resources/cicd-templates/dockerfile/Dockerfile.go-go.njk`
- `resources/cicd-templates/dockerfile/Dockerfile.rust-cargo.njk`

**Scope:**
1. Multi-stage builds (builder + runtime) for each language/build-tool combination
2. Template variables: `project_name`, `language_version`, `build_tool`, `docker_base_image`, `framework_port`, `health_path`
3. Use `DOCKER_BASE_IMAGES` from `stack-mapping.ts` for base images
4. Include `HEALTHCHECK` instruction using `framework_port` and `health_path`
5. Follow Docker best practices: non-root user, minimal layers, `.dockerignore` awareness

**Acceptance Criteria:**
- 7 Dockerfile templates (one per stack key in `LANGUAGE_COMMANDS`, excluding `csharp-dotnet`)
- Each uses multi-stage build pattern
- Each includes HEALTHCHECK

---

### TASK-3: Create Docker Compose template

- **Tier:** Junior
- **Budget:** S
- **Group:** G1
- **Parallel:** yes (with TASK-1, TASK-2, TASK-4 through TASK-6)
- **Depends On:** none
- **TDD:** N/A (template file, no TypeScript)

**File to create:** `resources/cicd-templates/docker-compose/docker-compose.yml.njk`

**Scope:**
1. Dev environment compose file with app service
2. Template variables: `project_name`, `framework_port`, `database_name`, `cache_name`
3. Conditional `{% if %}` blocks for database and cache services
4. Port mapping, volume mounts for development

**Acceptance Criteria:**
- Valid Docker Compose YAML syntax
- Conditional database/cache service sections
- App service with port mapping from `framework_port`

---

### TASK-4: Create Kubernetes manifest templates

- **Tier:** Mid
- **Budget:** M
- **Group:** G1
- **Parallel:** yes (with TASK-1 through TASK-3, TASK-5, TASK-6)
- **Depends On:** none
- **TDD:** N/A (template files, no TypeScript)

**Files to create:**
- `resources/cicd-templates/k8s/deployment.yaml.njk`
- `resources/cicd-templates/k8s/service.yaml.njk`
- `resources/cicd-templates/k8s/configmap.yaml.njk`

**Scope:**
1. `deployment.yaml.njk`: Deployment referencing project Docker image, health/readiness probes using `health_path` and `framework_port`, resource limits
2. `service.yaml.njk`: ClusterIP Service exposing `framework_port`
3. `configmap.yaml.njk`: Externalized config placeholder with `project_name`

**Acceptance Criteria:**
- Valid Kubernetes YAML manifests
- Deployment uses liveness/readiness probes with correct health path
- Service exposes the correct framework port

---

### TASK-5: Create smoke test config template

- **Tier:** Junior
- **Budget:** S
- **Group:** G1
- **Parallel:** yes (with TASK-1 through TASK-4, TASK-6)
- **Depends On:** none
- **TDD:** N/A (template file, no TypeScript)

**File to create:** `resources/cicd-templates/smoke-tests/smoke-config.md`

**Scope:**
1. Markdown scaffold for smoke test configuration
2. Sections: purpose, endpoints to test, expected responses, execution instructions
3. Stack-agnostic (no Nunjucks rendering needed -- plain Markdown copy)

**Acceptance Criteria:**
- File contains structured smoke test scaffold
- Sections cover basic health check verification

---

### TASK-6: Create deploy runbook template

- **Tier:** Mid
- **Budget:** M
- **Group:** G1
- **Parallel:** yes (with TASK-1 through TASK-5)
- **Depends On:** none
- **TDD:** N/A (template file, no TypeScript)

**File to create:** `resources/cicd-templates/deploy-runbook/deploy-runbook.md.njk`

**Scope:**
1. Nunjucks template following the 7-section structure from story-0004-0003
2. Sections: Service Info, Pre-conditions, Deploy Procedure, Post-Deploy Verification, Rollback Procedure, Troubleshooting, Contacts
3. Template variables: `project_name`, `language_name`, `framework_name`, `container`, `orchestrator`, `database_name`
4. Conditional `{% if %}` blocks for container-specific deploy steps, K8s-specific rollback, database migration steps

**Acceptance Criteria:**
- All 7 mandatory sections present
- Conditional sections for Docker, Kubernetes, database
- Renders correctly with any profile config

---

## G2: CORE ASSEMBLER IMPLEMENTATION

> **Parallel within group:** No -- TASK-7 (RED) must precede TASK-8 (GREEN), which must precede TASK-9 (REFACTOR).
> **Dependencies:** G1 (templates must exist for the assembler to render them)

### TASK-7: Write unit tests for CicdAssembler (RED phase)

- **Tier:** Senior
- **Budget:** L
- **Group:** G2
- **Parallel:** no
- **Depends On:** TASK-1, TASK-2, TASK-3, TASK-4, TASK-5, TASK-6
- **TDD:** RED -- all tests fail (class does not exist yet)

**File to create:** `tests/node/assembler/cicd-assembler.test.ts`

**Scope:**
1. Test setup: create temp directories, mock `ProjectConfig` with various field combinations, instantiate `TemplateEngine` with real `resources/` directory
2. Test cases (minimum):

| Test Name | Scenario | Key Assertion |
|-----------|----------|---------------|
| `assemble_withDockerAndK8s_generatesAllArtifacts` | container=docker, orchestrator=kubernetes, smokeTests=true | Returns 6+ files, 0 warnings |
| `assemble_withDockerNoK8s_skipsK8sManifests` | container=docker, orchestrator=none | No `k8s/` files, warning for K8s skip |
| `assemble_withNoContainer_skipsDockerAndCompose` | container=none | No Dockerfile, no docker-compose.yml, warnings for Docker skip |
| `assemble_withSmokeTestsDisabled_skipsSmoke` | smokeTests=false | No `tests/smoke/` files, warning for smoke skip |
| `assemble_always_generatesCiWorkflow` | Any config | `.github/workflows/ci.yml` always present |
| `assemble_always_generatesDeployRunbook` | Any config | `docs/runbook/deploy-runbook.md` always present |
| `assemble_skippedArtifacts_produceWarnings` | container=none, orchestrator=none, smokeTests=false | warnings array contains skip reasons |
| `assemble_perStack_rendersCorrectBuildCommands` | Each of the 7 stack combos | CI workflow contains correct `test_cmd`, `build_cmd` |
| `assemble_dockerfilePerStack_usesCorrectBaseImage` | Each of the 7 stack combos | Dockerfile contains correct base image from `DOCKER_BASE_IMAGES` |

3. Use Vitest `describe`/`it` structure consistent with existing assembler tests
4. Helper function to build config with override fields

**Acceptance Criteria:**
- All tests written and failing (RED)
- Tests cover all conditional branches (container, orchestrator, smokeTests)
- Tests cover per-stack variations

---

### TASK-8: Implement CicdAssembler class (GREEN phase)

- **Tier:** Senior
- **Budget:** XL
- **Group:** G2
- **Parallel:** no
- **Depends On:** TASK-7
- **TDD:** GREEN -- make all RED tests pass

**File to create:** `src/assembler/cicd-assembler.ts`

**Scope:**
1. Class `CicdAssembler` with `assemble()` method returning `AssembleResult`
2. Private methods:
   - `generateCiWorkflow()` -- always runs, renders `ci.yml.njk`, writes to `.github/workflows/ci.yml`
   - `generateDockerfile()` -- conditional on `config.infrastructure.container === "docker"`, selects template by `${language}-${buildTool}` key
   - `generateDockerCompose()` -- conditional on `config.infrastructure.container === "docker"`
   - `generateK8sManifests()` -- conditional on `config.infrastructure.orchestrator === "kubernetes"`, writes 3 files to `k8s/`
   - `generateSmokeTestConfig()` -- conditional on `config.testing.smokeTests === true`
   - `generateDeployRunbook()` -- always runs, renders `deploy-runbook.md.njk`, writes to `docs/runbook/`
3. Build extra template context from `LANGUAGE_COMMANDS`, `FRAMEWORK_PORTS`, `FRAMEWORK_HEALTH_PATHS`, `DOCKER_BASE_IMAGES`
4. Skipped artifacts add informational strings to `warnings` array
5. Each `generate*` method checks `fs.existsSync()` for template before rendering
6. Use `fs.mkdirSync({ recursive: true })` for output subdirectories

**Imports:**
- `ProjectConfig` from `../models.js`
- `TemplateEngine` from `../template-engine.js`
- `AssembleResult` from `./rules-assembler.js`
- `LANGUAGE_COMMANDS`, `FRAMEWORK_PORTS`, `FRAMEWORK_HEALTH_PATHS`, `DOCKER_BASE_IMAGES`, `DEFAULT_PORT_FALLBACK`, `DEFAULT_HEALTH_PATH`, `DEFAULT_DOCKER_IMAGE` from `../domain/stack-mapping.js`

**Estimated size:** ~120-150 lines (within 250-line class limit).

**Acceptance Criteria:**
- All TASK-7 unit tests pass (GREEN)
- Class follows existing assembler patterns (see `GithubPromptsAssembler`, `CodexAgentsMdAssembler`)
- Returns `AssembleResult` with files and warnings

---

### TASK-9: Refactor CicdAssembler (REFACTOR phase)

- **Tier:** Mid
- **Budget:** S
- **Group:** G2
- **Parallel:** no
- **Depends On:** TASK-8
- **TDD:** REFACTOR -- all tests still pass, code quality improved

**File to modify:** `src/assembler/cicd-assembler.ts`

**Scope:**
1. Extract helper for building extra template context (stack commands, port, health path, Docker image) into a private method `buildStackContext()`
2. Extract template resolution logic (template path selection by stack key) into a private method if repeated
3. Ensure method lengths <= 25 lines
4. Ensure no magic strings -- use named constants for template directory names
5. Review naming: verify intent-revealing names per coding standards

**Acceptance Criteria:**
- All TASK-7 tests still pass
- No method exceeds 25 lines
- No magic strings or numbers
- Class size within 250-line limit

---

## G3: PIPELINE INTEGRATION

> **Parallel within group:** TASK-10 and TASK-11 can run in parallel. TASK-12 depends on both.
> **Dependencies:** G2 (assembler must be implemented)

### TASK-10: Register CicdAssembler in pipeline

- **Tier:** Junior
- **Budget:** S
- **Group:** G3
- **Parallel:** yes (with TASK-11)
- **Depends On:** TASK-8
- **TDD:** RED/GREEN (modify pipeline test to expect 18 assemblers instead of 17, then add registration)

**File to modify:** `src/assembler/pipeline.ts`

**Scope:**
1. Import `CicdAssembler` from `./cicd-assembler.js`
2. Add descriptor to `buildAssemblers()` array at position 17 (before `ReadmeAssembler`):
   ```typescript
   { name: "CicdAssembler", target: "root", assembler: new CicdAssembler() },
   ```
3. Update JSDoc comment to reflect 18 assemblers (was 16/17)

**Acceptance Criteria:**
- `CicdAssembler` registered in pipeline before `ReadmeAssembler`
- Target is `"root"` (assembler manages its own subdirectory structure)

---

### TASK-11: Export CicdAssembler from barrel index

- **Tier:** Junior
- **Budget:** S
- **Group:** G3
- **Parallel:** yes (with TASK-10)
- **Depends On:** TASK-8
- **TDD:** N/A (barrel export)

**File to modify:** `src/assembler/index.ts`

**Scope:**
1. Add export line:
   ```typescript
   // --- STORY-0004-0011: CicdAssembler ---
   export * from "./cicd-assembler.js";
   ```
2. Place after the Codex assembler exports and before the pipeline export

**Acceptance Criteria:**
- `CicdAssembler` importable from `src/assembler/index.js`

---

### TASK-12: Update pipeline test for new assembler count

- **Tier:** Junior
- **Budget:** S
- **Group:** G3
- **Parallel:** no
- **Depends On:** TASK-10, TASK-11
- **TDD:** GREEN (fix existing pipeline test that now expects +1 assembler)

**File to modify:** `tests/node/assembler/pipeline.test.ts`

**Scope:**
1. Update the test that verifies `buildAssemblers()` length (from 17 to 18)
2. Add assertion that `CicdAssembler` is present in the assembler list
3. Verify `CicdAssembler` appears before `ReadmeAssembler` in the ordered list

**Acceptance Criteria:**
- Pipeline test passes with updated assembler count
- Order constraint validated

---

## G4: GOLDEN FILE UPDATES

> **Parallel within group:** Yes -- TASK-13 through TASK-15 can run in parallel.
> **Dependencies:** G3 (pipeline must be complete to regenerate golden files)

### TASK-13: Regenerate golden files for profiles with Docker + K8s

- **Tier:** Junior
- **Budget:** M
- **Group:** G4
- **Parallel:** yes (with TASK-14, TASK-15)
- **Depends On:** TASK-10, TASK-11
- **TDD:** N/A (golden file regeneration)

**Files to modify (7 profile directories -- all have container=docker, orchestrator=kubernetes):**
- `tests/golden/go-gin/`
- `tests/golden/java-quarkus/`
- `tests/golden/java-spring/`
- `tests/golden/kotlin-ktor/`
- `tests/golden/python-fastapi/`
- `tests/golden/rust-axum/`
- `tests/golden/typescript-nestjs/`

**New files per profile (~6 each):**
- `.github/workflows/ci.yml`
- `Dockerfile`
- `docker-compose.yml`
- `k8s/deployment.yaml`
- `k8s/service.yaml`
- `k8s/configmap.yaml`
- `tests/smoke/smoke-config.md`
- `docs/runbook/deploy-runbook.md`

**Scope:**
1. Run the pipeline against each profile config
2. Copy new CI/CD artifacts into the golden directories
3. Verify byte-for-byte match with pipeline output

**Acceptance Criteria:**
- All 7 profiles contain the full set of CI/CD artifacts
- Artifacts are byte-for-byte identical to pipeline output

---

### TASK-14: Regenerate golden files for python-click-cli (Docker, no K8s)

- **Tier:** Junior
- **Budget:** S
- **Group:** G4
- **Parallel:** yes (with TASK-13, TASK-15)
- **Depends On:** TASK-10, TASK-11
- **TDD:** N/A (golden file regeneration)

**Files to modify:** `tests/golden/python-click-cli/`

**New files (no K8s manifests -- orchestrator=none):**
- `.github/workflows/ci.yml`
- `Dockerfile`
- `docker-compose.yml`
- `tests/smoke/smoke-config.md`
- `docs/runbook/deploy-runbook.md`

**Scope:**
1. Run the pipeline against `python-click-cli` config
2. Verify that K8s manifests are NOT generated (orchestrator=none)
3. Copy new artifacts into the golden directory

**Acceptance Criteria:**
- No `k8s/` directory in golden output
- All other CI/CD artifacts present
- Profile serves as regression test for the `orchestrator=none` branch

---

### TASK-15: Update golden README files if counts changed

- **Tier:** Junior
- **Budget:** S
- **Group:** G4
- **Parallel:** yes (with TASK-13, TASK-14)
- **Depends On:** TASK-10, TASK-11
- **TDD:** N/A (golden file regeneration)

**Files to modify:** `tests/golden/*/CLAUDE.md` (8 profiles, if README generation summary changes)

**Scope:**
1. If `ReadmeAssembler` counts `.github/workflows/ci.yml` in the GitHub artifacts total, the generation summary numbers in `CLAUDE.md` will change
2. Regenerate all 8 `CLAUDE.md` golden files
3. Verify the count delta is exactly +1 for the GitHub component (or +0 if `.github/workflows/` is not counted by `countGithubFiles()`)

**Acceptance Criteria:**
- All `CLAUDE.md` golden files match pipeline output
- No unexpected count changes in generation summary

---

## G5: TEST IMPLEMENTATION AND VERIFICATION

> **Parallel within group:** No -- tasks are sequential.
> **Dependencies:** G4 (golden files must be updated)

### TASK-16: Run byte-for-byte integration tests

- **Tier:** Junior
- **Budget:** S
- **Group:** G5
- **Parallel:** no
- **Depends On:** TASK-13, TASK-14, TASK-15
- **TDD:** GREEN (integration tests pass with updated golden files)

**Command:** `npm test -- tests/node/integration/byte-for-byte.test.ts`

**Scope:**
1. Run the byte-for-byte integration test for all 8 profiles
2. Verify all golden files match pipeline output exactly
3. Fix any mismatches by regenerating the specific golden file

**Acceptance Criteria:**
- All 8 profile integration tests pass
- Zero byte-for-byte mismatches

---

### TASK-17: Run full test suite

- **Tier:** Junior
- **Budget:** S
- **Group:** G5
- **Parallel:** no
- **Depends On:** TASK-16
- **TDD:** GREEN (all tests pass)

**Command:** `npm test`

**Scope:**
1. Run the complete test suite (1,384+ tests across 46+ files)
2. Verify zero regressions in existing tests
3. Verify new `cicd-assembler.test.ts` tests all pass
4. Verify pipeline test passes with updated assembler count

**Acceptance Criteria:**
- All tests pass (0 failures)
- No regressions

---

### TASK-18: Verify coverage thresholds

- **Tier:** Junior
- **Budget:** S
- **Group:** G5
- **Parallel:** no
- **Depends On:** TASK-17
- **TDD:** N/A (verification)

**Scope:**
1. Verify line coverage >= 95% (quality gate)
2. Verify branch coverage >= 90% (quality gate)
3. New `cicd-assembler.ts` must have >= 95% line and >= 90% branch coverage
4. Overall coverage should not decrease from baseline (~99.6% lines, ~97.84% branches)

**Acceptance Criteria:**
- Line coverage >= 95%
- Branch coverage >= 90%
- No decrease from baseline

---

## G6: FINAL REVIEW

> **Dependencies:** G5 (all tests pass)

### TASK-19: Code review and cleanup

- **Tier:** Mid
- **Budget:** S
- **Group:** G6
- **Parallel:** no
- **Depends On:** TASK-18
- **TDD:** N/A (review)

**Scope:**
1. Verify RULE-001 (Dual Copy Consistency): CI workflow lands under `.github/workflows/` in the output
2. Verify RULE-002 (Source of Truth): all templates are in `resources/cicd-templates/`
3. Verify RULE-003 (Backward Compatibility): existing profiles unaffected beyond new additive files
4. Verify no wildcard imports, no magic strings, no methods > 25 lines
5. Verify JSDoc on public class and `assemble()` method
6. Verify zero compiler warnings: `npx tsc --noEmit`

**Acceptance Criteria:**
- All rules satisfied
- Clean compilation
- Ready for PR

---

## Dependency Graph

```
G1 (Templates - all parallel):
  TASK-1 (CI workflow) ──────────┐
  TASK-2 (Dockerfiles) ─────────┤
  TASK-3 (Docker Compose) ──────┤
  TASK-4 (K8s manifests) ───────┼──> G2:
  TASK-5 (Smoke config) ────────┤     TASK-7 (RED: tests) ──> TASK-8 (GREEN: impl) ──> TASK-9 (REFACTOR)
  TASK-6 (Deploy runbook) ──────┘                                    │
                                                                     │
                                                              G3:    ▼
                                                   TASK-10 (pipeline reg) ──┐
                                                   TASK-11 (barrel export) ─┤
                                                                            ├──> TASK-12 (pipeline test)
                                                                            │
                                                              G4:           ▼
                                                   TASK-13 (golden: 7 profiles) ──┐
                                                   TASK-14 (golden: click-cli) ───┤
                                                   TASK-15 (golden: READMEs) ─────┘
                                                                                  │
                                                              G5:                 ▼
                                                   TASK-16 (byte-for-byte) ──> TASK-17 (full suite) ──> TASK-18 (coverage)
                                                                                                              │
                                                              G6:                                             ▼
                                                                                                     TASK-19 (review)
```

## Execution Summary

| Task | Group | Tier | Budget | Parallel | Depends On | Files Changed | TDD Phase |
|------|-------|------|--------|----------|------------|---------------|-----------|
| TASK-1 | G1 | Mid | M | yes | none | 1 new | N/A |
| TASK-2 | G1 | Mid | L | yes | none | 7 new | N/A |
| TASK-3 | G1 | Junior | S | yes | none | 1 new | N/A |
| TASK-4 | G1 | Mid | M | yes | none | 3 new | N/A |
| TASK-5 | G1 | Junior | S | yes | none | 1 new | N/A |
| TASK-6 | G1 | Mid | M | yes | none | 1 new | N/A |
| TASK-7 | G2 | Senior | L | no | TASK-1..6 | 1 new | RED |
| TASK-8 | G2 | Senior | XL | no | TASK-7 | 1 new | GREEN |
| TASK-9 | G2 | Mid | S | no | TASK-8 | 1 modified | REFACTOR |
| TASK-10 | G3 | Junior | S | yes | TASK-8 | 1 modified | RED/GREEN |
| TASK-11 | G3 | Junior | S | yes | TASK-8 | 1 modified | N/A |
| TASK-12 | G3 | Junior | S | no | TASK-10,11 | 1 modified | GREEN |
| TASK-13 | G4 | Junior | M | yes | TASK-10,11 | ~56 new | N/A |
| TASK-14 | G4 | Junior | S | yes | TASK-10,11 | ~5 new | N/A |
| TASK-15 | G4 | Junior | S | yes | TASK-10,11 | 0-8 modified | N/A |
| TASK-16 | G5 | Junior | S | no | TASK-13,14,15 | 0 | GREEN |
| TASK-17 | G5 | Junior | S | no | TASK-16 | 0 | GREEN |
| TASK-18 | G5 | Junior | S | no | TASK-17 | 0 | N/A |
| TASK-19 | G6 | Mid | S | no | TASK-18 | 0 | N/A |

**New files:** ~16 (14 templates + 1 assembler + 1 test file)
**Modified files:** ~4 (pipeline.ts, index.ts, pipeline.test.ts, + golden README adjustments)
**New golden files:** ~61 (7 profiles x 8 files + 1 profile x 5 files)
**Total TypeScript source changes:** 3 files (cicd-assembler.ts, pipeline.ts, index.ts)
**Total TypeScript test changes:** 2 files (cicd-assembler.test.ts, pipeline.test.ts)
