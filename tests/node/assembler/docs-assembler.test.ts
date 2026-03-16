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
    expect(fs.existsSync(join(tempDir, "architecture"))).toBe(false);
  });
});

// ---------------------------------------------------------------------------
// Happy Path
// ---------------------------------------------------------------------------

describe("DocsAssembler — happy path", () => {
  const config = aProjectConfig();

  function assembleWithRealTemplate(): {
    result: string[];
    outputFile: string;
  } {
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new DocsAssembler();
    const result = assembler.assemble(config, tempDir, RESOURCES_DIR, engine);
    const outputFile = join(tempDir, "architecture", "service-architecture.md");
    return { result, outputFile };
  }

  it("assemble_templateExists_returnsFilePathArray", () => {
    const { result } = assembleWithRealTemplate();
    expect(result).toHaveLength(1);
    expect(result[0]).toContain("service-architecture.md");
  });

  it("assemble_templateExists_createsArchitectureDir", () => {
    assembleWithRealTemplate();
    const archDir = join(tempDir, "architecture");
    expect(fs.existsSync(archDir)).toBe(true);
    expect(fs.statSync(archDir).isDirectory()).toBe(true);
  });

  it("assemble_templateExists_writesServiceArchitectureMd", () => {
    const { outputFile } = assembleWithRealTemplate();
    expect(fs.existsSync(outputFile)).toBe(true);
  });

  it("assemble_templateExists_resolvesServiceNamePlaceholder", () => {
    const { outputFile } = assembleWithRealTemplate();
    const content = fs.readFileSync(outputFile, "utf-8");
    expect(content).toContain("my-service");
    expect(content).not.toContain("{{ project_name }}");
  });

  it("assemble_templateExists_resolvesArchitecturePlaceholder", () => {
    const { outputFile } = assembleWithRealTemplate();
    const content = fs.readFileSync(outputFile, "utf-8");
    expect(content).toContain("hexagonal");
    expect(content).not.toContain("{{ architecture_style }}");
  });

  it("assemble_templateExists_resolvesLanguagePlaceholder", () => {
    const { outputFile } = assembleWithRealTemplate();
    const content = fs.readFileSync(outputFile, "utf-8");
    expect(content).toContain("python");
    expect(content).not.toContain("{{ language_name }}");
  });

  it("assemble_templateExists_resolvesFrameworkPlaceholder", () => {
    const { outputFile } = assembleWithRealTemplate();
    const content = fs.readFileSync(outputFile, "utf-8");
    expect(content).toContain("click");
    expect(content).not.toContain("{{ framework_name }}");
  });

  it("assemble_templateExists_resolvesInterfacesListPlaceholder", () => {
    const { outputFile } = assembleWithRealTemplate();
    const content = fs.readFileSync(outputFile, "utf-8");
    expect(content).toContain("cli");
    expect(content).not.toContain("{{ interfaces_list }}");
  });

  it("assemble_templateExists_containsAllTenSections", () => {
    const { outputFile } = assembleWithRealTemplate();
    const content = fs.readFileSync(outputFile, "utf-8");
    expect(content).toContain("## 1. Overview");
    expect(content).toContain("## 2. C4 Diagrams");
    expect(content).toContain("## 3. Integrations");
    expect(content).toContain("## 4. Data Model");
    expect(content).toContain("## 5. Critical Flows");
    expect(content).toContain("## 6. NFRs");
    expect(content).toContain("## 7. Architectural Decisions");
    expect(content).toContain("## 8. Observability");
    expect(content).toContain("## 9. Resilience");
    expect(content).toContain("## 10. Change History");
  });

  it("assemble_templateExists_containsMermaidC4Diagram", () => {
    const { outputFile } = assembleWithRealTemplate();
    const content = fs.readFileSync(outputFile, "utf-8");
    expect(content).toContain("```mermaid");
    expect(content).toContain("graph TD");
  });

  it("assemble_templateExists_containsNfrTable", () => {
    const { outputFile } = assembleWithRealTemplate();
    const content = fs.readFileSync(outputFile, "utf-8");
    expect(content).toContain("| Metric | Target | Measurement |");
    expect(content).toContain("Latency (p95)");
  });

  it("assemble_templateExists_containsIntegrationsTable", () => {
    const { outputFile } = assembleWithRealTemplate();
    const content = fs.readFileSync(outputFile, "utf-8");
    expect(content).toContain("| System | Protocol | Purpose | SLO |");
  });
});

// ---------------------------------------------------------------------------
// Boundary / Edge Cases
// ---------------------------------------------------------------------------

describe("DocsAssembler — edge cases", () => {
  it("assemble_outputDirNotExists_createsRecursively", () => {
    const deepOutput = join(tempDir, "deep", "nested", "output");
    const config = aProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new DocsAssembler();
    const result = assembler.assemble(config, deepOutput, RESOURCES_DIR, engine);
    expect(result).toHaveLength(1);
    const outputFile = join(deepOutput, "architecture", "service-architecture.md");
    expect(fs.existsSync(outputFile)).toBe(true);
  });

  it("assemble_multipleInterfaces_joinsWithComma", () => {
    const multiConfig = new ProjectConfig(
      new ProjectIdentity("svc", "svc"),
      new ArchitectureConfig("hexagonal"),
      [new InterfaceConfig("rest"), new InterfaceConfig("grpc")],
      new LanguageConfig("java", "21"),
      new FrameworkConfig("quarkus", "3.0", "maven"),
    );
    const engine = new TemplateEngine(RESOURCES_DIR, multiConfig);
    const assembler = new DocsAssembler();
    assembler.assemble(multiConfig, tempDir, RESOURCES_DIR, engine);
    const outputFile = join(tempDir, "architecture", "service-architecture.md");
    const content = fs.readFileSync(outputFile, "utf-8");
    expect(content).toContain("rest, grpc");
  });

  it("assemble_singleInterface_noComma", () => {
    const singleConfig = new ProjectConfig(
      new ProjectIdentity("svc", "svc"),
      new ArchitectureConfig("hexagonal"),
      [new InterfaceConfig("rest")],
      new LanguageConfig("java", "21"),
      new FrameworkConfig("quarkus", "3.0", "maven"),
    );
    const engine = new TemplateEngine(RESOURCES_DIR, singleConfig);
    const assembler = new DocsAssembler();
    assembler.assemble(singleConfig, tempDir, RESOURCES_DIR, engine);
    const outputFile = join(tempDir, "architecture", "service-architecture.md");
    const content = fs.readFileSync(outputFile, "utf-8");
    expect(content).toContain("**Interfaces:** rest");
    expect(content).not.toMatch(/rest,/);
  });

  it("assemble_noInterfaces_defaultsToNone", () => {
    const emptyConfig = new ProjectConfig(
      new ProjectIdentity("svc", "svc"),
      new ArchitectureConfig("hexagonal"),
      [],
      new LanguageConfig("java", "21"),
      new FrameworkConfig("quarkus", "3.0", "maven"),
    );
    const engine = new TemplateEngine(RESOURCES_DIR, emptyConfig);
    const assembler = new DocsAssembler();
    assembler.assemble(emptyConfig, tempDir, RESOURCES_DIR, engine);
    const outputFile = join(tempDir, "architecture", "service-architecture.md");
    const content = fs.readFileSync(outputFile, "utf-8");
    expect(content).toContain("**Interfaces:** none");
  });
});

// ---------------------------------------------------------------------------
// Acceptance Tests
// ---------------------------------------------------------------------------

describe("DocsAssembler — acceptance", () => {
  it("assemble_fullConfig_generatesCompleteArchitectureDoc", () => {
    const config = aProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new DocsAssembler();
    const result = assembler.assemble(config, tempDir, RESOURCES_DIR, engine);
    expect(result).toHaveLength(1);
    const outputFile = join(tempDir, "architecture", "service-architecture.md");
    const content = fs.readFileSync(outputFile, "utf-8");
    // All 10 sections present
    for (let i = 1; i <= 10; i++) {
      expect(content).toContain(`## ${i}.`);
    }
    // Placeholders resolved
    expect(content).toContain("my-service");
    expect(content).toContain("hexagonal");
    expect(content).toContain("python");
    expect(content).toContain("click");
    // Mermaid diagrams present
    expect(content).toContain("```mermaid");
    // NFR table present
    expect(content).toContain("| Metric | Target | Measurement |");
    // No unresolved Nunjucks tokens
    expect(content).not.toMatch(/\{\{[^}]*\}\}/);
  });

  it("assemble_noTemplate_gracefulNoOp", () => {
    const fakeResources = join(tempDir, "no-templates");
    fs.mkdirSync(fakeResources, { recursive: true });
    const config = aProjectConfig();
    const engine = new TemplateEngine(fakeResources, config);
    const assembler = new DocsAssembler();
    const result = assembler.assemble(config, tempDir, fakeResources, engine);
    expect(result).toEqual([]);
    expect(fs.existsSync(join(tempDir, "architecture"))).toBe(false);
  });
});
