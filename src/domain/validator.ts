/**
 * Stack validation functions.
 *
 * Migrated from Python `domain/validator.py`.
 * Validates language-framework compatibility, version constraints,
 * native build support, interface types, and architecture styles.
 */
import { statSync } from "node:fs";
import { join } from "node:path";

import type { ProjectConfig } from "../models.js";
import {
  FRAMEWORK_LANGUAGE_RULES,
  NATIVE_SUPPORTED_FRAMEWORKS,
  VALID_ARCHITECTURE_STYLES,
  VALID_INTERFACE_TYPES,
} from "./stack-mapping.js";

export const JAVA_17_MINIMUM = 17;
export const PYTHON_310_MINOR = 10;
export const FRAMEWORK_VERSION_3 = 3;
export const FRAMEWORK_VERSION_5 = 5;

const EXPECTED_DIRECTORIES: readonly string[] = [
  "skills",
  ".claude/rules",
];

/** Extract major version number from a version string. */
export function extractMajor(version: string): number | undefined {
  if (!version) return undefined;
  const parts = version.split(".");
  const first = parts[0] ?? "";
  const major = parseInt(first, 10);
  return Number.isNaN(major) ? undefined : major;
}

/** Extract minor version number from a version string. */
export function extractMinor(version: string): number | undefined {
  if (!version) return undefined;
  const parts = version.split(".");
  if (parts.length < 2) return undefined;
  const second = parts[1] ?? "";
  const minor = parseInt(second, 10);
  return Number.isNaN(minor) ? undefined : minor;
}

function validateLanguageFramework(config: ProjectConfig): string[] {
  const frameworkName = config.framework.name;
  const languageName = config.language.name;
  const validLanguages = FRAMEWORK_LANGUAGE_RULES[frameworkName];
  if (validLanguages === undefined) return [];
  if (!validLanguages.includes(languageName)) {
    const expected = validLanguages.join(", ");
    return [
      `Framework '${frameworkName}' requires language `
      + `'${expected}', got '${languageName}'`,
    ];
  }
  return [];
}

function checkJava17Requirement(
  fw: string,
  fwMajor: number | undefined,
  langMajor: number,
): string[] {
  const needsCheck =
    fwMajor !== undefined
    && fwMajor >= FRAMEWORK_VERSION_3
    && (fw === "quarkus" || fw === "spring-boot");
  if (needsCheck && langMajor < JAVA_17_MINIMUM) {
    const fwTitle = fw.charAt(0).toUpperCase() + fw.slice(1);
    return [
      `${fwTitle} ${fwMajor}.x requires Java 17+, `
      + `got Java ${langMajor}`,
    ];
  }
  return [];
}

function checkJavaFrameworkVersion(config: ProjectConfig): string[] {
  const fw = config.framework.name;
  const lang = config.language.name;
  if (lang !== "java" || (fw !== "quarkus" && fw !== "spring-boot")) {
    return [];
  }
  const fwMajor = extractMajor(config.framework.version);
  const langMajor = extractMajor(config.language.version);
  if (langMajor === undefined) return [];
  return checkJava17Requirement(fw, fwMajor, langMajor);
}

export const PYTHON_3_MAJOR = 3;

function checkDjangoPythonVersion(config: ProjectConfig): string[] {
  if (config.framework.name !== "django") return [];
  const fwMajor = extractMajor(config.framework.version);
  if (fwMajor === undefined || fwMajor < FRAMEWORK_VERSION_5) return [];
  const pyMajor = extractMajor(config.language.version);
  const pyMinor = extractMinor(config.language.version);
  if (pyMajor === undefined || pyMinor === undefined) return [];
  if (pyMajor < PYTHON_3_MAJOR || pyMinor < PYTHON_310_MINOR) {
    return [
      `Django 5.x requires Python 3.10+, `
      + `got Python ${config.language.version}`,
    ];
  }
  return [];
}

function validateVersionRequirements(config: ProjectConfig): string[] {
  const errors: string[] = [];
  errors.push(...checkJavaFrameworkVersion(config));
  errors.push(...checkDjangoPythonVersion(config));
  return errors;
}

function validateNativeBuild(config: ProjectConfig): string[] {
  if (!config.framework.nativeBuild) return [];
  const fw = config.framework.name;
  if (!NATIVE_SUPPORTED_FRAMEWORKS.includes(fw)) {
    return [
      `Native build is not supported for framework '${fw}'`,
    ];
  }
  return [];
}

function validateInterfaceTypes(config: ProjectConfig): string[] {
  const errors: string[] = [];
  for (const iface of config.interfaces) {
    if (!VALID_INTERFACE_TYPES.includes(iface.type)) {
      errors.push(
        `Invalid interface type: '${iface.type}'. `
        + `Valid: ${VALID_INTERFACE_TYPES.join(", ")}`,
      );
    }
  }
  return errors;
}

function validateArchitectureStyle(config: ProjectConfig): string[] {
  const style = config.architecture.style;
  if (!VALID_ARCHITECTURE_STYLES.includes(style)) {
    return [
      `Invalid architecture style: '${style}'. `
      + `Valid: ${VALID_ARCHITECTURE_STYLES.join(", ")}`,
    ];
  }
  return [];
}

/** Run all validations and return aggregated errors. */
export function validateStack(config: ProjectConfig): string[] {
  const errors: string[] = [];
  errors.push(...validateLanguageFramework(config));
  errors.push(...validateVersionRequirements(config));
  errors.push(...validateNativeBuild(config));
  errors.push(...validateInterfaceTypes(config));
  errors.push(...validateArchitectureStyle(config));
  return errors;
}

function isDirectory(path: string): boolean {
  try {
    return statSync(path).isDirectory();
  } catch {
    return false;
  }
}

/** Verify referenced directories exist on filesystem. */
export function verifyCrossReferences(
  config: ProjectConfig,
  resourcesDir: string,
): string[] {
  if (!isDirectory(resourcesDir)) {
    return [`Source directory does not exist: ${resourcesDir}`];
  }
  const errors: string[] = [];
  for (const directory of EXPECTED_DIRECTORIES) {
    if (!isDirectory(join(resourcesDir, directory))) {
      errors.push(`Expected directory not found: ${directory}`);
    }
  }
  return errors;
}
