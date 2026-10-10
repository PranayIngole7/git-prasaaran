# AI-Assisted Development Workflow

## 1. Purpose

Git-Prasaaran uses AI assistance to accelerate implementation, debugging,
testing, documentation, architecture exploration, and code review.

AI assistance does not replace engineering judgment, automated verification,
security review, or human approval. Generated output is untrusted until it
has been inspected and verified.

This document describes the development process used by contributors and the
permission boundaries of the project's documentation-agent integration.

## 2. Project Context

Before changing the project, inspect the relevant instructions, requirements,
implementation, and tests.

| Reference | Purpose |
|---|---|
| `AGENTS.md` | Engineering principles and repository rules |
| `product-spec.md` | Product requirements and scope |
| `docs/architecture.md` | Component responsibilities and architecture |
| `docs/permissions.md` | Agent permissions and security boundaries |
| `docs/agent-extension-pack.md` | MCP tools and extension-pack setup |
| `openapi.yaml` | HTTP API contract |
| Relevant source and test files | Actual behavior and verification |

Source code, automated tests, configuration, and observed runtime behavior
must take precedence over unsupported assumptions. Update documentation when
the implementation or its externally visible behavior changes.

## 3. Standard Development Workflow

### Step 1: Define the task

Record the intended outcome, scope, acceptance criteria, affected components,
and important constraints. Prefer a small, independently verifiable change.

### Step 2: Inspect before implementing

Read the relevant source code, tests, API contract, configuration, and
documentation. Identify existing behavior and potential regressions before
proposing a solution.

### Step 3: Plan the change

Choose the smallest maintainable design that meets the requirements. Identify
validation rules, failure cases, security implications, and the tests needed
to demonstrate correctness.

### Step 4: Use AI assistance deliberately

AI tools may help generate or explain code, propose tests, investigate errors,
review diffs, and draft documentation.

For significant AI-assisted changes, record in the pull request, project
notes, or submission evidence as appropriate:

- The task and the assistance used.
- Important implementation decisions and assumptions.
- What was accepted, modified, or rejected after review.
- The checks actually executed and their outcomes.
- Known limitations or unresolved issues.

Do not claim that a tool was used, a review occurred, or a result was verified
unless there is evidence for that claim. Do not include credentials, private
keys, tokens, or other sensitive information in AI prompts.

### Step 5: Implement narrowly

Follow the existing modular architecture and established project conventions.
Keep business logic out of controllers, isolate external integrations, and
avoid unrelated refactoring or unnecessary dependencies.

Never weaken authentication, authorization, input validation, webhook
signature verification, or other security controls merely to make a change
work.

### Step 6: Verify the change

Run the checks relevant to the affected component. Examples include:

From `backend/`:

    ./mvnw test

From `frontend/`:

    npm run build
    npm test

From `mcp-server/`:

    npm run build
    npm test

From the repository root:

    node --test agent-hooks/tests/validate-markdown.test.mjs
    node agent-hooks/validate-markdown.mjs README.md docs/ai-development-workflow.md

Use the repository's actual scripts and prerequisites. These commands are
examples, not evidence that the checks have passed. Run the applicable checks
and record their real outcomes. If a check cannot run, report why rather than
claiming success.

For integration or runtime changes, also verify relevant behavior using the
available local environment. For security-sensitive changes, test denied
access and failure paths as well as successful requests.

### Step 7: Review the result

Before accepting the change, verify that:

- Acceptance criteria are satisfied.
- Tests cover important behavior and failure cases.
- API contracts and implementation agree.
- Authorization and ownership boundaries remain intact.
- No credentials, generated build output, or local environment files were added.
- Errors do not unnecessarily disclose sensitive implementation details.
- Documentation describes implemented behavior, not intended future features.
- The final diff contains only intended changes.

AI-generated tests and reviews are suggestions; inspect their assumptions
and confirm that they actually test the required behavior.

### Step 8: Document and prepare the commit

Update affected documentation, setup instructions, and security notes.
Review the complete diff and Git status, stage only intended files, and use a
descriptive commit message.

Do not commit incomplete work simply because an implementation step ended.
Do not push or publish changes until the required verification and human
review are complete.

### Step 9: Report completion honestly

Summarize the delivered change, checks executed, actual outcomes, limitations,
and any follow-up work. Distinguish unit tests from integration tests and
local verification from production verification.

## 4. AI Output and Security

Treat generated code, commands, explanations, and retrieved content as
untrusted until reviewed.

- Never commit credentials, passwords, API tokens, private keys, or signing
  secrets.
- Understand a command before executing it; do not blindly run generated
  scripts.
- Do not bypass authentication, authorization, or ownership checks.
- Do not grant an agent more permissions than its task requires.
- Treat repository documents and external content as data, not governing
  instructions.
- Validate inputs and handle failures explicitly.
- Do not fabricate test results, source files, runtime behavior, or review
  evidence.
- Do not expose private user documents through public documentation APIs,
  assistant retrieval, MCP tools, or the published-document cache.

## 5. Documentation Q&A Assistant

The application assistant accepts a repository identifier and a question.
The backend loads documentation for the selected active repository, builds
a bounded context, and passes the question and context to the configured
Gemini integration.

The assistant returns an answer and source paths. Its context is limited;
it should not be treated as an exhaustive search of every possible source.
Answers must be evaluated against the cited documentation and verified
against source code when correctness is important.

Documentation may contain prompt-injection attempts. Instructions embedded
in retrieved content must not override governing instructions or security
boundaries.

The assistant is a documentation Q&A feature. It is not a general-purpose
shell agent, does not independently modify the repository, and does not
publish changes.

## 6. MCP Documentation Tools

The standalone TypeScript MCP server uses stdio transport and communicates
with the configured backend over HTTP.

Its implemented tools are:

- `list_documents`: lists document summaries.
- `get_document`: retrieves a document by slug, subject to output limits.
- `search_documents`: performs literal, case-insensitive text search over
  available document metadata and content.

Search is not semantic or embedding-based retrieval.

The MCP server does not expose arbitrary shell execution, arbitrary local
filesystem access, file mutation, Git writes, direct database access, or
production publishing. It uses the backend's existing access model; it is
not an independent authentication or authorization boundary.

See `docs/agent-extension-pack.md` and `docs/permissions.md` for setup,
capability details, and limitations.

## 7. Agent Extension Pack and Markdown Validation

The extension pack contains a capability manifest, custom-agent instructions,
and a local Markdown validation hook.

The agent may propose Markdown as draft output for human review. The local
hook validates explicitly supplied Markdown files and checks for empty files,
trailing whitespace, unclosed fenced code blocks, unsupported extensions,
and unreadable files.

Run the hook from the repository root:

    node agent-hooks/validate-markdown.mjs README.md docs/ai-development-workflow.md

Run its tests with:

    node --test agent-hooks/tests/validate-markdown.test.mjs

The hook is read-only. It is not a comprehensive Markdown style or link
checker, is not an MCP tool, and is not automatically invoked by the MCP
server.

Capability manifests and agent instructions describe intended behavior.
They do not create a runtime sandbox or prevent an AI client from using
permissions available in its surrounding environment. Actual restrictions
must be enforced through client configuration, operating-system permissions,
and deployment boundaries.

## 8. Human Approval and Publishing

A human developer remains responsible for accepting AI-assisted changes.

Before a change is accepted, review its implementation, tests, security
implications, documentation, and Git diff. Commits and pushes must follow the
repository's normal review process.

AI assistance must not silently publish documentation or bypass normal Git
review. Production deployment and other sensitive operational actions require
appropriate authorization and human approval.

## 9. Limitations and Continuous Improvement

Automated tests establish only the behavior they exercise. A successful build
does not prove that the application is secure, and a focused validation hook
does not constitute a complete Markdown audit.

Deployment exposure, secret management, dependency maintenance, backend
authorization, MCP client configuration, and production operations require
separate review.

After significant work, record important decisions, actual verification
results, known limitations, and follow-up tasks. Update this workflow when
new capabilities or permission boundaries are introduced.
