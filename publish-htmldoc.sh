#!/bin/sh

set -eu

echo ----------------
echo publish documents
echo ----------------
sbt "run-main com.github.zabbicook.doc.HtmlDoc"
sbt ghpagesPushSite
