# Test Plan — STORY-007: Domain Validator, Resolver & Skill Registry

## Summary
- Total test classes: 3
- Total test methods: ~75 (estimated)
- Categories covered: Unit, Contract (parametrized)
- Estimated line coverage: ~97%
- Estimated branch coverage: ~93%

---

## Test Class 1: `validator.test.ts`

### Happy Path

| # | Method | Test Name | Description |
|---|--------|-----------|-------------|
| 1 | validateStack | validConfig_returnsEmptyErrors | Spring Boot + Java 21 returns no errors |
| 2 | validateStack | allValidFrameworkLanguageCombos_returnNoErrors | Parametrized: 15 valid combos |

### Error Path

| # | Exception | Test Name | Trigger |
|---|-----------|-----------|---------|
| 3 | framework-language | invalidFrameworkLanguage_returnsError | Parametrized: quarkus+python, django+java, etc. (10 combos) |
| 4 | java-version | java11WithSpringBoot3_returnsVersionError | Java 11 + Spring Boot 3.x |
| 5 | java-version | java11WithQuarkus3_returnsVersionError | Java 11 + Quarkus 3.x |
| 6 | python-version | python38WithDjango5_returnsVersionError | Python 3.8 + Django 5.x |
| 7 | native-build | nativeBuildUnsupportedFramework_returnsError | Gin + native_build=true |
| 8 | interface-type | invalidInterfaceType_returnsError | "soap" interface type |
| 9 | architecture | invalidArchitectureStyle_returnsError | "hexagonal" architecture |
| 10 | multiple | multipleViolations_returnsAllErrors | Config with 3+ violations |

### Boundary

| # | Boundary | Test Name | Values Tested |
|---|----------|-----------|---------------|
| 11 | java-version | java17ExactlyWithSpringBoot3_noError | Java 17 (boundary min) |
| 12 | java-version | java16WithSpringBoot3_returnsError | Java 16 (below min) |
| 13 | python-version | python310ExactlyWithDjango5_noError | Python 3.10 (boundary min) |
| 14 | python-version | python39WithDjango5_returnsError | Python 3.9 (below min) |
| 15 | framework-version | springBoot2xWithJava11_noError | Spring Boot 2.x skips Java 17 check |
| 16 | framework-version | django4xWithPython38_noError | Django 4.x skips Python 3.10 check |

### Parametrized — Version Parsing

| # | Matrix | Test Name | Source | Rows |
|---|--------|-----------|--------|------|
| 17 | extractMajor | validVersions_returnsCorrectMajor | inline | 5 ("21", "3.10", "17.0.2") |
| 18 | extractMajor | emptyOrInvalid_returnsUndefined | inline | 4 ("", "latest", "abc", undefined) |
| 19 | extractMinor | validVersions_returnsCorrectMinor | inline | 4 ("3.10", "17.0.2", "5.1") |
| 20 | extractMinor | noMinorOrInvalid_returnsUndefined | inline | 4 ("21", "", "abc.def", undefined) |

### Contract — Framework-Language Compatibility

| # | Matrix | Test Name | Source | Rows |
|---|--------|-----------|--------|------|
| 21 | valid-combos | frameworkLanguagePair_isValid | it.each | 15 rows (all FRAMEWORK_LANGUAGE_RULES entries) |
| 22 | invalid-combos | frameworkLanguagePair_isInvalid | it.each | 10 rows (from Python tests) |

### Cross-References (filesystem)

| # | Method | Test Name | Description |
|---|--------|-----------|-------------|
| 23 | verifyCrossReferences | validDirectory_returnsEmpty | All expected dirs exist |
| 24 | verifyCrossReferences | missingSourceDir_returnsError | Source dir doesn't exist |
| 25 | verifyCrossReferences | missingSubdirectory_returnsError | Expected subdir missing |

**Estimated: ~30 test methods**

---

## Test Class 2: `resolver.test.ts`

### Happy Path

| # | Method | Test Name | Description |
|---|--------|-----------|-------------|
| 1 | resolveStack | javaMavenConfig_returnsCompleteStack | Full resolution for java-maven |
| 2 | resolveStack | pythonPipConfig_returnsCompleteStack | Full resolution for python-pip |
| 3 | resolveStack | typescriptNpmConfig_returnsCompleteStack | Full resolution for ts-npm |

### Parametrized — Command Resolution

| # | Matrix | Test Name | Source | Rows |
|---|--------|-----------|--------|------|
| 4 | language-commands | languageBuildTool_returnsCorrectCommands | it.each | 8 rows (java-maven, java-gradle, kotlin-gradle, ts-npm, python-pip, go-go, rust-cargo, csharp-dotnet) |
| 5 | unknown-key | unknownLanguageBuildTool_returnsEmptyCommands | inline | 1 |

### Parametrized — Port Resolution

| # | Matrix | Test Name | Source | Rows |
|---|--------|-----------|--------|------|
| 6 | framework-ports | framework_returnsCorrectPort | it.each | 11 rows (all FRAMEWORK_PORTS entries) |
| 7 | unknown-fw | unknownFramework_returnsFallbackPort | inline | 1 |

### Parametrized — Health Path Resolution

| # | Matrix | Test Name | Source | Rows |
|---|--------|-----------|--------|------|
| 8 | framework-health | framework_returnsCorrectHealthPath | it.each | 11 rows |
| 9 | unknown-fw | unknownFramework_returnsFallbackHealthPath | inline | 1 |

### Parametrized — Docker Image Resolution

| # | Matrix | Test Name | Source | Rows |
|---|--------|-----------|--------|------|
| 10 | docker-images | language_returnsCorrectDockerImage | it.each | 7 rows (each language) |
| 11 | unknown-lang | unknownLanguage_returnsDefaultImage | inline | 1 |
| 12 | format-error | templateFormatError_returnsDefaultImage | inline | 1 |

### Parametrized — Project Type Derivation

| # | Matrix | Test Name | Source | Rows |
|---|--------|-----------|--------|------|
| 13 | project-types | architectureAndInterfaces_returnsCorrectType | it.each | 8 rows |

- microservice + rest → "api"
- microservice + event-consumer only → "worker"
- microservice + rest + event-consumer → "api"
- library + cli → "cli"
- library + no interfaces → "library"
- monolith → "api"
- modular-monolith → "api"
- serverless → "api"

### Parametrized — Protocol Derivation

| # | Matrix | Test Name | Source | Rows |
|---|--------|-----------|--------|------|
| 14 | protocols | interfaceType_returnsCorrectProtocol | it.each | 6 rows (rest→openapi, grpc→proto3, graphql→graphql, websocket→websocket, tcp→tcp, event-consumer→async-api) |
| 15 | no-protocol | cliOrScheduled_returnsNoProtocol | inline | 2 |

### Native Build Inference

| # | Method | Test Name | Description |
|---|--------|-----------|-------------|
| 16 | resolveStack | nativeBuildTrue_supportedFramework_returnsTrue | quarkus + native=true |
| 17 | resolveStack | nativeBuildTrue_unsupportedFramework_returnsFalse | gin + native=true |
| 18 | resolveStack | nativeBuildFalse_returnsFalse | Any framework + native=false |

### Readonly Validation

| # | Method | Test Name | Description |
|---|--------|-----------|-------------|
| 19 | resolveStack | returnedStack_isReadonly | Verify frozen/readonly result |

**Estimated: ~30 test methods**

---

## Test Class 3: `skill-registry.test.ts`

### Happy Path

| # | Method | Test Name | Description |
|---|--------|-----------|-------------|
| 1 | CORE_KNOWLEDGE_PACKS | hasExactly11Packs | Array length is 11 |
| 2 | CORE_KNOWLEDGE_PACKS | containsAllExpectedPacks | Each of the 11 specific names present |
| 3 | CORE_KNOWLEDGE_PACKS | isReadonly | Cannot be modified |

### Parametrized — buildInfraPackRules

| # | Matrix | Test Name | Source | Rows |
|---|--------|-----------|--------|------|
| 4 | k8s-rules | orchestratorKubernetes_includesK8sDeployment | inline | 1 |
| 5 | k8s-rules | templatingKustomize_includesK8sKustomize | inline | 1 |
| 6 | k8s-rules | templatingHelm_includesK8sHelm | inline | 1 |
| 7 | container-rules | containerNotNone_includesDockerfile | inline | 1 |
| 8 | registry-rules | registryNotNone_includesContainerRegistry | inline | 1 |
| 9 | iac-rules | iacTerraform_includesTerraform | inline | 1 |
| 10 | iac-rules | iacCrossplane_includesCrossplane | inline | 1 |

### Error Path (negative)

| # | Method | Test Name | Description |
|---|--------|-----------|-------------|
| 11 | buildInfraPackRules | allNone_returnsEmptyActiveRules | orchestrator=none, container=none, etc. |
| 12 | buildInfraPackRules | orchestratorNotK8s_excludesK8sRules | orchestrator="ecs" |
| 13 | buildInfraPackRules | fullInfraConfig_returnsAllActiveRules | All conditions true |

**Estimated: ~15 test methods**

---

## Coverage Estimation

| Class | Public Methods | Branches | Est. Tests | Line % | Branch % |
|-------|---------------|----------|-----------|--------|----------|
| validator.ts | 4 (validateStack, extractMajor, extractMinor, verifyCrossReferences) | ~33 | ~30 | ~97% | ~93% |
| resolver.ts | 1 (resolveStack) | ~20 | ~30 | ~98% | ~95% |
| skill-registry.ts | 1 (buildInfraPackRules) + 1 constant | ~8 | ~15 | ~99% | ~95% |
| **Total** | **7** | **~61** | **~75** | **~97%** | **~93%** |

## Quality Checks

- [x] Every acceptance criterion maps to >= 1 test (AC1-AC6 all covered)
- [x] Every error message path has >= 1 test
- [x] Unit + Contract categories represented
- [x] Boundary values use min/below-min pattern (Java 17/16, Python 3.10/3.9)
- [x] Parametrized matrices are complete (all FRAMEWORK_LANGUAGE_RULES entries)
- [x] Estimated coverage meets thresholds (97% line, 93% branch)
- [x] Test naming follows `[method]_[scenario]_[expected]` convention

## Risks and Gaps

- **Cross-reference tests (verifyCrossReferences)** require temp directory setup — may need `fs.mkdirSync`/`fs.mkdtempSync` in tests
- **Docker image template format errors** — edge case where template has unexpected placeholders; needs explicit test
- **Unknown architecture style** in resolver's `_derive_project_type` defaults to "api" — needs explicit test for unknown/fallback case
- **Version parsing edge cases** — very long version strings, versions with pre-release suffixes ("21-ea", "3.10.0rc1") should be tested
