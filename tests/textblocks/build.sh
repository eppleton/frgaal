#!/bin/bash -x
COMPILER=`ls ../../dist/compiler/compiler-*.jar | grep -v javadoc | grep -v sources`
TARGET=`dirname $0`/../../build/tests/textblocks
rm -rf $TARGET
mkdir -p $TARGET/main
if java -jar $COMPILER -target 8 -source 14 -d $TARGET/main `find src/main/java -type f` ; then
    echo "Incorrect result!"
    exit 1
fi

if java -jar $COMPILER -target 8 -source 16 -d $TARGET/main `find src/main/java -type f` ; then
    #OK
    echo >/dev/null
else
    echo "Incorrect result!"
    exit 1
fi

$JAVA_HOME8/bin/java -classpath $TARGET/main org.frgaal.tests.textblocks.Test | tee $TARGET/actual-output || exit 1

$JAVA_HOME20/bin/javac -target 16 -source 16 -d $TARGET/main `find src/main/java -type f`
$JAVA_HOME20/bin/java -classpath $TARGET/main org.frgaal.tests.textblocks.Test | tee $TARGET/expected-output || exit 1

diff $TARGET/actual-output $TARGET/expected-output || exit 1

echo "OK."
exit 0
