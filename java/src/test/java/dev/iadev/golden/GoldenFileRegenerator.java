package dev.iadev.golden;

import dev.iadev.application.assembler.AssemblerPipeline;
import dev.iadev.application.assembler.PipelineOptions;
import dev.iadev.config.ConfigProfiles;
import dev.iadev.domain.model.PipelineResult;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.smoke.SmokeProfiles;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Regenerates golden files for all 17 bundled profiles
 * by running the full assembler pipeline and copying
 * output to the test resources golden directory.
 *
 * <p>Usage:
 * <pre>{@code
 * mvn exec:java \
 *   -Dexec.mainClass=
 *     "dev.iadev.golden.GoldenFileRegenerator" \
 *   -Dexec.classpathScope="test"
 * }</pre>
 * </p>
 */
public final class GoldenFileRegenerator {

    private static final Path GOLDEN_DIR = Path.of(
            "src/test/resources/golden");

    private GoldenFileRegenerator() {
    }

    /**
     * Regenerates golden files for all profiles.
     *
     * @param args optional: golden directory path
     * @throws IOException if file operations fail
     */
    public static void main(String[] args)
            throws IOException {
        Path goldenRoot = args.length > 0
                ? Path.of(args[0])
                : GOLDEN_DIR;

        for (String profile : SmokeProfiles.profileList()) {
            regenerateProfile(profile, goldenRoot);
        }
    }

    private static void regenerateProfile(
            String profile, Path goldenRoot)
            throws IOException {
        Path goldenDir = goldenRoot.resolve(profile);
        Path tempDir = Files.createTempDirectory(
                "golden-regen-" + profile + "-");

        try {
            ProjectConfig config =
                    ConfigProfiles.getStack(profile);
            AssemblerPipeline pipeline =
                    new AssemblerPipeline(
                            AssemblerPipeline
                                    .buildAssemblers());
            PipelineOptions options = new PipelineOptions(
                    false, true, false, null);

            PipelineResult result = pipeline.runPipeline(
                    config, tempDir, options);

            if (!result.success()) {
                throw new IllegalStateException(
                        "Pipeline failed for: " + profile);
            }

            List<Path> preserved =
                    preservePlatformDirs(goldenDir);
            deleteTree(goldenDir);
            copyTree(tempDir, goldenDir);
            restorePlatformDirs(preserved, goldenDir);
        } finally {
            deleteTree(tempDir);
        }
    }

    private static List<Path> preservePlatformDirs(
            Path goldenDir) throws IOException {
        List<Path> preserved = new ArrayList<>();
        if (!Files.exists(goldenDir)) {
            return preserved;
        }
        try (var entries = Files.list(goldenDir)) {
            entries.filter(Files::isDirectory)
                    .filter(d -> d.getFileName()
                            .toString()
                            .startsWith("platform-"))
                    .forEach(d -> {
                        try {
                            Path backup =
                                    Files.createTempDirectory(
                                            "golden-plat-");
                            copyTree(d, backup);
                            preserved.add(backup);
                            preserved.add(d);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
        return preserved;
    }

    private static void restorePlatformDirs(
            List<Path> preserved, Path goldenDir)
            throws IOException {
        for (int i = 0; i < preserved.size(); i += 2) {
            Path backup = preserved.get(i);
            Path original = preserved.get(i + 1);
            Path target = goldenDir.resolve(
                    original.getFileName());
            copyTree(backup, target);
            deleteTree(backup);
        }
    }

    private static void copyTree(Path src, Path dest)
            throws IOException {
        Files.walkFileTree(src, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(
                    Path dir, BasicFileAttributes attrs)
                    throws IOException {
                Path target = dest.resolve(
                        src.relativize(dir));
                Files.createDirectories(target);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(
                    Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.copy(file,
                        dest.resolve(src.relativize(file)),
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
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(
                    Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(
                    Path dir, IOException exc)
                    throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
