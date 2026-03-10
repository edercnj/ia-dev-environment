import { describe, it, expect, beforeEach, afterEach } from "vitest";
import { mkdirSync, rmSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { tmpdir } from "node:os";
import {
  UNIVERSAL_PATTERNS,
  ARCHITECTURE_PATTERNS,
  EVENT_DRIVEN_PATTERNS,
  selectPatterns,
  selectPatternFiles,
} from "../../../src/domain/pattern-mapping.js";
import {
  ProjectConfig,
  ProjectIdentity,
  ArchitectureConfig,
  InterfaceConfig,
  LanguageConfig,
  FrameworkConfig,
} from "../../../src/models.js";

function buildConfig(style: string, eventDriven: boolean): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity("test", "test"),
    new ArchitectureConfig(style, false, eventDriven),
    [new InterfaceConfig("rest")],
    new LanguageConfig("java", "21"),
    new FrameworkConfig("quarkus", "3.0", "maven"),
  );
}

describe("constants", () => {
  it("universalPatterns_containsArchitecturalAndData", () => {
    expect(UNIVERSAL_PATTERNS).toEqual(["architectural", "data"]);
  });

  it("architecturePatterns_has4Styles", () => {
    expect(Object.keys(ARCHITECTURE_PATTERNS)).toHaveLength(4);
  });

  it("eventDrivenPatterns_has4Patterns", () => {
    expect(EVENT_DRIVEN_PATTERNS).toHaveLength(4);
    expect(EVENT_DRIVEN_PATTERNS).toContain("saga-pattern");
    expect(EVENT_DRIVEN_PATTERNS).toContain("dead-letter-queue");
  });
});

describe("selectPatterns", () => {
  it("microservice_includesUniversalAndStylePatterns", () => {
    const config = buildConfig("microservice", false);
    const result = selectPatterns(config);
    expect(result).toContain("architectural");
    expect(result).toContain("data");
    expect(result).toContain("microservice");
    expect(result).toContain("resilience");
    expect(result).toContain("integration");
  });

  it("microservice_withEventDriven_includesEventPatterns", () => {
    const config = buildConfig("microservice", true);
    const result = selectPatterns(config);
    expect(result).toContain("saga-pattern");
    expect(result).toContain("outbox-pattern");
    expect(result).toContain("event-sourcing");
    expect(result).toContain("dead-letter-queue");
  });

  it("microservice_resultIsSortedAndDeduplicated", () => {
    const config = buildConfig("microservice", true);
    const result = selectPatterns(config);
    const sorted = [...result].sort();
    expect(result).toEqual(sorted);
    expect(new Set(result).size).toBe(result.length);
  });

  it("library_returnsOnlyUniversalPatterns", () => {
    const config = buildConfig("library", false);
    const result = selectPatterns(config);
    expect(result).toEqual(["architectural", "data"]);
  });

  it("unknownStyle_returnsEmptyList", () => {
    const config = buildConfig("unknown-style", false);
    const result = selectPatterns(config);
    expect(result).toEqual([]);
  });

  it("hexagonal_includesIntegration", () => {
    const config = buildConfig("hexagonal", false);
    const result = selectPatterns(config);
    expect(result).toContain("integration");
    expect(result).not.toContain("microservice");
  });
});

describe("selectPatternFiles", () => {
  let tmpDir: string;

  beforeEach(() => {
    tmpDir = join(tmpdir(), `pattern-test-${Date.now()}`);
    mkdirSync(join(tmpDir, "patterns", "architectural"), { recursive: true });
    writeFileSync(join(tmpDir, "patterns", "architectural", "clean-arch.md"), "");
    writeFileSync(join(tmpDir, "patterns", "architectural", "hexagonal.md"), "");
  });

  afterEach(() => {
    rmSync(tmpDir, { recursive: true, force: true });
  });

  it("existingCategory_returnsMarkdownFiles", () => {
    const files = selectPatternFiles(tmpDir, ["architectural"]);
    expect(files).toHaveLength(2);
    expect(files[0]).toContain("clean-arch.md");
    expect(files[1]).toContain("hexagonal.md");
  });

  it("missingCategory_skipsWithoutError", () => {
    const files = selectPatternFiles(tmpDir, ["nonexistent"]);
    expect(files).toEqual([]);
  });

  it("mixedCategories_returnsOnlyExisting", () => {
    const files = selectPatternFiles(tmpDir, ["architectural", "nonexistent"]);
    expect(files).toHaveLength(2);
  });
});
