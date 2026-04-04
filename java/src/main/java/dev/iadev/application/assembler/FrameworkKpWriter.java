package dev.iadev.application.assembler;

import dev.iadev.domain.stack.StackPackMapping;
import dev.iadev.domain.stack.VersionResolver;
import dev.iadev.domain.model.ProjectConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Copies framework-specific knowledge pack files during
 * rules assembly.
 *
 * <p>Handles copying common framework files and
 * version-specific files for the appropriate stack pack
 * reference directory.</p>
 *
 * <p>Extracted from {@link RulesAssembler} per
 * story-0008-0014 to satisfy the 250-line SRP
 * constraint.</p>
 *
 * @see RulesAssembler
 * @see LanguageKpWriter
 */
public final class FrameworkKpWriter {

    private final Path resourcesDir;
    private final VersionResolver versionResolver;

    /**
     * Creates a FrameworkKpWriter with explicit resources
     * directory and version resolver.
     *
     * @param resourcesDir    the base resources directory
     * @param versionResolver the version resolver
     */
    FrameworkKpWriter(Path resourcesDir,
            VersionResolver versionResolver) {
        this.resourcesDir = resourcesDir;
        this.versionResolver = versionResolver;
    }

    /**
     * Copies framework-specific knowledge pack files.
     *
     * @param config    the project configuration
     * @param skillsDir the skills output directory
     * @return list of generated file paths
     */
    List<String> copyFrameworkKps(
            ProjectConfig config, Path skillsDir) {
        String fw = config.framework().name();
        String packName =
                StackPackMapping.getStackPackName(fw);
        if (packName.isEmpty()) {
            return List.of();
        }
        Path fwDir =
                resourcesDir.resolve("frameworks/" + fw);
        if (!Files.exists(fwDir)
                || !Files.isDirectory(fwDir)) {
            return List.of();
        }
        Path refsDir = skillsDir.resolve(
                packName + "/references");
        CopyHelpers.ensureDirectory(refsDir);
        List<String> generated = new ArrayList<>();
        generated.addAll(copyFwCommon(fwDir, refsDir));
        generated.addAll(
                copyFwVersion(config, fwDir, refsDir));
        return generated;
    }

    private List<String> copyFwCommon(
            Path fwDir, Path refsDir) {
        Path common = fwDir.resolve("common");
        if (!Files.exists(common)
                || !Files.isDirectory(common)) {
            return List.of();
        }
        List<String> generated = new ArrayList<>();
        List<Path> files =
                CopyHelpers.listMdFilesSorted(common);
        for (Path file : files) {
            generated.add(
                    CopyHelpers.copyStaticFile(
                            file,
                            refsDir.resolve(
                                    file.getFileName()
                                            .toString())));
        }
        return generated;
    }

    private List<String> copyFwVersion(
            ProjectConfig config,
            Path fwDir, Path refsDir) {
        Optional<Path> versionDir =
                versionResolver.findVersionDir(
                        fwDir,
                        config.framework().name(),
                        config.framework().version());
        if (versionDir.isEmpty()) {
            return List.of();
        }
        List<String> generated = new ArrayList<>();
        List<Path> files =
                CopyHelpers.listMdFilesSorted(
                        versionDir.get());
        for (Path file : files) {
            generated.add(
                    CopyHelpers.copyStaticFile(
                            file,
                            refsDir.resolve(
                                    file.getFileName()
                                            .toString())));
        }
        return generated;
    }
}
