#!/bin/bash
# Start Keycloak, then configure SCIM federation after startup
/opt/keycloak/bin/kc.sh "$@" &
KC_PID=$!

# Wait for Keycloak to be ready on management port
echo "Waiting for Keycloak to start..."
for i in $(seq 1 90); do
  if exec 3<>/dev/tcp/localhost/9000 2>/dev/null; then
    exec 3>&-
    break
  fi
  sleep 2
done
sleep 5

echo "Configuring Keycloak via kcadm..."
/opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:9090 --realm master --user admin --password admin 2>/dev/null

# Disable SSL on master realm
/opt/keycloak/bin/kcadm.sh update realms/master -s sslRequired=NONE 2>/dev/null && \
echo "Master realm SSL disabled" || echo "Warning: Could not disable SSL on master realm"

# Set up SCIM federation if not already present
SCIM_ENDPOINT="${SCIM_ENDPOINT:-http://backend:8080/scim/v2}"
REALM="scim-sample"

EXISTING=$(/opt/keycloak/bin/kcadm.sh get components -r "$REALM" 2>/dev/null | grep -o '"skss-scim2-storage"' || true)

if [ -z "$EXISTING" ]; then
  # Wait for the SCIM backend to be reachable before creating the federation
  # (the suvera extension validates the endpoint during component creation)
  # Use bash TCP check since curl may not be available in the Keycloak image
  SCIM_HOST=$(echo "$SCIM_ENDPOINT" | sed -E 's|https?://([^:/]+).*|\1|')
  SCIM_PORT=$(echo "$SCIM_ENDPOINT" | sed -E 's|https?://[^:]+:([0-9]+).*|\1|')
  SCIM_PORT=${SCIM_PORT:-8080}
  echo "Waiting for SCIM backend at $SCIM_HOST:$SCIM_PORT..."
  for i in $(seq 1 60); do
    if exec 3<>/dev/tcp/$SCIM_HOST/$SCIM_PORT 2>/dev/null; then
      exec 3>&-
      sleep 3
      break
    fi
    sleep 3
  done

  echo "Creating SCIM User Federation pointing to $SCIM_ENDPOINT..."
  /opt/keycloak/bin/kcadm.sh create components -r "$REALM" \
    -s name=scim-provider \
    -s providerId=skss-scim2-storage \
    -s providerType=org.keycloak.storage.UserStorageProvider \
    -s 'config.endPoint=["'"$SCIM_ENDPOINT"'"]' \
    -s 'config.priority=["0"]' \
    -s 'config.enabled=["true"]' \
    -s 'config.cachePolicy=["DEFAULT"]' 2>/dev/null && \
  echo "SCIM federation created" || echo "Warning: Could not create SCIM federation"
else
  echo "SCIM federation already exists"
fi

wait $KC_PID
