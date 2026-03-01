from __future__ import annotations

from typing import List


class ConfigValidationError(Exception):
    """Raised when required config sections are missing."""

    def __init__(self, missing_fields: List[str]) -> None:
        self.missing_fields = missing_fields
        super().__init__(
            f"Missing required config sections: {', '.join(missing_fields)}"
        )
