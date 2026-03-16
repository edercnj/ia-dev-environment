import { describe, it, expect } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

const TEMPLATE_PATH = path.resolve(
  __dirname,
  "../../..",
  "resources/templates/_TEMPLATE-PERFORMANCE-BASELINE.md",
);

describe("Performance baseline template — content validation", () => {
  // UT-1: Template file exists
  it("templateFile_exists_inResourcesTemplates", () => {
    expect(fs.existsSync(TEMPLATE_PATH)).toBe(true);
  });

  // UT-2: Template is not empty
  it("templateFile_isNotEmpty_hasContent", () => {
    const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");
    expect(content.trim().length).toBeGreaterThan(0);
  });

  // UT-3: Title heading
  it("templateFile_containsTitle_performanceBaselines", () => {
    const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");
    expect(content).toContain("# Performance Baselines");
  });

  // UT-4: Measurement Guide section
  it("templateFile_containsMeasurementGuideSection", () => {
    const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");
    expect(content).toContain("## Measurement Guide");
  });

  // UT-5: Baselines section
  it("templateFile_containsBaselinesSection", () => {
    const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");
    expect(content).toContain("## Baselines");
  });

  // UT-6: Baselines table with all 7 columns
  it.each([
    ["Feature/Story ID"],
    ["Date"],
    ["Metric"],
    ["Before"],
    ["After"],
    ["Delta"],
    ["Notes"],
  ])("templateFile_baselinesTable_containsColumn_%s", (column) => {
    const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");
    expect(content).toContain(column);
  });

  // UT-7: Measurement Guide documents all 6 metrics
  it.each([
    ["latency_p50"],
    ["latency_p95"],
    ["latency_p99"],
    ["throughput_rps"],
    ["memory_mb"],
    ["startup_ms"],
  ])("templateFile_measurementGuide_containsMetric_%s", (metric) => {
    const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");
    expect(content).toContain(metric);
  });

  // UT-8: Tools by Stack section
  it("templateFile_measurementGuide_containsToolsByStack", () => {
    const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");
    expect(content).toMatch(/Tools by Stack/);
  });

  // UT-9: Language placeholder
  it("templateFile_measurementGuide_containsLanguagePlaceholder", () => {
    const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");
    expect(content).toContain("{{LANGUAGE}}");
  });

  // UT-10: Framework placeholder
  it("templateFile_measurementGuide_containsFrameworkPlaceholder", () => {
    const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");
    expect(content).toContain("{{FRAMEWORK}}");
  });

  // UT-11: Delta Interpretation section
  it("templateFile_containsDeltaInterpretation", () => {
    const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");
    expect(content).toMatch(/Delta Interpretation/);
  });

  // UT-12: Acceptable threshold
  it("templateFile_deltaInterpretation_containsAcceptableThreshold", () => {
    const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");
    expect(content).toMatch(/<= \+10%/);
  });

  // UT-13: Warning threshold
  it("templateFile_deltaInterpretation_containsWarningThreshold", () => {
    const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");
    expect(content).toMatch(/\+10% to \+25%/);
  });

  // UT-14: Investigation threshold
  it("templateFile_deltaInterpretation_containsInvestigationThreshold", () => {
    const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");
    expect(content).toMatch(/> \+25%/);
  });

  // UT-15: Example baseline row
  it("templateFile_baselines_containsExampleRow", () => {
    const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");
    expect(content).toMatch(/example/i);
  });

  // UT-16: Measurement conditions
  it("templateFile_measurementGuide_containsMeasurementConditions", () => {
    const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");
    expect(content).toMatch(/Measurement Conditions/);
  });
});
