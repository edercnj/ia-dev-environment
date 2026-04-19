package dev.iadev.application.assembler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SkillCategoryResolver}.
 *
 * <p>Exercises the category-aware source layout (EPIC-0036)
 * with a synthetic on-disk fixture: flat skills, one level
 * of category nesting, the {@code lib/} prefix exception,
 * and the unknown-skill fallback.</p>
 */
@DisplayName("SkillCategoryResolver — source path resolution")
class SkillCategoryResolverTest {

    @Test
    @DisplayName("listSkills — flat skill under root is"
            + " discovered by its directory name")
    void listSkills_whenFlatSkill_returnsName(
            @TempDir Path tempDir) throws IOException {
        Path root = tempDir.resolve("core");
        writeSkill(root.resolve("x-flat"));

        List<String> skills =
                SkillCategoryResolver.listSkills(root);

        assertThat(skills).containsExactly("x-flat");
    }

    @Test
    @DisplayName("listSkills — skill inside a category dir"
            + " is emitted as the bare name (flat output)")
    void listSkills_whenCategorySkill_stripsCategory(
            @TempDir Path tempDir) throws IOException {
        Path root = tempDir.resolve("core");
        writeSkill(root.resolve("plan/x-epic-create"));
        writeSkill(root.resolve("dev/x-task-implement"));

        List<String> skills =
                SkillCategoryResolver.listSkills(root);

        assertThat(skills)
                .containsExactlyInAnyOrder(
                        "x-epic-create",
                        "x-task-implement");
    }

    @Test
    @DisplayName("listSkills — lib/ subtree keeps its"
            + " lib/ prefix in the emitted name")
    void listSkills_whenLibSkill_keepsLibPrefix(
            @TempDir Path tempDir) throws IOException {
        Path root = tempDir.resolve("core");
        writeSkill(root.resolve("lib/x-lib-tool"));

        List<String> skills =
                SkillCategoryResolver.listSkills(root);

        assertThat(skills).containsExactly(
                "lib/x-lib-tool");
    }

    @Test
    @DisplayName("listSkills — missing root returns empty"
            + " list (no exception)")
    void listSkills_whenRootMissing_returnsEmpty(
            @TempDir Path tempDir) {
        Path missing = tempDir.resolve("does-not-exist");

        List<String> skills =
                SkillCategoryResolver.listSkills(missing);

        assertThat(skills).isEmpty();
    }

    @Test
    @DisplayName("resolveSourcePath — flat skill resolves"
            + " to rootDir/{name}")
    void resolveSourcePath_whenFlatSkill_resolvesDirect(
            @TempDir Path tempDir) throws IOException {
        Path root = tempDir.resolve("core");
        Path expected = root.resolve("x-flat");
        writeSkill(expected);

        Path resolved =
                SkillCategoryResolver.resolveSourcePath(
                        root, "x-flat");

        assertThat(resolved).isEqualTo(expected);
    }

    @Test
    @DisplayName("resolveSourcePath — category skill"
            + " resolves through its category folder")
    void resolveSourcePath_whenCategorySkill_findsInCategory(
            @TempDir Path tempDir) throws IOException {
        Path root = tempDir.resolve("core");
        Path expected =
                root.resolve("plan/x-epic-create");
        writeSkill(expected);

        Path resolved =
                SkillCategoryResolver.resolveSourcePath(
                        root, "x-epic-create");

        assertThat(resolved).isEqualTo(expected);
    }

    @Test
    @DisplayName("resolveSourcePath — lib/ prefix resolves"
            + " through the lib subtree")
    void resolveSourcePath_whenLibPrefixed_resolvesInLib(
            @TempDir Path tempDir) throws IOException {
        Path root = tempDir.resolve("core");
        Path expected = root.resolve("lib/x-lib-tool");
        writeSkill(expected);

        Path resolved =
                SkillCategoryResolver.resolveSourcePath(
                        root, "lib/x-lib-tool");

        assertThat(resolved).isEqualTo(expected);
    }

    @Test
    @DisplayName("resolveSourcePath — unknown skill falls"
            + " back to rootDir/{name} (non-existent)")
    void resolveSourcePath_whenUnknownSkill_fallsBackToFlat(
            @TempDir Path tempDir) throws IOException {
        Path root = tempDir.resolve("core");
        Files.createDirectories(root);

        Path resolved =
                SkillCategoryResolver.resolveSourcePath(
                        root, "x-does-not-exist");

        assertThat(resolved)
                .isEqualTo(root.resolve("x-does-not-exist"));
        assertThat(Files.exists(resolved)).isFalse();
    }

    private static void writeSkill(Path dir)
            throws IOException {
        Files.createDirectories(dir);
        Files.writeString(
                dir.resolve("SKILL.md"),
                "---\nname: "
                        + dir.getFileName().toString()
                        + "\n---\n",
                StandardCharsets.UTF_8);
    }
}
