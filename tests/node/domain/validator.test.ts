import { describe, it, expect } from "vitest";
import { mkdtempSync, mkdirSync } from "node:fs";
import { join } from "node:path";
import { tmpdir } from "node:os";

import {
  validateStack,
  verifyCrossReferences,
  extractMajor,
  extractMinor,
} from "../../../src/domain/validator.js";
import {
  ProjectConfig,
  ProjectIdentity,
  ArchitectureConfig,
  InterfaceConfig,
  LanguageConfig,
  FrameworkConfig,
} from "../../../src/models.js";

function buildConfig(overrides: {
  language?: { name: string; version: string };
  framework?: {
    name: string; version: string;
    buildTool?: string; nativeBuild?: boolean;
  };
  architecture?: { style: string };
  interfaces?: Array<{ type: string }>;
} = {}): ProjectConfig {
  const lang = overrides.language ?? { name: "java", version: "21" };
  const fw = overrides.framework ?? {
    name: "quarkus", version: "3.0",
    buildTool: "maven", nativeBuild: false,
  };
  const arch = overrides.architecture ?? { style: "microservice" };
  const ifaces = overrides.interfaces ?? [{ type: "rest" }];
  return new ProjectConfig(
    new ProjectIdentity("test", "test"),
    new ArchitectureConfig(arch.style),
    ifaces.map((i) => new InterfaceConfig(i.type)),
    new LanguageConfig(lang.name, lang.version),
    new FrameworkConfig(
      fw.name, fw.version,
      fw.buildTool ?? "maven",
      fw.nativeBuild ?? false,
    ),
  );
}

describe("validateStack", () => {
  describe("valid framework-language combos", () => {
    it.each([
      ["java", "quarkus"],
      ["kotlin", "quarkus"],
      ["java", "spring-boot"],
      ["kotlin", "spring-boot"],
      ["typescript", "nestjs"],
      ["typescript", "express"],
      ["typescript", "fastify"],
      ["python", "fastapi"],
      ["python", "django"],
      ["python", "flask"],
      ["go", "gin"],
      ["kotlin", "ktor"],
      ["rust", "axum"],
      ["rust", "actix-web"],
      ["csharp", "aspnet"],
    ])(
      "%s_%s_returnsNoErrors",
      (lang, fw) => {
        const config = buildConfig({
          language: { name: lang, version: "17" },
          framework: {
            name: fw, version: "3.0",
            buildTool: "maven", nativeBuild: false,
          },
          architecture: { style: "microservice" },
          interfaces: [{ type: "rest" }],
        });
        expect(validateStack(config)).toEqual([]);
      },
    );
  });

  describe("invalid framework-language combos", () => {
    it.each([
      ["java", "fastapi"],
      ["python", "quarkus"],
      ["go", "spring-boot"],
      ["typescript", "gin"],
      ["rust", "django"],
      ["csharp", "quarkus"],
      ["java", "nestjs"],
      ["python", "express"],
      ["go", "ktor"],
      ["rust", "flask"],
    ])(
      "%s_%s_returnsError",
      (lang, fw) => {
        const config = buildConfig({
          language: { name: lang, version: "17" },
          framework: {
            name: fw, version: "3.0",
            buildTool: "maven", nativeBuild: false,
          },
        });
        const errors = validateStack(config);
        expect(errors.length).toBeGreaterThanOrEqual(1);
        expect(errors[0]).toContain("requires language");
      },
    );
  });

  describe("version constraints", () => {
    it.each([
      ["java", "11", "quarkus", "3.0", "requires Java 17+"],
      ["java", "11", "spring-boot", "3.2", "requires Java 17+"],
      ["python", "3.8", "django", "5.0", "requires Python 3.10+"],
    ])(
      "%s_%s_%s_%s_returnsVersionError",
      (lang, langVer, fw, fwVer, errFragment) => {
        const config = buildConfig({
          language: { name: lang, version: langVer },
          framework: {
            name: fw, version: fwVer,
            buildTool: "maven", nativeBuild: false,
          },
        });
        const errors = validateStack(config);
        const matching = errors.filter((e) => e.includes(errFragment));
        expect(matching.length).toBeGreaterThanOrEqual(1);
      },
    );

    it("java17_quarkus3_noError", () => {
      const config = buildConfig({
        language: { name: "java", version: "17" },
        framework: {
          name: "quarkus", version: "3.0",
          buildTool: "maven", nativeBuild: false,
        },
      });
      expect(validateStack(config)).toEqual([]);
    });

    it("java16_springBoot3_returnsError", () => {
      const config = buildConfig({
        language: { name: "java", version: "16" },
        framework: {
          name: "spring-boot", version: "3.0",
          buildTool: "maven", nativeBuild: false,
        },
      });
      const errors = validateStack(config);
      expect(errors.some((e) => e.includes("requires Java 17+"))).toBe(true);
    });

    it("python310_django5_noError", () => {
      const config = buildConfig({
        language: { name: "python", version: "3.10" },
        framework: {
          name: "django", version: "5.0",
          buildTool: "pip", nativeBuild: false,
        },
      });
      expect(validateStack(config)).toEqual([]);
    });

    it("python39_django5_returnsError", () => {
      const config = buildConfig({
        language: { name: "python", version: "3.9" },
        framework: {
          name: "django", version: "5.0",
          buildTool: "pip", nativeBuild: false,
        },
      });
      const errors = validateStack(config);
      expect(errors.some((e) => e.includes("requires Python 3.10+"))).toBe(
        true,
      );
    });

    it("springBoot2x_java11_noError", () => {
      const config = buildConfig({
        language: { name: "java", version: "11" },
        framework: {
          name: "spring-boot", version: "2.7",
          buildTool: "maven", nativeBuild: false,
        },
      });
      expect(validateStack(config)).toEqual([]);
    });

    it("django4x_python38_noError", () => {
      const config = buildConfig({
        language: { name: "python", version: "3.8" },
        framework: {
          name: "django", version: "4.2",
          buildTool: "pip", nativeBuild: false,
        },
      });
      expect(validateStack(config)).toEqual([]);
    });
  });

  describe("native build", () => {
    it("unsupportedFramework_returnsError", () => {
      const config = buildConfig({
        framework: {
          name: "gin", version: "1.9",
          buildTool: "go", nativeBuild: true,
        },
        language: { name: "go", version: "1.21" },
      });
      const errors = validateStack(config);
      const matching = errors.filter(
        (e) => e.includes("Native build is not supported"),
      );
      expect(matching).toHaveLength(1);
    });

    it("supportedFramework_noError", () => {
      const config = buildConfig({
        language: { name: "java", version: "17" },
        framework: {
          name: "quarkus", version: "3.0",
          buildTool: "maven", nativeBuild: true,
        },
      });
      expect(validateStack(config)).toEqual([]);
    });
  });

  describe("interface types", () => {
    it("invalidType_returnsError", () => {
      const config = buildConfig({ interfaces: [{ type: "soap" }] });
      const errors = validateStack(config);
      const matching = errors.filter(
        (e) => e.includes("Invalid interface type"),
      );
      expect(matching).toHaveLength(1);
      expect(matching[0]).toContain("'soap'");
    });

    it("validTypes_noError", () => {
      const config = buildConfig({
        interfaces: [{ type: "rest" }, { type: "grpc" }],
      });
      expect(validateStack(config)).toEqual([]);
    });

    it("eventProducer_valid", () => {
      const config = buildConfig({
        interfaces: [{ type: "event-producer" }],
      });
      expect(validateStack(config)).toEqual([]);
    });
  });

  describe("architecture style", () => {
    it("invalidStyle_returnsError", () => {
      const config = buildConfig({
        architecture: { style: "hexagonal" },
      });
      const errors = validateStack(config);
      const matching = errors.filter(
        (e) => e.includes("Invalid architecture style"),
      );
      expect(matching).toHaveLength(1);
      expect(matching[0]).toContain("'hexagonal'");
    });

    it("validStyle_noError", () => {
      const config = buildConfig({
        architecture: { style: "microservice" },
      });
      expect(validateStack(config)).toEqual([]);
    });
  });

  describe("multiple errors", () => {
    it("multipleViolations_returnsAllErrors", () => {
      const config = buildConfig({
        language: { name: "python", version: "3.8" },
        framework: {
          name: "quarkus", version: "3.0",
          buildTool: "maven", nativeBuild: true,
        },
        architecture: { style: "invalid-arch" },
        interfaces: [{ type: "soap" }],
      });
      const errors = validateStack(config);
      expect(errors.length).toBeGreaterThanOrEqual(3);
    });
  });

  describe("version parsing edge cases", () => {
    it("emptyVersion_noError", () => {
      const config = buildConfig({
        language: { name: "java", version: "" },
        framework: {
          name: "quarkus", version: "",
          buildTool: "maven", nativeBuild: false,
        },
      });
      expect(validateStack(config)).toEqual([]);
    });

    it("nonNumericVersion_noCrash", () => {
      const config = buildConfig({
        language: { name: "java", version: "latest" },
        framework: {
          name: "quarkus", version: "latest",
          buildTool: "maven", nativeBuild: false,
        },
      });
      expect(validateStack(config)).toBeInstanceOf(Array);
    });

    it("alphaMajorVersion_noCrash", () => {
      const config = buildConfig({
        language: { name: "java", version: "abc.def" },
        framework: {
          name: "quarkus", version: "xyz",
          buildTool: "maven", nativeBuild: false,
        },
      });
      expect(validateStack(config)).toBeInstanceOf(Array);
    });

    it("alphaMinorVersion_noCrash", () => {
      const config = buildConfig({
        language: { name: "python", version: "3.abc" },
        framework: {
          name: "django", version: "5.0",
          buildTool: "pip", nativeBuild: false,
        },
      });
      expect(validateStack(config)).toBeInstanceOf(Array);
    });
  });
});

describe("extractMajor", () => {
  it.each([
    ["21", 21],
    ["3.10", 3],
    ["17.0.2", 17],
    ["5", 5],
    ["1.21", 1],
  ])("validVersion_%s_returns_%i", (input, expected) => {
    expect(extractMajor(input)).toBe(expected);
  });

  it.each([
    ["", undefined],
    ["latest", undefined],
    ["abc", undefined],
  ])("invalidVersion_%s_returnsUndefined", (input, expected) => {
    expect(extractMajor(input)).toBe(expected);
  });
});

describe("extractMinor", () => {
  it.each([
    ["3.10", 10],
    ["17.0.2", 0],
    ["5.1", 1],
    ["1.21.3", 21],
  ])("validVersion_%s_returnsMinor", (input, expected) => {
    expect(extractMinor(input)).toBe(expected);
  });

  it.each([
    ["21", undefined],
    ["", undefined],
    ["abc.def", undefined],
  ])("invalidVersion_%s_returnsUndefined", (input, expected) => {
    expect(extractMinor(input)).toBe(expected);
  });
});

describe("verifyCrossReferences", () => {
  it("allDirectoriesExist_returnsEmpty", () => {
    const tmpDir = mkdtempSync(join(tmpdir(), "validator-"));
    mkdirSync(join(tmpDir, "skills"));
    mkdirSync(join(tmpDir, ".claude", "rules"), { recursive: true });
    const config = buildConfig();
    expect(verifyCrossReferences(config, tmpDir)).toEqual([]);
  });

  it("missingDirectories_returnsErrors", () => {
    const tmpDir = mkdtempSync(join(tmpdir(), "validator-"));
    const config = buildConfig();
    const errors = verifyCrossReferences(config, tmpDir);
    expect(errors).toHaveLength(2);
    expect(errors.some((e) => e.includes("skills"))).toBe(true);
    expect(errors.some((e) => e.includes(".claude/rules"))).toBe(true);
  });

  it("nonexistentSource_returnsError", () => {
    const tmpDir = mkdtempSync(join(tmpdir(), "validator-"));
    const fakeDir = join(tmpDir, "nonexistent");
    const config = buildConfig();
    const errors = verifyCrossReferences(config, fakeDir);
    expect(errors).toHaveLength(1);
    expect(errors[0]).toContain("does not exist");
  });

  it("partialDirectories_returnsPartialErrors", () => {
    const tmpDir = mkdtempSync(join(tmpdir(), "validator-"));
    mkdirSync(join(tmpDir, "skills"));
    const config = buildConfig();
    const errors = verifyCrossReferences(config, tmpDir);
    expect(errors).toHaveLength(1);
    expect(errors[0]).toContain(".claude/rules");
  });
});
