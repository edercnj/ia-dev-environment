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
