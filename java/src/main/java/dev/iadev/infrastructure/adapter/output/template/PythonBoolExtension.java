package dev.iadev.infrastructure.adapter.output.template;

import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Filter;

import java.util.Map;

/**
 * Pebble extension that registers the {@code python_bool}
 * filter.
 *
 * <p>This extension is an infrastructure adapter implementation
 * detail, registered with the PebbleEngine to provide
 * Python-style boolean formatting.</p>
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
