package dev.iadev.domain.qualitygate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Detects vague or prohibited terms in Gherkin scenario steps.
 *
 * <p>Maintains a list of prohibited terms (case-insensitive)
 * and checks each Given/When/Then step for matches.</p>
 */
public final class VaguenessDetector {

    private static final List<String> PROHIBITED_TERMS = List.of(
            "funciona corretamente",
            "funciona bem",
            "resultado esperado",
            "resultado correto",
            "configurado corretamente",
            "faz algo",
            "realiza operacao",
            "o sistema",
            "o usuario",
            "dados validos",
            "dados invalidos",
            "erro apropriado",
            "mensagem adequada"
    );

    private VaguenessDetector() {
    }

    /**
     * Checks a single text for vague terms.
     *
     * @param text     the step text to check
     * @param stepType which step type this text belongs to
     * @return list of detected vague terms (empty if clean)
     */
    public static List<VagueTerm> check(
            String text, StepType stepType) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        var lower = text.toLowerCase(Locale.ROOT);
        var found = new ArrayList<VagueTerm>();
        for (var term : PROHIBITED_TERMS) {
            if (lower.contains(term)) {
                found.add(new VagueTerm(term, stepType));
            }
        }
        return List.copyOf(found);
    }

    /**
     * Checks all steps of a scenario for vague terms.
     *
     * @param scenario the Gherkin scenario to check
     * @return list of all detected vague terms
     */
    public static List<VagueTerm> checkScenario(
            GherkinScenario scenario) {
        var allTerms = new ArrayList<VagueTerm>();
        for (var line : scenario.givenLines()) {
            allTerms.addAll(check(line, StepType.GIVEN));
        }
        for (var line : scenario.whenLines()) {
            allTerms.addAll(check(line, StepType.WHEN));
        }
        for (var line : scenario.thenLines()) {
            allTerms.addAll(check(line, StepType.THEN));
        }
        return List.copyOf(allTerms);
    }

    /**
     * Returns the list of prohibited terms.
     *
     * @return unmodifiable list of prohibited terms
     */
    public static List<String> prohibitedTerms() {
        return PROHIBITED_TERMS;
    }
}
