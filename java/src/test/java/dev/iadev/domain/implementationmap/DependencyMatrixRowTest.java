package dev.iadev.domain.implementationmap;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DependencyMatrixRowTest {

    @Nested
    class Creation {

        @Test
        void create_rootRow_blockedByIsEmpty() {
            var row = new DependencyMatrixRow(
                    "story-001", "Root Story",
                    Optional.empty(), List.of());

            assertThat(row.storyId()).isEqualTo("story-001");
            assertThat(row.title()).isEqualTo("Root Story");
            assertThat(row.jiraKey()).isEmpty();
            assertThat(row.blockedBy()).isEmpty();
        }

        @Test
        void create_dependentRow_blockedByContainsIds() {
            var row = new DependencyMatrixRow(
                    "story-005", "Dependent",
                    Optional.empty(),
                    List.of("story-001", "story-002"));

            assertThat(row.blockedBy())
                    .containsExactly("story-001", "story-002");
        }

        @Test
        void create_withJiraKey_jiraKeyPresent() {
            var row = new DependencyMatrixRow(
                    "story-001", "Root Story",
                    Optional.of("PROJ-123"), List.of());

            assertThat(row.jiraKey())
                    .isPresent()
                    .hasValue("PROJ-123");
        }

        @Test
        void create_withNullJiraKey_defaultsToEmpty() {
            var row = new DependencyMatrixRow(
                    "story-001", "Root Story",
                    null, List.of());

            assertThat(row.jiraKey()).isEmpty();
        }

        @Test
        void create_defensiveCopy_originalListModificationDoesNotAffect() {
            var deps = new ArrayList<>(List.of("story-001"));
            var row = new DependencyMatrixRow(
                    "story-005", "Title",
                    Optional.empty(), deps);

            deps.add("story-002");

            assertThat(row.blockedBy())
                    .containsExactly("story-001");
        }

        @Test
        void create_immutableBlockedBy_throwsOnModification() {
            var row = new DependencyMatrixRow(
                    "story-005", "Title",
                    Optional.empty(),
                    List.of("story-001"));

            assertThatThrownBy(
                    () -> row.blockedBy().add("story-002"))
                    .isInstanceOf(
                            UnsupportedOperationException.class);
        }
    }
}
