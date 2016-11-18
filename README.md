[![Build Status](https://travis-ci.org/rerorero/zabbicook.svg?branch=master)](https://travis-ci.org/rerorero/zabbicook)

# Zabbicook
Zabbicook will set up your zabbix server according to the configuration file described in [Hocon] (https://github.com/typesafehub/config/blob/master/HOCON.md) format.  
Some items of the setting have idempotency.

# Requirements
- JRE version: 8
- Zabbix version: 3.0.x, 3.2.x

# Getting Started
Please download the zip of [latest release] (https://github.com/rerorero/zabbicook/releases/latest) and unarchive it on the runtime environment.
```
unzip zabbicook-x.x.x.zip
cd zabbicook-x.x.x

zabbicook
# displays help
```

Let's add a new host group.  
Suppose the zabbix server is running with localhost:8080.  
To run zabbicook you need the user name and password of the zabbix administrator user.
```
cat << EOT > zabbicook.conf
hostGroups: [
  {name: "my group"}
]
EOT

zabbicook -f zabbicook.conf -i http://localhost:8080/ -u Admin -p zabbix
```
After checking the result of `Succeed: total changes = 1`, access `http://localhost:8080/hostgroups.php` in the browser and make sure that `my groups` has been added.

# Setting file
Unfortunately, not all settings of the zabbix server can be set, but only some of the setting items of the setting file to be passed to the `-f (- file)` option.  
You can check items that can be set with the `zabbicook --doc` command in tree.  
You can specify the root path of the tree with `-r` and the depth to display with `-L`.
```
Zabbicook --doc -r templates.items -L 1
```

# Change zabbix user's password
Normally the user's password can be set in `users` in the configuration file, but Administrator users who run zabbicook can not be written in the configuration file.  
You can set it in the browser in advance, or use the default user and password (Admin / zabbix).  
That might bring security problems in a product environment, or you want to use it with ansible etc. you would like to automate this operation as well.  
Zabbicook can change the zabbix user's password on the command line.
```
zabbicook --change-pass -i http://localhost:8080/ -u Admin -p zabbix --new-pass rDLz=a7w
```
