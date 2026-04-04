package dev.iadev.application.assembler;

/**
 * Metadata extracted from a generated agent {@code .md} file.
 *
 * @param name        the agent name (without {@code .md} extension)
 * @param description the first meaningful line of content
 */
public record AgentInfo(String name, String description) {
}
