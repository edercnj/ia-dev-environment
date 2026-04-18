package dev.iadev.release.integrity;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regex-based version extractor for {@link IntegrityChecker}.
 *
 * <p>Uses regex rather than a DOM/SAX parser to avoid XXE risk
 * (OWASP A05; Story 0039-0003 §Escalation Notes TASK-010).</p>
 */
final class VersionExtractor {

    // First <version> element found after the opening <project> tag, tolerating whitespace.
    private static final Pattern POM_VERSION = Pattern.compile(
            "(?s)<project[^>]*>.*?<version>\\s*([^<\\s]+)\\s*</version>");

    // <parent>...</parent> block: stripped before matching POM_VERSION so the parent's
    // own <version> does not shadow the project's <version> in inheritance scenarios.
    private static final Pattern POM_PARENT_BLOCK = Pattern.compile(
            "(?s)<parent>.*?</parent>");

    // Semantic version: X.Y.Z optionally prefixed by 'v', optionally suffixed.
    static final Pattern SEMVER = Pattern.compile(
            "\\bv?(\\d+\\.\\d+\\.\\d+)(?:-[A-Za-z0-9.]+)?\\b");

    private VersionExtractor() {
        throw new AssertionError("no instances");
    }

    static Optional<String> extractPomVersion(Map<String, String> files) {
        for (Map.Entry<String, String> e : files.entrySet()) {
            if (e.getKey() != null && e.getKey().endsWith("pom.xml") && e.getValue() != null) {
                Optional<String> version = extractPomVersionFromContent(e.getValue());
                if (version.isPresent()) {
                    return version;
                }
            }
        }
        return Optional.empty();
    }

    static Optional<String> extractPomVersionFromContent(String pomContent) {
        String stripped = POM_PARENT_BLOCK.matcher(pomContent).replaceAll("");
        Matcher m = POM_VERSION.matcher(stripped);
        if (m.find()) {
            return Optional.of(m.group(1));
        }
        return Optional.empty();
    }

    /**
     * Strip leading {@code v}/{@code V} and any suffix after the first hyphen (e.g., -SNAPSHOT).
     */
    static String normalize(String version) {
        String trimmed = version.trim();
        if (trimmed.startsWith("v") || trimmed.startsWith("V")) {
            trimmed = trimmed.substring(1);
        }
        int dashIdx = trimmed.indexOf('-');
        if (dashIdx > 0) {
            trimmed = trimmed.substring(0, dashIdx);
        }
        return trimmed;
    }

    static int lineNumberOfOffset(String content, int offset) {
        int line = 1;
        for (int i = 0; i < offset && i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }
}
