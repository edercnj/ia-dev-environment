package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.domain.stack.DatabaseSettingsMapping;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Copies conditional resource files (database, cache, security) to skill KPs. */
public final class RulesConditionals {

    private static final String NONE_VALUE = "none";
    /** Maps each database name to its category directory. */
    private static final Map<String, String>
            DB_CATEGORY_MAP = buildDbCategoryMap();

    private static Map<String, String>
            buildDbCategoryMap() {
        Map<String, String> map = new HashMap<>();
        for (String db : Set.of(
                "postgresql", "oracle", "mysql")) {
            map.put(db, "sql");
        }
        for (String db : Set.of(
                "mongodb", "cassandra", "eventstoredb")) {
            map.put(db, "nosql");
        }
        for (String db : Set.of("neo4j", "neptune")) {
            map.put(db, "graph");
        }
        for (String db : Set.of(
                "clickhouse", "druid")) {
            map.put(db, "columnar");
        }
        for (String db : Set.of(
                "yugabytedb", "cockroachdb", "tidb")) {
            map.put(db, "newsql");
        }
        for (String db : Set.of(
                "influxdb", "timescaledb")) {
            map.put(db, "timeseries");
        }
        for (String db : Set.of(
                "elasticsearch", "opensearch")) {
            map.put(db, "search");
        }
        return Map.copyOf(map);
    }

    private RulesConditionals() {
        // Utility class — no instantiation
    }

    /** Copies database reference files when configured. */
    public static List<String> copyDatabaseRefs(ConditionalCopyContext ctx) {
        String dbName = ctx.config().data().database().name();
        if (NONE_VALUE.equals(dbName)) {
            return List.of();
        }
        Path dbDir = ctx.resourceDir().resolve("knowledge/databases");
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
        if (!DatabaseSettingsMapping.CACHE_SETTINGS_MAP
                .containsKey(cacheName)) {
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
            ProjectConfig config, Path resourceDir, Path skillsDir) {
        return RulesInfraConditionals.assembleCloudKnowledge(
                config, resourceDir, skillsDir);
    }

    /** Delegates to {@link RulesInfraConditionals}. */
    public static List<String> assembleInfraKnowledge(
            ProjectConfig config, Path resourceDir, Path skillsDir) {
        return RulesInfraConditionals.assembleInfraKnowledge(
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
        String category = DB_CATEGORY_MAP.get(dbName);
        if (category == null) {
            return List.of();
        }
        return copyCategoryFiles(
                category, dbName, dbDir, target);
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
        List<Path> entries = MarkdownFileScanner
                .listMarkdownFilesSorted(sourceDir);
        List<String> generated = new ArrayList<>();
        for (Path entry : entries) {
            Path dest = target.resolve(
                    entry.getFileName().toString());
            generated.add(
                    CopyHelpers.copyStaticFile(
                            entry, dest));
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
            if (framework.contains("..") || framework.contains("/")) {
                continue;
            }
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
