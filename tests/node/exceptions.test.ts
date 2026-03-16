import { describe, it, expect } from "vitest";
import {
  CheckpointIOError,
  CheckpointValidationError,
  ConfigValidationError,
  PipelineError,
} from "../../src/exceptions.js";

describe("ConfigValidationError", () => {
  it("withSingleField_formatsMessageCorrectly", () => {
    const error = new ConfigValidationError(["database"]);
    expect(error.message).toBe(
      "Missing required config sections: database",
    );
  });

  it("withMultipleFields_joinsWithComma", () => {
    const error = new ConfigValidationError([
      "database",
      "server",
      "auth",
    ]);
    expect(error.message).toBe(
      "Missing required config sections: database, server, auth",
    );
  });

  it("constructor_setsNameProperty", () => {
    const error = new ConfigValidationError(["a"]);
    expect(error.name).toBe("ConfigValidationError");
  });

  it("constructor_storesMissingFieldsAsReadonly", () => {
    const fields = ["a", "b"];
    const error = new ConfigValidationError(fields);
    expect(error.missingFields).toEqual(["a", "b"]);
  });

  it("constructor_defensivelyCopiesInput", () => {
    const fields = ["a", "b"];
    const error = new ConfigValidationError(fields);
    fields.push("c");
    expect(error.missingFields).toEqual(["a", "b"]);
  });

  it("withEmptyArray_formatsEmptyMessage", () => {
    const error = new ConfigValidationError([]);
    expect(error.message).toBe("Missing required config sections: ");
  });

  it("instanceof_isError", () => {
    const error = new ConfigValidationError(["x"]);
    expect(error).toBeInstanceOf(Error);
  });
});

describe("PipelineError", () => {
  it("withAssemblerAndReason_formatsMessageCorrectly", () => {
    const error = new PipelineError(
      "RulesAssembler",
      "template not found",
    );
    expect(error.message).toBe(
      "Pipeline failed at 'RulesAssembler': template not found",
    );
  });

  it("constructor_setsNameProperty", () => {
    const error = new PipelineError("x", "y");
    expect(error.name).toBe("PipelineError");
  });

  it("constructor_storesAssemblerNameAsReadonly", () => {
    const error = new PipelineError("SkillsAssembler", "reason");
    expect(error.assemblerName).toBe("SkillsAssembler");
  });

  it("constructor_storesReasonAsReadonly", () => {
    const error = new PipelineError("x", "disk full");
    expect(error.reason).toBe("disk full");
  });

  it("instanceof_isError", () => {
    const error = new PipelineError("x", "y");
    expect(error).toBeInstanceOf(Error);
  });

  it("withEmptyStrings_formatsWithEmptyValues", () => {
    const error = new PipelineError("", "");
    expect(error.message).toBe("Pipeline failed at '': ");
  });
});

describe("CheckpointValidationError", () => {
  it("withFieldAndDetail_formatsMessageCorrectly", () => {
    const error = new CheckpointValidationError(
      "epicId",
      "is required",
    );
    expect(error.message).toBe(
      "Checkpoint validation failed: epicId -- is required",
    );
  });

  it("constructor_setsNameProperty", () => {
    const error = new CheckpointValidationError("f", "d");
    expect(error.name).toBe("CheckpointValidationError");
  });

  it("constructor_storesFieldAsReadonly", () => {
    const error = new CheckpointValidationError(
      "branch",
      "missing",
    );
    expect(error.field).toBe("branch");
  });

  it("constructor_storesDetailAsReadonly", () => {
    const error = new CheckpointValidationError(
      "branch",
      "must be a string",
    );
    expect(error.detail).toBe("must be a string");
  });

  it("instanceof_isError", () => {
    const error = new CheckpointValidationError("x", "y");
    expect(error).toBeInstanceOf(Error);
  });
});

describe("CheckpointIOError", () => {
  it("withPathAndOperation_formatsMessageCorrectly", () => {
    const error = new CheckpointIOError(
      "/tmp/epic-0042",
      "read",
    );
    expect(error.message).toBe(
      "Checkpoint I/O failed during 'read': /tmp/epic-0042",
    );
  });

  it("constructor_setsNameProperty", () => {
    const error = new CheckpointIOError("/tmp", "write");
    expect(error.name).toBe("CheckpointIOError");
  });

  it("constructor_storesPathAsReadonly", () => {
    const error = new CheckpointIOError(
      "/tmp/epic-0042",
      "stat",
    );
    expect(error.path).toBe("/tmp/epic-0042");
  });

  it("constructor_storesOperationAsReadonly", () => {
    const error = new CheckpointIOError("/tmp", "rename");
    expect(error.operation).toBe("rename");
  });

  it("instanceof_isError", () => {
    const error = new CheckpointIOError("/tmp", "read");
    expect(error).toBeInstanceOf(Error);
  });
});
