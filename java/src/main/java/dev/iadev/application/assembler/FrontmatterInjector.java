package dev.iadev.application.assembler;

import dev.iadev.domain.model.ContextBudget;

/**
 * Injects computed fields into YAML frontmatter of skill
 * files.
 *
 * <p>The {@code context-budget} field is inserted after the
 * {@code argument-hint} line if present, or before the
 * closing {@code ---} delimiter otherwise.</p>
 *
 * @see ContextBudget
 */
final class FrontmatterInjector {

    private static final String FRONTMATTER_DELIM = "---";
    private static final String ARGUMENT_HINT =
            "argument-hint:";
    private static final String BUDGET_FIELD =
            "context-budget: ";

    private FrontmatterInjector() {
        // utility class
    }

    /**
     * Injects the {@code context-budget} field into skill
     * frontmatter.
     *
     * @param content the full SKILL.md content
     * @param budget  the computed context budget
     * @return the content with context-budget injected
     */
    static String injectContextBudget(
            String content, ContextBudget budget) {
        if (!hasFrontmatter(content)) {
            return content;
        }
        String budgetLine =
                BUDGET_FIELD + budget.value();
        return insertInFrontmatter(content, budgetLine);
    }

    private static boolean hasFrontmatter(String content) {
        return content.startsWith(FRONTMATTER_DELIM)
                && content.indexOf(
                        FRONTMATTER_DELIM, 3) > 0;
    }

    private static String insertInFrontmatter(
            String content, String budgetLine) {
        int closingIdx = content.indexOf(
                "\n" + FRONTMATTER_DELIM, 3);
        if (closingIdx < 0) {
            return content;
        }
        int hintIdx = content.indexOf(
                ARGUMENT_HINT);
        int insertPos;
        if (hintIdx >= 0 && hintIdx < closingIdx) {
            insertPos = content.indexOf(
                    '\n', hintIdx) + 1;
        } else {
            insertPos = closingIdx + 1;
        }
        return content.substring(0, insertPos)
                + budgetLine + "\n"
                + content.substring(insertPos);
    }

}
