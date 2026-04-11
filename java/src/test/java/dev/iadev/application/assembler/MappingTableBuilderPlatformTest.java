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
 * Tests for MappingTableBuilder. After Codex removal the
 * mapping is vacuous — the builder always returns an
 * empty string.
 */
@DisplayName("MappingTableBuilder — platform filtering")
class MappingTableBuilderPlatformTest {

    private final MappingTableBuilder builder =
            new MappingTableBuilder();

    @Nested
    @DisplayName("build always returns empty")
    class AlwaysEmpty {

        @Test
        @DisplayName("empty string for claude-code only")
        void build_claudeOnly_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path claudeDir = setupMinimalOutput(tempDir);

            String result = builder.build(
                    claudeDir,
                    Set.of(Platform.CLAUDE_CODE));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("empty string for empty platforms")
        void build_emptyPlatforms_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path claudeDir = setupMinimalOutput(tempDir);

            String result = builder.build(
                    claudeDir, Set.of());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("empty string for all selectable")
        void build_allSelectable_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path claudeDir = setupMinimalOutput(tempDir);

            String result = builder.build(
                    claudeDir,
                    Platform.allUserSelectable());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("empty string for single-arg build")
        void build_singleArg_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path claudeDir = setupMinimalOutput(tempDir);

            String result = builder.build(claudeDir);

            assertThat(result).isEmpty();
        }
    }

    private Path setupMinimalOutput(Path tempDir)
            throws IOException {
        Path claudeDir = Files.createDirectories(
                tempDir.resolve(".claude"));
        return claudeDir;
    }
}
