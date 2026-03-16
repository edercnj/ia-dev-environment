# Implementation Plan — story-0004-0010: Event-Driven/WebSocket Documentation Generator

**Story:** story-0004-0010
**Epic:** EPIC-0004
**Dependency:** Blocked by story-0004-0005 (Documentation Phase in x-dev-lifecycle)
**Status of blocker:** story-0004-0005 is NOT yet implemented — the lifecycle skill template still shows 8 phases (0-7) with no documentation phase. This plan assumes story-0004-0005 will be completed first, adding a documentation dispatch phase to the lifecycle.

---

## 1. Affected Layers and Components

This project is a **CLI library** (not hexagonal). The relevant layers are:

| Layer | Role in This Story |
|-------|-------------------|
| **Resource templates** (`resources/`) | Source of truth. New event-catalog doc generator prompt/template added here. |
| **Assemblers** (`src/assembler/`) | Copy templates to output dirs with placeholder replacement. May need no changes if story-0004-0005 already defines the dispatch mechanism in the lifecycle skill template. |
| **Domain** (`src/domain/`) | Interface detection logic. `conditions.ts` already provides `hasAnyInterface()`. `protocol-mapping.ts` already maps `websocket`, `event-consumer`, `event-producer` to protocol dirs. No new domain logic needed. |
| **Models** (`src/models.ts`) | `InterfaceConfig` already carries `type`, `spec`, `broker` fields. No changes needed. |
| **Tests** (`tests/`) | Golden file tests for profiles that include `websocket`/`kafka`/`event-consumer`/`event-producer` interfaces. |

---

## 2. New Files to Create

### 2.1 Lifecycle Skill Template — Event-Driven Generator Section

The documentation phase dispatcher (from story-0004-0005) will be a section inside the `x-dev-lifecycle` SKILL.md template. Story-0004-0010 adds the **event-driven generator subagent prompt** that the dispatcher invokes when it detects `websocket`, `event-consumer`, or `event-producer` in the project interfaces.

**Approach decision:** The generator is NOT a separate assembler or TypeScript module. It is a **prompt template embedded in the lifecycle skill**. The lifecycle skill is a Markdown file that instructs the AI agent what to do. The "generator" is a subagent prompt block within the documentation phase that tells the AI to scan event definitions and produce `docs/api/event-catalog.md`.

The exact placement depends on how story-0004-0005 structures the dispatch. Two likely patterns:

**Pattern A — Inline in lifecycle SKILL.md:** The documentation phase section in the lifecycle contains conditional blocks like:
```
### If interfaces contain "websocket", "event-consumer", or "event-producer":
Launch subagent: [Event-Driven Doc Generator prompt]
```

**Pattern B — Separate generator template file:** A dedicated template file at `resources/skills-templates/core/x-dev-lifecycle/generators/event-catalog-generator.md` that is referenced from the documentation phase.

**Recommendation:** Pattern A (inline), matching how the existing Phase 1D (Event Schema Design) already inlines the subagent prompt directly in the lifecycle SKILL.md. This is the established pattern in this codebase.

### 2.2 New Files

| # | File Path (under `resources/`) | Description |
|---|-------------------------------|-------------|
| 1 | `resources/skills-templates/core/x-dev-lifecycle/generators/event-catalog-generator.md` | Event-driven doc generator prompt template (if story-0004-0005 adopts Pattern B with separate files). Otherwise, content goes inline into `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`. |
| 2 | `resources/github-skills-templates/dev/x-dev-lifecycle.md` | Dual copy of the lifecycle skill with event-driven generator section (RULE-001). |

**Note:** The exact file structure depends on the dispatch architecture chosen by story-0004-0005. If 0005 uses inline prompts (like existing Phase 1D), then no new standalone file is needed -- just new sections within the existing SKILL.md templates.

### 2.3 New Test Files

| # | File Path | Description |
|---|-----------|-------------|
| 1 | `tests/assembler/event-catalog-generator.test.ts` | Unit tests for event-driven generator dispatch conditions and output structure. |

### 2.4 Golden File Updates

Golden files for profiles that include event-driven interfaces will need updating. Affected profiles:

| Profile | Interfaces Containing Events | Expected Change |
|---------|------------------------------|-----------------|
| `java-spring` | `event-consumer` (kafka), `event-producer` (kafka) | Lifecycle SKILL.md includes event-catalog generator section |
| `java-quarkus` | `event-consumer` (kafka), `event-producer` (kafka) | Same |
| `typescript-nestjs` | `websocket`, `event-consumer` (kafka), `event-producer` (kafka) | Same (triggers on both websocket and kafka) |
| `go-gin` | `event-consumer` (kafka), `event-producer` (kafka) | Same |
| `rust-axum` | `event-consumer` (kafka), `event-producer` (kafka) | Same |
| `python-fastapi` | `websocket`, `event-consumer` (kafka), `event-producer` (kafka) | Same |
| `kotlin-ktor` | `websocket`, `event-consumer` (kafka), `event-producer` (kafka) | Same |
| `python-click-cli` | None (only `cli`) | NO event-catalog section — must verify it is NOT present |

All 8 profiles have golden files at `tests/golden/{profile}/.claude/skills/x-dev-lifecycle/SKILL.md`, `.github/skills/x-dev-lifecycle/SKILL.md`, and `.agents/skills/x-dev-lifecycle/SKILL.md` (3 copies per profile due to dual copy + Codex).

---

## 3. Existing Files to Modify

### 3.1 Lifecycle Skill Templates (Source of Truth)

| # | File | Change |
|---|------|--------|
| 1 | `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` | Add event-driven documentation generator subagent prompt inside the documentation phase (added by story-0004-0005). The prompt instructs the AI to: (a) detect `websocket`, `event-consumer`, or `event-producer` in interfaces, (b) scan event definitions, (c) generate `docs/api/event-catalog.md` with topics overview, per-event sections, payload schemas, Mermaid flow diagrams. |
| 2 | `resources/github-skills-templates/dev/x-dev-lifecycle.md` | Mirror the same event-driven generator section (RULE-001 dual copy). |

### 3.2 Conditional Generation Logic (if needed)

The story says the generator is "invoked when the project identity contains `websocket` or `kafka` in the list of interfaces." However, looking at the interface types in config templates, there is NO interface type called `kafka` -- instead there are `event-consumer` and `event-producer` with `broker: kafka`. The `websocket` type does exist.

**Trigger condition should be:**
```typescript
hasAnyInterface(config, "websocket", "event-consumer", "event-producer")
```

This condition already has a near-exact parallel in `skills-selection.ts`:
```typescript
if (hasAnyInterface(config, "event-consumer", "event-producer")) {
  skills.push("x-review-events");
}
```

**Key insight:** The documentation phase dispatch logic lives in the **lifecycle SKILL.md template text** (it is an AI prompt, not TypeScript code). The AI agent reads the project identity at runtime and decides which generators to invoke. Therefore, no new TypeScript condition logic is required in `src/` -- the condition is expressed as natural language in the SKILL.md prompt:

```markdown
### Event-Driven Documentation Generator
**Trigger:** Invoke when `interfaces` contains `websocket`, `event-consumer`, or `event-producer`.
```

### 3.3 No Assembler Changes Expected

The `SkillsAssembler` copies the entire `x-dev-lifecycle/` directory from `resources/skills-templates/core/`. If the generator prompt is added inline to the existing SKILL.md, no assembler code changes are needed. The `GithubSkillsAssembler` already handles `x-dev-lifecycle` in the `"dev"` group. The `CodexSkillsAssembler` mirrors from `.claude/skills/`.

### 3.4 Golden Files to Regenerate

All 24 golden files (8 profiles x 3 copies) for `x-dev-lifecycle/SKILL.md` will need regeneration after the template changes. This is done via `npm run test:update` or equivalent snapshot update command.

---

## 4. Dependency Direction Validation

```
resources/skills-templates/core/x-dev-lifecycle/SKILL.md   (Source of Truth)
    |
    v  (copied by SkillsAssembler)
.claude/skills/x-dev-lifecycle/SKILL.md                     (Output)
    |
    v  (mirrored by CodexSkillsAssembler)
.agents/skills/x-dev-lifecycle/SKILL.md                     (Output)

resources/github-skills-templates/dev/x-dev-lifecycle.md    (Source of Truth, dual copy)
    |
    v  (copied by GithubSkillsAssembler)
.github/skills/x-dev-lifecycle/SKILL.md                     (Output)
```

**Validation:** Dependencies flow from `resources/` (source) to outputs (`.claude/`, `.github/`, `.agents/`). No circular dependencies. No runtime TypeScript code changes required. This story modifies only Markdown templates and their golden file test expectations.

---

## 5. Integration Points

### 5.1 With story-0004-0005 (Documentation Phase)

This story **extends** the documentation phase dispatch mechanism created by story-0004-0005. The integration is:
- Story-0004-0005 creates the Phase 3 (Documentation) section in the lifecycle with a dispatcher pattern
- Story-0004-0010 adds the event-driven generator as one of the dispatched generators
- The dispatcher checks `interfaces` and invokes the event-driven generator when relevant

### 5.2 With Existing Protocol Resources

The event-driven generator prompt should reference existing protocol knowledge packs for context:
- `skills/protocols/references/event-driven-conventions.md` (already generated from `resources/protocols/event-driven/`)
- `skills/protocols/references/websocket-conventions.md` (already generated from `resources/protocols/websocket/`)
- `skills/protocols/references/messaging-conventions.md` (already generated from `resources/protocols/messaging/`)

These references give the AI agent the conventions it needs to produce consistent event-catalog documentation.

### 5.3 With `conditions.ts` and `protocol-mapping.ts`

No direct integration needed. These modules are used by assemblers at generation time. The event-driven doc generator operates at **runtime** (when an AI agent executes the lifecycle skill), not at generation time.

### 5.4 With Sibling Doc Generators (stories 0007-0009)

The event-driven generator follows the same pattern as:
- story-0004-0007 (OpenAPI generator) -- triggered by `rest` interface
- story-0004-0008 (gRPC generator) -- triggered by `grpc` interface
- story-0004-0009 (CLI generator) -- triggered by `cli` interface

All four generators share the same lifecycle integration pattern established by story-0004-0005. Consistency across generators is important.

---

## 6. Configuration Changes

No configuration changes needed. The generator trigger is based on the existing `interfaces` field in `ProjectConfig`, which already supports `websocket`, `event-consumer`, and `event-producer` interface types with optional `broker` and `spec` fields.

---

## 7. Risk Assessment

### 7.1 Hard Dependency on story-0004-0005

**Risk: HIGH** -- This story cannot be implemented until story-0004-0005 defines the documentation phase dispatch mechanism. The exact structure of the dispatch (inline prompts vs. separate files, phase numbering, subagent invocation pattern) determines how the event-driven generator is integrated.

**Mitigation:** Start with the generator prompt content (what the AI should do) and adapt the integration packaging once story-0004-0005 is complete.

### 7.2 Interface Type vs. Story Description Mismatch

**Risk: MEDIUM** -- The story description says "when project identity contains `websocket` or `kafka` in the list of interfaces." However, `kafka` is NOT an interface type -- it is a `broker` value within `event-consumer` and `event-producer` interface types. The trigger condition must use interface types (`websocket`, `event-consumer`, `event-producer`), not broker names.

**Mitigation:** Use `hasAnyInterface(config, "websocket", "event-consumer", "event-producer")` as the trigger condition. Document this clarification in the generator prompt text.

### 7.3 Golden File Update Volume

**Risk: LOW** -- Updating 24 golden files (8 profiles x 3 copies) is mechanical but can introduce merge conflicts if other stories modify the same files in parallel. All profiles except `python-click-cli` have event-driven interfaces.

**Mitigation:** Use the standard golden file regeneration workflow. Run byte-for-byte tests after regeneration. Coordinate with sibling story branches.

### 7.4 Generator Prompt Quality

**Risk: MEDIUM** -- The event-driven generator is an AI prompt, not deterministic code. The quality of the generated `event-catalog.md` depends on how well the prompt instructs the AI. If the prompt is too vague, output will be inconsistent.

**Mitigation:** Include explicit output structure in the prompt: required Markdown sections, table formats, Mermaid diagram templates. Reference existing protocol conventions KPs for domain knowledge. The prompt should specify:
1. Topics Overview table (Topic, Events, Partitioning)
2. Per-event sections (Topic, Producer, Consumers, Payload Schema table, Headers table)
3. Mermaid `sequenceDiagram` for event flows
4. CloudEvents envelope details (if applicable)

### 7.5 WebSocket vs. Kafka Differentiation

**Risk: LOW** -- WebSocket events (channels, messages) have different semantics from Kafka events (topics, partitions, consumer groups). The generator prompt must handle both, producing appropriate documentation for each.

**Mitigation:** The prompt should detect the interface type and adapt:
- For `event-consumer`/`event-producer` with `broker: kafka`: document topics, partition keys, consumer groups
- For `websocket`: document channels, message types, connection lifecycle
- Include both in a unified `event-catalog.md` if the project uses both

### 7.6 Backward Compatibility

**Risk: LOW** -- Projects without event-driven interfaces (like `python-click-cli` with only `cli`) must not be affected. The dispatch condition ensures the generator is a no-op for non-event projects.

**Mitigation:** Golden file test for `python-click-cli` verifies the lifecycle SKILL.md does NOT contain event-catalog generation instructions (or contains them in a clearly conditional block that the AI skips).

---

## 8. Implementation Order

1. **Verify story-0004-0005 is complete** -- Check that the documentation phase exists in the lifecycle template with a dispatch mechanism.
2. **Write event-driven generator prompt** -- Draft the subagent prompt that produces `docs/api/event-catalog.md`.
3. **Add prompt to lifecycle SKILL.md** -- Insert into `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` within the documentation phase dispatcher.
4. **Add prompt to GitHub dual copy** -- Mirror in `resources/github-skills-templates/dev/x-dev-lifecycle.md` (RULE-001).
5. **Write unit tests** -- Verify the generator section appears in output for event-driven profiles and is absent (or conditional) for non-event profiles.
6. **Regenerate golden files** -- Update all 24 affected golden files.
7. **Run full test suite** -- Ensure 1,384+ tests pass with >= 95% line / >= 90% branch coverage.

---

## 9. Acceptance Criteria Mapping

| Gherkin Scenario | Implementation Point |
|-----------------|---------------------|
| Event-Driven generator produces catalog for Kafka project | Generator prompt in lifecycle SKILL.md, triggered by `event-consumer`/`event-producer` |
| Topics table lists all topics with partitioning | Prompt specifies "Topics Overview" table with Topic, Events, Partition Key columns |
| Payload schema documented with all fields | Prompt specifies per-event payload schema table: Field, Type, Required, Description |
| Event flow diagram in Mermaid | Prompt specifies Mermaid `sequenceDiagram` template for Producer -> Broker -> Consumer |
| Generator skipped for non-event project | Dispatch condition in lifecycle checks interface types; no-op if none match |
| WebSocket events documented when websocket interface | Generator prompt handles `websocket` type alongside `event-consumer`/`event-producer` |

---

## 10. Files Summary

### New Files
| File | Purpose |
|------|---------|
| `tests/assembler/event-catalog-generator.test.ts` | Unit tests for event-driven generator presence/absence in lifecycle output |
| `resources/skills-templates/core/x-dev-lifecycle/generators/event-catalog-generator.md` | (Only if story-0004-0005 uses separate generator files pattern) |

### Modified Files
| File | Change |
|------|--------|
| `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` | Add event-driven doc generator subagent prompt in documentation phase |
| `resources/github-skills-templates/dev/x-dev-lifecycle.md` | Dual copy of the same (RULE-001) |
| `tests/golden/{7 profiles}/.claude/skills/x-dev-lifecycle/SKILL.md` | Regenerated golden files |
| `tests/golden/{7 profiles}/.github/skills/x-dev-lifecycle/SKILL.md` | Regenerated golden files |
| `tests/golden/{7 profiles}/.agents/skills/x-dev-lifecycle/SKILL.md` | Regenerated golden files |
| `tests/golden/python-click-cli/.claude/skills/x-dev-lifecycle/SKILL.md` | Verify NO event-catalog content (negative test) |
| `tests/golden/python-click-cli/.github/skills/x-dev-lifecycle/SKILL.md` | Same |
| `tests/golden/python-click-cli/.agents/skills/x-dev-lifecycle/SKILL.md` | Same |

### No Changes Expected
| File | Reason |
|------|--------|
| `src/assembler/skills-assembler.ts` | Already copies entire `x-dev-lifecycle/` directory |
| `src/assembler/github-skills-assembler.ts` | Already handles `x-dev-lifecycle` in `"dev"` group |
| `src/assembler/codex-skills-assembler.ts` | Mirrors from `.claude/skills/` |
| `src/assembler/conditions.ts` | `hasAnyInterface()` already exists |
| `src/domain/protocol-mapping.ts` | Already maps event interface types |
| `src/models.ts` | `InterfaceConfig` already has `type`, `broker` fields |
| `src/assembler/pipeline.ts` | No new assemblers needed |
