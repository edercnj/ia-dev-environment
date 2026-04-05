package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReviewChecklistSections")
class ReviewChecklistSectionsTest {

    @Nested
    @DisplayName("buildRubricRows()")
    class BuildRubricRows {

        @Test
        @DisplayName("no conditionals returns empty string")
        void buildRubricRows_noConditionals_empty() {
            String result = ReviewChecklistSections
                    .buildRubricRows(false, false, false);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("event-driven adds section L row")
        void buildRubricRows_eventDriven_hasSectionL() {
            String result = ReviewChecklistSections
                    .buildRubricRows(true, false, false);

            assertThat(result)
                    .contains("L. Event-Driven Review")
                    .contains("| 8");
        }

        @Test
        @DisplayName("pci-dss adds section M row")
        void buildRubricRows_pciDss_hasSectionM() {
            String result = ReviewChecklistSections
                    .buildRubricRows(false, true, false);

            assertThat(result)
                    .contains("M. PCI-DSS")
                    .contains("| 7");
        }

        @Test
        @DisplayName("lgpd adds section N row")
        void buildRubricRows_lgpd_hasSectionN() {
            String result = ReviewChecklistSections
                    .buildRubricRows(false, false, true);

            assertThat(result)
                    .contains("N. LGPD")
                    .contains("| 4");
        }

        @Test
        @DisplayName("all conditionals adds all rows")
        void buildRubricRows_all_hasAllSections() {
            String result = ReviewChecklistSections
                    .buildRubricRows(true, true, true);

            assertThat(result)
                    .contains("L. Event-Driven Review")
                    .contains("M. PCI-DSS")
                    .contains("N. LGPD");
        }
    }

    @Nested
    @DisplayName("buildDetailedCriteria()")
    class BuildDetailedCriteria {

        @Test
        @DisplayName("no conditionals returns empty string")
        void buildCriteria_noConditionals_empty() {
            String result = ReviewChecklistSections
                    .buildDetailedCriteria(
                            false, false, false);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("event-driven includes 8 criteria")
        void buildCriteria_eventDriven_has8Criteria() {
            String result = ReviewChecklistSections
                    .buildDetailedCriteria(
                            true, false, false);

            assertThat(result)
                    .contains("Section L")
                    .contains("Consumer idempotency")
                    .contains("Dead letter strategy")
                    .contains("Schema evolution")
                    .contains("Transactional outbox");
        }

        @Test
        @DisplayName("pci-dss includes 7 criteria")
        void buildCriteria_pciDss_has7Criteria() {
            String result = ReviewChecklistSections
                    .buildDetailedCriteria(
                            false, true, false);

            assertThat(result)
                    .contains("Section M")
                    .contains("Card data never logged")
                    .contains("Encryption in transit")
                    .contains("Tokenization")
                    .contains("Penetration tests");
        }

        @Test
        @DisplayName("lgpd includes 4 criteria")
        void buildCriteria_lgpd_has4Criteria() {
            String result = ReviewChecklistSections
                    .buildDetailedCriteria(
                            false, false, true);

            assertThat(result)
                    .contains("Section N")
                    .contains("Consent traceable")
                    .contains("Personal data deletion")
                    .contains("Processing operations log")
                    .contains("Anonymization");
        }

        @Test
        @DisplayName("all conditionals has all sections")
        void buildCriteria_all_hasAllSections() {
            String result = ReviewChecklistSections
                    .buildDetailedCriteria(
                            true, true, true);

            assertThat(result)
                    .contains("Section L")
                    .contains("Section M")
                    .contains("Section N");
        }
    }
}
