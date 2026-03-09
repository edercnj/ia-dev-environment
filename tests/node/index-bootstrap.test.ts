import { resolve } from "node:path";
import { beforeAll, describe, expect, it, vi } from "vitest";

const runCliMock = vi.fn(async () => undefined);

vi.mock("../../src/cli.js", () => ({
  runCli: runCliMock,
}));

let bootstrap: (argv?: readonly string[]) => Promise<void>;
let shouldRunAsCli: (entryUrl?: string, argv?: readonly string[]) => boolean;

beforeAll(async () => {
  ({ bootstrap, shouldRunAsCli } = await import("../../src/index.js"));
});

describe("bootstrap", () => {
  it("moduleImport_doesNotTriggerBootstrapByDefault", () => {
    expect(runCliMock).not.toHaveBeenCalled();
  });

  it("bootstrap_withCustomArgv_forwardsArgsToCliRunner", async () => {
    runCliMock.mockClear();

    await bootstrap(["node", "ia-dev-env", "--help"]);

    expect(runCliMock).toHaveBeenCalledWith(["node", "ia-dev-env", "--help"]);
  });

  it("bootstrap_whenCliFails_propagatesError", async () => {
    runCliMock.mockRejectedValueOnce(new Error("cli failed"));

    await expect(bootstrap(["node", "ia-dev-env"])).rejects.toThrow("cli failed");
  });
});

describe("shouldRunAsCli", () => {
  it("shouldRunAsCli_withMatchingPath_returnsTrue", () => {
    const result = shouldRunAsCli("file:///tmp/index.js", ["node", "/tmp/index.js"]);
    expect(result).toBe(true);
  });

  it("shouldRunAsCli_withoutArgvEntry_returnsFalse", () => {
    const result = shouldRunAsCli("file:///tmp/index.js", ["node"]);
    expect(result).toBe(false);
  });
});

describe("module entry execution", () => {
  it("moduleImport_whenExecutedAsCli_runsMain", async () => {
    const originalArgv = process.argv;
    const entryPath = resolve(process.cwd(), "src/index.ts");

    runCliMock.mockClear();
    process.argv = ["node", entryPath];
    vi.resetModules();

    try {
      await import("../../src/index.js");
      await new Promise((resolveTick) => setImmediate(resolveTick));
      expect(runCliMock).toHaveBeenCalledTimes(1);
    } finally {
      process.argv = originalArgv;
    }
  });

  it("moduleImport_whenExecutedAsCliAndFails_setsExitCodeAndLogsSafeError", async () => {
    const originalArgv = process.argv;
    const originalExitCode = process.exitCode;
    const entryPath = resolve(process.cwd(), "src/index.ts");
    const errorSpy = vi.spyOn(console, "error").mockImplementation(() => undefined);

    runCliMock.mockClear();
    runCliMock.mockRejectedValueOnce(new Error("expected failure"));
    process.argv = ["node", entryPath];
    process.exitCode = undefined;
    vi.resetModules();

    try {
      await import("../../src/index.js");
      await new Promise((resolveTick) => setImmediate(resolveTick));
      expect(errorSpy).toHaveBeenCalledWith("Command failed. Run with --help for usage.");
      expect(process.exitCode).toBe(1);
    } finally {
      errorSpy.mockRestore();
      process.argv = originalArgv;
      process.exitCode = originalExitCode;
    }
  });
});
