/**
 * ProtocolsAssembler — derives and concatenates protocol conventions.
 *
 * Migrated from Python `assembler/protocols_assembler.py` (84 lines).
 * Derivation logic lives in {@link ../domain/protocol-mapping.ts}.
 *
 * @remarks
 * Protocol files are concatenated raw — no template placeholder replacement.
 * The `engine` parameter is accepted for API uniformity but is not used.
 *
 * @module
 */
import * as fs from "node:fs";
import * as path from "node:path";
import type { ProjectConfig } from "../models.js";
import type { TemplateEngine } from "../template-engine.js";
import {
  deriveProtocols,
  deriveProtocolFiles,
} from "../domain/protocol-mapping.js";

const SKILLS_DIR = "skills";
const PROTOCOLS_SKILL_DIR = "protocols";
const REFERENCES_DIR = "references";
const PROTOCOL_SEPARATOR = "\n\n---\n\n";
const CONVENTIONS_SUFFIX = "-conventions.md";

/** Assembles protocol knowledge packs from source templates. */
export class ProtocolsAssembler {
  /** Orchestrate protocol derivation and concatenation. */
  assemble(
    config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    _engine: TemplateEngine,
  ): string[] {
    const protocolNames = deriveProtocols(config);
    if (protocolNames.length === 0) return [];
    const protocolFiles = deriveProtocolFiles(
      resourcesDir, protocolNames, config,
    );
    if (Object.keys(protocolFiles).length === 0) return [];
    return this.generateOutput(protocolFiles, outputDir);
  }

  private generateOutput(
    protocolFiles: Record<string, string[]>,
    outputDir: string,
  ): string[] {
    const refsDir = this.buildRefsDir(outputDir);
    fs.mkdirSync(refsDir, { recursive: true });
    const results: string[] = [];
    for (const protocol of Object.keys(protocolFiles).sort()) {
      const files = protocolFiles[protocol]!;
      const destName = protocol + CONVENTIONS_SUFFIX;
      const destPath = path.join(refsDir, destName);
      results.push(this.concatProtocolFiles(files, destPath));
    }
    return results;
  }

  private buildRefsDir(outputDir: string): string {
    return path.join(
      outputDir, SKILLS_DIR, PROTOCOLS_SKILL_DIR, REFERENCES_DIR,
    );
  }

  private concatProtocolFiles(
    protocolFiles: readonly string[],
    destPath: string,
  ): string {
    const sections = protocolFiles.map(
      (f) => fs.readFileSync(f, "utf-8"),
    );
    fs.writeFileSync(
      destPath, sections.join(PROTOCOL_SEPARATOR), "utf-8",
    );
    return destPath;
  }
}
