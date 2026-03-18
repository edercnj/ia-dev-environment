export class CliError extends Error {
  readonly code: string;

  constructor(message: string, code: string) {
    super(message);
    this.name = "CliError";
    this.code = code;
  }
}

export class ConfigValidationError extends Error {
  readonly missingFields: readonly string[];

  constructor(missingFields: readonly string[]) {
    super(`Missing required config sections: ${missingFields.join(", ")}`);
    this.name = "ConfigValidationError";
    this.missingFields = [...missingFields];
  }
}

export class ConfigParseError extends Error {
  readonly detail: string;

  constructor(detail: string) {
    super(`Failed to parse config file: ${detail}`);
    this.name = "ConfigParseError";
    this.detail = detail;
  }
}

export class PipelineError extends Error {
  readonly assemblerName: string;
  readonly reason: string;

  constructor(assemblerName: string, reason: string) {
    super(`Pipeline failed at '${assemblerName}': ${reason}`);
    this.name = "PipelineError";
    this.assemblerName = assemblerName;
    this.reason = reason;
  }
}

export class CheckpointValidationError extends Error {
  readonly field: string;
  readonly detail: string;

  constructor(field: string, detail: string) {
    super(
      `Checkpoint validation failed: ${field} -- ${detail}`,
    );
    this.name = "CheckpointValidationError";
    this.field = field;
    this.detail = detail;
  }
}

export class CheckpointIOError extends Error {
  readonly path: string;
  readonly operation: string;

  constructor(path: string, operation: string) {
    super(
      `Checkpoint I/O failed during '${operation}': ${path}`,
    );
    this.name = "CheckpointIOError";
    this.path = path;
    this.operation = operation;
  }
}

export class PartialExecutionError extends Error {
  readonly code: string;
  readonly context: Readonly<Record<string, unknown>>;

  constructor(
    message: string,
    code: string,
    context: Record<string, unknown>,
  ) {
    super(message);
    this.name = "PartialExecutionError";
    this.code = code;
    this.context = context;
  }
}
