import { describe, it, expect } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

const CLAUDE_SOURCE_PATH = path.resolve(
  __dirname,
  "../../..",
  "resources/skills-templates/core/x-story-create/SKILL.md",
);

const GITHUB_SOURCE_PATH = path.resolve(
  __dirname,
  "../../..",
  "resources/github-skills-templates/story/x-story-create.md",
);

const claudeSource = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
const githubSource = fs.readFileSync(GITHUB_SOURCE_PATH, "utf-8");

const MANDATORY_CATEGORIES = [
  "Degenerate cases",
  "Happy path",
  "Error paths",
  "Boundary values",
  "Complex edge cases",
];

const BOUNDARY_TRIPLET_TERMS = [
  "At-minimum",
  "At-maximum",
  "Past-maximum",
];

const NEW_COMMON_MISTAKES = [
  "Missing degenerate cases",
  "Boundary values without triplet",
  "Happy-path-first ordering",
  "Under-counting scenarios",
];

describe("x-story-create content validation", () => {
  describe("Claude source template", () => {
    it("containsRule13Reference_prerequisiteSection_referencesStoryDecomposition", () => {
      expect(claudeSource).toContain("story-planning/references/story-decomposition.md");
    });

    it("containsMandatoryCategoriesSection_gherkinSection_hasMandatoryScenarioCategories", () => {
      expect(claudeSource).toContain(
        "Mandatory scenario categories",
      );
    });

    it.each(
      MANDATORY_CATEGORIES.map((cat) => [cat]),
    )(
      "containsCategory_%s_inGherkinSection",
      (category) => {
        expect(claudeSource).toContain(category);
      },
    );

    it("containsTPPOrdering_degenerateCasesBeforeHappyPath_correctOrder", () => {
      const degenerateIndex = claudeSource.indexOf(
        "Degenerate cases",
      );
      const happyPathIndex = claudeSource.indexOf("Happy path");
      expect(degenerateIndex).toBeGreaterThan(-1);
      expect(happyPathIndex).toBeGreaterThan(-1);
      expect(degenerateIndex).toBeLessThan(happyPathIndex);
    });

    it("containsMinimumFloor_scenarioCount_requiresFourScenarios", () => {
      expect(claudeSource).toContain(
        "4 scenarios per story",
      );
    });

    it.each(
      BOUNDARY_TRIPLET_TERMS.map((term) => [term]),
    )(
      "containsBoundaryTripletTerm_%s_inTripletPattern",
      (term) => {
        expect(claudeSource).toContain(term);
      },
    );

    it("containsBoundaryTripletSection_gherkinSection_hasTripletPatternHeading", () => {
      expect(claudeSource).toContain(
        "Boundary value triplet pattern",
      );
    });

    it("containsUpdatedSizingHeuristic_tooSmallSection_usesFourScenarios", () => {
      expect(claudeSource).toContain(
        "Less than 4 Gherkin scenarios",
      );
    });

    it.each(
      NEW_COMMON_MISTAKES.map((mistake) => [mistake]),
    )(
      "containsCommonMistake_%s_inCommonMistakesSection",
      (mistake) => {
        expect(claudeSource).toContain(mistake);
      },
    );

    it("containsTPPOrderingRationale_gherkinSection_explainsTPPOrdering", () => {
      expect(claudeSource).toContain("TPP ordering rationale");
    });
  });

  describe("GitHub source template", () => {
    it("containsRule13Reference_prerequisiteSection_referencesStoryDecomposition", () => {
      expect(githubSource).toContain("story-planning/SKILL.md");
    });

    it("containsMandatoryCategoriesSection_gherkinSection_hasMandatoryScenarioCategories", () => {
      expect(githubSource).toContain(
        "Mandatory scenario categories",
      );
    });

    it.each(
      MANDATORY_CATEGORIES.map((cat) => [cat]),
    )(
      "containsCategory_%s_inGherkinSection",
      (category) => {
        expect(githubSource).toContain(category);
      },
    );

    it("containsTPPOrdering_degenerateCasesBeforeHappyPath_correctOrder", () => {
      const degenerateIndex = githubSource.indexOf(
        "Degenerate cases",
      );
      const happyPathIndex = githubSource.indexOf("Happy path");
      expect(degenerateIndex).toBeGreaterThan(-1);
      expect(happyPathIndex).toBeGreaterThan(-1);
      expect(degenerateIndex).toBeLessThan(happyPathIndex);
    });

    it("containsMinimumFloor_scenarioCount_requiresFourScenarios", () => {
      expect(githubSource).toContain(
        "4 scenarios per story",
      );
    });

    it.each(
      BOUNDARY_TRIPLET_TERMS.map((term) => [term]),
    )(
      "containsBoundaryTripletTerm_%s_inTripletPattern",
      (term) => {
        expect(githubSource).toContain(term);
      },
    );

    it("containsBoundaryTripletSection_gherkinSection_hasTripletPatternHeading", () => {
      expect(githubSource).toContain(
        "Boundary value triplet pattern",
      );
    });

    it("containsUpdatedSizingHeuristic_tooSmallSection_usesFourScenarios", () => {
      expect(githubSource).toContain(
        "Less than 4 Gherkin scenarios",
      );
    });

    it.each(
      NEW_COMMON_MISTAKES.map((mistake) => [mistake]),
    )(
      "containsCommonMistake_%s_inCommonMistakesSection",
      (mistake) => {
        expect(githubSource).toContain(mistake);
      },
    );

    it("containsTPPOrderingRationale_gherkinSection_explainsTPPOrdering", () => {
      expect(githubSource).toContain("TPP ordering rationale");
    });
  });

  describe("dual copy consistency (RULE-001)", () => {
    it.each(
      MANDATORY_CATEGORIES.map((cat) => [cat]),
    )(
      "bothContainCategory_%s_sameContent",
      (category) => {
        const claudeHas = claudeSource.includes(category);
        const githubHas = githubSource.includes(category);
        expect(claudeHas).toBe(true);
        expect(githubHas).toBe(true);
      },
    );

    it("bothContainTPPOrdering_sameRationale_degenerateCasesBeforeHappyPaths", () => {
      const claudeRationale = claudeSource.includes(
        "Degenerate cases MUST appear before happy paths",
      );
      const githubRationale = githubSource.includes(
        "Degenerate cases MUST appear before happy paths",
      );
      expect(claudeRationale).toBe(true);
      expect(githubRationale).toBe(true);
    });

    it("bothContainMinimumFloor_sameThreshold_fourScenarios", () => {
      const claudeFloor = claudeSource.includes(
        "4 scenarios per story",
      );
      const githubFloor = githubSource.includes(
        "4 scenarios per story",
      );
      expect(claudeFloor).toBe(true);
      expect(githubFloor).toBe(true);
    });

    it.each(
      BOUNDARY_TRIPLET_TERMS.map((term) => [term]),
    )(
      "bothContainBoundaryTripletTerm_%s_samePattern",
      (term) => {
        expect(claudeSource).toContain(term);
        expect(githubSource).toContain(term);
      },
    );

    it("bothContainSameCommonMistakesCount_newMistakes_fourEntries", () => {
      const claudeCount = NEW_COMMON_MISTAKES.filter(
        (m) => claudeSource.includes(m),
      ).length;
      const githubCount = NEW_COMMON_MISTAKES.filter(
        (m) => githubSource.includes(m),
      ).length;
      expect(claudeCount).toBe(NEW_COMMON_MISTAKES.length);
      expect(githubCount).toBe(NEW_COMMON_MISTAKES.length);
      expect(claudeCount).toBe(githubCount);
    });
  });
});
