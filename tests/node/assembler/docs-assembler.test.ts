import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import { mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join, resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { DocsAssembler } from "../../../src/assembler/docs-assembler.js";
import { TemplateEngine } from "../../../src/template-engine.js";
import {
  ProjectConfig,
  ProjectIdentity,
  ArchitectureConfig,
  InterfaceConfig,
  LanguageConfig,
  FrameworkConfig,
} from "../../../src/models.js";
import { aProjectConfig } from "../../fixtures/project-config.fixture.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const RESOURCES_DIR = resolve(__dirname, "../../../resources");

let tempDir: string;

beforeEach(() => {
  tempDir = mkdtempSync(join(tmpdir(), "docs-assembler-test-"));
});

afterEach(() => {
  rmSync(tempDir, { recursive: true, force: true });
});

// ---------------------------------------------------------------------------
// Degenerate / Error Path
// ---------------------------------------------------------------------------

describe("DocsAssembler — degenerate", () => {
  it("assemble_templateMissing_returnsEmptyArray", () => {
    const fakeResources = join(tempDir, "empty-resources");
    fs.mkdirSync(fakeResources, { recursive: true });
    const config = aProjectConfig();
    const engine = new TemplateEngine(fakeResources, config);
    const assembler = new DocsAssembler();
    const result = assembler.assemble(config, tempDir, fakeResources, engine);
    expect(result).toEqual([]);
  });

  it("assemble_templateMissing_doesNotCreateDocsDir", () => {
    const fakeResources = join(tempDir, "empty-resources");
    fs.mkdirSync(fakeResources, { recursive: true });
    const config = aProjectConfig();
    const engine = new TemplateEngine(fakeResources, config);
    const assembler = new DocsAssembler();
    assembler.assemble(config, tempDir, fakeResources, engine);
    expect(fs.existsSync(join(tempDir, "docs"))).toBe(false);
  });
});
