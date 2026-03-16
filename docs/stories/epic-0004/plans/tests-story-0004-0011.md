# Test Plan — story-0004-0011: CI/CD Artifact Generation

## Summary

This story introduces a new `CicdAssembler` that generates CI/CD artifacts (GitHub Actions workflow, Dockerfile, Docker Compose, Kubernetes manifests, smoke test config, deploy runbook) conditionally based on `ProjectConfig` fields `infrastructure.container`, `infrastructure.orchestrator`, and `testing.smokeTests`. The assembler uses Nunjucks templates from `resources/cicd-templates/`, returns `AssembleResult` with files and warnings, and is registered in the pipeline before `ReadmeAssembler`.

- **Total new test file:** 1 (`tests/node/assembler/cicd-assembler.test.ts`)
- **Total new test methods:** ~52 (estimated)
- **Categories covered:** Unit (assembler logic, conditional branches, template rendering), Integration (golden file byte-for-byte parity)
- **Estimated line coverage:** >95%
- **Estimated branch coverage:** >90%

---

## 1. Test Strategy Overview

| Category | Scope | New Tests? | Test File |
|----------|-------|------------|-----------|
| Unit — conditional generation | Verify each artifact type generated/skipped based on config flags | YES | `tests/node/assembler/cicd-assembler.test.ts` |
| Unit — always-generated artifacts | CI workflow and deploy runbook generated regardless of config | YES | `tests/node/assembler/cicd-assembler.test.ts` |
| Unit — template rendering | Variable substitution correctness per stack | YES | `tests/node/assembler/cicd-assembler.test.ts` |
| Unit — warnings on skip | Skipped artifacts produce informational warnings | YES | `tests/node/assembler/cicd-assembler.test.ts` |
| Unit — edge cases | Missing template files, unknown language, fallback values | YES | `tests/node/assembler/cicd-assembler.test.ts` |
| Integration — golden file parity | Pipeline output matches golden files byte-for-byte for all 8 profiles | NO (existing) | `tests/node/integration/byte-for-byte.test.ts` |
| Integration — pipeline registration | CicdAssembler present in `buildAssemblers()` array | YES | `tests/node/assembler/pipeline.test.ts` (extend existing) |

---

## 2. New Tests — Unit: `tests/node/assembler/cicd-assembler.test.ts`

This file follows the established assembler test pattern (see `hooks-assembler.test.ts`, `github-prompts-assembler.test.ts`): `beforeEach` creates a temp directory with `resourcesDir` containing necessary templates, `afterEach` cleans up.

### 2.0 Test Fixtures

```typescript
// Helper: buildConfig with overrides for container, orchestrator, smokeTests, language, buildTool
function buildConfig(overrides: {
  language?: string;
  buildTool?: string;
  framework?: string;
  languageVersion?: string;
  container?: string;
  orchestrator?: string;
  smokeTests?: boolean;
} = {}): ProjectConfig { /* ... */ }

// Helper: create Nunjucks template files in resourcesDir
function createCicdTemplates(resourcesDir: string, options: {
  ciWorkflow?: boolean;
  dockerfile?: boolean;
  dockerCompose?: boolean;
  k8s?: boolean;
  smokeTests?: boolean;
  deployRunbook?: boolean;
}): void { /* ... */ }
```

### 2.1 Always-Generated Artifacts (Degenerate Cases — TPP Step 1)

These tests validate the simplest transformation: artifacts that are generated unconditionally regardless of config flags.

| # | ID | Test Name | Description | Input/Setup | Expected Output | Depends On | Parallel |
|---|-----|-----------|-------------|-------------|-----------------|------------|----------|
| 1 | UT-1 | `assemble_minimalConfig_generatesCiWorkflow` | CI workflow is always generated for any valid config | Config with `container: "none"`, `orchestrator: "none"`, `smokeTests: false`; all templates present | Result files include `.github/workflows/ci.yml` path; file exists on disk | — | yes |
| 2 | UT-2 | `assemble_minimalConfig_generatesDeployRunbook` | Deploy runbook is always generated for any valid config | Same minimal config as UT-1; deploy runbook template present | Result files include `docs/runbook/deploy-runbook.md` path; file exists on disk | — | yes |
| 3 | UT-3 | `assemble_minimalConfig_resultIsAssembleResult` | Return type is `AssembleResult` with `files` and `warnings` arrays | Minimal config | Result has `files` (array of strings) and `warnings` (array of strings) | — | yes |

### 2.2 Conditional Generation — Docker Artifacts (TPP Step 2: Simple Conditions)

| # | ID | Test Name | Description | Input/Setup | Expected Output | Depends On | Parallel |
|---|-----|-----------|-------------|-------------|-----------------|------------|----------|
| 4 | UT-4 | `assemble_containerDocker_generatesDockerfile` | Dockerfile generated when `container === "docker"` | Config: `container: "docker"`, `language: "typescript"`, `buildTool: "npm"` | Result files include `Dockerfile` path; file exists on disk | — | yes |
| 5 | UT-5 | `assemble_containerDocker_generatesDockerCompose` | Docker Compose generated when `container === "docker"` | Same config as UT-4 | Result files include `docker-compose.yml` path; file exists on disk | — | yes |
| 6 | UT-6 | `assemble_containerNone_skipsDockerfile` | Dockerfile NOT generated when `container === "none"` | Config: `container: "none"` | Result files do NOT include any `Dockerfile` path; file does NOT exist on disk | — | yes |
| 7 | UT-7 | `assemble_containerNone_skipsDockerCompose` | Docker Compose NOT generated when `container === "none"` | Config: `container: "none"` | Result files do NOT include any `docker-compose.yml` path; file does NOT exist on disk | — | yes |

### 2.3 Conditional Generation — Kubernetes Manifests (TPP Step 3: Another Simple Condition)

| # | ID | Test Name | Description | Input/Setup | Expected Output | Depends On | Parallel |
|---|-----|-----------|-------------|-------------|-----------------|------------|----------|
| 8 | UT-8 | `assemble_orchestratorKubernetes_generatesDeployment` | K8s deployment generated when `orchestrator === "kubernetes"` | Config: `container: "docker"`, `orchestrator: "kubernetes"` | Result files include `k8s/deployment.yaml` path; file exists on disk | — | yes |
| 9 | UT-9 | `assemble_orchestratorKubernetes_generatesService` | K8s service generated when `orchestrator === "kubernetes"` | Same config as UT-8 | Result files include `k8s/service.yaml` path; file exists on disk | — | yes |
| 10 | UT-10 | `assemble_orchestratorKubernetes_generatesConfigMap` | K8s configmap generated when `orchestrator === "kubernetes"` | Same config as UT-8 | Result files include `k8s/configmap.yaml` path; file exists on disk | — | yes |
| 11 | UT-11 | `assemble_orchestratorNone_skipsK8sManifests` | K8s manifests NOT generated when `orchestrator === "none"` | Config: `orchestrator: "none"` | Result files do NOT include any `k8s/` paths; `k8s/` directory does NOT exist | — | yes |

### 2.4 Conditional Generation — Smoke Tests (TPP Step 4: Boolean Condition)

| # | ID | Test Name | Description | Input/Setup | Expected Output | Depends On | Parallel |
|---|-----|-----------|-------------|-------------|-----------------|------------|----------|
| 12 | UT-12 | `assemble_smokeTestsTrue_generatesSmokeConfig` | Smoke test config generated when `smokeTests === true` | Config: `smokeTests: true` | Result files include `tests/smoke/` path; file(s) exist on disk | — | yes |
| 13 | UT-13 | `assemble_smokeTestsFalse_skipsSmokeConfig` | Smoke test config NOT generated when `smokeTests === false` | Config: `smokeTests: false` | Result files do NOT include `tests/smoke/` paths; directory does NOT exist | — | yes |

### 2.5 Full Config — All Artifacts Generated (TPP Step 5: Compound)

| # | ID | Test Name | Description | Input/Setup | Expected Output | Depends On | Parallel |
|---|-----|-----------|-------------|-------------|-----------------|------------|----------|
| 14 | UT-14 | `assemble_fullConfig_generatesAllArtifacts` | All 6 artifact types generated with docker + k8s + smoke tests | Config: `container: "docker"`, `orchestrator: "kubernetes"`, `smokeTests: true` | Result files count includes CI workflow + Dockerfile + Docker Compose + 3 K8s files + smoke config + deploy runbook (8+ files) | — | yes |
| 15 | UT-15 | `assemble_fullConfig_noWarnings` | No skip warnings when all conditions are met | Same full config as UT-14 | `result.warnings` is empty array | UT-14 | yes |

### 2.6 Partial Config — Docker Without K8s (TPP Step 6: Mixed Conditions)

| # | ID | Test Name | Description | Input/Setup | Expected Output | Depends On | Parallel |
|---|-----|-----------|-------------|-------------|-----------------|------------|----------|
| 16 | UT-16 | `assemble_dockerNoK8s_generatesDockerSkipsK8s` | Docker artifacts present, K8s absent | Config: `container: "docker"`, `orchestrator: "none"`, `smokeTests: true` | Has Dockerfile, docker-compose.yml; NO k8s/ files | — | yes |
| 17 | UT-17 | `assemble_dockerNoK8s_warnsAboutK8sSkip` | Warning emitted for skipped K8s | Same config as UT-16 | `result.warnings` contains entry mentioning K8s skip | UT-16 | yes |

### 2.7 Minimal Config — No Docker, No K8s, No Smoke (TPP Step 7: Maximum Skip)

| # | ID | Test Name | Description | Input/Setup | Expected Output | Depends On | Parallel |
|---|-----|-----------|-------------|-------------|-----------------|------------|----------|
| 18 | UT-18 | `assemble_allDisabled_onlyGeneratesCiAndRunbook` | Only unconditional artifacts generated | Config: `container: "none"`, `orchestrator: "none"`, `smokeTests: false` | Result files include only CI workflow + deploy runbook (2 files); no Dockerfile, docker-compose, k8s, smoke | — | yes |
| 19 | UT-19 | `assemble_allDisabled_warnsAboutSkippedArtifacts` | Multiple skip warnings emitted | Same config as UT-18 | `result.warnings` contains entries for Docker, K8s, and smoke test skips | UT-18 | yes |

### 2.8 Template Variable Substitution (TPP Step 8: Content Correctness)

| # | ID | Test Name | Description | Input/Setup | Expected Output | Depends On | Parallel |
|---|-----|-----------|-------------|-------------|-----------------|------------|----------|
| 20 | UT-20 | `assemble_typescriptNpm_ciWorkflowContainsNodeSetup` | CI workflow uses correct language setup for TypeScript | Config: `language: "typescript"`, `buildTool: "npm"` | CI workflow content contains `node` or `Node.js`; contains `npm test` or test command | — | yes |
| 21 | UT-21 | `assemble_typescriptNpm_dockerfileContainsNodeImage` | Dockerfile uses correct base image for TypeScript | Config: `language: "typescript"`, `buildTool: "npm"`, `container: "docker"` | Dockerfile content contains `node:` base image reference | — | yes |
| 22 | UT-22 | `assemble_javaGradle_ciWorkflowContainsJavaSetup` | CI workflow uses correct language setup for Java | Config: `language: "java"`, `buildTool: "gradle"` | CI workflow content contains `java` and `gradle` references | — | yes |
| 23 | UT-23 | `assemble_javaGradle_dockerfileContainsTemurinImage` | Dockerfile uses correct base image for Java | Config: `language: "java"`, `buildTool: "gradle"`, `container: "docker"` | Dockerfile content contains `eclipse-temurin` base image reference | — | yes |
| 24 | UT-24 | `assemble_goGo_ciWorkflowContainsGoSetup` | CI workflow uses correct language setup for Go | Config: `language: "go"`, `buildTool: "go"` | CI workflow content contains `go` build/test references | — | yes |
| 25 | UT-25 | `assemble_pythonPip_ciWorkflowContainsPythonSetup` | CI workflow uses correct language setup for Python | Config: `language: "python"`, `buildTool: "pip"` | CI workflow content contains `python` and `pytest` references | — | yes |
| 26 | UT-26 | `assemble_rustCargo_ciWorkflowContainsRustSetup` | CI workflow uses correct language setup for Rust | Config: `language: "rust"`, `buildTool: "cargo"` | CI workflow content contains `rust` and `cargo` references | — | yes |
| 27 | UT-27 | `assemble_kotlinGradle_ciWorkflowContainsKotlinSetup` | CI workflow uses correct language setup for Kotlin | Config: `language: "kotlin"`, `buildTool: "gradle"` | CI workflow content contains `kotlin` and `gradle` references | — | yes |
| 28 | UT-28 | `assemble_anyConfig_ciWorkflowContainsNoRawPlaceholders` | All Nunjucks placeholders resolved in CI workflow | Full config with any language | CI workflow file content does NOT contain `{{` or `}}` | — | yes |
| 29 | UT-29 | `assemble_anyConfig_dockerfileContainsNoRawPlaceholders` | All Nunjucks placeholders resolved in Dockerfile | Config with `container: "docker"` | Dockerfile content does NOT contain `{{` or `}}` | — | yes |
| 30 | UT-30 | `assemble_anyConfig_deployRunbookContainsNoRawPlaceholders` | All Nunjucks placeholders resolved in deploy runbook | Any valid config | Deploy runbook content does NOT contain `{{` or `}}` | — | yes |

### 2.9 K8s Template Variable Substitution

| # | ID | Test Name | Description | Input/Setup | Expected Output | Depends On | Parallel |
|---|-----|-----------|-------------|-------------|-----------------|------------|----------|
| 31 | UT-31 | `assemble_k8sDeployment_containsProjectName` | K8s deployment references the project name | Config: `orchestrator: "kubernetes"`, `project.name: "my-app"` | `deployment.yaml` content contains `my-app` | — | yes |
| 32 | UT-32 | `assemble_k8sDeployment_containsContainerPort` | K8s deployment uses correct framework port | Config: `orchestrator: "kubernetes"`, `framework: "nestjs"` | `deployment.yaml` content contains port `3000` (nestjs default) | — | yes |
| 33 | UT-33 | `assemble_k8sDeployment_containsHealthCheck` | K8s deployment includes health check path | Config: `orchestrator: "kubernetes"`, `framework: "spring-boot"` | `deployment.yaml` content contains `/actuator/health` | — | yes |
| 34 | UT-34 | `assemble_k8sService_containsProjectName` | K8s service references the project name | Config: `orchestrator: "kubernetes"`, `project.name: "my-app"` | `service.yaml` content contains `my-app` | — | yes |

### 2.10 Dockerfile Content Validation

| # | ID | Test Name | Description | Input/Setup | Expected Output | Depends On | Parallel |
|---|-----|-----------|-------------|-------------|-----------------|------------|----------|
| 35 | UT-35 | `assemble_dockerfile_containsMultiStageBuild` | Dockerfile uses multi-stage build pattern | Config: `container: "docker"` | Dockerfile content contains at least 2 `FROM` directives (builder + runtime) | — | yes |
| 36 | UT-36 | `assemble_dockerfile_containsHealthCheck` | Dockerfile includes a HEALTHCHECK directive | Config: `container: "docker"` | Dockerfile content contains `HEALTHCHECK` or health check instruction | — | yes |

### 2.11 Deploy Runbook Content Validation

| # | ID | Test Name | Description | Input/Setup | Expected Output | Depends On | Parallel |
|---|-----|-----------|-------------|-------------|-----------------|------------|----------|
| 37 | UT-37 | `assemble_deployRunbook_containsProcedureSection` | Deploy runbook has deploy procedure section | Any valid config | Runbook content contains "Deploy Procedure" or "Procedure" heading | — | yes |
| 38 | UT-38 | `assemble_deployRunbook_containsVerificationSection` | Deploy runbook has verification section | Any valid config | Runbook content contains "Verification" heading | — | yes |
| 39 | UT-39 | `assemble_deployRunbook_containsRollbackSection` | Deploy runbook has rollback section | Any valid config | Runbook content contains "Rollback" heading | — | yes |
| 40 | UT-40 | `assemble_deployRunbook_containsProjectName` | Deploy runbook includes project-specific information | Config with `project.name: "my-app"` | Runbook content contains `my-app` | — | yes |

### 2.12 Directory Creation

| # | ID | Test Name | Description | Input/Setup | Expected Output | Depends On | Parallel |
|---|-----|-----------|-------------|-------------|-----------------|------------|----------|
| 41 | UT-41 | `assemble_createsWorkflowsDirectory` | `.github/workflows/` directory created | Any valid config; templates present | `.github/workflows/` directory exists after assemble | — | yes |
| 42 | UT-42 | `assemble_createsK8sDirectory_whenK8sEnabled` | `k8s/` directory created when orchestrator is kubernetes | Config: `orchestrator: "kubernetes"` | `k8s/` directory exists after assemble | — | yes |
| 43 | UT-43 | `assemble_createsRunbookDirectory` | `docs/runbook/` directory created | Any valid config | `docs/runbook/` directory exists after assemble | — | yes |
| 44 | UT-44 | `assemble_createsSmokeDirectory_whenEnabled` | `tests/smoke/` directory created when smoke tests enabled | Config: `smokeTests: true` | `tests/smoke/` directory exists after assemble | — | yes |

### 2.13 Edge Cases — Missing Templates and Fallbacks (TPP Step 9)

| # | ID | Test Name | Description | Input/Setup | Expected Output | Depends On | Parallel |
|---|-----|-----------|-------------|-------------|-----------------|------------|----------|
| 45 | UT-45 | `assemble_ciTemplatesMissing_returnsEmptyWithWarning` | Graceful handling when CI template directory is missing | No template files in resourcesDir | Result files empty or missing CI artifact; warning present | — | yes |
| 46 | UT-46 | `assemble_dockerfileTemplateMissing_returnsEmptyWithWarning` | Graceful handling when Dockerfile template is missing for the stack | Config: `container: "docker"`, `language: "unknown"`; no matching Dockerfile template | Dockerfile not generated; warning present | — | yes |
| 47 | UT-47 | `assemble_unknownFramework_usesDefaultPort` | Unknown framework falls back to default port | Config: `framework: "unknown-fw"`, `orchestrator: "kubernetes"` | K8s deployment uses `DEFAULT_PORT_FALLBACK` (8080) | — | yes |
| 48 | UT-48 | `assemble_unknownFramework_usesDefaultHealthPath` | Unknown framework falls back to default health path | Config: `framework: "unknown-fw"`, `orchestrator: "kubernetes"` | K8s deployment uses `DEFAULT_HEALTH_PATH` ("/health") | — | yes |
| 49 | UT-49 | `assemble_unknownLanguage_usesDefaultDockerImage` | Unknown language falls back to default Docker image | Config: `language: "unknown"`, `container: "docker"` | Dockerfile or warning references `DEFAULT_DOCKER_IMAGE` ("alpine:latest") | — | yes |

### 2.14 Per-Stack Parametrized Tests (TPP Step 10: All Supported Stacks)

| # | ID | Test Name | Description | Input/Setup | Expected Output | Depends On | Parallel |
|---|-----|-----------|-------------|-------------|-----------------|------------|----------|
| 50 | UT-50 | `assemble_perStack_generatesCorrectDockerfile` | Parametrized: each supported language/buildTool produces a Dockerfile | `it.each` over 7 stacks: java-maven, java-gradle, kotlin-gradle, typescript-npm, python-pip, go-go, rust-cargo | Dockerfile exists, contains correct base image per `DOCKER_BASE_IMAGES` mapping, no raw placeholders | — | yes |
| 51 | UT-51 | `assemble_perStack_generatesCorrectCiWorkflow` | Parametrized: each stack produces a CI workflow with correct commands | `it.each` over 7 stacks | CI workflow file exists, contains test/build commands from `LANGUAGE_COMMANDS` | — | yes |

---

## 3. Pipeline Registration Test

### 3.1 Extend: `tests/node/assembler/pipeline.test.ts`

| # | ID | Test Name | Description | Input/Setup | Expected Output | Depends On | Parallel |
|---|-----|-----------|-------------|-------------|-----------------|------------|----------|
| 52 | UT-52 | `buildAssemblers_includesCicdAssembler` | CicdAssembler is registered in the pipeline | Call `buildAssemblers()` | Array includes an entry with `name: "CicdAssembler"` and `target: "root"` | — | yes |
| 53 | UT-53 | `buildAssemblers_cicdBeforeReadme` | CicdAssembler appears before ReadmeAssembler in pipeline order | Call `buildAssemblers()` | Index of `CicdAssembler` < index of `ReadmeAssembler` | — | yes |

---

## 4. Existing Tests — No Changes Needed (Integration)

### 4.1 Golden File Integration Tests

- **File:** `tests/node/integration/byte-for-byte.test.ts`
- **What it validates:** Pipeline output matches golden files byte-for-byte for all 8 profiles
- **How it covers this story:** After adding `CicdAssembler` and regenerating golden files, the pipeline will produce output that includes new CI/CD artifacts. The golden file tests validate the complete output including new files.
- **Expected result:** All 8 profiles pass (40 test assertions: 5 per profile)
- **Test logic unchanged:** The test infrastructure is generic and works with any content

### 4.2 Profile Coverage Matrix

The 8 golden file profiles cover these CI/CD configuration combinations:

| Profile | container | orchestrator | smokeTests | CI workflow | Dockerfile | Docker Compose | K8s | Smoke | Runbook |
|---------|-----------|-------------|------------|:-----------:|:----------:|:--------------:|:---:|:-----:|:-------:|
| go-gin | docker | kubernetes | true | yes | yes | yes | yes | yes | yes |
| java-quarkus | docker | kubernetes | true | yes | yes | yes | yes | yes | yes |
| java-spring | docker | kubernetes | true | yes | yes | yes | yes | yes | yes |
| kotlin-ktor | docker | kubernetes | true | yes | yes | yes | yes | yes | yes |
| python-click-cli | docker | **none** | true | yes | yes | yes | **no** | yes | yes |
| python-fastapi | docker | kubernetes | true | yes | yes | yes | yes | yes | yes |
| rust-axum | docker | kubernetes | true | yes | yes | yes | yes | yes | yes |
| typescript-nestjs | docker | kubernetes | true | yes | yes | yes | yes | yes | yes |

**Key coverage:**
- 7 profiles test `container: docker` + `orchestrator: kubernetes` (all artifacts)
- 1 profile (python-click-cli) tests `container: docker` + `orchestrator: none` (K8s skipped)
- No profile has `container: none` — this edge case is covered by unit tests (UT-6, UT-7, UT-18)
- No profile has `smokeTests: false` — this edge case is covered by unit test (UT-13)

---

## 5. Golden Files Requiring Update

**Total: 8 golden directories** — each gains 4-8 new files depending on configuration.

### 5.1 New Golden Files Per Profile

| Profile | New Files |
|---------|-----------|
| go-gin | `.github/workflows/ci.yml`, `Dockerfile`, `docker-compose.yml`, `k8s/deployment.yaml`, `k8s/service.yaml`, `k8s/configmap.yaml`, `tests/smoke/*`, `docs/runbook/deploy-runbook.md` |
| java-quarkus | Same as go-gin |
| java-spring | Same as go-gin |
| kotlin-ktor | Same as go-gin |
| python-click-cli | `.github/workflows/ci.yml`, `Dockerfile`, `docker-compose.yml`, `tests/smoke/*`, `docs/runbook/deploy-runbook.md` (NO k8s/) |
| python-fastapi | Same as go-gin |
| rust-axum | Same as go-gin |
| typescript-nestjs | Same as go-gin |

### 5.2 Golden File Update Strategy

```bash
# After implementing CicdAssembler and templates:
# Regenerate golden files by running the pipeline for each profile
PROFILES=(go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs)

for profile in "${PROFILES[@]}"; do
  npx tsx src/index.ts generate \
    --config "resources/config-templates/setup-config.${profile}.yaml" \
    --output "tests/golden/${profile}"
done
```

---

## 6. TDD Execution Order

Following test-first approach (Double-Loop TDD):

| Step | Action | Test State |
|------|--------|-----------|
| 1 | Write acceptance-level unit tests: UT-1 through UT-3 (always-generated artifacts) | RED |
| 2 | Create stub `CicdAssembler` class with `assemble()` returning `{ files: [], warnings: [] }` | RED (tests still fail — no files generated) |
| 3 | Create `resources/cicd-templates/ci-workflow/ci.yml.njk` and `deploy-runbook/deploy-runbook.md.njk` templates | RED |
| 4 | Implement `generateCiWorkflow()` and `generateDeployRunbook()` in CicdAssembler | GREEN for UT-1, UT-2, UT-3 |
| 5 | Write conditional Docker tests: UT-4 through UT-7 | RED |
| 6 | Create Dockerfile and Docker Compose templates; implement `generateDockerfile()`, `generateDockerCompose()` | GREEN for UT-4 through UT-7 |
| 7 | Write K8s conditional tests: UT-8 through UT-11 | RED |
| 8 | Create K8s templates; implement `generateK8sManifests()` | GREEN for UT-8 through UT-11 |
| 9 | Write smoke test conditional tests: UT-12, UT-13 | RED |
| 10 | Create smoke test template; implement `generateSmokeTestConfig()` | GREEN for UT-12, UT-13 |
| 11 | Write full/partial/minimal config tests: UT-14 through UT-19 | GREEN (compound of previous implementations) |
| 12 | Write template substitution tests: UT-20 through UT-40 | RED for content-specific assertions |
| 13 | Refine templates with correct variable references | GREEN for UT-20 through UT-40 |
| 14 | Write edge case tests: UT-45 through UT-49 | RED |
| 15 | Implement fallback handling in assembler | GREEN for UT-45 through UT-49 |
| 16 | Write parametrized per-stack tests: UT-50, UT-51 | RED for untested stacks |
| 17 | Create per-stack Dockerfile templates (7 templates) | GREEN for UT-50, UT-51 |
| 18 | Write pipeline registration tests: UT-52, UT-53 | RED |
| 19 | Register CicdAssembler in `pipeline.ts` and export in `index.ts` | GREEN for UT-52, UT-53 |
| 20 | Regenerate golden files for all 8 profiles | N/A |
| 21 | Run `byte-for-byte.test.ts` — golden file parity confirmed | GREEN |
| 22 | Run full test suite (`npx vitest run`) | GREEN |

---

## 7. Coverage Estimation

| Class/Module | Public Methods | Branches | Est. Tests | Line % | Branch % |
|-------------|---------------|----------|-----------|--------|----------|
| `CicdAssembler` | 1 (`assemble`) + 6 private `generate*` methods | 10 (container check x2, orchestrator check, smokeTests check, 6 template-exists checks) | 49 | 98% | 95% |
| `pipeline.ts` (modification) | 0 new (existing `buildAssemblers`) | 0 new | 2 | 100% | N/A |
| **Total** | **7** | **10** | **51** | **98%** | **95%** |

### 7.1 Branch Coverage Detail

| Branch | Test(s) Covering True | Test(s) Covering False |
|--------|----------------------|----------------------|
| `container === "docker"` (Dockerfile) | UT-4, UT-50 | UT-6, UT-18 |
| `container === "docker"` (Docker Compose) | UT-5 | UT-7, UT-18 |
| `orchestrator === "kubernetes"` (Deployment) | UT-8, UT-14 | UT-11, UT-16, UT-18 |
| `orchestrator === "kubernetes"` (Service) | UT-9 | UT-11, UT-18 |
| `orchestrator === "kubernetes"` (ConfigMap) | UT-10 | UT-11, UT-18 |
| `smokeTests === true` | UT-12, UT-14 | UT-13, UT-18 |
| CI template exists check | UT-1 | UT-45 |
| Dockerfile template exists check | UT-4 | UT-46 |
| Framework port lookup fallback | UT-32 (found) | UT-47 (default) |
| Docker base image lookup fallback | UT-21 (found) | UT-49 (default) |

---

## 8. Suggested Test Implementation Pattern

```typescript
import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import { CicdAssembler } from "../../../src/assembler/cicd-assembler.js";
import { TemplateEngine } from "../../../src/template-engine.js";
import {
  ProjectConfig,
  ProjectIdentity,
  ArchitectureConfig,
  InterfaceConfig,
  LanguageConfig,
  FrameworkConfig,
  InfraConfig,
  TestingConfig,
} from "../../../src/models.js";

function buildConfig(overrides: {
  language?: string;
  buildTool?: string;
  framework?: string;
  languageVersion?: string;
  container?: string;
  orchestrator?: string;
  smokeTests?: boolean;
  projectName?: string;
} = {}): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity(
      overrides.projectName ?? "my-app",
      "A test application",
    ),
    new ArchitectureConfig("microservice", false, false),
    [new InterfaceConfig("rest")],
    new LanguageConfig(
      overrides.language ?? "typescript",
      overrides.languageVersion ?? "5",
    ),
    new FrameworkConfig(
      overrides.framework ?? "nestjs",
      "3.0",
      overrides.buildTool ?? "npm",
    ),
    undefined, // DataConfig defaults
    new InfraConfig(
      overrides.container ?? "docker",
      overrides.orchestrator ?? "kubernetes",
    ),
    undefined, // SecurityConfig defaults
    new TestingConfig(
      overrides.smokeTests ?? true,
    ),
  );
}

describe("CicdAssembler", () => {
  let tmpDir: string;
  let resourcesDir: string;
  let outputDir: string;
  let assembler: CicdAssembler;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(
      path.join(tmpdir(), "cicd-asm-test-"),
    );
    resourcesDir = path.join(tmpDir, "resources");
    fs.mkdirSync(resourcesDir, { recursive: true });
    outputDir = path.join(tmpDir, "output");
    fs.mkdirSync(outputDir, { recursive: true });
    assembler = new CicdAssembler();
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  describe("always-generated artifacts", () => {
    // UT-1, UT-2, UT-3
  });

  describe("conditional — Docker artifacts", () => {
    // UT-4, UT-5, UT-6, UT-7
  });

  describe("conditional — Kubernetes manifests", () => {
    // UT-8, UT-9, UT-10, UT-11
  });

  describe("conditional — smoke tests", () => {
    // UT-12, UT-13
  });

  describe("full config — all artifacts", () => {
    // UT-14, UT-15
  });

  describe("partial config — Docker without K8s", () => {
    // UT-16, UT-17
  });

  describe("minimal config — all disabled", () => {
    // UT-18, UT-19
  });

  describe("template variable substitution", () => {
    // UT-20 through UT-34
  });

  describe("Dockerfile content", () => {
    // UT-35, UT-36
  });

  describe("deploy runbook content", () => {
    // UT-37 through UT-40
  });

  describe("directory creation", () => {
    // UT-41, UT-42, UT-43, UT-44
  });

  describe("edge cases — missing templates and fallbacks", () => {
    // UT-45 through UT-49
  });

  describe("per-stack parametrized", () => {
    it.each([
      ["java", "maven", "java-maven"],
      ["java", "gradle", "java-gradle"],
      ["kotlin", "gradle", "kotlin-gradle"],
      ["typescript", "npm", "typescript-npm"],
      ["python", "pip", "python-pip"],
      ["go", "go", "go-go"],
      ["rust", "cargo", "rust-cargo"],
    ])(
      "assemble_%s_%s_generatesCorrectDockerfile",
      (language, buildTool, _stackKey) => {
        // UT-50
      },
    );

    it.each([/* same stacks */])(
      "assemble_%s_%s_generatesCorrectCiWorkflow",
      (language, buildTool, _stackKey) => {
        // UT-51
      },
    );
  });
});
```

---

## 9. Verification Checklist

- [ ] `npx vitest run tests/node/assembler/cicd-assembler.test.ts` — all ~51 unit tests pass
- [ ] `npx vitest run tests/node/assembler/pipeline.test.ts` — pipeline registration tests pass (including new UT-52, UT-53)
- [ ] `npx vitest run tests/node/integration/byte-for-byte.test.ts` — all 8 profiles pass (40 assertions)
- [ ] `npx vitest run` — full suite passes
- [ ] Coverage >= 95% line, >= 90% branch
- [ ] Zero compiler/linter warnings introduced
- [ ] No `{{` or `}}` raw placeholders in any generated artifact
- [ ] `CicdAssembler` class is <= 250 lines
- [ ] Each private method is <= 25 lines
- [ ] `src/assembler/index.ts` exports `cicd-assembler` module

---

## 10. Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| New assembler breaks pipeline for existing profiles | All 8 golden file tests (`byte-for-byte.test.ts`) serve as regression safety net |
| Template rendering errors (undefined variables) | `TemplateEngine` is configured with `throwOnUndefined: true`; UT-28, UT-29, UT-30 verify no raw placeholders remain |
| Conditional logic gap (untested config combo) | Unit tests explicitly cover all 4 conditions (container, orchestrator, smokeTests) in both true and false branches (Section 7.1) |
| Golden file regeneration scope | 8 directories updated; byte-for-byte tests catch any mismatch immediately |
| Stack key not found in mapping | UT-47, UT-48, UT-49 verify fallback to default values |
| Missing template file at runtime | UT-45, UT-46 verify graceful handling with warnings (not errors) |
| Dependency on story-0004-0003 deploy runbook template | Self-contained template created in `resources/cicd-templates/deploy-runbook/`; can be refactored later to share source |
| ReadmeAssembler counts affected by CI/CD files under `.github/` | UT-52, UT-53 verify CicdAssembler runs before ReadmeAssembler |

---

## 11. Files Summary

### 11.1 New Files

| # | File | Description |
|---|------|-------------|
| 1 | `src/assembler/cicd-assembler.ts` | New assembler class (~120-150 lines) |
| 2 | `resources/cicd-templates/ci-workflow/ci.yml.njk` | GitHub Actions CI workflow template |
| 3 | `resources/cicd-templates/dockerfile/Dockerfile.java-maven.njk` | Dockerfile for Java + Maven |
| 4 | `resources/cicd-templates/dockerfile/Dockerfile.java-gradle.njk` | Dockerfile for Java + Gradle |
| 5 | `resources/cicd-templates/dockerfile/Dockerfile.kotlin-gradle.njk` | Dockerfile for Kotlin + Gradle |
| 6 | `resources/cicd-templates/dockerfile/Dockerfile.typescript-npm.njk` | Dockerfile for TypeScript + npm |
| 7 | `resources/cicd-templates/dockerfile/Dockerfile.python-pip.njk` | Dockerfile for Python + pip |
| 8 | `resources/cicd-templates/dockerfile/Dockerfile.go-go.njk` | Dockerfile for Go |
| 9 | `resources/cicd-templates/dockerfile/Dockerfile.rust-cargo.njk` | Dockerfile for Rust + Cargo |
| 10 | `resources/cicd-templates/docker-compose/docker-compose.yml.njk` | Docker Compose dev environment |
| 11 | `resources/cicd-templates/k8s/deployment.yaml.njk` | K8s Deployment template |
| 12 | `resources/cicd-templates/k8s/service.yaml.njk` | K8s Service template |
| 13 | `resources/cicd-templates/k8s/configmap.yaml.njk` | K8s ConfigMap template |
| 14 | `resources/cicd-templates/smoke-tests/smoke-config.md` | Smoke test config scaffold |
| 15 | `resources/cicd-templates/deploy-runbook/deploy-runbook.md.njk` | Deploy runbook template |
| 16 | `tests/node/assembler/cicd-assembler.test.ts` | Unit tests (~51 cases) |

### 11.2 Modified Files

| # | File | Description |
|---|------|-------------|
| 1 | `src/assembler/pipeline.ts` | Register CicdAssembler in `buildAssemblers()` |
| 2 | `src/assembler/index.ts` | Add export for `cicd-assembler` module |
| 3 | `tests/golden/*` (8 directories) | Regenerate golden files with new CI/CD outputs |

### 11.3 Existing Test Files (unchanged, covering this story)

| File | Test Count | Coverage |
|------|-----------|----------|
| `tests/node/integration/byte-for-byte.test.ts` | 40 (8 profiles x 5 assertions) | Golden file parity for all CI/CD artifacts |
| `tests/node/assembler/pipeline.test.ts` | existing + 2 new | Pipeline structure validation |

---

## 12. Test Count Summary

| Category | New Tests | Existing Tests |
|----------|-----------|----------------|
| Unit — always-generated artifacts | 3 | 0 |
| Unit — conditional Docker | 4 | 0 |
| Unit — conditional K8s | 4 | 0 |
| Unit — conditional smoke | 2 | 0 |
| Unit — full/partial/minimal config | 6 | 0 |
| Unit — template substitution | 15 | 0 |
| Unit — Dockerfile content | 2 | 0 |
| Unit — deploy runbook content | 4 | 0 |
| Unit — directory creation | 4 | 0 |
| Unit — edge cases/fallbacks | 5 | 0 |
| Unit — per-stack parametrized | 2 (x7 stacks = 14 assertions) | 0 |
| Unit — pipeline registration | 2 | existing |
| Integration — golden file parity | 0 | 40 |
| **Total** | **53** | **40+** |
