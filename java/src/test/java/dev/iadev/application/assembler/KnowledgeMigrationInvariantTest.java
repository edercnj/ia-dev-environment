package dev.iadev.application.assembler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Invariant test enforcing RULE-051-01 (source-of-truth única para KPs).
 *
 * <p>After STORY-0051-0002, Knowledge Packs live ONLY under
 * {@code targets/claude/knowledge/}. The old location
 * {@code targets/claude/skills/knowledge-packs/} MUST NOT exist.
 * If both locations coexist, the migration is corrupt and the build
 * fails immediately.</p>
 *
 * <p>Covers Gherkin scenario "dupla-fonte detectada (error path)" from
 * story-0051-0002 §6.</p>
 */
@DisplayName("KnowledgeMigrationInvariant")
class KnowledgeMigrationInvariantTest {

    private static final Path RESOURCES =
            Path.of("src/main/resources");
    private static final Path OLD_KP_DIR =
            RESOURCES.resolve(
                    "targets/claude/skills/knowledge-packs");
    private static final Path NEW_KP_DIR =
            RESOURCES.resolve("targets/claude/knowledge");

    @Test
    @DisplayName("oldKnowledgePacksDir_doesNotExist")
    void oldKnowledgePacksDir_doesNotExist() {
        assertThat(Files.exists(OLD_KP_DIR))
                .as("Old KP directory %s must be removed"
                                + " (RULE-051-01); found: %s",
                        OLD_KP_DIR, OLD_KP_DIR)
                .isFalse();
    }

    @Test
    @DisplayName("newKnowledgeDir_exists")
    void newKnowledgeDir_exists() {
        assertThat(Files.isDirectory(NEW_KP_DIR))
                .as("New knowledge dir %s must exist after"
                                + " migration (RULE-051-01)",
                        NEW_KP_DIR)
                .isTrue();
    }

    @Test
    @DisplayName("newKnowledgeDir_containsAtLeast32Packs")
    void newKnowledgeDir_containsAtLeast32Packs()
            throws IOException {
        try (Stream<Path> entries =
                Files.list(NEW_KP_DIR)) {
            long count = entries
                    .filter(p -> !p.getFileName()
                            .toString().startsWith("."))
                    .count();
            assertThat(count)
                    .as("RULE-051-04: at least 32 KPs must"
                            + " exist under "
                            + "targets/claude/knowledge/")
                    .isGreaterThanOrEqualTo(32);
        }
    }

    // Note: verifying that core skills no longer reference
    // old KP paths (`skills/knowledge-packs/...`) is the
    // responsibility of STORY-0051-0003 (retrofit). This
    // test only enforces the RULE-051-01 dual-source invariant
    // (old dir gone, new dir populated).
}
