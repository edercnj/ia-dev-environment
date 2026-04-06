package dev.iadev.knowledge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates search engine knowledge files:
 * existence, line count, and required content.
 */
@DisplayName("Search Knowledge Files")
class SearchKnowledgeFilesTest {

    private static final int MAX_LINES = 300;
    private static final Path KNOWLEDGE_BASE =
            resolveKnowledgeBase();

    private static Path resolveKnowledgeBase() {
        Path current = Path.of("").toAbsolutePath();
        if (current.endsWith("java")) {
            return current.resolve(
                    "src/main/resources/knowledge"
                            + "/databases/search");
        }
        return current.resolve(
                "java/src/main/resources/knowledge"
                        + "/databases/search");
    }

    @Nested
    @DisplayName("common/search-principles.md")
    class SearchPrinciples {

        private final Path file = KNOWLEDGE_BASE
                .resolve("common/search-principles.md");

        @Test
        @DisplayName("file exists")
        void searchPrinciples_exists() {
            assertThat(file).exists();
        }

        @Test
        @DisplayName("does not exceed 300 lines")
        void searchPrinciples_lineCount_withinBudget()
                throws IOException {
            long lines = Files.lines(
                    file, StandardCharsets.UTF_8).count();
            assertThat(lines)
                    .as("search-principles.md line count")
                    .isLessThanOrEqualTo(MAX_LINES);
        }

        @Test
        @DisplayName("contains inverted index section")
        void searchPrinciples_containsInvertedIndex()
                throws IOException {
            String content = Files.readString(
                    file, StandardCharsets.UTF_8)
                    .toLowerCase();
            assertThat(content).contains("inverted index");
        }

        @Test
        @DisplayName("contains relevance scoring section")
        void searchPrinciples_containsRelevanceScoring()
                throws IOException {
            String content = Files.readString(
                    file, StandardCharsets.UTF_8)
                    .toLowerCase();
            assertThat(content).satisfiesAnyOf(
                    c -> assertThat(c)
                            .contains("relevance scoring"),
                    c -> assertThat(c).contains("bm25"));
        }

        @Test
        @DisplayName("contains analysis pipeline section")
        void searchPrinciples_containsAnalysisPipeline()
                throws IOException {
            String content = Files.readString(
                    file, StandardCharsets.UTF_8)
                    .toLowerCase();
            assertThat(content).satisfiesAnyOf(
                    c -> assertThat(c)
                            .contains("analysis pipeline"),
                    c -> assertThat(c)
                            .contains("analyzers"));
        }
    }

    @Nested
    @DisplayName("elasticsearch/ — 3 files")
    class ElasticsearchFiles {

        private final Path esDir = KNOWLEDGE_BASE
                .resolve("elasticsearch");

        @Test
        @DisplayName("directory contains exactly 3 files")
        void elasticsearch_directoryHasThreeFiles()
                throws IOException {
            assertThat(esDir).isDirectory();
            long count = Files.list(esDir)
                    .filter(Files::isRegularFile)
                    .count();
            assertThat(count).isEqualTo(3);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "modeling-patterns.md",
                "migration-patterns.md",
                "query-optimization.md"
        })
        @DisplayName("file exists: {0}")
        void elasticsearch_fileExists(String filename) {
            assertThat(esDir.resolve(filename)).exists();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "modeling-patterns.md",
                "migration-patterns.md",
                "query-optimization.md"
        })
        @DisplayName("{0} does not exceed 300 lines")
        void elasticsearch_lineCount_withinBudget(
                String filename) throws IOException {
            long lines = Files.lines(
                    esDir.resolve(filename),
                    StandardCharsets.UTF_8).count();
            assertThat(lines)
                    .as(filename + " line count")
                    .isLessThanOrEqualTo(MAX_LINES);
        }
    }

    @Nested
    @DisplayName("opensearch/ — 3 files")
    class OpensearchFiles {

        private final Path osDir = KNOWLEDGE_BASE
                .resolve("opensearch");

        @Test
        @DisplayName("directory contains exactly 3 files")
        void opensearch_directoryHasThreeFiles()
                throws IOException {
            assertThat(osDir).isDirectory();
            long count = Files.list(osDir)
                    .filter(Files::isRegularFile)
                    .count();
            assertThat(count).isEqualTo(3);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "modeling-patterns.md",
                "migration-patterns.md",
                "query-optimization.md"
        })
        @DisplayName("file exists: {0}")
        void opensearch_fileExists(String filename) {
            assertThat(osDir.resolve(filename)).exists();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "modeling-patterns.md",
                "migration-patterns.md",
                "query-optimization.md"
        })
        @DisplayName("{0} does not exceed 300 lines")
        void opensearch_lineCount_withinBudget(
                String filename) throws IOException {
            long lines = Files.lines(
                    osDir.resolve(filename),
                    StandardCharsets.UTF_8).count();
            assertThat(lines)
                    .as(filename + " line count")
                    .isLessThanOrEqualTo(MAX_LINES);
        }
    }
}
