/opt/keycloak/bin/kc.sh start-dev --import-realm &

echo "Waiting for Keycloak to be ready..."
sleep 30

#!/bin/bash

echo "Creating users for iot-platform realm..."

# Настраиваем аутентификацию
/opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 \
  --realm master \
  --user admin \
  --password admin

# Функция для создания пользователя без required actions
create_user() {
    local username=$1
    local password=$2
    local email=$3
    local roles=$4

    echo "Creating user: $username"

    # Создаем пользователя
    user_id=$(/opt/keycloak/bin/kcadm.sh create users -r iot-platform \
      -s username=$username \
      -s email=$email \
      -s enabled=true \
      -s emailVerified=true \
      --id)

    # Устанавливаем пароль
    /opt/keycloak/bin/kcadm.sh set-password -r iot-platform \
      --userid $user_id \
      --new-password $password \
      --temporary false

    # Очищаем required actions
    /opt/keycloak/bin/kcadm.sh update users/$user_id -r iot-platform \
      -s "requiredActions=[]"

    # Назначаем роли
    IFS=',' read -ra ROLE_ARRAY <<< "$roles"
    for role in "${ROLE_ARRAY[@]}"; do
        /opt/keycloak/bin/kcadm.sh add-roles -r iot-platform \
          --userid $user_id \
          --rolename "$role"
    done

    echo "User $username created successfully"
}

# Создаем пользователей
create_user "user" "user" "user@example.com" "USER"
create_user "admin" "admin" "admin@example.com" "ADMIN,USER"

echo "All users created successfully!"
