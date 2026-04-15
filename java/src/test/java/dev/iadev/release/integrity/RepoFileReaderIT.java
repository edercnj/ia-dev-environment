package dev.iadev.release.integrity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepoFileReaderIT {

    @Test
    @DisplayName("readText_existingUtf8File_returnsContent")
    void readText_existingUtf8File_returnsContent(@TempDir Path root) throws IOException {
        Path file = root.resolve("CHANGELOG.md");
        Files.writeString(file, "## [Unreleased]\n- açaí\n", StandardCharsets.UTF_8);

        Optional<String> content = new RepoFileReader(root).readText("CHANGELOG.md");

        assertThat(content).isPresent();
        assertThat(content.get()).contains("açaí");
    }

    @Test
    @DisplayName("readText_missingFile_returnsEmpty")
    void readText_missingFile_returnsEmpty(@TempDir Path root) {
        Optional<String> content = new RepoFileReader(root).readText("nope.md");

        assertThat(content).isEmpty();
    }

    @Test
    @DisplayName("readText_pathTraversalWithDotDot_throwsSecurityException")
    void readText_pathTraversalWithDotDot_throwsSecurityException(@TempDir Path root) {
        RepoFileReader reader = new RepoFileReader(root);

        assertThatThrownBy(() -> reader.readText("../escape.txt"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("traversal");
    }

    @Test
    @DisplayName("readText_blankPath_throwsSecurityException")
    void readText_blankPath_throwsSecurityException(@TempDir Path root) {
        RepoFileReader reader = new RepoFileReader(root);

        assertThatThrownBy(() -> reader.readText("  "))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    @DisplayName("readText_nullRelativePath_throwsNpe")
    void readText_nullRelativePath_throwsNpe(@TempDir Path root) {
        RepoFileReader reader = new RepoFileReader(root);

        assertThatThrownBy(() -> reader.readText(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("readTexts_mixedExistingAndMissing_returnsOnlyExisting")
    void readTexts_mixedExistingAndMissing_returnsOnlyExisting(@TempDir Path root) throws IOException {
        Files.writeString(root.resolve("pom.xml"), "<project><version>1.0.0</version></project>",
                StandardCharsets.UTF_8);

        Map<String, String> out = new RepoFileReader(root).readTexts(
                List.of("pom.xml", "README.md", "CHANGELOG.md"));

        assertThat(out).containsOnlyKeys("pom.xml");
        assertThat(out.get("pom.xml")).contains("1.0.0");
    }

    @Test
    @DisplayName("readText_errorMessageNeverIncludesContent")
    void readText_errorMessageNeverIncludesContent(@TempDir Path root) {
        RepoFileReader reader = new RepoFileReader(root);

        assertThatThrownBy(() -> reader.readText("../../secret.txt"))
                .isInstanceOf(SecurityException.class)
                .matches(t -> !t.getMessage().contains("secret content"));
    }

    @Test
    @DisplayName("constructor_nullRoot_throwsNpe")
    void constructor_nullRoot_throwsNpe() {
        assertThatThrownBy(() -> new RepoFileReader(null))
                .isInstanceOf(NullPointerException.class);
    }
}
