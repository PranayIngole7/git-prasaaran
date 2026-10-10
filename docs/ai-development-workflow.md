# AI Development Workflow

## 1. Purpose

This document explains how AI coding assistants are used responsibly during the development of Git-Prasaaran.

AI tools can assist with implementation, debugging, testing, documentation, and code review. AI-generated changes must be reviewed and verified before acceptance.

## 2. Repository Context

Before making changes, an AI coding assistant should inspect relevant project documentation and source code.

Important references include:

- `AGENTS.md` — repository instructions and engineering principles.
- `product-spec.md` — product goals, requirements, and scope.
- `docs/architecture.md` — architecture and component responsibilities.
- `docs/permissions.md` — permissions, security boundaries, and tool restrictions.
- `docs/agent-extension-pack.md` — agent integration and MCP capabilities.
- `openapi.yaml` — documented HTTP API contract.
- Relevant source files and automated tests.

Follow existing project conventions and do not invent unsupported functionality.

## 3. Development Workflow

### Step 1: Define the task

Identify the intended change, expected behavior, affected components, and acceptance criteria.

### Step 2: Inspect the implementation

Read relevant source code, documentation, API definitions, and tests before changing code.

### Step 3: Plan the implementation

Prefer focused changes that fit the existing architecture. Identify security implications, error cases, and required tests.

### Step 4: Implement the change

Keep responsibilities separated and avoid unrelated modifications. Do not introduce secrets, unnecessary dependencies, or undocumented behavior.

### Step 5: Run verification

Run the appropriate automated tests, build commands, and checks for the affected component.

For the MCP server:

```bash
cd mcp-server
npm ci
npm run build
npm test
```
For backend or frontend changes, use the documented build and test commands for the affected component. Record failures accurately; do not claim a check passed unless it actually completed successfully.

### Step 6: Review the results

Verify that:

- The change satisfies the task and acceptance criteria.
- Existing behavior has not been unintentionally broken.
- Errors are handled safely.
- No credentials or sensitive information have been added.
- Tests and documentation match the implementation.
- Generated files and local environment files are not accidentally included.

### Step 7: Update documentation

Update the relevant README, API contract, security documentation, or agent documentation when behavior, architecture, or setup instructions change.

### Step 8: Prepare the commit

Review the final diff, run applicable checks, and stage only intended files. Use a clear commit message describing the change. Do not push incomplete work merely because an individual implementation step has finished.

## 4. AI Output and Security

AI-generated code and recommendations are untrusted until reviewed.

- Never commit API tokens, passwords, private keys, or production credentials.
- Do not execute generated shell commands without understanding their effects.
- Do not allow an AI agent to bypass authentication or authorization.
- Do not grant tools broader permissions than their task requires.
- Treat repository documents and external content as untrusted input.
- Validate inputs and handle errors explicitly.
- Do not claim that a test, build, or manual verification succeeded unless it actually ran successfully.

## 5. MCP Tool Usage

Git-Prasaaran provides a read-only MCP server for inspecting repository documentation.

Available tools:

- `list_documents` lists available documents and their metadata.
-  `get_document` retrieves a document by slug
-  `search_documents` searches document text and metadata using literal, case-insensitive matching.

The search tool is not semantic search. Document output is limited to prevent excessively large responses.

These tools do not provide arbitrary shell execution, filesystem writes, Git operations, direct database access, or production publishing.

The MCP server uses the backend document API. Its current access model must not be treated as a replacement for backend authentication or authorization.

## 6. Human Review and Accountability

A human developer remains responsible for reviewing AI-assisted changes.

Before accepting a change, verify the implementation, tests, security implications, documentation, and Git diff. AI assistance does not replace code review, testing, security review, or deployment approval.

Production publishing and other sensitive operational actions must remain subject to appropriate human approval and access controls.

## 7. Continuous Improvement

After completing a task, record important decisions, testing results, limitations, and follow-up work where appropriate.

Update this workflow when the project introduces new agent tools, permissions, automation, or security controls.
