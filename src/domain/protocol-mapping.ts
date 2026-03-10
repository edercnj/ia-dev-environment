/**
 * Protocol derivation from interface types.
 *
 * Migrated from Python `domain/protocol_mapping.py`.
 */

import type { ProjectConfig } from "../models.js";

import { readdirSync, statSync } from "node:fs";
import { join } from "node:path";

/** Interface type to protocol directory names. */
export const INTERFACE_PROTOCOL_MAP: Readonly<Record<string, readonly string[]>> = {
  "rest": ["rest"],
  "grpc": ["grpc"],
  "graphql": ["graphql"],
  "websocket": ["websocket"],
  "event-consumer": ["event-driven", "messaging"],
  "event-producer": ["event-driven", "messaging"],
  "cli": [],
};

export const EVENT_PREFIX = "event-";
export const EVENT_DRIVEN_PROTOCOL = "event-driven";

/**
 * Map interface types to protocol directory names.
 *
 * Returns deduplicated, sorted list of protocol names.
 */
export function deriveProtocols(config: ProjectConfig): string[] {
  const protocols: string[] = [];
  for (const iface of config.interfaces) {
    const mapped = INTERFACE_PROTOCOL_MAP[iface.type];
    if (mapped !== undefined) {
      protocols.push(...mapped);
    } else if (iface.type.startsWith(EVENT_PREFIX)) {
      protocols.push(EVENT_DRIVEN_PROTOCOL);
    }
  }
  return [...new Set(protocols)].sort();
}

/**
 * List .md files per protocol directory.
 *
 * Applies broker-specific filtering for messaging protocol.
 * Skips missing directories without error.
 */
export function deriveProtocolFiles(
  resourcesDir: string,
  protocolNames: readonly string[],
  config: ProjectConfig,
): Record<string, string[]> {
  const protocolsRoot = join(resourcesDir, "protocols");
  const result: Record<string, string[]> = {};
  for (const protocol of protocolNames) {
    const protocolDir = join(protocolsRoot, protocol);
    try {
      if (!statSync(protocolDir).isDirectory()) {
        continue;
      }
    } catch {
      continue;
    }
    let files: string[];
    if (protocol === "messaging") {
      files = selectMessagingFiles(protocolDir, config);
    } else {
      files = readdirSync(protocolDir)
        .filter((f) => f.endsWith(".md"))
        .sort()
        .map((f) => join(protocolDir, f));
    }
    if (files.length > 0) {
      result[protocol] = files;
    }
  }
  return result;
}

/** Select broker-specific or all messaging files. */
export function selectMessagingFiles(
  messagingDir: string,
  config: ProjectConfig,
): string[] {
  const broker = extractBroker(config);
  if (broker) {
    const specific = join(messagingDir, `${broker}.md`);
    try {
      if (statSync(specific).isFile()) {
        return [specific];
      }
    } catch {
      // fall through to glob all
    }
  }
  return readdirSync(messagingDir)
    .filter((f) => f.endsWith(".md"))
    .sort()
    .map((f) => join(messagingDir, f));
}

/** Extract broker name from first interface that has one. */
export function extractBroker(config: ProjectConfig): string {
  for (const iface of config.interfaces) {
    if (iface.broker) {
      return iface.broker;
    }
  }
  return "";
}
