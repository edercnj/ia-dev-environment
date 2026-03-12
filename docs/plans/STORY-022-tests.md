# Test Plan -- STORY-022: CodexAgentsMdAssembler

## Summary

- New test file: `tests/node/assembler/codex-agents-md-assembler.test.ts`
- Total test methods: 34
- Categories: Unit (scanAgents, scanSkills, buildExtendedContext, assemble), Integration (Pipeline)
- Coverage targets: >= 95% line, >= 90% branch
- Module under test: `src/assembler/codex-agents-md-assembler.ts`
- Performance budget: < 5s (file I/O against temp directories + Nunjucks rendering)

---

## 1. Test File Location and Naming

**Path:** `tests/node/assembler/codex-agents-md-assembler.test.ts`

**Rationale:** Follows existing convention for assembler tests (`tests/node/assembler/`). The module under test is an assembler, not a template, so it belongs alongside other assembler tests rather than in `tests/node/`.

**Naming convention:** `[methodUnderTest]_[scenario]_[expectedBehavior]` per Rule 05.

---

## 2. Test Infrastructure

### 2.1 Temp Directory Setup

Each test group uses a temporary directory created via `mkdtemp` in a `beforeEach` hook and removed in `afterEach`. The temp directory simulates the pipeline output directory where previous assemblers (AgentsAssembler, SkillsAssembler) have already written files.

```typescript
import { mkdtemp, rm, mkdir, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import { describe, it, expect, beforeEach, afterEach } from "vitest";
import { TemplateEngine } from "../../../src/template-engine.js";
import {
  aFullProjectConfig,
  aMinimalProjectConfig,
} from "../../fixtures/project-config.fixture.js";

const RESOURCES_DIR = resolve(__dirname, "../../../resources");

let tempDir: string;

beforeEach(async () => {
  tempDir = await mkdtemp(join(tmpdir(), "codex-assembler-test-"));
});

afterEach(async () => {
  await rm(tempDir, { recursive: true, force: true });
});
```

### 2.2 Helper Functions

```typescript
/** Create a .claude/agents/ directory with N agent .md files. */
async function seedAgents(
  outputDir: string,
  agents: Array<{ name: string; content: string }>,
): Promise<void> {
  const agentsDir = join(outputDir, ".claude", "agents");
  await mkdir(agentsDir, { recursive: true });
  for (const agent of agents) {
    await writeFile(join(agentsDir, `${agent.name}.md`), agent.content);
  }
}

/** Create a .claude/skills/ directory with N skill directories containing SKILL.md. */
async function seedSkills(
  outputDir: string,
  skills: Array<{ name: string; frontmatter: string }>,
): Promise<void> {
  const skillsDir = join(outputDir, ".claude", "skills");
  await mkdir(skillsDir, { recursive: true });
  for (const skill of skills) {
    const skillDir = join(skillsDir, skill.name);
    await mkdir(skillDir, { recursive: true });
    await writeFile(join(skillDir, "SKILL.md"), skill.frontmatter);
  }
}

/** Create a .claude/hooks/ directory with a dummy hook file. */
async function seedHooks(outputDir: string): Promise<void> {
  const hooksDir = join(outputDir, ".claude", "hooks");
  await mkdir(hooksDir, { recursive: true });
  await writeFile(join(hooksDir, "post-compile-check.sh"), "#!/bin/bash\n");
}
```

---

## 3. Test Fixtures

### 3.1 Agent File Content Variants

```typescript
// First-line description
const AGENT_PLAIN = "System architect persona for design decisions.\n\nDetailed instructions...";

// Title-based description
const AGENT_TITLED = "# Technical Lead\n\nLeads code reviews and architectural decisions.";
```

### 3.2 Skill Frontmatter Variants

```typescript
const SKILL_INVOCABLE = `---
name: x-dev-implement
description: Implements a feature following project conventions
user-invocable: true
---
# Skill instructions...`;

const SKILL_KP = `---
name: coding-standards
description: Coding standards knowledge pack
user-invocable: false
---
# Knowledge pack content...`;
```

### 3.3 ResolvedStack Fixture

```typescript
const RESOLVED_STACK = {
  buildCmd: "npm run build",
  testCmd: "npm test",
  compileCmd: "npx tsc --noEmit",
  coverageCmd: "npm run test:coverage",
  dockerBaseImage: "node:20-alpine",
  healthPath: "/health",
  packageManager: "npm",
  defaultPort: 3000,
  fileExtension: ".ts",
  buildFile: "package.json",
  nativeSupported: false,
  projectType: "microservice",
  protocols: ["rest"],
};
```

---

## 4. Test Groups

### Group 1: scanAgents() (7 tests)

Unit tests for the agent scanning function that reads `.claude/agents/` directory.

| # | Test Name | Setup | Assertion |
|---|-----------|-------|-----------|
| 1 | `scanAgents_emptyDir_returnsEmptyArray` | Create empty `.claude/agents/` directory | Returns `[]` |
| 2 | `scanAgents_missingDir_returnsEmptyArray` | No `.claude/agents/` directory exists | Returns `[]` |
| 3 | `scanAgents_oneAgent_returnsSingleAgentInfo` | Seed 1 agent file (`architect.md`) | Returns `[{ name: "architect", description: "..." }]` |
| 4 | `scanAgents_multipleAgents_returnsSortedArray` | Seed 3 agent files: `tech-lead.md`, `architect.md`, `qa-engineer.md` | Returns array sorted alphabetically by name: `["architect", "qa-engineer", "tech-lead"]` |
| 5 | `scanAgents_plainFirstLine_extractsDescriptionFromFirstLine` | Seed agent with plain text first line (no `#` prefix) | Description equals the first line content |
| 6 | `scanAgents_titleLine_extractsDescriptionFromTitle` | Seed agent with `# Title` as first line | Description equals `"Title"` (stripped `#` prefix) |
| 7 | `scanAgents_nonMdFiles_ignoresNonMarkdownFiles` | Seed `.claude/agents/` with `agent.md` + `README.txt` + `config.json` | Returns only 1 entry (the `.md` file) |

#### Assertions Pattern

```typescript
// Test 1
const result = scanAgents(tempDir);
expect(result).toEqual([]);

// Test 3
const result = scanAgents(tempDir);
expect(result).toHaveLength(1);
expect(result[0]).toEqual({ name: "architect", description: expect.any(String) });

// Test 4
const result = scanAgents(tempDir);
expect(result).toHaveLength(3);
expect(result[0].name).toBe("architect");
expect(result[1].name).toBe("qa-engineer");
expect(result[2].name).toBe("tech-lead");

// Test 6
const result = scanAgents(tempDir);
expect(result[0].description).toBe("Technical Lead");
```

---

### Group 2: scanSkills() (7 tests)

Unit tests for the skill scanning function that reads `.claude/skills/` subdirectories.

| # | Test Name | Setup | Assertion |
|---|-----------|-------|-----------|
| 8 | `scanSkills_emptyDir_returnsEmptyArray` | Create empty `.claude/skills/` directory | Returns `[]` |
| 9 | `scanSkills_missingDir_returnsEmptyArray` | No `.claude/skills/` directory exists | Returns `[]` |
| 10 | `scanSkills_oneSkill_returnsSingleSkillInfo` | Seed 1 skill directory with `SKILL.md` | Returns `[{ name, description, user_invocable }]` |
| 11 | `scanSkills_multipleSkills_returnsSortedArray` | Seed 3 skill directories: `x-review`, `x-dev-implement`, `coding-standards` | Returns array sorted alphabetically by name |
| 12 | `scanSkills_userInvocableTrue_setsUserInvocableTrue` | Seed skill with `user-invocable: true` in frontmatter | `user_invocable` is `true` |
| 13 | `scanSkills_userInvocableFalse_setsUserInvocableFalse` | Seed skill with `user-invocable: false` in frontmatter | `user_invocable` is `false` |
| 14 | `scanSkills_dirWithoutSkillMd_ignoresDirectory` | Seed `.claude/skills/` with one dir containing `SKILL.md` and one dir without it | Returns only 1 entry |

#### Assertions Pattern

```typescript
// Test 10
const result = scanSkills(tempDir);
expect(result).toHaveLength(1);
expect(result[0]).toEqual({
  name: "x-dev-implement",
  description: "Implements a feature following project conventions",
  user_invocable: true,
});

// Test 12
const result = scanSkills(tempDir);
expect(result[0].user_invocable).toBe(true);

// Test 13
const result = scanSkills(tempDir);
expect(result[0].user_invocable).toBe(false);
```

---

### Group 3: buildExtendedContext() (7 tests)

Unit tests for context construction that merges flat config context with extended fields.

| # | Test Name | Setup | Assertion |
|---|-----------|-------|-----------|
| 15 | `buildExtendedContext_fullConfig_returnsAllFields` | Full config, agents, skills, hooks, MCP, security | Result has >= 30 keys (24 flat + 6 extended) |
| 16 | `buildExtendedContext_fullConfig_includesResolvedStackFields` | Full config with resolved stack | Result contains `resolved_stack` with all ResolvedStack fields |
| 17 | `buildExtendedContext_fullConfig_includesAgentsList` | 3 agents seeded | `agents_list` has 3 entries with `name` and `description` |
| 18 | `buildExtendedContext_fullConfig_includesSkillsList` | 2 skills seeded | `skills_list` has 2 entries with `name`, `description`, `user_invocable` |
| 19 | `buildExtendedContext_withHooks_setsHasHooksTrue` | Hooks directory seeded | `has_hooks` is `true` |
| 20 | `buildExtendedContext_fullConfig_includesMcpServers` | Config with MCP servers | `mcp_servers` array matches config MCP servers |
| 21 | `buildExtendedContext_fullConfig_includesSecurityFrameworks` | Config with security frameworks `["owasp", "pci-dss"]` | `security_frameworks` equals `["owasp", "pci-dss"]` |

#### Assertions Pattern

```typescript
// Test 15
const ctx = buildExtendedContext(config, resolvedStack, agents, skills, hasHooks);
expect(Object.keys(ctx).length).toBeGreaterThanOrEqual(30);
expect(ctx).toHaveProperty("project_name");
expect(ctx).toHaveProperty("resolved_stack");
expect(ctx).toHaveProperty("agents_list");
expect(ctx).toHaveProperty("skills_list");
expect(ctx).toHaveProperty("has_hooks");
expect(ctx).toHaveProperty("mcp_servers");
expect(ctx).toHaveProperty("security_frameworks");

// Test 16
expect(ctx.resolved_stack).toEqual(expect.objectContaining({
  buildCmd: "npm run build",
  testCmd: "npm test",
  compileCmd: "npx tsc --noEmit",
  coverageCmd: "npm run test:coverage",
}));

// Test 19
expect(ctx.has_hooks).toBe(true);
```

---

### Group 4: assemble() (11 tests)

Unit tests for the full assemble orchestration covering the 3-phase execution.

| # | Test Name | Setup | Assertion |
|---|-----------|-------|-----------|
| 22 | `assemble_fullConfig_generatesAgentsMdFile` | Full config, seeded agents + skills + hooks | File `.codex/AGENTS.md` exists in output directory |
| 23 | `assemble_fullConfig_returnsFileAndNoWarnings` | Full config, seeded agents + skills | Returns `{ files: [".codex/AGENTS.md"], warnings: [] }` |
| 24 | `assemble_minimalConfig_omitsDomainSection` | Minimal config with `domain_driven=false` | Generated AGENTS.md does NOT contain `"## Domain"` |
| 25 | `assemble_minimalConfig_omitsSecuritySection` | Minimal config with empty security frameworks | Generated AGENTS.md does NOT contain `"## Security"` |
| 26 | `assemble_noAgents_returnsWarning` | Empty agents directory | `warnings` contains `"No agents found"` (or similar message) |
| 27 | `assemble_noSkills_returnsWarning` | Empty skills directory | `warnings` contains `"No skills found"` (or similar message) |
| 28 | `assemble_noAgents_omitsAgentsSection` | Empty agents directory | Generated AGENTS.md does NOT contain `"## Agent Personas"` |
| 29 | `assemble_noSkills_omitsSkillsSection` | Empty skills directory | Generated AGENTS.md does NOT contain `"## Available Skills"` |
| 30 | `assemble_codexDirNotExists_createsDirectory` | No `.codex/` directory pre-exists | `.codex/` directory is created and contains `AGENTS.md` |
| 31 | `assemble_fullConfig_noTemplateArtifacts` | Full config, seeded agents + skills | Generated AGENTS.md does NOT match `/\{\{/`, `/\{%/`, `/\{#/` |
| 32 | `assemble_fullConfig_noExcessiveBlankLines` | Full config, seeded agents + skills | Generated AGENTS.md does NOT match `/\n{5,}/` |

#### Setup

```typescript
const config = aFullProjectConfig();
const engine = new TemplateEngine(RESOURCES_DIR, config);
const assembler = new CodexAgentsMdAssembler();

// Seed previous assembler output
await seedAgents(tempDir, [
  { name: "architect", content: "System architect persona." },
  { name: "tech-lead", content: "# Technical Lead" },
  { name: "qa-engineer", content: "Quality assurance engineer." },
]);
await seedSkills(tempDir, [
  { name: "x-dev-implement", frontmatter: SKILL_INVOCABLE },
  { name: "coding-standards", frontmatter: SKILL_KP },
]);
await seedHooks(tempDir);

const result = await assembler.assemble(config, tempDir, RESOURCES_DIR, engine, RESOLVED_STACK);
```

#### Assertions Pattern

```typescript
// Test 22
const agentsMdPath = join(tempDir, ".codex", "AGENTS.md");
expect(fs.existsSync(agentsMdPath)).toBe(true);

// Test 23
expect(result.files).toEqual([".codex/AGENTS.md"]);
expect(result.warnings).toEqual([]);

// Test 24
const content = fs.readFileSync(join(tempDir, ".codex", "AGENTS.md"), "utf-8");
expect(content).not.toContain("## Domain");

// Test 26
expect(result.warnings).toEqual(
  expect.arrayContaining([expect.stringContaining("No agents found")]),
);

// Test 30
expect(fs.existsSync(join(tempDir, ".codex"))).toBe(true);
expect(fs.existsSync(join(tempDir, ".codex", "AGENTS.md"))).toBe(true);

// Test 31
expect(content).not.toMatch(/\{\{/);
expect(content).not.toMatch(/\{%/);
expect(content).not.toMatch(/\{#/);

// Test 32
expect(content).not.toMatch(/\n{5,}/);
```

---

### Group 5: Pipeline Integration (2 tests)

Integration tests validating that `CodexAgentsMdAssembler` is correctly wired into the pipeline.

| # | Test Name | Assertion |
|---|-----------|-----------|
| 33 | `pipeline_assemblerList_includesCodexAgentsMdAssembler` | `buildAssemblers()` returns a descriptor with name `"CodexAgentsMdAssembler"` |
| 34 | `pipeline_assemblerOrder_codexAgentsMdAfterAgentsAndSkills` | `CodexAgentsMdAssembler` index is greater than both `AgentsAssembler` and `SkillsAssembler` indexes |

#### Assertions Pattern

```typescript
// Test 33
const assemblers = buildAssemblers();
const names = assemblers.map((a) => a.name);
expect(names).toContain("CodexAgentsMdAssembler");

// Test 34
const assemblers = buildAssemblers();
const names = assemblers.map((a) => a.name);
const codexIdx = names.indexOf("CodexAgentsMdAssembler");
const agentsIdx = names.indexOf("AgentsAssembler");
const skillsIdx = names.indexOf("SkillsAssembler");
expect(codexIdx).toBeGreaterThan(agentsIdx);
expect(codexIdx).toBeGreaterThan(skillsIdx);
```

---

## 5. Coverage Strategy

### 5.1 Branch Coverage Matrix

Every conditional path in the assembler is exercised in both true and false states:

| Condition | True Test IDs | False Test IDs |
|-----------|---------------|----------------|
| Agents directory exists | 1, 3, 4, 5, 6, 7 | 2 |
| Agents directory has `.md` files | 3, 4, 5, 6 | 1 |
| Skills directory exists | 8, 10, 11, 12, 13, 14 | 9 |
| Skills directory has `SKILL.md` | 10, 11, 12, 13 | 8, 14 |
| `user-invocable: true` in frontmatter | 12 | 13 |
| Hooks directory exists | 19, 22 | 9 (implicit) |
| `domain_driven == "True"` | 22 | 24 |
| `security_frameworks.length > 0` | 22, 21 | 25 |
| `agents_list.length > 0` | 22 | 26, 28 |
| `skills_list.length > 0` | 22 | 27, 29 |
| `.codex/` directory pre-exists | 22 | 30 |

### 5.2 Line Coverage

Every code path in the assembler module is exercised:

- **scanAgents()**: Tests 1-7 cover empty, missing, single, multiple, description extraction, and file filtering.
- **scanSkills()**: Tests 8-14 cover empty, missing, single, multiple, frontmatter parsing, and directory filtering.
- **buildExtendedContext()**: Tests 15-21 cover all 6 extended fields plus the 24 flat context fields.
- **assemble()**: Tests 22-32 cover full execution, conditional section omission, warnings, directory creation, and output quality.
- **Pipeline wiring**: Tests 33-34 cover assembler registration and ordering.

### 5.3 Coverage Targets

| Metric | Target | Strategy |
|--------|--------|----------|
| Line | >= 95% | All code paths exercised via unit + integration tests |
| Branch | >= 90% | Every conditional (directory existence, array length, frontmatter values) tested in both states |

---

## 6. Test Matrix Summary

| Group | Description | Test Count | Type |
|-------|-------------|------------|------|
| G1: scanAgents | Agent directory scanning and description extraction | 7 | Unit |
| G2: scanSkills | Skill directory scanning and frontmatter parsing | 7 | Unit |
| G3: buildExtendedContext | Context construction with flat + extended fields | 7 | Unit |
| G4: assemble | Full 3-phase assembler orchestration | 11 | Unit |
| G5: Pipeline | Pipeline integration and ordering | 2 | Integration |
| **Total** | | **34** | |

---

## 7. Execution Commands

### Run CodexAgentsMdAssembler Tests Only

```bash
npx vitest run tests/node/assembler/codex-agents-md-assembler.test.ts
```

### Run with Coverage

```bash
npx vitest run --coverage tests/node/assembler/codex-agents-md-assembler.test.ts
```

### Full Test Suite (regression check)

```bash
npx vitest run
```

---

## 8. Dependencies and Prerequisites

### Prerequisites

- STORY-021 templates exist in `resources/codex-templates/` (including `agents-md.md.njk`)
- `aFullProjectConfig()` and `aMinimalProjectConfig()` fixtures available in `tests/fixtures/project-config.fixture.ts`
- `TemplateEngine` with `renderTemplate()` functional
- `buildDefaultContext()` exported from `src/template-engine.ts`

### Import Dependencies

| Module | Import | Used For |
|--------|--------|----------|
| `src/assembler/codex-agents-md-assembler.ts` | `CodexAgentsMdAssembler`, `scanAgents`, `scanSkills`, `buildExtendedContext` | Module under test |
| `src/assembler/pipeline.ts` | `buildAssemblers` | Pipeline integration tests |
| `src/template-engine.ts` | `TemplateEngine` | Rendering templates |
| `tests/fixtures/project-config.fixture.ts` | `aFullProjectConfig`, `aMinimalProjectConfig` | Fixture configs |
| `src/domain/resolved-stack.ts` | `ResolvedStack` | Type for resolved stack fixture |
| `vitest` | `describe`, `it`, `expect`, `beforeEach`, `afterEach` | Test framework |
| `node:fs/promises` | `mkdtemp`, `rm`, `mkdir`, `writeFile` | Temp directory management |
| `node:fs` | `existsSync`, `readFileSync` | File existence and content checks |
| `node:path` | `join`, `resolve` | Path resolution |

---

## 9. Vitest Configuration Notes

- The existing `vitest.config.ts` pattern `tests/**/*.test.ts` automatically discovers `tests/node/assembler/codex-agents-md-assembler.test.ts`. No config changes needed.
- Pool: `forks` with `maxForks: 3`, `maxConcurrency: 5` (project-wide setting).
- Each test creates and destroys a temp directory. The `beforeEach`/`afterEach` pattern ensures clean isolation between tests.
- Expected duration: < 5s. File I/O is against the OS temp directory and Nunjucks rendering is synchronous in-memory.

---

## 10. Risk Areas

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Temp directory cleanup fails on test error | Low | Low | `afterEach` with `force: true` ensures cleanup even on assertion failures |
| Frontmatter parsing edge cases (missing fields, malformed YAML) | Medium | Medium | Tests 12-13 validate both `true` and `false` values. Implementation should handle missing `user-invocable` with a sensible default. |
| Agent description extraction with empty files | Low | Medium | Test 5 validates first-line extraction; implementation should handle empty files gracefully (return empty description). |
| Pipeline ordering changes break test 34 | Low | High | Test 34 uses relative ordering (greater-than), not absolute index, so inserting new assemblers elsewhere does not break it. |
| `assemble()` signature differs from story spec | Medium | High | Story defines `assemble(config, outputDir, resourcesDir, engine, resolvedStack)` with 5 params; tests follow this signature. If implementation changes, tests must adapt. |
| Race conditions in parallel test execution | Low | Low | Each test uses its own unique temp directory via `mkdtemp`. No shared mutable state. |

---

## 11. Naming Convention Reference

All test names follow `[methodUnderTest]_[scenario]_[expectedBehavior]`:

```
scanAgents_emptyDir_returnsEmptyArray
scanAgents_missingDir_returnsEmptyArray
scanAgents_oneAgent_returnsSingleAgentInfo
scanAgents_multipleAgents_returnsSortedArray
scanAgents_plainFirstLine_extractsDescriptionFromFirstLine
scanAgents_titleLine_extractsDescriptionFromTitle
scanAgents_nonMdFiles_ignoresNonMarkdownFiles
scanSkills_emptyDir_returnsEmptyArray
scanSkills_missingDir_returnsEmptyArray
scanSkills_oneSkill_returnsSingleSkillInfo
scanSkills_multipleSkills_returnsSortedArray
scanSkills_userInvocableTrue_setsUserInvocableTrue
scanSkills_userInvocableFalse_setsUserInvocableFalse
scanSkills_dirWithoutSkillMd_ignoresDirectory
buildExtendedContext_fullConfig_returnsAllFields
buildExtendedContext_fullConfig_includesResolvedStackFields
buildExtendedContext_fullConfig_includesAgentsList
buildExtendedContext_fullConfig_includesSkillsList
buildExtendedContext_withHooks_setsHasHooksTrue
buildExtendedContext_fullConfig_includesMcpServers
buildExtendedContext_fullConfig_includesSecurityFrameworks
assemble_fullConfig_generatesAgentsMdFile
assemble_fullConfig_returnsFileAndNoWarnings
assemble_minimalConfig_omitsDomainSection
assemble_minimalConfig_omitsSecuritySection
assemble_noAgents_returnsWarning
assemble_noSkills_returnsWarning
assemble_noAgents_omitsAgentsSection
assemble_noSkills_omitsSkillsSection
assemble_codexDirNotExists_createsDirectory
assemble_fullConfig_noTemplateArtifacts
assemble_fullConfig_noExcessiveBlankLines
pipeline_assemblerList_includesCodexAgentsMdAssembler
pipeline_assemblerOrder_codexAgentsMdAfterAgentsAndSkills
```
