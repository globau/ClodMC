#!/usr/bin/env bash
set -euo pipefail

# check if a release workflow is already running
STATUS=$(gh run list --workflow=release.yml --limit 1 --json status --jq '.[0].status // ""')
if [[ "$STATUS" == "queued" || "$STATUS" == "in_progress" ]]; then
    RUN_ID=$(gh run list --workflow=release.yml --limit 1 --json databaseId --jq '.[0].databaseId')
    echo "error: release workflow is already running (run $RUN_ID)"
    echo "use 'gh run watch $RUN_ID' to watch it"
    exit 1
fi

echo "triggering release workflow..."
gh workflow run release.yml

echo "waiting for workflow run to appear..."
while true; do
    STATUS=$(gh run list --workflow=release.yml --limit 1 --json status --jq '.[0].status')
    if [[ "$STATUS" == "queued" || "$STATUS" == "in_progress" ]]; then
        RUN_ID=$(gh run list --workflow=release.yml --limit 1 --json databaseId --jq '.[0].databaseId')
        break
    fi
    sleep 1
done

echo "watching run $RUN_ID..."
gh run watch "$RUN_ID" --exit-status
