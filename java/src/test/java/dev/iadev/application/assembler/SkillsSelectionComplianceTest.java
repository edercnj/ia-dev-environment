package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SkillsSelection — compliance skill inclusion:
 * x-review-compliance when pci-dss is in security frameworks.
 */
@DisplayName("SkillsSelection — compliance")
class SkillsSelectionComplianceTest {

    @Nested
    @DisplayName("selectComplianceSkills")
    class SelectComplianceSkills {

        @Test
        @DisplayName("config with pci-dss includes"
                + " x-review-compliance")
        void select_pciDss_includesCompliance() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("pci-dss")
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectComplianceSkills(config);

            assertThat(skills)
                    .contains("x-review-compliance");
        }

        @Test
        @DisplayName("config with pci-dss and lgpd includes"
                + " x-review-compliance")
        void select_pciDssAndLgpd_includesCompliance() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks(
                                    "pci-dss", "lgpd")
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectComplianceSkills(config);

            assertThat(skills)
                    .contains("x-review-compliance");
        }

        @Test
        @DisplayName("config without pci-dss returns empty")
        void select_noPciDss_returnsEmpty() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("lgpd")
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectComplianceSkills(config);

            assertThat(skills).isEmpty();
        }

        @Test
        @DisplayName("config with no frameworks returns empty")
        void select_noFrameworks_returnsEmpty() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectComplianceSkills(config);

            assertThat(skills).isEmpty();
        }
    }

    @Nested
    @DisplayName("selectConditionalSkills includes"
            + " compliance")
    class ConditionalIncludesCompliance {

        @Test
        @DisplayName("aggregation includes"
                + " x-review-compliance for pci-dss")
        void conditional_pciDss_includesCompliance() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .securityFrameworks("pci-dss")
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectConditionalSkills(config);

            assertThat(skills)
                    .contains("x-review-compliance")
                    .contains("x-review-security");
        }

        @Test
        @DisplayName("aggregation excludes"
                + " x-review-compliance without pci-dss")
        void conditional_noPciDss_excludesCompliance() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .securityFrameworks("lgpd")
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectConditionalSkills(config);

            assertThat(skills)
                    .doesNotContain("x-review-compliance")
                    .contains("x-review-security");
        }
    }
}
