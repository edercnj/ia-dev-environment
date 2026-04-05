package dev.iadev.application.assembler;

import dev.iadev.domain.stack.VersionResolver;
import dev.iadev.domain.model.ProjectConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Copies language-specific knowledge pack files during
 * rules assembly.
 *
 * <p>Handles copying common language files and
 * version-specific files for coding standards and testing
 * reference directories.</p>
 *
 * <p>Extracted from {@link RulesAssembler} per
 * story-0008-0014 to satisfy the 250-line SRP
 * constraint.</p>
 *
 * @see RulesAssembler
 * @see FrameworkKpWriter
 */
public final class LanguageKpWriter {

    private final Path resourcesDir;
    private final VersionResolver versionResolver;

    /**
     * Creates a LanguageKpWriter with explicit resources
     * directory and version resolver.
     *
     * @param resourcesDir    the base resources directory
     * @param versionResolver the version resolver
     */
    LanguageKpWriter(Path resourcesDir,
            VersionResolver versionResolver) {
        this.resourcesDir = resourcesDir;
        this.versionResolver = versionResolver;
    }

    /**
     * Copies language-specific knowledge pack files.
     *
     * @param config    the project configuration
     * @param skillsDir the skills output directory
     * @return list of generated file paths
     */
    List<String> copyLanguageKps(
            ProjectConfig config, Path skillsDir) {
        String lang = config.language().name();
        Path langDir =
                resourcesDir.resolve("knowledge/languages/" + lang);
        if (!Files.exists(langDir)
                || !Files.isDirectory(langDir)) {
            return List.of();
        }
        Path codingRefs = skillsDir.resolve(
                "coding-standards/references");
        Path testingRefs = skillsDir.resolve(
                "testing/references");
        CopyHelpers.ensureDirectory(codingRefs);
        CopyHelpers.ensureDirectory(testingRefs);
        List<String> generated = new ArrayList<>();
        generated.addAll(
                copyLangCommon(langDir,
                        codingRefs, testingRefs));
        generated.addAll(
                copyLangVersion(config,
                        langDir, codingRefs));
        return generated;
    }

    private List<String> copyLangCommon(
            Path langDir,
            Path codingRefs, Path testingRefs) {
        Path common = langDir.resolve("common");
        if (!Files.exists(common)
                || !Files.isDirectory(common)) {
            return List.of();
        }
        List<String> generated = new ArrayList<>();
        List<Path> files =
                CopyHelpers.listMdFilesSorted(common);
        for (Path file : files) {
            String name =
                    file.getFileName().toString();
            Path dest = name.contains("testing")
                    ? testingRefs : codingRefs;
            generated.add(
                    CopyHelpers.copyStaticFile(
                            file,
                            dest.resolve(name)));
        }
        return generated;
    }

    private List<String> copyLangVersion(
            ProjectConfig config,
            Path langDir, Path codingRefs) {
        Optional<Path> versionDir =
                versionResolver.findVersionDir(
                        langDir,
                        config.language().name(),
                        config.language().version());
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
                            codingRefs.resolve(
                                    file.getFileName()
                                            .toString())));
        }
        return generated;
    }
}
