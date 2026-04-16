package dev.iadev.release.summary;

import dev.iadev.release.ReleaseContext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coverage-driven tests for
 * {@link SummaryRenderer#sanitize(String)} branches
 * (story-0039-0014 TASK-013).
 */
@DisplayName("SummaryRenderer — sanitize")
class SummaryRendererSanitizeTest {

    @Test
    @DisplayName("passes newline / CR / tab verbatim")
    void sanitize_passesWhitespace() {
        // Use version wrapper — caller cannot inject \n in
        // release string under the strict SemVer guard, but
        // the sanitiser must not strip whitespace chars.
        String out = SummaryRenderer.sanitize("a\nb\tc\rd");

        assertThat(out).isEqualTo("a\nb\tc\rd");
    }

    @Test
    @DisplayName("strips low-range control chars")
    void sanitize_stripsLowRangeControl() {
        String out = SummaryRenderer.sanitize(
                "A\u0001\u0002B");

        assertThat(out).isEqualTo("AB");
    }

    @Test
    @DisplayName("strips ANSI escape introducer")
    void sanitize_stripsEsc() {
        String out = SummaryRenderer.sanitize(
                "A\u001bB");

        assertThat(out).isEqualTo("AB");
    }

    @Test
    @DisplayName("null input yields empty string")
    void sanitize_nullReturnsEmpty() {
        String out = SummaryRenderer.sanitize(null);

        assertThat(out).isEqualTo("");
    }

    @Test
    @DisplayName("render passes through printable ASCII")
    void render_printableAscii() {
        String out = SummaryRenderer.render(
                "3.1.0", "3.1.1", 0,
                ReleaseContext.forHotfix());

        assertThat(out).contains("3.1.1");
    }
}
