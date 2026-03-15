import { describe, it, expect } from "vitest";
import {
  CORE_TO_KP_MAPPING,
  CONDITIONAL_CORE_KP,
  getActiveRoutes,
} from "../../../src/domain/core-kp-routing.js";
import { aDomainTestConfig } from "../../fixtures/project-config.fixture.js";

describe("CORE_TO_KP_MAPPING", () => {
  it("contains_12_staticRoutes", () => {
    expect(CORE_TO_KP_MAPPING).toHaveLength(12);
  });

  it("firstRoute_isCleanCode", () => {
    const first = CORE_TO_KP_MAPPING[0]!;
    expect(first.sourceFile).toBe("01-clean-code.md");
    expect(first.kpName).toBe("coding-standards");
    expect(first.destFile).toBe("clean-code.md");
  });

  it("lastRoute_isRefactoringGuidelines", () => {
    const last = CORE_TO_KP_MAPPING[11]!;
    expect(last.sourceFile).toBe("14-refactoring-guidelines.md");
    expect(last.kpName).toBe("coding-standards");
    expect(last.destFile).toBe("refactoring-guidelines.md");
  });

  it("allDestFiles_areUnique_noDuplicates", () => {
    const destFiles = CORE_TO_KP_MAPPING.map((r) => r.destFile);
    const uniqueDestFiles = new Set(destFiles);
    expect(uniqueDestFiles.size).toBe(destFiles.length);
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
  it("microservice_includes13Routes", () => {
    const config = aDomainTestConfig({ style: "microservice" });
    const routes = getActiveRoutes(config);
    expect(routes).toHaveLength(13);
  });

  it("library_excludesCloudNative_returns12Routes", () => {
    const config = aDomainTestConfig({ style: "library" });
    const routes = getActiveRoutes(config);
    expect(routes).toHaveLength(12);
    const sourceFiles = routes.map((r) => r.sourceFile);
    expect(sourceFiles).not.toContain("12-cloud-native-principles.md");
  });

  it("monolith_includesCloudNative", () => {
    const config = aDomainTestConfig({ style: "monolith" });
    const routes = getActiveRoutes(config);
    expect(routes).toHaveLength(13);
    const sourceFiles = routes.map((r) => r.sourceFile);
    expect(sourceFiles).toContain("12-cloud-native-principles.md");
  });

  it("allRoutes_haveAllFields", () => {
    const config = aDomainTestConfig({ style: "microservice" });
    const routes = getActiveRoutes(config);
    for (const route of routes) {
      expect(route.sourceFile).toBeTruthy();
      expect(route.kpName).toBeTruthy();
      expect(route.destFile).toBeTruthy();
    }
  });
});
