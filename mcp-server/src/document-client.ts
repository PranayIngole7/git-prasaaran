export interface DocumentSummary {
  slug: string;
  title: string;
  description: string;
}

export interface Document extends DocumentSummary {
  content: string;
  html: string;
  sourcePath: string;
}

type FetchFunction = typeof fetch;

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function isDocument(value: unknown): value is Document {
  return (
    isRecord(value) &&
    typeof value.slug === "string" &&
    typeof value.title === "string" &&
    typeof value.description === "string" &&
    typeof value.content === "string" &&
    typeof value.html === "string" &&
    typeof value.sourcePath === "string"
  );
}

export class DocumentClient {
  constructor(
    private readonly baseUrl: string,
    private readonly fetchFunction: FetchFunction = fetch,
    private readonly timeoutMs = 10_000,
  ) {}

  private async request(path: string): Promise<unknown> {
    const controller = new AbortController();
    const timeout = setTimeout(
      () => controller.abort(),
      this.timeoutMs,
    );

    try {
      const response = await this.fetchFunction(
        `${this.baseUrl}${path}`,
        {
          method: "GET",
          headers: { Accept: "application/json" },
          signal: controller.signal,
        },
      );

      if (response.status === 404) {
        return null;
      }

      if (!response.ok) {
        throw new Error(
          `Backend request failed with HTTP ${response.status}`,
        );
      }

      return await response.json();
    } catch (error) {
      if (
        error instanceof Error &&
        error.name === "AbortError"
      ) {
        throw new Error("Backend request timed out");
      }

      if (error instanceof Error && error.message.startsWith("Backend ")) {
        throw error;
      }

      throw new Error("Unable to communicate with the backend");
    } finally {
      clearTimeout(timeout);
    }
  }

  async listDocuments(): Promise<Document[]> {
    const payload = await this.request("/api/v1/documents");

    if (
      !Array.isArray(payload) ||
      !payload.every(isDocument)
    ) {
      throw new Error("Backend returned an invalid document list");
    }

    return payload;
  }

  async getDocument(slug: string): Promise<Document | null> {
    const payload = await this.request(
      `/api/v1/documents/${encodeURIComponent(slug)}`,
    );

    if (payload === null) {
      return null;
    }

    if (!isDocument(payload)) {
      throw new Error("Backend returned an invalid document");
    }

    return payload;
  }
}
