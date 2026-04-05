package dev.iadev.domain.qualitygate;

/**
 * A vague term detected in a Gherkin scenario step.
 *
 * @param term     the prohibited term that was matched
 * @param stepType which step contains the term (Given/When/Then)
 */
public record VagueTerm(String term, StepType stepType) {

    public VagueTerm {
        if (term == null || term.isBlank()) {
            throw new IllegalArgumentException(
                    "term must not be null or blank");
        }
        if (stepType == null) {
            throw new IllegalArgumentException(
                    "stepType must not be null");
        }
    }
}
