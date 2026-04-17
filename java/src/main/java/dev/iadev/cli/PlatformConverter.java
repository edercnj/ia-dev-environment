package dev.iadev.cli;

import dev.iadev.domain.model.Platform;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

/**
 * Picocli type converter for {@link Platform} values.
 *
 * <p>Converts kebab-case CLI input (e.g., "claude-code")
 * to the corresponding {@link Platform} enum constant.
 * The special value "all" returns the
 * {@link Platform#ALL} sentinel to signal that no platform
 * filter should be applied (Rule 03 — never return null).</p>
 *
 * <p>Invalid values produce a
 * {@link TypeConversionException} with a message listing
 * all accepted values (RULE-005).</p>
 *
 * @see Platform#fromCliName(String)
 */
public final class PlatformConverter
        implements ITypeConverter<Platform> {

    private static final String ALL_KEYWORD = "all";

    private static final String ACCEPTED_VALUES =
            Platform.allUserSelectable().stream()
                    .map(Platform::cliName)
                    .sorted()
                    .collect(java.util.stream.Collectors
                            .joining(", "))
                    + ", all";

    /**
     * Converts a CLI string to a {@link Platform} value.
     *
     * <p>Trims whitespace before matching to tolerate
     * spaces around commas in comma-separated lists.</p>
     *
     * @param value the CLI input string
     * @return the matching {@link Platform}, or
     *         {@link Platform#ALL} when value equals "all"
     * @throws TypeConversionException if the value does
     *         not match any user-selectable platform
     */
    @Override
    public Platform convert(String value) {
        String normalized = value.trim();
        if (ALL_KEYWORD.equals(normalized)) {
            return Platform.ALL;
        }
        return Platform.fromCliName(normalized)
                .filter(Platform.allUserSelectable()::contains)
                .orElseThrow(
                        () -> buildException(normalized));
    }

    private TypeConversionException buildException(
            String value) {
        String message =
                ("Invalid platform: '%s'. "
                        + "Valid values: %s").formatted(
                        value, ACCEPTED_VALUES);
        return new TypeConversionException(message);
    }
}
