import * as fs from "node:fs";
import nunjucks from "nunjucks";
import type { ProjectConfig } from "./models.js";

/** Regex pattern for legacy `{placeholder}` replacement. */
export const PLACEHOLDER_PATTERN = /\{(\w+)\}/g;

const PYTHON_TRUE = "True";
const PYTHON_FALSE = "False";

/**
 * Convert a JavaScript boolean to a Python-style string
 * for byte-for-byte parity with Jinja2 output.
 */
function toPythonBool(value: boolean): string {
  return value ? PYTHON_TRUE : PYTHON_FALSE;
}

/**
 * Build a flat context dictionary from a {@link ProjectConfig}.
 *
 * Returns 24 fields matching the Python `_build_default_context`
 * function. Boolean values are converted to Python-style strings
 * (`"True"` / `"False"`) for rendering parity with Jinja2.
 */
export function buildDefaultContext(
  config: ProjectConfig,
): Record<string, unknown> {
  return {
    project_name: config.project.name,
    project_purpose: config.project.purpose,
    language_name: config.language.name,
    language_version: config.language.version,
    framework_name: config.framework.name,
    framework_version: config.framework.version,
    build_tool: config.framework.buildTool,
    architecture_style: config.architecture.style,
    domain_driven: toPythonBool(config.architecture.domainDriven),
    event_driven: toPythonBool(config.architecture.eventDriven),
    container: config.infrastructure.container,
    orchestrator: config.infrastructure.orchestrator,
    templating: config.infrastructure.templating,
    iac: config.infrastructure.iac,
    registry: config.infrastructure.registry,
    api_gateway: config.infrastructure.apiGateway,
    service_mesh: config.infrastructure.serviceMesh,
    database_name: config.data.database.name,
    cache_name: config.data.cache.name,
    smoke_tests: toPythonBool(config.testing.smokeTests),
    contract_tests: toPythonBool(config.testing.contractTests),
    performance_tests: toPythonBool(config.testing.performanceTests),
    coverage_line: config.testing.coverageLine,
    coverage_branch: config.testing.coverageBranch,
  };
}

/**
 * Derive a string-only mapping from a pre-computed context.
 */
function toPlaceholderMap(
  context: Record<string, unknown>,
): Record<string, string> {
  const map: Record<string, string> = {};
  for (const [key, value] of Object.entries(context)) {
    map[key] = String(value);
  }
  return map;
}

/**
 * Nunjucks-based template rendering engine.
 *
 * Drop-in replacement for the Python Jinja2 `TemplateEngine`,
 * producing byte-for-byte identical output.
 */
export class TemplateEngine {
  private readonly env: nunjucks.Environment;
  private readonly defaultContext: Record<string, unknown>;
  private readonly placeholderMap: Record<string, string>;

  /**
   * Create a new template engine.
   *
   * @param resourcesDir - Absolute path to the templates root directory.
   * @param config - Project configuration used to build the default context.
   */
  constructor(resourcesDir: string, config: ProjectConfig) {
    const loader = new nunjucks.FileSystemLoader(resourcesDir);
    // autoescape disabled: output is config/markdown files, not browser-rendered HTML
    this.env = new nunjucks.Environment(loader, {
      autoescape: false,
      trimBlocks: false,
      lstripBlocks: false,
      throwOnUndefined: true,
    });
    this.defaultContext = buildDefaultContext(config);
    this.placeholderMap = toPlaceholderMap(this.defaultContext);
  }

  /**
   * Merge the default context with optional overrides.
   */
  private mergeContext(
    context?: Record<string, unknown>,
  ): Record<string, unknown> {
    const merged = { ...this.defaultContext };
    if (context) {
      Object.assign(merged, context);
    }
    return merged;
  }

  /**
   * Load and render a template file relative to the resources directory.
   *
   * @param templatePath - Relative path to the template file.
   * @param context - Optional context overrides.
   * @returns The rendered template string.
   * @throws Error if the template is not found or contains undefined variables.
   */
  renderTemplate(
    templatePath: string,
    context?: Record<string, unknown>,
  ): string {
    const merged = this.mergeContext(context);
    return this.env.render(templatePath, merged);
  }

  /**
   * Render an inline template string.
   *
   * @param templateStr - The Nunjucks template string.
   * @param context - Optional context overrides.
   * @returns The rendered string.
   * @throws Error if the string contains undefined variables.
   */
  renderString(
    templateStr: string,
    context?: Record<string, unknown>,
  ): string {
    const merged = this.mergeContext(context);
    return this.env.renderString(templateStr, merged);
  }

  /**
   * Replace legacy `{placeholder}` patterns with config values.
   *
   * Known keys are replaced; unknown keys are preserved verbatim.
   *
   * @param content - The input string with placeholders.
   * @param config - Optional config override; uses constructor config if omitted.
   * @returns The string with known placeholders replaced.
   */
  replacePlaceholders(
    content: string,
    config?: ProjectConfig,
  ): string {
    const mapping = config
      ? toPlaceholderMap(buildDefaultContext(config))
      : this.placeholderMap;

    return content.replace(PLACEHOLDER_PATTERN, (match, key: string) => {
      if (key in mapping) {
        return mapping[key]!;
      }
      return match;
    });
  }

  /**
   * Replace all occurrences of a marker in base content with the given section.
   *
   * @param baseContent - The base content containing the marker.
   * @param section - The content to inject in place of the marker.
   * @param marker - The marker string to replace.
   * @returns The content with all marker occurrences replaced.
   */
  static injectSection(
    baseContent: string,
    section: string,
    marker: string,
  ): string {
    return baseContent.replaceAll(marker, section);
  }

  /**
   * Read and concatenate files with a separator.
   *
   * @param paths - Absolute file paths to concatenate.
   * @param separator - Separator between file contents (default: `"\n"`).
   * @returns The concatenated content, or `""` for an empty array.
   * @throws Error if any file does not exist.
   */
  static concatFiles(
    paths: readonly string[],
    separator: string = "\n",
  ): string {
    if (paths.length === 0) {
      return "";
    }
    const contents: string[] = [];
    for (const path of paths) {
      contents.push(fs.readFileSync(path, "utf-8"));
    }
    return contents.join(separator);
  }
}
