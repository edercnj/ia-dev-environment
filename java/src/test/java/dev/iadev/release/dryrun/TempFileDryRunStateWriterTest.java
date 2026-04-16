package dev.iadev.release.dryrun;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TempFileDryRunStateWriter}.
 */
@DisplayName("TempFileDryRunStateWriterTest")
class TempFileDryRunStateWriterTest {

    @Nested
    @DisplayName("happy — create and delete")
    class HappyCreateAndDelete {

        @Test
        @DisplayName("create_validVersion"
                + "_producesFileUnderTempDir")
        void create_validVersion_producesFileUnderTempDir() throws Exception {
            TempFileDryRunStateWriter writer =
                    new TempFileDryRunStateWriter();
            Path created = writer.create("3.2.0");

            try {
                assertThat(created).exists();
                assertThat(created.getFileName().toString())
                        .startsWith("release-state-dryrun-")
                        .endsWith(".json");
                assertThat(Files.size(created))
                        .isGreaterThan(0L);
            } finally {
                writer.delete(created);
            }
        }

        @Test
        @DisplayName("delete_existingFile"
                + "_removesFile")
        void delete_existingFile_removesFile() throws Exception {
            TempFileDryRunStateWriter writer =
                    new TempFileDryRunStateWriter();
            Path created = writer.create("3.2.0");

            writer.delete(created);

            assertThat(created).doesNotExist();
        }

        @Test
        @DisplayName("delete_null"
                + "_isNoOp")
        void delete_null_isNoOp() {
            TempFileDryRunStateWriter writer =
                    new TempFileDryRunStateWriter();

            writer.delete(null);
        }

        @Test
        @DisplayName("delete_nonExistingPath"
                + "_isNoOp")
        void delete_nonExistingPath_isNoOp() {
            TempFileDryRunStateWriter writer =
                    new TempFileDryRunStateWriter();
            Path missing = Path.of("/tmp",
                    "does-not-exist-"
                            + System.nanoTime() + ".json");

            writer.delete(missing);
        }
    }

    @Nested
    @DisplayName("security — POSIX permissions (0600)")
    @DisabledOnOs(OS.WINDOWS)
    class SecurityPosixPermissions {

        @Test
        @DisplayName("create_onPosix"
                + "_fileHasOwnerReadWriteOnly")
        void create_onPosix_fileHasOwnerReadWriteOnly() throws Exception {
            TempFileDryRunStateWriter writer =
                    new TempFileDryRunStateWriter();
            Path created = writer.create("3.2.0");

            try {
                Set<PosixFilePermission> perms =
                        Files.getPosixFilePermissions(created);
                assertThat(PosixFilePermissions.toString(perms))
                        .isEqualTo("rw-------");
            } finally {
                writer.delete(created);
            }
        }
    }
}
