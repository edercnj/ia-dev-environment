# Implementation Plan -- STORY-001: Global and Contextual Copilot Instructions

**Status:** IMPLEMENTED
**Date:** 2026-03-08

---

## 1. Affected Layers and Components

| Layer | Component | Impact |
|-------|-----------|--------|
| assembler | `GithubInstructionsAssembler` | New assembler added as 9th in the pipeline |
| assembler | `__init__.py` (pipeline orchestration) | Modified to register the new assembler |
| resources | `github-instructions-templates/` | New template directory with 4 contextual templates |
| CLI | `__main__.py` (`_classify_files`) | Already supported -- files under `github/` path classified as "GitHub" |
| tests | `test_pipeline.py` | Updated to validate 9 assemblers and ordering |
| tests | `test_byte_for_byte.py` | Validates generated output against golden files |
| golden files | `tests/golden/java-quarkus/github/` | New golden files for byte-for-byte validation |

## 2. New Classes/Interfaces Created

| Class / Module | Location | Responsibility |
|----------------|----------|----------------|
| `GithubInstructionsAssembler` | `src/ia_dev_env/assembler/github_instructions_assembler.py` | Generates `.github/copilot-instructions.md` (global, from `ProjectConfig`) and `instructions/*.instructions.md` (contextual, from templates) |
| `_build_copilot_instructions()` | Same file (module-level function) | Builds the global `copilot-instructions.md` content string programmatically from `ProjectConfig` fields |

**Templates (new files, not classes):**

| File | Location |
|------|----------|
| `domain.md` | `resources/github-instructions-templates/domain.md` |
| `coding-standards.md` | `resources/github-instructions-templates/coding-standards.md` |
| `architecture.md` | `resources/github-instructions-templates/architecture.md` |
| `quality-gates.md` | `resources/github-instructions-templates/quality-gates.md` |

**Golden files (new):**

| File | Location |
|------|----------|
| `copilot-instructions.md` | `tests/golden/java-quarkus/github/copilot-instructions.md` |
| `domain.instructions.md` | `tests/golden/java-quarkus/github/instructions/domain.instructions.md` |
| `coding-standards.instructions.md` | `tests/golden/java-quarkus/github/instructions/coding-standards.instructions.md` |
| `architecture.instructions.md` | `tests/golden/java-quarkus/github/instructions/architecture.instructions.md` |
| `quality-gates.instructions.md` | `tests/golden/java-quarkus/github/instructions/quality-gates.instructions.md` |

## 3. Existing Classes Modified

| Class / Module | Location | Change Description |
|----------------|----------|--------------------|
| `_build_assemblers()` | `src/ia_dev_env/assembler/__init__.py` | Added `GithubInstructionsAssembler` as the 9th (last) entry in the assembler list |
| `__all__` | `src/ia_dev_env/assembler/__init__.py` | Added `GithubInstructionsAssembler` to the public API exports |
| Import block | `src/ia_dev_env/assembler/__init__.py` | Added import of `GithubInstructionsAssembler` from the new module |
| `TestBuildAssemblers` | `tests/test_pipeline.py` | Updated `test_returns_nine_assemblers` (count from 8 to 9) and added `test_last_assembler_is_github_instructions` |

## 4. Dependency Direction Validation

```
GithubInstructionsAssembler
    imports: ProjectConfig (domain/models)
    imports: TemplateEngine (engine)
    imports: Path, List (stdlib)
    imports: logging (stdlib)
```

**Assessment: COMPLIANT.** The assembler depends only on domain models (`ProjectConfig`) and the template engine (`TemplateEngine`). It does not import any other assembler, framework code, or adapter layer. Dependencies point inward toward the domain, consistent with the hexagonal architecture rule.

The assembler follows the same contract as all other assemblers:
- Constructor receives `resources_dir: Path`
- `assemble(config, output_dir, engine) -> List[Path]`

## 5. Integration Points

| Integration Point | Direction | Description |
|--------------------|-----------|-------------|
| Pipeline orchestration | Inbound | `_execute_assemblers()` calls `assemble()` on `GithubInstructionsAssembler` as the 9th step |
| `TemplateEngine` | Outbound (dependency) | Used for placeholder replacement in contextual templates (`engine.replace_placeholders()`) |
| `ProjectConfig` | Outbound (dependency) | Provides data for the global file: project name, architecture style, language, framework, interfaces, infrastructure, testing config |
| File system | Outbound | Writes to `output_dir/github/` and `output_dir/github/instructions/` |
| CLI classification | Downstream | `_classify_files()` in `__main__.py` counts files with `"github"` in path parts under the "GitHub" category |

### Data Flow

1. `ProjectConfig` is parsed from `ia-dev-env.yaml`
2. `TemplateEngine` is created with config placeholders
3. `GithubInstructionsAssembler.assemble()` is called with config, output_dir, and engine
4. Global file: `_build_copilot_instructions(config)` extracts fields from `ProjectConfig` and writes `copilot-instructions.md`
5. Contextual files: For each of the 4 templates, reads the template, runs `engine.replace_placeholders()`, writes `*.instructions.md`
6. Returns list of all generated `Path` objects

### Rules-to-Instructions Mapping

| Source Rule | Generated Output | Generation Strategy |
|-------------|------------------|---------------------|
| `01-project-identity.md` | `github/copilot-instructions.md` | Programmatic from `ProjectConfig` (no template) |
| `02-domain.md` | `github/instructions/domain.instructions.md` | Template with placeholder replacement |
| `03-coding-standards.md` | `github/instructions/coding-standards.instructions.md` | Template with placeholder replacement |
| `04-architecture-summary.md` | `github/instructions/architecture.instructions.md` | Template with placeholder replacement |
| `05-quality-gates.md` | `github/instructions/quality-gates.instructions.md` | Template with placeholder replacement |

## 6. Database Changes

None. This feature is purely a file-generation pipeline component with no persistence layer.

## 7. API Changes

None. No HTTP/gRPC/event API surface is affected. The assembler operates within the CLI pipeline only.

## 8. Event Changes

None. No events are produced or consumed by this feature.

## 9. Configuration Changes

| Change | Location | Description |
|--------|----------|-------------|
| Templates directory | `resources/github-instructions-templates/` | New directory containing 4 Markdown templates used by the assembler |
| Assembler registration | `src/ia_dev_env/assembler/__init__.py` | `GithubInstructionsAssembler` registered as position 9 in `_build_assemblers()` |

No changes to `ia-dev-env.yaml` schema or `ProjectConfig` model were required. The assembler consumes existing fields already present in `ProjectConfig`.

### Template Placeholders Used

| Placeholder | Template(s) | Source in ProjectConfig |
|-------------|-------------|------------------------|
| `{project_name}` | `domain.md` | `config.project.name` |
| `{project_purpose}` | `domain.md` | `config.project.purpose` |
| `{language_name}` | `coding-standards.md` | `config.language.name` |
| `{language_version}` | `coding-standards.md` | `config.language.version` |
| `{coverage_line}` | `quality-gates.md` | `config.testing.coverage_line` |
| `{coverage_branch}` | `quality-gates.md` | `config.testing.coverage_branch` |

## 10. Risk Assessment

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| Template directory missing at runtime | Low | Low | `_generate_contextual()` logs a warning and returns empty list gracefully |
| Individual template file missing | Low | Low | Loop skips missing templates with a warning log; other templates still generated |
| Copilot does not load `.instructions.md` files | Medium | Low | Extension follows official Copilot documentation conventions; validated in acceptance criteria |
| Placeholder not replaced in template | Low | Low | `TemplateEngine.replace_placeholders()` leaves unknown placeholders as-is, making them visible in output for debugging |
| Golden file drift after config model changes | Medium | Medium | `test_byte_for_byte.py` catches any drift immediately; golden files must be regenerated when `ProjectConfig` fields change |
| Ordering dependency with other assemblers | Low | Low | `GithubInstructionsAssembler` is position 9 (last) and has no dependency on other assemblers' output; it only depends on `ProjectConfig` and templates |
| Cross-reference links between `.github/` and `.claude/` break | Medium | Low | Templates use relative paths (e.g., `.claude/skills/architecture/SKILL.md`); both directories are generated in the same `output_dir` by the pipeline |
