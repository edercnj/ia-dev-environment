package dev.iadev.application.assembler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AssemblerTarget enum — maps logical targets
 * to physical directories.
 */
@DisplayName("AssemblerTarget")
class AssemblerTargetTest {

    @ParameterizedTest
    @CsvSource({
            "ROOT, output",
            "CLAUDE, output/.claude",
            "GITHUB, output/.github",
            "CODEX, output/.codex",
            "CODEX_AGENTS, output/.agents",
            "DOCS, output/docs"
    })
    @DisplayName("resolves target to correct physical path")
    void resolve_returnsCorrectPath(
            String targetName, String expected) {
        AssemblerTarget target =
                AssemblerTarget.valueOf(targetName);
        Path base = Path.of("output");

        Path resolved = target.resolve(base);

        assertThat(resolved).isEqualTo(Path.of(expected));
    }

    @Test
    @DisplayName("has exactly 6 target values")
    void values_whenCalled_containsSixEntries() {
        assertThat(AssemblerTarget.values()).hasSize(6);
    }

    @Test
    @DisplayName("ROOT resolves to output dir itself")
    void root_whenCalled_resolvesToOutputDir() {
        Path base = Path.of("/tmp/project");

        assertThat(AssemblerTarget.ROOT.resolve(base))
                .isEqualTo(base);
    }
}
