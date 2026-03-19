package dev.iadev.exception;

/**
 * Thrown when a YAML configuration file is syntactically invalid.
 *
 * <p>Preserves the original cause (e.g., SnakeYAML's scanner exception)
 * for complete stack trace diagnostics, and carries the file path
 * that failed to parse.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * try {
 *     yaml.load(input);
 * } catch (ScannerException e) {
 *     throw new ConfigParseException(
 *         "Failed to parse config", "config.yaml", e);
 * }
 * }</pre>
 */
public class ConfigParseException extends RuntimeException {

    private final String filePath;

    /**
     * Creates a config parse exception with file path and original cause.
     *
     * @param message  description of the parse failure
     * @param filePath path to the file that failed to parse
     * @param cause    the original parsing exception
     */
    public ConfigParseException(
            String message, String filePath, Throwable cause) {
        super(message, cause);
        this.filePath = filePath;
    }

    /**
     * Returns the path of the file that failed to parse.
     *
     * @return the file path
     */
    public String getFilePath() {
        return filePath;
    }

    @Override
    public String toString() {
        return "ConfigParseException{message='%s', filePath='%s'}"
                .formatted(getMessage(), filePath);
    }
}
