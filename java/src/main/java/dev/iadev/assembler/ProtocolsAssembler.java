package dev.iadev.assembler;

import dev.iadev.domain.stack.ProtocolMapping;
import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Assembles {@code .claude/skills/protocols/references/}
 * from protocol documentation files.
 *
 * <p>This is the fifth assembler in the pipeline (position
 * 5 of 23 per RULE-005). It generates concatenated
 * protocol convention files named
 * {@code {protocol}-conventions.md} in the references
 * directory.</p>
 *
 * <p>Protocol files are concatenated raw — no template
 * placeholder replacement. The {@code engine} parameter
 * is accepted for API uniformity but is not used.</p>
 *
 * <p>If no interface types are defined, no files are
 * generated and an empty list is returned.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * Assembler protocols = new ProtocolsAssembler();
 * List<String> files = protocols.assemble(
 *     config, engine, outputDir);
 * }</pre>
 * </p>
 *
 * @see Assembler
 * @see ProtocolMapping
 */
public final class ProtocolsAssembler implements Assembler {

    private static final String SKILLS_DIR = "skills";
    private static final String PROTOCOLS_SKILL_DIR =
            "protocols";
    private static final String REFERENCES_DIR =
            "references";
    private static final String PROTOCOLS_RES_DIR =
            "protocols";
    private static final String CONVENTIONS_SUFFIX =
            "-conventions.md";
    private static final String PROTOCOL_SEPARATOR =
            "\n\n---\n\n";
    private static final String MESSAGING_PROTOCOL =
            "messaging";

    private final Path resourcesDir;

    /**
     * Creates a ProtocolsAssembler using classpath
     * resources.
     */
    public ProtocolsAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a ProtocolsAssembler with an explicit
     * resources directory.
     *
     * @param resourcesDir the base resources directory
     */
    public ProtocolsAssembler(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Derives protocol names from interface types,
     * collects source files per protocol, and concatenates
     * each group into a single conventions file.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        List<String> protocolNames =
                ProtocolMapping.deriveProtocols(config);
        if (protocolNames.isEmpty()) {
            return List.of();
        }

        Map<String, List<Path>> protocolFiles =
                collectProtocolFiles(
                        protocolNames, config);
        if (protocolFiles.isEmpty()) {
            return List.of();
        }

        return generateOutput(protocolFiles, outputDir);
    }

    /**
     * Collects source .md files for each protocol.
     *
     * <p>For the messaging protocol, applies broker-specific
     * filtering via
     * {@link #selectMessagingFiles(Path, ProjectConfig)}.
     * For other protocols, all .md files in the protocol
     * directory are included.</p>
     *
     * @param protocolNames the derived protocol names
     * @param config        the project configuration
     * @return map of protocol name to source file paths
     */
    private Map<String, List<Path>> collectProtocolFiles(
            List<String> protocolNames,
            ProjectConfig config) {
        Path protocolsRoot =
                resourcesDir.resolve(PROTOCOLS_RES_DIR);
        Map<String, List<Path>> result =
                new LinkedHashMap<>();

        for (String protocol : protocolNames) {
            Path protocolDir =
                    protocolsRoot.resolve(protocol);
            if (!Files.exists(protocolDir)
                    || !Files.isDirectory(protocolDir)) {
                continue;
            }

            List<Path> files;
            if (MESSAGING_PROTOCOL.equals(protocol)) {
                files = selectMessagingFiles(
                        protocolDir, config);
            } else {
                files = listMdFilesSorted(protocolDir);
            }

            if (!files.isEmpty()) {
                result.put(protocol, files);
            }
        }
        return result;
    }

    /**
     * Selects messaging protocol files based on the broker.
     *
     * <p>If a broker is specified and a matching .md file
     * exists, only that file is returned. Otherwise, all
     * .md files in the directory are returned.</p>
     *
     * @param messagingDir the messaging protocol directory
     * @param config       the project configuration
     * @return list of selected messaging files
     */
    private List<Path> selectMessagingFiles(
            Path messagingDir,
            ProjectConfig config) {
        String broker =
                ProtocolMapping.extractBroker(config);
        if (!broker.isEmpty()) {
            Path specific =
                    messagingDir.resolve(broker + ".md");
            if (Files.exists(specific)
                    && Files.isRegularFile(specific)) {
                return List.of(specific);
            }
        }
        return listMdFilesSorted(messagingDir);
    }

    /**
     * Generates output files by concatenating source files
     * per protocol.
     *
     * @param protocolFiles map of protocol to source files
     * @param outputDir     the output directory
     * @return list of generated file paths
     */
    private List<String> generateOutput(
            Map<String, List<Path>> protocolFiles,
            Path outputDir) {
        Path refsDir = outputDir
                .resolve(SKILLS_DIR)
                .resolve(PROTOCOLS_SKILL_DIR)
                .resolve(REFERENCES_DIR);
        CopyHelpers.ensureDirectory(refsDir);

        List<String> results = new ArrayList<>();
        List<String> sortedProtocols =
                new ArrayList<>(protocolFiles.keySet());
        sortedProtocols.sort(String::compareTo);

        for (String protocol : sortedProtocols) {
            List<Path> files =
                    protocolFiles.get(protocol);
            String destName =
                    protocol + CONVENTIONS_SUFFIX;
            Path destPath = refsDir.resolve(destName);
            results.add(
                    concatProtocolFiles(files, destPath));
        }
        return results;
    }

    /**
     * Concatenates multiple source files into a single
     * output file with section separators.
     *
     * @param sourceFiles the source files to concatenate
     * @param destPath    the destination file path
     * @return the destination path as a string
     */
    private String concatProtocolFiles(
            List<Path> sourceFiles,
            Path destPath) {
        List<String> sections = new ArrayList<>();
        for (Path file : sourceFiles) {
            try {
                sections.add(Files.readString(
                        file, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(
                        "Failed to read protocol file: "
                                + file, e);
            }
        }
        String content =
                String.join(PROTOCOL_SEPARATOR, sections);
        try {
            Files.writeString(
                    destPath, content,
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to write protocol file: "
                            + destPath, e);
        }
        return destPath.toString();
    }

    private static List<Path> listMdFilesSorted(Path dir) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(f -> f.toString()
                            .endsWith(".md"))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to list directory: " + dir, e);
        }
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(PROTOCOLS_RES_DIR);
    }
}
