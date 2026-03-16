import { describe, it, expect } from "vitest";

import {
  extractDependencyMatrix,
  extractPhaseSummary,
} from "../../../src/domain/implementation-map/markdown-parser.js";
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
});
