package dev.iadev.domain.stack;

import java.util.List;
import java.util.Map;

/**
 * Static mapping constants for stack resolution.
 * Pure domain utility — no external dependencies.
 *
 * <p>EPIC-0048 (v4.0.0): all non-Java language / framework
 * entries removed per ADR-0048-A (Java-only scope).</p>
 */
public final class StackMapping {

    private StackMapping() {
        // utility class
    }

    /** Language + build tool to command mapping (Java-only). */
    public static final Map<String, LanguageCommandSet> LANGUAGE_COMMANDS =
            Map.of(
                    "java-maven", new LanguageCommandSet(
                            "./mvnw compile -q",
                            "./mvnw package -DskipTests",
                            "./mvnw verify",
                            "./mvnw verify jacoco:report",
                            ".java", "pom.xml", "maven"),
                    "java-gradle", new LanguageCommandSet(
                            "./gradlew compileJava -q",
                            "./gradlew build -x test",
                            "./gradlew test",
                            "./gradlew test jacocoTestReport",
                            ".java", "build.gradle", "gradle"));

    /** Framework to default port (Java-only). */
    public static final Map<String, Integer> FRAMEWORK_PORTS =
            Map.of(
                    "quarkus", 8080,
                    "spring-boot", 8080);

    /** Default port when framework is not found. */
    public static final int DEFAULT_PORT_FALLBACK = 8080;

    /** Framework to health check path (Java-only). */
    public static final Map<String, String> FRAMEWORK_HEALTH_PATHS =
            Map.of(
                    "quarkus", "/q/health",
                    "spring-boot", "/actuator/health");

    /** Default health path when framework is not found. */
    public static final String DEFAULT_HEALTH_PATH = "/health";

    /** Framework to valid languages (Java-only). */
    public static final Map<String, List<String>> FRAMEWORK_LANGUAGE_RULES =
            Map.of(
                    "quarkus", List.of("java"),
                    "spring-boot", List.of("java"));

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

    /** Docker base image templates (Java-only). */
    public static final Map<String, String> DOCKER_BASE_IMAGES = Map.of(
            "java", "eclipse-temurin:{version}-jre-alpine");

    /** Default Docker image when language is not found. */
    public static final String DEFAULT_DOCKER_IMAGE = "alpine:latest";

    /** Hook template directory key (Java-only). */
    public static final Map<String, String> HOOK_TEMPLATE_MAP = Map.of(
            "java-maven", "java-maven",
            "java-gradle", "java-gradle");

    /** Settings language JSON key (Java-only). */
    public static final Map<String, String> SETTINGS_LANG_MAP = Map.of(
            "java-maven", "java-maven",
            "java-gradle", "java-gradle");

    /** Returns hook template key, or empty string. */
    public static String getHookTemplateKey(
            String language, String buildTool) {
        return HOOK_TEMPLATE_MAP.getOrDefault(
                language + "-" + buildTool, "");
    }

    /** Returns settings language key, or empty string. */
    public static String getSettingsLangKey(
            String language, String buildTool) {
        return SETTINGS_LANG_MAP.getOrDefault(
                language + "-" + buildTool, "");
    }
}
