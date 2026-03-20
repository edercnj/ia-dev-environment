package dev.iadev.template;

import io.pebbletemplates.pebble.extension.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PythonBoolFilter")
class PythonBoolFilterTest {

    private PythonBoolFilter filter;

    @BeforeEach
    void setUp() {
        filter = new PythonBoolFilter();
    }

    @Nested
    @DisplayName("filter name")
    class FilterName {

        @Test
        @DisplayName("getArgumentNames returns empty list")
        void getArgumentNames_noArgs_returnsEmptyList() {
            List<String> args = filter.getArgumentNames();

            assertThat(args).isEmpty();
        }
    }

    @Nested
    @DisplayName("boolean conversion (RULE-002)")
    class BooleanConversion {

        @Test
        @DisplayName("true converts to 'True'")
        void apply_booleanTrue_returnsTrue() {
            Object result = filter.apply(
                    true, Map.of(), null, null, 0);

            assertThat(result).isEqualTo("True");
        }

        @Test
        @DisplayName("false converts to 'False'")
        void apply_booleanFalse_returnsFalse() {
            Object result = filter.apply(
                    false, Map.of(), null, null, 0);

            assertThat(result).isEqualTo("False");
        }

        @Test
        @DisplayName("Boolean.TRUE converts to 'True'")
        void apply_boxedTrue_returnsTrue() {
            Object result = filter.apply(
                    Boolean.TRUE, Map.of(), null, null, 0);

            assertThat(result).isEqualTo("True");
        }

        @Test
        @DisplayName("Boolean.FALSE converts to 'False'")
        void apply_boxedFalse_returnsFalse() {
            Object result = filter.apply(
                    Boolean.FALSE, Map.of(), null, null, 0);

            assertThat(result).isEqualTo("False");
        }
    }

    @Nested
    @DisplayName("passthrough for non-booleans")
    class Passthrough {

        @Test
        @DisplayName("string input is returned unchanged")
        void apply_stringInput_returnsUnchanged() {
            Object result = filter.apply(
                    "hello", Map.of(), null, null, 0);

            assertThat(result).isEqualTo("hello");
        }

        @Test
        @DisplayName("integer input is returned unchanged")
        void apply_integerInput_returnsUnchanged() {
            Object result = filter.apply(
                    42, Map.of(), null, null, 0);

            assertThat(result).isEqualTo(42);
        }

        @Test
        @DisplayName("null input is returned unchanged")
        void apply_nullInput_returnsNull() {
            Object result = filter.apply(
                    null, Map.of(), null, null, 0);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("list input is returned unchanged")
        void apply_listInput_returnsUnchanged() {
            List<String> input = List.of("a", "b");

            Object result = filter.apply(
                    input, Map.of(), null, null, 0);

            assertThat(result).isEqualTo(input);
        }
    }

    @Nested
    @DisplayName("Filter interface compliance")
    class FilterInterface {

        @Test
        @DisplayName("implements Filter interface")
        void class_whenCalled_implementsFilter() {
            assertThat(filter).isInstanceOf(Filter.class);
        }
    }
}
