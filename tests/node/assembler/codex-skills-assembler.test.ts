import { mkdirSync, mkdtempSync, rmSync, writeFileSync, readFileSync, existsSync } from "node:fs";
import { join } from "node:path";
import { tmpdir } from "node:os";
import { afterEach, beforeEach, describe, expect, it } from "vitest";

import { CodexSkillsAssembler } from "../../../src/assembler/codex-skills-assembler.js";
import { buildAssemblers } from "../../../src/assembler/pipeline.js";
import { TemplateEngine } from "../../../src/template-engine.js";
import { aMinimalProjectConfig } from "../../fixtures/project-config.fixture.js";

function seedSkill(
  claudeDir: string,
  name: string,
  content: string,
): void {
  const skillDir = join(claudeDir, "skills", name);
  mkdirSync(skillDir, { recursive: true });
  writeFileSync(join(skillDir, "SKILL.md"), content, "utf-8");
}

function seedSkillWithRefs(
  claudeDir: string,
  name: string,
  skillContent: string,
  refs: Record<string, string>,
): void {
  seedSkill(claudeDir, name, skillContent);
  const refsDir = join(claudeDir, "skills", name, "references");
  mkdirSync(refsDir, { recursive: true });
  for (const [fname, content] of Object.entries(refs)) {
    writeFileSync(join(refsDir, fname), content, "utf-8");
  }
}

describe("CodexSkillsAssembler", () => {
  let tmpDir: string;
  let claudeDir: string;
  let agentsDir: string;
  let assembler: CodexSkillsAssembler;
  let engine: TemplateEngine;

  beforeEach(() => {
    tmpDir = mkdtempSync(join(tmpdir(), "codex-skills-"));
    claudeDir = join(tmpDir, ".claude");
    agentsDir = join(tmpDir, ".agents");
    mkdirSync(claudeDir, { recursive: true });
    assembler = new CodexSkillsAssembler();
    const config = aMinimalProjectConfig();
    const resourcesDir = join(tmpDir, "resources");
    mkdirSync(resourcesDir, { recursive: true });
    engine = new TemplateEngine(resourcesDir, config);
  });

  afterEach(() => {
    rmSync(tmpDir, { recursive: true, force: true });
  });

  it("assemble_multipleSkills_copiesAllToAgentsDir", () => {
    seedSkill(claudeDir, "x-dev", "---\nname: x-dev\n---\nDev skill");
    seedSkill(claudeDir, "x-test", "---\nname: x-test\n---\nTest skill");
    seedSkill(claudeDir, "api-design", "---\nname: api-design\nuser-invocable: false\n---\nKP");

    const result = assembler.assemble(
      aMinimalProjectConfig(), agentsDir, "", engine,
    );

    expect(result.files.length).toBe(3);
    expect(existsSync(join(agentsDir, "skills", "api-design", "SKILL.md"))).toBe(true);
    expect(existsSync(join(agentsDir, "skills", "x-dev", "SKILL.md"))).toBe(true);
    expect(existsSync(join(agentsDir, "skills", "x-test", "SKILL.md"))).toBe(true);
  });

  it("assemble_skillWithReferences_copiesRefsDirectory", () => {
    seedSkillWithRefs(
      claudeDir, "coding-standards",
      "---\nname: coding-standards\n---\nCoding",
      { "clean-code.md": "# Clean Code", "solid.md": "# SOLID" },
    );

    const result = assembler.assemble(
      aMinimalProjectConfig(), agentsDir, "", engine,
    );

    expect(result.files.length).toBe(3);
    expect(existsSync(join(agentsDir, "skills", "coding-standards", "SKILL.md"))).toBe(true);
    expect(existsSync(join(agentsDir, "skills", "coding-standards", "references", "clean-code.md"))).toBe(true);
    expect(existsSync(join(agentsDir, "skills", "coding-standards", "references", "solid.md"))).toBe(true);
  });

  it("assemble_noClaudeSkillsDir_returnsWarning", () => {
    rmSync(claudeDir, { recursive: true, force: true });

    const result = assembler.assemble(
      aMinimalProjectConfig(), agentsDir, "", engine,
    );

    expect(result.files).toHaveLength(0);
    expect(result.warnings).toContain(
      "No skills directory found in .claude/ output",
    );
  });

  it("assemble_emptySkillsDir_returnsWarning", () => {
    mkdirSync(join(claudeDir, "skills"), { recursive: true });

    const result = assembler.assemble(
      aMinimalProjectConfig(), agentsDir, "", engine,
    );

    expect(result.files).toHaveLength(0);
    expect(result.warnings).toContain(
      "No skills with SKILL.md found in .claude/skills/",
    );
  });

  it("assemble_dirWithoutSkillMd_skipsDirectory", () => {
    mkdirSync(join(claudeDir, "skills", "empty-dir"), { recursive: true });
    seedSkill(claudeDir, "valid", "---\nname: valid\n---\nOk");

    const result = assembler.assemble(
      aMinimalProjectConfig(), agentsDir, "", engine,
    );

    expect(result.files.length).toBe(1);
    expect(existsSync(join(agentsDir, "skills", "empty-dir"))).toBe(false);
    expect(existsSync(join(agentsDir, "skills", "valid", "SKILL.md"))).toBe(true);
  });

  it("assemble_preservesContent_byteForByte", () => {
    const content = "---\nname: precise\ndescription: exact copy\n---\n# Precise Skill\n\nContent here.";
    seedSkill(claudeDir, "precise", content);

    assembler.assemble(aMinimalProjectConfig(), agentsDir, "", engine);

    const copied = readFileSync(
      join(agentsDir, "skills", "precise", "SKILL.md"), "utf-8",
    );
    expect(copied).toBe(content);
  });

  it("assemble_nestedLibSkills_preservesNesting", () => {
    const libDir = join(claudeDir, "skills", "lib", "x-lib-task");
    mkdirSync(libDir, { recursive: true });
    writeFileSync(join(libDir, "SKILL.md"), "---\nname: x-lib-task\n---\nLib", "utf-8");

    const result = assembler.assemble(
      aMinimalProjectConfig(), agentsDir, "", engine,
    );

    expect(result.files.length).toBe(1);
    expect(existsSync(join(agentsDir, "skills", "lib", "x-lib-task", "SKILL.md"))).toBe(true);
  });

  it("assemble_returnsSortedFiles", () => {
    seedSkill(claudeDir, "z-last", "---\nname: z-last\n---\nZ");
    seedSkill(claudeDir, "a-first", "---\nname: a-first\n---\nA");

    const result = assembler.assemble(
      aMinimalProjectConfig(), agentsDir, "", engine,
    );

    const names = result.files.map((f) => f.split("/").at(-2));
    expect(names[0]).toBe("a-first");
    expect(names[1]).toBe("z-last");
  });

  it("assemble_noWarnings_onSuccess", () => {
    seedSkill(claudeDir, "ok", "---\nname: ok\n---\nOk");

    const result = assembler.assemble(
      aMinimalProjectConfig(), agentsDir, "", engine,
    );

    expect(result.warnings).toHaveLength(0);
  });
});

describe("Pipeline integration", () => {
  it("pipeline_containsCodexSkillsAssembler", () => {
    const assemblers = buildAssemblers();
    const descriptor = assemblers.find(
      (a) => a.name === "CodexSkillsAssembler",
    );
    expect(descriptor).toBeDefined();
    expect(descriptor!.target).toBe("codex-agents");
  });

  it("pipeline_codexSkillsBeforeReadme", () => {
    const assemblers = buildAssemblers();
    const skillsIdx = assemblers.findIndex(
      (a) => a.name === "CodexSkillsAssembler",
    );
    const readmeIdx = assemblers.findIndex(
      (a) => a.name === "ReadmeAssembler",
    );
    expect(skillsIdx).toBeLessThan(readmeIdx);
  });

  it("pipeline_codexSkillsAfterCodexConfig", () => {
    const assemblers = buildAssemblers();
    const configIdx = assemblers.findIndex(
      (a) => a.name === "CodexConfigAssembler",
    );
    const skillsIdx = assemblers.findIndex(
      (a) => a.name === "CodexSkillsAssembler",
    );
    expect(skillsIdx).toBeGreaterThan(configIdx);
  });
});
