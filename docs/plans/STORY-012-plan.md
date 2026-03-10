# STORY-012: PatternsAssembler + ProtocolsAssembler — Implementation Plan

## 1. New Files to Create

| File | Est. Lines | Purpose |
|------|------------|---------|
| `src/assembler/patterns-assembler.ts` | ~120 | `PatternsAssembler` class — selects patterns, renders via template engine, writes per-category refs, consolidates to `SKILL.md` |
| `src/assembler/protocols-assembler.ts` | ~100 | `ProtocolsAssembler` class — derives protocols, concatenates raw files (no template rendering) to `{protocol}-conventions.md` |
| `tests/node/assembler/patterns-assembler.test.ts` | ~300 | Unit tests for `PatternsAssembler` |
| `tests/node/assembler/protocols-assembler.test.ts` | ~250 | Unit tests for `ProtocolsAssembler` |

Both assemblers fit within the 250-line limit individually. No split into selection
modules is required (unlike STORY-010), since the selection logic already lives in
the domain layer (`pattern-mapping.ts`, `protocol-mapping.ts`).

## 2. Files to Modify

| File | Change |
|------|--------|
| `src/assembler/index.ts` | Add two export lines for the new modules |

```typescript
// --- STORY-012: PatternsAssembler + ProtocolsAssembler ---
export * from "./patterns-assembler.js";
export * from "./protocols-assembler.js";
```

No other production files require modification.

## 3. Class APIs

### 3.1 PatternsAssembler

```typescript
export class PatternsAssembler {
  assemble(
    config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    engine: TemplateEngine,
  ): AssembleResult;
}
```

Return type is `AssembleResult` (reusing `{ files: string[]; warnings: string[] }`
from `rules-assembler.ts`) — the story data contract specifies this shape.
The Python source returns `List[Path]` with no warnings, so `warnings` will always
be `[]` for `PatternsAssembler`.

### 3.2 ProtocolsAssembler

```typescript
export class ProtocolsAssembler {
  assemble(
    config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    engine: TemplateEngine,
  ): AssembleResult;
}
```

`engine` is accepted for API consistency with all other assemblers, but is **not
used** — protocols are concatenated raw (no placeholder replacement). This mirrors
the Python source exactly. `warnings` will always be `[]`.

## 4. Key Implementation Details

### 4.1 Module-level Constants

**`patterns-assembler.ts`:**
```typescript
const PATTERNS_SKILL_DIR  = "patterns";
const REFERENCES_DIR      = "references";
const SKILLS_DIR          = "skills";
const CONSOLIDATED_FILENAME = "SKILL.md";
const SECTION_SEPARATOR   = "\n\n---\n\n";
```

**`protocols-assembler.ts`:**
```typescript
const PROTOCOLS_SKILL_DIR  = "protocols";
const REFERENCES_DIR       = "references";
const SKILLS_DIR           = "skills";
const PROTOCOL_SEPARATOR   = "\n\n---\n\n";
const CONVENTIONS_SUFFIX   = "-conventions.md";
```

### 4.2 Python → TypeScript Differences

| Concern | Python | TypeScript |
|---------|--------|-----------|
| `resources_dir` placement | Constructor (`self._resources_dir`) | `assemble()` parameter (per TS convention from SkillsAssembler) |
| Path type | `pathlib.Path` | `string` + `node:path` |
| File read | `Path.read_text(encoding="utf-8")` | `fs.readFileSync(path, "utf-8")` |
| File write | `Path.write_text(content, encoding="utf-8")` | `fs.writeFileSync(path, content, "utf-8")` |
| Directory creation | `target_dir.mkdir(parents=True, exist_ok=True)` | `fs.mkdirSync(dir, { recursive: true })` |
| Template rendering | `engine.replace_placeholders(content)` | `engine.replacePlaceholders(content)` |
| Dict iteration (protocols) | `for protocol, files in sorted(protocol_files.items())` | `for (const protocol of Object.keys(protocolFiles).sort())` |
| Return type | `List[Path]` | `AssembleResult` |

### 4.3 PatternsAssembler: Critical Design Notes

**Render-once, write-twice pattern** — the Python assembler reads and renders each
pattern file exactly once, stores all rendered strings, then (a) writes individual
files under `references/{category}/filename.md` and (b) joins them with
`SECTION_SEPARATOR` into `SKILL.md`. The TS port must preserve this behaviour.

The category subdirectory for each reference file comes from the **parent directory
name** of the source file path (`path.basename(path.dirname(srcFile))`), matching
`src_file.parent.name` in Python.

Output paths:
- References: `{outputDir}/skills/patterns/references/{category}/{filename}.md`
- Consolidated: `{outputDir}/skills/patterns/SKILL.md`

Early-exit: if `selectPatterns()` returns `[]` or `selectPatternFiles()` returns
`[]`, return `{ files: [], warnings: [] }` immediately (no directories created).

Private method structure (all ≤ 25 lines):
```
assemble()              — orchestration, early-exit guards
renderContents()        — read + replacePlaceholders for each source file
buildRefsDir()          — path computation only
buildConsolidatedPath() — path computation only
flushPatterns()         — write per-category reference files
flushConsolidated()     — join with separator, write SKILL.md
```

### 4.4 ProtocolsAssembler: Critical Design Notes

**No template rendering** — protocol files are concatenated raw. The `engine`
parameter is received but never called. This is by design (matching Python behaviour).

Output path per protocol: `{outputDir}/skills/protocols/references/{protocol}-conventions.md`

Dictionary iteration must be **sorted by protocol name** to produce deterministic
output (matching `sorted(protocol_files.items())` in Python). Use
`Object.keys(protocolFiles).sort()` when iterating.

Empty protocol files list: write an empty string to the destination (matching
`dest_path.write_text("", encoding="utf-8")` in Python — though `deriveProtocolFiles`
already filters out empty protocols, this edge case exists in `_concat_protocol_dir`).

Private method structure (all ≤ 25 lines):
```
assemble()           — orchestration, early-exit guards
buildRefsDir()       — path computation only
generateOutput()     — iterate sorted protocols, call concatProtocolFiles
concatProtocolFiles() — read each file, join with PROTOCOL_SEPARATOR, write
```

### 4.5 Dependency Direction

```
patterns-assembler.ts
  |-- import --> src/domain/pattern-mapping.ts   (selectPatterns, selectPatternFiles)
  |-- import --> src/models.ts                   (ProjectConfig)
  |-- import --> src/template-engine.ts          (TemplateEngine)
  |-- import --> src/assembler/rules-assembler.ts (AssembleResult type)
  |-- import --> node:fs, node:path

protocols-assembler.ts
  |-- import --> src/domain/protocol-mapping.ts  (deriveProtocols, deriveProtocolFiles)
  |-- import --> src/models.ts                   (ProjectConfig)
  |-- import --> src/template-engine.ts          (TemplateEngine — param only, unused)
  |-- import --> src/assembler/rules-assembler.ts (AssembleResult type)
  |-- import --> node:fs, node:path
```

All dependencies point inward. No circular dependencies introduced.

## 5. Test Plan Outline

### 5.1 PatternsAssembler Tests (`patterns-assembler.test.ts`)

**Test fixtures (helpers):**
```typescript
function buildConfig(style: string, eventDriven?: boolean): ProjectConfig
function createPatternFile(resourcesDir: string, category: string, name: string, content?: string): string
function setupResources(tmpDir: string): string  // returns resourcesDir
```

**Test groups:**

| Group | Scenarios | Technique |
|-------|-----------|-----------|
| `assemble — empty result` | Unknown arch style returns `{ files: [], warnings: [] }` | Config with style `"unknown"` — no fs fixture needed |
| `assemble — empty result` | Known style with no pattern files in resourcesDir returns `{ files: [], warnings: [] }` | Config with `"microservice"` but no pattern dirs created |
| `assemble — references written` | Microservice: resilience + integration + architectural + data categories written to `references/` | Create fixture files per category, assert all paths present |
| `assemble — references path structure` | Files land at `skills/patterns/references/{category}/{name}.md` | Assert exact path |
| `assemble — SKILL.md consolidated` | `skills/patterns/SKILL.md` exists and contains all rendered content | Read output, check content includes each file's content |
| `assemble — SKILL.md separator` | Sections separated by `\n\n---\n\n` | Read output, assert separator present |
| `assemble — placeholder replacement` | Content with `{project_name}` is replaced in both ref files and SKILL.md | Write `{project_name}` in fixture, assert replaced |
| `assemble — event-driven` | `eventDriven: true` adds saga, outbox, event-sourcing, dead-letter-queue patterns | Create fixtures, assert all four categories present |
| `assemble — library style` | `style: "library"` → only universal patterns (architectural + data) | Assert only 2 categories |
| `assemble — universal always present` | Any valid style always includes `architectural` and `data` | |
| `assemble — files returned` | `files` array contains all written paths (refs + SKILL.md) | Assert length and paths |
| `assemble — warnings empty` | `warnings` is always `[]` | |

**Data-driven with `it.each`:**
```typescript
it.each([
  ["microservice", false, ["architectural", "data", "integration", "microservice", "resilience"]],
  ["hexagonal", false, ["architectural", "data", "integration"]],
  ["monolith", false, ["architectural", "data", "integration"]],
  ["library", false, ["architectural", "data"]],
])("selectsCorrectCategories_%s_eventDriven=%s", ...)
```

### 5.2 ProtocolsAssembler Tests (`protocols-assembler.test.ts`)

**Test fixtures (helpers):**
```typescript
function buildConfig(interfaces: string[], broker?: string): ProjectConfig
function createProtocolFile(resourcesDir: string, protocol: string, name: string, content?: string): string
function setupResources(tmpDir: string): string
```

**Test groups:**

| Group | Scenarios | Technique |
|-------|-----------|-----------|
| `assemble — empty result` | No interfaces returns `{ files: [], warnings: [] }` | Config with `interfaces: ["cli"]` |
| `assemble — empty result` | Interfaces that produce no files (no fixtures) returns `[]` | Config with `"rest"` but no protocol dirs |
| `assemble — REST protocol` | `rest` interface → `rest-conventions.md` in `references/` | Create fixture, assert path |
| `assemble — gRPC protocol` | `grpc` interface → `grpc-conventions.md` | |
| `assemble — multiple protocols` | `[rest, grpc]` → two files, sorted by protocol name | Assert both files exist |
| `assemble — concatenation with separator` | Multiple files in one protocol dir joined by `\n\n---\n\n` | Create 2 fixtures in `protocols/rest/`, assert separator in output |
| `assemble — output path structure` | Files land at `skills/protocols/references/{protocol}-conventions.md` | Assert exact path |
| `assemble — NO template rendering` | Content with `{project_name}` is NOT replaced | Write placeholder in fixture, assert it is unchanged in output |
| `assemble — event-consumer → event-driven + messaging` | `event-consumer` produces `event-driven` and `messaging` protocols | Create both dirs |
| `assemble — broker filtering` | `event-consumer` with `broker: "kafka"` → only `kafka.md` from messaging dir | Create `kafka.md` + `rabbitmq.md`, assert only kafka content |
| `assemble — broker fallback` | `event-consumer` with unknown broker → all messaging files included | Create multiple files, no specific broker file |
| `assemble — sorted output` | Multiple protocols written in alphabetical order | Assert files list is sorted |
| `assemble — warnings empty` | `warnings` is always `[]` | |
| `assemble — engine param unused` | Accepts engine but does not alter file content | |

**Data-driven with `it.each`:**
```typescript
it.each([
  [["rest"], ["rest"]],
  [["grpc"], ["grpc"]],
  [["graphql"], ["graphql"]],
  [["websocket"], ["websocket"]],
  [["event-consumer"], ["event-driven", "messaging"]],
  [["event-producer"], ["event-driven", "messaging"]],
  [["cli"], []],
])("derivesProtocols_%s → %s", ...)
```

### 5.3 Coverage Target

- Line coverage ≥ 95%, branch coverage ≥ 90%
- All early-exit branches (empty categories, empty files) must be explicitly tested
- Broker filtering has three branches: specific file exists, specific file missing (fallback), no broker configured

## 6. Implementation Order

1. Create `src/assembler/patterns-assembler.ts`
   - Constants, class skeleton, `assemble()` with early-exit
   - `renderContents()`, `buildRefsDir()`, `buildConsolidatedPath()`
   - `flushPatterns()`, `flushConsolidated()`
2. Create `src/assembler/protocols-assembler.ts`
   - Constants, class skeleton, `assemble()` with early-exit
   - `buildRefsDir()`, `generateOutput()`, `concatProtocolFiles()`
3. Update `src/assembler/index.ts` with two new exports
4. Create `tests/node/assembler/patterns-assembler.test.ts`
5. Create `tests/node/assembler/protocols-assembler.test.ts`
6. Compile: `npx tsc --noEmit`
7. Test: `npx vitest run --coverage` — verify ≥ 95% line / ≥ 90% branch

## 7. Risk Assessment

**Low risk overall.** Both assemblers are simpler than AgentsAssembler (no
checklist injection) and SkillsAssembler (no multi-category template scanning).

| Risk | Impact | Mitigation |
|------|--------|-----------|
| `resourcesDir` as `assemble()` param (not constructor) | Low — TS convention already established | Follow SkillsAssembler pattern |
| Category subdir derived from source path parent | Low — straightforward `path.basename(path.dirname(src))` | Test with real fixture files |
| Protocol dict iteration order must be sorted | Low | Use `Object.keys(...).sort()` explicitly |
| `engine` accepted but unused in `ProtocolsAssembler` | Low — by design | Add JSDoc `@param engine - Accepted for API consistency; not used` |
| `AssembleResult` import creates dependency on `rules-assembler.ts` | Negligible | Same pattern as `agents-assembler.ts` |
