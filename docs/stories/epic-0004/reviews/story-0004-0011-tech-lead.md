============================================================
 TECH LEAD REVIEW -- story-0004-0011
============================================================
 Decision:  GO
 Score:     39/40
 Critical:  0 issues
 Medium:    1 issue
 Low:       0 issues
------------------------------------------------------------

### A. Code Hygiene (8/8)

[A1] No unused imports or variables (1/1)
All imports in `cicd-assembler.ts` are used: `fs`, `path`, `ProjectConfig`, `TemplateEngine`,
`AssembleResult`, and all six stack-mapping constants. No dead imports in modified test files.

[A2] No dead code or unreachable branches (1/1)
Every branch in the conditional generation methods is reachable and tested:
container === "docker", orchestrator === "kubernetes", smokeTests === true/false.
The `catch` block in `renderAndWrite` handles template-not-found scenarios.

[A3] Zero compiler/linter warnings (1/1)
`npx --no-install tsc --noEmit` produces zero errors. All 1797 tests pass.

[A4] Method signatures consistent with existing codebase patterns (1/1)
The `assemble()` public method follows the same `(config, outputDir, resourcesDir, engine)`
signature as all other assemblers (`CodexConfigAssembler`, `RulesAssembler`, etc.).
Returns `AssembleResult` matching the `normalizeResult` contract in pipeline.ts.

[A5] No magic numbers or strings (1/1)
All string literals are extracted to named constants: `CICD_TEMPLATES`, `CI_TEMPLATE`,
`COMPOSE_TEMPLATE`, `RUNBOOK_TEMPLATE`, `SMOKE_SOURCE`, `DOCKER_CONDITION`, `K8S_CONDITION`,
`K8S_MANIFESTS`, `LINT_COMMANDS`, `DEFAULT_LINT_CMD`. Template variable keys
(`compile_cmd`, `build_cmd`, etc.) are contextual map keys, not magic strings.

[A6] No wildcard imports (1/1)
`import * as fs` and `import * as path` are Node.js namespace imports — the standard
pattern across the entire codebase (used in 15+ assembler files). No `import *` from
project modules.

[A7] Consistent formatting (1/1)
Indentation (2-space), spacing, and line breaks are consistent with all other assembler files.
Multi-line parameter lists follow the same trailing-comma style seen in `pipeline.ts`.

[A8] No TODO/FIXME without tracking issue (1/1)
No TODO, FIXME, HACK, or XXX comments in any modified production code.

### B. Naming (4/4)

[B1] Intention-revealing names (1/1)
`buildStackContext` — builds template context from stack mapping.
`renderAndWrite` — renders a template and writes to disk.
`generateCiWorkflow`, `generateDockerfile`, etc. — clearly describe what each method generates.
`GenerationOutput` — describes the accumulated files and warnings.

[B2] No disinformation (1/1)
All names match their behavior. `renderAndWrite` returns boolean success, which accurately
describes its behavior. `buildStackContext` returns the context object. `CicdAssembler` assembles
CI/CD artifacts.

[B3] Meaningful distinctions (1/1)
No noise words. `ctx` is consistently used for template context (matching `TemplateEngine`
conventions). `out` for the mutable output accumulator. `tpl` for template relative path.
`dest` for destination path.

[B4] Consistent naming across files (1/1)
File name `cicd-assembler.ts` follows kebab-case convention.
Class name `CicdAssembler` follows PascalCase convention.
Constants use UPPER_SNAKE_CASE (`CICD_TEMPLATES`, `K8S_MANIFESTS`, `LINT_COMMANDS`).
All consistent with existing assembler files.

### C. Functions (4/5)

[C1] Each function has single responsibility (1/1)
`buildStackContext` — only builds context. `renderAndWrite` — only renders and writes.
Each `generate*` method handles one artifact type. `assemble` orchestrates all generators.

[C2] All functions <= 25 lines (1/1)
Measured function bodies (content between braces):
- `buildStackContext`: 25 lines (at limit, passes)
- `renderAndWrite`: 10 lines
- `assemble`: 10 lines
- `generateCiWorkflow`: 9 lines
- `generateDockerfile`: 17 lines
- `generateDockerCompose`: 12 lines
- `generateK8sManifests`: 12 lines
- `generateSmokeTestConfig`: 15 lines
- `generateDeployRunbook`: 10 lines

[C3] Max 4 parameters per function (0/1)
Three private methods exceed the 4-parameter limit:
- `generateDockerfile`: 6 parameters (config, outputDir, resourcesDir, engine, ctx, out)
- `generateDockerCompose`: 5 parameters (config, outputDir, engine, ctx, out)
- `generateK8sManifests`: 5 parameters (config, outputDir, engine, ctx, out)

The public `assemble` method correctly has 4 parameters. The private decomposition
methods forward these plus the shared `ctx` and `out` objects. A parameter object
(e.g., `GenerationContext { config, outputDir, resourcesDir, engine, ctx }`) would
reduce all private methods to 2 parameters: `(genCtx, out)`. **[MEDIUM]**

[C4] No boolean flag parameters (1/1)
No boolean parameters in any function signature. `renderAndWrite` returns a boolean
(return value, not parameter flag).

[C5] Command-Query Separation where applicable (1/1)
`buildStackContext` is a pure query (no side effects).
`renderAndWrite` is a command that returns success status — acceptable CQS exception
for file I/O operations (same pattern as `fs.existsSync` usage elsewhere).
`assemble` returns a result object while performing side effects — consistent with
all other assemblers in the pipeline.

### D. Vertical Formatting (4/4)

[D1] Blank lines between concepts (1/1)
Constants grouped at top (lines 17-41), followed by `buildStackContext`, then
`renderAndWrite`, then `GenerationOutput` interface, then the class. Blank lines
separate each method within the class.

[D2] Newspaper Rule (1/1)
Public `assemble` method appears first in the class (line 103).
Private helper methods follow in call order: `generateCiWorkflow`, `generateDockerfile`,
`generateDockerCompose`, `generateK8sManifests`, `generateSmokeTestConfig`, `generateDeployRunbook`.

[D3] Class/module size <= 250 lines (1/1)
File is 249 lines total. Class body (`CicdAssembler`) spans lines 101-249 = 149 lines.

[D4] Related code grouped together (1/1)
Constants at top, utility functions (`buildStackContext`, `renderAndWrite`) before the class,
interface definition (`GenerationOutput`) immediately before the class that uses it.

### E. Design (3/3)

[E1] Law of Demeter — no train wrecks (1/1)
Deepest property access is `config.infrastructure.container` and
`config.language.name` — single-level navigation through typed model classes.
No chained method calls across objects.

[E2] DRY — no repeated logic (1/1)
The `renderAndWrite` helper eliminates duplication across all template-rendering methods.
Conditional guard checks are structurally similar but each handles distinct conditions
with different warning messages — not duplication.

[E3] CQS — commands don't return, queries don't mutate (1/1)
The `out` parameter is explicitly a mutable accumulator passed by reference — an accepted
pattern for collecting results in sequential processing. No hidden mutation.

### F. Error Handling (3/3)

[F1] Rich exceptions with context (1/1)
Warning messages include context: `"Dockerfile template not found for stack: ${stackKey}"`,
`"Dockerfile skipped: container is not docker"`, `"CI workflow template not found"`.
The `renderAndWrite` catch block swallows rendering errors intentionally — the caller
checks the boolean return and adds specific warning messages.

[F2] No null returns (1/1)
`assemble` returns `{ files: [], warnings: [] }` at minimum. `buildStackContext` returns
a complete object with `??` fallbacks for every field. No null or undefined returns.

[F3] Errors caught at appropriate level (1/1)
`renderAndWrite` catches at the template-rendering level (appropriate for "template not found").
The pipeline-level `executeAssemblers` in `pipeline.ts` catches assembler-level errors and
wraps them in `PipelineError` — proper error escalation.

### G. Architecture (5/5)

[G1] SRP at class level — one reason to change (1/1)
`CicdAssembler` has one responsibility: generating CI/CD artifacts from configuration.
Changes to CI/CD artifact generation are the only reason to modify this class.

[G2] DIP — depends on abstractions not concretions (1/1)
Depends on `ProjectConfig` (model), `TemplateEngine` (abstraction), and `AssembleResult`
(interface). Uses stack-mapping constants from the domain layer. No concrete infrastructure
dependencies.

[G3] Layer boundaries respected (1/1)
`cicd-assembler.ts` is in the `assembler` layer. It imports from:
- `../models.js` (domain models) — allowed
- `../template-engine.js` (shared infrastructure) — allowed
- `./rules-assembler.js` (same layer, for `AssembleResult` type) — allowed
- `../domain/stack-mapping.js` (domain constants) — allowed
No reverse dependencies from domain to assembler.

[G4] Follows implementation plan structure (1/1)
Registered in `pipeline.ts` at position 17 (before `ReadmeAssembler`, after `CodexSkillsAssembler`).
Exported from `index.ts` with story reference comment. Target is `"root"` (outputs to project root).

[G5] No circular dependencies introduced (1/1)
`cicd-assembler.ts` is a leaf module — it imports shared types but nothing imports from it
except `pipeline.ts` and `index.ts`. No circular dependency chain.

### H. Framework & Infra (4/4)

[H1] Dependency injection pattern followed (1/1)
`TemplateEngine` is injected via parameter (not constructed internally). The assembler
is instantiated in `buildAssemblers()` and receives dependencies at call time — consistent
with all other assemblers.

[H2] Configuration externalized (1/1)
All configurable values come from `ProjectConfig`. Stack mappings come from
`domain/stack-mapping.ts` constants. Template paths are relative to `resourcesDir`.
No hardcoded absolute paths.

[H3] Native-build compatible (1/1)
No reflection, no `eval`, no dynamic imports. All template resolution uses string
concatenation and `fs.existsSync` — fully compatible with native builds.

[H4] Generated templates follow best practices (1/1)
- **Dockerfiles**: Multi-stage builds, non-root user (`appuser`), HEALTHCHECK instruction,
  layer caching (dependency install before source copy). All 8 templates verified.
- **K8s**: SecurityContext with `runAsNonRoot`, `allowPrivilegeEscalation: false`,
  `readOnlyRootFilesystem: true`, `capabilities.drop: ["ALL"]`, seccompProfile,
  resource requests/limits, liveness/readiness probes, ConfigMap for environment config,
  `/tmp` emptyDir volume mount.
- **CI**: Build, test, coverage, lint steps. Conditional image scanning with Trivy when
  container is Docker. Per-language lint commands (eslint, spotless, ktlint, ruff,
  golangci-lint, clippy).
- **Docker Compose**: Port mapping, environment variables, conditional database/cache services.

### I. Tests (3/3)

[I1] Coverage >= 95% line, >= 90% branch for new code (1/1)
`cicd-assembler.ts`: 100% statements, 100% branch, 100% functions, 100% lines.
Overall project: 99.5% lines, 97.6% branch. Both exceed thresholds.

[I2] All acceptance criteria have corresponding tests (1/1)
67 tests in `cicd-assembler.test.ts` covering:
- CI workflow always generated (scenarios 1)
- Dockerfile conditional on container=docker (scenario 2)
- K8s conditional on orchestrator=kubernetes (scenario 3)
- Docker skipped when container=none (scenario 4)
- Deploy runbook with procedure/verification/rollback (scenario 5)
- Smoke tests conditional on smokeTests flag (scenario 6)
- Template variable substitution for all 7 stacks
- Edge cases (missing templates, unknown framework fallbacks)
- Review fixes (lint step, security context, image scan)

[I3] Test quality — independent, deterministic, no shared mutable state (1/1)
Each test creates its own `tmpDir`, `outputDir`, config, and engine in `beforeEach`.
`afterEach` cleans up with `fs.rmSync`. No shared mutable state. Tests use
`REAL_RESOURCES_DIR` (read-only) for template resolution. All tests are deterministic.

### J. Security & Production (1/1)

[J1] No sensitive data in code/templates, thread-safe patterns (1/1)
No secrets, tokens, or credentials in production code. Docker Compose placeholder
credentials (`dbuser`/`dbpass`) are for local-dev scaffolding only — acceptable per
security principles exception clause. K8s ConfigMap contains only `APP_ENV` and `LOG_LEVEL`.
All template context values are derived from configuration, not hardcoded secrets.
Single-threaded CLI with sequential pipeline execution — no thread safety concerns.

------------------------------------------------------------

### Issues Found

#### MEDIUM: Private methods exceed 4-parameter limit (C3)

**Files affected:**
- `src/assembler/cicd-assembler.ts:138` — `generateDockerfile` (6 params)
- `src/assembler/cicd-assembler.ts:166` — `generateDockerCompose` (5 params)
- `src/assembler/cicd-assembler.ts:186` — `generateK8sManifests` (5 params)
- `src/assembler/cicd-assembler.ts:121` — `generateCiWorkflow` (4 params - OK)
- `src/assembler/cicd-assembler.ts:209` — `generateSmokeTestConfig` (4 params - OK)

**Suggested fix:** Extract a parameter object to bundle the shared context:

```typescript
interface GenerationContext {
  readonly config: ProjectConfig;
  readonly outputDir: string;
  readonly resourcesDir: string;
  readonly engine: TemplateEngine;
  readonly ctx: Record<string, unknown>;
}
```

Then each private method becomes `(genCtx: GenerationContext, out: GenerationOutput)` (2 params).
This is a LOW-risk refactoring that does not change behavior.

**Verdict:** Non-blocking. The public API (`assemble`) correctly has 4 parameters. The private
method parameter counts are an internal code hygiene concern. The existing code is clear,
well-tested, and functional. This can be addressed in a follow-up refactoring commit.

------------------------------------------------------------

### Specialist Review Summary

| Specialist | Score | Status | Blocking Issues |
|-----------|-------|--------|----------------|
| Security | 20/20 | Approved | None |
| QA | 35/36 | Rejected | QA-16 (TDD strictness, LOW) |
| Performance | 26/26 | Approved | None |
| DevOps | 14/20 | Rejected | DEVOPS-05, DEVOPS-09, DEVOPS-10 |

**DevOps findings resolved:** The current code includes security context in K8s deployment
template (DEVOPS-05), lint step in CI workflow (DEVOPS-09), and Trivy image scanning
(DEVOPS-10). All three blocking issues from the DevOps review have been addressed in
subsequent commits.

**QA finding (QA-16):** Minor TDD discipline finding — 3 test assertions were refined in
the GREEN commit. The test intent did not change and no new tests were added. This is a
LOW severity process observation, not a code quality issue.

------------------------------------------------------------

### Merge Checklist

- [x] All tests passing (1797/1797)
- [x] Coverage >= 95% line (99.5%), >= 90% branch (97.6%)
- [x] Zero compiler/linter warnings
- [x] DB migration N/A
- [x] Security review approved (20/20)
- [x] Infrastructure manifests reviewed (K8s, Docker, CI)

------------------------------------------------------------

### Final Assessment

The implementation is clean, well-structured, and thoroughly tested. The `CicdAssembler`
follows all established codebase patterns for assemblers, integrates correctly into the
pipeline, and generates high-quality CI/CD artifacts with proper conditional logic. The
single finding (private method parameter counts) is a medium-severity code hygiene issue
that does not affect correctness, testability, or maintainability. All specialist review
blocking issues have been resolved.

**Decision: GO** — Approved for merge.
