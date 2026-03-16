# Task Breakdown: story-0004-0010 — Event-Driven/WebSocket Documentation Generator

**Story:** [story-0004-0010](../story-0004-0010.md)
**Plan:** [plan-story-0004-0010](./plan-story-0004-0010.md)
**Date:** 2026-03-15
**Mode:** Template-only (Markdown changes + golden file regeneration, no TypeScript code)

## Summary

This story adds a detailed **Event-Driven Documentation Generator** subagent prompt to Phase 3 (Documentation) of the `x-dev-lifecycle` SKILL.md template. The dispatch table in Phase 3 already references `story-0004-0010` on line 200 — this story provides the full generator prompt that the AI agent executes when `websocket`, `event-consumer`, or `event-producer` interfaces are detected.

The generator prompt instructs the AI to scan event definitions and produce `docs/api/event-catalog.md` containing: topics overview table, per-event sections with payload schemas, Mermaid sequence diagrams for event flows, CloudEvents envelope details, and WebSocket channel documentation.

**Files modified (Source of Truth):**
1. `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` (Claude template)
2. `resources/github-skills-templates/dev/x-dev-lifecycle.md` (GitHub template)

**Files regenerated (Golden Files):**
3-26. `tests/golden/{profile}/{.agents,.claude,.github}/skills/x-dev-lifecycle/SKILL.md` (8 profiles x 3 variants)

---

## Task Dependency Graph

```
TASK-1 ──> TASK-2 ──> TASK-3 ──> TASK-4 ──> TASK-5
                                              │
                                              └──> TASK-6
```

---

## TASK-1: Write the Event-Driven Documentation Generator section content

**Description:** Draft the full Markdown subagent prompt for the Event-Driven Documentation Generator. This is the detailed instruction block that the AI agent follows when `websocket`, `event-consumer`, or `event-producer` is detected in the project's `interfaces` list. The content is authored as standalone Markdown before insertion into the lifecycle template.

The section must cover all requirements from the story's acceptance criteria and data contract:

1. **Trigger condition:** Invoke when `interfaces` contains `websocket`, `event-consumer`, or `event-producer` (NOT `kafka` — that is a `broker` value, not an interface type)
2. **Scanning instructions:** Scan project source for event definitions — producers, consumers, event schemas, topic/channel definitions, message handlers
3. **Output file:** `docs/api/event-catalog.md`
4. **Required output sections:**
   - `# Event Catalog` (H1 title)
   - `## Topics Overview` — table with columns: Topic/Channel, Events, Partitioning/Routing
   - `## Event: {EventName}` — one H2 per event with:
     - Topic/Channel name
     - Producer service
     - Consumer services (list)
     - Payload schema table: Field, Type, Required, Description
     - Headers table (if applicable): Header, Type, Description
   - `## Event Flows` — Mermaid `sequenceDiagram` diagrams showing Producer -> Broker -> Consumer(s)
5. **Protocol differentiation:**
   - For `event-consumer`/`event-producer` with `broker: kafka`: document topics, partition keys, consumer groups, offset management
   - For `websocket`: document channels, message types, connection lifecycle, namespace conventions
   - For mixed (both): unified catalog with separate sections per protocol
6. **CloudEvents:** Include CloudEvents envelope metadata (type, source, specversion, id) if applicable
7. **Schema versioning:** Document backward compatibility notes and schema evolution strategy
8. **Reference reads:** Instruct the subagent to read:
   - `skills/protocols/references/event-driven-conventions.md`
   - `skills/protocols/references/websocket-conventions.md`
   - `skills/protocols/references/messaging-conventions.md`
   - The implementation plan at `docs/stories/epic-XXXX/plans/plan-story-XXXX-YYYY.md`
   - Event schema design at `docs/stories/epic-XXXX/plans/events-story-XXXX-YYYY.md` (if produced by Phase 1D)

**Acceptance criteria mapping:**
- Gherkin scenario 1 (catalog created for Kafka project) -> trigger condition + output file path
- Gherkin scenario 2 (topics table) -> Topics Overview table
- Gherkin scenario 3 (payload schema) -> per-event payload schema table
- Gherkin scenario 4 (Mermaid diagram) -> Event Flows section
- Gherkin scenario 5 (skip for non-event) -> trigger condition excludes `rest`, `cli`, `grpc`, `graphql`
- Gherkin scenario 6 (WebSocket variant) -> protocol differentiation block

**Deliverable:** The complete Markdown block ready for insertion.

**Depends On:** None
**Parallel:** no (must be completed before insertion)

---

## TASK-2: Insert the Event-Driven Generator section into the Claude lifecycle template

**Description:** Insert the Event-Driven Documentation Generator section (authored in TASK-1) into `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`. The section is placed **after** the Phase 3 dispatch table (line 200, which already contains the `story-0004-0010` reference) and **before** Phase 4 (line 210).

**Placement:** The new section goes between the end of the Phase 3 dispatch list (line 208: `Architecture docs -> docs/architecture/`) and the start of Phase 4 (line 210: `## Phase 4 — Parallel Review`). It should be a subsection within Phase 3, using an H3 heading (`### Event-Driven Documentation Generator`) to indicate it is a generator dispatched by Phase 3.

**Files to modify:**
- `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`

**Validation criteria:**
- The Claude template contains `### Event-Driven Documentation Generator` as a subsection within Phase 3
- The section appears AFTER line 200 (`Event-Driven doc generator (story-0004-0010)`) and BEFORE `## Phase 4`
- The section contains all required content markers:
  - `docs/api/event-catalog.md` (output path)
  - `## Topics Overview` (or reference to this heading in the output)
  - `## Event: {EventName}` (or equivalent per-event section reference)
  - `sequenceDiagram` (Mermaid diagram type)
  - `event-consumer`, `event-producer`, `websocket` (trigger interface types)
  - `CloudEvents` (envelope standard)
  - `skills/protocols/references/event-driven-conventions.md` (knowledge pack reference)
- The section does NOT break the existing Phase 3 dispatch structure (items 1-6 remain intact)
- Phase 4 heading (`## Phase 4 — Parallel Review`) immediately follows the new section

**Depends On:** TASK-1
**Parallel:** no

---

## TASK-3: Mirror the section into the GitHub template (RULE-001 dual copy)

**Description:** Apply the same Event-Driven Documentation Generator section to the GitHub Copilot template at `resources/github-skills-templates/dev/x-dev-lifecycle.md`, maintaining RULE-001 dual copy consistency. The GitHub template has an identical Phase 3 structure (lines 187-208), so the insertion point is the same: after the dispatch table, before Phase 4.

**Files to modify:**
- `resources/github-skills-templates/dev/x-dev-lifecycle.md`

**Validation criteria:**
- The GitHub template contains `### Event-Driven Documentation Generator` subsection within Phase 3
- The section content is identical to the Claude template version (TASK-2)
- The section placement mirrors the Claude template: after Phase 3 dispatch table, before `## Phase 4`
- Side-by-side diff between the two templates shows zero content divergence in the Event-Driven Generator section (accounting for any structural differences in surrounding content like YAML frontmatter)

**Depends On:** TASK-2
**Parallel:** no

---

## TASK-4: Regenerate golden files for all 8 profiles

**Description:** Regenerate the golden files for all 8 profiles to reflect the updated `x-dev-lifecycle/SKILL.md` template. Since the `x-dev-lifecycle` template uses no profile-specific placeholder substitution, all golden copies should be byte-for-byte identical to each other (and to the source template with its YAML frontmatter intact).

**Files to regenerate (24 files = 8 profiles x 3 variants):**

| Profile | .agents/ | .claude/ | .github/ |
|---------|----------|----------|----------|
| go-gin | tests/golden/go-gin/.agents/skills/x-dev-lifecycle/SKILL.md | tests/golden/go-gin/.claude/skills/x-dev-lifecycle/SKILL.md | tests/golden/go-gin/.github/skills/x-dev-lifecycle/SKILL.md |
| java-quarkus | tests/golden/java-quarkus/.agents/skills/x-dev-lifecycle/SKILL.md | tests/golden/java-quarkus/.claude/skills/x-dev-lifecycle/SKILL.md | tests/golden/java-quarkus/.github/skills/x-dev-lifecycle/SKILL.md |
| java-spring | tests/golden/java-spring/.agents/skills/x-dev-lifecycle/SKILL.md | tests/golden/java-spring/.claude/skills/x-dev-lifecycle/SKILL.md | tests/golden/java-spring/.github/skills/x-dev-lifecycle/SKILL.md |
| kotlin-ktor | tests/golden/kotlin-ktor/.agents/skills/x-dev-lifecycle/SKILL.md | tests/golden/kotlin-ktor/.claude/skills/x-dev-lifecycle/SKILL.md | tests/golden/kotlin-ktor/.github/skills/x-dev-lifecycle/SKILL.md |
| python-click-cli | tests/golden/python-click-cli/.agents/skills/x-dev-lifecycle/SKILL.md | tests/golden/python-click-cli/.claude/skills/x-dev-lifecycle/SKILL.md | tests/golden/python-click-cli/.github/skills/x-dev-lifecycle/SKILL.md |
| python-fastapi | tests/golden/python-fastapi/.agents/skills/x-dev-lifecycle/SKILL.md | tests/golden/python-fastapi/.claude/skills/x-dev-lifecycle/SKILL.md | tests/golden/python-fastapi/.github/skills/x-dev-lifecycle/SKILL.md |
| rust-axum | tests/golden/rust-axum/.agents/skills/x-dev-lifecycle/SKILL.md | tests/golden/rust-axum/.claude/skills/x-dev-lifecycle/SKILL.md | tests/golden/rust-axum/.github/skills/x-dev-lifecycle/SKILL.md |
| typescript-nestjs | tests/golden/typescript-nestjs/.agents/skills/x-dev-lifecycle/SKILL.md | tests/golden/typescript-nestjs/.claude/skills/x-dev-lifecycle/SKILL.md | tests/golden/typescript-nestjs/.github/skills/x-dev-lifecycle/SKILL.md |

**Regeneration method:** Run the generation pipeline for each profile (or directly copy the updated templates since `x-dev-lifecycle` has no profile-specific substitution). Verify that the `.claude/` and `.agents/` copies match the Claude template, and the `.github/` copies match the GitHub template.

**Depends On:** TASK-3 (both source templates must be finalized)
**Parallel:** no

---

## TASK-5: Run tests to verify byte-for-byte golden file match

**Description:** Run the full test suite (`npm test`) to validate that all byte-for-byte golden file tests pass and no regressions exist.

**Validation checklist:**
- [ ] All 24 golden files for `x-dev-lifecycle/SKILL.md` pass byte-for-byte comparison
- [ ] No missing files in golden file comparison
- [ ] No extra files in golden file comparison
- [ ] All other existing tests remain passing (no regressions across 1,384+ tests)
- [ ] Coverage: line >= 95%, branch >= 90%

**Command:** `npm test`

**Expected result:** All tests GREEN. The byte-for-byte integration test (`tests/node/integration/byte-for-byte.test.ts`) compares pipeline output against golden files. If any golden file was not regenerated correctly, this test will report the exact diff.

**Depends On:** TASK-4
**Parallel:** no

---

## TASK-6: Verify python-click-cli golden file does NOT have event-driven generator content (negative test)

**Description:** Verify that the `python-click-cli` profile's golden files contain the Event-Driven Documentation Generator section. This is a **structural verification**, not a negative test in the traditional sense, because the `x-dev-lifecycle` template has no profile-specific conditional content -- the generator section is present in all profiles (the trigger condition is a runtime AI decision, not a generation-time condition).

**Clarification of expected behavior:**

The `x-dev-lifecycle` template uses NO profile-specific placeholder substitution. The `### Event-Driven Documentation Generator` section is present in the template unconditionally. At **generation time**, all 8 profiles (including `python-click-cli`) receive an identical copy of the lifecycle SKILL.md. The generator section includes a **runtime trigger condition** (`Invoke when interfaces contains websocket, event-consumer, or event-producer`) -- this condition is evaluated by the AI agent at runtime, not by the assembler at generation time.

Therefore, the correct verification is:
1. `python-click-cli` golden files DO contain `### Event-Driven Documentation Generator` (the section is present)
2. The section contains the trigger condition text referencing `websocket`, `event-consumer`, `event-producer`
3. At runtime, when the AI reads `python-click-cli`'s project identity (interfaces: `["cli"]`), it will correctly skip the event-driven generator because none of the trigger interfaces match
4. The golden file for `python-click-cli` is byte-for-byte identical to all other profiles (since no profile-specific substitution occurs)

**Verification steps:**
- Confirm `tests/golden/python-click-cli/.claude/skills/x-dev-lifecycle/SKILL.md` contains the string `Event-Driven Documentation Generator`
- Confirm `tests/golden/python-click-cli/.claude/skills/x-dev-lifecycle/SKILL.md` is byte-for-byte identical to `tests/golden/typescript-nestjs/.claude/skills/x-dev-lifecycle/SKILL.md`
- Confirm the trigger condition text is present so the AI agent can skip it at runtime for CLI-only projects

**Depends On:** TASK-5 (tests must pass first to confirm golden files are correct)
**Parallel:** yes (can run alongside TASK-5 final validation review, but logically follows it)

---

## Implementation Notes

1. **No TypeScript source code changes required.** The assemblers (`SkillsAssembler`, `GithubSkillsAssembler`, `CodexSkillsAssembler`) copy the `x-dev-lifecycle` template verbatim with no profile-specific substitution. The change is purely to the Markdown template content.

2. **The dispatch table reference already exists.** Line 200 of the Claude template already says `websocket, event-consumer, event-producer -> Event-Driven doc generator (story-0004-0010)`. TASK-2 adds the detailed generator prompt that this dispatch entry points to, not a new dispatch entry.

3. **Golden files are identical across all 8 profiles** for `x-dev-lifecycle` because no profile-specific substitution occurs. This simplifies regeneration -- the template can be copied directly.

4. **Runtime vs. generation-time conditioning.** The event-driven generator section is present in ALL profiles' SKILL.md files. The trigger condition (`Invoke when interfaces contains websocket, event-consumer, or event-producer`) is evaluated by the AI agent at runtime, not by the template assembler at generation time. This matches the established pattern used by Phase 1D (Event Schema Design: `if event_driven`) and Phase 1E (Compliance Assessment: `if compliance active`).

5. **Sibling generators (stories 0007, 0008, 0009, 0011)** follow the same pattern. Consistency across all five documentation generators is important. If the other generators are implemented before or concurrently, coordinate section heading style (`### {Name} Documentation Generator`), prompt structure, and placement within Phase 3.

6. **Interface type clarification.** The story description mentions `kafka` as a trigger, but `kafka` is a `broker` value, not an interface type. The correct trigger interfaces are `websocket`, `event-consumer`, and `event-producer`. This is documented in the plan (Risk 7.2) and must be reflected in the generator prompt text.
