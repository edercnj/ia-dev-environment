/** Computed stack values derived from ProjectConfig. */
export interface ResolvedStack {
  readonly buildCmd: string;
  readonly testCmd: string;
  readonly compileCmd: string;
  readonly coverageCmd: string;
  readonly dockerBaseImage: string;
  readonly healthPath: string;
  readonly packageManager: string;
  readonly defaultPort: number;
  readonly fileExtension: string;
  readonly buildFile: string;
  readonly nativeSupported: boolean;
  readonly projectType: string;
  readonly protocols: readonly string[];
}
