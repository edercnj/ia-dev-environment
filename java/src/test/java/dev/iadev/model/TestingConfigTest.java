package dev.iadev.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TestingConfig")
class TestingConfigTest {

    @Nested
    @DisplayName("fromMap()")
    class FromMap {

        @Test
        @DisplayName("creates config with all fields")
        void fromMap_allFields_allSet() {
            var map = Map.<String, Object>of(
                    "smoke_tests", false,
                    "contract_tests", true,
                    "performance_tests", false,
                    "coverage_line", 80,
                    "coverage_branch", 70);

            var result = TestingConfig.fromMap(map);

            assertThat(result.smokeTests()).isFalse();
            assertThat(result.contractTests()).isTrue();
            assertThat(result.performanceTests()).isFalse();
            assertThat(result.coverageLine()).isEqualTo(80);
            assertThat(result.coverageBranch()).isEqualTo(70);
        }

        @Test
        @DisplayName("empty map uses default values 95/90")
        void fromMap_emptyMap_defaults() {
            var result = TestingConfig.fromMap(Map.of());

            assertThat(result.coverageLine()).isEqualTo(95);
            assertThat(result.coverageBranch()).isEqualTo(90);
            assertThat(result.smokeTests()).isTrue();
            assertThat(result.contractTests()).isFalse();
            assertThat(result.performanceTests()).isTrue();
        }

        @Test
        @DisplayName("partial map defaults missing fields")
        void fromMap_partialMap_missingDefaulted() {
            var map = Map.<String, Object>of(
                    "coverage_line", 85);

            var result = TestingConfig.fromMap(map);

            assertThat(result.coverageLine()).isEqualTo(85);
            assertThat(result.coverageBranch()).isEqualTo(90);
            assertThat(result.smokeTests()).isTrue();
        }

        @Test
        @DisplayName("non-numeric coverage values default to thresholds")
        void fromMap_nonNumericCoverage_defaults() {
            var map = Map.<String, Object>of(
                    "coverage_line", "high");

            var result = TestingConfig.fromMap(map);

            assertThat(result.coverageLine()).isEqualTo(95);
        }
    }
}
