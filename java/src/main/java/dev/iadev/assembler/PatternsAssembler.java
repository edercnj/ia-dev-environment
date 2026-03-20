package dev.iadev.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.stack.PatternMapping;
import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Assembles {@code .claude/skills/patterns/} from source
 * pattern documentation files.
 *
 * <p>This is the fourth assembler in the pipeline (position
 * 4 of 23 per RULE-005). It generates:
 * <ol>
 *   <li>Individual pattern reference files copied to
 *       {@code skills/patterns/references/{category}/}
 *       with placeholder replacement</li>
 *   <li>A consolidated {@code SKILL.md} containing all
 *       rendered patterns joined by {@code ---}
 *       separators</li>
 * </ol>
 *
 * <p>Pattern categories are selected by architecture style
 * via {@link PatternMapping#selectPatterns}.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * Assembler patterns = new PatternsAssembler();
 * List<String> files = patterns.assemble(
 *     config, engine, outputDir);
 * }</pre>
 * </p>
 *
 * @see Assembler
 * @see PatternMapping
 */
public final class PatternsAssembler implements Assembler {

    private static final String SKILLS_DIR = "skills";
    private static final String PATTERNS_DIR = "patterns";
    private static final String REFERENCES_DIR =
            "references";
    private static final String CONSOLIDATED_FILE =
            "SKILL.md";
    private static final String SECTION_SEPARATOR =
            "\n\n---\n\n";

    private final Path resourcesDir;

    /**
     * Creates a PatternsAssembler using classpath resources.
     */
    public PatternsAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a PatternsAssembler with an explicit resources
     * directory.
     *
     * @param resourcesDir the base resources directory
     */
    public PatternsAssembler(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Selects pattern categories based on config,
     * collects pattern files, copies each to references,
     * and creates a consolidated SKILL.md.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        List<String> categories =
                PatternMapping.selectPatterns(config);
        if (categories.isEmpty()) {
            return List.of();
        }

        List<PatternFile> patternFiles =
                collectPatternFiles(categories);
        if (patternFiles.isEmpty()) {
            return List.of();
        }

        Map<String, Object> context =
                ContextBuilder.buildContext(config);
        List<String> rendered =
                renderContents(patternFiles, engine, context);

        List<String> generated = new ArrayList<>();
        generated.addAll(
                flushPatterns(
                        patternFiles, rendered, outputDir));
        generated.add(
                flushConsolidated(rendered, outputDir));

        return generated;
    }

    /**
     * Collects all .md files from the selected pattern
     * category directories.
     *
     * @param categories the sorted list of category names
     * @return list of pattern files with category metadata
     */
    private List<PatternFile> collectPatternFiles(
            List<String> categories) {
        Path patternsRoot =
                resourcesDir.resolve(PATTERNS_DIR);
        List<PatternFile> files = new ArrayList<>();

        for (String category : categories) {
            Path catDir = patternsRoot.resolve(category);
            if (!Files.exists(catDir)
                    || !Files.isDirectory(catDir)) {
                continue;
            }
            List<Path> mdFiles = listMdFilesSorted(catDir);
            for (Path file : mdFiles) {
                files.add(new PatternFile(
                        category, file));
            }
        }
        return files;
    }

    /**
     * Renders pattern file contents with placeholder
     * replacement.
     *
     * @param patternFiles the pattern files to render
     * @param engine       the template engine
     * @param context      the context for placeholders
     * @return list of rendered content strings
     */
    private List<String> renderContents(
            List<PatternFile> patternFiles,
            TemplateEngine engine,
            Map<String, Object> context) {
        List<String> rendered = new ArrayList<>();
        for (PatternFile pf : patternFiles) {
            try {
                String content = Files.readString(
                        pf.path(), StandardCharsets.UTF_8);
                rendered.add(
                        engine.replacePlaceholders(
                                content, context));
            } catch (IOException e) {
                throw new UncheckedIOException(
                        "Failed to read pattern: "
                                + pf.path(), e);
            }
        }
        return rendered;
    }

    /**
     * Writes individual pattern files to the references
     * subdirectory.
     *
     * @param patternFiles the source pattern files
     * @param rendered     the rendered contents
     * @param outputDir    the output directory
     * @return list of written file paths
     */
    private List<String> flushPatterns(
            List<PatternFile> patternFiles,
            List<String> rendered,
            Path outputDir) {
        List<String> results = new ArrayList<>();
        for (int i = 0; i < patternFiles.size(); i++) {
            PatternFile pf = patternFiles.get(i);
            String content = rendered.get(i);
            Path targetDir = outputDir
                    .resolve(SKILLS_DIR)
                    .resolve(PATTERNS_DIR)
                    .resolve(REFERENCES_DIR)
                    .resolve(pf.category());
            CopyHelpers.ensureDirectory(targetDir);
            Path destFile = targetDir.resolve(
                    pf.path().getFileName().toString());
            CopyHelpers.writeFile(destFile, content);
            results.add(destFile.toString());
        }
        return results;
    }

    /**
     * Creates the consolidated SKILL.md by joining all
     * rendered pattern contents with section separators.
     *
     * @param rendered  the rendered contents
     * @param outputDir the output directory
     * @return the path of the consolidated file
     */
    private String flushConsolidated(
            List<String> rendered,
            Path outputDir) {
        Path destPath = outputDir
                .resolve(SKILLS_DIR)
                .resolve(PATTERNS_DIR)
                .resolve(CONSOLIDATED_FILE);
        CopyHelpers.ensureDirectory(destPath.getParent());
        String merged =
                String.join(SECTION_SEPARATOR, rendered);
        CopyHelpers.writeFile(destPath, merged);
        return destPath.toString();
    }

    private static List<Path> listMdFilesSorted(Path dir) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(f -> f.toString()
                            .endsWith(".md"))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to list directory: " + dir, e);
        }
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(PATTERNS_DIR);
    }

    /**
     * Pairs a pattern file with its category name.
     *
     * @param category the pattern category directory name
     * @param path     the absolute path to the pattern file
     */
    private record PatternFile(
            String category,
            Path path) {
    }
}
