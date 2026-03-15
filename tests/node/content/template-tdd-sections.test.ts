import { describe, it, expect } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

const STORY_TEMPLATE_PATH = path.resolve(
  __dirname,
  "../../..",
  "resources/templates/_TEMPLATE-STORY.md",
);
const EPIC_TEMPLATE_PATH = path.resolve(
  __dirname,
  "../../..",
  "resources/templates/_TEMPLATE-EPIC.md",
);
const storyContent = fs.readFileSync(STORY_TEMPLATE_PATH, "utf-8");
const epicContent = fs.readFileSync(EPIC_TEMPLATE_PATH, "utf-8");

// ---------------------------------------------------------------------------
// Story Template — Mandatory Scenario Categories Checklist
// ---------------------------------------------------------------------------

describe("Story template — TDD mandatory scenario categories", () => {
  it.each([
    ["Degenerate cases"],
    ["Happy path"],
    ["Error paths"],
    ["Boundary values"],
  ])("storyTemplate_containsCategory_%s", (category) => {
    expect(storyContent).toContain(category);
  });

  it("storyTemplate_categoriesAreChecklist_usesCheckboxSyntax", () => {
    const categories = [
      "Degenerate cases",
      "Happy path",
      "Error paths",
      "Boundary values",
    ];
    for (const cat of categories) {
      const checkboxPattern = new RegExp(`-\\s*\\[\\s*\\]\\s*.*${cat}`);
      expect(storyContent).toMatch(checkboxPattern);
    }
  });

  it("storyTemplate_boundaryValues_specifiesMinMaxPastMax", () => {
    expect(storyContent).toMatch(/at-min.*at-max.*past-max/);
  });
});

// ---------------------------------------------------------------------------
// Story Template — TPP Ordering Note
// ---------------------------------------------------------------------------

describe("Story template — TPP ordering note", () => {
  it("storyTemplate_containsTPPReference_transformationPriorityPremise", () => {
    expect(storyContent).toMatch(/Transformation Priority Premise|TPP/);
  });

  it("storyTemplate_containsOrderingGuidance_simpleToComplex", () => {
    expect(storyContent).toMatch(
      /degenerate.*unconditional.*condition|simples.*complexo|simple.*complex/i,
    );
  });
});

// ---------------------------------------------------------------------------
// Story Template — TDD Implementation Notes (Double-Loop)
// ---------------------------------------------------------------------------

describe("Story template — TDD Implementation Notes", () => {
  it("storyTemplate_containsDoubleLoopTDDReference", () => {
    expect(storyContent).toMatch(/Double-Loop TDD|double.loop/i);
  });

  it("storyTemplate_firstScenarioIsAcceptanceTest", () => {
    expect(storyContent).toMatch(/primeiro cen[áa]rio.*acceptance test|first scenario.*acceptance test/i);
  });

  it("storyTemplate_unitTestsGuidedByInnerLoop", () => {
    expect(storyContent).toMatch(/unit test|loop interno|inner loop/i);
  });
});

// ---------------------------------------------------------------------------
// Story Template — TDD Section Structure
// ---------------------------------------------------------------------------

describe("Story template — TDD section structure", () => {
  it("storyTemplate_tddSectionExists_hasHeading", () => {
    expect(storyContent).toMatch(/###?\s+.*TDD|###?\s+.*Scenario.*Ordering|###?\s+.*Mandatory.*Scenario/i);
  });

  it("storyTemplate_tddSectionAppearsAfterGherkin", () => {
    const gherkinIdx = storyContent.indexOf("Critérios de Aceite");
    const tddIdx = storyContent.search(/Scenario Ordering|Mandatory Scenario|TDD Implementation|TDD Scenarios/i);
    expect(gherkinIdx).toBeGreaterThan(-1);
    expect(tddIdx).toBeGreaterThan(-1);
    expect(tddIdx).toBeGreaterThan(gherkinIdx);
  });

  it("storyTemplate_tddSectionAppearsBeforeSubtasks", () => {
    const tddIdx = storyContent.search(/Scenario Ordering|Mandatory Scenario|TDD Implementation|TDD Scenarios/i);
    const subtasksIdx = storyContent.indexOf("Sub-tarefas");
    expect(tddIdx).toBeGreaterThan(-1);
    expect(subtasksIdx).toBeGreaterThan(-1);
    expect(tddIdx).toBeLessThan(subtasksIdx);
  });
});

// ---------------------------------------------------------------------------
// Epic Template — TDD Compliance in DoD
// ---------------------------------------------------------------------------

describe("Epic template — TDD Compliance in DoD", () => {
  it("epicTemplate_containsTDDComplianceItem", () => {
    expect(epicContent).toMatch(/TDD Compliance/i);
  });

  it("epicTemplate_tddCompliance_mentionsTestFirst", () => {
    expect(epicContent).toMatch(/test-first|test first/i);
  });

  it("epicTemplate_tddCompliance_mentionsRefactoring", () => {
    expect(epicContent).toMatch(/refactor/i);
  });

  it("epicTemplate_tddCompliance_mentionsTPP", () => {
    expect(epicContent).toMatch(/TPP|Transformation Priority Premise|simples.*complexo|simple.*complex/i);
  });
});

// ---------------------------------------------------------------------------
// Epic Template — Double-Loop TDD in DoD
// ---------------------------------------------------------------------------

describe("Epic template — Double-Loop TDD in DoD", () => {
  it("epicTemplate_containsDoubleLoopTDDItem", () => {
    expect(epicContent).toMatch(/Double-Loop TDD|double.loop/i);
  });

  it("epicTemplate_doubleLoop_mentionsAcceptanceTests", () => {
    expect(epicContent).toMatch(/[Aa]cceptance test/);
  });

  it("epicTemplate_doubleLoop_mentionsUnitTests", () => {
    expect(epicContent).toMatch(/[Uu]nit test/);
  });
});

// ---------------------------------------------------------------------------
// Story Template — Backward Compatibility (all 8 original sections)
// ---------------------------------------------------------------------------

describe("Story template — backward compatibility", () => {
  it.each([
    ["## 1. Dependências"],
    ["## 2. Regras Transversais Aplicáveis"],
    ["## 3. Descrição"],
    ["## 4. Definições de Qualidade Locais"],
    ["## 5. Contratos de Dados"],
    ["## 6. Diagramas"],
    ["## 7. Critérios de Aceite"],
    ["## 8. Sub-tarefas"],
  ])("storyTemplate_preservesOriginalSection_%s", (heading) => {
    expect(storyContent).toContain(heading);
  });

  it("storyTemplate_originalSectionCount_atLeast8", () => {
    const h2Sections = storyContent.match(/^## \d+\./gm) || [];
    expect(h2Sections.length).toBeGreaterThanOrEqual(8);
  });
});

// ---------------------------------------------------------------------------
// Epic Template — Backward Compatibility (all 5 original sections)
// ---------------------------------------------------------------------------

describe("Epic template — backward compatibility", () => {
  it.each([
    ["## 1. Visão Geral"],
    ["## 2. Anexos e Referências"],
    ["## 3. Definições de Qualidade Globais"],
    ["## 4. Regras de Negócio Transversais"],
    ["## 5. Índice de Histórias"],
  ])("epicTemplate_preservesOriginalSection_%s", (heading) => {
    expect(epicContent).toContain(heading);
  });

  it("epicTemplate_originalSectionCount_atLeast5", () => {
    const h2Sections = epicContent.match(/^## \d+\./gm) || [];
    expect(h2Sections.length).toBeGreaterThanOrEqual(5);
  });
});

// ---------------------------------------------------------------------------
// Structure Validation — Valid Markdown
// ---------------------------------------------------------------------------

describe("Structure validation", () => {
  it("storyTemplate_allHeadingsUseValidMarkdownSyntax", () => {
    const lines = storyContent.split("\n");
    for (const line of lines) {
      if (line.match(/^#{1,6}\s/)) {
        expect(line).toMatch(/^#{1,6}\s+\S/);
      }
    }
  });

  it("epicTemplate_allHeadingsUseValidMarkdownSyntax", () => {
    const lines = epicContent.split("\n");
    for (const line of lines) {
      if (line.match(/^#{1,6}\s/)) {
        expect(line).toMatch(/^#{1,6}\s+\S/);
      }
    }
  });
});
