# Task Breakdown — story-0004-0007: OpenAPI/Swagger Documentation Generator (REST)

## Summary

Six tasks decomposed from the implementation plan. The OpenAPI generator template is a standalone Markdown reference file under `x-dev-lifecycle/references/`. The `.claude/` pipeline requires no code changes (`copyTemplateTree()` handles `references/` recursively). The `.github/` pipeline requires extending `GithubSkillsAssembler` to copy companion reference files. The `.agents/` pipeline (`CodexSkillsAssembler`) already handles `references/` subdirectories and needs no changes.

Story-0004-0005 (Documentation Phase) is NOT on main yet. The generator template is created standalone — no lifecycle SKILL.md modifications.

---

## TASK-1: Create the OpenAPI Generator Template (Source of Truth)

**Description:** Create the OpenAPI generator prompt/instructions as a Markdown reference file under the x-dev-lifecycle skill. This is the source of truth per RULE-002. The file instructs a subagent to scan inbound REST adapters and produce an OpenAPI 3.1 YAML spec. It uses `{placeholder}` for generation-time substitution (project_name, framework_name, language_name) and `{{PLACEHOLDER}}` for runtime markers that the AI agent fills at execution time.

**Files to Create:**
- `resources/skills-templates/core/x-dev-lifecycle/references/openapi-generator.md`

**Files to Modify:** None

**Depends On:** None (independent)

**Parallel:** Yes (can start immediately)

**Content Requirements:**
- Title: `# OpenAPI/Swagger Documentation Generator (REST)`
- Section: When to invoke (interfaces contain "rest")
- Section: Scan instructions (inbound REST adapters — controllers, resources, handlers)
- Section: Extraction rules (paths, HTTP methods, request/response DTOs, status codes)
- Section: OpenAPI 3.1 YAML structure (info, servers, tags, paths, components/schemas)
- Section: RFC 7807 Problem Details for error responses
- Section: `$ref` schema deduplication rules
- Section: Output path (`docs/api/openapi.yaml`)
- Placeholders: `{project_name}`, `{framework_name}`, `{language_name}` (resolved at generation)
- Runtime markers: `{{PROJECT_NAME}}`, `{{FRAMEWORK}}` (preserved for AI agent)

**TDD Steps:**

- **RED:** Write content tests (TASK-3) first that assert required sections, keywords, and structural invariants in the template file. Tests fail because the file does not exist.
- **GREEN:** Create `resources/skills-templates/core/x-dev-lifecycle/references/openapi-generator.md` with all required sections and content. Content tests pass.
- **REFACTOR:** Review template prose for clarity, remove redundancy, ensure placeholder patterns are consistent with `buildDefaultContext()` in `src/template-engine.ts`.

---

## TASK-2: Handle GitHub Dual Copy Mechanism (RULE-001)

**Description:** The `GithubSkillsAssembler.renderSkill()` currently copies only a single `{name}.md` file per skill and writes it as `skills/{name}/SKILL.md`. It does NOT copy companion `references/` subdirectories. This is confirmed by the golden files: `.github/skills/x-story-epic-full/` contains only `SKILL.md` — no `references/` directory, while `.claude/skills/x-story-epic-full/` has `references/decomposition-guide.md`.

Two sub-tasks:

**TASK-2A: Extend GithubSkillsAssembler to copy reference files**

Modify `renderSkill()` to check if a companion `references/` directory exists in the source template directory and copy its contents to the output. This requires changing the source lookup from flat `{name}.md` to also check for `references/{name}/` or a shared `references/` directory alongside the skill `.md` file.

**Approach:** Add a `copyReferences()` private method to `GithubSkillsAssembler` that, after rendering the SKILL.md, checks for `{srcDir}/references/{skillName}/` and copies matching reference files with placeholder replacement to `{outputDir}/skills/{skillName}/references/`.

Alternative simpler approach: Since GitHub skills templates are flat `.md` files (not directory trees), create a dedicated `references/` directory under the group source directory (e.g., `resources/github-skills-templates/dev/references/x-dev-lifecycle/openapi-generator.md`). The assembler detects this structure and copies it.

**TASK-2B: Create the GitHub dual copy of the template**

Create the corresponding template file for GitHub output. This file has the same functional content as the Claude version but references `.github/skills/` paths instead of `.claude/skills/` paths (per RULE-001).

**Files to Create:**
- `resources/github-skills-templates/dev/references/x-dev-lifecycle/openapi-generator.md`

**Files to Modify:**
- `src/assembler/github-skills-assembler.ts` — add `copyReferences()` to `renderSkill()` flow

**Depends On:** TASK-1 (need source of truth content to create dual copy)

**Parallel:** TASK-2A (code change) can start in parallel with TASK-1; TASK-2B (content) requires TASK-1

**TDD Steps:**

- **RED (TASK-2A):** Write unit tests in `tests/node/assembler/github-skills-assembler.test.ts`:
  1. `renderSkill_withReferencesDir_copiesReferencesToOutput` — create a skill template with a companion `references/{name}/` directory containing a `.md` file; assert the reference file appears under `skills/{name}/references/` in output.
  2. `renderSkill_withoutReferencesDir_behaviorUnchanged` — existing skills without references still produce only `SKILL.md`.
  3. `renderSkill_referencesContent_placeholdersReplaced` — verify `{project_name}` in reference files is replaced.
  Tests fail because `renderSkill()` does not copy references.
- **GREEN (TASK-2A):** Add `copyReferences()` method to `GithubSkillsAssembler`. After writing `SKILL.md`, check if `{srcDir}/references/{name}/` exists. If so, copy all `.md` files with placeholder replacement to `{outputDir}/skills/{subDir?}/{name}/references/`.
- **GREEN (TASK-2B):** Create `resources/github-skills-templates/dev/references/x-dev-lifecycle/openapi-generator.md` with GitHub-adapted content.
- **REFACTOR:** Extract shared copy logic if it duplicates `copyTemplateTree` patterns. Ensure the method handles nested groups (lib) correctly.

---

## TASK-3: Write Content Tests for the OpenAPI Generator Template

**Description:** Create a content test file following the established pattern in `tests/node/content/`. The tests validate structural invariants of the OpenAPI generator template for both the Claude and GitHub copies, plus dual-copy consistency.

**Files to Create:**
- `tests/node/content/openapi-generator-content.test.ts`

**Files to Modify:** None

**Depends On:** None (tests are written first per TDD — they will fail until TASK-1 and TASK-2B are complete)

**Parallel:** Yes (should be written FIRST, before TASK-1)

**Test Cases (following `x-story-create-content.test.ts` pattern):**

```
describe("openapi-generator content validation")
  describe("Claude source template")
    containsOpenAPIVersion_specSection_references3dot1
    containsRFC7807Section_errorResponses_requiresProblemDetails
    containsSchemaDeduplication_componentsSection_usesRefPattern
    containsOutputPath_outputSection_specifiesDocsApiOpenapi
    containsScanInstructions_adapterSection_referencesInboundREST
    containsInfoSection_yamlStructure_hasInfoTitleAndVersion
    containsServersSection_yamlStructure_hasServersArray
    containsPathsSection_yamlStructure_hasPathsObject
    containsComponentsSection_yamlStructure_hasComponentsSchemas
    containsHTTPMethods_extractionRules_listsGETPOSTPUTDELETE
    containsStatusCodes_responseSection_lists200_201_400_404_422
    containsPlaceholder_projectName_usesGenerationTimePlaceholder
    containsFrameworkReference_scanPatterns_referencesFramework
  describe("GitHub source template")
    (mirrors Claude tests above for the GitHub copy)
  describe("dual copy consistency (RULE-001)")
    bothContainOpenAPIVersion_sameSpec_3dot1
    bothContainRFC7807_sameErrorPattern_problemDetails
    bothContainSchemaDeduplication_sameRefPattern
    bothContainOutputPath_samePath_docsApiOpenapi
```

**TDD Steps:**

- **RED:** Write all test cases. They fail because template files do not exist yet.
- **GREEN:** Tests pass once TASK-1 and TASK-2B create the template files.
- **REFACTOR:** Consolidate repetitive assertions using `it.each` where patterns repeat across Claude/GitHub.

---

## TASK-4: Update Golden Files for All 8 Profiles

**Description:** Regenerate golden files by running the full pipeline for each of the 8 profiles and copying the output to `tests/golden/{profile}/`. The new `references/openapi-generator.md` file will appear under `.claude/skills/x-dev-lifecycle/references/` and `.agents/skills/x-dev-lifecycle/references/` for all 8 profiles (the reference is part of a core skill copied unconditionally). For `.github/skills/x-dev-lifecycle/references/` it appears for all 8 profiles after TASK-2A extends the assembler.

**Why all 8 profiles (not just 7 REST ones):** The `openapi-generator.md` reference file lives under the `x-dev-lifecycle` core skill. Core skills are copied for ALL profiles unconditionally — the `SkillsAssembler.assembleCore()` method does not filter by interface type. The file is always present; the *runtime dispatch* decision (whether to invoke it) happens at AI agent execution time based on project interfaces. This matches the `decomposition-guide.md` precedent, which also appears in all 8 golden profiles including `python-click-cli`.

**Files to Create/Update (per profile, 8 profiles x 3 output targets = 24 new files):**
- `tests/golden/{profile}/.claude/skills/x-dev-lifecycle/references/openapi-generator.md` (8 files)
- `tests/golden/{profile}/.agents/skills/x-dev-lifecycle/references/openapi-generator.md` (8 files)
- `tests/golden/{profile}/.github/skills/x-dev-lifecycle/references/openapi-generator.md` (8 files — requires TASK-2A)

**Depends On:** TASK-1 (Claude template), TASK-2 (GitHub assembler + template)

**Parallel:** No (requires TASK-1 and TASK-2 to be complete)

**Procedure:**
1. Run `npx tsx src/index.ts generate --config resources/config-templates/setup-config.{profile}.yaml --output /tmp/{profile}` for each profile
2. Copy new `references/openapi-generator.md` files from output to golden directories
3. Verify byte-for-byte tests pass: `npx vitest run tests/node/integration/byte-for-byte.test.ts`

**TDD Steps:**

- **RED:** Run existing `byte-for-byte.test.ts` — it reports `EXTRA` files in pipeline output (the new reference files exist in output but not in golden).
- **GREEN:** Copy pipeline output to golden directories. Tests pass.
- **REFACTOR:** N/A (golden files are generated artifacts, not hand-written).

---

## TASK-5: Write/Update Integration Tests

**Description:** The existing `byte-for-byte.test.ts` already covers full pipeline parity validation. After TASK-4 updates the golden files, those tests will pass automatically for all 8 profiles. No new integration test file is needed — the existing infrastructure is sufficient.

However, add explicit assertions for the OpenAPI generator reference file in the `GithubSkillsAssembler` unit tests (TASK-2A) and verify the file count increases by the expected amount.

**Files to Modify:**
- `tests/node/assembler/github-skills-assembler.test.ts` — new tests from TASK-2A
- `tests/node/assembler/skills-assembler.test.ts` — optional: add a test confirming `references/` subdirectories are included in `copyCoreSkill()` output

**Depends On:** TASK-2A (GitHub assembler changes), TASK-4 (golden files)

**Parallel:** Partially — TASK-2A tests can be written first (RED phase), but full green requires TASK-4

**TDD Steps:**

- **RED:** Run `byte-for-byte.test.ts` — fails with extra/missing files.
- **GREEN:** After TASK-4 golden file updates, all integration tests pass.
- **REFACTOR:** Verify no test execution order dependencies exist.

---

## TASK-6: Verify Backward Compatibility (python-click-cli Negative Case)

**Description:** Verify that the `python-click-cli` profile, which defines `interfaces: [{type: "cli"}]` (no `rest`), still receives the OpenAPI generator reference file in its output. This confirms the file is part of the core skill tree (unconditional copy), not interface-filtered. The *runtime* skip behavior (AI agent decides not to invoke the generator for non-REST projects) is not testable at the pipeline level — it is a property of the template content, not the assembler logic.

This task validates:
1. The `python-click-cli` golden files include `x-dev-lifecycle/references/openapi-generator.md` (same as other profiles)
2. The pipeline output for `python-click-cli` succeeds without errors or warnings
3. No existing files are removed or modified (backward compatibility)
4. The template content itself contains the conditional instruction "invoke only when interfaces contain rest" — verified by TASK-3 content tests

**Files to Modify:** None (verification only)

**Depends On:** TASK-4 (golden files must be updated first)

**Parallel:** No (runs after TASK-4)

**TDD Steps:**

- **RED:** N/A (this is a verification task, not a code change)
- **GREEN:** Run `byte-for-byte.test.ts` for `python-click-cli` profile — passes with the generator reference file present.
- **REFACTOR:** N/A

---

## Dependency Graph

```
TASK-3 (content tests — RED)
   |
   v
TASK-1 (create template) ──────┐
   |                            |
   v                            v
TASK-2A (extend GH assembler)  TASK-2B (GH dual copy)
   |                            |
   └────────────┬───────────────┘
                v
            TASK-4 (golden files)
                |
                v
            TASK-5 (integration tests — GREEN)
                |
                v
            TASK-6 (backward compat verification)
```

## Parallelization Summary

| Task | Can Run In Parallel With | Must Wait For |
|------|--------------------------|---------------|
| TASK-3 | TASK-2A (code skeleton) | Nothing |
| TASK-1 | TASK-3 (but logically TASK-3 RED goes first) | Nothing |
| TASK-2A | TASK-1, TASK-3 | Nothing |
| TASK-2B | TASK-2A | TASK-1 (content source) |
| TASK-4 | — | TASK-1, TASK-2A, TASK-2B |
| TASK-5 | — | TASK-4 |
| TASK-6 | — | TASK-4 |

## Estimated File Changes

| Category | New Files | Modified Files |
|----------|-----------|----------------|
| Templates | 2 | 0 |
| Source code | 0 | 1 (`github-skills-assembler.ts`) |
| Tests | 1 | 1 (`github-skills-assembler.test.ts`) |
| Golden files | 24 | 0 |
| **Total** | **27** | **2** |
