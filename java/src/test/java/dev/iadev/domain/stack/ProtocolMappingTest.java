package dev.iadev.domain.stack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProtocolMapping")
class ProtocolMappingTest {

    @Nested
    @DisplayName("Constants")
    class ConstantsTests {

        @Test
        @DisplayName("INTERFACE_PROTOCOL_MAP has 7 entries")
        void interfaceProtocolMap_sevenEntries() {
            assertThat(ProtocolMapping.INTERFACE_PROTOCOL_MAP).hasSize(7);
        }

        @Test
        @DisplayName("rest maps to [rest]")
        void interfaceProtocolMap_rest() {
            assertThat(ProtocolMapping.INTERFACE_PROTOCOL_MAP.get("rest"))
                    .containsExactly("rest");
        }

        @Test
        @DisplayName("event-consumer maps to [event-driven, messaging]")
        void interfaceProtocolMap_eventConsumer() {
            assertThat(ProtocolMapping.INTERFACE_PROTOCOL_MAP
                    .get("event-consumer"))
                    .containsExactly("event-driven", "messaging");
        }

        @Test
        @DisplayName("cli maps to empty list")
        void interfaceProtocolMap_cli() {
            assertThat(ProtocolMapping.INTERFACE_PROTOCOL_MAP.get("cli"))
                    .isEmpty();
        }

        @Test
        @DisplayName("EVENT_PREFIX is event-")
        void eventPrefix() {
            assertThat(ProtocolMapping.EVENT_PREFIX).isEqualTo("event-");
        }

        @Test
        @DisplayName("EVENT_DRIVEN_PROTOCOL is event-driven")
        void eventDrivenProtocol() {
            assertThat(ProtocolMapping.EVENT_DRIVEN_PROTOCOL)
                    .isEqualTo("event-driven");
        }
    }

    @Nested
    @DisplayName("deriveProtocols()")
    class DeriveProtocolsTests {

        @Test
        @DisplayName("rest interface produces [rest]")
        void deriveProtocols_rest_restOnly() {
            var config = new TestProjectConfigBuilder()
                    .clearInterfaces()
                    .addInterface("rest", "", "")
                    .build();

            var result = ProtocolMapping.deriveProtocols(config);

            assertThat(result).containsExactly("rest");
        }

        @Test
        @DisplayName("grpc interface produces [grpc]")
        void deriveProtocols_grpc_grpcOnly() {
            var config = new TestProjectConfigBuilder()
                    .clearInterfaces()
                    .addInterface("grpc", "", "")
                    .build();

            var result = ProtocolMapping.deriveProtocols(config);

            assertThat(result).containsExactly("grpc");
        }

        @Test
        @DisplayName("event-consumer produces [event-driven, messaging]")
        void deriveProtocols_eventConsumer_eventDrivenMessaging() {
            var config = new TestProjectConfigBuilder()
                    .clearInterfaces()
                    .addInterface("event-consumer", "", "kafka")
                    .build();

            var result = ProtocolMapping.deriveProtocols(config);

            assertThat(result)
                    .containsExactly("event-driven", "messaging");
        }

        @Test
        @DisplayName("multiple interfaces deduplicated and sorted")
        void deriveProtocols_multiple_deduplicatedSorted() {
            var config = new TestProjectConfigBuilder()
                    .clearInterfaces()
                    .addInterface("rest", "", "")
                    .addInterface("grpc", "", "")
                    .addInterface("event-consumer", "", "kafka")
                    .addInterface("event-producer", "", "kafka")
                    .build();

            var result = ProtocolMapping.deriveProtocols(config);

            assertThat(result).doesNotHaveDuplicates();
            assertThat(result).isSorted();
            assertThat(result).contains(
                    "event-driven", "grpc", "messaging", "rest");
        }

        @Test
        @DisplayName("cli produces empty list")
        void deriveProtocols_cli_empty() {
            var config = new TestProjectConfigBuilder()
                    .clearInterfaces()
                    .addInterface("cli", "", "")
                    .build();

            var result = ProtocolMapping.deriveProtocols(config);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("unknown event- prefix falls back to event-driven")
        void deriveProtocols_unknownEventPrefix_eventDriven() {
            var config = new TestProjectConfigBuilder()
                    .clearInterfaces()
                    .addInterface("event-custom", "", "")
                    .build();

            var result = ProtocolMapping.deriveProtocols(config);

            assertThat(result).containsExactly("event-driven");
        }

        @Test
        @DisplayName("completely unknown type produces no protocol")
        void deriveProtocols_unknown_empty() {
            var config = new TestProjectConfigBuilder()
                    .clearInterfaces()
                    .addInterface("scheduled", "", "")
                    .build();

            var result = ProtocolMapping.deriveProtocols(config);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("extractBroker()")
    class ExtractBrokerTests {

        @Test
        @DisplayName("returns broker from first interface with broker")
        void extractBroker_kafkaBroker_kafka() {
            var config = new TestProjectConfigBuilder()
                    .clearInterfaces()
                    .addInterface("event-consumer", "", "kafka")
                    .build();

            assertThat(ProtocolMapping.extractBroker(config))
                    .isEqualTo("kafka");
        }

        @Test
        @DisplayName("returns empty when no interface has broker")
        void extractBroker_noBroker_empty() {
            var config = new TestProjectConfigBuilder()
                    .clearInterfaces()
                    .addInterface("rest", "", "")
                    .build();

            assertThat(ProtocolMapping.extractBroker(config)).isEmpty();
        }

        @Test
        @DisplayName("returns first broker when multiple exist")
        void extractBroker_multipleBrokers_returnsFirst() {
            var config = new TestProjectConfigBuilder()
                    .clearInterfaces()
                    .addInterface("event-consumer", "", "kafka")
                    .addInterface("event-producer", "", "rabbitmq")
                    .build();

            assertThat(ProtocolMapping.extractBroker(config))
                    .isEqualTo("kafka");
        }
    }
}
