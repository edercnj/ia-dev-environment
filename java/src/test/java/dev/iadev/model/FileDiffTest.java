package dev.iadev.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FileDiff")
class FileDiffTest {

    @Test
    @DisplayName("stores all fields correctly")
    void constructor_allFields_storedCorrectly() {
        var diff = new FileDiff(
                "rules/01-identity.md",
                "- old line\n+ new line",
                120L,
                125L);

        assertThat(diff.path())
                .isEqualTo("rules/01-identity.md");
        assertThat(diff.diff())
                .isEqualTo("- old line\n+ new line");
        assertThat(diff.sourceSize()).isEqualTo(120L);
        assertThat(diff.referenceSize()).isEqualTo(125L);
    }

    @Test
    @DisplayName("equality works for same values")
    void equality_sameValues_equal() {
        var a = new FileDiff("f.md", "diff", 10L, 20L);
        var b = new FileDiff("f.md", "diff", 10L, 20L);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
