import { describe, it, expect } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

const CLAUDE_SOURCE = path.resolve(
  __dirname,
  "../../..",
  "resources/skills-templates/core/x-dev-lifecycle/SKILL.md",
);
const GITHUB_SOURCE = path.resolve(
  __dirname,
  "../../..",
  "resources/github-skills-templates/dev/x-dev-lifecycle.md",
);

const claudeContent = fs.readFileSync(CLAUDE_SOURCE, "utf-8");
const githubContent = fs.readFileSync(GITHUB_SOURCE, "utf-8");

function extractSection(
  content: string,
  headingPattern: RegExp,
): string {
  const match = content.match(headingPattern);
  if (!match || match.index === undefined) return "";
  const start = match.index;
  const rest = content.slice(start + match[0].length);
  const nextHeading = rest.search(/^#{2,3}\s/m);
  return nextHeading === -1
    ? content.slice(start)
    : content.slice(start, start + match[0].length + nextHeading);
}

const eventGenSection = extractSection(
  claudeContent,
  /###.*Event.Driven.*Doc.*Generator/i,
);
const githubEventGenSection = extractSection(
  githubContent,
  /###.*Event.Driven.*Doc.*Generator/i,
);

// ---------------------------------------------------------------------------
// UT-1: Event-Driven generator heading exists in Phase 3
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Event-Driven generator — degenerate", () => {
  it("claudeSource_phase3_containsEventDrivenGeneratorHeading", () => {
    expect(claudeContent).toMatch(/###.*Event.Driven.*Doc.*Generator/i);
  });

  it("githubSource_phase3_containsEventDrivenGeneratorHeading", () => {
    expect(githubContent).toMatch(/###.*Event.Driven.*Doc.*Generator/i);
  });
});

// ---------------------------------------------------------------------------
// UT-2: Generator trigger condition lists all three interface types
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Event-Driven generator — trigger condition", () => {
  it("claudeSource_eventGen_triggerContainsWebsocket", () => {
    expect(eventGenSection).toMatch(/websocket/i);
  });

  it("claudeSource_eventGen_triggerContainsEventConsumer", () => {
    expect(eventGenSection).toContain("event-consumer");
  });

  it("claudeSource_eventGen_triggerContainsEventProducer", () => {
    expect(eventGenSection).toContain("event-producer");
  });

  it("githubSource_eventGen_triggerContainsWebsocket", () => {
    expect(githubEventGenSection).toMatch(/websocket/i);
  });

  it("githubSource_eventGen_triggerContainsEventConsumer", () => {
    expect(githubEventGenSection).toContain("event-consumer");
  });

  it("githubSource_eventGen_triggerContainsEventProducer", () => {
    expect(githubEventGenSection).toContain("event-producer");
  });
});

// ---------------------------------------------------------------------------
// UT-3: Generator specifies output path docs/api/event-catalog.md
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Event-Driven generator — output path", () => {
  it("claudeSource_eventGen_outputPathEventCatalog", () => {
    expect(eventGenSection).toContain("docs/api/event-catalog.md");
  });

  it("githubSource_eventGen_outputPathEventCatalog", () => {
    expect(githubEventGenSection).toContain("docs/api/event-catalog.md");
  });
});

// ---------------------------------------------------------------------------
// UT-4: Topics Overview table
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Event-Driven generator — topics overview", () => {
  it("claudeSource_eventGen_containsTopicsOverview", () => {
    expect(eventGenSection).toMatch(/[Tt]opics?\s+[Oo]verview/);
  });

  it("claudeSource_eventGen_topicsTableHasPartitioning", () => {
    expect(eventGenSection).toMatch(/[Pp]artition/i);
  });

  it("githubSource_eventGen_containsTopicsOverview", () => {
    expect(githubEventGenSection).toMatch(/[Tt]opics?\s+[Oo]verview/);
  });
});

// ---------------------------------------------------------------------------
// UT-5: Per-event sections with payload schema
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Event-Driven generator — payload schema", () => {
  it("claudeSource_eventGen_payloadSchemaTable", () => {
    expect(eventGenSection).toMatch(/[Pp]ayload.*[Ss]chema/i);
  });

  it("claudeSource_eventGen_payloadFieldColumn", () => {
    expect(eventGenSection).toMatch(/[Ff]ield/i);
  });

  it("claudeSource_eventGen_payloadTypeColumn", () => {
    expect(eventGenSection).toMatch(/[Tt]ype/i);
  });

  it("claudeSource_eventGen_payloadRequiredColumn", () => {
    expect(eventGenSection).toMatch(/[Rr]equired/i);
  });

  it("claudeSource_eventGen_payloadDescriptionColumn", () => {
    expect(eventGenSection).toMatch(/[Dd]escription/i);
  });

  it("githubSource_eventGen_payloadSchemaTable", () => {
    expect(githubEventGenSection).toMatch(/[Pp]ayload.*[Ss]chema/i);
  });
});

// ---------------------------------------------------------------------------
// UT-6: Producer/consumer contract fields
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Event-Driven generator — producer/consumer", () => {
  it("claudeSource_eventGen_containsProducer", () => {
    expect(eventGenSection).toMatch(/[Pp]roducer/i);
  });

  it("claudeSource_eventGen_containsConsumer", () => {
    expect(eventGenSection).toMatch(/[Cc]onsumer/i);
  });

  it("claudeSource_eventGen_containsTopicOrChannel", () => {
    expect(eventGenSection).toMatch(/[Tt]opic|[Cc]hannel/i);
  });

  it("githubSource_eventGen_containsProducerAndConsumer", () => {
    expect(githubEventGenSection).toMatch(/[Pp]roducer/);
    expect(githubEventGenSection).toMatch(/[Cc]onsumer/);
  });
});

// ---------------------------------------------------------------------------
// UT-7: Mermaid event flow diagrams
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Event-Driven generator — Mermaid diagrams", () => {
  it("claudeSource_eventGen_containsMermaidDiagram", () => {
    expect(eventGenSection).toMatch(/[Mm]ermaid/i);
  });

  it("claudeSource_eventGen_containsSequenceDiagram", () => {
    expect(eventGenSection).toMatch(/sequenceDiagram|sequence.*diagram/i);
  });

  it("githubSource_eventGen_containsMermaidDiagram", () => {
    expect(githubEventGenSection).toMatch(/[Mm]ermaid/i);
  });
});

// ---------------------------------------------------------------------------
// UT-8: Protocol knowledge pack references
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Event-Driven generator — protocol references", () => {
  it("claudeSource_eventGen_referencesEventDrivenConventions", () => {
    expect(eventGenSection).toMatch(
      /event.driven.conventions|protocols.*event/i,
    );
  });

  it("claudeSource_eventGen_referencesWebsocketConventions", () => {
    expect(eventGenSection).toMatch(
      /websocket.conventions|protocols.*websocket/i,
    );
  });

  it("githubSource_eventGen_referencesProtocolConventions", () => {
    expect(githubEventGenSection).toMatch(/conventions|protocols/i);
  });
});

// ---------------------------------------------------------------------------
// UT-9: CloudEvents envelope
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Event-Driven generator — CloudEvents", () => {
  it("claudeSource_eventGen_containsCloudEvents", () => {
    expect(eventGenSection).toMatch(/[Cc]loud[Ee]vents?/);
  });

  it("githubSource_eventGen_containsCloudEvents", () => {
    expect(githubEventGenSection).toMatch(/[Cc]loud[Ee]vents?/);
  });
});

// ---------------------------------------------------------------------------
// UT-10: Schema versioning
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Event-Driven generator — versioning", () => {
  it("claudeSource_eventGen_containsVersioning", () => {
    expect(eventGenSection).toMatch(
      /[Vv]ersion|[Bb]ackward.*[Cc]ompati/i,
    );
  });

  it("githubSource_eventGen_containsVersioning", () => {
    expect(githubEventGenSection).toMatch(
      /[Vv]ersion|[Bb]ackward.*[Cc]ompati/i,
    );
  });
});

// ---------------------------------------------------------------------------
// UT-11: Generator positioned within Phase 3 (between Phase 3 and Phase 4)
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Event-Driven generator — positioning", () => {
  it("claudeSource_eventGen_afterPhase3Heading", () => {
    const phase3Idx = claudeContent.search(/## Phase 3/);
    const eventGenIdx = claudeContent.search(
      /###.*Event.Driven.*Doc.*Generator/i,
    );
    expect(phase3Idx).toBeGreaterThan(-1);
    expect(eventGenIdx).toBeGreaterThan(-1);
    expect(eventGenIdx).toBeGreaterThan(phase3Idx);
  });

  it("claudeSource_eventGen_beforePhase4Heading", () => {
    const eventGenIdx = claudeContent.search(
      /###.*Event.Driven.*Doc.*Generator/i,
    );
    const phase4Idx = claudeContent.search(/## Phase 4/);
    expect(eventGenIdx).toBeGreaterThan(-1);
    expect(phase4Idx).toBeGreaterThan(-1);
    expect(eventGenIdx).toBeLessThan(phase4Idx);
  });

  it("githubSource_eventGen_afterPhase3Heading", () => {
    const phase3Idx = githubContent.search(/## Phase 3/);
    const eventGenIdx = githubContent.search(
      /###.*Event.Driven.*Doc.*Generator/i,
    );
    expect(phase3Idx).toBeGreaterThan(-1);
    expect(eventGenIdx).toBeGreaterThan(-1);
    expect(eventGenIdx).toBeGreaterThan(phase3Idx);
  });

  it("githubSource_eventGen_beforePhase4Heading", () => {
    const eventGenIdx = githubContent.search(
      /###.*Event.Driven.*Doc.*Generator/i,
    );
    const phase4Idx = githubContent.search(/## Phase 4/);
    expect(eventGenIdx).toBeGreaterThan(-1);
    expect(phase4Idx).toBeGreaterThan(-1);
    expect(eventGenIdx).toBeLessThan(phase4Idx);
  });
});

// ---------------------------------------------------------------------------
// UT-12: Dispatch table entry references Event-Driven generator
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Event-Driven generator — dispatch table", () => {
  it("claudeSource_dispatchTable_storyReference", () => {
    expect(claudeContent).toContain("story-0004-0010");
  });

  it("githubSource_dispatchTable_storyReference", () => {
    expect(githubContent).toContain("story-0004-0010");
  });
});

// ---------------------------------------------------------------------------
// UT-13: WebSocket vs Kafka differentiation
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Event-Driven generator — WebSocket vs Kafka", () => {
  it("claudeSource_eventGen_websocketChannels", () => {
    expect(eventGenSection).toMatch(
      /[Cc]hannel|[Ww]eb[Ss]ocket.*message/i,
    );
  });

  it("claudeSource_eventGen_kafkaTopics", () => {
    expect(eventGenSection).toMatch(
      /[Tt]opic|[Pp]artition.*key|[Cc]onsumer.*group/i,
    );
  });

  it("claudeSource_eventGen_handlesMultipleProtocols", () => {
    expect(eventGenSection).toMatch(/both|unified|multiple/i);
  });
});

// ---------------------------------------------------------------------------
// UT-14: Structural preservation — existing Phase 3 elements intact
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Event-Driven generator — backward compat", () => {
  it("claudeSource_phase3_preservesInterfaceFieldRead", () => {
    expect(claudeContent).toMatch(
      /Read.*interfaces.*field|interfaces.*field.*from/i,
    );
  });

  it("claudeSource_phase3_preservesDispatchTable", () => {
    expect(claudeContent).toMatch(/rest.*OpenAPI|OpenAPI.*Swagger/i);
    expect(claudeContent).toMatch(/grpc.*Proto|gRPC.*doc/i);
    expect(claudeContent).toMatch(/cli.*doc|CLI.*generator/i);
  });

  it("claudeSource_phase3_preservesChangelogGeneration", () => {
    expect(claudeContent).toMatch(/changelog.*entry|CHANGELOG\.md/i);
  });

  it("claudeSource_phase3_preservesNoInterfaceSkipLog", () => {
    expect(claudeContent).toMatch(/[Nn]o documentable interfaces/);
  });

  it("claudeSource_phase3_preservesDocsOutputPaths", () => {
    expect(claudeContent).toContain("docs/api/");
    expect(claudeContent).toContain("docs/architecture/");
  });
});

// ---------------------------------------------------------------------------
// IT-2: Negative validation — trigger does not match cli
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Event-Driven generator — negative validation", () => {
  it("triggerCondition_requiresExplicitEventInterfaces", () => {
    expect(eventGenSection).toContain("websocket");
    expect(eventGenSection).toContain("event-consumer");
    expect(eventGenSection).toContain("event-producer");
  });

  it("triggerCondition_doesNotMatchCliInterface", () => {
    const triggerLine = eventGenSection
      .split("\n")
      .find(
        (line) =>
          line.toLowerCase().includes("trigger") ||
          line.toLowerCase().includes("invoke when") ||
          line.toLowerCase().includes("invoke if"),
      );
    if (triggerLine) {
      expect(triggerLine).not.toMatch(/\bcli\b/);
    }
  });

  it("dispatchTable_cliMapsToCliGenerator", () => {
    expect(claudeContent).toMatch(/cli.*CLI.*doc.*generator/i);
  });
});

// ---------------------------------------------------------------------------
// UT-15: Dual copy consistency (RULE-001)
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Event-Driven generator — dual copy (RULE-001)", () => {
  it("dualCopy_bothContainEventDrivenGeneratorHeading", () => {
    expect(claudeContent).toMatch(/###.*Event.Driven.*Doc.*Generator/i);
    expect(githubContent).toMatch(/###.*Event.Driven.*Doc.*Generator/i);
  });

  it("dualCopy_bothContainTriggerConditionWebsocket", () => {
    expect(eventGenSection).toContain("websocket");
    expect(githubEventGenSection).toContain("websocket");
  });

  it("dualCopy_bothContainTriggerConditionEventConsumer", () => {
    expect(eventGenSection).toContain("event-consumer");
    expect(githubEventGenSection).toContain("event-consumer");
  });

  it("dualCopy_bothContainTriggerConditionEventProducer", () => {
    expect(eventGenSection).toContain("event-producer");
    expect(githubEventGenSection).toContain("event-producer");
  });

  it("dualCopy_bothContainOutputPath", () => {
    expect(eventGenSection).toContain("docs/api/event-catalog.md");
    expect(githubEventGenSection).toContain("docs/api/event-catalog.md");
  });

  it("dualCopy_bothContainTopicsOverview", () => {
    expect(eventGenSection).toMatch(/[Tt]opics?\s+[Oo]verview/);
    expect(githubEventGenSection).toMatch(/[Tt]opics?\s+[Oo]verview/);
  });

  it("dualCopy_bothContainPayloadSchema", () => {
    expect(eventGenSection).toMatch(/[Pp]ayload.*[Ss]chema/i);
    expect(githubEventGenSection).toMatch(/[Pp]ayload.*[Ss]chema/i);
  });

  it("dualCopy_bothContainMermaidDiagram", () => {
    expect(eventGenSection).toMatch(/[Mm]ermaid/i);
    expect(githubEventGenSection).toMatch(/[Mm]ermaid/i);
  });

  it("dualCopy_bothContainCloudEvents", () => {
    expect(eventGenSection).toMatch(/[Cc]loud[Ee]vents?/);
    expect(githubEventGenSection).toMatch(/[Cc]loud[Ee]vents?/);
  });

  it("dualCopy_bothContainProducerConsumer", () => {
    expect(eventGenSection).toMatch(/[Pp]roducer/);
    expect(githubEventGenSection).toMatch(/[Pp]roducer/);
    expect(eventGenSection).toMatch(/[Cc]onsumer/);
    expect(githubEventGenSection).toMatch(/[Cc]onsumer/);
  });

  it("dualCopy_bothContainStoryReference", () => {
    expect(claudeContent).toContain("story-0004-0010");
    expect(githubContent).toContain("story-0004-0010");
  });
});
