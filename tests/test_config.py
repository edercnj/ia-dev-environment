from __future__ import annotations

import warnings
from pathlib import Path
from typing import Dict

import pytest
import yaml

from claude_setup.config import (
    detect_v2_format,
    load_config,
    migrate_v2_to_v3,
    validate_config,
)
from claude_setup.exceptions import ConfigValidationError
from claude_setup.models import ProjectConfig


class TestDetectV2Format:

    def test_detect_v2_format_with_type_returns_true(self) -> None:
        data = {"type": "api", "project": {"name": "x"}}
        assert detect_v2_format(data) is True

    def test_detect_v2_format_with_stack_returns_true(self) -> None:
        data = {"stack": "java-quarkus", "project": {"name": "x"}}
        assert detect_v2_format(data) is True

    def test_detect_v2_format_with_both_returns_true(self) -> None:
        data = {"type": "cli", "stack": "python-click-cli"}
        assert detect_v2_format(data) is True

    def test_detect_v2_format_v3_dict_returns_false(
        self,
        valid_v3_dict: Dict,
    ) -> None:
        assert detect_v2_format(valid_v3_dict) is False

    def test_detect_v2_format_empty_dict_returns_false(self) -> None:
        assert detect_v2_format({}) is False

    def test_detect_v2_format_unknown_type_returns_false(self) -> None:
        data = {"type": "unknown-type", "project": {"name": "x"}}
        assert detect_v2_format(data) is False


class TestValidateConfig:

    def test_validate_config_complete_raises_nothing(
        self,
        valid_v3_dict: Dict,
    ) -> None:
        validate_config(valid_v3_dict)

    def test_validate_config_missing_language_raises_error(self) -> None:
        data = {
            "project": {"name": "x"},
            "architecture": {"style": "library"},
            "interfaces": [{"type": "cli"}],
            "framework": {"name": "click", "version": "8.1"},
        }
        with pytest.raises(ConfigValidationError) as exc_info:
            validate_config(data)
        assert "language" in exc_info.value.missing_fields

    def test_validate_config_missing_multiple_lists_all(self) -> None:
        data = {"project": {"name": "x"}, "architecture": {"style": "lib"}}
        with pytest.raises(ConfigValidationError) as exc_info:
            validate_config(data)
        missing = exc_info.value.missing_fields
        assert "interfaces" in missing
        assert "language" in missing
        assert "framework" in missing

    def test_validate_config_none_input_raises_error(self) -> None:
        with pytest.raises(ConfigValidationError) as exc_info:
            validate_config(None)
        assert len(exc_info.value.missing_fields) == 5

    def test_validate_config_optional_sections_not_required(self) -> None:
        data = {
            "project": {"name": "x", "purpose": "y"},
            "architecture": {"style": "library"},
            "interfaces": [{"type": "cli"}],
            "language": {"name": "python", "version": "3.9"},
            "framework": {"name": "click", "version": "8.1"},
        }
        validate_config(data)


class TestMigrateV2ToV3:

    def test_migrate_preserves_project_section(
        self,
        valid_v2_dict: Dict,
    ) -> None:
        result = migrate_v2_to_v3(valid_v2_dict)
        assert result["project"]["name"] == "legacy"
        assert result["project"]["purpose"] == "Legacy"

    def test_migrate_does_not_mutate_input(
        self,
        valid_v2_dict: Dict,
    ) -> None:
        import copy
        original = copy.deepcopy(valid_v2_dict)
        migrate_v2_to_v3(valid_v2_dict)
        assert valid_v2_dict == original

    def test_migrate_removes_type_and_stack_keys(
        self,
        valid_v2_dict: Dict,
    ) -> None:
        result = migrate_v2_to_v3(valid_v2_dict)
        assert "type" not in result
        assert "stack" not in result

    def test_migrate_result_passes_validation(
        self,
        valid_v2_dict: Dict,
    ) -> None:
        result = migrate_v2_to_v3(valid_v2_dict)
        validate_config(result)

    def test_migrate_emits_warning(self, valid_v2_dict: Dict) -> None:
        with warnings.catch_warnings(record=True) as caught:
            warnings.simplefilter("always")
            migrate_v2_to_v3(valid_v2_dict)
        assert len(caught) == 1
        assert "v2 format" in str(caught[0].message).lower()

    def test_migrate_unknown_type_uses_default(self) -> None:
        data = {
            "type": "unknown",
            "stack": "java-quarkus",
            "project": {"name": "x", "purpose": "y"},
        }
        result = migrate_v2_to_v3(data)
        assert result["architecture"]["style"] == "microservice"
        assert result["interfaces"] == [{"type": "rest"}]

    def test_migrate_unknown_stack_raises_error(self) -> None:
        data = {
            "type": "api",
            "stack": "unknown-stack",
            "project": {"name": "x", "purpose": "y"},
        }
        with pytest.raises(ConfigValidationError):
            migrate_v2_to_v3(data)

    def test_migrate_no_project_section_uses_defaults(self) -> None:
        data = {"type": "api", "stack": "java-quarkus"}
        result = migrate_v2_to_v3(data)
        assert result["project"]["name"] == "unnamed"


class TestLoadConfig:

    def test_load_config_valid_v3_returns_project_config(
        self,
        valid_v3_path: Path,
    ) -> None:
        result = load_config(valid_v3_path)
        assert isinstance(result, ProjectConfig)

    def test_load_config_v3_project_name_correct(
        self,
        valid_v3_path: Path,
    ) -> None:
        result = load_config(valid_v3_path)
        assert result.project.name == "test-cli-tool"

    def test_load_config_v3_language_correct(
        self,
        valid_v3_path: Path,
    ) -> None:
        result = load_config(valid_v3_path)
        assert result.language.name == "python"
        assert result.language.version == "3.9"

    def test_load_config_v3_framework_correct(
        self,
        valid_v3_path: Path,
    ) -> None:
        result = load_config(valid_v3_path)
        assert result.framework.name == "click"
        assert result.framework.version == "8.1"

    def test_load_config_v2_auto_migrates(
        self,
        valid_v2_type_path: Path,
    ) -> None:
        result = load_config(valid_v2_type_path)
        assert isinstance(result, ProjectConfig)
        assert result.architecture.style == "microservice"

    def test_load_config_v2_emits_warning(
        self,
        valid_v2_type_path: Path,
    ) -> None:
        with warnings.catch_warnings(record=True) as caught:
            warnings.simplefilter("always")
            load_config(valid_v2_type_path)
        assert any("v2 format" in str(w.message).lower() for w in caught)

    def test_load_config_missing_section_raises_error(
        self,
        missing_language_path: Path,
    ) -> None:
        with pytest.raises(ConfigValidationError) as exc_info:
            load_config(missing_language_path)
        assert "language" in exc_info.value.missing_fields

    def test_load_config_empty_file_raises_error(
        self,
        tmp_path: Path,
    ) -> None:
        empty_file = tmp_path / "empty.yaml"
        empty_file.write_text("")
        with pytest.raises(ConfigValidationError):
            load_config(empty_file)

    def test_load_config_malformed_yaml_raises_error(
        self,
        tmp_path: Path,
    ) -> None:
        bad_file = tmp_path / "bad.yaml"
        bad_file.write_text('project:\n  name: "bad\n  purpose: [unclosed')
        with pytest.raises(yaml.YAMLError):
            load_config(bad_file)

    def test_load_config_nonexistent_file_raises_error(self) -> None:
        with pytest.raises(FileNotFoundError):
            load_config(Path("/nonexistent/config.yaml"))

    def test_load_config_minimal_v3_defaults(
        self,
        minimal_v3_path: Path,
    ) -> None:
        result = load_config(minimal_v3_path)
        assert result.data.database.name == "none"
        assert result.testing.smoke_tests is True

    def test_load_config_v2_stack_migration(
        self,
        valid_v2_stack_path: Path,
    ) -> None:
        result = load_config(valid_v2_stack_path)
        assert result.language.name == "python"
        assert result.framework.name == "click"


class TestConfigValidationError:

    def test_single_field_carries_name(self) -> None:
        err = ConfigValidationError(["language"])
        assert err.missing_fields == ["language"]

    def test_multiple_fields_carries_all(self) -> None:
        err = ConfigValidationError(["language", "framework"])
        assert err.missing_fields == ["language", "framework"]

    def test_message_lists_fields(self) -> None:
        err = ConfigValidationError(["language"])
        assert "language" in str(err)

    def test_inherits_from_exception(self) -> None:
        err = ConfigValidationError(["language"])
        assert isinstance(err, Exception)

    def test_empty_fields_list_allowed(self) -> None:
        err = ConfigValidationError([])
        assert err.missing_fields == []
