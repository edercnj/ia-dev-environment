# Implementation Plan — STORY-012: GithubPromptsAssembler

## 1. Affected Layers and Components

| Layer | Component | Action |
|-------|-----------|--------|
| Assembler | `github_prompts_assembler.py` | Create new |
| Templates | `resources/github-prompts-templates/*.prompt.md.j2` | Create 4 templates |
| Pipeline | `assembler/__init__.py` | Register new assembler |
| Tests | `tests/assembler/test_github_prompts_assembler.py` | Create unit tests |
| Golden | `tests/golden/*/github/prompts/*.prompt.md` | Create for all 8 profiles |

## 2. New Classes/Interfaces

| Class | Package | Purpose |
|-------|---------|---------|
| `GithubPromptsAssembler` | `src/ia_dev_env/assembler/github_prompts_assembler.py` | Renders Jinja2 templates to `.prompt.md` files |

## 3. Existing Classes to Modify

| Class | File | Change |
|-------|------|--------|
| `_build_assemblers()` | `src/ia_dev_env/assembler/__init__.py` | Add `GithubPromptsAssembler` entry |
| `__all__` | `src/ia_dev_env/assembler/__init__.py` | Add export |

## 4. Design Decisions

- **Template rendering**: Use `engine.render_template()` (Jinja2) since templates use `.j2` extension and may reference config variables like `{{ project_name }}`
- **Output path**: `output_dir/github/prompts/*.prompt.md`
- **Pattern**: Follow `GithubInstructionsAssembler` pattern — constructor takes `resources_dir`, `assemble()` takes `(config, output_dir, engine)`
- **Template names**: Strip `.j2` suffix to produce output filenames

## 5. Templates to Create

| Template | Output | Skills Referenced | Agents Referenced |
|----------|--------|-------------------|-------------------|
| `new-feature.prompt.md.j2` | `new-feature.prompt.md` | x-dev-lifecycle, x-dev-implement, x-review | java-developer, tech-lead |
| `decompose-spec.prompt.md.j2` | `decompose-spec.prompt.md` | x-story-epic-full | product-owner, architect |
| `code-review.prompt.md.j2` | `code-review.prompt.md` | x-review, x-review-api, x-review-pr | tech-lead, security-engineer, qa-engineer |
| `troubleshoot.prompt.md.j2` | `troubleshoot.prompt.md` | x-ops-troubleshoot | java-developer |

## 6. Risk Assessment

- **Low risk**: Follows well-established assembler pattern
- **No breaking changes**: Additive only — new assembler, new templates, new golden files
- **Template content**: Prompts reference skills/agents by name (orchestration, not duplication)
