from __future__ import annotations

import logging
from pathlib import Path

import pytest

from ia_dev_env.utils import (
    _reject_dangerous_path,
    _validate_dest_path,
    atomic_output,
    find_resources_dir,
    setup_logging,
)


class TestAtomicOutput:

    def test_success_copies_files_to_dest(self, tmp_path: Path) -> None:
        dest = tmp_path / "output"
        with atomic_output(dest) as temp:
            (temp / "file.txt").write_text("hello")
        assert (dest / "file.txt").read_text() == "hello"

    def test_success_preserves_directory_structure(self, tmp_path: Path) -> None:
        dest = tmp_path / "output"
        with atomic_output(dest) as temp:
            nested = temp / "sub" / "deep"
            nested.mkdir(parents=True)
            (nested / "data.txt").write_text("nested")
        assert (dest / "sub" / "deep" / "data.txt").read_text() == "nested"

    def test_success_removes_temp_dir(self, tmp_path: Path) -> None:
        dest = tmp_path / "output"
        captured_temp = None
        with atomic_output(dest) as temp:
            captured_temp = temp
            (temp / "file.txt").write_text("data")
        assert not captured_temp.exists()

    def test_failure_cleans_temp_dir(self, tmp_path: Path) -> None:
        dest = tmp_path / "output"
        captured_temp = None
        with pytest.raises(RuntimeError):
            with atomic_output(dest) as temp:
                captured_temp = temp
                (temp / "file.txt").write_text("data")
                raise RuntimeError("boom")
        assert not captured_temp.exists()

    def test_failure_does_not_write_dest(self, tmp_path: Path) -> None:
        dest = tmp_path / "output"
        with pytest.raises(RuntimeError):
            with atomic_output(dest) as temp:
                (temp / "file.txt").write_text("data")
                raise RuntimeError("boom")
        assert not dest.exists()

    def test_failure_reraises_exception(self, tmp_path: Path) -> None:
        dest = tmp_path / "output"
        with pytest.raises(ValueError, match="test error"):
            with atomic_output(dest) as temp:
                raise ValueError("test error")

    def test_replaces_existing_dest_dir(self, tmp_path: Path) -> None:
        dest = tmp_path / "output"
        dest.mkdir()
        (dest / "old.txt").write_text("old")
        with atomic_output(dest) as temp:
            (temp / "new.txt").write_text("new")
        assert (dest / "new.txt").exists()
        assert not (dest / "old.txt").exists()

    def test_yields_path_object(self, tmp_path: Path) -> None:
        dest = tmp_path / "output"
        with atomic_output(dest) as temp:
            assert isinstance(temp, Path)
            assert temp.is_dir()

    def test_temp_dir_is_writable(self, tmp_path: Path) -> None:
        dest = tmp_path / "output"
        with atomic_output(dest) as temp:
            path = temp / "writable.txt"
            path.write_text("test")
            assert path.read_text() == "test"

    def test_keyboard_interrupt_cleans_up(self, tmp_path: Path) -> None:
        dest = tmp_path / "output"
        captured_temp = None
        with pytest.raises(KeyboardInterrupt):
            with atomic_output(dest) as temp:
                captured_temp = temp
                raise KeyboardInterrupt()
        assert not captured_temp.exists()

    def test_symlink_dest_raises_value_error(self, tmp_path: Path) -> None:
        real_dir = tmp_path / "real"
        real_dir.mkdir()
        link = tmp_path / "link"
        link.symlink_to(real_dir)
        with pytest.raises(ValueError, match="symlink"):
            with atomic_output(link) as temp:
                pass

    def test_resolves_relative_paths(self, tmp_path: Path) -> None:
        dest = tmp_path / "a" / ".." / "output"
        with atomic_output(dest) as temp:
            (temp / "file.txt").write_text("resolved")
        resolved = (tmp_path / "output")
        assert (resolved / "file.txt").read_text() == "resolved"


class TestValidateDestPath:

    def test_resolves_path(self, tmp_path: Path) -> None:
        result = _validate_dest_path(tmp_path / "sub")
        assert result.is_absolute()

    def test_rejects_symlink(self, tmp_path: Path) -> None:
        real = tmp_path / "real"
        real.mkdir()
        link = tmp_path / "link"
        link.symlink_to(real)
        with pytest.raises(ValueError, match="symlink"):
            _validate_dest_path(link)


class TestRejectDangerousPath:

    def test_rejects_cwd(self) -> None:
        cwd = Path.cwd().resolve()
        with pytest.raises(ValueError, match="current directory"):
            _reject_dangerous_path(cwd)

    def test_rejects_home(self) -> None:
        home = Path.home().resolve()
        with pytest.raises(ValueError, match="home directory"):
            _reject_dangerous_path(home)

    def test_rejects_root(self) -> None:
        with pytest.raises(ValueError, match="protected system path"):
            _reject_dangerous_path(Path("/"))

    def test_rejects_system_paths(self) -> None:
        for path_str in ("/tmp", "/var", "/etc", "/usr"):
            with pytest.raises(ValueError, match="protected system path"):
                _reject_dangerous_path(Path(path_str))

    def test_accepts_subdirectory_of_cwd(self, tmp_path: Path) -> None:
        safe = tmp_path / "output"
        _reject_dangerous_path(safe)

    def test_cwd_dest_blocked_in_atomic_output(self) -> None:
        with pytest.raises(ValueError, match="current directory"):
            with atomic_output(Path.cwd()):
                pass


class TestSetupLogging:

    def test_verbose_true_sets_debug_level(self) -> None:
        setup_logging(verbose=True)
        root = logging.getLogger()
        assert root.level == logging.DEBUG

    def test_verbose_false_sets_info_level(self) -> None:
        setup_logging(verbose=False)
        root = logging.getLogger()
        assert root.level == logging.INFO


class TestFindResourcesDir:

    def test_returns_existing_resources_dir(self) -> None:
        result = find_resources_dir()
        assert result.exists()
        assert result.is_dir()

    def test_returns_path_ending_in_resources(self) -> None:
        result = find_resources_dir()
        assert result.name == "resources"

    def test_returns_absolute_path(self) -> None:
        result = find_resources_dir()
        assert result.is_absolute()

    def test_raises_when_resources_missing(self, monkeypatch, tmp_path: Path) -> None:
        fake_file = tmp_path / "src" / "pkg" / "utils.py"
        fake_file.parent.mkdir(parents=True)
        fake_file.write_text("")
        monkeypatch.setattr(
            "ia_dev_env.utils.__file__",
            str(fake_file),
        )
        # After monkeypatch, find_resources_dir will look for
        # tmp_path/resources which doesn't exist
        with pytest.raises(FileNotFoundError, match="Resources directory not found"):
            find_resources_dir()
