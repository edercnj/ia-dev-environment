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
                    name: x-dev-lifecycle
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
    }
}
