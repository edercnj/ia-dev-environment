# Implementation Plan — story-0004-0011: CI/CD Artifact Generation

## Overview

This story adds a new `CicdAssembler` to the generation pipeline that produces CI/CD artifacts
(GitHub Actions workflow, Dockerfile, Docker Compose, Kubernetes manifests, smoke test config,
and deploy runbook) conditionally based on the project identity fields `container`, `orchestrator`,
and `smoke_tests`.

The tool (`ia-dev-env`) follows a **library architecture** (not hexagonal). The codebase is
organized as: `src/domain/` (mappings, validation, resolution), `src/assembler/` (file generation
pipeline), `src/` root (CLI, config, models, template engine). Each assembler is a class with an
`assemble()` method that receives `(config, outputDir, resourcesDir, engine)` and returns
`string[] | AssembleResult`.

---

## 1. Affected Layers and Components

| Layer | Component | Impact |
|-------|-----------|--------|
| `src/assembler/` | New `CicdAssembler` | **New file** — core of this story |
| `src/assembler/pipeline.ts` | `buildAssemblers()` | **Modify** — register `CicdAssembler` in pipeline |
| `src/assembler/index.ts` | Barrel exports | **Modify** — export new assembler |
| `src/domain/stack-mapping.ts` | `LANGUAGE_COMMANDS`, `FRAMEWORK_PORTS`, `DOCKER_BASE_IMAGES` | **Read-only** — consumed by templates; no changes needed |
| `resources/` | New `cicd-templates/` directory | **New directory** — CI/CD template files |
| `tests/node/assembler/` | New `cicd-assembler.test.ts` | **New file** — unit tests |
| `tests/golden/` | All 8 profile directories | **Modify** — regenerate golden files to include new CI/CD outputs |

---

## 2. New Classes/Interfaces to Create

### 2.1 `src/assembler/cicd-assembler.ts` (New File)

```
CicdAssembler {
  assemble(config: ProjectConfig, outputDir: string, resourcesDir: string, engine: TemplateEngine): AssembleResult
  - generateCiWorkflow(config, outputDir, resourcesDir, engine): string[]
  - generateDockerfile(config, outputDir, resourcesDir, engine): string[]
  - generateDockerCompose(config, outputDir, resourcesDir, engine): string[]
  - generateK8sManifests(config, outputDir, resourcesDir, engine): string[]
  - generateSmokeTestConfig(config, outputDir, resourcesDir, engine): string[]
  - generateDeployRunbook(config, outputDir, resourcesDir, engine): string[]
}
```

**Design decisions:**
- Single assembler class with private methods per artifact type (follows SRP at method level).
- Returns `AssembleResult` (not bare `string[]`) to include informational warnings when artifacts are skipped.
- Each `generate*` method checks the relevant config condition and returns `[]` if the condition is not met, plus adds a warning string.
- Uses `TemplateEngine.renderTemplate()` for Nunjucks templates (`.njk` files), same pattern as `GithubPromptsAssembler`.
- Uses `copyTemplateFile()` for simple file copies with placeholder replacement, same pattern as `copy-helpers.ts`.

**Estimated size:** ~120-150 lines (well within the 250-line class limit).

### 2.2 `resources/cicd-templates/` (New Template Directory)

```
resources/cicd-templates/
├── ci-workflow/
│   ├── ci.yml.njk                     # GitHub Actions CI workflow (Nunjucks)
├── dockerfile/
│   ├── Dockerfile.java-maven.njk
│   ├── Dockerfile.java-gradle.njk
│   ├── Dockerfile.kotlin-gradle.njk
│   ├── Dockerfile.typescript-npm.njk
│   ├── Dockerfile.python-pip.njk
│   ├── Dockerfile.go-go.njk
│   ├── Dockerfile.rust-cargo.njk
│   └── Dockerfile.csharp-dotnet.njk
├── docker-compose/
│   └── docker-compose.yml.njk         # Dev environment compose file
├── k8s/
│   ├── deployment.yaml.njk
│   ├── service.yaml.njk
│   └── configmap.yaml.njk
├── smoke-tests/
│   └── smoke-config.md                # Smoke test configuration scaffold
└── deploy-runbook/
    └── deploy-runbook.md.njk          # Deploy runbook (consumes story-0004-0003 structure)
```

**Template context variables** (all available from `buildDefaultContext()` in `template-engine.ts`):
- `project_name`, `language_name`, `language_version`
- `framework_name`, `framework_version`, `build_tool`
- `container`, `orchestrator`, `database_name`, `cache_name`
- `smoke_tests`, `coverage_line`, `coverage_branch`

**Additional context** needed from `stack-mapping.ts` (passed as overrides):
- `compile_cmd`, `build_cmd`, `test_cmd`, `coverage_cmd`
- `file_extension`, `build_file`, `package_manager`
- `framework_port`, `health_path`
- `docker_base_image`

### 2.3 `tests/node/assembler/cicd-assembler.test.ts` (New File)

Unit tests covering:
- CI workflow always generated for any config
- Dockerfile generated only when `container === "docker"`
- Docker Compose generated only when `container === "docker"`
- K8s manifests generated only when `orchestrator === "kubernetes"`
- Smoke test config generated only when `smokeTests === true`
- Deploy runbook always generated
- Skipped artifacts produce informational warnings (not errors)
- Template rendering uses correct context variables per stack

---

## 3. Existing Classes to Modify

### 3.1 `src/assembler/pipeline.ts`

**Change:** Add `CicdAssembler` to the `buildAssemblers()` array.

```typescript
// Insert BEFORE ReadmeAssembler (so README can count CI/CD files)
{ name: "CicdAssembler", target: "root", assembler: new CicdAssembler() },
```

**Target: `"root"`** because CI/CD artifacts go to the project root (not inside `.claude/` or `.github/`):
- `.github/workflows/ci.yml` goes under outputDir (root-relative `.github/`)
- `Dockerfile` goes at outputDir root
- `docker-compose.yml` goes at outputDir root
- `k8s/` goes under outputDir root
- `tests/smoke/` goes under outputDir root
- `docs/runbook/` goes under outputDir root

**Wait** -- re-examining the pipeline target resolution:
- `"root"` maps to `outputDir` directly
- `.github/workflows/` is already the output structure for GithubHooksAssembler using `"github"` target
- The CI workflow goes under `.github/workflows/` which is `githubDir + "/workflows/"`

**Revised approach:** The `CicdAssembler` should use target `"root"` and construct paths manually relative to `outputDir`, since artifacts span multiple top-level directories (`.github/workflows/`, root-level `Dockerfile`, `k8s/`, `docs/runbook/`, `tests/smoke/`). This matches how `CodexAgentsMdAssembler` uses `"root"` and builds its own subdirectory structure.

### 3.2 `src/assembler/index.ts`

**Change:** Add export for the new assembler module.

```typescript
// --- STORY-0004-0011: CicdAssembler ---
export * from "./cicd-assembler.js";
```

### 3.3 Golden Files (All 8 Profiles)

All golden directories must be regenerated to include the new CI/CD outputs. The regeneration
is done by running the pipeline against each profile config and updating the golden snapshots.

**Impact per profile:**

| Profile | CI workflow | Dockerfile | Docker Compose | K8s manifests | Smoke config | Deploy runbook |
|---------|:-----------:|:----------:|:--------------:|:-------------:|:------------:|:--------------:|
| go-gin | yes | yes | yes | yes | yes | yes |
| java-quarkus | yes | yes | yes | yes | yes | yes |
| java-spring | yes | yes | yes | yes | yes | yes |
| kotlin-ktor | yes | yes | yes | yes | yes | yes |
| python-click-cli | yes | yes | yes | **no** (orchestrator=none) | yes | yes |
| python-fastapi | yes | yes | yes | yes | yes | yes |
| rust-axum | yes | yes | yes | yes | yes | yes |
| typescript-nestjs | yes | yes | yes | yes | yes | yes |

The `typescript-commander-cli` profile is NOT in the golden test set (no golden directory exists).

---

## 4. Dependency Direction Validation

```
CicdAssembler (src/assembler/)
  ├── imports ProjectConfig from src/models.ts           ✓ assembler → models
  ├── imports TemplateEngine from src/template-engine.ts  ✓ assembler → template-engine
  ├── imports LANGUAGE_COMMANDS, FRAMEWORK_PORTS,
  │          DOCKER_BASE_IMAGES from src/domain/stack-mapping.ts  ✓ assembler → domain
  └── imports AssembleResult from src/assembler/rules-assembler.ts  ✓ within assembler layer
```

No circular dependencies. The assembler layer depends on domain and models, which is the
established pattern used by all other assemblers (e.g., `HooksAssembler` imports
`getHookTemplateKey` from `domain/stack-mapping.ts`).

---

## 5. Integration Points

### 5.1 Pipeline Registration

The `CicdAssembler` is registered in `buildAssemblers()` at position 17 (after
`CodexSkillsAssembler`, before `ReadmeAssembler`). This ensures:
- All `.claude/` and `.github/` assemblers run first (their outputs are needed for README counts).
- CI/CD artifacts are written before `ReadmeAssembler` scans the output directory.
- The `ReadmeAssembler` generation summary will count CI/CD files if they are under `.claude/` or `.github/`.

### 5.2 Template Engine Integration

Templates use Nunjucks syntax (`.njk` extension). The `TemplateEngine` constructor receives
`resourcesDir` as the file system loader root. Templates are referenced as relative paths:
```typescript
engine.renderTemplate("cicd-templates/ci-workflow/ci.yml.njk", extraContext);
```

### 5.3 Stack Mapping Integration

The assembler builds extra template context from `stack-mapping.ts` constants:
```typescript
const langKey = `${config.language.name}-${config.framework.buildTool}`;
const commands = LANGUAGE_COMMANDS[langKey];
const port = FRAMEWORK_PORTS[config.framework.name] ?? DEFAULT_PORT_FALLBACK;
const healthPath = FRAMEWORK_HEALTH_PATHS[config.framework.name] ?? DEFAULT_HEALTH_PATH;
const dockerImage = DOCKER_BASE_IMAGES[config.language.name] ?? DEFAULT_DOCKER_IMAGE;
```

### 5.4 Deploy Runbook Integration (story-0004-0003 dependency)

The deploy runbook template uses the same structure defined in story-0004-0003:
- 7 mandatory sections (Service Info, Pre-conditions, Deploy Procedure, Post-Deploy Verification, Rollback Procedure, Troubleshooting, Contacts)
- Conditional sections based on `container`, `orchestrator`, `database_name`
- The template is a Nunjucks file with `{% if %}` blocks for conditional sections

If the deploy runbook template from story-0004-0003 already exists in `resources/templates/`,
the `CicdAssembler` should reference it. If not (because that story may not be implemented yet),
we create the template as part of this story in `resources/cicd-templates/deploy-runbook/`.

### 5.5 Documentation Phase Integration (story-0004-0005 dependency)

Story-0004-0005 defines the documentation dispatch phase in `x-dev-lifecycle`. The CI/CD
generation is a **pipeline assembler** (runs during `ia-dev-env generate`), NOT a lifecycle
phase. The story text says "A geracao de artefatos CI/CD acontece na fase de documentacao
(Phase 3) **Ou pode ser invocada standalone como parte do `ia-dev-env generate`**."

For this implementation, we focus on the standalone `ia-dev-env generate` path (pipeline
assembler). The lifecycle phase integration would be handled by the documentation phase
dispatcher from story-0004-0005 calling the same assembler.

---

## 6. Database Changes

None. This is a CLI tool with no database.

---

## 7. API Changes

None. This is a CLI tool. The only interface change is that `ia-dev-env generate` now produces
additional output files (CI/CD artifacts).

---

## 8. Event Changes

None. The project is not event-driven.

---

## 9. Configuration Changes

### 9.1 No New Config Fields Required

All required configuration fields already exist in the `ProjectConfig` model:
- `config.infrastructure.container` (string: "docker" | "none")
- `config.infrastructure.orchestrator` (string: "kubernetes" | "none")
- `config.testing.smokeTests` (boolean)
- `config.language.name`, `config.language.version`
- `config.framework.name`, `config.framework.buildTool`

### 9.2 Template Files (New Resources)

New template files are added under `resources/cicd-templates/`. These are the source of truth
per RULE-002.

### 9.3 ReadmeAssembler Impact

The `ReadmeAssembler`'s `buildGenerationSummary()` counts files under `.claude/`, `.github/`,
and `.codex/`. CI/CD artifacts that land under `.github/workflows/` will be counted automatically
by the existing `countGithubFiles()` / `countGithubComponent()` functions in `readme-utils.ts`.

Artifacts placed at the project root (`Dockerfile`, `docker-compose.yml`, `k8s/`, `docs/runbook/`,
`tests/smoke/`) are **not** counted in the README generation summary. This is acceptable since
the README documents `.claude/` and `.github/` configuration, not project-level files.

---

## 10. Risk Assessment

### 10.1 Low Risk

| Risk | Mitigation |
|------|-----------|
| New assembler breaks pipeline for existing profiles | All 8 golden file tests serve as regression safety net. Run `byte-for-byte.test.ts` after changes. |
| Template rendering errors (undefined variables) | TemplateEngine is configured with `throwOnUndefined: true`. Integration tests catch rendering failures. |
| Conditional logic gaps (missing combo) | All 9 config profiles cover the key combinations: 7 with docker+k8s, 2 with docker+no-k8s. Unit tests cover the `container=none` edge case explicitly. |

### 10.2 Medium Risk

| Risk | Mitigation |
|------|-----------|
| Golden file regeneration scope | 8 golden directories need updates. Each gains 4-6 new files. Regeneration script must be run and diffs reviewed. |
| Dependency on story-0004-0003 (deploy runbook template) | If not yet implemented, create a self-contained runbook template within `cicd-templates/`. The template structure follows the 7-section spec from story-0004-0003. Can be refactored later to share a single template source. |
| File path conflicts with future assemblers | CI/CD artifacts use well-known paths (`.github/workflows/ci.yml`, `Dockerfile`, `k8s/`) that are unlikely to conflict. No other assembler writes to these paths. |

### 10.3 Mitigated by Design

| Risk | Design Choice |
|------|--------------|
| Assembler order matters for README counts | `CicdAssembler` is placed before `ReadmeAssembler` in the pipeline. |
| Nunjucks template not found | Each `generate*` method checks `fs.existsSync()` before rendering, returns `[]` and adds a warning if template is missing. |
| Stack key not found in mapping | Falls back to sensible defaults (`DEFAULT_PORT_FALLBACK`, `DEFAULT_HEALTH_PATH`, `DEFAULT_DOCKER_IMAGE`). |

---

## Implementation Order

Following the project convention of inner layers first:

1. **Templates first** — Create all Nunjucks templates in `resources/cicd-templates/`
2. **Assembler** — Implement `CicdAssembler` class in `src/assembler/cicd-assembler.ts`
3. **Pipeline registration** — Add to `buildAssemblers()` in `pipeline.ts` and export in `index.ts`
4. **Unit tests** — `tests/node/assembler/cicd-assembler.test.ts` covering all conditional branches
5. **Golden file regeneration** — Update all 8 golden directories
6. **Integration tests** — Verify `byte-for-byte.test.ts` passes for all profiles

---

## Test Strategy

### Unit Tests (`cicd-assembler.test.ts`)

| Test Name | Scenario |
|-----------|----------|
| `assemble_withDockerAndK8s_generatesAllArtifacts` | Full config (docker + kubernetes + smoke_tests) |
| `assemble_withDockerNoK8s_skipsK8sManifests` | container=docker, orchestrator=none |
| `assemble_withNoContainer_skipsDockerAndCompose` | container=none |
| `assemble_withSmokeTestsDisabled_skipsSmoke` | smokeTests=false |
| `assemble_always_generatesCiWorkflow` | CI workflow generated regardless of config |
| `assemble_always_generatesDeployRunbook` | Deploy runbook generated regardless of config |
| `assemble_skippedArtifacts_produceWarnings` | Verify warnings array contains skip reasons |
| `assemble_perStack_rendersCorrectBuildCommands` | Test each lang-buildtool combo renders correct commands |
| `assemble_missingTemplate_returnsEmptyWithWarning` | Template file missing from resources |

### Integration Tests

The existing `byte-for-byte.test.ts` covers all 8 profiles. After golden file regeneration,
it will validate that CI/CD artifacts are produced correctly for each profile.

### Coverage Estimate

- Unit tests: ~25-30 test cases covering all conditional branches
- Integration tests: 8 profiles x 5 assertions = 40 assertions
- Expected coverage: >95% line, >90% branch (meets quality gates)

---

## File Summary

| Action | File | Description |
|--------|------|-------------|
| **CREATE** | `src/assembler/cicd-assembler.ts` | New assembler class (~120-150 lines) |
| **CREATE** | `resources/cicd-templates/ci-workflow/ci.yml.njk` | GitHub Actions CI workflow template |
| **CREATE** | `resources/cicd-templates/dockerfile/Dockerfile.{stack}.njk` | 8 Dockerfile templates (one per stack) |
| **CREATE** | `resources/cicd-templates/docker-compose/docker-compose.yml.njk` | Docker Compose dev environment template |
| **CREATE** | `resources/cicd-templates/k8s/deployment.yaml.njk` | K8s Deployment + Service template |
| **CREATE** | `resources/cicd-templates/k8s/service.yaml.njk` | K8s Service template |
| **CREATE** | `resources/cicd-templates/k8s/configmap.yaml.njk` | K8s ConfigMap template |
| **CREATE** | `resources/cicd-templates/smoke-tests/smoke-config.md` | Smoke test config scaffold |
| **CREATE** | `resources/cicd-templates/deploy-runbook/deploy-runbook.md.njk` | Deploy runbook template |
| **CREATE** | `tests/node/assembler/cicd-assembler.test.ts` | Unit tests (~25-30 cases) |
| **MODIFY** | `src/assembler/pipeline.ts` | Register CicdAssembler in buildAssemblers() |
| **MODIFY** | `src/assembler/index.ts` | Add export for cicd-assembler |
| **MODIFY** | `tests/golden/*` (8 directories) | Regenerate golden files with new CI/CD outputs |
