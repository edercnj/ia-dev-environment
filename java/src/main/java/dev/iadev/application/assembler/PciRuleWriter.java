package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Conditionally generates {@code 11-security-pci.md}
 * when the project {@code compliance} field includes
 * {@code pci-dss}.
 *
 * <p>The writer copies the template from
 * {@code targets/claude/rules/conditional/11-security-pci.md}
 * with placeholder replacement when the condition is
 * met.</p>
 *
 * <p>Extracted per SRP — the {@link CoreRulesWriter}
 * delegates PCI rule generation to this class.</p>
 *
 * @see CoreRulesWriter
 * @see RulesAssembler
 */
public final class PciRuleWriter {

    private static final String TEMPLATE_PATH =
            "targets/claude/rules/conditional/"
                    + "11-security-pci.md";
    private static final String OUTPUT_FILENAME =
            "11-security-pci.md";
    private static final String PCI_DSS = "pci-dss";

    private final Path resourcesDir;

    /**
     * Creates a PciRuleWriter with an explicit resources
     * directory.
     *
     * @param resourcesDir the base resources directory
     */
    PciRuleWriter(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * Conditionally generates the PCI security rule file
     * when the project compliance includes pci-dss.
     *
     * @param config   the project configuration
     * @param rulesDir the rules output directory
     * @param engine   the template engine
     * @param context  the placeholder context
     * @return list of generated file paths (0 or 1)
     */
    List<String> copyConditionalPciRule(
            ProjectConfig config,
            Path rulesDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        if (!config.compliance().contains(PCI_DSS)) {
            return List.of();
        }

        Path template =
                resourcesDir.resolve(TEMPLATE_PATH);
        if (!Files.exists(template)
                || !Files.isRegularFile(template)) {
            throw new IllegalStateException(
                    "PCI-DSS is enabled, but required "
                            + "template is missing: "
                            + template);
        }

        Path dest = rulesDir.resolve(OUTPUT_FILENAME);
        String path = CopyHelpers.copyTemplateFile(
                template, dest, engine, context);
        return List.of(path);
    }
}
