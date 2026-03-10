# STORY-009: RulesAssembler Migration — Implementation Plan

## Affected Layers

- `src/assembler/` — new files for rules assembly
- `tests/node/assembler/` — test files

## New Files

| File | Lines | Purpose |
|------|-------|---------|
| `src/assembler/rules-identity.ts` | ~90 | Identity content builders (Layer 4) |
| `src/assembler/rules-conditionals.ts` | ~200 | Conditional assembly functions |
| `src/assembler/rules-assembler.ts` | ~200 | Main RulesAssembler class |
| `tests/node/assembler/rules-assembler.test.ts` | ~300 | Tests for all layers |
| `tests/node/assembler/rules-identity.test.ts` | ~150 | Tests for identity builders |
| `tests/node/assembler/rules-conditionals.test.ts` | ~200 | Tests for conditionals |

## Modified Files

| File | Change |
|------|--------|
| `src/assembler/index.ts` | Export RulesAssembler + conditionals + identity |

## Layer Mapping

| Layer | Method | Description |
|-------|--------|-------------|
| 1 | `copyCoreRules()` | Copy core-rules/*.md with placeholder replacement |
| 1b | `routeCoreToKps()` | Route core detailed rules to KPs |
| 2 | `copyLanguageKps()` | Language files to coding-standards + testing KPs |
| 3 | `copyFrameworkKps()` | Framework files to stack-patterns KP |
| 4 | `generateProjectIdentity()` | Generate 01-project-identity.md |
| 4 | `copyDomainTemplate()` | Copy/generate 02-domain.md |
| C1 | `copyDatabaseRefs()` | Database references (SQL/NoSQL) |
| C2 | `copyCacheRefs()` | Cache references |
| C3 | `assembleSecurityRules()` | Security + compliance files |
| C4 | `assembleCloudKnowledge()` | Cloud provider files |
| C5 | `assembleInfraKnowledge()` | K8s, containers, IaC files |

## Dependencies Used

- `src/domain/version-resolver.ts` → `findVersionDir()`
- `src/domain/core-kp-routing.ts` → `getActiveRoutes()`
- `src/domain/stack-pack-mapping.ts` → `getStackPackName()`
- `src/assembler/auditor.ts` → `auditRulesContext()`
- `src/assembler/copy-helpers.ts` → `replacePlaceholdersInDir()`
- `src/template-engine.ts` → `TemplateEngine`
- `src/models.ts` → `ProjectConfig`

## Return Type

```typescript
interface AssembleResult {
  files: string[];
  warnings: string[];
}
```

## Risk Assessment

- Low risk: all dependencies migrated and tested (STORY-006, 007, 008)
- Pattern well-established from previous assembler migrations
- Module split keeps each file under 250-line limit
