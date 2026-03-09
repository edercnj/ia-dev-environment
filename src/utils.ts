import { cp, lstat, mkdtemp, rename, rm, stat } from "node:fs/promises";
import { statSync } from "node:fs";
import { homedir, tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const DIRECTORY_SEPARATOR_PATTERN = /[\\/]+$/u;
const ROOT_PATH_PATTERN = /^[\\/]+$/u;
const WINDOWS_ROOT_PATH_PATTERN = /^[A-Za-z]:[\\/]$/u;

const PROTECTED_PATHS_SET = new Set([
  "/", "/tmp", "/var", "/etc", "/usr",
]);

export const PROTECTED_PATHS: ReadonlyArray<string> = Object.freeze(
  [...PROTECTED_PATHS_SET],
);

export function normalizeDirectory(path: string): string {
  if (ROOT_PATH_PATTERN.test(path)) {
    return "/";
  }

  if (WINDOWS_ROOT_PATH_PATTERN.test(path)) {
    return path[0] + ":\\";
  }

  const normalizedPath = path.replace(DIRECTORY_SEPARATOR_PATTERN, "");
  return normalizedPath.length > 0 ? normalizedPath : path;
}

export function rejectDangerousPath(resolvedPath: string): void {
  const cwd = process.cwd();
  const home = homedir();

  if (resolvedPath === cwd) {
    throw new Error(
      `Destination must not be the current directory: ${resolvedPath}`,
    );
  }
  if (resolvedPath === home) {
    throw new Error(
      `Destination must not be the home directory: ${resolvedPath}`,
    );
  }
  if (PROTECTED_PATHS_SET.has(resolvedPath)) {
    throw new Error(
      `Destination is a protected system path: ${resolvedPath}`,
    );
  }
}

let originalDebug: typeof console.debug | undefined;

export function setupLogging(verbose: boolean): void {
  if (verbose) {
    if (originalDebug !== undefined) {
      console.debug = originalDebug;
      originalDebug = undefined;
    }
  } else {
    if (originalDebug === undefined) {
      originalDebug = console.debug;
    }
    console.debug = () => {};
  }
}

export function findResourcesDir(metaUrl?: string): string {
  const currentFile = fileURLToPath(metaUrl ?? import.meta.url);
  const packageRoot = resolve(dirname(currentFile), "..");
  const resourcesPath = join(packageRoot, "resources");

  const stats = statSync(resourcesPath, { throwIfNoEntry: false });
  if (!stats?.isDirectory()) {
    throw new Error(
      `Resources directory not found: ${resourcesPath}`,
    );
  }
  return resourcesPath;
}

export async function validateDestPath(
  destDir: string,
): Promise<string> {
  try {
    const stats = await lstat(destDir);
    if (stats.isSymbolicLink()) {
      throw new Error(
        `Destination must not be a symlink: ${destDir}`,
      );
    }
  } catch (error: unknown) {
    if (
      error instanceof Error &&
      "code" in error &&
      error.code === "ENOENT"
    ) {
      // Path does not exist yet — valid for new destinations
    } else {
      throw error;
    }
  }

  const resolvedPath = resolve(destDir);
  rejectDangerousPath(resolvedPath);
  return resolvedPath;
}

export async function atomicOutput<T>(
  destDir: string,
  callback: (tempDir: string) => Promise<T>,
): Promise<T> {
  const resolvedDest = await validateDestPath(destDir);
  const tempDir = await mkdtemp(join(tmpdir(), "ia-dev-env-"));
  try {
    const result = await callback(tempDir);

    const destExists = await pathExists(resolvedDest);
    let backupDir: string | undefined;

    if (destExists) {
      backupDir = `${resolvedDest}.bak-${Date.now()}`;
      await rename(resolvedDest, backupDir);
    }

    try {
      await cp(tempDir, resolvedDest, { recursive: true });
    } catch (copyError: unknown) {
      if (backupDir !== undefined) {
        await rename(backupDir, resolvedDest);
      }
      throw copyError;
    }

    if (backupDir !== undefined) {
      await rm(backupDir, { recursive: true, force: true });
    }

    return result;
  } finally {
    await rm(tempDir, { recursive: true, force: true });
  }
}

async function pathExists(target: string): Promise<boolean> {
  try {
    await stat(target);
    return true;
  } catch {
    return false;
  }
}
