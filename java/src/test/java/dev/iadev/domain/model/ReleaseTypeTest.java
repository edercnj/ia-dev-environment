package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReleaseTypeTest {

    @Test
    @DisplayName("wireValue_release_returnsLowercase")
    void wireValue_release_returnsLowercase() {
        assertThat(ReleaseType.RELEASE.wireValue())
                .isEqualTo("release");
    }

    @Test
    @DisplayName("wireValue_hotfix_returnsLowercase")
    void wireValue_hotfix_returnsLowercase() {
        assertThat(ReleaseType.HOTFIX.wireValue())
                .isEqualTo("hotfix");
    }

    @Test
    @DisplayName("fromWire_null_returnsRelease (default per §5.1)")
    void fromWire_null_returnsRelease() {
        assertThat(ReleaseType.fromWire(null))
                .isEqualTo(ReleaseType.RELEASE);
    }

    @Test
    @DisplayName("fromWire_release_returnsRelease")
    void fromWire_release_returnsRelease() {
        assertThat(ReleaseType.fromWire("release"))
                .isEqualTo(ReleaseType.RELEASE);
    }

    @Test
    @DisplayName("fromWire_hotfix_returnsHotfix")
    void fromWire_hotfix_returnsHotfix() {
        assertThat(ReleaseType.fromWire("hotfix"))
                .isEqualTo(ReleaseType.HOTFIX);
    }

    @Test
    @DisplayName("fromWire_unknown_throws")
    void fromWire_unknown_throws() {
        assertThatThrownBy(() ->
                ReleaseType.fromWire("bogus"))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining("bogus");
    }
}
