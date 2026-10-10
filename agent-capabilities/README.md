# Agent Capabilities

## Purpose

Define the minimum capabilities granted to the Git-Prasaaran
documentation agent.

## Allowed Capabilities

- Discover published documents through the existing MCP server.
- Retrieve and search published documentation.
- Propose Markdown drafts as text for human review.
- Validate explicitly supplied local Markdown files using the
  read-only validation hook.

## Prohibited Capabilities

The agent is not granted arbitrary shell execution, unrestricted
filesystem access, Git write operations, direct database access,
credential disclosure, or production publishing.

The capability manifest describes the intended policy. It is not
a runtime sandbox or an authorization mechanism.

## Human Review

Review proposed content and validation results before accepting
changes. Use the normal Git workflow for commits and publishing.
