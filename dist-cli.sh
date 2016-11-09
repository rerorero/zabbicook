#!/bin/sh

VERSION=$1
if [ "$#" -ne 1 ]; then
  echo "No versions specified."
  exit 1
fi

NAME=zabbicook
WORK=./.dist-work
rm -rf $WORK
mkdir -p $WORK/$NAME

sbt clean
sbt compile
sbt assembly

cp target/scala-2.11/*.jar $WORK/$NAME
cp scripts/$NAME $WORK/$NAME
zip -r $WORK/$NAME-$VERSION.zip $WORK/$NAME

echo packaged $WORK/$NAME-$VERSION.zip
