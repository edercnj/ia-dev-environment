package dev.iadev.smoke;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.iadev.application.assembler.AssemblerPipeline;
import dev.iadev.application.assembler.PipelineOptions;
import dev.iadev.config.ConfigProfiles;
import dev.iadev.domain.model.PipelineResult;
import dev.iadev.domain.model.ProjectConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates the expected artifacts manifest by running
 * the pipeline for all 13 bundled profiles and collecting
 * file counts, directories, and categories from the
 * output.
 *
 * <p>File-walking logic is delegated to
 * {@link FileTreeWalker}.</p>
 *
 * <p>Usage:
 * <pre>{@code
 * ExpectedArtifactsGenerator.generate(
 *     Path.of("expected-artifacts.json"));
 * }</pre>
 * </p>
 */
public final class ExpectedArtifactsGenerator {

    private static final List<String> PROFILES = List.of(
            "go-gin", "java-quarkus", "java-spring",
            "java-spring-hexagonal",
            "java-spring-cqrs-es",
            "java-spring-event-driven",
            "java-spring-fintech-pci",
            "kotlin-ktor", "python-click-cli",
            "python-fastapi", "rust-axum",
            "typescript-commander-cli",
            "typescript-nestjs");

    private static final ObjectMapper MAPPER =
            new ObjectMapper()
                    .enable(SerializationFeature
                            .INDENT_OUTPUT);

    private ExpectedArtifactsGenerator() {
    }

    /**
     * Generates the manifest JSON at the given path.
     *
     * <p>Runs the pipeline for each of the 13 profiles,
     * collects output metrics, and writes them as JSON.</p>
     *
     * @param outputPath target file path for the JSON
     * @throws IllegalArgumentException if outputPath is
     *         null
     * @throws IOException if file operations fail
     */
    public static void generate(Path outputPath)
            throws IOException {
        if (outputPath == null) {
            throw new IllegalArgumentException(
                    "Output path must not be null");
        }

        Map<String, Object> manifest =
                new LinkedHashMap<>();
        Map<String, Object> profilesMap =
                new LinkedHashMap<>();

        for (String profile : PROFILES) {
            profilesMap.put(profile,
                    generateForProfile(profile));
        }

        manifest.put("profiles", profilesMap);

        if (outputPath.getParent() != null) {
            Files.createDirectories(
                    outputPath.getParent());
        }
        MAPPER.writeValue(outputPath.toFile(), manifest);
    }

    /**
     * Runs as standalone tool to regenerate the manifest.
     *
     * @param args optional: output path (defaults to
     *             src/test/resources/smoke/expected
     *             -artifacts.json)
     * @throws IOException if file operations fail
     */
    public static void main(String[] args)
            throws IOException {
        Path output = args.length > 0
                ? Path.of(args[0])
                : Path.of("src/test/resources/smoke/"
                        + "expected-artifacts.json");
        generate(output);
    }

    private static Map<String, Object> generateForProfile(
            String profile) throws IOException {
        Path tempDir = Files.createTempDirectory(
                "manifest-gen-",
                PosixFilePermissions.asFileAttribute(
                        PosixFilePermissions.fromString(
                                "rwx------")));
        try {
            runPipeline(profile, tempDir);
            return FileTreeWalker
                    .collectMetrics(tempDir);
        } finally {
            FileTreeWalker.deleteTree(tempDir);
        }
    }

    private static void runPipeline(
            String profile, Path outputDir) {
        ProjectConfig config =
                ConfigProfiles.getStack(profile);
        AssemblerPipeline pipeline =
                new AssemblerPipeline(
                        AssemblerPipeline
                                .buildAssemblers());
        PipelineOptions options = new PipelineOptions(
                false, true, false, null);

        PipelineResult result = pipeline.runPipeline(
                config, outputDir, options);
        if (!result.success()) {
            throw new IllegalStateException(
                    "Pipeline failed for profile: "
                            + profile);
        }
    }
}
