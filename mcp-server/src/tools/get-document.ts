import { z } from "zod";
import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import type { DocumentClient } from "../document-client.js";

const MAX_CONTENT_LENGTH = 30_000;

export function registerGetDocumentTool(
  server: McpServer,
  client: DocumentClient,
): void {
  server.tool(
    "get_document",
    "Retrieve a published Markdown document by its exact slug. Returns Markdown content, not rendered HTML.",
    {
      slug: z.string().trim().min(1).max(200)
        .describe("Exact document slug"),
    },
    async ({ slug }) => {
      try {
        const document = await client.getDocument(slug);

        if (!document) {
          return {
            isError: true,
            content: [{
              type: "text" as const,
              text: `Document not found: ${slug}`,
            }],
          };
        }

        const truncated = document.content.length > MAX_CONTENT_LENGTH;

        const result = {
          slug: document.slug,
          title: document.title,
          description: document.description,
          content: truncated
            ? document.content.slice(0, MAX_CONTENT_LENGTH)
            : document.content,
          truncated,
        };

        return {
          content: [{
            type: "text" as const,
            text: JSON.stringify(result, null, 2),
          }],
        };
      } catch (error) {
        return {
          isError: true,
          content: [{
            type: "text" as const,
            text: error instanceof Error
              ? error.message
              : "Unable to retrieve document",
          }],
        };
      }
    },
  );
}
