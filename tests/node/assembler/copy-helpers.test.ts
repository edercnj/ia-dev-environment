import {
  describe,
  it,
  expect,
  beforeEach,
  afterEach,
  vi,
} from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import * as os from "node:os";
import {
  copyTemplateFile,
  copyTemplateFileIfExists,
  copyTemplateTree,
  copyTemplateTreeIfExists,
  replacePlaceholdersInDir,
} from "../../../src/assembler/copy-helpers.js";
import type { TemplateEngine } from "../../../src/template-engine.js";

let tmpDir: string;
let srcDir: string;
let destDir: string;

const mockEngine: TemplateEngine = {
  replacePlaceholders: vi.fn((content: string) =>
    content.replace("{project_name}", "my-app"),
  ),
} as unknown as TemplateEngine;

beforeEach(() => {
  tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), "copy-helpers-"));
  srcDir = path.join(tmpDir, "src");
  destDir = path.join(tmpDir, "dest");
  fs.mkdirSync(srcDir, { recursive: true });
  vi.clearAllMocks();
});

afterEach(() => {
  fs.rmSync(tmpDir, { recursive: true, force: true });
});

describe("copyTemplateFile", () => {
  it("copyTemplateFile_mdWithPlaceholder_replacesPlaceholder", () => {
    const srcFile = path.join(srcDir, "readme.md");
    const destFile = path.join(destDir, "readme.md");
    fs.writeFileSync(srcFile, "# {project_name}");

    const result = copyTemplateFile(srcFile, destFile, mockEngine);

    expect(result).toBe(destFile);
    expect(fs.readFileSync(destFile, "utf-8")).toBe("# my-app");
  });

  it("copyTemplateFile_createsParentDirs_ifNotExist", () => {
    const srcFile = path.join(srcDir, "file.md");
    const destFile = path.join(destDir, "deep", "nested", "file.md");
    fs.writeFileSync(srcFile, "content");

    copyTemplateFile(srcFile, destFile, mockEngine);

    expect(fs.existsSync(destFile)).toBe(true);
  });

  it("copyTemplateFile_callsReplacePlaceholders_withContent", () => {
    const srcFile = path.join(srcDir, "test.md");
    fs.writeFileSync(srcFile, "hello {project_name}");
    const destFile = path.join(destDir, "test.md");

    copyTemplateFile(srcFile, destFile, mockEngine);

    expect(mockEngine.replacePlaceholders).toHaveBeenCalledWith(
      "hello {project_name}",
    );
  });
});

describe("copyTemplateFileIfExists", () => {
  it("copyTemplateFileIfExists_sourceExists_copiesFile", () => {
    const srcFile = path.join(srcDir, "exists.md");
    const destFile = path.join(destDir, "exists.md");
    fs.writeFileSync(srcFile, "data");

    const result = copyTemplateFileIfExists(
      srcFile,
      destFile,
      mockEngine,
    );

    expect(result).toBe(destFile);
    expect(fs.existsSync(destFile)).toBe(true);
  });

  it("copyTemplateFileIfExists_sourceMissing_returnsNull", () => {
    const result = copyTemplateFileIfExists(
      path.join(srcDir, "missing.md"),
      path.join(destDir, "missing.md"),
      mockEngine,
    );

    expect(result).toBeNull();
  });
});

describe("copyTemplateTree", () => {
  it("copyTemplateTree_preservesStructure_replacesPlaceholdersInMd", () => {
    fs.mkdirSync(path.join(srcDir, "sub"), { recursive: true });
    fs.writeFileSync(
      path.join(srcDir, "root.md"),
      "# {project_name}",
    );
    fs.writeFileSync(
      path.join(srcDir, "sub", "nested.md"),
      "## {project_name}",
    );
    fs.writeFileSync(
      path.join(srcDir, "data.txt"),
      "{project_name}",
    );

    const result = copyTemplateTree(srcDir, destDir, mockEngine);

    expect(result).toBe(destDir);
    expect(fs.readFileSync(path.join(destDir, "root.md"), "utf-8")).toBe(
      "# my-app",
    );
    expect(
      fs.readFileSync(path.join(destDir, "sub", "nested.md"), "utf-8"),
    ).toBe("## my-app");
    // Non-md files are copied but not replaced
    expect(
      fs.readFileSync(path.join(destDir, "data.txt"), "utf-8"),
    ).toBe("{project_name}");
  });
});

describe("copyTemplateTreeIfExists", () => {
  it("copyTemplateTreeIfExists_sourceExists_copiesTree", () => {
    fs.writeFileSync(path.join(srcDir, "file.md"), "content");

    const result = copyTemplateTreeIfExists(
      srcDir,
      destDir,
      mockEngine,
    );

    expect(result).toBe(destDir);
    expect(fs.existsSync(path.join(destDir, "file.md"))).toBe(true);
  });

  it("copyTemplateTreeIfExists_sourceMissing_returnsNull", () => {
    const result = copyTemplateTreeIfExists(
      path.join(tmpDir, "no-dir"),
      destDir,
      mockEngine,
    );

    expect(result).toBeNull();
  });
});

describe("replacePlaceholdersInDir", () => {
  it("replacePlaceholdersInDir_replacesMdFilesRecursively", () => {
    fs.mkdirSync(path.join(srcDir, "inner"), { recursive: true });
    fs.writeFileSync(
      path.join(srcDir, "a.md"),
      "{project_name}",
    );
    fs.writeFileSync(
      path.join(srcDir, "inner", "b.md"),
      "{project_name}",
    );

    replacePlaceholdersInDir(srcDir, mockEngine);

    expect(fs.readFileSync(path.join(srcDir, "a.md"), "utf-8")).toBe(
      "my-app",
    );
    expect(
      fs.readFileSync(path.join(srcDir, "inner", "b.md"), "utf-8"),
    ).toBe("my-app");
  });

  it("replacePlaceholdersInDir_ignoresNonMd_leavesUntouched", () => {
    fs.writeFileSync(
      path.join(srcDir, "script.sh"),
      "{project_name}",
    );

    replacePlaceholdersInDir(srcDir, mockEngine);

    expect(
      fs.readFileSync(path.join(srcDir, "script.sh"), "utf-8"),
    ).toBe("{project_name}");
  });
});
