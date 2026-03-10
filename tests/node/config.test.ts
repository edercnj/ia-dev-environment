import { describe, it, expect, vi, afterEach } from "vitest";
import { resolve, join } from "node:path";
import { mkdtempSync, writeFileSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import {
  detectV2Format,
  migrateV2ToV3,
  validateConfig,
  loadConfig,
  REQUIRED_SECTIONS,
  TYPE_MAPPING,
  STACK_MAPPING,
} from "../../src/config.js";
import { CliError, ConfigParseError, ConfigValidationError } from "../../src/exceptions.js";
import { ProjectConfig } from "../../src/models.js";

const FIXTURES_DIR = resolve(__dirname, "../fixtures");

describe("detectV2Format", () => {
  it.each(Object.keys(TYPE_MAPPING))(
    "detectV2Format_typeIs%s_returnsTrue",
    (type) => {
      expect(detectV2Format({ type })).toBe(true);
    },
  );

  it.each(Object.keys(STACK_MAPPING))(
    "detectV2Format_stackIs%s_returnsTrue",
    (stack) => {
      expect(detectV2Format({ stack })).toBe(true);
    },
  );

  it("detectV2Format_bothTypeAndStack_returnsTrue", () => {
    expect(
      detectV2Format({ type: "api", stack: "java-spring" }),
    ).toBe(true);
  });

  it("detectV2Format_neitherTypeNorStack_returnsFalse", () => {
    expect(
      detectV2Format({ project: { name: "x" } }),
    ).toBe(false);
  });

  it("detectV2Format_emptyObject_returnsFalse", () => {
    expect(detectV2Format({})).toBe(false);
  });

  it("detectV2Format_unknownType_returnsFalse", () => {
    expect(detectV2Format({ type: "unknown" })).toBe(false);
  });

  it("detectV2Format_unknownStack_returnsFalse", () => {
    expect(detectV2Format({ stack: "unknown" })).toBe(false);
  });
});

describe("migrateV2ToV3 — TYPE_MAPPING", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it.each(
    Object.entries(TYPE_MAPPING).map(([type, mapping]) => ({
      type,
      expectedStyle: mapping.style,
      expectedInterfaces: mapping.interfaces,
    })),
  )(
    "migrateV2ToV3_type$type_producesCorrectArchAndInterfaces",
    ({ type, expectedStyle, expectedInterfaces }) => {
      vi.spyOn(console, "warn").mockImplementation(() => {});
      const data = {
        type,
        stack: "java-spring",
        project: { name: "test", purpose: "t" },
      };

      const result = migrateV2ToV3(data);

      const arch = result["architecture"] as { style: string };
      expect(arch.style).toBe(expectedStyle);
      expect(result["interfaces"]).toEqual(expectedInterfaces);
    },
  );
});

describe("migrateV2ToV3 — STACK_MAPPING", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it.each(
    Object.entries(STACK_MAPPING).map(([stack, mapping]) => ({
      stack,
      expectedLang: mapping.language,
      expectedVer: mapping.version,
      expectedFw: mapping.framework,
      expectedFwVer: mapping.frameworkVersion,
    })),
  )(
    "migrateV2ToV3_stack$stack_producesCorrectLangAndFramework",
    ({ stack, expectedLang, expectedVer, expectedFw, expectedFwVer }) => {
      vi.spyOn(console, "warn").mockImplementation(() => {});
      const data = {
        type: "api",
        stack,
        project: { name: "test", purpose: "t" },
      };

      const result = migrateV2ToV3(data);

      const lang = result["language"] as {
        name: string;
        version: string;
      };
      const fw = result["framework"] as {
        name: string;
        version: string;
      };
      expect(lang.name).toBe(expectedLang);
      expect(lang.version).toBe(expectedVer);
      expect(fw.name).toBe(expectedFw);
      expect(fw.version).toBe(expectedFwVer);
    },
  );
});

describe("migrateV2ToV3 — edge cases", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("migrateV2ToV3_called_emitsDeprecationWarning", () => {
    const warnSpy = vi
      .spyOn(console, "warn")
      .mockImplementation(() => {});
    const data = {
      type: "api",
      stack: "java-spring",
      project: { name: "t", purpose: "" },
    };

    migrateV2ToV3(data);

    expect(warnSpy).toHaveBeenCalledWith(
      "Config uses legacy v2 format. Auto-migrating to v3.",
    );
  });

  it("migrateV2ToV3_withExistingProject_preservesProject", () => {
    vi.spyOn(console, "warn").mockImplementation(() => {});
    const project = { name: "my-app", purpose: "My app" };
    const data = {
      type: "api",
      stack: "java-spring",
      project,
    };

    const result = migrateV2ToV3(data);

    expect(result["project"]).toEqual(project);
  });

  it("migrateV2ToV3_withoutProject_defaultsToUnnamed", () => {
    vi.spyOn(console, "warn").mockImplementation(() => {});
    const data = { type: "api", stack: "java-spring" };

    const result = migrateV2ToV3(data);

    expect(result["project"]).toEqual({
      name: "unnamed",
      purpose: "",
    });
  });

  it("migrateV2ToV3_unknownStack_throwsCliError", () => {
    vi.spyOn(console, "warn").mockImplementation(() => {});
    const data = {
      type: "api",
      stack: "unknown-stack",
      project: { name: "t", purpose: "" },
    };

    expect(() => migrateV2ToV3(data)).toThrow(CliError);
  });

  it("migrateV2ToV3_unknownStack_doesNotEmitWarning", () => {
    const warnSpy = vi
      .spyOn(console, "warn")
      .mockImplementation(() => {});
    const data = {
      type: "api",
      stack: "unknown-stack",
      project: { name: "t", purpose: "" },
    };

    try {
      migrateV2ToV3(data);
    } catch {
      // expected
    }

    expect(warnSpy).not.toHaveBeenCalled();
  });

  it("migrateV2ToV3_unknownType_usesDefaultTypeMapping", () => {
    vi.spyOn(console, "warn").mockImplementation(() => {});
    const data = {
      type: "unknown-type",
      stack: "java-spring",
      project: { name: "t", purpose: "" },
    };

    const result = migrateV2ToV3(data);

    const arch = result["architecture"] as { style: string };
    expect(arch.style).toBe("microservice");
    expect(result["interfaces"]).toEqual([{ type: "rest" }]);
  });

  it("migrateV2ToV3_absentType_usesDefaultTypeMapping", () => {
    vi.spyOn(console, "warn").mockImplementation(() => {});
    const data = {
      stack: "java-spring",
      project: { name: "t", purpose: "" },
    };

    const result = migrateV2ToV3(data);

    const arch = result["architecture"] as { style: string };
    expect(arch.style).toBe("microservice");
    expect(result["interfaces"]).toEqual([{ type: "rest" }]);
  });
});

describe("validateConfig", () => {
  it("validateConfig_validData_doesNotThrow", () => {
    const data: Record<string, unknown> = {
      project: {},
      architecture: {},
      interfaces: [],
      language: {},
      framework: {},
    };

    expect(() => validateConfig(data)).not.toThrow();
  });

  it.each(REQUIRED_SECTIONS.map((s) => [s]))(
    "validateConfig_missing%s_throwsWithSectionName",
    (section) => {
      const data: Record<string, unknown> = {
        project: {},
        architecture: {},
        interfaces: [],
        language: {},
        framework: {},
      };
      delete data[section];

      let caught: ConfigValidationError | undefined;
      try {
        validateConfig(data);
      } catch (e) {
        caught = e as ConfigValidationError;
      }

      expect(caught).toBeInstanceOf(ConfigValidationError);
      expect(caught!.missingFields).toContain(section);
    },
  );

  it("validateConfig_nullInput_throwsWithAllSections", () => {
    let caught: ConfigValidationError | undefined;
    try {
      validateConfig(null);
    } catch (e) {
      caught = e as ConfigValidationError;
    }

    expect(caught).toBeInstanceOf(ConfigValidationError);
    expect(caught!.missingFields).toEqual([...REQUIRED_SECTIONS]);
  });

  it("validateConfig_undefinedInput_throwsWithAllSections", () => {
    let caught: ConfigValidationError | undefined;
    try {
      validateConfig(undefined);
    } catch (e) {
      caught = e as ConfigValidationError;
    }

    expect(caught).toBeInstanceOf(ConfigValidationError);
    expect(caught!.missingFields).toEqual([...REQUIRED_SECTIONS]);
  });

  it("validateConfig_emptyObject_throwsWithAllSections", () => {
    let caught: ConfigValidationError | undefined;
    try {
      validateConfig({});
    } catch (e) {
      caught = e as ConfigValidationError;
    }

    expect(caught).toBeInstanceOf(ConfigValidationError);
    expect(caught!.missingFields).toEqual([...REQUIRED_SECTIONS]);
  });
});

describe("loadConfig", () => {
  let tempDir: string;

  afterEach(() => {
    if (tempDir) {
      rmSync(tempDir, { recursive: true, force: true });
    }
    vi.restoreAllMocks();
  });

  it("loadConfig_validV3File_returnsProjectConfig", () => {
    const filePath = join(FIXTURES_DIR, "valid-v3.yaml");

    const result = loadConfig(filePath);

    expect(result).toBeInstanceOf(ProjectConfig);
    expect(result.project.name).toBe("test-project");
    expect(result.architecture.style).toBe("microservice");
    expect(result.language.name).toBe("java");
    expect(result.language.version).toBe("21");
    expect(result.framework.name).toBe("spring-boot");
    expect(result.framework.version).toBe("3.4");
    expect(result.interfaces).toHaveLength(1);
    expect(result.interfaces[0]!.type).toBe("rest");
  });

  it("loadConfig_v2ApiJavaSpring_migratesAndReturns", () => {
    vi.spyOn(console, "warn").mockImplementation(() => {});
    const filePath = join(
      FIXTURES_DIR,
      "v2-api-java-spring.yaml",
    );

    const result = loadConfig(filePath);

    expect(result).toBeInstanceOf(ProjectConfig);
    expect(result.project.name).toBe("my-api");
    expect(result.architecture.style).toBe("microservice");
    expect(result.language.name).toBe("java");
    expect(result.framework.name).toBe("spring-boot");
  });

  it("loadConfig_v2WorkerPythonFastapi_migratesAndReturns", () => {
    vi.spyOn(console, "warn").mockImplementation(() => {});
    const filePath = join(
      FIXTURES_DIR,
      "v2-worker-python-fastapi.yaml",
    );

    const result = loadConfig(filePath);

    expect(result).toBeInstanceOf(ProjectConfig);
    expect(result.project.name).toBe("my-worker");
    expect(result.architecture.style).toBe("microservice");
    expect(result.language.name).toBe("python");
    expect(result.framework.name).toBe("fastapi");
  });

  it("loadConfig_missingSections_throwsConfigValidationError", () => {
    const filePath = join(
      FIXTURES_DIR,
      "invalid-missing-language.yaml",
    );

    expect(() => loadConfig(filePath)).toThrow(
      ConfigValidationError,
    );
  });

  it("loadConfig_nonExistentFile_throwsError", () => {
    expect(() => loadConfig("/nonexistent/path.yaml")).toThrow();
  });

  it("loadConfig_malformedYaml_throwsConfigParseError", () => {
    tempDir = mkdtempSync(join(tmpdir(), "config-test-"));
    const filePath = join(tempDir, "bad.yaml");
    writeFileSync(filePath, ":\n  invalid: [unclosed");

    expect(() => loadConfig(filePath)).toThrow(ConfigParseError);
  });

  it("loadConfig_emptyFile_throwsConfigValidationError", () => {
    tempDir = mkdtempSync(join(tmpdir(), "config-test-"));
    const filePath = join(tempDir, "empty.yaml");
    writeFileSync(filePath, "");

    expect(() => loadConfig(filePath)).toThrow(
      ConfigValidationError,
    );
  });

  it("loadConfig_scalarContent_throwsConfigValidationError", () => {
    tempDir = mkdtempSync(join(tmpdir(), "config-test-"));
    const filePath = join(tempDir, "scalar.yaml");
    writeFileSync(filePath, "just a string");

    expect(() => loadConfig(filePath)).toThrow(
      ConfigValidationError,
    );
  });

  it("loadConfig_tempV3File_returnsProjectConfig", () => {
    tempDir = mkdtempSync(join(tmpdir(), "config-test-"));
    const filePath = join(tempDir, "config.yaml");
    writeFileSync(
      filePath,
      [
        "project:",
        "  name: temp-project",
        "  purpose: Temp test",
        "architecture:",
        "  style: library",
        "interfaces:",
        "  - type: cli",
        "language:",
        "  name: typescript",
        '  version: "5"',
        "framework:",
        "  name: commander",
        '  version: "12"',
      ].join("\n"),
    );

    const result = loadConfig(filePath);

    expect(result).toBeInstanceOf(ProjectConfig);
    expect(result.project.name).toBe("temp-project");
    expect(result.language.name).toBe("typescript");
  });
});
