import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import {
  GithubSkillsAssembler,
  SKILL_GROUPS,
  INFRA_SKILL_CONDITIONS,
  NESTED_GROUPS,
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
  it("SKILL_GROUPS_has8Groups", () => {
    expect(Object.keys(SKILL_GROUPS)).toHaveLength(8);
  });

  it("SKILL_GROUPS_libGroup_contains3Skills", () => {
    expect(SKILL_GROUPS["lib"]).toEqual([
      "x-lib-task-decomposer",
      "x-lib-audit-rules",
      "x-lib-group-verifier",
    ]);
  });

  it("SKILL_GROUPS_libSkillNames_followNamingConvention", () => {
    for (const name of SKILL_GROUPS["lib"]!) {
      expect(name).toMatch(/^x-lib-/);
    }
  });

  it("INFRA_SKILL_CONDITIONS_has5Entries", () => {
    expect(Object.keys(INFRA_SKILL_CONDITIONS)).toHaveLength(5);
  });
});

describe("GithubSkillsAssembler — filterSkills", () => {
  const assembler = new GithubSkillsAssembler();

  it("filterSkills_nonInfraGroup_returnsAllSkills", () => {
    const config = buildConfig();
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
    const engine = new TemplateEngine(resourcesDir, config);
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
      (f) => f.includes("skills/"),
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
        outputDir, "skills", "x-dev-implement", "SKILL.md",
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
        outputDir, "skills", "x-dev-implement", "SKILL.md",
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
      outputDir, "skills", "x-dev-implement",
    );
    expect(fs.existsSync(skillDir)).toBe(true);
    expect(fs.statSync(skillDir).isDirectory()).toBe(true);
  });
});

function createAllLibSkills(resourcesDir: string): void {
  for (const name of SKILL_GROUPS["lib"]!) {
    createSkillTemplate(resourcesDir, "lib", name);
  }
}

describe("NESTED_GROUPS constant", () => {
  it("NESTED_GROUPS_containsLib", () => {
    expect(NESTED_GROUPS.has("lib")).toBe(true);
  });

  it("NESTED_GROUPS_excludesNonNestedGroups", () => {
    for (const group of ["story", "dev", "review", "testing",
      "infrastructure", "knowledge-packs", "git-troubleshooting"]) {
      expect(NESTED_GROUPS.has(group)).toBe(false);
    }
  });
});

describe("GithubSkillsAssembler — lib group", () => {
  let tmpDir: string;
  let resourcesDir: string;
  let outputDir: string;
  let assembler: GithubSkillsAssembler;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(
      path.join(tmpdir(), "gh-skills-lib-test-"),
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

  it("assemble_libGroup_generates3Skills", () => {
    createAllLibSkills(resourcesDir);
    const config = buildConfig();
    const engine = new TemplateEngine(resourcesDir, config);
    const result = assembler.assemble(
      config, outputDir, resourcesDir, engine,
    );
    expect(result).toHaveLength(3);
  });

  it("assemble_libGroup_nestedUnderLibDir", () => {
    createAllLibSkills(resourcesDir);
    const config = buildConfig();
    const engine = new TemplateEngine(resourcesDir, config);
    const result = assembler.assemble(
      config, outputDir, resourcesDir, engine,
    );
    for (const file of result) {
      expect(file).toContain(
        path.join("skills", "lib"),
      );
    }
  });

  it("assemble_libGroup_exactOutputPaths", () => {
    createAllLibSkills(resourcesDir);
    const config = buildConfig();
    const engine = new TemplateEngine(resourcesDir, config);
    const result = assembler.assemble(
      config, outputDir, resourcesDir, engine,
    );
    const expected = SKILL_GROUPS["lib"]!.map((name) =>
      path.join(
        outputDir, "skills", "lib", name, "SKILL.md",
      ),
    );
    expect(result).toEqual(expected);
  });

  it("assemble_libGroup_appliesPlaceholderReplacement", () => {
    createSkillTemplate(
      resourcesDir, "lib", "x-lib-task-decomposer",
      "# Decomposer for {project_name}",
    );
    const config = buildConfig();
    const engine = new TemplateEngine(resourcesDir, config);
    assembler.assemble(config, outputDir, resourcesDir, engine);
    const content = fs.readFileSync(
      path.join(
        outputDir, "skills", "lib",
        "x-lib-task-decomposer", "SKILL.md",
      ),
      "utf-8",
    );
    expect(content).toBe("# Decomposer for my-app");
  });

  it("assemble_libGroup_createsNestedDirectory", () => {
    createAllLibSkills(resourcesDir);
    const config = buildConfig();
    const engine = new TemplateEngine(resourcesDir, config);
    assembler.assemble(config, outputDir, resourcesDir, engine);
    const libDir = path.join(
      outputDir, "skills", "lib",
    );
    expect(fs.existsSync(libDir)).toBe(true);
    expect(fs.statSync(libDir).isDirectory()).toBe(true);
  });

  it("assemble_libGroup_templateMissing_skipsSkill", () => {
    createSkillTemplate(resourcesDir, "lib", "x-lib-task-decomposer");
    const config = buildConfig();
    const engine = new TemplateEngine(resourcesDir, config);
    const result = assembler.assemble(
      config, outputDir, resourcesDir, engine,
    );
    expect(result).toHaveLength(1);
    expect(result[0]).toContain("x-lib-task-decomposer");
  });

  it("assemble_libGroup_unconditional_noConfigFiltering", () => {
    createAllLibSkills(resourcesDir);
    const config = buildConfig({
      orchestrator: "none", container: "none",
      iac: "none", templating: "none",
    });
    const engine = new TemplateEngine(resourcesDir, config);
    const result = assembler.assemble(
      config, outputDir, resourcesDir, engine,
    );
    expect(result).toHaveLength(3);
  });

  it("assemble_mixedGroups_libNestedOthersFlat", () => {
    createSkillTemplate(resourcesDir, "dev", "x-dev-implement");
    createAllLibSkills(resourcesDir);
    const config = buildConfig();
    const engine = new TemplateEngine(resourcesDir, config);
    const result = assembler.assemble(
      config, outputDir, resourcesDir, engine,
    );
    const devFile = result.find((f) => f.includes("x-dev-implement"));
    const libFile = result.find(
      (f) => f.includes("x-lib-task-decomposer"),
    );
    expect(devFile).toBe(
      path.join(
        outputDir, "skills",
        "x-dev-implement", "SKILL.md",
      ),
    );
    expect(libFile).toBe(
      path.join(
        outputDir, "skills", "lib",
        "x-lib-task-decomposer", "SKILL.md",
      ),
    );
  });

  it("assemble_libGroup_dirMissing_skipsGroup", () => {
    const config = buildConfig();
    const engine = new TemplateEngine(resourcesDir, config);
    const result = assembler.assemble(
      config, outputDir, resourcesDir, engine,
    );
    const libFiles = result.filter(
      (f) => f.includes(path.join("skills", "lib")),
    );
    expect(libFiles).toHaveLength(0);
  });
});

function createSkillWithReferences(
  resourcesDir: string,
  group: string,
  skillName: string,
  refFiles: Record<string, string>,
  skillContent: string = "# Skill\n{project_name}",
): void {
  createSkillTemplate(resourcesDir, group, skillName, skillContent);
  const refsDir = path.join(
    resourcesDir, "github-skills-templates", group,
    "references", skillName,
  );
  fs.mkdirSync(refsDir, { recursive: true });
  for (const [name, content] of Object.entries(refFiles)) {
    fs.writeFileSync(path.join(refsDir, name), content, "utf-8");
  }
}

describe("GithubSkillsAssembler — references", () => {
  let tmpDir: string;
  let resourcesDir: string;
  let outputDir: string;
  let assembler: GithubSkillsAssembler;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(
      path.join(tmpdir(), "gh-skills-refs-test-"),
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

  it("renderSkill_withReferencesDir_copiesReferencesToOutput", () => {
    createSkillWithReferences(
      resourcesDir, "dev", "x-dev-lifecycle",
      { "openapi-generator.md": "# OpenAPI\n{project_name}" },
    );
    const config = buildConfig();
    const engine = new TemplateEngine(resourcesDir, config);
    assembler.assemble(config, outputDir, resourcesDir, engine);
    const refPath = path.join(
      outputDir, "skills", "x-dev-lifecycle",
      "references", "openapi-generator.md",
    );
    expect(fs.existsSync(refPath)).toBe(true);
  });

  it("renderSkill_withoutReferencesDir_behaviorUnchanged", () => {
    createSkillTemplate(resourcesDir, "dev", "x-dev-implement");
    const config = buildConfig();
    const engine = new TemplateEngine(resourcesDir, config);
    assembler.assemble(config, outputDir, resourcesDir, engine);
    const refsDir = path.join(
      outputDir, "skills", "x-dev-implement", "references",
    );
    expect(fs.existsSync(refsDir)).toBe(false);
  });

  it("renderSkill_referencesContent_placeholdersReplaced", () => {
    createSkillWithReferences(
      resourcesDir, "dev", "x-dev-lifecycle",
      { "ref.md": "# Ref for {project_name}" },
    );
    const config = buildConfig();
    const engine = new TemplateEngine(resourcesDir, config);
    assembler.assemble(config, outputDir, resourcesDir, engine);
    const refPath = path.join(
      outputDir, "skills", "x-dev-lifecycle",
      "references", "ref.md",
    );
    const content = fs.readFileSync(refPath, "utf-8");
    expect(content).toBe("# Ref for my-app");
  });

  it("renderSkill_multipleReferenceFiles_allCopied", () => {
    createSkillWithReferences(
      resourcesDir, "dev", "x-dev-lifecycle",
      {
        "openapi-generator.md": "# OpenAPI",
        "grpc-generator.md": "# gRPC",
      },
    );
    const config = buildConfig();
    const engine = new TemplateEngine(resourcesDir, config);
    assembler.assemble(config, outputDir, resourcesDir, engine);
    const refsDir = path.join(
      outputDir, "skills", "x-dev-lifecycle", "references",
    );
    const files = fs.readdirSync(refsDir).sort();
    expect(files).toEqual([
      "grpc-generator.md", "openapi-generator.md",
    ]);
  });

  it("renderSkill_libGroupWithReferences_copiedToNestedPath", () => {
    createSkillWithReferences(
      resourcesDir, "lib", "x-lib-task-decomposer",
      { "guide.md": "# Guide\n{project_name}" },
    );
    const config = buildConfig();
    const engine = new TemplateEngine(resourcesDir, config);
    assembler.assemble(config, outputDir, resourcesDir, engine);
    const refPath = path.join(
      outputDir, "skills", "lib", "x-lib-task-decomposer",
      "references", "guide.md",
    );
    expect(fs.existsSync(refPath)).toBe(true);
    const content = fs.readFileSync(refPath, "utf-8");
    expect(content).toBe("# Guide\nmy-app");
  });
});
