package dev.iadev.assembler;

import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Observability KP extension with SLO/SLI
 * Framework, Error Budget Management, and Alerting
 * Strategy sections.
 */
@DisplayName("Observability KP — SLO/SLI Extension")
class ObservabilitySloExtensionTest {

    @Nested
    @DisplayName("SLO/SLI Framework section")
    class SloSliFramework {

        @Test
        @DisplayName("contains SLO/SLI Framework section")
        void assemble_observability_containsSloSliFramework(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateObservability(tempDir);
            assertThat(content)
                    .contains("## SLO/SLI Framework");
        }

        @Test
        @DisplayName("covers SLI types request-based"
                + " and window-based")
        void assemble_observability_coversSliTypes(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateObservability(tempDir);
            assertThat(content)
                    .contains("Request-based");
            assertThat(content)
                    .contains("Window-based");
        }

        @Test
        @DisplayName("covers measurement methods"
                + " server-side, client-side, synthetic")
        void assemble_observability_coversMeasurementMethods(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateObservability(tempDir);
            assertThat(content)
                    .contains("Server-side");
            assertThat(content)
                    .contains("Client-side");
            assertThat(content)
                    .contains("Synthetic");
        }

        @Test
        @DisplayName("covers window types rolling"
                + " and calendar")
        void assemble_observability_coversWindowTypes(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateObservability(tempDir);
            assertThat(content).contains("Rolling");
            assertThat(content).contains("Calendar");
        }

        @Test
        @DisplayName("contains SLI specification pattern")
        void assemble_observability_containsSliSpec(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateObservability(tempDir);
            assertThat(content)
                    .contains("SLI Specification Pattern");
            assertThat(content)
                    .contains("good events");
            assertThat(content)
                    .contains("valid events");
        }
    }

    @Nested
    @DisplayName("Error Budget Management section")
    class ErrorBudgetManagement {

        @Test
        @DisplayName("contains Error Budget Management"
                + " section")
        void assemble_observability_containsErrorBudget(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateObservability(tempDir);
            assertThat(content)
                    .contains("## Error Budget Management");
        }

        @Test
        @DisplayName("contains error budget calculation"
                + " formula")
        void assemble_observability_containsFormula(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateObservability(tempDir);
            assertThat(content)
                    .contains("Error Budget = "
                            + "(1 - SLO Target) x Time Window");
        }

        @Test
        @DisplayName("contains burn rate alerts with"
                + " fast and slow burn")
        void assemble_observability_containsBurnRates(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateObservability(tempDir);
            assertThat(content)
                    .contains("Fast Burn");
            assertThat(content)
                    .contains("Slow Burn");
            assertThat(content)
                    .contains("14.4x");
            assertThat(content)
                    .contains("6x");
        }

        @Test
        @DisplayName("contains exhaustion policy"
                + " escalation ladder")
        void assemble_observability_containsEscalation(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateObservability(tempDir);
            assertThat(content)
                    .contains("Exhaustion Policy");
            assertThat(content)
                    .contains("50%");
            assertThat(content)
                    .contains("75%");
            assertThat(content)
                    .contains("100%");
        }

        @Test
        @DisplayName("contains budget allocation"
                + " between teams")
        void assemble_observability_containsBudgetAlloc(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateObservability(tempDir);
            assertThat(content)
                    .contains("Budget Allocation Between"
                            + " Teams");
        }
    }

    @Nested
    @DisplayName("Alerting Strategy section")
    class AlertingStrategy {

        @Test
        @DisplayName("contains Alerting Strategy section")
        void assemble_observability_containsAlertStrategy(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateObservability(tempDir);
            assertThat(content)
                    .contains("## Alerting Strategy");
        }

        @Test
        @DisplayName("contains alert routing by severity"
                + " P1-P4")
        void assemble_observability_containsSeverityRouting(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateObservability(tempDir);
            assertThat(content).contains("P1");
            assertThat(content).contains("P2");
            assertThat(content).contains("P3");
            assertThat(content).contains("P4");
            assertThat(content).contains("PagerDuty");
        }

        @Test
        @DisplayName("contains PagerDuty/OpsGenie"
                + " integration patterns")
        void assemble_observability_containsPagerDuty(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateObservability(tempDir);
            assertThat(content)
                    .contains("PagerDuty/OpsGenie"
                            + " Integration Patterns");
        }

        @Test
        @DisplayName("contains on-call rotation alerting")
        void assemble_observability_containsOnCall(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateObservability(tempDir);
            assertThat(content)
                    .contains("On-Call Rotation Alerting");
        }

        @Test
        @DisplayName("contains alert fatigue prevention"
                + " strategies")
        void assemble_observability_containsFatiguePrev(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateObservability(tempDir);
            assertThat(content)
                    .contains("Alert Fatigue Prevention");
            assertThat(content)
                    .contains("Deduplication");
            assertThat(content)
                    .contains("Grouping");
            assertThat(content)
                    .contains("Silencing");
        }
    }

    @Nested
    @DisplayName("alerting-patterns reference file")
    class AlertingPatternsRef {

        @Test
        @DisplayName("alerting-patterns.md file exists"
                + " in observability references")
        void assemble_observability_alertPatternsExists(
                @TempDir Path tempDir)
                throws IOException {
            generateObservability(tempDir);
            Path refs = tempDir.resolve("output/skills"
                    + "/observability/references");
            assertThat(refs.resolve(
                    "alerting-patterns.md")).exists();
        }

        @Test
        @DisplayName("alerting-patterns contains"
                + " symptom-based alerting pattern")
        void assemble_observability_symptomBased(
                @TempDir Path tempDir)
                throws IOException {
            generateObservability(tempDir);
            Path ref = tempDir.resolve("output/skills"
                    + "/observability/references"
                    + "/alerting-patterns.md");
            String content = Files.readString(
                    ref, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Symptom-Based Alerting");
        }

        @Test
        @DisplayName("alerting-patterns contains"
                + " golden signals alerting pattern")
        void assemble_observability_goldenSignals(
                @TempDir Path tempDir)
                throws IOException {
            generateObservability(tempDir);
            Path ref = tempDir.resolve("output/skills"
                    + "/observability/references"
                    + "/alerting-patterns.md");
            String content = Files.readString(
                    ref, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Golden Signals Alerting");
        }

        @Test
        @DisplayName("alerting-patterns contains"
                + " multi-window multi-burn-rate pattern")
        void assemble_observability_multiWindow(
                @TempDir Path tempDir)
                throws IOException {
            generateObservability(tempDir);
            Path ref = tempDir.resolve("output/skills"
                    + "/observability/references"
                    + "/alerting-patterns.md");
            String content = Files.readString(
                    ref, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Multi-Window Multi-Burn-Rate");
        }

        @Test
        @DisplayName("alerting-patterns contains"
                + " anti-patterns section")
        void assemble_observability_antiPatterns(
                @TempDir Path tempDir)
                throws IOException {
            generateObservability(tempDir);
            Path ref = tempDir.resolve("output/skills"
                    + "/observability/references"
                    + "/alerting-patterns.md");
            String content = Files.readString(
                    ref, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Alert on Every Metric");
            assertThat(content)
                    .contains("Threshold-Only Alerting");
            assertThat(content)
                    .contains("Missing Runbook Links");
        }
    }

    @Nested
    @DisplayName("backward compatibility (RULE-010)")
    class BackwardCompatibility {

        @Test
        @DisplayName("existing sections remain unchanged")
        void assemble_observability_existingSectionsIntact(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateObservability(tempDir);
            assertThat(content)
                    .contains("# Knowledge Pack:"
                            + " Observability");
            assertThat(content)
                    .contains("## Purpose");
            assertThat(content)
                    .contains("## Quick Reference");
            assertThat(content)
                    .contains("## Detailed References");
        }

        @Test
        @DisplayName("existing reference table rows"
                + " preserved")
        void assemble_observability_existingRefsPreserved(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateObservability(tempDir);
            assertThat(content)
                    .contains("distributed-tracing.md");
            assertThat(content)
                    .contains("metrics-collection.md");
            assertThat(content)
                    .contains("structured-logging.md");
            assertThat(content)
                    .contains("health-checks.md");
            assertThat(content)
                    .contains("correlation-ids.md");
            assertThat(content)
                    .contains("opentelemetry-setup.md");
        }

        @Test
        @DisplayName("new sections appear after existing"
                + " sections")
        void assemble_observability_newSectionsAfterExisting(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateObservability(tempDir);
            int detailedRefsIdx =
                    content.indexOf("## Detailed References");
            int sloFrameworkIdx =
                    content.indexOf("## SLO/SLI Framework");
            int errorBudgetIdx =
                    content.indexOf(
                            "## Error Budget Management");
            int alertingIdx =
                    content.indexOf("## Alerting Strategy");

            assertThat(detailedRefsIdx)
                    .isGreaterThan(-1);
            assertThat(sloFrameworkIdx)
                    .isGreaterThan(detailedRefsIdx);
            assertThat(errorBudgetIdx)
                    .isGreaterThan(sloFrameworkIdx);
            assertThat(alertingIdx)
                    .isGreaterThan(errorBudgetIdx);
        }

        @Test
        @DisplayName("frontmatter not modified")
        void assemble_observability_frontmatterIntact(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateObservability(tempDir);
            assertThat(content)
                    .contains("name: observability");
            assertThat(content)
                    .contains("distributed tracing");
        }

        @Test
        @DisplayName("alerting-patterns referenced in"
                + " Detailed References table")
        void assemble_observability_alertingInTable(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateObservability(tempDir);
            assertThat(content)
                    .contains(
                            "references/alerting-patterns.md");
        }
    }

    private static String generateObservability(
            Path tempDir) throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        SkillsAssembler assembler = new SkillsAssembler();
        assembler.assemble(
                TestConfigBuilder.minimal(),
                new TemplateEngine(), outputDir);
        return Files.readString(
                outputDir.resolve(
                        "skills/observability/SKILL.md"),
                StandardCharsets.UTF_8);
    }
}
