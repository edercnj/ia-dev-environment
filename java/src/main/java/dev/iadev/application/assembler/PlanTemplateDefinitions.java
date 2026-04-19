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
 * both classes under 250 lines per RULE-004. The
 * insertion-ordered map lists every template by filename
 * and the mandatory section headings that must be present
 * before it is copied to {@code .claude/templates/}
 * (RULE-010 validation).</p>
 *
 * <p>Adding a template: append a new entry to the matching
 * group constant below and bump {@link #TEMPLATE_COUNT}.
 * The assembler picks it up automatically.</p>
 *
 * @see PlanTemplatesAssembler
 */
final class PlanTemplateDefinitions {

    /** Number of templates currently managed. */
    static final int TEMPLATE_COUNT = 20;

    private static final List<Map.Entry<String, List<String>>>
            STORY_PLANNING_TEMPLATES = List.of(
                    Map.entry("_TEMPLATE-IMPLEMENTATION-PLAN.md", List.of(
                            "Header",
                            "Executive Summary",
                            "Affected Layers and Components",
                            "New Classes/Interfaces",
                            "Existing Classes to Modify",
                            "Class Diagram",
                            "Method Signatures",
                            "TDD Strategy")),
                    Map.entry("_TEMPLATE-TEST-PLAN.md", List.of(
                            "Header",
                            "Summary",
                            "Acceptance Tests (Outer Loop)",
                            "Unit Tests (Inner Loop - TPP Order)",
                            "Integration Tests",
                            "Coverage Estimation Table",
                            "Risks and Gaps")),
                    Map.entry("_TEMPLATE-ARCHITECTURE-PLAN.md", List.of(
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
                            "Impact Analysis")),
                    Map.entry("_TEMPLATE-TASK-BREAKDOWN.md", List.of(
                            "Header",
                            "Summary",
                            "Dependency Graph",
                            "Tasks Table",
                            "Escalation Notes")));

    private static final List<Map.Entry<String, List<String>>>
            ASSESSMENT_TEMPLATES = List.of(
                    Map.entry("_TEMPLATE-SECURITY-ASSESSMENT.md", List.of(
                            "Data Classification",
                            "Encryption Requirements",
                            "Authentication & Authorization",
                            "Input Validation",
                            "Audit Logging Requirements",
                            "OWASP Top 10 Assessment",
                            "Dependency Security",
                            "Regulatory Considerations",
                            "Risk Matrix")),
                    Map.entry("_TEMPLATE-COMPLIANCE-ASSESSMENT.md", List.of(
                            "Data Classification Impact",
                            "Framework-Specific Assessment",
                            "Personal Data Processing",
                            "Audit Trail Requirements",
                            "Cross-Border Considerations",
                            "Remediation Actions",
                            "Sign-off")));

    private static final List<Map.Entry<String, List<String>>>
            REVIEW_TEMPLATES = List.of(
                    Map.entry("_TEMPLATE-SPECIALIST-REVIEW.md", List.of(
                            "Review Scope",
                            "Score Summary",
                            "Passed Items",
                            "Failed Items",
                            "Partial Items",
                            "Severity Summary",
                            "Recommendations")),
                    Map.entry("_TEMPLATE-TECH-LEAD-REVIEW.md", List.of(
                            "Decision",
                            "Section Scores",
                            "Cross-File Consistency",
                            "Critical Issues",
                            "Medium Issues",
                            "Low Issues",
                            "TDD Compliance Assessment",
                            "Specialist Review Validation",
                            "Verdict")),
                    Map.entry("_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md", List.of(
                            "Overall Score",
                            "Engineer Scores Table",
                            "Tech Lead Score",
                            "Critical Issues Summary",
                            "Severity Distribution",
                            "Remediation Status",
                            "Review History")),
                    Map.entry("_TEMPLATE-REVIEW-REMEDIATION.md", List.of(
                            "Findings Tracker",
                            "Remediation Summary",
                            "Deferred Justifications",
                            "Re-review Results")));

    private static final List<Map.Entry<String, List<String>>>
            EPIC_EXECUTION_TEMPLATES = List.of(
                    Map.entry("_TEMPLATE-EPIC-EXECUTION-PLAN.md", List.of(
                            "Execution Strategy",
                            "Phase Timeline",
                            "Story Execution Order",
                            "Pre-flight Analysis Summary",
                            "Resource Requirements",
                            "Risk Assessment",
                            "Checkpoint Strategy")),
                    Map.entry("_TEMPLATE-PHASE-COMPLETION-REPORT.md", List.of(
                            "Stories Completed",
                            "Integrity Gate Results",
                            "Findings Summary",
                            "TDD Compliance",
                            "Coverage Delta",
                            "Blockers Encountered",
                            "Next Phase Readiness")));

    private static final List<Map.Entry<String, List<String>>>
            TASK_FIRST_TEMPLATES = List.of(
                    Map.entry("_TEMPLATE-TASK-PLAN.md", List.of(
                            "Header",
                            "Objective",
                            "Implementation Guide",
                            "Definition of Done",
                            "Dependencies",
                            "Estimated Effort",
                            "Risks")),
                    Map.entry("_TEMPLATE-STORY-PLANNING-REPORT.md", List.of(
                            "Header",
                            "Planning Summary",
                            "Architecture Assessment",
                            "Test Strategy Summary",
                            "Security Assessment Summary",
                            "Implementation Approach",
                            "Task Breakdown Summary",
                            "Consolidated Risk Matrix",
                            "DoR Status")),
                    Map.entry("_TEMPLATE-TASK.md", List.of(
                            "1. Objetivo",
                            "2. Contratos I/O",
                            "3. Definition of Done",
                            "4. Dependências",
                            "5. Plano de Implementação")),
                    Map.entry("_TEMPLATE-TASK-IMPLEMENTATION-MAP.md", List.of(
                            "Dependency Graph",
                            "Execution Order",
                            "Coalesced Groups",
                            "Parallelism Analysis")),
                    Map.entry("_TEMPLATE-DOR-CHECKLIST.md", List.of(
                            "Header",
                            "Architecture Readiness",
                            "Test Readiness",
                            "Security Readiness",
                            "Implementation Readiness",
                            "Task Decomposition Readiness",
                            "Blockers and Open Questions",
                            "Final Verdict")));

    private static final List<Map.Entry<String, List<String>>>
            EPIC_STORY_TEMPLATES = List.of(
                    Map.entry("_TEMPLATE-EPIC.md", List.of(
                            "1. Visão Geral",
                            "2. Anexos e Referências",
                            "3. Definições de Qualidade Globais",
                            "4. Regras de Negócio Transversais (Source of Truth)",
                            "5. Índice de Histórias")),
                    Map.entry("_TEMPLATE-STORY.md", List.of(
                            "1. Dependências",
                            "2. Regras Transversais Aplicáveis",
                            "3. Descrição",
                            "3.5 Entrega de Valor",
                            "4. Definições de Qualidade Locais",
                            "5. Contratos de Dados (Data Contract)",
                            "6. Diagramas",
                            "7. Critérios de Aceite (Gherkin)",
                            "8. Tasks")),
                    Map.entry("_TEMPLATE-IMPLEMENTATION-MAP.md", List.of(
                            "1. Matriz de Dependências",
                            "2. Fases de Implementação",
                            "3. Caminho Crítico",
                            "4. Grafo de Dependências (Mermaid)",
                            "5. Resumo por Fase",
                            "6. Detalhamento por Fase",
                            "7. Observações Estratégicas")));

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
        Map<String, List<String>> map = new LinkedHashMap<>();
        List<List<Map.Entry<String, List<String>>>> groups = List.of(
                STORY_PLANNING_TEMPLATES,
                ASSESSMENT_TEMPLATES,
                REVIEW_TEMPLATES,
                EPIC_EXECUTION_TEMPLATES,
                TASK_FIRST_TEMPLATES,
                EPIC_STORY_TEMPLATES);
        for (List<Map.Entry<String, List<String>>> group : groups) {
            for (Map.Entry<String, List<String>> e : group) {
                map.put(e.getKey(), e.getValue());
            }
        }
        return Collections.unmodifiableMap(map);
    }
}
