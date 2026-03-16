/**
 * CicdAssembler — generates CI/CD artifacts conditionally.
 *
 * Produces: GitHub Actions CI workflow, Dockerfile, Docker Compose,
 * Kubernetes manifests, smoke test config, and deploy runbook.
 * Conditional logic based on container, orchestrator, and smokeTests.
 *
 * @module
 */
import * as fs from "node:fs";
import * as path from "node:path";
import type { ProjectConfig } from "../models.js";
import type { TemplateEngine } from "../template-engine.js";
import type { AssembleResult } from "./rules-assembler.js";
import {
  LANGUAGE_COMMANDS,
  FRAMEWORK_PORTS,
  FRAMEWORK_HEALTH_PATHS,
  DOCKER_BASE_IMAGES,
  DEFAULT_PORT_FALLBACK,
  DEFAULT_HEALTH_PATH,
  DEFAULT_DOCKER_IMAGE,
} from "../domain/stack-mapping.js";

const CICD_TEMPLATES = "cicd-templates";
const CI_TEMPLATE = "ci-workflow/ci.yml.njk";
const COMPOSE_TEMPLATE = "docker-compose/docker-compose.yml.njk";
const RUNBOOK_TEMPLATE = "deploy-runbook/deploy-runbook.md.njk";
const SMOKE_SOURCE = "smoke-tests/smoke-config.md";
const DOCKER_CONDITION = "docker";
const K8S_CONDITION = "kubernetes";

const K8S_MANIFESTS: readonly string[] = [
  "deployment.yaml",
  "service.yaml",
  "configmap.yaml",
];

/** Build stack-specific template context from mapping constants. */
function buildStackContext(
  config: ProjectConfig,
): Record<string, unknown> {
  const langKey = `${config.language.name}-${config.framework.buildTool}`;
  const commands = LANGUAGE_COMMANDS[langKey];
  const port = FRAMEWORK_PORTS[config.framework.name]
    ?? DEFAULT_PORT_FALLBACK;
  const healthPath = FRAMEWORK_HEALTH_PATHS[config.framework.name]
    ?? DEFAULT_HEALTH_PATH;
  const baseImage = DOCKER_BASE_IMAGES[config.language.name]
    ?? DEFAULT_DOCKER_IMAGE;
  const resolvedImage = baseImage.replace(
    "{version}", config.language.version,
  );
  return {
    compile_cmd: commands?.compileCmd ?? "",
    build_cmd: commands?.buildCmd ?? "",
    test_cmd: commands?.testCmd ?? "",
    coverage_cmd: commands?.coverageCmd ?? "",
    file_extension: commands?.fileExtension ?? "",
    build_file: commands?.buildFile ?? "",
    package_manager: commands?.packageManager ?? "",
    framework_port: port,
    health_path: healthPath,
    docker_base_image: resolvedImage,
  };
}

/** Renders a template and writes result to disk. */
function renderAndWrite(
  engine: TemplateEngine,
  templateRelPath: string,
  destPath: string,
  extraContext: Record<string, unknown>,
): boolean {
  try {
    const content = engine.renderTemplate(
      `${CICD_TEMPLATES}/${templateRelPath}`,
      extraContext,
    );
    fs.mkdirSync(path.dirname(destPath), { recursive: true });
    fs.writeFileSync(destPath, content, "utf-8");
    return true;
  } catch {
    return false;
  }
}

/** Generates CI/CD artifacts conditionally based on config. */
export class CicdAssembler {
  /** Assemble CI/CD artifacts and return files + warnings. */
  assemble(
    config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    engine: TemplateEngine,
  ): AssembleResult {
    const files: string[] = [];
    const warnings: string[] = [];
    const ctx = buildStackContext(config);
    this.generateCiWorkflow(
      outputDir, engine, ctx, files, warnings,
    );
    this.generateDockerfile(
      config, outputDir, resourcesDir, engine, ctx,
      files, warnings,
    );
    this.generateDockerCompose(
      config, outputDir, engine, ctx, files, warnings,
    );
    this.generateK8sManifests(
      config, outputDir, engine, ctx, files, warnings,
    );
    this.generateSmokeTestConfig(
      config, outputDir, resourcesDir, files, warnings,
    );
    this.generateDeployRunbook(
      outputDir, engine, ctx, files, warnings,
    );
    return { files, warnings };
  }

  /** CI workflow — always generated. */
  private generateCiWorkflow(
    outputDir: string,
    engine: TemplateEngine,
    ctx: Record<string, unknown>,
    files: string[],
    warnings: string[],
  ): void {
    const dest = path.join(
      outputDir, ".github", "workflows", "ci.yml",
    );
    if (renderAndWrite(engine, CI_TEMPLATE, dest, ctx)) {
      files.push(dest);
    } else {
      warnings.push("CI workflow template not found");
    }
  }

  /** Dockerfile — conditional on container === "docker". */
  private generateDockerfile(
    config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    engine: TemplateEngine,
    ctx: Record<string, unknown>,
    files: string[],
    warnings: string[],
  ): void {
    if (config.infrastructure.container !== DOCKER_CONDITION) {
      warnings.push("Dockerfile skipped: container is not docker");
      return;
    }
    const stackKey = `${config.language.name}-${config.framework.buildTool}`;
    const templateFile = `dockerfile/Dockerfile.${stackKey}.njk`;
    const srcPath = path.join(
      resourcesDir, CICD_TEMPLATES, templateFile,
    );
    if (!fs.existsSync(srcPath)) {
      warnings.push(
        `Dockerfile template not found for stack: ${stackKey}`,
      );
      return;
    }
    const dest = path.join(outputDir, "Dockerfile");
    if (renderAndWrite(engine, templateFile, dest, ctx)) {
      files.push(dest);
    }
  }

  /** Docker Compose — conditional on container === "docker". */
  private generateDockerCompose(
    config: ProjectConfig,
    outputDir: string,
    engine: TemplateEngine,
    ctx: Record<string, unknown>,
    files: string[],
    warnings: string[],
  ): void {
    if (config.infrastructure.container !== DOCKER_CONDITION) {
      warnings.push(
        "Docker Compose skipped: container is not docker",
      );
      return;
    }
    const dest = path.join(outputDir, "docker-compose.yml");
    if (renderAndWrite(engine, COMPOSE_TEMPLATE, dest, ctx)) {
      files.push(dest);
    }
  }

  /** K8s manifests — conditional on orchestrator === "kubernetes". */
  private generateK8sManifests(
    config: ProjectConfig,
    outputDir: string,
    engine: TemplateEngine,
    ctx: Record<string, unknown>,
    files: string[],
    warnings: string[],
  ): void {
    if (config.infrastructure.orchestrator !== K8S_CONDITION) {
      warnings.push(
        "K8s manifests skipped: orchestrator is not kubernetes",
      );
      return;
    }
    for (const manifest of K8S_MANIFESTS) {
      const dest = path.join(outputDir, "k8s", manifest);
      const tpl = `k8s/${manifest.replace(".yaml", ".yaml.njk")}`;
      if (renderAndWrite(engine, tpl, dest, ctx)) {
        files.push(dest);
      }
    }
  }

  /** Smoke test config — conditional on smokeTests === true. */
  private generateSmokeTestConfig(
    config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    files: string[],
    warnings: string[],
  ): void {
    if (!config.testing.smokeTests) {
      warnings.push(
        "Smoke test config skipped: smokeTests is false",
      );
      return;
    }
    const src = path.join(
      resourcesDir, CICD_TEMPLATES, SMOKE_SOURCE,
    );
    if (!fs.existsSync(src)) return;
    const dest = path.join(
      outputDir, "tests", "smoke", "smoke-config.md",
    );
    fs.mkdirSync(path.dirname(dest), { recursive: true });
    fs.copyFileSync(src, dest);
    files.push(dest);
  }

  /** Deploy runbook — always generated. */
  private generateDeployRunbook(
    outputDir: string,
    engine: TemplateEngine,
    ctx: Record<string, unknown>,
    files: string[],
    warnings: string[],
  ): void {
    const dest = path.join(
      outputDir, "docs", "runbook", "deploy-runbook.md",
    );
    if (renderAndWrite(engine, RUNBOOK_TEMPLATE, dest, ctx)) {
      files.push(dest);
    } else {
      warnings.push("Deploy runbook template not found");
    }
  }
}
