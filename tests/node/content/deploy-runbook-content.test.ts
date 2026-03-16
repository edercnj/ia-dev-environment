import { describe, it, expect } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

const TEMPLATE_PATH = path.resolve(
  __dirname,
  "../../..",
  "resources/templates/_TEMPLATE-DEPLOY-RUNBOOK.md",
);

describe("Deploy runbook template — content validation", () => {
  // UT-1: Template file exists
  it("templateFile_exists_inResourcesTemplates", () => {
    expect(fs.existsSync(TEMPLATE_PATH)).toBe(true);
  });

  // UT-2: Template is not empty
  it("templateFile_isNotEmpty_hasContent", () => {
    const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");
    expect(content.trim().length).toBeGreaterThan(0);
  });

  // UT-3: All 7 mandatory sections present
  it.each([
    ["## 1. Service Info"],
    ["## 2. Pre-conditions"],
    ["## 3. Deploy Procedure"],
    ["## 4. Post-Deploy Verification"],
    ["## 5. Rollback Procedure"],
    ["## 6. Troubleshooting"],
    ["## 7. Contacts"],
  ])("templateFile_containsMandatorySection_%s", (section) => {
    const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");
    expect(content).toContain(section);
  });

  // UT-4: Docker conditional block present
  it("templateFile_containsDockerConditional_ifContainerBlock", () => {
    const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");
    expect(content).toContain('{% if container');
  });

  // UT-5: Kubernetes conditional block present
  it("templateFile_containsKubernetesConditional_ifOrchestratorBlock", () => {
    const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");
    expect(content).toContain('{% if orchestrator');
  });

  // UT-6: kubectl rollout undo in K8s conditional
  it("templateFile_containsKubectlRolloutUndo_inRollbackSection", () => {
    const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");
    expect(content).toContain("kubectl rollout undo");
  });

  // UT-7: Database migration conditional block present
  it("templateFile_containsDatabaseMigrationConditional_ifDatabaseBlock", () => {
    const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");
    expect(content).toContain('{% if database_name');
  });

  // UT-8: Title with project_name placeholder
  it("templateFile_containsTitle_withServiceNamePlaceholder", () => {
    const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");
    expect(content).toContain("# Deploy Runbook — {{ project_name }}");
  });

  // UT-9: Troubleshooting table structure
  it("templateFile_containsTroubleshootingTable_withProblemCauseSolution", () => {
    const content = fs.readFileSync(TEMPLATE_PATH, "utf-8");
    expect(content).toContain("Problem");
    expect(content).toContain("Cause");
    expect(content).toContain("Solution");
    expect(content).toMatch(/\|.*Problem.*\|.*Cause.*\|.*Solution.*\|/);
  });
});
