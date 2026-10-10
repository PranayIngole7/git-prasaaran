import { describe, expect, it, vi } from "vitest";
import {
  DocumentClient,
  type Document,
} from "../src/document-client.js";
import { searchDocuments } from "../src/tools/search-documents.js";

const sampleDocument: Document = {
  slug: "getting-started",
  title: "Getting Started",
  description: "Project setup guide",
  content: "# Getting Started\n\nConfigure the backend and run tests.",
  html: "<h1>Getting Started</h1>",
  sourcePath: "docs/getting-started.md",
};

function mockFetch(
  response: Response,
): typeof fetch {
  return vi.fn(async () => response) as unknown as typeof fetch;
}

describe("DocumentClient", () => {
  it("lists valid documents", async () => {
    const client = new DocumentClient(
      "http://localhost:8080",
      mockFetch(
        new Response(JSON.stringify([sampleDocument]), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      ),
    );

    await expect(client.listDocuments()).resolves.toEqual([
      sampleDocument,
    ]);
  });

  it("rejects malformed document lists", async () => {
    const client = new DocumentClient(
      "http://localhost:8080",
      mockFetch(
        new Response(JSON.stringify([{ slug: "incomplete" }]), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      ),
    );

    await expect(client.listDocuments()).rejects.toThrow(
      "Backend returned an invalid document list",
    );
  });

  it("returns null for a missing document", async () => {
    const client = new DocumentClient(
      "http://localhost:8080",
      mockFetch(new Response(null, { status: 404 })),
    );

    await expect(client.getDocument("missing")).resolves.toBeNull();
  });

  it("encodes document slugs in request URLs", async () => {
    const fetchMock = vi.fn(async () =>
      new Response(JSON.stringify(sampleDocument), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    ) as unknown as typeof fetch;

    const client = new DocumentClient(
      "http://localhost:8080",
      fetchMock,
    );

    await client.getDocument("architecture guide");

    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/v1/documents/architecture%20guide",
      expect.objectContaining({ method: "GET" }),
    );
  });

  it("does not expose backend response bodies on server errors", async () => {
    const client = new DocumentClient(
      "http://localhost:8080",
      mockFetch(
        new Response("internal details and secrets", {
          status: 500,
        }),
      ),
    );

    await expect(client.listDocuments()).rejects.toThrow(
      "Backend request failed with HTTP 500",
    );
  });
});

describe("searchDocuments", () => {
  it("finds case-insensitive matches in Markdown content", () => {
    expect(
      searchDocuments([sampleDocument], "BACKEND"),
    ).toHaveLength(1);
  });

  it("returns no results for unmatched queries", () => {
    expect(
      searchDocuments([sampleDocument], "nonexistent-topic"),
    ).toEqual([]);
  });

  it("rejects empty queries", () => {
    expect(() =>
      searchDocuments([sampleDocument], "   "),
    ).toThrow("Search query must not be empty");
  });

  it("respects the requested result limit", () => {
    expect(
      searchDocuments(
        [sampleDocument, { ...sampleDocument, slug: "another-guide" }],
        "getting",
        1,
      ),
    ).toHaveLength(1);
  });
});
