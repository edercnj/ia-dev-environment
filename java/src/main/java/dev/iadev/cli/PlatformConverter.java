package dev.iadev.cli;

import dev.iadev.domain.model.Platform;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

/**
 * Picocli type converter for {@link Platform} values.
 *
 * <p>Converts kebab-case CLI input (e.g., "claude-code")
 * to the corresponding {@link Platform} enum constant.
 * The special value "all" returns {@code null} to signal
 * that no platform filter should be applied.</p>
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
            "claude-code, copilot, codex, all";

    /**
     * Converts a CLI string to a {@link Platform} value.
     *
     * @param value the CLI input string
     * @return the matching {@link Platform}, or
     *         {@code null} when value equals "all"
     * @throws TypeConversionException if the value does
     *         not match any user-selectable platform
     */
    @Override
    public Platform convert(String value) {
        if (ALL_KEYWORD.equals(value)) {
            return null;
        }
        return Platform.fromCliName(value)
                .filter(Platform.allUserSelectable()::contains)
                .orElseThrow(() -> buildException(value));
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
