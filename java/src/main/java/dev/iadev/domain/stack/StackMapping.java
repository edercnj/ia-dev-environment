package dev.iadev.domain.stack;

import java.util.List;
import java.util.Map;

/**
 * Static mapping constants for stack resolution.
 *
 * <p>Contains all language/build-tool command mappings, framework ports,
 * health paths, language-framework compatibility rules, Docker images,
 * and other configuration lookup tables.</p>
 *
 * <p>All maps are unmodifiable. This is a pure domain utility class
 * with no external framework dependencies (RULE-007).</p>
 */
public final class StackMapping {

    private StackMapping() {
        // utility class
    }

    /**
     * Language + build tool to command mapping (8 entries).
     *
     * <p>Key format: "{language}-{buildTool}" (e.g. "java-maven").</p>
     */
    public static final Map<String, LanguageCommandSet> LANGUAGE_COMMANDS =
            Map.ofEntries(
                    Map.entry("java-maven", new LanguageCommandSet(
                            "./mvnw compile -q",
                            "./mvnw package -DskipTests",
                            "./mvnw verify",
                            "./mvnw verify jacoco:report",
                            ".java", "pom.xml", "maven")),
                    Map.entry("java-gradle", new LanguageCommandSet(
                            "./gradlew compileJava -q",
                            "./gradlew build -x test",
                            "./gradlew test",
                            "./gradlew test jacocoTestReport",
                            ".java", "build.gradle", "gradle")),
                    Map.entry("kotlin-gradle", new LanguageCommandSet(
                            "./gradlew compileKotlin -q",
                            "./gradlew build -x test",
                            "./gradlew test",
                            "./gradlew test jacocoTestReport",
                            ".kt", "build.gradle.kts", "gradle")),
                    Map.entry("typescript-npm", new LanguageCommandSet(
                            "npx --no-install tsc --noEmit",
                            "npm run build",
                            "npm test",
                            "npm test -- --coverage",
                            ".ts", "package.json", "npm")),
                    Map.entry("python-pip", new LanguageCommandSet(
                            "python3 -m py_compile",
                            "pip install -e .",
                            "pytest",
                            "pytest --cov",
                            ".py", "pyproject.toml", "pip")),
                    Map.entry("go-go", new LanguageCommandSet(
                            "go build ./...",
                            "go build ./...",
                            "go test ./...",
                            "go test -coverprofile=coverage.out ./...",
                            ".go", "go.mod", "go")),
                    Map.entry("rust-cargo", new LanguageCommandSet(
                            "cargo check",
                            "cargo build",
                            "cargo test",
                            "cargo tarpaulin",
                            ".rs", "Cargo.toml", "cargo")),
                    Map.entry("csharp-dotnet", new LanguageCommandSet(
                            "dotnet build --no-restore --verbosity quiet",
                            "dotnet build",
                            "dotnet test",
                            "dotnet test --collect:\"XPlat Code Coverage\"",
                            ".cs", "*.csproj", "dotnet"))
            );

    /** Framework to default port (11 entries). */
    public static final Map<String, Integer> FRAMEWORK_PORTS =
            Map.ofEntries(
                    Map.entry("quarkus", 8080),
                    Map.entry("spring-boot", 8080),
                    Map.entry("nestjs", 3000),
                    Map.entry("express", 3000),
                    Map.entry("fastapi", 8000),
                    Map.entry("django", 8000),
                    Map.entry("gin", 8080),
                    Map.entry("ktor", 8080),
                    Map.entry("axum", 3000),
                    Map.entry("actix-web", 8080),
                    Map.entry("aspnet", 5000)
            );

    /** Default port when framework is not found. */
    public static final int DEFAULT_PORT_FALLBACK = 8080;

    /** Framework to health check path (11 entries). */
    public static final Map<String, String> FRAMEWORK_HEALTH_PATHS =
            Map.ofEntries(
                    Map.entry("quarkus", "/q/health"),
                    Map.entry("spring-boot", "/actuator/health"),
                    Map.entry("nestjs", "/health"),
                    Map.entry("express", "/health"),
                    Map.entry("fastapi", "/health"),
                    Map.entry("django", "/health"),
                    Map.entry("gin", "/health"),
                    Map.entry("ktor", "/health"),
                    Map.entry("axum", "/health"),
                    Map.entry("actix-web", "/health"),
                    Map.entry("aspnet", "/health")
            );

    /** Default health path when framework is not found. */
    public static final String DEFAULT_HEALTH_PATH = "/health";

    /** Framework to valid languages (16 entries). */
    public static final Map<String, List<String>> FRAMEWORK_LANGUAGE_RULES =
            Map.ofEntries(
                    Map.entry("quarkus", List.of("java", "kotlin")),
                    Map.entry("spring-boot", List.of("java", "kotlin")),
                    Map.entry("nestjs", List.of("typescript")),
                    Map.entry("express", List.of("typescript")),
                    Map.entry("fastify", List.of("typescript")),
                    Map.entry("commander", List.of("typescript")),
                    Map.entry("fastapi", List.of("python")),
                    Map.entry("django", List.of("python")),
                    Map.entry("flask", List.of("python")),
                    Map.entry("stdlib", List.of("go")),
                    Map.entry("gin", List.of("go")),
                    Map.entry("fiber", List.of("go")),
                    Map.entry("ktor", List.of("kotlin")),
                    Map.entry("axum", List.of("rust")),
                    Map.entry("actix-web", List.of("rust")),
                    Map.entry("aspnet", List.of("csharp"))
            );

    /** Frameworks that support native/GraalVM builds. */
    public static final List<String> NATIVE_SUPPORTED_FRAMEWORKS =
            List.of("quarkus", "spring-boot");

    /** Valid interface types (9 entries). */
    public static final List<String> VALID_INTERFACE_TYPES = List.of(
            "rest", "grpc", "graphql", "websocket", "tcp-custom",
            "cli", "event-consumer", "event-producer", "scheduled");

    /** Valid architecture styles (10 entries). */
    public static final List<String> VALID_ARCHITECTURE_STYLES =
            List.of(
                    "microservice", "modular-monolith",
                    "monolith", "library", "serverless",
                    "ddd", "hexagonal", "cqrs",
                    "event-driven", "clean");

    /** Valid event store types. */
    public static final List<String> VALID_EVENT_STORES =
            List.of("eventstoredb", "axon", "custom");

    /** Valid schema registry types. */
    public static final List<String> VALID_SCHEMA_REGISTRIES =
            List.of("confluent", "apicurio", "glue");

    /** Valid dead letter strategy types. */
    public static final List<String> VALID_DEAD_LETTER_STRATEGIES =
            List.of("kafka-dlq", "sqs-dlq", "database");

    /** Interface type to protocol spec name. */
    public static final Map<String, String> INTERFACE_SPEC_PROTOCOL_MAP =
            Map.of(
                    "rest", "openapi",
                    "grpc", "proto3",
                    "graphql", "graphql",
                    "websocket", "websocket",
                    "tcp-custom", "tcp-custom",
                    "event-consumer", "kafka",
                    "event-producer", "kafka"
            );

    /** Docker base image templates with {version} placeholder. */
    public static final Map<String, String> DOCKER_BASE_IMAGES = Map.of(
            "java", "eclipse-temurin:{version}-jre-alpine",
            "kotlin", "eclipse-temurin:{version}-jre-alpine",
            "typescript", "node:{version}-alpine",
            "python", "python:{version}-slim",
            "go", "golang:{version}-alpine",
            "rust", "rust:{version}-slim",
            "csharp", "mcr.microsoft.com/dotnet/aspnet:{version}"
    );

    /** Default Docker image when language is not found. */
    public static final String DEFAULT_DOCKER_IMAGE = "alpine:latest";

    /** Hook template directory key (8 entries). */
    public static final Map<String, String> HOOK_TEMPLATE_MAP = Map.of(
            "java-maven", "java-maven",
            "java-gradle", "java-gradle",
            "kotlin-gradle", "kotlin",
            "typescript-npm", "typescript",
            "python-pip", "",
            "go-go", "go",
            "rust-cargo", "rust",
            "csharp-dotnet", "csharp"
    );

    /** Settings language JSON key (8 entries). */
    public static final Map<String, String> SETTINGS_LANG_MAP = Map.of(
            "java-maven", "java-maven",
            "java-gradle", "java-gradle",
            "kotlin-gradle", "java-gradle",
            "typescript-npm", "typescript-npm",
            "python-pip", "python-pip",
            "go-go", "go",
            "rust-cargo", "rust-cargo",
            "csharp-dotnet", "csharp-dotnet"
    );

    /** Database name to settings key. */
    public static final Map<String, String> DATABASE_SETTINGS_MAP = Map.of(
            "postgresql", "database-psql",
            "mysql", "database-mysql",
            "oracle", "database-oracle",
            "mongodb", "database-mongodb",
            "cassandra", "database-cassandra"
    );

    /** Cache name to settings key. */
    public static final Map<String, String> CACHE_SETTINGS_MAP = Map.of(
            "redis", "cache-redis",
            "dragonfly", "cache-dragonfly",
            "memcached", "cache-memcached"
    );

    /** Returns hook template key for language/build-tool, or empty. */
    public static String getHookTemplateKey(
            String language, String buildTool) {
        return HOOK_TEMPLATE_MAP.getOrDefault(
                language + "-" + buildTool, "");
    }

    /** Returns settings language key for language/build-tool, or empty. */
    public static String getSettingsLangKey(
            String language, String buildTool) {
        return SETTINGS_LANG_MAP.getOrDefault(
                language + "-" + buildTool, "");
    }

    /** Returns database settings key for a database name, or empty. */
    public static String getDatabaseSettingsKey(String dbName) {
        return DATABASE_SETTINGS_MAP.getOrDefault(dbName, "");
    }

    /** Returns cache settings key for a cache name, or empty. */
    public static String getCacheSettingsKey(String cacheName) {
        return CACHE_SETTINGS_MAP.getOrDefault(cacheName, "");
    }
}
