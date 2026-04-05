package dev.iadev.domain.scopeassessment;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StoryAnalyzerTest {

    @Nested
    class CountComponents {

        @Test
        void countComponents_emptyContent_returnsZero() {
            assertThat(StoryAnalyzer.countComponents(""))
                    .isZero();
        }

        @Test
        void countComponents_noJavaFiles_returnsZero() {
            var content = "This is a simple story with no code.";
            assertThat(StoryAnalyzer.countComponents(content))
                    .isZero();
        }

        @Test
        void countComponents_oneJavaFile_returnsOne() {
            var content = """
                    ## Tech Description
                    Modify `PaymentController.java` to add endpoint.
                    """;
            assertThat(StoryAnalyzer.countComponents(content))
                    .isEqualTo(1);
        }

        @Test
        void countComponents_threeJavaFiles_returnsThree() {
            var content = """
                    ## Tech Description
                    - `PaymentController.java`
                    - `PaymentService.java`
                    - `PaymentRepository.java`
                    """;
            assertThat(StoryAnalyzer.countComponents(content))
                    .isEqualTo(3);
        }

        @Test
        void countComponents_fiveJavaFiles_returnsFive() {
            var content = """
                    ## Tech Description
                    - `PaymentController.java`
                    - `PaymentService.java`
                    - `PaymentRepository.java`
                    - `PaymentMapper.java`
                    - `PaymentDto.java`
                    """;
            assertThat(StoryAnalyzer.countComponents(content))
                    .isEqualTo(5);
        }

        @Test
        void countComponents_duplicateFiles_countsDistinct() {
            var content = """
                    Modify `PaymentService.java` here.
                    Also change `PaymentService.java` there.
                    """;
            assertThat(StoryAnalyzer.countComponents(content))
                    .isEqualTo(1);
        }

        @Test
        void countComponents_mixedExtensions_countsAll() {
            var content = """
                    - `Controller.java`
                    - `schema.sql`
                    - `config.yaml`
                    - `Service.kt`
                    """;
            assertThat(StoryAnalyzer.countComponents(content))
                    .isEqualTo(4);
        }
    }

    @Nested
    class CountEndpoints {

        @Test
        void countEndpoints_emptyContent_returnsZero() {
            assertThat(StoryAnalyzer.countEndpoints(""))
                    .isZero();
        }

        @Test
        void countEndpoints_noEndpoints_returnsZero() {
            var content = "Simple change with no new APIs.";
            assertThat(StoryAnalyzer.countEndpoints(content))
                    .isZero();
        }

        @Test
        void countEndpoints_onePostEndpoint_returnsOne() {
            var content = """
                    ## Data Contract
                    POST /api/payments
                    """;
            assertThat(StoryAnalyzer.countEndpoints(content))
                    .isEqualTo(1);
        }

        @Test
        void countEndpoints_multipleEndpoints_returnsCount() {
            var content = """
                    ## Data Contract
                    POST /api/payments
                    GET /api/payments/{id}
                    DELETE /api/payments/{id}
                    """;
            assertThat(StoryAnalyzer.countEndpoints(content))
                    .isEqualTo(3);
        }

        @Test
        void countEndpoints_allHttpMethods_countsAll() {
            var content = """
                    POST /api/a
                    GET /api/b
                    PUT /api/c
                    DELETE /api/d
                    PATCH /api/e
                    """;
            assertThat(StoryAnalyzer.countEndpoints(content))
                    .isEqualTo(5);
        }

        @Test
        void countEndpoints_duplicateEndpoints_countsDistinct() {
            var content = """
                    POST /api/payments
                    POST /api/payments
                    """;
            assertThat(StoryAnalyzer.countEndpoints(content))
                    .isEqualTo(1);
        }
    }

    @Nested
    class HasSchemaChanges {

        @Test
        void hasSchemaChanges_emptyContent_returnsFalse() {
            assertThat(StoryAnalyzer.hasSchemaChanges(""))
                    .isFalse();
        }

        @Test
        void hasSchemaChanges_noMigration_returnsFalse() {
            var content = "Simple refactoring, no DB changes.";
            assertThat(StoryAnalyzer.hasSchemaChanges(content))
                    .isFalse();
        }

        @Test
        void hasSchemaChanges_migrationScript_returnsTrue() {
            var content = """
                    Apply the migration script to add column.
                    """;
            assertThat(StoryAnalyzer.hasSchemaChanges(content))
                    .isTrue();
        }

        @Test
        void hasSchemaChanges_alterTable_returnsTrue() {
            var content = """
                    ALTER TABLE payments ADD COLUMN status VARCHAR;
                    """;
            assertThat(StoryAnalyzer.hasSchemaChanges(content))
                    .isTrue();
        }

        @Test
        void hasSchemaChanges_createTable_returnsTrue() {
            var content = """
                    CREATE TABLE audit_log (id BIGINT PRIMARY KEY);
                    """;
            assertThat(StoryAnalyzer.hasSchemaChanges(content))
                    .isTrue();
        }

        @Test
        void hasSchemaChanges_dropTable_returnsTrue() {
            var content = """
                    DROP TABLE legacy_data;
                    """;
            assertThat(StoryAnalyzer.hasSchemaChanges(content))
                    .isTrue();
        }

        @Test
        void hasSchemaChanges_addColumn_returnsTrue() {
            var content = """
                    ADD COLUMN email VARCHAR(255);
                    """;
            assertThat(StoryAnalyzer.hasSchemaChanges(content))
                    .isTrue();
        }

        @Test
        void hasSchemaChanges_caseInsensitive_returnsTrue() {
            var content = "alter table payments add column x int;";
            assertThat(StoryAnalyzer.hasSchemaChanges(content))
                    .isTrue();
        }
    }

    @Nested
    class HasCompliance {

        @Test
        void hasCompliance_emptyContent_returnsFalse() {
            assertThat(StoryAnalyzer.hasCompliance(""))
                    .isFalse();
        }

        @Test
        void hasCompliance_noCompliance_returnsFalse() {
            var content = "No compliance requirements here.";
            assertThat(StoryAnalyzer.hasCompliance(content))
                    .isFalse();
        }

        @Test
        void hasCompliance_complianceNone_returnsFalse() {
            var content = "compliance: none\n";
            assertThat(StoryAnalyzer.hasCompliance(content))
                    .isFalse();
        }

        @Test
        void hasCompliance_pciDss_returnsTrue() {
            var content = "compliance: pci-dss\n";
            assertThat(StoryAnalyzer.hasCompliance(content))
                    .isTrue();
        }

        @Test
        void hasCompliance_hipaa_returnsTrue() {
            var content = "compliance: hipaa\n";
            assertThat(StoryAnalyzer.hasCompliance(content))
                    .isTrue();
        }

        @Test
        void hasCompliance_gdpr_returnsTrue() {
            var content = "compliance: gdpr\n";
            assertThat(StoryAnalyzer.hasCompliance(content))
                    .isTrue();
        }
    }

    @Nested
    class CountDependents {

        @Test
        void countDependents_emptyMap_returnsZero() {
            assertThat(StoryAnalyzer.countDependents("s-001", ""))
                    .isZero();
        }

        @Test
        void countDependents_noDependents_returnsZero() {
            var mapContent = """
                    | Story | Blocked By |
                    |-------|------------|
                    | s-001 | — |
                    | s-002 | s-003 |
                    """;
            assertThat(StoryAnalyzer.countDependents(
                    "s-001", mapContent)).isZero();
        }

        @Test
        void countDependents_oneDependent_returnsOne() {
            var mapContent = """
                    | Story | Blocked By |
                    |-------|------------|
                    | s-001 | — |
                    | s-002 | s-001 |
                    """;
            assertThat(StoryAnalyzer.countDependents(
                    "s-001", mapContent)).isEqualTo(1);
        }

        @Test
        void countDependents_multipleDependents_returnsCount() {
            var mapContent = """
                    | Story | Blocked By |
                    |-------|------------|
                    | s-001 | — |
                    | s-002 | s-001 |
                    | s-003 | s-001 |
                    | s-004 | s-001, s-002 |
                    """;
            assertThat(StoryAnalyzer.countDependents(
                    "s-001", mapContent)).isEqualTo(3);
        }
    }
}
