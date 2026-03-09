from __future__ import annotations

from ia_dev_env.domain.stack_mapping import (
    DOCKER_BASE_IMAGES,
    FRAMEWORK_HEALTH_PATHS,
    FRAMEWORK_LANGUAGE_RULES,
    FRAMEWORK_PORTS,
    INTERFACE_PROTOCOL_MAP,
    LANGUAGE_COMMANDS,
    VALID_ARCHITECTURE_STYLES,
    VALID_INTERFACE_TYPES,
)

EXPECTED_LANGUAGE_STACKS = [
    ("java", "maven"),
    ("java", "gradle"),
    ("kotlin", "gradle"),
    ("typescript", "npm"),
    ("python", "pip"),
    ("go", "go"),
    ("rust", "cargo"),
    ("csharp", "dotnet"),
]

EXPECTED_COMMAND_KEYS = (
    "compile_cmd",
    "build_cmd",
    "test_cmd",
    "coverage_cmd",
    "file_extension",
    "build_file",
    "package_manager",
)

EXPECTED_FRAMEWORK_COUNT = 11
EXPECTED_LANGUAGE_RULE_COUNT = 15
EXPECTED_INTERFACE_TYPE_COUNT = 9
EXPECTED_ARCHITECTURE_STYLE_COUNT = 5

NON_PROTOCOL_TYPES = {"cli", "scheduled"}


class TestStackMapping:

    def test_language_commands_covers_all_stacks(self) -> None:
        for stack_key in EXPECTED_LANGUAGE_STACKS:
            assert stack_key in LANGUAGE_COMMANDS, (
                f"Missing stack: {stack_key}"
            )

    def test_language_commands_has_all_keys(self) -> None:
        for stack_key, commands in LANGUAGE_COMMANDS.items():
            for cmd_key in EXPECTED_COMMAND_KEYS:
                assert cmd_key in commands, (
                    f"Missing '{cmd_key}' in {stack_key}"
                )

    def test_framework_ports_covers_all_frameworks(
        self,
    ) -> None:
        assert len(FRAMEWORK_PORTS) == EXPECTED_FRAMEWORK_COUNT

    def test_framework_health_paths_covers_all(self) -> None:
        for fw in FRAMEWORK_PORTS:
            assert fw in FRAMEWORK_HEALTH_PATHS, (
                f"Missing health path for {fw}"
            )

    def test_framework_language_rules_covers_all(self) -> None:
        assert (
            len(FRAMEWORK_LANGUAGE_RULES)
            == EXPECTED_LANGUAGE_RULE_COUNT
        )

    def test_interface_protocol_map_covers_non_cli(
        self,
    ) -> None:
        for itype in VALID_INTERFACE_TYPES:
            if itype not in NON_PROTOCOL_TYPES:
                assert itype in INTERFACE_PROTOCOL_MAP, (
                    f"Missing protocol for {itype}"
                )

    def test_valid_interface_types_is_nonempty_tuple(
        self,
    ) -> None:
        assert isinstance(VALID_INTERFACE_TYPES, tuple)
        assert len(VALID_INTERFACE_TYPES) == (
            EXPECTED_INTERFACE_TYPE_COUNT
        )

    def test_valid_architecture_styles_is_nonempty_tuple(
        self,
    ) -> None:
        assert isinstance(VALID_ARCHITECTURE_STYLES, tuple)
        assert len(VALID_ARCHITECTURE_STYLES) == (
            EXPECTED_ARCHITECTURE_STYLE_COUNT
        )

    def test_docker_base_images_covers_all_languages(
        self,
    ) -> None:
        expected_langs = {
            key[0] for key in LANGUAGE_COMMANDS
        }
        for lang in expected_langs:
            assert lang in DOCKER_BASE_IMAGES, (
                f"Missing docker image for {lang}"
            )

    def test_language_commands_values_are_nonempty(
        self,
    ) -> None:
        for stack_key, commands in LANGUAGE_COMMANDS.items():
            for cmd_key, value in commands.items():
                assert value != "", (
                    f"Empty value for {cmd_key} in {stack_key}"
                )
