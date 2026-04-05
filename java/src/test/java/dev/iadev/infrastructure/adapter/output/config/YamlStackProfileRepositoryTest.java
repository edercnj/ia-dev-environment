package dev.iadev.infrastructure.adapter.output.config;

import dev.iadev.domain.model.StackProfile;
import dev.iadev.domain.port.output.StackProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit and integration tests for {@link YamlStackProfileRepository}.
 *
 * <p>Tests use real YAML files from the classpath
 * ({@code shared/config-templates/setup-config.*.yaml}) to verify
 * correct loading and mapping to {@link StackProfile}.</p>
 */
class YamlStackProfileRepositoryTest {

    private StackProfileRepository repository;

    @BeforeEach
    void setUp() {
        repository = new YamlStackProfileRepository();
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("returns all bundled profiles")
        void findAll_bundledProfiles_returnsAllProfiles() {
            List<StackProfile> profiles = repository.findAll();

            assertThat(profiles)
                    .isNotNull()
                    .isNotEmpty();
            assertThat(profiles.size())
                    .isGreaterThanOrEqualTo(8);
        }

        @Test
        @DisplayName("returns immutable list")
        void findAll_returnedList_isImmutable() {
            List<StackProfile> profiles = repository.findAll();

            assertThatThrownBy(() -> profiles.add(
                    new StackProfile("test", "java",
                            "spring", "maven", null)))
                    .isInstanceOf(
                            UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("each profile has valid fields")
        void findAll_eachProfile_hasValidFields() {
            List<StackProfile> profiles = repository.findAll();

            for (StackProfile profile : profiles) {
                assertThat(profile.name())
                        .isNotNull().isNotBlank();
                assertThat(profile.language())
                        .isNotNull().isNotBlank();
                assertThat(profile.framework())
                        .isNotNull().isNotBlank();
                assertThat(profile.buildTool())
                        .isNotNull().isNotBlank();
            }
        }
    }

    @Nested
    @DisplayName("findByName")
    class FindByName {

        @ParameterizedTest
        @ValueSource(strings = {
                "go-gin",
                "java-quarkus",
                "java-spring",
                "kotlin-ktor",
                "python-click-cli",
                "python-fastapi",
                "rust-axum",
                "typescript-nestjs"
        })
        @DisplayName("finds each of the 8 standard profiles")
        void findByName_knownProfile_returnsProfile(
                String profileName) {
            Optional<StackProfile> result =
                    repository.findByName(profileName);

            assertThat(result).isPresent();
            assertThat(result.get().name())
                    .isEqualTo(profileName);
        }

        @Test
        @DisplayName("returns empty for nonexistent profile")
        void findByName_nonexistent_returnsEmpty() {
            Optional<StackProfile> result =
                    repository.findByName(
                            "nonexistent-profile");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("throws on null profile name")
        void findByName_nullName_throwsException() {
            assertThatThrownBy(
                    () -> repository.findByName(null))
                    .isInstanceOf(
                            IllegalArgumentException.class);
        }

        @Test
        @DisplayName("throws on blank profile name")
        void findByName_blankName_throwsException() {
            assertThatThrownBy(
                    () -> repository.findByName("  "))
                    .isInstanceOf(
                            IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("exists")
    class Exists {

        @ParameterizedTest
        @ValueSource(strings = {
                "go-gin",
                "java-quarkus",
                "java-spring",
                "kotlin-ktor",
                "python-click-cli",
                "python-fastapi",
                "rust-axum",
                "typescript-nestjs"
        })
        @DisplayName("returns true for each of the 8 profiles")
        void exists_knownProfile_returnsTrue(
                String profileName) {
            assertThat(repository.exists(profileName))
                    .isTrue();
        }

        @Test
        @DisplayName("returns false for nonexistent profile")
        void exists_nonexistent_returnsFalse() {
            assertThat(repository.exists(
                    "nonexistent-profile")).isFalse();
        }

        @Test
        @DisplayName("throws on null profile name")
        void exists_nullName_throwsException() {
            assertThatThrownBy(
                    () -> repository.exists(null))
                    .isInstanceOf(
                            IllegalArgumentException.class);
        }

        @Test
        @DisplayName("throws on blank profile name")
        void exists_blankName_throwsException() {
            assertThatThrownBy(
                    () -> repository.exists("  "))
                    .isInstanceOf(
                            IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("profile content validation")
    class ProfileContent {

        @Test
        @DisplayName("java-spring has correct fields")
        void javaSpring_hasCorrectFields() {
            StackProfile profile =
                    repository.findByName("java-spring")
                            .orElseThrow();

            assertThat(profile.name())
                    .isEqualTo("java-spring");
            assertThat(profile.language())
                    .isEqualTo("java");
            assertThat(profile.framework())
                    .isEqualTo("spring-boot");
            assertThat(profile.buildTool())
                    .isEqualTo("gradle");
        }

        @Test
        @DisplayName("go-gin has correct fields")
        void goGin_hasCorrectFields() {
            StackProfile profile =
                    repository.findByName("go-gin")
                            .orElseThrow();

            assertThat(profile.name())
                    .isEqualTo("go-gin");
            assertThat(profile.language())
                    .isEqualTo("go");
            assertThat(profile.framework())
                    .isEqualTo("gin");
            assertThat(profile.buildTool())
                    .isEqualTo("go-mod");
        }

        @Test
        @DisplayName("rust-axum has correct fields")
        void rustAxum_hasCorrectFields() {
            StackProfile profile =
                    repository.findByName("rust-axum")
                            .orElseThrow();

            assertThat(profile.name())
                    .isEqualTo("rust-axum");
            assertThat(profile.language())
                    .isEqualTo("rust");
            assertThat(profile.framework())
                    .isEqualTo("axum");
            assertThat(profile.buildTool())
                    .isEqualTo("cargo");
        }

        @Test
        @DisplayName("profiles have non-empty properties")
        void profiles_haveProperties() {
            StackProfile profile =
                    repository.findByName("java-spring")
                            .orElseThrow();

            assertThat(profile.properties())
                    .isNotNull()
                    .isNotEmpty();
        }
    }

    @Nested
    @DisplayName("implements port interface")
    class PortContract {

        @Test
        @DisplayName("implements StackProfileRepository")
        void implementsInterface() {
            assertThat(repository)
                    .isInstanceOf(
                            StackProfileRepository.class);
        }
    }
}
