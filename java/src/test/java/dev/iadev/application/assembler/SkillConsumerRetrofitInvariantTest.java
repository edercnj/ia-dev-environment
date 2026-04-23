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
 * Grep-enforced invariant for RULE-051-03 (canonical
 * knowledge path).
 *
 * <p>After STORY-0051-0003, no SKILL.md under
 * {@code targets/claude/skills/core/} may reference KPs via
 * the old {@code skills/{kp}/SKILL.md} path pattern. All
 * references must use the new
 * {@code knowledge/{kp}.md} (simple) or
 * {@code knowledge/{kp}/index.md} (complex) paths.</p>
 */
@DisplayName("SkillConsumerRetrofitInvariant")
class SkillConsumerRetrofitInvariantTest {

    private static final Path CORE_SKILLS =
            Path.of("src/main/resources/targets/claude/"
                    + "skills/core");

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
    @DisplayName("noOldKpReferences_inCoreSkills")
    void noOldKpReferences_inCoreSkills()
            throws IOException {
        if (!Files.isDirectory(CORE_SKILLS)) {
            return;
        }
        List<Path> offenders;
        try (Stream<Path> files = Files.walk(CORE_SKILLS)) {
            offenders = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName()
                            .toString().equals("SKILL.md"))
                    .filter(SkillConsumerRetrofitInvariantTest
                            ::containsOldKpReference)
                    .toList();
        }
        assertThat(offenders)
                .as("RULE-051-03: no core SKILL.md may"
                        + " reference the old"
                        + " skills/{kp}/SKILL.md or"
                        + " skills/{kp}/references/ path —"
                        + " found: %s", offenders)
                .isEmpty();
    }

    private static boolean containsOldKpReference(
            Path skillMd) {
        try {
            String content = Files.readString(skillMd);
            return OLD_KP_REF.matcher(content).find();
        } catch (IOException e) {
            return false;
        }
    }
}
