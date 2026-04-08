package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
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
 * Tests for PlanTemplatesAssembler -- copies 15 planning
 * and review templates to .claude/templates/ and
 * .github/templates/.
 *
 * <p>TPP order: degenerate (empty source) -> constant
 * (single template) -> collection (all 15) -> conditional
 * (section validation) -> error (missing template).</p>
 */
@DisplayName("PlanTemplatesAssembler")
class PlanTemplatesAssemblerTest {

    private static final List<String> ALL_TEMPLATE_NAMES =
            List.of(
                    "_TEMPLATE-IMPLEMENTATION-PLAN.md",
                    "_TEMPLATE-TEST-PLAN.md",
                    "_TEMPLATE-ARCHITECTURE-PLAN.md",
                    "_TEMPLATE-TASK-BREAKDOWN.md",
                    "_TEMPLATE-SECURITY-ASSESSMENT.md",
                    "_TEMPLATE-COMPLIANCE-ASSESSMENT.md",
                    "_TEMPLATE-SPECIALIST-REVIEW.md",
                    "_TEMPLATE-TECH-LEAD-REVIEW.md",
                    "_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md",
                    "_TEMPLATE-REVIEW-REMEDIATION.md",
                    "_TEMPLATE-EPIC-EXECUTION-PLAN.md",
                    "_TEMPLATE-PHASE-COMPLETION-REPORT.md",
                    "_TEMPLATE-TASK-PLAN.md",
                    "_TEMPLATE-STORY-PLANNING-REPORT.md",
                    "_TEMPLATE-DOR-CHECKLIST.md");

    @Nested
    @DisplayName("implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void implements_whenCalled_isAssemblerInstance() {
            Path dummyDir = Path.of("dummy");
            assertThat(
                    new PlanTemplatesAssembler(dummyDir))
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("constants validation")
    class ConstantsValidation {

        @Test
        @DisplayName("TEMPLATE_COUNT equals 15")
        void templateCount_equals15() {
            assertThat(
                    PlanTemplatesAssembler.TEMPLATE_COUNT)
                    .isEqualTo(15);
        }

        @Test
        @DisplayName("TEMPLATE_SECTIONS has exactly 15"
                + " entries")
        void templateSections_has15Entries() {
            assertThat(
                    PlanTemplatesAssembler
                            .TEMPLATE_SECTIONS)
                    .hasSize(15);
        }

        @Test
        @DisplayName("TEMPLATE_SECTIONS keys match"
                + " ALL_TEMPLATE_NAMES")
        void templateSections_keysMatchNames() {
            assertThat(
                    PlanTemplatesAssembler
                            .TEMPLATE_SECTIONS.keySet())
                    .containsExactlyInAnyOrderElementsOf(
                            ALL_TEMPLATE_NAMES);
        }

        @Test
        @DisplayName("each template has at least one"
                + " mandatory section")
        void templateSections_eachHasSections() {
            for (var entry : PlanTemplatesAssembler
                    .TEMPLATE_SECTIONS.entrySet()) {
                assertThat(entry.getValue())
                        .as("Sections for %s",
                                entry.getKey())
                        .isNotEmpty();
            }
        }
    }

    @Nested
    @DisplayName("assemble -- degenerate cases")
    class DegenerateCases {

        @Test
        @DisplayName(
                "empty source produces empty result")
        void assemble_emptySource_returnsEmpty(
                @TempDir Path tempDir) throws IOException {
            Path resourcesDir =
                    tempDir.resolve("empty-res");
            Files.createDirectories(resourcesDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PlanTemplatesAssembler assembler =
                    new PlanTemplatesAssembler(
                            resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            AssemblerResult result =
                    assembler.assembleWithResult(
                            config,
                            new TemplateEngine(),
                            outputDir);

            assertThat(result.files()).isEmpty();
            assertThat(result.warnings()).isEmpty();
        }
    }

    @Nested
    @DisplayName("assemble -- happy path")
    class HappyPath {

        @Test
        @DisplayName("copies 15 templates to both targets"
                + " producing 30 files")
        void assemble_allValid_copies30Files(
                @TempDir Path tempDir) throws IOException {
            Path resourcesDir =
                    setupAllTemplates(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PlanTemplatesAssembler assembler =
                    new PlanTemplatesAssembler(
                            resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            AssemblerResult result =
                    assembler.assembleWithResult(
                            config,
                            new TemplateEngine(),
                            outputDir);

            assertThat(result.files()).hasSize(30);
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("all 15 templates exist in"
                + " .claude/templates/")
        void assemble_allValid_existsInClaude(
                @TempDir Path tempDir) throws IOException {
            Path resourcesDir =
                    setupAllTemplates(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PlanTemplatesAssembler assembler =
                    new PlanTemplatesAssembler(
                            resourcesDir);
            assembler.assembleWithResult(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(),
                    outputDir);

            for (String name : ALL_TEMPLATE_NAMES) {
                assertThat(outputDir.resolve(
                        ".claude/templates/" + name))
                        .exists();
            }
        }

        @Test
        @DisplayName("all 15 templates exist in"
                + " .github/templates/")
        void assemble_allValid_existsInGithub(
                @TempDir Path tempDir) throws IOException {
            Path resourcesDir =
                    setupAllTemplates(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PlanTemplatesAssembler assembler =
                    new PlanTemplatesAssembler(
                            resourcesDir);
            assembler.assembleWithResult(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(),
                    outputDir);

            for (String name : ALL_TEMPLATE_NAMES) {
                assertThat(outputDir.resolve(
                        ".github/templates/" + name))
                        .exists();
            }
        }

        @Test
        @DisplayName("preserves {{LANGUAGE}} and"
                + " {{FRAMEWORK}} placeholders verbatim")
        void assemble_content_preservesPlaceholders(
                @TempDir Path tempDir) throws IOException {
            Path resourcesDir =
                    setupAllTemplates(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PlanTemplatesAssembler assembler =
                    new PlanTemplatesAssembler(
                            resourcesDir);
            assembler.assembleWithResult(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(),
                    outputDir);

            String content = Files.readString(
                    outputDir.resolve(
                            ".claude/templates/"
                                    + "_TEMPLATE-IMPLEMENTATION"
                                    + "-PLAN.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("{{LANGUAGE}}")
                    .contains("{{FRAMEWORK}}");
        }

        @Test
        @DisplayName("claude and github copies are"
                + " identical")
        void assemble_content_bothCopiesIdentical(
                @TempDir Path tempDir) throws IOException {
            Path resourcesDir =
                    setupAllTemplates(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PlanTemplatesAssembler assembler =
                    new PlanTemplatesAssembler(
                            resourcesDir);
            assembler.assembleWithResult(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(),
                    outputDir);

            for (String name : ALL_TEMPLATE_NAMES) {
                String claude = Files.readString(
                        outputDir.resolve(
                                ".claude/templates/"
                                        + name),
                        StandardCharsets.UTF_8);
                String github = Files.readString(
                        outputDir.resolve(
                                ".github/templates/"
                                        + name),
                        StandardCharsets.UTF_8);
                assertThat(claude)
                        .as("Both copies of %s must"
                                + " match", name)
                        .isEqualTo(github);
            }
        }

        @Test
        @DisplayName("output is byte-for-byte identical"
                + " to source template")
        void assemble_content_byteForByteMatchesSource(
                @TempDir Path tempDir) throws IOException {
            Path resourcesDir =
                    setupAllTemplates(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PlanTemplatesAssembler assembler =
                    new PlanTemplatesAssembler(
                            resourcesDir);
            assembler.assembleWithResult(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(),
                    outputDir);

            for (String name : ALL_TEMPLATE_NAMES) {
                byte[] source = Files.readAllBytes(
                        resourcesDir
                                .resolve("shared/templates")
                                .resolve(name));
                byte[] claudeOut = Files.readAllBytes(
                        outputDir.resolve(
                                ".claude/templates/"
                                        + name));
                byte[] githubOut = Files.readAllBytes(
                        outputDir.resolve(
                                ".github/templates/"
                                        + name));

                assertThat(claudeOut)
                        .as(".claude copy of %s must"
                                + " be byte-for-byte"
                                + " identical to source",
                                name)
                        .isEqualTo(source);
                assertThat(githubOut)
                        .as(".github copy of %s must"
                                + " be byte-for-byte"
                                + " identical to source",
                                name)
                        .isEqualTo(source);
            }
        }

        @Test
        @DisplayName("output preserves UTF-8 encoding")
        void assemble_content_preservesUtf8(
                @TempDir Path tempDir) throws IOException {
            Path resourcesDir =
                    setupAllTemplates(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PlanTemplatesAssembler assembler =
                    new PlanTemplatesAssembler(
                            resourcesDir);
            assembler.assembleWithResult(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(),
                    outputDir);

            for (String name : ALL_TEMPLATE_NAMES) {
                byte[] bytes = Files.readAllBytes(
                        outputDir.resolve(
                                ".claude/templates/"
                                        + name));
                String content = new String(
                        bytes, StandardCharsets.UTF_8);
                byte[] roundTrip = content.getBytes(
                        StandardCharsets.UTF_8);
                assertThat(bytes)
                        .as("UTF-8 round-trip for %s",
                                name)
                        .isEqualTo(roundTrip);
            }
        }
    }

    @Nested
    @DisplayName("assemble -- section validation")
    class SectionValidation {

        @Test
        @DisplayName("template with missing mandatory"
                + " section is skipped with warning")
        void assemble_missingSections_skipsWithWarning(
                @TempDir Path tempDir) throws IOException {
            Path resourcesDir =
                    setupAllTemplates(tempDir);

            Path incomplete = resourcesDir
                    .resolve("shared/templates")
                    .resolve(
                            "_TEMPLATE-TEST-PLAN.md");
            Files.writeString(incomplete,
                    "# Test Plan\n\n## Header\n\n"
                            + "## Summary\n\n"
                            + "Only two sections.\n",
                    StandardCharsets.UTF_8);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PlanTemplatesAssembler assembler =
                    new PlanTemplatesAssembler(
                            resourcesDir);
            AssemblerResult result =
                    assembler.assembleWithResult(
                            TestConfigBuilder.minimal(),
                            new TemplateEngine(),
                            outputDir);

            assertThat(result.files()).hasSize(28);
            assertThat(result.warnings())
                    .anyMatch(w -> w.contains(
                            "_TEMPLATE-TEST-PLAN.md"))
                    .anyMatch(w -> w.contains(
                            "Missing mandatory section"));

            assertThat(outputDir.resolve(
                    ".claude/templates/"
                            + "_TEMPLATE-TEST-PLAN.md"))
                    .doesNotExist();
            assertThat(outputDir.resolve(
                    ".github/templates/"
                            + "_TEMPLATE-TEST-PLAN.md"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("valid templates still copied when"
                + " one is invalid")
        void assemble_oneInvalid_othersCopied(
                @TempDir Path tempDir) throws IOException {
            Path resourcesDir =
                    setupAllTemplates(tempDir);

            Path incomplete = resourcesDir
                    .resolve("shared/templates")
                    .resolve(
                            "_TEMPLATE-TASK-BREAKDOWN.md");
            Files.writeString(incomplete,
                    "# Task Breakdown\n\nNo sections.",
                    StandardCharsets.UTF_8);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PlanTemplatesAssembler assembler =
                    new PlanTemplatesAssembler(
                            resourcesDir);
            AssemblerResult result =
                    assembler.assembleWithResult(
                            TestConfigBuilder.minimal(),
                            new TemplateEngine(),
                            outputDir);

            assertThat(result.files()).hasSize(28);

            assertThat(outputDir.resolve(
                    ".claude/templates/"
                            + "_TEMPLATE-IMPLEMENTATION"
                            + "-PLAN.md"))
                    .exists();
        }
    }

    @Nested
    @DisplayName("assemble -- template not found")
    class TemplateNotFound {

        @Test
        @DisplayName("missing template generates warning"
                + " without exception")
        void assemble_missingTemplate_warnsAndContinues(
                @TempDir Path tempDir) throws IOException {
            Path resourcesDir =
                    setupAllTemplates(tempDir);

            Files.delete(resourcesDir
                    .resolve("shared/templates")
                    .resolve(
                            "_TEMPLATE-ARCHITECTURE"
                                    + "-PLAN.md"));

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PlanTemplatesAssembler assembler =
                    new PlanTemplatesAssembler(
                            resourcesDir);
            AssemblerResult result =
                    assembler.assembleWithResult(
                            TestConfigBuilder.minimal(),
                            new TemplateEngine(),
                            outputDir);

            assertThat(result.files()).hasSize(28);
            assertThat(result.warnings())
                    .anyMatch(w -> w.contains(
                            "Template not found"))
                    .anyMatch(w -> w.contains(
                            "_TEMPLATE-ARCHITECTURE"
                                    + "-PLAN.md"));
        }
    }

    @Nested
    @DisplayName("pipeline position")
    class PipelinePosition {

        @Test
        @DisplayName("registered after"
                + " EpicReportAssembler in factory")
        void factory_position_afterEpicReport() {
            List<AssemblerDescriptor> descriptors =
                    AssemblerFactory.buildAssemblers();
            List<String> names = descriptors.stream()
                    .map(AssemblerDescriptor::name)
                    .toList();

            int epicIdx = names.indexOf(
                    "EpicReportAssembler");
            int planIdx = names.indexOf(
                    "PlanTemplatesAssembler");

            assertThat(epicIdx)
                    .as("EpicReportAssembler must"
                            + " exist")
                    .isGreaterThanOrEqualTo(0);
            assertThat(planIdx)
                    .as("PlanTemplatesAssembler must"
                            + " exist")
                    .isGreaterThanOrEqualTo(0);
            assertThat(planIdx)
                    .as("PlanTemplatesAssembler must be"
                            + " immediately after"
                            + " EpicReportAssembler")
                    .isEqualTo(epicIdx + 1);
        }
    }

    @Nested
    @DisplayName("assembleWithResult -- structured"
            + " result")
    class StructuredResult {

        @Test
        @DisplayName("returns AssemblerResult with files"
                + " and empty warnings on success")
        void assembleWithResult_success_returnsResult(
                @TempDir Path tempDir) throws IOException {
            Path resourcesDir =
                    setupAllTemplates(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PlanTemplatesAssembler assembler =
                    new PlanTemplatesAssembler(
                            resourcesDir);
            AssemblerResult result =
                    assembler.assembleWithResult(
                            TestConfigBuilder.minimal(),
                            new TemplateEngine(),
                            outputDir);

            assertThat(result).isNotNull();
            assertThat(result.files()).hasSize(30);
            assertThat(result.warnings()).isEmpty();
        }
    }

    @Nested
    @DisplayName("assemble -- file list interface")
    class AssembleFileList {

        @Test
        @DisplayName("assemble returns 30 file paths")
        void assemble_allValid_returns30Paths(
                @TempDir Path tempDir) throws IOException {
            Path resourcesDir =
                    setupAllTemplates(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PlanTemplatesAssembler assembler =
                    new PlanTemplatesAssembler(
                            resourcesDir);
            List<String> files = assembler.assemble(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(),
                    outputDir);

            assertThat(files).hasSize(30);
        }

        @Test
        @DisplayName("assemble returns empty list when"
                + " no templates exist")
        void assemble_noTemplates_returnsEmpty(
                @TempDir Path tempDir) throws IOException {
            Path resourcesDir =
                    tempDir.resolve("empty-res");
            Files.createDirectories(resourcesDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PlanTemplatesAssembler assembler =
                    new PlanTemplatesAssembler(
                            resourcesDir);
            List<String> files = assembler.assemble(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(),
                    outputDir);

            assertThat(files).isEmpty();
        }
    }

    // -- Helpers ------------------------------------------------

    /**
     * Creates all 15 templates with valid mandatory
     * sections in {tempDir}/res/shared/templates/.
     */
    private static Path setupAllTemplates(Path tempDir)
            throws IOException {
        Path resourcesDir = tempDir.resolve("res");
        Path templateDir =
                resourcesDir.resolve("shared/templates");
        Files.createDirectories(templateDir);

        writeTemplate(templateDir,
                "_TEMPLATE-IMPLEMENTATION-PLAN.md",
                buildContent(
                        "Implementation Plan",
                        List.of("Header",
                                "Executive Summary",
                                "Affected Layers"
                                        + " and Components",
                                "New Classes/Interfaces",
                                "Existing Classes"
                                        + " to Modify",
                                "Class Diagram",
                                "Method Signatures",
                                "TDD Strategy"),
                        true));

        writeTemplate(templateDir,
                "_TEMPLATE-TEST-PLAN.md",
                buildContent(
                        "Test Plan",
                        List.of("Header",
                                "Summary",
                                "Acceptance Tests"
                                        + " (Outer Loop)",
                                "Unit Tests (Inner"
                                        + " Loop - TPP Order)",
                                "Integration Tests",
                                "Coverage Estimation"
                                        + " Table",
                                "Risks and Gaps"),
                        false));

        writeTemplate(templateDir,
                "_TEMPLATE-ARCHITECTURE-PLAN.md",
                buildContent(
                        "Architecture Plan",
                        List.of("Header",
                                "Executive Summary",
                                "Component Diagram",
                                "Sequence Diagrams",
                                "Deployment Diagram",
                                "External Connections",
                                "Architecture Decisions",
                                "Technology Stack",
                                "Non-Functional"
                                        + " Requirements",
                                "Data Model",
                                "Observability Strategy",
                                "Resilience Strategy",
                                "Impact Analysis"),
                        false));

        writeTemplate(templateDir,
                "_TEMPLATE-TASK-BREAKDOWN.md",
                buildContent(
                        "Task Breakdown",
                        List.of("Header",
                                "Summary",
                                "Dependency Graph",
                                "Tasks Table",
                                "Escalation Notes"),
                        false));

        writeTemplate(templateDir,
                "_TEMPLATE-SECURITY-ASSESSMENT.md",
                buildContent(
                        "Security Assessment",
                        List.of("Data Classification",
                                "Encryption Requirements",
                                "Authentication"
                                        + " & Authorization",
                                "Input Validation",
                                "Audit Logging"
                                        + " Requirements",
                                "OWASP Top 10 Assessment",
                                "Dependency Security",
                                "Regulatory"
                                        + " Considerations",
                                "Risk Matrix"),
                        false));

        writeTemplate(templateDir,
                "_TEMPLATE-COMPLIANCE-ASSESSMENT.md",
                buildContent(
                        "Compliance Assessment",
                        List.of("Data Classification"
                                        + " Impact",
                                "Framework-Specific"
                                        + " Assessment",
                                "Personal Data"
                                        + " Processing",
                                "Audit Trail"
                                        + " Requirements",
                                "Cross-Border"
                                        + " Considerations",
                                "Remediation Actions",
                                "Sign-off"),
                        false));

        writeTemplate(templateDir,
                "_TEMPLATE-SPECIALIST-REVIEW.md",
                buildContent(
                        "Specialist Review",
                        List.of("Review Scope",
                                "Score Summary",
                                "Passed Items",
                                "Failed Items",
                                "Partial Items",
                                "Severity Summary",
                                "Recommendations"),
                        false));

        writeTemplate(templateDir,
                "_TEMPLATE-TECH-LEAD-REVIEW.md",
                buildContent(
                        "Tech Lead Review",
                        List.of("Decision",
                                "Section Scores",
                                "Cross-File Consistency",
                                "Critical Issues",
                                "Medium Issues",
                                "Low Issues",
                                "TDD Compliance"
                                        + " Assessment",
                                "Specialist Review"
                                        + " Validation",
                                "Verdict"),
                        false));

        writeTemplate(templateDir,
                "_TEMPLATE-CONSOLIDATED-REVIEW"
                        + "-DASHBOARD.md",
                buildContent(
                        "Review Dashboard",
                        List.of("Overall Score",
                                "Engineer Scores Table",
                                "Tech Lead Score",
                                "Critical Issues Summary",
                                "Severity Distribution",
                                "Remediation Status",
                                "Review History"),
                        false));

        writeTemplate(templateDir,
                "_TEMPLATE-REVIEW-REMEDIATION.md",
                buildContent(
                        "Review Remediation",
                        List.of("Findings Tracker",
                                "Remediation Summary",
                                "Deferred"
                                        + " Justifications",
                                "Re-review Results"),
                        false));

        writeTemplate(templateDir,
                "_TEMPLATE-EPIC-EXECUTION-PLAN.md",
                buildContent(
                        "Epic Execution Plan",
                        List.of("Execution Strategy",
                                "Phase Timeline",
                                "Story Execution Order",
                                "Pre-flight Analysis"
                                        + " Summary",
                                "Resource Requirements",
                                "Risk Assessment",
                                "Checkpoint Strategy"),
                        false));

        writeTemplate(templateDir,
                "_TEMPLATE-PHASE-COMPLETION-REPORT.md",
                buildContent(
                        "Phase Completion Report",
                        List.of("Stories Completed",
                                "Integrity Gate Results",
                                "Findings Summary",
                                "TDD Compliance",
                                "Coverage Delta",
                                "Blockers Encountered",
                                "Next Phase Readiness"),
                        false));

        writeTemplate(templateDir,
                "_TEMPLATE-TASK-PLAN.md",
                buildContent(
                        "Task Plan",
                        List.of("Header",
                                "Objective",
                                "Implementation Guide",
                                "Definition of Done",
                                "Dependencies",
                                "Estimated Effort",
                                "Risks"),
                        false));

        writeTemplate(templateDir,
                "_TEMPLATE-STORY-PLANNING-REPORT.md",
                buildContent(
                        "Story Planning Report",
                        List.of("Header",
                                "Planning Summary",
                                "Architecture Assessment",
                                "Test Strategy Summary",
                                "Security Assessment"
                                        + " Summary",
                                "Implementation Approach",
                                "Task Breakdown Summary",
                                "Consolidated Risk"
                                        + " Matrix",
                                "DoR Status"),
                        false));

        writeTemplate(templateDir,
                "_TEMPLATE-DOR-CHECKLIST.md",
                buildContent(
                        "DoR Checklist",
                        List.of("Header",
                                "Architecture Readiness",
                                "Test Readiness",
                                "Security Readiness",
                                "Implementation"
                                        + " Readiness",
                                "Task Decomposition"
                                        + " Readiness",
                                "Blockers and Open"
                                        + " Questions",
                                "Final Verdict"),
                        false));

        return resourcesDir;
    }

    private static String buildContent(
            String title,
            List<String> sections,
            boolean withPlaceholders) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(title).append("\n\n");
        for (String section : sections) {
            sb.append("## ").append(section)
                    .append("\n\n")
                    .append("Content for ")
                    .append(section).append(".\n\n");
        }
        if (withPlaceholders) {
            sb.append("Language: {{LANGUAGE}}\n");
            sb.append("Framework: {{FRAMEWORK}}\n");
        }
        return sb.toString();
    }

    private static void writeTemplate(
            Path dir,
            String filename,
            String content) throws IOException {
        Files.writeString(
                dir.resolve(filename),
                content,
                StandardCharsets.UTF_8);
    }
}
