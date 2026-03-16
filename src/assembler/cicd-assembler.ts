/** @module CicdAssembler — generates CI/CD artifacts conditionally. */
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
const SMOKE_SOURCE = "smoke-tests/smoke-config.md";
const DOCKER_CONDITION = "docker";
const K8S_CONDITION = "kubernetes";

const K8S_MANIFESTS: readonly string[] = [
  "deployment.yaml",
  "service.yaml",
  "configmap.yaml",
];

const LINT_COMMANDS: Readonly<Record<string, string>> = {
  "java-maven": "./mvnw checkstyle:check",
  "java-gradle": "./gradlew spotlessCheck",
  "kotlin-gradle": "./gradlew ktlintCheck",
  "typescript-npm": "npm run lint",
  "python-pip": "ruff check .",
  "go-go": "golangci-lint run",
  "go-go-mod": "golangci-lint run",
  "rust-cargo": "cargo clippy -- -D warnings",
};
const DEFAULT_LINT_CMD = "echo 'No linter configured'";

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
    lint_cmd: LINT_COMMANDS[langKey] ?? DEFAULT_LINT_CMD,
    file_extension: commands?.fileExtension ?? "",
    build_file: commands?.buildFile ?? "",
    package_manager: commands?.packageManager ?? "",
    framework_port: port,
    health_path: healthPath,
    docker_base_image: resolvedImage,
    container: config.infrastructure.container,
  };
}

/** Renders a template and writes to disk. Returns null on success, error on failure. */
function renderAndWrite(
  engine: TemplateEngine,
  templateRelPath: string,
  destPath: string,
  extraContext: Record<string, unknown>,
): string | null {
  try {
    const content = engine.renderTemplate(
      `${CICD_TEMPLATES}/${templateRelPath}`,
      extraContext,
    );
    fs.mkdirSync(path.dirname(destPath), { recursive: true });
    fs.writeFileSync(destPath, content, "utf-8");
    return null;
  } catch (error: unknown) {
    const msg = error instanceof Error ? error.message : String(error);
    return `Failed to render ${templateRelPath}: ${msg}`;
  }
}

/** Bundled context for generation methods (max 4 params rule). */
interface GenerationContext {
  readonly config: ProjectConfig;
  readonly outputDir: string;
  readonly resourcesDir: string;
  readonly engine: TemplateEngine;
  readonly ctx: Record<string, unknown>;
  readonly files: string[];
  readonly warnings: string[];
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
    const gc: GenerationContext = {
      config, outputDir, resourcesDir, engine,
      ctx: buildStackContext(config), files: [], warnings: [],
    };
    this.generateCiWorkflow(gc);
    this.generateDockerfile(gc);
    this.generateDockerCompose(gc);
    this.generateK8sManifests(gc);
    this.generateSmokeTestConfig(gc);
    return { files: gc.files, warnings: gc.warnings };
  }

  /** CI workflow — always generated. */
  private generateCiWorkflow(gc: GenerationContext): void {
    const dest = path.join(
      gc.outputDir, ".github", "workflows", "ci.yml",
    );
    const err = renderAndWrite(gc.engine, CI_TEMPLATE, dest, gc.ctx);
    if (err === null) {
      gc.files.push(dest);
    } else {
      gc.warnings.push(err);
    }
  }

  /** Dockerfile — conditional on container === "docker". */
  private generateDockerfile(gc: GenerationContext): void {
    if (gc.config.infrastructure.container !== DOCKER_CONDITION) {
      gc.warnings.push("Dockerfile skipped: container is not docker");
      return;
    }
    const stackKey =
      `${gc.config.language.name}-${gc.config.framework.buildTool}`;
    const tpl = `dockerfile/Dockerfile.${stackKey}.njk`;
    const srcPath = path.join(gc.resourcesDir, CICD_TEMPLATES, tpl);
    if (!fs.existsSync(srcPath)) {
      gc.warnings.push(
        `Dockerfile template not found for stack: ${stackKey}`,
      );
      return;
    }
    const dest = path.join(gc.outputDir, "Dockerfile");
    const err = renderAndWrite(gc.engine, tpl, dest, gc.ctx);
    if (err === null) {
      gc.files.push(dest);
    } else {
      gc.warnings.push(err);
    }
  }

  /** Docker Compose — conditional on container === "docker". */
  private generateDockerCompose(gc: GenerationContext): void {
    if (gc.config.infrastructure.container !== DOCKER_CONDITION) {
      gc.warnings.push(
        "Docker Compose skipped: container is not docker",
      );
      return;
    }
    const dest = path.join(gc.outputDir, "docker-compose.yml");
    const err = renderAndWrite(gc.engine, COMPOSE_TEMPLATE, dest, gc.ctx);
    if (err === null) {
      gc.files.push(dest);
    } else {
      gc.warnings.push(err);
    }
  }

  /** K8s manifests — conditional on orchestrator === "kubernetes". */
  private generateK8sManifests(gc: GenerationContext): void {
    if (gc.config.infrastructure.orchestrator !== K8S_CONDITION) {
      gc.warnings.push(
        "K8s manifests skipped: orchestrator is not kubernetes",
      );
      return;
    }
    for (const manifest of K8S_MANIFESTS) {
      const dest = path.join(gc.outputDir, "k8s", manifest);
      const tpl = `k8s/${manifest.replace(".yaml", ".yaml.njk")}`;
      const err = renderAndWrite(gc.engine, tpl, dest, gc.ctx);
      if (err === null) {
        gc.files.push(dest);
      } else {
        gc.warnings.push(err);
      }
    }
  }

  /** Smoke test config — conditional on smokeTests === true. */
  private generateSmokeTestConfig(gc: GenerationContext): void {
    if (!gc.config.testing.smokeTests) {
      gc.warnings.push(
        "Smoke test config skipped: smokeTests is false",
      );
      return;
    }
    const src = path.join(
      gc.resourcesDir, CICD_TEMPLATES, SMOKE_SOURCE,
    );
    if (!fs.existsSync(src)) return;
    const dest = path.join(
      gc.outputDir, "tests", "smoke", "smoke-config.md",
    );
    fs.mkdirSync(path.dirname(dest), { recursive: true });
    fs.copyFileSync(src, dest);
    gc.files.push(dest);
  }
}
