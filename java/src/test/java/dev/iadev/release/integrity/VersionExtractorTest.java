package dev.iadev.release.integrity;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VersionExtractorTest {

    @Test
    @DisplayName("extractPomVersion_noPomInMap_returnsEmpty")
    void extractPomVersion_noPomInMap_returnsEmpty() {
        Map<String, String> files = Map.of("README.md", "v1.0.0");

        assertThat(VersionExtractor.extractPomVersion(files)).isEmpty();
    }

    @Test
    @DisplayName("extractPomVersion_pomWithNullContent_isIgnored")
    void extractPomVersion_pomWithNullContent_isIgnored() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("pom.xml", null);

        assertThat(VersionExtractor.extractPomVersion(files)).isEmpty();
    }

    @Test
    @DisplayName("extractPomVersionFromContent_noVersionTag_returnsEmpty")
    void extractPomVersionFromContent_noVersionTag_returnsEmpty() {
        String pom = "<project><groupId>x</groupId></project>";

        assertThat(VersionExtractor.extractPomVersionFromContent(pom)).isEmpty();
    }

    @Test
    @DisplayName("extractPomVersionFromContent_validVersion_returnsValue")
    void extractPomVersionFromContent_validVersion_returnsValue() {
        String pom = "<project><version>2.5.0</version></project>";

        assertThat(VersionExtractor.extractPomVersionFromContent(pom))
                .contains("2.5.0");
    }

    @Test
    @DisplayName("extractPomVersionFromContent_parentVersionBeforeProjectVersion_returnsProjectVersion")
    void extractPomVersionFromContent_parentVersionBeforeProjectVersion_returnsProjectVersion() {
        String pom = "<project>"
                + "<modelVersion>4.0.0</modelVersion>"
                + "<parent>"
                + "<groupId>dev.iadev</groupId>"
                + "<artifactId>parent</artifactId>"
                + "<version>1.0.0</version>"
                + "</parent>"
                + "<artifactId>child</artifactId>"
                + "<version>2.5.0</version>"
                + "</project>";

        assertThat(VersionExtractor.extractPomVersionFromContent(pom))
                .contains("2.5.0");
    }

    @Test
    @DisplayName("normalize_leadingVUppercase_stripped")
    void normalize_leadingVUppercase_stripped() {
        assertThat(VersionExtractor.normalize("V1.2.3")).isEqualTo("1.2.3");
    }

    @Test
    @DisplayName("normalize_leadingVLowercase_stripped")
    void normalize_leadingVLowercase_stripped() {
        assertThat(VersionExtractor.normalize("v1.2.3")).isEqualTo("1.2.3");
    }

    @Test
    @DisplayName("normalize_snapshotSuffix_stripped")
    void normalize_snapshotSuffix_stripped() {
        assertThat(VersionExtractor.normalize("3.1.0-SNAPSHOT")).isEqualTo("3.1.0");
    }

    @Test
    @DisplayName("normalize_plainSemver_unchanged")
    void normalize_plainSemver_unchanged() {
        assertThat(VersionExtractor.normalize("3.1.0")).isEqualTo("3.1.0");
    }

    @Test
    @DisplayName("lineNumberOfOffset_zeroOffset_returnsOne")
    void lineNumberOfOffset_zeroOffset_returnsOne() {
        assertThat(VersionExtractor.lineNumberOfOffset("abc", 0)).isEqualTo(1);
    }

    @Test
    @DisplayName("lineNumberOfOffset_offsetBeyondLength_clampsToContent")
    void lineNumberOfOffset_offsetBeyondLength_clampsToContent() {
        String content = "line1\nline2\nline3";
        // Offset past end should count newlines seen in content (2).
        int line = VersionExtractor.lineNumberOfOffset(content, 999);
        assertThat(line).isEqualTo(3);
    }

    @Test
    @DisplayName("lineNumberOfOffset_multilineMidContent")
    void lineNumberOfOffset_multilineMidContent() {
        String content = "a\nb\nc\nd";
        int line = VersionExtractor.lineNumberOfOffset(content, 4);
        assertThat(line).isEqualTo(3);
    }
}
