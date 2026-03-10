# Test Plan — STORY-012: PatternsAssembler + ProtocolsAssembler

## Scope

Unit tests for `src/assembler/patterns-assembler.ts` and
`src/assembler/protocols-assembler.ts`, covering selection logic, file I/O,
consolidation, and broker-specific filtering. All tests use `vitest` and a
`mkdtempSync`-backed temporary directory that is cleaned up in `afterEach`.

**Target file:** `tests/node/assembler/patterns-assembler.test.ts`
and `tests/node/assembler/protocols-assembler.test.ts`

**Coverage targets:** ≥ 95 % line, ≥ 90 % branch.

---

## Conventions

- Test names follow `[methodOrBehavior]_[scenario]_[expectedBehavior]`.
- `buildConfig(overrides?)` helper mirrors the one in `skills-assembler.test.ts`;
  tests typically override `style` (mapped to `architecture.style`) and, for
  protocol scenarios, `interfaces`.
- `createPatternFile(resourcesDir, category, filename, content)` helper creates
  `{resourcesDir}/patterns/{category}/{filename}`.
- `createProtocolFile(resourcesDir, protocol, filename, content)` helper creates
  `{resourcesDir}/protocols/{protocol}/{filename}`.
- Both assemblers receive `(config, outputDir, resourcesDir, engine)` per the
  TypeScript data contract defined in the story.
- `TemplateEngine` is instantiated with `new TemplateEngine(resourcesDir, config)`.

---

## Part 1 — PatternsAssembler

### Test Group: `assemble` — early-exit paths

---

**Test PA-01**
- **Name:** `assemble_unknownArchitectureStyle_returnsEmptyArray`
- **Scenario:** Config has `architecture.style = "unknown-style"` which is not
  present in `ARCHITECTURE_PATTERNS`.
- **Setup:** No pattern files on disk; no categories will be derived.
- **Call:** `assembler.assemble(config, outputDir, resourcesDir, engine)`
- **Expected:**
  - Return value is `[]` (empty array).
  - No directories are created under `{outputDir}/skills/patterns/`.

---

**Test PA-02**
- **Name:** `assemble_categoriesDerivedButNoFilesOnDisk_returnsEmptyArray`
- **Scenario:** Config has `architecture.style = "microservice"` (valid style that
  produces categories), but the `{resourcesDir}/patterns/` directory is empty —
  no `.md` files exist under any category subdirectory.
- **Setup:** Create `{resourcesDir}/patterns/` directory but do NOT create any
  category subdirectories.
- **Call:** `assembler.assemble(config, outputDir, resourcesDir, engine)`
- **Expected:**
  - Return value is `[]`.
  - `{outputDir}/skills/patterns/SKILL.md` does NOT exist.

---

**Test PA-03**
- **Name:** `assemble_unknownStyle_noCategoriesEmptyDir_returnsEmptyArray`
- **Scenario:** Combination — unknown style AND no files on disk.
- **Setup:** `architecture.style = "serverless"`, no pattern resources.
- **Expected:** Return value is `[]`. (Exercises the first guard branch.)

---

### Test Group: `assemble` — microservice style (category selection)

---

**Test PA-04**
- **Name:** `assemble_microserviceStyle_includesArchitecturalDataResilienceIntegration`
- **Scenario:** Config has `architecture.style = "microservice"`,
  `architecture.eventDriven = false`.
  Pattern files exist for categories: `architectural`, `data`, `resilience`,
  `integration`.
- **Setup:**
  - `createPatternFile(resourcesDir, "architectural", "hexagonal.md", "# Hex")`
  - `createPatternFile(resourcesDir, "data", "cqrs.md", "# CQRS")`
  - `createPatternFile(resourcesDir, "resilience", "circuit-breaker.md", "# CB")`
  - `createPatternFile(resourcesDir, "integration", "anti-corruption.md", "# ACL")`
- **Call:** `assembler.assemble(config, outputDir, resourcesDir, engine)`
- **Expected:**
  - Return array length is 5 (4 individual files + 1 consolidated SKILL.md).
  - Returned paths include one entry per individual file:
    - `{outputDir}/skills/patterns/references/architectural/hexagonal.md`
    - `{outputDir}/skills/patterns/references/data/cqrs.md`
    - `{outputDir}/skills/patterns/references/resilience/circuit-breaker.md`
    - `{outputDir}/skills/patterns/references/integration/anti-corruption.md`
  - Returned paths include `{outputDir}/skills/patterns/SKILL.md`.
  - All five files physically exist on disk.

---

**Test PA-05**
- **Name:** `assemble_hexagonalStyle_includesArchitecturalDataIntegrationOnly`
- **Scenario:** Config has `architecture.style = "hexagonal"`,
  `architecture.eventDriven = false`. Only `architectural`, `data`, `integration`
  categories are selected; `resilience` and `microservice` are NOT selected.
- **Setup:** Create files for `architectural`, `data`, `integration` plus a stray
  file in `resilience/` that must NOT be picked up.
- **Expected:**
  - The `resilience` file is NOT in the return array.
  - Paths for `architectural`, `data`, and `integration` files ARE present.

---

**Test PA-06**
- **Name:** `assemble_libraryStyle_includesUniversalArchitecturalAndDataPatterns`
- **Scenario:** `architecture.style = "library"` maps to an empty list in
  `ARCHITECTURE_PATTERNS`, but `selectPatterns` still returns the
  `UNIVERSAL_PATTERNS` set (`architectural` + `data`) for any known style.
- **Setup:** Pattern files exist on disk for `architectural`, `data`, and at
  least one non-universal category such as `integration`.
- **Expected:**
  - Returned paths include only `architectural` and `data` category files.
  - No files from non-universal categories (e.g. `integration`, `resilience`)
    are included in the result.

---

### Test Group: `assemble` — event-driven patterns

---

**Test PA-07**
- **Name:** `assemble_eventDrivenTrue_includesSagaOutboxEventSourcingDlq`
- **Scenario:** Config has `architecture.style = "microservice"`,
  `architecture.eventDriven = true`.
- **Setup:**
  - `createPatternFile(resourcesDir, "architectural", "hex.md", "# Hex")`
  - `createPatternFile(resourcesDir, "saga-pattern", "saga.md", "# Saga")`
  - `createPatternFile(resourcesDir, "outbox-pattern", "outbox.md", "# Outbox")`
  - `createPatternFile(resourcesDir, "event-sourcing", "es.md", "# ES")`
  - `createPatternFile(resourcesDir, "dead-letter-queue", "dlq.md", "# DLQ")`
- **Expected:**
  - Return array includes paths for `saga-pattern/saga.md`,
    `outbox-pattern/outbox.md`, `event-sourcing/es.md`,
    `dead-letter-queue/dlq.md`.
  - `SKILL.md` content contains `# Saga`, `# Outbox`, `# ES`, `# DLQ`.

---

**Test PA-08**
- **Name:** `assemble_eventDrivenFalse_excludesEventDrivenPatterns`
- **Scenario:** Same architecture style `microservice` but `eventDriven = false`.
  Event-driven pattern directories exist on disk.
- **Setup:** Same files as PA-07.
- **Expected:**
  - Return array does NOT contain entries under `saga-pattern`, `outbox-pattern`,
    `event-sourcing`, or `dead-letter-queue`.

---

**Test PA-09**
- **Name:** `assemble_eventDrivenTrue_onlyPresentCategoriesAreIncluded`
- **Scenario:** `eventDriven = true` but only `saga-pattern` directory exists on
  disk; `outbox-pattern`, `event-sourcing`, `dead-letter-queue` directories are
  absent. Exercises the "skip missing directories without error" path.
- **Expected:**
  - Only `saga-pattern/saga.md` appears in results (plus SKILL.md).
  - No error is thrown.

---

### Test Group: `assemble` — individual file writes

---

**Test PA-10**
- **Name:** `assemble_writesIndividualFiles_underReferencesWithCategorySubdirectory`
- **Scenario:** Two files from two different categories.
- **Setup:**
  - `createPatternFile(resourcesDir, "architectural", "patterns.md", "# Arch")`
  - `createPatternFile(resourcesDir, "data", "data-patterns.md", "# Data")`
  - Config: `style = "microservice"`.
- **Expected:**
  - File exists at `{outputDir}/skills/patterns/references/architectural/patterns.md`.
  - File exists at `{outputDir}/skills/patterns/references/data/data-patterns.md`.
  - Content of each file matches source content (after placeholder rendering).

---

**Test PA-11**
- **Name:** `assemble_multipleFilesPerCategory_allWritten`
- **Scenario:** A single category has multiple `.md` files.
- **Setup:**
  - `createPatternFile(resourcesDir, "architectural", "a.md", "# A")`
  - `createPatternFile(resourcesDir, "architectural", "b.md", "# B")`
  - Config: `style = "microservice"`.
- **Expected:**
  - Both `references/architectural/a.md` and `references/architectural/b.md`
    exist on disk.

---

**Test PA-12**
- **Name:** `assemble_createsParentDirectories_automaticallyForEachCategory`
- **Scenario:** The `references/` directory does not pre-exist.
- **Setup:** Clean output dir, one pattern file.
- **Expected:**
  - `{outputDir}/skills/patterns/references/{category}/` is created automatically.
  - The individual file is written successfully.

---

### Test Group: `assemble` — SKILL.md consolidation

---

**Test PA-13**
- **Name:** `assemble_consolidatedSkillMd_containsAllRenderedContentJoinedBySeparator`
- **Scenario:** Two pattern files with distinct content.
- **Setup:**
  - `createPatternFile(resourcesDir, "architectural", "a.md", "# Section A")`
  - `createPatternFile(resourcesDir, "data", "b.md", "# Section B")`
  - Config: `style = "microservice"`.
- **Expected:**
  - `{outputDir}/skills/patterns/SKILL.md` content equals
    `"# Section A\n\n---\n\n# Section B"` (order determined by sorted category names,
    then sorted filenames within each category).

---

**Test PA-14**
- **Name:** `assemble_consolidatedSkillMd_singleFile_noSeparator`
- **Scenario:** Only one pattern file exists on disk.
- **Setup:** `createPatternFile(resourcesDir, "architectural", "only.md", "# Only")`
- **Expected:**
  - SKILL.md content is `"# Only"` — no separator prepended or appended.

---

**Test PA-15**
- **Name:** `assemble_consolidatedSkillMdIsAlwaysIncludedInReturnedPaths`
- **Scenario:** General assembly with any pattern files.
- **Expected:**
  - The last (or any) element in the returned array equals
    `{outputDir}/skills/patterns/SKILL.md`.
  - The file exists on disk.

---

**Test PA-16**
- **Name:** `assemble_consolidatedSkillMd_createsParentDirectoryAutomatically`
- **Scenario:** Clean output dir, no `skills/patterns/` pre-created.
- **Expected:**
  - `{outputDir}/skills/patterns/` is created automatically.
  - `SKILL.md` is written without error.

---

### Test Group: `assemble` — template engine (placeholder replacement)

---

**Test PA-17**
- **Name:** `assemble_renderedContent_placeholdersReplacedByEngine`
- **Scenario:** Pattern file contains `{project_name}` placeholder.
- **Setup:**
  - Config with `project.name = "my-app"`.
  - `createPatternFile(resourcesDir, "architectural", "p.md",
    "# Patterns for {project_name}")`
- **Expected:**
  - Individual file on disk contains `"# Patterns for my-app"`.
  - SKILL.md contains `"# Patterns for my-app"`.
  - Original placeholder `{project_name}` does NOT appear in either file.

---

**Test PA-18**
- **Name:** `assemble_renderedContent_sameRenderingUsedForBothIndividualAndConsolidated`
- **Scenario:** Verifies that the render pass is single: the same rendered string
  goes to both the individual file and the SKILL.md — no double-rendering.
- **Setup:** Pattern file with `{project_name}` placeholder.
- **Expected:**
  - Individual file content equals SKILL.md content (since there is only one file).

---

### Test Group: `assemble` — category directory structure

---

**Test PA-19**
- **Name:** `assemble_preservesCategoryDirectoryStructure_underReferences`
- **Scenario:** Files from three distinct categories are assembled.
- **Expected:**
  - `references/architectural/` directory exists.
  - `references/resilience/` directory exists.
  - `references/integration/` directory exists.
  - Each directory contains only its own category's files.

---

**Test PA-20**
- **Name:** `assemble_universalPatterns_alwaysPresentForAnyKnownStyle`
- **Scenario:** Uses each known style (`microservice`, `hexagonal`, `monolith`);
  `architectural` and `data` category files exist.
- **Setup (it.each):** Styles: `["microservice", "hexagonal", "monolith"]`.
- **Expected for each style:**
  - Return array includes the `architectural` and `data` pattern files.

---

## Part 2 — ProtocolsAssembler

### Test Group: `assemble` — early-exit paths

---

**Test PR-01**
- **Name:** `assemble_noInterfacesMapped_returnsEmptyArray`
- **Scenario:** Config has only interfaces that yield no protocol mappings —
  specifically `cli`, which maps to `[]` in `INTERFACE_PROTOCOL_MAP`.
- **Setup:** No protocol files on disk.
- **Expected:**
  - Return value is `[]`.
  - No directories created under `{outputDir}/skills/protocols/`.

---

**Test PR-02**
- **Name:** `assemble_protocolsDerivedButNoFilesOnDisk_returnsEmptyArray`
- **Scenario:** Config has `interfaces: [rest]` (produces protocol `rest`), but
  `{resourcesDir}/protocols/rest/` does NOT exist on disk. The `derive_protocol_files`
  function silently skips missing directories.
- **Setup:** Empty `resourcesDir`, no protocol directories.
- **Expected:**
  - Return value is `[]`.
  - `{outputDir}/skills/protocols/references/` does NOT exist.

---

**Test PR-03**
- **Name:** `assemble_emptyInterfaces_returnsEmptyArray`
- **Scenario:** Config with an empty `interfaces` array.
- **Expected:** Return value is `[]`.

---

### Test Group: `assemble` — rest + grpc interfaces

---

**Test PR-04**
- **Name:** `assemble_restAndGrpcInterfaces_generatesBothConventionsFiles`
- **Scenario:** Config has `interfaces: [rest, grpc]`. Protocol files exist for both.
- **Setup:**
  - `createProtocolFile(resourcesDir, "rest", "http-conventions.md", "# HTTP")`
  - `createProtocolFile(resourcesDir, "grpc", "grpc-conventions.md", "# gRPC")`
- **Call:** `assembler.assemble(config, outputDir, resourcesDir, engine)`
- **Expected:**
  - Return array has length 2.
  - `{outputDir}/skills/protocols/references/rest-conventions.md` exists.
  - `{outputDir}/skills/protocols/references/grpc-conventions.md` exists.

---

**Test PR-05**
- **Name:** `assemble_singleRestInterface_generatesOnlyRestConventions`
- **Scenario:** Config has `interfaces: [rest]`.
- **Setup:** `createProtocolFile(resourcesDir, "rest", "conventions.md", "# REST")`
- **Expected:**
  - Return array length is 1.
  - `rest-conventions.md` exists.
  - `grpc-conventions.md` does NOT exist.

---

**Test PR-06**
- **Name:** `assemble_eventConsumerInterface_generatesEventDrivenAndMessagingFiles`
- **Scenario:** Config has `interfaces: [event-consumer]`, which maps to
  `["event-driven", "messaging"]`.
- **Setup:**
  - `createProtocolFile(resourcesDir, "event-driven", "event.md", "# Events")`
  - `createProtocolFile(resourcesDir, "messaging", "messaging.md", "# Messaging")`
- **Expected:**
  - `event-driven-conventions.md` exists.
  - `messaging-conventions.md` exists.

---

### Test Group: `assemble` — output file naming

---

**Test PR-07**
- **Name:** `assemble_outputFilesNamedWithConventionsSuffix`
- **Scenario:** Various protocol names must produce `{protocol}-conventions.md`.
- **Setup (it.each):**

  | Interface | Protocol | Expected output filename |
  |-----------|----------|--------------------------|
  | `rest` | `rest` | `rest-conventions.md` |
  | `grpc` | `grpc` | `grpc-conventions.md` |
  | `graphql` | `graphql` | `graphql-conventions.md` |
  | `websocket` | `websocket` | `websocket-conventions.md` |

- **Expected:** For each row, the file exists under
  `{outputDir}/skills/protocols/references/`.

---

**Test PR-08**
- **Name:** `assemble_outputFiles_writtenUnderSkillsProtocolsReferences`
- **Scenario:** Verifies exact output directory.
- **Expected:**
  - Output directory is `{outputDir}/skills/protocols/references/`.
  - No files are written outside that directory.

---

### Test Group: `assemble` — concatenation with separator

---

**Test PR-09**
- **Name:** `assemble_multipleFilesPerProtocol_concatenatedWithSeparator`
- **Scenario:** The `rest` protocol directory contains two `.md` files.
- **Setup:**
  - `createProtocolFile(resourcesDir, "rest", "a.md", "# Part A")`
  - `createProtocolFile(resourcesDir, "rest", "b.md", "# Part B")`
  - Config: `interfaces: [rest]`.
- **Expected:**
  - `rest-conventions.md` content equals `"# Part A\n\n---\n\n# Part B"`.
    (Files sorted alphabetically, joined by `\n\n---\n\n`.)

---

**Test PR-10**
- **Name:** `assemble_singleFilePerProtocol_noSeparatorAdded`
- **Scenario:** Protocol directory contains exactly one file.
- **Expected:**
  - Convention file content equals the single file's raw content without
    any leading or trailing separator.

---

**Test PR-11**
- **Name:** `assemble_rawConcatenation_noTemplateEngineApplied`
- **Scenario:** Protocol source file contains `{project_name}` placeholder.
  The assembler must NOT pass content through the template engine — it must
  write the placeholder as-is.
- **Setup:**
  - `createProtocolFile(resourcesDir, "rest", "conv.md",
    "# REST for {project_name}")`
  - Config: project name `"my-app"`, `interfaces: [rest]`.
- **Expected:**
  - `rest-conventions.md` content contains the literal string `{project_name}`,
    NOT `my-app`.

---

### Test Group: `assemble` — broker-specific filtering for messaging

---

**Test PR-12**
- **Name:** `assemble_messagingProtocol_brokerKafka_includesOnlyKafkaFile`
- **Scenario:** Config has `interfaces: [{ type: "event-consumer", broker: "kafka" }]`.
  The `messaging` protocol directory contains `kafka.md` and `rabbitmq.md`.
- **Setup:**
  - `createProtocolFile(resourcesDir, "messaging", "kafka.md", "# Kafka")`
  - `createProtocolFile(resourcesDir, "messaging", "rabbitmq.md", "# RabbitMQ")`
  - `createProtocolFile(resourcesDir, "event-driven", "event.md", "# Events")`
- **Expected:**
  - `messaging-conventions.md` content equals `"# Kafka"` (only the broker-specific
    file; `rabbitmq.md` is excluded).

---

**Test PR-13**
- **Name:** `assemble_messagingProtocol_brokerRabbitmq_includesOnlyRabbitmqFile`
- **Scenario:** Same structure as PR-12 but with `broker: "rabbitmq"`.
- **Expected:**
  - `messaging-conventions.md` content equals `"# RabbitMQ"`.

---

**Test PR-14**
- **Name:** `assemble_messagingProtocol_noBrokerSpecified_includesAllMessagingFiles`
- **Scenario:** `interfaces: [{ type: "event-consumer" }]` — no broker.
  Messaging directory has `kafka.md` and `rabbitmq.md`.
- **Expected:**
  - `messaging-conventions.md` contains both files concatenated with separator.

---

**Test PR-15**
- **Name:** `assemble_messagingProtocol_brokerFileAbsent_fallsBackToAllFiles`
- **Scenario:** Broker is `"activemq"` but `activemq.md` does not exist in the
  messaging directory. Only `kafka.md` exists.
- **Expected:**
  - Falls back to all available files: `messaging-conventions.md` contains
    `kafka.md` content.

---

**Test PR-16**
- **Name:** `assemble_nonMessagingProtocols_notAffectedByBrokerFiltering`
- **Scenario:** Config has `rest` interface and a broker set on event interface.
  `rest` directory has multiple files.
- **Expected:**
  - `rest-conventions.md` contains ALL files from the `rest/` directory, concatenated.
  - Broker filtering applies ONLY to `messaging` protocol.

---

### Test Group: `assemble` — empty protocol file list edge case

---

**Test PR-17**
- **Name:** `assemble_emptyProtocolFileList_writesEmptyConventionsFile`
- **Scenario:** Exercises `_concat_protocol_dir` when called with an empty file list.
  In practice this happens if the broker-specific file is absent and the directory
  is empty. The directory exists (so `derive_protocol_files` includes the key),
  but the file list is empty.
- **Note:** This is an internal branch in `_concat_protocol_dir`. If the TypeScript
  implementation makes `derive_protocol_files` exclude protocols with empty file
  lists (as the Python does: `if files: result[protocol] = files`), this branch
  may be unreachable from `assemble`. Test should target the internal method
  directly OR verify the guard in `derive_protocol_files` that prevents empty
  lists from reaching `_concat_protocol_dir`. Document which applies.
- **Expected (option A — empty list reaches concat):**
  - `{protocol}-conventions.md` exists with content `""` (empty string).
- **Expected (option B — guard excludes empty lists):**
  - Protocol is not present in return array at all (protocol dir exists but
    has no matching `.md` files).

---

### Test Group: `assemble` — multi-interface deduplication

---

**Test PR-18**
- **Name:** `assemble_eventConsumerAndEventProducer_bothMapToSameProtocols_deduplicates`
- **Scenario:** Config has `interfaces: [event-consumer, event-producer]`. Both map
  to `["event-driven", "messaging"]`. After deduplication, only one set of
  convention files is created.
- **Setup:** Create files for `event-driven` and `messaging`.
- **Expected:**
  - Return array has length 2 (one file per unique protocol).
  - `event-driven-conventions.md` exists once.
  - `messaging-conventions.md` exists once.

---

**Test PR-19**
- **Name:** `assemble_unknownInterfaceWithEventPrefix_mapsToEventDrivenProtocol`
- **Scenario:** Config has `interfaces: [{ type: "event-stream" }]` — not in
  `INTERFACE_PROTOCOL_MAP` but starts with `"event-"`. Should map to
  `"event-driven"` protocol via the fallback branch in `derive_protocols`.
- **Setup:** `createProtocolFile(resourcesDir, "event-driven", "event.md", "# Events")`
- **Expected:**
  - `event-driven-conventions.md` exists.

---

## Summary Table

| ID | Assembler | Behavior Under Test | Key Assertion |
|----|-----------|---------------------|---------------|
| PA-01 | Patterns | Unknown architecture style → empty return | `result === []` |
| PA-02 | Patterns | Valid style, no files on disk → empty return | `result === []` |
| PA-03 | Patterns | Unknown style + no files → empty return | First guard branch |
| PA-04 | Patterns | Microservice: architectural+data+resilience+integration files written | 5 paths returned |
| PA-05 | Patterns | Hexagonal: only architectural+data+integration selected | `resilience` absent |
| PA-06 | Patterns | Library style → only universal patterns (architectural + data) | Universal categories present, non-universal absent |
| PA-07 | Patterns | eventDriven=true → saga+outbox+ES+DLQ patterns included | 4 extra files |
| PA-08 | Patterns | eventDriven=false → event-driven patterns excluded | Paths absent |
| PA-09 | Patterns | eventDriven=true, only some dirs exist → no error | Graceful skip |
| PA-10 | Patterns | Individual files written under references/{category}/ | File existence |
| PA-11 | Patterns | Multiple files per category all written | Both files exist |
| PA-12 | Patterns | Parent directories created automatically | Dir creation |
| PA-13 | Patterns | SKILL.md joins all rendered content with `\n\n---\n\n` | Exact content |
| PA-14 | Patterns | Single file → SKILL.md has no separator | Content without `---` |
| PA-15 | Patterns | SKILL.md path always in returned array | Path included |
| PA-16 | Patterns | SKILL.md parent dir created automatically | Dir creation |
| PA-17 | Patterns | Template engine replaces `{project_name}` in output | Placeholder resolved |
| PA-18 | Patterns | Same render used for individual file and SKILL.md | Content equality |
| PA-19 | Patterns | Category directories preserved under references/ | 3 category dirs exist |
| PA-20 | Patterns | Universal patterns (architectural, data) always present | it.each 3 styles |
| PR-01 | Protocols | CLI interface (maps to []) → empty return | `result === []` |
| PR-02 | Protocols | Protocols derived, no dirs on disk → empty return | `result === []` |
| PR-03 | Protocols | Empty interfaces array → empty return | `result === []` |
| PR-04 | Protocols | rest+grpc → both convention files generated | 2 files |
| PR-05 | Protocols | rest only → only rest-conventions.md | 1 file |
| PR-06 | Protocols | event-consumer → event-driven + messaging files | 2 files |
| PR-07 | Protocols | Output filename = `{protocol}-conventions.md` | it.each 4 protocols |
| PR-08 | Protocols | Output dir = skills/protocols/references/ | Exact path |
| PR-09 | Protocols | Multiple files concatenated with `\n\n---\n\n` | Exact content |
| PR-10 | Protocols | Single file → no separator | Content without `---` |
| PR-11 | Protocols | No template engine applied (raw concat) | Placeholder preserved |
| PR-12 | Protocols | Broker=kafka → only kafka.md included | Exclusion of rabbitmq |
| PR-13 | Protocols | Broker=rabbitmq → only rabbitmq.md included | Exclusion of kafka |
| PR-14 | Protocols | No broker → all messaging files included | All files present |
| PR-15 | Protocols | Broker file absent → fallback to all files | Graceful fallback |
| PR-16 | Protocols | Broker filtering only for messaging, not rest/grpc | rest unaffected |
| PR-17 | Protocols | Empty file list → empty string written OR guard excludes | See note |
| PR-18 | Protocols | Duplicate protocol derivation deduplicated | 2 files, not 4 |
| PR-19 | Protocols | Unknown event- prefix interface → event-driven protocol | Fallback mapping |

---

## Notes

1. **File ordering in SKILL.md:** The separator order mirrors Python — categories
   are sorted, then filenames within each category are sorted. Tests PA-13 and PR-09
   must seed files whose sort order is predictable (e.g., `a.md` before `b.md`).

2. **Return type:** TypeScript implementation returns `string[]` (absolute paths).
   All path assertions use `path.join(outputDir, ...)` for cross-platform
   compatibility.

3. **TemplateEngine scope:** PatternsAssembler passes content through
   `engine.replacePlaceholders()`; ProtocolsAssembler does raw `fs.readFileSync`
   only. Test PR-11 enforces this contract.

4. **broker extraction:** `_extract_broker` uses the first interface that has a
   non-empty `broker` field. Test PR-16 can set `broker` on a second interface
   to confirm it only affects `messaging`, not `rest`.

5. **it.each usage:** Tests PA-05, PA-07/PA-08, PA-20, PR-04/PR-05, PR-07, and
   PR-12/PR-13 are good candidates for `it.each` to reduce boilerplate, following
   the pattern established in `skills-assembler.test.ts`.
