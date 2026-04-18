package dev.iadev.release.dryrun;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link PhaseDescriptor} validation branches.
 */
@DisplayName("PhaseDescriptor")
class PhaseDescriptorTest {

    @Test
    @DisplayName("constructor_blankName_throws")
    void constructor_blankName_throws() {
        assertThatThrownBy(
                () -> new PhaseDescriptor(
                        "   ", List.of("cmd")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    @DisplayName("constructor_validName_storesDefensiveCopy")
    void constructor_validName_storesDefensiveCopy() {
        PhaseDescriptor pd = new PhaseDescriptor(
                "VALIDATE_DEEP", List.of("mvn verify"));

        assertThat(pd.name()).isEqualTo("VALIDATE_DEEP");
        assertThat(pd.commands())
                .containsExactly("mvn verify");
    }
}
