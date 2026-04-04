package dev.iadev.assembler;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Injects checklist sections into agent files based on
 * configuration-driven rules.
 *
 * <p>Extracted from {@link AgentsAssembler} to keep both
 * classes under 250 lines per RULE-004.</p>
 *
 * @see AgentsAssembler
 * @see AgentsSelection
 */
final class ChecklistInjector {

    private static final String AGENTS_TEMPLATES_DIR =
            "agents-templates";
    private static final String CHECKLISTS_DIR =
            "checklists";
    private static final String AGENTS_OUTPUT = "agents";

    private ChecklistInjector() {
        // utility class
    }

    /**
     * Iterates checklist rules and injects active ones.
     *
     * @param config       the project configuration
     * @param resourcesDir the resources directory
     * @param outputDir    the output directory
     */
    static void injectChecklists(
            ProjectConfig config,
            Path resourcesDir,
            Path outputDir) {
        for (var rule : AgentsSelection
                .buildChecklistRules(config)) {
            if (!rule.active()) {
                continue;
            }
            injectSingleChecklist(
                    rule.agent(),
                    rule.checklist(),
                    resourcesDir,
                    outputDir);
        }
    }

    private static void injectSingleChecklist(
            String agentFile,
            String checklistFile,
            Path resourcesDir,
            Path outputDir) {
        Path agentPath = outputDir.resolve(
                AGENTS_OUTPUT + "/" + agentFile);
        Path checklistSrc = resolveChecklistSrc(
                resourcesDir, checklistFile);

        if (!Files.exists(agentPath)
                || !Files.exists(checklistSrc)) {
            return;
        }

        performInjection(
                agentPath, checklistSrc, checklistFile);
    }

    private static Path resolveChecklistSrc(
            Path resourcesDir, String checklistFile) {
        return resourcesDir.resolve(
                AGENTS_TEMPLATES_DIR + "/"
                        + CHECKLISTS_DIR + "/"
                        + checklistFile);
    }

    private static void performInjection(
            Path agentPath, Path checklistSrc,
            String checklistFile) {
        try {
            String marker = AgentsSelection
                    .checklistMarker(checklistFile);
            String section = Files.readString(
                    checklistSrc, StandardCharsets.UTF_8);
            String base = Files.readString(
                    agentPath, StandardCharsets.UTF_8);
            String result = TemplateEngine.injectSection(
                    base, section, marker);
            Files.writeString(agentPath, result,
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to inject checklist: "
                            + checklistFile, e);
        }
    }
}
