package dev.iadev.golden;

import dev.iadev.application.assembler.AssemblerPipeline;
import dev.iadev.application.assembler.PipelineOptions;
import dev.iadev.config.ConfigProfiles;
import dev.iadev.domain.model.ProjectConfig;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Utility to regenerate Java golden files by running the
 * pipeline for each profile and copying output to the test
 * resources golden directory.
 *
 * <p>Run this manually when template changes require
 * golden file updates. Not a test -- invoked via main().
 */
public final class GoldenFileRegenerator {

    private GoldenFileRegenerator() {
    }

    /**
     * Regenerates golden files for all 17 profiles.
     *
     * @param args not used
     * @throws IOException if file operations fail
     */
    public static void main(String[] args)
            throws IOException {
        Path goldenRoot = Path.of(
                "src/test/resources/golden");

        String[] profiles = {
                "go-gin", "java-quarkus", "java-spring",
                "java-spring-clickhouse",
                "java-spring-cqrs-es",
                "java-spring-elasticsearch",
                "java-spring-event-driven",
                "java-spring-fintech-pci",
                "java-spring-hexagonal",
                "java-spring-neo4j",
                "kotlin-ktor", "python-click-cli",
                "python-fastapi",
                "python-fastapi-timescale",
                "rust-axum",
                "typescript-commander-cli",
                "typescript-nestjs"
        };

        for (String profile : profiles) {
            regenerateProfile(profile, goldenRoot);
        }
        System.out.println("Done.");
    }

    private static void regenerateProfile(
            String profile, Path goldenRoot)
            throws IOException {
        System.out.println("Regenerating: " + profile);
        Path tempDir = Files.createTempDirectory(
                "golden-regen-");
        try {
            ProjectConfig config =
                    ConfigProfiles.getStack(profile);
            AssemblerPipeline pipeline =
                    new AssemblerPipeline(
                            AssemblerPipeline
                                    .buildAssemblers());
            pipeline.runPipeline(config, tempDir,
                    new PipelineOptions(
                            false, true, false, null));

            Path goldenDir =
                    goldenRoot.resolve(profile);
            copyTree(tempDir, goldenDir);
            System.out.println("  OK: " + profile);
        } finally {
            deleteTree(tempDir);
        }
    }

    private static void copyTree(Path src, Path dest)
            throws IOException {
        Files.walkFileTree(src,
                newCopyVisitor(src, dest));
    }

    private static SimpleFileVisitor<Path> newCopyVisitor(
            Path src, Path dest) {
        return new SimpleFileVisitor<>() {
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
        };
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
