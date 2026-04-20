package dev.iadev.application.lifecycle;

import dev.iadev.domain.lifecycle.LifecycleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link StatusFieldParser}. Covers the
 * degenerate / happy / error / boundary TPP hierarchy from
 * the story Gherkin scenarios.
 */
@DisplayName("StatusFieldParser — read + atomic write")
class StatusFieldParserTest {

    @Test
    @DisplayName("readStatus returns empty when file has no "
            + "Status line (degenerate)")
    void readStatus_noStatusLine_returnsEmpty(
            @TempDir Path dir) throws IOException {
        Path file = dir.resolve("no-status.md");
        Files.writeString(file, "# Title\n\nBody.\n",
                StandardCharsets.UTF_8);

        Optional<LifecycleStatus> result =
                StatusFieldParser.readStatus(file);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("readStatus parses each canonical value "
            + "(happy)")
    void readStatus_eachCanonical_parsesCorrectly(
            @TempDir Path dir) throws IOException {
        for (LifecycleStatus s : LifecycleStatus.values()) {
            Path file = dir.resolve(
                    "artifact-" + s.name() + ".md");
            Files.writeString(file,
                    "# Title\n\n**Status:** " + s.label()
                            + "\n\nBody.\n",
                    StandardCharsets.UTF_8);

            assertThat(StatusFieldParser.readStatus(file))
                    .contains(s);
        }
    }

    @Test
    @DisplayName("readStatus tolerates multi-space whitespace"
            + " (boundary)")
    void readStatus_extraWhitespace_parses(
            @TempDir Path dir) throws IOException {
        Path file = dir.resolve("ws.md");
        Files.writeString(file,
                "**Status:**    Pendente   \n",
                StandardCharsets.UTF_8);

        assertThat(StatusFieldParser.readStatus(file))
                .contains(LifecycleStatus.PENDENTE);
    }

    @Test
    @DisplayName("readStatus returns empty when Status value "
            + "is outside the enum")
    void readStatus_unknownValue_returnsEmpty(
            @TempDir Path dir) throws IOException {
        Path file = dir.resolve("unknown.md");
        // "Bogus" fails the regex (not in alternation); the
        // parser sees no match and returns empty.
        Files.writeString(file,
                "**Status:** Bogus\n",
                StandardCharsets.UTF_8);

        assertThat(StatusFieldParser.readStatus(file))
                .isEmpty();
    }

    @Test
    @DisplayName("readStatus throws StatusSyncException "
            + "when file is missing (fail loud)")
    void readStatus_missingFile_throws(
            @TempDir Path dir) {
        Path missing = dir.resolve("missing.md");

        assertThatThrownBy(() ->
                StatusFieldParser.readStatus(missing))
                .isInstanceOf(StatusSyncException.class)
                .hasMessageContaining("STATUS_SYNC_FAILED")
                .hasMessageContaining("missing.md");
    }

    @Test
    @DisplayName("readStatus throws when path is null")
    void readStatus_nullPath_throws() {
        assertThatThrownBy(() ->
                StatusFieldParser.readStatus(null))
                .isInstanceOf(StatusSyncException.class)
                .hasMessageContaining("STATUS_SYNC_FAILED");
    }

    @Test
    @DisplayName("writeStatus updates only the first "
            + "occurrence and preserves body")
    void writeStatus_replacesFirstOccurrence(
            @TempDir Path dir) throws IOException {
        Path file = dir.resolve("story.md");
        Files.writeString(file,
                "# Story\n\n**Status:** Pendente\n\n"
                        + "Body references **Status:** "
                        + "Pendente inline in a sentence.\n",
                StandardCharsets.UTF_8);

        StatusFieldParser.writeStatus(
                file, LifecycleStatus.PLANEJADA);

        String after = Files.readString(file,
                StandardCharsets.UTF_8);
        // Header updated:
        assertThat(after)
                .contains("**Status:** Planejada");
        // First-only semantic: body mention still says
        // Pendente because the regex matches only the first
        // occurrence.
        assertThat(after.indexOf("Pendente"))
                .isGreaterThan(0);
    }

    @Test
    @DisplayName("writeStatus round-trips via readStatus")
    void writeStatus_readBack_matches(
            @TempDir Path dir) throws IOException {
        Path file = dir.resolve("roundtrip.md");
        Files.writeString(file,
                "**Status:** Pendente\n",
                StandardCharsets.UTF_8);

        StatusFieldParser.writeStatus(
                file, LifecycleStatus.EM_ANDAMENTO);

        assertThat(StatusFieldParser.readStatus(file))
                .contains(LifecycleStatus.EM_ANDAMENTO);
    }

    @Test
    @DisplayName("writeStatus prepends Status when the file "
            + "lacks the header (recovery)")
    void writeStatus_noExistingHeader_prepends(
            @TempDir Path dir) throws IOException {
        Path file = dir.resolve("no-header.md");
        Files.writeString(file,
                "# Body only\n", StandardCharsets.UTF_8);

        StatusFieldParser.writeStatus(
                file, LifecycleStatus.PLANEJADA);

        String content = Files.readString(file,
                StandardCharsets.UTF_8);
        assertThat(content)
                .startsWith("**Status:** Planejada");
        assertThat(content)
                .contains("# Body only");
    }

    @Test
    @DisplayName("writeStatus throws StatusSyncException "
            + "when file is missing (fail loud)")
    void writeStatus_missingFile_throws(
            @TempDir Path dir) {
        Path missing = dir.resolve("gone.md");

        assertThatThrownBy(() ->
                StatusFieldParser.writeStatus(missing,
                        LifecycleStatus.PLANEJADA))
                .isInstanceOf(StatusSyncException.class)
                .hasMessageContaining("STATUS_SYNC_FAILED");
    }

    @Test
    @DisplayName("writeStatus throws when newStatus is null")
    void writeStatus_nullStatus_throws(
            @TempDir Path dir) throws IOException {
        Path file = dir.resolve("target.md");
        Files.writeString(file, "**Status:** Pendente\n",
                StandardCharsets.UTF_8);

        assertThatThrownBy(() ->
                StatusFieldParser.writeStatus(file, null))
                .isInstanceOf(StatusSyncException.class)
                .hasMessageContaining("newStatus is null");
    }

    @Test
    @DisplayName("writeStatus throws when path is null")
    void writeStatus_nullPath_throws() {
        assertThatThrownBy(() ->
                StatusFieldParser.writeStatus(null,
                        LifecycleStatus.PLANEJADA))
                .isInstanceOf(StatusSyncException.class)
                .hasMessageContaining("STATUS_SYNC_FAILED");
    }

    @Test
    @DisplayName("StatusSyncException exposes file path and "
            + "stable error code")
    void exception_exposesFileAndCode(
            @TempDir Path dir) {
        Path target = dir.resolve("x.md");
        StatusSyncException ex =
                new StatusSyncException(
                        target, "detail message");

        assertThat(ex.file()).isEqualTo(target);
        assertThat(ex.code())
                .isEqualTo("STATUS_SYNC_FAILED");
        assertThat(ex.getMessage())
                .contains("STATUS_SYNC_FAILED")
                .contains("detail message")
                .contains("x.md");
    }

    @Test
    @DisplayName("STATUS_REGEX exposes the canonical pattern")
    void constant_exposesRegex() {
        assertThat(StatusFieldParser.STATUS_REGEX)
                .contains("Pendente")
                .contains("Planejada")
                .contains("Em Andamento")
                .contains("Concluída")
                .contains("Falha")
                .contains("Bloqueada");
    }
}
