# Test Plan -- STORY-003: Models (Python to TypeScript Migration)

**Status:** COMPLETED
**Date:** 2026-03-09
**Test Framework:** vitest
**Test File:** `tests/node/models.test.ts`
**Coverage Targets:** >= 95% line, >= 90% branch
**Naming Convention:** `[methodUnderTest]_[scenario]_[expectedBehavior]`

---

## 1. `requireField()` Helper Function

### 1.1 Happy Path

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 1 | `keyExists_returnsValue` | `{ name: "test" }`, key `"name"`, model `"M"` | Returns `"test"` |
| 2 | `falsyValue_zero_returnsValue` | `{ v: 0 }`, key `"v"`, model `"M"` | Returns `0` |
| 3 | `falsyValue_false_returnsValue` | `{ v: false }`, key `"v"`, model `"M"` | Returns `false` |
| 4 | `falsyValue_emptyString_returnsValue` | `{ v: "" }`, key `"v"`, model `"M"` | Returns `""` |
| 5 | `falsyValue_null_returnsValue` | `{ v: null }`, key `"v"`, model `"M"` | Returns `null` |

### 1.2 Error Cases

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 6 | `keyMissing_throwsErrorWithMessage` | `{}`, key `"name"`, model `"ProjectIdentity"` | Throws `Error` with message `"Missing required field 'name' in ProjectIdentity"` |
| 7 | `keyMissingDifferentModel_includesModelNameInMessage` | `{}`, key `"style"`, model `"ArchitectureConfig"` | Throws `Error` with message containing `"style"` and `"ArchitectureConfig"` |

### 1.3 Edge Cases

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 8 | `keyExistsWithUndefinedValue_returnsUndefined` | `{ name: undefined }`, key `"name"`, model `"M"` | Returns `undefined` (key exists via `key in data` check) |

> **Note:** Implementation uses `key in data` check, so `{ name: undefined }` passes (key exists) and returns `undefined`. This matches the plan's recommended approach.

---

## 2. Leaf Models (No Nested Types)

### 2.1 TechComponent

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 9 | `TechComponent.fromDict_allFields_createsWithProvidedValues` | `{ name: "postgres", version: "15" }` | `name === "postgres"`, `version === "15"` |
| 10 | `TechComponent.fromDict_emptyObject_usesDefaults` | `{}` | `name === "none"`, `version === ""` |
| 11 | `TechComponent.fromDict_onlyName_usesDefaultVersion` | `{ name: "redis" }` | `name === "redis"`, `version === ""` |
| 12 | `TechComponent.fromDict_onlyVersion_usesDefaultName` | `{ version: "3.2" }` | `name === "none"`, `version === "3.2"` |

### 2.2 ProjectIdentity

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 13 | `ProjectIdentity.fromDict_allFields_createsWithProvidedValues` | `{ name: "my-app", purpose: "REST API" }` | `name === "my-app"`, `purpose === "REST API"` |
| 14 | `ProjectIdentity.fromDict_missingName_throwsError` | `{ purpose: "REST API" }` | Throws `Error` with message containing `"name"` and `"ProjectIdentity"` |
| 15 | `ProjectIdentity.fromDict_missingPurpose_throwsError` | `{ name: "my-app" }` | Throws `Error` with message containing `"purpose"` and `"ProjectIdentity"` |
| 16 | `ProjectIdentity.fromDict_emptyObject_throwsError` | `{}` | Throws `Error` (missing `name`) |

### 2.3 ArchitectureConfig

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 17 | `ArchitectureConfig.fromDict_allFields_createsWithProvidedValues` | `{ style: "hexagonal", domain_driven: true, event_driven: true }` | `style === "hexagonal"`, `domainDriven === true`, `eventDriven === true` |
| 18 | `ArchitectureConfig.fromDict_onlyRequired_usesDefaults` | `{ style: "layered" }` | `domainDriven === false`, `eventDriven === false` |
| 19 | `ArchitectureConfig.fromDict_missingStyle_throwsError` | `{}` | Throws `Error` with message containing `"style"` and `"ArchitectureConfig"` |

### 2.4 InterfaceConfig

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 20 | `InterfaceConfig.fromDict_allFields_createsWithProvidedValues` | `{ type: "rest", spec: "openapi", broker: "kafka" }` | `type === "rest"`, `spec === "openapi"`, `broker === "kafka"` |
| 21 | `InterfaceConfig.fromDict_onlyRequired_usesDefaults` | `{ type: "grpc" }` | `spec === ""`, `broker === ""` |
| 22 | `InterfaceConfig.fromDict_missingType_throwsError` | `{}` | Throws `Error` with message containing `"type"` and `"InterfaceConfig"` |

### 2.5 LanguageConfig

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 23 | `LanguageConfig.fromDict_allFields_createsWithProvidedValues` | `{ name: "typescript", version: "5" }` | `name === "typescript"`, `version === "5"` |
| 24 | `LanguageConfig.fromDict_missingName_throwsError` | `{ version: "5" }` | Throws `Error` containing `"name"` and `"LanguageConfig"` |
| 25 | `LanguageConfig.fromDict_missingVersion_throwsError` | `{ name: "typescript" }` | Throws `Error` containing `"version"` and `"LanguageConfig"` |

### 2.6 FrameworkConfig

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 26 | `FrameworkConfig.fromDict_allFields_createsWithProvidedValues` | `{ name: "express", version: "4", build_tool: "npm", native_build: true }` | All fields match input |
| 27 | `FrameworkConfig.fromDict_onlyRequired_usesDefaults` | `{ name: "express", version: "4" }` | `buildTool === "pip"`, `nativeBuild === false` |
| 28 | `FrameworkConfig.fromDict_missingName_throwsError` | `{ version: "4" }` | Throws `Error` containing `"name"` and `"FrameworkConfig"` |
| 29 | `FrameworkConfig.fromDict_missingVersion_throwsError` | `{ name: "express" }` | Throws `Error` containing `"version"` and `"FrameworkConfig"` |

### 2.7 SecurityConfig

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 30 | `SecurityConfig.fromDict_withFrameworks_createsWithList` | `{ frameworks: ["oauth2", "jwt"] }` | `frameworks` equals `["oauth2", "jwt"]` |
| 31 | `SecurityConfig.fromDict_emptyObject_usesDefaultEmptyArray` | `{}` | `frameworks` equals `[]` |
| 32 | `SecurityConfig.fromDict_emptyFrameworks_createsEmptyList` | `{ frameworks: [] }` | `frameworks` equals `[]` |

### 2.8 ObservabilityConfig

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 33 | `ObservabilityConfig.fromDict_allFields_createsWithProvidedValues` | `{ tool: "prometheus", metrics: "micrometer", tracing: "jaeger" }` | All fields match input |
| 34 | `ObservabilityConfig.fromDict_emptyObject_usesDefaults` | `{}` | `tool === "none"`, `metrics === "none"`, `tracing === "none"` |
| 35 | `ObservabilityConfig.fromDict_partialFields_usesDefaultsForMissing` | `{ tool: "grafana" }` | `tool === "grafana"`, `metrics === "none"`, `tracing === "none"` |

### 2.9 TestingConfig

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 36 | `TestingConfig.fromDict_allFields_createsWithProvidedValues` | `{ smoke_tests: false, contract_tests: true, performance_tests: false, coverage_line: 80, coverage_branch: 70 }` | All fields match input |
| 37 | `TestingConfig.fromDict_emptyObject_usesDefaults` | `{}` | `smokeTests === true`, `contractTests === false`, `performanceTests === true`, `coverageLine === 95`, `coverageBranch === 90` |
| 38 | `TestingConfig.fromDict_partialOverride_mergesWithDefaults` | `{ coverage_line: 80 }` | `coverageLine === 80`, all others use defaults |

### 2.10 McpServerConfig

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 39 | `McpServerConfig.fromDict_allFields_createsWithProvidedValues` | `{ id: "srv1", url: "http://localhost", capabilities: ["read"], env: { API_KEY: "x" } }` | All fields match input |
| 40 | `McpServerConfig.fromDict_onlyRequired_usesDefaults` | `{ id: "srv1", url: "http://localhost" }` | `capabilities` equals `[]`, `env` equals `{}` |
| 41 | `McpServerConfig.fromDict_missingId_throwsError` | `{ url: "http://localhost" }` | Throws `Error` containing `"id"` and `"McpServerConfig"` |
| 42 | `McpServerConfig.fromDict_missingUrl_throwsError` | `{ id: "srv1" }` | Throws `Error` containing `"url"` and `"McpServerConfig"` |

---

## 3. Composite Models (Nested Types)

### 3.1 DataConfig

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 43 | `DataConfig.fromDict_allFields_createsNestedTechComponents` | `{ database: { name: "postgres", version: "15" }, migration: { name: "flyway", version: "9" }, cache: { name: "redis", version: "7" } }` | Each nested field is a `TechComponent` with provided values |
| 44 | `DataConfig.fromDict_emptyObject_usesDefaultTechComponents` | `{}` | `database.name === "none"`, `migration.name === "none"`, `cache.name === "none"` |
| 45 | `DataConfig.fromDict_partialNested_usesDefaultsForMissingSubs` | `{ database: { name: "postgres" } }` | `database.name === "postgres"`, `database.version === ""`, `migration.name === "none"`, `cache.name === "none"` |
| 46 | `DataConfig.fromDict_nestedAreInstances_correctType` | `{ database: { name: "pg" } }` | `database` is instance of `TechComponent` |

### 3.2 InfraConfig

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 47 | `InfraConfig.fromDict_allFields_createsWithProvidedValues` | Full object with all 8 fields including nested observability | All fields match, `observability` is `ObservabilityConfig` instance |
| 48 | `InfraConfig.fromDict_emptyObject_usesAllDefaults` | `{}` | `container === "docker"`, `orchestrator === "none"`, `templating === "kustomize"`, `iac === "none"`, `registry === "none"`, `apiGateway === "none"`, `serviceMesh === "none"`, `observability.tool === "none"` |
| 49 | `InfraConfig.fromDict_nestedObservability_parsesCorrectly` | `{ observability: { tool: "prometheus", metrics: "micrometer" } }` | `observability.tool === "prometheus"`, `observability.metrics === "micrometer"`, `observability.tracing === "none"` |
| 50 | `InfraConfig.fromDict_partialTopLevel_usesDefaultsForMissing` | `{ container: "podman" }` | `container === "podman"`, all others use defaults |

### 3.3 McpConfig

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 51 | `McpConfig.fromDict_withServers_parsesAllServerConfigs` | `{ servers: [{ id: "a", url: "http://a" }, { id: "b", url: "http://b" }] }` | `servers.length === 2`, each is `McpServerConfig` instance with correct values |
| 52 | `McpConfig.fromDict_emptyObject_usesDefaultEmptyServers` | `{}` | `servers` equals `[]` |
| 53 | `McpConfig.fromDict_emptyServersArray_createsEmptyList` | `{ servers: [] }` | `servers` equals `[]` |
| 54 | `McpConfig.fromDict_serverMissingRequired_throwsError` | `{ servers: [{ id: "a" }] }` | Throws `Error` (McpServerConfig missing `url`) |

---

## 4. Root Model: ProjectConfig

### 4.1 Full Data

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 55 | `ProjectConfig.fromDict_allFields_createsCompleteConfig` | Full object with all 10 sections (project, architecture, interfaces, language, framework, data, infrastructure, security, testing, mcp) | All fields populated, nested types are correct instances |

### 4.2 Minimal Data (Required Only)

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 56 | `ProjectConfig.fromDict_onlyRequired_usesDefaultsForOptional` | Object with only `project`, `architecture`, `interfaces`, `language`, `framework` | `data.database.name === "none"`, `infrastructure.container === "docker"`, `security.frameworks === []`, `testing.smokeTests === true`, `mcp.servers === []` |

### 4.3 Missing Required Fields

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 57 | `ProjectConfig.fromDict_missingProject_throwsError` | Data without `project` key | Throws `Error` containing `"project"` and `"ProjectConfig"` |
| 58 | `ProjectConfig.fromDict_missingArchitecture_throwsError` | Data without `architecture` key | Throws `Error` containing `"architecture"` and `"ProjectConfig"` |
| 59 | `ProjectConfig.fromDict_missingInterfaces_throwsError` | Data without `interfaces` key | Throws `Error` containing `"interfaces"` and `"ProjectConfig"` |
| 60 | `ProjectConfig.fromDict_missingLanguage_throwsError` | Data without `language` key | Throws `Error` containing `"language"` and `"ProjectConfig"` |
| 61 | `ProjectConfig.fromDict_missingFramework_throwsError` | Data without `framework` key | Throws `Error` containing `"framework"` and `"ProjectConfig"` |

### 4.4 Interfaces List Parsing

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 62 | `ProjectConfig.fromDict_multipleInterfaces_parsesAll` | `interfaces: [{ type: "rest" }, { type: "grpc", spec: "proto3" }]` + other required fields | `interfaces.length === 2`, `interfaces[0].type === "rest"`, `interfaces[1].spec === "proto3"` |
| 63 | `ProjectConfig.fromDict_singleInterface_parsesList` | `interfaces: [{ type: "cli" }]` + other required fields | `interfaces.length === 1` |
| 64 | `ProjectConfig.fromDict_emptyInterfaces_createsEmptyList` | `interfaces: []` + other required fields | `interfaces.length === 0` |

### 4.5 Nested Type Validation

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 65 | `ProjectConfig.fromDict_nestedProject_isProjectIdentityInstance` | Full valid data | `project` is instance of `ProjectIdentity` |
| 66 | `ProjectConfig.fromDict_nestedArchitecture_isArchitectureConfigInstance` | Full valid data | `architecture` is instance of `ArchitectureConfig` |
| 67 | `ProjectConfig.fromDict_nestedData_containsTechComponentInstances` | Full valid data with `data` section | `data.database` is instance of `TechComponent` |
| 68 | `ProjectConfig.fromDict_nestedInfra_containsObservabilityInstance` | Full valid data with `infrastructure.observability` | `infrastructure.observability` is instance of `ObservabilityConfig` |
| 69 | `ProjectConfig.fromDict_nestedMcp_containsMcpServerConfigInstances` | Full valid data with `mcp.servers` | Each server is instance of `McpServerConfig` |

---

## 5. Constructor-Only Models (No `fromDict`)

### 5.1 PipelineResult

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 70 | `PipelineResult_constructor_createsInstanceWithAllFields` | `success: true, outputDir: "/out", filesGenerated: ["a.ts", "b.ts"], warnings: ["warn1"], durationMs: 1500` | All `readonly` fields match constructor args |
| 71 | `PipelineResult_constructor_emptyArrays_createsWithEmptyLists` | `success: false, outputDir: "/out", filesGenerated: [], warnings: [], durationMs: 0` | `filesGenerated === []`, `warnings === []` |

### 5.2 FileDiff

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 72 | `FileDiff_constructor_createsInstanceWithAllFields` | `path: "src/index.ts", diff: "- old\n+ new", pythonSize: 100, referenceSize: 120` | All fields match |
| 73 | `FileDiff_constructor_emptyDiff_createsWithEmptyString` | `path: "a.ts", diff: "", pythonSize: 0, referenceSize: 0` | `diff === ""` |

### 5.3 VerificationResult

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 74 | `VerificationResult_constructor_createsInstanceWithAllFields` | `success: true, totalFiles: 10, mismatches: [fileDiff], missingFiles: ["x.ts"], extraFiles: ["y.ts"]` | All fields match, `mismatches` contains `FileDiff` instance |
| 75 | `VerificationResult_constructor_noMismatches_createsWithEmptyArrays` | `success: true, totalFiles: 5, mismatches: [], missingFiles: [], extraFiles: []` | All arrays empty |

---

## 6. Edge Cases and Mutable Default Independence

### 6.1 Mutable Default Independence

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 76 | `SecurityConfig.fromDict_twoInstances_frameworksArraysAreIndependent` | Create two `SecurityConfig` from `{}`, push to one's `frameworks` | Other instance's `frameworks` remains `[]` |
| 77 | `McpConfig.fromDict_twoInstances_serversArraysAreIndependent` | Create two `McpConfig` from `{}`, verify `servers` arrays are distinct references | `servers` arrays are not the same reference |
| 78 | `McpServerConfig.fromDict_twoInstances_capabilitiesAreIndependent` | Create two `McpServerConfig` with `{ id: "a", url: "u" }` | `capabilities` arrays are not the same reference |
| 79 | `McpServerConfig.fromDict_twoInstances_envObjectsAreIndependent` | Create two `McpServerConfig` with `{ id: "a", url: "u" }` | `env` objects are not the same reference |
| 80 | `DataConfig.fromDict_twoInstances_nestedTechComponentsAreIndependent` | Create two `DataConfig` from `{}` | `database` instances are distinct objects |

### 6.2 Snake_case Key Mapping

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 81 | `ArchitectureConfig.fromDict_snakeCaseKeys_mapsToCamelCase` | `{ style: "hex", domain_driven: true, event_driven: true }` | `domainDriven === true`, `eventDriven === true` |
| 82 | `FrameworkConfig.fromDict_snakeCaseKeys_mapsToCamelCase` | `{ name: "x", version: "1", build_tool: "gradle", native_build: true }` | `buildTool === "gradle"`, `nativeBuild === true` |
| 83 | `TestingConfig.fromDict_snakeCaseKeys_mapsToCamelCase` | `{ smoke_tests: false, contract_tests: true, performance_tests: false, coverage_line: 80, coverage_branch: 75 }` | All camelCase properties match |
| 84 | `InfraConfig.fromDict_snakeCaseKeys_mapsToCamelCase` | `{ api_gateway: "kong", service_mesh: "istio" }` | `apiGateway === "kong"`, `serviceMesh === "istio"` |

### 6.3 Empty Object for All-Optional Models

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 85 | `TechComponent.fromDict_emptyObject_returnsValidDefaults` | `{}` | `name === "none"`, `version === ""` |
| 86 | `DataConfig.fromDict_emptyObject_returnsValidDefaults` | `{}` | All three nested `TechComponent` have default values |
| 87 | `SecurityConfig.fromDict_emptyObject_returnsValidDefaults` | `{}` | `frameworks === []` |
| 88 | `ObservabilityConfig.fromDict_emptyObject_returnsValidDefaults` | `{}` | All fields are `"none"` |
| 89 | `InfraConfig.fromDict_emptyObject_returnsValidDefaults` | `{}` | All string fields have correct defaults, nested observability has defaults |
| 90 | `TestingConfig.fromDict_emptyObject_returnsValidDefaults` | `{}` | All boolean and numeric defaults correct |
| 91 | `McpConfig.fromDict_emptyObject_returnsValidDefaults` | `{}` | `servers === []` |

---

## 7. Test Fixtures

### 7.1 Minimal Valid ProjectConfig Data

```typescript
function aMinimalProjectConfigData(): Record<string, unknown> {
  return {
    project: { name: "test-app", purpose: "testing" },
    architecture: { style: "hexagonal" },
    interfaces: [{ type: "rest" }],
    language: { name: "typescript", version: "5" },
    framework: { name: "express", version: "4" },
  };
}
```

### 7.2 Full ProjectConfig Data

```typescript
function aFullProjectConfigData(): Record<string, unknown> {
  return {
    project: { name: "test-app", purpose: "full test" },
    architecture: { style: "hexagonal", domain_driven: true, event_driven: false },
    interfaces: [
      { type: "rest", spec: "openapi", broker: "" },
      { type: "grpc", spec: "proto3", broker: "kafka" },
    ],
    language: { name: "typescript", version: "5" },
    framework: { name: "express", version: "4", build_tool: "npm", native_build: false },
    data: {
      database: { name: "postgres", version: "15" },
      migration: { name: "flyway", version: "9" },
      cache: { name: "redis", version: "7" },
    },
    infrastructure: {
      container: "docker",
      orchestrator: "kubernetes",
      templating: "helm",
      iac: "terraform",
      registry: "ecr",
      api_gateway: "kong",
      service_mesh: "istio",
      observability: { tool: "prometheus", metrics: "micrometer", tracing: "jaeger" },
    },
    security: { frameworks: ["oauth2", "jwt"] },
    testing: {
      smoke_tests: true,
      contract_tests: true,
      performance_tests: true,
      coverage_line: 95,
      coverage_branch: 90,
    },
    mcp: {
      servers: [
        { id: "srv1", url: "http://mcp1.local", capabilities: ["read", "write"], env: { TOKEN: "abc" } },
      ],
    },
  };
}
```

---

## 8. Test Structure

```
tests/node/models.test.ts
  describe("requireField helper")
    it("keyExists_returnsValue")
    it.each(falsyValues)("falsyValue_$label_returnsValue")
    it("keyMissing_throwsErrorWithMessage")
    it("keyMissingDifferentModel_includesModelNameInMessage")
    it("keyExistsWithUndefinedValue_returnsUndefined")

  describe("TechComponent")
    describe("fromDict")
      it(...)  // tests 9-12

  describe("ProjectIdentity")
    describe("fromDict")
      it(...)  // tests 13-16

  describe("ArchitectureConfig")
    describe("fromDict")
      it(...)  // tests 17-19

  describe("InterfaceConfig")
    describe("fromDict")
      it(...)  // tests 20-22

  describe("LanguageConfig")
    describe("fromDict")
      it(...)  // tests 23-25

  describe("FrameworkConfig")
    describe("fromDict")
      it(...)  // tests 26-29

  describe("SecurityConfig")
    describe("fromDict")
      it(...)  // tests 30-32

  describe("ObservabilityConfig")
    describe("fromDict")
      it(...)  // tests 33-35

  describe("TestingConfig")
    describe("fromDict")
      it(...)  // tests 36-38

  describe("McpServerConfig")
    describe("fromDict")
      it(...)  // tests 39-42

  describe("DataConfig")
    describe("fromDict")
      it(...)  // tests 43-46

  describe("InfraConfig")
    describe("fromDict")
      it(...)  // tests 47-50

  describe("McpConfig")
    describe("fromDict")
      it(...)  // tests 51-54

  describe("ProjectConfig")
    describe("fromDict")
      it(...)  // tests 55-69

  describe("PipelineResult")
    it(...)  // tests 70-71

  describe("FileDiff")
    it(...)  // tests 72-73

  describe("VerificationResult")
    it(...)  // tests 74-75

  describe("mutable default independence")
    it(...)  // tests 76-80

  describe("snake_case key mapping")
    it(...)  // tests 81-84

  describe("empty object defaults")
    it(...)  // tests 85-91
```

---

## 9. Coverage Analysis

| Category | Test Count | Models Covered | Branch Points |
|----------|-----------|----------------|---------------|
| `requireField` helper | 8 | 1 function | key present / absent / falsy values |
| Leaf model `fromDict` (happy) | 14 | 10 models | All optional field present / absent |
| Leaf model `fromDict` (error) | 10 | 5 models (with required fields) | Missing each required field |
| Composite model `fromDict` | 12 | 3 models | Nested present / absent / partial |
| `ProjectConfig.fromDict` | 15 | 1 model | 5 required missing + interfaces parsing + nested types |
| Constructor-only models | 6 | 3 models | Constructor with full / empty data |
| Mutable default independence | 5 | 4 models | Array/object reference identity |
| Snake_case key mapping | 4 | 4 models | Key name mapping correctness |
| Empty object defaults | 7 | 7 models | All-optional models with `{}` |
| **Total** | **81** | **17 models + 1 helper** | |

### Branch Coverage Strategy

Every `fromDict` method has branches for:
- Required field present vs absent (covered by happy path + error tests)
- Optional field provided vs default (covered by full data + minimal data tests)
- Nested object provided vs empty `{}` fallback (covered by composite tests)

Estimated coverage: **>= 95% line, >= 90% branch** assuming all 81 tests pass.

---

## 10. Assertions Library

Use vitest built-in assertions:
- `expect(value).toBe(expected)` for primitives
- `expect(value).toEqual(expected)` for deep equality (arrays, objects)
- `expect(value).toBeInstanceOf(ClassName)` for type checks
- `expect(() => fn()).toThrow(Error)` for error cases
- `expect(() => fn()).toThrow(/pattern/)` for error message matching
- `expect(arr1).not.toBe(arr2)` for reference inequality (independence checks)

---

## 11. Run Commands

```bash
# Run model tests only
npx vitest run tests/node/models.test.ts

# Run with coverage
npx vitest run tests/node/models.test.ts --coverage

# Run in watch mode during development
npx vitest tests/node/models.test.ts
```
