import { describe, it, expect } from "vitest";
import {
  CORE_TO_KP_MAPPING,
  CONDITIONAL_CORE_KP,
  getActiveRoutes,
} from "../../../src/domain/core-kp-routing.js";
import {
  ProjectConfig,
  ProjectIdentity,
  ArchitectureConfig,
  InterfaceConfig,
  LanguageConfig,
  FrameworkConfig,
} from "../../../src/models.js";

function buildConfig(style: string): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity("test", "test"),
    new ArchitectureConfig(style, false, false),
    [new InterfaceConfig("rest")],
    new LanguageConfig("java", "21"),
    new FrameworkConfig("quarkus", "3.0", "maven"),
  );
}

describe("CORE_TO_KP_MAPPING", () => {
  it("contains_11_staticRoutes", () => {
    expect(CORE_TO_KP_MAPPING).toHaveLength(11);
  });

  it("firstRoute_isCleanCode", () => {
    const first = CORE_TO_KP_MAPPING[0]!;
    expect(first.sourceFile).toBe("01-clean-code.md");
    expect(first.kpName).toBe("coding-standards");
    expect(first.destFile).toBe("clean-code.md");
  });

  it("lastRoute_isStoryDecomposition", () => {
    const last = CORE_TO_KP_MAPPING[10]!;
    expect(last.sourceFile).toBe("13-story-decomposition.md");
    expect(last.kpName).toBe("story-planning");
    expect(last.destFile).toBe("story-decomposition.md");
  });
});

describe("CONDITIONAL_CORE_KP", () => {
  it("contains_1_conditionalRoute", () => {
    expect(CONDITIONAL_CORE_KP).toHaveLength(1);
  });

  it("conditionalRoute_isCloudNative", () => {
    const route = CONDITIONAL_CORE_KP[0]!;
    expect(route.sourceFile).toBe("12-cloud-native-principles.md");
    expect(route.kpName).toBe("infrastructure");
    expect(route.conditionField).toBe("architecture_style");
    expect(route.conditionExclude).toBe("library");
  });
});

describe("getActiveRoutes", () => {
  it("microservice_includes12Routes", () => {
    const config = buildConfig("microservice");
    const routes = getActiveRoutes(config);
    expect(routes).toHaveLength(12);
  });

  it("library_excludesCloudNative_returns11Routes", () => {
    const config = buildConfig("library");
    const routes = getActiveRoutes(config);
    expect(routes).toHaveLength(11);
    const sourceFiles = routes.map((r) => r.sourceFile);
    expect(sourceFiles).not.toContain("12-cloud-native-principles.md");
  });

  it("monolith_includesCloudNative", () => {
    const config = buildConfig("monolith");
    const routes = getActiveRoutes(config);
    expect(routes).toHaveLength(12);
    const sourceFiles = routes.map((r) => r.sourceFile);
    expect(sourceFiles).toContain("12-cloud-native-principles.md");
  });

  it("allRoutes_haveAllFields", () => {
    const config = buildConfig("microservice");
    const routes = getActiveRoutes(config);
    for (const route of routes) {
      expect(route.sourceFile).toBeTruthy();
      expect(route.kpName).toBeTruthy();
      expect(route.destFile).toBeTruthy();
    }
  });
});
