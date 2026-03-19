package dev.iadev.domain.implementationmap;

/**
 * Thrown when the markdown input is malformed and cannot be parsed.
 *
 * <p>Examples: missing dependency table, invalid table format,
 * or unparseable cell content.</p>
 */
public class MapParseException extends RuntimeException {

    /**
     * Creates a map parse exception with a descriptive message.
     *
     * @param message description of the parse failure
     */
    public MapParseException(String message) {
        super(message);
    }

    @Override
    public String toString() {
        return "MapParseException{message='%s'}"
                .formatted(getMessage());
    }
}
