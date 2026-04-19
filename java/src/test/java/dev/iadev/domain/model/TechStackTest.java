package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TechStack")
class TechStackTest {

    @Nested
    @DisplayName("compact constructor")
    class CompactConstructor {

        @Test
        @DisplayName("builds with all five optional sections")
        void ctor_allSections_allSet() {
            DataConfig data = DataConfig.fromMap(Map.of());
            InfraConfig infra = InfraConfig.fromMap(Map.of());
            SecurityConfig security =
                    SecurityConfig.fromMap(Map.of());
            TestingConfig testing =
                    TestingConfig.fromMap(Map.of());
            McpConfig mcp = McpConfig.fromMap(Map.of());

            TechStack stack = new TechStack(
                    data, infra, security, testing, mcp);

            assertThat(stack.data()).isSameAs(data);
            assertThat(stack.infrastructure()).isSameAs(infra);
            assertThat(stack.security()).isSameAs(security);
            assertThat(stack.testing()).isSameAs(testing);
            assertThat(stack.mcp()).isSameAs(mcp);
        }
    }

    @Nested
    @DisplayName("fromMap()")
    class FromMap {

        @Test
        @DisplayName("returns defaults when all sections absent")
        void fromMap_emptyRoot_allDefaults() {
            TechStack stack = TechStack.fromMap(Map.of());

            assertThat(stack.data()).isNotNull();
            assertThat(stack.infrastructure()).isNotNull();
            assertThat(stack.security()).isNotNull();
            assertThat(stack.testing()).isNotNull();
            assertThat(stack.mcp()).isNotNull();
        }

        @Test
        @DisplayName("parses each optional section when present")
        void fromMap_allSectionsPresent_populated() {
            Map<String, Object> root = Map.of(
                    "data", Map.of(
                            "database", Map.of(
                                    "name", "postgresql",
                                    "version", "15")),
                    "infrastructure", Map.of(
                            "container", "docker"),
                    "security", Map.of(
                            "compliance", "none"),
                    "testing", Map.of(
                            "line-coverage", 95,
                            "branch-coverage", 90),
                    "mcp", Map.of("servers", java.util.List.of()));

            TechStack stack = TechStack.fromMap(root);

            assertThat(stack.data().database().name())
                    .isEqualTo("postgresql");
            assertThat(stack.infrastructure().container())
                    .isEqualTo("docker");
        }

        @Test
        @DisplayName("defaults testing to 95/90 coverage when absent")
        void fromMap_missingTesting_defaultCoverage() {
            TechStack stack = TechStack.fromMap(Map.of());

            assertThat(stack.testing().coverageLine())
                    .isEqualTo(95);
            assertThat(stack.testing().coverageBranch())
                    .isEqualTo(90);
        }

        @Test
        @DisplayName("defaults mcp to empty server list when absent")
        void fromMap_missingMcp_emptyServers() {
            TechStack stack = TechStack.fromMap(Map.of());

            assertThat(stack.mcp().servers()).isEmpty();
        }
    }
}
