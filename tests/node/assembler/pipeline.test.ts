import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import {
  buildAssemblers,
  executeAssemblers,
  normalizeResult,
  runPipeline,
  DRY_RUN_WARNING,
} from "../../../src/assembler/pipeline.js";
import type { AssemblerDescriptor } from "../../../src/assembler/pipeline.js";
import { PipelineError } from "../../../src/exceptions.js";
import { TemplateEngine } from "../../../src/template-engine.js";
import {
  ProjectConfig,
  ProjectIdentity,
  ArchitectureConfig,
  InterfaceConfig,
  LanguageConfig,
  FrameworkConfig,
  PipelineResult,
} from "../../../src/models.js";

function buildConfig(): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity("test-app", "A test application"),
    new ArchitectureConfig("microservice", false, false),
    [new InterfaceConfig("rest")],
    new LanguageConfig("typescript", "5"),
    new FrameworkConfig("commander", "12", "npm"),
  );
}

function createStubDescriptor(
  name: string,
  files: string[],
  warnings?: string[],
): AssemblerDescriptor {
  return {
    name,
    assembler: {
      assemble: () => warnings !== undefined
        ? { files, warnings }
        : files,
    },
  };
}

function createFailingDescriptor(
  name: string,
  error: Error,
): AssemblerDescriptor {
  return {
    name,
    assembler: {
      assemble: () => { throw error; },
    },
  };
}

describe("normalizeResult", () => {
  it("normalizeResult_stringArray_returnsFilesAndEmptyWarnings", () => {
    const result = normalizeResult(["a.md", "b.md"]);
    expect(result).toEqual({ files: ["a.md", "b.md"], warnings: [] });
  });

  it("normalizeResult_assembleResult_returnsFilesAndWarnings", () => {
    const result = normalizeResult({
      files: ["a.md"], warnings: ["warn1"],
    });
    expect(result).toEqual({ files: ["a.md"], warnings: ["warn1"] });
  });

  it("normalizeResult_emptyArray_returnsEmptyResult", () => {
    const result = normalizeResult([]);
    expect(result).toEqual({ files: [], warnings: [] });
  });

  it("normalizeResult_assembleResultEmptyWarnings_returnsEmptyWarnings", () => {
    const result = normalizeResult({ files: ["a.md"], warnings: [] });
    expect(result.warnings).toEqual([]);
  });

  it("normalizeResult_assembleResult_copiesArrays", () => {
    const original = { files: ["a.md"], warnings: ["w"] };
    const result = normalizeResult(original);
    result.files.push("extra");
    expect(original.files).toEqual(["a.md"]);
  });
});

describe("buildAssemblers", () => {
  const EXPECTED_ORDER = [
    "RulesAssembler",
    "SkillsAssembler",
    "AgentsAssembler",
    "PatternsAssembler",
    "ProtocolsAssembler",
    "HooksAssembler",
    "SettingsAssembler",
    "GithubInstructionsAssembler",
    "GithubMcpAssembler",
    "GithubSkillsAssembler",
    "GithubAgentsAssembler",
    "GithubHooksAssembler",
    "GithubPromptsAssembler",
    "ReadmeAssembler",
    "CodexAgentsMdAssembler",
    "CodexConfigAssembler",
  ];

  it("buildAssemblers_returns16Assemblers", () => {
    const assemblers = buildAssemblers();
    expect(assemblers).toHaveLength(16);
  });

  it.each(
    EXPECTED_ORDER.map((name, index) => [index, name] as const),
  )("buildAssemblers_index%i_is%s", (index, expectedName) => {
    const assemblers = buildAssemblers();
    expect(assemblers[index].name).toBe(expectedName);
  });

  it("buildAssemblers_eachHasAssembleMethod", () => {
    const assemblers = buildAssemblers();
    for (const desc of assemblers) {
      expect(typeof desc.assembler.assemble).toBe("function");
    }
  });

  it("buildAssemblers_orderMatchesRule008", () => {
    const assemblers = buildAssemblers();
    const names = assemblers.map((a) => a.name);
    expect(names).toEqual(EXPECTED_ORDER);
  });
});

describe("executeAssemblers", () => {
  let config: ProjectConfig;
  let tmpDir: string;
  let engine: TemplateEngine;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(path.join(tmpdir(), "pipe-exec-"));
    config = buildConfig();
    const resourcesDir = path.join(tmpDir, "resources");
    fs.mkdirSync(resourcesDir, { recursive: true });
    engine = new TemplateEngine(resourcesDir, config);
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  it("executeAssemblers_callsAllInOrder", () => {
    const callOrder: string[] = [];
    const assemblers: AssemblerDescriptor[] = [
      {
        name: "First",
        assembler: {
          assemble: () => { callOrder.push("First"); return []; },
        },
      },
      {
        name: "Second",
        assembler: {
          assemble: () => { callOrder.push("Second"); return []; },
        },
      },
      {
        name: "Third",
        assembler: {
          assemble: () => { callOrder.push("Third"); return []; },
        },
      },
    ];
    executeAssemblers(
      assemblers, config, tmpDir, tmpDir, engine,
    );
    expect(callOrder).toEqual(["First", "Second", "Third"]);
  });

  it("executeAssemblers_aggregatesFilesFromMultiple", () => {
    const assemblers = [
      createStubDescriptor("A", ["a.md"]),
      createStubDescriptor("B", ["b.md", "c.md"]),
    ];
    const result = executeAssemblers(
      assemblers, config, tmpDir, tmpDir, engine,
    );
    expect(result.files).toEqual(["a.md", "b.md", "c.md"]);
  });

  it("executeAssemblers_aggregatesWarningsFromAssembleResult", () => {
    const assemblers = [
      createStubDescriptor("A", ["a.md"], ["warn1"]),
      createStubDescriptor("B", ["b.md"], ["warn2"]),
    ];
    const result = executeAssemblers(
      assemblers, config, tmpDir, tmpDir, engine,
    );
    expect(result.warnings).toEqual(["warn1", "warn2"]);
  });

  it("executeAssemblers_stringArray_noWarnings", () => {
    const assemblers = [
      createStubDescriptor("A", ["a.md"]),
    ];
    const result = executeAssemblers(
      assemblers, config, tmpDir, tmpDir, engine,
    );
    expect(result.warnings).toEqual([]);
  });

  it("executeAssemblers_mixedReturnTypes_aggregatesCorrectly", () => {
    const assemblers = [
      createStubDescriptor("WithWarnings", ["a.md"], ["warn"]),
      createStubDescriptor("NoWarnings", ["b.md"]),
    ];
    const result = executeAssemblers(
      assemblers, config, tmpDir, tmpDir, engine,
    );
    expect(result.files).toEqual(["a.md", "b.md"]);
    expect(result.warnings).toEqual(["warn"]);
  });

  it("executeAssemblers_assemblerThrows_wrapsPipelineError", () => {
    const assemblers = [
      createFailingDescriptor("BadAssembler", new Error("disk full")),
    ];
    expect(() => executeAssemblers(
      assemblers, config, tmpDir, tmpDir, engine,
    )).toThrow(PipelineError);
  });

  it("executeAssemblers_assemblerThrows_includesName", () => {
    const assemblers = [
      createFailingDescriptor("BadAssembler", new Error("disk full")),
    ];
    try {
      executeAssemblers(assemblers, config, tmpDir, tmpDir, engine);
      expect.unreachable("Should have thrown");
    } catch (error) {
      expect(error).toBeInstanceOf(PipelineError);
      const pe = error as PipelineError;
      expect(pe.assemblerName).toBe("BadAssembler");
      expect(pe.reason).toBe("disk full");
    }
  });

  it("executeAssemblers_nonErrorThrown_wrapsAsString", () => {
    const assemblers: AssemblerDescriptor[] = [{
      name: "StringThrower",
      assembler: {
        assemble: () => { throw "raw string error"; },
      },
    }];
    try {
      executeAssemblers(assemblers, config, tmpDir, tmpDir, engine);
      expect.unreachable("Should have thrown");
    } catch (error) {
      expect(error).toBeInstanceOf(PipelineError);
      expect((error as PipelineError).reason).toBe("raw string error");
    }
  });

  it("executeAssemblers_emptyList_returnsEmptyResult", () => {
    const result = executeAssemblers(
      [], config, tmpDir, tmpDir, engine,
    );
    expect(result).toEqual({ files: [], warnings: [] });
  });

  it("executeAssemblers_pipelineErrorRethrown_notDoubleWrapped", () => {
    const original = new PipelineError("InnerAssembler", "already wrapped");
    const assemblers: AssemblerDescriptor[] = [{
      name: "Outer",
      assembler: {
        assemble: () => { throw original; },
      },
    }];
    try {
      executeAssemblers(assemblers, config, tmpDir, tmpDir, engine);
      expect.unreachable("Should have thrown");
    } catch (error) {
      expect(error).toBe(original);
      expect((error as PipelineError).assemblerName).toBe("InnerAssembler");
    }
  });

  it("executeAssemblers_subsequentNotCalledAfterFailure", () => {
    const called: string[] = [];
    const assemblers: AssemblerDescriptor[] = [
      {
        name: "First",
        assembler: {
          assemble: () => { called.push("First"); throw new Error("fail"); },
        },
      },
      {
        name: "Second",
        assembler: {
          assemble: () => { called.push("Second"); return []; },
        },
      },
    ];
    expect(() => executeAssemblers(
      assemblers, config, tmpDir, tmpDir, engine,
    )).toThrow(PipelineError);
    expect(called).toEqual(["First"]);
  });
});

describe("runPipeline", () => {
  let tmpDir: string;
  let resourcesDir: string;
  let outputDir: string;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(path.join(tmpdir(), "pipe-run-"));
    resourcesDir = path.join(tmpDir, "resources");
    outputDir = path.join(tmpDir, "output");
    fs.mkdirSync(resourcesDir, { recursive: true });
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  describe("dry-run mode", () => {
    it("runPipeline_dryRun_returnsSuccessTrue", async () => {
      const result = await runPipeline(
        buildConfig(), resourcesDir, outputDir, true,
      );
      expect(result.success).toBe(true);
    });

    it("runPipeline_dryRun_appendsDryRunWarning", async () => {
      const result = await runPipeline(
        buildConfig(), resourcesDir, outputDir, true,
      );
      expect(result.warnings).toContain(DRY_RUN_WARNING);
    });

    it("runPipeline_dryRun_outputDirMatchesParam", async () => {
      const result = await runPipeline(
        buildConfig(), resourcesDir, outputDir, true,
      );
      expect(result.outputDir).toBe(outputDir);
    });

    it("runPipeline_dryRun_doesNotCreateOutputDir", async () => {
      await runPipeline(
        buildConfig(), resourcesDir, outputDir, true,
      );
      expect(fs.existsSync(outputDir)).toBe(false);
    });

    it("runPipeline_dryRun_durationMsIsPositive", async () => {
      const result = await runPipeline(
        buildConfig(), resourcesDir, outputDir, true,
      );
      expect(result.durationMs).toBeGreaterThanOrEqual(0);
      expect(typeof result.durationMs).toBe("number");
    });

    it("runPipeline_dryRun_returnsPipelineResult", async () => {
      const result = await runPipeline(
        buildConfig(), resourcesDir, outputDir, true,
      );
      expect(result).toBeInstanceOf(PipelineResult);
    });
  });

  describe("real mode", () => {
    it("runPipeline_real_returnsSuccessTrue", async () => {
      const result = await runPipeline(
        buildConfig(), resourcesDir, outputDir, false,
      );
      expect(result.success).toBe(true);
    });

    it("runPipeline_real_createsOutputDir", async () => {
      await runPipeline(
        buildConfig(), resourcesDir, outputDir, false,
      );
      expect(fs.existsSync(outputDir)).toBe(true);
    });

    it("runPipeline_real_outputDirMatchesParam", async () => {
      const result = await runPipeline(
        buildConfig(), resourcesDir, outputDir, false,
      );
      expect(result.outputDir).toBe(outputDir);
    });

    it("runPipeline_real_durationMsIsPositive", async () => {
      const result = await runPipeline(
        buildConfig(), resourcesDir, outputDir, false,
      );
      expect(result.durationMs).toBeGreaterThanOrEqual(0);
    });

    it("runPipeline_real_doesNotContainDryRunWarning", async () => {
      const result = await runPipeline(
        buildConfig(), resourcesDir, outputDir, false,
      );
      expect(result.warnings).not.toContain(DRY_RUN_WARNING);
    });

    it("runPipeline_real_returnsPipelineResult", async () => {
      const result = await runPipeline(
        buildConfig(), resourcesDir, outputDir, false,
      );
      expect(result).toBeInstanceOf(PipelineResult);
    });

    it("runPipeline_real_filesGeneratedRootedAtOutputDir", async () => {
      const result = await runPipeline(
        buildConfig(), resourcesDir, outputDir, false,
      );
      const resolvedOutput = path.resolve(outputDir);
      for (const file of result.filesGenerated) {
        expect(file.startsWith(resolvedOutput)).toBe(true);
      }
    });
  });

  describe("duration measurement", () => {
    it("runPipeline_durationMs_isInteger", async () => {
      const result = await runPipeline(
        buildConfig(), resourcesDir, outputDir, true,
      );
      expect(Number.isInteger(result.durationMs)).toBe(true);
    });

    it("runPipeline_durationMs_reasonableRange", async () => {
      const result = await runPipeline(
        buildConfig(), resourcesDir, outputDir, true,
      );
      expect(result.durationMs).toBeLessThan(30000);
    });
  });
});
