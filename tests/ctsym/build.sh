#!/bin/bash -x
COMPILER=`ls ../../dist/compiler-*.jar | grep -v javadoc | grep -v sources`
TARGET=`dirname $0`/../../build/tests/ctsym
rm -rf $TARGET
mkdir -p $TARGET/main
if java -jar $COMPILER -target 11 -d $TARGET/main `find src/main/java -type f` ; then
    #OK
    echo >/dev/null
else
    echo "Incorrect result!"
    exit 1
fi

if $JAVA_HOME8/bin/java -jar $COMPILER -target 11 -d $TARGET/main `find src/main/java -type f` ; then
    #OK
    echo >/dev/null
else
    echo "Incorrect result!"
    exit 1
fi

if java -jar $COMPILER -target 8 -d $TARGET/main `find src/main/java -type f` ; then
    echo "Incorrect result!"
    exit 1
fi

echo "OK."
exit 0
