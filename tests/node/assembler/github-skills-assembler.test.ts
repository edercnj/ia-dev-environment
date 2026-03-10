import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import {
  GithubSkillsAssembler,
  SKILL_GROUPS,
  INFRA_SKILL_CONDITIONS,
} from "../../../src/assembler/github-skills-assembler.js";
import { TemplateEngine } from "../../../src/template-engine.js";
import {
  ProjectConfig,
  ProjectIdentity,
  ArchitectureConfig,
  InterfaceConfig,
  LanguageConfig,
  FrameworkConfig,
  InfraConfig,
} from "../../../src/models.js";

function buildConfig(overrides: {
  orchestrator?: string;
  container?: string;
  iac?: string;
  templating?: string;
} = {}): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity("my-app", "A test application"),
    new ArchitectureConfig("microservice", false, false),
    [new InterfaceConfig("rest")],
    new LanguageConfig("typescript", "5"),
    new FrameworkConfig("commander", "", "npm"),
    undefined,
    new InfraConfig(
      overrides.container ?? "none",
      overrides.orchestrator ?? "none",
      overrides.templating ?? "kustomize",
      overrides.iac ?? "none",
    ),
  );
}

function createSkillTemplate(
  resourcesDir: string,
  group: string,
  skillName: string,
  content: string = "# Skill\n{project_name}",
): void {
  const dir = path.join(
    resourcesDir, "github-skills-templates", group,
  );
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(
    path.join(dir, `${skillName}.md`), content, "utf-8",
  );
}

function createAllInfraSkills(resourcesDir: string): void {
  for (const name of SKILL_GROUPS["infrastructure"]!) {
    createSkillTemplate(resourcesDir, "infrastructure", name);
  }
}

describe("SKILL_GROUPS and INFRA_SKILL_CONDITIONS", () => {
  it("SKILL_GROUPS_has7Groups", () => {
    expect(Object.keys(SKILL_GROUPS)).toHaveLength(7);
  });

  it("INFRA_SKILL_CONDITIONS_has5Entries", () => {
    expect(Object.keys(INFRA_SKILL_CONDITIONS)).toHaveLength(5);
  });
});

describe("GithubSkillsAssembler — filterSkills", () => {
  const assembler = new GithubSkillsAssembler();

  it("filterSkills_nonInfraGroup_returnsAllSkills", () => {
    const config = buildConfig();
    const engine = new TemplateEngine(
      fs.mkdtempSync(path.join(tmpdir(), "filter-")), config,
    );
    const storySkills = SKILL_GROUPS["story"]!;
    const tmpDir = fs.mkdtempSync(
      path.join(tmpdir(), "filter-test-"),
    );
    const resourcesDir = path.join(tmpDir, "resources");
    fs.mkdirSync(resourcesDir, { recursive: true });
    for (const name of storySkills) {
      createSkillTemplate(resourcesDir, "story", name);
    }
    const outputDir = path.join(tmpDir, "output");
    fs.mkdirSync(outputDir, { recursive: true });
    const result = assembler.assemble(
      config, outputDir, resourcesDir, engine,
    );
    expect(result).toHaveLength(storySkills.length);
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  it("filterSkills_infraGroup_orchestratorNone_excludesSetupAndK8s", () => {
    const config = buildConfig({
      orchestrator: "none", container: "none",
      iac: "none", templating: "none",
    });
    const tmpDir = fs.mkdtempSync(
      path.join(tmpdir(), "filter-infra-"),
    );
    const resourcesDir = path.join(tmpDir, "resources");
    fs.mkdirSync(resourcesDir, { recursive: true });
    createAllInfraSkills(resourcesDir);
    const outputDir = path.join(tmpDir, "output");
    fs.mkdirSync(outputDir, { recursive: true });
    const engine = new TemplateEngine(resourcesDir, config);
    const result = assembler.assemble(
      config, outputDir, resourcesDir, engine,
    );
    const infraFiles = result.filter(
      (f) => f.includes("github/skills/"),
    );
    const names = infraFiles.map(
      (f) => path.basename(path.dirname(f)),
    );
    expect(names).not.toContain("setup-environment");
    expect(names).not.toContain("k8s-deployment");
    expect(names).not.toContain("k8s-kustomize");
    expect(names).not.toContain("dockerfile");
    expect(names).not.toContain("iac-terraform");
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  it("filterSkills_infraGroup_orchestratorKubernetes_includesK8sSkills", () => {
    const config = buildConfig({
      orchestrator: "kubernetes",
      templating: "kustomize",
    });
    const tmpDir = fs.mkdtempSync(
      path.join(tmpdir(), "filter-k8s-"),
    );
    const resourcesDir = path.join(tmpDir, "resources");
    fs.mkdirSync(resourcesDir, { recursive: true });
    createAllInfraSkills(resourcesDir);
    const outputDir = path.join(tmpDir, "output");
    fs.mkdirSync(outputDir, { recursive: true });
    const engine = new TemplateEngine(resourcesDir, config);
    const result = assembler.assemble(
      config, outputDir, resourcesDir, engine,
    );
    const names = result.map(
      (f) => path.basename(path.dirname(f)),
    );
    expect(names).toContain("setup-environment");
    expect(names).toContain("k8s-deployment");
    expect(names).toContain("k8s-kustomize");
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  it("filterSkills_infraGroup_containerNone_excludesDockerfile", () => {
    const config = buildConfig({ container: "none" });
    const tmpDir = fs.mkdtempSync(
      path.join(tmpdir(), "filter-dock-"),
    );
    const resourcesDir = path.join(tmpDir, "resources");
    fs.mkdirSync(resourcesDir, { recursive: true });
    createAllInfraSkills(resourcesDir);
    const outputDir = path.join(tmpDir, "output");
    fs.mkdirSync(outputDir, { recursive: true });
    const engine = new TemplateEngine(resourcesDir, config);
    const result = assembler.assemble(
      config, outputDir, resourcesDir, engine,
    );
    const names = result.map(
      (f) => path.basename(path.dirname(f)),
    );
    expect(names).not.toContain("dockerfile");
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  it("filterSkills_infraGroup_containerDocker_includesDockerfile", () => {
    const config = buildConfig({ container: "docker" });
    const tmpDir = fs.mkdtempSync(
      path.join(tmpdir(), "filter-dock2-"),
    );
    const resourcesDir = path.join(tmpDir, "resources");
    fs.mkdirSync(resourcesDir, { recursive: true });
    createAllInfraSkills(resourcesDir);
    const outputDir = path.join(tmpDir, "output");
    fs.mkdirSync(outputDir, { recursive: true });
    const engine = new TemplateEngine(resourcesDir, config);
    const result = assembler.assemble(
      config, outputDir, resourcesDir, engine,
    );
    const names = result.map(
      (f) => path.basename(path.dirname(f)),
    );
    expect(names).toContain("dockerfile");
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  it("filterSkills_infraGroup_iacTerraform_includesIacTerraform", () => {
    const config = buildConfig({ iac: "terraform" });
    const tmpDir = fs.mkdtempSync(
      path.join(tmpdir(), "filter-iac-"),
    );
    const resourcesDir = path.join(tmpDir, "resources");
    fs.mkdirSync(resourcesDir, { recursive: true });
    createAllInfraSkills(resourcesDir);
    const outputDir = path.join(tmpDir, "output");
    fs.mkdirSync(outputDir, { recursive: true });
    const engine = new TemplateEngine(resourcesDir, config);
    const result = assembler.assemble(
      config, outputDir, resourcesDir, engine,
    );
    const names = result.map(
      (f) => path.basename(path.dirname(f)),
    );
    expect(names).toContain("iac-terraform");
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });
});

describe("GithubSkillsAssembler — assemble", () => {
  let tmpDir: string;
  let resourcesDir: string;
  let outputDir: string;
  let assembler: GithubSkillsAssembler;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(
      path.join(tmpdir(), "gh-skills-asm-test-"),
    );
    resourcesDir = path.join(tmpDir, "resources");
    fs.mkdirSync(resourcesDir, { recursive: true });
    outputDir = path.join(tmpDir, "output");
    fs.mkdirSync(outputDir, { recursive: true });
    assembler = new GithubSkillsAssembler();
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  it("assemble_templateDirMissing_skipsGroup", () => {
    const config = buildConfig();
    const engine = new TemplateEngine(resourcesDir, config);
    const result = assembler.assemble(
      config, outputDir, resourcesDir, engine,
    );
    expect(result).toEqual([]);
  });

  it("assemble_templateFileMissing_skipsSkill", () => {
    createSkillTemplate(resourcesDir, "dev", "x-dev-implement");
    const config = buildConfig();
    const engine = new TemplateEngine(resourcesDir, config);
    const result = assembler.assemble(
      config, outputDir, resourcesDir, engine,
    );
    expect(result).toHaveLength(1);
    expect(result[0]).toContain("x-dev-implement");
  });

  it("assemble_appliesPlaceholderReplacement", () => {
    createSkillTemplate(
      resourcesDir, "dev", "x-dev-implement",
      "# Dev for {project_name}",
    );
    const config = buildConfig();
    const engine = new TemplateEngine(resourcesDir, config);
    assembler.assemble(config, outputDir, resourcesDir, engine);
    const content = fs.readFileSync(
      path.join(
        outputDir, "github", "skills", "x-dev-implement", "SKILL.md",
      ),
      "utf-8",
    );
    expect(content).toBe("# Dev for my-app");
  });

  it("assemble_outputStructure_skillName_SKILL_md", () => {
    createSkillTemplate(resourcesDir, "dev", "x-dev-implement");
    const config = buildConfig();
    const engine = new TemplateEngine(resourcesDir, config);
    const result = assembler.assemble(
      config, outputDir, resourcesDir, engine,
    );
    expect(result[0]).toBe(
      path.join(
        outputDir, "github", "skills", "x-dev-implement", "SKILL.md",
      ),
    );
  });

  it("assemble_infraFiltered_reducedOutput", () => {
    createAllInfraSkills(resourcesDir);
    const config = buildConfig({
      orchestrator: "none", container: "none",
      iac: "none", templating: "none",
    });
    const engine = new TemplateEngine(resourcesDir, config);
    const result = assembler.assemble(
      config, outputDir, resourcesDir, engine,
    );
    expect(result).toHaveLength(0);
  });

  it("assemble_createsSkillsDirectory", () => {
    createSkillTemplate(resourcesDir, "dev", "x-dev-implement");
    const config = buildConfig();
    const engine = new TemplateEngine(resourcesDir, config);
    assembler.assemble(config, outputDir, resourcesDir, engine);
    const skillDir = path.join(
      outputDir, "github", "skills", "x-dev-implement",
    );
    expect(fs.existsSync(skillDir)).toBe(true);
    expect(fs.statSync(skillDir).isDirectory()).toBe(true);
  });
});
