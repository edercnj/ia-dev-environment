package dev.iadev.checkpoint;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.iadev.exception.CheckpointIOException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Jackson-based implementation of {@link CheckpointPersistence}.
 *
 * <p>Serializes {@link ExecutionState} to pretty-printed JSON using
 * Jackson {@link ObjectMapper}. Writes atomically via a temp file +
 * rename strategy. Ignores unknown properties for forward
 * compatibility.</p>
 *
 * <p>Configuration:
 * <ul>
 *   <li>{@code INDENT_OUTPUT} for human-readable JSON</li>
 *   <li>{@code WRITE_DATES_AS_TIMESTAMPS} disabled (ISO-8601)</li>
 *   <li>{@code FAIL_ON_UNKNOWN_PROPERTIES} disabled</li>
 * </ul>
 */
public final class JacksonCheckpointPersistence
        implements CheckpointPersistence {

    private final ObjectMapper objectMapper;

    /**
     * Creates a new instance with a pre-configured ObjectMapper.
     */
    public JacksonCheckpointPersistence() {
        this.objectMapper = createMapper();
    }

    private static ObjectMapper createMapper() {
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
        );
        mapper.disable(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
        );
        return mapper;
    }

    @Override
    public void save(ExecutionState state, Path path) {
        try {
            var json = objectMapper.writeValueAsString(state);
            var tmpFile = path.resolveSibling(
                    "." + path.getFileName() + ".tmp"
            );
            Files.writeString(
                    tmpFile, json, StandardCharsets.UTF_8
            );
            Files.move(
                    tmpFile, path,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (IOException e) {
            throw new CheckpointIOException(
                    "Failed to save checkpoint",
                    path.toString(), e
            );
        }
    }

    @Override
    public ExecutionState load(Path path) {
        try {
            var json = Files.readString(
                    path, StandardCharsets.UTF_8
            );
            var state = objectMapper.readValue(
                    json, ExecutionState.class
            );
            var errors = CheckpointValidation.validate(state);
            if (!errors.isEmpty()) {
                throw new dev.iadev.exception
                        .CheckpointValidationException(
                        "Invalid checkpoint: "
                                + String.join("; ", errors),
                        "ExecutionState"
                );
            }
            return state;
        } catch (IOException e) {
            throw new CheckpointIOException(
                    "Failed to load checkpoint",
                    path.toString(), e
            );
        }
    }
}
