from __future__ import annotations

import subprocess
import sys

from ia_dev_env import __version__


class TestEntryPoint:

    def test_module_help_returns_zero(self) -> None:
        result = subprocess.run(
            [sys.executable, "-m", "ia_dev_env", "--help"],
            capture_output=True,
            text=True,
            timeout=10,
        )
        assert result.returncode == 0
        assert "usage" in result.stdout.lower()

    def test_module_version_returns_version(self) -> None:
        result = subprocess.run(
            [sys.executable, "-m", "ia_dev_env", "--version"],
            capture_output=True,
            text=True,
            timeout=10,
        )
        assert result.returncode == 0
        assert __version__ in result.stdout
