package dev.iadev.infrastructure.config;

import dev.iadev.domain.port.input.GenerateEnvironmentUseCase;
import dev.iadev.domain.port.input.ListStackProfilesUseCase;
import dev.iadev.domain.port.input.ValidateConfigUseCase;
import dev.iadev.domain.port.output.CheckpointStore;
import dev.iadev.domain.port.output.FileSystemWriter;
import dev.iadev.domain.port.output.ProgressReporter;
import dev.iadev.domain.port.output.StackProfileRepository;
import dev.iadev.domain.port.output.TemplateRenderer;
import dev.iadev.domain.service.GenerateEnvironmentService;
import dev.iadev.domain.service.ListStackProfilesService;
import dev.iadev.domain.service.ValidateConfigService;
import dev.iadev.infrastructure.adapter.output.checkpoint
        .FileCheckpointStore;
import dev.iadev.infrastructure.adapter.output.config
        .YamlStackProfileRepository;
import dev.iadev.infrastructure.adapter.output.filesystem
        .FileSystemWriterAdapter;
import dev.iadev.infrastructure.adapter.output.progress
        .ConsoleProgressReporter;
import dev.iadev.infrastructure.adapter.output.template
        .PebbleTemplateRenderer;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Composition Root — single point of manual dependency wiring.
 *
 * <p>This is the ONLY class allowed to instantiate concrete
 * adapter implementations (RULE-005). It creates all Output
 * Adapters, injects them into Domain Services, and exposes
 * the fully-wired Input Ports for use by CLI commands.</p>
 *
 * <p>Dependency graph:</p>
 * <ol>
 *   <li>Create Output Adapters (concrete implementations)</li>
 *   <li>Create Domain Services (injecting Output Ports)</li>
 *   <li>Expose Input Ports (Domain Services as interfaces)</li>
 * </ol>
 *
 * <p>Implements {@link CommandLine.IFactory} so Picocli can
 * use constructor injection for CLI commands.</p>
 *
 * @see GenerateEnvironmentUseCase
 * @see ValidateConfigUseCase
 * @see ListStackProfilesUseCase
 */
public final class ApplicationFactory
        implements CommandLine.IFactory {

    private static final String DEFAULT_CHECKPOINT_DIR =
            ".ia-dev-env/checkpoints";

    // Output Adapters
    private final StackProfileRepository profileRepository;
    private final TemplateRenderer templateRenderer;
    private final FileSystemWriter fileSystemWriter;
    private final CheckpointStore checkpointStore;
    private final ProgressReporter progressReporter;

    // Domain Services (exposed as Input Ports)
    private final GenerateEnvironmentUseCase generateUseCase;
    private final ValidateConfigUseCase validateUseCase;
    private final ListStackProfilesUseCase
            listProfilesUseCase;

    /**
     * Creates the application factory with the default
     * checkpoint directory ({@code .ia-dev-env/checkpoints}
     * relative to the current working directory).
     */
    public ApplicationFactory() {
        this(defaultCheckpointDir());
    }

    /**
     * Creates the application factory with a custom
     * checkpoint directory.
     *
     * @param checkpointDir directory for checkpoint files
     * @throws NullPointerException if checkpointDir is null
     */
    public ApplicationFactory(Path checkpointDir) {
        Objects.requireNonNull(checkpointDir,
                "checkpointDir must not be null");

        // 1. Create Output Adapters
        this.profileRepository =
                new YamlStackProfileRepository();
        this.templateRenderer =
                new PebbleTemplateRenderer();
        this.fileSystemWriter =
                new FileSystemWriterAdapter();
        this.checkpointStore =
                new FileCheckpointStore(checkpointDir);
        this.progressReporter =
                new ConsoleProgressReporter();

        // 2. Create Domain Services (injecting Output Ports)
        this.generateUseCase =
                new GenerateEnvironmentService(
                        profileRepository,
                        templateRenderer,
                        fileSystemWriter,
                        checkpointStore,
                        progressReporter);
        this.validateUseCase =
                new ValidateConfigService(
                        profileRepository);
        this.listProfilesUseCase =
                new ListStackProfilesService(
                        profileRepository);
    }

    /**
     * Returns the generation use case (Input Port).
     *
     * @return the fully-wired generation use case
     */
    public GenerateEnvironmentUseCase generateUseCase() {
        return generateUseCase;
    }

    /**
     * Returns the validation use case (Input Port).
     *
     * @return the fully-wired validation use case
     */
    public ValidateConfigUseCase validateUseCase() {
        return validateUseCase;
    }

    /**
     * Returns the list-profiles use case (Input Port).
     *
     * @return the fully-wired list-profiles use case
     */
    public ListStackProfilesUseCase listProfilesUseCase() {
        return listProfilesUseCase;
    }

    /**
     * Picocli IFactory implementation — creates command
     * instances using the default constructor. This allows
     * Picocli to instantiate commands that do not require
     * constructor injection.
     *
     * <p><strong>Note:</strong> This delegates to the default
     * factory, which only supports no-arg constructors. The
     * new hexagonal CLI commands (CliGenerateCommand,
     * CliValidateCommand, CliListStackProfilesCommand) require
     * manual instantiation via the factory's use case
     * accessors. This is a known limitation pending full CLI
     * migration.</p>
     *
     * @param cls the class to instantiate
     * @param <K> the type parameter
     * @return a new instance of the given class
     * @throws Exception if instantiation fails
     */
    @Override
    public <K> K create(Class<K> cls) throws Exception {
        return CommandLine.defaultFactory().create(cls);
    }

    private static Path defaultCheckpointDir() {
        return Path.of(System.getProperty("user.home"))
                .resolve(DEFAULT_CHECKPOINT_DIR);
    }
}
