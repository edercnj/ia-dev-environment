package dev.iadev.domain.implementationmap;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownParserTest {

    @Nested
    class EmptyInput {

        @Test
        void parse_nullInput_returnsEmptyList() {
            var result = MarkdownParser.parse(null);

            assertThat(result).isEmpty();
        }

        @Test
        void parse_emptyString_returnsEmptyList() {
            var result = MarkdownParser.parse("");

            assertThat(result).isEmpty();
        }

        @Test
        void parse_blankString_returnsEmptyList() {
            var result = MarkdownParser.parse("   \n  ");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class SimpleTable {

        @Test
        void parse_singleRootStory_returnsOneRow() {
            var markdown = """
                    | ID | Title | Blocked By |
                    | :--- | :--- | :--- |
                    | story-001 | Root Story | - |
                    """;

            var result = MarkdownParser.parse(markdown);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().storyId())
                    .isEqualTo("story-001");
            assertThat(result.getFirst().title())
                    .isEqualTo("Root Story");
            assertThat(result.getFirst().blockedBy()).isEmpty();
        }

        @Test
        void parse_twoRows_oneRootOneDep_returnsBoth() {
            var markdown = """
                    | ID | Title | Blocked By |
                    | :--- | :--- | :--- |
                    | story-001 | Root | \u2014 |
                    | story-002 | Dep | story-001 |
                    """;

            var result = MarkdownParser.parse(markdown);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).blockedBy()).isEmpty();
            assertThat(result.get(1).blockedBy())
                    .containsExactly("story-001");
        }

        @Test
        void parse_emDash_treatedAsNoDependent() {
            var markdown = """
                    | ID | Title | Blocked By |
                    | :--- | :--- | :--- |
                    | story-001 | Root | \u2014 |
                    """;

            var result = MarkdownParser.parse(markdown);

            assertThat(result.getFirst().blockedBy()).isEmpty();
        }
    }

    @Nested
    class MultipleDependencies {

        @Test
        void parse_commaSeparated_allParsed() {
            var markdown = """
                    | ID | Title | Blocked By |
                    | :--- | :--- | :--- |
                    | story-005 | Multi | story-001, story-002, story-003 |
                    """;

            var result = MarkdownParser.parse(markdown);

            assertThat(result.getFirst().blockedBy())
                    .containsExactly(
                            "story-001", "story-002", "story-003");
        }
    }

    @Nested
    class FiveStories {

        @Test
        void parse_fiveStoriesWithDependencies_correctRowCount() {
            var markdown = buildFiveStoryMarkdown();

            var result = MarkdownParser.parse(markdown);

            assertThat(result).hasSize(5);
        }

        @Test
        void parse_fiveStories_rootsHaveEmptyBlockedBy() {
            var markdown = buildFiveStoryMarkdown();

            var result = MarkdownParser.parse(markdown);

            var roots = result.stream()
                    .filter(r -> r.blockedBy().isEmpty())
                    .toList();
            assertThat(roots).hasSize(2);
            assertThat(roots.stream().map(
                    DependencyMatrixRow::storyId))
                    .containsExactlyInAnyOrder(
                            "story-001", "story-002");
        }

        @Test
        void parse_fiveStories_dependentsHaveCorrectBlockedBy() {
            var markdown = buildFiveStoryMarkdown();

            var result = MarkdownParser.parse(markdown);

            var story003 = result.stream()
                    .filter(r -> r.storyId().equals("story-003"))
                    .findFirst().orElseThrow();
            assertThat(story003.blockedBy())
                    .containsExactly("story-001", "story-002");

            var story004 = result.stream()
                    .filter(r -> r.storyId().equals("story-004"))
                    .findFirst().orElseThrow();
            assertThat(story004.blockedBy())
                    .containsExactly("story-001");

            var story005 = result.stream()
                    .filter(r -> r.storyId().equals("story-005"))
                    .findFirst().orElseThrow();
            assertThat(story005.blockedBy())
                    .containsExactly("story-003", "story-004");
        }

        private String buildFiveStoryMarkdown() {
            return """
                    | ID | Title | Blocked By |
                    | :--- | :--- | :--- |
                    | story-001 | Root A | - |
                    | story-002 | Root B | - |
                    | story-003 | Middle AB | story-001, story-002 |
                    | story-004 | Middle A | story-001 |
                    | story-005 | Final | story-003, story-004 |
                    """;
        }
    }

    @Nested
    class MalformedInput {

        @Test
        void parse_noTable_returnsEmptyList() {
            var markdown = """
                    # Implementation Map

                    Some text without any table.
                    """;

            var result = MarkdownParser.parse(markdown);

            assertThat(result).isEmpty();
        }

        @Test
        void parse_insufficientColumns_skipsRow() {
            var markdown = """
                    | ID | Title | Blocked By |
                    | :--- | :--- | :--- |
                    | story-001 | Only two columns |
                    | story-002 | Valid | - |
                    """;

            var result = MarkdownParser.parse(markdown);

            // Row with only 2 cells should be skipped
            assertThat(result).hasSizeLessThanOrEqualTo(1);
        }
    }
}
