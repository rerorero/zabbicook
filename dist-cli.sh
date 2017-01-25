#!/bin/sh

set -eu

VERSION=$(sbt showVersion | grep 'zabbicook-version' | awk '{ print $2;}')

echo ----------------
echo build package $VERSION
echo ----------------
NAME=zabbicook
WORK=./.dist-work
rm -rf $WORK
DEST=$WORK/$NAME-$VERSION
mkdir -p $DEST

sbt clean
sbt compile
sbt assembly

cp target/scala-2.11/*.jar $DEST/
cp scripts/$NAME $DEST
cp scripts/$NAME.cmd $DEST
cd $WORK
zip -r $NAME-$VERSION.zip $NAME-$VERSION

echo packaged $WORK/$NAME-$VERSION.zip

