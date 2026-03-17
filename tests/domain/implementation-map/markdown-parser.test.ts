import { describe, it, expect } from "vitest";

import {
  extractDependencyMatrix,
  extractPhaseSummary,
} from "../../../src/domain/implementation-map/markdown-parser.js";
import { MapParseError } from "../../../src/domain/implementation-map/types.js";
import { readFixture } from "./helpers.js";

describe("extractDependencyMatrix", () => {
  it("extractDependencyMatrix_emptyContent_returnsEmptyArray", () => {
    const result = extractDependencyMatrix("");
    expect(result).toEqual([]);
  });

  it("extractDependencyMatrix_headerOnlyNoDataRows_returnsEmptyArray", () => {
    const content = readFixture("empty-map.md");
    const result = extractDependencyMatrix(content);
    expect(result).toEqual([]);
  });

  it("extractDependencyMatrix_singleRowNoDeps_returnsSingleRow", () => {
    const content = readFixture("single-story.md");
    const result = extractDependencyMatrix(content);

    expect(result).toHaveLength(1);
    expect(result[0]).toEqual({
      storyId: "story-0042-0001",
      title: "Execution State Schema",
      blockedBy: [],
      blocks: [],
      status: "Pendente",
    });
  });

  it("extractDependencyMatrix_singleRowWithDeps_parsesCommaSeparatedIds", () => {
    const content = [
      "## 1. Matriz de Dependencias",
      "",
      "| Story | Titulo | Blocked By | Blocks | Status |",
      "| :--- | :--- | :--- | :--- | :--- |",
      "| story-0042-0002 | My Story | story-0042-0001 | story-0042-0003, story-0042-0004 | Pendente |",
    ].join("\n");
    const result = extractDependencyMatrix(content);

    expect(result).toHaveLength(1);
    expect(result[0]?.blockedBy).toEqual(["story-0042-0001"]);
    expect(result[0]?.blocks).toEqual([
      "story-0042-0003",
      "story-0042-0004",
    ]);
  });

  it("extractDependencyMatrix_multipleRows_returnsAllRows", () => {
    const content = readFixture("linear-chain.md");
    const result = extractDependencyMatrix(content);

    expect(result).toHaveLength(3);
    expect(result[0]?.storyId).toBe("story-0042-0001");
    expect(result[1]?.storyId).toBe("story-0042-0002");
    expect(result[2]?.storyId).toBe("story-0042-0003");
    expect(result[0]?.blocks).toEqual(["story-0042-0002"]);
    expect(result[1]?.blockedBy).toEqual(["story-0042-0001"]);
    expect(result[1]?.blocks).toEqual(["story-0042-0003"]);
    expect(result[2]?.blockedBy).toEqual(["story-0042-0002"]);
  });

  it("extractDependencyMatrix_extraWhitespace_toleratesFormatting", () => {
    const content = [
      "## 1. Matriz de Dependencias",
      "",
      "|  Story  |  Titulo  |  Blocked By  |  Blocks  |  Status  |",
      "| :--- | :--- | :--- | :--- | :--- |",
      "|  story-0042-0001  |  First Story  |  -  |  story-0042-0002  |  Pendente  |",
      "|  story-0042-0002  |  Second Story  |  story-0042-0001  |  -  |  Pendente  |",
    ].join("\n");
    const result = extractDependencyMatrix(content);

    expect(result).toHaveLength(2);
    expect(result[0]?.storyId).toBe("story-0042-0001");
    expect(result[1]?.blockedBy).toEqual(["story-0042-0001"]);
  });

  it("extractDependencyMatrix_dashSeparatorRow_skipsIt", () => {
    const content = readFixture("single-story.md");
    const result = extractDependencyMatrix(content);

    expect(result).toHaveLength(1);
    expect(result[0]?.storyId).toBe("story-0042-0001");
  });

  it("extractDependencyMatrix_statusVariations_preservesOriginal", () => {
    const content = [
      "## 1. Matriz de Dependencias",
      "",
      "| Story | Titulo | Blocked By | Blocks | Status |",
      "| :--- | :--- | :--- | :--- | :--- |",
      "| story-0042-0001 | S1 | - | - | Pendente |",
      "| story-0042-0002 | S2 | - | - | Concluido |",
      "| story-0042-0003 | S3 | - | - | Em andamento |",
    ].join("\n");
    const result = extractDependencyMatrix(content);

    expect(result).toHaveLength(3);
    expect(result[0]?.status).toBe("Pendente");
    expect(result[1]?.status).toBe("Concluido");
    expect(result[2]?.status).toBe("Em andamento");
  });

  it("extractDependencyMatrix_emDashSeparator_treatedAsNoDeps", () => {
    const content = [
      "## 1. Matriz de Dependencias",
      "",
      "| Story | Titulo | Blocked By | Blocks | Status |",
      "| :--- | :--- | :--- | :--- | :--- |",
      "| story-0042-0001 | S1 | \u2014 | \u2014 | Pendente |",
    ].join("\n");
    const result = extractDependencyMatrix(content);

    expect(result).toHaveLength(1);
    expect(result[0]?.blockedBy).toEqual([]);
    expect(result[0]?.blocks).toEqual([]);
  });
});

describe("extractPhaseSummary", () => {
  it("extractPhaseSummary_emptyContent_returnsEmptyArray", () => {
    const result = extractPhaseSummary("");
    expect(result).toEqual([]);
  });

  it("extractPhaseSummary_singlePhaseRow_returnsSingleRow", () => {
    const content = readFixture("single-story.md");
    const result = extractPhaseSummary(content);

    expect(result).toHaveLength(1);
    expect(result[0]).toEqual({
      phase: 0,
      stories: ["0001"],
      layer: "Foundation",
      parallelism: "1",
      prerequisite: "-",
    });
  });

  it("extractPhaseSummary_multiplePhaseRows_returnsAllPhases", () => {
    const content = readFixture("linear-chain.md");
    const result = extractPhaseSummary(content);

    expect(result).toHaveLength(3);
    expect(result[0]?.phase).toBe(0);
    expect(result[1]?.phase).toBe(1);
    expect(result[2]?.phase).toBe(2);
    expect(result[0]?.stories).toEqual(["0001"]);
    expect(result[1]?.stories).toEqual(["0002"]);
    expect(result[2]?.stories).toEqual(["0003"]);
  });

  it("extractPhaseSummary_commaSeparatedStories_splitsCorrectly", () => {
    const content = readFixture("parallel-roots.md");
    const result = extractPhaseSummary(content);

    expect(result).toHaveLength(2);
    expect(result[0]?.stories).toEqual(["0001", "0002"]);
    expect(result[0]?.parallelism).toBe("2 paralelas");
  });

  it("extractPhaseSummary_nonNumericPhase_skipsRow", () => {
    const content = [
      "## 5. Resumo por Fase",
      "",
      "| Fase | Historias | Camada | Paralelismo | Pre-requisito |",
      "| :--- | :--- | :--- | :--- | :--- |",
      "| N/A | 0001 | Foundation | 1 | - |",
    ].join("\n");
    const result = extractPhaseSummary(content);
    expect(result).toEqual([]);
  });
});

describe("MapParseError", () => {
  it("MapParseError_canBeConstructedWithMessage", () => {
    const error = new MapParseError("bad markdown at line 5");
    expect(error).toBeInstanceOf(Error);
    expect(error.name).toBe("MapParseError");
    expect(error.message).toBe("bad markdown at line 5");
  });
});

describe("extractDependencyMatrix edge cases", () => {
  it("extractDependencyMatrix_rowWithTooFewCells_skipsRow", () => {
    const content = [
      "## 1. Matriz de Dependencias",
      "",
      "| Story | Titulo | Blocked By | Blocks | Status |",
      "| :--- | :--- | :--- | :--- | :--- |",
      "| story-0042-0001 | only two cells |",
      "| story-0042-0002 | Full Row | - | - | Pendente |",
    ].join("\n");
    const result = extractDependencyMatrix(content);
    expect(result).toHaveLength(1);
    expect(result[0]?.storyId).toBe("story-0042-0002");
  });

  it("extractDependencyMatrix_contentWithNoSection1_returnsEmpty", () => {
    const content = [
      "## 3. Some Other Section",
      "",
      "| Story | Titulo | Blocked By | Blocks | Status |",
      "| :--- | :--- | :--- | :--- | :--- |",
      "| story-0042-0001 | S1 | - | - | Pendente |",
    ].join("\n");
    const result = extractDependencyMatrix(content);
    expect(result).toEqual([]);
  });

  it("extractDependencyMatrix_section1StopsAtSection2", () => {
    const content = [
      "## 1. Matriz de Dependencias",
      "",
      "| Story | Titulo | Blocked By | Blocks | Status |",
      "| :--- | :--- | :--- | :--- | :--- |",
      "| story-0042-0001 | S1 | - | - | Pendente |",
      "",
      "## 2. Fases de Implementacao",
      "",
      "| story-0042-0002 | S2 | - | - | Pendente |",
    ].join("\n");
    const result = extractDependencyMatrix(content);
    expect(result).toHaveLength(1);
    expect(result[0]?.storyId).toBe("story-0042-0001");
  });
});

describe("extractPhaseSummary edge cases", () => {
  it("extractPhaseSummary_rowWithTooFewCells_skipsRow", () => {
    const content = [
      "## 5. Resumo por Fase",
      "",
      "| Fase | Historias | Camada | Paralelismo | Pre-requisito |",
      "| :--- | :--- | :--- | :--- | :--- |",
      "| 0 | too few |",
      "| 1 | 0001 | Core | 1 | Fase 0 |",
    ].join("\n");
    const result = extractPhaseSummary(content);
    expect(result).toHaveLength(1);
    expect(result[0]?.phase).toBe(1);
  });
});
