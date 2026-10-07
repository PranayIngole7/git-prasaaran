# Git-Prasaaran

## Git-Backed Markdown Documentation Publishing Engine

Git-Prasaaran transforms Markdown documents stored in Git repositories into secure, cached, and web-accessible documentation.

## Overview

Git-Prasaaran is a lightweight documentation publishing engine built around a simple principle:

> Git is the source of truth for documentation content.

Documentation authors maintain Markdown files in Git. Git-Prasaaran retrieves the content, processes Markdown and YAML front matter, sanitizes the generated HTML, caches the result in Redis, and exposes the documentation through a REST API and React web interface.

GitHub webhooks allow the system to invalidate cached content when documentation changes.

The project also provides a controlled MCP-based agent extension for AI-assisted documentation workflows.

## Architecture

```text
                 ┌─────────────────────┐
                 │   GitHub Repository │
                 │   Markdown Source   │
                 └──────────┬──────────┘
                            │
                       GitHub Webhook
                            │
                            ▼
┌──────────────┐     ┌──────────────────┐
│    React     │────▶│  Spring Boot API │
│   Frontend   │     │                  │
└──────────────┘     └───────┬──────────┘
                             │
                 ┌───────────┼───────────┐
                 │           │           │
                 ▼           ▼           ▼
            PostgreSQL     Redis      GitHub API
             Metadata      Cache        Source

                         │
                         ▼
                    MCP Extension
                 Controlled AI Tools
```