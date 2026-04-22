package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.application.assembler.AssemblerDescriptor;
import dev.iadev.application.assembler.AssemblerFactory;
import dev.iadev.application.assembler.AssemblerPipeline;
import dev.iadev.application.assembler.PipelineOptions;
import dev.iadev.config.ConfigProfiles;
import dev.iadev.domain.model.Platform;
import dev.iadev.domain.model.ProjectConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * EPIC-0048 invariant: the `ia-dev-env` CLI must not emit
 * any empty directory into a generated project.
 *
 * <p>Background: story-0048-0001 investigation found Bug A
 * NOT reproducible on develop at commit d8f7ff0c2 across
 * all 17 stacks — zero empty directories were found. This
 * test is therefore a <b>regression gate</b>, not a
 * reproduction of a live bug: if any commit in the future
 * introduces an empty directory in any stack's output,
 * this parameterized test catches it.
 *
 * <p>RULE-048-04: Zero Empty Directories invariant
 * (definition: directory with no regular files and no
 * subdirectories that contain regular files — bottom-up).
 *
 * @see dev.iadev.application.assembler.AssemblerPipeline
 * @see ConfigProfiles
 */
@DisplayName("Output directory integrity — zero empty dirs (RULE-048-04)")
class OutputDirectoryIntegrityTest {

    @TempDir
    Path tempDir;

    @ParameterizedTest(name = "[{index}] {0} has no empty dirs")
    @MethodSource("javaProfiles")
    @DisplayName("no empty directories after generation")
    void generate_profile_producesNoEmptyDirectories(
            String profileName) throws IOException {
        ProjectConfig config =
                ConfigProfiles.getStack(profileName);
        Path dest = tempDir.resolve(profileName);
        Files.createDirectories(dest);

        PipelineOptions options = new PipelineOptions(
                false, true, false, false, null,
                EnumSet.allOf(Platform.class));
        List<AssemblerDescriptor> assemblers =
                AssemblerFactory.buildAssemblers(options);
        new AssemblerPipeline(assemblers)
                .runPipeline(config, dest, options);

        List<Path> emptyDirs;
        try (Stream<Path> walk = Files.walk(dest)) {
            emptyDirs = walk.filter(Files::isDirectory)
                    .filter(OutputDirectoryIntegrityTest::isEmptyDir)
                    .toList();
        }

        assertThat(emptyDirs)
                .as("profile %s must not produce empty directories — "
                        + "bottom-up check (RULE-048-04)",
                        profileName)
                .isEmpty();
    }

    private static boolean isEmptyDir(Path dir) {
        try (Stream<Path> entries = Files.list(dir)) {
            return entries.findAny().isEmpty();
        } catch (IOException e) {
            return false;
        }
    }

    static Stream<String> javaProfiles() {
        return Stream.of(
                "java-spring",
                "java-quarkus",
                "java-spring-clickhouse",
                "java-spring-cqrs-es",
                "java-spring-elasticsearch",
                "java-spring-event-driven",
                "java-spring-fintech-pci",
                "java-spring-hexagonal",
                "java-spring-neo4j");
    }
}
