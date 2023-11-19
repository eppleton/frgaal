#!/bin/bash -x
COMPILER=`ls ../../dist/compiler/compiler-*.jar | grep -v javadoc | grep -v sources`
TARGET=`dirname $0`/../../build/tests/patterns-instanceof
rm -rf $TARGET
mkdir -p $TARGET/main
if java -jar $COMPILER -target 8 -source 17 -d $TARGET/main `find src/main/java -type f` ; then
    echo "Incorrect result!"
fi

$JAVA_HOME21/bin/javac -target 17 -source 17 -d $TARGET/main `find src/main/java -type f`
$JAVA_HOME21/bin/java -classpath $TARGET/main org.frgaal.tests.patterns.Test | tee $TARGET/expected-output || exit 1

if java -jar $COMPILER -target 8 -source 17 -d $TARGET/main `find src/main/java -type f` ; then
    #OK
    echo >/dev/null
else
    echo "Incorrect result!"
    exit 1
fi

$JAVA_HOME8/bin/java -classpath $TARGET/main org.frgaal.tests.patterns.Test | tee $TARGET/actual-output || exit 1

diff $TARGET/actual-output $TARGET/expected-output || exit 1

if java -jar $COMPILER -target 8 -source 17 -d $TARGET/main `find src/main/java -type f` ; then
    #OK
    echo >/dev/null
else
    echo "Incorrect result!"
    exit 1
fi

$JAVA_HOME8/bin/java -classpath $TARGET/main org.frgaal.tests.patterns.Test | tee $TARGET/actual-output || exit 1

diff $TARGET/actual-output $TARGET/expected-output || exit 1

echo "OK."
exit 0
