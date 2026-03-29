# Shared Frontend

React 19 + Keycloak-js frontend shared between the Spring Boot and plain Java full-stack samples.

## How it works

The frontend adapts to different backend types via the `VITE_API_MODE` environment variable:

| Mode | Backend | API calls | Content-Type |
|---|---|---|---|
| `rest` (default) | Spring Boot | `/api/users`, `/api/groups` | `application/json` |
| `scim` | Plain Java | `/scim/v2/Users`, `/scim/v2/Groups` | `application/scim+json` |

When `VITE_API_MODE=scim`, the frontend wraps request bodies with the `schemas` array required by the SCIM protocol.

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `VITE_KEYCLOAK_URL` | `http://localhost:9090` | Keycloak base URL |
| `VITE_KEYCLOAK_REALM` | `scim-sample` | Keycloak realm name |
| `VITE_KEYCLOAK_CLIENT_ID` | `scim-sample-app` | Keycloak client ID |
| `VITE_API_MODE` | `rest` | `rest` for Spring Boot, `scim` for plain Java |

## Local Development

```bash
npm install
npm run dev
```

The dev server starts on `http://localhost:5173` and proxies API requests to `http://localhost:8080`.

## Playwright E2E Tests

Browser-based tests that validate the full user flow through the UI.

**Prerequisites:** The full stack must be running (`docker compose up -d` from a sample directory).

```bash
# Install Playwright browsers (first time only)
npx playwright install

# Run tests (headless)
npm run test:e2e

# Run tests (visible browser)
npm run test:e2e:headed

# Run with Playwright UI
npm run test:e2e:ui
```

### Test coverage

| File | Tests |
|---|---|
| `e2e/auth.setup.js` | Keycloak login, session persistence |
| `e2e/users.spec.js` | List, create, search/filter, delete users |
| `e2e/groups.spec.js` | List, create, delete groups |

## Docker

The frontend is built as a Docker image with nginx serving the static build. Both sample docker-compose files reference this directory:

```yaml
frontend:
  build:
    context: ../shared-frontend
    args:
      VITE_API_MODE: rest  # or scim
```
