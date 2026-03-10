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

  it("hasExactlyTheExpectedFields", () => {
    // Compile-time assertion: if ResolvedStack gains or loses a field,
    // this type will produce a TS error (excess/missing property).
    type AssertExactKeys<T, U extends T> = U extends T ? T extends U ? true : false : false;
    type Expected = {
      readonly buildCmd: string;
      readonly testCmd: string;
      readonly compileCmd: string;
      readonly coverageCmd: string;
      readonly dockerBaseImage: string;
      readonly healthPath: string;
      readonly packageManager: string;
      readonly defaultPort: number;
      readonly fileExtension: string;
      readonly buildFile: string;
      readonly nativeSupported: boolean;
      readonly projectType: string;
      readonly protocols: readonly string[];
    };
    const check: AssertExactKeys<ResolvedStack, Expected> = true;
    expect(check).toBe(true);
  });
});
