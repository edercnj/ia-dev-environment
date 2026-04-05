package dev.iadev.domain.qualitygate;

/**
 * A field extracted from a story data contract table.
 *
 * @param name        field name
 * @param type        declared type (e.g., "String", "int")
 * @param mandatory   true if the field is mandatory (M)
 */
public record DataContractField(
        String name,
        String type,
        boolean mandatory
) {
}
