# Test Plan — story-0004-0010

## Summary

This story adds a detailed **Event-Driven/WebSocket Documentation Generator** subagent prompt to the Phase 3 (Documentation) section of the `x-dev-lifecycle` skill templates. The generator instructs the AI to produce `docs/api/event-catalog.md` when the project identity contains `websocket`, `event-consumer`, or `event-producer` interfaces. The implementation modifies only Markdown templates (no TypeScript source changes). Testing validates prompt content via unit-level substring/regex checks against both source templates (RULE-001 dual copy), and byte-for-byte golden file parity across all 8 profiles.

**Important context:** The lifecycle SKILL.md is a "core" skill copied identically to all 8 profiles. The event-driven generator section appears in every profile's output as a conditional AI instruction (the AI reads the `interfaces` field at runtime to decide whether to invoke the generator). Therefore, there is no compile-time conditional logic that omits the section for `python-click-cli`. Instead, the test plan verifies that the conditional trigger language is correct so that the AI skips execution for non-event profiles.

---

## 1. Test Strategy Overview

| Category | Scope | New Tests? | Test File |
|----------|-------|------------|-----------|
| Content validation (Claude source) | Verify Event-Driven generator prompt, trigger condition, output structure, and protocol references in `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` | YES | `tests/node/content/x-dev-lifecycle-event-doc.test.ts` |
| Content validation (GitHub source) | Same validations against `resources/github-skills-templates/dev/x-dev-lifecycle.md` | YES | `tests/node/content/x-dev-lifecycle-event-doc.test.ts` (separate describe block) |
| Dual copy consistency (RULE-001) | Both sources contain semantically identical Event-Driven generator content | YES | `tests/node/content/x-dev-lifecycle-event-doc.test.ts` |
| Golden file integration | Pipeline output matches updated golden files for all 8 profiles | NO (existing) | `tests/node/integration/byte-for-byte.test.ts` |

---

## 2. Interface Classification by Profile

This table drives the edge case analysis. The dispatch condition `websocket`, `event-consumer`, `event-producer` targets different combinations across profiles.

| Profile | Event Interfaces | WebSocket? | Kafka (`event-consumer`/`event-producer`)? | Event Generator Triggered at Runtime? |
|---------|-----------------|------------|-------------------------------------------|---------------------------------------|
| `java-spring` | `event-consumer` (kafka), `event-producer` (kafka) | NO | YES | YES |
| `java-quarkus` | `event-consumer` (kafka), `event-producer` (kafka) | NO | YES | YES |
| `go-gin` | `event-consumer` (kafka), `event-producer` (kafka) | NO | YES | YES |
| `rust-axum` | `event-consumer` (kafka), `event-producer` (kafka) | NO | YES | YES |
| `typescript-nestjs` | `websocket`, `event-consumer` (kafka), `event-producer` (kafka) | YES | YES | YES |
| `python-fastapi` | `websocket`, `event-consumer` (kafka), `event-producer` (kafka) | YES | YES | YES |
| `kotlin-ktor` | `websocket`, `event-consumer` (kafka), `event-producer` (kafka) | YES | YES | YES |
| `python-click-cli` | (none -- only `cli`) | NO | NO | NO |

**Edge case groups:**
- **Kafka-only (no websocket):** `java-spring`, `java-quarkus`, `go-gin`, `rust-axum`
- **WebSocket + Kafka (both):** `typescript-nestjs`, `python-fastapi`, `kotlin-ktor`
- **No event interfaces:** `python-click-cli`

---

## 3. Test Scenarios

### AT-1: Lifecycle SKILL.md contains Event-Driven Documentation Generator section

> **Category:** Acceptance Test (integration-level)
> **Depends On:** story-0004-0005 (Documentation Phase must exist)
> **Parallel:** no (outer-loop acceptance test)

**Description:** After modifying both source templates, running the full pipeline for any profile produces a `x-dev-lifecycle/SKILL.md` output that contains the Event-Driven documentation generator subagent prompt within Phase 3.

**Verification:**
- Run `npx vitest run tests/node/content/x-dev-lifecycle-event-doc.test.ts` -- all content tests pass
- Run `npx vitest run tests/node/integration/byte-for-byte.test.ts` -- all 8 profiles pass
- Combined: confirms end-to-end that the event-driven generator prompt is correctly generated and distributed

---

### UT-1: Event-Driven generator heading exists in Phase 3

> **Category:** Unit Test -- Content Validation (Level 1: Degenerate)
> **Depends On:** none
> **Parallel:** yes (independent of UT-2 through UT-11)

**Claude source:** `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`

| # | Test Name | Assertion |
|---|-----------|-----------|
| 1 | `claudeSource_phase3_containsEventDrivenGeneratorHeading` | `expect(claudeContent).toMatch(/###.*Event.Driven.*Doc.*Generator/i)` |

**GitHub source:** `resources/github-skills-templates/dev/x-dev-lifecycle.md`

| # | Test Name | Assertion |
|---|-----------|-----------|
| 2 | `githubSource_phase3_containsEventDrivenGeneratorHeading` | `expect(githubContent).toMatch(/###.*Event.Driven.*Doc.*Generator/i)` |

---

### UT-2: Generator trigger condition lists all three interface types

> **Category:** Unit Test -- Content Validation (Level 2: Unconditional)
> **Depends On:** none
> **Parallel:** yes

The trigger condition must reference `websocket`, `event-consumer`, and `event-producer` -- NOT `kafka` (which is a broker, not an interface type).

| # | Test Name | Assertion |
|---|-----------|-----------|
| 3 | `claudeSource_eventGen_triggerContainsWebsocket` | `expect(eventGenSection).toMatch(/websocket/i)` |
| 4 | `claudeSource_eventGen_triggerContainsEventConsumer` | `expect(eventGenSection).toContain("event-consumer")` |
| 5 | `claudeSource_eventGen_triggerContainsEventProducer` | `expect(eventGenSection).toContain("event-producer")` |
| 6 | `githubSource_eventGen_triggerContainsWebsocket` | `expect(githubEventGenSection).toMatch(/websocket/i)` |
| 7 | `githubSource_eventGen_triggerContainsEventConsumer` | `expect(githubEventGenSection).toContain("event-consumer")` |
| 8 | `githubSource_eventGen_triggerContainsEventProducer` | `expect(githubEventGenSection).toContain("event-producer")` |

---

### UT-3: Generator specifies output path `docs/api/event-catalog.md`

> **Category:** Unit Test -- Content Validation (Level 2: Unconditional)
> **Depends On:** none
> **Parallel:** yes

| # | Test Name | Assertion |
|---|-----------|-----------|
| 9 | `claudeSource_eventGen_outputPathEventCatalog` | `expect(eventGenSection).toContain("docs/api/event-catalog.md")` |
| 10 | `githubSource_eventGen_outputPathEventCatalog` | `expect(githubEventGenSection).toContain("docs/api/event-catalog.md")` |

---

### UT-4: Generator prompt specifies Topics Overview table

> **Category:** Unit Test -- Content Validation (Level 3: Simple Condition)
> **Depends On:** UT-1
> **Parallel:** yes (with UT-5 through UT-8)

| # | Test Name | Assertion |
|---|-----------|-----------|
| 11 | `claudeSource_eventGen_containsTopicsOverview` | `expect(eventGenSection).toMatch(/[Tt]opics?\s+[Oo]verview/)` |
| 12 | `claudeSource_eventGen_topicsTableHasTopicColumn` | `expect(eventGenSection).toMatch(/[Tt]opic/i)` (within table context) |
| 13 | `claudeSource_eventGen_topicsTableHasEventsColumn` | `expect(eventGenSection).toMatch(/[Ee]vents?/i)` (within table context) |
| 14 | `claudeSource_eventGen_topicsTableHasPartitioningColumn` | `expect(eventGenSection).toMatch(/[Pp]artition/i)` |
| 15 | `githubSource_eventGen_containsTopicsOverview` | `expect(githubEventGenSection).toMatch(/[Tt]opics?\s+[Oo]verview/)` |

---

### UT-5: Generator prompt specifies per-event sections with payload schema

> **Category:** Unit Test -- Content Validation (Level 3: Simple Condition)
> **Depends On:** UT-1
> **Parallel:** yes (with UT-4, UT-6 through UT-8)

| # | Test Name | Assertion |
|---|-----------|-----------|
| 16 | `claudeSource_eventGen_perEventSectionPattern` | `expect(eventGenSection).toMatch(/per.event|per event|each event|Event:\s*\{/i)` |
| 17 | `claudeSource_eventGen_payloadSchemaTable` | `expect(eventGenSection).toMatch(/[Pp]ayload.*[Ss]chema/i)` |
| 18 | `claudeSource_eventGen_payloadFieldColumn` | `expect(eventGenSection).toMatch(/[Ff]ield/i)` |
| 19 | `claudeSource_eventGen_payloadTypeColumn` | `expect(eventGenSection).toMatch(/[Tt]ype/i)` |
| 20 | `claudeSource_eventGen_payloadRequiredColumn` | `expect(eventGenSection).toMatch(/[Rr]equired/i)` |
| 21 | `claudeSource_eventGen_payloadDescriptionColumn` | `expect(eventGenSection).toMatch(/[Dd]escription/i)` |
| 22 | `githubSource_eventGen_payloadSchemaTable` | `expect(githubEventGenSection).toMatch(/[Pp]ayload.*[Ss]chema/i)` |

---

### UT-6: Generator prompt specifies producer/consumer contract fields

> **Category:** Unit Test -- Content Validation (Level 3: Simple Condition)
> **Depends On:** UT-1
> **Parallel:** yes (with UT-4, UT-5, UT-7, UT-8)

| # | Test Name | Assertion |
|---|-----------|-----------|
| 23 | `claudeSource_eventGen_containsProducer` | `expect(eventGenSection).toMatch(/[Pp]roducer/i)` |
| 24 | `claudeSource_eventGen_containsConsumer` | `expect(eventGenSection).toMatch(/[Cc]onsumer/i)` |
| 25 | `claudeSource_eventGen_containsTopicOrChannel` | `expect(eventGenSection).toMatch(/[Tt]opic|[Cc]hannel/i)` |
| 26 | `githubSource_eventGen_containsProducerAndConsumer` | `expect(githubEventGenSection).toMatch(/[Pp]roducer/)` and `expect(githubEventGenSection).toMatch(/[Cc]onsumer/)` |

---

### UT-7: Generator prompt specifies Mermaid event flow diagrams

> **Category:** Unit Test -- Content Validation (Level 3: Simple Condition)
> **Depends On:** UT-1
> **Parallel:** yes (with UT-4 through UT-6, UT-8)

| # | Test Name | Assertion |
|---|-----------|-----------|
| 27 | `claudeSource_eventGen_containsMermaidDiagram` | `expect(eventGenSection).toMatch(/[Mm]ermaid/i)` |
| 28 | `claudeSource_eventGen_containsSequenceDiagram` | `expect(eventGenSection).toMatch(/sequenceDiagram|sequence.*diagram/i)` |
| 29 | `claudeSource_eventGen_flowShowsProducerBrokerConsumer` | `expect(eventGenSection).toMatch(/[Pp]roducer.*[Bb]roker.*[Cc]onsumer/s)` (multiline) |
| 30 | `githubSource_eventGen_containsMermaidDiagram` | `expect(githubEventGenSection).toMatch(/[Mm]ermaid/i)` |

---

### UT-8: Generator prompt references protocol knowledge packs

> **Category:** Unit Test -- Content Validation (Level 4: Complex Condition)
> **Depends On:** UT-1
> **Parallel:** yes (with UT-9, UT-10)

The generator should reference existing protocol convention files so the AI agent has domain context.

| # | Test Name | Assertion |
|---|-----------|-----------|
| 31 | `claudeSource_eventGen_referencesEventDrivenConventions` | `expect(eventGenSection).toMatch(/event.driven.conventions|protocols.*event/i)` |
| 32 | `claudeSource_eventGen_referencesWebsocketConventions` | `expect(eventGenSection).toMatch(/websocket.conventions|protocols.*websocket/i)` |
| 33 | `githubSource_eventGen_referencesProtocolConventions` | `expect(githubEventGenSection).toMatch(/conventions|protocols/i)` |

---

### UT-9: Generator handles CloudEvents envelope

> **Category:** Unit Test -- Content Validation (Level 4: Complex Condition)
> **Depends On:** UT-1
> **Parallel:** yes (with UT-8, UT-10)

| # | Test Name | Assertion |
|---|-----------|-----------|
| 34 | `claudeSource_eventGen_containsCloudEvents` | `expect(eventGenSection).toMatch(/[Cc]loud[Ee]vents?/)` |
| 35 | `githubSource_eventGen_containsCloudEvents` | `expect(githubEventGenSection).toMatch(/[Cc]loud[Ee]vents?/)` |

---

### UT-10: Generator handles schema versioning

> **Category:** Unit Test -- Content Validation (Level 4: Complex Condition)
> **Depends On:** UT-1
> **Parallel:** yes (with UT-8, UT-9)

| # | Test Name | Assertion |
|---|-----------|-----------|
| 36 | `claudeSource_eventGen_containsVersioning` | `expect(eventGenSection).toMatch(/[Vv]ersion|[Bb]ackward.*[Cc]ompati/i)` |
| 37 | `githubSource_eventGen_containsVersioning` | `expect(githubEventGenSection).toMatch(/[Vv]ersion|[Bb]ackward.*[Cc]ompati/i)` |

---

### UT-11: Generator positioned within Phase 3 section (not elsewhere)

> **Category:** Unit Test -- Content Validation (Level 5: Ordering/Positioning)
> **Depends On:** UT-1
> **Parallel:** no

| # | Test Name | Assertion |
|---|-----------|-----------|
| 38 | `claudeSource_eventGen_afterPhase3Heading` | Event-Driven generator index > Phase 3 heading index |
| 39 | `claudeSource_eventGen_beforePhase4Heading` | Event-Driven generator index < Phase 4 heading index |
| 40 | `githubSource_eventGen_afterPhase3Heading` | Event-Driven generator index > Phase 3 heading index |
| 41 | `githubSource_eventGen_beforePhase4Heading` | Event-Driven generator index < Phase 4 heading index |

---

### UT-12: Dispatch table entry references Event-Driven generator

> **Category:** Unit Test -- Content Validation (Level 2: Unconditional)
> **Depends On:** none
> **Parallel:** yes

The Phase 3 dispatch table (lines 195-200 in current template) must list the event-driven mapping.

| # | Test Name | Assertion |
|---|-----------|-----------|
| 42 | `claudeSource_dispatchTable_websocketMapsToEventDriven` | `expect(claudeContent).toMatch(/websocket.*event-consumer.*event-producer.*Event.Driven/is)` or each mapped separately |
| 43 | `claudeSource_dispatchTable_storyReference` | `expect(claudeContent).toContain("story-0004-0010")` |
| 44 | `githubSource_dispatchTable_websocketMapsToEventDriven` | `expect(githubContent).toMatch(/websocket.*event-consumer.*event-producer.*Event.Driven/is)` |
| 45 | `githubSource_dispatchTable_storyReference` | `expect(githubContent).toContain("story-0004-0010")` |

---

### UT-13: WebSocket-specific documentation instructions

> **Category:** Unit Test -- Content Validation (Level 5: Edge Case -- WebSocket variant)
> **Depends On:** UT-1, UT-2
> **Parallel:** yes

The generator must differentiate between Kafka topics and WebSocket channels.

| # | Test Name | Assertion |
|---|-----------|-----------|
| 46 | `claudeSource_eventGen_websocketChannels` | `expect(eventGenSection).toMatch(/[Cc]hannel|[Ww]eb[Ss]ocket.*message/i)` |
| 47 | `claudeSource_eventGen_kafkaTopics` | `expect(eventGenSection).toMatch(/[Tt]opic|[Pp]artition.*key|[Cc]onsumer.*group/i)` |
| 48 | `claudeSource_eventGen_handlesMultipleProtocols` | `expect(eventGenSection).toMatch(/both|unified|multiple/i)` or semantic equivalent indicating the generator handles projects with both websocket and kafka |

---

### UT-14: Structural preservation -- existing Phase 3 elements intact

> **Category:** Unit Test -- Content Validation (Level 6: Edge Case -- backward compatibility)
> **Depends On:** none
> **Parallel:** yes

| # | Test Name | Assertion |
|---|-----------|-----------|
| 49 | `claudeSource_phase3_preservesInterfaceFieldRead` | `expect(claudeContent).toMatch(/Read.*interfaces.*field|interfaces.*field.*from/i)` (step 1 of Phase 3) |
| 50 | `claudeSource_phase3_preservesDispatchTable` | `expect(claudeContent).toMatch(/rest.*OpenAPI|grpc.*Proto|cli.*doc/i)` (other dispatch entries) |
| 51 | `claudeSource_phase3_preservesChangelogGeneration` | `expect(claudeContent).toMatch(/changelog.*entry|CHANGELOG\.md/i)` |
| 52 | `claudeSource_phase3_preservesNoInterfaceSkipLog` | `expect(claudeContent).toMatch(/[Nn]o documentable interfaces/)` |
| 53 | `claudeSource_phase3_preservesDocsOutputPaths` | `expect(claudeContent).toContain("docs/api/")` and `expect(claudeContent).toContain("docs/architecture/")` |

---

### UT-15: Dual copy consistency -- Event-Driven generator present in both (RULE-001)

> **Category:** Unit Test -- Dual Copy Consistency
> **Depends On:** UT-1
> **Parallel:** yes

| # | Test Name | Assertion |
|---|-----------|-----------|
| 54 | `dualCopy_bothContainEventDrivenGeneratorHeading` | Both contain event-driven generator heading |
| 55 | `dualCopy_bothContainTriggerConditionWebsocket` | Both contain `websocket` in trigger |
| 56 | `dualCopy_bothContainTriggerConditionEventConsumer` | Both contain `event-consumer` in trigger |
| 57 | `dualCopy_bothContainTriggerConditionEventProducer` | Both contain `event-producer` in trigger |
| 58 | `dualCopy_bothContainOutputPath` | Both contain `docs/api/event-catalog.md` |
| 59 | `dualCopy_bothContainTopicsOverview` | Both contain Topics Overview reference |
| 60 | `dualCopy_bothContainPayloadSchema` | Both contain Payload Schema reference |
| 61 | `dualCopy_bothContainMermaidDiagram` | Both contain Mermaid diagram reference |
| 62 | `dualCopy_bothContainCloudEvents` | Both contain CloudEvents reference |
| 63 | `dualCopy_bothContainProducerConsumer` | Both contain producer/consumer contract references |
| 64 | `dualCopy_bothContainStoryReference` | Both contain `story-0004-0010` |

---

### IT-1: Golden file test -- all 8 profiles byte-for-byte parity

> **Category:** Integration Test -- Golden File Parity
> **Depends On:** UT-1 through UT-15 (all content tests must pass before golden files can be regenerated)
> **Parallel:** no (sequential per profile, as per existing `describe.sequential.each` pattern)

**Description:** The existing `tests/node/integration/byte-for-byte.test.ts` runs the pipeline for all 8 profiles and verifies byte-for-byte parity with golden files. After updating the source templates and regenerating golden files, this test validates that the pipeline correctly distributes the updated lifecycle SKILL.md (now containing the Event-Driven generator section) to all output directories.

Since the lifecycle SKILL.md is a "core" skill (not conditionally assembled), the Event-Driven generator section appears identically in all 8 profiles. The AI-runtime condition (checking `interfaces`) determines whether the generator actually runs -- not whether the prompt text is present.

**Profiles:**

| Profile | Golden File Paths (3 per profile) | Event-Driven Section Present in Output? |
|---------|----------------------------------|----------------------------------------|
| go-gin | `.claude/skills/x-dev-lifecycle/SKILL.md`, `.agents/skills/x-dev-lifecycle/SKILL.md`, `.github/skills/x-dev-lifecycle/SKILL.md` | YES (identical content) |
| java-quarkus | (same 3 paths) | YES (identical content) |
| java-spring | (same 3 paths) | YES (identical content) |
| kotlin-ktor | (same 3 paths) | YES (identical content) |
| python-click-cli | (same 3 paths) | YES (identical content -- AI skips at runtime) |
| python-fastapi | (same 3 paths) | YES (identical content) |
| rust-axum | (same 3 paths) | YES (identical content) |
| typescript-nestjs | (same 3 paths) | YES (identical content) |

**Total golden files affected:** 24 (8 profiles x 3 output directories)

**Test assertions per profile (existing infrastructure):**
1. `pipelineSuccessForProfile_{profile}` -- pipeline runs without error
2. `pipelineMatchesGoldenFiles_{profile}` -- byte-for-byte match
3. `noMissingFiles_{profile}` -- no expected files absent
4. `noExtraFiles_{profile}` -- no unexpected files generated
5. `totalFilesGreaterThanZero_{profile}` -- at least one file generated

---

### IT-2: Negative validation -- python-click-cli conditional trigger is correct

> **Category:** Integration Test -- Edge Case (no event interfaces)
> **Depends On:** UT-2 (trigger condition correct)
> **Parallel:** yes (with IT-1)

**Description:** This is a content-level validation (not a pipeline conditional). Since the lifecycle template is static across all profiles, `python-click-cli` receives the same SKILL.md with the Event-Driven generator section. The test verifies that the **trigger condition** is correctly worded so the AI agent will skip the generator when `interfaces` contains only `cli`.

| # | Test Name | Assertion |
|---|-----------|-----------|
| 65 | `triggerCondition_requiresExplicitEventInterfaces` | Trigger mentions `websocket`, `event-consumer`, `event-producer` as required interface types (not just "event" generically) |
| 66 | `triggerCondition_doesNotMatchCliInterface` | The word `cli` does NOT appear as a trigger for the event-driven generator (it appears only in the CLI generator dispatch entry) |
| 67 | `dispatchTable_cliMapsToCliGenerator` | `expect(claudeContent).toMatch(/cli.*CLI.*doc.*generator/i)` (separate from event-driven) |

---

## 4. Edge Case Analysis

### 4.1 Kafka-only profiles (no websocket)

**Profiles:** `java-spring`, `java-quarkus`, `go-gin`, `rust-axum`

**What to verify:** The generator prompt must handle projects that have `event-consumer`/`event-producer` but no `websocket`. The prompt should produce documentation focused on Kafka topics, partition keys, and consumer groups without requiring websocket channels.

**Covered by:** UT-13 (#47 -- Kafka topics), UT-6 (#23-25 -- producer/consumer), UT-4 (#11-14 -- Topics Overview with partitioning)

### 4.2 WebSocket + Kafka profiles (both)

**Profiles:** `typescript-nestjs`, `python-fastapi`, `kotlin-ktor`

**What to verify:** The generator prompt must handle projects with both websocket and kafka interfaces. It should produce a unified `event-catalog.md` covering both Kafka topics and WebSocket channels.

**Covered by:** UT-13 (#46 -- WebSocket channels, #47 -- Kafka topics, #48 -- handles multiple protocols)

### 4.3 No event interfaces

**Profile:** `python-click-cli`

**What to verify:** The generator section is present in the template text (static core skill), but the trigger condition ensures the AI agent will not invoke it when only `cli` is configured.

**Covered by:** IT-2 (#65-67 -- trigger condition validation)

### 4.4 WebSocket-only (hypothetical)

**Note:** No current profile has `websocket` without also having `event-consumer`/`event-producer`. However, the trigger condition should support websocket-only projects. This is validated by UT-2 (#3) which confirms `websocket` is listed as an independent trigger, not grouped exclusively with kafka interfaces.

---

## 5. Golden Files Requiring Update

### 5.1 Golden File Update Strategy

After editing both source templates to include the Event-Driven generator section, regenerate all 24 golden files. Since the lifecycle is a core skill, all profiles receive identical content.

```bash
# Option A: Run the pipeline for each profile and capture output
for profile in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
  npx tsx src/cli.ts generate --config "resources/config-templates/setup-config.${profile}.yaml" --output "/tmp/regen-${profile}"
  cp "/tmp/regen-${profile}/.claude/skills/x-dev-lifecycle/SKILL.md" "tests/golden/${profile}/.claude/skills/x-dev-lifecycle/SKILL.md"
  cp "/tmp/regen-${profile}/.agents/skills/x-dev-lifecycle/SKILL.md" "tests/golden/${profile}/.agents/skills/x-dev-lifecycle/SKILL.md"
  cp "/tmp/regen-${profile}/.github/skills/x-dev-lifecycle/SKILL.md" "tests/golden/${profile}/.github/skills/x-dev-lifecycle/SKILL.md"
done

# Option B: Direct copy (valid because core skill is identical across profiles)
CLAUDE_SRC="resources/skills-templates/core/x-dev-lifecycle/SKILL.md"
GITHUB_SRC="resources/github-skills-templates/dev/x-dev-lifecycle.md"
for profile in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
  cp "$CLAUDE_SRC" "tests/golden/${profile}/.claude/skills/x-dev-lifecycle/SKILL.md"
  cp "$CLAUDE_SRC" "tests/golden/${profile}/.agents/skills/x-dev-lifecycle/SKILL.md"
  cp "$GITHUB_SRC" "tests/golden/${profile}/.github/skills/x-dev-lifecycle/SKILL.md"
done
```

### 5.2 Verification after regeneration

```bash
npx vitest run tests/node/integration/byte-for-byte.test.ts
```

All 40 assertions (8 profiles x 5 checks) must pass.

---

## 6. Suggested Test Implementation Pattern

```typescript
import { describe, it, expect } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

const CLAUDE_SOURCE = path.resolve(
  __dirname, "../../..",
  "resources/skills-templates/core/x-dev-lifecycle/SKILL.md",
);
const GITHUB_SOURCE = path.resolve(
  __dirname, "../../..",
  "resources/github-skills-templates/dev/x-dev-lifecycle.md",
);

const claudeContent = fs.readFileSync(CLAUDE_SOURCE, "utf-8");
const githubContent = fs.readFileSync(GITHUB_SOURCE, "utf-8");

// Extract the Event-Driven generator section (between its heading and the next ### or ## heading)
function extractSection(content: string, headingPattern: RegExp): string {
  const match = content.match(headingPattern);
  if (!match || match.index === undefined) return "";
  const start = match.index;
  const rest = content.slice(start + match[0].length);
  const nextHeading = rest.search(/^#{2,3}\s/m);
  return nextHeading === -1
    ? content.slice(start)
    : content.slice(start, start + match[0].length + nextHeading);
}

const eventGenSection = extractSection(
  claudeContent,
  /###.*Event.Driven.*Doc.*Generator/i,
);
const githubEventGenSection = extractSection(
  githubContent,
  /###.*Event.Driven.*Doc.*Generator/i,
);

describe("x-dev-lifecycle Event-Driven generator -- degenerate", () => {
  // UT-1: Heading exists
});

describe("x-dev-lifecycle Event-Driven generator -- unconditional", () => {
  // UT-2: Trigger condition
  // UT-3: Output path
  // UT-12: Dispatch table
});

describe("x-dev-lifecycle Event-Driven generator -- conditional", () => {
  // UT-4: Topics Overview
  // UT-5: Payload schema
  // UT-6: Producer/consumer
  // UT-7: Mermaid diagrams
});

describe("x-dev-lifecycle Event-Driven generator -- complex conditions", () => {
  // UT-8: Protocol references
  // UT-9: CloudEvents
  // UT-10: Schema versioning
});

describe("x-dev-lifecycle Event-Driven generator -- edge cases", () => {
  // UT-11: Positioning within Phase 3
  // UT-13: WebSocket vs Kafka differentiation
  // UT-14: Structural preservation
  // IT-2: Negative validation (trigger does not match cli)
});

describe("x-dev-lifecycle Event-Driven generator -- dual copy (RULE-001)", () => {
  // UT-15: Both copies consistent
});
```

---

## 7. TDD Execution Order

| Step | Action | Test State |
|------|--------|-----------|
| 1 | Write content validation tests (`tests/node/content/x-dev-lifecycle-event-doc.test.ts`) with all ~67 assertions | RED (source templates not yet modified) |
| 2 | Edit Claude source template -- add Event-Driven generator subagent prompt in Phase 3 | Partial GREEN (Claude tests pass, GitHub tests RED) |
| 3 | Edit GitHub source template -- add same Event-Driven generator section (RULE-001) | GREEN (all content + consistency tests pass) |
| 4 | Regenerate 24 golden files (script from Section 5.1) | N/A (golden files updated) |
| 5 | Run byte-for-byte integration tests | GREEN (golden file parity confirmed) |
| 6 | Run full test suite (`npx vitest run`) | GREEN (all existing + new tests pass) |

---

## 8. Test Count Summary

| Category | New Tests | Existing Tests |
|----------|-----------|----------------|
| Content validation -- Claude source | ~35 | 0 |
| Content validation -- GitHub source | ~18 | 0 |
| Dual copy consistency (RULE-001) | 11 | 0 |
| Trigger condition / negative (IT-2) | 3 | 0 |
| Golden file integration (IT-1) | 0 | 40 (8 profiles x 5 assertions) |
| **Total** | **~67** | **~40** |

---

## 9. Backward Compatibility Verification

These tests ensure no existing Phase 3 functionality is broken:

| Verification | Test(s) |
|--------------|---------|
| Phase 3 heading preserved | UT-14 (#49 -- interface field read) |
| Dispatch table entries for rest/grpc/cli/graphql preserved | UT-14 (#50) |
| Changelog generation preserved | UT-14 (#51) |
| No-interface skip log preserved | UT-14 (#52) |
| Output paths (docs/api/, docs/architecture/) preserved | UT-14 (#53) |

---

## 10. Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Golden file mismatch after source edit | Mechanical copy script eliminates drift; byte-for-byte tests catch any mismatch |
| Content tests too brittle | Use `toContain()` for substrings and `toMatch()` for regex; test semantic presence, not exact phrasing |
| Dual copy inconsistency (RULE-001) | Dedicated consistency tests (#54-#64) verify both copies have equivalent event-driven generator content |
| Generator section incorrectly positioned | UT-11 tests document-order of generator relative to Phase 3 and Phase 4 headings via index comparison |
| Trigger condition incorrectly matches non-event profiles | IT-2 validates trigger wording excludes `cli` and requires explicit event interface types |
| WebSocket vs Kafka not differentiated | UT-13 validates both protocol types are addressed with distinct terminology |
| Story dependency (story-0004-0005) incomplete | UT-14 preserves existing Phase 3 structure; tests assume Phase 3 already exists per dependency |

---

## 11. Verification Checklist

- [ ] `npx vitest run tests/node/content/x-dev-lifecycle-event-doc.test.ts` -- all ~67 new content tests pass
- [ ] `npx vitest run tests/node/integration/byte-for-byte.test.ts` -- all 8 profiles pass (40 assertions)
- [ ] `npx vitest run` -- full suite passes (1,384+ existing tests + ~67 new tests)
- [ ] Coverage remains >= 95% line, >= 90% branch (no TypeScript code changes, so coverage unaffected)
- [ ] No compiler/linter warnings introduced
- [ ] Deployed copy (`.claude/skills/x-dev-lifecycle/SKILL.md`) matches Claude source template exactly
- [ ] Both source templates contain identical Event-Driven generator semantics (RULE-001)
