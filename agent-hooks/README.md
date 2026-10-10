# Agent Hooks

## Markdown Validation Hook

`validate-markdown.mjs` is a local, read-only validation hook for
explicitly supplied Markdown files.

It checks:

- Empty files.
- Trailing whitespace.
- Unclosed fenced code blocks.
- File extensions and file readability.

### Run

From the repository root:

```bash
node agent-hooks/validate-markdown.mjs README.md docs/agent-extension-pack.md
```

The hook returns exit code 0 when all files pass, 1 when validation
finds issues, and 2 when no input files are supplied.

## Security Boundary

The hook reads only the paths explicitly supplied by the caller.
It does not execute shell commands, modify files, invoke Git, access
credentials, or publish content.

This is a validation utility, not a security sandbox. Only pass it
files that the caller is authorized to inspect.
