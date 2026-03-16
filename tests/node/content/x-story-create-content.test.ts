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

const DIAGRAM_REQUIREMENT_MATRIX_ROWS = [
  "Request",
  "Event-driven",
  "Infrastructure",
  "Documentation",
  "Complex business logic",
  "Refactoring",
];

const DIAGRAM_TYPES = [
  "Sequence",
  "Deployment",
  "Activity",
];

const DIAGRAM_OBLIGATION_LEVELS = [
  "MANDATORY",
  "Recommended",
  "Not required",
];

const SEQUENCE_DIAGRAM_PARTICIPANTS = [
  "Inbound",
  "Application",
  "Domain",
  "Outbound",
];

const DIAGRAM_CHECKLIST_ITEMS = [
  "real component names",
  "error path",
  "architecture layers",
  "data transformations",
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

    describe("diagram requirement matrix", () => {
      it("containsDiagramRequirementMatrix_section6_hasHeading", () => {
        expect(claudeSource).toContain("Diagram Requirement Matrix");
      });

      it.each(
        DIAGRAM_REQUIREMENT_MATRIX_ROWS.map((row) => [row]),
      )(
        "containsMatrixRow_%s_inRequirementMatrix",
        (row) => {
          expect(claudeSource).toContain(row);
        },
      );

      it.each(
        DIAGRAM_TYPES.map((type) => [type]),
      )(
        "containsDiagramType_%s_inRequirementMatrix",
        (type) => {
          expect(claudeSource).toContain(type);
        },
      );

      it.each(
        DIAGRAM_OBLIGATION_LEVELS.map((level) => [level]),
      )(
        "containsObligationLevel_%s_inRequirementMatrix",
        (level) => {
          expect(claudeSource).toContain(level);
        },
      );

      it("matrixMandatesSequenceDiagram_requestResponseStory_mandatory", () => {
        const matrixStart = claudeSource.indexOf(
          "Diagram Requirement Matrix",
        );
        const matrixSection = claudeSource.slice(matrixStart, matrixStart + 800);
        const requestRow = matrixSection.split("\n").find(
          (line) => line.includes("Request") && line.includes("Response"),
        );
        expect(requestRow).toBeDefined();
        expect(requestRow).toContain("MANDATORY");
      });

      it("matrixMandatesDeploymentDiagram_infrastructureStory_mandatory", () => {
        const matrixStart = claudeSource.indexOf(
          "Diagram Requirement Matrix",
        );
        const matrixSection = claudeSource.slice(matrixStart, matrixStart + 800);
        const infraRow = matrixSection.split("\n").find(
          (line) => line.includes("Infrastructure"),
        );
        expect(infraRow).toBeDefined();
        expect(infraRow).toContain("MANDATORY");
      });

      it("matrixAllowsNoDiagram_documentationStory_notRequired", () => {
        const matrixStart = claudeSource.indexOf(
          "Diagram Requirement Matrix",
        );
        const matrixSection = claudeSource.slice(matrixStart, matrixStart + 800);
        const docRow = matrixSection.split("\n").find(
          (line) => line.includes("Documentation"),
        );
        expect(docRow).toBeDefined();
        expect(docRow).toContain("Not required");
      });
    });

    describe("inter-layer sequence diagram template", () => {
      it("containsMermaidSequenceDiagram_section6_hasSequenceDiagramBlock", () => {
        expect(claudeSource).toContain("sequenceDiagram");
      });

      it.each(
        SEQUENCE_DIAGRAM_PARTICIPANTS.map((p) => [p]),
      )(
        "containsParticipant_%s_inSequenceDiagram",
        (participant) => {
          expect(claudeSource).toContain(participant);
        },
      );

      it("containsAltBlock_sequenceDiagram_hasErrorScenario", () => {
        const mermaidStart = claudeSource.indexOf("sequenceDiagram");
        const mermaidSection = claudeSource.slice(mermaidStart, mermaidStart + 1200);
        expect(mermaidSection).toContain("alt");
      });

      it("sequenceDiagramShowsFlow_triggerToResponse_completeInteraction", () => {
        const mermaidStart = claudeSource.indexOf("sequenceDiagram");
        const mermaidSection = claudeSource.slice(mermaidStart, mermaidStart + 1200);
        expect(mermaidSection).toContain("Request");
        expect(mermaidSection).toContain("business rules");
        expect(mermaidSection).toContain("Persist");
        expect(mermaidSection).toContain("Response");
      });
    });

    describe("diagram validation checklist", () => {
      it("containsDiagramValidationChecklist_section6_hasChecklistHeading", () => {
        expect(claudeSource).toContain("Diagram Validation Checklist");
      });

      it.each(
        DIAGRAM_CHECKLIST_ITEMS.map((item) => [item]),
      )(
        "containsChecklistItem_%s_inValidationChecklist",
        (item) => {
          expect(claudeSource).toContain(item);
        },
      );

      it("checklistHasMinimumItems_validationChecklist_atLeastFourItems", () => {
        const checklistStart = claudeSource.indexOf(
          "Diagram Validation Checklist",
        );
        const checklistSection = claudeSource.slice(
          checklistStart,
          checklistStart + 600,
        );
        const checkboxCount = (checklistSection.match(/- \[ \]/g) || []).length;
        expect(checkboxCount).toBeGreaterThanOrEqual(4);
      });
    });

    describe("backward compatibility", () => {
      it("preservesExistingSection6Content_diagramSection_originalContentIntact", () => {
        expect(claudeSource).toContain(
          "Create Mermaid sequence diagrams showing the complete flow",
        );
        expect(claudeSource).toContain("The trigger (client request");
        expect(claudeSource).toContain("Error paths (at least one error scenario)");
      });
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

    describe("diagram requirement matrix", () => {
      it("containsDiagramRequirementMatrix_section6_hasHeading", () => {
        expect(githubSource).toContain("Diagram Requirement Matrix");
      });

      it.each(
        DIAGRAM_REQUIREMENT_MATRIX_ROWS.map((row) => [row]),
      )(
        "containsMatrixRow_%s_inRequirementMatrix",
        (row) => {
          expect(githubSource).toContain(row);
        },
      );

      it.each(
        DIAGRAM_TYPES.map((type) => [type]),
      )(
        "containsDiagramType_%s_inRequirementMatrix",
        (type) => {
          expect(githubSource).toContain(type);
        },
      );

      it.each(
        DIAGRAM_OBLIGATION_LEVELS.map((level) => [level]),
      )(
        "containsObligationLevel_%s_inRequirementMatrix",
        (level) => {
          expect(githubSource).toContain(level);
        },
      );

      it("matrixMandatesSequenceDiagram_requestResponseStory_mandatory", () => {
        const matrixStart = githubSource.indexOf(
          "Diagram Requirement Matrix",
        );
        const matrixSection = githubSource.slice(matrixStart, matrixStart + 800);
        const requestRow = matrixSection.split("\n").find(
          (line) => line.includes("Request") && line.includes("Response"),
        );
        expect(requestRow).toBeDefined();
        expect(requestRow).toContain("MANDATORY");
      });

      it("matrixMandatesDeploymentDiagram_infrastructureStory_mandatory", () => {
        const matrixStart = githubSource.indexOf(
          "Diagram Requirement Matrix",
        );
        const matrixSection = githubSource.slice(matrixStart, matrixStart + 800);
        const infraRow = matrixSection.split("\n").find(
          (line) => line.includes("Infrastructure"),
        );
        expect(infraRow).toBeDefined();
        expect(infraRow).toContain("MANDATORY");
      });

      it("matrixAllowsNoDiagram_documentationStory_notRequired", () => {
        const matrixStart = githubSource.indexOf(
          "Diagram Requirement Matrix",
        );
        const matrixSection = githubSource.slice(matrixStart, matrixStart + 800);
        const docRow = matrixSection.split("\n").find(
          (line) => line.includes("Documentation"),
        );
        expect(docRow).toBeDefined();
        expect(docRow).toContain("Not required");
      });
    });

    describe("inter-layer sequence diagram template", () => {
      it("containsMermaidSequenceDiagram_section6_hasSequenceDiagramBlock", () => {
        expect(githubSource).toContain("sequenceDiagram");
      });

      it.each(
        SEQUENCE_DIAGRAM_PARTICIPANTS.map((p) => [p]),
      )(
        "containsParticipant_%s_inSequenceDiagram",
        (participant) => {
          expect(githubSource).toContain(participant);
        },
      );

      it("containsAltBlock_sequenceDiagram_hasErrorScenario", () => {
        const mermaidStart = githubSource.indexOf("sequenceDiagram");
        const mermaidSection = githubSource.slice(mermaidStart, mermaidStart + 1200);
        expect(mermaidSection).toContain("alt");
      });

      it("sequenceDiagramShowsFlow_triggerToResponse_completeInteraction", () => {
        const mermaidStart = githubSource.indexOf("sequenceDiagram");
        const mermaidSection = githubSource.slice(mermaidStart, mermaidStart + 1200);
        expect(mermaidSection).toContain("Request");
        expect(mermaidSection).toContain("business rules");
        expect(mermaidSection).toContain("Persist");
        expect(mermaidSection).toContain("Response");
      });
    });

    describe("diagram validation checklist", () => {
      it("containsDiagramValidationChecklist_section6_hasChecklistHeading", () => {
        expect(githubSource).toContain("Diagram Validation Checklist");
      });

      it.each(
        DIAGRAM_CHECKLIST_ITEMS.map((item) => [item]),
      )(
        "containsChecklistItem_%s_inValidationChecklist",
        (item) => {
          expect(githubSource).toContain(item);
        },
      );

      it("checklistHasMinimumItems_validationChecklist_atLeastFourItems", () => {
        const checklistStart = githubSource.indexOf(
          "Diagram Validation Checklist",
        );
        const checklistSection = githubSource.slice(
          checklistStart,
          checklistStart + 600,
        );
        const checkboxCount = (checklistSection.match(/- \[ \]/g) || []).length;
        expect(checkboxCount).toBeGreaterThanOrEqual(4);
      });
    });

    describe("backward compatibility", () => {
      it("preservesExistingSection6Content_diagramSection_originalContentIntact", () => {
        expect(githubSource).toContain(
          "Create Mermaid sequence diagrams showing the complete flow",
        );
        expect(githubSource).toContain("The trigger (client request");
        expect(githubSource).toContain("Error paths (at least one error scenario)");
      });
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

    it("bothContainDiagramRequirementMatrix_dualCopy_sameContent", () => {
      expect(claudeSource).toContain("Diagram Requirement Matrix");
      expect(githubSource).toContain("Diagram Requirement Matrix");
    });

    it("bothContainSequenceDiagramTemplate_dualCopy_sameContent", () => {
      expect(claudeSource).toContain("sequenceDiagram");
      expect(githubSource).toContain("sequenceDiagram");
    });

    it("bothContainDiagramChecklist_dualCopy_sameContent", () => {
      expect(claudeSource).toContain("Diagram Validation Checklist");
      expect(githubSource).toContain("Diagram Validation Checklist");
    });

    it.each(
      SEQUENCE_DIAGRAM_PARTICIPANTS.map((p) => [p]),
    )(
      "bothContainParticipant_%s_dualCopy_sameParticipants",
      (participant) => {
        expect(claudeSource).toContain(participant);
        expect(githubSource).toContain(participant);
      },
    );

    it.each(
      DIAGRAM_REQUIREMENT_MATRIX_ROWS.map((row) => [row]),
    )(
      "bothContainMatrixRow_%s_dualCopy_sameStoryTypes",
      (row) => {
        expect(claudeSource).toContain(row);
        expect(githubSource).toContain(row);
      },
    );
  });
});
