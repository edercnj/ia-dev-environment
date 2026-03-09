import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    include: ["tests/**/*.test.ts"],
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
