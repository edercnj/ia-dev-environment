import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    include: ["tests/**/*.test.ts"],
    pool: "forks",
    poolOptions: {
      forks: {
        maxForks: 3,
      },
    },
    maxConcurrency: 5,
    coverage: {
      provider: "v8",
      reporter: ["text", "lcov"],
      include: ["src/**/*.ts"],
      exclude: ["dist/**", "resources/**", "tests/**"],
      thresholds: {
        lines: 95,
        branches: 90,
      },
    },
  },
});
