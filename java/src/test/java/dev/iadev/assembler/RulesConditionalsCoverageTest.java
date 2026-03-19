package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Additional coverage tests for RulesConditionals —
 * targeting uncovered branches and edge cases.
 */
@DisplayName("RulesConditionals — coverage")
class RulesConditionalsCoverageTest {

    @Nested
    @DisplayName("copyDatabaseRefs — SQL types")
    class CopyDatabaseRefsSql {

        @Test
        @DisplayName("mysql copies SQL common + mysql dir")
        void mysqlCopiesSqlFiles(@TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("mysql", "8")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path sqlCommon = resourceDir.resolve(
                    "databases/sql/common");
            Files.createDirectories(sqlCommon);
            Files.writeString(
                    sqlCommon.resolve("sql-common.md"),
                    "SQL common");
            Path sqlMysql = resourceDir.resolve(
                    "databases/sql/mysql");
            Files.createDirectories(sqlMysql);
            Files.writeString(
                    sqlMysql.resolve("mysql-types.md"),
                    "MySQL types");

            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.copyDatabaseRefs(
                            config, resourceDir,
                            skillsDir,
                            new TemplateEngine(),
                            Map.of());

            assertThat(result).hasSize(2);
            assertThat(result.get(0))
                    .contains("sql-common.md");
            assertThat(result.get(1))
                    .contains("mysql-types.md");
        }

        @Test
        @DisplayName("oracle copies SQL common + oracle dir")
        void oracleCopiesSqlFiles(@TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("oracle", "19")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path sqlCommon = resourceDir.resolve(
                    "databases/sql/common");
            Files.createDirectories(sqlCommon);
            Files.writeString(
                    sqlCommon.resolve("sql-base.md"),
                    "SQL base");

            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.copyDatabaseRefs(
                            config, resourceDir,
                            skillsDir,
                            new TemplateEngine(),
                            Map.of());

            assertThat(result).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("copyDatabaseRefs — NoSQL types")
    class CopyDatabaseRefsNosql {

        @Test
        @DisplayName("cassandra copies NoSQL common"
                + " + cassandra dir")
        void cassandraCopiesNosqlFiles(
                @TempDir Path tempDir) throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("cassandra", "4")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path nosqlCommon = resourceDir.resolve(
                    "databases/nosql/common");
            Files.createDirectories(nosqlCommon);
            Files.writeString(
                    nosqlCommon.resolve("nosql-base.md"),
                    "NoSQL base");
            Path nosqlCassandra = resourceDir.resolve(
                    "databases/nosql/cassandra");
            Files.createDirectories(nosqlCassandra);
            Files.writeString(
                    nosqlCassandra.resolve(
                            "cassandra-patterns.md"),
                    "Cassandra patterns");

            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.copyDatabaseRefs(
                            config, resourceDir,
                            skillsDir,
                            new TemplateEngine(),
                            Map.of());

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("unknown db type copies no type files")
        void unknownDbNoCopy(@TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("h2", "2")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path dbDir = resourceDir.resolve("databases");
            Files.createDirectories(dbDir);

            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.copyDatabaseRefs(
                            config, resourceDir,
                            skillsDir,
                            new TemplateEngine(),
                            Map.of());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("copyDatabaseRefs — version matrix")
    class VersionMatrix {

        @Test
        @DisplayName("no version matrix file returns empty")
        void noVersionMatrixReturnsEmpty(
                @TempDir Path tempDir) throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("postgresql", "16")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path dbDir = resourceDir.resolve("databases");
            Files.createDirectories(dbDir);

            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.copyDatabaseRefs(
                            config, resourceDir,
                            skillsDir,
                            new TemplateEngine(),
                            Map.of());

            assertThat(result).noneMatch(
                    f -> f.contains("version-matrix"));
        }
    }

    @Nested
    @DisplayName("copyCacheRefs — edge cases")
    class CopyCacheRefsEdgeCases {

        @Test
        @DisplayName("cache common dir missing returns"
                + " only specific cache")
        void cacheCommonMissing(@TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .cache("redis", "7.4")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path cacheRedis = resourceDir.resolve(
                    "databases/cache/redis");
            Files.createDirectories(cacheRedis);
            Files.writeString(
                    cacheRedis.resolve("redis.md"),
                    "Redis content");

            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.copyCacheRefs(
                            config, resourceDir, skillsDir);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("cache specific dir missing returns"
                + " only common")
        void cacheSpecificMissing(@TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .cache("memcached", "1.6")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path common = resourceDir.resolve(
                    "databases/cache/common");
            Files.createDirectories(common);
            Files.writeString(
                    common.resolve("cache-basics.md"),
                    "Cache basics");

            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.copyCacheRefs(
                            config, resourceDir, skillsDir);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("assembleSecurityRules — with frameworks")
    class SecurityWithFrameworks {

        @Test
        @DisplayName("security frameworks copies base"
                + " and compliance files")
        void copiesBaseAndCompliance(
                @TempDir Path tempDir) throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .securityFrameworks("owasp", "pci-dss")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path secDir = resourceDir.resolve("security");
            Files.createDirectories(secDir);
            Files.writeString(
                    secDir.resolve(
                            "application-security.md"),
                    "App security content");
            Files.writeString(
                    secDir.resolve("cryptography.md"),
                    "Crypto content");
            Path compDir = secDir.resolve("compliance");
            Files.createDirectories(compDir);
            Files.writeString(
                    compDir.resolve("owasp.md"),
                    "OWASP content");
            Files.writeString(
                    compDir.resolve("pci-dss.md"),
                    "PCI content");

            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.assembleSecurityRules(
                            config, resourceDir, skillsDir);

            assertThat(result).hasSize(4);
            assertThat(result).anyMatch(
                    f -> f.contains("application-security"));
            assertThat(result).anyMatch(
                    f -> f.contains("cryptography"));
            assertThat(result).anyMatch(
                    f -> f.contains("owasp.md"));
            assertThat(result).anyMatch(
                    f -> f.contains("pci-dss.md"));
        }

        @Test
        @DisplayName("security base files missing"
                + " returns only compliance")
        void baseFilesMissing(@TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .securityFrameworks("owasp")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path secDir = resourceDir.resolve("security");
            Path compDir = secDir.resolve("compliance");
            Files.createDirectories(compDir);
            Files.writeString(
                    compDir.resolve("owasp.md"),
                    "OWASP content");

            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.assembleSecurityRules(
                            config, resourceDir, skillsDir);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("compliance file missing for"
                + " framework is skipped")
        void complianceFileMissing(@TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .securityFrameworks("unknown-fw")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path secDir = resourceDir.resolve("security");
            Files.createDirectories(secDir);
            Files.writeString(
                    secDir.resolve(
                            "application-security.md"),
                    "App sec");
            Path compDir = secDir.resolve("compliance");
            Files.createDirectories(compDir);

            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.assembleSecurityRules(
                            config, resourceDir, skillsDir);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("assembleCloudKnowledge — with provider")
    class CloudWithProvider {

        @Test
        @DisplayName("aws provider copies cloud file")
        void awsCopiesCloudFile(@TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .cloudProvider("aws")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path cloudDir = resourceDir.resolve(
                    "cloud-providers");
            Files.createDirectories(cloudDir);
            Files.writeString(
                    cloudDir.resolve("aws.md"),
                    "AWS patterns");

            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.assembleCloudKnowledge(
                            config, resourceDir, skillsDir);

            assertThat(result).hasSize(1);
            assertThat(result.get(0))
                    .contains("cloud-aws.md");
        }

        @Test
        @DisplayName("provider file missing returns empty")
        void providerFileMissing(@TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .cloudProvider("gcp")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path cloudDir = resourceDir.resolve(
                    "cloud-providers");
            Files.createDirectories(cloudDir);

            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.assembleCloudKnowledge(
                            config, resourceDir, skillsDir);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("empty provider returns empty")
        void emptyProviderReturnsEmpty(
                @TempDir Path tempDir) {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .cloudProvider("")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.assembleCloudKnowledge(
                            config, resourceDir, skillsDir);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("assembleInfraKnowledge — K8s, container,"
            + " IaC")
    class InfraKnowledge {

        @Test
        @DisplayName("kubernetes copies k8s deployment"
                + " patterns")
        void k8sCopiesDeployment(@TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .orchestrator("kubernetes")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path k8sDir = resourceDir.resolve(
                    "infrastructure/kubernetes");
            Files.createDirectories(k8sDir);
            Files.writeString(
                    k8sDir.resolve(
                            "deployment-patterns.md"),
                    "K8s patterns");

            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.assembleInfraKnowledge(
                            config, resourceDir, skillsDir);

            assertThat(result).anyMatch(
                    f -> f.contains("k8s-deployment.md"));
        }

        @Test
        @DisplayName("kubernetes file missing returns empty"
                + " k8s result")
        void k8sFileMissing(@TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .orchestrator("kubernetes")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path k8sDir = resourceDir.resolve(
                    "infrastructure/kubernetes");
            Files.createDirectories(k8sDir);

            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.assembleInfraKnowledge(
                            config, resourceDir, skillsDir);

            assertThat(result).noneMatch(
                    f -> f.contains("k8s-deployment"));
        }

        @Test
        @DisplayName("docker copies container files")
        void dockerCopiesContainerFiles(
                @TempDir Path tempDir) throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("docker")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path containers = resourceDir.resolve(
                    "infrastructure/containers");
            Files.createDirectories(containers);
            Files.writeString(
                    containers.resolve(
                            "dockerfile-patterns.md"),
                    "Dockerfile patterns");
            Files.writeString(
                    containers.resolve(
                            "registry-patterns.md"),
                    "Registry patterns");

            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.assembleInfraKnowledge(
                            config, resourceDir, skillsDir);

            assertThat(result).hasSize(2);
            assertThat(result).anyMatch(
                    f -> f.contains("dockerfile.md"));
            assertThat(result).anyMatch(
                    f -> f.contains("registry.md"));
        }

        @Test
        @DisplayName("container=none returns no"
                + " container files")
        void noContainerFiles(@TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("none")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir);

            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.assembleInfraKnowledge(
                            config, resourceDir, skillsDir);

            assertThat(result).noneMatch(
                    f -> f.contains("dockerfile"));
        }

        @Test
        @DisplayName("container files missing returns"
                + " empty")
        void containerFilesMissing(@TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("docker")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir);

            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.assembleInfraKnowledge(
                            config, resourceDir, skillsDir);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("terraform copies IaC patterns")
        void terraformCopiesIac(@TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .iac("terraform")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path iacDir = resourceDir.resolve(
                    "infrastructure/iac");
            Files.createDirectories(iacDir);
            Files.writeString(
                    iacDir.resolve(
                            "terraform-patterns.md"),
                    "Terraform patterns");

            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.assembleInfraKnowledge(
                            config, resourceDir, skillsDir);

            assertThat(result).anyMatch(
                    f -> f.contains("iac-terraform.md"));
        }

        @Test
        @DisplayName("iac=none returns no IaC files")
        void noIacFiles(@TempDir Path tempDir) {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .iac("none")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.assembleInfraKnowledge(
                            config, resourceDir, skillsDir);

            assertThat(result).noneMatch(
                    f -> f.contains("iac-"));
        }

        @Test
        @DisplayName("empty iac returns no IaC files")
        void emptyIacReturnsEmpty(@TempDir Path tempDir) {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .iac("")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.assembleInfraKnowledge(
                            config, resourceDir, skillsDir);

            assertThat(result).noneMatch(
                    f -> f.contains("iac-"));
        }

        @Test
        @DisplayName("iac file missing returns empty")
        void iacFileMissing(@TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .iac("pulumi")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path iacDir = resourceDir.resolve(
                    "infrastructure/iac");
            Files.createDirectories(iacDir);

            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.assembleInfraKnowledge(
                            config, resourceDir, skillsDir);

            assertThat(result).noneMatch(
                    f -> f.contains("iac-"));
        }
    }

    @Nested
    @DisplayName("copyDatabaseRefs — placeholder"
            + " replacement")
    class PlaceholderReplacement {

        @Test
        @DisplayName("database files have placeholders"
                + " replaced")
        void placeholdersReplaced(@TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("postgresql", "16")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path dbDir = resourceDir.resolve("databases");
            Files.createDirectories(dbDir);
            Files.writeString(
                    dbDir.resolve("version-matrix.md"),
                    "DB: {{DATABASE_NAME}}");

            Path skillsDir = tempDir.resolve("skills");
            Map<String, Object> context = Map.of(
                    "database_name", "postgresql");

            List<String> result =
                    RulesConditionals.copyDatabaseRefs(
                            config, resourceDir,
                            skillsDir,
                            new TemplateEngine(),
                            context);

            assertThat(result).isNotEmpty();
            Path target = skillsDir.resolve(
                    "database-patterns/references/"
                            + "version-matrix.md");
            String content = Files.readString(
                    target, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("postgresql");
        }
    }
}
