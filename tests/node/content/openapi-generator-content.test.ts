import { describe, it, expect } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

const CLAUDE_SOURCE_PATH = path.resolve(
  __dirname,
  "../../..",
  "resources/skills-templates/core/x-dev-lifecycle/references/openapi-generator.md",
);

const GITHUB_SOURCE_PATH = path.resolve(
  __dirname,
  "../../..",
  "resources/github-skills-templates/dev/references/x-dev-lifecycle/openapi-generator.md",
);

describe("openapi-generator content validation", () => {
  describe("Claude source template", () => {
    it("templateExists_atExpectedPath_fileIsReadable", () => {
      expect(fs.existsSync(CLAUDE_SOURCE_PATH)).toBe(true);
    });

    it("templateContent_isNonEmpty_hasSubstantialContent", () => {
      const content = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
      const lines = content.split("\n").length;
      expect(lines).toBeGreaterThanOrEqual(50);
    });

    it("templateContent_containsOpenAPI31Requirement_specVersion", () => {
      const content = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
      expect(content).toMatch(/OpenAPI\s+3\.1|3\.1\.0/);
    });

    it("templateContent_containsOpenAPIKeyword_identifiesSpec", () => {
      const content = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
      expect(content).toContain("OpenAPI");
    });

    it("templateContent_containsRFC7807Reference_problemDetails", () => {
      const content = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
      expect(content).toMatch(/RFC\s*7807|Problem Details/i);
    });

    it("templateContent_containsProblemDetailFields_typeStatusTitleDetail", () => {
      const content = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
      for (const field of ["type", "title", "status", "detail"]) {
        expect(content).toContain(field);
      }
    });

    it("templateContent_containsRefInstruction_schemaDeduplication", () => {
      const content = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
      expect(content).toContain("$ref");
    });

    it("templateContent_containsComponentsSchemas_centralSchemaRegistry", () => {
      const content = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
      expect(content).toMatch(/components\/schemas|components\.schemas/);
    });

    it("templateContent_containsPathExtraction_endpointDiscovery", () => {
      const content = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
      expect(content).toMatch(/paths|endpoints/i);
    });

    it("templateContent_containsFrameworkPlaceholder_doubleOrSingleBrace", () => {
      const content = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
      expect(content).toMatch(/\{\{FRAMEWORK\}\}|\{framework_name\}/);
    });

    it("templateContent_containsLanguagePlaceholder_doubleOrSingleBrace", () => {
      const content = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
      expect(content).toMatch(/\{\{LANGUAGE\}\}|\{language_name\}/);
    });

    it("templateContent_containsOutputPath_docsApiOpenapiYaml", () => {
      const content = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
      expect(content).toContain("docs/api/openapi.yaml");
    });

    it.each([
      ["GET"], ["POST"], ["PUT"], ["DELETE"], ["PATCH"],
    ])(
      "templateContent_containsHTTPMethod_%s",
      (method) => {
        const content = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
        expect(content).toContain(method);
      },
    );

    it("templateContent_containsDTOExtraction_requestResponseSchemas", () => {
      const content = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
      expect(content).toMatch(/DTO|request.*response.*schema/is);
    });

    it("templateContent_containsErrorResponseHandling_statusCodes", () => {
      const content = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
      expect(content).toMatch(/400|404|422|500/);
    });

    it("templateContent_containsInfoSection_apiMetadata", () => {
      const content = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
      expect(content).toContain("info");
    });

    it("templateContent_containsServersSection_environmentURLs", () => {
      const content = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
      expect(content).toContain("servers");
    });

    it("templateContent_containsTagsSection_endpointGrouping", () => {
      const content = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
      expect(content).toContain("tags");
    });

    it("templateContent_containsInboundAdapterScanning_controllerHandlerResource", () => {
      const content = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
      expect(content).toMatch(/controller|handler|resource|adapter/i);
    });

    it("templateContent_containsYAMLFormat_outputFormat", () => {
      const content = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
      expect(content).toContain("YAML");
    });

    it("templateContent_containsConditionalInvocation_restInterfaceCheck", () => {
      const content = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
      expect(content).toMatch(/rest|REST/);
    });

    it("templateContent_containsSkipInstruction_nonRESTProjectsSkipped", () => {
      const content = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
      expect(content).toMatch(/[Ss]kip/);
      expect(content).toMatch(/non-REST|no REST|not.*REST/i);
    });

    it("templateContent_containsIncrementalUpdate_preserveExistingEndpoints", () => {
      const content = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
      expect(content).toMatch(/[Pp]reserve/);
      expect(content).toMatch(/[Ii]ncremental/);
    });
  });

  describe("GitHub source template", () => {
    it("templateExists_atExpectedPath_fileIsReadable", () => {
      expect(fs.existsSync(GITHUB_SOURCE_PATH)).toBe(true);
    });

    it("templateContent_isNonEmpty_hasSubstantialContent", () => {
      const content = fs.readFileSync(GITHUB_SOURCE_PATH, "utf-8");
      const lines = content.split("\n").length;
      expect(lines).toBeGreaterThanOrEqual(50);
    });

    it("templateContent_containsOpenAPI31Requirement_specVersion", () => {
      const content = fs.readFileSync(GITHUB_SOURCE_PATH, "utf-8");
      expect(content).toMatch(/OpenAPI\s+3\.1|3\.1\.0/);
    });

    it("templateContent_containsRFC7807Reference_problemDetails", () => {
      const content = fs.readFileSync(GITHUB_SOURCE_PATH, "utf-8");
      expect(content).toMatch(/RFC\s*7807|Problem Details/i);
    });

    it("templateContent_containsRefInstruction_schemaDeduplication", () => {
      const content = fs.readFileSync(GITHUB_SOURCE_PATH, "utf-8");
      expect(content).toContain("$ref");
    });

    it("templateContent_containsOutputPath_docsApiOpenapiYaml", () => {
      const content = fs.readFileSync(GITHUB_SOURCE_PATH, "utf-8");
      expect(content).toContain("docs/api/openapi.yaml");
    });

    it.each([
      ["GET"], ["POST"], ["PUT"], ["DELETE"], ["PATCH"],
    ])(
      "templateContent_containsHTTPMethod_%s",
      (method) => {
        const content = fs.readFileSync(GITHUB_SOURCE_PATH, "utf-8");
        expect(content).toContain(method);
      },
    );

    it("templateContent_containsInboundAdapterScanning_controllerHandlerResource", () => {
      const content = fs.readFileSync(GITHUB_SOURCE_PATH, "utf-8");
      expect(content).toMatch(/controller|handler|resource|adapter/i);
    });

    it("templateContent_containsYAMLFormat_outputFormat", () => {
      const content = fs.readFileSync(GITHUB_SOURCE_PATH, "utf-8");
      expect(content).toContain("YAML");
    });
  });

  describe("dual copy consistency (RULE-001)", () => {
    const DUAL_COPY_KEYWORDS = [
      "OpenAPI 3.1",
      "$ref",
      "docs/api/openapi.yaml",
      "YAML",
    ];

    it.each(
      DUAL_COPY_KEYWORDS.map((kw) => [kw]),
    )(
      "bothContain_%s_sameContent",
      (keyword) => {
        const claude = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
        const github = fs.readFileSync(GITHUB_SOURCE_PATH, "utf-8");
        expect(claude).toContain(keyword);
        expect(github).toContain(keyword);
      },
    );

    it("bothContainRFC7807_sameErrorPattern_problemDetails", () => {
      const claude = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
      const github = fs.readFileSync(GITHUB_SOURCE_PATH, "utf-8");
      expect(claude).toMatch(/RFC\s*7807|Problem Details/i);
      expect(github).toMatch(/RFC\s*7807|Problem Details/i);
    });

    it.each([
      ["GET"], ["POST"], ["PUT"], ["DELETE"], ["PATCH"],
    ])(
      "bothContainHTTPMethod_%s",
      (method) => {
        const claude = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
        const github = fs.readFileSync(GITHUB_SOURCE_PATH, "utf-8");
        expect(claude).toContain(method);
        expect(github).toContain(method);
      },
    );

    it("bothContainInboundAdapterScanning_scanInstructions", () => {
      const claude = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
      const github = fs.readFileSync(GITHUB_SOURCE_PATH, "utf-8");
      expect(claude).toMatch(/controller|handler|resource|adapter/i);
      expect(github).toMatch(/controller|handler|resource|adapter/i);
    });

    it("bothContainDTOExtraction_schemaInstructions", () => {
      const claude = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
      const github = fs.readFileSync(GITHUB_SOURCE_PATH, "utf-8");
      expect(claude).toMatch(/DTO|request.*response.*schema/is);
      expect(github).toMatch(/DTO|request.*response.*schema/is);
    });

    it("bothContainErrorResponseHandling_statusCodes", () => {
      const claude = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
      const github = fs.readFileSync(GITHUB_SOURCE_PATH, "utf-8");
      expect(claude).toMatch(/400|404|422|500/);
      expect(github).toMatch(/400|404|422|500/);
    });
  });
});
