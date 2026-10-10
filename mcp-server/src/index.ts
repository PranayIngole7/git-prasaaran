import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";

import { config } from "./config.js";
import { DocumentClient } from "./document-client.js";
import { registerListDocumentsTool } from "./tools/list-documents.js";
import { registerGetDocumentTool } from "./tools/get-document.js";
import { registerSearchDocumentsTool } from "./tools/search-documents.js";

const server = new McpServer({
  name: "git-prasaaran",
  version: "1.0.0",
});

const documentClient = new DocumentClient(config.backendBaseUrl);

registerListDocumentsTool(server, documentClient);
registerGetDocumentTool(server, documentClient);
registerSearchDocumentsTool(server, documentClient);

const transport = new StdioServerTransport();

try {
  await server.connect(transport);
  console.error("Git-Prasaaran MCP server started");
} catch (error) {
  console.error(
    "Failed to start Git-Prasaaran MCP server:",
    error instanceof Error ? error.message : "Unknown error",
  );
  process.exitCode = 1;
}
