#!/bin/bash

# Получаем admin token
ADMIN_TOKEN=$(curl -s -X POST \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin&password=admin&grant_type=password&client_id=admin-cli" \
  http://localhost:7878/realms/master/protocol/openid-connect/token \
  | jq -r '.access_token')

echo $ADMIN_TOKEN

# Создаем пользователя
curl -X POST \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user",
    "enabled": true,
    "email": "user@example.com",
    "emailVerified": true,
    "credentials": [{
      "type": "password",
      "value": "user",
      "temporary": false
    }]
  }' \
  http://localhost:7878/admin/realms/iot-platform/users

# Назначаем роль
USER_ID=$(curl -s -X GET \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:7878/admin/realms/iot-platform/users?username=user \
  | jq -r '.[0].id')

curl -X POST \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '[{"name":"USER"}]' \
  http://localhost:7878/admin/realms/iot-platform/users/$USER_ID/role-mappings/realm
