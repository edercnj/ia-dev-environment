import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import { mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join, resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { GrpcDocsAssembler } from "../../../src/assembler/grpc-docs-assembler.js";
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
  tempDir = mkdtempSync(join(tmpdir(), "grpc-docs-assembler-test-"));
});

afterEach(() => {
  rmSync(tempDir, { recursive: true, force: true });
});

function grpcConfig(): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity("my-grpc-service", "A gRPC service"),
    new ArchitectureConfig("microservice"),
    [new InterfaceConfig("grpc", "proto3")],
    new LanguageConfig("go", "1.22"),
    new FrameworkConfig("gin", "1.9", "go-mod"),
  );
}

function assembleWithRealTemplate(config?: ProjectConfig): {
  result: string[];
  outputFile: string;
} {
  const cfg = config ?? grpcConfig();
  const engine = new TemplateEngine(RESOURCES_DIR, cfg);
  const assembler = new GrpcDocsAssembler();
  const result = assembler.assemble(cfg, tempDir, RESOURCES_DIR, engine);
  const outputFile = join(tempDir, "api", "grpc-reference.md");
  return { result, outputFile };
}

// ---------------------------------------------------------------------------
// Degenerate / Error Path
// ---------------------------------------------------------------------------

describe("GrpcDocsAssembler — degenerate", () => {
  it("assemble_templateMissing_returnsEmptyArray", () => {
    const fakeResources = join(tempDir, "empty-resources");
    fs.mkdirSync(fakeResources, { recursive: true });
    const config = grpcConfig();
    const engine = new TemplateEngine(fakeResources, config);
    const assembler = new GrpcDocsAssembler();
    const result = assembler.assemble(
      config, tempDir, fakeResources, engine,
    );
    expect(result).toEqual([]);
  });

  it("assemble_templateMissing_doesNotCreateApiDir", () => {
    const fakeResources = join(tempDir, "empty-resources");
    fs.mkdirSync(fakeResources, { recursive: true });
    const config = grpcConfig();
    const engine = new TemplateEngine(fakeResources, config);
    const assembler = new GrpcDocsAssembler();
    assembler.assemble(config, tempDir, fakeResources, engine);
    expect(fs.existsSync(join(tempDir, "api"))).toBe(false);
  });

  it("assemble_noGrpcInterface_returnsEmptyArray", () => {
    const config = new ProjectConfig(
      new ProjectIdentity("svc", "svc"),
      new ArchitectureConfig("microservice"),
      [new InterfaceConfig("rest")],
      new LanguageConfig("java", "21"),
      new FrameworkConfig("quarkus", "3.0", "maven"),
    );
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new GrpcDocsAssembler();
    const result = assembler.assemble(
      config, tempDir, RESOURCES_DIR, engine,
    );
    expect(result).toEqual([]);
  });

  it("assemble_noGrpcInterface_doesNotCreateApiDir", () => {
    const config = new ProjectConfig(
      new ProjectIdentity("svc", "svc"),
      new ArchitectureConfig("microservice"),
      [new InterfaceConfig("rest")],
      new LanguageConfig("java", "21"),
      new FrameworkConfig("quarkus", "3.0", "maven"),
    );
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new GrpcDocsAssembler();
    assembler.assemble(config, tempDir, RESOURCES_DIR, engine);
    expect(fs.existsSync(join(tempDir, "api"))).toBe(false);
  });

  it("assemble_emptyInterfaces_returnsEmptyArray", () => {
    const config = new ProjectConfig(
      new ProjectIdentity("svc", "svc"),
      new ArchitectureConfig("microservice"),
      [],
      new LanguageConfig("java", "21"),
      new FrameworkConfig("quarkus", "3.0", "maven"),
    );
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new GrpcDocsAssembler();
    const result = assembler.assemble(
      config, tempDir, RESOURCES_DIR, engine,
    );
    expect(result).toEqual([]);
  });
});

// ---------------------------------------------------------------------------
// Happy Path
// ---------------------------------------------------------------------------

describe("GrpcDocsAssembler — happy path", () => {
  it("assemble_grpcInterface_returnsFilePathArray", () => {
    const { result } = assembleWithRealTemplate();
    expect(result).toHaveLength(1);
    expect(result[0]).toContain("grpc-reference.md");
  });

  it("assemble_grpcInterface_createsApiDirectory", () => {
    assembleWithRealTemplate();
    const apiDir = join(tempDir, "api");
    expect(fs.existsSync(apiDir)).toBe(true);
    expect(fs.statSync(apiDir).isDirectory()).toBe(true);
  });

  it("assemble_grpcInterface_writesGrpcReferenceMd", () => {
    const { outputFile } = assembleWithRealTemplate();
    expect(fs.existsSync(outputFile)).toBe(true);
  });
});

// ---------------------------------------------------------------------------
// Content Validation
// ---------------------------------------------------------------------------

describe("GrpcDocsAssembler — content validation", () => {
  it("assemble_grpcInterface_rendersProjectName", () => {
    const { outputFile } = assembleWithRealTemplate();
    const content = fs.readFileSync(outputFile, "utf-8");
    expect(content).toContain("my-grpc-service");
    expect(content).not.toContain("{{ project_name }}");
  });

  it("assemble_grpcInterface_rendersLanguageName", () => {
    const { outputFile } = assembleWithRealTemplate();
    const content = fs.readFileSync(outputFile, "utf-8");
    expect(content).toContain("go");
    expect(content).not.toContain("{{ language_name }}");
  });

  it("assemble_grpcInterface_rendersFrameworkName", () => {
    const { outputFile } = assembleWithRealTemplate();
    const content = fs.readFileSync(outputFile, "utf-8");
    expect(content).toContain("gin");
    expect(content).not.toContain("{{ framework_name }}");
  });

  it("assemble_grpcInterface_containsOverviewSection", () => {
    const { outputFile } = assembleWithRealTemplate();
    const content = fs.readFileSync(outputFile, "utf-8");
    expect(content).toContain("## Overview");
  });

  it("assemble_grpcInterface_containsServiceSection", () => {
    const { outputFile } = assembleWithRealTemplate();
    const content = fs.readFileSync(outputFile, "utf-8");
    expect(content).toContain("## Service:");
  });

  it("assemble_grpcInterface_containsRpcTable", () => {
    const { outputFile } = assembleWithRealTemplate();
    const content = fs.readFileSync(outputFile, "utf-8");
    expect(content).toContain("| Method |");
    expect(content).toContain("| Request |");
    expect(content).toContain("| Response |");
    expect(content).toContain("| Type |");
  });

  it("assemble_grpcInterface_containsMessageSection", () => {
    const { outputFile } = assembleWithRealTemplate();
    const content = fs.readFileSync(outputFile, "utf-8");
    expect(content).toContain("### Message:");
  });

  it("assemble_grpcInterface_containsFieldTable", () => {
    const { outputFile } = assembleWithRealTemplate();
    const content = fs.readFileSync(outputFile, "utf-8");
    expect(content).toContain("| Field |");
    expect(content).toContain("| Type |");
    expect(content).toContain("| Number |");
    expect(content).toContain("| Description |");
  });

  it("assemble_grpcInterface_containsAllRpcTypes", () => {
    const { outputFile } = assembleWithRealTemplate();
    const content = fs.readFileSync(outputFile, "utf-8");
    expect(content).toContain("Unary");
    expect(content).toContain("Server Streaming");
    expect(content).toContain("Client Streaming");
    expect(content).toContain("Bidirectional");
  });

  it("assemble_grpcInterface_containsDeprecatedMarker", () => {
    const { outputFile } = assembleWithRealTemplate();
    const content = fs.readFileSync(outputFile, "utf-8");
    expect(content).toContain("[DEPRECATED]");
  });

  it("assemble_grpcInterface_containsBackwardCompatSection", () => {
    const { outputFile } = assembleWithRealTemplate();
    const content = fs.readFileSync(outputFile, "utf-8");
    expect(content).toContain("## Backward Compatibility");
  });
});

// ---------------------------------------------------------------------------
// Edge Cases
// ---------------------------------------------------------------------------

describe("GrpcDocsAssembler — edge cases", () => {
  it("assemble_deepOutputDir_createsRecursively", () => {
    const deepOutput = join(tempDir, "deep", "nested", "output");
    const config = grpcConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new GrpcDocsAssembler();
    const result = assembler.assemble(
      config, deepOutput, RESOURCES_DIR, engine,
    );
    expect(result).toHaveLength(1);
    const outputFile = join(deepOutput, "api", "grpc-reference.md");
    expect(fs.existsSync(outputFile)).toBe(true);
  });

  it("assemble_multipleInterfacesIncludingGrpc_generatesFile", () => {
    const config = new ProjectConfig(
      new ProjectIdentity("svc", "svc"),
      new ArchitectureConfig("microservice"),
      [new InterfaceConfig("rest"), new InterfaceConfig("grpc", "proto3")],
      new LanguageConfig("java", "21"),
      new FrameworkConfig("quarkus", "3.0", "maven"),
    );
    const { result } = assembleWithRealTemplate(config);
    expect(result).toHaveLength(1);
  });

  it("assemble_grpcNotFirstInterface_stillGenerates", () => {
    const config = new ProjectConfig(
      new ProjectIdentity("svc", "svc"),
      new ArchitectureConfig("microservice"),
      [
        new InterfaceConfig("rest"),
        new InterfaceConfig("grpc", "proto3"),
        new InterfaceConfig("event-consumer", "", "kafka"),
      ],
      new LanguageConfig("java", "21"),
      new FrameworkConfig("quarkus", "3.0", "maven"),
    );
    const { result } = assembleWithRealTemplate(config);
    expect(result).toHaveLength(1);
  });
});

// ---------------------------------------------------------------------------
// Acceptance Tests
// ---------------------------------------------------------------------------

describe("GrpcDocsAssembler — acceptance", () => {
  it("assemble_fullGrpcConfig_generatesCompleteReference", () => {
    const config = grpcConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new GrpcDocsAssembler();
    const result = assembler.assemble(
      config, tempDir, RESOURCES_DIR, engine,
    );
    expect(result).toHaveLength(1);
    const outputFile = join(tempDir, "api", "grpc-reference.md");
    const content = fs.readFileSync(outputFile, "utf-8");
    // All major sections present
    expect(content).toContain("# gRPC API Reference");
    expect(content).toContain("## Overview");
    expect(content).toContain("## Service:");
    expect(content).toContain("### Message:");
    expect(content).toContain("## Backward Compatibility");
    expect(content).toContain("## Change History");
    // Placeholders resolved
    expect(content).toContain("my-grpc-service");
    expect(content).toContain("go");
    expect(content).toContain("gin");
    // All 4 RPC types
    expect(content).toContain("Unary");
    expect(content).toContain("Server Streaming");
    expect(content).toContain("Client Streaming");
    expect(content).toContain("Bidirectional");
    // Deprecated marker
    expect(content).toContain("[DEPRECATED]");
    // No unresolved Nunjucks tokens
    expect(content).not.toMatch(/\{\{[^}]*\}\}/);
  });
});
