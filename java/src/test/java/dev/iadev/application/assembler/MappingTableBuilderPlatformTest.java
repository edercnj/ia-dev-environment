package dev.iadev.application.assembler;

import dev.iadev.domain.model.Platform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for platform-filtered mapping table in
 * MappingTableBuilder.
 */
@DisplayName("MappingTableBuilder — platform filtering")
class MappingTableBuilderPlatformTest {

    private final MappingTableBuilder builder =
            new MappingTableBuilder();

    @Nested
    @DisplayName("build with platforms")
    class FilteredMapping {

        @Test
        @DisplayName("empty string for single platform")
        void build_singlePlatform_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path claudeDir = setupMinimalOutput(tempDir);

            String result = builder.build(
                    claudeDir,
                    Set.of(Platform.CLAUDE_CODE));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("shows table for multiple platforms")
        void build_multiplePlatforms_showsTable(
                @TempDir Path tempDir)
                throws IOException {
            Path claudeDir = setupMinimalOutput(tempDir);

            String result = builder.build(
                    claudeDir,
                    Set.of(Platform.CLAUDE_CODE,
                            Platform.COPILOT));

            assertThat(result)
                    .contains("| .claude/ | .github/");
        }

        @Test
        @DisplayName("shows table for empty platforms (all)")
        void build_emptyPlatforms_showsTable(
                @TempDir Path tempDir)
                throws IOException {
            Path claudeDir = setupMinimalOutput(tempDir);

            String result = builder.build(
                    claudeDir, Set.of());

            assertThat(result)
                    .contains("| .claude/ | .github/");
        }

        @Test
        @DisplayName("shows table for all selectable")
        void build_allSelectable_showsTable(
                @TempDir Path tempDir)
                throws IOException {
            Path claudeDir = setupMinimalOutput(tempDir);

            String result = builder.build(
                    claudeDir,
                    Platform.allUserSelectable());

            assertThat(result)
                    .contains("| .claude/ | .github/");
        }

        @Test
        @DisplayName("copilot-only returns empty")
        void build_copilotOnly_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path claudeDir = setupMinimalOutput(tempDir);

            String result = builder.build(
                    claudeDir,
                    Set.of(Platform.COPILOT));

            assertThat(result).isEmpty();
        }
    }

    private Path setupMinimalOutput(Path tempDir)
            throws IOException {
        Path claudeDir = Files.createDirectories(
                tempDir.resolve(".claude"));
        Files.createDirectories(
                tempDir.resolve(".github"));
        return claudeDir;
    }
}
