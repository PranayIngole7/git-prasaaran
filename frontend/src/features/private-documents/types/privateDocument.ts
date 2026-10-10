export interface PrivateDocument {
  id: number
  title: string
  slug: string
  /** Stored Markdown source. Do not render this as HTML. */
  content: string
  /** Sanitized HTML produced by the backend. */
  html: string
  createdAt: string
  updatedAt: string
}

export interface PrivateDocumentRequest {
  title: string
  slug: string
  content: string
}
