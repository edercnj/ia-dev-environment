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
