import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import { mkdtemp, rm, mkdir, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join, resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { EpicReportAssembler } from "../../../src/assembler/epic-report-assembler.js";
import { TemplateEngine } from "../../../src/template-engine.js";
import {
  aFullProjectConfig,
} from "../../fixtures/project-config.fixture.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const RESOURCES_DIR = resolve(__dirname, "../../../resources");
const TEMPLATE_FILENAME = "_TEMPLATE-EPIC-EXECUTION-REPORT.md";

let tempDir: string;

beforeEach(async () => {
  tempDir = await mkdtemp(
    join(tmpdir(), "epic-report-assembler-test-"),
  );
});

afterEach(async () => {
  await rm(tempDir, { recursive: true, force: true });
});

// ---------------------------------------------------------------------------
// EpicReportAssembler
// ---------------------------------------------------------------------------

describe("EpicReportAssembler", () => {
  // --- Cycle 1: Degenerate case — template missing ---

  it("assemble_templateMissing_returnsEmptyArray", () => {
    const config = aFullProjectConfig();
    const fakeResourcesDir = join(tempDir, "empty-resources");
    fs.mkdirSync(fakeResourcesDir, { recursive: true });
    const engine = new TemplateEngine(fakeResourcesDir, config);
    const assembler = new EpicReportAssembler();
    const result = assembler.assemble(
      config, tempDir, fakeResourcesDir, engine,
    );
    expect(result).toEqual([]);
  });

  // --- Cycle 2: Degenerate case — missing mandatory sections ---

  it("assemble_templateMissingMandatorySection_returnsEmptyArray", async () => {
    const config = aFullProjectConfig();
    const fakeResourcesDir = join(tempDir, "resources-incomplete");
    const templatesDir = join(fakeResourcesDir, "templates");
    await mkdir(templatesDir, { recursive: true });
    const incompleteTemplate = [
      "# Epic Execution Report",
      "",
      "## Sumario Executivo",
      "",
      "Some content",
      "",
      "## Coverage Delta",
      "",
      "More content",
    ].join("\n");
    await writeFile(
      join(templatesDir, TEMPLATE_FILENAME),
      incompleteTemplate,
    );
    const engine = new TemplateEngine(fakeResourcesDir, config);
    const assembler = new EpicReportAssembler();
    const result = assembler.assemble(
      config, tempDir, fakeResourcesDir, engine,
    );
    expect(result).toEqual([]);
  });

  // --- Cycle 3: Directory creation ---

  it("assemble_outputDirDoesNotExist_createsDirectoryStructure", () => {
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new EpicReportAssembler();
    const deepOutputDir = join(tempDir, "nested", "deep", "output");
    const result = assembler.assemble(
      config, deepOutputDir, RESOURCES_DIR, engine,
    );
    expect(result.length).toBeGreaterThan(0);
    const docsPath = join(
      deepOutputDir, "docs", "epic", TEMPLATE_FILENAME,
    );
    expect(fs.existsSync(docsPath)).toBe(true);
  });

  // --- Cycle 4: Copy to docs/epic/ ---

  it("assemble_validTemplate_copiesToDocsEpicPath", () => {
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new EpicReportAssembler();
    assembler.assemble(config, tempDir, RESOURCES_DIR, engine);
    const destPath = join(
      tempDir, "docs", "epic", TEMPLATE_FILENAME,
    );
    expect(fs.existsSync(destPath)).toBe(true);
  });

  // --- Cycle 5: Copy to .claude/templates/ ---

  it("assemble_validTemplate_copiesToClaudeTemplatesPath", () => {
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new EpicReportAssembler();
    assembler.assemble(config, tempDir, RESOURCES_DIR, engine);
    const destPath = join(
      tempDir, ".claude", "templates", TEMPLATE_FILENAME,
    );
    expect(fs.existsSync(destPath)).toBe(true);
  });

  // --- Cycle 6: Copy to .github/templates/ ---

  it("assemble_validTemplate_copiesToGithubTemplatesPath", () => {
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new EpicReportAssembler();
    assembler.assemble(config, tempDir, RESOURCES_DIR, engine);
    const destPath = join(
      tempDir, ".github", "templates", TEMPLATE_FILENAME,
    );
    expect(fs.existsSync(destPath)).toBe(true);
  });

  // --- Cycle 7: Verbatim copy (no placeholder resolution) ---

  it("assemble_validTemplate_copiesContentVerbatimWithoutPlaceholderResolution", () => {
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new EpicReportAssembler();
    assembler.assemble(config, tempDir, RESOURCES_DIR, engine);
    const sourcePath = join(
      RESOURCES_DIR, "templates", TEMPLATE_FILENAME,
    );
    const sourceContent = fs.readFileSync(sourcePath, "utf-8");
    const outputPath = join(
      tempDir, "docs", "epic", TEMPLATE_FILENAME,
    );
    const outputContent = fs.readFileSync(outputPath, "utf-8");
    expect(outputContent).toBe(sourceContent);
  });

  // --- Cycle 8: Return array with 3 paths ---

  it("assemble_validTemplate_returnsThreeFilePaths", () => {
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new EpicReportAssembler();
    const result = assembler.assemble(
      config, tempDir, RESOURCES_DIR, engine,
    );
    expect(result).toHaveLength(3);
    expect(result[0]).toContain(
      join("docs", "epic", TEMPLATE_FILENAME),
    );
    expect(result[1]).toContain(
      join(".claude", "templates", TEMPLATE_FILENAME),
    );
    expect(result[2]).toContain(
      join(".github", "templates", TEMPLATE_FILENAME),
    );
  });

  // --- Cycle 9: Dual copy parity ---

  it("assemble_validTemplate_allThreeOutputsAreIdentical", () => {
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new EpicReportAssembler();
    assembler.assemble(config, tempDir, RESOURCES_DIR, engine);
    const docsContent = fs.readFileSync(
      join(tempDir, "docs", "epic", TEMPLATE_FILENAME), "utf-8",
    );
    const claudeContent = fs.readFileSync(
      join(tempDir, ".claude", "templates", TEMPLATE_FILENAME),
      "utf-8",
    );
    const githubContent = fs.readFileSync(
      join(tempDir, ".github", "templates", TEMPLATE_FILENAME),
      "utf-8",
    );
    expect(claudeContent).toBe(docsContent);
    expect(githubContent).toBe(docsContent);
  });

  // --- Cycle 10: Existing files preserved ---

  it("assemble_existingFilesInOutputDir_doesNotModifyThem", async () => {
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new EpicReportAssembler();
    const existingDir = join(tempDir, "src");
    await mkdir(existingDir, { recursive: true });
    const existingFile = join(existingDir, "index.ts");
    const existingContent = "export const x = 1;";
    await writeFile(existingFile, existingContent);
    assembler.assemble(config, tempDir, RESOURCES_DIR, engine);
    const preserved = fs.readFileSync(existingFile, "utf-8");
    expect(preserved).toBe(existingContent);
  });
});
