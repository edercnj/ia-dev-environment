import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import { ProtocolsAssembler } from "../../../src/assembler/protocols-assembler.js";
import { TemplateEngine } from "../../../src/template-engine.js";
import {
  ProjectConfig,
  ProjectIdentity,
  ArchitectureConfig,
  InterfaceConfig,
  LanguageConfig,
  FrameworkConfig,
} from "../../../src/models.js";

function buildConfig(overrides: {
  interfaces?: InterfaceConfig[];
} = {}): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity("my-app", "A test application"),
    new ArchitectureConfig("microservice", false, false),
    overrides.interfaces ?? [new InterfaceConfig("rest")],
    new LanguageConfig("java", "21"),
    new FrameworkConfig("quarkus", "3.0", "maven"),
  );
}

function createProtocolFile(
  resourcesDir: string,
  protocol: string,
  filename: string,
  content: string = `# ${filename}`,
): void {
  const dir = path.join(resourcesDir, "protocols", protocol);
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, filename), content, "utf-8");
}

describe("ProtocolsAssembler", () => {
  let tmpDir: string;
  let resourcesDir: string;
  let outputDir: string;
  let assembler: ProtocolsAssembler;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(
      path.join(tmpdir(), "protocols-asm-test-"),
    );
    resourcesDir = path.join(tmpDir, "resources");
    fs.mkdirSync(resourcesDir, { recursive: true });
    outputDir = path.join(tmpDir, "output");
    fs.mkdirSync(outputDir, { recursive: true });
    assembler = new ProtocolsAssembler();
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  describe("assemble", () => {
    it("returnsEmpty_whenNoProtocolsDerived", () => {
      const config = buildConfig({
        interfaces: [new InterfaceConfig("cli")],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toEqual([]);
    });

    it("returnsEmpty_whenProtocolsDerivedButNoFilesOnDisk", () => {
      const config = buildConfig({
        interfaces: [new InterfaceConfig("rest")],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toEqual([]);
    });

    it("writesRestConventions", () => {
      createProtocolFile(
        resourcesDir, "rest", "api-design.md", "# REST API",
      );
      const config = buildConfig({
        interfaces: [new InterfaceConfig("rest")],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toHaveLength(1);
      const destFile = path.join(
        outputDir, "skills", "protocols", "references",
        "rest-conventions.md",
      );
      expect(result[0]).toBe(destFile);
      expect(fs.existsSync(destFile)).toBe(true);
      expect(fs.readFileSync(destFile, "utf-8")).toBe("# REST API");
    });

    it("writesMultipleProtocolConventions", () => {
      createProtocolFile(
        resourcesDir, "rest", "rest.md", "# REST",
      );
      createProtocolFile(
        resourcesDir, "grpc", "grpc.md", "# gRPC",
      );
      const config = buildConfig({
        interfaces: [
          new InterfaceConfig("rest"),
          new InterfaceConfig("grpc"),
        ],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toHaveLength(2);
      expect(
        result.some((f) => f.endsWith("grpc-conventions.md")),
      ).toBe(true);
      expect(
        result.some((f) => f.endsWith("rest-conventions.md")),
      ).toBe(true);
    });

    it("concatenatesFilesWithSeparator", () => {
      createProtocolFile(
        resourcesDir, "rest", "a.md", "AAA",
      );
      createProtocolFile(
        resourcesDir, "rest", "b.md", "BBB",
      );
      const config = buildConfig({
        interfaces: [new InterfaceConfig("rest")],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(
          outputDir, "skills", "protocols", "references",
          "rest-conventions.md",
        ),
        "utf-8",
      );
      expect(content).toBe("AAA\n\n---\n\nBBB");
    });

    it("singleFile_noSeparator", () => {
      createProtocolFile(
        resourcesDir, "rest", "only.md", "ONLY",
      );
      const config = buildConfig({
        interfaces: [new InterfaceConfig("rest")],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(
          outputDir, "skills", "protocols", "references",
          "rest-conventions.md",
        ),
        "utf-8",
      );
      expect(content).toBe("ONLY");
      expect(content).not.toContain("---");
    });

    it("doesNotApplyTemplateEngine", () => {
      createProtocolFile(
        resourcesDir,
        "rest",
        "api.md",
        "Project: {project_name}",
      );
      const config = buildConfig({
        interfaces: [new InterfaceConfig("rest")],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(
          outputDir, "skills", "protocols", "references",
          "rest-conventions.md",
        ),
        "utf-8",
      );
      expect(content).toBe("Project: {project_name}");
    });

    it("brokerSpecificFiltering_kafkaOnly", () => {
      createProtocolFile(
        resourcesDir, "messaging", "kafka.md", "# Kafka",
      );
      createProtocolFile(
        resourcesDir, "messaging", "rabbitmq.md", "# RabbitMQ",
      );
      const config = buildConfig({
        interfaces: [
          new InterfaceConfig("event-consumer", "", "kafka"),
        ],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(
          outputDir, "skills", "protocols", "references",
          "messaging-conventions.md",
        ),
        "utf-8",
      );
      expect(content).toBe("# Kafka");
      expect(content).not.toContain("RabbitMQ");
    });

    it("brokerSpecificFiltering_fallbackAllWhenBrokerFileAbsent", () => {
      createProtocolFile(
        resourcesDir, "messaging", "rabbitmq.md", "# RabbitMQ",
      );
      createProtocolFile(
        resourcesDir, "messaging", "general.md", "# General",
      );
      const config = buildConfig({
        interfaces: [
          new InterfaceConfig(
            "event-consumer", "", "nonexistent-broker",
          ),
        ],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(
          outputDir, "skills", "protocols", "references",
          "messaging-conventions.md",
        ),
        "utf-8",
      );
      expect(content).toContain("# General");
      expect(content).toContain("# RabbitMQ");
    });

    it("noBroker_includesAllMessagingFiles", () => {
      createProtocolFile(
        resourcesDir, "messaging", "kafka.md", "# Kafka",
      );
      createProtocolFile(
        resourcesDir, "messaging", "rabbitmq.md", "# RabbitMQ",
      );
      const config = buildConfig({
        interfaces: [new InterfaceConfig("event-consumer")],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(
          outputDir, "skills", "protocols", "references",
          "messaging-conventions.md",
        ),
        "utf-8",
      );
      expect(content).toContain("# Kafka");
      expect(content).toContain("# RabbitMQ");
    });

    it("eventConsumer_derivesEventDrivenAndMessaging", () => {
      createProtocolFile(
        resourcesDir, "event-driven", "events.md", "# Events",
      );
      createProtocolFile(
        resourcesDir, "messaging", "kafka.md", "# Kafka",
      );
      const config = buildConfig({
        interfaces: [
          new InterfaceConfig("event-consumer", "", "kafka"),
        ],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(
        result.some((f) =>
          f.endsWith("event-driven-conventions.md"),
        ),
      ).toBe(true);
      expect(
        result.some((f) =>
          f.endsWith("messaging-conventions.md"),
        ),
      ).toBe(true);
    });

    it("resultsSortedByProtocolName", () => {
      createProtocolFile(
        resourcesDir, "rest", "rest.md", "# REST",
      );
      createProtocolFile(
        resourcesDir, "grpc", "grpc.md", "# gRPC",
      );
      const config = buildConfig({
        interfaces: [
          new InterfaceConfig("rest"),
          new InterfaceConfig("grpc"),
        ],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result[0]).toContain("grpc-conventions.md");
      expect(result[1]).toContain("rest-conventions.md");
    });

    it("createsReferencesDirectory", () => {
      createProtocolFile(
        resourcesDir, "rest", "rest.md",
      );
      const config = buildConfig({
        interfaces: [new InterfaceConfig("rest")],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const refsDir = path.join(
        outputDir, "skills", "protocols", "references",
      );
      expect(fs.existsSync(refsDir)).toBe(true);
      expect(fs.statSync(refsDir).isDirectory()).toBe(true);
    });
  });
});
