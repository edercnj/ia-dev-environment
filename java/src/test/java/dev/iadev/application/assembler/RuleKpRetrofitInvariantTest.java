package dev.iadev.application.assembler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Grep-enforced invariant for RULE-051-03 on rules/ tree.
 *
 * <p>After STORY-0051-0004, no rule file under
 * {@code targets/claude/rules/} may reference KPs via the
 * old {@code skills/{kp}/SKILL.md} path pattern.</p>
 */
@DisplayName("RuleKpRetrofitInvariant")
class RuleKpRetrofitInvariantTest {

    private static final Path RULES_DIR =
            Path.of("src/main/resources/targets/claude/"
                    + "rules");

    private static final List<String> KP_NAMES = List.of(
            "api-design", "architecture",
            "architecture-cqrs", "architecture-hexagonal",
            "architecture-patterns", "ci-cd-patterns",
            "coding-standards", "compliance",
            "data-management", "data-modeling",
            "database-patterns", "ddd-strategic",
            "disaster-recovery", "feature-flags", "finops",
            "infra-patterns", "infrastructure",
            "layer-templates", "observability",
            "owasp-asvs", "parallelism-heuristics",
            "patterns-outbox", "pci-dss-requirements",
            "performance-engineering", "protocols",
            "release-management", "resilience", "security",
            "sre-practices", "stack-patterns",
            "story-planning", "testing");

    private static final Pattern OLD_KP_REF =
            Pattern.compile(
                    "skills/("
                            + String.join("|", KP_NAMES)
                            + ")/(SKILL\\.md|references/)");

    @Test
    @DisplayName("noOldKpReferences_inRules")
    void noOldKpReferences_inRules() throws IOException {
        if (!Files.isDirectory(RULES_DIR)) {
            return;
        }
        List<Path> offenders;
        try (Stream<Path> files = Files.walk(RULES_DIR)) {
            offenders = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName()
                            .toString().endsWith(".md"))
                    .filter(RuleKpRetrofitInvariantTest
                            ::containsOldKpReference)
                    .toList();
        }
        assertThat(offenders)
                .as("RULE-051-03: no rule may reference"
                        + " the old skills/{kp}/ path —"
                        + " found: %s", offenders)
                .isEmpty();
    }

    private static boolean containsOldKpReference(
            Path file) {
        try {
            String content = Files.readString(file);
            return OLD_KP_REF.matcher(content).find();
        } catch (IOException e) {
            return false;
        }
    }
}
