# Implementation Plan: STORY-0003-0015 — x-review — Checklist TDD para Review de QA

## Summary

Add 6 TDD checklist items to the QA Engineer specialist in the x-review skill. The QA Engineer currently has 12 items (scoring /24). After this story, it will have 18 items (scoring /36). Additionally, update the QA Knowledge Pack reference paths to include TDD-specific sections from the testing KP. Both source templates and all 24 golden files must be updated. This is a content-only change -- no TypeScript logic is modified.

---

## 1. Affected Layers and Components

| Layer | Component | Impact |
|-------|-----------|--------|
| Resource (content) | `resources/skills-templates/core/x-review/SKILL.md` | **Modify**: update QA checklist line, QA KP path, consolidation table |
| Resource (content) | `resources/github-skills-templates/review/x-review.md` | **Modify**: update QA checklist line, QA KP path, consolidation table |
| Assembler | `src/assembler/skills-assembler.ts` | **No change** (generic `copyTemplateTree` copies the file as-is) |
| Assembler | `src/assembler/github-skills-assembler.ts` | **No change** (reads and renders template as-is) |
| Assembler | `src/assembler/codex-skills-assembler.ts` | **No change** (mirrors `.claude/skills/` to `.agents/skills/` automatically) |
| Golden files (.claude) | `tests/golden/{profile}/.claude/skills/x-review/SKILL.md` | **Regenerate**: 8 files |
| Golden files (.github) | `tests/golden/{profile}/.github/skills/x-review/SKILL.md` | **Regenerate**: 8 files |
| Golden files (.agents) | `tests/golden/{profile}/.agents/skills/x-review/SKILL.md` | **Regenerate**: 8 files |
| Tests (integration) | `tests/node/integration/byte-for-byte.test.ts` | **No change** (test structure unchanged; golden files drive assertions) |

---

## 2. New Files to Create

None. This story modifies existing files only. No new classes, interfaces, or resource files are introduced.

---

## 3. Existing Files to Modify

### 3.1 `resources/skills-templates/core/x-review/SKILL.md` (Source of Truth -- Claude)

Three changes in this file:

#### Change A: QA KP Path (line 104)

**Current:**
```
| QA | `skills/testing/references/testing-philosophy.md`, `skills/testing/references/testing-conventions.md` |
```

**Updated:**
```
| QA | `skills/testing/references/testing-philosophy.md`, `skills/testing/references/testing-conventions.md` (focus on TDD Workflow, Double-Loop TDD, and Transformation Priority Premise sections) |
```

**Rationale:** The QA engineer needs to read the TDD-specific sections (TDD Workflow at line 166, Double-Loop TDD at line 200, TPP at line 235 of `03-testing-philosophy.md`) to properly evaluate the 6 new TDD checklist items.

#### Change B: QA Checklist (line 116)

**Current:**
```
**QA (12 items, /24):** Test exists for each AC, line coverage ≥95%, branch coverage ≥90%, test naming convention, AAA pattern, parametrized tests for data-driven, exception paths tested, no test interdependency, fixtures centralized, unique test data, edge cases, integration tests for DB/API.
```

**Updated:**
```
**QA (18 items, /36):** Test exists for each AC, line coverage ≥95%, branch coverage ≥90%, test naming convention, AAA pattern, parametrized tests for data-driven, exception paths tested, no test interdependency, fixtures centralized, unique test data, edge cases, integration tests for DB/API, commits show test-first pattern, explicit refactoring after green, tests follow TPP progression, no test written after implementation, acceptance tests validate E2E behavior, TDD coverage thresholds maintained.
```

**Details of the 6 new items:**

| # | Item | Evaluation Criteria |
|---|------|-------------------|
| 13 | Commits show test-first pattern | Test precedes implementation in git log (`git log --oneline` shows test commits before or alongside implementation) |
| 14 | Explicit refactoring after green | Refactoring commits exist after green phase, with no behavior change (only structural improvement) |
| 15 | Tests follow TPP progression | Tests progress from simple to complex following Transformation Priority Premise (`{} -> nil -> constant -> scalar -> collection -> ...`) |
| 16 | No test written after implementation | No commits show tests added retroactively after production code |
| 17 | Acceptance tests validate E2E behavior | At least one acceptance/E2E test validates the end-to-end behavior of the feature |
| 18 | TDD coverage thresholds maintained | Coverage meets >=95% line / >=90% branch thresholds through TDD, not through test-after padding |

#### Change C: Consolidation Table (line 141)

**Current:**
```
| QA            | XX/24 | Rejected           |
```

**Updated:**
```
| QA            | XX/36 | Rejected           |
```

### 3.2 `resources/github-skills-templates/review/x-review.md` (Source of Truth -- GitHub)

Three changes in this file, mirroring the Claude template changes:

#### Change A: QA KP Path (line 100)

**Current:**
```
| QA | `.github/skills/testing/SKILL.md` |
```

**Updated:**
```
| QA | `.github/skills/testing/SKILL.md` (focus on TDD Workflow, Double-Loop TDD, and Transformation Priority Premise sections) |
```

#### Change B: QA Checklist (line 112)

**Current:**
```
**QA (12 items, /24):** Test exists for each AC, line coverage >=95%, branch coverage >=90%, test naming convention, AAA pattern, parametrized tests for data-driven, exception paths tested, no test interdependency, fixtures centralized, unique test data, edge cases, integration tests for DB/API.
```

**Updated:**
```
**QA (18 items, /36):** Test exists for each AC, line coverage >=95%, branch coverage >=90%, test naming convention, AAA pattern, parametrized tests for data-driven, exception paths tested, no test interdependency, fixtures centralized, unique test data, edge cases, integration tests for DB/API, commits show test-first pattern, explicit refactoring after green, tests follow TPP progression, no test written after implementation, acceptance tests validate E2E behavior, TDD coverage thresholds maintained.
```

**Note:** The GitHub template uses `>=` instead of `≥` -- this distinction must be preserved.

#### Change C: Consolidation Table (line 137)

**Current:**
```
| QA            | XX/24 | Rejected           |
```

**Updated:**
```
| QA            | XX/36 | Rejected           |
```

### 3.3 Golden Files (24 files -- regenerated, not manually edited)

All 24 golden files must be updated to reflect the changes in the source templates.

| Profile | `.claude/` | `.github/` | `.agents/` |
|---------|-----------|-----------|-----------|
| go-gin | Update | Update | Update |
| java-quarkus | Update | Update | Update |
| java-spring | Update | Update | Update |
| kotlin-ktor | Update | Update | Update |
| python-click-cli | Update | Update | Update |
| python-fastapi | Update | Update | Update |
| rust-axum | Update | Update | Update |
| typescript-nestjs | Update | Update | Update |

**Important character encoding differences between golden file variants:**

| Variant | Dash style | Comparison symbols | Arrow style | Source template |
|---------|-----------|-------------------|-------------|----------------|
| `.claude/` | em-dash (`—`) | `≥` | `→` | Claude template |
| `.agents/` | em-dash (`—`) | `≥` | `→` | Claude template (mirrored from `.claude/`) |
| `.github/` | double-dash (`--`) | `>=` | `->` | GitHub template |

**Generation flow:**
- `.claude/` golden files: Generated by `SkillsAssembler` from `resources/skills-templates/core/x-review/SKILL.md`
- `.github/` golden files: Generated by `GithubSkillsAssembler` from `resources/github-skills-templates/review/x-review.md`
- `.agents/` golden files: Generated by `CodexSkillsAssembler` which mirrors the `.claude/skills/` output (byte-identical to `.claude/` copies)

---

## 4. Dependency Direction Validation

```
resources/skills-templates/core/x-review/SKILL.md   (static content, no code dependency)
                          |
                          v
src/assembler/skills-assembler.ts      (copies template tree, applies TemplateEngine)
                          |
                          v
Output: .claude/skills/x-review/SKILL.md
                          |
                          v
src/assembler/codex-skills-assembler.ts  (mirrors .claude/ -> .agents/)

resources/github-skills-templates/review/x-review.md  (static content)
                          |
                          v
src/assembler/github-skills-assembler.ts  (renders template, writes SKILL.md)
                          |
                          v
Output: .github/skills/x-review/SKILL.md
```

Direction: Content flows outward from resources through assemblers to output. No dependency violations. No TypeScript source files are modified.

---

## 5. Integration Points

| From | To | Mechanism |
|------|----|-----------|
| `resources/skills-templates/core/x-review/SKILL.md` | `SkillsAssembler.copyCoreSkill()` | `copyTemplateTree()` copies entire directory, `TemplateEngine` substitutes placeholders |
| `resources/github-skills-templates/review/x-review.md` | `GithubSkillsAssembler.renderSkill()` | Reads `.md`, applies `engine.replacePlaceholders()`, writes `SKILL.md` |
| `.claude/skills/x-review/SKILL.md` (generated) | `CodexSkillsAssembler.copySkill()` | `fs.copyFileSync()` mirrors to `.agents/skills/` |
| Golden files | `byte-for-byte.test.ts` | `verifyOutput()` compares generated output against golden files byte-for-byte |
| `x-review` skill | `x-dev-lifecycle` skill | `x-dev-lifecycle` references `x-review` during Phase 3 (parallel review). The expanded QA checklist provides additional TDD validation during review. |
| QA KP path | `testing` KP | QA subagent reads `testing-philosophy.md` which contains TDD Workflow (line 166), Double-Loop TDD (line 200), and TPP (line 235) sections |

---

## 6. Database Changes

None. This project is a CLI tool with no database.

---

## 7. API Changes

None. No REST/gRPC endpoints affected.

---

## 8. Event Changes

None. No event-driven components affected.

---

## 9. Configuration Changes

None. No config templates, environment variables, or `setup-config.{profile}.yaml` files need updating. The skill template has no conditional logic gated by config values -- it is unconditionally included as a core skill for all profiles.

---

## 10. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Golden file mismatch breaks byte-for-byte tests | **High** (expected) | **Blocking** | Update all 24 golden files (8 profiles x 3 dirs) to reflect the new QA checklist content |
| Dual copy inconsistency between Claude and GitHub templates | **Medium** | **Blocking** | Apply equivalent content to both templates while preserving encoding differences (`≥` vs `>=`, `—` vs `--`, `→` vs `->`). Verify with diff that the semantic content matches. Review against RULE-001 |
| Existing QA checklist items accidentally modified | **Low** | **High** | Append-only change -- add 6 new items after the existing 12. Verify with `git diff` that existing items are unchanged |
| Item count mismatch (header says 18 but items differ) | **Low** | **Medium** | Count all items in the updated line to confirm exactly 18 comma-separated items |
| Consolidation table score not updated | **Low** | **Medium** | Change `XX/24` to `XX/36` in the consolidation table section |
| Template placeholder issues | **None** | **None** | New content contains no profile-specific placeholders (`{{...}}`). Static Markdown passes through `TemplateEngine` unchanged |
| Backward compatibility regression | **None** | **None** | Additive change only -- all existing 12 items preserved. 6 new items appended |

---

## 11. Implementation Order

1. **Edit Claude source template:** `resources/skills-templates/core/x-review/SKILL.md`
   - Update QA KP path (line 104): add TDD section focus instruction
   - Update QA checklist (line 116): change `12 items, /24` to `18 items, /36` and append 6 TDD items
   - Update consolidation table (line 141): change `XX/24` to `XX/36`

2. **Edit GitHub source template:** `resources/github-skills-templates/review/x-review.md`
   - Update QA KP path (line 100): add TDD section focus instruction
   - Update QA checklist (line 112): change `12 items, /24` to `18 items, /36` and append 6 TDD items (using `>=` not `≥`)
   - Update consolidation table (line 137): change `XX/24` to `XX/36`

3. **Verify consistency:** Diff the QA-related content of both templates to confirm RULE-001 compliance (semantic equivalence with expected encoding differences)

4. **Update golden files (`.claude/` and `.agents/` -- 16 files):**
   All 8 profiles x 2 dirs (`.claude/` and `.agents/` are byte-identical, both sourced from Claude template):
   - Update QA KP path (line 104): add TDD section focus instruction
   - Update QA checklist (line 116): change `12 items, /24` to `18 items, /36` and append 6 TDD items (using `≥`)
   - Update consolidation table (line 141): change `XX/24` to `XX/36`

5. **Update golden files (`.github/` -- 8 files):**
   All 8 profiles:
   - Update QA KP path (line 100): add TDD section focus instruction
   - Update QA checklist (line 112): change `12 items, /24` to `18 items, /36` and append 6 TDD items (using `>=`)
   - Update consolidation table (line 137): change `XX/24` to `XX/36`

6. **Run full test suite:** `npx vitest run` to confirm byte-for-byte parity across all 24 golden files

7. **Verify coverage:** Ensure >= 95% line, >= 90% branch coverage is maintained (no TypeScript code changed, so coverage should be unaffected)

---

## 12. Content Specification

### 6 New TDD Checklist Items

These 6 items are appended to the existing 12 QA items, separated by commas:

| # | Short Name | Full Item Text (as appears in checklist) | Scoring Criteria |
|---|-----------|----------------------------------------|-----------------|
| 13 | test-first commits | `commits show test-first pattern` | 0: no evidence of test-first; 1: some commits show pattern but not all; 2: all test commits precede or accompany implementation |
| 14 | refactoring after green | `explicit refactoring after green` | 0: no refactoring commits; 1: refactoring exists but mixes behavior changes; 2: clean refactoring commits with no behavior change |
| 15 | TPP progression | `tests follow TPP progression` | 0: tests jump to complex cases; 1: partial progression; 2: clear simple-to-complex test ordering following TPP |
| 16 | no test-after | `no test written after implementation` | 0: tests clearly added retroactively; 1: some ambiguous ordering; 2: all tests precede or accompany their implementation |
| 17 | acceptance tests | `acceptance tests validate E2E behavior` | 0: no acceptance tests; 1: acceptance tests exist but incomplete; 2: acceptance tests fully validate end-to-end behavior |
| 18 | TDD coverage | `TDD coverage thresholds maintained` | 0: coverage below thresholds; 1: coverage meets thresholds but via test-after; 2: coverage meets thresholds through TDD process |

### Updated QA Line (Claude template variant with `≥`)

```
**QA (18 items, /36):** Test exists for each AC, line coverage ≥95%, branch coverage ≥90%, test naming convention, AAA pattern, parametrized tests for data-driven, exception paths tested, no test interdependency, fixtures centralized, unique test data, edge cases, integration tests for DB/API, commits show test-first pattern, explicit refactoring after green, tests follow TPP progression, no test written after implementation, acceptance tests validate E2E behavior, TDD coverage thresholds maintained.
```

### Updated QA Line (GitHub template variant with `>=`)

```
**QA (18 items, /36):** Test exists for each AC, line coverage >=95%, branch coverage >=90%, test naming convention, AAA pattern, parametrized tests for data-driven, exception paths tested, no test interdependency, fixtures centralized, unique test data, edge cases, integration tests for DB/API, commits show test-first pattern, explicit refactoring after green, tests follow TPP progression, no test written after implementation, acceptance tests validate E2E behavior, TDD coverage thresholds maintained.
```

### Updated QA KP Path (Claude template variant)

```
| QA | `skills/testing/references/testing-philosophy.md`, `skills/testing/references/testing-conventions.md` (focus on TDD Workflow, Double-Loop TDD, and Transformation Priority Premise sections) |
```

### Updated QA KP Path (GitHub template variant)

```
| QA | `.github/skills/testing/SKILL.md` (focus on TDD Workflow, Double-Loop TDD, and Transformation Priority Premise sections) |
```

### Updated Consolidation Table Row

```
| QA            | XX/36 | Rejected           |
```

---

## 13. Exact Change Locations Per File

### Source Templates

| File | Line | Change |
|------|------|--------|
| `resources/skills-templates/core/x-review/SKILL.md` | 104 | QA KP path: add TDD focus instruction |
| `resources/skills-templates/core/x-review/SKILL.md` | 116 | QA checklist: 12->18 items, /24->/36, append 6 items |
| `resources/skills-templates/core/x-review/SKILL.md` | 141 | Consolidation: XX/24 -> XX/36 |
| `resources/github-skills-templates/review/x-review.md` | 100 | QA KP path: add TDD focus instruction |
| `resources/github-skills-templates/review/x-review.md` | 112 | QA checklist: 12->18 items, /24->/36, append 6 items |
| `resources/github-skills-templates/review/x-review.md` | 137 | Consolidation: XX/24 -> XX/36 |

### Golden Files (`.claude/` and `.agents/` -- 16 files, same line numbers)

| Line | Change |
|------|--------|
| 104 | QA KP path: add TDD focus instruction |
| 116 | QA checklist: 12->18 items, /24->/36, append 6 items (using `≥`) |
| 141 | Consolidation: XX/24 -> XX/36 |

Profiles: go-gin, java-quarkus, java-spring, kotlin-ktor, python-click-cli, python-fastapi, rust-axum, typescript-nestjs

### Golden Files (`.github/` -- 8 files, same line numbers)

| Line | Change |
|------|--------|
| 100 | QA KP path: add TDD focus instruction |
| 112 | QA checklist: 12->18 items, /24->/36, append 6 items (using `>=`) |
| 137 | Consolidation: XX/24 -> XX/36 |

Profiles: go-gin, java-quarkus, java-spring, kotlin-ktor, python-click-cli, python-fastapi, rust-axum, typescript-nestjs

---

## 14. Preservation Checklist

- [ ] All existing 12 QA items unchanged (text and order preserved)
- [ ] All other 7 engineer checklists unchanged (Security, Performance, Database, Observability, DevOps, API, Event)
- [ ] Parallel review pattern unchanged (Phase 2 subagent launch mechanism intact)
- [ ] Phase 3 consolidation logic unchanged (only QA score denominator changes)
- [ ] Phase 4 story generation unchanged
- [ ] YAML frontmatter unchanged in both templates
- [ ] Character encoding preserved per variant (`≥` in Claude/agents, `>=` in GitHub)
- [ ] No TypeScript source code modified
