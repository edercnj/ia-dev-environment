package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Copies conditional resource files (database, cache,
 * security) to skill knowledge packs. Infrastructure
 * conditionals are in {@link RulesInfraConditionals}.
 *
 * @see RulesAssembler
 * @see ConditionEvaluator
 */
public final class RulesConditionals {

    private static final String NONE_VALUE = "none";
    private static final Set<String> SQL_DB_TYPES =
            Set.of("postgresql", "oracle", "mysql");
    private static final Set<String> NOSQL_DB_TYPES =
            Set.of("mongodb", "cassandra",
                    "eventstoredb");

    private static final Set<String> GRAPH_DB_TYPES =
            Set.of("neo4j", "neptune");
    private static final Set<String> COLUMNAR_DB_TYPES =
            Set.of("clickhouse", "druid");
    private static final Set<String> NEWSQL_DB_TYPES =
            Set.of("yugabytedb", "cockroachdb", "tidb");
    private static final Set<String> TIMESERIES_DB_TYPES =
            Set.of("influxdb", "timescaledb");
    private static final Set<String> SEARCH_DB_TYPES =
            Set.of("elasticsearch", "opensearch");

    /** Maps category set to directory name for routing. */
    private static final Map<Set<String>, String>
            DB_CATEGORY_MAP = Map.of(
            SQL_DB_TYPES, "sql",
            NOSQL_DB_TYPES, "nosql",
            GRAPH_DB_TYPES, "graph",
            COLUMNAR_DB_TYPES, "columnar",
            NEWSQL_DB_TYPES, "newsql",
            TIMESERIES_DB_TYPES, "timeseries",
            SEARCH_DB_TYPES, "search");

    private RulesConditionals() {
        // Utility class — no instantiation
    }

    /** Copies database reference files when configured. */
    public static List<String> copyDatabaseRefs(
            ConditionalCopyContext ctx) {
        String dbName =
                ctx.config().data().database().name();
        if (NONE_VALUE.equals(dbName)) {
            return List.of();
        }
        Path dbDir =
                ctx.resourceDir().resolve("knowledge/databases");
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

    /** Copies cache reference files when configured. */
    public static List<String> copyCacheRefs(
            ProjectConfig config,
            Path resourceDir,
            Path skillsDir) {
        String cacheName = config.data().cache().name();
        if (NONE_VALUE.equals(cacheName)) {
            return List.of();
        }
        Path dbDir = resourceDir.resolve("knowledge/databases");
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

    /** Copies security files when frameworks configured. */
    public static List<String> assembleSecurityRules(
            ProjectConfig config,
            Path resourceDir,
            Path skillsDir) {
        if (config.security().frameworks().isEmpty()) {
            return List.of();
        }
        Path secDir = resourceDir.resolve("knowledge/security");
        List<String> generated = new ArrayList<>();
        generated.addAll(
                copySecurityBase(secDir, skillsDir));
        generated.addAll(
                copyCompliance(config, secDir, skillsDir));
        return generated;
    }

    /** Delegates to {@link RulesInfraConditionals}. */
    public static List<String> assembleCloudKnowledge(
            ProjectConfig config,
            Path resourceDir,
            Path skillsDir) {
        return RulesInfraConditionals
                .assembleCloudKnowledge(
                        config, resourceDir, skillsDir);
    }

    /** Delegates to {@link RulesInfraConditionals}. */
    public static List<String> assembleInfraKnowledge(
            ProjectConfig config,
            Path resourceDir,
            Path skillsDir) {
        return RulesInfraConditionals
                .assembleInfraKnowledge(
                        config, resourceDir, skillsDir);
    }

    private static List<String> copyDbVersionMatrix(
            Path dbDir, Path target) {
        Path matrix = dbDir.resolve("version-matrix.md");
        if (Files.exists(matrix)
                && Files.isRegularFile(matrix)) {
            Path dest = target.resolve("version-matrix.md");
            return List.of(
                    CopyHelpers.copyStaticFile(
                            matrix, dest));
        }
        return List.of();
    }

    private static List<String> copyDbTypeFiles(
            String dbName, Path dbDir, Path target) {
        for (var entry : DB_CATEGORY_MAP.entrySet()) {
            if (entry.getKey().contains(dbName)) {
                return copyCategoryFiles(
                        entry.getValue(), dbName,
                        dbDir, target);
            }
        }
        return List.of();
    }

    private static List<String> copyCategoryFiles(
            String category, String dbName,
            Path dbDir, Path target) {
        List<String> generated = new ArrayList<>();
        generated.addAll(copyMdDir(
                dbDir.resolve(category + "/common"),
                target));
        generated.addAll(copyMdDir(
                dbDir.resolve(category + "/" + dbName),
                target));
        return generated;
    }

    static List<String> copyMdDir(
            Path sourceDir, Path target) {
        if (!Files.exists(sourceDir)
                || !Files.isDirectory(sourceDir)) {
            return List.of();
        }
        List<String> generated = new ArrayList<>();
        try (var stream = Files.list(sourceDir)) {
            List<Path> entries = stream
                    .filter(f -> f.toString().endsWith(".md"))
                    .sorted()
                    .toList();
            for (Path entry : entries) {
                if (!Files.isRegularFile(entry)) {
                    continue;
                }
                Path dest = target.resolve(
                        entry.getFileName().toString());
                generated.add(
                        CopyHelpers.copyStaticFile(
                                entry, dest));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(
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
            if (Files.exists(src)
                    && Files.isRegularFile(src)) {
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
            if (Files.exists(src)
                    && Files.isRegularFile(src)) {
                Path dest = compKp.resolve(
                        framework + ".md");
                generated.add(
                        CopyHelpers.copyStaticFile(
                                src, dest));
            }
        }
        return generated;
    }
}
