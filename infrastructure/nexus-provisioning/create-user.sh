#!/bin/bash
cd /opt/sonatype/nexus

./bin/nexus run &

echo "Waiting for Nexus to be ready..."
until curl -sf http://localhost:8081 > /dev/null; do
    echo "Nexus not ready yet, sleeping 5s..."
    sleep 5
done
echo "Nexus is ready ✅"

PASSWORD="${NEXUS_USER_PASSWORD:-user}"
USER="${NEXUS_USER:-user}"

echo "Создаем скрипт create-user"
curl -s -u admin:admin123 -X POST "http://localhost:8081/service/rest/v1/script" \
    -H "Content-Type: application/json" \
    -d "{
        \"name\": \"create-user\",
        \"type\": \"groovy\",
        \"content\": \"security.addUser('$USER', 'user', 'user', 'user@mail', true, '$PASSWORD', ['nx-admin']);\"
    }"


#echo "Запускаем Groovy скрипт create-admin.groovy"
#curl -s -u admin:${NEXUS_ADMIN_PASSWORD} \
#  -H "Content-Type: text/plain" \
#  -X POST "http://localhost:8081/service/rest/v1/script/create-user/run"

curl --location --request POST 'http://localhost:8081/service/rest/v1/script/create-user/run' \
--header 'Content-Type: text/plain' \
--header 'Authorization: Basic YWRtaW46YWRtaW4xMjM=' \
--data ''

wait
