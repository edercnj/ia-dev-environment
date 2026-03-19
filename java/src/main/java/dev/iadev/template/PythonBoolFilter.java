package dev.iadev.template;

import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.List;
import java.util.Map;

/**
 * Pebble filter that converts Java booleans to Python-style
 * string representations (RULE-002).
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
 * @see io.pebbletemplates.pebble.extension.Filter
 */
public final class PythonBoolFilter implements Filter {

    private static final String PYTHON_TRUE = "True";
    private static final String PYTHON_FALSE = "False";

    /**
     * Applies the python_bool filter to the given input.
     *
     * <p>If the input is a {@link Boolean}, returns
     * {@code "True"} or {@code "False"}. Otherwise, returns
     * the input unchanged.</p>
     *
     * @param input      the value to filter
     * @param args       filter arguments (unused)
     * @param self       the template being evaluated
     * @param context    the evaluation context
     * @param lineNumber the line number in the template
     * @return "True", "False", or the original input
     */
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

    /**
     * Returns the list of argument names for this filter.
     *
     * <p>This filter takes no arguments.</p>
     *
     * @return an empty list
     */
    @Override
    public List<String> getArgumentNames() {
        return List.of();
    }
}
