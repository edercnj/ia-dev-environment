package dev.iadev.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResourceNotFoundException")
class ResourceNotFoundExceptionTest {

    @Test
    @DisplayName("carries resource path and strategies in message")
    void constructor_whenCalled_carriesPathAndStrategies() {
        var ex = new ResourceNotFoundException(
                "shared/templates/missing.txt",
                "classpath, filesystem(/tmp/res)");

        assertThat(ex.getMessage())
                .contains("Resource not found: shared/templates/missing.txt")
                .contains("classpath, filesystem(/tmp/res)");
    }

    @Test
    @DisplayName("extends RuntimeException")
    void create_whenCalled_extendsRuntimeException() {
        var ex = new ResourceNotFoundException("path", "classpath");

        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("getResourcePath returns the path")
    void getResourcePath_whenCalled_returnsPath() {
        var ex = new ResourceNotFoundException(
                "core/01-clean-code.md", "classpath");

        assertThat(ex.getResourcePath())
                .isEqualTo("core/01-clean-code.md");
    }

    @Test
    @DisplayName("getSearchStrategies returns strategies tried")
    void getSearchStrategies_whenCalled_returnsStrategies() {
        var ex = new ResourceNotFoundException(
                "missing.txt", "classpath, filesystem(/tmp)");

        assertThat(ex.getSearchStrategies())
                .isEqualTo("classpath, filesystem(/tmp)");
    }

    @Test
    @DisplayName("toString includes class name and details")
    void toString_whenCalled_includesDetails() {
        var ex = new ResourceNotFoundException(
                "foo/bar.txt", "classpath");

        assertThat(ex.toString())
                .contains("ResourceNotFoundException")
                .contains("foo/bar.txt");
    }
}
