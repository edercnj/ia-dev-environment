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
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;

/**
 * Regenerates golden files for all standard profiles by running
 * the pipeline and overwriting the golden directory.
 *
 * <p>Two passes are performed: first, all standard profiles
 * ({@link #PROFILES}) are regenerated with the default pipeline;
 * then, platform-specific output is regenerated for a subset of
 * profiles ({@link #PLATFORM_PROFILES}).</p>
 *
 * <p>Usage: run via mvn exec:java with
 * test classpath scope.</p>
 */
public final class GoldenFileRegenerator {

    private static final List<String> PLATFORM_PROFILES =
            List.of("java-spring");

    private static final String PLATFORM_SUBDIR =
            "platform-claude-code";

    private static final List<String> PROFILES = List.of(
            "java-quarkus", "java-spring",
            "java-spring-clickhouse",
            "java-spring-cqrs-es",
            "java-spring-elasticsearch",
            "java-spring-event-driven",
            "java-spring-fintech-pci",
            "java-spring-hexagonal",
            "java-spring-neo4j");

    private GoldenFileRegenerator() {
    }

    /**
     * Regenerates golden files.
     *
     * @param args optional: golden base path
     * @throws IOException if file operations fail
     */
    public static void main(String[] args)
            throws IOException {
        String base = args.length > 0
                ? args[0]
                : "src/test/resources/golden";
        Path goldenBase = Path.of(base);

        for (String profile : PROFILES) {
            regenerateProfile(profile, goldenBase);
        }
        for (String profile : PLATFORM_PROFILES) {
            regeneratePlatformProfile(
                    profile, goldenBase);
        }
    }

    private static void regeneratePlatformProfile(
            String profile, Path goldenBase)
            throws IOException {
        Path tempDir = Files.createTempDirectory(
                "golden-platform-regen-");
        try {
            Path outputDir = tempDir.resolve(profile);
            Files.createDirectories(outputDir);

            ProjectConfig config =
                    ConfigProfiles.getStack(profile);
            PipelineOptions options =
                    new PipelineOptions(
                            false, true, false, false,
                            null,
                            Set.of(Platform.CLAUDE_CODE));
            List<AssemblerDescriptor> assemblers =
                    AssemblerFactory.buildAssemblers(
                            options);
            AssemblerPipeline pipeline =
                    new AssemblerPipeline(assemblers);

            PipelineResult result =
                    pipeline.runPipeline(
                            config, outputDir, options);

            if (!result.success()) {
                System.err.println(
                        "PLATFORM FAILED: " + profile);
                return;
            }

            Path goldenDir = goldenBase.resolve(profile)
                    .resolve(PLATFORM_SUBDIR);
            deleteTree(goldenDir);
            copyTree(outputDir, goldenDir);
            System.out.println(
                    "Regenerated platform: " + profile);
        } finally {
            deleteTree(tempDir);
        }
    }

    private static void regenerateProfile(
            String profile, Path goldenBase)
            throws IOException {
        Path tempDir = Files.createTempDirectory(
                "golden-regen-");
        try {
            Path outputDir = tempDir.resolve(profile);
            Files.createDirectories(outputDir);

            ProjectConfig config =
                    ConfigProfiles.getStack(profile);
            AssemblerPipeline pipeline =
                    new AssemblerPipeline(
                            AssemblerPipeline
                                    .buildAssemblers());
            PipelineOptions options =
                    new PipelineOptions(
                            false, true, false, null);

            PipelineResult result =
                    pipeline.runPipeline(
                            config, outputDir, options);

            if (!result.success()) {
                System.err.println(
                        "FAILED: " + profile);
                return;
            }

            Path goldenDir = goldenBase.resolve(profile);
            cleanGoldenDir(goldenDir);
            copyTree(outputDir, goldenDir);
            System.out.println("Regenerated: " + profile);
        } finally {
            deleteTree(tempDir);
        }
    }

    private static void cleanGoldenDir(Path dir)
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
                String rel = dir.relativize(file)
                        .toString();
                if (!rel.startsWith(
                        "platform-claude-code")) {
                    Files.delete(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(
                    Path d, IOException exc)
                    throws IOException {
                String rel = dir.relativize(d).toString();
                if (!rel.isEmpty()
                        && !rel.startsWith(
                        "platform-claude-code")) {
                    try {
                        Files.delete(d);
                    } catch (
                            DirectoryNotEmptyException e) {
                        // OK
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void copyTree(Path src, Path dest)
            throws IOException {
        Files.walkFileTree(src,
                new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(
                    Path dir,
                    BasicFileAttributes attrs)
                    throws IOException {
                Path target = dest.resolve(
                        src.relativize(dir));
                Files.createDirectories(target);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(
                    Path file,
                    BasicFileAttributes attrs)
                    throws IOException {
                Path target = dest.resolve(
                        src.relativize(file));
                Files.copy(file, target,
                        StandardCopyOption
                                .REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteTree(Path dir)
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
