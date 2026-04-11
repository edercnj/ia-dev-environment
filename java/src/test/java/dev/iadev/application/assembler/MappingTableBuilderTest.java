package dev.iadev.application.assembler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for MappingTableBuilder. After Codex target
 * removal this builder always returns an empty string.
 */
@DisplayName("MappingTableBuilder")
class MappingTableBuilderTest {

    private final MappingTableBuilder builder =
            new MappingTableBuilder();

    @Nested
    @DisplayName("build — mapping table")
    class Build {

        @Test
        @DisplayName("returns empty for any claude dir")
        void build_whenCalled_returnsEmpty(
                @TempDir Path tempDir) throws IOException {
            Path claudeDir =
                    Files.createDirectories(
                            tempDir.resolve(".claude"));

            String table = builder.build(claudeDir);

            assertThat(table).isEmpty();
        }

        @Test
        @DisplayName("returns empty for missing dir")
        void build_whenCalled_missingDir_returnsEmpty(
                @TempDir Path tempDir) throws IOException {
            String table = builder.build(
                    tempDir.resolve("nonexistent"));

            assertThat(table).isEmpty();
        }
    }
}
