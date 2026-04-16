package dev.iadev.release.state;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link WaitingFor} enum.
 *
 * <p>Covers TPP-2 (constant) enumeration of the 6 declared
 * values per story-0039-0002 §3.1.
 */
@DisplayName("WaitingForTest")
class WaitingForTest {

    @Test
    @DisplayName("enum has exactly 6 declared values")
    void enumHasSixDeclaredValues() {
        assertThat(WaitingFor.values())
                .hasSize(6)
                .containsExactly(
                        WaitingFor.NONE,
                        WaitingFor.PR_REVIEW,
                        WaitingFor.PR_MERGE,
                        WaitingFor.BACKMERGE_REVIEW,
                        WaitingFor.BACKMERGE_MERGE,
                        WaitingFor.USER_CONFIRMATION);
    }

    @Test
    @DisplayName("enum valueOf accepts every canonical name")
    void enumValueOfAcceptsCanonicalNames() {
        assertThat(WaitingFor.valueOf("NONE"))
                .isEqualTo(WaitingFor.NONE);
        assertThat(WaitingFor.valueOf("PR_REVIEW"))
                .isEqualTo(WaitingFor.PR_REVIEW);
        assertThat(WaitingFor.valueOf("PR_MERGE"))
                .isEqualTo(WaitingFor.PR_MERGE);
        assertThat(WaitingFor.valueOf("BACKMERGE_REVIEW"))
                .isEqualTo(WaitingFor.BACKMERGE_REVIEW);
        assertThat(WaitingFor.valueOf("BACKMERGE_MERGE"))
                .isEqualTo(WaitingFor.BACKMERGE_MERGE);
        assertThat(WaitingFor.valueOf("USER_CONFIRMATION"))
                .isEqualTo(WaitingFor.USER_CONFIRMATION);
    }

    @Test
    @DisplayName("enum rejects unknown values via valueOf")
    void enumRejectsUnknownValues() {
        assertThatThrownBy(() ->
                WaitingFor.valueOf("UNKNOWN_VALUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
