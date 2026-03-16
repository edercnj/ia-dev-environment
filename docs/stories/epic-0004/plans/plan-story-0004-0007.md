# Implementation Plan — story-0004-0007: OpenAPI/Swagger Documentation Generator (REST)

## Prerequisite Status

**story-0004-0005 (Documentation Phase) is NOT yet implemented.** The `x-dev-lifecycle` SKILL.md still uses 8 phases (0-7) with Phase 3 = "Parallel Review." This story's artifacts (the OpenAPI generator template/prompt) must be created assuming that story-0004-0005 will insert a Documentation Phase (Phase 3) with a dispatch mechanism that routes to interface-specific generators. The OpenAPI generator will be one such generator, invoked when the project identity's `interfaces` array contains an entry with `type: "rest"`.

---

## 1. Affected Layers and Components

This is a **CLI library project** that generates `.claude/` and `.github/` boilerplate. There is no hexagonal architecture with domain/adapter layers in the traditional sense. Instead, the relevant layers are:

| Layer | Role in This Story |
|-------|-------------------|
| **Resources (templates)** | New template files for the OpenAPI generator prompt/instructions |
| **Assembler (pipeline)** | No changes expected — the existing `SkillsAssembler` and `GithubSkillsAssembler` already copy/template skill files from `resources/` to output |
| **Domain (conditions/selection)** | No changes expected — `hasInterface(config, "rest")` already exists in `src/assembler/conditions.ts` |
| **Golden files (tests)** | Updates to golden files for profiles that include `rest` interface |

The key insight: this story adds **template content** (Markdown files that serve as prompts/instructions for the documentation phase subagent), not application logic. The dispatch mechanism that invokes these generators belongs to story-0004-0005.

---

## 2. New Files to Create

### 2.1 Claude Skills Template (Source of Truth — RULE-002)

**Path:** `resources/skills-templates/core/x-dev-lifecycle/references/openapi-generator.md`

This is the OpenAPI generator prompt/instructions that the documentation phase will reference when it dispatches the REST documentation generator. It lives as a reference file under the lifecycle skill, following the same pattern as existing references (e.g., `x-story-epic-full/references/decomposition-guide.md`).

Content scope:
- Instructions for the subagent to scan inbound REST adapters (controllers, resources, handlers)
- Extraction rules: paths, HTTP methods, request/response DTOs, status codes, error responses
- OpenAPI 3.1 YAML structure requirements (info, servers, tags, paths, components/schemas)
- RFC 7807 Problem Details for error responses
- `$ref` usage for schema deduplication
- Output path: `docs/api/openapi.yaml`
- Template placeholders: `{{PROJECT_NAME}}`, `{{FRAMEWORK}}`, `{{LANGUAGE}}`

### 2.2 GitHub Skills Template (Dual Copy — RULE-001)

**Path:** `resources/github-skills-templates/dev/references/openapi-generator.md`

OR, depending on how story-0004-0005 structures the documentation generators:

**Path:** `resources/github-skills-templates/documentation/openapi-generator.md`

This is the dual copy of the same generator content, adapted for GitHub Copilot context (e.g., referencing `.github/skills/` paths instead of `.claude/skills/` paths).

### 2.3 Golden File Updates

For every profile with `type: rest` in interfaces (7 of 8 profiles):
- `tests/golden/java-spring/.claude/skills/x-dev-lifecycle/references/openapi-generator.md`
- `tests/golden/java-quarkus/.claude/skills/x-dev-lifecycle/references/openapi-generator.md`
- `tests/golden/typescript-nestjs/.claude/skills/x-dev-lifecycle/references/openapi-generator.md`
- `tests/golden/rust-axum/.claude/skills/x-dev-lifecycle/references/openapi-generator.md`
- `tests/golden/go-gin/.claude/skills/x-dev-lifecycle/references/openapi-generator.md`
- `tests/golden/python-fastapi/.claude/skills/x-dev-lifecycle/references/openapi-generator.md`
- `tests/golden/kotlin-ktor/.claude/skills/x-dev-lifecycle/references/openapi-generator.md`

And corresponding `.github/skills/` golden files for each.

The `python-click-cli` profile does NOT have `rest` in interfaces, so it should NOT receive this file — validating the "skip non-REST" acceptance criterion.

---

## 3. Existing Files to Modify

### 3.1 Lifecycle SKILL.md Template (Conditional — depends on story-0004-0005)

**File:** `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`

The documentation phase (story-0004-0005) will add a Phase 3 — Documentation section. This story adds a reference to the OpenAPI generator within that phase's dispatch logic. If story-0004-0005 is not yet merged, this modification must be coordinated or deferred.

Specifically, the Documentation Phase section should include:
```
### REST API Documentation (if interfaces contain "rest")
Read `skills/x-dev-lifecycle/references/openapi-generator.md` for instructions.
Generate OpenAPI 3.1 spec at `docs/api/openapi.yaml`.
```

### 3.2 GitHub Lifecycle SKILL.md Template (Dual Copy)

**File:** `resources/github-skills-templates/dev/x-dev-lifecycle.md`

Same modification as 3.1, with `.github/skills/` path references.

### 3.3 Golden Files for x-dev-lifecycle SKILL.md

All 8 profiles' golden copies of the lifecycle SKILL.md need updating if the phase content changes.

---

## 4. Dependency Direction Validation

```
resources/skills-templates/  →  (copied by)  →  src/assembler/skills-assembler.ts
                                                    ↓
                                               output/.claude/skills/
```

- **No new dependencies introduced.** The OpenAPI generator is a pure Markdown template file.
- The `SkillsAssembler` already handles the `x-dev-lifecycle` directory via `copyTemplateTree()`, which recursively copies the entire tree including `references/` subdirectories. No code changes needed in the assembler.
- The `GithubSkillsAssembler` uses a flat `renderSkill()` approach for the `dev` group — it only copies the single `.md` file per skill. If the generator needs to be a separate file under the GitHub skill, the assembler may need enhancement (see Risk Assessment).

---

## 5. Integration Points

### 5.1 Template Engine

The OpenAPI generator template will use `{placeholder}` patterns that are resolved by `TemplateEngine.replacePlaceholders()`:
- `{project_name}` — service name for OpenAPI `info.title`
- `{framework_name}` — framework-specific patterns (e.g., JAX-RS annotations for Quarkus, Spring MVC for Spring)
- `{language_name}` — language-specific file patterns for scanning

Runtime markers (`{{PLACEHOLDER}}`) remain unresolved during generation and are filled by the AI agent at execution time.

### 5.2 SkillsAssembler (No Changes Expected)

The `copyTemplateTree()` function at `src/assembler/copy-helpers.ts:47` already recursively copies all files and replaces placeholders in `.md` files. Adding a `references/openapi-generator.md` file under `resources/skills-templates/core/x-dev-lifecycle/` will be automatically picked up.

### 5.3 GithubSkillsAssembler (Potential Change)

The `GithubSkillsAssembler` at `src/assembler/github-skills-assembler.ts` uses `renderSkill()` which copies a single `.md` file per skill name. It does NOT copy `references/` subdirectories. Options:
1. **Add references support** to `GithubSkillsAssembler.renderSkill()` to also copy reference files
2. **Embed the generator content inline** in the GitHub version of `x-dev-lifecycle.md`
3. **Create a separate skill** for the OpenAPI generator in the GitHub `documentation` group

Recommended: Option 1 (add references support) aligns with existing patterns in `SkillsAssembler`.

### 5.4 Byte-for-Byte Tests

The integration test at `tests/node/integration/byte-for-byte.test.ts` compares pipeline output against golden files. All golden files must be regenerated after adding the new template.

---

## 6. Database Changes

None. This is a CLI library project with no database.

---

## 7. API Changes

None. This project exposes a CLI interface, not a REST/gRPC API.

---

## 8. Event Changes

None. This project is not event-driven.

---

## 9. Configuration Changes

### 9.1 No Config Schema Changes

The `interfaces` field in `ProjectConfig` already supports `type: "rest"`. The `InterfaceConfig` model at `src/models.ts:95-119` already has `type`, `spec`, and `broker` fields. No schema changes needed.

### 9.2 Setup Config Templates

The following config templates already declare `type: rest`:
- `resources/config-templates/setup-config.java-spring.yaml`
- `resources/config-templates/setup-config.java-quarkus.yaml`
- `resources/config-templates/setup-config.typescript-nestjs.yaml`
- `resources/config-templates/setup-config.rust-axum.yaml`
- `resources/config-templates/setup-config.go-gin.yaml`
- `resources/config-templates/setup-config.python-fastapi.yaml`
- `resources/config-templates/setup-config.kotlin-ktor.yaml`

The `python-click-cli` template does NOT have `rest`, which provides the negative test case.

---

## 10. Risk Assessment

### 10.1 Dependency Risk: story-0004-0005 Not Implemented (HIGH)

The documentation phase dispatch mechanism does not exist yet. This story creates the OpenAPI generator **template/prompt**, but the mechanism to invoke it (story-0004-0005) is a prerequisite.

**Mitigation:** Implement the generator as a standalone reference document that can be integrated later. The template file (`references/openapi-generator.md`) can be created and tested independently. The lifecycle SKILL.md modifications should be deferred until story-0004-0005 is merged, or implemented conditionally.

### 10.2 GithubSkillsAssembler References Support (MEDIUM)

The `GithubSkillsAssembler` currently copies only a single `.md` file per skill and does not support `references/` subdirectories. If we add a `references/openapi-generator.md` for the `.claude/` copy, we need equivalent handling for `.github/`.

**Mitigation:** Extend `GithubSkillsAssembler.renderSkill()` to check for and copy companion reference files. This is a small, contained change. Alternatively, inline the OpenAPI generator content in the GitHub lifecycle template.

### 10.3 Golden File Explosion (LOW)

Adding a new file to 7 of 8 golden file profiles increases maintenance surface. Each profile gets approximately 1 new file for `.claude/` and 1 for `.github/`.

**Mitigation:** The golden file update is mechanical (run pipeline, copy output). The byte-for-byte test infrastructure handles this well.

### 10.4 Template Placeholder Conflicts (LOW)

The OpenAPI generator template will contain both `{placeholder}` (resolved at generation time) and `{{PLACEHOLDER}}` (runtime markers). The `replacePlaceholders()` function only replaces known keys and preserves unknown ones, so `{{DOUBLE_BRACE}}` patterns pass through safely.

**Mitigation:** Ensure all framework-specific and language-specific references use the standard `{key}` patterns defined in `buildDefaultContext()` at `src/template-engine.ts:26-55`.

### 10.5 Backward Compatibility (LOW)

Adding new files does not remove or modify existing behavior. Projects without REST interfaces will not receive the OpenAPI generator. The `python-click-cli` profile serves as the negative control.

**Mitigation:** Golden file tests for all 8 profiles ensure backward compatibility. The `python-click-cli` golden file must NOT contain the OpenAPI generator reference.

---

## Implementation Order

1. **Create the OpenAPI generator template** (`resources/skills-templates/core/x-dev-lifecycle/references/openapi-generator.md`)
2. **Create the GitHub dual copy** (method depends on whether `GithubSkillsAssembler` gets references support)
3. **If story-0004-0005 is merged:** Update lifecycle SKILL.md templates to reference the generator
4. **If story-0004-0005 is NOT merged:** Create the generator files as standalone; defer lifecycle integration
5. **Update golden files** for all 8 profiles
6. **Write tests:** Unit tests for content structure, integration golden file tests
7. **Verify backward compatibility:** Ensure `python-click-cli` profile does NOT include the generator

---

## Open Questions

1. **Story-0004-0005 coordination:** Should this story wait for 0005 to merge, or create the generator template independently and integrate later?
2. **Generator location for GitHub:** Should the GitHub copy be a reference file under `x-dev-lifecycle`, or a separate skill in a new `documentation` group?
3. **Conditional inclusion:** Should the OpenAPI generator reference only be included in profiles with `rest` interface, or should it always be included (with runtime skip logic)? Current analysis suggests it should always be included since the lifecycle template is the same across all profiles and the dispatch decision happens at runtime.
