package dev.iadev.architecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the hexagonal package structure scaffolding
 * required by story-0015-0002.
 *
 * <p>Tests validate:
 * <ul>
 *   <li>All 14 hexagonal directories exist</li>
 *   <li>Each directory contains a package-info.java</li>
 *   <li>Each package-info.java has Javadoc documentation</li>
 *   <li>Existing packages are not modified</li>
 * </ul>
 */
@DisplayName("Hexagonal Package Structure (story-0015-0002)")
class HexagonalPackageStructureTest {

    private static final Path SRC_ROOT = Path.of(
        "src/main/java/dev/iadev"
    );

    /**
     * All hexagonal leaf packages that must exist with
     * package-info.java files.
     */
    private static final List<String> HEXAGONAL_PACKAGES = List.of(
        "domain/model",
        "domain/port/input",
        "domain/port/output",
        "domain/service",
        "application/assembler",
        "application/dag",
        "application/factory",
        "infrastructure/adapter/input/cli",
        "infrastructure/adapter/output/template",
        "infrastructure/adapter/output/filesystem",
        "infrastructure/adapter/output/config",
        "infrastructure/adapter/output/checkpoint",
        "infrastructure/adapter/output/progress",
        "infrastructure/adapter/output/telemetry",
        "infrastructure/config"
    );

    /**
     * Output adapter subdirectories that must exist under
     * infrastructure/adapter/output/.
     * Extended in story-0039-0012 with the telemetry adapter.
     */
    private static final List<String> OUTPUT_ADAPTER_SUBDIRS = List.of(
        "template", "filesystem", "config", "checkpoint", "progress",
        "telemetry"
    );

    @Nested
    @DisplayName("GK-2: Scaffolding completo criado com sucesso")
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
    @DisplayName("GK-3: Scaffolding nao altera pacotes existentes")
    class ExistingPackagesPreserved {

        private static final List<String> EXISTING_PACKAGES = List.of(
            "cli", "config", "domain",
            "domain/implementationmap", "domain/stack",
            "application/assembler", "template",
            "checkpoint", "progress",
            "exception", "util", "smoke"
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
    @DisplayName("GK-4: Todos os package-info.java seguem "
        + "convencao de documentacao")
    class PackageInfoDocumentation {

        @Test
        @DisplayName("All hexagonal package-info.java files "
            + "contain Javadoc with responsibility description")
        void allPackageInfosHaveJavadoc() throws IOException {
            for (String pkg : HEXAGONAL_PACKAGES) {
                Path packageInfoPath = SRC_ROOT
                    .resolve(pkg)
                    .resolve("package-info.java");

                String content = Files.readString(packageInfoPath);

                assertThat(content)
                    .as("Javadoc in %s/package-info.java", pkg)
                    .contains("/**")
                    .contains("*/");
            }
        }

        @Test
        @DisplayName("No package-info.java is empty or has "
            + "only the package declaration")
        void noPackageInfoIsEmpty() throws IOException {
            for (String pkg : HEXAGONAL_PACKAGES) {
                Path packageInfoPath = SRC_ROOT
                    .resolve(pkg)
                    .resolve("package-info.java");

                String content = Files.readString(packageInfoPath);
                String withoutPackageDecl = content
                    .replaceAll("package\\s+[\\w.]+;", "")
                    .trim();

                assertThat(withoutPackageDecl)
                    .as("Content beyond package declaration in %s",
                        pkg)
                    .isNotEmpty();
            }
        }

        @Test
        @DisplayName("All hexagonal package-info.java files "
            + "reference a RULE")
        void allPackageInfosReferenceRule() throws IOException {
            for (String pkg : HEXAGONAL_PACKAGES) {
                Path packageInfoPath = SRC_ROOT
                    .resolve(pkg)
                    .resolve("package-info.java");

                String content = Files.readString(packageInfoPath);

                assertThat(content)
                    .as("RULE reference in %s/package-info.java",
                        pkg)
                    .containsPattern("RULE-\\d+");
            }
        }
    }

    @Nested
    @DisplayName("GK-5: Estrutura de subdiretorios de output "
        + "adapters esta completa")
    class OutputAdapterSubdirectories {

        @Test
        @DisplayName("infrastructure/adapter/output/ has "
            + "exactly 6 subdirectories")
        void outputAdapterHasSixSubdirs() throws IOException {
            Path outputAdapterPath = SRC_ROOT
                .resolve("infrastructure/adapter/output");

            assertThat(outputAdapterPath)
                .exists()
                .isDirectory();

            for (String subdir : OUTPUT_ADAPTER_SUBDIRS) {
                Path subdirPath = outputAdapterPath.resolve(subdir);
                assertThat(subdirPath)
                    .as("Output adapter subdirectory: %s", subdir)
                    .exists()
                    .isDirectory();
            }

            long actualCount;
            try (var stream = Files.list(outputAdapterPath)) {
                actualCount = stream
                    .filter(Files::isDirectory)
                    .count();
            }
            assertThat(actualCount)
                .as("Exact number of output adapter "
                    + "subdirectories")
                .isEqualTo(OUTPUT_ADAPTER_SUBDIRS.size());
        }

        @Test
        @DisplayName("Each output adapter subdirectory has "
            + "its own package-info.java")
        void eachOutputAdapterSubdirHasPackageInfo() {
            for (String subdir : OUTPUT_ADAPTER_SUBDIRS) {
                Path packageInfoPath = SRC_ROOT
                    .resolve("infrastructure/adapter/output")
                    .resolve(subdir)
                    .resolve("package-info.java");

                assertThat(packageInfoPath)
                    .as("package-info.java in output/%s", subdir)
                    .exists()
                    .isRegularFile();
            }
        }
    }
}
