#!/bin/bash
# Start Keycloak, then disable SSL on master realm via kcadm
/opt/keycloak/bin/kc.sh "$@" &
KC_PID=$!

# Wait for Keycloak to be ready on management port
echo "Waiting for Keycloak to start..."
for i in $(seq 1 60); do
  if exec 3<>/dev/tcp/localhost/9000 2>/dev/null; then
    exec 3>&-
    break
  fi
  sleep 2
done
sleep 3

echo "Disabling SSL requirement on master realm..."
/opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:9090 --realm master --user admin --password admin 2>/dev/null && \
/opt/keycloak/bin/kcadm.sh update realms/master -s sslRequired=NONE 2>/dev/null && \
echo "Master realm SSL disabled" || echo "Warning: Could not disable SSL on master realm"

wait $KC_PID
