#!/bin/bash

pass=my_password
port=8080

docker run \
    -d \
    --name zabbix-db \
    --env="MARIADB_USER=zabbix" \
    --env="MARIADB_PASS=$pass" \
    monitoringartist/zabbix-db-mariadb

echo 'wait 30 sec for db is up...'
sleep 30s

docker run \
    -d \
    --name zabbix \
    -p $port:80 \
    -p 10051:10051 \
    -v /etc/localtime:/etc/localtime:ro \
    --link zabbix-db:zabbix.db \
    --env="ZS_DBHost=zabbix.db" \
    --env="ZS_DBUser=zabbix" \
    --env="ZS_DBPassword=$pass" \
    --env="XXL_zapix=true" \
    --env="XXL_grapher=true" \
    monitoringartist/zabbix-3.0-xxl:latest
