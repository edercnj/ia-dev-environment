package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Evaluates project configuration conditions and copies
 * conditional resource files to skill knowledge packs.
 *
 * <p>Each conditional function checks a project feature
 * (database, cache, security, cloud, infrastructure) and
 * copies the corresponding resource files when the feature
 * is enabled. Output must match the TypeScript implementation
 * byte-for-byte (RULE-001).</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * List<String> files = RulesConditionals
 *     .copyDatabaseRefs(config, resourcesDir,
 *         skillsDir, engine, context);
 * }</pre>
 * </p>
 *
 * @see RulesAssembler
 * @see ConditionEvaluator
 */
public final class RulesConditionals {

    private static final String NONE_VALUE = "none";
    private static final Set<String> SQL_DB_TYPES =
            Set.of("postgresql", "oracle", "mysql");
    private static final Set<String> NOSQL_DB_TYPES =
            Set.of("mongodb", "cassandra");

    private RulesConditionals() {
        // Utility class — no instantiation
    }

    /**
     * Copies database reference files when database is
     * configured.
     *
     * @param ctx the conditional copy context containing
     *            config, directories, engine, and context
     * @return list of generated file paths
     */
    public static List<String> copyDatabaseRefs(
            ConditionalCopyContext ctx) {
        String dbName =
                ctx.config().data().database().name();
        if (NONE_VALUE.equals(dbName)) {
            return List.of();
        }
        Path dbDir =
                ctx.resourceDir().resolve("databases");
        Path target = ctx.skillsDir().resolve(
                "database-patterns/references");
        CopyHelpers.ensureDirectory(target);
        List<String> generated = new ArrayList<>();
        generated.addAll(
                copyDbVersionMatrix(dbDir, target));
        generated.addAll(
                copyDbTypeFiles(dbName, dbDir, target));
        CopyHelpers.replacePlaceholdersInDir(
                target, ctx.engine(), ctx.context());
        return generated;
    }

    /**
     * Copies cache reference files when cache is configured.
     *
     * @param config      the project configuration
     * @param resourceDir the resources root directory
     * @param skillsDir   the skills output directory
     * @return list of generated file paths
     */
    public static List<String> copyCacheRefs(
            ProjectConfig config,
            Path resourceDir,
            Path skillsDir) {
        String cacheName = config.data().cache().name();
        if (NONE_VALUE.equals(cacheName)) {
            return List.of();
        }
        Path dbDir = resourceDir.resolve("databases");
        Path target = skillsDir.resolve(
                "database-patterns/references");
        CopyHelpers.ensureDirectory(target);
        List<String> generated = new ArrayList<>();
        generated.addAll(copyMdDir(
                dbDir.resolve("cache/common"), target));
        generated.addAll(copyMdDir(
                dbDir.resolve("cache/" + cacheName),
                target));
        return generated;
    }

    /**
     * Copies security files when security frameworks are
     * configured.
     *
     * @param config      the project configuration
     * @param resourceDir the resources root directory
     * @param skillsDir   the skills output directory
     * @return list of generated file paths
     */
    public static List<String> assembleSecurityRules(
            ProjectConfig config,
            Path resourceDir,
            Path skillsDir) {
        if (config.security().frameworks().isEmpty()) {
            return List.of();
        }
        Path secDir = resourceDir.resolve("security");
        List<String> generated = new ArrayList<>();
        generated.addAll(
                copySecurityBase(secDir, skillsDir));
        generated.addAll(
                copyCompliance(config, secDir, skillsDir));
        return generated;
    }

    /**
     * Copies cloud provider files when cloud is configured.
     *
     * @param config      the project configuration
     * @param resourceDir the resources root directory
     * @param skillsDir   the skills output directory
     * @return list of generated file paths
     */
    public static List<String> assembleCloudKnowledge(
            ProjectConfig config,
            Path resourceDir,
            Path skillsDir) {
        String provider =
                config.infrastructure().cloudProvider();
        if (NONE_VALUE.equals(provider)
                || provider == null
                || provider.isEmpty()) {
            return List.of();
        }
        Path cloudDir =
                resourceDir.resolve("cloud-providers");
        Path kpDir =
                skillsDir.resolve("knowledge-packs");
        CopyHelpers.ensureDirectory(kpDir);
        Path src = cloudDir.resolve(provider + ".md");
        if (java.nio.file.Files.exists(src)
                && java.nio.file.Files.isRegularFile(src)) {
            Path dest = kpDir.resolve(
                    "cloud-" + provider + ".md");
            return List.of(
                    CopyHelpers.copyStaticFile(src, dest));
        }
        return List.of();
    }

    /**
     * Copies infrastructure knowledge packs when infra
     * features are configured.
     *
     * @param config      the project configuration
     * @param resourceDir the resources root directory
     * @param skillsDir   the skills output directory
     * @return list of generated file paths
     */
    public static List<String> assembleInfraKnowledge(
            ProjectConfig config,
            Path resourceDir,
            Path skillsDir) {
        Path infraDir = resourceDir.resolve("infrastructure");
        Path kpDir = skillsDir.resolve("knowledge-packs");
        CopyHelpers.ensureDirectory(kpDir);
        List<String> generated = new ArrayList<>();
        generated.addAll(
                copyK8sFiles(config, infraDir, kpDir));
        generated.addAll(
                copyContainerFiles(config, infraDir, kpDir));
        generated.addAll(
                copyIacFiles(config, infraDir, kpDir));
        return generated;
    }

    private static List<String> copyDbVersionMatrix(
            Path dbDir, Path target) {
        Path matrix = dbDir.resolve("version-matrix.md");
        if (java.nio.file.Files.exists(matrix)
                && java.nio.file.Files
                .isRegularFile(matrix)) {
            Path dest = target.resolve("version-matrix.md");
            return List.of(
                    CopyHelpers.copyStaticFile(
                            matrix, dest));
        }
        return List.of();
    }

    private static List<String> copyDbTypeFiles(
            String dbName, Path dbDir, Path target) {
        List<String> generated = new ArrayList<>();
        if (SQL_DB_TYPES.contains(dbName)) {
            generated.addAll(copyMdDir(
                    dbDir.resolve("sql/common"), target));
            generated.addAll(copyMdDir(
                    dbDir.resolve("sql/" + dbName), target));
        } else if (NOSQL_DB_TYPES.contains(dbName)) {
            generated.addAll(copyMdDir(
                    dbDir.resolve("nosql/common"), target));
            generated.addAll(copyMdDir(
                    dbDir.resolve("nosql/" + dbName),
                    target));
        }
        return generated;
    }

    private static List<String> copyMdDir(
            Path sourceDir, Path target) {
        if (!java.nio.file.Files.exists(sourceDir)
                || !java.nio.file.Files
                .isDirectory(sourceDir)) {
            return List.of();
        }
        List<String> generated = new ArrayList<>();
        try (var stream =
                     java.nio.file.Files.list(sourceDir)) {
            List<Path> entries = stream
                    .filter(f -> f.toString().endsWith(".md"))
                    .sorted()
                    .toList();
            for (Path entry : entries) {
                if (!java.nio.file.Files
                        .isRegularFile(entry)) {
                    continue;
                }
                Path dest = target.resolve(
                        entry.getFileName().toString());
                generated.add(
                        CopyHelpers.copyStaticFile(
                                entry, dest));
            }
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(
                    "Failed to list directory: "
                            + sourceDir, e);
        }
        return generated;
    }

    private static List<String> copySecurityBase(
            Path secDir, Path skillsDir) {
        Path secKp =
                skillsDir.resolve("security/references");
        CopyHelpers.ensureDirectory(secKp);
        List<String> generated = new ArrayList<>();
        for (String name : List.of(
                "application-security.md",
                "cryptography.md")) {
            Path src = secDir.resolve(name);
            if (java.nio.file.Files.exists(src)
                    && java.nio.file.Files
                    .isRegularFile(src)) {
                Path dest = secKp.resolve(name);
                generated.add(
                        CopyHelpers.copyStaticFile(
                                src, dest));
            }
        }
        return generated;
    }

    private static List<String> copyCompliance(
            ProjectConfig config,
            Path secDir, Path skillsDir) {
        Path compKp =
                skillsDir.resolve("compliance/references");
        CopyHelpers.ensureDirectory(compKp);
        List<String> generated = new ArrayList<>();
        for (String framework :
                config.security().frameworks()) {
            Path src = secDir.resolve(
                    "compliance/" + framework + ".md");
            if (java.nio.file.Files.exists(src)
                    && java.nio.file.Files
                    .isRegularFile(src)) {
                Path dest = compKp.resolve(
                        framework + ".md");
                generated.add(
                        CopyHelpers.copyStaticFile(
                                src, dest));
            }
        }
        return generated;
    }

    private static List<String> copyK8sFiles(
            ProjectConfig config,
            Path infraDir, Path kpDir) {
        if (!"kubernetes".equals(
                config.infrastructure().orchestrator())) {
            return List.of();
        }
        Path src = infraDir.resolve(
                "kubernetes/deployment-patterns.md");
        if (java.nio.file.Files.exists(src)
                && java.nio.file.Files
                .isRegularFile(src)) {
            Path dest = kpDir.resolve(
                    "k8s-deployment.md");
            return List.of(
                    CopyHelpers.copyStaticFile(src, dest));
        }
        return List.of();
    }

    private static List<String> copyContainerFiles(
            ProjectConfig config,
            Path infraDir, Path kpDir) {
        if (NONE_VALUE.equals(
                config.infrastructure().container())) {
            return List.of();
        }
        List<String> generated = new ArrayList<>();
        String[][] filePairs = {
                {"dockerfile-patterns.md",
                        "dockerfile.md"},
                {"registry-patterns.md",
                        "registry.md"}
        };
        for (String[] pair : filePairs) {
            Path src = infraDir.resolve(
                    "containers/" + pair[0]);
            if (java.nio.file.Files.exists(src)
                    && java.nio.file.Files
                    .isRegularFile(src)) {
                Path dest = kpDir.resolve(pair[1]);
                generated.add(
                        CopyHelpers.copyStaticFile(
                                src, dest));
            }
        }
        return generated;
    }

    private static List<String> copyIacFiles(
            ProjectConfig config,
            Path infraDir, Path kpDir) {
        String iac = config.infrastructure().iac();
        if (NONE_VALUE.equals(iac)
                || iac == null || iac.isEmpty()) {
            return List.of();
        }
        Path src = infraDir.resolve(
                "iac/" + iac + "-patterns.md");
        if (java.nio.file.Files.exists(src)
                && java.nio.file.Files
                .isRegularFile(src)) {
            Path dest = kpDir.resolve(
                    "iac-" + iac + ".md");
            return List.of(
                    CopyHelpers.copyStaticFile(src, dest));
        }
        return List.of();
    }
}
