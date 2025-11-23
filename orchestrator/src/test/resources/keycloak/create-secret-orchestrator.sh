#!/bin/bash
. ../.env
KEYCLOAK_URL="${ENV_KEYCLOAK_URL}"
ADMIN_USER="${KEYCLOAK_ADMIN}"
ADMIN_PASS="${KEYCLOAK_ADMIN_PASSWORD}"
REALM="${ENV_KEYCLOAK_REALM}"
CLIENT_ID_NAME="${ENV_KEYCLOAK_ORCHESTRATOR_CLIENT}"
CLIENT_SECRET_VALUE="${APP_CLIENT_ORCHESTRATOR_SECRET_KEYCLOAK}"

# 1. Получаем admin token
ADMIN_TOKEN=$(curl -s -X POST \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=$ADMIN_USER&password=$ADMIN_PASS&grant_type=password&client_id=admin-cli" \
  $KEYCLOAK_URL/realms/master/protocol/openid-connect/token \
  | jq -r '.access_token')

echo "ADMIN TOKEN получен"

# 2. Получаем UUID клиента
CLIENT_UUID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM/clients?clientId=$CLIENT_ID_NAME" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" | jq -r '.[0].id')

# Проверка, что клиент найден
if [ -z "$CLIENT_UUID" ] || [ "$CLIENT_UUID" = "null" ]; then
  echo "Клиент $CLIENT_ID_NAME не найден"
  exit 1
fi

# обновляем атрибуты клиента с нужным секретом
 RESPONSE=$(curl -s -w "%{http_code}" -X PUT \
   "$KEYCLOAK_URL/admin/realms/$REALM/clients/$CLIENT_UUID" \
   -H "Authorization: Bearer $ADMIN_TOKEN" \
   -H "Content-Type: application/json" \
   -d "{\"secret\":\"$CLIENT_SECRET_VALUE\"}")

 HTTP_CODE=${RESPONSE: -3}
 if [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 204 ]; then
   echo "Client Secret установлен через обновление клиента"
 else
   echo "Ошибка установки Client Secret: HTTP $HTTP_CODE"
   exit 1
 fi

