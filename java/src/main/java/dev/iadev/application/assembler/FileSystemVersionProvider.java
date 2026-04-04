package dev.iadev.assembler;

import dev.iadev.domain.stack.VersionDirectoryProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Filesystem-backed implementation of
 * {@link VersionDirectoryProvider}.
 *
 * <p>Delegates directory existence checks and listing to
 * {@link java.nio.file.Files}. This is an infrastructure
 * adapter that keeps the domain layer free of I/O
 * dependencies.</p>
 */
public final class FileSystemVersionProvider
        implements VersionDirectoryProvider {

    @Override
    public boolean exists(Path path) {
        return Files.isDirectory(path);
    }

    @Override
    public List<Path> listVersionDirectories(
            Path basePath) {
        if (!Files.isDirectory(basePath)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(basePath)) {
            return stream
                    .filter(Files::isDirectory)
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }
}
