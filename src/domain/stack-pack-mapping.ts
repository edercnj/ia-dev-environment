/**
 * Framework to knowledge pack mapping.
 *
 * Migrated from Python `domain/stack_pack_mapping.py`.
 */

/** Framework name to knowledge pack directory name (11 entries). */
export const FRAMEWORK_STACK_PACK: Readonly<Record<string, string>> = {
  "quarkus": "quarkus-patterns",
  "spring-boot": "spring-patterns",
  "nestjs": "nestjs-patterns",
  "express": "express-patterns",
  "fastapi": "fastapi-patterns",
  "django": "django-patterns",
  "gin": "gin-patterns",
  "ktor": "ktor-patterns",
  "axum": "axum-patterns",
  "dotnet": "dotnet-patterns",
  "click": "click-cli-patterns",
};

/** Return the knowledge pack directory name for a framework, or empty string. */
export function getStackPackName(framework: string): string {
  return FRAMEWORK_STACK_PACK[framework] ?? "";
}
