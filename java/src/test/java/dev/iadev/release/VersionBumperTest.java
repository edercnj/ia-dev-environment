package dev.iadev.release;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("VersionBumper — pure-domain bump rules")
class VersionBumperTest {

    static Stream<Arguments> bumpCases() {
        return Stream.of(
                Arguments.of(new SemVer(3, 1, 0, null), BumpType.MINOR, "3.2.0"),
                Arguments.of(new SemVer(3, 1, 0, null), BumpType.PATCH, "3.1.1"),
                Arguments.of(new SemVer(3, 1, 0, null), BumpType.MAJOR, "4.0.0"),
                Arguments.of(SemVer.ZERO, BumpType.MINOR, "0.1.0"),
                Arguments.of(SemVer.ZERO, BumpType.PATCH, "0.0.1"),
                Arguments.of(SemVer.ZERO, BumpType.MAJOR, "1.0.0"),
                Arguments.of(new SemVer(1, 2, 3, "rc.1"), BumpType.PATCH, "1.2.4"));
    }

    @ParameterizedTest
    @MethodSource("bumpCases")
    @DisplayName("bump_baseAndType_producesExpectedVersion")
    void bump_baseAndType_producesExpectedVersion(SemVer base, BumpType type, String expected) {
        SemVer result = VersionBumper.bump(base, type);

        assertThat(result).hasToString(expected);
    }

    @Test
    @DisplayName("bump_explicitType_rejectedWithInvalidBumpCombination")
    void bump_explicitType_rejectedWithInvalidBumpCombination() {
        assertThatThrownBy(() -> VersionBumper.bump(SemVer.ZERO, BumpType.EXPLICIT))
                .isInstanceOf(InvalidBumpException.class)
                .extracting("code")
                .isEqualTo(InvalidBumpException.Code.INVALID_BUMP_COMBINATION);
    }

    @Test
    @DisplayName("bump_nullBase_throwsNullPointer")
    void bump_nullBase_throwsNullPointer() {
        assertThatThrownBy(() -> VersionBumper.bump(null, BumpType.PATCH))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("bump_nullType_throwsNullPointer")
    void bump_nullType_throwsNullPointer() {
        assertThatThrownBy(() -> VersionBumper.bump(SemVer.ZERO, null))
                .isInstanceOf(NullPointerException.class);
    }
}
