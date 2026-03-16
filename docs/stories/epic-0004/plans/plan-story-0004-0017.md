# Implementation Plan: Post-Deploy Verification Step

**Story:** story-0004-0017
**Epic:** EPIC-0004 (Feature Lifecycle Evolution)
**Date:** 2026-03-16

---

## 1. Affected Layers and Components

This story modifies **generated template content only** -- it adds a post-deploy verification sub-step to the final phase (Phase 7) of the `x-dev-lifecycle` skill. No TypeScript source code logic changes are required.

| Layer | Component | Change Type |
|-------|-----------|-------------|
| Resources (templates) | `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` | Modify (source of truth) |
| Resources (templates) | `resources/github-skills-templates/dev/x-dev-lifecycle.md` | Modify (GitHub dual copy) |
| Generated output (.claude) | `.claude/skills/x-dev-lifecycle/SKILL.md` | Regenerated from template |
| Generated output (.github) | `.github/skills/x-dev-lifecycle/SKILL.md` | Regenerated from template |
| Generated output (.agents) | `.agents/skills/x-dev-lifecycle/SKILL.md` | Auto-mirrored from .claude |
| Tests (golden files) | `tests/golden/{all-8-profiles}/.claude/skills/x-dev-lifecycle/SKILL.md` | Update expected output |
| Tests (golden files) | `tests/golden/{all-8-profiles}/.github/skills/x-dev-lifecycle/SKILL.md` | Update expected output |
| Tests (golden files) | `tests/golden/{all-8-profiles}/.agents/skills/x-dev-lifecycle/SKILL.md` | Update expected output |

## 2. New Classes/Interfaces to Create

**None.** This story is purely a template content change. The lifecycle skill template is a Markdown file, not TypeScript code. No new classes, interfaces, assemblers, or domain objects are needed.

## 3. Existing Classes to Modify

### 3.1 Template Files (Source of Truth)

#### `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`

**What changes:**
- Add a new sub-section within **Phase 7 -- Final Verification + Cleanup** for post-deploy verification
- The sub-section is conditional on `smoke_tests == true` in the project identity
- Add a new conditional DoD item for smoke test verification
- Update the phase count description if needed (remains 8 phases, 0-7)

The sub-section will include:
1. Conditional check: `if testing.smoke_tests == true`
2. Invoke `/run-e2e` or smoke test script against the deployed environment
3. Verify: health check (GET /health -> 200), critical path request, response time SLO, error rate
4. Report PASS/FAIL/SKIP with details
5. Non-blocking: emit result for human decision, do not auto-rollback

#### `resources/github-skills-templates/dev/x-dev-lifecycle.md`

**What changes:**
- Mirror the same post-deploy verification sub-section added to the .claude template
- Adjust skill path references to use `.github/skills/` prefix (GitHub Copilot convention)

### 3.2 Golden Files (8 profiles x 3 output targets = 24 files)

All 8 profiles have `smoke_tests: true`, so all golden files will include the post-deploy verification step.

| Profile | Files to Update |
|---------|----------------|
| go-gin | `.claude/`, `.github/`, `.agents/` |
| java-quarkus | `.claude/`, `.github/`, `.agents/` |
| java-spring | `.claude/`, `.github/`, `.agents/` |
| kotlin-ktor | `.claude/`, `.github/`, `.agents/` |
| python-click-cli | `.claude/`, `.github/`, `.agents/` |
| python-fastapi | `.claude/`, `.github/`, `.agents/` |
| rust-axum | `.claude/`, `.github/`, `.agents/` |
| typescript-nestjs | `.claude/`, `.github/`, `.agents/` |

**Approach:** Regenerate golden files by running the pipeline and updating snapshots.

### 3.3 Source Code -- No Changes Needed

The following components require **zero modifications**:

- `src/assembler/skills-assembler.ts` -- Already copies the entire `core/x-dev-lifecycle/` directory tree with placeholder replacement. The new content is just more Markdown text in the template.
- `src/assembler/github-skills-assembler.ts` -- Already copies `x-dev-lifecycle.md` from `github-skills-templates/dev/`. No structural change.
- `src/assembler/codex-skills-assembler.ts` -- Mirrors `.claude/skills/` to `.agents/skills/` automatically.
- `src/template-engine.ts` -- No new placeholders needed. The `smoke_tests` context variable is already in `buildDefaultContext()` and available for any Nunjucks conditionals.
- `src/models.ts` -- `TestingConfig.smokeTests` already exists.
- `src/assembler/skills-selection.ts` -- No new conditional skills to add.

## 4. Dependency Direction Validation

This change is entirely within the **resources layer** (templates). It does not alter any TypeScript source code dependency. The existing dependency chain remains:

```
Template (resources/) --> TemplateEngine (src/) --> Assembler (src/) --> Output (generated)
```

No new imports, no new dependencies, no inversion violations.

## 5. Integration Points

### 5.1 Template Engine Integration

The `x-dev-lifecycle/SKILL.md` template does NOT use Nunjucks conditionals for the post-deploy section. The reason: all `{{PLACEHOLDER}}` tokens in the lifecycle template are **runtime markers** filled by the AI agent at execution time, not by the template engine at generation time (see Integration Notes at bottom of the template).

However, the story requirement says the post-deploy verification should be **conditional on `smoke_tests: true`**. Two approaches:

**Option A (Recommended): Always include the section with a runtime condition**
Add the post-deploy verification sub-section unconditionally in the template, with an explicit instruction like:
> "If `smoke_tests == true` in the project identity, execute post-deploy verification. Otherwise, emit SKIP."

This matches the existing pattern for conditional DoD items (contract_tests, event_driven, compliance) which are always present in the template but conditionally executed at runtime.

**Option B: Use Nunjucks conditional in template**
Wrap the section in `{% if smoke_tests == "True" %}...{% endif %}`. This would exclude it from generated output when `smoke_tests` is false.

**Decision: Option A** is consistent with all existing conditional items in the lifecycle template. The section is always generated; the AI agent decides at runtime whether to execute or skip.

### 5.2 Skill Invocation

The post-deploy verification references:
- `/run-e2e` skill -- already exists as a conditional skill, always selected (see `selectTestingSkills()` in `skills-selection.ts`)
- `/run-smoke-api` skill -- already exists as a conditional skill, selected when `smoke_tests && rest`
- `/run-smoke-socket` skill -- exists for TCP-based protocols

### 5.3 Dual Copy Consistency (RULE-001)

Changes must be applied to both:
1. `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` (Claude Code source of truth)
2. `resources/github-skills-templates/dev/x-dev-lifecycle.md` (GitHub Copilot dual copy)

The `.agents/` copy is auto-generated from `.claude/` by `CodexSkillsAssembler`, so no separate template exists.

## 6. Database Changes

**None.** This is a CLI tool that generates configuration files.

## 7. API Changes

**None.** No CLI interface changes. No new commands, flags, or options.

## 8. Event Changes

**None.** The project is not event-driven.

## 9. Configuration Changes

**None.** No changes to:
- `src/models.ts` (TestingConfig already has `smokeTests`)
- `src/template-engine.ts` (context already includes `smoke_tests`)
- Config templates (`resources/config-templates/setup-config.*.yaml`)
- Settings files (`.claude/settings.json`)

## 10. Risk Assessment

### Low Risk

| Risk | Mitigation |
|------|------------|
| Template change breaks golden file tests | Expected. All 24 golden files (8 profiles x 3 targets) need regeneration. The byte-for-byte test suite will catch any missed files. |
| Dual copy divergence | Apply changes to both templates in the same commit. Review diff side-by-side. |
| Backward compatibility (RULE-003) | The post-deploy verification is a runtime instruction for the AI agent. Projects with `smoke_tests: false` will see the section but the agent will skip it (same pattern as existing conditional DoD items). No config schema change. |
| Phase numbering confusion | The story mentions "Phase 8 after renumbering from story-0004-0005". However, the current template has Phases 0-7 with no renumbering visible. The plan adds the sub-step **within Phase 7** as stated in story section 3.4, not as a new Phase 8. |
| Dependent story not complete (story-0004-0013) | Story-0004-0013 adds architecture plan integration in Phase 1. This story-0004-0017 adds post-deploy in Phase 7. They modify different phases and do not conflict. However, if story-0004-0013 changes Phase 7's structure, the merge order matters. Mitigated by targeting different sections of the template. |

### Implementation Notes

1. **Template-only change**: No TypeScript compilation risks. The `post-compile-check.sh` hook only triggers on `.ts` file changes.
2. **Test regeneration**: Run the pipeline for all 8 profiles and update golden files. The `byte-for-byte.test.ts` integration test validates all output.
3. **Content language**: All generated content must be in English (RULE-012).

---

## Implementation Order

1. **Modify source template**: `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`
   - Add post-deploy verification sub-section in Phase 7
   - Add conditional DoD item for smoke test verification
2. **Modify GitHub template**: `resources/github-skills-templates/dev/x-dev-lifecycle.md`
   - Mirror the same changes with `.github/skills/` path references
3. **Regenerate golden files**: Run pipeline for all 8 profiles, copy output to `tests/golden/`
4. **Run tests**: `npm test` to verify byte-for-byte parity across all profiles
5. **Verify dual copy consistency**: Diff the two templates to ensure content parity (allowing path reference differences)

---

## Detailed Template Changes

### Phase 7 Addition (after existing item 5 "Conditional DoD items")

Add between existing items 5 and 6 (Report PASS/FAIL):

```markdown
6. Post-Deploy Verification (if testing.smoke_tests == true):
   - **Health Check**: GET /health (or configured endpoint) -> 200 OK
   - **Critical Path**: Execute primary request flow -> valid response
   - **Response Time**: Verify p95 latency < configured SLO
   - **Error Rate**: Verify error rate < 1% threshold
   - Result: PASS (all checks green -> "Deploy confirmed") | FAIL (any check red -> "Investigate rollback") | SKIP (smoke_tests == false -> "Post-deploy verification skipped")
   - Non-blocking: emit result for human decision; do NOT auto-rollback
   - If available, invoke `/run-e2e` or `/run-smoke-api` for automated verification
```

### Conditional DoD Item Addition

Add to the existing item 5 "Conditional DoD items" list:

```markdown
   - Post-deploy verification passed or skipped (if testing.smoke_tests == true)
```

### Renumber subsequent items

Items 6 and 7 become 7 and 8:
- 7. Report PASS/FAIL result
- 8. `git checkout main && git pull origin main`
