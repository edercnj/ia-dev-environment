package dev.iadev.domain.implementationmap;

/**
 * Facade for the complete implementation map parsing pipeline.
 *
 * <p>Orchestrates: parse markdown -> build DAG -> validate ->
 * compute phases -> find critical path -> mark critical path
 * -> produce ParsedMap.</p>
 */
public final class ImplementationMapParser {

    private ImplementationMapParser() {
    }

    /**
     * Parses IMPLEMENTATION-MAP.md content into a structured
     * ParsedMap.
     *
     * @param content the full markdown content
     * @return the parsed implementation map
     * @throws MapParseException if markdown is invalid
     * @throws CircularDependencyException if cycle detected
     * @throws InvalidDagException if DAG is structurally invalid
     */
    public static ParsedMap parse(String content) {
        var rows = MarkdownParser.parse(content);
        var dag = DagBuilder.build(rows);
        var warnings = DagValidator.validate(dag);
        var phases = PhaseComputer.compute(dag);
        var criticalPath = CriticalPathFinder.find(dag, phases);
        CriticalPathFinder.markCriticalPath(dag, criticalPath);

        return new ParsedMap(
                dag,
                phases,
                criticalPath,
                phases.size(),
                warnings
        );
    }
}
