import { describe, expect, it, vi } from "vitest";

import type { Assembler } from "../../src/assembler/index.js";
import { ASSEMBLER_LAYER } from "../../src/assembler/index.js";
import { createRuntimePaths } from "../../src/config.js";
import { createCli, runCli } from "../../src/cli.js";
import type { DomainModule } from "../../src/domain/index.js";
import { DOMAIN_LAYER } from "../../src/domain/index.js";
import { CliError } from "../../src/exceptions.js";
import { promptConfirmation } from "../../src/interactive.js";
import { DEFAULT_FOUNDATION } from "../../src/models.js";
import { TemplateEngine } from "../../src/template-engine.js";
import { normalizeDirectory } from "../../src/utils.js";

const promptMock = vi.hoisted(() => vi.fn(async () => ({ confirmed: true })));

vi.mock("inquirer", () => ({
  default: {
    prompt: promptMock,
  },
}));

describe("createCli", () => {
  it("createCli_helpOutput_containsFoundationDescription", () => {
    const program = createCli();

    expect(program.name()).toBe("ia-dev-env");
    expect(program.description()).toContain("STORY-001");
    expect(program.helpInformation()).toContain("Usage: ia-dev-env");
  });

  it("runCli_withInjectedRunner_callsParseWithProvidedArgs", async () => {
    const parseAsync = vi.fn(async () => undefined);

    await runCli(["node", "ia-dev-env", "--help"], { parseAsync });

    expect(parseAsync).toHaveBeenCalledWith(["node", "ia-dev-env", "--help"]);
  });

  it("runCli_withInjectedRunnerFailure_propagatesError", async () => {
    const parseAsync = vi.fn(async () => {
      throw new Error("parse failed");
    });

    await expect(runCli(["node", "ia-dev-env"], { parseAsync })).rejects.toThrow("parse failed");
  });
});

describe("foundation modules", () => {
  it.each([
    ["/tmp/foundation", "/tmp/foundation/dist", "/tmp/foundation/resources"],
    ["", `${process.cwd()}/dist`, `${process.cwd()}/resources`],
  ])(
    "createRuntimePaths_withDifferentInputs_resolvesExpectedDirectories",
    (cwd, expectedOutputDir, expectedResourcesDir) => {
      const paths = createRuntimePaths(cwd);
      expect(paths.outputDir).toBe(expectedOutputDir);
      expect(paths.resourcesDir).toBe(expectedResourcesDir);
    },
  );

  it("templateAndUtils_withSimpleInput_returnExpectedValues", () => {
    const engine = new TemplateEngine();

    expect(engine.render("Hello {{ name }}", { name: "Node" })).toBe("Hello Node");
    expect(normalizeDirectory("/tmp/project///")).toBe("/tmp/project");
  });

  it.each([
    ["/", "/"],
    ["///", "/"],
    ["C:\\", "C:\\"],
    ["C:/", "C:\\"],
  ])("normalizeDirectory_withRootPaths_preservesRoot", (inputPath, expectedPath) => {
    expect(normalizeDirectory(inputPath)).toBe(expectedPath);
  });

  it("templateEngine_withInvalidTemplate_throws", () => {
    const engine = new TemplateEngine();

    expect(() => engine.render("{{", {})).toThrow();
  });

  it("cliError_constructor_setsNameAndCode", () => {
    const error = new CliError("failure", "E_STUB");

    expect(error.name).toBe("CliError");
    expect(error.code).toBe("E_STUB");
  });

  it("promptConfirmation_whenPromptResolves_returnsBoolean", async () => {
    const confirmed = await promptConfirmation("Proceed?");

    expect(confirmed).toBe(true);
  });

  it("promptConfirmation_whenPromptRejects_propagatesError", async () => {
    promptMock.mockRejectedValueOnce(new Error("prompt failed"));

    await expect(promptConfirmation("Proceed?")).rejects.toThrow("prompt failed");
  });

  it("promptConfirmation_withInvalidTimeout_throwsCliError", async () => {
    await expect(promptConfirmation("Proceed?", true, 0)).rejects.toThrow("Prompt timeout must be greater than zero.");
  });

  it("typeContracts_compileWithReadonlyShape", () => {
    const module: DomainModule = { id: "domain.foundation" };
    const assembler: Assembler = { assemble: async () => undefined };

    expect(DEFAULT_FOUNDATION.moduleType).toBe("module");
    expect(module.id).toBe("domain.foundation");
    expect(typeof assembler.assemble).toBe("function");
    expect(ASSEMBLER_LAYER).toBe("assembler");
    expect(DOMAIN_LAYER).toBe("domain");
  });
});
