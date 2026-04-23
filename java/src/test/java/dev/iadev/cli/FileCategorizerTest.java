package dev.iadev.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the package-private FileCategorizer utility.
 * Coverage-completion tests added in EPIC-0050 to exercise
 * the short-circuit OR branches inside the isRootFile and
 * isInfraFile predicates (Rule 05 Quality Gates).
 */
@DisplayName("FileCategorizer")
class FileCategorizerTest {

    @Nested
    @DisplayName("normalizePath")
    class NormalizePath {

        @Test
        @DisplayName("replaces backslashes with"
                + " forward slashes")
        void normalizePath_backslash_replaced() {
            assertThat(FileCategorizer.normalizePath(
                    "a\\b\\c.md")).isEqualTo("a/b/c.md");
        }

        @Test
        @DisplayName("preserves already-normalized path")
        void normalizePath_alreadyForward_preserved() {
            assertThat(FileCategorizer.normalizePath(
                    "a/b/c.md")).isEqualTo("a/b/c.md");
        }
    }

    @Nested
    @DisplayName("categorize — root files")
    class RootFiles {

        @Test
        @DisplayName("CLAUDE.md -> Root Files")
        void claudeMd_rootFiles() {
            assertThat(FileCategorizer.categorize(
                    "CLAUDE.md")).isEqualTo("Root Files");
        }

        @Test
        @DisplayName("README.md -> Root Files")
        void readmeMd_rootFiles() {
            assertThat(FileCategorizer.categorize(
                    "README.md")).isEqualTo("Root Files");
        }

        @Test
        @DisplayName("AGENTS.md -> Root Files")
        void agentsMd_rootFiles() {
            assertThat(FileCategorizer.categorize(
                    "AGENTS.md")).isEqualTo("Root Files");
        }

        @Test
        @DisplayName("AGENTS.override.md -> Root Files")
        void agentsOverrideMd_rootFiles() {
            assertThat(FileCategorizer.categorize(
                    "AGENTS.override.md"))
                    .isEqualTo("Root Files");
        }

        @Test
        @DisplayName("CONSTITUTION.md -> Root Files")
        void constitutionMd_rootFiles() {
            assertThat(FileCategorizer.categorize(
                    "CONSTITUTION.md"))
                    .isEqualTo("Root Files");
        }
    }

    @Nested
    @DisplayName("categorize — infrastructure files")
    class InfraFiles {

        @Test
        @DisplayName("Dockerfile -> Infrastructure")
        void dockerfile_infrastructure() {
            assertThat(FileCategorizer.categorize(
                    "Dockerfile"))
                    .isEqualTo("Infrastructure");
        }

        @Test
        @DisplayName("docker-compose.yml -> Infrastructure")
        void dockerCompose_infrastructure() {
            assertThat(FileCategorizer.categorize(
                    "docker-compose.yml"))
                    .isEqualTo("Infrastructure");
        }

        @Test
        @DisplayName(".dockerignore -> Infrastructure")
        void dockerignore_infrastructure() {
            assertThat(FileCategorizer.categorize(
                    ".dockerignore"))
                    .isEqualTo("Infrastructure");
        }
    }

    @Nested
    @DisplayName("categorize — unknown falls back")
    class Fallback {

        @Test
        @DisplayName("unknown file -> Other")
        void unknownPath_other() {
            assertThat(FileCategorizer.categorize(
                    "random.txt")).isEqualTo("Other");
        }

        @Test
        @DisplayName(".claude/rules/23-model-selection.md"
                + " -> Rules")
        void clauderules_rules() {
            assertThat(FileCategorizer.categorize(
                    ".claude/rules/23-model-selection.md"))
                    .isEqualTo("Rules");
        }
    }
}
