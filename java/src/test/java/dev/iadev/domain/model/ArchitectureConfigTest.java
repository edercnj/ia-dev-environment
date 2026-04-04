package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ArchitectureConfig")
class ArchitectureConfigTest {

    @Nested
    @DisplayName("fromMap()")
    class FromMap {

        @Test
        @DisplayName("creates config with all fields")
        void fromMap_allFields_allSet() {
            var map = Map.<String, Object>of(
                    "style", "microservice",
                    "domain_driven", true,
                    "event_driven", true,
                    "ddd_enabled", true);

            var result = ArchitectureConfig.fromMap(map);

            assertThat(result.style())
                    .isEqualTo("microservice");
            assertThat(result.domainDriven()).isTrue();
            assertThat(result.eventDriven()).isTrue();
            assertThat(result.dddEnabled()).isTrue();
        }

        @Test
        @DisplayName("defaults domainDriven and eventDriven"
                + " to false")
        void fromMap_onlyStyle_booleansDefaultFalse() {
            var map = Map.<String, Object>of(
                    "style", "library");

            var result = ArchitectureConfig.fromMap(map);

            assertThat(result.style())
                    .isEqualTo("library");
            assertThat(result.domainDriven()).isFalse();
            assertThat(result.eventDriven()).isFalse();
        }

        @Test
        @DisplayName("throws when style is missing")
        void fromMap_missingStyle_throwsException() {
            var map = Map.<String, Object>of(
                    "domain_driven", true);

            assertThatThrownBy(
                    () -> ArchitectureConfig.fromMap(map))
                    .isInstanceOf(
                            ConfigValidationException.class)
                    .hasMessageContaining("style");
        }

        @Test
        @DisplayName("non-boolean domain_driven defaults"
                + " to false")
        void fromMap_nonBooleanDomainDriven_defaultsFalse() {
            var map = Map.<String, Object>of(
                    "style", "monolith",
                    "domain_driven", "yes");

            var result = ArchitectureConfig.fromMap(map);

            assertThat(result.domainDriven()).isFalse();
        }

        @Test
        @DisplayName("defaults validateWithArchUnit to false")
        void fromMap_noArchUnit_defaultsFalse() {
            var map = Map.<String, Object>of(
                    "style", "hexagonal");

            var result = ArchitectureConfig.fromMap(map);

            assertThat(result.validateWithArchUnit())
                    .isFalse();
        }

        @Test
        @DisplayName("defaults basePackage to empty string")
        void fromMap_noBasePackage_defaultsEmpty() {
            var map = Map.<String, Object>of(
                    "style", "hexagonal");

            var result = ArchitectureConfig.fromMap(map);

            assertThat(result.basePackage()).isEmpty();
        }

        @Test
        @DisplayName("parses validateWithArchUnit and"
                + " basePackage")
        void fromMap_allNewFields_allSet() {
            var map = Map.<String, Object>of(
                    "style", "hexagonal",
                    "validate_with_archunit", true,
                    "base_package", "com.example.myapp");

            var result = ArchitectureConfig.fromMap(map);

            assertThat(result.validateWithArchUnit())
                    .isTrue();
            assertThat(result.basePackage())
                    .isEqualTo("com.example.myapp");
        }

        @Test
        @DisplayName("non-boolean validate_with_archunit"
                + " defaults to false")
        void fromMap_nonBoolArchUnit_defaultsFalse() {
            var map = Map.<String, Object>of(
                    "style", "hexagonal",
                    "validate_with_archunit", "yes");

            var result = ArchitectureConfig.fromMap(map);

            assertThat(result.validateWithArchUnit())
                    .isFalse();
        }

        @Test
        @DisplayName("ddd_enabled true is parsed correctly")
        void fromMap_dddEnabledTrue_parsedCorrectly() {
            var map = Map.<String, Object>of(
                    "style", "microservice",
                    "ddd_enabled", true);

            var result = ArchitectureConfig.fromMap(map);

            assertThat(result.dddEnabled()).isTrue();
        }

        @Test
        @DisplayName("ddd_enabled defaults to false when absent")
        void fromMap_dddEnabledAbsent_defaultsFalse() {
            var map = Map.<String, Object>of(
                    "style", "library");

            var result = ArchitectureConfig.fromMap(map);

            assertThat(result.dddEnabled()).isFalse();
        }

        @Test
        @DisplayName("non-boolean ddd_enabled defaults to false")
        void fromMap_nonBooleanDddEnabled_defaultsFalse() {
            var map = Map.<String, Object>of(
                    "style", "monolith",
                    "ddd_enabled", "yes");

            var result = ArchitectureConfig.fromMap(map);

            assertThat(result.dddEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("eventStore field")
    class EventStoreField {

        @Test
        @DisplayName("defaults eventStore to eventstoredb")
        void fromMap_noEventStore_defaultsEventstoredb() {
            var map = Map.<String, Object>of(
                    "style", "cqrs");

            var result = ArchitectureConfig.fromMap(map);

            assertThat(result.eventStore())
                    .isEqualTo("eventstoredb");
        }

        @Test
        @DisplayName("accepts explicit eventStore value")
        void fromMap_explicitEventStore_setsValue() {
            var map = Map.<String, Object>of(
                    "style", "cqrs",
                    "event_store", "axon");

            var result = ArchitectureConfig.fromMap(map);

            assertThat(result.eventStore())
                    .isEqualTo("axon");
        }

        @Test
        @DisplayName("accepts custom eventStore value")
        void fromMap_customEventStore_setsValue() {
            var map = Map.<String, Object>of(
                    "style", "cqrs",
                    "event_store", "custom");

            var result = ArchitectureConfig.fromMap(map);

            assertThat(result.eventStore())
                    .isEqualTo("custom");
        }
    }

    @Nested
    @DisplayName("snapshotPolicy.eventsPerSnapshot field")
    class SnapshotPolicyField {

        @Test
        @DisplayName("defaults eventsPerSnapshot to 100")
        void fromMap_noSnapshot_defaults100() {
            var map = Map.<String, Object>of(
                    "style", "cqrs");

            var result = ArchitectureConfig.fromMap(map);

            assertThat(result.eventsPerSnapshot())
                    .isEqualTo(100);
        }

        @Test
        @DisplayName("accepts explicit eventsPerSnapshot")
        void fromMap_explicitSnapshot_setsValue() {
            Map<String, Object> snapshotPolicy =
                    Map.of("events_per_snapshot", 50);
            Map<String, Object> map = new HashMap<>();
            map.put("style", "cqrs");
            map.put("snapshot_policy", snapshotPolicy);

            var result = ArchitectureConfig.fromMap(map);

            assertThat(result.eventsPerSnapshot())
                    .isEqualTo(50);
        }
    }
}
