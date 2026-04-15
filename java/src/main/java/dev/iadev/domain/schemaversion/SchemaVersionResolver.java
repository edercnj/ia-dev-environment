package dev.iadev.domain.schemaversion;

import dev.iadev.domain.schemaversion.SchemaVersionResolution.FallbackReason;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads {@code plans/epic-XXXX/execution-state.json} and resolves the
 * {@link PlanningSchemaVersion} per the matrix in EPIC-0038 story-0038-0008:
 *
 * <ul>
 *   <li>File absent -&gt; V1, {@link FallbackReason#NO_FILE}</li>
 *   <li>File present, field missing -&gt; V1, {@link FallbackReason#MISSING_FIELD}</li>
 *   <li>Field equals {@code "2.0"} -&gt; V2 (no fallback)</li>
 *   <li>Field equals {@code "1.0"} -&gt; V1 (no fallback)</li>
 *   <li>Field holds any other value -&gt; V1, {@link FallbackReason#INVALID_VALUE}</li>
 * </ul>
 *
 * <p>Only a hard-fail condition is unparseable JSON: {@link UncheckedIOException} is
 * thrown so orchestrators can abort with a clear diagnostic (distinct from the soft
 * fallback to V1).</p>
 *
 * <p>The resolver uses a zero-dependency regex lookup to avoid pulling a JSON parser into
 * the domain layer — the field is a string at the top level with a narrow grammar, so a
 * regex suffices and keeps the class testable without fixtures.</p>
 */
public final class SchemaVersionResolver {

    private static final Pattern EXECUTION_STATE_FIELD = Pattern.compile(
            "\"planningSchemaVersion\"\\s*:\\s*\"([^\"]*)\"");

    private static final Pattern LOOKS_LIKE_JSON_OBJECT = Pattern.compile(
            "\\A\\s*\\{[\\s\\S]*\\}\\s*\\z");

    private SchemaVersionResolver() {
        // static-only
    }

    /**
     * @param epicDir the epic directory (e.g. {@code plans/epic-0029})
     * @return the resolved schema version with optional fallback reason
     */
    public static SchemaVersionResolution resolve(Path epicDir) {
        Path executionState = epicDir.resolve("execution-state.json");
        if (!Files.exists(executionState)) {
            return new SchemaVersionResolution(
                    PlanningSchemaVersion.V1, Optional.of(FallbackReason.NO_FILE));
        }
        String content = readOrThrow(executionState);
        if (!LOOKS_LIKE_JSON_OBJECT.matcher(content).find()) {
            throw new UncheckedIOException(new IOException(
                    "execution-state.json is not a JSON object: " + executionState));
        }
        Matcher m = EXECUTION_STATE_FIELD.matcher(content);
        if (!m.find()) {
            return new SchemaVersionResolution(
                    PlanningSchemaVersion.V1, Optional.of(FallbackReason.MISSING_FIELD));
        }
        String wire = m.group(1);
        if (PlanningSchemaVersion.V2.wireValue().equals(wire)) {
            return new SchemaVersionResolution(PlanningSchemaVersion.V2, Optional.empty());
        }
        if (PlanningSchemaVersion.V1.wireValue().equals(wire)) {
            return new SchemaVersionResolution(PlanningSchemaVersion.V1, Optional.empty());
        }
        return new SchemaVersionResolution(
                PlanningSchemaVersion.V1, Optional.of(FallbackReason.INVALID_VALUE));
    }

    private static String readOrThrow(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read " + path, e);
        }
    }
}
