/**
 * Stack mapping constants and helpers.
 *
 * Migrated from Python `domain/stack_mapping.py`.
 * Key: `"language-build_tool"` string (Python used tuple keys).
 */

/** Command set for a language + build tool combination. */
export interface LanguageCommandSet {
  readonly compileCmd: string;
  readonly buildCmd: string;
  readonly testCmd: string;
  readonly coverageCmd: string;
  readonly fileExtension: string;
  readonly buildFile: string;
  readonly packageManager: string;
}

/** Language + build tool to command mapping (8 entries). */
export const LANGUAGE_COMMANDS: Readonly<Record<string, LanguageCommandSet>> = {
  "java-maven": {
    compileCmd: "./mvnw compile -q",
    buildCmd: "./mvnw package -DskipTests",
    testCmd: "./mvnw verify",
    coverageCmd: "./mvnw verify jacoco:report",
    fileExtension: ".java",
    buildFile: "pom.xml",
    packageManager: "maven",
  },
  "java-gradle": {
    compileCmd: "./gradlew compileJava -q",
    buildCmd: "./gradlew build -x test",
    testCmd: "./gradlew test",
    coverageCmd: "./gradlew test jacocoTestReport",
    fileExtension: ".java",
    buildFile: "build.gradle",
    packageManager: "gradle",
  },
  "kotlin-gradle": {
    compileCmd: "./gradlew compileKotlin -q",
    buildCmd: "./gradlew build -x test",
    testCmd: "./gradlew test",
    coverageCmd: "./gradlew test jacocoTestReport",
    fileExtension: ".kt",
    buildFile: "build.gradle.kts",
    packageManager: "gradle",
  },
  "typescript-npm": {
    compileCmd: "npx --no-install tsc --noEmit",
    buildCmd: "npm run build",
    testCmd: "npm test",
    coverageCmd: "npm test -- --coverage",
    fileExtension: ".ts",
    buildFile: "package.json",
    packageManager: "npm",
  },
  "python-pip": {
    compileCmd: "python3 -m py_compile",
    buildCmd: "pip install -e .",
    testCmd: "pytest",
    coverageCmd: "pytest --cov",
    fileExtension: ".py",
    buildFile: "pyproject.toml",
    packageManager: "pip",
  },
  "go-go": {
    compileCmd: "go build ./...",
    buildCmd: "go build ./...",
    testCmd: "go test ./...",
    coverageCmd: "go test -coverprofile=coverage.out ./...",
    fileExtension: ".go",
    buildFile: "go.mod",
    packageManager: "go",
  },
  "rust-cargo": {
    compileCmd: "cargo check",
    buildCmd: "cargo build",
    testCmd: "cargo test",
    coverageCmd: "cargo tarpaulin",
    fileExtension: ".rs",
    buildFile: "Cargo.toml",
    packageManager: "cargo",
  },
  "csharp-dotnet": {
    compileCmd: "dotnet build --no-restore --verbosity quiet",
    buildCmd: "dotnet build",
    testCmd: "dotnet test",
    coverageCmd: 'dotnet test --collect:"XPlat Code Coverage"',
    fileExtension: ".cs",
    buildFile: "*.csproj",
    packageManager: "dotnet",
  },
};

/** Framework to default port (11 entries). */
export const FRAMEWORK_PORTS: Readonly<Record<string, number>> = {
  "quarkus": 8080,
  "spring-boot": 8080,
  "nestjs": 3000,
  "express": 3000,
  "fastapi": 8000,
  "django": 8000,
  "gin": 8080,
  "ktor": 8080,
  "axum": 3000,
  "actix-web": 8080,
  "aspnet": 5000,
};

export const DEFAULT_PORT_FALLBACK = 8080;

/** Framework to health check path (11 entries). */
export const FRAMEWORK_HEALTH_PATHS: Readonly<Record<string, string>> = {
  "quarkus": "/q/health",
  "spring-boot": "/actuator/health",
  "nestjs": "/health",
  "express": "/health",
  "fastapi": "/health",
  "django": "/health",
  "gin": "/health",
  "ktor": "/health",
  "axum": "/health",
  "actix-web": "/health",
  "aspnet": "/health",
};

export const DEFAULT_HEALTH_PATH = "/health";

/** Framework to valid languages (15 entries). */
export const FRAMEWORK_LANGUAGE_RULES: Readonly<Record<string, readonly string[]>> = {
  "quarkus": ["java", "kotlin"],
  "spring-boot": ["java", "kotlin"],
  "nestjs": ["typescript"],
  "express": ["typescript"],
  "fastify": ["typescript"],
  "fastapi": ["python"],
  "django": ["python"],
  "flask": ["python"],
  "stdlib": ["go"],
  "gin": ["go"],
  "fiber": ["go"],
  "ktor": ["kotlin"],
  "axum": ["rust"],
  "actix-web": ["rust"],
  "aspnet": ["csharp"],
};

/** Frameworks that support native/GraalVM builds. */
export const NATIVE_SUPPORTED_FRAMEWORKS: readonly string[] = [
  "quarkus",
  "spring-boot",
];

/** Valid interface types (9 entries). */
export const VALID_INTERFACE_TYPES: readonly string[] = [
  "rest",
  "grpc",
  "graphql",
  "websocket",
  "tcp-custom",
  "cli",
  "event-consumer",
  "event-producer",
  "scheduled",
];

/** Valid architecture styles (5 entries). */
export const VALID_ARCHITECTURE_STYLES: readonly string[] = [
  "microservice",
  "modular-monolith",
  "monolith",
  "library",
  "serverless",
];

/** Interface type to protocol spec name (e.g. openapi, proto3). */
export const INTERFACE_SPEC_PROTOCOL_MAP: Readonly<Record<string, string>> = {
  "rest": "openapi",
  "grpc": "proto3",
  "graphql": "graphql",
  "websocket": "websocket",
  "tcp-custom": "tcp-custom",
  "event-consumer": "kafka",
  "event-producer": "kafka",
};

/** Docker base image templates with `{version}` placeholder. */
export const DOCKER_BASE_IMAGES: Readonly<Record<string, string>> = {
  "java": "eclipse-temurin:{version}-jre-alpine",
  "kotlin": "eclipse-temurin:{version}-jre-alpine",
  "typescript": "node:{version}-alpine",
  "python": "python:{version}-slim",
  "go": "golang:{version}-alpine",
  "rust": "rust:{version}-slim",
  "csharp": "mcr.microsoft.com/dotnet/aspnet:{version}",
};

export const DEFAULT_DOCKER_IMAGE = "alpine:latest";

/** Hook template directory key. Empty string means no compile hook. */
export const HOOK_TEMPLATE_MAP: Readonly<Record<string, string>> = {
  "java-maven": "java-maven",
  "java-gradle": "java-gradle",
  "kotlin-gradle": "kotlin",
  "typescript-npm": "typescript",
  "python-pip": "",
  "go-go": "go",
  "rust-cargo": "rust",
  "csharp-dotnet": "csharp",
};

/** Settings language JSON key (without .json). */
export const SETTINGS_LANG_MAP: Readonly<Record<string, string>> = {
  "java-maven": "java-maven",
  "java-gradle": "java-gradle",
  "kotlin-gradle": "java-gradle",
  "typescript-npm": "typescript-npm",
  "python-pip": "python-pip",
  "go-go": "go",
  "rust-cargo": "rust-cargo",
  "csharp-dotnet": "csharp-dotnet",
};

/** Database name to settings key. */
export const DATABASE_SETTINGS_MAP: Readonly<Record<string, string>> = {
  "postgresql": "database-psql",
  "mysql": "database-mysql",
  "oracle": "database-oracle",
  "mongodb": "database-mongodb",
  "cassandra": "database-cassandra",
};

/** Cache name to settings key. */
export const CACHE_SETTINGS_MAP: Readonly<Record<string, string>> = {
  "redis": "cache-redis",
  "dragonfly": "cache-dragonfly",
  "memcached": "cache-memcached",
};

/** Return hook template directory key, or empty string if none. */
export function getHookTemplateKey(language: string, buildTool: string): string {
  return HOOK_TEMPLATE_MAP[`${language}-${buildTool}`] ?? "";
}

/** Return settings language JSON key, or empty string if none. */
export function getSettingsLangKey(language: string, buildTool: string): string {
  return SETTINGS_LANG_MAP[`${language}-${buildTool}`] ?? "";
}

/** Return database settings JSON key, or empty string if none. */
export function getDatabaseSettingsKey(dbName: string): string {
  return DATABASE_SETTINGS_MAP[dbName] ?? "";
}

/** Return cache settings JSON key, or empty string if none. */
export function getCacheSettingsKey(cacheName: string): string {
  return CACHE_SETTINGS_MAP[cacheName] ?? "";
}
