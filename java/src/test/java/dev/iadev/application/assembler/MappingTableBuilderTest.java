package dev.iadev.application.assembler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for MappingTableBuilder — builds the platform
 * mapping table for README.md.
 */
@DisplayName("MappingTableBuilder")
class MappingTableBuilderTest {

    private final MappingTableBuilder builder =
            new MappingTableBuilder();

    @Nested
    @DisplayName("build — mapping table")
    class Build {

        @Test
        @DisplayName("contains 6 mapping rows")
        void build_whenCalled_containsSixMappingRows(
                @TempDir Path tempDir) throws IOException {
            Path claudeDir =
                    Files.createDirectories(
                            tempDir.resolve(".claude"));

            String table = builder.build(claudeDir);

            assertThat(table)
                    .contains("| .claude/ | .codex/ "
                            + "| Notes |");
            assertThat(table)
                    .contains("`.codex/requirements.toml`")
                    .contains("`AGENTS.override.md`");
            int dataRows = 0;
            for (String line : table.split("\n")) {
                if (line.startsWith("| ")
                        && !line.startsWith("| .claude/")
                        && !line.startsWith("|---")) {
                    dataRows++;
                }
            }
            assertThat(dataRows).isEqualTo(6);
        }

        @Test
        @DisplayName("contains mapping arrow unicode")
        void build_whenCalled_containsMappingArrow(
                @TempDir Path tempDir) throws IOException {
            Path claudeDir =
                    Files.createDirectories(
                            tempDir.resolve(".claude"));

            String table = builder.build(claudeDir);

            assertThat(table).contains("\u2192");
        }

        @Test
        @DisplayName("contains platform names")
        void build_whenCalled_containsPlatformNames(
                @TempDir Path tempDir) throws IOException {
            Path claudeDir = Files.createDirectories(
                    tempDir.resolve(".claude"));

            String table = builder.build(claudeDir);

            assertThat(table)
                    .contains("Rules")
                    .contains("Skills")
                    .contains("Agents")
                    .contains("Hooks")
                    .contains("Settings");
        }
    }
}
