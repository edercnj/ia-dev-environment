from __future__ import annotations

import pytest

from ia_dev_env.domain.resolver import resolve_stack


class TestResolverLanguageCommands:

    @pytest.mark.parametrize(
        "lang, build_tool, fw, exp_build, exp_test, "
        "exp_compile, exp_coverage, exp_ext, exp_file",
        [
            (
                "java", "maven", "quarkus",
                "./mvnw package -DskipTests",
                "./mvnw verify",
                "./mvnw compile -q",
                "./mvnw verify jacoco:report",
                ".java", "pom.xml",
            ),
            (
                "java", "gradle", "spring-boot",
                "./gradlew build -x test",
                "./gradlew test",
                "./gradlew compileJava -q",
                "./gradlew test jacocoTestReport",
                ".java", "build.gradle",
            ),
            (
                "kotlin", "gradle", "ktor",
                "./gradlew build -x test",
                "./gradlew test",
                "./gradlew compileKotlin -q",
                "./gradlew test jacocoTestReport",
                ".kt", "build.gradle.kts",
            ),
            (
                "typescript", "npm", "nestjs",
                "npm run build",
                "npm test",
                "npx --no-install tsc --noEmit",
                "npm test -- --coverage",
                ".ts", "package.json",
            ),
            (
                "python", "pip", "fastapi",
                "pip install -e .",
                "pytest",
                "python3 -m py_compile",
                "pytest --cov",
                ".py", "pyproject.toml",
            ),
            (
                "go", "go", "gin",
                "go build ./...",
                "go test ./...",
                "go build ./...",
                "go test -coverprofile=coverage.out ./...",
                ".go", "go.mod",
            ),
            (
                "rust", "cargo", "axum",
                "cargo build",
                "cargo test",
                "cargo check",
                "cargo tarpaulin",
                ".rs", "Cargo.toml",
            ),
            (
                "csharp", "dotnet", "aspnet",
                "dotnet build",
                "dotnet test",
                "dotnet build --no-restore --verbosity quiet",
                'dotnet test --collect:"XPlat Code Coverage"',
                ".cs", "*.csproj",
            ),
        ],
        ids=[
            "java-maven", "java-gradle", "kotlin-gradle",
            "typescript-npm", "python-pip", "go-go",
            "rust-cargo", "csharp-dotnet",
        ],
    )
    def test_resolve_stack_language_commands(
        self,
        create_project_config,
        lang,
        build_tool,
        fw,
        exp_build,
        exp_test,
        exp_compile,
        exp_coverage,
        exp_ext,
        exp_file,
    ) -> None:
        config = create_project_config(
            language={"name": lang, "version": "17"},
            framework={
                "name": fw,
                "version": "3.0",
                "build_tool": build_tool,
                "native_build": False,
            },
        )
        result = resolve_stack(config)
        assert result.build_cmd == exp_build
        assert result.test_cmd == exp_test
        assert result.compile_cmd == exp_compile
        assert result.coverage_cmd == exp_coverage
        assert result.file_extension == exp_ext
        assert result.build_file == exp_file


class TestResolverDefaultPort:

    @pytest.mark.parametrize(
        "fw, expected_port",
        [
            ("quarkus", 8080),
            ("spring-boot", 8080),
            ("nestjs", 3000),
            ("express", 3000),
            ("fastapi", 8000),
            ("django", 8000),
            ("gin", 8080),
            ("ktor", 8080),
            ("axum", 3000),
            ("actix-web", 8080),
            ("aspnet", 5000),
        ],
    )
    def test_resolve_stack_default_port(
        self,
        create_project_config,
        fw,
        expected_port,
    ) -> None:
        config = create_project_config(
            framework={
                "name": fw,
                "version": "3.0",
                "build_tool": "maven",
                "native_build": False,
            },
        )
        result = resolve_stack(config)
        assert result.default_port == expected_port


class TestResolverHealthPath:

    @pytest.mark.parametrize(
        "fw, expected_path",
        [
            ("quarkus", "/q/health"),
            ("spring-boot", "/actuator/health"),
            ("nestjs", "/health"),
            ("express", "/health"),
            ("fastapi", "/health"),
            ("django", "/health"),
            ("gin", "/health"),
            ("ktor", "/health"),
            ("axum", "/health"),
            ("actix-web", "/health"),
            ("aspnet", "/health"),
        ],
    )
    def test_resolve_stack_health_path(
        self,
        create_project_config,
        fw,
        expected_path,
    ) -> None:
        config = create_project_config(
            framework={
                "name": fw,
                "version": "3.0",
                "build_tool": "maven",
                "native_build": False,
            },
        )
        result = resolve_stack(config)
        assert result.health_path == expected_path


class TestResolverDockerImage:

    @pytest.mark.parametrize(
        "lang, version, expected_image",
        [
            ("java", "17", "eclipse-temurin:17-jre-alpine"),
            ("java", "21", "eclipse-temurin:21-jre-alpine"),
            ("kotlin", "17", "eclipse-temurin:17-jre-alpine"),
            ("typescript", "18", "node:18-alpine"),
            ("python", "3.9", "python:3.9-slim"),
            ("go", "1.21", "golang:1.21-alpine"),
            ("rust", "1.75", "rust:1.75-slim"),
            ("csharp", "8.0", "mcr.microsoft.com/dotnet/aspnet:8.0"),
        ],
    )
    def test_resolve_stack_docker_base_image(
        self,
        create_project_config,
        lang,
        version,
        expected_image,
    ) -> None:
        config = create_project_config(
            language={"name": lang, "version": version},
        )
        result = resolve_stack(config)
        assert result.docker_base_image == expected_image


class TestResolverProtocols:

    @pytest.mark.parametrize(
        "interfaces, expected_protocols",
        [
            (
                [{"type": "rest"}],
                ["openapi"],
            ),
            (
                [{"type": "grpc"}],
                ["proto3"],
            ),
            (
                [
                    {"type": "rest"},
                    {"type": "grpc"},
                    {"type": "event-consumer"},
                ],
                ["openapi", "proto3", "kafka"],
            ),
            (
                [{"type": "event-producer"}],
                ["kafka"],
            ),
            (
                [{"type": "cli"}],
                [],
            ),
            (
                [
                    {"type": "rest"},
                    {"type": "grpc"},
                    {"type": "graphql"},
                    {"type": "websocket"},
                    {"type": "tcp-custom"},
                ],
                [
                    "openapi", "proto3", "graphql",
                    "websocket", "tcp-custom",
                ],
            ),
        ],
        ids=[
            "rest-only", "grpc-only",
            "rest-grpc-event", "event-producer-only",
            "cli-only", "all-network-protocols",
        ],
    )
    def test_resolve_stack_derives_protocols(
        self,
        create_project_config,
        interfaces,
        expected_protocols,
    ) -> None:
        config = create_project_config(
            interfaces=interfaces,
        )
        result = resolve_stack(config)
        assert result.protocols == expected_protocols


class TestResolverProjectType:

    @pytest.mark.parametrize(
        "style, interfaces, expected_type",
        [
            (
                "microservice",
                [{"type": "rest"}],
                "api",
            ),
            (
                "library",
                [{"type": "cli"}],
                "cli",
            ),
            (
                "microservice",
                [{"type": "event-consumer"}],
                "worker",
            ),
            (
                "monolith",
                [{"type": "rest"}],
                "api",
            ),
            (
                "library",
                [],
                "library",
            ),
        ],
        ids=[
            "microservice-rest-api",
            "library-cli",
            "microservice-event-worker",
            "monolith-rest-api",
            "library-no-interfaces",
        ],
    )
    def test_resolve_stack_project_type(
        self,
        create_project_config,
        style,
        interfaces,
        expected_type,
    ) -> None:
        config = create_project_config(
            architecture={"style": style},
            interfaces=interfaces,
        )
        result = resolve_stack(config)
        assert result.project_type == expected_type


class TestResolverNativeBuild:

    @pytest.mark.parametrize(
        "fw, native_build, expected",
        [
            ("quarkus", True, True),
            ("spring-boot", True, True),
            ("click", True, False),
            ("gin", True, False),
            ("quarkus", False, False),
        ],
        ids=[
            "quarkus-native-true",
            "spring-boot-native-true",
            "click-native-unsupported",
            "gin-native-unsupported",
            "quarkus-native-false",
        ],
    )
    def test_resolve_stack_native_supported(
        self,
        create_project_config,
        fw,
        native_build,
        expected,
    ) -> None:
        config = create_project_config(
            framework={
                "name": fw,
                "version": "3.0",
                "build_tool": "maven",
                "native_build": native_build,
            },
        )
        result = resolve_stack(config)
        assert result.native_supported is expected


class TestResolverFullResolution:

    def test_resolve_stack_java_quarkus_full(
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
            architecture={"style": "microservice"},
            interfaces=[{"type": "rest"}],
        )
        result = resolve_stack(config)
        assert result.build_cmd == "./mvnw package -DskipTests"
        assert result.health_path == "/q/health"
        assert result.docker_base_image == (
            "eclipse-temurin:17-jre-alpine"
        )
        assert result.native_supported is True
        assert result.project_type == "api"

    def test_resolve_stack_python_click_full(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            language={"name": "python", "version": "3.9"},
            framework={
                "name": "click",
                "version": "8.1",
                "build_tool": "pip",
                "native_build": False,
            },
            architecture={"style": "library"},
            interfaces=[{"type": "cli"}],
        )
        result = resolve_stack(config)
        assert result.build_cmd == "pip install -e ."
        assert result.build_file == "pyproject.toml"
        assert result.package_manager == "pip"
        assert result.project_type == "cli"

    def test_resolve_stack_unknown_language_defaults(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            language={"name": "haskell", "version": "9"},
            framework={
                "name": "unknown",
                "version": "1.0",
                "build_tool": "cabal",
                "native_build": False,
            },
        )
        result = resolve_stack(config)
        assert result.build_cmd == ""
        assert result.test_cmd == ""
        assert result.docker_base_image == "alpine:latest"
        assert result.default_port == 8080

    def test_resolve_stack_special_version_chars_no_crash(
        self,
        create_project_config,
    ) -> None:
        """Version with special chars does not crash."""
        config = create_project_config(
            language={"name": "python", "version": "3.9-rc1"},
        )
        result = resolve_stack(config)
        assert "3.9-rc1" in result.docker_base_image
