package dev.iadev.domain.service;

import dev.iadev.domain.model.StackProfile;
import dev.iadev.domain.port.output.StackProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ListStackProfilesService}.
 *
 * <p>Tests use mocked Output Ports to validate business logic
 * in isolation from infrastructure.</p>
 */
@ExtendWith(MockitoExtension.class)
class ListStackProfilesServiceTest {

    @Mock
    private StackProfileRepository profileRepository;

    private ListStackProfilesService service;

    @BeforeEach
    void setUp() {
        service = new ListStackProfilesService(
                profileRepository);
    }

    @Nested
    @DisplayName("Constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("constructor_nullRepository"
                + "_throwsNullPointerException")
        void constructor_nullRepository_throwsNpe() {
            assertThatThrownBy(
                    () -> new ListStackProfilesService(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining(
                            "profileRepository");
        }
    }

    @Nested
    @DisplayName("listProfiles")
    class ListProfiles {

        @Test
        @DisplayName("listProfiles_repositoryReturnsProfiles"
                + "_returnsSameList")
        void listProfiles_repositoryReturnsProfiles_returnsSameList() {
            List<StackProfile> profiles = List.of(
                    new StackProfile("java-spring", "java",
                            "spring", "maven", Map.of()),
                    new StackProfile("python-fastapi",
                            "python", "fastapi", "pip",
                            Map.of()));
            when(profileRepository.findAll())
                    .thenReturn(profiles);

            List<StackProfile> result =
                    service.listProfiles();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).name())
                    .isEqualTo("java-spring");
            assertThat(result.get(1).name())
                    .isEqualTo("python-fastapi");
            verify(profileRepository).findAll();
        }

        @Test
        @DisplayName("listProfiles_repositoryReturnsEmpty"
                + "_returnsEmptyList")
        void listProfiles_repositoryReturnsEmpty_returnsEmptyList() {
            when(profileRepository.findAll())
                    .thenReturn(List.of());

            List<StackProfile> result =
                    service.listProfiles();

            assertThat(result).isEmpty();
            verify(profileRepository).findAll();
        }

        @Test
        @DisplayName("listProfiles_repositoryReturnsEight"
                + "_returnsExactlyEight")
        void listProfiles_repositoryReturnsEight_returnsExactlyEight() {
            List<StackProfile> profiles = List.of(
                    new StackProfile("go-gin", "go",
                            "gin", "go", Map.of()),
                    new StackProfile("java-quarkus", "java",
                            "quarkus", "maven", Map.of()),
                    new StackProfile("java-spring", "java",
                            "spring", "maven", Map.of()),
                    new StackProfile("kotlin-ktor", "kotlin",
                            "ktor", "gradle", Map.of()),
                    new StackProfile("python-click-cli",
                            "python", "click", "pip",
                            Map.of()),
                    new StackProfile("python-fastapi",
                            "python", "fastapi", "pip",
                            Map.of()),
                    new StackProfile("rust-axum", "rust",
                            "axum", "cargo", Map.of()),
                    new StackProfile("typescript-nestjs",
                            "typescript", "nestjs", "npm",
                            Map.of()));
            when(profileRepository.findAll())
                    .thenReturn(profiles);

            List<StackProfile> result =
                    service.listProfiles();

            assertThat(result).hasSize(8);
            verify(profileRepository).findAll();
        }
    }

    @Nested
    @DisplayName("Interface implementation")
    class InterfaceImplementation {

        @Test
        @DisplayName("service_implementsListStackProfiles"
                + "UseCase")
        void service_implementsListStackProfilesUseCase() {
            assertThat(service)
                    .isInstanceOf(
                            dev.iadev.domain.port.input
                                    .ListStackProfilesUseCase
                                    .class);
        }
    }
}
