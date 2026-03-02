from __future__ import annotations

from typing import List


class ConfigValidationError(Exception):
    """Raised when required config sections are missing."""

    def __init__(self, missing_fields: List[str]) -> None:
        self.missing_fields = missing_fields
        super().__init__(
            f"Missing required config sections: {', '.join(missing_fields)}"
        )


class PipelineError(Exception):
    """Raised when the assembly pipeline fails fatally."""

    def __init__(self, assembler_name: str, reason: str) -> None:
        self.assembler_name = assembler_name
        self.reason = reason
        super().__init__(
            f"Pipeline failed at '{assembler_name}': {reason}"
        )
