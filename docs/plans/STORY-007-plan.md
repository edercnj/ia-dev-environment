# STORY-007 Implementation Plan — Domain Validator, Resolver & Skill Registry

## 1. Affected Layers and Components

- **Domain layer only** (`src/domain/`)
- No application, adapter, or infrastructure changes
- Depends on STORY-003 models (`src/models.ts`) and STORY-006 mappings (`src/domain/stack-mapping.ts`, `src/domain/resolved-stack.ts`)

## 2. New Files to Create

| File | Purpose | Python Source | Lines (est.) |
|------|---------|--------------|-------------|
| `src/domain/validator.ts` | Stack validation: language-framework compatibility, version constraints, native build, interface types, architecture styles, cross-references | `domain/validator.py` (196 lines) | ~170 |
| `src/domain/resolver.ts` | `resolveStack(config)`: derives commands, Docker image, health path, port, project type, protocols from `ProjectConfig` | `domain/resolver.py` (135 lines) | ~120 |
| `src/domain/skill-registry.ts` | `CORE_KNOWLEDGE_PACKS` constant (11 packs) and `buildInfraPackRules(config)` conditional infra pack selection | `domain/skill_registry.py` (35 lines) | ~40 |

## 3. Existing Files to Modify

| File | Change |
|------|--------|
| `src/domain/index.ts` | Add re-exports: `export * from "./validator.js"`, `export * from "./resolver.js"`, `export * from "./skill-registry.js"` |

## 4. Dependency Direction Validation

```
validator.ts  --> stack-mapping.ts (FRAMEWORK_LANGUAGE_RULES, NATIVE_SUPPORTED_FRAMEWORKS,
                                    VALID_INTERFACE_TYPES, VALID_ARCHITECTURE_STYLES)
              --> models.ts (ProjectConfig)

resolver.ts   --> stack-mapping.ts (LANGUAGE_COMMANDS, FRAMEWORK_PORTS, FRAMEWORK_HEALTH_PATHS,
                                    DOCKER_BASE_IMAGES, DEFAULT_*, NATIVE_SUPPORTED_FRAMEWORKS,
                                    INTERFACE_SPEC_PROTOCOL_MAP)
              --> resolved-stack.ts (ResolvedStack)
              --> models.ts (ProjectConfig, LanguageConfig, FrameworkConfig)

skill-registry.ts --> models.ts (ProjectConfig, InfraConfig)
```

All dependencies point inward (domain only). No framework, adapter, or external library imports. Only `node:fs` and `node:path` needed in `validator.ts` for `verifyCrossReferences` (filesystem check, same pattern as existing `version-resolver.ts`).

## 5. Integration Points

| Consumer | What it uses |
|----------|-------------|
| STORY-009 (assemblers) | `resolveStack()` to get computed values |
| STORY-010 (pipeline) | `validateStack()` as pre-flight check |
| STORY-011 (CLI) | `validateStack()` before generation |
| Assemblers | `CORE_KNOWLEDGE_PACKS` and `buildInfraPackRules()` to determine which KPs to include |
| `tests/fixtures/project-config.fixture.ts` | Shared fixture already exists; extend with validator/resolver-specific helpers |

## 6. Database Changes

N/A -- no persistence in this project.

## 7. API Changes

N/A -- no HTTP/CLI interface changes.

## 8. Event Changes

N/A -- no event system.

## 9. Configuration Changes

N/A -- no new configuration required.

## 10. Risk Assessment

| Risk | Level | Mitigation |
|------|-------|------------|
| Data fidelity: validation rules must reject same combos as Python | Medium | Parametrized tests mirroring all Python test cases (15 valid combos, 10 invalid combos, 3 version constraints) |
| Resolver output parity with Python | Medium | Full resolution tests for java-quarkus, python-click, unknown-language; assert every field |
| Version parsing edge cases (empty, non-numeric, alpha) | Low | Dedicated tests for `extractMajor()` / `extractMinor()` edge cases |
| `INTERFACE_SPEC_PROTOCOL_MAP` vs `INTERFACE_PROTOCOL_MAP` naming divergence | Low | Python `resolver.py` uses `INTERFACE_PROTOCOL_MAP` (string values), while STORY-006 TypeScript has `INTERFACE_SPEC_PROTOCOL_MAP` (string values). The resolver must use `INTERFACE_SPEC_PROTOCOL_MAP` from `stack-mapping.ts`. Note: `protocol-mapping.ts` has a different `INTERFACE_PROTOCOL_MAP` (array values) used for file routing, not for `ResolvedStack.protocols`. |
| `verifyCrossReferences` uses filesystem | Low | Same pattern as `version-resolver.ts` (`statSync`); test with temp dirs |

## 11. Design Decisions

### validator.ts
- `validateStack(config: ProjectConfig): string[]` -- returns array of error strings (not throwing), matching Python behavior
- `verifyCrossReferences(config: ProjectConfig, resourcesDir: string): string[]` -- filesystem check
- `extractMajor(version: string): number | undefined` and `extractMinor(version: string): number | undefined` -- exported for testability
- Named constants: `JAVA_17_MINIMUM`, `PYTHON_310_MINOR`, `FRAMEWORK_VERSION_3`, `FRAMEWORK_VERSION_5`, `EXPECTED_DIRECTORIES`

### resolver.ts
- `resolveStack(config: ProjectConfig): ResolvedStack` -- returns frozen/readonly object
- Private helpers: `resolveCommands()`, `resolveDockerImage()`, `resolveHealthPath()`, `resolveDefaultPort()`, `inferNativeBuild()`, `deriveProjectType()`, `deriveProtocols()`
- Uses string key `"${language}-${buildTool}"` to look up `LANGUAGE_COMMANDS` (matching STORY-006 convention)
- Protocol derivation uses `INTERFACE_SPEC_PROTOCOL_MAP` from `stack-mapping.ts` (maps interface type to protocol spec name like "openapi", "proto3", "kafka")

### skill-registry.ts
- `CORE_KNOWLEDGE_PACKS: readonly string[]` -- 11 entries
- `buildInfraPackRules(config: ProjectConfig): ReadonlyArray<readonly [string, boolean]>` -- returns tuple array `[packName, condition]`
- Uses `InfraConfig` fields: `orchestrator`, `templating`, `container`, `registry`, `iac`

## 12. Test Files

| File | Coverage |
|------|----------|
| `tests/node/domain/validator.test.ts` | 15 valid combos, 10 invalid combos, 3 version constraints, native build, interface types, architecture styles, multiple errors, version parsing edge cases, cross-references |
| `tests/node/domain/resolver.test.ts` | 8 language-command combos, 11 ports, 11 health paths, 8 Docker images, 6 protocol combos, 5 project types, 5 native build combos, 3 full resolution tests |
| `tests/node/domain/skill-registry.test.ts` | CORE_KNOWLEDGE_PACKS count + membership, 7 infra pack rules |

## 13. Implementation Order

1. `src/domain/validator.ts` -- version parsing helpers first, then validation functions
2. `src/domain/resolver.ts` -- depends on `stack-mapping.ts` constants and `ResolvedStack` interface
3. `src/domain/skill-registry.ts` -- standalone, depends only on `models.ts`
4. Update `src/domain/index.ts` -- add re-exports
5. `tests/node/domain/validator.test.ts`
6. `tests/node/domain/resolver.test.ts`
7. `tests/node/domain/skill-registry.test.ts`
