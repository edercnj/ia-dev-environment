import { describe, it, expect } from "vitest";

import {
  CORE_KNOWLEDGE_PACKS,
  buildInfraPackRules,
} from "../../../src/domain/skill-registry.js";
import { InfraConfig } from "../../../src/models.js";

function buildInfraOverrides(overrides: {
  container?: string;
  orchestrator?: string;
  templating?: string;
  iac?: string;
  registry?: string;
} = {}): { readonly infrastructure: InfraConfig } {
  return {
    infrastructure: new InfraConfig(
      overrides.container ?? "none",
      overrides.orchestrator ?? "none",
      overrides.templating ?? "none",
      overrides.iac ?? "none",
      overrides.registry ?? "none",
    ),
  };
}

describe("CORE_KNOWLEDGE_PACKS", () => {
  it("hasExactly11Packs", () => {
    expect(CORE_KNOWLEDGE_PACKS).toHaveLength(11);
  });

  it("containsAllExpectedPacks", () => {
    const expected = [
      "coding-standards", "architecture", "testing",
      "security", "compliance", "api-design",
      "observability", "resilience", "infrastructure",
      "protocols", "story-planning",
    ];
    for (const pack of expected) {
      expect(CORE_KNOWLEDGE_PACKS).toContain(pack);
    }
  });

  it("isReadonly", () => {
    expect(Object.isFrozen(CORE_KNOWLEDGE_PACKS)).toBe(true);
  });
});

describe("buildInfraPackRules", () => {
  it("kubernetes_includesK8sDeployment", () => {
    const config = buildInfraOverrides({
      orchestrator: "kubernetes",
    });
    const rules = buildInfraPackRules(config);
    const active = rules
      .filter(([, cond]) => cond)
      .map(([name]) => name);
    expect(active).toContain("k8s-deployment");
  });

  it("kustomize_includesK8sKustomize", () => {
    const config = buildInfraOverrides({
      templating: "kustomize",
    });
    const rules = buildInfraPackRules(config);
    const active = rules
      .filter(([, cond]) => cond)
      .map(([name]) => name);
    expect(active).toContain("k8s-kustomize");
  });

  it("helm_includesK8sHelm", () => {
    const config = buildInfraOverrides({
      templating: "helm",
    });
    const rules = buildInfraPackRules(config);
    const active = rules
      .filter(([, cond]) => cond)
      .map(([name]) => name);
    expect(active).toContain("k8s-helm");
  });

  it("docker_includesDockerfile", () => {
    const config = buildInfraOverrides({
      container: "docker",
    });
    const rules = buildInfraPackRules(config);
    const active = rules
      .filter(([, cond]) => cond)
      .map(([name]) => name);
    expect(active).toContain("dockerfile");
  });

  it("registry_includesContainerRegistry", () => {
    const config = buildInfraOverrides({
      registry: "ecr",
    });
    const rules = buildInfraPackRules(config);
    const active = rules
      .filter(([, cond]) => cond)
      .map(([name]) => name);
    expect(active).toContain("container-registry");
  });

  it("terraform_includesTerraform", () => {
    const config = buildInfraOverrides({
      iac: "terraform",
    });
    const rules = buildInfraPackRules(config);
    const active = rules
      .filter(([, cond]) => cond)
      .map(([name]) => name);
    expect(active).toContain("iac-terraform");
  });

  it("crossplane_includesCrossplane", () => {
    const config = buildInfraOverrides({
      iac: "crossplane",
    });
    const rules = buildInfraPackRules(config);
    const active = rules
      .filter(([, cond]) => cond)
      .map(([name]) => name);
    expect(active).toContain("iac-crossplane");
  });

  it("allNone_returnsNoActiveRules", () => {
    const config = buildInfraOverrides();
    const rules = buildInfraPackRules(config);
    const active = rules
      .filter(([, cond]) => cond)
      .map(([name]) => name);
    expect(active).toEqual([]);
  });

  it("orchestratorNotK8s_excludesK8sRules", () => {
    const config = buildInfraOverrides({
      orchestrator: "ecs",
    });
    const rules = buildInfraPackRules(config);
    const active = rules
      .filter(([, cond]) => cond)
      .map(([name]) => name);
    expect(active).not.toContain("k8s-deployment");
  });

  it("returnsSevenRules", () => {
    const config = buildInfraOverrides();
    const rules = buildInfraPackRules(config);
    expect(rules).toHaveLength(7);
  });

  it("fullInfraConfig_returnsAllActiveRules", () => {
    const config = buildInfraOverrides({
      orchestrator: "kubernetes",
      templating: "helm",
      container: "docker",
      registry: "ecr",
      iac: "terraform",
    });
    const rules = buildInfraPackRules(config);
    const active = rules
      .filter(([, cond]) => cond)
      .map(([name]) => name);
    expect(active).toContain("k8s-deployment");
    expect(active).toContain("k8s-helm");
    expect(active).toContain("dockerfile");
    expect(active).toContain("container-registry");
    expect(active).toContain("iac-terraform");
    expect(active).not.toContain("k8s-kustomize");
    expect(active).not.toContain("iac-crossplane");
  });
});
