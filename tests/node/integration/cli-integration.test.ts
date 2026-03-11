import {
  describe, it, expect, beforeEach, afterEach, vi,
} from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import { createCli } from "../../../src/cli.js";

const PROJECT_ROOT = path.resolve(__dirname, "../../..");
const RESOURCES_DIR = path.join(PROJECT_ROOT, "resources");
const FIXTURES_DIR = path.join(
  PROJECT_ROOT, "tests", "fixtures", "integration",
);

describe("CLI integration", { timeout: 30000 }, () => {
  let tmpDir: string;
  let errorSpy: ReturnType<typeof vi.spyOn>;
  let logSpy: ReturnType<typeof vi.spyOn>;
  let exitSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(path.join(tmpdir(), "cli-int-"));
    errorSpy = vi.spyOn(console, "error")
      .mockImplementation(() => {});
    logSpy = vi.spyOn(console, "log")
      .mockImplementation(() => {});
    exitSpy = vi.spyOn(process, "exit")
      .mockImplementation((() => {}) as never);
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
    vi.restoreAllMocks();
  });

  describe("DryRun", () => {
    it("dryRun_producesNoOutputFiles", async () => {
      const outputDir = path.join(tmpDir, "output");
      fs.mkdirSync(outputDir);
      const configPath = path.join(FIXTURES_DIR, "minimal.yaml");
      const cli = createCli();
      await cli.parseAsync([
        "node", "test", "generate",
        "--config", configPath,
        "--output-dir", outputDir,
        "--resources-dir", RESOURCES_DIR,
        "--dry-run",
      ]);
      const files = fs.readdirSync(outputDir);
      expect(files).toHaveLength(0);
    });

    it("dryRun_containsDryRunWarning", async () => {
      const outputDir = path.join(tmpDir, "output");
      fs.mkdirSync(outputDir);
      const configPath = path.join(FIXTURES_DIR, "minimal.yaml");
      const cli = createCli();
      await cli.parseAsync([
        "node", "test", "generate",
        "--config", configPath,
        "--output-dir", outputDir,
        "--resources-dir", RESOURCES_DIR,
        "--dry-run",
      ]);
      const allOutput = logSpy.mock.calls
        .map((c) => String(c[0])).join("\n");
      expect(allOutput.toLowerCase()).toContain("dry run");
    });

    it("dryRun_withValidConfig_succeeds", async () => {
      const outputDir = path.join(tmpDir, "output");
      fs.mkdirSync(outputDir);
      const configPath = path.join(FIXTURES_DIR, "minimal.yaml");
      const cli = createCli();
      await cli.parseAsync([
        "node", "test", "generate",
        "--config", configPath,
        "--output-dir", outputDir,
        "--resources-dir", RESOURCES_DIR,
        "--dry-run",
      ]);
      expect(exitSpy).not.toHaveBeenCalledWith(1);
    });
  });

  describe("ValidateCommand", () => {
    it("validate_validConfig_printsValid", async () => {
      const configPath = path.join(FIXTURES_DIR, "minimal.yaml");
      const cli = createCli();
      await cli.parseAsync([
        "node", "test", "validate",
        "--config", configPath,
      ]);
      expect(logSpy).toHaveBeenCalledWith("Config is valid.");
    });

    it("validate_invalidConfig_exitsWithError", async () => {
      const configPath = path.join(
        FIXTURES_DIR, "invalid-missing-section.yaml",
      );
      const cli = createCli();
      await cli.parseAsync([
        "node", "test", "validate",
        "--config", configPath,
      ]);
      expect(exitSpy).toHaveBeenCalledWith(1);
    });

    it("validate_missingSection_showsSectionName", async () => {
      const configPath = path.join(
        FIXTURES_DIR, "invalid-missing-section.yaml",
      );
      const cli = createCli();
      await cli.parseAsync([
        "node", "test", "validate",
        "--config", configPath,
      ]);
      const allErrors = errorSpy.mock.calls
        .map((c) => String(c[0])).join("\n");
      expect(allErrors).toContain("language");
    });
  });

  describe("ErrorHandling", () => {
    it("generate_invalidConfigPath_showsError", async () => {
      const cli = createCli();
      await cli.parseAsync([
        "node", "test", "generate",
        "--config", "nonexistent.yaml",
        "--output-dir", tmpDir,
        "--resources-dir", RESOURCES_DIR,
      ]);
      const allErrors = errorSpy.mock.calls
        .map((c) => String(c[0])).join("\n");
      expect(allErrors).toContain("Config file not found");
      expect(exitSpy).toHaveBeenCalledWith(1);
    });

    it("generate_malformedYaml_showsParseError", async () => {
      const configPath = path.join(
        FIXTURES_DIR, "invalid-bad-yaml.yaml",
      );
      const cli = createCli();
      await cli.parseAsync([
        "node", "test", "generate",
        "--config", configPath,
        "--output-dir", tmpDir,
        "--resources-dir", RESOURCES_DIR,
      ]);
      const allErrors = errorSpy.mock.calls
        .map((c) => String(c[0])).join("\n");
      expect(allErrors).toContain("Failed to parse config file");
      expect(exitSpy).toHaveBeenCalledWith(1);
    });

    it("generate_missingRequiredSection_showsError", async () => {
      const configPath = path.join(
        FIXTURES_DIR, "invalid-missing-section.yaml",
      );
      const cli = createCli();
      await cli.parseAsync([
        "node", "test", "generate",
        "--config", configPath,
        "--output-dir", tmpDir,
        "--resources-dir", RESOURCES_DIR,
      ]);
      const allErrors = errorSpy.mock.calls
        .map((c) => String(c[0])).join("\n");
      expect(allErrors).toContain(
        "Missing required config sections",
      );
      expect(exitSpy).toHaveBeenCalledWith(1);
    });

    it("validate_nonexistentConfig_showsError", async () => {
      const cli = createCli();
      await cli.parseAsync([
        "node", "test", "validate",
        "--config", "nonexistent.yaml",
      ]);
      const allErrors = errorSpy.mock.calls
        .map((c) => String(c[0])).join("\n");
      expect(allErrors).toContain("Config file not found");
      expect(exitSpy).toHaveBeenCalledWith(1);
    });
  });
});
