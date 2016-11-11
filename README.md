# zabbicooks
Zabbix configuration management tool.

[![Build Status](https://travis-ci.org/rerorero/zabbicook.svg?branch=master)](https://travis-ci.org/rerorero/zabbicook)

## Requirements
- JRE version: 8
- Zabbix version: 3.0, 3.2

## Getting Started
Go to release page and download a zip package.   
Unarchive to any location, then you can use zabbicook command line tool.  

zabbicook uses configuration files in Hocon format.   
```
# When zabbix is running on localhost:8080 and default Admin password is set.
zabbicook -f zabbicook.conf -i http://localhost:8080/ -u Admin -p zabbix
```

TODO: write usages and hocon descriptions here..

## Testing
We use docker to stub zabbix server, so you need to install docker on the system to run specs.
```
./docker/provision.sh
# Zabbix is now running on a local virtual machine.
# test suites require zabbix stub server
sbt test
```
If you want to run in debug mode:
```
sbt -Dlogback.configurationFile=logback-debug.xml test
```
