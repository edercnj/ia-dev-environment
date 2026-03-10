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
 * Build a ProjectConfig matching the pre-existing test fixtures
 * (simple_rendered.md, multivar_rendered.md, legacy_replaced.txt, etc.).
 *
 * Values: my-service / python 3.9 / click 8.1 / hexagonal / postgresql / redis.
 */
/**
 * Build a ProjectConfig for validator/resolver tests with fine-grained overrides.
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
