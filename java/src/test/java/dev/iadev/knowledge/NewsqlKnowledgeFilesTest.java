package dev.iadev.knowledge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates NewSQL knowledge files existence, content,
 * and line budget compliance (RULE-001: max 300 lines).
 */
@DisplayName("NewSQL Knowledge Files")
class NewsqlKnowledgeFilesTest {

    private static final Path KNOWLEDGE_BASE =
            Path.of("src/main/resources/"
                    + "knowledge/databases/newsql");
    private static final int MAX_LINES = 300;

    @Nested
    @DisplayName("@GK-1: common/ directory")
    class CommonDirectory {

        @Test
        @DisplayName("newsql-principles.md exists")
        void commonDir_principlesFileExists() {
            Path file = KNOWLEDGE_BASE.resolve(
                    "common/newsql-principles.md");
            assertThat(file).exists();
        }

        @Test
        @DisplayName("common/ has exactly 1 file")
        void commonDir_hasExactlyOneFile()
                throws IOException {
            Path dir = KNOWLEDGE_BASE.resolve("common");
            long count;
            try (var stream = Files.list(dir)) {
                count = stream.count();
            }
            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("@GK-4: newsql-principles.md content")
    class PrinciplesContent {

        @Test
        @DisplayName("contains Raft or Paxos section")
        void principles_containsRaftOrPaxos()
                throws IOException {
            String content = readPrinciples();
            assertThat(content).satisfiesAnyOf(
                    c -> assertThat(c)
                            .containsIgnoringCase("raft"),
                    c -> assertThat(c)
                            .containsIgnoringCase("paxos"));
        }

        @Test
        @DisplayName("contains clock synchronization")
        void principles_containsClockSync()
                throws IOException {
            String content = readPrinciples();
            assertThat(content).satisfiesAnyOf(
                    c -> assertThat(c).containsIgnoringCase(
                            "clock synchronization"),
                    c -> assertThat(c).containsIgnoringCase(
                            "hybrid logical clock"));
        }

        @Test
        @DisplayName("contains distributed transactions")
        void principles_containsDistributedTx()
                throws IOException {
            String content = readPrinciples();
            assertThat(content).containsIgnoringCase(
                    "distributed transaction");
        }

        private String readPrinciples() throws IOException {
            return Files.readString(
                    KNOWLEDGE_BASE.resolve(
                            "common/newsql-principles.md"));
        }
    }

    @Nested
    @DisplayName("@GK-2: YugaByteDB files")
    class YugaByteDbFiles {

        private static final Path DB_DIR =
                KNOWLEDGE_BASE.resolve("yugabytedb");

        @Test
        @DisplayName("directory has exactly 3 files")
        void yugaDir_hasThreeFiles() throws IOException {
            assertDirectoryFileCount(DB_DIR, 3);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "types-and-conventions.md",
                "migration-patterns.md",
                "query-optimization.md"
        })
        @DisplayName("contains {0}")
        void yugaDir_containsFile(String filename) {
            assertThat(DB_DIR.resolve(filename)).exists();
        }
    }

    @Nested
    @DisplayName("@GK-3: CockroachDB files")
    class CockroachDbFiles {

        private static final Path DB_DIR =
                KNOWLEDGE_BASE.resolve("cockroachdb");

        @Test
        @DisplayName("directory has exactly 3 files")
        void cockroachDir_hasThreeFiles()
                throws IOException {
            assertDirectoryFileCount(DB_DIR, 3);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "types-and-conventions.md",
                "migration-patterns.md",
                "query-optimization.md"
        })
        @DisplayName("contains {0}")
        void cockroachDir_containsFile(String filename) {
            assertThat(DB_DIR.resolve(filename)).exists();
        }
    }

    @Nested
    @DisplayName("@GK-3: TiDB files")
    class TiDbFiles {

        private static final Path DB_DIR =
                KNOWLEDGE_BASE.resolve("tidb");

        @Test
        @DisplayName("directory has exactly 3 files")
        void tidbDir_hasThreeFiles() throws IOException {
            assertDirectoryFileCount(DB_DIR, 3);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "types-and-conventions.md",
                "migration-patterns.md",
                "query-optimization.md"
        })
        @DisplayName("contains {0}")
        void tidbDir_containsFile(String filename) {
            assertThat(DB_DIR.resolve(filename)).exists();
        }
    }

    @Nested
    @DisplayName("@GK-5: line budget (max 300)")
    class LineBudget {

        @ParameterizedTest
        @ValueSource(strings = {
                "common/newsql-principles.md",
                "yugabytedb/types-and-conventions.md",
                "yugabytedb/migration-patterns.md",
                "yugabytedb/query-optimization.md",
                "cockroachdb/types-and-conventions.md",
                "cockroachdb/migration-patterns.md",
                "cockroachdb/query-optimization.md",
                "tidb/types-and-conventions.md",
                "tidb/migration-patterns.md",
                "tidb/query-optimization.md"
        })
        @DisplayName("{0} has <= 300 lines")
        void file_withinLineBudget(String relativePath)
                throws IOException {
            Path file = KNOWLEDGE_BASE.resolve(relativePath);
            long lineCount = Files.lines(file).count();
            assertThat(lineCount)
                    .as("Line count for %s", relativePath)
                    .isLessThanOrEqualTo(MAX_LINES);
        }
    }

    private static void assertDirectoryFileCount(
            Path dir, int expected) throws IOException {
        long count;
        try (var stream = Files.list(dir)) {
            count = stream.count();
        }
        assertThat(count).isEqualTo(expected);
    }
}
