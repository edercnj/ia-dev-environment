package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Copies incident response and postmortem templates to
 * {@code docs/templates/} in the output directory.
 *
 * <p>Both templates are <strong>unconditional</strong>:
 * they are always generated regardless of the project
 * profile, since incident management is a universal
 * practice.</p>
 *
 * <p>Content is copied verbatim (no Pebble rendering)
 * because these templates contain placeholder text
 * intended for manual completion during actual
 * incidents.</p>
 *
 * <p>Graceful no-op: if the source template files do
 * not exist in the resources directory, returns an
 * empty list (backward compatibility).</p>
 *
 * @see Assembler
 */
public final class IncidentTemplatesAssembler
        implements Assembler {

    private static final String TEMPLATES_SUBDIR =
            "templates";
    private static final String IR_FILENAME =
            "_TEMPLATE-INCIDENT-RESPONSE.md";
    private static final String PM_FILENAME =
            "_TEMPLATE-POSTMORTEM.md";
    private static final String OUTPUT_SUBDIR =
            "docs/templates";

    /** The 7 mandatory incident response sections. */
    static final List<String> INCIDENT_RESPONSE_SECTIONS =
            List.of(
                    "Severity Classification",
                    "Detection & Triage",
                    "Communication Plan",
                    "Mitigation Steps",
                    "Escalation Matrix",
                    "Resolution Verification",
                    "Timeline Template");

    /** The 8 mandatory postmortem sections. */
    static final List<String> POSTMORTEM_SECTIONS =
            List.of(
                    "Incident Summary",
                    "Timeline",
                    "Root Cause Analysis",
                    "Impact Assessment",
                    "Contributing Factors",
                    "Action Items",
                    "Lessons Learned",
                    "Prevention Measures");

    private final Path resourcesDir;

    /**
     * Creates an IncidentTemplatesAssembler using
     * classpath resources.
     */
    public IncidentTemplatesAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates an IncidentTemplatesAssembler with an
     * explicit resources directory.
     *
     * @param resourcesDir the base resources directory
     */
    public IncidentTemplatesAssembler(
            Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Copies both incident response and postmortem
     * templates to {@code docs/templates/}. Returns
     * empty list if either template is missing.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        Path irSource = resolveTemplate(IR_FILENAME);
        Path pmSource = resolveTemplate(PM_FILENAME);

        if (!Files.exists(irSource)
                || !Files.exists(pmSource)) {
            return List.of();
        }

        String irContent =
                CopyHelpers.readFile(irSource);
        String pmContent =
                CopyHelpers.readFile(pmSource);

        if (!hasAllSections(irContent,
                INCIDENT_RESPONSE_SECTIONS)
                || !hasAllSections(pmContent,
                POSTMORTEM_SECTIONS)) {
            return List.of();
        }

        return copyToOutput(
                irContent, pmContent, outputDir);
    }

    private Path resolveTemplate(String filename) {
        return resourcesDir
                .resolve(TEMPLATES_SUBDIR)
                .resolve(filename);
    }

    private List<String> copyToOutput(
            String irContent,
            String pmContent,
            Path outputDir) {
        Path destDir = outputDir.resolve(OUTPUT_SUBDIR);
        CopyHelpers.ensureDirectory(destDir);

        List<String> results = new ArrayList<>();

        Path irDest = destDir.resolve(IR_FILENAME);
        CopyHelpers.writeFile(irDest, irContent);
        results.add(irDest.toString());

        Path pmDest = destDir.resolve(PM_FILENAME);
        CopyHelpers.writeFile(pmDest, pmContent);
        results.add(pmDest.toString());

        return results;
    }

    private static boolean hasAllSections(
            String content, List<String> sections) {
        return CopyHelpers.hasAllMandatorySections(
                content, sections);
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(
                        TEMPLATES_SUBDIR + "/"
                                + IR_FILENAME,
                        2);
    }
}
