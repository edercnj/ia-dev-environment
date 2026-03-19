package dev.iadev.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProjectFoundation")
class ProjectFoundationTest {

    @Test
    @DisplayName("DEFAULT has correct values matching TypeScript")
    void default_matchesTypeScript() {
        var foundation = ProjectFoundation.DEFAULT;

        assertThat(foundation.name())
                .isEqualTo("ia-dev-environment");
        assertThat(foundation.version()).isEqualTo("0.1.0");
        assertThat(foundation.moduleType()).isEqualTo("module");
    }

    @Test
    @DisplayName("custom values work correctly")
    void customValues_storedCorrectly() {
        var foundation = new ProjectFoundation(
                "my-project", "1.0.0", "module");

        assertThat(foundation.name()).isEqualTo("my-project");
        assertThat(foundation.version()).isEqualTo("1.0.0");
    }
}
