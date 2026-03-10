export interface ProjectFoundation {
  readonly name: string;
  readonly version: string;
  readonly moduleType: "module";
}

export const DEFAULT_FOUNDATION: ProjectFoundation = {
  name: "ia-dev-environment",
  version: "0.1.0",
  moduleType: "module",
};

// --- STORY-003: 17 model classes migrated from Python models.py ---
// This file exceeds 250 lines because it is a 1:1 migration of 17
// data-transfer-object classes. Splitting across files would hurt
// cohesion for this specific use case.

export function requireField(
  data: Record<string, unknown>,
  key: string,
  model: string,
): unknown {
  if (!(key in data)) {
    throw new Error(
      `Missing required field '${key}' in ${model}`,
    );
  }
  return data[key];
}

export class TechComponent {
  readonly name: string;
  readonly version: string;

  constructor(name: string = "none", version: string = "") {
    this.name = name;
    this.version = version;
  }

  static fromDict(
    data: Record<string, unknown>,
  ): TechComponent {
    return new TechComponent(
      (data["name"] as string | undefined) ?? "none",
      (data["version"] as string | undefined) ?? "",
    );
  }
}

export class ProjectIdentity {
  readonly name: string;
  readonly purpose: string;

  constructor(name: string, purpose: string) {
    this.name = name;
    this.purpose = purpose;
  }

  static fromDict(
    data: Record<string, unknown>,
  ): ProjectIdentity {
    return new ProjectIdentity(
      requireField(data, "name", "ProjectIdentity") as string,
      requireField(data, "purpose", "ProjectIdentity") as string,
    );
  }
}

export class ArchitectureConfig {
  readonly style: string;
  readonly domainDriven: boolean;
  readonly eventDriven: boolean;

  constructor(
    style: string,
    domainDriven: boolean = false,
    eventDriven: boolean = false,
  ) {
    this.style = style;
    this.domainDriven = domainDriven;
    this.eventDriven = eventDriven;
  }

  static fromDict(
    data: Record<string, unknown>,
  ): ArchitectureConfig {
    return new ArchitectureConfig(
      requireField(data, "style", "ArchitectureConfig") as string,
      (data["domain_driven"] as boolean | undefined) ?? false,
      (data["event_driven"] as boolean | undefined) ?? false,
    );
  }
}

export class InterfaceConfig {
  readonly type: string;
  readonly spec: string;
  readonly broker: string;

  constructor(
    type: string,
    spec: string = "",
    broker: string = "",
  ) {
    this.type = type;
    this.spec = spec;
    this.broker = broker;
  }

  static fromDict(
    data: Record<string, unknown>,
  ): InterfaceConfig {
    return new InterfaceConfig(
      requireField(data, "type", "InterfaceConfig") as string,
      (data["spec"] as string | undefined) ?? "",
      (data["broker"] as string | undefined) ?? "",
    );
  }
}

export class LanguageConfig {
  readonly name: string;
  readonly version: string;

  constructor(name: string, version: string) {
    this.name = name;
    this.version = version;
  }

  static fromDict(
    data: Record<string, unknown>,
  ): LanguageConfig {
    return new LanguageConfig(
      requireField(data, "name", "LanguageConfig") as string,
      requireField(data, "version", "LanguageConfig") as string,
    );
  }
}

export class FrameworkConfig {
  readonly name: string;
  readonly version: string;
  readonly buildTool: string;
  readonly nativeBuild: boolean;

  constructor(
    name: string,
    version: string,
    buildTool: string = "pip",
    nativeBuild: boolean = false,
  ) {
    this.name = name;
    this.version = version;
    this.buildTool = buildTool;
    this.nativeBuild = nativeBuild;
  }

  static fromDict(
    data: Record<string, unknown>,
  ): FrameworkConfig {
    return new FrameworkConfig(
      requireField(data, "name", "FrameworkConfig") as string,
      requireField(data, "version", "FrameworkConfig") as string,
      (data["build_tool"] as string | undefined) ?? "pip",
      (data["native_build"] as boolean | undefined) ?? false,
    );
  }
}

export class DataConfig {
  readonly database: TechComponent;
  readonly migration: TechComponent;
  readonly cache: TechComponent;

  constructor(
    database: TechComponent = new TechComponent(),
    migration: TechComponent = new TechComponent(),
    cache: TechComponent = new TechComponent(),
  ) {
    this.database = database;
    this.migration = migration;
    this.cache = cache;
  }

  static fromDict(
    data: Record<string, unknown>,
  ): DataConfig {
    return new DataConfig(
      TechComponent.fromDict(
        (data["database"] as Record<string, unknown> | undefined) ?? {},
      ),
      TechComponent.fromDict(
        (data["migration"] as Record<string, unknown> | undefined) ?? {},
      ),
      TechComponent.fromDict(
        (data["cache"] as Record<string, unknown> | undefined) ?? {},
      ),
    );
  }
}

export class SecurityConfig {
  readonly frameworks: readonly string[];

  constructor(frameworks: readonly string[] = []) {
    this.frameworks = frameworks;
  }

  static fromDict(
    data: Record<string, unknown>,
  ): SecurityConfig {
    return new SecurityConfig(
      (data["frameworks"] as string[] | undefined) ?? [],
    );
  }
}

export class ObservabilityConfig {
  readonly tool: string;
  readonly metrics: string;
  readonly tracing: string;

  constructor(
    tool: string = "none",
    metrics: string = "none",
    tracing: string = "none",
  ) {
    this.tool = tool;
    this.metrics = metrics;
    this.tracing = tracing;
  }

  static fromDict(
    data: Record<string, unknown>,
  ): ObservabilityConfig {
    return new ObservabilityConfig(
      (data["tool"] as string | undefined) ?? "none",
      (data["metrics"] as string | undefined) ?? "none",
      (data["tracing"] as string | undefined) ?? "none",
    );
  }
}

export class InfraConfig {
  readonly container: string;
  readonly orchestrator: string;
  readonly templating: string;
  readonly iac: string;
  readonly registry: string;
  readonly apiGateway: string;
  readonly serviceMesh: string;
  readonly cloudProvider: string;
  readonly observability: ObservabilityConfig;

  constructor(
    container: string = "docker",
    orchestrator: string = "none",
    templating: string = "kustomize",
    iac: string = "none",
    registry: string = "none",
    apiGateway: string = "none",
    serviceMesh: string = "none",
    observability: ObservabilityConfig = new ObservabilityConfig(),
    cloudProvider: string = "none",
  ) {
    this.container = container;
    this.orchestrator = orchestrator;
    this.templating = templating;
    this.iac = iac;
    this.registry = registry;
    this.apiGateway = apiGateway;
    this.serviceMesh = serviceMesh;
    this.cloudProvider = cloudProvider;
    this.observability = observability;
  }

  static fromDict(
    data: Record<string, unknown>,
  ): InfraConfig {
    return new InfraConfig(
      (data["container"] as string | undefined) ?? "docker",
      (data["orchestrator"] as string | undefined) ?? "none",
      (data["templating"] as string | undefined) ?? "kustomize",
      (data["iac"] as string | undefined) ?? "none",
      (data["registry"] as string | undefined) ?? "none",
      (data["api_gateway"] as string | undefined) ?? "none",
      (data["service_mesh"] as string | undefined) ?? "none",
      ObservabilityConfig.fromDict(
        (data["observability"] as Record<string, unknown> | undefined) ?? {},
      ),
      (data["cloud_provider"] as string | undefined) ?? "none",
    );
  }
}

export class TestingConfig {
  readonly smokeTests: boolean;
  readonly contractTests: boolean;
  readonly performanceTests: boolean;
  readonly coverageLine: number;
  readonly coverageBranch: number;

  constructor(
    smokeTests: boolean = true,
    contractTests: boolean = false,
    performanceTests: boolean = true,
    coverageLine: number = 95,
    coverageBranch: number = 90,
  ) {
    this.smokeTests = smokeTests;
    this.contractTests = contractTests;
    this.performanceTests = performanceTests;
    this.coverageLine = coverageLine;
    this.coverageBranch = coverageBranch;
  }

  static fromDict(
    data: Record<string, unknown>,
  ): TestingConfig {
    return new TestingConfig(
      (data["smoke_tests"] as boolean | undefined) ?? true,
      (data["contract_tests"] as boolean | undefined) ?? false,
      (data["performance_tests"] as boolean | undefined) ?? true,
      (data["coverage_line"] as number | undefined) ?? 95,
      (data["coverage_branch"] as number | undefined) ?? 90,
    );
  }
}

export class McpServerConfig {
  readonly id: string;
  readonly url: string;
  readonly capabilities: readonly string[];
  /** May contain sensitive values (API keys, tokens). Use {@link toJSON} for safe serialization. */
  readonly env: Readonly<Record<string, string>>;

  constructor(
    id: string,
    url: string,
    capabilities: readonly string[] = [],
    env: Readonly<Record<string, string>> = {},
  ) {
    this.id = id;
    this.url = url;
    this.capabilities = capabilities;
    this.env = env;
  }

  static fromDict(
    data: Record<string, unknown>,
  ): McpServerConfig {
    return new McpServerConfig(
      requireField(data, "id", "McpServerConfig") as string,
      requireField(data, "url", "McpServerConfig") as string,
      (data["capabilities"] as string[] | undefined) ?? [],
      (data["env"] as Record<string, string> | undefined) ?? {},
    );
  }

  toJSON(): Record<string, unknown> {
    const maskedEnv: Record<string, string> = {};
    for (const key of Object.keys(this.env)) {
      maskedEnv[key] = "***";
    }
    return {
      id: this.id,
      url: this.url,
      capabilities: [...this.capabilities],
      env: maskedEnv,
    };
  }
}

export class McpConfig {
  readonly servers: readonly McpServerConfig[];

  constructor(
    servers: readonly McpServerConfig[] = [],
  ) {
    this.servers = servers;
  }

  static fromDict(
    data: Record<string, unknown>,
  ): McpConfig {
    const raw = (data["servers"] as Array<Record<string, unknown>> | undefined) ?? [];
    return new McpConfig(
      raw.map((s) => McpServerConfig.fromDict(s)),
    );
  }
}

export class ProjectConfig {
  readonly project: ProjectIdentity;
  readonly architecture: ArchitectureConfig;
  readonly interfaces: readonly InterfaceConfig[];
  readonly language: LanguageConfig;
  readonly framework: FrameworkConfig;
  readonly data: DataConfig;
  readonly infrastructure: InfraConfig;
  readonly security: SecurityConfig;
  readonly testing: TestingConfig;
  readonly mcp: McpConfig;

  constructor(
    project: ProjectIdentity,
    architecture: ArchitectureConfig,
    interfaces: readonly InterfaceConfig[],
    language: LanguageConfig,
    framework: FrameworkConfig,
    data: DataConfig = new DataConfig(),
    infrastructure: InfraConfig = new InfraConfig(),
    security: SecurityConfig = new SecurityConfig(),
    testing: TestingConfig = new TestingConfig(),
    mcp: McpConfig = new McpConfig(),
  ) {
    this.project = project;
    this.architecture = architecture;
    this.interfaces = interfaces;
    this.language = language;
    this.framework = framework;
    this.data = data;
    this.infrastructure = infrastructure;
    this.security = security;
    this.testing = testing;
    this.mcp = mcp;
  }

  static fromDict(
    data: Record<string, unknown>,
  ): ProjectConfig {
    const interfacesRaw = requireField(
      data, "interfaces", "ProjectConfig",
    ) as Array<Record<string, unknown>>;
    return new ProjectConfig(
      ProjectIdentity.fromDict(
        requireField(data, "project", "ProjectConfig") as Record<string, unknown>,
      ),
      ArchitectureConfig.fromDict(
        requireField(data, "architecture", "ProjectConfig") as Record<string, unknown>,
      ),
      interfacesRaw.map((i) => InterfaceConfig.fromDict(i)),
      LanguageConfig.fromDict(
        requireField(data, "language", "ProjectConfig") as Record<string, unknown>,
      ),
      FrameworkConfig.fromDict(
        requireField(data, "framework", "ProjectConfig") as Record<string, unknown>,
      ),
      DataConfig.fromDict(
        (data["data"] as Record<string, unknown> | undefined) ?? {},
      ),
      InfraConfig.fromDict(
        (data["infrastructure"] as Record<string, unknown> | undefined) ?? {},
      ),
      SecurityConfig.fromDict(
        (data["security"] as Record<string, unknown> | undefined) ?? {},
      ),
      TestingConfig.fromDict(
        (data["testing"] as Record<string, unknown> | undefined) ?? {},
      ),
      McpConfig.fromDict(
        (data["mcp"] as Record<string, unknown> | undefined) ?? {},
      ),
    );
  }
}

export class PipelineResult {
  readonly success: boolean;
  readonly outputDir: string;
  readonly filesGenerated: readonly string[];
  readonly warnings: readonly string[];
  readonly durationMs: number;

  constructor(
    success: boolean,
    outputDir: string,
    filesGenerated: readonly string[],
    warnings: readonly string[],
    durationMs: number,
  ) {
    this.success = success;
    this.outputDir = outputDir;
    this.filesGenerated = filesGenerated;
    this.warnings = warnings;
    this.durationMs = durationMs;
  }
}

export class FileDiff {
  readonly path: string;
  readonly diff: string;
  readonly pythonSize: number;
  readonly referenceSize: number;

  constructor(
    path: string,
    diff: string,
    pythonSize: number,
    referenceSize: number,
  ) {
    this.path = path;
    this.diff = diff;
    this.pythonSize = pythonSize;
    this.referenceSize = referenceSize;
  }
}

export class VerificationResult {
  readonly success: boolean;
  readonly totalFiles: number;
  readonly mismatches: readonly FileDiff[];
  readonly missingFiles: readonly string[];
  readonly extraFiles: readonly string[];

  constructor(
    success: boolean,
    totalFiles: number,
    mismatches: readonly FileDiff[],
    missingFiles: readonly string[],
    extraFiles: readonly string[],
  ) {
    this.success = success;
    this.totalFiles = totalFiles;
    this.mismatches = mismatches;
    this.missingFiles = missingFiles;
    this.extraFiles = extraFiles;
  }
}
