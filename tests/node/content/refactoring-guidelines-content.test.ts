import { describe, it, expect } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { CORE_TO_KP_MAPPING } from "../../../src/domain/core-kp-routing.js";

const RESOURCE_PATH = path.resolve(
  __dirname,
  "../../..",
  "resources/core/14-refactoring-guidelines.md",
);
const content = fs.readFileSync(RESOURCE_PATH, "utf-8");

describe("refactoring-guidelines content validation", () => {
  describe("required H2 sections", () => {
    it.each([
      ["## Refactoring Triggers"],
      ["## Prioritized Techniques"],
      ["## Safety Rules"],
      ["## Refactoring Anti-Patterns (FORBIDDEN)"],
    ])("content_containsH2Section_%s", (heading) => {
      expect(content).toContain(heading);
    });
  });

  describe("required H3 sections", () => {
    it.each([
      ["### When to Extract Method"],
      ["### When to Extract Class"],
      ["### When to Inline"],
      ["### When to Rename"],
    ])("content_containsH3Section_%s", (heading) => {
      expect(content).toContain(heading);
    });
  });

  describe("trigger keywords", () => {
    it.each([
      ["25 lines"],
      ["250 lines"],
    ])("refactoringTriggers_contains_%s", (keyword) => {
      expect(content).toContain(keyword);
    });
  });

  it("prioritizedTechniques_firstTechnique_isExtractMethod", () => {
    const techniquesSection = content.split("## Prioritized Techniques")[1]!;
    const tableRows = techniquesSection
      .split("\n")
      .filter((line) => line.startsWith("| ") && !line.startsWith("| #") && !line.startsWith("|--"));
    const firstRow = tableRows[0]!;
    expect(firstRow).toContain("Extract Method");
    expect(firstRow).toContain("| 1 |");
  });

  describe("safety rules key phrases", () => {
    it.each([
      ["NEVER add behavior"],
      ["GREEN before starting"],
      ["GREEN after each step"],
      ["UNDO if any test breaks"],
      ["Small, safe steps"],
    ])("safetyRules_containsPhrase_%s", (phrase) => {
      const safetySection = content.split("## Safety Rules")[1]!;
      expect(safetySection).toContain(phrase);
    });
  });
});

describe("CORE_TO_KP_MAPPING destFile uniqueness", () => {
  it("allDestFiles_areUnique_noDuplicates", () => {
    const destFiles = CORE_TO_KP_MAPPING.map((r) => r.destFile);
    const uniqueDestFiles = new Set(destFiles);
    expect(uniqueDestFiles.size).toBe(destFiles.length);
  });
});

describe("refactoring-guidelines route", () => {
  it("route_kpName_isCodingStandards", () => {
    const route = CORE_TO_KP_MAPPING.find(
      (r) => r.sourceFile === "14-refactoring-guidelines.md",
    );
    expect(route).toBeDefined();
    expect(route!.kpName).toBe("coding-standards");
  });
});
