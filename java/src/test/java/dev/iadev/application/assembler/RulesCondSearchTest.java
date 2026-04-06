package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for RulesConditionals — search engine
 * knowledge file copying.
 */
@DisplayName("RulesConditionals — Search Engines")
class RulesCondSearchTest {

    @Nested
    @DisplayName("copyDatabaseRefs — search engines")
    class CopyDatabaseRefsSearch {

        @Test
        @DisplayName(
                "elasticsearch copies search common + es files")
        void copyDatabaseRefs_elasticsearch_copiesFiles(
                @TempDir Path tempDir) throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("elasticsearch", "8")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path searchCommon = resourceDir.resolve(
                    "knowledge/databases/search/common");
            Files.createDirectories(searchCommon);
            Files.writeString(
                    searchCommon.resolve(
                            "search-principles.md"),
                    "Search principles content");
            Path esDir = resourceDir.resolve(
                    "knowledge/databases/search/"
                            + "elasticsearch");
            Files.createDirectories(esDir);
            Files.writeString(
                    esDir.resolve("modeling-patterns.md"),
                    "ES modeling");
            Files.writeString(
                    esDir.resolve("migration-patterns.md"),
                    "ES migration");
            Files.writeString(
                    esDir.resolve(
                            "query-optimization.md"),
                    "ES query optimization");
            Path skillsDir = tempDir.resolve("skills");
            List<String> result =
                    RulesConditionals.copyDatabaseRefs(
                            new ConditionalCopyContext(
                                    config, resourceDir,
                                    skillsDir,
                                    new TemplateEngine(),
                                    Map.of()));
            assertThat(result).hasSize(4);
            assertThat(result).anyMatch(
                    f -> f.contains(
                            "search-principles.md"));
            assertThat(result).anyMatch(
                    f -> f.contains(
                            "modeling-patterns.md"));
            assertThat(result).anyMatch(
                    f -> f.contains(
                            "migration-patterns.md"));
            assertThat(result).anyMatch(
                    f -> f.contains(
                            "query-optimization.md"));
        }

        @Test
        @DisplayName(
                "opensearch copies search common + os files")
        void copyDatabaseRefs_opensearch_copiesFiles(
                @TempDir Path tempDir) throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("opensearch", "2")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path searchCommon = resourceDir.resolve(
                    "knowledge/databases/search/common");
            Files.createDirectories(searchCommon);
            Files.writeString(
                    searchCommon.resolve(
                            "search-principles.md"),
                    "Search principles content");
            Path osDir = resourceDir.resolve(
                    "knowledge/databases/search/opensearch");
            Files.createDirectories(osDir);
            Files.writeString(
                    osDir.resolve("modeling-patterns.md"),
                    "OS modeling");
            Files.writeString(
                    osDir.resolve("migration-patterns.md"),
                    "OS migration");
            Files.writeString(
                    osDir.resolve(
                            "query-optimization.md"),
                    "OS query optimization");
            Path skillsDir = tempDir.resolve("skills");
            List<String> result =
                    RulesConditionals.copyDatabaseRefs(
                            new ConditionalCopyContext(
                                    config, resourceDir,
                                    skillsDir,
                                    new TemplateEngine(),
                                    Map.of()));
            assertThat(result).hasSize(4);
            assertThat(result).anyMatch(
                    f -> f.contains(
                            "search-principles.md"));
            assertThat(result).anyMatch(
                    f -> f.contains(
                            "modeling-patterns.md"));
        }

        @Test
        @DisplayName("search engines use modeling-patterns "
                + "not types-and-conventions")
        void copyDatabaseRefs_search_noTypesAndConventions(
                @TempDir Path tempDir) throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("elasticsearch", "8")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path searchCommon = resourceDir.resolve(
                    "knowledge/databases/search/common");
            Files.createDirectories(searchCommon);
            Files.writeString(
                    searchCommon.resolve(
                            "search-principles.md"),
                    "Search principles content");
            Path esDir = resourceDir.resolve(
                    "knowledge/databases/search/"
                            + "elasticsearch");
            Files.createDirectories(esDir);
            Files.writeString(
                    esDir.resolve("modeling-patterns.md"),
                    "ES modeling");
            Path skillsDir = tempDir.resolve("skills");
            List<String> result =
                    RulesConditionals.copyDatabaseRefs(
                            new ConditionalCopyContext(
                                    config, resourceDir,
                                    skillsDir,
                                    new TemplateEngine(),
                                    Map.of()));
            assertThat(result).noneMatch(
                    f -> f.contains(
                            "types-and-conventions.md"));
        }
    }
}
