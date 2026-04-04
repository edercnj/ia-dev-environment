package dev.iadev.application.assembler;

import dev.iadev.domain.model.McpServerConfig;
import dev.iadev.domain.model.ProjectConfig;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Shared constants and helpers for Codex assemblers.
 *
 * <p>Centralizes model defaults, policy derivation, hooks
 * detection, MCP server mapping, and TOML key validation
 * used by {@link CodexAgentsMdAssembler} and
 * {@link CodexConfigAssembler}.</p>
 *
 * @see CodexAgentsMdAssembler
 * @see CodexConfigAssembler
 */
public final class CodexShared {

    /** Default Codex model for cost/performance balance. */
    public static final String DEFAULT_MODEL = "o4-mini";

    /** Approval policy when hooks are present. */
    static final String POLICY_ON_REQUEST = "on-request";

    /** Approval policy when no hooks are detected. */
    static final String POLICY_UNTRUSTED = "untrusted";

    /** Default sandbox mode — allows editing project files. */
    public static final String SANDBOX_WORKSPACE_WRITE =
            "workspace-write";

    /** Pattern for valid TOML bare keys. */
    private static final Pattern TOML_BARE_KEY =
            Pattern.compile("^[A-Za-z0-9_-]+$");

    private CodexShared() {
        // Utility class — no instantiation
    }

    /**
     * Checks if a path is an accessible directory.
     *
     * @param dirPath the path to check
     * @return true if the path exists and is a directory
     */
    public static boolean isAccessibleDirectory(Path dirPath) {
        return Files.isDirectory(dirPath);
    }

    /**
     * Detects if hooks exist in the given directory.
     *
     * @param hooksDir the hooks directory path
     * @return true if directory exists and contains entries
     */
    public static boolean detectHooks(Path hooksDir) {
        if (!isAccessibleDirectory(hooksDir)) {
            return false;
        }
        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(hooksDir)) {
            return stream.iterator().hasNext();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Derives the approval policy based on hooks presence.
     *
     * @param hookPresence whether hooks are detected
     * @return "on-request" if hooks exist, "untrusted"
     *         otherwise
     */
    public static String deriveApprovalPolicy(
            HookPresence hookPresence) {
        return hookPresence.hasHooks()
                ? POLICY_ON_REQUEST : POLICY_UNTRUSTED;
    }

    /**
     * Validates that an MCP server id is a safe TOML bare key.
     *
     * @param id the server identifier to validate
     * @return true if id matches {@code [A-Za-z0-9_-]+}
     */
    public static boolean isValidTomlBareKey(String id) {
        return TOML_BARE_KEY.matcher(id).matches();
    }

    /**
     * Sanitizes an arbitrary identifier into a safe TOML bare key.
     *
     * @param id the raw identifier
     * @return sanitized key using {@code [A-Za-z0-9_-]+}
     */
    public static String sanitizeTomlBareKey(String id) {
        if (id == null || id.isBlank()) {
            return "agent";
        }
        String sanitized = id.replaceAll("[^A-Za-z0-9_-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        return sanitized.isBlank() ? "agent" : sanitized;
    }

    /**
     * Escapes a string value for safe TOML rendering inside
     * double quotes.
     *
     * @param value the raw string value
     * @return the escaped string
     */
    public static String escapeTomlValue(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Maps MCP server configurations to template-ready objects.
     *
     * <p>Trims/filters URL parts to avoid empty command
     * elements. Escapes env values for safe TOML rendering.</p>
     *
     * @param config the project configuration
     * @return list of MCP server context maps
     */
    public static List<Map<String, Object>> mapMcpServers(
            ProjectConfig config) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (McpServerConfig server : config.mcp().servers()) {
            Map<String, Object> ctx = new LinkedHashMap<>();
            ctx.put("id", server.id());

            String trimmed = server.url() != null
                    ? server.url().trim() : "";
            List<String> command = trimmed.isEmpty()
                    ? List.of()
                    : List.of(trimmed.split("\\s+"));
            ctx.put("command", command);

            Map<String, String> rawEnv = server.env();
            if (rawEnv != null && !rawEnv.isEmpty()) {
                Map<String, String> escaped =
                        new LinkedHashMap<>();
                for (Map.Entry<String, String> entry
                        : rawEnv.entrySet()) {
                    escaped.put(entry.getKey(),
                            escapeTomlValue(entry.getValue()));
                }
                ctx.put("env", escaped);
            } else {
                ctx.put("env", null);
            }
            result.add(ctx);
        }
        return result;
    }
}
