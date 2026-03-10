import {
  describe,
  it,
  expect,
  beforeEach,
  afterEach,
} from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import * as os from "node:os";
import {
  consolidateFiles,
  consolidateFrameworkRules,
  sanitizeFilenameSegment,
  GENERATED_HEADER,
  CORE_PATTERNS,
  DATA_PATTERNS,
  OPS_PATTERNS,
} from "../../../src/assembler/consolidator.js";

let tmpDir: string;

beforeEach(() => {
  tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), "consolidator-"));
});

afterEach(() => {
  fs.rmSync(tmpDir, { recursive: true, force: true });
});

function writeFile(name: string, content: string): string {
  const filePath = path.join(tmpDir, name);
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, content);
  return filePath;
}

describe("consolidateFiles", () => {
  it("consolidateFiles_multipleSources_mergesWithSeparators", () => {
    const src1 = writeFile("a.md", "Alpha");
    const src2 = writeFile("b.md", "Beta");
    const output = path.join(tmpDir, "out", "merged.md");

    consolidateFiles(output, [src1, src2]);

    const content = fs.readFileSync(output, "utf-8");
    expect(content).toContain(GENERATED_HEADER);
    expect(content).toContain("---");
    expect(content).toContain("Alpha");
    expect(content).toContain("Beta");
  });

  it("consolidateFiles_skipsMissingFiles_onlyIncludesExisting", () => {
    const src1 = writeFile("exists.md", "Present");
    const missing = path.join(tmpDir, "missing.md");
    const output = path.join(tmpDir, "out.md");

    consolidateFiles(output, [src1, missing]);

    const content = fs.readFileSync(output, "utf-8");
    expect(content).toContain("Present");
  });

  it("consolidateFiles_allMissing_doesNotCreateOutput", () => {
    const output = path.join(tmpDir, "out.md");

    consolidateFiles(output, [
      path.join(tmpDir, "x.md"),
      path.join(tmpDir, "y.md"),
    ]);

    expect(fs.existsSync(output)).toBe(false);
  });

  it("consolidateFiles_createsParentDirectories_ifMissing", () => {
    const src = writeFile("src.md", "Content");
    const output = path.join(tmpDir, "deep", "nested", "out.md");

    consolidateFiles(output, [src]);

    expect(fs.existsSync(output)).toBe(true);
  });

  it("consolidateFiles_singleSource_includesHeaderAndSeparator", () => {
    const src = writeFile("only.md", "Solo");
    const output = path.join(tmpDir, "out.md");

    consolidateFiles(output, [src]);

    const content = fs.readFileSync(output, "utf-8");
    expect(content.startsWith(GENERATED_HEADER)).toBe(true);
    expect(content).toContain("---");
    expect(content).toContain("Solo");
  });
});

describe("consolidateFrameworkRules", () => {
  it("consolidateFrameworkRules_classifiesIntoThreeGroups", () => {
    const sourceDir = path.join(tmpDir, "framework");
    const rulesDir = path.join(tmpDir, "rules");
    fs.mkdirSync(sourceDir, { recursive: true });
    fs.mkdirSync(rulesDir, { recursive: true });

    fs.writeFileSync(
      path.join(sourceDir, "quarkus-cdi.md"),
      "CDI content",
    );
    fs.writeFileSync(
      path.join(sourceDir, "quarkus-panache.md"),
      "Panache content",
    );
    fs.writeFileSync(
      path.join(sourceDir, "quarkus-testing.md"),
      "Testing content",
    );

    const generated = consolidateFrameworkRules(
      "quarkus",
      rulesDir,
      sourceDir,
    );

    expect(generated).toHaveLength(3);
    expect(generated[0]).toContain("30-quarkus-core.md");
    expect(generated[1]).toContain("31-quarkus-data.md");
    expect(generated[2]).toContain("32-quarkus-operations.md");
  });

  it("consolidateFrameworkRules_missingSourceDir_returnsEmpty", () => {
    const result = consolidateFrameworkRules(
      "spring",
      path.join(tmpDir, "rules"),
      path.join(tmpDir, "no-dir"),
    );
    expect(result).toEqual([]);
  });

  it("consolidateFrameworkRules_noMatchingPatterns_returnsEmpty", () => {
    const sourceDir = path.join(tmpDir, "framework");
    const rulesDir = path.join(tmpDir, "rules");
    fs.mkdirSync(sourceDir, { recursive: true });
    fs.mkdirSync(rulesDir, { recursive: true });
    fs.writeFileSync(
      path.join(sourceDir, "unrelated.md"),
      "No match",
    );

    const result = consolidateFrameworkRules(
      "quarkus",
      rulesDir,
      sourceDir,
    );

    expect(result).toEqual([]);
  });

  it("consolidateFrameworkRules_partialGroups_onlyGeneratesNonEmpty", () => {
    const sourceDir = path.join(tmpDir, "framework");
    const rulesDir = path.join(tmpDir, "rules");
    fs.mkdirSync(sourceDir, { recursive: true });
    fs.mkdirSync(rulesDir, { recursive: true });

    fs.writeFileSync(
      path.join(sourceDir, "quarkus-web.md"),
      "Web content",
    );

    const generated = consolidateFrameworkRules(
      "quarkus",
      rulesDir,
      sourceDir,
    );

    expect(generated).toHaveLength(1);
    expect(generated[0]).toContain("30-quarkus-core.md");
  });
});

describe("pattern constants", () => {
  it("CORE_PATTERNS_includesExpectedValues", () => {
    expect(CORE_PATTERNS).toEqual(
      expect.arrayContaining([
        "-cdi",
        "-di",
        "-config",
        "-web",
        "-resteasy",
        "-middleware",
        "-resilience",
      ]),
    );
  });

  it("DATA_PATTERNS_includesExpectedValues", () => {
    expect(DATA_PATTERNS).toEqual(
      expect.arrayContaining([
        "-panache",
        "-jpa",
        "-prisma",
        "-sqlalchemy",
        "-exposed",
        "-ef",
        "-orm",
        "-database",
      ]),
    );
  });

  it("OPS_PATTERNS_includesExpectedValues", () => {
    expect(OPS_PATTERNS).toEqual(
      expect.arrayContaining([
        "-testing",
        "-observability",
        "-native-build",
        "-infrastructure",
      ]),
    );
  });
});

describe("pattern classification via consolidateFrameworkRules", () => {
  it.each([
    ["-cdi", "30", "core"],
    ["-di", "30", "core"],
    ["-config", "30", "core"],
    ["-web", "30", "core"],
    ["-resteasy", "30", "core"],
    ["-middleware", "30", "core"],
    ["-resilience", "30", "core"],
    ["-panache", "31", "data"],
    ["-jpa", "31", "data"],
    ["-prisma", "31", "data"],
    ["-sqlalchemy", "31", "data"],
    ["-exposed", "31", "data"],
    ["-ef", "31", "data"],
    ["-orm", "31", "data"],
    ["-database", "31", "data"],
    ["-testing", "32", "operations"],
    ["-observability", "32", "operations"],
    ["-native-build", "32", "operations"],
    ["-infrastructure", "32", "operations"],
  ] as const)(
    "file_with_%s_classifiedAs_%s-%s",
    (pattern, prefix, label) => {
      const sourceDir = path.join(tmpDir, "fw-classify");
      const rulesDir = path.join(tmpDir, "rules-classify");
      fs.mkdirSync(sourceDir, { recursive: true });
      fs.mkdirSync(rulesDir, { recursive: true });
      fs.writeFileSync(
        path.join(sourceDir, `quarkus${pattern}.md`),
        `Content for ${pattern}`,
      );

      const generated = consolidateFrameworkRules(
        "quarkus",
        rulesDir,
        sourceDir,
      );

      expect(generated).toHaveLength(1);
      expect(path.basename(generated[0]!)).toBe(
        `${prefix}-quarkus-${label}.md`,
      );
    },
  );
});

describe("sanitizeFilenameSegment", () => {
  it("sanitizeFilenameSegment_cleanInput_returnsUnchanged", () => {
    expect(sanitizeFilenameSegment("quarkus")).toBe("quarkus");
  });

  it("sanitizeFilenameSegment_pathTraversal_stripsSlashesAndDots", () => {
    expect(sanitizeFilenameSegment("../../etc")).toBe("etc");
  });

  it("sanitizeFilenameSegment_forwardSlash_stripped", () => {
    expect(sanitizeFilenameSegment("a/b/c")).toBe("abc");
  });

  it("sanitizeFilenameSegment_backslash_stripped", () => {
    expect(sanitizeFilenameSegment("a\\b\\c")).toBe("abc");
  });

  it("sanitizeFilenameSegment_mixedTraversal_fullyClean", () => {
    expect(sanitizeFilenameSegment("../..\\evil")).toBe("evil");
  });
});

describe("consolidateFrameworkRules path safety", () => {
  it("consolidateFrameworkRules_pathTraversalFramework_sanitized", () => {
    const sourceDir = path.join(tmpDir, "framework-safe");
    const rulesDir = path.join(tmpDir, "rules-safe");
    fs.mkdirSync(sourceDir, { recursive: true });
    fs.mkdirSync(rulesDir, { recursive: true });
    fs.writeFileSync(
      path.join(sourceDir, "evil-cdi.md"),
      "content",
    );

    const generated = consolidateFrameworkRules(
      "../../etc",
      rulesDir,
      sourceDir,
    );

    expect(generated).toHaveLength(1);
    expect(path.basename(generated[0]!)).toBe("30-etc-core.md");
    expect(path.dirname(generated[0]!)).toBe(rulesDir);
  });
});
