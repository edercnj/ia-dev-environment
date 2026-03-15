# Tech Lead Review -- STORY-021: Codex Nunjucks Templates

**Date:** 2026-03-11
**Reviewer:** Tech Lead (automated review)
**Scope:** 13 Nunjucks templates + 1 test file + 1 fixture file + 1 snapshot file

---

```
============================================================
 TECH LEAD REVIEW -- STORY-021
============================================================
 Decision:  CONDITIONAL GO
 Score:     36/40
 Critical:  0 issues
 Medium:    2 issues
 Low:       3 issues
------------------------------------------------------------
```

---

## A. Code Hygiene (7/8)

- **A.1 Unused imports (0.5):** `aProjectConfig` is imported on line 10 of `codex-templates.test.ts` but never used anywhere in the file. Only `aMinimalProjectConfig` and `aFullProjectConfig` are called. **(-0.5)**
- **A.2 Dead code:** No dead code found in templates or test file.
- **A.3 Warnings:** Zero TypeScript compilation warnings (`npx tsc --noEmit` clean).
- **A.4 Method signatures:** All helper functions (`fullContext`, `minimalContext`, `section`) have clean signatures.
- **A.5 Magic numbers/strings:** Template path constants (`AGENTS_MD`, `CONFIG_TOML`) are properly extracted. Context values in fixtures are documented with JSDoc comments.
- **A.6 Test name interpolation (-0.5):** `it.each` on line 118 uses `${field}` (JS template literal syntax inside a regular string) which does not interpolate in Vitest -- verbose output shows literal `techStack_${field}None_omits${label}Row`. Similarly line 473 shows `qualityGates_undefined` in verbose output because `$field$value` without separators fails to interpolate. Should use `$field` with separators (e.g., `qualityGates_${label}_${value}_renders`). Tests pass but reporter output is unhelpful for debugging. **(-0.5 combined with A.1)**

**Score: 7/8**

---

## B. Naming (4/4)

- **B.1 Intention-revealing:** `fullContext()`, `minimalContext()`, `section()`, `aFullProjectConfig()`, `aMinimalProjectConfig()` -- all clearly communicate intent.
- **B.2 No disinformation:** Template file names match their content (`header.md.njk` renders the header, `agents.md.njk` renders agents).
- **B.3 Meaningful distinctions:** `fullContext` vs `minimalContext` clearly distinguish the two test scenarios. `aFullProjectConfig` vs `aMinimalProjectConfig` vs `aProjectConfig` -- well differentiated.
- **B.4 Constants:** `AGENTS_MD`, `CONFIG_TOML`, `RESOURCES_DIR` follow UPPER_SNAKE convention.

**Score: 4/4**

---

## C. Functions (5/5)

- **C.1 Single responsibility:** Each helper function does one thing. `fullContext()` builds full context, `minimalContext()` builds minimal context, `section()` resolves section template paths.
- **C.2 Size:** `fullContext()` is 33 lines (data construction), `minimalContext()` is 19 lines. Both are pure data factories, not logic. The `section()` helper is 2 lines.
- **C.3 Max 4 params:** All functions have 0-1 parameters.
- **C.4 No boolean flags:** No boolean parameters used as function flags.
- **C.5 Template functions:** Template logic is minimal -- simple `{% if %}` guards and `{% for %}` loops. No complex logic.

**Score: 5/5**

---

## D. Vertical Formatting (4/4)

- **D.1 Blank lines:** Template files use blank lines to separate Markdown sections. Test file uses comment separators between describe blocks.
- **D.2 Newspaper Rule:** Test file follows top-down: imports, constants, helpers, then test groups ordered from atomic (section tests) to composite (orchestrator, config, snapshots, edge cases).
- **D.3 File sizes:** Test file is 540 lines -- within the 250-line limit for source files, but this is a test file which traditionally has higher limits. Templates are all under 50 lines each.
- **D.4 Grouping:** Related tests are grouped in describe blocks with clear section headers.

**Score: 4/4**

---

## E. Design (3/3)

- **E.1 Law of Demeter:** No train wreck chains. Template access to `resolved_stack.buildCmd` is a single-level property access on a flat object, which is acceptable.
- **E.2 CQS:** Not applicable for templates. Test assertions follow query-only pattern.
- **E.3 DRY:** Context construction is centralized in `fullContext()` and `minimalContext()`. Section path resolution uses the `section()` helper. No duplication.

**Score: 3/3**

---

## F. Error Handling (3/3)

- **F.1 throwOnUndefined compliance:** Test 52 (`agentsMd_missingExtendedVariable_throwsError`) validates that missing required variables throw errors.
- **F.2 No null returns:** Not applicable for templates.
- **F.3 No generic catch:** No try/catch blocks in the code. Error paths tested via `expect().toThrow()`.

**Score: 3/3**

---

## G. Architecture (4/5)

- **G.1 SRP:** Each section template has a single responsibility. Orchestrator composes sections. Config template is standalone.
- **G.2 DIP:** Templates depend on context variables (abstractions), not on concrete config classes.
- **G.3 Layer boundaries:** Templates are pure resources. Test file imports only from `src/template-engine.ts` and fixtures. No circular dependencies.
- **G.4 Follows plan:** Implementation follows the STORY-021 plan closely. All 13 templates created. Directory structure matches spec. Include paths use `codex-templates/sections/` prefix as documented.
- **G.5 Quality gates numbering (-1):** The `quality-gates.md.njk` template uses hardcoded list numbers (1, 2, 3, 4, 5, 6, 7) with conditional items. When `contract_tests` is False, the output skips item 4 but continues with 5 (E2E), 6 (Performance), 7 (Smoke). This produces non-sequential numbering in the rendered Markdown (1, 2, 3, 5, 6, 7). While Markdown renderers may handle this, it is semantically incorrect and visually confusing in plain text. **(-1)**

**Score: 4/5**

---

## H. Framework & Infra (4/4)

- **H.1 DI:** `TemplateEngine` receives `resourcesDir` and `config` via constructor injection.
- **H.2 Externalized config:** Template variables come from `ProjectConfig` and extended context -- all externalized.
- **H.3 Native-compatible:** Pure Nunjucks templates with no native dependencies.
- **H.4 Config.toml security:** The `config.toml.njk` template includes a `# WARNING: This file may contain secrets` comment -- good practice.

**Score: 4/4**

---

## I. Tests (2/3)

- **I.1 Coverage thresholds:** 55 tests covering all 13 templates. Full and minimal contexts exercise all conditional branches. All Gherkin acceptance criteria from the story are covered (see cross-reference below).
- **I.2 Scenarios covered:** Individual section rendering (15 tests), orchestrator full/minimal (10 tests), config.toml (9 tests), snapshots (4 tests), edge cases (16 tests), throwOnUndefined (1 test). Test plan specified 52 tests; implementation delivers 55 -- exceeds plan.
- **I.3 Test quality (-1):** The `it.each` test name format strings do not interpolate correctly in Vitest. Six quality-gates tests show as `qualityGates_undefined` and four tech-stack tests show as `techStack_${field}None_omits${label}Row` in verbose/reporter output. This makes it impossible to identify which specific case failed from CI/CD output. **(-1)**

**Score: 2/3**

---

## J. Security & Production (1/1)

- **J.1 Sensitive data:** Config.toml template warns about secrets with a comment. Test fixtures use obviously fake values (`test-key-xxx`, `ghp_xxx`, `s3cret`). No real secrets in templates or tests.

**Score: 1/1**

---

## Cross-File Consistency

| Check | Result |
|-------|--------|
| Templates consume same variables documented in plan Section 3.5 | PASS -- all 31 context variables are used correctly |
| `resolved_stack` field names match actual `ResolvedStack` model | PASS -- `buildCmd`, `testCmd`, `compileCmd`, `coverageCmd` (camelCase matches codebase; story doc used snake_case which was inaccurate) |
| `fullContext()` overrides from `buildDefaultContext()` for `observability` | PASS -- `observability` is not in default context (24 fields), correctly provided as extended context |
| `aFullProjectConfig()` includes `ObservabilityConfig("opentelemetry")` | PASS |
| `aMinimalProjectConfig()` values produce `domain_driven: "False"`, empty arrays | PASS |
| Conditional guards in orchestrator match story Section 3.4 conditions | PASS -- all 9 conditions implemented |
| Template include paths use `codex-templates/sections/` prefix | PASS |
| Snapshot output has no template artifacts (`{{`, `{%`, `{#`) | PASS -- verified in snapshots |

---

## Acceptance Criteria Cross-Reference (Gherkin)

| Gherkin Scenario | Test IDs | Status |
|-----------------|----------|--------|
| Full context renders all sections | agentsMd_fullContext_containsAllSections | PASS |
| No template artifacts in output | agentsMd_fullContext_noTemplateArtifacts, configToml_fullContext_noTemplateArtifacts, minimal variants | PASS |
| Domain omitted when `domain_driven == "False"` | agentsMd_minimalContext_omitsDomainSection | PASS |
| Security omitted when `security_frameworks = []` | agentsMd_minimalContext_omitsSecuritySection | PASS |
| Tech Stack omits database row when `"none"` | techStack_databaseNone_omitsDatabaseRow | PASS |
| Tech Stack omits cache row when `"none"` | techStack_cacheNone_omitsCacheRow | PASS |
| config.toml with MCP servers | configToml_fullContext_rendersMcpServers, configToml_multipleMcpServers_rendersAllSections | PASS |
| config.toml without MCP servers | configToml_noMcpServers_omitsMcpSection, configToml_emptyMcpServers_noMcpSection | PASS |
| Approval policy "on-request" when `has_hooks == true` | configToml_fullContext_rendersModelAndPolicy | PASS |
| Approval policy "untrusted" when `has_hooks == false` | configToml_noHooks_usesUntrustedPolicy | PASS |

---

## ISSUES

### MEDIUM

1. **Quality gates non-sequential numbering** -- `resources/codex-templates/sections/quality-gates.md.njk`:11-24 -- The hardcoded ordered list numbers (1-7) with conditional items produce gaps (e.g., 1, 2, 3, 5 when Contract is omitted). **Suggestion:** Either use unordered lists (`-`) for the conditional items, or dynamically manage numbering. Alternatively, always render all categories with a "(disabled)" annotation for disabled ones.

2. **`it.each` test names not interpolating** -- `tests/node/codex-templates.test.ts`:118,473 -- Vitest `it.each` with object arrays uses `$property` syntax but line 118 uses `${field}` (JavaScript template literal syntax, not Vitest interpolation) and line 473 concatenates `$field$value` without separator. Verbose output shows `qualityGates_undefined` and `techStack_${field}None_omits${label}Row`. **Suggestion:** Fix test name strings: line 118 should be `"techStack_$field_None_omits_$label_Row"` and line 473 should be `"qualityGates_$field_$value_$label"`.

### LOW

1. **Unused import** -- `tests/node/codex-templates.test.ts`:10 -- `aProjectConfig` is imported but never used. **Suggestion:** Remove from imports.

2. **Config.toml env section placement** -- `resources/codex-templates/config.toml.njk`:15-18 -- The `[mcp_servers.X.env]` section header is rendered on a separate line from the env key-value pairs. While the snapshot shows valid output, the intermediate blank line between `command = [...]` and `[mcp_servers.X.env]` is produced by the `{% if server.env %}` tag. This is cosmetic but could be tightened.

3. **`aFullProjectConfig` near-duplicate of `aProjectConfig`** -- `tests/fixtures/project-config.fixture.ts`:128-153 vs 84-100 -- `aFullProjectConfig()` is nearly identical to `aProjectConfig()` with added `ObservabilityConfig` and `SecurityConfig(["owasp", "pci-dss"])`. Consider composing `aFullProjectConfig` from `aProjectConfig` with overrides rather than duplicating the constructor call. This reduces maintenance burden if constructor parameters change.

---

## Verification Results

| Check | Result |
|-------|--------|
| `npx tsc --noEmit` | PASS -- zero warnings |
| `npx vitest run tests/node/codex-templates.test.ts` | PASS -- 55/55 tests |
| `npx vitest run` (full suite) | PASS -- 1439/1439 tests (47 files) |
| Template count | 13/13 templates created |
| Snapshot file present | PASS |

---

## Decision

```
============================================================
 TECH LEAD REVIEW -- STORY-021
============================================================
 Decision:  CONDITIONAL GO
 Score:     36/40
 Critical:  0 issues
 Medium:    2 issues
 Low:       3 issues
------------------------------------------------------------
 Conditions for GO:
 1. Fix it.each test name interpolation (MEDIUM #2)
 2. Remove unused aProjectConfig import (LOW #1)

 Recommended (not blocking):
 - Fix quality-gates numbering gaps (MEDIUM #1)
 - Tighten config.toml whitespace (LOW #2)
 - Reduce fixture duplication (LOW #3)
============================================================
```
