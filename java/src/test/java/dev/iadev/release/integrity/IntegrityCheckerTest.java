package dev.iadev.release.integrity;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntegrityCheckerTest {

    @Nested
    @DisplayName("checkChangelogUnreleased")
    class Changelog {

        @Test
        @DisplayName("checkChangelogUnreleased_nullContent_returnsFail")
        void checkChangelogUnreleased_nullContent_returnsFail() {
            CheckResult r = IntegrityChecker.checkChangelogUnreleased(null);

            assertThat(r.status()).isEqualTo(CheckStatus.FAIL);
            assertThat(r.name()).isEqualTo(IntegrityChecker.CHECK_CHANGELOG_UNRELEASED);
            assertThat(r.files()).containsExactly("CHANGELOG.md");
        }

        @Test
        @DisplayName("checkChangelogUnreleased_emptyUnreleasedSection_returnsFail")
        void checkChangelogUnreleased_emptyUnreleasedSection_returnsFail() {
            String content = """
                    # Changelog

                    ## [Unreleased]

                    ### Added

                    ## [1.0.0] - 2025-01-01
                    - Initial release
                    """;

            CheckResult r = IntegrityChecker.checkChangelogUnreleased(content);

            assertThat(r.status()).isEqualTo(CheckStatus.FAIL);
            assertThat(r.files()).containsExactly("CHANGELOG.md");
        }

        @Test
        @DisplayName("checkChangelogUnreleased_sectionWithBullet_returnsPass")
        void checkChangelogUnreleased_sectionWithBullet_returnsPass() {
            String content = """
                    # Changelog

                    ## [Unreleased]

                    ### Added
                    - New integrity checker

                    ## [1.0.0] - 2025-01-01
                    """;

            CheckResult r = IntegrityChecker.checkChangelogUnreleased(content);

            assertThat(r.status()).isEqualTo(CheckStatus.PASS);
            assertThat(r.files()).isEmpty();
        }

        @Test
        @DisplayName("checkChangelogUnreleased_noUnreleasedHeader_returnsFail")
        void checkChangelogUnreleased_noUnreleasedHeader_returnsFail() {
            String content = "# Changelog\n\n## [1.0.0] - 2025-01-01\n- stuff\n";

            CheckResult r = IntegrityChecker.checkChangelogUnreleased(content);

            assertThat(r.status()).isEqualTo(CheckStatus.FAIL);
        }
    }

    @Nested
    @DisplayName("checkVersionAlignment")
    class VersionAlignment {

        @Test
        @DisplayName("checkVersionAlignment_nullTarget_returnsPass")
        void checkVersionAlignment_nullTarget_returnsPass() {
            CheckResult r = IntegrityChecker.checkVersionAlignment(null, Map.of());

            assertThat(r.status()).isEqualTo(CheckStatus.PASS);
        }

        @Test
        @DisplayName("checkVersionAlignment_allFilesAligned_returnsPass")
        void checkVersionAlignment_allFilesAligned_returnsPass() {
            Map<String, String> files = new LinkedHashMap<>();
            files.put("README.md", "[![version](badge-v3.1.0)](x)");
            files.put("CLAUDE.md", "Current version: 3.1.0");

            CheckResult r = IntegrityChecker.checkVersionAlignment("3.1.0-SNAPSHOT", files);

            assertThat(r.status()).isEqualTo(CheckStatus.PASS);
        }

        @Test
        @DisplayName("checkVersionAlignment_divergentBadge_returnsFailWithFileRef")
        void checkVersionAlignment_divergentBadge_returnsFailWithFileRef() {
            Map<String, String> files = new LinkedHashMap<>();
            files.put("README.md", "line1\nline2\n[![version](v3.0.0-badge)](x)\n");

            CheckResult r = IntegrityChecker.checkVersionAlignment("3.1.0", files);

            assertThat(r.status()).isEqualTo(CheckStatus.FAIL);
            assertThat(r.files()).hasSize(1);
            assertThat(r.files().get(0)).startsWith("README.md:").endsWith(":3");
        }

        @Test
        @DisplayName("checkVersionAlignment_fileWithoutSemver_isIgnored")
        void checkVersionAlignment_fileWithoutSemver_isIgnored() {
            Map<String, String> files = new LinkedHashMap<>();
            files.put("NOTES.md", "just prose, no version strings");

            CheckResult r = IntegrityChecker.checkVersionAlignment("3.1.0", files);

            assertThat(r.status()).isEqualTo(CheckStatus.PASS);
        }

        @Test
        @DisplayName("checkVersionAlignment_filesNull_throwsNpe")
        void checkVersionAlignment_filesNull_throwsNpe() {
            assertThatThrownBy(() -> IntegrityChecker.checkVersionAlignment("3.1.0", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("checkNoNewTodos")
    class NoNewTodos {

        @Test
        @DisplayName("checkNoNewTodos_emptyDiff_returnsPass")
        void checkNoNewTodos_emptyDiff_returnsPass() {
            CheckResult r = IntegrityChecker.checkNoNewTodos("");

            assertThat(r.status()).isEqualTo(CheckStatus.PASS);
        }

        @Test
        @DisplayName("checkNoNewTodos_newTodoInJavaFile_returnsWarn")
        void checkNoNewTodos_newTodoInJavaFile_returnsWarn() {
            String diff = """
                    diff --git a/src/main/java/Foo.java b/src/main/java/Foo.java
                    --- a/src/main/java/Foo.java
                    +++ b/src/main/java/Foo.java
                    @@ -1,3 +1,4 @@
                     public class Foo {
                    +    // TODO fix this later
                     }
                    """;

            CheckResult r = IntegrityChecker.checkNoNewTodos(diff);

            assertThat(r.status()).isEqualTo(CheckStatus.WARN);
            assertThat(r.files()).containsExactly("src/main/java/Foo.java");
        }

        @Test
        @DisplayName("checkNoNewTodos_documentedTodoParen_isExcluded")
        void checkNoNewTodos_documentedTodoParen_isExcluded() {
            String diff = """
                    diff --git a/src/main/java/Bar.java b/src/main/java/Bar.java
                    --- a/src/main/java/Bar.java
                    +++ b/src/main/java/Bar.java
                    @@ -1,3 +1,4 @@
                     class Bar {
                    +    // TODO(jira-123) add retry
                     }
                    """;

            CheckResult r = IntegrityChecker.checkNoNewTodos(diff);

            assertThat(r.status()).isEqualTo(CheckStatus.PASS);
        }

        @Test
        @DisplayName("checkNoNewTodos_todoInTestFile_isExcluded")
        void checkNoNewTodos_todoInTestFile_isExcluded() {
            String diff = """
                    diff --git a/src/test/java/FooTest.java b/src/test/java/FooTest.java
                    --- a/src/test/java/FooTest.java
                    +++ b/src/test/java/FooTest.java
                    @@ -1,3 +1,4 @@
                     class FooTest {
                    +    // FIXME brittle assertion
                     }
                    """;

            CheckResult r = IntegrityChecker.checkNoNewTodos(diff);

            assertThat(r.status()).isEqualTo(CheckStatus.PASS);
        }

        @Test
        @DisplayName("checkNoNewTodos_unsupportedExtension_isIgnored")
        void checkNoNewTodos_unsupportedExtension_isIgnored() {
            String diff = """
                    diff --git a/config.yaml b/config.yaml
                    --- a/config.yaml
                    +++ b/config.yaml
                    @@ -1,1 +1,2 @@
                     foo: bar
                    +# TODO update later
                    """;

            CheckResult r = IntegrityChecker.checkNoNewTodos(diff);

            assertThat(r.status()).isEqualTo(CheckStatus.PASS);
        }

        @Test
        @DisplayName("checkNoNewTodos_nullDiff_returnsPass")
        void checkNoNewTodos_nullDiff_returnsPass() {
            CheckResult r = IntegrityChecker.checkNoNewTodos(null);

            assertThat(r.status()).isEqualTo(CheckStatus.PASS);
        }
    }

    @Nested
    @DisplayName("run (aggregate)")
    class Run {

        @Test
        @DisplayName("run_allPass_returnsPassOverall")
        void run_allPass_returnsPassOverall() {
            Map<String, String> files = new LinkedHashMap<>();
            files.put("pom.xml", pom("3.1.0-SNAPSHOT"));
            files.put("README.md", "current release is v3.1.0");

            IntegrityReport report = IntegrityChecker.run(
                    "# Changelog\n\n## [Unreleased]\n- something new\n",
                    files,
                    "");

            assertThat(report.overallStatus()).isEqualTo(CheckStatus.PASS);
            assertThat(report.errorCode()).isEmpty();
            assertThat(report.checks()).hasSize(3);
        }

        @Test
        @DisplayName("run_emptyChangelogAndVersionDrift_returnsFailWithErrorCode")
        void run_emptyChangelogAndVersionDrift_returnsFailWithErrorCode() {
            Map<String, String> files = new LinkedHashMap<>();
            files.put("pom.xml", pom("3.1.0-SNAPSHOT"));
            files.put("README.md", "release v3.0.0 current");

            IntegrityReport report = IntegrityChecker.run(
                    "# Changelog\n\n## [Unreleased]\n\n## [1.0.0]\n- old\n",
                    files,
                    "");

            assertThat(report.overallStatus()).isEqualTo(CheckStatus.FAIL);
            assertThat(report.errorCode()).contains(IntegrityReport.ERROR_CODE);
        }

        @Test
        @DisplayName("run_warnOnlyDoesNotDemoteToFail")
        void run_warnOnlyDoesNotDemoteToFail() {
            Map<String, String> files = new LinkedHashMap<>();
            files.put("pom.xml", pom("3.1.0-SNAPSHOT"));
            files.put("README.md", "v3.1.0");

            String diffWithTodo = """
                    diff --git a/src/main/java/X.java b/src/main/java/X.java
                    --- a/src/main/java/X.java
                    +++ b/src/main/java/X.java
                    @@ -1,1 +1,2 @@
                     class X {}
                    +// TODO fix
                    """;

            IntegrityReport report = IntegrityChecker.run(
                    "## [Unreleased]\n- entry\n",
                    files,
                    diffWithTodo);

            assertThat(report.overallStatus()).isEqualTo(CheckStatus.WARN);
            assertThat(report.errorCode()).isEmpty();
        }

        @Test
        @DisplayName("run_versionedFilesNull_throwsNpe")
        void run_versionedFilesNull_throwsNpe() {
            assertThatThrownBy(() -> IntegrityChecker.run("cl", null, "")).isInstanceOf(NullPointerException.class);
        }

        private String pom(String version) {
            return "<?xml version=\"1.0\"?>\n"
                    + "<project>\n"
                    + "  <groupId>x</groupId>\n"
                    + "  <artifactId>y</artifactId>\n"
                    + "  <version>" + version + "</version>\n"
                    + "</project>\n";
        }
    }
}
