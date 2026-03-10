import { describe, it, expect, vi, afterEach } from "vitest";
import { resolve } from "node:path";
import { readFileSync } from "node:fs";
import {
  TemplateEngine,
  buildDefaultContext,
  PLACEHOLDER_PATTERN,
} from "../../src/template-engine.js";
import {
  ProjectConfig,
  ProjectIdentity,
  ArchitectureConfig,
  InterfaceConfig,
  LanguageConfig,
  FrameworkConfig,
} from "../../src/models.js";
import { aProjectConfig } from "../fixtures/project-config.fixture.js";

const FIXTURES_DIR = resolve(__dirname, "../fixtures");
const TEMPLATES_DIR = resolve(FIXTURES_DIR, "templates");
const REFERENCE_DIR = resolve(FIXTURES_DIR, "reference");

afterEach(() => {
  vi.restoreAllMocks();
});

// ---------------------------------------------------------------------------
// PLACEHOLDER_PATTERN
// ---------------------------------------------------------------------------

describe("PLACEHOLDER_PATTERN", () => {
  it("PLACEHOLDER_PATTERN_validPattern_matchesSingleWordInBraces", () => {
    const matches = [..."{project_name}".matchAll(PLACEHOLDER_PATTERN)];
    expect(matches).toHaveLength(1);
    expect(matches[0]![1]).toBe("project_name");
  });
});

// ---------------------------------------------------------------------------
// buildDefaultContext
// ---------------------------------------------------------------------------

describe("buildDefaultContext", () => {
  const config = aProjectConfig();
  const context = buildDefaultContext(config);

  it("buildDefaultContext_validConfig_returns24Fields", () => {
    expect(Object.keys(context)).toHaveLength(24);
  });

  it("buildDefaultContext_validConfig_mapsProjectFields", () => {
    expect(context["project_name"]).toBe("my-service");
    expect(context["project_purpose"]).toBe("A sample service");
  });

  it("buildDefaultContext_validConfig_mapsLanguageFields", () => {
    expect(context["language_name"]).toBe("python");
    expect(context["language_version"]).toBe("3.9");
  });

  it("buildDefaultContext_validConfig_mapsFrameworkFields", () => {
    expect(context["framework_name"]).toBe("click");
    expect(context["framework_version"]).toBe("8.1");
    expect(context["build_tool"]).toBe("pip");
  });

  it("buildDefaultContext_validConfig_mapsArchitectureFields", () => {
    expect(context["architecture_style"]).toBe("hexagonal");
    expect(context["domain_driven"]).toBe("True");
    expect(context["event_driven"]).toBe("False");
  });

  it("buildDefaultContext_validConfig_mapsInfraFields", () => {
    expect(context["container"]).toBe("docker");
    expect(context["orchestrator"]).toBe("kubernetes");
    expect(context["templating"]).toBe("kustomize");
    expect(context["iac"]).toBe("none");
    expect(context["registry"]).toBe("none");
    expect(context["api_gateway"]).toBe("none");
    expect(context["service_mesh"]).toBe("none");
  });

  it("buildDefaultContext_validConfig_mapsDataFields", () => {
    expect(context["database_name"]).toBe("postgresql");
    expect(context["cache_name"]).toBe("redis");
  });

  it("buildDefaultContext_validConfig_mapsTestingFields", () => {
    expect(context["smoke_tests"]).toBe("True");
    expect(context["contract_tests"]).toBe("False");
    expect(context["performance_tests"]).toBe("True");
    expect(context["coverage_line"]).toBe(95);
    expect(context["coverage_branch"]).toBe(90);
  });

  it.each([
    ["project_name", "my-service"],
    ["project_purpose", "A sample service"],
    ["language_name", "python"],
    ["language_version", "3.9"],
    ["framework_name", "click"],
    ["framework_version", "8.1"],
    ["build_tool", "pip"],
    ["architecture_style", "hexagonal"],
    ["domain_driven", "True"],
    ["event_driven", "False"],
    ["container", "docker"],
    ["orchestrator", "kubernetes"],
    ["templating", "kustomize"],
    ["iac", "none"],
    ["registry", "none"],
    ["api_gateway", "none"],
    ["service_mesh", "none"],
    ["database_name", "postgresql"],
    ["cache_name", "redis"],
    ["smoke_tests", "True"],
    ["contract_tests", "False"],
    ["performance_tests", "True"],
    ["coverage_line", 95],
    ["coverage_branch", 90],
  ] as const)(
    "buildDefaultContext_field_%s_mapsCorrectly",
    (field, expected) => {
      expect(context[field]).toBe(expected);
    },
  );
});

// ---------------------------------------------------------------------------
// constructor
// ---------------------------------------------------------------------------

describe("constructor", () => {
  it("constructor_validArgs_createsInstance", () => {
    const engine = new TemplateEngine(TEMPLATES_DIR, aProjectConfig());
    expect(engine).toBeInstanceOf(TemplateEngine);
  });

  it("constructor_nonExistentDir_createsInstanceWithoutError", () => {
    const engine = new TemplateEngine("/nonexistent/dir", aProjectConfig());
    expect(engine).toBeInstanceOf(TemplateEngine);
  });
});

// ---------------------------------------------------------------------------
// renderTemplate
// ---------------------------------------------------------------------------

describe("renderTemplate", () => {
  const engine = new TemplateEngine(TEMPLATES_DIR, aProjectConfig());

  it("renderTemplate_simpleTemplate_matchesReference", () => {
    const expected = readFileSync(
      resolve(REFERENCE_DIR, "simple_rendered.md"),
      "utf-8",
    );
    const result = engine.renderTemplate("simple.md.j2");
    expect(result).toBe(expected);
  });

  it("renderTemplate_multivarTemplate_matchesReference", () => {
    const expected = readFileSync(
      resolve(REFERENCE_DIR, "multivar_rendered.md"),
      "utf-8",
    );
    const result = engine.renderTemplate("multivar.md.j2");
    expect(result).toBe(expected);
  });

  it("renderTemplate_whitespaceTemplate_matchesReference", () => {
    const expected = readFileSync(
      resolve(REFERENCE_DIR, "whitespace_rendered.txt"),
      "utf-8",
    );
    const result = engine.renderTemplate("whitespace.txt.j2");
    expect(result).toBe(expected);
  });

  it("renderTemplate_withContextOverrides_usesOverrides", () => {
    const result = engine.renderTemplate("simple.md.j2", {
      project_name: "overridden-name",
    });
    expect(result).toContain("overridden-name");
    expect(result).not.toContain("my-service");
  });

  it("renderTemplate_trailingNewline_preserved", () => {
    const result = engine.renderTemplate("simple.md.j2");
    expect(result.endsWith("\n")).toBe(true);
  });

  it("renderTemplate_undefinedVariable_throwsError", () => {
    const minimalEngine = new TemplateEngine(TEMPLATES_DIR, aProjectConfig());
    expect(() =>
      minimalEngine.renderString("{{ unknown_var }}"),
    ).toThrow();
  });

  it("renderTemplate_nonExistentTemplate_throwsError", () => {
    expect(() =>
      engine.renderTemplate("does-not-exist.md.j2"),
    ).toThrow();
  });
});

// ---------------------------------------------------------------------------
// renderString
// ---------------------------------------------------------------------------

describe("renderString", () => {
  const engine = new TemplateEngine(TEMPLATES_DIR, aProjectConfig());

  it("renderString_simpleString_rendersVariables", () => {
    const result = engine.renderString("Hello {{ project_name }}");
    expect(result).toBe("Hello my-service");
  });

  it("renderString_withOverrides_usesOverrides", () => {
    const result = engine.renderString("Hello {{ project_name }}", {
      project_name: "other-service",
    });
    expect(result).toBe("Hello other-service");
  });

  it("renderString_noVariables_returnsUnchanged", () => {
    const result = engine.renderString("plain text");
    expect(result).toBe("plain text");
  });

  it("renderString_emptyString_returnsEmpty", () => {
    const result = engine.renderString("");
    expect(result).toBe("");
  });

  it("renderString_undefinedVariable_throwsError", () => {
    expect(() =>
      engine.renderString("{{ nonexistent }}"),
    ).toThrow();
  });
});

// ---------------------------------------------------------------------------
// replacePlaceholders
// ---------------------------------------------------------------------------

describe("replacePlaceholders", () => {
  const engine = new TemplateEngine(TEMPLATES_DIR, aProjectConfig());

  it("replacePlaceholders_legacyFixture_matchesReference", () => {
    const input = readFileSync(
      resolve(FIXTURES_DIR, "legacy_placeholders.txt"),
      "utf-8",
    );
    const expected = readFileSync(
      resolve(REFERENCE_DIR, "legacy_replaced.txt"),
      "utf-8",
    );
    const result = engine.replacePlaceholders(input);
    expect(result).toBe(expected);
  });

  it("replacePlaceholders_knownKeys_replacesAll", () => {
    const result = engine.replacePlaceholders("{project_name}");
    expect(result).toBe("my-service");
  });

  it("replacePlaceholders_unknownKey_preservesOriginal", () => {
    const result = engine.replacePlaceholders("{unknown_key}");
    expect(result).toBe("{unknown_key}");
  });

  it("replacePlaceholders_mixedKnownUnknown_replacesOnlyKnown", () => {
    const result = engine.replacePlaceholders(
      "{project_name} and {unknown_key}",
    );
    expect(result).toBe("my-service and {unknown_key}");
  });

  it("replacePlaceholders_noPlaceholders_returnsUnchanged", () => {
    const result = engine.replacePlaceholders("plain text");
    expect(result).toBe("plain text");
  });

  it("replacePlaceholders_emptyString_returnsEmpty", () => {
    const result = engine.replacePlaceholders("");
    expect(result).toBe("");
  });

  it("replacePlaceholders_withExplicitConfig_usesProvidedConfig", () => {
    const otherConfig = new ProjectConfig(
      new ProjectIdentity("other-project", "other purpose"),
      new ArchitectureConfig("layered"),
      [new InterfaceConfig("rest")],
      new LanguageConfig("java", "17"),
      new FrameworkConfig("spring", "3.0", "gradle"),
    );
    const result = engine.replacePlaceholders(
      "{project_name}",
      otherConfig,
    );
    expect(result).toBe("other-project");
  });

  it("replacePlaceholders_emptyBraces_noMatch", () => {
    const result = engine.replacePlaceholders("{}");
    expect(result).toBe("{}");
  });

  it("replacePlaceholders_specialCharsInBraces_noMatch", () => {
    const result = engine.replacePlaceholders("{project-name}");
    expect(result).toBe("{project-name}");
  });

  it("replacePlaceholders_nestedBraces_handlesCorrectly", () => {
    const result = engine.replacePlaceholders("{{project_name}}");
    expect(result).toContain("my-service");
  });
});

// ---------------------------------------------------------------------------
// injectSection (static)
// ---------------------------------------------------------------------------

describe("injectSection", () => {
  it("injectSection_fixtureMarker_matchesReference", () => {
    const base = readFileSync(
      resolve(FIXTURES_DIR, "section_base.md"),
      "utf-8",
    );
    const section = readFileSync(
      resolve(FIXTURES_DIR, "section_inject.md"),
      "utf-8",
    );
    const expected = readFileSync(
      resolve(REFERENCE_DIR, "section_injected.md"),
      "utf-8",
    );
    const result = TemplateEngine.injectSection(
      base,
      section,
      "<!-- INSERT:rules -->",
    );
    expect(result).toBe(expected);
  });

  it("injectSection_simpleMarker_replacesMarker", () => {
    const result = TemplateEngine.injectSection(
      "before MARKER after",
      "INJECTED",
      "MARKER",
    );
    expect(result).toBe("before INJECTED after");
  });

  it("injectSection_multipleOccurrences_replacesAll", () => {
    const result = TemplateEngine.injectSection(
      "A MARKER B MARKER C",
      "X",
      "MARKER",
    );
    expect(result).toBe("A X B X C");
  });

  it("injectSection_markerNotFound_returnsUnchanged", () => {
    const result = TemplateEngine.injectSection(
      "no marker here",
      "section",
      "MISSING",
    );
    expect(result).toBe("no marker here");
  });

  it("injectSection_emptySection_removesMarker", () => {
    const result = TemplateEngine.injectSection(
      "before MARKER after",
      "",
      "MARKER",
    );
    expect(result).toBe("before  after");
  });

  it("injectSection_emptyContent_returnsEmpty", () => {
    const result = TemplateEngine.injectSection("", "section", "MARKER");
    expect(result).toBe("");
  });
});

// ---------------------------------------------------------------------------
// concatFiles (static)
// ---------------------------------------------------------------------------

describe("concatFiles", () => {
  const fileA = resolve(FIXTURES_DIR, "concat_a.txt");
  const fileB = resolve(FIXTURES_DIR, "concat_b.txt");

  it("concatFiles_twoFixtures_matchesReference", () => {
    const expected = readFileSync(
      resolve(REFERENCE_DIR, "concat_result.txt"),
      "utf-8",
    );
    const result = TemplateEngine.concatFiles([fileA, fileB]);
    expect(result).toBe(expected);
  });

  it("concatFiles_defaultSeparator_usesNewline", () => {
    const result = TemplateEngine.concatFiles([fileA, fileB]);
    const contentA = readFileSync(fileA, "utf-8");
    const contentB = readFileSync(fileB, "utf-8");
    expect(result).toBe(contentA + "\n" + contentB);
  });

  it("concatFiles_customSeparator_usesSeparator", () => {
    const contentA = readFileSync(fileA, "utf-8");
    const contentB = readFileSync(fileB, "utf-8");
    const result = TemplateEngine.concatFiles([fileA, fileB], "---");
    expect(result).toBe(contentA + "---" + contentB);
  });

  it("concatFiles_emptyArray_returnsEmpty", () => {
    const result = TemplateEngine.concatFiles([]);
    expect(result).toBe("");
  });

  it("concatFiles_singleFile_returnsContentsNoSeparator", () => {
    const expected = readFileSync(fileA, "utf-8");
    const result = TemplateEngine.concatFiles([fileA]);
    expect(result).toBe(expected);
  });

  it("concatFiles_nonExistentFile_throwsError", () => {
    expect(() =>
      TemplateEngine.concatFiles(["/nonexistent/file.txt"]),
    ).toThrow();
  });
});
