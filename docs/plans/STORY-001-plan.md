# Implementation Plan — STORY-001: Project Foundation (Node.js + TypeScript)

**Status:** PLANNED  
**Scope:** Foundation only (tooling + stubs), no feature migration yet  
**Constraints Applied:** RULE-011 (`resources/` must remain unchanged)  
**Context Notes:** `docs/adr/` was checked and no ADR files were found.

---

## 1) Affected layers and components

| Layer | Component | Planned impact |
|---|---|---|
| Foundation / Build | `package.json` | Initialize Node package metadata, scripts, runtime/dev dependencies, CLI `bin` mapping |
| Foundation / TS Compiler | `tsconfig.json` | Enable strict TypeScript + NodeNext ESM output to `dist/` |
| Foundation / Bundling | `tsup.config.ts` | Bundle CLI entrypoint, emit ESM + d.ts, preserve shebang |
| Foundation / Testing | `vitest.config.ts` | Configure runner + coverage (v8, thresholds 95/90, excludes) |
| Inbound CLI adapter (stub) | `src/index.ts`, `src/cli.ts` | Minimal executable CLI entry with `--help` working |
| Shared core modules (stubs) | `src/config.ts`, `src/models.ts`, `src/template-engine.ts`, `src/utils.ts`, `src/exceptions.ts`, `src/interactive.ts` | Placeholder modules to unblock STORIES 002–005 |
| Assembly boundary (stub) | `src/assembler/index.ts` | Initial orchestrator contract (no migration logic yet) |
| Domain boundary (stub) | `src/domain/` | Reserved directory for future domain modules |
| Test assets | `tests/fixtures/` | Base fixture directory for future Vitest specs |
| Repo hygiene | `.gitignore` (and optionally README quickstart note) | Add Node artifacts ignore rules; document Node foundation usage if needed |

---

## 2) New classes/interfaces to create (with package locations)

> STORY-001 is intentionally stub-heavy. Interfaces below are minimal contracts to allow later stories to compile cleanly.

| Class / Interface / Function | Package location | Responsibility in STORY-001 |
|---|---|---|
| `bootstrap()` / CLI entry | `src/index.ts` | Entry point with shebang, invokes CLI command setup |
| `createCli()` | `src/cli.ts` | Builds Commander program with placeholder command/help |
| `RuntimePaths` (interface) | `src/config.ts` | Centralize resolved paths (`cwd`, `outputDir`, `resourcesDir`) |
| `ProjectConfig` (interface stub) | `src/models.ts` | Initial typed config contract placeholder for future stories |
| `TemplateEngine` (class stub) | `src/template-engine.ts` | Placeholder for Nunjucks integration in STORY-005 |
| `CliError` / base error hierarchy | `src/exceptions.ts` | Shared typed error foundation for future flows |
| `prompt*` helpers (stubs) | `src/interactive.ts` | Placeholder interactive helpers for STORY-017 |
| `Assembler` (interface) | `src/assembler/index.ts` | Pipeline contract placeholder for future assemblers |
| `src/domain/index.ts` (barrel stub) | `src/domain/index.ts` | Domain namespace root for STORY-006+ |

---

## 3) Existing classes to modify

| File / Module | Planned change |
|---|---|
| `.gitignore` | Add/validate Node/TS ignores (`node_modules/`, `coverage/`, `*.tsbuildinfo`) while preserving existing Python ignores |
| `README.md` (optional but recommended) | Add “Node.js + TypeScript foundation” section with install/build/test commands; keep current Python docs during migration period |
| `pyproject.toml` | **No change in STORY-001** (kept temporarily for migration continuity) |
| `src/ia_dev_env/**` (Python code) | **No change in STORY-001**; migration logic starts in later stories |

---

## 4) Dependency direction validation

Planned dependency direction for foundation stubs:

1. `src/index.ts` (inbound CLI entry) → `src/cli.ts`
2. `src/cli.ts` (inbound adapter) → shared stubs (`config/models/exceptions/interactive`) and, later, orchestration
3. `src/assembler/index.ts` (application/orchestration boundary) → domain contracts (`src/domain/**`, `src/models.ts`)
4. `src/domain/**` (core) → no adapter/framework imports

Validation rules to enforce from day 1:
- `src/domain/**` must not import Commander, Inquirer, filesystem adapters, or CLI modules.
- Third-party framework libraries (`commander`, `inquirer`) stay in inbound-facing modules.
- Dependencies point inward toward domain boundaries, consistent with architecture principles.

---

## 5) Integration points

| Integration point | Description |
|---|---|
| CLI binary resolution | `package.json` maps `ia-dev-env` to `./dist/index.js` |
| Build output | `tsup` compiles `src/index.ts` to runnable ESM in `dist/` |
| Local execution | `npx ia-dev-env --help` validates bundled CLI stub |
| Test harness | Vitest runs even with no tests yet (foundation readiness check) |
| Existing repository state | Python implementation remains present; Node foundation is added in parallel to unblock incremental migration |
| Resources contract | `resources/` is only referenced, never modified (RULE-011) |

---

## 6) Database changes (if applicable)

Not applicable. STORY-001 has no persistence, schema, migration, or database adapter changes.

---

## 7) API changes (if applicable)

No HTTP/gRPC/event API changes.  
CLI surface adds only a basic executable help stub (`ia-dev-env --help`) as foundation behavior.

---

## 8) Event changes (if applicable)

Not applicable. No message bus, domain event, or integration event behavior is introduced in STORY-001.

---

## 9) Configuration changes

| Config artifact | Planned values / behavior |
|---|---|
| `package.json` | `name=ia-dev-environment`, `version=0.1.0`, `license=MIT`, `type=module`, `bin.ia-dev-env=./dist/index.js` |
| npm scripts | `build`, `dev`, `test`, `test:coverage`, `lint` (foundation commands only) |
| Runtime deps | `commander`, `js-yaml`, `nunjucks`, `inquirer` |
| Dev deps | `typescript`, `tsup`, `vitest`, `@types/node`, `@types/js-yaml`, `@types/nunjucks`, `tsx` |
| `tsconfig.json` | `target=ES2022`, `module=NodeNext`, `strict=true`, `rootDir=src`, `outDir=dist`, include/exclude per story |
| `tsup.config.ts` | Entry `src/index.ts`, ESM output, Node 18 target, bundle enabled, declarations enabled, shebang preserved |
| `vitest.config.ts` | Coverage provider `v8`, thresholds line 95 / branch 90, excludes include `dist/`, `resources/`, `tests/` |
| Directory scaffold | Create `src/`, `src/assembler/`, `src/domain/`, `tests/fixtures/` and stub files listed in section 2 |

Execution order:
1) package + dependency setup  
2) TS + tsup + vitest configs  
3) directory/file stubs  
4) CLI help smoke check (`npx ia-dev-env --help`)

---

## 10) Risk assessment

| Risk | Impact | Mitigation |
|---|---|---|
| ESM/NodeNext import resolution issues | Build/runtime failures early in migration | Keep extensions/import style consistent from STORY-001; validate with `npm run build` and CLI smoke run |
| Shebang not preserved after bundling | Generated binary not executable via `npx ia-dev-env` | Explicit shebang in `src/index.ts` + verify output file header in DoD check |
| Coverage thresholds with zero tests | `test:coverage` may fail before test stories | Configure Vitest to allow empty suites for foundation and validate coverage behavior explicitly |
| Parallel Python + Node toolchains cause confusion | Contributors run wrong commands | README section and script names clarify transitional workflow (Python legacy vs Node foundation) |
| Accidental edits under `resources/` | Violates RULE-011 and risks parity drift | Limit STORY-001 changes to root config + `src/`/`tests/` scaffolding; add review checklist item “resources unchanged” |
| Over-scaffolding beyond foundation | Scope creep and delayed STORY-001 completion | Keep implementations as stubs/contracts only; defer business logic to STORIES 002+ |
