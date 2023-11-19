#!/bin/bash -x
COMPILER=`ls ../../dist/compiler/compiler-*.jar | grep -v javadoc.jar | grep -v sources`
JAVADOC=`ls ../../dist/javadoc/javadoc-*.jar | grep -v javadoc.jar | grep -v sources`
TARGET=`dirname $0`/../../build/tests/javadoc1
rm -rf $TARGET
mkdir -p $TARGET/main
if java -classpath $COMPILER:$JAVADOC org.frgaal.javadoc.Main -source 16 -d $TARGET/main `find src/main/java -type f` ; then
    #OK
    echo >/dev/null
else
    echo "Incorrect result!"
    exit 1
fi

echo "OK."
exit 0
