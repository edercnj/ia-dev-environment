package dev.iadev.application.assembler;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Declarative catalog of planning and review templates
 * shipped with the generator.
 *
 * <p>Extracted from {@link PlanTemplatesAssembler} to keep
 * both classes under the 250-line Rule-03 limit. The
 * insertion-ordered map lists every template by filename
 * and the mandatory section headings that must be present
 * before it is copied to {@code .claude/templates/}
 * (Rule-10 validation).</p>
 *
 * <p>Adding a template: append a new {@code map.put(...)}
 * entry in {@link #buildTemplateSections()} and bump
 * {@link #TEMPLATE_COUNT}. The assembler picks it up
 * automatically.</p>
 *
 * @see PlanTemplatesAssembler
 */
final class PlanTemplateDefinitions {

    /** Number of templates currently managed. */
    static final int TEMPLATE_COUNT = 15;

    /**
     * Template definitions: filename to mandatory sections
     * mapping. {@link LinkedHashMap} preserves insertion
     * order for deterministic processing.
     */
    static final Map<String, List<String>>
            TEMPLATE_SECTIONS = buildTemplateSections();

    private PlanTemplateDefinitions() {
        // utility class
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

        map.put("_TEMPLATE-TASK-PLAN.md",
                List.of(
                        "Header",
                        "Objective",
                        "Implementation Guide",
                        "Definition of Done",
                        "Dependencies",
                        "Estimated Effort",
                        "Risks"));

        map.put("_TEMPLATE-STORY-PLANNING-REPORT.md",
                List.of(
                        "Header",
                        "Planning Summary",
                        "Architecture Assessment",
                        "Test Strategy Summary",
                        "Security Assessment Summary",
                        "Implementation Approach",
                        "Task Breakdown Summary",
                        "Consolidated Risk Matrix",
                        "DoR Status"));

        map.put("_TEMPLATE-DOR-CHECKLIST.md",
                List.of(
                        "Header",
                        "Architecture Readiness",
                        "Test Readiness",
                        "Security Readiness",
                        "Implementation Readiness",
                        "Task Decomposition Readiness",
                        "Blockers and Open Questions",
                        "Final Verdict"));

        return Collections.unmodifiableMap(map);
    }
}
