package dev.iadev.exception;

/**
 * Thrown when a required resource (file, template, configuration)
 * cannot be found via any search strategy.
 *
 * <p>Carries the resource path that was searched for and a description
 * of the strategies that were attempted (classpath, filesystem, etc.),
 * enabling clear diagnosis of missing resources.</p>
 *
 * <p>Application-level exception — used across multiple layers.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * throw new ResourceNotFoundException(
 *     "templates/missing.txt",
 *     "classpath, filesystem(/opt/resources)");
 * }</pre>
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourcePath;
    private final String searchStrategies;

    /**
     * Creates a resource-not-found exception.
     *
     * @param resourcePath     the relative path of the resource
     * @param searchStrategies description of strategies attempted
     */
    public ResourceNotFoundException(
            String resourcePath, String searchStrategies) {
        super("Resource not found: %s. Searched via: %s".formatted(
                resourcePath, searchStrategies));
        this.resourcePath = resourcePath;
        this.searchStrategies = searchStrategies;
    }

    /**
     * Returns the relative path of the resource that was not found.
     *
     * @return the resource path
     */
    public String getResourcePath() {
        return resourcePath;
    }

    /**
     * Returns the search strategies that were attempted.
     *
     * @return the search strategies description
     */
    public String getSearchStrategies() {
        return searchStrategies;
    }

    @Override
    public String toString() {
        return "ResourceNotFoundException{resourcePath='%s', strategies='%s'}"
                .formatted(resourcePath, searchStrategies);
    }
}
