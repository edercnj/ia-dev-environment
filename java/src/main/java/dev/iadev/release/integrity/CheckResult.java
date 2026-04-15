package dev.iadev.release.integrity;

import java.util.List;
import java.util.Objects;

/**
 * Result of a single integrity check.
 *
 * @param name   stable machine-readable check name (snake_case)
 * @param status PASS/WARN/FAIL
 * @param files  list of offending file references in {@code path:line} or {@code path} form;
 *               empty when {@code status == PASS}
 */
public record CheckResult(String name, CheckStatus status, List<String> files) {

    public CheckResult {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(files, "files");
        files = List.copyOf(files);
    }

    public static CheckResult pass(String name) {
        return new CheckResult(name, CheckStatus.PASS, List.of());
    }

    public static CheckResult fail(String name, List<String> files) {
        return new CheckResult(name, CheckStatus.FAIL, files);
    }

    public static CheckResult warn(String name, List<String> files) {
        return new CheckResult(name, CheckStatus.WARN, files);
    }
}
