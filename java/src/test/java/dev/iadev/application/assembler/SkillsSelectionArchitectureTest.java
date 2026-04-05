package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SkillsSelection — knowledge pack selection
 * including architecture packs and conditional packs.
 */
@DisplayName("SkillsSelection — architecture + KP")
class SkillsSelectionArchitectureTest {

    @Nested
    @DisplayName("selectKnowledgePacks")
    class SelectKnowledgePacks {

        @Test
        @DisplayName("always returns core knowledge packs"
                + " plus layer-templates")
        void select_always_returnsCoreAndLayerTemplates() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .build();

            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);

            assertThat(packs)
                    .contains("coding-standards")
                    .contains("architecture")
                    .contains("testing")
                    .contains("security")
                    .contains("compliance")
                    .contains("api-design")
                    .contains("observability")
                    .contains("resilience")
                    .contains("infrastructure")
                    .contains("protocols")
                    .contains("story-planning")
                    .contains("ci-cd-patterns")
                    .contains("data-management")
                    .contains("performance-engineering")
                    .contains("layer-templates");
        }

        @Test
        @DisplayName("config with style cqrs includes"
                + " architecture-cqrs")
        void select_styleCqrs_includesCqrsPack() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .archStyle("cqrs")
                    .build();

            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);

            assertThat(packs)
                    .contains("architecture-cqrs");
        }

        @Test
        @DisplayName("config with style microservice"
                + " excludes architecture-cqrs")
        void select_styleMicroservice_excludesCqrsPack() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .archStyle("microservice")
                    .build();

            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);

            assertThat(packs)
                    .doesNotContain("architecture-cqrs");
        }

        @Test
        @DisplayName("config without style does not include"
                + " architecture-cqrs")
        void select_defaultStyle_excludesCqrsPack() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .build();

            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);

            assertThat(packs)
                    .doesNotContain("architecture-cqrs");
        }

        @Test
        @DisplayName("config with database includes"
                + " database-patterns")
        void select_database_includesDbPatterns() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .database("postgresql", "16")
                    .build();

            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);

            assertThat(packs)
                    .contains("database-patterns");
        }

        @Test
        @DisplayName("config with cache includes"
                + " database-patterns")
        void select_cache_includesDbPatterns() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .cache("redis", "7")
                    .build();

            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);

            assertThat(packs)
                    .contains("database-patterns");
        }

        @Test
        @DisplayName("config without database or cache"
                + " excludes database-patterns")
        void select_noDbNoCache_excludesDbPatterns() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .build();

            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);

            assertThat(packs)
                    .doesNotContain("database-patterns");
        }

        @Test
        @DisplayName("returns at least 15 packs for"
                + " minimal config")
        void select_minimalConfig_returnsAtLeast15Packs() {
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);

            assertThat(packs).hasSizeGreaterThanOrEqualTo(15);
        }

        @Test
        @DisplayName("config with hexagonal style includes"
                + " architecture-hexagonal KP")
        void select_hexagonal_includesHexKp() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .archStyle("hexagonal")
                            .basePackage("com.example")
                            .build();

            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);

            assertThat(packs)
                    .contains("architecture-hexagonal");
        }

        @Test
        @DisplayName("config with layered style excludes"
                + " architecture-hexagonal KP")
        void select_layered_excludesHexKp() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .archStyle("layered")
                            .build();

            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);

            assertThat(packs)
                    .doesNotContain(
                            "architecture-hexagonal");
        }

        @Test
        @DisplayName("config without style excludes"
                + " architecture-hexagonal KP")
        void select_noStyleDeclared_excludesHexKp() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .archStyle("microservice")
                            .build();

            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);

            assertThat(packs)
                    .doesNotContain(
                            "architecture-hexagonal");
        }
    }
}
