[![Build Status](https://travis-ci.org/rerorero/zabbicook.svg?branch=master)](https://travis-ci.org/rerorero/zabbicook)

# zabbicook
zabbicookは[Hocon](https://github.com/typesafehub/config/blob/master/HOCON.md)フォーマットで記述された設定ファイルのとおりに、あなたのzabbixサーバーをセットアップします。  
設定の一部の項目は冪等性があります。

# 要件
- JRE version: 8
- Zabbix version: 3.0.x, 3.2.x

# Getting Started
[最新リリース](https://github.com/rerorero/zabbicook/releases/latest)のzipをダウンロードし、実行環境に展開してください。  
```
unzip zabbicook-x.x.x.zip
cd zabbicook-x.x.x

zabbicook
# helpを表示
```
Windowsユーザーの方は `zabbicook` を `zabbicook.cmd` に置き換えてください。
```
# for windows
zabbicook.cmd
```

ホストグループを追加してみましょう。  
zabbixサーバーが localhost:8080 で起動しているとします。  
zabbicook を実行するにはAdministratorユーザーのユーザー名とパスワードが必要です。
```
cat << EOT > zabbicook.conf
 hostGroups: [
  { name: "my group" }
 ]
EOT

zabbicook -f zabbicook.conf -i http://localhost:8080/ -u Admin -p zabbix
```
`Succeed: Total changes = 1`という結果を確認したら、ブラウザで `http://localhost:8080/hostgroups.php` にアクセスし、`my groups` が追加されていることを確認しましょう。

# 設定ファイル
`-f(--file)` オプションに渡す設定ファイルの設定項目は、今の所残念ながら、zabbixサーバーの全ての設定ができるわけではなく、一部しか対応していません。  
`zabbicook --doc` コマンドで設定できる項目をツリー形式で確認することができます。  
`-r` でツリーのルートパスを、`-L` 表示する深さを指定できます。
```
zabbicook --doc -r templates.items -L 1
```

# zabbixユーザーのパスワード変更
通常ユーザーのパスワードは設定ファイルの `users` で設定できますが、zabbicookを動かすAdministratorユーザーは設定ファイルに記述できません。  
あらかじめブラウザで設定しておくか、初期設定のユーザーとパスワード(Admin/zabbix)を使えば良いのですが、  
商用環境ではセキュリティ的に問題があったり、ansibleなどで利用したい場合この操作も自動化したいでしょう。  
zabbicook はコマンドラインでzabbixユーザーのパスワードを変更することができます。  
```
zabbicook --change-pass -i http://localhost:8080/ -u Admin -p zabbix --new-pass rDLz=a7w
```
