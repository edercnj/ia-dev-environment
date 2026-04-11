package dev.iadev.util;

/**
 * Centralized JSON utility methods for manual JSON
 * construction. Implements escaping per RFC 8259 Section 7.
 *
 * <p>Replaces duplicated {@code escapeJson} and
 * {@code indent} methods previously in
 * {@code SettingsAssembler}.</p>
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc8259#section-7">
 *     RFC 8259 Section 7</a>
 */
public final class JsonHelpers {

    private JsonHelpers() {
        // utility class — not instantiable
    }

    /**
     * Returns an indentation string of
     * {@code level * 2} spaces.
     *
     * @param level indentation level (&gt;= 0)
     * @return indentation string
     * @throws IllegalArgumentException if level is negative
     */
    public static String indent(int level) {
        if (level < 0) {
            throw new IllegalArgumentException(
                    "Indent level must be >= 0, got: %d"
                            .formatted(level));
        }
        return "  ".repeat(level);
    }

    /**
     * Escapes a string value according to RFC 8259
     * Section 7 (Strings).
     *
     * <p>Handles the following characters:</p>
     * <ul>
     *   <li>{@code "} (quotation mark)</li>
     *   <li>{@code \} (reverse solidus)</li>
     *   <li>{@code \n} (newline)</li>
     *   <li>{@code \r} (carriage return)</li>
     *   <li>{@code \t} (tab)</li>
     *   <li>{@code \b} (backspace)</li>
     *   <li>{@code \f} (form feed)</li>
     *   <li>U+0000 to U+001F (control characters)</li>
     * </ul>
     *
     * @param value input string (may be null)
     * @return escaped string safe for JSON embedding;
     *         empty string if null
     */
    public static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        var sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            appendEscaped(sb, value.charAt(i));
        }
        return sb.toString();
    }

    private static void appendEscaped(
            StringBuilder sb, char ch) {
        switch (ch) {
            case '"' -> sb.append("\\\"");
            case '\\' -> sb.append("\\\\");
            case '\n' -> sb.append("\\n");
            case '\r' -> sb.append("\\r");
            case '\t' -> sb.append("\\t");
            case '\b' -> sb.append("\\b");
            case '\f' -> sb.append("\\f");
            default -> appendDefault(sb, ch);
        }
    }

    private static void appendDefault(
            StringBuilder sb, char ch) {
        if (ch < 0x20) {
            sb.append("\\u%04x".formatted((int) ch));
        } else {
            sb.append(ch);
        }
    }
}
