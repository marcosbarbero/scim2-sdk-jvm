#!/usr/bin/env bash
set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:9090}"
REALM="scim-sample"
SCIM_ENDPOINT="${SCIM_ENDPOINT:-http://backend:8080/scim/v2}"

echo "Waiting for Keycloak to be ready..."
until curl -sf "$KEYCLOAK_URL/health/ready" > /dev/null 2>&1 || curl -sf "$KEYCLOAK_URL/realms/$REALM" > /dev/null 2>&1; do
  sleep 2
done

BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
echo "Waiting for backend SCIM endpoint at $BACKEND_URL..."
until curl -sf "$BACKEND_URL/scim/v2/ServiceProviderConfig" > /dev/null 2>&1; do
  sleep 2
done

echo "Getting admin token..."
TOKEN=$(curl -sf -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -d "grant_type=password&client_id=admin-cli&username=admin&password=admin" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

echo "Getting realm ID..."
REALM_ID=$(curl -sf -H "Authorization: Bearer $TOKEN" \
  "$KEYCLOAK_URL/admin/realms/$REALM" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
echo "Realm ID: $REALM_ID"

echo "Checking if SCIM federation already exists..."
EXISTING=$(curl -sf -H "Authorization: Bearer $TOKEN" \
  "$KEYCLOAK_URL/admin/realms/$REALM/components" \
  | python3 -c "
import sys, json
components = json.load(sys.stdin)
for c in components:
    if c.get('providerId') == 'skss-scim2-storage':
        print(c['id'])
        break
" 2>/dev/null || true)

if [ -n "$EXISTING" ]; then
  echo "SCIM federation already configured (id: $EXISTING). Skipping."
  exit 0
fi

echo "Creating SCIM User Federation provider..."
curl -sf -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  "$KEYCLOAK_URL/admin/realms/$REALM/components" \
  -d '{
    "name": "scim-provider",
    "providerId": "skss-scim2-storage",
    "providerType": "org.keycloak.storage.UserStorageProvider",
    "parentId": "'"$REALM_ID"'",
    "config": {
      "endPoint": ["'"$SCIM_ENDPOINT"'"],
      "priority": ["0"],
      "enabled": ["true"],
      "cachePolicy": ["DEFAULT"]
    }
  }'

echo ""
echo "SCIM User Federation configured successfully!"
echo "  Keycloak: $KEYCLOAK_URL/admin/master/console/#/$REALM/user-federation"
echo "  SCIM endpoint: $SCIM_ENDPOINT"
