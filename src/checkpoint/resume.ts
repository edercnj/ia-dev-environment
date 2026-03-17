import type {
  ExecutionState,
  ReclassificationEntry,
  StoryEntry,
} from "./types.js";
import { MAX_RETRIES, StoryStatus } from "./types.js";

type ReclassifyResult = {
  readonly stories: Record<string, StoryEntry>;
  readonly reclassified: readonly ReclassificationEntry[];
};

function reclassifySingle(
  entry: StoryEntry,
  maxRetries: number,
): StoryEntry | null {
  const { status, retries } = entry;
  if (status === StoryStatus.IN_PROGRESS) {
    return { ...entry, status: StoryStatus.PENDING };
  }
  if (status === StoryStatus.PARTIAL) {
    return { ...entry, status: StoryStatus.PENDING };
  }
  if (status === StoryStatus.FAILED && retries < maxRetries) {
    return { ...entry, status: StoryStatus.PENDING };
  }
  return null;
}

export function reclassifyStories(
  stories: Readonly<Record<string, StoryEntry>>,
  maxRetries: number,
): ReclassifyResult {
  const result: Record<string, StoryEntry> = {};
  const reclassified: ReclassificationEntry[] = [];

  for (const [id, entry] of Object.entries(stories)) {
    const updated = reclassifySingle(entry, maxRetries);
    if (updated !== null) {
      result[id] = updated;
      reclassified.push({
        storyId: id,
        from: entry.status,
        to: updated.status,
      });
    } else {
      result[id] = { ...entry };
    }
  }

  return { stories: result, reclassified };
}

function allDepsSucceeded(
  blockedBy: readonly string[],
  stories: Readonly<Record<string, StoryEntry>>,
): boolean {
  return blockedBy.every(
    (dep) => stories[dep]?.status === StoryStatus.SUCCESS,
  );
}

export function reevaluateBlocked(
  stories: Readonly<Record<string, StoryEntry>>,
): ReclassifyResult {
  const result: Record<string, StoryEntry> = {};
  const reclassified: ReclassificationEntry[] = [];

  for (const [id, entry] of Object.entries(stories)) {
    if (entry.status !== StoryStatus.BLOCKED) {
      result[id] = { ...entry };
      continue;
    }
    const deps = entry.blockedBy;
    if (deps === undefined) {
      result[id] = { ...entry };
      continue;
    }
    if (allDepsSucceeded(deps, stories)) {
      const updated = { ...entry, status: StoryStatus.PENDING };
      result[id] = updated;
      reclassified.push({
        storyId: id,
        from: StoryStatus.BLOCKED,
        to: StoryStatus.PENDING,
      });
    } else {
      result[id] = { ...entry };
    }
  }

  return { stories: result, reclassified };
}

export function prepareResume(
  state: ExecutionState,
  maxRetries: number = MAX_RETRIES,
): {
  state: ExecutionState;
  reclassified: readonly ReclassificationEntry[];
} {
  const first = reclassifyStories(
    state.stories,
    maxRetries,
  );
  const second = reevaluateBlocked(first.stories);
  const allReclassified = [
    ...first.reclassified,
    ...second.reclassified,
  ];

  return {
    state: { ...state, stories: second.stories },
    reclassified: allReclassified,
  };
}
