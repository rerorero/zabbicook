#!/bin/bash

pass=my_password

docker run \
    -d \
    --name zabbix-db-30 \
    --env="MARIADB_USER=zabbix" \
    --env="MARIADB_PASS=$pass" \
    monitoringartist/zabbix-db-mariadb

docker run \
    -d \
    --name zabbix-db-32 \
    --env="MARIADB_USER=zabbix" \
    --env="MARIADB_PASS=$pass" \
    monitoringartist/zabbix-db-mariadb

echo 'wait 60 sec for db is up...'
sleep 90s

docker run \
    -d \
    --name zabbix30 \
    -p 8080:80 \
    -p 10051:10051 \
    -v /etc/localtime:/etc/localtime:ro \
    --link zabbix-db-30:zabbix.db \
    --env="ZS_DBHost=zabbix.db" \
    --env="ZS_DBUser=zabbix" \
    --env="ZS_DBPassword=$pass" \
    --env="XXL_zapix=true" \
    --env="XXL_grapher=true" \
    monitoringartist/zabbix-xxl:3.0.5

docker run \
    -d \
    --name zabbix32 \
    -p 8081:80 \
    -p 10052:10051 \
    -v /etc/localtime:/etc/localtime:ro \
    --link zabbix-db-32:zabbix.db \
    --env="ZS_DBHost=zabbix.db" \
    --env="ZS_DBUser=zabbix" \
    --env="ZS_DBPassword=$pass" \
    --env="XXL_zapix=true" \
    --env="XXL_grapher=true" \
    monitoringartist/zabbix-xxl:3.2.1

