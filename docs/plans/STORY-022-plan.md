# Implementation Plan — STORY-022: GitHub Skills Copilot Compatibility Audit & Fix

## 1. Affected Files and Exact Changes per GAP Category

### GAP-1: Wrong Path Prefix (`.claude/` -> `.github/`)

#### 1.1 `resources/github-skills-templates/dev/x-dev-lifecycle.md`

| Line(s) | Current | Replacement |
| :--- | :--- | :--- |
| 53 | `.claude/skills/architecture/references/architecture-principles.md` | `.github/skills/architecture/SKILL.md` |
| 54 | `.claude/skills/layer-templates/SKILL.md` | `.github/skills/layer-templates/SKILL.md` |
| 85 | `.claude/skills/protocols/references/event-driven-conventions.md` | `.github/skills/protocols/SKILL.md` |
| 94 | `.claude/skills/security/SKILL.md` | `.github/skills/security/SKILL.md` |
| 95 | `.claude/skills/compliance/SKILL.md` | `.github/skills/compliance/SKILL.md` |
| 109 | `.claude/skills/coding-standards/references/coding-conventions.md` | `.github/skills/coding-standards/SKILL.md` |
| 110 | `.claude/skills/coding-standards/references/version-features.md` | `.github/skills/coding-standards/SKILL.md` (merge with line 109 description) |
| 111 | `.claude/skills/layer-templates/SKILL.md` | `.github/skills/layer-templates/SKILL.md` |
| 112 | `.claude/skills/architecture/references/architecture-principles.md` | `.github/skills/architecture/SKILL.md` |
| 180 | `.claude/skills/x-dev-lifecycle/SKILL.md` | `.github/skills/x-dev-lifecycle/SKILL.md` |
| 181 | `.claude/skills/x-dev-implement/SKILL.md` | `.github/skills/x-dev-implement/SKILL.md` |
| 182 | `.claude/skills/x-review/SKILL.md` | `.github/skills/x-review/SKILL.md` |
| 183 | `.claude/skills/x-review-pr/SKILL.md` | `.github/skills/x-review-pr/SKILL.md` |

**Total: 13 replacements**

**Notes on references/ collapsing:**
- Lines 109-110: Both reference `coding-standards/references/`. Collapse to a single line referencing `.github/skills/coding-standards/SKILL.md` with combined description: "{{LANGUAGE}} conventions and {{LANGUAGE_VERSION}} features".
- Lines 94-95: Keep as separate lines since they point to different knowledge packs (security vs compliance).
- Lines where `references/X.md` is cited: Replace with `SKILL.md` and update the inline description to note the content is condensed.

#### 1.2 `resources/github-skills-templates/review/x-review.md`

| Line(s) | Current | Replacement |
| :--- | :--- | :--- |
| 95 | `.claude/skills/security/SKILL.md` -> then read `references/application-security.md`, `references/cryptography.md` | `.github/skills/security/SKILL.md` |
| 96 | `.claude/skills/testing/references/testing-philosophy.md`, `.claude/skills/testing/references/testing-conventions.md` | `.github/skills/testing/SKILL.md` |
| 97 | `.claude/skills/resilience/references/resilience-principles.md` | `.github/skills/resilience/SKILL.md` |
| 98 | `.claude/skills/database-patterns/SKILL.md` -> then read files listed in references/ | `.github/skills/database-patterns/SKILL.md` |
| 99 | `.claude/skills/observability/references/observability-principles.md` | `.github/skills/observability/SKILL.md` |
| 100 | `.claude/skills/infrastructure/references/infrastructure-principles.md` | `.github/skills/infrastructure/SKILL.md` |
| 101 | `.claude/skills/api-design/references/api-design-principles.md` + `.claude/skills/protocols/references/` | `.github/skills/api-design/SKILL.md`, `.github/skills/protocols/SKILL.md` |
| 102 | `.claude/skills/protocols/references/event-driven-conventions.md` | `.github/skills/protocols/SKILL.md` |
| 166 | `.claude/skills/x-review/SKILL.md` | `.github/skills/x-review/SKILL.md` |
| 167 | `.claude/skills/security/SKILL.md` | `.github/skills/security/SKILL.md` |
| 168 | `.claude/skills/testing/SKILL.md` | `.github/skills/testing/SKILL.md` |
| 169 | `.claude/skills/observability/SKILL.md` | `.github/skills/observability/SKILL.md` |

**Total: 12 replacements (8 in KP table, 4 in Detailed References)**

#### 1.3 `resources/github-skills-templates/review/x-review-pr.md`

| Line(s) | Current | Replacement |
| :--- | :--- | :--- |
| 46 | `.claude/skills/coding-standards/references/coding-conventions.md` | `.github/skills/coding-standards/SKILL.md` |
| 47 | `.claude/skills/architecture/references/architecture-principles.md` | `.github/skills/architecture/SKILL.md` |
| 48 | `.claude/rules/05-quality-gates.md` | `.github/instructions/05-quality-gates.instructions.md` |
| 125 | `.claude/skills/x-review-pr/SKILL.md` | `.github/skills/x-review-pr/SKILL.md` |
| 126 | `.claude/skills/coding-standards/SKILL.md` | `.github/skills/coding-standards/SKILL.md` |
| 127 | `.claude/skills/architecture/SKILL.md` | `.github/skills/architecture/SKILL.md` |

**Total: 6 replacements**

**Note:** Line 48 references `.claude/rules/` which maps to `.github/instructions/` per the Data Contracts table in the story. The GitHub instructions use the naming pattern `*.instructions.md`. Verify the exact filename of the quality gates instruction file before editing.

#### 1.4 `resources/github-skills-templates/git-troubleshooting/x-ops-troubleshoot.md`

| Line(s) | Current | Replacement |
| :--- | :--- | :--- |
| 8 (YAML) | `Reference: \`../../.claude/skills/x-ops-troubleshoot/references/\`` | `Reference: \`.github/skills/x-ops-troubleshoot/SKILL.md\`` |
| 132 | `Reference: \`../../.claude/skills/x-ops-troubleshoot/SKILL.md\`` | `Reference: \`.github/skills/x-ops-troubleshoot/SKILL.md\`` |

**Total: 2 replacements**

#### 1.5 `resources/github-skills-templates/lib/x-lib-task-decomposer.md`

| Line(s) | Current | Replacement |
| :--- | :--- | :--- |
| 7 (YAML) | `Reference: \`../../.claude/skills/lib/x-lib-task-decomposer/\`` | `Reference: \`.github/skills/lib/x-lib-task-decomposer/SKILL.md\`` |
| 31 | `.claude/skills/architecture/references/architecture-principles.md` | `.github/skills/architecture/SKILL.md` |
| 32 | `.claude/skills/layer-templates/SKILL.md` | `.github/skills/layer-templates/SKILL.md` |
| 100 | `Reference: \`../../.claude/skills/lib/x-lib-task-decomposer/SKILL.md\`` | `Reference: \`.github/skills/lib/x-lib-task-decomposer/SKILL.md\`` |

**Total: 4 replacements**

#### 1.6 `resources/github-skills-templates/lib/x-lib-group-verifier.md`

| Line(s) | Current | Replacement |
| :--- | :--- | :--- |
| 7 (YAML) | `Reference: \`../../.claude/skills/lib/x-lib-group-verifier/\`` | `Reference: \`.github/skills/lib/x-lib-group-verifier/SKILL.md\`` |
| 93 | `Reference: \`../../.claude/skills/lib/x-lib-group-verifier/SKILL.md\`` | `Reference: \`.github/skills/lib/x-lib-group-verifier/SKILL.md\`` |

**Total: 2 replacements**

#### 1.7 `resources/github-skills-templates/lib/x-lib-audit-rules.md`

| Line(s) | Current | Replacement |
| :--- | :--- | :--- |
| 8 (YAML) | `Reference: \`../../.claude/skills/lib/x-lib-audit-rules/\`` | `Reference: \`.github/skills/lib/x-lib-audit-rules/SKILL.md\`` |
| 36 | `.claude/rules/*.md` | `.github/instructions/*.instructions.md` |
| 40 | `.claude/skills/*/references/*.md` | `.github/skills/*/SKILL.md` |
| 83 | `Reference: \`../../.claude/skills/lib/x-lib-audit-rules/SKILL.md\`` | `Reference: \`.github/skills/lib/x-lib-audit-rules/SKILL.md\`` |

**Total: 4 replacements**

**Note:** Lines 36 and 40 are contextual references describing discovery logic (listing rules, finding reference files). In the GitHub context:
- `.claude/rules/*.md` maps to `.github/instructions/*.instructions.md`
- `.claude/skills/*/references/*.md` does not exist in GitHub output; the GitHub equivalent is `.github/skills/*/SKILL.md` (each SKILL.md already contains condensed KP content)

### GAP-2 + GAP-5: Portuguese -> English Translation (4 Story Skills)

All 4 files require full rewrite: translate body content to English, translate YAML `description` to English.

#### 2.1 `resources/github-skills-templates/story/x-story-create.md`

**Translation reference:** `resources/skills-templates/core/x-story-create/SKILL.md`

**Changes required:**
- YAML frontmatter `description`: Translate from Portuguese to English (use Claude Code SKILL.md description as reference)
- Body content: Full translation of all sections (headings, prose, tables, Gherkin keywords)
- Section headings: Match English structure from Claude Code counterpart
- Fix GAP-1 references on lines 111-112 (`.claude/skills/` -> `.github/skills/`)
- Fix GAP-3 reference on line 29 (`.claude/templates/` -> `resources/templates/`)
- Fix line 32 reference to decomposition guide
- Language Rules section: Keep as-is (the rules specify pt-BR output for generated content, which is by design)

**Approach:** Use the Claude Code SKILL.md as the primary translation source. The GitHub template should mirror its structure and content, adapted for the GitHub Copilot context (`.github/` paths instead of `.claude/` paths).

#### 2.2 `resources/github-skills-templates/story/x-story-epic.md`

**Translation reference:** `resources/skills-templates/core/x-story-epic/SKILL.md`

**Changes required:**
- YAML frontmatter `description`: Translate to English
- Body content: Full translation
- Fix line 29 (`.claude/templates/` -> `resources/templates/`)
- Fix lines 32, 101-102 (`.claude/skills/` -> `.github/skills/`)

#### 2.3 `resources/github-skills-templates/story/x-story-map.md`

**Translation reference:** `resources/skills-templates/core/x-story-map/SKILL.md`

**Changes required:**
- YAML frontmatter `description`: Translate to English
- Body content: Full translation
- Fix line 29 (`.claude/templates/` -> `resources/templates/`)
- Fix lines 99-100 (`.claude/skills/` -> `.github/skills/`)

#### 2.4 `resources/github-skills-templates/story/x-story-epic-full.md`

**Translation reference:** `resources/skills-templates/core/x-story-epic-full/SKILL.md`

**Changes required:**
- YAML frontmatter `description`: Translate to English
- Body content: Full translation
- Fix lines 31-33 (`.claude/templates/` -> `resources/templates/`)
- Fix lines 36, 116-117 (`.claude/skills/` -> `.github/skills/`)

#### 2.5 `resources/github-skills-templates/story/story-planning.md`

**Translation reference:** No direct Claude Code counterpart exists as a separate file (story-planning is a knowledge pack).

**Changes required:**
- YAML frontmatter `description`: Translate to English
- Body content: Full translation from Portuguese to English
- Fix lines 41-42 (`.claude/skills/` -> `.github/skills/`)

### GAP-3: Broken Template Path References

| File | Line | Old Path | New Path |
| :--- | :--- | :--- | :--- |
| `story/x-story-epic-full.md` | 31 | `.claude/templates/_TEMPLATE-EPIC.md` | `resources/templates/_TEMPLATE-EPIC.md` |
| `story/x-story-epic-full.md` | 32 | `.claude/templates/_TEMPLATE-STORY.md` | `resources/templates/_TEMPLATE-STORY.md` |
| `story/x-story-epic-full.md` | 33 | `.claude/templates/_TEMPLATE-IMPLEMENTATION-MAP.md` | `resources/templates/_TEMPLATE-IMPLEMENTATION-MAP.md` |
| `story/x-story-create.md` | 29 | `.claude/templates/_TEMPLATE-STORY.md` | `resources/templates/_TEMPLATE-STORY.md` |
| `story/x-story-epic.md` | 29 | `.claude/templates/_TEMPLATE-EPIC.md` | `resources/templates/_TEMPLATE-EPIC.md` |
| `story/x-story-map.md` | 29 | `.claude/templates/_TEMPLATE-IMPLEMENTATION-MAP.md` | `resources/templates/_TEMPLATE-IMPLEMENTATION-MAP.md` |

**Total: 6 replacements (3 in epic-full + 1 each in create, epic, map)**

> **IMPORTANT DISCREPANCY:** The story document (Section 5, Data Contracts) specifies the replacement path as `docs/templates/_TEMPLATE-*.md`, but `docs/templates/` does **not** exist in the project. The actual template files reside at `resources/templates/_TEMPLATE-*.md`. This plan uses `resources/templates/` as the correct target path. If the intent is for generated projects (not the ia-dev-environment source repo), clarify with the Product Owner.

### GAP-4: Knowledge Pack Reference Simplification

Fully addressed by GAP-1 changes to `review/x-review.md` (Section 1.2 above). The KP mapping table on lines 93-102 will be rewritten to point each engineer directly to `.github/skills/{pack}/SKILL.md` instead of `references/*.md` subdirectories.

**New KP Mapping Table content:**

| Engineer | KP Paths to Read |
| :--- | :--- |
| Security | `.github/skills/security/SKILL.md` |
| QA | `.github/skills/testing/SKILL.md` |
| Performance | `.github/skills/resilience/SKILL.md` |
| Database | `.github/skills/database-patterns/SKILL.md` |
| Observability | `.github/skills/observability/SKILL.md` |
| DevOps | `.github/skills/infrastructure/SKILL.md` |
| API | `.github/skills/api-design/SKILL.md`, `.github/skills/protocols/SKILL.md` |
| Event | `.github/skills/protocols/SKILL.md` |

---

## 2. Path Replacement Mapping (Old -> New)

### Systematic Replacement Rules

| # | Old Pattern | New Pattern | Scope |
| :--- | :--- | :--- | :--- |
| R1 | `.claude/skills/{pack}/references/{file}.md` | `.github/skills/{pack}/SKILL.md` | KP references in subagent prompts |
| R2 | `.claude/skills/{pack}/SKILL.md` | `.github/skills/{pack}/SKILL.md` | Direct skill SKILL.md references |
| R3 | `../../.claude/skills/{name}/SKILL.md` | `.github/skills/{name}/SKILL.md` | Relative refs in lib/troubleshoot |
| R4 | `../../.claude/skills/{name}/references/` | `.github/skills/{name}/SKILL.md` | Relative ref directory pointers |
| R5 | `../../.claude/skills/lib/{name}/SKILL.md` | `.github/skills/lib/{name}/SKILL.md` | Lib skill relative refs |
| R6 | `../../.claude/skills/lib/{name}/` | `.github/skills/lib/{name}/SKILL.md` | Lib skill relative dir pointers |
| R7 | `.claude/templates/_TEMPLATE-*.md` | `resources/templates/_TEMPLATE-*.md` | Story template file refs |
| R8 | `.claude/rules/*.md` | `.github/instructions/*.instructions.md` | Rule file references |
| R9 | `.claude/skills/*/references/*.md` (as glob pattern in discovery) | `.github/skills/*/SKILL.md` | Audit rules discovery |

### Application Order

Apply rules in specificity order (most specific first) to avoid partial matches:
1. R5, R6 (lib-specific relative paths with `../../.claude/skills/lib/`)
2. R3, R4 (non-lib relative paths with `../../.claude/skills/`)
3. R1 (references/ paths)
4. R7 (template paths)
5. R8 (rule paths)
6. R9 (glob-style discovery patterns)
7. R2 (direct SKILL.md paths -- broadest match, applied last)

---

## 3. Translation Approach for Portuguese Templates

### Strategy

Use the Claude Code counterparts as the canonical English source. For each Portuguese template:

1. **YAML frontmatter**: Translate `description` field using the Claude Code SKILL.md `description` as reference. Keep `name` unchanged (it is already English).

2. **Body content**: Do NOT blindly translate word-for-word. Instead:
   - Use the Claude Code SKILL.md as the primary content source (it is already in English)
   - Adapt paths from `.claude/` to `.github/`
   - Remove `references/` subdirectory pointers (GitHub skills have no `references/` dirs)
   - Preserve any GitHub-specific content that differs from the Claude Code version (e.g., integration notes specific to Copilot context)

3. **Language Rules section**: The story skills instruct the AI to generate output in pt-BR. This is an intentional business decision (the generated stories/epics are in Portuguese). The Language Rules section itself should be translated to English as instructional text, but the rule "All generated content must be in Brazilian Portuguese (pt-BR)" remains unchanged in meaning.

4. **Gherkin keywords**: The instruction to use Portuguese Gherkin (`DADO`/`QUANDO`/`ENTÃO`) remains as-is in meaning -- the instruction text wrapping it gets translated to English.

### Translation Mapping (5 files)

| GitHub Template | Claude Code Reference | YAML Description Source |
| :--- | :--- | :--- |
| `story/x-story-create.md` | `resources/skills-templates/core/x-story-create/SKILL.md` | Lines 2-13 of the SKILL.md |
| `story/x-story-epic.md` | `resources/skills-templates/core/x-story-epic/SKILL.md` | Lines 2-12 of the SKILL.md |
| `story/x-story-map.md` | `resources/skills-templates/core/x-story-map/SKILL.md` | Lines 2-12 of the SKILL.md |
| `story/x-story-epic-full.md` | `resources/skills-templates/core/x-story-epic-full/SKILL.md` | Lines 2-14 of the SKILL.md |
| `story/story-planning.md` | No direct counterpart | Translate existing description manually |

---

## 4. KP Reference Simplification Strategy

### Problem

GitHub Copilot skills are generated as a single `SKILL.md` per skill directory. Unlike Claude Code (which can have `references/` subdirectories with multiple files), GitHub skills are flat. The current templates instruct the AI to read `references/application-security.md` etc., which do not exist in the `.github/skills/` output.

### Solution (Approach B from story)

Replace all `references/*.md` pointers with the parent `SKILL.md`:

```
OLD: .claude/skills/security/SKILL.md -> then read references/application-security.md, references/cryptography.md
NEW: .github/skills/security/SKILL.md
```

This works because:
- Each GitHub `SKILL.md` is generated from the same knowledge pack template that already contains a condensed summary of all reference content
- The GithubSkillsAssembler generates a single `SKILL.md` per skill, incorporating all relevant content
- No information is lost -- it is already embedded in the SKILL.md

### Affected Locations

1. **x-review.md KP mapping table** (lines 93-102): 8 engineer rows, each pointing to `references/` files -> simplify to `SKILL.md`
2. **x-dev-lifecycle.md subagent prompts** (lines 53, 85, 109-112): Architecture/coding references -> `SKILL.md`
3. **x-review-pr.md context gathering** (lines 46-47): Coding standards and architecture references -> `SKILL.md`
4. **x-lib-task-decomposer.md STEP 0** (lines 31-32): Architecture references -> `SKILL.md`
5. **x-lib-audit-rules.md Phase 1b** (line 40): References glob pattern -> `SKILL.md` pattern

---

## 5. Golden File Regeneration Approach

### Impact Assessment

Template content changes propagate to the generated output through the GithubSkillsAssembler. All 8 profiles generate GitHub skills from the same templates (with `{{PLACEHOLDER}}` substitution). Therefore, **all 8 golden profiles** will have affected files.

### Affected Golden Files per Profile

Each profile has these GitHub skill directories that will change:

| Golden Skill Directory | Source Template |
| :--- | :--- |
| `github/skills/x-dev-lifecycle/SKILL.md` | `dev/x-dev-lifecycle.md` |
| `github/skills/x-review/SKILL.md` | `review/x-review.md` |
| `github/skills/x-review-pr/SKILL.md` | `review/x-review-pr.md` |
| `github/skills/x-ops-troubleshoot/SKILL.md` | `git-troubleshooting/x-ops-troubleshoot.md` |
| `github/skills/lib/x-lib-task-decomposer/SKILL.md` | `lib/x-lib-task-decomposer.md` |
| `github/skills/lib/x-lib-group-verifier/SKILL.md` | `lib/x-lib-group-verifier.md` |
| `github/skills/lib/x-lib-audit-rules/SKILL.md` | `lib/x-lib-audit-rules.md` |
| `github/skills/x-story-create/SKILL.md` | `story/x-story-create.md` |
| `github/skills/x-story-epic/SKILL.md` | `story/x-story-epic.md` |
| `github/skills/x-story-map/SKILL.md` | `story/x-story-map.md` |
| `github/skills/x-story-epic-full/SKILL.md` | `story/x-story-epic-full.md` |
| `github/skills/story-planning/SKILL.md` | `story/story-planning.md` |

**Total files affected: 12 skills x 8 profiles = 96 golden files**

### Regeneration Process

1. Apply all template changes
2. Run `npm run build` to compile
3. Run `npx tsx src/index.ts generate` (or equivalent) for each of the 8 profiles to produce output
4. Copy the generated `github/skills/` output to the corresponding `tests/golden/{profile}/github/skills/` directories
5. Alternatively, write a script that iterates over `CONFIG_PROFILES` and runs the pipeline, then copies output to golden dirs
6. Run `npm run test:integration` to verify byte-for-byte parity passes

### Efficient Approach

Since all 8 profiles use the same GitHub skill templates (no profile-specific template content for the affected skills), the golden file diffs will be identical across profiles except for `{{PLACEHOLDER}}` values like `{{LANGUAGE}}`, `{{COMPILE_COMMAND}}`, etc. Many of the affected templates (story skills, lib skills, troubleshoot) do not contain language-specific placeholders, so their golden files will be byte-for-byte identical across all 8 profiles.

---

## 6. Risk Assessment

### Low Risk

| Risk | Impact | Mitigation |
| :--- | :--- | :--- |
| Typo in path replacement | Broken reference in one skill | Grep validation: zero matches for `.claude/skills/` and `.claude/templates/` in the scoped templates |
| Translation quality | Unclear instructions for Copilot users | Use Claude Code SKILL.md as canonical English source (already reviewed and accepted) |
| Golden file mismatch | Test failures | Regenerate all 8 profiles after template changes; run full test suite |

### Medium Risk

| Risk | Impact | Mitigation |
| :--- | :--- | :--- |
| GAP-3 path correction: `docs/templates/` vs `resources/templates/` | The story document specifies `docs/templates/` as the target, but the actual files are at `resources/templates/`. Using the wrong path means the reference is still broken. | **Use `resources/templates/` as the target** (verified: files exist there). Flag the story document discrepancy to the Product Owner. Note that the Claude Code SKILL.md counterparts also reference `.claude/templates/` which is a generated path -- in the GitHub context, the templates are not generated into `.github/`, so the reference should point to the source path. |
| `.claude/rules/` -> `.github/instructions/` mapping | Rule file naming conventions differ between `.claude/` and `.github/` contexts. The exact filename needs verification. | Verify that the instructions assembler generates a file named `05-quality-gates.instructions.md` (or similar) before writing the replacement path. |

### Important Scope Observation

The story scopes 12 template files, but the full grep reveals **39 files** in `resources/github-skills-templates/` containing `.claude/` references. The remaining 27 files include:

- `dev/x-dev-implement.md` (4 references)
- `dev/layer-templates.md` (3 references)
- `git-troubleshooting/x-git-push.md` (2 references)
- `infrastructure/*.md` (5 files, ~17 references)
- `knowledge-packs/*.md` (8 files, ~16 references)
- `review/x-review-api.md`, `x-review-events.md`, `x-review-gateway.md`, `x-review-grpc.md` (4 files, ~14 references)
- `testing/*.md` (6 files, ~16 references)

**These 27 out-of-scope files will still have broken `.claude/` paths after STORY-022 is complete.** This should be tracked as a follow-up story. The story document does not address these files, so they must remain untouched per the scope definition.

### No Risk

| Item | Why |
| :--- | :--- |
| Assembler code changes | Not needed -- the assembler correctly generates from templates |
| Test code changes | Not needed -- `github-skills-assembler.test.ts` tests assembler behavior, not template content |
| Coverage impact | Template-only changes; no source code changes affect coverage |

---

## 7. Implementation Order

### Phase 1: GAP-1 Path Fixes (7 non-story files)

Order does not matter; these are independent edits:

1. `dev/x-dev-lifecycle.md` -- 13 path replacements
2. `review/x-review.md` -- 12 path replacements (includes GAP-4 KP table rewrite)
3. `review/x-review-pr.md` -- 6 path replacements (includes `.claude/rules/` -> `.github/instructions/`)
4. `git-troubleshooting/x-ops-troubleshoot.md` -- 2 path replacements
5. `lib/x-lib-task-decomposer.md` -- 4 path replacements
6. `lib/x-lib-group-verifier.md` -- 2 path replacements
7. `lib/x-lib-audit-rules.md` -- 4 path replacements

### Phase 2: GAP-2 + GAP-3 + GAP-5 Translation + Path Fixes (5 story files)

Order: epic-full first (it is the orchestrator that references the others), then the 3 focused skills, then the KP:

1. `story/x-story-epic-full.md` -- Full English rewrite + GAP-3 template paths
2. `story/x-story-create.md` -- Full English rewrite + GAP-3 template path
3. `story/x-story-epic.md` -- Full English rewrite + GAP-3 template path
4. `story/x-story-map.md` -- Full English rewrite + GAP-3 template path
5. `story/story-planning.md` -- Full English rewrite

### Phase 3: Golden File Regeneration

1. Build the project: `npm run build`
2. Regenerate output for all 8 profiles
3. Copy affected golden files
4. Run full test suite: `npm run test:coverage`

### Phase 4: Validation

1. Grep validation: zero matches for `.claude/skills/` in scoped files
2. Grep validation: zero matches for `.claude/templates/` in scoped files
3. All tests passing
4. Coverage >= 95% line, >= 90% branch

---

## 8. Estimated Effort

| Phase | Files | Effort |
| :--- | :--- | :--- |
| Phase 1 (Path fixes) | 7 | Low -- mechanical find-and-replace |
| Phase 2 (Translation) | 5 | Medium -- translation requires careful alignment with Claude Code counterparts |
| Phase 3 (Golden files) | 96 | Low -- automated regeneration |
| Phase 4 (Validation) | — | Low -- grep + test run |

**Total: ~43 replacements across 12 files + 5 full translations + 96 golden file updates**
