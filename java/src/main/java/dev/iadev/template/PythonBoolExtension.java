package dev.iadev.template;

import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Filter;

import java.util.Map;

/**
 * Pebble extension that registers the {@code python_bool} filter.
 *
 * <p>This extension is automatically registered with the Pebble
 * engine by {@link TemplateEngine} to provide Python-style
 * boolean formatting (RULE-002).</p>
 *
 * @see PythonBoolFilter
 */
final class PythonBoolExtension extends AbstractExtension {

    private static final String FILTER_NAME = "python_bool";

    @Override
    public Map<String, Filter> getFilters() {
        return Map.of(FILTER_NAME, new PythonBoolFilter());
    }
}
