# Git-Prasaaran Frontend

The Git-Prasaaran frontend is a React and TypeScript application for browsing GitHub-backed Markdown documentation and accessing the platform's documentation features.

## Technology Stack

- React
- TypeScript
- Vite
- Tailwind CSS
- React Router
- TanStack Query
- Axios
- Vitest

## Prerequisites

- Node.js 22 or compatible
- npm
- Git-Prasaaran backend running locally

## Setup

From the project root, navigate to the frontend directory:

```bash
cd frontend
npm ci
```

Create a local environment file:

```bash
cp .env.example .env.local
```

Configure the backend API URL in `.env.local`:

```dotenv
VITE_API_BASE_URL=http://localhost:8080
```

Ensure the backend is running and accessible at the configured URL.

## Development

Start the Vite development server:

```bash
npm run dev
```

Open the local URL printed in the terminal, normally `http://localhost:5173`.

## Testing and Validation

Run the frontend tests:

```bash
npm test
```

Run the linter:

```bash
npm run lint
```

Create a production build:

```bash
npm run build
```

Preview the production build locally:

```bash
npm run preview
```

## Configuration

The frontend uses `VITE_API_BASE_URL` to identify the backend API.

Variables prefixed with `VITE_` may be included in the client-side bundle. Never put API secrets, JWT signing secrets, database credentials, or other private credentials in frontend environment variables.

## Current Scope

The frontend integrates with the backend APIs implemented by Git-Prasaaran. Features not supported by the backend should not be represented as functional merely for demonstration purposes.