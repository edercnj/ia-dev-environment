package dev.iadev.infrastructure.adapter.output.template;

import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.List;
import java.util.Map;

/**
 * Pebble filter that converts Java booleans to Python-style
 * string representations.
 *
 * <p>When applied to a {@code Boolean} value, returns
 * {@code "True"} or {@code "False"} (capitalized). Non-boolean
 * values pass through unchanged.</p>
 *
 * <p>Usage in templates:
 * <pre>{@code {{ domain_driven | python_bool }}}</pre>
 *
 * <p>This filter ensures byte-for-byte parity with the
 * Jinja2/Nunjucks output from the TypeScript implementation,
 * where Python-style boolean strings are used in generated
 * configuration files.</p>
 *
 * <p>This is an infrastructure adapter implementation detail;
 * it is NOT a domain concern.</p>
 *
 * @see io.pebbletemplates.pebble.extension.Filter
 */
final class PythonBoolFilter implements Filter {

    private static final String PYTHON_TRUE = "True";
    private static final String PYTHON_FALSE = "False";

    @Override
    public Object apply(
            Object input,
            Map<String, Object> args,
            PebbleTemplate self,
            EvaluationContext context,
            int lineNumber) {
        if (input instanceof Boolean boolValue) {
            return boolValue ? PYTHON_TRUE : PYTHON_FALSE;
        }
        return input;
    }

    @Override
    public List<String> getArgumentNames() {
        return List.of();
    }
}
