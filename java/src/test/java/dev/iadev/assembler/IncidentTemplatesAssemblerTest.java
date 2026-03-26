package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for IncidentTemplatesAssembler -- copies incident
 * response and postmortem templates to docs/templates/.
 */
@DisplayName("IncidentTemplatesAssembler")
class IncidentTemplatesAssemblerTest {

    @Nested
    @DisplayName("implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssembler() {
            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("assemble -- generates both templates")
    class AssembleTemplates {

        @Test
        @DisplayName("generates incident-response and"
                + " postmortem in docs/templates/")
        void assemble_whenCalled_generatesBothFiles(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(2);
            Path irPath = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-INCIDENT-RESPONSE.md");
            Path pmPath = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-POSTMORTEM.md");
            assertThat(irPath).exists();
            assertThat(pmPath).exists();
        }

        @Test
        @DisplayName("creates docs/templates/ subdirectory")
        void assemble_whenCalled_createsDocsTemplatesSubdir(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            assertThat(
                    outputDir.resolve("docs/templates"))
                    .exists()
                    .isDirectory();
        }

        @Test
        @DisplayName("returns both file paths in result list")
        void assemble_whenCalled_returnsBothFilePaths(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(2);
            assertThat(files.get(0)).endsWith(
                    "_TEMPLATE-INCIDENT-RESPONSE.md");
            assertThat(files.get(1)).endsWith(
                    "_TEMPLATE-POSTMORTEM.md");
        }
    }

    @Nested
    @DisplayName("incident response template content")
    class IncidentResponseContent {

        @Test
        @DisplayName("contains Severity Classification"
                + " section with SEV1-SEV4")
        void assemble_incidentResponse_containsSeverityClassification(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path irPath = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-INCIDENT-RESPONSE.md");
            String content = readFile(irPath);
            assertThat(content)
                    .contains("Severity Classification");
            assertThat(content).contains("SEV1");
            assertThat(content).contains("SEV2");
            assertThat(content).contains("SEV3");
            assertThat(content).contains("SEV4");
        }

        @Test
        @DisplayName("contains Detection & Triage section")
        void assemble_incidentResponse_containsDetectionTriage(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path irPath = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-INCIDENT-RESPONSE.md");
            String content = readFile(irPath);
            assertThat(content)
                    .contains("Detection & Triage");
        }

        @Test
        @DisplayName("contains Communication Plan section")
        void assemble_incidentResponse_containsCommunicationPlan(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path irPath = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-INCIDENT-RESPONSE.md");
            String content = readFile(irPath);
            assertThat(content)
                    .contains("Communication Plan");
        }

        @Test
        @DisplayName("contains Mitigation Steps section")
        void assemble_incidentResponse_containsMitigationSteps(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path irPath = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-INCIDENT-RESPONSE.md");
            String content = readFile(irPath);
            assertThat(content)
                    .contains("Mitigation Steps");
        }

        @Test
        @DisplayName("contains Escalation Matrix section"
                + " with response times")
        void assemble_incidentResponse_containsEscalationMatrix(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path irPath = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-INCIDENT-RESPONSE.md");
            String content = readFile(irPath);
            assertThat(content)
                    .contains("Escalation Matrix");
            assertThat(content)
                    .contains("Incident Commander");
            assertThat(content).contains("15 min");
        }

        @Test
        @DisplayName("contains Resolution Verification"
                + " section")
        void assemble_incidentResponse_containsResolutionVerification(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path irPath = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-INCIDENT-RESPONSE.md");
            String content = readFile(irPath);
            assertThat(content)
                    .contains("Resolution Verification");
        }

        @Test
        @DisplayName("contains Timeline Template section")
        void assemble_incidentResponse_containsTimelineTemplate(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path irPath = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-INCIDENT-RESPONSE.md");
            String content = readFile(irPath);
            assertThat(content)
                    .contains("Timeline Template");
        }

        @Test
        @DisplayName("has all 7 mandatory sections")
        void assemble_incidentResponse_hasAllMandatorySections(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path irPath = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-INCIDENT-RESPONSE.md");
            String content = readFile(irPath);
            assertThat(content)
                    .contains("# Incident Response Guide");
            for (String section
                    : IncidentTemplatesAssembler
                    .INCIDENT_RESPONSE_SECTIONS) {
                assertThat(content)
                        .as("Missing section: %s", section)
                        .contains(section);
            }
        }

        @Test
        @DisplayName("SEV1 indicates critical impact")
        void assemble_incidentResponse_sev1IndicatesCriticalImpact(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path irPath = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-INCIDENT-RESPONSE.md");
            String content = readFile(irPath);
            assertThat(content).containsIgnoringCase(
                    "critical");
        }

        @Test
        @DisplayName("Communication Plan defines 30 min"
                + " frequency for SEV1")
        void assemble_incidentResponse_commPlan30MinForSev1(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path irPath = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-INCIDENT-RESPONSE.md");
            String content = readFile(irPath);
            assertThat(content).contains("30 min");
        }
    }

    @Nested
    @DisplayName("postmortem template content")
    class PostmortemContent {

        @Test
        @DisplayName("contains Incident Summary section")
        void assemble_postmortem_containsIncidentSummary(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path pmPath = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-POSTMORTEM.md");
            String content = readFile(pmPath);
            assertThat(content)
                    .contains("Incident Summary");
        }

        @Test
        @DisplayName("contains Timeline section")
        void assemble_postmortem_containsTimeline(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path pmPath = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-POSTMORTEM.md");
            String content = readFile(pmPath);
            assertThat(content)
                    .contains("## Timeline");
        }

        @Test
        @DisplayName("contains Root Cause Analysis with"
                + " 5 Whys structure")
        void assemble_postmortem_containsRootCauseAnalysis(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path pmPath = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-POSTMORTEM.md");
            String content = readFile(pmPath);
            assertThat(content)
                    .contains("Root Cause Analysis");
            assertThat(content)
                    .contains("Why #1");
            assertThat(content)
                    .contains("Why #5");
        }

        @Test
        @DisplayName("contains Impact Assessment section")
        void assemble_postmortem_containsImpactAssessment(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path pmPath = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-POSTMORTEM.md");
            String content = readFile(pmPath);
            assertThat(content)
                    .contains("Impact Assessment");
        }

        @Test
        @DisplayName("contains Contributing Factors section")
        void assemble_postmortem_containsContributingFactors(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path pmPath = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-POSTMORTEM.md");
            String content = readFile(pmPath);
            assertThat(content)
                    .contains("Contributing Factors");
        }

        @Test
        @DisplayName("contains Action Items with table"
                + " columns")
        void assemble_postmortem_containsActionItemsTable(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path pmPath = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-POSTMORTEM.md");
            String content = readFile(pmPath);
            assertThat(content)
                    .contains("Action Items");
            assertThat(content).contains("Action");
            assertThat(content).contains("Owner");
            assertThat(content).contains("Deadline");
            assertThat(content).contains("Priority");
        }

        @Test
        @DisplayName("contains Lessons Learned section")
        void assemble_postmortem_containsLessonsLearned(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path pmPath = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-POSTMORTEM.md");
            String content = readFile(pmPath);
            assertThat(content)
                    .contains("Lessons Learned");
        }

        @Test
        @DisplayName("contains Prevention Measures section")
        void assemble_postmortem_containsPreventionMeasures(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path pmPath = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-POSTMORTEM.md");
            String content = readFile(pmPath);
            assertThat(content)
                    .contains("Prevention Measures");
        }

        @Test
        @DisplayName("has all 8 mandatory sections")
        void assemble_postmortem_hasAllMandatorySections(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path pmPath = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-POSTMORTEM.md");
            String content = readFile(pmPath);
            assertThat(content)
                    .contains("# Postmortem Report");
            for (String section
                    : IncidentTemplatesAssembler
                    .POSTMORTEM_SECTIONS) {
                assertThat(content)
                        .as("Missing section: %s", section)
                        .contains(section);
            }
        }

        @Test
        @DisplayName("Action Items has P0-P3 priorities")
        void assemble_postmortem_actionItemsHasPriorities(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path pmPath = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-POSTMORTEM.md");
            String content = readFile(pmPath);
            assertThat(content).contains("P0");
            assertThat(content).contains("P3");
        }
    }

    @Nested
    @DisplayName("assemble -- graceful no-op")
    class GracefulNoOp {

        @Test
        @DisplayName("returns empty list when template"
                + " files absent")
        void assemble_whenCalled_returnsEmptyWhenAbsent(
                @TempDir Path tempDir) {
            Path resourcesDir =
                    tempDir.resolve("nonexistent");
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler(
                            resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("does not create output directory"
                + " when templates absent")
        void assemble_whenCalled_doesNotCreateOutputDir(
                @TempDir Path tempDir) {
            Path resourcesDir =
                    tempDir.resolve("nonexistent");
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler(
                            resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            assertThat(outputDir).doesNotExist();
        }
    }

    @Nested
    @DisplayName("assemble -- unconditional generation")
    class UnconditionalGeneration {

        @Test
        @DisplayName("generates for minimal config")
        void assemble_minimalConfig_generatesBothTemplates(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(2);
        }

        @Test
        @DisplayName("generates for java-spring profile")
        void assemble_javaSpring_generatesBothTemplates(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("spring-boot", "3.2")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(2);
        }

        @Test
        @DisplayName("generates for python-fastapi profile")
        void assemble_pythonFastapi_generatesBothTemplates(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("python", "3.12")
                            .framework("fastapi", "0.110")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(2);
        }

        @Test
        @DisplayName("generates for go-gin profile")
        void assemble_goGin_generatesBothTemplates(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            IncidentTemplatesAssembler assembler =
                    new IncidentTemplatesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("go", "1.22")
                            .framework("gin", "1.9")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(2);
        }
    }

    private static String readFile(Path path) {
        try {
            return Files.readString(
                    path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to read: " + path, e);
        }
    }
}
