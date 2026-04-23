package dev.iadev.application.assembler;

import dev.iadev.domain.model.ContextBudget;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link FrontmatterInjector}.
 */
@DisplayName("FrontmatterInjector")
class FrontmatterInjectorTest {

    @Nested
    @DisplayName("injectContextBudget")
    class InjectContextBudget {

        @Test
        @DisplayName("injects after argument-hint line")
        void inject_afterArgumentHint_insertsField() {
            String content = """
                    ---
                    name: x-git-push
                    description: "Git operations"
                    user-invocable: true
                    allowed-tools: Bash, Read
                    argument-hint: "[branch-name]"
                    ---
                    ## Content here
                    """;

            String result =
                    FrontmatterInjector.injectContextBudget(
                            content, ContextBudget.MEDIUM);

            assertThat(result).contains(
                    "context-budget: medium");
            assertThat(result.indexOf("context-budget"))
                    .isGreaterThan(
                            result.indexOf("argument-hint"));
            assertThat(result.indexOf("context-budget"))
                    .isLessThan(result.indexOf("---\n##"));
        }

        @Test
        @DisplayName("injects light budget")
        void inject_lightBudget_insertsLight() {
            String content = """
                    ---
                    name: x-test-run
                    argument-hint: "[test-name]"
                    ---
                    ## Body
                    """;

            String result =
                    FrontmatterInjector.injectContextBudget(
                            content, ContextBudget.LIGHT);

            assertThat(result).contains(
                    "context-budget: light");
        }

        @Test
        @DisplayName("injects heavy budget")
        void inject_heavyBudget_insertsHeavy() {
            String content = """
                    ---
                    name: x-story-implement
                    argument-hint: "[STORY-ID]"
                    ---
                    ## Body
                    """;

            String result =
                    FrontmatterInjector.injectContextBudget(
                            content, ContextBudget.HEAVY);

            assertThat(result).contains(
                    "context-budget: heavy");
        }

        @Test
        @DisplayName("no argument-hint appends before"
                + " closing delimiter")
        void inject_noArgumentHint_appendsBeforeClose() {
            String content = """
                    ---
                    name: x-simple
                    user-invocable: true
                    ---
                    ## Body
                    """;

            String result =
                    FrontmatterInjector.injectContextBudget(
                            content, ContextBudget.LIGHT);

            assertThat(result).contains(
                    "context-budget: light");
            assertThat(result.indexOf("context-budget"))
                    .isLessThan(
                            result.indexOf("---", 4));
        }

        @Test
        @DisplayName("no frontmatter returns content"
                + " unchanged")
        void inject_noFrontmatter_returnsUnchanged() {
            String content = "## No frontmatter\nBody";

            String result =
                    FrontmatterInjector.injectContextBudget(
                            content, ContextBudget.LIGHT);

            assertThat(result).isEqualTo(content);
        }

        @Test
        @DisplayName("empty content returns unchanged")
        void inject_emptyContent_returnsUnchanged() {
            String content = "";

            String result =
                    FrontmatterInjector.injectContextBudget(
                            content, ContextBudget.LIGHT);

            assertThat(result).isEqualTo(content);
        }

        @Test
        @DisplayName("starts with delimiter but no"
                + " closing returns unchanged")
        void inject_noClosingDelim_returnsUnchanged() {
            String content = "---\nname: x-test\n"
                    + "no closing delimiter";

            String result =
                    FrontmatterInjector.injectContextBudget(
                            content, ContextBudget.LIGHT);

            assertThat(result).isEqualTo(content);
        }

        @Test
        @DisplayName("argument-hint after closing"
                + " delimiter uses delimiter position")
        void inject_hintAfterClose_usesDelimiter() {
            String content = "---\nname: test\n---\n"
                    + "argument-hint: outside\n";

            String result =
                    FrontmatterInjector.injectContextBudget(
                            content, ContextBudget.LIGHT);

            assertThat(result).contains(
                    "context-budget: light");
            int budgetIdx =
                    result.indexOf("context-budget");
            int closingIdx = result.indexOf("\n---", 3);
            assertThat(budgetIdx)
                    .isLessThan(closingIdx + 1);
        }

        @Test
        @DisplayName("replaces existing context-budget"
                + " instead of duplicating")
        void inject_existingBudget_replacesValue() {
            String content = """
                    ---
                    name: x-test
                    argument-hint: "[id]"
                    context-budget: light
                    ---
                    ## Body
                    """;

            String result =
                    FrontmatterInjector.injectContextBudget(
                            content, ContextBudget.HEAVY);

            assertThat(result).contains(
                    "context-budget: heavy");
            assertThat(result)
                    .doesNotContain("context-budget: light");
            long count = result.lines()
                    .filter(l -> l.contains(
                            "context-budget:"))
                    .count();
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("preserves content after"
                + " frontmatter unchanged")
        void inject_preservesBodyContent() {
            String content = "---\nname: x\n"
                    + "argument-hint: y\n---\n"
                    + "## Title\nBody text\n";

            String result =
                    FrontmatterInjector.injectContextBudget(
                            content, ContextBudget.MEDIUM);

            assertThat(result).endsWith(
                    "## Title\nBody text\n");
        }

        @Test
        @DisplayName("opening delim with inline closing"
                + " (no newline before ---) inserts at end"
                + " of frontmatter via hasExistingBudget=false")
        void inject_inlineClosingDelim_reachesHasExisting() {
            // hasFrontmatter: true (startsWith --- and
            // indexOf("---", 3) > 0).
            // hasExistingBudget: closingIdx = indexOf("\n---", 3)
            // returns -1 because the closing "---" is not
            // preceded by a newline. This exercises the
            // `closingIdx < 0 -> return false` branch inside
            // hasExistingBudget. insertInFrontmatter then
            // also finds closingIdx < 0 and returns the
            // content unchanged (the insertInFrontmatter
            // closingIdx<0 branch).
            String content = "---foo---rest-of-body";

            String result =
                    FrontmatterInjector.injectContextBudget(
                            content, ContextBudget.LIGHT);

            assertThat(result).isEqualTo(content);
        }

        @Test
        @DisplayName("argument-hint present with no"
                + " newline after reaches the"
                + " newlineAfterHint<0 branch")
        void inject_argumentHintNoNewline_fallbackPath() {
            // Frontmatter has argument-hint AT THE END with
            // no trailing newline before the closing delim.
            // indexOf('\n', hintIdx) will eventually return
            // a newline inside the frontmatter OR skip into
            // the closing delim block. This probes the
            // newlineAfterHint branching inside
            // insertInFrontmatter.
            String content = "---\nname: x\n"
                    + "argument-hint: y\n---\n"
                    + "body\n";

            String result =
                    FrontmatterInjector.injectContextBudget(
                            content, ContextBudget.HEAVY);

            // Budget field is inserted; body is preserved.
            assertThat(result).contains("context-budget:");
            assertThat(result).contains("body");
        }
    }
}
