# Task Decomposition -- STORY-001: Global and Contextual Copilot Instructions

**Status:** IMPLEMENTED
**Date:** 2026-03-08

---

## G1: Foundation -- Domain Model Additions

**Dependencies:** None

No domain model changes were required. `GithubInstructionsAssembler` consumes existing
`ProjectConfig` fields (project, architecture, language, framework, interfaces,
infrastructure, testing). The assembler contract (`assemble(config, output_dir, engine) -> List[Path]`)
was already established by previous assemblers.

| Task | Files Affected | Notes |
|------|---------------|-------|
| Verify `ProjectConfig` exposes all needed fields | `src/ia_dev_env/models.py` | No modification needed; fields already present |

---

## G2: Templates -- 4 Template Files in `resources/`

**Dependencies:** G1 (confirm available placeholders)

Create the 4 contextual Markdown templates consumed by `_generate_contextual()`.
Each template uses `TemplateEngine` placeholders (`{placeholder_name}`) that map to
`ProjectConfig` fields.

| Task | Files Affected | Notes |
|------|---------------|-------|
| T2.1 Create `domain.md` template | `resources/github-instructions-templates/domain.md` | New file. Placeholders: `{project_name}`, `{project_purpose}` |
| T2.2 Create `coding-standards.md` template | `resources/github-instructions-templates/coding-standards.md` | New file. Placeholders: `{language_name}`, `{language_version}` |
| T2.3 Create `architecture.md` template | `resources/github-instructions-templates/architecture.md` | New file. No dynamic placeholders (static adaptation of rule 04) |
| T2.4 Create `quality-gates.md` template | `resources/github-instructions-templates/quality-gates.md` | New file. Placeholders: `{coverage_line}`, `{coverage_branch}` |

---

## G3: Core Assembler -- `GithubInstructionsAssembler` Class

**Dependencies:** G2 (templates must exist for contextual generation)

Implement the assembler in a single module. Two generation paths: global file
(programmatic from `ProjectConfig`) and contextual files (template-based via
`TemplateEngine`).

| Task | Files Affected | Notes |
|------|---------------|-------|
| T3.1 Create `GithubInstructionsAssembler` class | `src/ia_dev_env/assembler/github_instructions_assembler.py` | New file. Constructor receives `resources_dir: Path` |
| T3.2 Implement `assemble()` method | Same file | Orchestrates `_generate_global()` and `_generate_contextual()`, returns `List[Path]` |
| T3.3 Implement `_generate_global()` | Same file | Calls `_build_copilot_instructions(config)`, writes `github/copilot-instructions.md` |
| T3.4 Implement `_build_copilot_instructions()` | Same file (module-level function) | Extracts identity, stack, constraints, contextual references from `ProjectConfig` |
| T3.5 Implement `_generate_contextual()` | Same file | Iterates `CONTEXTUAL_TEMPLATES`, reads templates, runs `engine.replace_placeholders()`, writes `*.instructions.md` |
| T3.6 Add graceful fallback for missing templates dir | Same file | Logs warning, returns empty list if `github-instructions-templates/` absent |
| T3.7 Add graceful fallback for individual missing template | Same file | Logs warning, skips template, continues loop |

---

## G4: Pipeline Integration -- Registration in `__init__.py`

**Dependencies:** G3 (assembler class must exist)

Register `GithubInstructionsAssembler` as the 9th (last) assembler in the pipeline.

| Task | Files Affected | Notes |
|------|---------------|-------|
| T4.1 Add import of `GithubInstructionsAssembler` | `src/ia_dev_env/assembler/__init__.py` | Import from `github_instructions_assembler` module |
| T4.2 Add entry to `_build_assemblers()` | Same file | Append as 9th tuple: `("GithubInstructionsAssembler", GithubInstructionsAssembler(resources_dir))` |
| T4.3 Add to `__all__` exports | Same file | Include `"GithubInstructionsAssembler"` in the public API list |
| T4.4 Verify CLI classification | `src/ia_dev_env/__main__.py` | No change needed; `_classify_files()` already classifies paths containing `"github"` as "GitHub" category |

---

## G5: Golden Files -- Test Fixtures

**Dependencies:** G3 (assembler must produce correct output to capture as golden files)

Generate and commit the expected byte-for-byte output used by `test_byte_for_byte.py`.

| Task | Files Affected | Notes |
|------|---------------|-------|
| T5.1 Create `copilot-instructions.md` golden file | `tests/golden/java-quarkus/github/copilot-instructions.md` | New file. Global instructions generated from default test config |
| T5.2 Create `domain.instructions.md` golden file | `tests/golden/java-quarkus/github/instructions/domain.instructions.md` | New file |
| T5.3 Create `coding-standards.instructions.md` golden file | `tests/golden/java-quarkus/github/instructions/coding-standards.instructions.md` | New file |
| T5.4 Create `architecture.instructions.md` golden file | `tests/golden/java-quarkus/github/instructions/architecture.instructions.md` | New file |
| T5.5 Create `quality-gates.instructions.md` golden file | `tests/golden/java-quarkus/github/instructions/quality-gates.instructions.md` | New file |

---

## G6: Tests -- Unit and Integration Tests

**Dependencies:** G3, G4, G5 (assembler, pipeline registration, and golden files must all be in place)

### Unit Tests (assembler behavior)

No dedicated `test_github_instructions_assembler.py` was created; assembler correctness
is validated through the byte-for-byte golden file test and the pipeline integration test.

| Task | Files Affected | Notes |
|------|---------------|-------|
| T6.1 Update assembler count assertion to 9 | `tests/test_pipeline.py` | `test_returns_nine_assemblers`: `assert len(assemblers) == 9` |
| T6.2 Add test for last assembler identity | `tests/test_pipeline.py` | `test_last_assembler_is_github_instructions`: asserts name == `"GithubInstructionsAssembler"` |
| T6.3 Validate golden files byte-for-byte | `tests/test_byte_for_byte.py` | Existing parametrized test picks up new golden files under `github/` automatically |

### Validation Points Covered

| Validation | Test / Mechanism |
|------------|-----------------|
| 9 assemblers in correct order | `test_pipeline.py::TestBuildAssemblers` |
| `GithubInstructionsAssembler` is last | `test_pipeline.py::test_last_assembler_is_github_instructions` |
| Output matches expected content | `test_byte_for_byte.py` (golden file comparison) |
| `.instructions.md` extension enforced | Golden files use correct extension; byte-for-byte test catches deviation |
| Relative links valid | Golden file content includes correct relative paths |
| CLI "GitHub" classification | Implicitly covered by path-based classification in `__main__.py` |

---

## G7: Documentation -- Story Updates

**Dependencies:** G6 (all implementation and tests must pass)

| Task | Files Affected | Notes |
|------|---------------|-------|
| T7.1 Mark story as IMPLEMENTED | `docs/stories/github-structure/STORY-001.md` | Status field updated; all DoD checkboxes checked |
| T7.2 Create implementation plan | `docs/plans/STORY-001-plan.md` | Documents layers, classes, dependencies, risks |
| T7.3 Create task decomposition (this file) | `docs/plans/STORY-001-tasks.md` | Groups G1-G7 with dependencies and files |

---

## Dependency Graph

```
G1 (Foundation)
 └─> G2 (Templates)
      └─> G3 (Core Assembler)
           ├─> G4 (Pipeline Integration)
           └─> G5 (Golden Files)
                └─> G6 (Tests) ← also depends on G4
                     └─> G7 (Documentation)
```

## File Summary

| File | Group(s) | Action |
|------|----------|--------|
| `src/ia_dev_env/models.py` | G1 | Verified (no change) |
| `resources/github-instructions-templates/domain.md` | G2 | Created |
| `resources/github-instructions-templates/coding-standards.md` | G2 | Created |
| `resources/github-instructions-templates/architecture.md` | G2 | Created |
| `resources/github-instructions-templates/quality-gates.md` | G2 | Created |
| `src/ia_dev_env/assembler/github_instructions_assembler.py` | G3 | Created |
| `src/ia_dev_env/assembler/__init__.py` | G4 | Modified (import, `_build_assemblers`, `__all__`) |
| `src/ia_dev_env/__main__.py` | G4 | Verified (no change) |
| `tests/golden/java-quarkus/github/copilot-instructions.md` | G5 | Created |
| `tests/golden/java-quarkus/github/instructions/domain.instructions.md` | G5 | Created |
| `tests/golden/java-quarkus/github/instructions/coding-standards.instructions.md` | G5 | Created |
| `tests/golden/java-quarkus/github/instructions/architecture.instructions.md` | G5 | Created |
| `tests/golden/java-quarkus/github/instructions/quality-gates.instructions.md` | G5 | Created |
| `tests/test_pipeline.py` | G6 | Modified (count + ordering assertions) |
| `tests/test_byte_for_byte.py` | G6 | Verified (auto-discovers golden files) |
| `docs/stories/github-structure/STORY-001.md` | G7 | Updated |
| `docs/plans/STORY-001-plan.md` | G7 | Created |
| `docs/plans/STORY-001-tasks.md` | G7 | Created (this file) |
