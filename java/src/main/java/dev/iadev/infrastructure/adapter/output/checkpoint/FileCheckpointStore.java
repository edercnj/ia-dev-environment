package dev.iadev.infrastructure.adapter.output.checkpoint;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.iadev.domain.model.CheckpointState;
import dev.iadev.domain.port.output.CheckpointStore;
import dev.iadev.exception.CheckpointIOException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;

/**
 * File-based implementation of {@link CheckpointStore}.
 *
 * <p>Persists {@link CheckpointState} as JSON files in a
 * configured directory, one file per executionId. Uses atomic
 * write (temp file + rename) to prevent corruption.</p>
 *
 * <p>Jackson ObjectMapper configuration:
 * <ul>
 *   <li>{@code INDENT_OUTPUT} for human-readable JSON</li>
 *   <li>{@code WRITE_DATES_AS_TIMESTAMPS} disabled (ISO-8601)</li>
 *   <li>{@code FAIL_ON_UNKNOWN_PROPERTIES} disabled for
 *       forward compatibility</li>
 * </ul>
 *
 * @see CheckpointStore
 * @see CheckpointState
 */
public final class FileCheckpointStore
        implements CheckpointStore {

    private final Path checkpointDir;
    private final ObjectMapper objectMapper;

    /**
     * Creates a file-based checkpoint store.
     *
     * @param checkpointDir directory for checkpoint files
     * @throws NullPointerException if checkpointDir is null
     */
    public FileCheckpointStore(Path checkpointDir) {
        this.checkpointDir = Objects.requireNonNull(
                checkpointDir,
                "checkpointDir must not be null");
        this.objectMapper = createMapper();
    }

    @Override
    public void save(CheckpointState state) {
        validateState(state);
        ensureDirectoryExists();
        Path file = resolveFile(state.executionId());
        writeAtomically(state, file);
    }

    @Override
    public Optional<CheckpointState> load(
            String executionId) {
        validateExecutionId(executionId);
        Path file = resolveFile(executionId);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        return Optional.of(readCheckpoint(file));
    }

    @Override
    public void clear(String executionId) {
        validateExecutionId(executionId);
        Path file = resolveFile(executionId);
        deleteIfExists(file);
    }

    private Path resolveFile(String executionId) {
        return checkpointDir.resolve(
                executionId + ".json");
    }

    private void ensureDirectoryExists() {
        try {
            Files.createDirectories(checkpointDir);
        } catch (IOException e) {
            throw new CheckpointIOException(
                    "Failed to create checkpoint directory",
                    checkpointDir.toString(), e);
        }
    }

    private void writeAtomically(
            CheckpointState state, Path file) {
        try {
            String json = objectMapper
                    .writeValueAsString(state);
            Path tmpFile = file.resolveSibling(
                    "." + file.getFileName() + ".tmp");
            Files.writeString(
                    tmpFile, json, StandardCharsets.UTF_8);
            Files.move(
                    tmpFile, file,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new CheckpointIOException(
                    "Failed to save checkpoint",
                    file.toString(), e);
        }
    }

    private CheckpointState readCheckpoint(Path file) {
        try {
            String json = Files.readString(
                    file, StandardCharsets.UTF_8);
            return objectMapper.readValue(
                    json, CheckpointState.class);
        } catch (IOException e) {
            throw new CheckpointIOException(
                    "Failed to load checkpoint",
                    file.toString(), e);
        }
    }

    private static void deleteIfExists(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new CheckpointIOException(
                    "Failed to clear checkpoint",
                    file.toString(), e);
        }
    }

    private static void validateState(
            CheckpointState state) {
        if (state == null) {
            throw new IllegalArgumentException(
                    "state must not be null");
        }
    }

    private static void validateExecutionId(
            String executionId) {
        if (executionId == null
                || executionId.isBlank()) {
            throw new IllegalArgumentException(
                    "executionId must not be null or blank");
        }
    }

    private static ObjectMapper createMapper() {
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(
                SerializationFeature
                        .WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(
                DeserializationFeature
                        .FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }
}
