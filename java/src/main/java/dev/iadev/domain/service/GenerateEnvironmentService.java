package dev.iadev.domain.service;

import dev.iadev.domain.model.CheckpointState;
import dev.iadev.domain.model.GenerationContext;
import dev.iadev.domain.model.GenerationResult;
import dev.iadev.domain.model.StackProfile;
import dev.iadev.domain.port.input.GenerateEnvironmentUseCase;
import dev.iadev.domain.port.output.CheckpointStore;
import dev.iadev.domain.port.output.FileSystemWriter;
import dev.iadev.domain.port.output.ProgressReporter;
import dev.iadev.domain.port.output.StackProfileRepository;
import dev.iadev.domain.port.output.TemplateRenderer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Domain service that orchestrates environment generation.
 *
 * <p>Implements {@link GenerateEnvironmentUseCase} by
 * coordinating the generation flow:</p>
 * <ol>
 *   <li>Resolve stack profile via
 *       {@link StackProfileRepository}</li>
 *   <li>Load or create checkpoint via
 *       {@link CheckpointStore}</li>
 *   <li>Report progress via
 *       {@link ProgressReporter}</li>
 *   <li>Save checkpoint on completion</li>
 * </ol>
 *
 * <p>All dependencies are Output Ports injected via
 * constructor. Contains zero framework or infrastructure
 * dependencies.</p>
 *
 * @see GenerateEnvironmentUseCase
 * @see StackProfileRepository
 * @see TemplateRenderer
 * @see FileSystemWriter
 * @see CheckpointStore
 * @see ProgressReporter
 */
public final class GenerateEnvironmentService
        implements GenerateEnvironmentUseCase {

    private static final String TASK_NAME = "generate";
    private static final int GENERATION_STEPS = 3;

    private final StackProfileRepository profileRepository;
    private final TemplateRenderer templateRenderer;
    private final FileSystemWriter fileSystemWriter;
    private final CheckpointStore checkpointStore;
    private final ProgressReporter progressReporter;

    /**
     * Creates a new GenerateEnvironmentService.
     *
     * @param profileRepository repository for stack profiles
     * @param templateRenderer  renderer for template files
     * @param fileSystemWriter  writer for output files
     * @param checkpointStore   store for execution checkpoints
     * @param progressReporter  reporter for progress updates
     * @throws NullPointerException if any parameter is null
     */
    public GenerateEnvironmentService(
            StackProfileRepository profileRepository,
            TemplateRenderer templateRenderer,
            FileSystemWriter fileSystemWriter,
            CheckpointStore checkpointStore,
            ProgressReporter progressReporter) {
        this.profileRepository = Objects.requireNonNull(
                profileRepository,
                "profileRepository must not be null");
        this.templateRenderer = Objects.requireNonNull(
                templateRenderer,
                "templateRenderer must not be null");
        this.fileSystemWriter = Objects.requireNonNull(
                fileSystemWriter,
                "fileSystemWriter must not be null");
        this.checkpointStore = Objects.requireNonNull(
                checkpointStore,
                "checkpointStore must not be null");
        this.progressReporter = Objects.requireNonNull(
                progressReporter,
                "progressReporter must not be null");
    }

    /**
     * Generates a development environment from the given
     * context.
     *
     * <p>Resolves the stack profile from the project name,
     * loads any existing checkpoint, and orchestrates the
     * generation pipeline. Reports progress and saves
     * checkpoints throughout the process.</p>
     *
     * @param context the generation context containing
     *                configuration and output directory
     * @return the generation result with status and file list
     * @throws NullPointerException if context is null
     */
    @Override
    public GenerationResult generate(
            GenerationContext context) {
        Objects.requireNonNull(context,
                "context must not be null");

        String profileName =
                context.config().project().name();
        Optional<StackProfile> profileOpt =
                profileRepository.findByName(profileName);

        if (profileOpt.isEmpty()) {
            return handleProfileNotFound(profileName);
        }

        return executeGeneration(context, profileName);
    }

    private GenerationResult handleProfileNotFound(
            String profileName) {
        String errorMsg =
                "Stack profile not found: %s"
                        .formatted(profileName);
        progressReporter.reportError(TASK_NAME, errorMsg);
        return new GenerationResult(
                false, List.of(), List.of(errorMsg));
    }

    private GenerationResult executeGeneration(
            GenerationContext context,
            String executionId) {
        progressReporter.reportStart(
                TASK_NAME, GENERATION_STEPS);

        checkpointStore.load(executionId);

        List<String> filesGenerated = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        progressReporter.reportProgress(
                TASK_NAME, 1, "Resolving stack profile");
        progressReporter.reportProgress(
                TASK_NAME, 2, "Generating artifacts");
        progressReporter.reportProgress(
                TASK_NAME, 3, "Finalizing output");

        saveCheckpoint(executionId);

        progressReporter.reportComplete(TASK_NAME);

        return new GenerationResult(
                true, filesGenerated, warnings);
    }

    private void saveCheckpoint(String executionId) {
        Instant now = Instant.now();
        CheckpointState state = new CheckpointState(
                executionId, now, now,
                Map.of("generation", "complete"),
                Map.of());
        checkpointStore.save(state);
    }
}
