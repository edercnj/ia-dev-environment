package dev.iadev.domain.schemaversion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.domain.schemaversion.SchemaVersionResolution.FallbackReason;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SchemaVersionResolverTest {

    private static void writeState(Path epicDir, String json) throws IOException {
        Files.writeString(
                epicDir.resolve("execution-state.json"), json, StandardCharsets.UTF_8);
    }

    @Nested
    class FallbackPaths {

        @Test
        void noFile_returnsV1WithNoFileReason(@TempDir Path epicDir) {
            SchemaVersionResolution r = SchemaVersionResolver.resolve(epicDir);
            assertThat(r.version()).isEqualTo(PlanningSchemaVersion.V1);
            assertThat(r.reason()).contains(FallbackReason.NO_FILE);
            assertThat(r.isFallback()).isTrue();
        }

        @Test
        void fileWithoutField_returnsV1WithMissingFieldReason(@TempDir Path epicDir)
                throws IOException {
            writeState(epicDir, "{\"epicId\": \"0029\"}");
            SchemaVersionResolution r = SchemaVersionResolver.resolve(epicDir);
            assertThat(r.version()).isEqualTo(PlanningSchemaVersion.V1);
            assertThat(r.reason()).contains(FallbackReason.MISSING_FIELD);
        }

        @Test
        void invalidFieldValue_returnsV1WithInvalidValueReason(@TempDir Path epicDir)
                throws IOException {
            writeState(epicDir, "{\"planningSchemaVersion\": \"legacy\"}");
            SchemaVersionResolution r = SchemaVersionResolver.resolve(epicDir);
            assertThat(r.version()).isEqualTo(PlanningSchemaVersion.V1);
            assertThat(r.reason()).contains(FallbackReason.INVALID_VALUE);
        }

        @Test
        void futureVersionValue_returnsV1WithInvalidValueReason(@TempDir Path epicDir)
                throws IOException {
            writeState(epicDir, "{\"planningSchemaVersion\": \"3.0\"}");
            SchemaVersionResolution r = SchemaVersionResolver.resolve(epicDir);
            assertThat(r.version()).isEqualTo(PlanningSchemaVersion.V1);
            assertThat(r.reason()).contains(FallbackReason.INVALID_VALUE);
        }
    }

    @Nested
    class HappyPaths {

        @Test
        void version_1_0_explicit_returnsV1WithoutFallback(@TempDir Path epicDir)
                throws IOException {
            writeState(epicDir, "{\"planningSchemaVersion\": \"1.0\"}");
            SchemaVersionResolution r = SchemaVersionResolver.resolve(epicDir);
            assertThat(r.version()).isEqualTo(PlanningSchemaVersion.V1);
            assertThat(r.isFallback()).isFalse();
        }

        @Test
        void version_2_0_explicit_returnsV2(@TempDir Path epicDir) throws IOException {
            writeState(epicDir, "{\"planningSchemaVersion\": \"2.0\", \"epicId\": \"0099\"}");
            SchemaVersionResolution r = SchemaVersionResolver.resolve(epicDir);
            assertThat(r.version()).isEqualTo(PlanningSchemaVersion.V2);
            assertThat(r.isFallback()).isFalse();
        }

        @Test
        void fieldTolerantToWhitespace(@TempDir Path epicDir) throws IOException {
            writeState(epicDir, "{ \"planningSchemaVersion\"  :   \"2.0\" }");
            SchemaVersionResolution r = SchemaVersionResolver.resolve(epicDir);
            assertThat(r.version()).isEqualTo(PlanningSchemaVersion.V2);
        }
    }

    @Nested
    class HardFailure {

        @Test
        void notJsonObject_throwsUncheckedIO(@TempDir Path epicDir) throws IOException {
            writeState(epicDir, "this is not json");
            assertThatThrownBy(() -> SchemaVersionResolver.resolve(epicDir))
                    .isInstanceOf(UncheckedIOException.class)
                    .hasMessageContaining("not a JSON object");
        }

        @Test
        void emptyFile_throwsUncheckedIO(@TempDir Path epicDir) throws IOException {
            writeState(epicDir, "");
            assertThatThrownBy(() -> SchemaVersionResolver.resolve(epicDir))
                    .isInstanceOf(UncheckedIOException.class);
        }
    }

    @Nested
    class Enum {

        @Test
        void wireValues_matchMatrix() {
            assertThat(PlanningSchemaVersion.V1.wireValue()).isEqualTo("1.0");
            assertThat(PlanningSchemaVersion.V2.wireValue()).isEqualTo("2.0");
        }

        @Test
        void resolution_nullArgs_throwNullPointer() {
            assertThatThrownBy(() -> new SchemaVersionResolution(null, java.util.Optional.empty()))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new SchemaVersionResolution(PlanningSchemaVersion.V1, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
