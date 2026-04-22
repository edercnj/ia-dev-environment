package dev.iadev.cli;

import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for compliance multi-select mapping and summary
 * display with new fields.
 *
 * <p>Covers GK-4, GK-5 from story-0017-0010.</p>
 */
@DisplayName("InteractivePrompter — compliance")
class InteractivePrompterComplianceTest {

    @Nested
    @DisplayName("GK-4: compliance multi-select mapping")
    class ComplianceMultiSelect {

        @Test
        @DisplayName("pci-dss and lgpd maps correctly")
        void prompt_pciDssLgpd_mapsToSecurityConfig() {
            var mock = new MockTerminalProvider()
                    .addReadLine("comp-svc")
                    .addReadLine(
                            "A service for compliance "
                                    + "testing")
                    .addSelect("microservice")
                    .addSelect("quarkus")
                    .addSelect("maven")
                    .addMultiSelect(List.of("rest"))
                    .addReadLine("")
                    .addReadLine("")
                    .addSelect("layered")
                    .addMultiSelect(
                            List.of("pci-dss", "lgpd"))
                    .addConfirm(true);
            var prompter =
                    new InteractivePrompter(mock);
            ProjectConfig config = prompter.prompt();
            assertThat(
                    config.security().frameworks())
                    .containsExactly(
                            "pci-dss", "lgpd");
        }

        @Test
        @DisplayName("none maps to empty list")
        void prompt_complianceNone_mapsToEmpty() {
            var mock = new MockTerminalProvider()
                    .addReadLine("comp-svc")
                    .addReadLine(
                            "A service for compliance "
                                    + "testing")
                    .addSelect("microservice")
                    .addSelect("quarkus")
                    .addSelect("maven")
                    .addMultiSelect(List.of("rest"))
                    .addReadLine("")
                    .addReadLine("")
                    .addSelect("layered")
                    .addMultiSelect(List.of("none"))
                    .addConfirm(true);
            var prompter =
                    new InteractivePrompter(mock);
            ProjectConfig config = prompter.prompt();
            assertThat(
                    config.security().frameworks())
                    .isEmpty();
        }

        @Test
        @DisplayName("all compliance frameworks selected")
        void prompt_allCompliance_mapsAll() {
            var mock = new MockTerminalProvider()
                    .addReadLine("comp-svc")
                    .addReadLine(
                            "A service for full "
                                    + "compliance")
                    .addSelect("microservice")
                    .addSelect("quarkus")
                    .addSelect("maven")
                    .addMultiSelect(List.of("rest"))
                    .addReadLine("")
                    .addReadLine("")
                    .addSelect("layered")
                    .addMultiSelect(List.of(
                            "pci-dss", "lgpd",
                            "sox", "hipaa"))
                    .addConfirm(true);
            var prompter =
                    new InteractivePrompter(mock);
            ProjectConfig config = prompter.prompt();
            assertThat(
                    config.security().frameworks())
                    .containsExactly(
                            "pci-dss", "lgpd",
                            "sox", "hipaa");
        }
    }

    @Nested
    @DisplayName("GK-5: none + other is error")
    class ComplianceNoneConflict {

        @Test
        @DisplayName("none + pci-dss re-prompts")
        void resolveCompliance_noneAndOther_error() {
            var prompter = new InteractivePrompter(
                    new MockTerminalProvider()
                            .addMultiSelect(
                                    List.of("pci-dss")));
            List<String> result =
                    prompter.resolveCompliance(
                            List.of("none", "pci-dss"));
            assertThat(result)
                    .containsExactly("pci-dss");
        }

        @Test
        @DisplayName("error message displayed")
        void resolveCompliance_displaysError() {
            var mock = new MockTerminalProvider()
                    .addMultiSelect(
                            List.of("pci-dss"));
            var prompter =
                    new InteractivePrompter(mock);
            prompter.resolveCompliance(
                    List.of("none", "pci-dss"));
            assertThat(mock.getDisplayedMessages())
                    .contains(InteractivePrompter
                            .COMPLIANCE_NONE_ERROR);
        }
    }

    @Nested
    @DisplayName("Summary display with new fields")
    class SummaryWithNewFields {

        @Test
        @DisplayName("summary shows arch pattern")
        void prompt_archPattern_displayedInSummary() {
            var mock = new MockTerminalProvider()
                    .addReadLine("hex-svc")
                    .addReadLine(
                            "A service for summary "
                                    + "testing")
                    .addSelect("microservice")
                    .addSelect("quarkus")
                    .addSelect("maven")
                    .addMultiSelect(List.of("rest"))
                    .addReadLine("")
                    .addReadLine("")
                    .addSelect("hexagonal")
                    .addConfirm(true)
                    .addMultiSelect(
                            List.of("pci-dss", "lgpd"))
                    .addConfirm(true);
            var prompter =
                    new InteractivePrompter(mock);
            prompter.prompt();
            String summary = mock
                    .getDisplayedMessages()
                    .getFirst();
            assertThat(summary)
                    .contains("Arch Pattern:  hexagonal");
            assertThat(summary)
                    .contains("ArchUnit:      yes");
            assertThat(summary)
                    .contains(
                            "Compliance:    pci-dss, lgpd");
        }

        // prompt_goLanguage_noArchPatternInSummary removed in
        // EPIC-0048 full cleanup — go no longer selectable.
    }
}
