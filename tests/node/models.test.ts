import { describe, it, expect } from "vitest";
import {
  requireField,
  TechComponent,
  ProjectIdentity,
  ArchitectureConfig,
  InterfaceConfig,
  LanguageConfig,
  FrameworkConfig,
  DataConfig,
  SecurityConfig,
  ObservabilityConfig,
  InfraConfig,
  TestingConfig,
  McpServerConfig,
  McpConfig,
  ProjectConfig,
  PipelineResult,
  FileDiff,
  VerificationResult,
} from "../../src/models.js";

function aMinimalProjectConfigData(): Record<string, unknown> {
  return {
    project: { name: "test-app", purpose: "testing" },
    architecture: { style: "hexagonal" },
    interfaces: [{ type: "rest" }],
    language: { name: "typescript", version: "5" },
    framework: { name: "express", version: "4" },
  };
}

function aFullProjectConfigData(): Record<string, unknown> {
  return {
    project: { name: "test-app", purpose: "full test" },
    architecture: {
      style: "hexagonal",
      domain_driven: true,
      event_driven: false,
    },
    interfaces: [
      { type: "rest", spec: "openapi", broker: "" },
      { type: "grpc", spec: "proto3", broker: "kafka" },
    ],
    language: { name: "typescript", version: "5" },
    framework: {
      name: "express",
      version: "4",
      build_tool: "npm",
      native_build: false,
    },
    data: {
      database: { name: "postgres", version: "15" },
      migration: { name: "flyway", version: "9" },
      cache: { name: "redis", version: "7" },
    },
    infrastructure: {
      container: "docker",
      orchestrator: "kubernetes",
      templating: "helm",
      iac: "terraform",
      registry: "ecr",
      api_gateway: "kong",
      service_mesh: "istio",
      observability: {
        tool: "prometheus",
        metrics: "micrometer",
        tracing: "jaeger",
      },
    },
    security: { frameworks: ["oauth2", "jwt"] },
    testing: {
      smoke_tests: true,
      contract_tests: true,
      performance_tests: true,
      coverage_line: 95,
      coverage_branch: 90,
    },
    mcp: {
      servers: [
        {
          id: "srv1",
          url: "http://mcp1.local",
          capabilities: ["read", "write"],
          env: { TOKEN: "abc" },
        },
      ],
    },
  };
}

describe("requireField helper", () => {
  it("keyExists_returnsValue", () => {
    expect(requireField({ name: "test" }, "name", "M")).toBe(
      "test",
    );
  });

  it("valueIsZero_returnsZero", () => {
    expect(requireField({ count: 0 }, "count", "M")).toBe(0);
  });

  it("valueIsFalse_returnsFalse", () => {
    expect(requireField({ flag: false }, "flag", "M")).toBe(
      false,
    );
  });

  it("valueIsEmptyString_returnsEmptyString", () => {
    expect(requireField({ label: "" }, "label", "M")).toBe("");
  });

  it("valueIsNull_returnsNull", () => {
    expect(requireField({ x: null }, "x", "M")).toBeNull();
  });

  it("keyMissing_throwsErrorWithMessage", () => {
    expect(() =>
      requireField({}, "name", "ProjectIdentity"),
    ).toThrow(
      "Missing required field 'name' in ProjectIdentity",
    );
  });

  it("keyMissingDifferentModel_includesModelNameInMessage", () => {
    expect(() =>
      requireField({}, "style", "ArchitectureConfig"),
    ).toThrow(/style.*ArchitectureConfig/);
  });

  it("keyExistsWithUndefinedValue_returnsUndefined", () => {
    const data: Record<string, unknown> = { name: undefined };
    expect(requireField(data, "name", "M")).toBeUndefined();
  });
});

describe("TechComponent", () => {
  describe("fromDict", () => {
    it("allFields_createsWithProvidedValues", () => {
      const tc = TechComponent.fromDict({
        name: "postgres",
        version: "15",
      });
      expect(tc.name).toBe("postgres");
      expect(tc.version).toBe("15");
    });

    it("emptyObject_usesDefaults", () => {
      const tc = TechComponent.fromDict({});
      expect(tc.name).toBe("none");
      expect(tc.version).toBe("");
    });

    it("onlyName_usesDefaultVersion", () => {
      const tc = TechComponent.fromDict({ name: "redis" });
      expect(tc.name).toBe("redis");
      expect(tc.version).toBe("");
    });

    it("onlyVersion_usesDefaultName", () => {
      const tc = TechComponent.fromDict({ version: "3.2" });
      expect(tc.name).toBe("none");
      expect(tc.version).toBe("3.2");
    });
  });
});

describe("ProjectIdentity", () => {
  describe("fromDict", () => {
    it("allFields_createsWithProvidedValues", () => {
      const pi = ProjectIdentity.fromDict({
        name: "my-app",
        purpose: "REST API",
      });
      expect(pi.name).toBe("my-app");
      expect(pi.purpose).toBe("REST API");
    });

    it("missingName_throwsError", () => {
      expect(() =>
        ProjectIdentity.fromDict({ purpose: "REST API" }),
      ).toThrow(/name.*ProjectIdentity/);
    });

    it("missingPurpose_throwsError", () => {
      expect(() =>
        ProjectIdentity.fromDict({ name: "my-app" }),
      ).toThrow(/purpose.*ProjectIdentity/);
    });

    it("emptyObject_throwsError", () => {
      expect(() =>
        ProjectIdentity.fromDict({}),
      ).toThrow(Error);
    });
  });
});

describe("ArchitectureConfig", () => {
  describe("fromDict", () => {
    it("allFields_createsWithProvidedValues", () => {
      const ac = ArchitectureConfig.fromDict({
        style: "hexagonal",
        domain_driven: true,
        event_driven: true,
      });
      expect(ac.style).toBe("hexagonal");
      expect(ac.domainDriven).toBe(true);
      expect(ac.eventDriven).toBe(true);
    });

    it("onlyRequired_usesDefaults", () => {
      const ac = ArchitectureConfig.fromDict({
        style: "layered",
      });
      expect(ac.domainDriven).toBe(false);
      expect(ac.eventDriven).toBe(false);
    });

    it("missingStyle_throwsError", () => {
      expect(() =>
        ArchitectureConfig.fromDict({}),
      ).toThrow(/style.*ArchitectureConfig/);
    });
  });
});

describe("InterfaceConfig", () => {
  describe("fromDict", () => {
    it("allFields_createsWithProvidedValues", () => {
      const ic = InterfaceConfig.fromDict({
        type: "rest",
        spec: "openapi",
        broker: "kafka",
      });
      expect(ic.type).toBe("rest");
      expect(ic.spec).toBe("openapi");
      expect(ic.broker).toBe("kafka");
    });

    it("onlyRequired_usesDefaults", () => {
      const ic = InterfaceConfig.fromDict({ type: "grpc" });
      expect(ic.spec).toBe("");
      expect(ic.broker).toBe("");
    });

    it("missingType_throwsError", () => {
      expect(() =>
        InterfaceConfig.fromDict({}),
      ).toThrow(/type.*InterfaceConfig/);
    });
  });
});

describe("LanguageConfig", () => {
  describe("fromDict", () => {
    it("allFields_createsWithProvidedValues", () => {
      const lc = LanguageConfig.fromDict({
        name: "typescript",
        version: "5",
      });
      expect(lc.name).toBe("typescript");
      expect(lc.version).toBe("5");
    });

    it("missingName_throwsError", () => {
      expect(() =>
        LanguageConfig.fromDict({ version: "5" }),
      ).toThrow(/name.*LanguageConfig/);
    });

    it("missingVersion_throwsError", () => {
      expect(() =>
        LanguageConfig.fromDict({ name: "typescript" }),
      ).toThrow(/version.*LanguageConfig/);
    });
  });
});

describe("FrameworkConfig", () => {
  describe("fromDict", () => {
    it("allFields_createsWithProvidedValues", () => {
      const fc = FrameworkConfig.fromDict({
        name: "express",
        version: "4",
        build_tool: "npm",
        native_build: true,
      });
      expect(fc.name).toBe("express");
      expect(fc.version).toBe("4");
      expect(fc.buildTool).toBe("npm");
      expect(fc.nativeBuild).toBe(true);
    });

    it("onlyRequired_usesDefaults", () => {
      const fc = FrameworkConfig.fromDict({
        name: "express",
        version: "4",
      });
      expect(fc.buildTool).toBe("pip");
      expect(fc.nativeBuild).toBe(false);
    });

    it("missingName_throwsError", () => {
      expect(() =>
        FrameworkConfig.fromDict({ version: "4" }),
      ).toThrow(/name.*FrameworkConfig/);
    });

    it("missingVersion_throwsError", () => {
      expect(() =>
        FrameworkConfig.fromDict({ name: "express" }),
      ).toThrow(/version.*FrameworkConfig/);
    });
  });
});

describe("SecurityConfig", () => {
  describe("fromDict", () => {
    it("withFrameworks_createsWithList", () => {
      const sc = SecurityConfig.fromDict({
        frameworks: ["oauth2", "jwt"],
      });
      expect(sc.frameworks).toEqual(["oauth2", "jwt"]);
    });

    it("emptyObject_usesDefaultEmptyArray", () => {
      const sc = SecurityConfig.fromDict({});
      expect(sc.frameworks).toEqual([]);
    });

    it("emptyFrameworks_createsEmptyList", () => {
      const sc = SecurityConfig.fromDict({ frameworks: [] });
      expect(sc.frameworks).toEqual([]);
    });
  });
});

describe("ObservabilityConfig", () => {
  describe("fromDict", () => {
    it("allFields_createsWithProvidedValues", () => {
      const oc = ObservabilityConfig.fromDict({
        tool: "prometheus",
        metrics: "micrometer",
        tracing: "jaeger",
      });
      expect(oc.tool).toBe("prometheus");
      expect(oc.metrics).toBe("micrometer");
      expect(oc.tracing).toBe("jaeger");
    });

    it("emptyObject_usesDefaults", () => {
      const oc = ObservabilityConfig.fromDict({});
      expect(oc.tool).toBe("none");
      expect(oc.metrics).toBe("none");
      expect(oc.tracing).toBe("none");
    });

    it("partialFields_usesDefaultsForMissing", () => {
      const oc = ObservabilityConfig.fromDict({
        tool: "grafana",
      });
      expect(oc.tool).toBe("grafana");
      expect(oc.metrics).toBe("none");
      expect(oc.tracing).toBe("none");
    });
  });
});

describe("TestingConfig", () => {
  describe("fromDict", () => {
    it("allFields_createsWithProvidedValues", () => {
      const tc = TestingConfig.fromDict({
        smoke_tests: false,
        contract_tests: true,
        performance_tests: false,
        coverage_line: 80,
        coverage_branch: 70,
      });
      expect(tc.smokeTests).toBe(false);
      expect(tc.contractTests).toBe(true);
      expect(tc.performanceTests).toBe(false);
      expect(tc.coverageLine).toBe(80);
      expect(tc.coverageBranch).toBe(70);
    });

    it("emptyObject_usesDefaults", () => {
      const tc = TestingConfig.fromDict({});
      expect(tc.smokeTests).toBe(true);
      expect(tc.contractTests).toBe(false);
      expect(tc.performanceTests).toBe(true);
      expect(tc.coverageLine).toBe(95);
      expect(tc.coverageBranch).toBe(90);
    });

    it("partialOverride_mergesWithDefaults", () => {
      const tc = TestingConfig.fromDict({
        coverage_line: 80,
      });
      expect(tc.coverageLine).toBe(80);
      expect(tc.smokeTests).toBe(true);
      expect(tc.contractTests).toBe(false);
      expect(tc.performanceTests).toBe(true);
      expect(tc.coverageBranch).toBe(90);
    });
  });
});

describe("McpServerConfig", () => {
  describe("fromDict", () => {
    it("allFields_createsWithProvidedValues", () => {
      const ms = McpServerConfig.fromDict({
        id: "srv1",
        url: "http://localhost",
        capabilities: ["read"],
        env: { API_KEY: "x" },
      });
      expect(ms.id).toBe("srv1");
      expect(ms.url).toBe("http://localhost");
      expect(ms.capabilities).toEqual(["read"]);
      expect(ms.env).toEqual({ API_KEY: "x" });
    });

    it("onlyRequired_usesDefaults", () => {
      const ms = McpServerConfig.fromDict({
        id: "srv1",
        url: "http://localhost",
      });
      expect(ms.capabilities).toEqual([]);
      expect(ms.env).toEqual({});
    });

    it("missingId_throwsError", () => {
      expect(() =>
        McpServerConfig.fromDict({ url: "http://localhost" }),
      ).toThrow(/id.*McpServerConfig/);
    });

    it("missingUrl_throwsError", () => {
      expect(() =>
        McpServerConfig.fromDict({ id: "srv1" }),
      ).toThrow(/url.*McpServerConfig/);
    });
  });
});

describe("DataConfig", () => {
  describe("fromDict", () => {
    it("allFields_createsNestedTechComponents", () => {
      const dc = DataConfig.fromDict({
        database: { name: "postgres", version: "15" },
        migration: { name: "flyway", version: "9" },
        cache: { name: "redis", version: "7" },
      });
      expect(dc.database.name).toBe("postgres");
      expect(dc.database.version).toBe("15");
      expect(dc.migration.name).toBe("flyway");
      expect(dc.cache.name).toBe("redis");
    });

    it("emptyObject_usesDefaultTechComponents", () => {
      const dc = DataConfig.fromDict({});
      expect(dc.database.name).toBe("none");
      expect(dc.migration.name).toBe("none");
      expect(dc.cache.name).toBe("none");
    });

    it("partialNested_usesDefaultsForMissingSubs", () => {
      const dc = DataConfig.fromDict({
        database: { name: "postgres" },
      });
      expect(dc.database.name).toBe("postgres");
      expect(dc.database.version).toBe("");
      expect(dc.migration.name).toBe("none");
      expect(dc.cache.name).toBe("none");
    });

    it("nestedAreInstances_correctType", () => {
      const dc = DataConfig.fromDict({
        database: { name: "pg" },
      });
      expect(dc.database).toBeInstanceOf(TechComponent);
      expect(dc.migration).toBeInstanceOf(TechComponent);
      expect(dc.cache).toBeInstanceOf(TechComponent);
    });
  });
});

describe("InfraConfig", () => {
  describe("fromDict", () => {
    it("allFields_createsWithProvidedValues", () => {
      const ic = InfraConfig.fromDict({
        container: "podman",
        orchestrator: "kubernetes",
        templating: "helm",
        iac: "terraform",
        registry: "ecr",
        api_gateway: "kong",
        service_mesh: "istio",
        observability: {
          tool: "prometheus",
          metrics: "micrometer",
          tracing: "jaeger",
        },
      });
      expect(ic.container).toBe("podman");
      expect(ic.orchestrator).toBe("kubernetes");
      expect(ic.templating).toBe("helm");
      expect(ic.iac).toBe("terraform");
      expect(ic.registry).toBe("ecr");
      expect(ic.apiGateway).toBe("kong");
      expect(ic.serviceMesh).toBe("istio");
      expect(ic.observability).toBeInstanceOf(ObservabilityConfig);
      expect(ic.observability.tool).toBe("prometheus");
    });

    it("emptyObject_usesAllDefaults", () => {
      const ic = InfraConfig.fromDict({});
      expect(ic.container).toBe("docker");
      expect(ic.orchestrator).toBe("none");
      expect(ic.templating).toBe("kustomize");
      expect(ic.iac).toBe("none");
      expect(ic.registry).toBe("none");
      expect(ic.apiGateway).toBe("none");
      expect(ic.serviceMesh).toBe("none");
      expect(ic.observability.tool).toBe("none");
    });

    it("nestedObservability_parsesCorrectly", () => {
      const ic = InfraConfig.fromDict({
        observability: {
          tool: "prometheus",
          metrics: "micrometer",
        },
      });
      expect(ic.observability.tool).toBe("prometheus");
      expect(ic.observability.metrics).toBe("micrometer");
      expect(ic.observability.tracing).toBe("none");
    });

    it("partialTopLevel_usesDefaultsForMissing", () => {
      const ic = InfraConfig.fromDict({
        container: "podman",
      });
      expect(ic.container).toBe("podman");
      expect(ic.orchestrator).toBe("none");
      expect(ic.templating).toBe("kustomize");
    });
  });
});

describe("McpConfig", () => {
  describe("fromDict", () => {
    it("withServers_parsesAllServerConfigs", () => {
      const mc = McpConfig.fromDict({
        servers: [
          { id: "a", url: "http://a" },
          { id: "b", url: "http://b" },
        ],
      });
      expect(mc.servers).toHaveLength(2);
      expect(mc.servers[0]).toBeInstanceOf(McpServerConfig);
      expect(mc.servers[0]?.id).toBe("a");
      expect(mc.servers[1]?.url).toBe("http://b");
    });

    it("emptyObject_usesDefaultEmptyServers", () => {
      const mc = McpConfig.fromDict({});
      expect(mc.servers).toEqual([]);
    });

    it("emptyServersArray_createsEmptyList", () => {
      const mc = McpConfig.fromDict({ servers: [] });
      expect(mc.servers).toEqual([]);
    });

    it("serverMissingRequired_throwsError", () => {
      expect(() =>
        McpConfig.fromDict({ servers: [{ id: "a" }] }),
      ).toThrow(/url.*McpServerConfig/);
    });
  });
});

describe("ProjectConfig", () => {
  describe("fromDict", () => {
    it("allFields_createsCompleteConfig", () => {
      const pc = ProjectConfig.fromDict(
        aFullProjectConfigData(),
      );
      expect(pc.project.name).toBe("test-app");
      expect(pc.architecture.style).toBe("hexagonal");
      expect(pc.architecture.domainDriven).toBe(true);
      expect(pc.interfaces).toHaveLength(2);
      expect(pc.interfaces[0]?.type).toBe("rest");
      expect(pc.interfaces[1]?.spec).toBe("proto3");
      expect(pc.language.name).toBe("typescript");
      expect(pc.framework.buildTool).toBe("npm");
      expect(pc.data.database.name).toBe("postgres");
      expect(pc.infrastructure.apiGateway).toBe("kong");
      expect(pc.infrastructure.observability.tool).toBe(
        "prometheus",
      );
      expect(pc.security.frameworks).toEqual(["oauth2", "jwt"]);
      expect(pc.testing.contractTests).toBe(true);
      expect(pc.mcp.servers).toHaveLength(1);
      expect(pc.mcp.servers[0]?.id).toBe("srv1");
    });

    it("onlyRequired_usesDefaultsForOptional", () => {
      const pc = ProjectConfig.fromDict(
        aMinimalProjectConfigData(),
      );
      expect(pc.data.database.name).toBe("none");
      expect(pc.infrastructure.container).toBe("docker");
      expect(pc.security.frameworks).toEqual([]);
      expect(pc.testing.smokeTests).toBe(true);
      expect(pc.mcp.servers).toEqual([]);
    });

    it("missingProject_throwsError", () => {
      const data = aMinimalProjectConfigData();
      delete data["project"];
      expect(() => ProjectConfig.fromDict(data)).toThrow(
        /project.*ProjectConfig/,
      );
    });

    it("missingArchitecture_throwsError", () => {
      const data = aMinimalProjectConfigData();
      delete data["architecture"];
      expect(() => ProjectConfig.fromDict(data)).toThrow(
        /architecture.*ProjectConfig/,
      );
    });

    it("missingInterfaces_throwsError", () => {
      const data = aMinimalProjectConfigData();
      delete data["interfaces"];
      expect(() => ProjectConfig.fromDict(data)).toThrow(
        /interfaces.*ProjectConfig/,
      );
    });

    it("missingLanguage_throwsError", () => {
      const data = aMinimalProjectConfigData();
      delete data["language"];
      expect(() => ProjectConfig.fromDict(data)).toThrow(
        /language.*ProjectConfig/,
      );
    });

    it("missingFramework_throwsError", () => {
      const data = aMinimalProjectConfigData();
      delete data["framework"];
      expect(() => ProjectConfig.fromDict(data)).toThrow(
        /framework.*ProjectConfig/,
      );
    });

    it("multipleInterfaces_parsesAll", () => {
      const data = aMinimalProjectConfigData();
      data["interfaces"] = [
        { type: "rest" },
        { type: "grpc", spec: "proto3" },
      ];
      const pc = ProjectConfig.fromDict(data);
      expect(pc.interfaces).toHaveLength(2);
      expect(pc.interfaces[0]?.type).toBe("rest");
      expect(pc.interfaces[1]?.spec).toBe("proto3");
    });

    it("singleInterface_parsesList", () => {
      const data = aMinimalProjectConfigData();
      data["interfaces"] = [{ type: "cli" }];
      const pc = ProjectConfig.fromDict(data);
      expect(pc.interfaces).toHaveLength(1);
    });

    it("emptyInterfaces_createsEmptyList", () => {
      const data = aMinimalProjectConfigData();
      data["interfaces"] = [];
      const pc = ProjectConfig.fromDict(data);
      expect(pc.interfaces).toHaveLength(0);
    });

    it("nestedProject_isProjectIdentityInstance", () => {
      const pc = ProjectConfig.fromDict(
        aFullProjectConfigData(),
      );
      expect(pc.project).toBeInstanceOf(ProjectIdentity);
    });

    it("nestedArchitecture_isArchitectureConfigInstance", () => {
      const pc = ProjectConfig.fromDict(
        aFullProjectConfigData(),
      );
      expect(pc.architecture).toBeInstanceOf(
        ArchitectureConfig,
      );
    });

    it("nestedData_containsTechComponentInstances", () => {
      const pc = ProjectConfig.fromDict(
        aFullProjectConfigData(),
      );
      expect(pc.data.database).toBeInstanceOf(TechComponent);
    });

    it("nestedInfra_containsObservabilityInstance", () => {
      const pc = ProjectConfig.fromDict(
        aFullProjectConfigData(),
      );
      expect(pc.infrastructure.observability).toBeInstanceOf(
        ObservabilityConfig,
      );
    });

    it("nestedMcp_containsMcpServerConfigInstances", () => {
      const pc = ProjectConfig.fromDict(
        aFullProjectConfigData(),
      );
      expect(pc.mcp.servers[0]).toBeInstanceOf(
        McpServerConfig,
      );
    });
  });
});

describe("PipelineResult", () => {
  it("constructor_createsInstanceWithAllFields", () => {
    const pr = new PipelineResult(
      true,
      "/out",
      ["a.ts", "b.ts"],
      ["warn1"],
      1500,
    );
    expect(pr.success).toBe(true);
    expect(pr.outputDir).toBe("/out");
    expect(pr.filesGenerated).toEqual(["a.ts", "b.ts"]);
    expect(pr.warnings).toEqual(["warn1"]);
    expect(pr.durationMs).toBe(1500);
  });

  it("constructor_emptyArrays_createsWithEmptyLists", () => {
    const pr = new PipelineResult(false, "/out", [], [], 0);
    expect(pr.filesGenerated).toEqual([]);
    expect(pr.warnings).toEqual([]);
  });
});

describe("FileDiff", () => {
  it("constructor_createsInstanceWithAllFields", () => {
    const fd = new FileDiff(
      "src/index.ts",
      "- old\n+ new",
      100,
      120,
    );
    expect(fd.path).toBe("src/index.ts");
    expect(fd.diff).toBe("- old\n+ new");
    expect(fd.pythonSize).toBe(100);
    expect(fd.referenceSize).toBe(120);
  });

  it("constructor_emptyDiff_createsWithEmptyString", () => {
    const fd = new FileDiff("a.ts", "", 0, 0);
    expect(fd.diff).toBe("");
  });
});

describe("VerificationResult", () => {
  it("constructor_createsInstanceWithAllFields", () => {
    const diff = new FileDiff("x.ts", "diff", 10, 20);
    const vr = new VerificationResult(
      true,
      10,
      [diff],
      ["x.ts"],
      ["y.ts"],
    );
    expect(vr.success).toBe(true);
    expect(vr.totalFiles).toBe(10);
    expect(vr.mismatches).toHaveLength(1);
    expect(vr.mismatches[0]).toBeInstanceOf(FileDiff);
    expect(vr.missingFiles).toEqual(["x.ts"]);
    expect(vr.extraFiles).toEqual(["y.ts"]);
  });

  it("constructor_noMismatches_createsWithEmptyArrays", () => {
    const vr = new VerificationResult(true, 5, [], [], []);
    expect(vr.mismatches).toEqual([]);
    expect(vr.missingFiles).toEqual([]);
    expect(vr.extraFiles).toEqual([]);
  });
});

describe("mutable default independence", () => {
  it("SecurityConfig_twoInstances_frameworksArraysAreIndependent", () => {
    const sc1 = SecurityConfig.fromDict({});
    const sc2 = SecurityConfig.fromDict({});
    expect(sc1.frameworks).not.toBe(sc2.frameworks);
  });

  it("McpConfig_twoInstances_serversArraysAreIndependent", () => {
    const mc1 = McpConfig.fromDict({});
    const mc2 = McpConfig.fromDict({});
    expect(mc1.servers).not.toBe(mc2.servers);
  });

  it("McpServerConfig_twoInstances_capabilitiesAreIndependent", () => {
    const ms1 = McpServerConfig.fromDict({
      id: "a",
      url: "u",
    });
    const ms2 = McpServerConfig.fromDict({
      id: "a",
      url: "u",
    });
    expect(ms1.capabilities).not.toBe(ms2.capabilities);
  });

  it("McpServerConfig_twoInstances_envObjectsAreIndependent", () => {
    const ms1 = McpServerConfig.fromDict({
      id: "a",
      url: "u",
    });
    const ms2 = McpServerConfig.fromDict({
      id: "a",
      url: "u",
    });
    expect(ms1.env).not.toBe(ms2.env);
  });

  it("DataConfig_twoInstances_nestedTechComponentsAreIndependent", () => {
    const dc1 = DataConfig.fromDict({});
    const dc2 = DataConfig.fromDict({});
    expect(dc1.database).not.toBe(dc2.database);
  });
});

describe("snake_case key mapping", () => {
  it("ArchitectureConfig_snakeCaseKeys_mapsToCamelCase", () => {
    const ac = ArchitectureConfig.fromDict({
      style: "hex",
      domain_driven: true,
      event_driven: true,
    });
    expect(ac.domainDriven).toBe(true);
    expect(ac.eventDriven).toBe(true);
  });

  it("FrameworkConfig_snakeCaseKeys_mapsToCamelCase", () => {
    const fc = FrameworkConfig.fromDict({
      name: "x",
      version: "1",
      build_tool: "gradle",
      native_build: true,
    });
    expect(fc.buildTool).toBe("gradle");
    expect(fc.nativeBuild).toBe(true);
  });

  it("TestingConfig_snakeCaseKeys_mapsToCamelCase", () => {
    const tc = TestingConfig.fromDict({
      smoke_tests: false,
      contract_tests: true,
      performance_tests: false,
      coverage_line: 80,
      coverage_branch: 75,
    });
    expect(tc.smokeTests).toBe(false);
    expect(tc.contractTests).toBe(true);
    expect(tc.performanceTests).toBe(false);
    expect(tc.coverageLine).toBe(80);
    expect(tc.coverageBranch).toBe(75);
  });

  it("InfraConfig_snakeCaseKeys_mapsToCamelCase", () => {
    const ic = InfraConfig.fromDict({
      api_gateway: "kong",
      service_mesh: "istio",
    });
    expect(ic.apiGateway).toBe("kong");
    expect(ic.serviceMesh).toBe("istio");
  });
});

describe("empty object defaults", () => {
  it("TechComponent_emptyObject_returnsValidDefaults", () => {
    const tc = TechComponent.fromDict({});
    expect(tc.name).toBe("none");
    expect(tc.version).toBe("");
  });

  it("DataConfig_emptyObject_returnsValidDefaults", () => {
    const dc = DataConfig.fromDict({});
    expect(dc.database.name).toBe("none");
    expect(dc.migration.name).toBe("none");
    expect(dc.cache.name).toBe("none");
  });

  it("SecurityConfig_emptyObject_returnsValidDefaults", () => {
    const sc = SecurityConfig.fromDict({});
    expect(sc.frameworks).toEqual([]);
  });

  it("ObservabilityConfig_emptyObject_returnsValidDefaults", () => {
    const oc = ObservabilityConfig.fromDict({});
    expect(oc.tool).toBe("none");
    expect(oc.metrics).toBe("none");
    expect(oc.tracing).toBe("none");
  });

  it("InfraConfig_emptyObject_returnsValidDefaults", () => {
    const ic = InfraConfig.fromDict({});
    expect(ic.container).toBe("docker");
    expect(ic.orchestrator).toBe("none");
    expect(ic.templating).toBe("kustomize");
    expect(ic.observability.tool).toBe("none");
  });

  it("TestingConfig_emptyObject_returnsValidDefaults", () => {
    const tc = TestingConfig.fromDict({});
    expect(tc.smokeTests).toBe(true);
    expect(tc.contractTests).toBe(false);
    expect(tc.performanceTests).toBe(true);
    expect(tc.coverageLine).toBe(95);
    expect(tc.coverageBranch).toBe(90);
  });

  it("McpConfig_emptyObject_returnsValidDefaults", () => {
    const mc = McpConfig.fromDict({});
    expect(mc.servers).toEqual([]);
  });
});
