import {
  ProjectConfig,
  ProjectIdentity,
  ArchitectureConfig,
  InterfaceConfig,
  LanguageConfig,
  FrameworkConfig,
  DataConfig,
  InfraConfig,
  SecurityConfig,
  TechComponent,
  TestingConfig,
  ObservabilityConfig,
} from "../../src/models.js";

/**
 * Build a minimal ProjectConfig for domain mapping tests.
 *
 * Overrides: architecture style, event_driven flag, interfaces.
 */
export function aDomainTestConfig(
  overrides: {
    style?: string;
    eventDriven?: boolean;
    interfaces?: InterfaceConfig[];
  } = {},
): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity("test", "test"),
    new ArchitectureConfig(
      overrides.style ?? "microservice",
      false,
      overrides.eventDriven ?? false,
    ),
    overrides.interfaces ?? [new InterfaceConfig("rest")],
    new LanguageConfig("java", "21"),
    new FrameworkConfig("quarkus", "3.0", "maven"),
  );
}

/**
 * Build a ProjectConfig for validator/resolver tests with fine-grained overrides.
 *
 * Allows overriding language, framework, architecture style, and interfaces
 * while keeping sensible default values suitable for validation fixtures.
 */
export function aValidationTestConfig(
  overrides: {
    language?: { name: string; version: string };
    framework?: {
      name: string; version: string;
      buildTool?: string; nativeBuild?: boolean;
    };
    architecture?: { style: string };
    interfaces?: Array<{ type: string }>;
  } = {},
): ProjectConfig {
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

/**
 * Build a ProjectConfig matching the pre-existing test fixtures
 * (simple_rendered.md, multivar_rendered.md, legacy_replaced.txt, etc.).
 *
 * Values: my-service / python 3.9 / click 8.1 / hexagonal / postgresql / redis.
 */
export function aProjectConfig(): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity("my-service", "A sample service"),
    new ArchitectureConfig("hexagonal", true, false),
    [new InterfaceConfig("cli")],
    new LanguageConfig("python", "3.9"),
    new FrameworkConfig("click", "8.1", "pip"),
    new DataConfig(
      new TechComponent("postgresql"),
      new TechComponent(),
      new TechComponent("redis"),
    ),
    new InfraConfig("docker", "kubernetes"),
    new SecurityConfig(),
    new TestingConfig(true, false, true, 95, 90),
  );
}

/**
 * Build a minimal ProjectConfig with conditional sections disabled.
 *
 * Values: minimal-api / go 1.22 / gin 1.9 / layered / no database / no cache.
 * domain_driven=false, security_frameworks=[], orchestrator="none".
 */
export function aMinimalProjectConfig(): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity("minimal-api", "A minimal API service"),
    new ArchitectureConfig("layered", false, false),
    [new InterfaceConfig("rest")],
    new LanguageConfig("go", "1.22"),
    new FrameworkConfig("gin", "1.9", "go"),
    new DataConfig(),
    new InfraConfig("docker", "none"),
    new SecurityConfig(),
    new TestingConfig(false, false, false, 80, 70),
  );
}

/**
 * Build a ProjectConfig for gRPC-enabled projects.
 *
 * Values: my-grpc-service / go 1.22 / gin 1.9 / microservice / grpc(proto3).
 */
export function aGrpcProjectConfig(): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity("my-grpc-service", "A gRPC service"),
    new ArchitectureConfig("microservice"),
    [new InterfaceConfig("grpc", "proto3")],
    new LanguageConfig("go", "1.22"),
    new FrameworkConfig("gin", "1.9", "go-mod"),
  );
}

/**
 * Build a full ProjectConfig with all conditional sections enabled.
 *
 * Values: my-service / python 3.9 / click 8.1 / hexagonal / postgresql / redis.
 * domain_driven=true, security_frameworks=["owasp","pci-dss"], orchestrator="kubernetes".
 */
export function aFullProjectConfig(): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity("my-service", "A sample service"),
    new ArchitectureConfig("hexagonal", true, false),
    [new InterfaceConfig("cli")],
    new LanguageConfig("python", "3.9"),
    new FrameworkConfig("click", "8.1", "pip"),
    new DataConfig(
      new TechComponent("postgresql"),
      new TechComponent(),
      new TechComponent("redis"),
    ),
    new InfraConfig(
      "docker",
      "kubernetes",
      "kustomize",
      "none",
      "none",
      "none",
      "none",
      new ObservabilityConfig("opentelemetry"),
    ),
    new SecurityConfig(["owasp", "pci-dss"]),
    new TestingConfig(true, false, true, 95, 90),
  );
}
