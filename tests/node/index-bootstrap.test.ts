import { beforeAll, describe, expect, it, vi } from "vitest";

const runCliMock = vi.fn(async () => undefined);

vi.mock("../../src/cli.js", () => ({
  runCli: runCliMock,
}));

let bootstrap: (argv?: readonly string[]) => Promise<void>;

beforeAll(async () => {
  ({ bootstrap } = await import("../../src/index.js"));
});

describe("bootstrap", () => {
  it("moduleImport_triggersDefaultBootstrapCall", () => {
    expect(runCliMock).toHaveBeenCalledTimes(1);
    expect(runCliMock).toHaveBeenCalledWith(expect.any(Array));
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
