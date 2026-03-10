import type { ProjectConfig } from "../models.js";

/**
 * Return list of interface type strings from config.
 */
export function extractInterfaceTypes(
  config: ProjectConfig,
): string[] {
  return config.interfaces.map((iface) => iface.type);
}

/**
 * Check if a specific interface type exists in config.
 */
export function hasInterface(
  config: ProjectConfig,
  ifaceType: string,
): boolean {
  return config.interfaces.some(
    (iface) => iface.type === ifaceType,
  );
}

/**
 * Check if any of the specified interface types exist.
 */
export function hasAnyInterface(
  config: ProjectConfig,
  ...types: string[]
): boolean {
  const typeSet = new Set(types);
  return config.interfaces.some(
    (iface) => typeSet.has(iface.type),
  );
}
