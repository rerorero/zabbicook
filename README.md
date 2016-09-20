# zabbicooks

[![Build Status](https://travis-ci.org/rerorero/zabbicooks.svg?branch=master)](https://travis-ci.org/rerorero/zabbicooks)

## Tests
### Requirements
- docker
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
