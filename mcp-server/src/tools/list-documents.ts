import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import type { DocumentClient } from "../document-client.js";

export function registerListDocumentsTool(
  server: McpServer,
  client: DocumentClient,
): void {
  server.tool(
    "list_documents",
    "List published Git-Prasaaran documentation. Returns document slugs, titles, and descriptions.",
    {},
    async () => {
      try {
        const documents = await client.listDocuments();

        const result = documents.map(({ slug, title, description }) => ({
          slug,
          title,
          description,
        }));

        return {
          content: [{ type: "text" as const, text: JSON.stringify(result, null, 2) }],
        };
      } catch (error) {
        return {
          isError: true,
          content: [{
            type: "text" as const,
            text: error instanceof Error
              ? error.message
              : "Unable to list documents",
          }],
        };
      }
    },
  );
}
