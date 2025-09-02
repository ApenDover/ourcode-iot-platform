#!/bin/bash

set -e
KEYCLOAK_URL="${ENV_KEYCLOAK_URL}"
REALM="${ENV_KEYCLOAK_REALM}"
ADMIN_USER="${ENV_KEYCLOAK_ADMIN}"
ADMIN_PASS="${ENV_KEYCLOAK_ADMIN_PASSWORD}"
NEW_USER="${ENV_KEYCLOAK_ADMIN}"
NEW_PASS="${ENV_KEYCLOAK_ADMIN_PASSWORD}"
NEW_EMAIL="${ENV_KEYCLOAK_ADMIN}@example.com"
CLIENT_ID_NAME="${ENV_KEYCLOAK_CLIENT}"
ROLE_NAME="${ENV_KEYCLOAK_ADMIN_ROLE}"

# 1. Получаем admin token
ADMIN_TOKEN=$(curl -s -X POST \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=$ADMIN_USER&password=$ADMIN_PASS&grant_type=password&client_id=admin-cli" \
  $KEYCLOAK_URL/realms/master/protocol/openid-connect/token \
  | jq -r '.access_token')

echo "ADMIN TOKEN получен"

# 2. Создаём пользователя (если ещё нет)
EXISTING_USER=$(curl -s -X GET \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$KEYCLOAK_URL/admin/realms/$REALM/users?username=$NEW_USER" \
  | jq -r '.[0].id')

if [ "$EXISTING_USER" = "null" ] || [ -z "$EXISTING_USER" ]; then
  echo "Создаю пользователя $NEW_USER..."
  curl -s -X POST \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
      \"username\": \"$NEW_USER\",
      \"enabled\": true,
      \"email\": \"$NEW_EMAIL\",
      \"firstName\": \"test\",
      \"lastName\": \"test\",
      \"emailVerified\": true,
      \"credentials\": [{
        \"type\": \"password\",
        \"value\": \"$NEW_PASS\",
        \"temporary\": false
      }]
    }" \
    $KEYCLOAK_URL/admin/realms/$REALM/users
  sleep 1
  USER_ID=$(curl -s -X GET \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    "$KEYCLOAK_URL/admin/realms/$REALM/users?username=$NEW_USER" \
    | jq -r '.[0].id')
else
  echo "Пользователь $NEW_USER уже существует"
  USER_ID=$EXISTING_USER
fi

echo "USER_ID=$USER_ID"

# 3. Находим clientId по имени
CLIENT_UUID=$(curl -s -X GET \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$KEYCLOAK_URL/admin/realms/$REALM/clients?clientId=$CLIENT_ID_NAME" \
  | jq -r '.[0].id')

if [ -z "$CLIENT_UUID" ] || [ "$CLIENT_UUID" = "null" ]; then
  echo "❌ Клиент $CLIENT_ID_NAME не найден"
  exit 1
fi

echo "CLIENT_UUID=$CLIENT_UUID"

# 4. Получаем объект роли
ROLE_OBJ=$(curl -s -X GET \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$KEYCLOAK_URL/admin/realms/$REALM/clients/$CLIENT_UUID/roles/$ROLE_NAME")

if [ -z "$ROLE_OBJ" ] || [ "$ROLE_OBJ" = "null" ]; then
  echo "❌ Роль $ROLE_NAME не найдена"
  exit 1
fi

echo "ROLE_OBJ=$ROLE_OBJ"

# 5. Назначаем роль пользователю
curl -s -X POST \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "[$ROLE_OBJ]" \
  "$KEYCLOAK_URL/admin/realms/$REALM/users/$USER_ID/role-mappings/clients/$CLIENT_UUID"

echo "✅ Пользователю $NEW_USER назначена роль $ROLE_NAME"
