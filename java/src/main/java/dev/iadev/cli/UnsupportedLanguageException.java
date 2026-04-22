package dev.iadev.cli;

/**
 * Thrown when a language outside the supported set is requested.
 *
 * <p>Since EPIC-0048 (v4.0.0) the generator is restricted to Java
 * only (see ADR-0048-A). Attempting to generate with any other
 * language — including {@code python}, {@code go}, {@code kotlin},
 * {@code typescript}, {@code rust}, or {@code csharp} — results
 * in this exception with the RULE-048-06 canonical message.
 *
 * <p>Extends {@link IllegalArgumentException} so that the existing
 * {@link GenerateCommand} exception-to-exit-code mapping translates
 * it to a validation error (exit 1) without additional plumbing.
 *
 * @see dev.iadev.cli.LanguageFrameworkMapping#LANGUAGES
 */
public final class UnsupportedLanguageException
        extends IllegalArgumentException {

    private static final String MESSAGE_TEMPLATE =
            "Language '%s' is not supported. Only 'java' "
                    + "is available (see CHANGELOG v4.0.0"
                    + " / EPIC-0048).";

    private final String attemptedLanguage;

    /**
     * Creates an exception for the given attempted language.
     *
     * @param attemptedLanguage the language value rejected
     *        (may be empty string but not null; callers are
     *        expected to pre-handle null as "use default")
     */
    public UnsupportedLanguageException(
            String attemptedLanguage) {
        super(MESSAGE_TEMPLATE.formatted(
                attemptedLanguage == null
                        ? ""
                        : attemptedLanguage));
        this.attemptedLanguage =
                attemptedLanguage == null
                        ? ""
                        : attemptedLanguage;
    }

    /**
     * Returns the language string that was rejected.
     *
     * @return the original attempted language, or empty
     *         string if the caller passed {@code null}
     */
    public String attemptedLanguage() {
        return attemptedLanguage;
    }
}
