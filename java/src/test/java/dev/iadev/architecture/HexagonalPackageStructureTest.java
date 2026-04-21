package dev.iadev.architecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Hexagonal Package Structure")
class HexagonalPackageStructureTest {

    private static final Path SRC_ROOT = Path.of(
        "src/main/java/dev/iadev"
    );

    private static final List<String> HEXAGONAL_PACKAGES = List.of(
        "domain/model",
        "domain/port/input",
        "domain/port/output",
        "domain/service",
        "application/assembler",
        "application/dag",
        "application/factory",
        "infrastructure/adapter/output/template",
        "infrastructure/adapter/output/filesystem",
        "infrastructure/adapter/output/config",
        "infrastructure/adapter/output/progress",
        "infrastructure/config"
    );

    private static final List<String> OUTPUT_ADAPTER_SUBDIRS = List.of(
        "template", "filesystem", "config", "progress"
    );

    @Nested
    @DisplayName("Scaffolding completo criado com sucesso")
    class ScaffoldingCreated {

        @Test
        @DisplayName("Each hexagonal directory contains a "
            + "package-info.java")
        void eachDirectoryContainsPackageInfo() {
            for (String pkg : HEXAGONAL_PACKAGES) {
                Path packageInfoPath = SRC_ROOT
                    .resolve(pkg)
                    .resolve("package-info.java");

                assertThat(packageInfoPath)
                    .as("package-info.java in %s", pkg)
                    .exists()
                    .isRegularFile();
            }
        }
    }

    @Nested
    @DisplayName("Scaffolding nao altera pacotes existentes")
    class ExistingPackagesPreserved {

        private static final List<String> EXISTING_PACKAGES = List.of(
            "cli", "config", "domain",
            "domain/stack",
            "application/assembler", "template",
            "exception", "util"
        );

        @Test
        @DisplayName("All existing packages still have their "
            + "package-info.java")
        void existingPackageInfosPreserved() {
            for (String pkg : EXISTING_PACKAGES) {
                Path packageInfoPath = SRC_ROOT
                    .resolve(pkg)
                    .resolve("package-info.java");

                assertThat(packageInfoPath)
                    .as("Existing package-info.java in %s", pkg)
                    .exists()
                    .isRegularFile();
            }
        }
    }

    @Nested
    @DisplayName("Todos os package-info.java seguem "
        + "convencao de documentacao")
    class PackageInfoDocumentation {

        @Test
        @DisplayName("All hexagonal package-info.java files "
            + "contain Javadoc")
        void allPackageInfosHaveJavadoc() throws IOException {
            for (String pkg : HEXAGONAL_PACKAGES) {
                Path packageInfoPath = SRC_ROOT
                    .resolve(pkg)
                    .resolve("package-info.java");

                String content =
                        Files.readString(packageInfoPath);

                assertThat(content)
                    .as("Javadoc in %s/package-info.java", pkg)
                    .contains("/**")
                    .contains("*/");
            }
        }

        @Test
        @DisplayName("No package-info.java is empty")
        void noPackageInfoIsEmpty() throws IOException {
            for (String pkg : HEXAGONAL_PACKAGES) {
                Path packageInfoPath = SRC_ROOT
                    .resolve(pkg)
                    .resolve("package-info.java");

                String content =
                        Files.readString(packageInfoPath);
                String withoutPackageDecl = content
                    .replaceAll(
                            "package\\s+[\\w.]+;", "")
                    .trim();

                assertThat(withoutPackageDecl)
                    .as("Content beyond package declaration "
                            + "in %s", pkg)
                    .isNotEmpty();
            }
        }

        @Test
        @DisplayName("All hexagonal package-info.java files "
            + "reference a RULE")
        void allPackageInfosReferenceRule()
                throws IOException {
            for (String pkg : HEXAGONAL_PACKAGES) {
                Path packageInfoPath = SRC_ROOT
                    .resolve(pkg)
                    .resolve("package-info.java");

                String content =
                        Files.readString(packageInfoPath);

                assertThat(content)
                    .as("RULE reference in "
                            + "%s/package-info.java", pkg)
                    .containsPattern("RULE-\\d+");
            }
        }
    }

    @Nested
    @DisplayName("Estrutura de subdiretorios de output "
        + "adapters esta completa")
    class OutputAdapterSubdirectories {

        @Test
        @DisplayName("infrastructure/adapter/output/ has "
            + "exactly 4 subdirectories")
        void outputAdapterHasFourSubdirs()
                throws IOException {
            Path outputAdapterPath = SRC_ROOT
                .resolve("infrastructure/adapter/output");

            assertThat(outputAdapterPath)
                .exists()
                .isDirectory();

            for (String subdir : OUTPUT_ADAPTER_SUBDIRS) {
                Path subdirPath =
                        outputAdapterPath.resolve(subdir);
                assertThat(subdirPath)
                    .as("Output adapter subdirectory: %s",
                            subdir)
                    .exists()
                    .isDirectory();
            }

            long actualCount;
            try (var stream =
                    Files.list(outputAdapterPath)) {
                actualCount = stream
                    .filter(Files::isDirectory)
                    .count();
            }
            assertThat(actualCount)
                .as("Exact number of output adapter "
                    + "subdirectories")
                .isEqualTo(
                        OUTPUT_ADAPTER_SUBDIRS.size());
        }

        @Test
        @DisplayName("Each output adapter subdirectory has "
            + "its own package-info.java")
        void eachOutputAdapterSubdirHasPackageInfo() {
            for (String subdir : OUTPUT_ADAPTER_SUBDIRS) {
                Path packageInfoPath = SRC_ROOT
                    .resolve(
                            "infrastructure/adapter/output")
                    .resolve(subdir)
                    .resolve("package-info.java");

                assertThat(packageInfoPath)
                    .as("package-info.java in output/%s",
                            subdir)
                    .exists()
                    .isRegularFile();
            }
        }
    }
}
