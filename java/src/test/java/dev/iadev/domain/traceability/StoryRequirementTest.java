package dev.iadev.domain.traceability;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoryRequirementTest {

    @Nested
    class Construction {

        @Test
        void create_withAllFields_accessorsReturnValues() {
            var req = new StoryRequirement(
                    "@GK-1", "payment approved",
                    Optional.of("AT-1"));

            assertThat(req.gherkinId()).isEqualTo("@GK-1");
            assertThat(req.title())
                    .isEqualTo("payment approved");
            assertThat(req.acceptanceTestId())
                    .isPresent()
                    .hasValue("AT-1");
        }

        @Test
        void create_withoutAtId_defaultsToEmpty() {
            var req = new StoryRequirement(
                    "@GK-2", "payment denied", null);

            assertThat(req.acceptanceTestId()).isEmpty();
        }

        @Test
        void create_withEmptyOptional_acceptanceTestIdEmpty() {
            var req = new StoryRequirement(
                    "@GK-3", "timeout", Optional.empty());

            assertThat(req.acceptanceTestId()).isEmpty();
        }
    }

    @Nested
    class Validation {

        @Test
        void create_nullGherkinId_throwsException() {
            assertThatThrownBy(() -> new StoryRequirement(
                    null, "title", Optional.empty()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("gherkinId");
        }

        @Test
        void create_blankGherkinId_throwsException() {
            assertThatThrownBy(() -> new StoryRequirement(
                    "  ", "title", Optional.empty()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("gherkinId");
        }

        @Test
        void create_nullTitle_throwsException() {
            assertThatThrownBy(() -> new StoryRequirement(
                    "@GK-1", null, Optional.empty()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("title");
        }

        @Test
        void create_blankTitle_throwsException() {
            assertThatThrownBy(() -> new StoryRequirement(
                    "@GK-1", "", Optional.empty()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("title");
        }
    }
}
