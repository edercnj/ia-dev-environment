import { describe, it, expect } from "vitest";
import {
  FRAMEWORK_STACK_PACK,
  getStackPackName,
} from "../../../src/domain/stack-pack-mapping.js";

describe("FRAMEWORK_STACK_PACK", () => {
  it("contains_11_entries", () => {
    expect(Object.keys(FRAMEWORK_STACK_PACK)).toHaveLength(11);
  });

  it.each([
    ["quarkus", "quarkus-patterns"],
    ["spring-boot", "spring-patterns"],
    ["nestjs", "nestjs-patterns"],
    ["express", "express-patterns"],
    ["fastapi", "fastapi-patterns"],
    ["django", "django-patterns"],
    ["gin", "gin-patterns"],
    ["ktor", "ktor-patterns"],
    ["axum", "axum-patterns"],
    ["dotnet", "dotnet-patterns"],
    ["click", "click-cli-patterns"],
  ])("%s_returns_%s", (framework, pack) => {
    expect(FRAMEWORK_STACK_PACK[framework]).toBe(pack);
  });
});

describe("getStackPackName", () => {
  it("knownFramework_returnsPackName", () => {
    expect(getStackPackName("quarkus")).toBe("quarkus-patterns");
  });

  it("unknownFramework_returnsEmpty", () => {
    expect(getStackPackName("unknown")).toBe("");
  });
});
