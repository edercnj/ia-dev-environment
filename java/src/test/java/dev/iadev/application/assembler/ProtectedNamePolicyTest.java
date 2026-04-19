package dev.iadev.application.assembler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ProtectedNamePolicy}.
 *
 * <p>Extracted alongside the helper in audit remediation
 * M-002 so the "reserved top-level names" policy has a
 * dedicated test surface separate from the
 * {@code SkillsAssemblerPruneTest} integration coverage.</p>
 */
@DisplayName("ProtectedNamePolicy — reserved names policy")
class ProtectedNamePolicyTest {

    @Test
    @DisplayName("knowledge-packs is reserved")
    void isProtected_whenKnowledgePacks_returnsTrue() {
        assertThat(ProtectedNamePolicy
                .isProtected("knowledge-packs"))
                .isTrue();
    }

    @Test
    @DisplayName("database-patterns is reserved")
    void isProtected_whenDatabasePatterns_returnsTrue() {
        assertThat(ProtectedNamePolicy
                .isProtected("database-patterns"))
                .isTrue();
    }

    @Test
    @DisplayName("arbitrary skill name is not reserved")
    void isProtected_whenArbitrarySkillName_returnsFalse() {
        assertThat(ProtectedNamePolicy
                .isProtected("x-task-implement"))
                .isFalse();
    }

    @Test
    @DisplayName("empty string is not reserved")
    void isProtected_whenEmptyString_returnsFalse() {
        assertThat(ProtectedNamePolicy.isProtected(""))
                .isFalse();
    }

    @Test
    @DisplayName("case-sensitive — uppercase variant is"
            + " NOT reserved")
    void isProtected_whenUppercaseVariant_returnsFalse() {
        assertThat(ProtectedNamePolicy
                .isProtected("Knowledge-Packs"))
                .isFalse();
    }

    @Test
    @DisplayName("reserved set exposes exactly the two"
            + " documented entries")
    void protectedNames_containsExactlyCanonicalSet() {
        assertThat(ProtectedNamePolicy.PROTECTED_NAMES)
                .containsExactlyInAnyOrder(
                        "knowledge-packs",
                        "database-patterns");
    }
}
