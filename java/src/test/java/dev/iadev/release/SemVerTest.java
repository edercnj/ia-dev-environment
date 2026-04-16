package dev.iadev.release;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("SemVer — parse / toString / validation")
class SemVerTest {

    static Stream<Arguments> validLiterals() {
        return Stream.of(
                Arguments.of("0.0.0", 0, 0, 0, null),
                Arguments.of("1.2.3", 1, 2, 3, null),
                Arguments.of("10.20.30", 10, 20, 30, null),
                Arguments.of("v3.1.0", 3, 1, 0, null),
                Arguments.of("1.0.0-rc.1", 1, 0, 0, "rc.1"));
    }

    @ParameterizedTest
    @MethodSource("validLiterals")
    @DisplayName("parse_validSemVer_returnsComponents")
    void parse_validSemVer_returnsComponents(
            String raw, int major, int minor, int patch, String pre) {
        SemVer version = SemVer.parse(raw);

        assertThat(version.major()).isEqualTo(major);
        assertThat(version.minor()).isEqualTo(minor);
        assertThat(version.patch()).isEqualTo(patch);
        assertThat(version.preRelease()).isEqualTo(pre);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.2", "01.2.3", "1.2.3.4", "abc", "", "1.2.3+build.1"})
    @DisplayName("parse_invalidSemVer_raisesVersionInvalidFormat")
    void parse_invalidSemVer_raisesVersionInvalidFormat(String raw) {
        assertThatThrownBy(() -> SemVer.parse(raw))
                .isInstanceOf(InvalidBumpException.class)
                .extracting("code")
                .isEqualTo(InvalidBumpException.Code.VERSION_INVALID_FORMAT);
    }

    @Test
    @DisplayName("toString_stableRelease_returnsDottedForm")
    void toString_stableRelease_returnsDottedForm() {
        assertThat(new SemVer(2, 4, 1, null)).hasToString("2.4.1");
    }

    @Test
    @DisplayName("toString_preRelease_appendsSuffix")
    void toString_preRelease_appendsSuffix() {
        assertThat(new SemVer(2, 4, 1, "rc.2")).hasToString("2.4.1-rc.2");
    }

    @Test
    @DisplayName("new_negativeComponent_rejected")
    void new_negativeComponent_rejected() {
        assertThatThrownBy(() -> new SemVer(-1, 0, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("new_emptyPreRelease_rejected")
    void new_emptyPreRelease_rejected() {
        assertThatThrownBy(() -> new SemVer(1, 0, 0, ""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
