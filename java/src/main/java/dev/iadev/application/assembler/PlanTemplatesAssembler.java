package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Copies the 12 planning and review templates to both
 * {@code .claude/templates/} and {@code .github/templates/}
 * in the output directory.
 *
 * <p>Templates contain {@code {{PLACEHOLDER}}} tokens
 * intended for runtime resolution by the LLM, NOT for
 * build-time rendering. Content is copied verbatim
 * (RULE-003).</p>
 *
 * <p>Each template is validated for mandatory sections
 * before copying (RULE-010). Templates with missing
 * sections are skipped with a warning. Templates not
 * found on the classpath produce a warning without
 * throwing an exception.</p>
 *
 * <p>This assembler writes to both {@code .claude/} and
 * {@code .github/} targets per RULE-004 (dual-target
 * copy).</p>
 *
 * @see Assembler
 * @see EpicReportAssembler
 */
public final class PlanTemplatesAssembler
        implements Assembler {

    private static final String TEMPLATES_SUBDIR =
            "shared/templates";
    private static final String CLAUDE_OUTPUT_SUBDIR =
            ".claude/templates";
    private static final String GITHUB_OUTPUT_SUBDIR =
            ".github/templates";

    /** Number of templates this assembler manages. */
    static final int TEMPLATE_COUNT = 12;

    /**
     * Template definitions: filename to mandatory
     * sections mapping. LinkedHashMap preserves
     * insertion order for deterministic processing.
     */
    static final Map<String, List<String>>
            TEMPLATE_SECTIONS = buildTemplateSections();

    private final Path resourcesDir;

    /**
     * Creates a PlanTemplatesAssembler using classpath
     * resources.
     */
    public PlanTemplatesAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a PlanTemplatesAssembler with an explicit
     * resources directory.
     *
     * @param resourcesDir the base resources directory
     */
    public PlanTemplatesAssembler(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Copies validated templates verbatim to
     * {@code .claude/templates/} and
     * {@code .github/templates/}. Returns only paths
     * of successfully copied files.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        return assembleWithResult(
                config, engine, outputDir).files();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns a structured result with both copied
     * file paths and validation warnings.</p>
     */
    @Override
    public AssemblerResult assembleWithResult(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        Path templatesDir = resourcesDir
                .resolve(TEMPLATES_SUBDIR);
        if (!Files.isDirectory(templatesDir)) {
            return AssemblerResult.empty();
        }

        List<String> files = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (var entry
                : TEMPLATE_SECTIONS.entrySet()) {
            processTemplate(
                    entry.getKey(),
                    entry.getValue(),
                    outputDir,
                    files,
                    warnings);
        }

        return AssemblerResult.of(files, warnings);
    }

    private void processTemplate(
            String filename,
            List<String> mandatorySections,
            Path outputDir,
            List<String> files,
            List<String> warnings) {
        Path sourcePath = resourcesDir
                .resolve(TEMPLATES_SUBDIR)
                .resolve(filename);

        if (!Files.exists(sourcePath)) {
            warnings.add(
                    "Template not found: " + filename);
            return;
        }

        String content =
                CopyHelpers.readFile(sourcePath);

        if (!CopyHelpers.hasAllMandatorySections(
                content, mandatorySections)) {
            warnings.add(
                    "Missing mandatory section in "
                            + filename);
            return;
        }

        copyToTargets(
                filename, content, outputDir, files);
    }

    private void copyToTargets(
            String filename,
            String content,
            Path outputDir,
            List<String> files) {
        List<String> targets = List.of(
                CLAUDE_OUTPUT_SUBDIR,
                GITHUB_OUTPUT_SUBDIR);

        for (String target : targets) {
            Path destDir = outputDir.resolve(target);
            CopyHelpers.ensureDirectory(destDir);
            Path destPath = destDir.resolve(filename);
            CopyHelpers.writeFile(destPath, content);
            files.add(destPath.toString());
        }
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(
                        TEMPLATES_SUBDIR + "/"
                                + "_TEMPLATE-IMPLEMENTATION"
                                + "-PLAN.md",
                        3);
    }

    private static Map<String, List<String>>
            buildTemplateSections() {
        Map<String, List<String>> map =
                new LinkedHashMap<>();

        map.put("_TEMPLATE-IMPLEMENTATION-PLAN.md",
                List.of(
                        "Header",
                        "Executive Summary",
                        "Affected Layers and Components",
                        "New Classes/Interfaces",
                        "Existing Classes to Modify",
                        "Class Diagram",
                        "Method Signatures",
                        "TDD Strategy"));

        map.put("_TEMPLATE-TEST-PLAN.md",
                List.of(
                        "Header",
                        "Summary",
                        "Acceptance Tests (Outer Loop)",
                        "Unit Tests (Inner Loop"
                                + " - TPP Order)",
                        "Integration Tests",
                        "Coverage Estimation Table",
                        "Risks and Gaps"));

        map.put("_TEMPLATE-ARCHITECTURE-PLAN.md",
                List.of(
                        "Header",
                        "Executive Summary",
                        "Component Diagram",
                        "Sequence Diagrams",
                        "Deployment Diagram",
                        "External Connections",
                        "Architecture Decisions",
                        "Technology Stack",
                        "Non-Functional Requirements",
                        "Data Model",
                        "Observability Strategy",
                        "Resilience Strategy",
                        "Impact Analysis"));

        map.put("_TEMPLATE-TASK-BREAKDOWN.md",
                List.of(
                        "Header",
                        "Summary",
                        "Dependency Graph",
                        "Tasks Table",
                        "Escalation Notes"));

        map.put("_TEMPLATE-SECURITY-ASSESSMENT.md",
                List.of(
                        "Data Classification",
                        "Encryption Requirements",
                        "Authentication & Authorization",
                        "Input Validation",
                        "Audit Logging Requirements",
                        "OWASP Top 10 Assessment",
                        "Dependency Security",
                        "Regulatory Considerations",
                        "Risk Matrix"));

        map.put("_TEMPLATE-COMPLIANCE-ASSESSMENT.md",
                List.of(
                        "Data Classification Impact",
                        "Framework-Specific Assessment",
                        "Personal Data Processing",
                        "Audit Trail Requirements",
                        "Cross-Border Considerations",
                        "Remediation Actions",
                        "Sign-off"));

        map.put("_TEMPLATE-SPECIALIST-REVIEW.md",
                List.of(
                        "Review Scope",
                        "Score Summary",
                        "Passed Items",
                        "Failed Items",
                        "Partial Items",
                        "Severity Summary",
                        "Recommendations"));

        map.put("_TEMPLATE-TECH-LEAD-REVIEW.md",
                List.of(
                        "Decision",
                        "Section Scores",
                        "Cross-File Consistency",
                        "Critical Issues",
                        "Medium Issues",
                        "Low Issues",
                        "TDD Compliance Assessment",
                        "Specialist Review Validation",
                        "Verdict"));

        map.put("_TEMPLATE-CONSOLIDATED-REVIEW"
                        + "-DASHBOARD.md",
                List.of(
                        "Overall Score",
                        "Engineer Scores Table",
                        "Tech Lead Score",
                        "Critical Issues Summary",
                        "Severity Distribution",
                        "Remediation Status",
                        "Review History"));

        map.put("_TEMPLATE-REVIEW-REMEDIATION.md",
                List.of(
                        "Findings Tracker",
                        "Remediation Summary",
                        "Deferred Justifications",
                        "Re-review Results"));

        map.put("_TEMPLATE-EPIC-EXECUTION-PLAN.md",
                List.of(
                        "Execution Strategy",
                        "Phase Timeline",
                        "Story Execution Order",
                        "Pre-flight Analysis Summary",
                        "Resource Requirements",
                        "Risk Assessment",
                        "Checkpoint Strategy"));

        map.put("_TEMPLATE-PHASE-COMPLETION-REPORT.md",
                List.of(
                        "Stories Completed",
                        "Integrity Gate Results",
                        "Findings Summary",
                        "TDD Compliance",
                        "Coverage Delta",
                        "Blockers Encountered",
                        "Next Phase Readiness"));

        return Map.copyOf(map);
    }
}
