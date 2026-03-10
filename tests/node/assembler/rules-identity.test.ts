import { describe, it, expect } from "vitest";
import {
  buildIdentityContent,
  fallbackDomainContent,
} from "../../../src/assembler/rules-identity.js";
import { aProjectConfig } from "../../fixtures/project-config.fixture.js";
import {
  ProjectConfig,
  ProjectIdentity,
  ArchitectureConfig,
  InterfaceConfig,
  LanguageConfig,
  FrameworkConfig,
  DataConfig,
  TechComponent,
  InfraConfig,
  ObservabilityConfig,
  SecurityConfig,
  TestingConfig,
} from "../../../src/models.js";

describe("buildIdentityContent", () => {
  it("containsProjectName_inHeaderAndIdentity", () => {
    const config = aProjectConfig();
    const content = buildIdentityContent(config);
    expect(content).toContain("# Project Identity — my-service");
    expect(content).toContain("- **Name:** my-service");
  });

  it("containsPurpose", () => {
    const config = aProjectConfig();
    const content = buildIdentityContent(config);
    expect(content).toContain("- **Purpose:** A sample service");
  });

  it("containsArchitectureStyle", () => {
    const config = aProjectConfig();
    const content = buildIdentityContent(config);
    expect(content).toContain("- **Architecture Style:** hexagonal");
    expect(content).toContain("| Architecture | hexagonal |");
  });

  it("containsDomainDrivenAsLowercase", () => {
    const config = aProjectConfig();
    const content = buildIdentityContent(config);
    expect(content).toContain("- **Domain-Driven Design:** true");
  });

  it("containsEventDrivenAsLowercase", () => {
    const config = aProjectConfig();
    const content = buildIdentityContent(config);
    expect(content).toContain("- **Event-Driven:** false");
  });

  it("containsLanguageAndVersion", () => {
    const config = aProjectConfig();
    const content = buildIdentityContent(config);
    expect(content).toContain("- **Language:** python 3.9");
    expect(content).toContain("| Language | python 3.9 |");
  });

  it("containsFrameworkWithVersion", () => {
    const config = aProjectConfig();
    const content = buildIdentityContent(config);
    expect(content).toContain("- **Framework:** click 8.1");
    expect(content).toContain("| Framework | click 8.1 |");
  });

  it("containsInterfaceTypes", () => {
    const config = aProjectConfig();
    const content = buildIdentityContent(config);
    expect(content).toContain("- **Interfaces:** cli");
  });

  it("multipleInterfaces_joinedWithComma", () => {
    const config = new ProjectConfig(
      new ProjectIdentity("test", "test"),
      new ArchitectureConfig("microservice"),
      [new InterfaceConfig("rest"), new InterfaceConfig("grpc")],
      new LanguageConfig("java", "21"),
      new FrameworkConfig("quarkus", "3.0", "maven"),
    );
    const content = buildIdentityContent(config);
    expect(content).toContain("- **Interfaces:** rest, grpc");
  });

  it("noInterfaces_showsNone", () => {
    const config = new ProjectConfig(
      new ProjectIdentity("test", "test"),
      new ArchitectureConfig("library"),
      [],
      new LanguageConfig("typescript", "5"),
      new FrameworkConfig("commander", "12", "npm"),
    );
    const content = buildIdentityContent(config);
    expect(content).toContain("- **Interfaces:** none");
  });

  it("containsTechStackTable", () => {
    const config = aProjectConfig();
    const content = buildIdentityContent(config);
    expect(content).toContain("## Technology Stack");
    expect(content).toContain("| Layer | Technology |");
    expect(content).toContain("| Database | postgresql |");
    expect(content).toContain("| Cache | redis |");
    expect(content).toContain("| Container | docker |");
    expect(content).toContain("| Orchestrator | kubernetes |");
  });

  it("containsObservabilityWithTracing", () => {
    const config = new ProjectConfig(
      new ProjectIdentity("test", "test"),
      new ArchitectureConfig("microservice"),
      [new InterfaceConfig("rest")],
      new LanguageConfig("java", "21"),
      new FrameworkConfig("quarkus", "3.0", "maven"),
      new DataConfig(),
      new InfraConfig(
        "docker", "kubernetes", "kustomize", "none", "none", "none", "none",
        new ObservabilityConfig("jaeger", "prometheus", "opentelemetry"),
      ),
    );
    const content = buildIdentityContent(config);
    expect(content).toContain("| Observability | jaeger (opentelemetry) |");
  });

  it("containsTestingFlags", () => {
    const config = aProjectConfig();
    const content = buildIdentityContent(config);
    expect(content).toContain("| Smoke Tests | true |");
    expect(content).toContain("| Contract Tests | false |");
  });

  it("containsNativeBuild", () => {
    const config = aProjectConfig();
    const content = buildIdentityContent(config);
    expect(content).toContain("| Native Build | false |");
  });

  it("containsSourceOfTruthHierarchy", () => {
    const config = aProjectConfig();
    const content = buildIdentityContent(config);
    expect(content).toContain("## Source of Truth (Hierarchy)");
    expect(content).toContain("1. Epics / PRDs (vision and global rules)");
  });

  it("containsLanguageSection", () => {
    const config = aProjectConfig();
    const content = buildIdentityContent(config);
    expect(content).toContain("## Language");
    expect(content).toContain("- Code: English (classes, methods, variables)");
  });

  it("containsConstraintsSection", () => {
    const config = aProjectConfig();
    const content = buildIdentityContent(config);
    expect(content).toContain("## Constraints");
    expect(content).toContain("- Cloud-Agnostic: ZERO dependencies on cloud-specific services");
  });

  it("containsGlobalBehaviorHeader", () => {
    const config = aProjectConfig();
    const content = buildIdentityContent(config);
    expect(content).toContain("# Global Behavior & Language Policy");
    expect(content).toContain("- **Output Language**: English ONLY.");
  });

  it("endsWithNewline", () => {
    const config = aProjectConfig();
    const content = buildIdentityContent(config);
    expect(content.endsWith("\n")).toBe(true);
  });

  it("frameworkWithEmptyVersion_noTrailingSpace", () => {
    const config = new ProjectConfig(
      new ProjectIdentity("test", "test"),
      new ArchitectureConfig("library"),
      [],
      new LanguageConfig("typescript", "5"),
      new FrameworkConfig("commander", "", "npm"),
    );
    const content = buildIdentityContent(config);
    expect(content).toContain("- **Framework:** commander");
    expect(content).not.toContain("- **Framework:** commander ");
  });
});

describe("fallbackDomainContent", () => {
  it("containsProjectName", () => {
    const config = aProjectConfig();
    const content = fallbackDomainContent(config);
    expect(content).toContain("my-service");
  });

  it("containsDomainNamePlaceholder", () => {
    const config = aProjectConfig();
    const content = fallbackDomainContent(config);
    expect(content).toContain("{DOMAIN_NAME}");
  });

  it("endsWithNewline", () => {
    const config = aProjectConfig();
    const content = fallbackDomainContent(config);
    expect(content.endsWith("\n")).toBe(true);
  });
});
