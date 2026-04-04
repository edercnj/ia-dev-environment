package dev.iadev.config;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.domain.model.ReviewChecklistScore;
import dev.iadev.domain.model.ReviewChecklistSections;

import java.util.Map;

/**
 * Builds architecture, interface, and review checklist
 * sections of the template context map.
 *
 * <p>Extracted from {@link ContextBuilder} to keep both
 * classes under 250 lines per RULE-004.</p>
 *
 * @see ContextBuilder
 */
final class ContextArchitectureBuilder {

    private ContextArchitectureBuilder() {
        // utility class
    }

    /**
     * Adds architecture-related fields to the context.
     *
     * @param config the project configuration
     * @param ctx the context map to populate
     */
    static void buildArchitecture(
            ProjectConfig config,
            Map<String, Object> ctx) {
        ctx.put("architecture_style",
                config.architecture().style());
        ctx.put("domain_driven",
                ContextBuilder.toPythonBool(
                        config.architecture()
                                .domainDriven()));
        ctx.put("event_driven",
                ContextBuilder.toPythonBool(
                        config.architecture()
                                .eventDriven()));
        ctx.put("validate_with_archunit",
                ContextBuilder.toPythonBool(
                        config.architecture()
                                .validateWithArchUnit()));
        ctx.put("base_package",
                config.architecture().basePackage());
        ctx.put("event_store",
                config.architecture().eventStore());
        ctx.put("schema_registry",
                config.architecture().schemaRegistry());
        ctx.put("outbox_pattern",
                ContextBuilder.toPythonBool(
                        config.architecture()
                                .outboxPattern()));
        ctx.put("dead_letter_strategy",
                config.architecture()
                        .deadLetterStrategy());
        ctx.put("events_per_snapshot",
                config.architecture()
                        .eventsPerSnapshot());
        ctx.put("ddd_enabled",
                ContextBuilder.toPythonBool(
                        config.architecture()
                                .dddEnabled()));
    }

    /**
     * Adds review checklist fields to the context.
     *
     * @param config the project configuration
     * @param ctx the context map to populate
     */
    static void buildReviewChecklist(
            ProjectConfig config,
            Map<String, Object> ctx) {
        boolean hasEvent = hasEventInterface(config);
        boolean hasPciDss = config.security().frameworks()
                .contains("pci-dss");
        boolean hasLgpd = config.security().frameworks()
                .contains("lgpd");
        ctx.put("has_event_interface",
                ContextBuilder.toPythonBool(hasEvent));
        ctx.put("has_pci_dss",
                ContextBuilder.toPythonBool(hasPciDss));
        ctx.put("has_lgpd",
                ContextBuilder.toPythonBool(hasLgpd));
        ReviewChecklistScore score =
                ReviewChecklistScore.compute(
                        hasEvent, hasPciDss, hasLgpd);
        ctx.put("review_max_score", score.maxScore());
        ctx.put("review_go_threshold",
                score.goThreshold());
        ctx.put("review_conditional_rubric",
                ReviewChecklistSections.buildRubricRows(
                        hasEvent, hasPciDss, hasLgpd));
        ctx.put("review_conditional_criteria",
                ReviewChecklistSections
                        .buildDetailedCriteria(
                                hasEvent, hasPciDss,
                                hasLgpd));
    }

    private static boolean hasEventInterface(
            ProjectConfig config) {
        return config.interfaces().stream()
                .anyMatch(i -> i.type().contains("event"));
    }
}
