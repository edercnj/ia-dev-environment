package dev.iadev.exception;

/**
 * Thrown when I/O operations on checkpoint files fail (read, write, delete).
 *
 * <p>Preserves the file path and the original I/O cause for diagnostics.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * try {
 *     Files.readString(checkpointPath);
 * } catch (IOException e) {
 *     throw new CheckpointIOException(
 *         "Failed to read checkpoint", checkpointPath.toString(), e);
 * }
 * }</pre>
 */
public class CheckpointIOException extends RuntimeException {

    private final String filePath;

    /**
     * Creates a checkpoint I/O exception with file path and original cause.
     *
     * @param message  description of the I/O failure
     * @param filePath path to the checkpoint file
     * @param cause    the original I/O exception
     */
    public CheckpointIOException(
            String message, String filePath, Throwable cause) {
        super(message, cause);
        this.filePath = filePath;
    }

    /**
     * Returns the path of the checkpoint file that caused the error.
     *
     * @return the file path
     */
    public String getFilePath() {
        return filePath;
    }

    @Override
    public String toString() {
        return "CheckpointIOException{message='%s', filePath='%s'}"
                .formatted(getMessage(), filePath);
    }
}
