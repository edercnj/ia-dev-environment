import { describe, it, expect, beforeEach, afterEach } from "vitest";
import { mkdirSync, rmSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { tmpdir } from "node:os";
import {
  INTERFACE_PROTOCOL_MAP,
  EVENT_PREFIX,
  EVENT_DRIVEN_PROTOCOL,
  deriveProtocols,
  deriveProtocolFiles,
  selectMessagingFiles,
  extractBroker,
} from "../../../src/domain/protocol-mapping.js";
import { InterfaceConfig } from "../../../src/models.js";
import { aDomainTestConfig } from "../../fixtures/project-config.fixture.js";

describe("constants", () => {
  it("interfaceProtocolMap_has7Entries", () => {
    expect(Object.keys(INTERFACE_PROTOCOL_MAP)).toHaveLength(7);
  });

  it("eventPrefix_isEventDash", () => {
    expect(EVENT_PREFIX).toBe("event-");
  });

  it("eventDrivenProtocol_isEventDriven", () => {
    expect(EVENT_DRIVEN_PROTOCOL).toBe("event-driven");
  });

  it("cli_mapsToEmptyArray", () => {
    expect(INTERFACE_PROTOCOL_MAP["cli"]).toEqual([]);
  });

  it("eventConsumer_mapsToEventDrivenAndMessaging", () => {
    expect(INTERFACE_PROTOCOL_MAP["event-consumer"]).toEqual(["event-driven", "messaging"]);
  });
});

describe("deriveProtocols", () => {
  it("restInterface_returnsRest", () => {
    const config = aDomainTestConfig({ interfaces: [new InterfaceConfig("rest")] });
    expect(deriveProtocols(config)).toEqual(["rest"]);
  });

  it("multipleInterfaces_returnsDeduplicated", () => {
    const config = aDomainTestConfig({
      interfaces: [
        new InterfaceConfig("rest"),
        new InterfaceConfig("grpc"),
        new InterfaceConfig("event-consumer"),
      ],
    });
    const result = deriveProtocols(config);
    expect(result).toContain("rest");
    expect(result).toContain("grpc");
    expect(result).toContain("event-driven");
    expect(result).toContain("messaging");
  });

  it("duplicateInterfaces_returnsDeduplicated", () => {
    const config = aDomainTestConfig({
      interfaces: [
        new InterfaceConfig("event-consumer"),
        new InterfaceConfig("event-producer"),
      ],
    });
    const result = deriveProtocols(config);
    expect(result).toEqual(["event-driven", "messaging"]);
  });

  it("resultIsSorted", () => {
    const config = aDomainTestConfig({
      interfaces: [
        new InterfaceConfig("grpc"),
        new InterfaceConfig("rest"),
        new InterfaceConfig("event-consumer"),
      ],
    });
    const result = deriveProtocols(config);
    const sorted = [...result].sort();
    expect(result).toEqual(sorted);
  });

  it("customEventType_addsEventDriven", () => {
    const config = aDomainTestConfig({ interfaces: [new InterfaceConfig("event-custom-type")] });
    const result = deriveProtocols(config);
    expect(result).toContain("event-driven");
  });

  it("cliInterface_returnsEmpty", () => {
    const config = aDomainTestConfig({ interfaces: [new InterfaceConfig("cli")] });
    expect(deriveProtocols(config)).toEqual([]);
  });
});

describe("extractBroker", () => {
  it("noBroker_returnsEmpty", () => {
    const config = aDomainTestConfig({ interfaces: [new InterfaceConfig("rest")] });
    expect(extractBroker(config)).toBe("");
  });

  it("hasBroker_returnsBrokerName", () => {
    const config = aDomainTestConfig({
      interfaces: [new InterfaceConfig("event-consumer", "", "kafka")],
    });
    expect(extractBroker(config)).toBe("kafka");
  });

  it("multipleBrokers_returnsFirst", () => {
    const config = aDomainTestConfig({
      interfaces: [
        new InterfaceConfig("event-consumer", "", "rabbitmq"),
        new InterfaceConfig("event-producer", "", "kafka"),
      ],
    });
    expect(extractBroker(config)).toBe("rabbitmq");
  });
});

describe("deriveProtocolFiles", () => {
  let tmpDir: string;

  beforeEach(() => {
    tmpDir = join(tmpdir(), `protocol-test-${Date.now()}`);
    mkdirSync(join(tmpDir, "protocols", "rest"), { recursive: true });
    writeFileSync(join(tmpDir, "protocols", "rest", "openapi.md"), "");
  });

  afterEach(() => {
    rmSync(tmpDir, { recursive: true, force: true });
  });

  it("existingProtocol_returnsFiles", () => {
    const config = aDomainTestConfig({ interfaces: [new InterfaceConfig("rest")] });
    const result = deriveProtocolFiles(tmpDir, ["rest"], config);
    expect(result["rest"]).toHaveLength(1);
    expect(result["rest"]![0]).toContain("openapi.md");
  });

  it("missingProtocol_skipsWithoutError", () => {
    const config = aDomainTestConfig({ interfaces: [new InterfaceConfig("rest")] });
    const result = deriveProtocolFiles(tmpDir, ["nonexistent"], config);
    expect(result).toEqual({});
  });
});

describe("selectMessagingFiles", () => {
  let tmpDir: string;

  beforeEach(() => {
    tmpDir = join(tmpdir(), `messaging-test-${Date.now()}`);
    mkdirSync(tmpDir, { recursive: true });
    writeFileSync(join(tmpDir, "kafka.md"), "");
    writeFileSync(join(tmpDir, "rabbitmq.md"), "");
  });

  afterEach(() => {
    rmSync(tmpDir, { recursive: true, force: true });
  });

  it("withBroker_returnsSpecificFile", () => {
    const config = aDomainTestConfig({
      interfaces: [new InterfaceConfig("event-consumer", "", "kafka")],
    });
    const files = selectMessagingFiles(tmpDir, config);
    expect(files).toHaveLength(1);
    expect(files[0]).toContain("kafka.md");
  });

  it("withoutBroker_returnsAllFiles", () => {
    const config = aDomainTestConfig({ interfaces: [new InterfaceConfig("event-consumer")] });
    const files = selectMessagingFiles(tmpDir, config);
    expect(files).toHaveLength(2);
  });

  it("withUnknownBroker_returnsAllFiles", () => {
    const config = aDomainTestConfig({
      interfaces: [new InterfaceConfig("event-consumer", "", "unknown-broker")],
    });
    const files = selectMessagingFiles(tmpDir, config);
    expect(files).toHaveLength(2);
  });
});
