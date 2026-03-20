package dev.iadev.template;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import io.pebbletemplates.pebble.loader.DelegatingLoader;
import io.pebbletemplates.pebble.loader.FileLoader;
import io.pebbletemplates.pebble.loader.Loader;
import io.pebbletemplates.pebble.loader.StringLoader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory methods for creating PebbleEngine instances
 * with the project's standard configuration.
 *
 * <p>Extracted from {@link TemplateEngine} to keep both
 * classes under 250 lines per RULE-004.</p>
 *
 * @see TemplateEngine
 */
final class TemplateEngineFactory {

    private TemplateEngineFactory() {
        // utility class
    }

    /**
     * Builds a PebbleEngine for file-based template
     * loading.
     *
     * @param basePath the base directory for filesystem
     *                 templates, or null for classpath only
     * @return the configured PebbleEngine
     */
    static PebbleEngine buildFileEngine(Path basePath) {
        List<Loader<?>> loaders = new ArrayList<>(2);
        if (basePath != null) {
            FileLoader fileLoader = new FileLoader();
            fileLoader.setPrefix(
                    basePath.toAbsolutePath().toString());
            loaders.add(fileLoader);
        }
        ClasspathLoader classpathLoader =
                new ClasspathLoader();
        loaders.add(classpathLoader);

        Loader<?> loader = loaders.size() == 1
                ? loaders.getFirst()
                : new DelegatingLoader(loaders);

        return new PebbleEngine.Builder()
                .loader(loader)
                .autoEscaping(false)
                .strictVariables(false)
                .newLineTrimming(false)
                .extension(new PythonBoolExtension())
                .build();
    }

    /**
     * Builds a PebbleEngine for string-based template
     * rendering.
     *
     * @return the configured PebbleEngine
     */
    static PebbleEngine buildStringEngine() {
        return new PebbleEngine.Builder()
                .loader(new StringLoader())
                .autoEscaping(false)
                .strictVariables(false)
                .newLineTrimming(false)
                .extension(new PythonBoolExtension())
                .build();
    }
}
