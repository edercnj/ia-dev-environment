import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import { mkdtemp, rm, mkdir, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join, resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import {
  DocsAdrAssembler,
  getNextAdrNumber,
  formatAdrFilename,
} from "../../../src/assembler/docs-adr-assembler.js";
import { TemplateEngine } from "../../../src/template-engine.js";
import {
  aFullProjectConfig,
} from "../../fixtures/project-config.fixture.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const RESOURCES_DIR = resolve(__dirname, "../../../resources");
const TEMPLATE_PATH = resolve(
  RESOURCES_DIR, "templates", "_TEMPLATE-ADR.md",
);

let tempDir: string;

beforeEach(async () => {
  tempDir = await mkdtemp(join(tmpdir(), "docs-adr-assembler-test-"));
});

afterEach(async () => {
  await rm(tempDir, { recursive: true, force: true });
});

// ---------------------------------------------------------------------------
// DocsAdrAssembler
// ---------------------------------------------------------------------------

describe("DocsAdrAssembler", () => {
  // --- Cycle 1: Degenerate case ---

  it("assemble_templateMissing_returnsEmptyArray", () => {
    const config = aFullProjectConfig();
    const fakeResourcesDir = join(tempDir, "empty-resources");
    fs.mkdirSync(fakeResourcesDir, { recursive: true });
    const engine = new TemplateEngine(fakeResourcesDir, config);
    const assembler = new DocsAdrAssembler();
    const result = assembler.assemble(
      config, tempDir, fakeResourcesDir, engine,
    );
    expect(result).toEqual([]);
  });

  // --- Cycle 2: README generation ---

  it("assemble_validConfig_generatesDocsAdrReadme", () => {
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new DocsAdrAssembler();
    assembler.assemble(config, tempDir, RESOURCES_DIR, engine);
    const readmePath = join(tempDir, "docs", "adr", "README.md");
    expect(fs.existsSync(readmePath)).toBe(true);
  });

  // --- Cycle 3: Placeholder substitution ---

  it("assemble_validConfig_readmeContainsProjectName", () => {
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new DocsAdrAssembler();
    assembler.assemble(config, tempDir, RESOURCES_DIR, engine);
    const readmePath = join(tempDir, "docs", "adr", "README.md");
    const content = fs.readFileSync(readmePath, "utf-8");
    expect(content).toContain(config.project.name);
  });

  // --- Cycle 4: Return file paths ---

  it("assemble_validConfig_returnsGeneratedFilePaths", () => {
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new DocsAdrAssembler();
    const result = assembler.assemble(
      config, tempDir, RESOURCES_DIR, engine,
    );
    expect(result).toHaveLength(1);
    expect(result[0]).toContain("docs");
    expect(result[0]).toContain("README.md");
  });

  // --- Cycle 5: README content validation ---

  it("assemble_validConfig_readmeContainsAdrTableHeaders", () => {
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new DocsAdrAssembler();
    assembler.assemble(config, tempDir, RESOURCES_DIR, engine);
    const readmePath = join(tempDir, "docs", "adr", "README.md");
    const content = fs.readFileSync(readmePath, "utf-8");
    expect(content).toContain("| ID | Title | Status | Date |");
  });

  it("assemble_validConfig_readmeContainsArchitectureDecisionRecordsTitle", () => {
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new DocsAdrAssembler();
    assembler.assemble(config, tempDir, RESOURCES_DIR, engine);
    const readmePath = join(tempDir, "docs", "adr", "README.md");
    const content = fs.readFileSync(readmePath, "utf-8");
    expect(content).toContain("# Architecture Decision Records");
  });

  // --- Cycle 6: Template section validation ---

  it("assemble_templateWithAllSections_generatesSuccessfully", () => {
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new DocsAdrAssembler();
    const result = assembler.assemble(
      config, tempDir, RESOURCES_DIR, engine,
    );
    expect(result).toHaveLength(1);
    const readmePath = join(tempDir, "docs", "adr", "README.md");
    expect(fs.existsSync(readmePath)).toBe(true);
  });

  // --- Cycle 10: Directory creation ---

  it("assemble_outputDirDoesNotExist_createsDirectoryStructure", () => {
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new DocsAdrAssembler();
    const deepOutputDir = join(tempDir, "nested", "deep", "output");
    assembler.assemble(config, deepOutputDir, RESOURCES_DIR, engine);
    const readmePath = join(
      deepOutputDir, "docs", "adr", "README.md",
    );
    expect(fs.existsSync(readmePath)).toBe(true);
  });
});

// ---------------------------------------------------------------------------
// getNextAdrNumber
// ---------------------------------------------------------------------------

describe("getNextAdrNumber", () => {
  it("getNextAdrNumber_emptyDirectory_returns1", async () => {
    const adrDir = join(tempDir, "adr");
    await mkdir(adrDir, { recursive: true });
    expect(getNextAdrNumber(adrDir)).toBe(1);
  });

  it("getNextAdrNumber_nonExistentDirectory_returns1", () => {
    const adrDir = join(tempDir, "nonexistent");
    expect(getNextAdrNumber(adrDir)).toBe(1);
  });

  it("getNextAdrNumber_existingAdrs_returnsNextSequential", async () => {
    const adrDir = join(tempDir, "adr");
    await mkdir(adrDir, { recursive: true });
    await writeFile(join(adrDir, "ADR-0001-foo.md"), "");
    await writeFile(join(adrDir, "ADR-0002-bar.md"), "");
    expect(getNextAdrNumber(adrDir)).toBe(3);
  });

  it("getNextAdrNumber_gapInNumbers_returnsMaxPlusOne", async () => {
    const adrDir = join(tempDir, "adr");
    await mkdir(adrDir, { recursive: true });
    await writeFile(join(adrDir, "ADR-0001-foo.md"), "");
    await writeFile(join(adrDir, "ADR-0003-baz.md"), "");
    expect(getNextAdrNumber(adrDir)).toBe(4);
  });

  // --- PT-1: Parametrized sequential numbering ---

  it.each([
    { files: [], expected: 1 },
    { files: ["ADR-0001-foo.md"], expected: 2 },
    {
      files: ["ADR-0001-foo.md", "ADR-0002-bar.md"],
      expected: 3,
    },
    {
      files: ["ADR-0001-foo.md", "ADR-0003-baz.md"],
      expected: 4,
    },
  ])(
    "getNextAdrNumber_variousExistingCounts_returnsCorrectNext ($expected)",
    async ({ files, expected }) => {
      const adrDir = join(tempDir, "adr");
      await mkdir(adrDir, { recursive: true });
      for (const file of files) {
        await writeFile(join(adrDir, file), "");
      }
      expect(getNextAdrNumber(adrDir)).toBe(expected);
    },
  );
});

// ---------------------------------------------------------------------------
// formatAdrFilename
// ---------------------------------------------------------------------------

describe("formatAdrFilename", () => {
  it("formatAdrFilename_simpleTitle_returnsKebabCase", () => {
    expect(formatAdrFilename(1, "Use PostgreSQL")).toBe(
      "ADR-0001-use-postgresql.md",
    );
  });

  it("formatAdrFilename_specialCharacters_sanitizesTitle", () => {
    expect(formatAdrFilename(1, "Use gRPC (v2)")).toBe(
      "ADR-0001-use-grpc-v2.md",
    );
  });

  it("formatAdrFilename_largeNumber_padsCorrectly", () => {
    expect(formatAdrFilename(42, "Title")).toBe(
      "ADR-0042-title.md",
    );
  });

  // --- PT-2: Parametrized kebab-case formatting ---

  it.each([
    { num: 1, title: "Use PostgreSQL", expected: "ADR-0001-use-postgresql.md" },
    {
      num: 2,
      title: "Adopt Hexagonal Architecture",
      expected: "ADR-0002-adopt-hexagonal-architecture.md",
    },
    { num: 10, title: "Use gRPC (v2)", expected: "ADR-0010-use-grpc-v2.md" },
    { num: 42, title: "Simple", expected: "ADR-0042-simple.md" },
    {
      num: 1,
      title: "Title--with---dashes",
      expected: "ADR-0001-title-with-dashes.md",
    },
  ])(
    "formatAdrFilename_variousTitles_formatsCorrectly ($expected)",
    ({ num, title, expected }) => {
      expect(formatAdrFilename(num, title)).toBe(expected);
    },
  );
});

// ---------------------------------------------------------------------------
// Template Structure Validation
// ---------------------------------------------------------------------------

describe("Template Structure Validation", () => {
  let templateContent: string;

  beforeEach(() => {
    templateContent = fs.readFileSync(TEMPLATE_PATH, "utf-8");
  });

  it("template_containsFrontmatterWithStatusField", () => {
    expect(templateContent).toMatch(/^---\n[\s\S]*?status:/m);
  });

  it("template_containsFrontmatterWithDateField", () => {
    expect(templateContent).toMatch(/^---\n[\s\S]*?date:/m);
  });

  it("template_containsFrontmatterWithDecidersField", () => {
    expect(templateContent).toMatch(/^---\n[\s\S]*?deciders:/m);
  });

  it("template_containsMandatorySection_status", () => {
    expect(templateContent).toContain("## Status");
  });

  it("template_containsMandatorySection_context", () => {
    expect(templateContent).toContain("## Context");
  });

  it("template_containsMandatorySection_decision", () => {
    expect(templateContent).toContain("## Decision");
  });

  it("template_containsMandatorySection_consequences", () => {
    expect(templateContent).toContain("## Consequences");
  });

  it("template_containsOptionalSection_relatedAdrs", () => {
    expect(templateContent).toContain("## Related ADRs");
  });

  it("template_containsOptionalSection_storyReference", () => {
    expect(templateContent).toContain("## Story Reference");
  });

  it("template_containsProjectNamePlaceholder", () => {
    expect(templateContent).toContain("{{PROJECT_NAME}}");
  });
});
