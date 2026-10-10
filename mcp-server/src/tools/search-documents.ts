import { z } from "zod";
import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import type { Document, DocumentClient } from "../document-client.js";

type MatchField = "slug" | "title" | "description" | "content";

function createSnippet(content: string, query: string): string {
  const matchIndex = content.toLocaleLowerCase().indexOf(query);

  if (matchIndex === -1) {
    return content.slice(0, 300).trim();
  }

  let start = Math.max(0, matchIndex - 120);
  let end = Math.min(
    content.length,
    matchIndex + query.length + 300,
  );

  if (start > 0) {
    const boundary = content.slice(start, matchIndex).search(/[\s]/);

    if (boundary !== -1) {
      start += boundary + 1;
    }
  }

  if (end < content.length) {
    const boundary = content.slice(end).search(/[\s]/);

    if (boundary !== -1) {
      end += boundary;
    }
  }

  return [
    start > 0 ? "…" : "",
    content.slice(start, end).trim(),
    end < content.length ? "…" : "",
  ].join("");
}

export function searchDocuments(
  documents: Document[],
  query: string,
  limit = 10,
) {
  const normalizedQuery = query.trim().toLocaleLowerCase();

  if (!normalizedQuery) {
    throw new Error("Search query must not be empty");
  }

  const matches = documents
    .map((document) => {
      const fields: Array<{ field: MatchField; value: string }> = [
        { field: "slug", value: document.slug },
        { field: "title", value: document.title },
        { field: "description", value: document.description },
        { field: "content", value: document.content },
      ];

      const matchedField = fields.find(({ value }) =>
        value.toLocaleLowerCase().includes(normalizedQuery),
      );

      if (!matchedField) {
        return null;
      }

      return {
        slug: document.slug,
        title: document.title,
        description: document.description,
        matchedIn: matchedField.field,
        snippet: createSnippet(
          matchedField.value,
          normalizedQuery,
        ),
      };
    })
    .filter((match) => match !== null);

  return matches.slice(0, limit);
}

export function registerSearchDocumentsTool(
  server: McpServer,
  client: DocumentClient,
): void {
  server.tool(
    "search_documents",
    "Search published documentation by a literal, case-insensitive query across document metadata and Markdown content.",
    {
      query: z.string().trim().min(1).max(200)
        .describe("Text to find in documentation"),
      limit: z.number().int().min(1).max(20).default(10)
        .describe("Maximum number of results (1–20)"),
    },
    async ({ query, limit }) => {
      try {
        const documents = await client.listDocuments();
        const matches = searchDocuments(documents, query, limit);

        return {
          content: [{
            type: "text" as const,
            text: JSON.stringify({
              query,
              count: matches.length,
              results: matches,
            }, null, 2),
          }],
        };
      } catch (error) {
        return {
          isError: true,
          content: [{
            type: "text" as const,
            text: error instanceof Error
              ? error.message
              : "Unable to search documents",
          }],
        };
      }
    },
  );
}
