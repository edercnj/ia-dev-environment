package dev.iadev.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

/**
 * Extracts JAR resources to a temporary filesystem
 * directory for assembler access.
 *
 * <p>Extracted from {@link ResourceResolver} to keep
 * both classes under 250 lines per RULE-004.</p>
 *
 * @see ResourceResolver
 */
final class JarResourceExtractor {

    private JarResourceExtractor() {
        // utility class
    }

    /**
     * Extracts all non-class, non-META-INF resources from
     * a JAR to a temporary directory.
     *
     * @param url the JAR URL pointing to a resource
     * @return the path to the temp directory with
     *         extracted resources
     */
    static Path extractJarResources(URL url) {
        try {
            Path tempDir =
                    ResourceResolver.createSecureTempDir(
                            "ia-dev-env-res-");
            String jarUrl = url.toString();
            int bang = jarUrl.indexOf('!');
            String jarUri = jarUrl.substring(0, bang + 2);
            URI fsUri = URI.create(jarUri);

            try (FileSystem jarFs =
                         FileSystems.newFileSystem(
                                 fsUri, Map.of())) {
                Path jarRoot = jarFs.getPath("/");
                Files.walkFileTree(jarRoot,
                        new JarCopyVisitor(
                                jarRoot, tempDir));
            }
            return tempDir;
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to extract JAR resources", e);
        }
    }

    private static final class JarCopyVisitor
            extends SimpleFileVisitor<Path> {

        private final Path jarRoot;
        private final Path tempDir;

        JarCopyVisitor(Path jarRoot, Path tempDir) {
            this.jarRoot = jarRoot;
            this.tempDir = tempDir;
        }

        @Override
        public FileVisitResult preVisitDirectory(
                Path dir,
                BasicFileAttributes attrs)
                throws IOException {
            if (ResourceResolver.shouldSkip(dir)) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            Path target = tempDir.resolve(
                    jarRoot.relativize(dir).toString());
            Files.createDirectories(target);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(
                Path file,
                BasicFileAttributes attrs)
                throws IOException {
            String rel = jarRoot.relativize(file)
                    .toString();
            if (rel.endsWith(".class")
                    || rel.startsWith("META-INF/")) {
                return FileVisitResult.CONTINUE;
            }
            Path target = tempDir.resolve(rel);
            Files.createDirectories(target.getParent());
            try (InputStream is =
                         Files.newInputStream(file)) {
                Files.copy(is, target,
                        StandardCopyOption
                                .REPLACE_EXISTING);
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
