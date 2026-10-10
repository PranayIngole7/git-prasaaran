import { describe, expect, it } from "vitest";
import type { Document } from "../src/document-client.js";
import { searchDocuments } from "../src/tools/search-documents.js";

const sampleDocument: Document = {
  slug: "getting-started",
  title: "Getting Started",
  description: "Project setup guide",
  content: [
    "# Getting Started",
    "",
    "Configure the backend and run tests.",
    "",
    "JWT authentication protects private endpoints.",
  ].join("\n"),
  html: "<h1>Getting Started</h1>",
  sourcePath: "docs/getting-started.md",
};

describe("searchDocuments", () => {
  it("reports when a match is in Markdown content", () => {
    const results = searchDocuments([sampleDocument], "BACKEND");

    expect(results).toHaveLength(1);
    expect(results[0].matchedIn).toBe("content");
    expect(results[0].snippet.toLowerCase()).toContain("backend");
  });

  it("reports when a match is in the title", () => {
    const results = searchDocuments([sampleDocument], "started");

    expect(results).toHaveLength(1);
    expect(results[0].matchedIn).toBe("slug");
  });

  it("reports when a match is in the description", () => {
    const results = searchDocuments([sampleDocument], "setup");

    expect(results).toHaveLength(1);
    expect(results[0].matchedIn).toBe("description");
    expect(results[0].snippet).toContain("Project setup guide");
  });

  it("does not slice content using a metadata match offset", () => {
    const results = searchDocuments([sampleDocument], "getting");

    expect(results).toHaveLength(1);
    expect(results[0].matchedIn).toBe("slug");
    expect(results[0].snippet).toBe("getting-started");
  });

  it("returns a readable content snippet for content matches", () => {
    const results = searchDocuments([sampleDocument], "JWT");

    expect(results).toHaveLength(1);
    expect(results[0].matchedIn).toBe("content");
    expect(results[0].snippet).toContain(
      "JWT authentication protects private endpoints.",
    );
  });

  it("returns no results for unmatched queries", () => {
    expect(
      searchDocuments([sampleDocument], "not-a-real-topic"),
    ).toEqual([]);
  });

  it("rejects empty queries", () => {
    expect(() =>
      searchDocuments([sampleDocument], "   "),
    ).toThrow("Search query must not be empty");
  });

  it("respects the requested result limit", () => {
    const secondDocument = {
      ...sampleDocument,
      slug: "another-guide",
      title: "Another Guide",
    };

    expect(
      searchDocuments(
        [sampleDocument, secondDocument],
        "getting",
        1,
      ),
    ).toHaveLength(1);
  });
});
