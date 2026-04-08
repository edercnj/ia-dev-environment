package dev.iadev.golden;

import dev.iadev.application.assembler.AssemblerFactory;
import dev.iadev.application.assembler.AssemblerPipeline;
import dev.iadev.application.assembler.PipelineOptions;
import dev.iadev.config.ConfigProfiles;
import dev.iadev.domain.model.PipelineResult;
import dev.iadev.domain.model.Platform;
import dev.iadev.domain.model.ProjectConfig;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

/**
 * Utility to regenerate golden files for all profiles.
 * Run this when pipeline output changes (e.g., new skill).
 *
 * <p>Usage: {@code mvn exec:java
 *   -Dexec.mainClass=dev.iadev.golden.GoldenFileRegenerator
 *   -Dexec.classpathScope=test}</p>
 */
public final class GoldenFileRegenerator {

    private static final List<String> PROFILES = List.of(
            "go-gin",
            "java-quarkus",
            "java-spring",
            "java-spring-clickhouse",
            "java-spring-cqrs-es",
            "java-spring-elasticsearch",
            "java-spring-event-driven",
            "java-spring-fintech-pci",
            "java-spring-hexagonal",
            "java-spring-neo4j",
            "kotlin-ktor",
            "python-click-cli",
            "python-fastapi",
            "python-fastapi-timescale",
            "rust-axum",
            "typescript-commander-cli",
            "typescript-nestjs"
    );

    private GoldenFileRegenerator() {
    }

    /**
     * Regenerates golden files for all 17 profiles.
     *
     * @param args unused
     * @throws IOException if file operations fail
     */
    public static void main(String[] args)
            throws IOException {
        Path goldenRoot = Path.of(
                "src/test/resources/golden");

        for (String profile : PROFILES) {
            Path tempDir = Files.createTempDirectory(
                    "golden-regen-" + profile);
            try {
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
                                config, tempDir, options);
                if (!result.success()) {
                    System.err.println(
                            "FAILED: " + profile);
                    continue;
                }

                Path goldenDir =
                        goldenRoot.resolve(profile);
                deleteDirectory(goldenDir);
                copyDirectory(tempDir, goldenDir);
                System.out.println("OK: " + profile);
            } finally {
                deleteDirectory(tempDir);
            }
        }
        regeneratePlatformGoldens(goldenRoot);
    }

    private static void regeneratePlatformGoldens(
            Path goldenRoot) throws IOException {
        String[] platformProfiles = {
                "go-gin", "java-spring"
        };
        for (String profile : platformProfiles) {
            Path tempDir = Files.createTempDirectory(
                    "golden-platform-" + profile);
            try {
                ProjectConfig config =
                        ConfigProfiles.getStack(profile);
                PipelineOptions options =
                        new PipelineOptions(
                                false, true, false,
                                false, null,
                                java.util.Set.of(
                                        Platform
                                                .CLAUDE_CODE));
                AssemblerPipeline pipeline =
                        new AssemblerPipeline(
                                AssemblerFactory
                                        .buildAssemblers(
                                                options));
                PipelineResult result =
                        pipeline.runPipeline(
                                config, tempDir, options);
                if (!result.success()) {
                    System.err.println(
                            "FAILED platform: "
                                    + profile);
                    continue;
                }
                Path platformDir = goldenRoot.resolve(
                        profile
                                + "/platform-claude-code");
                deleteDirectory(platformDir);
                copyDirectory(tempDir, platformDir);
                System.out.println(
                        "OK platform: " + profile);
            } finally {
                deleteDirectory(tempDir);
            }
        }
    }

    private static void deleteDirectory(Path dir)
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
                    public FileVisitResult
                            postVisitDirectory(
                                    Path d, IOException e)
                            throws IOException {
                        Files.delete(d);
                        return FileVisitResult.CONTINUE;
                    }
                });
    }

    private static void copyDirectory(
            Path src, Path dest) throws IOException {
        Files.walkFileTree(src,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult
                            preVisitDirectory(
                                    Path dir,
                                    BasicFileAttributes a)
                            throws IOException {
                        Files.createDirectories(
                                dest.resolve(
                                        src.relativize(
                                                dir)));
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(
                            Path file,
                            BasicFileAttributes a)
                            throws IOException {
                        Files.copy(file,
                                dest.resolve(
                                        src.relativize(
                                                file)),
                                StandardCopyOption
                                        .REPLACE_EXISTING);
                        return FileVisitResult.CONTINUE;
                    }
                });
    }
}
