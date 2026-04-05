package dev.iadev.domain.qualitygate;

import java.util.List;

/**
 * Parsed story content for quality gate evaluation.
 */
record ParsedContent(
        List<DataContractField> fields,
        List<String> dependencies,
        List<String> epicIndex) {
}
