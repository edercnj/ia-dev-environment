# Test Plan -- STORY-004: Config Loader (Python to TypeScript Migration)

**Status:** DRAFT
**Date:** 2026-03-09
**Test Framework:** vitest
**Test File:** `tests/node/config.test.ts`
**Coverage Targets:** >= 95% line, >= 90% branch
**Naming Convention:** `[methodUnderTest]_[scenario]_[expectedBehavior]`

---

## 1. Constants Validation

### 1.1 TYPE_MAPPING (5 entries)

| # | Test Name | Input Key | Expected Style | Expected Interfaces |
|---|-----------|-----------|---------------|---------------------|
| 1 | `TYPE_MAPPING_apiKey_returnsMicroserviceWithRest` | `"api"` | `"microservice"` | `[{ type: "rest" }]` |
| 2 | `TYPE_MAPPING_cliKey_returnsLibraryWithCli` | `"cli"` | `"library"` | `[{ type: "cli" }]` |
| 3 | `TYPE_MAPPING_libraryKey_returnsLibraryWithNoInterfaces` | `"library"` | `"library"` | `[]` |
| 4 | `TYPE_MAPPING_workerKey_returnsMicroserviceWithEventConsumer` | `"worker"` | `"microservice"` | `[{ type: "event-consumer" }]` |
| 5 | `TYPE_MAPPING_fullstackKey_returnsMonolithWithRest` | `"fullstack"` | `"monolith"` | `[{ type: "rest" }]` |

### 1.2 STACK_MAPPING (8 entries)

| # | Test Name | Input Key | Expected Language | Expected Version | Expected Framework | Expected FW Version |
|---|-----------|-----------|-------------------|------------------|--------------------|---------------------|
| 6 | `STACK_MAPPING_javaQuarkus_returnsCorrectValues` | `"java-quarkus"` | `"java"` | `"21"` | `"quarkus"` | `"3.17"` |
| 7 | `STACK_MAPPING_javaSpring_returnsCorrectValues` | `"java-spring"` | `"java"` | `"21"` | `"spring-boot"` | `"3.4"` |
| 8 | `STACK_MAPPING_pythonFastapi_returnsCorrectValues` | `"python-fastapi"` | `"python"` | `"3.12"` | `"fastapi"` | `"0.115"` |
| 9 | `STACK_MAPPING_pythonClickCli_returnsCorrectValues` | `"python-click-cli"` | `"python"` | `"3.9"` | `"click"` | `"8.1"` |
| 10 | `STACK_MAPPING_goGin_returnsCorrectValues` | `"go-gin"` | `"go"` | `"1.23"` | `"gin"` | `"1.10"` |
| 11 | `STACK_MAPPING_kotlinKtor_returnsCorrectValues` | `"kotlin-ktor"` | `"kotlin"` | `"2.1"` | `"ktor"` | `"3.0"` |
| 12 | `STACK_MAPPING_typescriptNestjs_returnsCorrectValues` | `"typescript-nestjs"` | `"typescript"` | `"5.7"` | `"nestjs"` | `"10.4"` |
| 13 | `STACK_MAPPING_rustAxum_returnsCorrectValues` | `"rust-axum"` | `"rust"` | `"1.83"` | `"axum"` | `"0.8"` |

### 1.3 REQUIRED_SECTIONS (5 entries)

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 14 | `REQUIRED_SECTIONS_containsAllFiveSections` | `REQUIRED_SECTIONS` | Contains exactly `["project", "architecture", "interfaces", "language", "framework"]` |

---

## 2. `detectV2Format(data)`

### 2.1 Returns `true` (v2 detected)

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 15 | `detectV2Format_typeInMapping_returnsTrue` | `{ type: "api" }` | `true` |
| 16 | `detectV2Format_stackInMapping_returnsTrue` | `{ stack: "java-spring" }` | `true` |
| 17 | `detectV2Format_bothTypeAndStack_returnsTrue` | `{ type: "cli", stack: "python-click-cli" }` | `true` |

### 2.2 Returns `false` (v3 or unknown)

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 18 | `detectV2Format_v3FormatNoTypeNoStack_returnsFalse` | `{ project: { name: "app" } }` | `false` |
| 19 | `detectV2Format_unknownType_returnsFalse` | `{ type: "unknown" }` | `false` |
| 20 | `detectV2Format_unknownStack_returnsFalse` | `{ stack: "unknown-framework" }` | `false` |
| 21 | `detectV2Format_emptyObject_returnsFalse` | `{}` | `false` |

### 2.3 Parametrized: All TYPE_MAPPING keys

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 22 | `detectV2Format_eachTypeMappingKey_returnsTrue` | `it.each` over `["api", "cli", "library", "worker", "fullstack"]` | Each returns `true` |

### 2.4 Parametrized: All STACK_MAPPING keys

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 23 | `detectV2Format_eachStackMappingKey_returnsTrue` | `it.each` over all 8 stack keys | Each returns `true` |

---

## 3. `migrateV2ToV3(data)`

### 3.1 Happy Path -- Type Migration

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 24 | `migrateV2ToV3_apiType_producesCorrectArchitectureAndInterfaces` | `{ type: "api", stack: "java-spring", project: { name: "svc" } }` | `architecture.style === "microservice"`, `interfaces === [{ type: "rest" }]` |
| 25 | `migrateV2ToV3_cliType_producesLibraryWithCliInterface` | `{ type: "cli", stack: "python-click-cli", project: { name: "tool" } }` | `architecture.style === "library"`, `interfaces === [{ type: "cli" }]` |
| 26 | `migrateV2ToV3_libraryType_producesLibraryWithNoInterfaces` | `{ type: "library", stack: "typescript-nestjs", project: { name: "lib" } }` | `architecture.style === "library"`, `interfaces === []` |
| 27 | `migrateV2ToV3_workerType_producesMicroserviceWithEventConsumer` | `{ type: "worker", stack: "java-quarkus", project: { name: "wkr" } }` | `architecture.style === "microservice"`, `interfaces === [{ type: "event-consumer" }]` |
| 28 | `migrateV2ToV3_fullstackType_producesMonolithWithRest` | `{ type: "fullstack", stack: "typescript-nestjs", project: { name: "app" } }` | `architecture.style === "monolith"`, `interfaces === [{ type: "rest" }]` |

### 3.2 Happy Path -- Stack Migration

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 29 | `migrateV2ToV3_javaSpringStack_producesCorrectLanguageAndFramework` | `{ type: "api", stack: "java-spring" }` | `language === { name: "java", version: "21" }`, `framework === { name: "spring-boot", version: "3.4" }` |
| 30 | `migrateV2ToV3_rustAxumStack_producesCorrectLanguageAndFramework` | `{ type: "api", stack: "rust-axum" }` | `language === { name: "rust", version: "1.83" }`, `framework === { name: "axum", version: "0.8" }` |

### 3.3 Parametrized: All 8 stacks produce correct language/framework

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 31 | `migrateV2ToV3_eachStack_producesCorrectLanguageAndFramework` | `it.each` over all 8 stack entries | `language.name`, `language.version`, `framework.name`, `framework.version` match STACK_MAPPING |

### 3.4 Project Section Handling

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 32 | `migrateV2ToV3_withProjectSection_preservesProject` | `{ type: "api", stack: "java-spring", project: { name: "my-svc", purpose: "payments" } }` | `result.project === { name: "my-svc", purpose: "payments" }` |
| 33 | `migrateV2ToV3_withoutProjectSection_createsDefaultProject` | `{ type: "api", stack: "java-spring" }` | `result.project === { name: "unnamed", purpose: "" }` |

### 3.5 Warning Emission

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 34 | `migrateV2ToV3_anyV2Input_emitsDeprecationWarning` | `{ type: "api", stack: "java-spring" }` | `console.warn` called with message containing `"legacy v2 format"` |

### 3.6 Error Cases

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 35 | `migrateV2ToV3_unknownStack_throwsConfigValidationError` | `{ type: "api", stack: "unknown-stack" }` | Throws `ConfigValidationError` with message containing `"Unknown stack"` |

### 3.7 Default Type Mapping (unknown type key)

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 36 | `migrateV2ToV3_unknownTypeWithValidStack_usesDefaultTypeMapping` | `{ type: "exotic", stack: "java-spring" }` | `architecture.style === "microservice"`, `interfaces === [{ type: "rest" }]` (DEFAULT_TYPE_MAPPING) |
| 37 | `migrateV2ToV3_missingTypeWithValidStack_usesDefaultTypeMapping` | `{ stack: "java-spring" }` | `architecture.style === "microservice"`, `interfaces === [{ type: "rest" }]` |

---

## 4. `validateConfig(data)`

### 4.1 Valid Config

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 38 | `validateConfig_allRequiredSectionsPresent_doesNotThrow` | `{ project: {}, architecture: {}, interfaces: [], language: {}, framework: {} }` | No error thrown |

### 4.2 Missing Sections (parametrized)

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 39 | `validateConfig_missingSingleSection_throwsConfigValidationErrorWithSectionName` | `it.each` over each of the 5 required sections, removing one at a time | Throws `ConfigValidationError` with `missingFields` containing the removed section |

### 4.3 Multiple Missing Sections

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 40 | `validateConfig_missingMultipleSections_throwsWithAllMissingSections` | `{ project: {} }` (missing 4 sections) | Throws `ConfigValidationError` with `missingFields.length === 4` |

### 4.4 Null/Undefined Input

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 41 | `validateConfig_nullInput_throwsConfigValidationErrorWithAllSections` | `null` | Throws `ConfigValidationError` with all 5 required sections in `missingFields` |
| 42 | `validateConfig_undefinedInput_throwsConfigValidationErrorWithAllSections` | `undefined` | Throws `ConfigValidationError` with all 5 required sections in `missingFields` |

### 4.5 Empty Object

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 43 | `validateConfig_emptyObject_throwsConfigValidationErrorWithAllSections` | `{}` | Throws `ConfigValidationError` with all 5 required sections in `missingFields` |

---

## 5. `loadConfig(path)`

### 5.1 Valid V3 Config

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 44 | `loadConfig_validV3Yaml_returnsProjectConfig` | Path to valid v3 YAML fixture | Returns `ProjectConfig` with correct `project.name`, `architecture.style`, `language.name`, `framework.name` |
| 45 | `loadConfig_validV3YamlWithOptionalSections_returnsCompleteConfig` | Path to full v3 YAML with data, infra, security, testing, mcp | Returns `ProjectConfig` with all optional sections populated |

### 5.2 V2 Config (Auto-Migration)

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 46 | `loadConfig_v2YamlWithTypeAndStack_migratesAndReturnsProjectConfig` | Path to v2 YAML fixture (`type: api`, `stack: java-spring`) | Returns valid `ProjectConfig` with `architecture.style === "microservice"`, `language.name === "java"` |
| 47 | `loadConfig_v2Yaml_emitsWarningDuringMigration` | Path to v2 YAML fixture | `console.warn` called with `"legacy v2 format"` message |

### 5.3 Error Cases

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 48 | `loadConfig_missingRequiredSection_throwsConfigValidationError` | Path to YAML missing `language` section | Throws `ConfigValidationError` with `missingFields` containing `"language"` |
| 49 | `loadConfig_emptyYamlFile_throwsConfigValidationError` | Path to empty YAML file | Throws `ConfigValidationError` with all 5 required sections |
| 50 | `loadConfig_nonExistentFile_throwsError` | Path to non-existent file | Throws `Error` (filesystem error) |
| 51 | `loadConfig_yamlWithOnlyComments_throwsConfigValidationError` | Path to YAML file containing only `# comment` | Throws `ConfigValidationError` (YAML parses to `null`) |

### 5.4 Integration: V2 with Each Stack

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 52 | `loadConfig_v2EachStack_migratesCorrectly` | `it.each` over all 8 stacks with valid v2 YAML content (written to temp file) | Each returns `ProjectConfig` with correct `language.name` and `framework.name` |

---

## 6. McpServerConfig `toJSON` (env masking -- inherited from STORY-003)

> Already covered in `models.test.ts`. No duplication needed here.

---

## 7. Test Fixtures

### 7.1 Valid V3 YAML (Minimal)

```yaml
project:
  name: test-app
  purpose: unit testing
architecture:
  style: hexagonal
interfaces:
  - type: rest
language:
  name: typescript
  version: "5"
framework:
  name: express
  version: "4"
```

### 7.2 Valid V3 YAML (Full)

```yaml
project:
  name: test-app
  purpose: full integration testing
architecture:
  style: hexagonal
  domain_driven: true
  event_driven: false
interfaces:
  - type: rest
    spec: openapi
language:
  name: typescript
  version: "5"
framework:
  name: express
  version: "4"
  build_tool: npm
  native_build: false
data:
  database:
    name: postgres
    version: "15"
  migration:
    name: flyway
    version: "9"
  cache:
    name: redis
    version: "7"
security:
  frameworks:
    - oauth2
    - jwt
testing:
  smoke_tests: true
  contract_tests: true
  coverage_line: 95
  coverage_branch: 90
```

### 7.3 V2 YAML Fixture (API + Java Spring)

```yaml
type: api
stack: java-spring
project:
  name: legacy-service
  purpose: payment processing
```

### 7.4 V2 YAML Fixture (CLI + Python Click)

```yaml
type: cli
stack: python-click-cli
project:
  name: my-tool
  purpose: CLI utility
```

### 7.5 Invalid YAML (Missing Language)

```yaml
project:
  name: broken-app
  purpose: testing
architecture:
  style: layered
interfaces:
  - type: rest
framework:
  name: express
  version: "4"
```

### 7.6 Empty YAML

```yaml
```

### 7.7 Fixture Factory Functions

```typescript
function aValidV3ConfigData(): Record<string, unknown> {
  return {
    project: { name: "test-app", purpose: "unit testing" },
    architecture: { style: "hexagonal" },
    interfaces: [{ type: "rest" }],
    language: { name: "typescript", version: "5" },
    framework: { name: "express", version: "4" },
  };
}

function aV2ConfigData(
  type: string,
  stack: string,
): Record<string, unknown> {
  return {
    type,
    stack,
    project: { name: "legacy-app", purpose: "testing" },
  };
}

function aV2ConfigDataWithoutProject(
  type: string,
  stack: string,
): Record<string, unknown> {
  return { type, stack };
}
```

---

## 8. Test Structure

```
tests/node/config.test.ts
  describe("TYPE_MAPPING")
    it.each(5 entries)("${key}_returnsCorrectStyleAndInterfaces")  // tests 1-5

  describe("STACK_MAPPING")
    it.each(8 entries)("${key}_returnsCorrectValues")  // tests 6-13

  describe("REQUIRED_SECTIONS")
    it("containsAllFiveSections")  // test 14

  describe("detectV2Format")
    it("typeInMapping_returnsTrue")  // test 15
    it("stackInMapping_returnsTrue")  // test 16
    it("bothTypeAndStack_returnsTrue")  // test 17
    it("v3FormatNoTypeNoStack_returnsFalse")  // test 18
    it("unknownType_returnsFalse")  // test 19
    it("unknownStack_returnsFalse")  // test 20
    it("emptyObject_returnsFalse")  // test 21
    it.each(5 types)("eachTypeMappingKey_${key}_returnsTrue")  // test 22
    it.each(8 stacks)("eachStackMappingKey_${key}_returnsTrue")  // test 23

  describe("migrateV2ToV3")
    describe("type migration")
      it.each(5 types)("${type}_producesCorrectArchAndInterfaces")  // tests 24-28
    describe("stack migration")
      it("javaSpringStack_producesCorrectLanguageAndFramework")  // test 29
      it("rustAxumStack_producesCorrectLanguageAndFramework")  // test 30
      it.each(8 stacks)("eachStack_producesCorrectLangAndFw")  // test 31
    describe("project section")
      it("withProjectSection_preservesProject")  // test 32
      it("withoutProjectSection_createsDefaultProject")  // test 33
    describe("warning emission")
      it("anyV2Input_emitsDeprecationWarning")  // test 34
    describe("error cases")
      it("unknownStack_throwsConfigValidationError")  // test 35
    describe("default type mapping")
      it("unknownTypeWithValidStack_usesDefaultTypeMapping")  // test 36
      it("missingTypeWithValidStack_usesDefaultTypeMapping")  // test 37

  describe("validateConfig")
    it("allRequiredSectionsPresent_doesNotThrow")  // test 38
    it.each(5 sections)("missing_${section}_throwsConfigValidationError")  // test 39
    it("missingMultipleSections_throwsWithAllMissingSections")  // test 40
    it("nullInput_throwsConfigValidationError")  // test 41
    it("undefinedInput_throwsConfigValidationError")  // test 42
    it("emptyObject_throwsConfigValidationError")  // test 43

  describe("loadConfig")
    it("validV3Yaml_returnsProjectConfig")  // test 44
    it("validV3YamlWithOptionalSections_returnsCompleteConfig")  // test 45
    it("v2YamlWithTypeAndStack_migratesAndReturnsProjectConfig")  // test 46
    it("v2Yaml_emitsWarningDuringMigration")  // test 47
    it("missingRequiredSection_throwsConfigValidationError")  // test 48
    it("emptyYamlFile_throwsConfigValidationError")  // test 49
    it("nonExistentFile_throwsError")  // test 50
    it("yamlWithOnlyComments_throwsConfigValidationError")  // test 51
    it.each(8 stacks)("v2EachStack_${stack}_migratesCorrectly")  // test 52
```

---

## 9. Coverage Analysis

| Category | Test Count | Functions Covered | Branch Points |
|----------|-----------|-------------------|---------------|
| Constants (TYPE_MAPPING) | 5 | Constant validation | Each of 5 entries |
| Constants (STACK_MAPPING) | 8 | Constant validation | Each of 8 entries |
| Constants (REQUIRED_SECTIONS) | 1 | Constant validation | 5 entries exist |
| `detectV2Format` | 9 + parametrized | 1 function | `type in` / `stack in` / both / neither / unknown |
| `migrateV2ToV3` | 14 + parametrized | 3 functions (`migrateV2ToV3`, `_buildArchitectureSection`, `_buildLanguageFramework`) | type in/not in mapping, stack in/not in mapping, project present/absent, warning path |
| `validateConfig` | 6 + parametrized | 1 function | null input, empty, single missing, multiple missing, all present |
| `loadConfig` | 8 + parametrized | 1 function | null YAML, v2 detected, v3 passthrough, file not found, validation fail |
| **Total** | **~52 test cases** (before parametrized expansion), **~78 assertions** (after expansion) | **6 functions + 3 constants** | |

### Branch Coverage Strategy

| Branch | Covered By |
|--------|------------|
| `detectV2Format`: `type` in TYPE_MAPPING | Tests 15, 22 |
| `detectV2Format`: `stack` in STACK_MAPPING | Tests 16, 23 |
| `detectV2Format`: neither match | Tests 18-21 |
| `migrateV2ToV3`: `project` key present | Test 32 |
| `migrateV2ToV3`: `project` key absent | Test 33 |
| `migrateV2ToV3`: type in TYPE_MAPPING | Tests 24-28 |
| `migrateV2ToV3`: type not in TYPE_MAPPING (default) | Tests 36-37 |
| `migrateV2ToV3`: stack not in STACK_MAPPING | Test 35 |
| `validateConfig`: data is null | Test 41 |
| `validateConfig`: missing sections present | Tests 39-40, 43 |
| `validateConfig`: no missing sections | Test 38 |
| `loadConfig`: YAML parses to null | Tests 49, 51 |
| `loadConfig`: v2 format detected | Tests 46-47, 52 |
| `loadConfig`: v3 format (no migration) | Tests 44-45 |
| `loadConfig`: validation fails | Test 48 |

Estimated coverage: **>= 95% line, >= 90% branch**.

---

## 10. Filesystem Strategy for `loadConfig` Tests

`loadConfig` reads from the filesystem. Strategy:

1. Use `node:fs` + `node:os` to create temporary YAML files in `beforeEach`
2. Clean up in `afterEach` using `fs.rmSync`
3. Use `os.tmpdir()` + unique suffix per test to avoid collisions

```typescript
import { mkdtempSync, writeFileSync, rmSync } from "node:fs";
import { join } from "node:path";
import { tmpdir } from "node:os";

let tempDir: string;

beforeEach(() => {
  tempDir = mkdtempSync(join(tmpdir(), "config-test-"));
});

afterEach(() => {
  rmSync(tempDir, { recursive: true, force: true });
});

function writeTempYaml(filename: string, content: string): string {
  const filePath = join(tempDir, filename);
  writeFileSync(filePath, content, "utf-8");
  return filePath;
}
```

---

## 11. Warning/Console Capture Strategy

For tests that verify warning emission (tests 34, 47):

```typescript
let warnSpy: ReturnType<typeof vi.spyOn>;

beforeEach(() => {
  warnSpy = vi.spyOn(console, "warn").mockImplementation(() => {});
});

afterEach(() => {
  warnSpy.mockRestore();
});
```

> **Note:** The Python implementation uses `warnings.warn()`. The TypeScript port should use `console.warn()` or a similar mechanism. Adjust spy target based on actual implementation.

---

## 12. Assertions Library

Use vitest built-in assertions:
- `expect(value).toBe(expected)` for primitives
- `expect(value).toEqual(expected)` for deep equality (arrays, objects)
- `expect(value).toBeInstanceOf(ClassName)` for type checks
- `expect(() => fn()).toThrow(ConfigValidationError)` for error class matching
- `expect(() => fn()).toThrow(/pattern/)` for error message matching
- `expect(spy).toHaveBeenCalledWith(expect.stringContaining("..."))` for warning verification

---

## 13. Run Commands

```bash
# Run config tests only
npx vitest run tests/node/config.test.ts

# Run with coverage
npx vitest run tests/node/config.test.ts --coverage

# Run in watch mode during development
npx vitest tests/node/config.test.ts

# Run all tests
npx vitest run
```
