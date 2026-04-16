package dev.iadev.release.prompt;

/**
 * Thrown when the operator provides an unexpected response
 * to a halt-point prompt. Maps to exit code 1 with error
 * code {@code PROMPT_INVALID_RESPONSE} per
 * story-0039-0007 §5.3.
 */
public final class PromptInvalidResponseException
        extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private static final String ERROR_CODE =
            "PROMPT_INVALID_RESPONSE";

    public PromptInvalidResponseException(String response) {
        super(ERROR_CODE + ": unexpected response '"
                + response + "'");
    }

    public String errorCode() {
        return ERROR_CODE;
    }
}
