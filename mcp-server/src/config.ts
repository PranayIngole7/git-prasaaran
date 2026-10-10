const configuredUrl =
  process.env.BACKEND_BASE_URL ?? "http://localhost:8080";

const parsedUrl = new URL(configuredUrl);

if (!["http:", "https:"].includes(parsedUrl.protocol)) {
  throw new Error("BACKEND_BASE_URL must use HTTP or HTTPS");
}

export const config = {
  backendBaseUrl: parsedUrl.toString().replace(/\/+$/, ""),
  requestTimeoutMs: 10_000,
};
