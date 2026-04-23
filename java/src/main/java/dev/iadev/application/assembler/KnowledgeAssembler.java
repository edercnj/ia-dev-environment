package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Assembles {@code .claude/knowledge/} from
 * {@code targets/claude/knowledge/}.
 *
 * <p>Copies {@code .md} files verbatim, preserving
 * subdirectory structure. Validates frontmatter to
 * reject skill-only fields (RULE-051-07).</p>
 *
 * @see Assembler
 * @see RulesAssembler
 */
public final class KnowledgeAssembler implements Assembler {

    private static final String SOURCE_RELATIVE =
            "targets/claude/knowledge";

    private static final List<String> FORBIDDEN_FIELDS =
            List.of("user-invocable", "allowed-tools",
                    "argument-hint", "context-budget");

    /**
     * Per-line regex for each forbidden field. Matches a
     * key at the start of a line (optionally indented, but
     * anchored at the line start to avoid false positives
     * inside string values). Built once at class load.
     */
    private static final Map<String, Pattern>
            FIELD_PATTERNS = buildFieldPatterns();

    private static Map<String, Pattern>
            buildFieldPatterns() {
        Map<String, Pattern> map = new LinkedHashMap<>();
        for (String field : FORBIDDEN_FIELDS) {
            map.put(field, Pattern.compile(
                    "^" + Pattern.quote(field) + "\\s*:"));
        }
        return map;
    }

    private final Path overrideSourceDir;

    /** Default constructor resolves source from classpath. */
    public KnowledgeAssembler() {
        this.overrideSourceDir = null;
    }

    /**
     * Test constructor with explicit source directory.
     *
     * @param sourceDir the source directory to use instead
     *                  of the classpath-resolved default
     */
    KnowledgeAssembler(Path sourceDir) {
        this.overrideSourceDir = sourceDir;
    }

    /**
     * Implements the {@link Assembler} contract.
     *
     * <p>Resolves {@code targets/claude/knowledge/} from
     * the classpath (or override), then delegates to
     * {@link #assemble(Path, Path)}. Returns an empty list
     * when the source directory does not exist.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        Path sourceDir = resolveEffectiveSourceDir();
        if (!Files.isDirectory(sourceDir)) {
            return List.of();
        }
        Path targetDir = outputDir.resolve("knowledge");
        List<String> generated = new ArrayList<>();
        walkAndCopy(sourceDir, targetDir, generated);
        // RULE-051-07 / PR #609 review: resolve
        // {{PLACEHOLDER}} tokens after verbatim copy so
        // generated KPs carry no unresolved markers. Uses
        // the same context builder as other assemblers.
        CopyHelpers.replacePlaceholdersInDir(
                targetDir, engine,
                dev.iadev.config.ContextBuilder
                        .buildContext(config));
        return List.copyOf(generated);
    }

    /**
     * Copies all {@code .md} files from {@code sourceDir}
     * to {@code targetDir}, preserving subdirectory
     * structure and validating frontmatter contracts.
     *
     * @param sourceDir the source knowledge directory
     * @param targetDir the target output directory
     * @throws IllegalStateException  if a file contains a
     *                                forbidden frontmatter
     *                                field or is not an
     *                                {@code .md} file
     * @throws UncheckedIOException   on I/O failure
     */
    public void assemble(Path sourceDir, Path targetDir) {
        walkAndCopy(sourceDir, targetDir, new ArrayList<>());
    }

    private void walkAndCopy(
            Path sourceDir,
            Path targetDir,
            List<String> generated) {
        CopyHelpers.ensureDirectory(targetDir);
        try {
            Files.walkFileTree(sourceDir,
                    new KnowledgeCopyVisitor(
                            sourceDir, targetDir,
                            generated));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path resolveEffectiveSourceDir() {
        if (overrideSourceDir != null) {
            return overrideSourceDir;
        }
        return dev.iadev.util.ResourceResolver
                .resolveResourceDir(SOURCE_RELATIVE);
    }

    private final class KnowledgeCopyVisitor
            extends SimpleFileVisitor<Path> {

        private final Path sourceDir;
        private final Path targetDir;
        private final List<String> generated;

        KnowledgeCopyVisitor(
                Path sourceDir,
                Path targetDir,
                List<String> generated) {
            this.sourceDir = sourceDir;
            this.targetDir = targetDir;
            this.generated = generated;
        }

        @Override
        public FileVisitResult preVisitDirectory(
                Path dir, BasicFileAttributes attrs)
                throws IOException {
            Path target = targetDir.resolve(
                    sourceDir.relativize(dir));
            Files.createDirectories(target);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(
                Path file, BasicFileAttributes attrs)
                throws IOException {
            String name = file.getFileName().toString();
            if (name.startsWith(".")) {
                return FileVisitResult.CONTINUE;
            }
            if (!name.endsWith(".md")) {
                throw new IllegalStateException(
                        "Knowledge source contains"
                                + " non-md file: %s"
                                .formatted(
                                        file.getFileName()));
            }
            validateFrontmatter(file);
            Path dest = targetDir.resolve(
                    sourceDir.relativize(file));
            Files.copy(file, dest,
                    StandardCopyOption.REPLACE_EXISTING);
            generated.add(dest.toString());
            return FileVisitResult.CONTINUE;
        }
    }

    /**
     * Validates that YAML frontmatter (if present) does not
     * declare any {@link #FORBIDDEN_FIELDS}.
     *
     * <p>Parses the frontmatter block line-by-line: the
     * opening delimiter MUST be {@code ---} as the first
     * line; the closing delimiter is the first subsequent
     * line that is exactly {@code ---} (after trimming),
     * not a substring inside a value. Field detection uses
     * a per-line regex {@code ^<field>\s*:} so that spaces,
     * comments, and string values containing the field name
     * cannot produce false positives (PR #609 review).</p>
     */
    private static void validateFrontmatter(Path file)
            throws IOException {
        String content = Files.readString(
                file, StandardCharsets.UTF_8);
        String[] lines = content.split("\n", -1);
        if (lines.length == 0
                || !"---".equals(lines[0].trim())) {
            return;
        }
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if ("---".equals(line.trim())) {
                return;
            }
            for (String field : FORBIDDEN_FIELDS) {
                if (FIELD_PATTERNS.get(field)
                        .matcher(line).find()) {
                    throw new IllegalStateException(
                            ("Knowledge pack %s declares"
                                    + " skill-only field %s")
                                    .formatted(
                                            file.getFileName(),
                                            field));
                }
            }
        }
    }
}
