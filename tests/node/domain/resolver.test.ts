import { describe, it, expect } from "vitest";

import { resolveStack } from "../../../src/domain/resolver.js";
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

describe("resolveStack", () => {
  describe("language commands", () => {
    it.each([
      [
        "java", "maven", "quarkus",
        "./mvnw package -DskipTests", "./mvnw verify",
        "./mvnw compile -q", "./mvnw verify jacoco:report",
        ".java", "pom.xml",
      ],
      [
        "java", "gradle", "spring-boot",
        "./gradlew build -x test", "./gradlew test",
        "./gradlew compileJava -q", "./gradlew test jacocoTestReport",
        ".java", "build.gradle",
      ],
      [
        "kotlin", "gradle", "ktor",
        "./gradlew build -x test", "./gradlew test",
        "./gradlew compileKotlin -q", "./gradlew test jacocoTestReport",
        ".kt", "build.gradle.kts",
      ],
      [
        "typescript", "npm", "nestjs",
        "npm run build", "npm test",
        "npx --no-install tsc --noEmit", "npm test -- --coverage",
        ".ts", "package.json",
      ],
      [
        "python", "pip", "fastapi",
        "pip install -e .", "pytest",
        "python3 -m py_compile", "pytest --cov",
        ".py", "pyproject.toml",
      ],
      [
        "go", "go", "gin",
        "go build ./...", "go test ./...",
        "go build ./...", "go test -coverprofile=coverage.out ./...",
        ".go", "go.mod",
      ],
      [
        "rust", "cargo", "axum",
        "cargo build", "cargo test",
        "cargo check", "cargo tarpaulin",
        ".rs", "Cargo.toml",
      ],
      [
        "csharp", "dotnet", "aspnet",
        "dotnet build", "dotnet test",
        "dotnet build --no-restore --verbosity quiet",
        'dotnet test --collect:"XPlat Code Coverage"',
        ".cs", "*.csproj",
      ],
    ])(
      "%s_%s_returnsCorrectCommands",
      (lang, buildTool, fw, expBuild, expTest, expCompile,
        expCoverage, expExt, expFile) => {
        const config = buildConfig({
          language: { name: lang, version: "17" },
          framework: {
            name: fw, version: "3.0",
            buildTool, nativeBuild: false,
          },
        });
        const result = resolveStack(config);
        expect(result.buildCmd).toBe(expBuild);
        expect(result.testCmd).toBe(expTest);
        expect(result.compileCmd).toBe(expCompile);
        expect(result.coverageCmd).toBe(expCoverage);
        expect(result.fileExtension).toBe(expExt);
        expect(result.buildFile).toBe(expFile);
      },
    );

    it("unknownLanguageBuildTool_returnsEmptyCommands", () => {
      const config = buildConfig({
        language: { name: "haskell", version: "9" },
        framework: {
          name: "unknown", version: "1.0",
          buildTool: "cabal", nativeBuild: false,
        },
      });
      const result = resolveStack(config);
      expect(result.buildCmd).toBe("");
      expect(result.testCmd).toBe("");
    });
  });

  describe("default port", () => {
    it.each([
      ["quarkus", 8080],
      ["spring-boot", 8080],
      ["nestjs", 3000],
      ["express", 3000],
      ["fastapi", 8000],
      ["django", 8000],
      ["gin", 8080],
      ["ktor", 8080],
      ["axum", 3000],
      ["actix-web", 8080],
      ["aspnet", 5000],
    ])("%s_returnsPort_%i", (fw, expectedPort) => {
      const config = buildConfig({
        framework: {
          name: fw, version: "3.0",
          buildTool: "maven", nativeBuild: false,
        },
      });
      expect(resolveStack(config).defaultPort).toBe(expectedPort);
    });

    it("unknownFramework_returnsFallbackPort", () => {
      const config = buildConfig({
        framework: {
          name: "unknown", version: "1.0",
          buildTool: "maven", nativeBuild: false,
        },
      });
      expect(resolveStack(config).defaultPort).toBe(8080);
    });
  });

  describe("health path", () => {
    it.each([
      ["quarkus", "/q/health"],
      ["spring-boot", "/actuator/health"],
      ["nestjs", "/health"],
      ["express", "/health"],
      ["fastapi", "/health"],
      ["django", "/health"],
      ["gin", "/health"],
      ["ktor", "/health"],
      ["axum", "/health"],
      ["actix-web", "/health"],
      ["aspnet", "/health"],
    ])("%s_returnsHealthPath_%s", (fw, expectedPath) => {
      const config = buildConfig({
        framework: {
          name: fw, version: "3.0",
          buildTool: "maven", nativeBuild: false,
        },
      });
      expect(resolveStack(config).healthPath).toBe(expectedPath);
    });

    it("unknownFramework_returnsFallbackHealthPath", () => {
      const config = buildConfig({
        framework: {
          name: "unknown", version: "1.0",
          buildTool: "maven", nativeBuild: false,
        },
      });
      expect(resolveStack(config).healthPath).toBe("/health");
    });
  });

  describe("docker image", () => {
    it.each([
      ["java", "17", "eclipse-temurin:17-jre-alpine"],
      ["java", "21", "eclipse-temurin:21-jre-alpine"],
      ["kotlin", "17", "eclipse-temurin:17-jre-alpine"],
      ["typescript", "18", "node:18-alpine"],
      ["python", "3.9", "python:3.9-slim"],
      ["go", "1.21", "golang:1.21-alpine"],
      ["rust", "1.75", "rust:1.75-slim"],
      ["csharp", "8.0", "mcr.microsoft.com/dotnet/aspnet:8.0"],
    ])("%s_%s_returnsImage_%s", (lang, version, expectedImage) => {
      const config = buildConfig({
        language: { name: lang, version },
      });
      expect(resolveStack(config).dockerBaseImage).toBe(expectedImage);
    });

    it("unknownLanguage_returnsDefaultImage", () => {
      const config = buildConfig({
        language: { name: "haskell", version: "9" },
      });
      expect(resolveStack(config).dockerBaseImage).toBe("alpine:latest");
    });

    it("specialVersionChars_noCrash", () => {
      const config = buildConfig({
        language: { name: "python", version: "3.9-rc1" },
      });
      expect(resolveStack(config).dockerBaseImage).toContain("3.9-rc1");
    });
  });

  describe("protocols", () => {
    it.each([
      [[{ type: "rest" }], ["openapi"]],
      [[{ type: "grpc" }], ["proto3"]],
      [
        [{ type: "rest" }, { type: "grpc" }, { type: "event-consumer" }],
        ["openapi", "proto3", "kafka"],
      ],
      [[{ type: "event-producer" }], ["kafka"]],
      [[{ type: "cli" }], []],
      [
        [
          { type: "rest" }, { type: "grpc" }, { type: "graphql" },
          { type: "websocket" }, { type: "tcp-custom" },
        ],
        ["openapi", "proto3", "graphql", "websocket", "tcp-custom"],
      ],
    ])("interfaces_%j_returnsProtocols_%j", (interfaces, expected) => {
      const config = buildConfig({ interfaces });
      expect(resolveStack(config).protocols).toEqual(expected);
    });
  });

  describe("project type", () => {
    it.each([
      ["microservice", [{ type: "rest" }], "api"],
      ["library", [{ type: "cli" }], "cli"],
      ["microservice", [{ type: "event-consumer" }], "worker"],
      ["monolith", [{ type: "rest" }], "api"],
      ["library", [] as Array<{ type: string }>, "library"],
      ["modular-monolith", [{ type: "rest" }], "api"],
      ["serverless", [{ type: "rest" }], "api"],
    ])(
      "%s_with_%j_returns_%s",
      (style, interfaces, expectedType) => {
        const config = buildConfig({
          architecture: { style },
          interfaces,
        });
        expect(resolveStack(config).projectType).toBe(expectedType);
      },
    );

    it("microservice_restAndEvent_returnsApi", () => {
      const config = buildConfig({
        architecture: { style: "microservice" },
        interfaces: [
          { type: "rest" }, { type: "event-consumer" },
        ],
      });
      expect(resolveStack(config).projectType).toBe("api");
    });
  });

  describe("native build", () => {
    it.each([
      ["quarkus", true, true],
      ["spring-boot", true, true],
      ["click", true, false],
      ["gin", true, false],
      ["quarkus", false, false],
    ])(
      "%s_native_%s_returns_%s",
      (fw, nativeBuild, expected) => {
        const config = buildConfig({
          framework: {
            name: fw, version: "3.0",
            buildTool: "maven", nativeBuild,
          },
        });
        expect(resolveStack(config).nativeSupported).toBe(expected);
      },
    );
  });

  describe("full resolution", () => {
    it("javaQuarkus_fullResolution", () => {
      const config = buildConfig({
        language: { name: "java", version: "17" },
        framework: {
          name: "quarkus", version: "3.0",
          buildTool: "maven", nativeBuild: true,
        },
        architecture: { style: "microservice" },
        interfaces: [{ type: "rest" }],
      });
      const result = resolveStack(config);
      expect(result.buildCmd).toBe("./mvnw package -DskipTests");
      expect(result.healthPath).toBe("/q/health");
      expect(result.dockerBaseImage).toBe(
        "eclipse-temurin:17-jre-alpine",
      );
      expect(result.nativeSupported).toBe(true);
      expect(result.projectType).toBe("api");
    });

    it("pythonClick_fullResolution", () => {
      const config = buildConfig({
        language: { name: "python", version: "3.9" },
        framework: {
          name: "click", version: "8.1",
          buildTool: "pip", nativeBuild: false,
        },
        architecture: { style: "library" },
        interfaces: [{ type: "cli" }],
      });
      const result = resolveStack(config);
      expect(result.buildCmd).toBe("pip install -e .");
      expect(result.buildFile).toBe("pyproject.toml");
      expect(result.packageManager).toBe("pip");
      expect(result.projectType).toBe("cli");
    });

    it("unknownLanguage_defaultResolution", () => {
      const config = buildConfig({
        language: { name: "haskell", version: "9" },
        framework: {
          name: "unknown", version: "1.0",
          buildTool: "cabal", nativeBuild: false,
        },
      });
      const result = resolveStack(config);
      expect(result.buildCmd).toBe("");
      expect(result.testCmd).toBe("");
      expect(result.dockerBaseImage).toBe("alpine:latest");
      expect(result.defaultPort).toBe(8080);
    });
  });

  describe("readonly validation", () => {
    it("returnedStack_isReadonly", () => {
      const config = buildConfig();
      const result = resolveStack(config);
      expect(Object.isFrozen(result)).toBe(true);
    });
  });
});
