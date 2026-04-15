package dev.iadev.release.integrity;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiffTodoScannerTest {

    @Test
    @DisplayName("scan_todoBeforeAnyFileHeader_isSkipped")
    void scan_todoBeforeAnyFileHeader_isSkipped() {
        // A '+' line with TODO that appears BEFORE any `+++ b/path` header has no owning file
        // and must be ignored (fileAtOffset returns null branch).
        String diff = "+ // TODO stray line without file header\n";

        List<String> hits = DiffTodoScanner.scan(diff);

        assertThat(hits).isEmpty();
    }

    @Test
    @DisplayName("scan_markdownFile_isIncluded")
    void scan_markdownFile_isIncluded() {
        String diff = """
                +++ b/docs/guide.md
                @@
                +## New section
                +TODO: fill this in
                """;

        List<String> hits = DiffTodoScanner.scan(diff);

        assertThat(hits).contains("docs/guide.md");
    }

    @Test
    @DisplayName("scan_pebTemplate_isIncluded")
    void scan_pebTemplate_isIncluded() {
        String diff = """
                +++ b/templates/sample.peb
                @@
                +{{ name }}
                +{# TODO add more fields #}
                """;

        List<String> hits = DiffTodoScanner.scan(diff);

        assertThat(hits).contains("templates/sample.peb");
    }

    @Test
    @DisplayName("scan_fixmeMarker_isDetected")
    void scan_fixmeMarker_isDetected() {
        String diff = """
                +++ b/src/main/java/Y.java
                @@
                +// FIXME broken
                """;

        List<String> hits = DiffTodoScanner.scan(diff);

        assertThat(hits).contains("src/main/java/Y.java");
    }

    @Test
    @DisplayName("scan_hackMarker_isDetected")
    void scan_hackMarker_isDetected() {
        String diff = """
                +++ b/src/main/java/Z.java
                @@
                +// HACK workaround
                """;

        List<String> hits = DiffTodoScanner.scan(diff);

        assertThat(hits).contains("src/main/java/Z.java");
    }

    @Test
    @DisplayName("scan_xxxMarker_isDetected")
    void scan_xxxMarker_isDetected() {
        String diff = """
                +++ b/src/main/java/W.java
                @@
                +// XXX revisit
                """;

        List<String> hits = DiffTodoScanner.scan(diff);

        assertThat(hits).contains("src/main/java/W.java");
    }

    @Test
    @DisplayName("scan_testsPathFragment_isExcluded")
    void scan_testsPathFragment_isExcluded() {
        String diff = """
                +++ b/module/tests/ModuleTest.java
                @@
                +// TODO brittle
                """;

        List<String> hits = DiffTodoScanner.scan(diff);

        assertThat(hits).isEmpty();
    }
}
