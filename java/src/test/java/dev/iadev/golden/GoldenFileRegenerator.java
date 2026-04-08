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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;

/**
 * Regenerates golden files for all profiles by running the
 * assembler pipeline and copying output to the golden
 * directories under {@code src/test/resources/golden/}.
 *
 * <p>Usage: {@code mvn exec:java
 * -Dexec.mainClass="dev.iadev.golden.GoldenFileRegenerator"
 * -Dexec.classpathScope="test"}</p>
 */
public final class GoldenFileRegenerator {

    private static final String PLATFORM_SUBDIR =
            "platform-claude-code";

    private static final List<String> PLATFORM_PROFILES =
            List.of("java-spring", "go-gin");

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
        // utility
    }

    /**
     * Main entry point for golden file regeneration.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        Path goldenRoot = Path.of(
                "src/test/resources/golden");
        int success = 0;
        int failed = 0;

        for (String profile : PROFILES) {
            try {
                regenerateProfile(profile, goldenRoot);
                success++;
                System.out.printf(
                        "  [OK] %s%n", profile);
            } catch (Exception e) {
                failed++;
                System.err.printf(
                        "  [FAIL] %s: %s%n",
                        profile, e.getMessage());
            }
        }

        for (String profile : PLATFORM_PROFILES) {
            try {
                regeneratePlatformProfile(
                        profile, goldenRoot);
                System.out.printf(
                        "  [OK] %s/%s%n",
                        profile, PLATFORM_SUBDIR);
            } catch (Exception e) {
                failed++;
                System.err.printf(
                        "  [FAIL] %s/%s: %s%n",
                        profile, PLATFORM_SUBDIR,
                        e.getMessage());
            }
        }

        int total = PROFILES.size()
                + PLATFORM_PROFILES.size();
        System.out.printf(
                "%nRegenerated %d/%d targets "
                        + "(%d failed)%n",
                success + PLATFORM_PROFILES.size()
                        - failed,
                total, failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void regenerateProfile(
            String profile, Path goldenRoot)
            throws IOException {
        Path tempDir = Files.createTempDirectory(
                "golden-" + profile);
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
                throw new IllegalStateException(
                        "Pipeline failed for: "
                                + profile);
            }

            Path goldenDir =
                    goldenRoot.resolve(profile);
            deleteDirectory(goldenDir);
            copyDirectory(tempDir, goldenDir);
        } finally {
            deleteDirectory(tempDir);
        }
    }

    private static void regeneratePlatformProfile(
            String profile, Path goldenRoot)
            throws IOException {
        Path tempDir = Files.createTempDirectory(
                "golden-platform-" + profile);
        try {
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
                            config, tempDir, options);
            if (!result.success()) {
                throw new IllegalStateException(
                        "Platform pipeline failed for: "
                                + profile);
            }

            Path platformDir = goldenRoot
                    .resolve(profile)
                    .resolve(PLATFORM_SUBDIR);
            deleteDirectory(platformDir);
            copyDirectory(tempDir, platformDir);
        } finally {
            deleteDirectory(tempDir);
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
            public FileVisitResult postVisitDirectory(
                    Path d, IOException exc)
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
            public FileVisitResult preVisitDirectory(
                    Path dir,
                    BasicFileAttributes attrs)
                    throws IOException {
                Path target =
                        dest.resolve(
                                src.relativize(dir));
                Files.createDirectories(target);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(
                    Path file,
                    BasicFileAttributes attrs)
                    throws IOException {
                Files.copy(file,
                        dest.resolve(
                                src.relativize(file)),
                        StandardCopyOption
                                .REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
