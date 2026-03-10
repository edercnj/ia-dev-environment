import { describe, it, expect } from "vitest";
import type { ResolvedStack } from "../../../src/domain/resolved-stack.js";

describe("ResolvedStack interface", () => {
  it("canBeConstructedWithAllFields", () => {
    const stack: ResolvedStack = {
      buildCmd: "npm run build",
      testCmd: "npm test",
      compileCmd: "npx tsc --noEmit",
      coverageCmd: "npm test -- --coverage",
      dockerBaseImage: "node:18-alpine",
      healthPath: "/health",
      packageManager: "npm",
      defaultPort: 3000,
      fileExtension: ".ts",
      buildFile: "package.json",
      nativeSupported: false,
      projectType: "microservice",
      protocols: ["rest", "grpc"],
    };
    expect(stack.buildCmd).toBe("npm run build");
    expect(stack.defaultPort).toBe(3000);
    expect(stack.protocols).toEqual(["rest", "grpc"]);
  });

  it("protocolsCanBeEmpty", () => {
    const stack: ResolvedStack = {
      buildCmd: "",
      testCmd: "",
      compileCmd: "",
      coverageCmd: "",
      dockerBaseImage: "",
      healthPath: "",
      packageManager: "",
      defaultPort: 0,
      fileExtension: "",
      buildFile: "",
      nativeSupported: false,
      projectType: "",
      protocols: [],
    };
    expect(stack.protocols).toEqual([]);
  });

  it("hasAllExpected13Fields", () => {
    const fieldNames: (keyof ResolvedStack)[] = [
      "buildCmd", "testCmd", "compileCmd", "coverageCmd",
      "dockerBaseImage", "healthPath", "packageManager",
      "defaultPort", "fileExtension", "buildFile",
      "nativeSupported", "projectType", "protocols",
    ];
    expect(fieldNames).toHaveLength(13);
  });
});
