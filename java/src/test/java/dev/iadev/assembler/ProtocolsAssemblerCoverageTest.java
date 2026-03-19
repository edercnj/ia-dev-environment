package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Additional coverage tests for ProtocolsAssembler —
 * targeting uncovered branches and edge cases.
 */
@DisplayName("ProtocolsAssembler — coverage")
class ProtocolsAssemblerCoverageTest {

    @Nested
    @DisplayName("messaging protocol — broker selection")
    class MessagingBroker {

        @Test
        @DisplayName("event with specific broker selects"
                + " broker file only")
        void specificBrokerSelectsBrokerFile(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path msgDir = resourceDir.resolve(
                    "protocols/messaging");
            Files.createDirectories(msgDir);
            Files.writeString(
                    msgDir.resolve("kafka.md"),
                    "Kafka content");
            Files.writeString(
                    msgDir.resolve("rabbitmq.md"),
                    "RabbitMQ content");
            Path eventDir = resourceDir.resolve(
                    "protocols/event-driven");
            Files.createDirectories(eventDir);
            Files.writeString(
                    eventDir.resolve("patterns.md"),
                    "Event patterns");

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            ProtocolsAssembler assembler =
                    new ProtocolsAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface(
                                    "event-consumer",
                                    "", "kafka")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
            Path msgConv = outputDir.resolve(
                    "skills/protocols/references/"
                            + "messaging-conventions.md");
            assertThat(msgConv).exists();
            String content = Files.readString(
                    msgConv, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Kafka content")
                    .doesNotContain("RabbitMQ content");
        }

        @Test
        @DisplayName("event with no broker file"
                + " falls back to all files")
        void noBrokerFileFallsBack(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path msgDir = resourceDir.resolve(
                    "protocols/messaging");
            Files.createDirectories(msgDir);
            Files.writeString(
                    msgDir.resolve("generic.md"),
                    "Generic messaging");
            Path eventDir = resourceDir.resolve(
                    "protocols/event-driven");
            Files.createDirectories(eventDir);
            Files.writeString(
                    eventDir.resolve("patterns.md"),
                    "Event patterns");

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            ProtocolsAssembler assembler =
                    new ProtocolsAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface(
                                    "event-producer",
                                    "", "unknown-broker")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
        }

        @Test
        @DisplayName("event without broker uses all"
                + " messaging files")
        void noBrokerUsesAllFiles(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path msgDir = resourceDir.resolve(
                    "protocols/messaging");
            Files.createDirectories(msgDir);
            Files.writeString(
                    msgDir.resolve("concepts.md"),
                    "Concepts");
            Files.writeString(
                    msgDir.resolve("patterns.md"),
                    "Patterns");
            Path eventDir = resourceDir.resolve(
                    "protocols/event-driven");
            Files.createDirectories(eventDir);
            Files.writeString(
                    eventDir.resolve("event.md"),
                    "Event content");

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            ProtocolsAssembler assembler =
                    new ProtocolsAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("event-consumer")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("protocol dir missing for protocol")
    class ProtocolDirMissing {

        @Test
        @DisplayName("protocol dir missing skips"
                + " that protocol")
        void missingProtocolDirSkipped(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path restDir = resourceDir.resolve(
                    "protocols/rest");
            Files.createDirectories(restDir);
            Files.writeString(
                    restDir.resolve("rest.md"),
                    "REST content");

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            ProtocolsAssembler assembler =
                    new ProtocolsAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .addInterface("graphql")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
            assertThat(files).anyMatch(
                    f -> f.contains(
                            "rest-conventions.md"));
        }
    }

    @Nested
    @DisplayName("concatenation with separator")
    class Concatenation {

        @Test
        @DisplayName("multiple files concatenated with"
                + " separator")
        void multipleFilesConcatenated(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path restDir = resourceDir.resolve(
                    "protocols/rest");
            Files.createDirectories(restDir);
            Files.writeString(
                    restDir.resolve("01-basics.md"),
                    "Basics");
            Files.writeString(
                    restDir.resolve("02-advanced.md"),
                    "Advanced");

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            ProtocolsAssembler assembler =
                    new ProtocolsAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path conventions = outputDir.resolve(
                    "skills/protocols/references/"
                            + "rest-conventions.md");
            String content = Files.readString(
                    conventions, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Basics")
                    .contains("---")
                    .contains("Advanced");
        }
    }

    @Nested
    @DisplayName("protocol files empty after filtering")
    class EmptyAfterFiltering {

        @Test
        @DisplayName("protocol dir with no md files"
                + " returns empty")
        void noMdFilesReturnsEmpty(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path restDir = resourceDir.resolve(
                    "protocols/rest");
            Files.createDirectories(restDir);
            Files.writeString(
                    restDir.resolve("readme.txt"),
                    "Not a markdown file");

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            ProtocolsAssembler assembler =
                    new ProtocolsAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }
    }
}
