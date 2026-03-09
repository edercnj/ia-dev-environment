import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import {
  mkdtempSync,
  mkdirSync,
  writeFileSync,
  readFileSync,
  existsSync,
  symlinkSync,
  rmSync,
  chmodSync,
} from "node:fs";
import { writeFile } from "node:fs/promises";
import { homedir, tmpdir } from "node:os";
import { join, resolve } from "node:path";
import {
  PROTECTED_PATHS,
  rejectDangerousPath,
  setupLogging,
  findResourcesDir,
  validateDestPath,
  atomicOutput,
} from "../../src/utils.js";

describe("PROTECTED_PATHS", () => {
  it("contents_containsAllExpectedPaths", () => {
    const expected = ["/", "/tmp", "/var", "/etc", "/usr"];
    for (const p of expected) {
      expect(PROTECTED_PATHS).toContain(p);
    }
    expect(PROTECTED_PATHS).toHaveLength(5);
  });

  it("type_isFrozenArray", () => {
    expect(Array.isArray(PROTECTED_PATHS)).toBe(true);
    expect(Object.isFrozen(PROTECTED_PATHS)).toBe(true);
  });
});

describe("rejectDangerousPath", () => {
  it("withRootPath_throws", () => {
    expect(() => rejectDangerousPath("/")).toThrow(
      "Destination is a protected system path: /",
    );
  });

  it("withTmpPath_throws", () => {
    expect(() => rejectDangerousPath("/tmp")).toThrow(
      "protected system path",
    );
  });

  it("withVarPath_throws", () => {
    expect(() => rejectDangerousPath("/var")).toThrow(
      "protected system path",
    );
  });

  it("withEtcPath_throws", () => {
    expect(() => rejectDangerousPath("/etc")).toThrow(
      "protected system path",
    );
  });

  it("withUsrPath_throws", () => {
    expect(() => rejectDangerousPath("/usr")).toThrow(
      "protected system path",
    );
  });

  it("withCwd_throws", () => {
    const fakeCwd = "/fake/cwd/for/test";
    vi.spyOn(process, "cwd").mockReturnValue(fakeCwd);
    expect(() => rejectDangerousPath(fakeCwd)).toThrow(
      "current directory",
    );
    vi.restoreAllMocks();
  });

  it("withHomeDir_throws", () => {
    const realHome = homedir();
    expect(() => rejectDangerousPath(realHome)).toThrow(
      "home directory",
    );
  });

  it("withSafePath_doesNotThrow", () => {
    expect(() =>
      rejectDangerousPath("/home/user/projects/output"),
    ).not.toThrow();
  });

  it("withSubdirOfProtected_doesNotThrow", () => {
    expect(() =>
      rejectDangerousPath("/tmp/ia-dev-env-output"),
    ).not.toThrow();
  });
});

describe("setupLogging", () => {
  const realDebug = console.debug;

  afterEach(() => {
    console.debug = realDebug;
    setupLogging(true);
  });

  it("withVerboseFalse_suppressesDebug", () => {
    setupLogging(false);
    const spy = vi.spyOn(console, "debug");
    console.debug("test message");
    expect(spy).toHaveBeenCalledTimes(1);
    // The function is a no-op, so it should not produce output
    // We verify by checking that debug was replaced
    expect(console.debug).not.toBe(realDebug);
    spy.mockRestore();
  });

  it("withVerboseTrue_restoresDebug", () => {
    setupLogging(false);
    setupLogging(true);
    expect(console.debug).toBe(realDebug);
  });
});

describe("findResourcesDir", () => {
  it("fromSourceLayout_resolvesToResourcesDir", () => {
    const result = findResourcesDir();
    expect(result).toContain("resources");
  });

  it("returnedPath_isAbsolute", () => {
    const result = findResourcesDir();
    expect(result.startsWith("/")).toBe(true);
  });

  it("returnedPath_existsOnDisk", () => {
    const result = findResourcesDir();
    expect(existsSync(result)).toBe(true);
  });

  it("withInvalidMetaUrl_throwsNotFound", () => {
    const fakeUrl = "file:///nonexistent/path/src/utils.ts";
    expect(() => findResourcesDir(fakeUrl)).toThrow(
      "Resources directory not found",
    );
  });
});

describe("validateDestPath", () => {
  let tempBase: string;

  beforeEach(() => {
    tempBase = mkdtempSync(join(tmpdir(), "validate-test-"));
  });

  afterEach(() => {
    rmSync(tempBase, { recursive: true, force: true });
  });

  it("withValidDirectory_returnsResolvedPath", async () => {
    const result = await validateDestPath(tempBase);
    expect(result).toBe(resolve(tempBase));
  });

  it("withSymlink_rejectsWithError", async () => {
    const target = join(tempBase, "target");
    mkdirSync(target);
    const link = join(tempBase, "link");
    symlinkSync(target, link);

    await expect(validateDestPath(link)).rejects.toThrow(
      "must not be a symlink",
    );
  });

  it("withDangerousPath_delegatesToRejectDangerousPath", async () => {
    await expect(validateDestPath("/")).rejects.toThrow(
      "protected system path",
    );
  });

  it("withNonExistentPath_handlesGracefully", async () => {
    const nonExistent = join(tempBase, "does-not-exist");
    const result = await validateDestPath(nonExistent);
    expect(result).toBe(resolve(nonExistent));
  });

  it("withRelativePath_resolvesToAbsolute", async () => {
    const nonExistent = join(tempBase, "relative-output");
    const result = await validateDestPath(nonExistent);
    expect(result.startsWith("/")).toBe(true);
  });
});

describe("atomicOutput", () => {
  let tempBase: string;

  beforeEach(() => {
    tempBase = mkdtempSync(join(tmpdir(), "atomic-test-"));
  });

  afterEach(() => {
    rmSync(tempBase, { recursive: true, force: true });
  });

  it("withSuccessfulCallback_copiesToDest", async () => {
    const destDir = join(tempBase, "output");

    await atomicOutput(destDir, async (tempDir) => {
      await writeFile(join(tempDir, "test.txt"), "hello");
    });

    expect(existsSync(join(destDir, "test.txt"))).toBe(true);
    expect(readFileSync(join(destDir, "test.txt"), "utf-8")).toBe(
      "hello",
    );
  });

  it("withSuccessfulCallback_cleansTempDir", async () => {
    const destDir = join(tempBase, "output");
    let capturedTempDir = "";

    await atomicOutput(destDir, async (tempDir) => {
      capturedTempDir = tempDir;
      await writeFile(join(tempDir, "test.txt"), "data");
    });

    expect(existsSync(capturedTempDir)).toBe(false);
  });

  it("withSuccessfulCallback_returnsCallbackResult", async () => {
    const destDir = join(tempBase, "output");

    const result = await atomicOutput(destDir, async (tempDir) => {
      await writeFile(join(tempDir, "f.txt"), "x");
      return 42;
    });

    expect(result).toBe(42);
  });

  it("withFailingCallback_doesNotModifyDest", async () => {
    const destDir = join(tempBase, "output");
    mkdirSync(destDir);
    writeFileSync(join(destDir, "existing.txt"), "original");

    await expect(
      atomicOutput(destDir, async () => {
        throw new Error("boom");
      }),
    ).rejects.toThrow("boom");

    expect(readFileSync(join(destDir, "existing.txt"), "utf-8")).toBe(
      "original",
    );
  });

  it("withFailingCallback_cleansTempDir", async () => {
    const destDir = join(tempBase, "output");
    let capturedTempDir = "";

    await expect(
      atomicOutput(destDir, async (tempDir) => {
        capturedTempDir = tempDir;
        throw new Error("boom");
      }),
    ).rejects.toThrow("boom");

    expect(capturedTempDir).not.toBe("");
    expect(existsSync(capturedTempDir)).toBe(false);
  });

  it("withFailingCallback_propagatesOriginalError", async () => {
    const destDir = join(tempBase, "output");

    await expect(
      atomicOutput(destDir, async () => {
        throw new Error("specific error message");
      }),
    ).rejects.toThrow("specific error message");
  });

  it("withExistingDestDir_replacesContents", async () => {
    const destDir = join(tempBase, "output");
    mkdirSync(destDir);
    writeFileSync(join(destDir, "old.txt"), "old");

    await atomicOutput(destDir, async (tempDir) => {
      await writeFile(join(tempDir, "new.txt"), "new");
    });

    expect(existsSync(join(destDir, "old.txt"))).toBe(false);
    expect(readFileSync(join(destDir, "new.txt"), "utf-8")).toBe(
      "new",
    );
  });

  it("withNonExistentDestDir_createsIt", async () => {
    const destDir = join(tempBase, "brand-new");
    expect(existsSync(destDir)).toBe(false);

    await atomicOutput(destDir, async (tempDir) => {
      await writeFile(join(tempDir, "file.txt"), "content");
    });

    expect(existsSync(destDir)).toBe(true);
    expect(
      readFileSync(join(destDir, "file.txt"), "utf-8"),
    ).toBe("content");
  });

  it("callbackReceivesTempDir_asFunctionArg", async () => {
    const destDir = join(tempBase, "output");
    let receivedTempDir = "";

    await atomicOutput(destDir, async (tempDir) => {
      receivedTempDir = tempDir;
      await writeFile(join(tempDir, "f.txt"), "x");
    });

    expect(receivedTempDir).toContain("ia-dev-env-");
  });

  it("withInaccessibleDestParent_propagatesError", async () => {
    const restrictedDir = join(tempBase, "restricted");
    mkdirSync(restrictedDir);
    const destDir = join(restrictedDir, "output");
    mkdirSync(destDir);

    // Remove read+execute permissions from parent to cause EACCES on stat
    chmodSync(restrictedDir, 0o000);

    try {
      await expect(
        atomicOutput(destDir, async (tempDir) => {
          await writeFile(join(tempDir, "f.txt"), "x");
        }),
      ).rejects.toThrow();
    } finally {
      // Restore permissions for cleanup
      chmodSync(restrictedDir, 0o755);
    }
  });
});
