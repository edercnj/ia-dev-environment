package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates {@code .agents/skills/} from already-generated
 * {@code .claude/skills/}.
 *
 * <p>Operates in 2 phases:
 * <ol>
 *   <li>Scan {@code .claude/skills/} (already generated
 *       by {@link SkillsAssembler})</li>
 *   <li>Copy each skill to {@code .agents/skills/{name}/}
 *       with {@code SKILL.md} and optional
 *       {@code references/} subdirectory</li>
 * </ol>
 *
 * <p>This is the nineteenth assembler in the pipeline
 * (position 19 of 23 per RULE-005). Its target is
 * {@link AssemblerTarget#CODEX_AGENTS}.</p>
 *
 * @see Assembler
 */
public final class CodexSkillsAssembler
        implements Assembler {

    private static final String SKILL_MD = "SKILL.md";
    private static final String REFERENCES_DIR = "references";

    /**
     * {@inheritDoc}
     *
     * <p>Copies skills from {@code .claude/skills/} to
     * {@code .agents/skills/}.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        List<String> warnings = new ArrayList<>();

        Path claudeSkillsDir = outputDir.getParent()
                .resolve(".claude").resolve("skills");

        if (!CodexShared.isAccessibleDirectory(
                claudeSkillsDir)) {
            warnings.add(
                    "No skills directory found"
                            + " in .claude/ output");
            return List.of();
        }

        Path destSkillsDir =
                outputDir.resolve("skills");
        List<String> files =
                copySkillsTree(
                        claudeSkillsDir, destSkillsDir);

        if (files.isEmpty()) {
            warnings.add(
                    "No skills with SKILL.md found"
                            + " in .claude/skills/");
        }

        return files;
    }

    /**
     * Walks a skills directory, copying each skill
     * subdirectory.
     *
     * @param sourceDir the source skills directory
     * @param destDir   the destination skills directory
     * @return list of copied file paths
     */
    static List<String> copySkillsTree(
            Path sourceDir, Path destDir) {
        List<String> files = new ArrayList<>();
        List<Path> entries = sortedDirectories(sourceDir);

        for (Path entry : entries) {
            Path srcChild = entry;
            Path destChild = destDir.resolve(
                    entry.getFileName().toString());
            Path srcSkillMd = srcChild.resolve(SKILL_MD);

            if (Files.exists(srcSkillMd)) {
                files.addAll(
                        copySkill(srcChild, destChild));
            } else if (CodexShared.isAccessibleDirectory(
                    srcChild)) {
                files.addAll(
                        copySkillsTree(
                                srcChild, destChild));
            }
        }

        return files;
    }

    /**
     * Copies a skill directory ({@code SKILL.md} +
     * {@code references/}) to destination.
     *
     * @param srcSkillDir  the source skill directory
     * @param destSkillDir the destination skill directory
     * @return list of copied file paths
     */
    static List<String> copySkill(
            Path srcSkillDir, Path destSkillDir) {
        List<String> files = new ArrayList<>();
        Path srcSkillMd = srcSkillDir.resolve(SKILL_MD);
        if (!Files.exists(srcSkillMd)) {
            return files;
        }

        CopyHelpers.ensureDirectory(destSkillDir);

        Path destSkillMd = destSkillDir.resolve(SKILL_MD);
        copyFile(srcSkillMd, destSkillMd);
        files.add(destSkillMd.toString());

        Path srcRefs =
                srcSkillDir.resolve(REFERENCES_DIR);
        if (CodexShared.isAccessibleDirectory(srcRefs)) {
            Path destRefs =
                    destSkillDir.resolve(REFERENCES_DIR);
            CopyHelpers.copyDirectory(srcRefs, destRefs);
            files.addAll(collectFiles(destRefs));
        }

        return files;
    }

    /**
     * Recursively collects all file paths under a directory.
     *
     * @param dir the directory to scan
     * @return list of absolute file paths
     */
    static List<String> collectFiles(Path dir) {
        List<String> results = new ArrayList<>();
        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    results.add(entry.toString());
                } else if (Files.isDirectory(entry)) {
                    results.addAll(collectFiles(entry));
                }
            }
        } catch (IOException e) {
            return results;
        }
        return results;
    }

    private static List<Path> sortedDirectories(
            Path parent) {
        List<Path> entries = new ArrayList<>();
        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(parent)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    entries.add(entry);
                }
            }
        } catch (IOException e) {
            return entries;
        }
        entries.sort((a, b) -> a.getFileName().toString()
                .compareTo(b.getFileName().toString()));
        return entries;
    }

    private static void copyFile(Path src, Path dest) {
        try {
            Files.copy(src, dest,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to copy file: " + src, e);
        }
    }
}
