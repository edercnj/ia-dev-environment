package dev.iadev.golden;

import dev.iadev.application.assembler.AssemblerDescriptor;
import dev.iadev.application.assembler.AssemblerFactory;
import dev.iadev.application.assembler.AssemblerPipeline;
import dev.iadev.application.assembler.PipelineOptions;
import dev.iadev.config.ConfigProfiles;
import dev.iadev.domain.model.PipelineResult;
import dev.iadev.domain.model.Platform;
import dev.iadev.domain.model.ProjectConfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;

/**
 * Utility to generate platform-specific golden files.
 *
 * <p>Generates golden files for the given profiles with
 * {@code --platform claude-code} and copies them to the
 * test resources golden directory.</p>
 *
 * <p>Usage: run {@code main()} to regenerate golden
 * files after pipeline changes.</p>
 */
final class PlatformGoldenFileGenerator {

    private static final String GOLDEN_DIR =
            "src/test/resources/golden";

    private PlatformGoldenFileGenerator() {
        // utility class
    }

    /**
     * Generates golden files for java-spring and go-gin
     * with platform claude-code.
     *
     * @param args unused
     * @throws IOException if file operations fail
     */
    public static void main(String[] args)
            throws IOException {
        generate("java-spring");
        generate("go-gin");
    }

    /**
     * Generates golden files for the given profile with
     * platform claude-code.
     *
     * @param profile the bundled stack profile name
     * @throws IOException if file operations fail
     */
    static void generate(String profile)
            throws IOException {
        Path tempDir = Files.createTempDirectory(
                "golden-platform-");
        try {
            runPipeline(profile, tempDir);
            Path goldenTarget = Path.of(GOLDEN_DIR)
                    .resolve(profile)
                    .resolve("platform-claude-code");
            copyDirectory(tempDir, goldenTarget);
        } finally {
            deleteRecursive(tempDir);
        }
    }

    private static void runPipeline(
            String profile, Path outputDir) {
        ProjectConfig config =
                ConfigProfiles.getStack(profile);
        PipelineOptions options = new PipelineOptions(
                false, true, false, false,
                null,
                Set.of(Platform.CLAUDE_CODE));
        List<AssemblerDescriptor> assemblers =
                AssemblerFactory.buildAssemblers(options);
        AssemblerPipeline pipeline =
                new AssemblerPipeline(assemblers);

        PipelineResult result = pipeline.runPipeline(
                config, outputDir, options);
        if (!result.success()) {
            throw new IllegalStateException(
                    "Pipeline failed for: " + profile);
        }
    }

    private static void copyDirectory(
            Path source, Path target) throws IOException {
        if (Files.exists(target)) {
            deleteRecursive(target);
        }
        Files.walkFileTree(source,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(
                            Path dir,
                            BasicFileAttributes attrs)
                            throws IOException {
                        Path dest = target.resolve(
                                source.relativize(dir));
                        Files.createDirectories(dest);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(
                            Path file,
                            BasicFileAttributes attrs)
                            throws IOException {
                        Path dest = target.resolve(
                                source.relativize(file));
                        Files.copy(file, dest,
                                StandardCopyOption
                                        .REPLACE_EXISTING);
                        return FileVisitResult.CONTINUE;
                    }
                });
    }

    private static void deleteRecursive(Path dir)
            throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        Files.walkFileTree(dir,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(
                            Path file,
                            BasicFileAttributes attrs)
                            throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(
                            Path d, IOException exc)
                            throws IOException {
                        Files.delete(d);
                        return FileVisitResult.CONTINUE;
                    }
                });
    }
}
