# Test Plan — STORY-005: Template Engine (Nunjucks)

## Summary

- Total test classes: 1 (`template-engine.test.ts`)
- Total test methods: ~45 (estimated)
- Categories covered: Unit, Parametrized (Contract-style)
- Estimated line coverage: ~97%
- Estimated branch coverage: ~93%

## Pre-existing Fixtures

| Fixture | Path | Purpose |
|---------|------|---------|
| simple.md.j2 | `tests/fixtures/templates/` | Simple template with 2 vars |
| multivar.md.j2 | `tests/fixtures/templates/` | Template with 15 vars |
| whitespace.txt.j2 | `tests/fixtures/templates/` | Leading spaces, blank lines, trailing newline |
| simple_rendered.md | `tests/fixtures/reference/` | Expected output for simple template |
| multivar_rendered.md | `tests/fixtures/reference/` | Expected output for multivar template |
| whitespace_rendered.txt | `tests/fixtures/reference/` | Expected output for whitespace template |
| legacy_placeholders.txt | `tests/fixtures/` | Input with `{placeholder}` patterns |
| legacy_replaced.txt | `tests/fixtures/reference/` | Expected output after placeholder replacement |
| section_base.md | `tests/fixtures/` | Base content with `<!-- INSERT:rules -->` marker |
| section_inject.md | `tests/fixtures/` | Content to inject |
| section_injected.md | `tests/fixtures/reference/` | Expected output after injection |
| concat_a.txt | `tests/fixtures/` | First file for concatenation |
| concat_b.txt | `tests/fixtures/` | Second file for concatenation |
| concat_result.txt | `tests/fixtures/reference/` | Expected concatenation result |

## Critical Risk: Boolean Rendering Parity

Python Jinja2 renders `True`/`False` (capital). Nunjucks renders `true`/`false` (lowercase).
Reference fixtures use Python-style `True`/`False`. Decision needed:
- **Option A**: Convert booleans to `"True"`/`"False"` strings in `buildDefaultContext`
- **Option B**: Update reference fixtures to JS-style `true`/`false`

Recommendation: **Option A** for byte-for-byte parity with Python output.

---

## Test Class: `TemplateEngineTest` (`tests/node/template-engine.test.ts`)

### Setup

- `FIXTURES_DIR` pointing to `tests/fixtures`
- `TEMPLATES_DIR` pointing to `tests/fixtures/templates`
- `REFERENCE_DIR` pointing to `tests/fixtures/reference`
- Factory function `aProjectConfig()` returning a `ProjectConfig` matching fixture expected values
- `afterEach`: `vi.restoreAllMocks()`

---

### Group 1: `buildDefaultContext` (exported function)

#### Happy Path

| # | Test Name | Description |
|---|-----------|-------------|
| 1 | `buildDefaultContext_validConfig_returns24Fields` | Verify exactly 24 keys returned |
| 2 | `buildDefaultContext_validConfig_mapsProjectFields` | Verify project_name, project_purpose |
| 3 | `buildDefaultContext_validConfig_mapsLanguageFields` | Verify language_name, language_version |
| 4 | `buildDefaultContext_validConfig_mapsFrameworkFields` | Verify framework_name, framework_version, build_tool |
| 5 | `buildDefaultContext_validConfig_mapsArchitectureFields` | Verify architecture_style, domain_driven, event_driven |
| 6 | `buildDefaultContext_validConfig_mapsInfraFields` | Verify container, orchestrator, templating, iac, registry, api_gateway, service_mesh |
| 7 | `buildDefaultContext_validConfig_mapsDataFields` | Verify database_name, cache_name |
| 8 | `buildDefaultContext_validConfig_mapsTestingFields` | Verify smoke_tests, contract_tests, performance_tests, coverage_line, coverage_branch |

#### Parametrized (Contract-style)

| # | Test Name | Source | Rows |
|---|-----------|--------|------|
| 9 | `buildDefaultContext_field_%s_mapsCorrectly` | `it.each` with all 24 field mappings | 24 |

---

### Group 2: `constructor`

| # | Test Name | Description |
|---|-----------|-------------|
| 10 | `constructor_validArgs_createsInstance` | Verify TemplateEngine instantiates without error |
| 11 | `constructor_nonExistentDir_createsInstanceWithoutError` | Nunjucks lazy-loads; error on render |

---

### Group 3: `renderTemplate`

#### Happy Path

| # | Test Name | Description |
|---|-----------|-------------|
| 12 | `renderTemplate_simpleTemplate_matchesReference` | Compare against `simple_rendered.md` |
| 13 | `renderTemplate_multivarTemplate_matchesReference` | Compare against `multivar_rendered.md` |
| 14 | `renderTemplate_whitespaceTemplate_matchesReference` | Compare against `whitespace_rendered.txt` |
| 15 | `renderTemplate_withContextOverrides_usesOverrides` | Override `project_name` → verify new value |
| 16 | `renderTemplate_trailingNewline_preserved` | Verify output ends with `\n` |

#### Error Path

| # | Test Name | Description |
|---|-----------|-------------|
| 17 | `renderTemplate_undefinedVariable_throwsError` | Template with `{{ unknown_var }}` |
| 18 | `renderTemplate_nonExistentTemplate_throwsError` | Path `does-not-exist.md.j2` |

---

### Group 4: `renderString`

#### Happy Path

| # | Test Name | Description |
|---|-----------|-------------|
| 19 | `renderString_simpleString_rendersVariables` | `"Hello {{ project_name }}"` → `"Hello my-service"` |
| 20 | `renderString_withOverrides_usesOverrides` | Override project_name |
| 21 | `renderString_noVariables_returnsUnchanged` | Plain string unchanged |
| 22 | `renderString_emptyString_returnsEmpty` | `""` → `""` |

#### Error Path

| # | Test Name | Description |
|---|-----------|-------------|
| 23 | `renderString_undefinedVariable_throwsError` | `{{ nonexistent }}` throws |

---

### Group 5: `replacePlaceholders`

#### Happy Path

| # | Test Name | Description |
|---|-----------|-------------|
| 24 | `replacePlaceholders_legacyFixture_matchesReference` | Compare against `legacy_replaced.txt` |
| 25 | `replacePlaceholders_knownKeys_replacesAll` | `{project_name}` → value |
| 26 | `replacePlaceholders_unknownKey_preservesOriginal` | `{unknown_key}` → `{unknown_key}` |
| 27 | `replacePlaceholders_mixedKnownUnknown_replacesOnlyKnown` | Mix of known/unknown |
| 28 | `replacePlaceholders_noPlaceholders_returnsUnchanged` | Plain text |
| 29 | `replacePlaceholders_emptyString_returnsEmpty` | `""` → `""` |
| 30 | `replacePlaceholders_withExplicitConfig_usesProvidedConfig` | Different config → different values |

#### Boundary

| # | Test Name | Description |
|---|-----------|-------------|
| 31 | `replacePlaceholders_nestedBraces_handlesCorrectly` | `{{project_name}}` — double braces (Nunjucks syntax, not placeholder) |
| 32 | `replacePlaceholders_emptyBraces_noMatch` | `{}` — regex `\w+` requires at least one char |
| 33 | `replacePlaceholders_specialCharsInBraces_noMatch` | `{project-name}` — hyphen not `\w` |

---

### Group 6: `injectSection` (static)

#### Happy Path

| # | Test Name | Description |
|---|-----------|-------------|
| 34 | `injectSection_fixtureMarker_matchesReference` | Compare against `section_injected.md` |
| 35 | `injectSection_simpleMarker_replacesMarker` | Basic replacement |
| 36 | `injectSection_multipleOccurrences_replacesAll` | `.replace()` replaces first only — verify behavior |

#### Boundary

| # | Test Name | Description |
|---|-----------|-------------|
| 37 | `injectSection_markerNotFound_returnsUnchanged` | No marker in content |
| 38 | `injectSection_emptySection_removesMarker` | Replace marker with `""` |
| 39 | `injectSection_emptyContent_returnsEmpty` | Empty base content |

---

### Group 7: `concatFiles` (static)

#### Happy Path

| # | Test Name | Description |
|---|-----------|-------------|
| 40 | `concatFiles_twoFixtures_matchesReference` | Compare against `concat_result.txt` |
| 41 | `concatFiles_defaultSeparator_usesNewline` | Verify `\n` separator |
| 42 | `concatFiles_customSeparator_usesSeparator` | `"---"` separator |

#### Boundary

| # | Test Name | Description |
|---|-----------|-------------|
| 43 | `concatFiles_emptyArray_returnsEmpty` | `[]` → `""` |
| 44 | `concatFiles_singleFile_returnsContentsNoSeparator` | One file, no separator added |

#### Error Path

| # | Test Name | Description |
|---|-----------|-------------|
| 45 | `concatFiles_nonExistentFile_throwsError` | File not found |

---

## Coverage Estimation

| Component | Public Methods | Branches | Est. Tests | Line % | Branch % |
|-----------|---------------|----------|-----------|--------|----------|
| `buildDefaultContext` | 1 | 0 | 9 | 100% | 100% |
| `constructor` | 1 | 0 | 2 | 100% | 100% |
| `renderTemplate` | 1 | 2 (context null check, merge) | 5 | 98% | 95% |
| `renderString` | 1 | 2 | 5 | 98% | 95% |
| `replacePlaceholders` | 1 | 4 (config null, key in map, regex) | 10 | 97% | 93% |
| `injectSection` | 1 | 0 | 6 | 100% | 100% |
| `concatFiles` | 1 | 2 (empty check, loop) | 5 | 100% | 100% |
| **Total** | **7** | **10** | **~45** | **~97%** | **~93%** |

## Risks and Gaps

1. **Boolean parity**: MITIGATED — `buildDefaultContext` converts booleans to Python-style `"True"`/`"False"` strings
2. **`injectSection` replaces first occurrence only**: MITIGATED — implementation uses `.replaceAll()` for all occurrences
3. **Trailing newline**: MITIGATED — Nunjucks preserves trailing newlines by default; confirmed by test
4. **Nunjucks error types**: MITIGATED — tests use generic `.toThrow()` matching
5. **File encoding**: MITIGATED — `concatFiles` explicitly passes `"utf-8"` to `readFileSync`
