#!/bin/bash

. ../.env

set -e
KEYCLOAK_URL="${ENV_KEYCLOAK_URL}"
REALM="${ENV_KEYCLOAK_REALM}"
ADMIN_USER="${ENV_KEYCLOAK_ADMIN}"
ADMIN_PASS="${ENV_KEYCLOAK_ADMIN_PASSWORD}"
NEW_USER="${ENV_KEYCLOAK_USER}"
NEW_PASS="${ENV_KEYCLOAK_USER_PASSWORD}"
NEW_EMAIL="${ENV_KEYCLOAK_USER}@example.com"
CLIENT_ID_NAME="${ENV_KEYCLOAK_CLIENT}"
ROLE_NAME="${ENV_KEYCLOAK_USER_ROLE}"

# 1. Получаем admin token
ADMIN_TOKEN=$(curl -s -X POST \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=$ADMIN_USER&password=$ADMIN_PASS&grant_type=password&client_id=admin-cli" \
  $KEYCLOAK_URL/realms/master/protocol/openid-connect/token \
  | jq -r '.access_token')

echo "ADMIN TOKEN получен"

