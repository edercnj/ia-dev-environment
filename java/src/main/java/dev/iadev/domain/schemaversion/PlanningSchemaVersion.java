package dev.iadev.domain.schemaversion;

/**
 * Planning-pipeline schema version declared at {@code plans/epic-XXXX/execution-state.json}
 * root level. Introduced by EPIC-0038 story-0038-0008 to gate the task-first flow against
 * legacy (monolithic) epics 0025-0037.
 */
public enum PlanningSchemaVersion {

    /** Legacy top-down planning (story-as-unit). Default when the field is absent. */
    V1("1.0"),

    /** Task-first bottom-up planning (EPIC-0038 onwards). */
    V2("2.0");

    private final String wireValue;

    PlanningSchemaVersion(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}
