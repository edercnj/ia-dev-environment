package dev.iadev.domain.stack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("VersionResolver")
class VersionResolverTest {

    private final VersionDirectoryProvider provider =
            mock(VersionDirectoryProvider.class);
    private final VersionResolver resolver =
            new VersionResolver(provider);
    private final Path baseDir = Path.of("/resources");

    @Nested
    @DisplayName("findVersionDir()")
    class FindVersionDirTests {

        @Test
        @DisplayName("finds exact match directory")
        void findVersionDir_exactMatch_found() {
            Path exact = baseDir.resolve("java-21");
            when(provider.exists(exact)).thenReturn(true);

            var result = resolver.findVersionDir(
                    baseDir, "java", "21");

            assertThat(result).isPresent();
            assertThat(result.get().getFileName().toString())
                    .isEqualTo("java-21");
            verify(provider).exists(exact);
        }

        @Test
        @DisplayName("falls back to major version wildcard")
        void findVersionDir_majorFallback_found() {
            Path exact =
                    baseDir.resolve("quarkus-3.17.1");
            Path fallback =
                    baseDir.resolve("quarkus-3.x");
            when(provider.exists(exact)).thenReturn(false);
            when(provider.exists(fallback)).thenReturn(true);

            var result = resolver.findVersionDir(
                    baseDir, "quarkus", "3.17.1");

            assertThat(result).isPresent();
            assertThat(result.get().getFileName().toString())
                    .isEqualTo("quarkus-3.x");
            verify(provider).exists(exact);
            verify(provider).exists(fallback);
        }

        @Test
        @DisplayName("prefers exact match over major fallback")
        void findVersionDir_bothExist_prefersExact() {
            Path exact = baseDir.resolve("java-21");
            when(provider.exists(exact)).thenReturn(true);

            var result = resolver.findVersionDir(
                    baseDir, "java", "21");

            assertThat(result).isPresent();
            assertThat(result.get().getFileName().toString())
                    .isEqualTo("java-21");
        }

        @Test
        @DisplayName("returns empty when neither exists")
        void findVersionDir_noneExist_empty() {
            Path exact = baseDir.resolve("java-21");
            Path fallback = baseDir.resolve("java-21.x");
            when(provider.exists(exact)).thenReturn(false);
            when(provider.exists(fallback)).thenReturn(false);

            var result = resolver.findVersionDir(
                    baseDir, "java", "21");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty when provider reports "
                + "no match for file-not-dir scenario")
        void findVersionDir_providerReturnsFalse_empty() {
            Path exact = baseDir.resolve("java-21");
            Path fallback = baseDir.resolve("java-21.x");
            when(provider.exists(exact)).thenReturn(false);
            when(provider.exists(fallback)).thenReturn(false);

            var result = resolver.findVersionDir(
                    baseDir, "java", "21");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("calls provider.exists with correct "
                + "exact path")
        void findVersionDir_exactPath_correctlyResolved() {
            Path expected =
                    baseDir.resolve("spring-3.4.1");
            when(provider.exists(expected))
                    .thenReturn(true);

            var result = resolver.findVersionDir(
                    baseDir, "spring", "3.4.1");

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(expected);
        }

        @Test
        @DisplayName("calls provider.exists twice when "
                + "exact miss triggers fallback")
        void findVersionDir_fallback_callsExistsTwice() {
            Path exact =
                    baseDir.resolve("java-21.0.2");
            Path fallback =
                    baseDir.resolve("java-21.x");
            when(provider.exists(exact)).thenReturn(false);
            when(provider.exists(fallback)).thenReturn(true);

            var result = resolver.findVersionDir(
                    baseDir, "java", "21.0.2");

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(fallback);
            verify(provider).exists(exact);
            verify(provider).exists(fallback);
        }
    }

    @Nested
    @DisplayName("extractMajorPart()")
    class ExtractMajorPartTests {

        @Test
        @DisplayName("extracts before first dot")
        void extractMajor_dotted_beforeDot() {
            assertThat(VersionResolver
                    .extractMajorPart("3.17.1"))
                    .isEqualTo("3");
        }

        @Test
        @DisplayName("returns full string when no dot")
        void extractMajor_noDot_fullString() {
            assertThat(VersionResolver
                    .extractMajorPart("21"))
                    .isEqualTo("21");
        }

        @Test
        @DisplayName("returns empty for empty string")
        void extractMajor_empty_empty() {
            assertThat(VersionResolver
                    .extractMajorPart(""))
                    .isEmpty();
        }

        @Test
        @DisplayName("returns null for null")
        void extractMajor_null_null() {
            assertThat(VersionResolver
                    .extractMajorPart(null))
                    .isNull();
        }
    }
}
