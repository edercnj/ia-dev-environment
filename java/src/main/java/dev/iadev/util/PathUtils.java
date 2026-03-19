package dev.iadev.util;

import dev.iadev.exception.CliException;

import java.nio.file.Path;
import java.util.Set;

/**
 * Path safety validation and normalization utilities.
 *
 * <p>Enforces RULE-011 (Dangerous Path Rejection) and RULE-009
 * (Cross-Platform Path Handling) by using {@link java.nio.file.Path}
 * exclusively and rejecting writes to dangerous system directories.
 *
 * <p>Dangerous paths include the user's home directory, filesystem
 * root, and standard system directories ({@code /usr}, {@code /etc},
 * {@code /var}, {@code /bin}, {@code /sbin}).
 *
 * <p>Example usage:
 * <pre>{@code
 * Path dest = PathUtils.normalizeDirectory("./output");
 * PathUtils.rejectDangerousPath(dest);
 * PathUtils.validateDestPath(dest);
 * }</pre>
 *
 * @see dev.iadev.exception.CliException
 */
public final class PathUtils {

    private static final String ROOT_PATH = "/";

    private static final Set<String> DANGEROUS_SYSTEM_PATHS = Set.of(
            "/usr", "/etc", "/var", "/bin", "/sbin"
    );

    private PathUtils() {
        // Utility class — no instantiation
    }

    /**
     * Resolves a path string to an absolute, normalized {@link Path}.
     *
     * <p>Relative paths are resolved against the current working
     * directory. Path components like {@code ..} and {@code .} are
     * normalized away.
     *
     * @param path the path string to normalize (may be relative)
     * @return an absolute, normalized {@link Path}
     */
    public static Path normalizeDirectory(String path) {
        return Path.of(path).toAbsolutePath().normalize();
    }

    /**
     * Validates that a path is not a dangerous system directory.
     *
     * <p>Rejects the user's home directory, filesystem root, and
     * standard system directories: {@code /usr}, {@code /etc},
     * {@code /var}, {@code /bin}, {@code /sbin}.
     *
     * @param path the path to validate (should be absolute)
     * @throws CliException with errorCode 1 if the path is dangerous
     */
    public static void rejectDangerousPath(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        String pathStr = normalized.toString();

        Path homePath = Path.of(
                System.getProperty("user.home")).toAbsolutePath()
                .normalize();
        if (normalized.equals(homePath)) {
            throw new CliException(
                    ("Rejected dangerous destination path: %s "
                            + "(home directory — risk of overwriting "
                            + "personal configuration files)")
                            .formatted(pathStr),
                    1);
        }

        Path rootPath = Path.of(ROOT_PATH)
                .toAbsolutePath().normalize();
        if (normalized.equals(rootPath)) {
            throw new CliException(
                    ("Rejected dangerous destination path: %s "
                            + "(protected system directory)")
                            .formatted(pathStr),
                    1);
        }

        for (String dangerous : DANGEROUS_SYSTEM_PATHS) {
            Path dangerousPath = Path.of(dangerous)
                    .toAbsolutePath().normalize();
            if (normalized.equals(dangerousPath)) {
                throw new CliException(
                        ("Rejected dangerous destination path: %s "
                                + "(protected system directory)")
                                .formatted(pathStr),
                        1);
            }
        }
    }

    /**
     * Combines normalization and dangerous path rejection.
     *
     * <p>Normalizes the path, rejects dangerous destinations, and
     * returns the validated path. This is the recommended entry point
     * for validating user-supplied destination paths.
     *
     * @param path the path to validate (may be relative)
     * @throws CliException with errorCode 1 if the path is dangerous
     */
    public static void validateDestPath(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        rejectDangerousPath(normalized);
    }
}
