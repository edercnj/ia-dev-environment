package dev.iadev.config;

import dev.iadev.domain.model.ArchitectureConfig;
import dev.iadev.domain.model.DataConfig;
import dev.iadev.domain.model.FrameworkConfig;
import dev.iadev.domain.model.InfraConfig;
import dev.iadev.domain.model.InterfaceConfig;
import dev.iadev.domain.model.LanguageConfig;
import dev.iadev.domain.model.McpConfig;
import dev.iadev.domain.model.ObservabilityConfig;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.domain.model.ProjectIdentity;
import dev.iadev.domain.model.SecurityConfig;
import dev.iadev.domain.model.TestingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ContextBuilder — conditional checklist fields")
class ContextBuilderConditionalChecklistTest {

    @Nested
    @DisplayName("has_event_interface")
    class HasEventInterface {

        @Test
        @DisplayName("false when no event interfaces")
        void buildContext_noEvent_hasFalse() {
            var config = configWithInterfaces(
                    List.of(iface("rest", "")));

            Map<String, Object> ctx =
                    ContextBuilder.buildContext(config);

            assertThat(ctx.get("has_event_interface"))
                    .isEqualTo("False");
        }

        @Test
        @DisplayName("true when event-consumer present")
        void buildContext_eventConsumer_hasTrue() {
            var config = configWithInterfaces(
                    List.of(iface("event-consumer", "kafka")));

            Map<String, Object> ctx =
                    ContextBuilder.buildContext(config);

            assertThat(ctx.get("has_event_interface"))
                    .isEqualTo("True");
        }

        @Test
        @DisplayName("true when event-producer present")
        void buildContext_eventProducer_hasTrue() {
            var config = configWithInterfaces(
                    List.of(iface("event-producer", "kafka")));

            Map<String, Object> ctx =
                    ContextBuilder.buildContext(config);

            assertThat(ctx.get("has_event_interface"))
                    .isEqualTo("True");
        }
    }

    @Nested
    @DisplayName("has_pci_dss")
    class HasPciDss {

        @Test
        @DisplayName("false when no compliance frameworks")
        void buildContext_noCompliance_hasFalse() {
            var config = configWithSecurity(List.of());

            Map<String, Object> ctx =
                    ContextBuilder.buildContext(config);

            assertThat(ctx.get("has_pci_dss"))
                    .isEqualTo("False");
        }

        @Test
        @DisplayName("true when pci-dss in frameworks")
        void buildContext_pciDss_hasTrue() {
            var config = configWithSecurity(
                    List.of("pci-dss"));

            Map<String, Object> ctx =
                    ContextBuilder.buildContext(config);

            assertThat(ctx.get("has_pci_dss"))
                    .isEqualTo("True");
        }
    }

    @Nested
    @DisplayName("has_lgpd")
    class HasLgpd {

        @Test
        @DisplayName("false when no compliance frameworks")
        void buildContext_noCompliance_hasFalse() {
            var config = configWithSecurity(List.of());

            Map<String, Object> ctx =
                    ContextBuilder.buildContext(config);

            assertThat(ctx.get("has_lgpd"))
                    .isEqualTo("False");
        }

        @Test
        @DisplayName("true when lgpd in frameworks")
        void buildContext_lgpd_hasTrue() {
            var config = configWithSecurity(
                    List.of("lgpd"));

            Map<String, Object> ctx =
                    ContextBuilder.buildContext(config);

            assertThat(ctx.get("has_lgpd"))
                    .isEqualTo("True");
        }
    }

    @Nested
    @DisplayName("review_max_score and review_go_threshold")
    class ReviewScoreFields {

        @Test
        @DisplayName("base score 45 and threshold 38")
        void buildContext_noConditionals_baseScore() {
            var config = configWithInterfaces(
                    List.of(iface("rest", "")));

            Map<String, Object> ctx =
                    ContextBuilder.buildContext(config);

            assertThat(ctx.get("review_max_score"))
                    .isEqualTo(45);
            assertThat(ctx.get("review_go_threshold"))
                    .isEqualTo(38);
        }

        @Test
        @DisplayName("event + pci-dss gives 60 max, 51 threshold")
        void buildContext_eventPciDss_computedScore() {
            var config = configWithAll(
                    List.of(iface("event-consumer", "kafka")),
                    List.of("pci-dss"));

            Map<String, Object> ctx =
                    ContextBuilder.buildContext(config);

            assertThat(ctx.get("review_max_score"))
                    .isEqualTo(60);
            assertThat(ctx.get("review_go_threshold"))
                    .isEqualTo(51);
        }

        @Test
        @DisplayName("all conditionals gives 64 max, 54 threshold")
        void buildContext_allConditionals_fullScore() {
            var config = configWithAll(
                    List.of(iface("event-producer", "kafka")),
                    List.of("pci-dss", "lgpd"));

            Map<String, Object> ctx =
                    ContextBuilder.buildContext(config);

            assertThat(ctx.get("review_max_score"))
                    .isEqualTo(64);
            assertThat(ctx.get("review_go_threshold"))
                    .isEqualTo(54);
        }
    }

    private static InterfaceConfig iface(
            String type, String broker) {
        return new InterfaceConfig(type, "", broker);
    }

    private static ProjectConfig configWithInterfaces(
            List<InterfaceConfig> interfaces) {
        return configWithAll(interfaces, List.of());
    }

    private static ProjectConfig configWithSecurity(
            List<String> frameworks) {
        return configWithAll(
                List.of(iface("rest", "")), frameworks);
    }

    private static ProjectConfig configWithAll(
            List<InterfaceConfig> interfaces,
            List<String> frameworks) {
        return new ProjectConfig(
                new ProjectIdentity("test", "test"),
                new ArchitectureConfig(
                        "microservice", true, false,
                        false, "",
                        new ArchitectureConfig.CqrsConfig(
                                "eventstoredb", 100,
                                "", false, ""),
                        false),
                interfaces,
                new LanguageConfig("java", "21"),
                new FrameworkConfig(
                        "spring-boot", "3.x",
                        "maven", false),
                DataConfig.fromMap(Map.of()),
                InfraConfig.fromMap(Map.of()),
                new SecurityConfig(
                        frameworks,
                        SecurityConfig.ScanningConfig
                                .defaults(),
                        SecurityConfig.QualityGateConfig
                                .defaults(),
                        false, "local"),
                TestingConfig.fromMap(Map.of()),
                McpConfig.fromMap(Map.of()),
                "none");
    }
}
