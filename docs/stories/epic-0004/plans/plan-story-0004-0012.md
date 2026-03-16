# Implementation Plan — story-0004-0012

## Performance Baseline Tracking

### 1. Summary

This story creates a `_TEMPLATE-PERFORMANCE-BASELINE.md` template in `resources/templates/` and integrates performance baseline tracking into the feature lifecycle. The template provides a structured Measurement Guide and an incremental Baselines table for recording before/after performance metrics (latency p50/p95/p99, throughput, memory, startup time). A prompt is added to the lifecycle documentation phase (introduced by story-0004-0005) recommending developers record baselines for features affecting the request path or infrastructure.

**Key constraint:** This story is blocked by story-0004-0005 (Documentation Phase in `x-dev-lifecycle`). The doc phase does not exist yet in the lifecycle -- the current `x-dev-lifecycle` SKILL.md has 8 phases (0-7) with no documentation phase. This plan assumes story-0004-0005 is completed first, introducing a Phase 3 -- Documentation with interface-based dispatch and changelog generation.

**Nature of changes:** This is a content-only story. No TypeScript pipeline code or assembler logic changes. The template is a static Markdown file added to `resources/templates/`. The lifecycle integration is a text edit to the `x-dev-lifecycle` SKILL.md adding a performance baseline prompt to the documentation phase. Skills reference the template at runtime (AI agents read it when invoked).

---

### 2. Architecture — How Template Files Work in ia-dev-env

```
resources/templates/_TEMPLATE-PERFORMANCE-BASELINE.md   <-- SOURCE OF TRUTH
        |
        | Referenced at runtime by skills/agents (AI reads the file)
        | NOT copied by the pipeline to .claude/ or .github/
        |
        v
Skills/Agents reference: "Read resources/templates/_TEMPLATE-PERFORMANCE-BASELINE.md"
  or: "Read .claude/templates/_TEMPLATE-PERFORMANCE-BASELINE.md" (in generated projects)
```

Key properties of `resources/templates/_TEMPLATE*.md` files:
- They live in `resources/templates/` as the source of truth (RULE-002).
- They are NOT processed by any assembler -- they are NOT copied to `.claude/` or `.github/` output directories.
- They are referenced by skill SKILL.md files and agent templates as "read this template before generating artifacts."
- They use `{{PLACEHOLDER}}` markers that are NOT resolved during generation -- they are runtime markers for AI agents to fill contextually.
- The existing templates (`_TEMPLATE.md`, `_TEMPLATE-EPIC.md`, `_TEMPLATE-STORY.md`, `_TEMPLATE-IMPLEMENTATION-MAP.md`) follow this exact pattern.

The lifecycle integration modifies the `x-dev-lifecycle` SKILL.md to add a performance baseline prompt in the documentation phase. This follows the same dual-copy pattern as other lifecycle changes:

```
resources/skills-templates/core/x-dev-lifecycle/SKILL.md   <-- SOURCE OF TRUTH (Claude)
        |
        | copyTemplateTree (no {{placeholders}} resolved)
        | via: src/assembler/skills-assembler.ts :: assembleCore()
        v
{outputDir}/.claude/skills/x-dev-lifecycle/SKILL.md        <-- pipeline output (.claude)
        |
        | mirror
        | via: src/assembler/codex-skills-assembler.ts
        v
{outputDir}/.agents/skills/x-dev-lifecycle/SKILL.md        <-- pipeline output (.agents)

---

resources/github-skills-templates/dev/x-dev-lifecycle.md   <-- SOURCE OF TRUTH (GitHub)
        |
        | renderSkill
        | via: src/assembler/github-skills-assembler.ts
        v
{outputDir}/.github/skills/x-dev-lifecycle/SKILL.md       <-- pipeline output (.github)
```

---

### 3. Affected Layers and Components

| Layer | Component | Impact |
|-------|-----------|--------|
| Resources (templates) | `resources/templates/_TEMPLATE-PERFORMANCE-BASELINE.md` | **NEW** -- performance baseline template |
| Resources (Claude source) | `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` | **MODIFIED** -- add performance baseline prompt in doc phase |
| Resources (GitHub source) | `resources/github-skills-templates/dev/x-dev-lifecycle.md` | **MODIFIED** -- parallel changes with GitHub-specific references |
| Golden files (.claude) | `tests/golden/{profile}/.claude/skills/x-dev-lifecycle/SKILL.md` | **MUST UPDATE** -- 8 files |
| Golden files (.agents) | `tests/golden/{profile}/.agents/skills/x-dev-lifecycle/SKILL.md` | **MUST UPDATE** -- 8 files |
| Golden files (.github) | `tests/golden/{profile}/.github/skills/x-dev-lifecycle/SKILL.md` | **MUST UPDATE** -- 8 files |

**Total: 1 new template + 2 source templates modified + 24 golden files = 27 files.**

---

### 4. New Files to Create

#### 4.1 `resources/templates/_TEMPLATE-PERFORMANCE-BASELINE.md`

This is the core deliverable. Structure per the story requirements:

```markdown
# Performance Baselines

## Measurement Guide

### Metrics

| Metric | Description | How to Measure |
| :--- | :--- | :--- |
| `latency_p50` | 50th percentile response time | ... |
| `latency_p95` | 95th percentile response time | ... |
| `latency_p99` | 99th percentile response time | ... |
| `throughput_rps` | Requests per second at stable load | ... |
| `memory_mb` | RSS memory footprint in MB | ... |
| `startup_ms` | Application cold-start time in ms | ... |

### Measurement Conditions

- ... (environment, load, warm-up, repetitions)

### Tools by Stack

| Stack | Latency/Throughput | Memory | Startup |
| :--- | :--- | :--- | :--- |
| {{LANGUAGE}} / {{FRAMEWORK}} | ... | ... | ... |

## Baselines

| Feature/Story ID | Date | Metric | Before | After | Delta | Notes |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| _example-0001_ | _2025-01-15_ | _latency_p95_ | _45ms_ | _52ms_ | _+15.6%_ | _Added validation middleware_ |

### Delta Interpretation

| Delta Range | Severity | Action |
| :--- | :--- | :--- |
| <= +10% | Acceptable | No action needed |
| +10% to +25% | Warning | Document reason, consider optimization |
| > +25% | Investigation | Mandatory investigation and optimization plan |
```

The template uses `{{LANGUAGE}}` and `{{FRAMEWORK}}` placeholders that are NOT resolved by the pipeline (they remain as-is for AI agents to interpret contextually).

**Note on delta calculation and regression alerts (story sections 3.3):** The delta calculation formula and threshold warnings are documented in the template itself as static guidance. They are NOT implemented as runtime code -- the AI agent reading the template applies these rules when helping developers record baselines. This is consistent with how all other `_TEMPLATE*.md` files work (they are instruction documents, not executable templates).

---

### 5. Existing Files to Modify

#### 5.1 `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`

**Prerequisite:** Story-0004-0005 must be implemented first. That story adds a "Phase 3 -- Documentation" section with interface dispatch and changelog generation. This story adds a performance baseline prompt to that phase.

**Change:** Add a performance baseline subsection to the documentation phase, after the interface generators and changelog entry, but before the phase completion marker. Content:

```markdown
### Performance Baseline (Recommended)

If the implemented feature affects the request path, startup, or memory footprint:

1. Read `resources/templates/_TEMPLATE-PERFORMANCE-BASELINE.md` for measurement guide
2. Record "before" metrics (prior to the feature branch)
3. Record "after" metrics (with the feature branch)
4. Append a row to `docs/performance/baselines.md`
5. If Delta > 10%, add a WARNING note
6. If Delta > 25%, add an INVESTIGATION note with optimization plan

This step is recommended but not mandatory. Skip does not block the phase.
```

**For the Claude copy** (`resources/skills-templates/core/x-dev-lifecycle/SKILL.md`):
- Path reference: `resources/templates/_TEMPLATE-PERFORMANCE-BASELINE.md`
- Output file: `docs/performance/baselines.md`

#### 5.2 `resources/github-skills-templates/dev/x-dev-lifecycle.md`

**Parallel change** to the GitHub copy with GitHub-specific path references:

**For the GitHub copy:**
- Path reference: `resources/templates/_TEMPLATE-PERFORMANCE-BASELINE.md` (same -- templates are project-level, not IDE-specific)
- Output file: `docs/performance/baselines.md` (same)

The content is semantically identical to the Claude copy. Path differences follow the systematic pattern documented in plan-story-0003-0016 Section 14.

---

### 6. Dependency Direction Validation

This change does not introduce any new code dependencies. The analysis:

| Component | Depends On | Valid? |
|-----------|-----------|--------|
| `_TEMPLATE-PERFORMANCE-BASELINE.md` | Nothing (static Markdown) | Yes |
| `x-dev-lifecycle` SKILL.md | References the template path | Yes (runtime AI reference, not code import) |
| Pipeline assemblers | No changes | N/A |
| Golden files | Mirror source templates | Yes (output artifacts) |

No dependency direction violations. The template is a leaf node with no dependencies.

---

### 7. Integration Points

| Integration Point | Description |
|------------------|-------------|
| `x-dev-lifecycle` Phase 3 (Documentation) | Performance baseline prompt added as optional step |
| `resources/templates/` directory | New template joins existing `_TEMPLATE*.md` family |
| `docs/performance/baselines.md` | Output artifact created by developers following the template |
| Story-planning KP | May optionally reference the template for stories that modify performance-sensitive paths |

The `docs/performance/baselines.md` file is NOT generated by the pipeline. It is created manually by developers following the template guidance during the lifecycle documentation phase. This is consistent with how other `docs/` artifacts work (e.g., `docs/adr/` ADRs are created by developers, not by the pipeline).

---

### 8. Database Changes

None. This project has no database.

---

### 9. API Changes

None. No CLI commands, endpoints, or interfaces are modified.

---

### 10. Event Changes

None. No event-driven components affected.

---

### 11. Configuration Changes

None. No `ProjectConfig` model changes, no new config fields, no `setup-config.*.yaml` changes. The template uses `{{LANGUAGE}}` and `{{FRAMEWORK}}` as runtime markers for AI agents, not pipeline-resolved placeholders.

---

### 12. Golden File Impact

#### 12.1 x-dev-lifecycle Skill -- 24 Golden Files

**IMPORTANT:** These golden files will ALSO be affected by story-0004-0005 (documentation phase). Since story-0004-0005 is a prerequisite, the golden files will already contain the doc phase when this story is implemented. This story adds content to that phase.

**Golden files (.claude) -- 8 files:**

| # | File |
|---|------|
| 1 | `tests/golden/go-gin/.claude/skills/x-dev-lifecycle/SKILL.md` |
| 2 | `tests/golden/java-quarkus/.claude/skills/x-dev-lifecycle/SKILL.md` |
| 3 | `tests/golden/java-spring/.claude/skills/x-dev-lifecycle/SKILL.md` |
| 4 | `tests/golden/kotlin-ktor/.claude/skills/x-dev-lifecycle/SKILL.md` |
| 5 | `tests/golden/python-click-cli/.claude/skills/x-dev-lifecycle/SKILL.md` |
| 6 | `tests/golden/python-fastapi/.claude/skills/x-dev-lifecycle/SKILL.md` |
| 7 | `tests/golden/rust-axum/.claude/skills/x-dev-lifecycle/SKILL.md` |
| 8 | `tests/golden/typescript-nestjs/.claude/skills/x-dev-lifecycle/SKILL.md` |

**Golden files (.agents) -- 8 files, identical to Claude source:**

| # | File |
|---|------|
| 9 | `tests/golden/go-gin/.agents/skills/x-dev-lifecycle/SKILL.md` |
| 10 | `tests/golden/java-quarkus/.agents/skills/x-dev-lifecycle/SKILL.md` |
| 11 | `tests/golden/java-spring/.agents/skills/x-dev-lifecycle/SKILL.md` |
| 12 | `tests/golden/kotlin-ktor/.agents/skills/x-dev-lifecycle/SKILL.md` |
| 13 | `tests/golden/python-click-cli/.agents/skills/x-dev-lifecycle/SKILL.md` |
| 14 | `tests/golden/python-fastapi/.agents/skills/x-dev-lifecycle/SKILL.md` |
| 15 | `tests/golden/rust-axum/.agents/skills/x-dev-lifecycle/SKILL.md` |
| 16 | `tests/golden/typescript-nestjs/.agents/skills/x-dev-lifecycle/SKILL.md` |

**Golden files (.github) -- 8 files, from GitHub source:**

| # | File |
|---|------|
| 17 | `tests/golden/go-gin/.github/skills/x-dev-lifecycle/SKILL.md` |
| 18 | `tests/golden/java-quarkus/.github/skills/x-dev-lifecycle/SKILL.md` |
| 19 | `tests/golden/java-spring/.github/skills/x-dev-lifecycle/SKILL.md` |
| 20 | `tests/golden/kotlin-ktor/.github/skills/x-dev-lifecycle/SKILL.md` |
| 21 | `tests/golden/python-click-cli/.github/skills/x-dev-lifecycle/SKILL.md` |
| 22 | `tests/golden/python-fastapi/.github/skills/x-dev-lifecycle/SKILL.md` |
| 23 | `tests/golden/rust-axum/.github/skills/x-dev-lifecycle/SKILL.md` |
| 24 | `tests/golden/typescript-nestjs/.github/skills/x-dev-lifecycle/SKILL.md` |

#### 12.2 Golden File Update Strategy

```bash
# After editing the source templates:
CLAUDE_SRC="resources/skills-templates/core/x-dev-lifecycle/SKILL.md"
GITHUB_SRC="resources/github-skills-templates/dev/x-dev-lifecycle.md"
PROFILES=(go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs)

for profile in "${PROFILES[@]}"; do
  cp "$CLAUDE_SRC" "tests/golden/$profile/.claude/skills/x-dev-lifecycle/SKILL.md"
  cp "$CLAUDE_SRC" "tests/golden/$profile/.agents/skills/x-dev-lifecycle/SKILL.md"
  cp "$GITHUB_SRC" "tests/golden/$profile/.github/skills/x-dev-lifecycle/SKILL.md"
done
```

Verification:
```bash
npx vitest run tests/node/integration/byte-for-byte.test.ts
```

---

### 13. Files to Modify (Complete List)

#### 13.1 New Files (1 file)

| # | File | Description |
|---|------|-------------|
| 1 | `resources/templates/_TEMPLATE-PERFORMANCE-BASELINE.md` | Performance baseline template with Measurement Guide, Baselines table, and delta interpretation |

#### 13.2 Source of Truth (2 files)

| # | File | Change |
|---|------|--------|
| 2 | `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` | Add performance baseline prompt in doc phase |
| 3 | `resources/github-skills-templates/dev/x-dev-lifecycle.md` | Parallel changes with GitHub-specific paths |

#### 13.3 Golden Files (24 files)

Listed in Section 12.1.

#### 13.4 Files Explicitly UNCHANGED

| File | Reason |
|------|--------|
| `src/assembler/skills-assembler.ts` | No pipeline logic changes -- template is not assembled |
| `src/assembler/pipeline.ts` | No new assembler needed |
| `src/template-engine.ts` | Template uses `{{}}` runtime markers, not pipeline-resolved |
| `src/models.ts` | No new config fields |
| `resources/config-templates/setup-config.*.yaml` | No config schema changes |
| `resources/skills-templates/knowledge-packs/story-planning/SKILL.md` | Template reference is optional (developers read it directly) |
| `tests/node/integration/byte-for-byte.test.ts` | Test infrastructure unchanged |
| Other `_TEMPLATE*.md` files | Not affected by this story |

---

### 14. RULE-001 Compliance (Dual Copy Consistency)

| Dimension | Claude Source | GitHub Source |
|-----------|-------------|--------------|
| File path | `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` | `resources/github-skills-templates/dev/x-dev-lifecycle.md` |
| Performance baseline prompt | Identical content | Identical content |
| Template path reference | `resources/templates/_TEMPLATE-PERFORMANCE-BASELINE.md` | `resources/templates/_TEMPLATE-PERFORMANCE-BASELINE.md` |
| Output path reference | `docs/performance/baselines.md` | `docs/performance/baselines.md` |

Both copies reference the same template and output paths because these are project-level paths, not IDE-specific. The performance baseline content must be **semantically identical** in both copies.

**Note:** The `_TEMPLATE-PERFORMANCE-BASELINE.md` file itself does NOT have a dual copy. It lives only in `resources/templates/`. This is consistent with existing templates (`_TEMPLATE-EPIC.md`, `_TEMPLATE-STORY.md`, etc.) which also exist only in `resources/templates/`.

---

### 15. Backward Compatibility Assessment

This change is **purely additive**:
- The new `_TEMPLATE-PERFORMANCE-BASELINE.md` does not affect existing templates or generation.
- The lifecycle modification adds an optional (recommended, not mandatory) step to the documentation phase.
- Projects without `docs/performance/` are unaffected -- the directory is created on first use.
- The performance baseline prompt explicitly states "skip does not block the phase."
- No existing phases, skills, or templates are removed or modified in breaking ways.
- All 8 profiles produce the same lifecycle output (the skill has no profile-specific content).

---

### 16. Risk Assessment

| Risk | Severity | Probability | Mitigation |
|------|----------|-------------|------------|
| Story-0004-0005 not implemented yet | HIGH | CERTAIN | This story cannot be implemented until story-0004-0005 introduces the documentation phase. Plan assumes 0005 is done first. |
| Golden file mismatch after edit | HIGH | LOW | Mechanical copy script (Section 12.2) eliminates drift. Run byte-for-byte tests immediately. |
| Inconsistency between Claude and GitHub lifecycle copies | HIGH | MEDIUM | After editing both, diff them to verify only systematic differences (YAML frontmatter, KP paths). Performance baseline content must be identical. |
| Template `{{PLACEHOLDER}}` confusion with pipeline placeholders | LOW | LOW | The `{{}}` markers in the template are NOT resolved by the pipeline. This is documented in the template and consistent with existing templates. The pipeline uses `{single_brace}` for its own placeholders. |
| `docs/performance/` directory not auto-created | LOW | LOW | The template instructs developers to create the file. The lifecycle prompt can include `mkdir -p docs/performance/` as a step. |
| Performance metrics not measurable for all stacks | MEDIUM | MEDIUM | The Measurement Guide includes a "Tools by Stack" table with placeholder rows. Each stack's tools are suggestions, not requirements. |

---

### 17. Implementation Order

| Step | Action | Verification |
|------|--------|-------------|
| 1 | Verify story-0004-0005 is completed (doc phase exists in lifecycle) | Inspect `x-dev-lifecycle` SKILL.md for Phase 3 -- Documentation |
| 2 | Write failing test: verify `resources/templates/_TEMPLATE-PERFORMANCE-BASELINE.md` exists with required sections | Test fails (RED) |
| 3 | Create `resources/templates/_TEMPLATE-PERFORMANCE-BASELINE.md` with Measurement Guide, Baselines table, Delta Interpretation | Test passes (GREEN) |
| 4 | Write failing test: verify `x-dev-lifecycle` SKILL.md contains performance baseline prompt in doc phase | Test fails (RED) |
| 5 | Edit Claude source: add performance baseline prompt to doc phase | Test passes (GREEN) |
| 6 | Edit GitHub source: add parallel performance baseline prompt | Diff both sources |
| 7 | Copy Claude source to 16 golden files (.claude + .agents) | Script execution |
| 8 | Copy GitHub source to 8 golden files (.github) | Script execution |
| 9 | Run byte-for-byte test suite | All assertions pass (GREEN) |
| 10 | Run full test suite | All tests pass, coverage >= 95% line / >= 90% branch |

---

### 18. Out of Scope

- Implementing the documentation phase itself (story-0004-0005)
- Runtime delta calculation code or regression alert automation
- Pipeline changes to auto-generate `docs/performance/baselines.md`
- Modifications to `ProjectConfig` or config templates
- Modifying other `_TEMPLATE*.md` files
- Creating a new assembler for performance-related artifacts
- Adding performance baseline tracking to the `x-review` or `x-review-pr` skills
- Implementing benchmarking tools or CI integration for performance testing
