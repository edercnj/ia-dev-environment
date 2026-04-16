package dev.iadev.telemetry.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verification that the telemetry contract is documented:
 *
 * <ul>
 *   <li>_TEMPLATE-TELEMETRY-EVENT.README.md is packaged as a classpath
 *       resource alongside the schema and explains every field / exhibits
 *       at least 3 canonical examples.
 *   <li>The project-level .gitignore excludes the global cache index
 *       (.claude/telemetry/index.json) while permitting the committed
 *       per-epic NDJSON files.
 * </ul>
 */
class TelemetryContractDocsTest {

    private static final String README_CLASSPATH =
            "/shared/templates/_TEMPLATE-TELEMETRY-EVENT.README.md";

    private static final Path GITIGNORE_PATH =
            resolveRepoRoot().resolve(".gitignore");

    @Test
    @DisplayName("readme_isPackagedAndNonEmpty")
    void readme_isPackagedAndNonEmpty() throws IOException {
        try (InputStream in =
                TelemetryContractDocsTest.class.getResourceAsStream(
                        README_CLASSPATH)) {
            assertThat(in)
                    .as("README must be packaged at " + README_CLASSPATH)
                    .isNotNull();
            String body = new String(in.readAllBytes());
            assertThat(body).isNotBlank();
        }
    }

    @Test
    @DisplayName("readme_documentsEveryRequiredField")
    void readme_documentsEveryRequiredField() throws IOException {
        String body = readReadme();
        assertThat(body)
                .contains("schemaVersion")
                .contains("eventId")
                .contains("timestamp")
                .contains("sessionId")
                .contains("type");
    }

    @Test
    @DisplayName("readme_citesStorageLayoutPaths")
    void readme_citesStorageLayoutPaths() throws IOException {
        String body = readReadme();
        assertThat(body)
                .contains("plans/epic-XXXX/telemetry/events.ndjson")
                .contains(".claude/telemetry/index.json");
    }

    @Test
    @DisplayName("readme_showsAtLeastThreeFixtureExamples")
    void readme_showsAtLeastThreeFixtureExamples() throws IOException {
        String body = readReadme();
        assertThat(body)
                .contains("session-start-minimal.json")
                .contains("skill-end-with-duration.json")
                .contains("tool-call-failed.json");
    }

    @Test
    @DisplayName("gitignore_excludesTelemetryIndex")
    void gitignore_excludesTelemetryIndex() throws IOException {
        assertThat(GITIGNORE_PATH)
                .as("Project .gitignore must exist")
                .exists();
        String body =
                Files.readString(GITIGNORE_PATH, StandardCharsets.UTF_8);
        assertThat(body).contains(".claude/telemetry/index.json");
    }

    private static String readReadme() throws IOException {
        try (InputStream in =
                TelemetryContractDocsTest.class.getResourceAsStream(
                        README_CLASSPATH)) {
            assertThat(in).isNotNull();
            return new String(in.readAllBytes());
        }
    }

    private static Path resolveRepoRoot() {
        // src/test/java tests run with cwd = module root (java/). Repo root
        // is the parent directory.
        return Path.of(System.getProperty("user.dir")).getParent();
    }
}
