import { describe, it, expect } from "vitest";
import {
  extractInterfaceTypes,
  hasInterface,
  hasAnyInterface,
} from "../../../src/assembler/conditions.js";
import { aDomainTestConfig } from "../../fixtures/project-config.fixture.js";
import { InterfaceConfig } from "../../../src/models.js";

describe("extractInterfaceTypes", () => {
  it("extractInterfaceTypes_multipleInterfaces_returnsAllTypes", () => {
    const config = aDomainTestConfig({
      interfaces: [
        new InterfaceConfig("rest"),
        new InterfaceConfig("grpc"),
        new InterfaceConfig("cli"),
      ],
    });
    expect(extractInterfaceTypes(config)).toEqual([
      "rest",
      "grpc",
      "cli",
    ]);
  });

  it("extractInterfaceTypes_singleInterface_returnsSingleElement", () => {
    const config = aDomainTestConfig({
      interfaces: [new InterfaceConfig("rest")],
    });
    expect(extractInterfaceTypes(config)).toEqual(["rest"]);
  });

  it("extractInterfaceTypes_noInterfaces_returnsEmptyArray", () => {
    const config = aDomainTestConfig({ interfaces: [] });
    expect(extractInterfaceTypes(config)).toEqual([]);
  });
});

describe("hasInterface", () => {
  it("hasInterface_existingType_returnsTrue", () => {
    const config = aDomainTestConfig({
      interfaces: [
        new InterfaceConfig("rest"),
        new InterfaceConfig("grpc"),
      ],
    });
    expect(hasInterface(config, "rest")).toBe(true);
  });

  it("hasInterface_missingType_returnsFalse", () => {
    const config = aDomainTestConfig({
      interfaces: [new InterfaceConfig("rest")],
    });
    expect(hasInterface(config, "grpc")).toBe(false);
  });

  it("hasInterface_emptyInterfaces_returnsFalse", () => {
    const config = aDomainTestConfig({ interfaces: [] });
    expect(hasInterface(config, "rest")).toBe(false);
  });
});

describe("hasAnyInterface", () => {
  it("hasAnyInterface_oneMatch_returnsTrue", () => {
    const config = aDomainTestConfig({
      interfaces: [
        new InterfaceConfig("rest"),
        new InterfaceConfig("grpc"),
      ],
    });
    expect(hasAnyInterface(config, "cli", "grpc")).toBe(true);
  });

  it("hasAnyInterface_noMatch_returnsFalse", () => {
    const config = aDomainTestConfig({
      interfaces: [new InterfaceConfig("rest")],
    });
    expect(hasAnyInterface(config, "cli", "grpc")).toBe(false);
  });

  it("hasAnyInterface_emptyTypes_returnsFalse", () => {
    const config = aDomainTestConfig({
      interfaces: [new InterfaceConfig("rest")],
    });
    expect(hasAnyInterface(config)).toBe(false);
  });

  it("hasAnyInterface_emptyInterfaces_returnsFalse", () => {
    const config = aDomainTestConfig({ interfaces: [] });
    expect(hasAnyInterface(config, "rest")).toBe(false);
  });
});
