# STORY-006 Implementation Plan — Domain Mappings & Constants

## 1. Affected Layers

- **Domain layer only** (`src/domain/`)
- No application, adapter, or infrastructure changes

## 2. New Files to Create

| File | Purpose | Lines (est.) |
|------|---------|-------------|
| `src/domain/stack-mapping.ts` | Language commands, framework ports/health/rules, Docker images, settings keys | ~220 |
| `src/domain/stack-pack-mapping.ts` | Framework to knowledge pack mapping | ~25 |
| `src/domain/pattern-mapping.ts` | Pattern selection by architecture style | ~55 |
| `src/domain/protocol-mapping.ts` | Protocol derivation from interfaces | ~75 |
| `src/domain/core-kp-routing.ts` | Core rule to KP routing with conditions | ~60 |
| `src/domain/version-resolver.ts` | Version directory lookup with fallback | ~25 |
| `src/domain/resolved-stack.ts` | ResolvedStack interface | ~20 |

## 3. Existing Files to Modify

| File | Change |
|------|--------|
| `src/domain/index.ts` | Re-export all new domain modules |

## 4. Dependency Direction

All modules depend only on `src/models.ts` (STORY-003). No framework or adapter dependencies.

## 5. Integration Points

- `ProjectConfig` from `src/models.ts` used by pattern-mapping, protocol-mapping, core-kp-routing
- `ResolvedStack` will be consumed by STORY-007 (resolver)
- All mappings consumed by assemblers (STORY-009 through STORY-014)

## 6. Test Files

| File | Coverage |
|------|----------|
| `tests/node/domain/stack-mapping.test.ts` | All 8 LANGUAGE_COMMANDS entries, ports, health paths, helpers |
| `tests/node/domain/stack-pack-mapping.test.ts` | All 11 framework entries |
| `tests/node/domain/pattern-mapping.test.ts` | selectPatterns for each style, event-driven |
| `tests/node/domain/protocol-mapping.test.ts` | deriveProtocols, deriveProtocolFiles, broker filtering |
| `tests/node/domain/core-kp-routing.test.ts` | getActiveRoutes with/without conditions |
| `tests/node/domain/version-resolver.test.ts` | Exact match, major.x fallback, not found |
| `tests/node/domain/resolved-stack.test.ts` | Interface structure validation |

## 7. Design Decisions

- Python tuple keys `(language, build_tool)` become string keys `"language-build_tool"` in TypeScript
- Python `Optional[Path]` becomes `string | undefined` for version-resolver (no Node `Path` objects)
- `fs` module used for directory checks in version-resolver and file listing
- All readonly arrays use `readonly` modifier
- `ResolvedStack` as interface (not class) since it's a pure data structure

## 8. Risk Assessment

- **Low risk**: Pure data migration with no external dependencies
- **Data fidelity**: Every mapping value must be identical to Python
