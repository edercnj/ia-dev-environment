package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ConditionEvaluator — feature gate evaluation.
 */
@DisplayName("ConditionEvaluator")
class ConditionEvaluatorTest {

    @Nested
    @DisplayName("hasInterface")
    class HasInterface {

        @Test
        @DisplayName("returns true for existing interface")
        void existingInterface_whenCalled_returnsTrue() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("rest")
                    .addInterface("grpc")
                    .build();

            assertThat(ConditionEvaluator
                    .hasInterface(config, "rest"))
                    .isTrue();
        }

        @Test
        @DisplayName("returns false for missing interface")
        void missingInterface_forMissingInterface_returnsFalse() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("rest")
                    .build();

            assertThat(ConditionEvaluator
                    .hasInterface(config, "graphql"))
                    .isFalse();
        }

        @Test
        @DisplayName("returns false for empty interfaces")
        void emptyInterfaces_forEmptyInterfaces_returnsFalse() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .build();

            assertThat(ConditionEvaluator
                    .hasInterface(config, "rest"))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("hasAnyInterface")
    class HasAnyInterface {

        @Test
        @DisplayName("returns true when at least one matches")
        void oneMatches_whenCalled_returnsTrue() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("rest")
                    .build();

            assertThat(ConditionEvaluator
                    .hasAnyInterface(config, "grpc", "rest"))
                    .isTrue();
        }

        @Test
        @DisplayName("returns false when none matches")
        void noneMatches_whenNoneMatches_returnsFalse() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("cli")
                    .build();

            assertThat(ConditionEvaluator
                    .hasAnyInterface(config, "rest", "grpc"))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("extractInterfaceTypes")
    class ExtractInterfaceTypes {

        @Test
        @DisplayName("returns list of type strings")
        void multipleInterfaces_whenCalled_returnsList() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("rest")
                    .addInterface("grpc")
                    .addInterface("cli")
                    .build();

            List<String> types = ConditionEvaluator
                    .extractInterfaceTypes(config);

            assertThat(types)
                    .containsExactly("rest", "grpc", "cli");
        }

        @Test
        @DisplayName("returns empty list for no interfaces")
        void noInterfaces_forNoInterfaces_returnsEmpty() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .build();

            assertThat(ConditionEvaluator
                    .extractInterfaceTypes(config))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("hasDatabase")
    class HasDatabase {

        @Test
        @DisplayName("returns true when database is configured")
        void databaseConfigured_whenCalled_returnsTrue() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .database("postgresql", "16")
                    .build();

            assertThat(ConditionEvaluator.hasDatabase(config))
                    .isTrue();
        }

        @Test
        @DisplayName("returns false when database is none")
        void databaseNone_whenDatabase_returnsFalse() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .database("none", "")
                    .build();

            assertThat(ConditionEvaluator.hasDatabase(config))
                    .isFalse();
        }

        @Test
        @DisplayName("returns false when database is empty")
        void databaseEmpty_whenDatabase_returnsFalse() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .database("", "")
                    .build();

            assertThat(ConditionEvaluator.hasDatabase(config))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("hasCache")
    class HasCache {

        @Test
        @DisplayName("returns true when cache is configured")
        void cacheConfigured_whenCalled_returnsTrue() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .cache("redis", "7")
                    .build();

            assertThat(ConditionEvaluator.hasCache(config))
                    .isTrue();
        }

        @Test
        @DisplayName("returns false when cache is none")
        void cacheNone_whenCache_returnsFalse() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .cache("none", "")
                    .build();

            assertThat(ConditionEvaluator.hasCache(config))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("hasFeature")
    class HasFeature {

        @Test
        @DisplayName("domain_driven returns true when enabled")
        void domainDriven_whenCalled_returnsTrue() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .domainDriven(true)
                    .build();

            assertThat(ConditionEvaluator
                    .hasFeature(config, "domain_driven"))
                    .isTrue();
        }

        @Test
        @DisplayName("domain_driven returns false when disabled")
        void domainDriven_whenDisabled_returnsFalse() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .domainDriven(false)
                    .build();

            assertThat(ConditionEvaluator
                    .hasFeature(config, "domain_driven"))
                    .isFalse();
        }

        @Test
        @DisplayName("event_driven returns true when enabled")
        void eventDriven_whenEnabled_returnsTrue() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .eventDriven(true)
                    .build();

            assertThat(ConditionEvaluator
                    .hasFeature(config, "event_driven"))
                    .isTrue();
        }

        @Test
        @DisplayName("native_build returns true when enabled")
        void nativeBuild_whenEnabled_returnsTrue() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .nativeBuild(true)
                    .build();

            assertThat(ConditionEvaluator
                    .hasFeature(config, "native_build"))
                    .isTrue();
        }

        @Test
        @DisplayName("database returns true when configured")
        void database_whenConfigured_returnsTrue() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .database("postgresql", "16")
                    .build();

            assertThat(ConditionEvaluator
                    .hasFeature(config, "database"))
                    .isTrue();
        }

        @Test
        @DisplayName("cache returns true when configured")
        void cache_whenConfigured_returnsTrue() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .cache("redis", "7")
                    .build();

            assertThat(ConditionEvaluator
                    .hasFeature(config, "cache"))
                    .isTrue();
        }

        @Test
        @DisplayName("unknown feature returns false")
        void unknownFeature_whenCalled_returnsFalse() {
            ProjectConfig config = TestConfigBuilder.minimal();

            assertThat(ConditionEvaluator
                    .hasFeature(config, "unknown_feature"))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("evaluate")
    class Evaluate {

        @Test
        @DisplayName("evaluates interface condition")
        void evaluate_whenCalled_interfaceCondition() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("grpc")
                    .build();

            assertThat(ConditionEvaluator
                    .evaluate(config, "interface:grpc"))
                    .isTrue();
            assertThat(ConditionEvaluator
                    .evaluate(config, "interface:rest"))
                    .isFalse();
        }

        @Test
        @DisplayName("evaluates feature condition")
        void evaluate_whenCalled_featureCondition() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .domainDriven(true)
                    .build();

            assertThat(ConditionEvaluator
                    .evaluate(config, "feature:domain_driven"))
                    .isTrue();
        }

        @Test
        @DisplayName("evaluates plain feature name")
        void evaluate_whenCalled_plainFeatureName() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .database("postgresql", "16")
                    .build();

            assertThat(ConditionEvaluator
                    .evaluate(config, "database"))
                    .isTrue();
        }
    }
}
