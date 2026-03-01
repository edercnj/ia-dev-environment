from __future__ import annotations

import pytest

from claude_setup.domain.resolved_stack import ResolvedStack


class TestResolvedStack:

    def test_resolved_stack_init_stores_all_fields(self) -> None:
        stack = ResolvedStack(
            build_cmd="./mvnw package -DskipTests",
            test_cmd="./mvnw verify",
            compile_cmd="./mvnw compile -q",
            coverage_cmd="./mvnw verify jacoco:report",
            docker_base_image="eclipse-temurin:17-jre-alpine",
            health_path="/q/health",
            package_manager="maven",
            default_port=8080,
            file_extension=".java",
            build_file="pom.xml",
            native_supported=True,
            project_type="api",
            protocols=["rest", "grpc"],
        )
        assert stack.build_cmd == "./mvnw package -DskipTests"
        assert stack.test_cmd == "./mvnw verify"
        assert stack.compile_cmd == "./mvnw compile -q"
        assert stack.coverage_cmd == "./mvnw verify jacoco:report"
        assert stack.docker_base_image == "eclipse-temurin:17-jre-alpine"
        assert stack.health_path == "/q/health"
        assert stack.package_manager == "maven"
        assert stack.default_port == 8080
        assert stack.file_extension == ".java"
        assert stack.build_file == "pom.xml"
        assert stack.native_supported is True
        assert stack.project_type == "api"
        assert stack.protocols == ["rest", "grpc"]

    def test_resolved_stack_protocols_stores_list(self) -> None:
        stack = ResolvedStack(
            build_cmd="cmd",
            test_cmd="cmd",
            compile_cmd="cmd",
            coverage_cmd="cmd",
            docker_base_image="img",
            health_path="/h",
            package_manager="pm",
            default_port=80,
            file_extension=".x",
            build_file="f",
            native_supported=False,
            project_type="api",
            protocols=["rest", "grpc", "graphql"],
        )
        assert isinstance(stack.protocols, list)
        assert len(stack.protocols) == 3

    def test_resolved_stack_empty_protocols_list(self) -> None:
        stack = ResolvedStack(
            build_cmd="cmd",
            test_cmd="cmd",
            compile_cmd="cmd",
            coverage_cmd="cmd",
            docker_base_image="img",
            health_path="/h",
            package_manager="pm",
            default_port=80,
            file_extension=".x",
            build_file="f",
            native_supported=False,
            project_type="library",
            protocols=[],
        )
        assert stack.protocols == []

    def test_resolved_stack_frozen_raises_on_assign(self) -> None:
        stack = ResolvedStack(
            build_cmd="cmd",
            test_cmd="cmd",
            compile_cmd="cmd",
            coverage_cmd="cmd",
            docker_base_image="img",
            health_path="/h",
            package_manager="pm",
            default_port=80,
            file_extension=".x",
            build_file="f",
            native_supported=False,
            project_type="api",
            protocols=[],
        )
        with pytest.raises(AttributeError):
            stack.build_cmd = "new"  # type: ignore[misc]

    def test_resolved_stack_default_protocols_empty(self) -> None:
        stack = ResolvedStack(
            build_cmd="cmd",
            test_cmd="cmd",
            compile_cmd="cmd",
            coverage_cmd="cmd",
            docker_base_image="img",
            health_path="/h",
            package_manager="pm",
            default_port=80,
            file_extension=".x",
            build_file="f",
            native_supported=False,
            project_type="api",
        )
        assert stack.protocols == []
