package dev.iadev.template;

import io.pebbletemplates.pebble.extension.Filter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PythonBoolExtension")
class PythonBoolExtensionTest {

    @Test
    @DisplayName("registers python_bool filter")
    void getFilters_default_containsPythonBool() {
        PythonBoolExtension extension =
                new PythonBoolExtension();

        Map<String, Filter> filters = extension.getFilters();

        assertThat(filters).containsKey("python_bool");
        assertThat(filters.get("python_bool"))
                .isInstanceOf(PythonBoolFilter.class);
    }

    @Test
    @DisplayName("returns exactly one filter")
    void getFilters_default_returnsOneFilter() {
        PythonBoolExtension extension =
                new PythonBoolExtension();

        Map<String, Filter> filters = extension.getFilters();

        assertThat(filters).hasSize(1);
    }
}
