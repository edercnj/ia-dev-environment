import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { join, resolve } from "node:path";
import { tmpdir } from "node:os";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { PipelineResult } from "../../src/models.js";
import {
  ArchitectureConfig,
  FrameworkConfig,
  InterfaceConfig,
  LanguageConfig,
  ProjectConfig,
  ProjectIdentity,
} from "../../src/models.js";

// --- Mock Setup ---

const mockLoadConfig = vi.fn<(path: string) => ProjectConfig>();
const mockRunPipeline = vi.fn<
  (
    config: ProjectConfig,
    resourcesDir: string,
    outputDir: string,
    dryRun: boolean,
  ) => Promise<PipelineResult>
>();
const mockRunInteractive = vi.fn<() => Promise<ProjectConfig>>();
const mockFindResourcesDir = vi.fn<() => string>();
const mockSetupLogging = vi.fn<(verbose: boolean) => void>();
const mockValidateStack = vi.fn<(config: ProjectConfig) => string[]>();
const mockDisplayResult = vi.fn<(result: PipelineResult) => void>();

vi.mock("../../src/config.js", () => ({
  loadConfig: (...args: unknown[]) => mockLoadConfig(args[0] as string),
}));

vi.mock("../../src/assembler/pipeline.js", () => ({
  runPipeline: (...args: unknown[]) =>
    mockRunPipeline(
      args[0] as ProjectConfig,
      args[1] as string,
      args[2] as string,
      args[3] as boolean,
    ),
}));

vi.mock("../../src/interactive.js", () => ({
  runInteractive: () => mockRunInteractive(),
}));

vi.mock("../../src/utils.js", () => ({
  findResourcesDir: () => mockFindResourcesDir(),
  setupLogging: (v: boolean) => mockSetupLogging(v),
}));

vi.mock("../../src/domain/validator.js", () => ({
  validateStack: (config: ProjectConfig) => mockValidateStack(config),
}));

vi.mock("../../src/cli-display.js", () => ({
  displayResult: (r: PipelineResult) => mockDisplayResult(r),
}));

import { createCli } from "../../src/cli.js";
import {
  ConfigParseError,
  ConfigValidationError,
  PipelineError,
} from "../../src/exceptions.js";

// --- Helpers ---

function buildTestConfig(): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity("test-project", "A test"),
    new ArchitectureConfig("library"),
    [new InterfaceConfig("cli")],
    new LanguageConfig("typescript", "5"),
    new FrameworkConfig("commander", "12", "npm"),
  );
}

function buildTestPipelineResult(): PipelineResult {
  return new PipelineResult(
    true,
    ".",
    ["rules/01-identity.md"],
    [],
    42,
  );
}

let tmpDir: string;
let configFilePath: string;
let exitSpy: ReturnType<typeof vi.spyOn>;
let errorSpy: ReturnType<typeof vi.spyOn>;
let logSpy: ReturnType<typeof vi.spyOn>;

beforeEach(() => {
  vi.clearAllMocks();

  tmpDir = mkdtempSync(join(tmpdir(), "cli-test-"));
  configFilePath = join(tmpDir, "config.yaml");
  writeFileSync(configFilePath, "project:\n  name: test\n", "utf-8");

  mockLoadConfig.mockReturnValue(buildTestConfig());
  mockRunPipeline.mockResolvedValue(buildTestPipelineResult());
  mockRunInteractive.mockResolvedValue(buildTestConfig());
  mockFindResourcesDir.mockReturnValue("/mock/resources");
  mockValidateStack.mockReturnValue([]);

  exitSpy = vi.spyOn(process, "exit").mockImplementation(
    (() => undefined) as never,
  );
  errorSpy = vi.spyOn(console, "error").mockImplementation(
    () => undefined,
  );
  logSpy = vi.spyOn(console, "log").mockImplementation(
    () => undefined,
  );
});

afterEach(() => {
  rmSync(tmpDir, { recursive: true, force: true });
  exitSpy.mockRestore();
  errorSpy.mockRestore();
  logSpy.mockRestore();
});

/** Parse CLI args through a fresh createCli(). */
async function parseCli(args: string[]): Promise<void> {
  const cli = createCli();
  await cli.parseAsync(["node", "test", ...args]);
}

// --- Tests ---

describe("generate command", () => {
  it("generate_withConfig_callsLoadConfig", async () => {
    await parseCli(["generate", "--config", configFilePath]);

    expect(mockLoadConfig).toHaveBeenCalledWith(configFilePath);
  });

  it("generate_withConfig_callsRunPipeline", async () => {
    await parseCli(["generate", "--config", configFilePath]);

    expect(mockRunPipeline).toHaveBeenCalledWith(
      expect.any(ProjectConfig),
      "/mock/resources",
      ".",
      false,
    );
  });

  it("generate_withConfig_callsDisplayResult", async () => {
    await parseCli(["generate", "--config", configFilePath]);

    expect(mockDisplayResult).toHaveBeenCalledWith(
      expect.any(PipelineResult),
    );
  });

  it("generate_withInteractive_callsRunInteractive", async () => {
    await parseCli(["generate", "--interactive"]);

    expect(mockRunInteractive).toHaveBeenCalledTimes(1);
  });

  it("generate_withInteractive_passesResultToPipeline", async () => {
    const testConfig = buildTestConfig();
    mockRunInteractive.mockResolvedValue(testConfig);

    await parseCli(["generate", "--interactive"]);

    expect(mockRunPipeline).toHaveBeenCalledWith(
      testConfig,
      "/mock/resources",
      ".",
      false,
    );
  });

  it("generate_bothConfigAndInteractive_throwsError", async () => {
    await parseCli([
      "generate",
      "--config",
      configFilePath,
      "--interactive",
    ]);

    expect(errorSpy).toHaveBeenCalledWith(
      expect.stringContaining("mutually exclusive"),
    );
    expect(exitSpy).toHaveBeenCalledWith(1);
  });

  it("generate_neitherConfigNorInteractive_throwsError", async () => {
    await parseCli(["generate"]);

    expect(errorSpy).toHaveBeenCalledWith(
      expect.stringContaining("Either --config or --interactive"),
    );
    expect(exitSpy).toHaveBeenCalledWith(1);
  });

  it("generate_withVerbose_callsSetupLogging", async () => {
    await parseCli([
      "generate",
      "--config",
      configFilePath,
      "--verbose",
    ]);

    expect(mockSetupLogging).toHaveBeenCalledWith(true);
  });

  it("generate_withDryRun_passedToPipeline", async () => {
    await parseCli([
      "generate",
      "--config",
      configFilePath,
      "--dry-run",
    ]);

    expect(mockRunPipeline).toHaveBeenCalledWith(
      expect.any(ProjectConfig),
      "/mock/resources",
      ".",
      true,
    );
  });

  it("generate_withResourcesDir_usesProvidedPath", async () => {
    const resDir = join(tmpDir, "resources");
    mkdirSync(resDir, { recursive: true });

    await parseCli([
      "generate",
      "--config",
      configFilePath,
      "--resources-dir",
      resDir,
    ]);

    expect(mockRunPipeline).toHaveBeenCalledWith(
      expect.any(ProjectConfig),
      resDir,
      ".",
      false,
    );
    expect(mockFindResourcesDir).not.toHaveBeenCalled();
  });

  it("generate_withoutResourcesDir_callsFindResourcesDir", async () => {
    await parseCli(["generate", "--config", configFilePath]);

    expect(mockFindResourcesDir).toHaveBeenCalledTimes(1);
  });

  it("generate_outputDirDefault_usesDot", async () => {
    await parseCli(["generate", "--config", configFilePath]);

    expect(mockRunPipeline).toHaveBeenCalledWith(
      expect.any(ProjectConfig),
      "/mock/resources",
      ".",
      false,
    );
  });

  it("generate_customOutputDir_passedToPipeline", async () => {
    await parseCli([
      "generate",
      "--config",
      configFilePath,
      "--output-dir",
      "/custom/out",
    ]);

    expect(mockRunPipeline).toHaveBeenCalledWith(
      expect.any(ProjectConfig),
      "/mock/resources",
      "/custom/out",
      false,
    );
  });

  it("generate_configNotFound_showsError", async () => {
    await parseCli([
      "generate",
      "--config",
      "/nonexistent/config.yaml",
    ]);

    expect(errorSpy).toHaveBeenCalledWith(
      expect.stringContaining("Config file not found"),
    );
    expect(exitSpy).toHaveBeenCalledWith(1);
  });

  it("generate_resourcesDirNotFound_showsError", async () => {
    await parseCli([
      "generate",
      "--config",
      configFilePath,
      "--resources-dir",
      "/nonexistent/resources",
    ]);

    expect(errorSpy).toHaveBeenCalledWith(
      expect.stringContaining("Resources directory not found"),
    );
    expect(exitSpy).toHaveBeenCalledWith(1);
  });

  it("generate_configValidationError_showsFriendlyMessage", async () => {
    mockLoadConfig.mockImplementation(() => {
      throw new ConfigValidationError(["project", "language"]);
    });

    await parseCli(["generate", "--config", configFilePath]);

    expect(errorSpy).toHaveBeenCalledWith(
      expect.stringContaining("Missing required config sections"),
    );
    expect(exitSpy).toHaveBeenCalledWith(1);
  });

  it("generate_pipelineError_showsFriendlyMessage", async () => {
    mockRunPipeline.mockRejectedValue(
      new PipelineError("RulesAssembler", "Template not found"),
    );

    await parseCli(["generate", "--config", configFilePath]);

    expect(errorSpy).toHaveBeenCalledWith(
      expect.stringContaining("Pipeline failed"),
    );
    expect(exitSpy).toHaveBeenCalledWith(1);
  });

  it("generate_configParseError_showsFriendlyMessage", async () => {
    mockLoadConfig.mockImplementation(() => {
      throw new ConfigParseError("invalid yaml");
    });

    await parseCli(["generate", "--config", configFilePath]);

    expect(errorSpy).toHaveBeenCalledWith(
      expect.stringContaining("Failed to parse config file"),
    );
    expect(exitSpy).toHaveBeenCalledWith(1);
  });

  it("generate_genericError_nonVerbose_showsGenericMessage", async () => {
    mockRunPipeline.mockRejectedValue(new Error("unexpected"));

    await parseCli(["generate", "--config", configFilePath]);

    expect(errorSpy).toHaveBeenCalledWith(
      "Command failed. Run with --help for usage.",
    );
    expect(exitSpy).toHaveBeenCalledWith(1);
  });

  it("generate_genericError_verbose_showsStackTrace", async () => {
    const err = new Error("unexpected verbose");
    mockRunPipeline.mockRejectedValue(err);

    await parseCli([
      "generate",
      "--config",
      configFilePath,
      "--verbose",
    ]);

    const stackCall = errorSpy.mock.calls.find(
      (call) =>
        typeof call[0] === "string"
        && call[0].includes("unexpected verbose"),
    );
    expect(stackCall).toBeDefined();
    expect(exitSpy).toHaveBeenCalledWith(1);
  });
});

describe("validate command", () => {
  it("validate_validConfig_printsValidMessage", async () => {
    mockValidateStack.mockReturnValue([]);

    await parseCli(["validate", "--config", configFilePath]);

    expect(logSpy).toHaveBeenCalledWith("Config is valid.");
  });

  it("validate_validConfig_callsLoadConfig", async () => {
    await parseCli(["validate", "--config", configFilePath]);

    expect(mockLoadConfig).toHaveBeenCalledWith(configFilePath);
  });

  it("validate_validConfig_callsValidateStack", async () => {
    await parseCli(["validate", "--config", configFilePath]);

    expect(mockValidateStack).toHaveBeenCalledWith(
      expect.any(ProjectConfig),
    );
  });

  it("validate_invalidStack_printsErrors", async () => {
    mockValidateStack.mockReturnValue([
      "Missing language",
      "Invalid framework",
    ]);

    await parseCli(["validate", "--config", configFilePath]);

    expect(errorSpy).toHaveBeenCalledWith(
      "Missing language\nInvalid framework",
    );
    expect(exitSpy).toHaveBeenCalledWith(1);
  });

  it("validate_configParseError_showsFriendlyMessage", async () => {
    mockLoadConfig.mockImplementation(() => {
      throw new ConfigParseError("bad yaml syntax");
    });

    await parseCli(["validate", "--config", configFilePath]);

    expect(errorSpy).toHaveBeenCalledWith(
      expect.stringContaining("Failed to parse config file"),
    );
    expect(exitSpy).toHaveBeenCalledWith(1);
  });

  it("validate_configValidationError_showsFriendlyMessage", async () => {
    mockLoadConfig.mockImplementation(() => {
      throw new ConfigValidationError(["project"]);
    });

    await parseCli(["validate", "--config", configFilePath]);

    expect(errorSpy).toHaveBeenCalledWith(
      expect.stringContaining("Missing required config sections"),
    );
    expect(exitSpy).toHaveBeenCalledWith(1);
  });

  it("validate_withVerbose_callsSetupLogging", async () => {
    await parseCli([
      "validate",
      "--config",
      configFilePath,
      "--verbose",
    ]);

    expect(mockSetupLogging).toHaveBeenCalledWith(true);
  });

  it("validate_configNotFound_showsError", async () => {
    await parseCli([
      "validate",
      "--config",
      "/nonexistent/config.yaml",
    ]);

    expect(errorSpy).toHaveBeenCalledWith(
      expect.stringContaining("Config file not found"),
    );
    expect(exitSpy).toHaveBeenCalledWith(1);
  });

  it("validate_missingConfigOption_showsError", async () => {
    const stderrSpy = vi
      .spyOn(process.stderr, "write")
      .mockImplementation(() => true);

    try {
      await parseCli(["validate"]);

      expect(exitSpy).toHaveBeenCalledWith(1);
    } finally {
      stderrSpy.mockRestore();
    }
  });
});

describe("CLI help and version", () => {
  it("cli_help_showsAvailableCommands", () => {
    const cli = createCli();
    const help = cli.helpInformation();

    expect(help).toContain("generate");
    expect(help).toContain("validate");
  });

  it("cli_generateHelp_showsAllOptions", () => {
    const cli = createCli();
    const generateCmd = cli.commands.find(
      (cmd) => cmd.name() === "generate",
    );

    expect(generateCmd).toBeDefined();
    const help = generateCmd!.helpInformation();

    expect(help).toContain("--config");
    expect(help).toContain("--interactive");
    expect(help).toContain("--output-dir");
    expect(help).toContain("--resources-dir");
    expect(help).toContain("--verbose");
    expect(help).toContain("--dry-run");
  });

  it("cli_validateHelp_showsAllOptions", () => {
    const cli = createCli();
    const validateCmd = cli.commands.find(
      (cmd) => cmd.name() === "validate",
    );

    expect(validateCmd).toBeDefined();
    const help = validateCmd!.helpInformation();

    expect(help).toContain("--config");
    expect(help).toContain("--verbose");
  });

  it("cli_version_showsVersion", () => {
    const cli = createCli();
    cli.exitOverride();

    let output = "";
    cli.configureOutput({
      writeOut: (str: string) => {
        output += str;
      },
    });

    try {
      cli.parse(["node", "test", "--version"]);
    } catch {
      // Commander throws on --version with exitOverride
    }

    expect(output).toContain("0.1.0");
  });

  it("cli_description_matchesProjectIdentity", () => {
    const cli = createCli();

    expect(cli.description()).toContain("Claude Setup");
  });
});

describe("index.ts error handling", () => {
  it("main_configValidationError_printsFriendlyMessage", async () => {
    const originalArgv = process.argv;
    const originalExitCode = process.exitCode;
    const entryPath = resolve(process.cwd(), "src/index.ts");

    vi.resetModules();

    // Import from fresh module graph so instanceof matches in index.ts
    const { ConfigValidationError: FreshCVE } =
      await import("../../src/exceptions.js");

    vi.doMock("../../src/cli.js", () => ({
      runCli: vi.fn(async () => {
        throw new FreshCVE(["project"]);
      }),
    }));

    const errorSpyLocal = vi
      .spyOn(console, "error")
      .mockImplementation(() => undefined);

    process.argv = ["node", entryPath];
    process.exitCode = undefined;

    try {
      await import("../../src/index.js");
      await new Promise((tick) => setImmediate(tick));

      expect(errorSpyLocal).toHaveBeenCalledWith(
        expect.stringContaining("Missing required config sections"),
      );
      expect(process.exitCode).toBe(1);
    } finally {
      errorSpyLocal.mockRestore();
      process.argv = originalArgv;
      process.exitCode = originalExitCode;
      vi.doUnmock("../../src/cli.js");
      vi.resetModules();
    }
  });

  it("main_pipelineError_printsFriendlyMessage", async () => {
    const originalArgv = process.argv;
    const originalExitCode = process.exitCode;
    const entryPath = resolve(process.cwd(), "src/index.ts");

    vi.resetModules();

    // Import from fresh module graph so instanceof matches in index.ts
    const { PipelineError: FreshPE } =
      await import("../../src/exceptions.js");

    vi.doMock("../../src/cli.js", () => ({
      runCli: vi.fn(async () => {
        throw new FreshPE("TestAssembler", "broke");
      }),
    }));

    const errorSpyLocal = vi
      .spyOn(console, "error")
      .mockImplementation(() => undefined);

    process.argv = ["node", entryPath];
    process.exitCode = undefined;

    try {
      await import("../../src/index.js");
      await new Promise((tick) => setImmediate(tick));

      expect(errorSpyLocal).toHaveBeenCalledWith(
        expect.stringContaining("Pipeline failed at 'TestAssembler'"),
      );
      expect(process.exitCode).toBe(1);
    } finally {
      errorSpyLocal.mockRestore();
      process.argv = originalArgv;
      process.exitCode = originalExitCode;
      vi.doUnmock("../../src/cli.js");
      vi.resetModules();
    }
  });
});
