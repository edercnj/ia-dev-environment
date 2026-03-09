const DIRECTORY_SEPARATOR_PATTERN = /[\\/]+$/u;
const ROOT_PATH_PATTERN = /^[\\/]+$/u;
const WINDOWS_ROOT_PATH_PATTERN = /^[A-Za-z]:[\\/]$/u;

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
