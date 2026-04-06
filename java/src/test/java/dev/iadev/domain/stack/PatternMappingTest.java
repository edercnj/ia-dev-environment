package dev.iadev.domain.stack;

import dev.iadev.testutil.TestConfigBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PatternMapping")
class PatternMappingTest {

    @Nested
    @DisplayName("Constants")
    class ConstantsTests {

        @Test
        @DisplayName("UNIVERSAL_PATTERNS has architectural and data")
        void universalPatterns_whenCalled_twoEntries() {
            assertThat(PatternMapping.UNIVERSAL_PATTERNS)
                    .containsExactly("architectural", "data");
        }

        @Test
        @DisplayName("ARCHITECTURE_PATTERNS has 7 entries")
        void architecturePatterns_whenCalled_sevenEntries() {
            assertThat(PatternMapping.ARCHITECTURE_PATTERNS)
                    .hasSize(7);
        }

        @Test
        @DisplayName("microservice has 3 pattern categories")
        void architecturePatterns_microservice_three() {
            assertThat(PatternMapping.ARCHITECTURE_PATTERNS
                    .get("microservice"))
                    .containsExactly("microservice", "resilience",
                            "integration");
        }

        @Test
        @DisplayName("library has empty pattern list")
        void architecturePatterns_library_empty() {
            assertThat(PatternMapping.ARCHITECTURE_PATTERNS.get("library"))
                    .isEmpty();
        }

        @Test
        @DisplayName("EVENT_DRIVEN_PATTERNS has 4 entries")
        void eventDrivenPatterns_whenCalled_fourEntries() {
            assertThat(PatternMapping.EVENT_DRIVEN_PATTERNS).hasSize(4);
            assertThat(PatternMapping.EVENT_DRIVEN_PATTERNS)
                    .contains("saga-pattern", "outbox-pattern",
                            "event-sourcing", "dead-letter-queue");
        }
    }

    @Nested
    @DisplayName("selectPatterns()")
    class SelectPatternsTests {

        @Test
        @DisplayName("microservice returns sorted universal + microservice")
        void selectPatterns_microservice_sorted() {
            var config = TestConfigBuilder.builder()
                    .architectureStyle("microservice")
                    .build();

            var result = PatternMapping.selectPatterns(config);

            assertThat(result).containsExactly(
                    "architectural", "data", "integration",
                    "microservice", "resilience");
        }

        @Test
        @DisplayName("monolith returns universal + integration")
        void selectPatterns_monolith_integration() {
            var config = TestConfigBuilder.builder()
                    .architectureStyle("monolith")
                    .build();

            var result = PatternMapping.selectPatterns(config);

            assertThat(result).containsExactly(
                    "architectural", "data", "integration");
        }

        @Test
        @DisplayName("library returns universal only")
        void selectPatterns_library_universalOnly() {
            var config = TestConfigBuilder.builder()
                    .architectureStyle("library")
                    .build();

            var result = PatternMapping.selectPatterns(config);

            assertThat(result).containsExactly(
                    "architectural", "data");
        }

        @Test
        @DisplayName("unknown style returns empty list")
        void selectPatterns_unknown_empty() {
            var config = TestConfigBuilder.builder()
                    .architectureStyle("unknown-style")
                    .build();

            var result = PatternMapping.selectPatterns(config);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("event-driven adds event patterns")
        void selectPatterns_eventDriven_includesEvents() {
            var config = TestConfigBuilder.builder()
                    .architectureStyle("microservice")
                    .eventDriven(true)
                    .build();

            var result = PatternMapping.selectPatterns(config);

            assertThat(result).contains(
                    "saga-pattern", "outbox-pattern",
                    "event-sourcing", "dead-letter-queue");
        }

        @Test
        @DisplayName("non-event-driven excludes event patterns")
        void selectPatterns_notEventDriven_excludesEvents() {
            var config = TestConfigBuilder.builder()
                    .architectureStyle("microservice")
                    .eventDriven(false)
                    .build();

            var result = PatternMapping.selectPatterns(config);

            assertThat(result).doesNotContain(
                    "saga-pattern", "outbox-pattern");
        }

        @Test
        @DisplayName("result is sorted")
        void selectPatterns_whenCalled_sorted() {
            var config = TestConfigBuilder.builder()
                    .architectureStyle("microservice")
                    .eventDriven(true)
                    .build();

            var result = PatternMapping.selectPatterns(config);

            assertThat(result).isSorted();
        }

        @Test
        @DisplayName("result is deduplicated")
        void selectPatterns_whenCalled_deduplicated() {
            var config = TestConfigBuilder.builder()
                    .architectureStyle("microservice")
                    .build();

            var result = PatternMapping.selectPatterns(config);

            assertThat(result).doesNotHaveDuplicates();
        }
    }
}
