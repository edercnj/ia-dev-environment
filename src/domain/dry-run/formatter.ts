/**
 * Dry-run plan formatter — renders plans as human-readable text.
 */

import type {
  DryRunPlan,
  DryRunPhaseInfo,
  DryRunStoryInfo,
  DryRunStoryDetail,
} from "./types.js";

const CRITICAL_MARKER = " [CRITICAL]";

export function formatPlan(plan: DryRunPlan): string {
  const lines: string[] = [];
  lines.push(formatHeader(plan));
  lines.push("");
  for (const phase of plan.phases) {
    lines.push(...formatPhase(phase));
    lines.push("");
  }
  return lines.join("\n");
}

function formatHeader(plan: DryRunPlan): string {
  return (
    `Dry-Run Plan: ${plan.epicId}` +
    ` | ${plan.totalStories} stories` +
    ` | ${plan.totalPhases} phases` +
    ` | mode: ${plan.mode}`
  );
}

function formatPhase(phase: DryRunPhaseInfo): string[] {
  const lines: string[] = [];
  const header =
    `Phase ${phase.phase}` +
    ` (${phase.stories.length} stories,` +
    ` parallel: ${phase.parallelCount})`;
  lines.push(header);
  for (const story of phase.stories) {
    lines.push(formatStoryLine(story));
  }
  return lines;
}

function formatStoryLine(story: DryRunStoryInfo): string {
  const critical = story.isCriticalPath
    ? CRITICAL_MARKER
    : "";
  const deps = story.dependenciesSatisfied
    ? ""
    : ` (blocked by: ${story.blockedBy.join(", ")})`;
  return (
    `  ${story.id} - ${story.title}` +
    ` [${story.status}]${critical}${deps}`
  );
}

export function formatStoryDetail(
  detail: DryRunStoryDetail,
): string {
  const lines: string[] = [];
  lines.push(`Story: ${detail.id} - ${detail.title}`);
  lines.push(`Phase: ${detail.phase}`);
  lines.push(`Status: ${detail.status}`);
  lines.push(formatCriticalLine(detail.isCriticalPath));
  lines.push(formatDepList("Dependencies", detail.dependencies));
  lines.push(formatDepList("Dependents", detail.dependents));
  return lines.join("\n");
}

function formatCriticalLine(isCritical: boolean): string {
  return `Critical Path: ${isCritical ? "Yes" : "No"}`;
}

function formatDepList(
  label: string,
  items: readonly string[],
): string {
  if (items.length === 0) {
    return `${label}: none`;
  }
  return `${label}: ${items.join(", ")}`;
}
