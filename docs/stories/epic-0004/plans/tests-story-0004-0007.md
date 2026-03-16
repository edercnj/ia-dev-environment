# Test Plan — story-0004-0007: OpenAPI/Swagger Documentation Generator (REST)

## Summary

This story adds an OpenAPI generator template/prompt as a reference file under the `x-dev-lifecycle` skill at `resources/skills-templates/core/x-dev-lifecycle/references/openapi-generator.md`. The template instructs a subagent to scan REST inbound adapters and generate an OpenAPI 3.1 YAML specification. The assembler's existing `copyTemplateTree()` function recursively copies the entire skill directory tree including `references/`, so no assembler code changes are needed for the `.claude/` and `.agents/` outputs. The `.github/` output uses `GithubSkillsAssembler.renderSkill()`, which does NOT copy `references/` subdirectories -- so either the assembler needs enhancement or the GitHub copy is handled differently (see Risk 10.2 in the implementation plan).

Testing covers: (1) content validation of the template file, (2) dual copy consistency between `.claude/` and `.github/` versions, (3) golden file byte-for-byte parity for all 8 profiles, and (4) negative case verification that `python-click-cli` does NOT receive the template.

---

## 1. Test Strategy Overview

| Category | Scope | New Tests? | Test File |
|----------|-------|------------|-----------|
| Content validation (template) | Verify OpenAPI generator template contains required sections and keywords | YES | `tests/node/content/openapi-generator-content.test.ts` |
| Dual copy consistency (RULE-001) | Verify `.claude/` and `.github/` versions contain semantically equivalent content | YES | `tests/node/content/openapi-generator-content.test.ts` |
| Golden file integration | Pipeline output matches updated golden files byte-for-byte | NO (existing) | `tests/node/integration/byte-for-byte.test.ts` |
| Negative case (non-REST) | `python-click-cli` profile does NOT include `openapi-generator.md` | YES | `tests/node/content/openapi-generator-content.test.ts` |

---

## 2. Unit Tests -- Content Validation

### 2.1 File: `tests/node/content/openapi-generator-content.test.ts`

This new test file validates the OpenAPI generator template content. It follows the pattern established in `tests/node/content/refactoring-guidelines-content.test.ts` and `tests/node/content/x-story-create-content.test.ts`.

**Source file under test:** `resources/skills-templates/core/x-dev-lifecycle/references/openapi-generator.md`

#### 2.1.1 Template Existence and Structure

| # | ID | Test Name | What It Validates | TPP | Parallel | Depends On |
|---|-----|-----------|-------------------|-----|----------|------------|
| 1 | UT-1 | `templateExists_atExpectedPath_fileIsReadable` | Template file exists at `resources/skills-templates/core/x-dev-lifecycle/references/openapi-generator.md` and can be read | Degenerate | Yes | -- |
| 2 | UT-1b | `templateContent_isNonEmpty_hasSubstantialContent` | Template content is non-empty and has at least 50 lines (substantive document, not a stub) | Degenerate | Yes | -- |

#### 2.1.2 OpenAPI 3.1 Specification Requirements

| # | ID | Test Name | What It Validates | TPP | Parallel | Depends On |
|---|-----|-----------|-------------------|-----|----------|------------|
| 3 | UT-2 | `templateContent_containsOpenAPI31Requirement_specVersion` | Template contains "3.1" or "OpenAPI 3.1" to specify the target spec version | Unconditional | Yes | UT-1 |
| 4 | UT-2b | `templateContent_containsOpenAPIKeyword_identifiesSpec` | Template contains "OpenAPI" as a top-level concept | Unconditional | Yes | UT-1 |

#### 2.1.3 RFC 7807 Problem Details

| # | ID | Test Name | What It Validates | TPP | Parallel | Depends On |
|---|-----|-----------|-------------------|-----|----------|------------|
| 5 | UT-3 | `templateContent_containsRFC7807Reference_problemDetails` | Template references RFC 7807 or "Problem Details" for error response schemas | Condition | Yes | UT-1 |
| 6 | UT-3b | `templateContent_containsProblemDetailFields_typeStatusTitleDetail` | Template mentions the RFC 7807 fields: `type`, `title`, `status`, `detail` | Condition | Yes | UT-3 |

#### 2.1.4 Schema Deduplication ($ref)

| # | ID | Test Name | What It Validates | TPP | Parallel | Depends On |
|---|-----|-----------|-------------------|-----|----------|------------|
| 7 | UT-4 | `templateContent_containsRefInstruction_schemaDeduplication` | Template contains `$ref` instruction for schema deduplication (avoids inline schema repetition) | Condition | Yes | UT-1 |
| 8 | UT-4b | `templateContent_containsComponentsSchemas_centralSchemaRegistry` | Template mentions `components/schemas` or `components.schemas` as the central schema registry | Condition | Yes | UT-4 |

#### 2.1.5 Path/Endpoint Extraction

| # | ID | Test Name | What It Validates | TPP | Parallel | Depends On |
|---|-----|-----------|-------------------|-----|----------|------------|
| 9 | UT-5 | `templateContent_containsPathExtraction_endpointDiscovery` | Template contains instructions to extract paths/endpoints from REST adapters | Unconditional | Yes | UT-1 |
| 10 | UT-5b | `templateContent_containsPathsSection_openAPIPathsObject` | Template references the OpenAPI `paths` object or section | Unconditional | Yes | UT-5 |

#### 2.1.6 Framework-Specific Placeholders

| # | ID | Test Name | What It Validates | TPP | Parallel | Depends On |
|---|-----|-----------|-------------------|-----|----------|------------|
| 11 | UT-6a | `templateContent_containsFrameworkPlaceholder_doubleOrSingleBrace` | Template contains `{{FRAMEWORK}}` (runtime marker) or `{framework_name}` (build-time placeholder) | Condition | Yes | UT-1 |
| 12 | UT-6b | `templateContent_containsLanguagePlaceholder_doubleOrSingleBrace` | Template contains `{{LANGUAGE}}` (runtime marker) or `{language_name}` (build-time placeholder) | Condition | Yes | UT-1 |

#### 2.1.7 Output Path

| # | ID | Test Name | What It Validates | TPP | Parallel | Depends On |
|---|-----|-----------|-------------------|-----|----------|------------|
| 13 | UT-7 | `templateContent_containsOutputPath_docsApiOpenapiYaml` | Template specifies output path `docs/api/openapi.yaml` | Unconditional | Yes | UT-1 |

#### 2.1.8 HTTP Method Extraction

| # | ID | Test Name | What It Validates | TPP | Parallel | Depends On |
|---|-----|-----------|-------------------|-----|----------|------------|
| 14 | UT-8a | `templateContent_containsGETMethod_httpMethodExtraction` | Template mentions GET method | Unconditional | Yes | UT-1 |
| 15 | UT-8b | `templateContent_containsPOSTMethod_httpMethodExtraction` | Template mentions POST method | Unconditional | Yes | UT-1 |
| 16 | UT-8c | `templateContent_containsPUTMethod_httpMethodExtraction` | Template mentions PUT method | Unconditional | Yes | UT-1 |
| 17 | UT-8d | `templateContent_containsDELETEMethod_httpMethodExtraction` | Template mentions DELETE method | Unconditional | Yes | UT-1 |
| 18 | UT-8e | `templateContent_containsPATCHMethod_httpMethodExtraction` | Template mentions PATCH method | Unconditional | Yes | UT-1 |

#### 2.1.9 DTO/Schema Extraction

| # | ID | Test Name | What It Validates | TPP | Parallel | Depends On |
|---|-----|-----------|-------------------|-----|----------|------------|
| 19 | UT-9a | `templateContent_containsDTOExtraction_requestResponseSchemas` | Template contains instructions to extract DTOs or request/response schemas | Unconditional | Yes | UT-1 |
| 20 | UT-9b | `templateContent_containsSchemaKeyword_jsonSchemaOrOpenAPISchema` | Template references "schema" or "JSON Schema" in the context of DTO mapping | Unconditional | Yes | UT-1 |

#### 2.1.10 Error Response Handling

| # | ID | Test Name | What It Validates | TPP | Parallel | Depends On |
|---|-----|-----------|-------------------|-----|----------|------------|
| 21 | UT-10a | `templateContent_containsErrorResponseHandling_statusCodes` | Template contains instructions for error response status codes (e.g., 400, 404, 422, 500) | Condition | Yes | UT-1 |
| 22 | UT-10b | `templateContent_containsResponsesSection_allStatusCodes` | Template instructs documenting all status codes (success and error) in OpenAPI responses | Condition | Yes | UT-10a |

#### 2.1.11 Additional Content Requirements (from story acceptance criteria)

| # | ID | Test Name | What It Validates | TPP | Parallel | Depends On |
|---|-----|-----------|-------------------|-----|----------|------------|
| 23 | UT-11a | `templateContent_containsInfoSection_apiMetadata` | Template references the OpenAPI `info` section (title, version) | Unconditional | Yes | UT-1 |
| 24 | UT-11b | `templateContent_containsServersSection_environmentURLs` | Template references the OpenAPI `servers` section | Unconditional | Yes | UT-1 |
| 25 | UT-11c | `templateContent_containsTagsSection_endpointGrouping` | Template references tags for endpoint grouping | Unconditional | Yes | UT-1 |
| 26 | UT-11d | `templateContent_containsInboundAdapterScanning_controllerHandlerResource` | Template instructs scanning inbound REST adapters (controllers, handlers, resources) | Unconditional | Yes | UT-1 |
| 27 | UT-11e | `templateContent_containsYAMLFormat_outputFormat` | Template specifies YAML as the output format (not JSON) | Unconditional | Yes | UT-1 |

---

## 3. Dual Copy Consistency (RULE-001)

### 3.1 GitHub Copy Existence

Depending on the implementation approach chosen (see plan Risk 10.2), the GitHub copy may be:
- A reference file at `resources/github-skills-templates/dev/references/openapi-generator.md`, OR
- Inline content in `resources/github-skills-templates/dev/x-dev-lifecycle.md`, OR
- A separate skill file in a new documentation group

The tests below validate whichever approach is taken.

| # | ID | Test Name | What It Validates | TPP | Parallel | Depends On |
|---|-----|-----------|-------------------|-----|----------|------------|
| 28 | IT-4a | `githubCopy_exists_openAPIGeneratorContentAvailable` | A GitHub-side copy of the OpenAPI generator content exists (file or inline) | Degenerate | Yes | -- |
| 29 | IT-4b | `dualCopy_bothContainOpenAPI31Requirement` | Both `.claude/` and `.github/` versions contain OpenAPI 3.1 requirement | Unconditional | Yes | UT-1, IT-4a |
| 30 | IT-4c | `dualCopy_bothContainRFC7807Reference` | Both versions reference RFC 7807 / Problem Details | Unconditional | Yes | UT-1, IT-4a |
| 31 | IT-4d | `dualCopy_bothContainRefSchemaDeduplication` | Both versions contain `$ref` schema deduplication instructions | Unconditional | Yes | UT-1, IT-4a |
| 32 | IT-4e | `dualCopy_bothContainOutputPath` | Both versions specify `docs/api/openapi.yaml` output path | Unconditional | Yes | UT-1, IT-4a |
| 33 | IT-4f | `dualCopy_bothContainHTTPMethods` | Both versions mention GET, POST, PUT, DELETE, PATCH | Unconditional | Yes | UT-1, IT-4a |
| 34 | IT-4g | `dualCopy_bothContainInboundAdapterScanning` | Both versions instruct scanning inbound REST adapters | Unconditional | Yes | UT-1, IT-4a |
| 35 | IT-4h | `dualCopy_bothContainDTOExtraction` | Both versions contain DTO/schema extraction instructions | Unconditional | Yes | UT-1, IT-4a |
| 36 | IT-4i | `dualCopy_bothContainErrorResponseHandling` | Both versions contain error response handling instructions | Unconditional | Yes | UT-1, IT-4a |

---

## 4. Integration Tests -- Golden File / Pipeline

### 4.1 Existing Tests (No Changes to Test Code)

**File:** `tests/node/integration/byte-for-byte.test.ts`

The existing byte-for-byte parity tests validate pipeline output against golden files for all 8 profiles. After adding the new template file and updating golden files, these tests will verify:

| # | ID | Test Name (existing) | What It Validates | TPP | Parallel | Depends On |
|---|-----|-----------|-------------------|-----|----------|------------|
| 37 | IT-3a | `pipelineSuccessForProfile_${profile}` (x8) | Pipeline runs successfully for each profile | Degenerate | No (sequential per profile) | -- |
| 38 | IT-3b | `pipelineMatchesGoldenFiles_${profile}` (x8) | Output matches golden files byte-for-byte, including new `openapi-generator.md` for REST profiles | Unconditional | No (sequential per profile) | IT-3a |
| 39 | IT-3c | `noMissingFiles_${profile}` (x8) | No expected files are missing from pipeline output | Unconditional | No (sequential per profile) | IT-3a |
| 40 | IT-3d | `noExtraFiles_${profile}` (x8) | No unexpected files in pipeline output | Unconditional | No (sequential per profile) | IT-3a |

### 4.2 New Tests -- Pipeline Inclusion/Exclusion

| # | ID | Test Name | What It Validates | TPP | Parallel | Depends On |
|---|-----|-----------|-------------------|-----|----------|------------|
| 41 | IT-1 | `pipelineOutput_RESTProfiles_includeOpenapiGenerator` | Pipeline output for 7 REST profiles includes `openapi-generator.md` under `.claude/skills/x-dev-lifecycle/references/` | Unconditional | No (sequential, shares pipeline) | IT-3a |
| 42 | IT-2 | `pipelineOutput_pythonClickCli_excludesOpenapiGenerator` | Pipeline output for `python-click-cli` does NOT include `openapi-generator.md` anywhere | Edge case | No (sequential, shares pipeline) | IT-3a |
| 43 | IT-5 | `pipelineOutput_RESTProfiles_placeholdersResolved` | Template placeholders (`{project_name}`, `{framework_name}`, `{language_name}`) are replaced in generated output (no unresolved `{single_brace}` patterns remain) | Condition | No (sequential, shares pipeline) | IT-1 |

**Implementation note:** Tests IT-1, IT-2, and IT-5 can be added to a new test file `tests/node/integration/openapi-generator-pipeline.test.ts` or integrated into the existing `byte-for-byte.test.ts` as additional assertions per profile. Given the `describe.sequential.each` pattern in the existing test, a separate file is recommended to avoid coupling.

### 4.3 Golden File Inclusion Expectations

#### REST Profiles (7 profiles -- MUST include `openapi-generator.md`)

| Profile | `.claude/` golden path | `.agents/` golden path |
|---------|----------------------|----------------------|
| go-gin | `tests/golden/go-gin/.claude/skills/x-dev-lifecycle/references/openapi-generator.md` | `tests/golden/go-gin/.agents/skills/x-dev-lifecycle/references/openapi-generator.md` |
| java-quarkus | `tests/golden/java-quarkus/.claude/skills/x-dev-lifecycle/references/openapi-generator.md` | `tests/golden/java-quarkus/.agents/skills/x-dev-lifecycle/references/openapi-generator.md` |
| java-spring | `tests/golden/java-spring/.claude/skills/x-dev-lifecycle/references/openapi-generator.md` | `tests/golden/java-spring/.agents/skills/x-dev-lifecycle/references/openapi-generator.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.claude/skills/x-dev-lifecycle/references/openapi-generator.md` | `tests/golden/kotlin-ktor/.agents/skills/x-dev-lifecycle/references/openapi-generator.md` |
| python-fastapi | `tests/golden/python-fastapi/.claude/skills/x-dev-lifecycle/references/openapi-generator.md` | `tests/golden/python-fastapi/.agents/skills/x-dev-lifecycle/references/openapi-generator.md` |
| rust-axum | `tests/golden/rust-axum/.claude/skills/x-dev-lifecycle/references/openapi-generator.md` | `tests/golden/rust-axum/.agents/skills/x-dev-lifecycle/references/openapi-generator.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.claude/skills/x-dev-lifecycle/references/openapi-generator.md` | `tests/golden/typescript-nestjs/.agents/skills/x-dev-lifecycle/references/openapi-generator.md` |

**Note on `.github/` golden files:** The `GithubSkillsAssembler` currently does NOT copy `references/` subdirectories (confirmed by inspecting `x-story-epic-full` golden files where `.github/` lacks the `references/decomposition-guide.md`). If the implementation adds references support to `GithubSkillsAssembler`, then 7 additional `.github/` golden files will be needed. If the GitHub copy is handled differently (inline or separate skill), the golden files will reflect that approach.

#### Non-REST Profile (1 profile -- MUST NOT include `openapi-generator.md`)

| Profile | Expectation |
|---------|-------------|
| python-click-cli | `tests/golden/python-click-cli/.claude/skills/x-dev-lifecycle/references/openapi-generator.md` MUST NOT exist |

**Important consideration:** The current `copyTemplateTree()` function copies the entire `x-dev-lifecycle/` directory tree unconditionally for ALL profiles. The `openapi-generator.md` file will be copied to all 8 profiles unless conditional inclusion logic is added. If the template is unconditionally present (with runtime skip logic in the lifecycle phase), then `python-click-cli` WILL also receive the file, and IT-2 should validate that the file is present but the lifecycle SKILL.md documents runtime skipping. This is an open question (see plan Open Question #3). The test plan accounts for both scenarios:

- **Scenario A (conditional inclusion):** Tests IT-1 and IT-2 as described above.
- **Scenario B (unconditional inclusion with runtime skip):** IT-2 becomes `pipelineOutput_pythonClickCli_includesOpenapiGeneratorWithRuntimeSkipLogic`, validating the file IS present but the lifecycle phase documents conditional invocation.

---

## 5. Acceptance Tests

| # | ID | Test Name | What It Validates | TPP | Parallel | Depends On |
|---|-----|-----------|-------------------|-----|----------|------------|
| 44 | AT-1 | `acceptance_RESTProfiles_openapiGeneratorGenerated` | OpenAPI generator template is generated for all 7 REST profiles with correct content (combines IT-1 + UT-2 through UT-11) | Happy path | No | IT-1, UT-2 |
| 45 | AT-2 | `acceptance_nonRESTProfile_openapiGeneratorAbsentOrSkipped` | Non-REST profile (`python-click-cli`) either does not receive the template or the lifecycle documents runtime skipping | Edge case | No | IT-2 |

These acceptance tests are composite validations. AT-1 is satisfied when IT-1 passes (file present in output) AND the content tests (UT-2 through UT-11) pass. AT-2 is satisfied when IT-2 passes (file absent for non-REST, or file present with documented runtime skip).

---

## 6. Content Verification -- Key Sections That Must Appear

The following keywords and patterns MUST be present in the OpenAPI generator template. Content tests use `toContain()` for substring checks and `toMatch()` for regex patterns (not brittle exact line matching).

### 6.1 OpenAPI Specification Core

| Keyword/Pattern | Purpose | Test # |
|-----------------|---------|--------|
| `3.1` or `OpenAPI 3.1` | Target spec version | UT-2 |
| `openapi` | OpenAPI keyword | UT-2b |
| `info` | API metadata section | UT-11a |
| `servers` | Environment URLs | UT-11b |
| `paths` | Endpoint definitions | UT-5b |
| `tags` | Endpoint grouping | UT-11c |
| `components` + `schemas` | Schema registry | UT-4b |

### 6.2 REST Adapter Scanning

| Keyword/Pattern | Purpose | Test # |
|-----------------|---------|--------|
| `controller` or `handler` or `resource` or `adapter` | Inbound adapter types | UT-11d |
| `GET` | HTTP method | UT-8a |
| `POST` | HTTP method | UT-8b |
| `PUT` | HTTP method | UT-8c |
| `DELETE` | HTTP method | UT-8d |
| `PATCH` | HTTP method | UT-8e |

### 6.3 Schema and Error Handling

| Keyword/Pattern | Purpose | Test # |
|-----------------|---------|--------|
| `$ref` | Schema deduplication | UT-4 |
| `RFC 7807` or `Problem Details` | Error response standard | UT-3 |
| `type` + `title` + `status` + `detail` | RFC 7807 fields | UT-3b |
| `DTO` or `request` + `response` + `schema` | Schema extraction | UT-9a |
| `400` or `404` or `422` or `500` or `error.*status` | Error status codes | UT-10a |

### 6.4 Output and Placeholders

| Keyword/Pattern | Purpose | Test # |
|-----------------|---------|--------|
| `docs/api/openapi.yaml` | Output path | UT-7 |
| `YAML` | Output format | UT-11e |
| `{{FRAMEWORK}}` or `{framework_name}` | Framework placeholder | UT-6a |
| `{{LANGUAGE}}` or `{language_name}` | Language placeholder | UT-6b |

---

## 7. Golden Files Requiring Update

### 7.1 New Golden Files (14 files minimum)

7 REST profiles x 2 output directories (`.claude/`, `.agents/`) = 14 new golden files.

If `GithubSkillsAssembler` is enhanced to copy references: +7 files (`.github/`) = 21 total.

### 7.2 Golden File Update Strategy

```bash
# After creating the template and running the pipeline:
PROFILES=(go-gin java-quarkus java-spring kotlin-ktor python-fastapi rust-axum typescript-nestjs)

for profile in "${PROFILES[@]}"; do
  # Run pipeline for profile, copy output to golden directory
  # Or manually copy the reference file after pipeline run
done

# Verify python-click-cli golden dir does NOT contain the file
ls tests/golden/python-click-cli/.claude/skills/x-dev-lifecycle/references/
# Expected: directory should not exist (or should not contain openapi-generator.md)
```

### 7.3 Existing Golden Files (unchanged)

The `SKILL.md` golden files for `x-dev-lifecycle` are NOT modified by this story (the lifecycle SKILL.md modifications are deferred until story-0004-0005 is merged, per the implementation plan).

---

## 8. Suggested Test Implementation Pattern

```typescript
import { describe, it, expect } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

const TEMPLATE_PATH = path.resolve(
  __dirname, "../../..",
  "resources/skills-templates/core/x-dev-lifecycle/references/openapi-generator.md",
);

// GitHub copy path (adjust based on implementation approach)
const GITHUB_TEMPLATE_PATH = path.resolve(
  __dirname, "../../..",
  "resources/github-skills-templates/dev/references/openapi-generator.md",
);

describe("OpenAPI generator template — existence", () => {
  it("templateExists_atExpectedPath_fileIsReadable", () => {
    expect(fs.existsSync(TEMPLATE_PATH)).toBe(true);
  });

  it("templateContent_isNonEmpty_hasSubstantialContent", () => {
    const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");
    const lines = content.split("\n").length;
    expect(lines).toBeGreaterThanOrEqual(50);
  });
});

describe("OpenAPI generator template — OpenAPI 3.1 requirements", () => {
  const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");

  it("templateContent_containsOpenAPI31Requirement_specVersion", () => {
    expect(content).toMatch(/OpenAPI\s+3\.1|3\.1\.0/);
  });

  // ... additional content tests
});

describe("OpenAPI generator template — RFC 7807", () => {
  const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");

  it("templateContent_containsRFC7807Reference_problemDetails", () => {
    expect(content).toMatch(/RFC\s*7807|Problem Details/i);
  });

  it.each([
    ["type"], ["title"], ["status"], ["detail"],
  ])("templateContent_containsProblemDetailField_%s", (field) => {
    expect(content).toContain(field);
  });
});

describe("OpenAPI generator template — HTTP methods", () => {
  const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");

  it.each([
    ["GET"], ["POST"], ["PUT"], ["DELETE"], ["PATCH"],
  ])("templateContent_containsHTTPMethod_%s", (method) => {
    expect(content).toContain(method);
  });
});

describe("OpenAPI generator — dual copy consistency (RULE-001)", () => {
  const claudeContent = fs.readFileSync(TEMPLATE_PATH, "utf-8");
  const githubContent = fs.readFileSync(GITHUB_TEMPLATE_PATH, "utf-8");

  it.each([
    ["OpenAPI 3.1"],
    ["$ref"],
    ["docs/api/openapi.yaml"],
    ["RFC 7807"],
  ])("dualCopy_bothContain_%s", (keyword) => {
    expect(claudeContent).toContain(keyword);
    expect(githubContent).toContain(keyword);
  });
});
```

---

## 9. TDD Execution Order

Following test-first approach:

| Step | Action | Test State |
|------|--------|-----------|
| 1 | Write content validation tests (`tests/node/content/openapi-generator-content.test.ts`) with all UT-1 through UT-11 test cases | RED (template file does not exist yet) |
| 2 | Create Claude template (`resources/skills-templates/core/x-dev-lifecycle/references/openapi-generator.md`) | Partial GREEN (Claude content tests pass, dual copy tests still RED) |
| 3 | Create GitHub template (location depends on implementation approach) | GREEN (all content + consistency tests pass) |
| 4 | Write pipeline inclusion/exclusion tests (IT-1, IT-2, IT-5) | RED (golden files not yet updated) |
| 5 | Regenerate golden files for all 8 profiles via pipeline | GREEN (golden files updated, byte-for-byte passes) |
| 6 | Run full test suite (`npx vitest run`) | GREEN (all existing + new tests pass) |

---

## 10. Backward Compatibility Verification

| Verification | How Tested |
|--------------|-----------|
| Existing `x-dev-lifecycle/SKILL.md` content unchanged | Existing golden file byte-for-byte tests (SKILL.md golden files are not modified by this story) |
| No existing files removed or renamed | `noMissingFiles_${profile}` assertions in byte-for-byte test |
| `python-click-cli` profile unaffected | IT-2: negative case test |
| No assembler code changes (if template-only approach) | Existing assembler unit tests continue to pass |
| All 8 profiles still produce successful pipeline output | IT-3a: `pipelineSuccessForProfile_${profile}` |

---

## 11. Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| `copyTemplateTree()` copies unconditionally to all profiles | IT-2 validates whether conditional exclusion is needed. If unconditional, adjust IT-2 to validate runtime skip documentation instead |
| `GithubSkillsAssembler` does not copy references | Dual copy tests (IT-4a through IT-4i) will fail if GitHub copy is missing, surfacing the issue early. Implementation plan offers 3 options for resolution |
| Golden file mismatch after template addition | Mechanical regeneration via pipeline; byte-for-byte tests catch drift immediately |
| Template content too brittle in tests | Use `toContain()` for substring checks and `toMatch()` for regex patterns, not exact line matching |
| Placeholder conflicts (`{single}` vs `{{double}}` braces) | UT-6a/UT-6b validate placeholders exist; IT-5 validates `{single_brace}` patterns are resolved in output |
| Story-0004-0005 not yet implemented | Template created as standalone reference; lifecycle SKILL.md modifications deferred |

---

## 12. Files Summary

### 12.1 New Files (Template)

| # | File | Description |
|---|------|-------------|
| 1 | `resources/skills-templates/core/x-dev-lifecycle/references/openapi-generator.md` | OpenAPI generator template (Claude source of truth, RULE-002) |
| 2 | `resources/github-skills-templates/dev/references/openapi-generator.md` (or equivalent) | GitHub dual copy (RULE-001) |

### 12.2 New Test File

| File | Test Count |
|------|-----------|
| `tests/node/content/openapi-generator-content.test.ts` | 45 |

### 12.3 Golden Files Created (14-21 files)

7 REST profiles x 2-3 output directories = 14-21 new golden files (depending on GitHub references support).

### 12.4 Existing Test Files (unchanged, covering this story)

| File | Test Count | Coverage |
|------|-----------|----------|
| `tests/node/integration/byte-for-byte.test.ts` | 40 (8 profiles x 5 assertions) | Golden file parity |
| `tests/node/assembler/skills-assembler.test.ts` | ~20 | Claude copy mechanism |
| `tests/node/assembler/codex-skills-assembler.test.ts` | ~15 | Agents copy mechanism |
| `tests/node/assembler/github-skills-assembler.test.ts` | ~15 | GitHub copy mechanism |

---

## 13. Test Count Summary

| Category | New Tests | Existing Tests |
|----------|-----------|----------------|
| Content validation (Claude template) | 27 | 0 |
| Dual copy consistency (RULE-001) | 9 | 0 |
| Pipeline inclusion/exclusion | 3 | 0 |
| Acceptance (composite) | 2 | 0 |
| Golden file integration | 0 | 40 (8 profiles x 5 assertions) |
| Assembler unit tests | 0 | ~50 (across 3 assembler test files) |
| **Total** | **41** (+ 4 composite/acceptance) | **~90** |

---

## 14. Verification Checklist

- [ ] `npx vitest run tests/node/content/openapi-generator-content.test.ts` -- all content validation tests pass
- [ ] `npx vitest run tests/node/integration/byte-for-byte.test.ts` -- all 8 profiles pass (40 assertions)
- [ ] `npx vitest run` -- full suite passes
- [ ] Coverage remains >= 95% line, >= 90% branch
- [ ] No compiler/linter warnings introduced
- [ ] `python-click-cli` golden directory does NOT contain `openapi-generator.md` (or contains it with documented runtime skip)
- [ ] All 7 REST profile golden directories contain `openapi-generator.md` with resolved placeholders
