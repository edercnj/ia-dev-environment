from __future__ import annotations

from typing import Dict, List, Tuple

# --- Language + Build Tool -> Commands Mapping ---
# Key: (language, build_tool) tuple
# Value: dict with compile_cmd, build_cmd, test_cmd, coverage_cmd,
#        file_extension, build_file, package_manager

LANGUAGE_COMMANDS: Dict[Tuple[str, str], Dict[str, str]] = {
    ("java", "maven"): {
        "compile_cmd": "./mvnw compile -q",
        "build_cmd": "./mvnw package -DskipTests",
        "test_cmd": "./mvnw verify",
        "coverage_cmd": "./mvnw verify jacoco:report",
        "file_extension": ".java",
        "build_file": "pom.xml",
        "package_manager": "maven",
    },
    ("java", "gradle"): {
        "compile_cmd": "./gradlew compileJava -q",
        "build_cmd": "./gradlew build -x test",
        "test_cmd": "./gradlew test",
        "coverage_cmd": "./gradlew test jacocoTestReport",
        "file_extension": ".java",
        "build_file": "build.gradle",
        "package_manager": "gradle",
    },
    ("kotlin", "gradle"): {
        "compile_cmd": "./gradlew compileKotlin -q",
        "build_cmd": "./gradlew build -x test",
        "test_cmd": "./gradlew test",
        "coverage_cmd": "./gradlew test jacocoTestReport",
        "file_extension": ".kt",
        "build_file": "build.gradle.kts",
        "package_manager": "gradle",
    },
    ("typescript", "npm"): {
        "compile_cmd": "npx --no-install tsc --noEmit",
        "build_cmd": "npm run build",
        "test_cmd": "npm test",
        "coverage_cmd": "npm test -- --coverage",
        "file_extension": ".ts",
        "build_file": "package.json",
        "package_manager": "npm",
    },
    ("python", "pip"): {
        "compile_cmd": "python3 -m py_compile",
        "build_cmd": "pip install -e .",
        "test_cmd": "pytest",
        "coverage_cmd": "pytest --cov",
        "file_extension": ".py",
        "build_file": "pyproject.toml",
        "package_manager": "pip",
    },
    ("go", "go"): {
        "compile_cmd": "go build ./...",
        "build_cmd": "go build ./...",
        "test_cmd": "go test ./...",
        "coverage_cmd": "go test -coverprofile=coverage.out ./...",
        "file_extension": ".go",
        "build_file": "go.mod",
        "package_manager": "go",
    },
    ("rust", "cargo"): {
        "compile_cmd": "cargo check",
        "build_cmd": "cargo build",
        "test_cmd": "cargo test",
        "coverage_cmd": "cargo tarpaulin",
        "file_extension": ".rs",
        "build_file": "Cargo.toml",
        "package_manager": "cargo",
    },
    ("csharp", "dotnet"): {
        "compile_cmd": "dotnet build --no-restore --verbosity quiet",
        "build_cmd": "dotnet build",
        "test_cmd": "dotnet test",
        "coverage_cmd": 'dotnet test --collect:"XPlat Code Coverage"',
        "file_extension": ".cs",
        "build_file": "*.csproj",
        "package_manager": "dotnet",
    },
}

# --- Framework -> Default Port ---

FRAMEWORK_PORTS: Dict[str, int] = {
    "quarkus": 8080,
    "spring-boot": 8080,
    "nestjs": 3000,
    "express": 3000,
    "fastapi": 8000,
    "django": 8000,
    "gin": 8080,
    "ktor": 8080,
    "axum": 3000,
    "actix-web": 8080,
    "aspnet": 5000,
}

DEFAULT_PORT_FALLBACK = 8080

# --- Framework -> Health Path ---

FRAMEWORK_HEALTH_PATHS: Dict[str, str] = {
    "quarkus": "/q/health",
    "spring-boot": "/actuator/health",
    "nestjs": "/health",
    "express": "/health",
    "fastapi": "/health",
    "django": "/health",
    "gin": "/health",
    "ktor": "/health",
    "axum": "/health",
    "actix-web": "/health",
    "aspnet": "/health",
}

DEFAULT_HEALTH_PATH = "/health"

# --- Framework -> Valid Languages ---

FRAMEWORK_LANGUAGE_RULES: Dict[str, List[str]] = {
    "quarkus": ["java", "kotlin"],
    "spring-boot": ["java", "kotlin"],
    "nestjs": ["typescript"],
    "express": ["typescript"],
    "fastify": ["typescript"],
    "fastapi": ["python"],
    "django": ["python"],
    "flask": ["python"],
    "stdlib": ["go"],
    "gin": ["go"],
    "fiber": ["go"],
    "ktor": ["kotlin"],
    "axum": ["rust"],
    "actix-web": ["rust"],
    "aspnet": ["csharp"],
}

# --- Native Build Support ---

NATIVE_SUPPORTED_FRAMEWORKS: Tuple[str, ...] = (
    "quarkus",
    "spring-boot",
)

# --- Interface Types ---

VALID_INTERFACE_TYPES: Tuple[str, ...] = (
    "rest",
    "grpc",
    "graphql",
    "websocket",
    "tcp-custom",
    "cli",
    "event-consumer",
    "event-producer",
    "scheduled",
)

# --- Architecture Styles ---

VALID_ARCHITECTURE_STYLES: Tuple[str, ...] = (
    "microservice",
    "modular-monolith",
    "monolith",
    "library",
    "serverless",
)

# --- Interface Type -> Protocol ---

INTERFACE_PROTOCOL_MAP: Dict[str, str] = {
    "rest": "openapi",
    "grpc": "proto3",
    "graphql": "graphql",
    "websocket": "websocket",
    "tcp-custom": "tcp-custom",
    "event-consumer": "kafka",
    "event-producer": "kafka",
}

# --- Docker Base Images ---
# Template strings with {version} placeholder

DOCKER_BASE_IMAGES: Dict[str, str] = {
    "java": "eclipse-temurin:{version}-jre-alpine",
    "kotlin": "eclipse-temurin:{version}-jre-alpine",
    "typescript": "node:{version}-alpine",
    "python": "python:{version}-slim",
    "go": "golang:{version}-alpine",
    "rust": "rust:{version}-slim",
    "csharp": "mcr.microsoft.com/dotnet/aspnet:{version}",
}

DEFAULT_DOCKER_IMAGE = "alpine:latest"
