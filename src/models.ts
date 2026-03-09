export interface ProjectFoundation {
  readonly name: string;
  readonly version: string;
  readonly moduleType: "module";
}

export const DEFAULT_FOUNDATION: ProjectFoundation = {
  name: "ia-dev-environment",
  version: "0.1.0",
  moduleType: "module",
};
