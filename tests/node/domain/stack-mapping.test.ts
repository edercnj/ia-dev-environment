import { describe, it, expect } from "vitest";
import {
  LANGUAGE_COMMANDS,
  FRAMEWORK_PORTS,
  FRAMEWORK_HEALTH_PATHS,
  FRAMEWORK_LANGUAGE_RULES,
  NATIVE_SUPPORTED_FRAMEWORKS,
  VALID_INTERFACE_TYPES,
  VALID_ARCHITECTURE_STYLES,
  INTERFACE_SPEC_PROTOCOL_MAP,
  DOCKER_BASE_IMAGES,
  HOOK_TEMPLATE_MAP,
  SETTINGS_LANG_MAP,
  DATABASE_SETTINGS_MAP,
  CACHE_SETTINGS_MAP,
  DEFAULT_PORT_FALLBACK,
  DEFAULT_HEALTH_PATH,
  DEFAULT_DOCKER_IMAGE,
  getHookTemplateKey,
  getSettingsLangKey,
  getDatabaseSettingsKey,
  getCacheSettingsKey,
} from "../../../src/domain/stack-mapping.js";

describe("LANGUAGE_COMMANDS", () => {
  it("contains_8_entries", () => {
    expect(Object.keys(LANGUAGE_COMMANDS)).toHaveLength(8);
  });

  it.each([
    ["java-maven", "./mvnw compile -q", "./mvnw package -DskipTests", "./mvnw verify", "./mvnw verify jacoco:report", ".java", "pom.xml", "maven"],
    ["java-gradle", "./gradlew compileJava -q", "./gradlew build -x test", "./gradlew test", "./gradlew test jacocoTestReport", ".java", "build.gradle", "gradle"],
    ["kotlin-gradle", "./gradlew compileKotlin -q", "./gradlew build -x test", "./gradlew test", "./gradlew test jacocoTestReport", ".kt", "build.gradle.kts", "gradle"],
    ["typescript-npm", "npx --no-install tsc --noEmit", "npm run build", "npm test", "npm test -- --coverage", ".ts", "package.json", "npm"],
    ["python-pip", "python3 -m py_compile", "pip install -e .", "pytest", "pytest --cov", ".py", "pyproject.toml", "pip"],
    ["go-go", "go build ./...", "go build ./...", "go test ./...", "go test -coverprofile=coverage.out ./...", ".go", "go.mod", "go"],
    ["rust-cargo", "cargo check", "cargo build", "cargo test", "cargo tarpaulin", ".rs", "Cargo.toml", "cargo"],
    ["csharp-dotnet", "dotnet build --no-restore --verbosity quiet", "dotnet build", "dotnet test", 'dotnet test --collect:"XPlat Code Coverage"', ".cs", "*.csproj", "dotnet"],
  ])("%s_returnsCorrectCommands", (key, compileCmd, buildCmd, testCmd, coverageCmd, ext, buildFile, pkgMgr) => {
    const cmd = LANGUAGE_COMMANDS[key];
    expect(cmd).toBeDefined();
    expect(cmd!.compileCmd).toBe(compileCmd);
    expect(cmd!.buildCmd).toBe(buildCmd);
    expect(cmd!.testCmd).toBe(testCmd);
    expect(cmd!.coverageCmd).toBe(coverageCmd);
    expect(cmd!.fileExtension).toBe(ext);
    expect(cmd!.buildFile).toBe(buildFile);
    expect(cmd!.packageManager).toBe(pkgMgr);
  });
});

describe("FRAMEWORK_PORTS", () => {
  it("contains_11_entries", () => {
    expect(Object.keys(FRAMEWORK_PORTS)).toHaveLength(11);
  });

  it.each([
    ["quarkus", 8080],
    ["spring-boot", 8080],
    ["nestjs", 3000],
    ["fastapi", 8000],
    ["aspnet", 5000],
    ["axum", 3000],
  ])("%s_returnsPort_%i", (fw, port) => {
    expect(FRAMEWORK_PORTS[fw]).toBe(port);
  });

  it("defaultPortFallback_is8080", () => {
    expect(DEFAULT_PORT_FALLBACK).toBe(8080);
  });
});

describe("FRAMEWORK_HEALTH_PATHS", () => {
  it("contains_11_entries", () => {
    expect(Object.keys(FRAMEWORK_HEALTH_PATHS)).toHaveLength(11);
  });

  it("quarkus_hasSpecificPath", () => {
    expect(FRAMEWORK_HEALTH_PATHS["quarkus"]).toBe("/q/health");
  });

  it("springBoot_hasActuatorPath", () => {
    expect(FRAMEWORK_HEALTH_PATHS["spring-boot"]).toBe("/actuator/health");
  });

  it("defaultHealthPath_isSlashHealth", () => {
    expect(DEFAULT_HEALTH_PATH).toBe("/health");
  });
});

describe("FRAMEWORK_LANGUAGE_RULES", () => {
  it("contains_15_entries", () => {
    expect(Object.keys(FRAMEWORK_LANGUAGE_RULES)).toHaveLength(15);
  });

  it("quarkus_supportsJavaAndKotlin", () => {
    expect(FRAMEWORK_LANGUAGE_RULES["quarkus"]).toEqual(["java", "kotlin"]);
  });

  it("nestjs_supportsOnlyTypescript", () => {
    expect(FRAMEWORK_LANGUAGE_RULES["nestjs"]).toEqual(["typescript"]);
  });

  it("aspnet_supportsOnlyCsharp", () => {
    expect(FRAMEWORK_LANGUAGE_RULES["aspnet"]).toEqual(["csharp"]);
  });
});

describe("NATIVE_SUPPORTED_FRAMEWORKS", () => {
  it("containsQuarkusAndSpringBoot", () => {
    expect(NATIVE_SUPPORTED_FRAMEWORKS).toEqual(["quarkus", "spring-boot"]);
  });
});

describe("VALID_INTERFACE_TYPES", () => {
  it("contains_9_entries", () => {
    expect(VALID_INTERFACE_TYPES).toHaveLength(9);
  });

  it("includesExpectedTypes", () => {
    expect(VALID_INTERFACE_TYPES).toContain("rest");
    expect(VALID_INTERFACE_TYPES).toContain("grpc");
    expect(VALID_INTERFACE_TYPES).toContain("event-consumer");
    expect(VALID_INTERFACE_TYPES).toContain("scheduled");
  });
});

describe("VALID_ARCHITECTURE_STYLES", () => {
  it("contains_5_entries", () => {
    expect(VALID_ARCHITECTURE_STYLES).toHaveLength(5);
  });

  it("includesExpectedStyles", () => {
    expect(VALID_ARCHITECTURE_STYLES).toContain("microservice");
    expect(VALID_ARCHITECTURE_STYLES).toContain("library");
    expect(VALID_ARCHITECTURE_STYLES).toContain("serverless");
  });
});

describe("INTERFACE_SPEC_PROTOCOL_MAP", () => {
  it("mapsRestToOpenapi", () => {
    expect(INTERFACE_SPEC_PROTOCOL_MAP["rest"]).toBe("openapi");
  });

  it("mapsGrpcToProto3", () => {
    expect(INTERFACE_SPEC_PROTOCOL_MAP["grpc"]).toBe("proto3");
  });

  it("mapsEventConsumerToKafka", () => {
    expect(INTERFACE_SPEC_PROTOCOL_MAP["event-consumer"]).toBe("kafka");
  });
});

describe("DOCKER_BASE_IMAGES", () => {
  it("contains_7_entries", () => {
    expect(Object.keys(DOCKER_BASE_IMAGES)).toHaveLength(7);
  });

  it("javaUsesTemurin", () => {
    expect(DOCKER_BASE_IMAGES["java"]).toBe("eclipse-temurin:{version}-jre-alpine");
  });

  it("defaultDockerImage_isAlpine", () => {
    expect(DEFAULT_DOCKER_IMAGE).toBe("alpine:latest");
  });
});

describe("getHookTemplateKey", () => {
  it("javaMaven_returnsJavaMaven", () => {
    expect(getHookTemplateKey("java", "maven")).toBe("java-maven");
  });

  it("kotlinGradle_returnsKotlin", () => {
    expect(getHookTemplateKey("kotlin", "gradle")).toBe("kotlin");
  });

  it("pythonPip_returnsEmpty", () => {
    expect(getHookTemplateKey("python", "pip")).toBe("");
  });

  it("unknownCombo_returnsEmpty", () => {
    expect(getHookTemplateKey("unknown", "unknown")).toBe("");
  });
});

describe("getSettingsLangKey", () => {
  it("javaMaven_returnsJavaMaven", () => {
    expect(getSettingsLangKey("java", "maven")).toBe("java-maven");
  });

  it("kotlinGradle_returnsJavaGradle", () => {
    expect(getSettingsLangKey("kotlin", "gradle")).toBe("java-gradle");
  });

  it("unknownCombo_returnsEmpty", () => {
    expect(getSettingsLangKey("unknown", "unknown")).toBe("");
  });
});

describe("getDatabaseSettingsKey", () => {
  it("postgresql_returnsDatabasePsql", () => {
    expect(getDatabaseSettingsKey("postgresql")).toBe("database-psql");
  });

  it("mongodb_returnsDatabaseMongodb", () => {
    expect(getDatabaseSettingsKey("mongodb")).toBe("database-mongodb");
  });

  it("unknownDb_returnsEmpty", () => {
    expect(getDatabaseSettingsKey("unknown")).toBe("");
  });
});

describe("getCacheSettingsKey", () => {
  it("redis_returnsCacheRedis", () => {
    expect(getCacheSettingsKey("redis")).toBe("cache-redis");
  });

  it("unknownCache_returnsEmpty", () => {
    expect(getCacheSettingsKey("unknown")).toBe("");
  });
});
