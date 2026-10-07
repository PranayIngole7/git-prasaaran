/** Mirrors the backend `Document` record and the Document schema in openapi.yaml. */
export interface Document {
  slug: string
  title: string
  description: string | null
  /** Raw Markdown source. Do not render this as HTML. */
  content: string
  /** Sanitized HTML produced by the backend. */
  html: string
  sourcePath: string
}
