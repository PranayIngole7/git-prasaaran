# Git-Prasaaran — Product Specification

## 1. Product Overview

**Project Name:** Git-Prasaaran

**Tagline:** Git-Backed Markdown Documentation Publishing Engine

**One-line Description:**

Git-Prasaaran transforms Markdown documents stored in Git repositories into secure, cached, and web-accessible documentation.

## 2. Problem Statement

Technical documentation is often stored in Git repositories because Git provides version control, review workflows, history, and collaboration.

However, publishing Git-backed Markdown as a clean documentation website requires additional infrastructure for:

- retrieving Markdown from Git repositories
- parsing document metadata
- rendering Markdown as HTML
- sanitizing rendered HTML
- caching frequently requested documents
- detecting content changes
- invalidating stale cached content
- exposing documentation through a web interface
- providing controlled tooling for AI-assisted documentation workflows

Git-Prasaaran provides a small, focused publishing engine around these requirements.

## 3. Core Product Principle

**Git is the source of truth for documentation content.**

Git-Prasaaran does not store Markdown documents as the primary content database.

The system retrieves Markdown from GitHub when required, processes it, and caches the resulting representation for efficient access.

PostgreSQL stores only application metadata.

Redis is used for caching and must never become the source of truth for documentation content.

### Application Repository Context

A Git-Prasaaran Repository is an application-level configuration boundary
identified by a stable internal repository ID. It references a GitHub
owner, repository name, branch, and content path, and selects the external
repository configuration used to retrieve documentation. The external
GitHub repository remains the content source of truth; PostgreSQL stores
only the application repository metadata.

Repository ownership and multi-user repository authorization are not
implemented yet. Repository deletion is not supported; an administrator
deactivates a repository by setting `active=false`.

## 4. Target Users

### 4.1 Documentation Author

A developer or technical writer who maintains Markdown documentation in a Git repository.

### 4.2 Documentation Reader

A user who wants to browse published documentation through a web interface.

### 4.3 AI-Assisted Documentation Workflow

An authorized AI development tool or agent that needs controlled capabilities such as:

- creating a draft
- linting Markdown
- inspecting documentation

AI tooling must operate within explicitly documented permissions.

## 5. Core User Journeys

### Journey 1 — Browse Documentation

1. User opens the React frontend.
2. Frontend requests the document list from the backend.
3. Backend checks the appropriate data/cache path.
4. Published documentation is returned.
5. Frontend renders the documentation.

### Journey 2 — Retrieve a Document on Cache Miss

1. User requests a document.
2. Backend checks Redis.
3. Redis does not contain the requested document.
4. Backend retrieves the Markdown from GitHub.
5. YAML front matter is parsed.
6. Markdown is converted to HTML.
7. Generated HTML is sanitized.
8. Rendered content is stored in Redis.
9. Backend returns the document to the frontend.

### Journey 3 — Documentation Update

1. Author pushes a documentation change to GitHub.
2. GitHub sends a webhook to Git-Prasaaran.
3. Backend validates the webhook signature.
4. Backend identifies relevant changed documentation.
5. Corresponding Redis cache entries are invalidated.
6. The next request retrieves the updated Markdown from GitHub.
7. The updated document is rendered and cached.

### Journey 4 — AI-Assisted Documentation

An authorized AI tool may use controlled MCP capabilities to:

- create a documentation draft
- lint Markdown
- inspect an existing document

The agent extension must not provide unrestricted shell execution or unrestricted repository access.

Production publishing remains under explicit human-controlled Git workflows.

## 6. Functional Requirements

### FR-01 — Document Listing

The backend shall provide:

```http
GET /api/v1/documents
```