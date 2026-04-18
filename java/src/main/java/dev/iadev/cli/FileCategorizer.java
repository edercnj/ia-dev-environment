package dev.iadev.cli;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Categorizes file paths into display categories based on
 * path prefixes.
 *
 * <p>Extracted from {@link CliDisplay} to keep both
 * classes under 250 lines per RULE-004.</p>
 *
 * @see CliDisplay
 */
final class FileCategorizer {

    /**
     * Ordered rule table mapping path predicates to their
     * display category. Iterated in insertion order so the
     * first matching predicate wins — matching the original
     * if/else ladder semantics.
     */
    private static final Map<Predicate<String>, String>
            CATEGORY_RULES = buildCategoryRules();

    private FileCategorizer() {
        // utility class
    }

    /**
     * Normalizes path separators to forward slashes.
     *
     * @param path the file path to normalize
     * @return the normalized path
     */
    static String normalizePath(String path) {
        return path.replace('\\', '/');
    }

    /**
     * Categorizes a normalized file path into a display
     * category.
     *
     * @param path the normalized file path
     * @return the category name
     */
    static String categorize(String path) {
        for (Map.Entry<Predicate<String>, String> entry
                : CATEGORY_RULES.entrySet()) {
            if (entry.getKey().test(path)) {
                return entry.getValue();
            }
        }
        return "Other";
    }

    private static Map<Predicate<String>, String>
            buildCategoryRules() {
        Map<Predicate<String>, String> rules =
                new LinkedHashMap<>();
        rules.put(prefix(".claude/rules/"), "Rules");
        rules.put(prefix(".claude/skills/"), "Skills");
        rules.put(prefix(".claude/agents/"), "Agents");
        rules.put(prefix(".claude/hooks/"), "Hooks");
        rules.put(prefix(".claude/settings"), "Settings");
        rules.put(prefix("steering/"), "Steering");
        rules.put(prefix("specs/"), "Specs");
        rules.put(prefix("results/"), "Results");
        rules.put(prefix("contracts/"), "Contracts");
        rules.put(prefix("adr/"), "ADR");
        rules.put(prefix("plans/"), "Plans");
        rules.put(prefix("k8s/"), "Kubernetes");
        rules.put(prefix("tests/"), "Tests");
        rules.put(prefix(".claude/templates/"), "Templates");
        rules.put(FileCategorizer::isRootFile, "Root Files");
        rules.put(FileCategorizer::isInfraFile,
                "Infrastructure");
        return rules;
    }

    private static Predicate<String> prefix(String p) {
        return path -> path.startsWith(p);
    }

    private static boolean isRootFile(String path) {
        return "CLAUDE.md".equals(path)
                || "README.md".equals(path)
                || "AGENTS.md".equals(path)
                || "AGENTS.override.md".equals(path)
                || "CONSTITUTION.md".equals(path);
    }

    private static boolean isInfraFile(String path) {
        return "Dockerfile".equals(path)
                || "docker-compose.yml".equals(path)
                || ".dockerignore".equals(path);
    }
}
