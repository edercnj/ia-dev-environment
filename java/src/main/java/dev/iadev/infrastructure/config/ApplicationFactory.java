package dev.iadev.infrastructure.config;

import dev.iadev.domain.port.input.GenerateEnvironmentUseCase;
import dev.iadev.domain.port.input.ValidateConfigUseCase;
import dev.iadev.domain.port.output.FileSystemWriter;
import dev.iadev.domain.port.output.ProgressReporter;
import dev.iadev.domain.port.output.StackProfileRepository;
import dev.iadev.domain.port.output.TemplateRenderer;
import dev.iadev.domain.service.GenerateEnvironmentService;
import dev.iadev.domain.service.ValidateConfigService;
import dev.iadev.infrastructure.adapter.output.config
        .YamlStackProfileRepository;
import dev.iadev.infrastructure.adapter.output.filesystem
        .FileSystemWriterAdapter;
import dev.iadev.infrastructure.adapter.output.progress
        .ConsoleProgressReporter;
import dev.iadev.infrastructure.adapter.output.template
        .PebbleTemplateRenderer;
import picocli.CommandLine;

/**
 * Composition Root — single point of manual dependency
 * wiring.
 *
 * <p>Creates all Output Adapters, injects them into Domain
 * Services, and exposes the fully-wired Input Ports for
 * use by CLI commands.</p>
 *
 * @see GenerateEnvironmentUseCase
 * @see ValidateConfigUseCase
 */
public final class ApplicationFactory
        implements CommandLine.IFactory {

    private final StackProfileRepository profileRepository;
    private final TemplateRenderer templateRenderer;
    private final FileSystemWriter fileSystemWriter;
    private final ProgressReporter progressReporter;

    private final GenerateEnvironmentUseCase generateUseCase;
    private final ValidateConfigUseCase validateUseCase;

    public ApplicationFactory() {
        this.profileRepository =
                new YamlStackProfileRepository();
        this.templateRenderer =
                new PebbleTemplateRenderer();
        this.fileSystemWriter =
                new FileSystemWriterAdapter();
        this.progressReporter =
                new ConsoleProgressReporter();

        this.generateUseCase =
                new GenerateEnvironmentService(
                        profileRepository,
                        templateRenderer,
                        fileSystemWriter,
                        progressReporter);
        this.validateUseCase =
                new ValidateConfigService(
                        profileRepository);
    }

    public GenerateEnvironmentUseCase generateUseCase() {
        return generateUseCase;
    }

    public ValidateConfigUseCase validateUseCase() {
        return validateUseCase;
    }

    @Override
    public <K> K create(Class<K> cls) throws Exception {
        return CommandLine.defaultFactory().create(cls);
    }
}
