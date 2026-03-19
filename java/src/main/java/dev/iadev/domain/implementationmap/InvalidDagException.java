package dev.iadev.domain.implementationmap;

/**
 * Thrown when the DAG is structurally invalid.
 *
 * <p>Examples: no root nodes, unresolvable references,
 * or other structural problems that prevent phase computation.</p>
 */
public class InvalidDagException extends RuntimeException {

    /**
     * Creates an invalid DAG exception with a descriptive message.
     *
     * @param message description of the structural problem
     */
    public InvalidDagException(String message) {
        super(message);
    }

    @Override
    public String toString() {
        return "InvalidDagException{message='%s'}"
                .formatted(getMessage());
    }
}
