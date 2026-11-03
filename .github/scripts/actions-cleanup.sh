#!/usr/bin/env bash
# Deletes finished GitHub Actions runs (together with their logs and artifacts)
# and stale Gradle cache entries. Queued and running workflows are never touched.
#
# Kept runs:
#   * the newest finished run of every workflow on every branch, so the latest
#     APK, reports, screenshots or dependency patch stay downloadable;
#   * the newest successful build run on the default branch (current APK).
# Kept caches: the newest entry of every Gradle cache kind on the default branch.
# Caches of other refs are removed: branch and pull request jobs only read caches.
#
# DRY_RUN=1 prints the plan without deleting anything.
set -euo pipefail

repo="${GITHUB_REPOSITORY:?GITHUB_REPOSITORY is required}"
default_branch="${DEFAULT_BRANCH:-master}"
build_workflow="${BUILD_WORKFLOW_PATH:-.github/workflows/android.yml}"
dry_run="${DRY_RUN:-0}"

remove() {
  if [ "$dry_run" = 1 ]; then
    printf 'would delete %s\n' "$2"
  else
    gh api --method DELETE "$1" --silent
    printf 'deleted %s\n' "$2"
  fi
}

runs=$(gh api --paginate "repos/$repo/actions/runs?per_page=100" \
  --jq '.workflow_runs[] | {id, path, branch: .head_branch, status, conclusion, created_at}' | jq -s .)

keep=$(jq -c --arg def "$default_branch" --arg build "$build_workflow" '
  map(select(.status == "completed")) as $done
  | ([$done | group_by([.path, .branch])[] | max_by(.created_at) | .id]
     + ($done | map(select(.path == $build and .branch == $def and .conclusion == "success"))
        | if length > 0 then [max_by(.created_at).id] else [] end))
  | unique' <<<"$runs")

echo "Runs: $(jq length <<<"$runs") total, keeping finished $(jq -c . <<<"$keep")"
jq -r --argjson keep "$keep" '
  .[] | select(.status == "completed") | select(.id as $id | $keep | index($id) | not)
  | "\(.id)\t\(.path) \(.branch) \(.conclusion) \(.created_at)"' <<<"$runs" |
while IFS=$'\t' read -r id description; do
  remove "repos/$repo/actions/runs/$id" "run $id ($description)"
done

caches=$(gh api --paginate "repos/$repo/actions/caches?per_page=100" \
  --jq '.actions_caches[] | {id, key, ref, created_at, size: .size_in_bytes}' | jq -s .)

echo "Caches: $(jq length <<<"$caches") entries, $(jq '[.[].size] | add // 0 | . / 1048576 | floor' <<<"$caches") MiB"
jq -r --arg ref "refs/heads/$default_branch" '
  (map(select(.ref != $ref))[]),
  (map(select(.ref == $ref))
   | group_by(.key | sub("-[0-9a-f]{8,}$"; ""))[]
   | sort_by(.created_at) | .[:-1][])
  | "\(.id)\t\(.ref) \(.key) \(.size / 1048576 | floor) MiB"' <<<"$caches" |
while IFS=$'\t' read -r id description; do
  remove "repos/$repo/actions/caches/$id" "cache $id ($description)"
done
