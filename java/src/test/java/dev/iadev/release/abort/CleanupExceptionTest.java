package dev.iadev.release.abort;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CleanupException}.
 */
@DisplayName("CleanupExceptionTest")
class CleanupExceptionTest {

    @Test
    @DisplayName(
            "constructor_withCodeAndMessage_preservesBoth")
    void constructor_withCodeAndMessage_preservesBoth() {
        CleanupException ex = new CleanupException(
                "ABORT_PR_CLOSE_FAILED",
                "gh CLI failed");

        assertThat(ex.errorCode())
                .isEqualTo("ABORT_PR_CLOSE_FAILED");
        assertThat(ex.getMessage())
                .isEqualTo("gh CLI failed");
    }

    @Test
    @DisplayName(
            "constructor_withCause_preservesCause")
    void constructor_withCause_preservesCause() {
        IOException cause = new IOException("timeout");
        CleanupException ex = new CleanupException(
                "ABORT_BRANCH_DELETE_FAILED",
                "branch delete failed",
                cause);

        assertThat(ex.errorCode())
                .isEqualTo("ABORT_BRANCH_DELETE_FAILED");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
