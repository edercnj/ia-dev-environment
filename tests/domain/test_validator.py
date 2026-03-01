from __future__ import annotations

import os

import pytest

from claude_setup.domain.validator import (
    validate_stack,
    verify_cross_references,
)


class TestValidatorValidCombos:

    @pytest.mark.parametrize(
        "lang, fw",
        [
            ("java", "quarkus"),
            ("kotlin", "quarkus"),
            ("java", "spring-boot"),
            ("kotlin", "spring-boot"),
            ("typescript", "nestjs"),
            ("typescript", "express"),
            ("typescript", "fastify"),
            ("python", "fastapi"),
            ("python", "django"),
            ("python", "flask"),
            ("go", "gin"),
            ("kotlin", "ktor"),
            ("rust", "axum"),
        ],
        ids=[
            "java-quarkus", "kotlin-quarkus",
            "java-spring-boot", "kotlin-spring-boot",
            "ts-nestjs", "ts-express", "ts-fastify",
            "python-fastapi", "python-django", "python-flask",
            "go-gin", "kotlin-ktor", "rust-axum",
        ],
    )
    def test_validate_stack_valid_combos_no_errors(
        self,
        create_project_config,
        lang,
        fw,
    ) -> None:
        config = create_project_config(
            language={"name": lang, "version": "17"},
            framework={
                "name": fw,
                "version": "3.0",
                "build_tool": "maven",
                "native_build": False,
            },
            architecture={"style": "microservice"},
            interfaces=[{"type": "rest"}],
        )
        errors = validate_stack(config)
        assert errors == []


class TestValidatorInvalidCombos:

    @pytest.mark.parametrize(
        "lang, fw",
        [
            ("java", "fastapi"),
            ("python", "quarkus"),
            ("go", "spring-boot"),
            ("typescript", "gin"),
            ("rust", "django"),
            ("csharp", "quarkus"),
            ("java", "nestjs"),
            ("python", "express"),
            ("go", "ktor"),
            ("rust", "flask"),
        ],
        ids=[
            "java-fastapi", "python-quarkus",
            "go-spring-boot", "ts-gin",
            "rust-django", "csharp-quarkus",
            "java-nestjs", "python-express",
            "go-ktor", "rust-flask",
        ],
    )
    def test_validate_stack_invalid_lang_fw_returns_error(
        self,
        create_project_config,
        lang,
        fw,
    ) -> None:
        config = create_project_config(
            language={"name": lang, "version": "17"},
            framework={
                "name": fw,
                "version": "3.0",
                "build_tool": "maven",
                "native_build": False,
            },
        )
        errors = validate_stack(config)
        assert len(errors) >= 1
        assert "requires language" in errors[0]


class TestValidatorVersionConstraints:

    @pytest.mark.parametrize(
        "lang, lang_ver, fw, fw_ver, err_fragment",
        [
            (
                "java", "11", "quarkus", "3.0",
                "requires Java 17+",
            ),
            (
                "java", "11", "spring-boot", "3.2",
                "requires Java 17+",
            ),
            (
                "python", "3.8", "django", "5.0",
                "requires Python 3.10+",
            ),
        ],
        ids=[
            "quarkus3-java11",
            "spring-boot3-java11",
            "django5-python38",
        ],
    )
    def test_validate_stack_version_constraints_error(
        self,
        create_project_config,
        lang,
        lang_ver,
        fw,
        fw_ver,
        err_fragment,
    ) -> None:
        config = create_project_config(
            language={"name": lang, "version": lang_ver},
            framework={
                "name": fw,
                "version": fw_ver,
                "build_tool": "maven",
                "native_build": False,
            },
        )
        errors = validate_stack(config)
        matching = [e for e in errors if err_fragment in e]
        assert len(matching) >= 1

    def test_validate_stack_quarkus3_java17_no_error(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            language={"name": "java", "version": "17"},
            framework={
                "name": "quarkus",
                "version": "3.0",
                "build_tool": "maven",
                "native_build": False,
            },
        )
        errors = validate_stack(config)
        assert errors == []

    def test_validate_stack_django5_python310_no_error(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            language={"name": "python", "version": "3.10"},
            framework={
                "name": "django",
                "version": "5.0",
                "build_tool": "pip",
                "native_build": False,
            },
        )
        errors = validate_stack(config)
        assert errors == []


class TestValidatorNativeBuild:

    def test_validate_stack_native_unsupported_error(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            framework={
                "name": "gin",
                "version": "1.9",
                "build_tool": "go",
                "native_build": True,
            },
            language={"name": "go", "version": "1.21"},
        )
        errors = validate_stack(config)
        matching = [
            e for e in errors
            if "Native build is not supported" in e
        ]
        assert len(matching) == 1

    def test_validate_stack_native_supported_no_error(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            language={"name": "java", "version": "17"},
            framework={
                "name": "quarkus",
                "version": "3.0",
                "build_tool": "maven",
                "native_build": True,
            },
        )
        errors = validate_stack(config)
        assert errors == []


class TestValidatorInterfaceTypes:

    def test_validate_stack_invalid_interface_error(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            interfaces=[{"type": "soap"}],
        )
        errors = validate_stack(config)
        matching = [
            e for e in errors
            if "Invalid interface type" in e
        ]
        assert len(matching) == 1
        assert "'soap'" in matching[0]

    def test_validate_stack_valid_interface_no_error(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            interfaces=[{"type": "rest"}, {"type": "grpc"}],
        )
        errors = validate_stack(config)
        assert errors == []


class TestValidatorArchitectureStyle:

    def test_validate_stack_invalid_style_error(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            architecture={"style": "hexagonal"},
        )
        errors = validate_stack(config)
        matching = [
            e for e in errors
            if "Invalid architecture style" in e
        ]
        assert len(matching) == 1
        assert "'hexagonal'" in matching[0]

    def test_validate_stack_valid_style_no_error(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            architecture={"style": "microservice"},
        )
        errors = validate_stack(config)
        assert errors == []


class TestValidatorMultipleErrors:

    def test_validate_stack_multiple_errors_returns_all(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            language={"name": "python", "version": "3.8"},
            framework={
                "name": "quarkus",
                "version": "3.0",
                "build_tool": "maven",
                "native_build": True,
            },
            architecture={"style": "invalid-arch"},
            interfaces=[{"type": "soap"}],
        )
        errors = validate_stack(config)
        assert len(errors) >= 3


class TestValidatorVersionParsing:

    def test_validate_stack_empty_version_no_error(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            language={"name": "java", "version": ""},
            framework={
                "name": "quarkus",
                "version": "",
                "build_tool": "maven",
                "native_build": False,
            },
        )
        errors = validate_stack(config)
        assert errors == []

    def test_validate_stack_non_numeric_version_no_crash(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            language={"name": "java", "version": "latest"},
            framework={
                "name": "quarkus",
                "version": "latest",
                "build_tool": "maven",
                "native_build": False,
            },
        )
        errors = validate_stack(config)
        assert isinstance(errors, list)

    def test_validate_stack_alpha_major_version_no_crash(
        self,
        create_project_config,
    ) -> None:
        """Exercises ValueError branch in _parse_major_version."""
        config = create_project_config(
            language={"name": "java", "version": "abc.def"},
            framework={
                "name": "quarkus",
                "version": "xyz",
                "build_tool": "maven",
                "native_build": False,
            },
        )
        errors = validate_stack(config)
        assert isinstance(errors, list)

    def test_validate_stack_alpha_minor_version_no_crash(
        self,
        create_project_config,
    ) -> None:
        """Exercises ValueError branch in _parse_minor_version."""
        config = create_project_config(
            language={"name": "python", "version": "3.abc"},
            framework={
                "name": "django",
                "version": "5.0",
                "build_tool": "pip",
                "native_build": False,
            },
        )
        errors = validate_stack(config)
        assert isinstance(errors, list)


class TestVerifyCrossReferences:

    def test_verify_cross_references_all_exist(
        self,
        create_project_config,
        tmp_path,
    ) -> None:
        config = create_project_config()
        skills_dir = tmp_path / "skills"
        skills_dir.mkdir()
        rules_dir = tmp_path / ".claude" / "rules"
        rules_dir.mkdir(parents=True)
        errors = verify_cross_references(
            config, str(tmp_path),
        )
        assert errors == []

    def test_verify_cross_references_missing_dirs(
        self,
        create_project_config,
        tmp_path,
    ) -> None:
        config = create_project_config()
        errors = verify_cross_references(
            config, str(tmp_path),
        )
        assert len(errors) == 2
        assert any("skills" in e for e in errors)
        assert any(".claude/rules" in e for e in errors)

    def test_verify_cross_references_nonexistent_src(
        self,
        create_project_config,
        tmp_path,
    ) -> None:
        config = create_project_config()
        fake_dir = str(tmp_path / "nonexistent")
        errors = verify_cross_references(config, fake_dir)
        assert len(errors) == 1
        assert "does not exist" in errors[0]

    def test_verify_cross_references_partial(
        self,
        create_project_config,
        tmp_path,
    ) -> None:
        config = create_project_config()
        skills_dir = tmp_path / "skills"
        skills_dir.mkdir()
        errors = verify_cross_references(
            config, str(tmp_path),
        )
        assert len(errors) == 1
        assert ".claude/rules" in errors[0]
