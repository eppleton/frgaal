#!/bin/bash -x
COMPILER=`ls ../../dist/compiler/compiler-*.jar | grep -v javadoc | grep -v sources`
TARGET=`dirname $0`/../../build/tests/ctsym-removal
rm -rf $TARGET
mkdir -p $TARGET/main
if java -jar $COMPILER -XDrawDiagnostics -target 8 -d $TARGET/main `find src/main/java -type f` >$TARGET/actual-output-1 2>&1 ; then
    #OK
    echo >/dev/null
else
    echo "Incorrect result!"
    exit 1
fi

cat >$TARGET/expected-output-1 <<EOF
Test.java:8:9: compiler.warn.symbol.removed.in.future.version: kindname.class, Pack200, 14
Test.java:9:36: compiler.warn.symbol.removed.in.future.version: kindname.method, checkTopLevelWindow, 11
- compiler.note.deprecated.filename: Test.java
- compiler.note.deprecated.recompile
2 warnings
EOF

diff $TARGET/actual-output-1 $TARGET/expected-output-1 || exit 1


if java -jar $COMPILER -Xlint:deprecation -XDrawDiagnostics -target 8 -d $TARGET/main `find src/main/java -type f` >$TARGET/actual-output-2 2>&1 ; then
    #OK
    echo >/dev/null
else
    echo "Incorrect result!"
    exit 1
fi

cat >$TARGET/expected-output-2 <<EOF
Test.java:8:9: compiler.warn.symbol.removed.in.future.version: kindname.class, Pack200, 14
Test.java:9:15: compiler.warn.symbol.deprecated.in.future.version: kindname.method, getSecurityManager, 17
Test.java:9:36: compiler.warn.has.been.deprecated: checkTopLevelWindow(java.lang.Object), java.lang.SecurityManager
Test.java:9:36: compiler.warn.symbol.removed.in.future.version: kindname.method, checkTopLevelWindow, 11
Test.java:10:29: compiler.warn.symbol.deprecated.in.future.version: kindname.method, getFreePhysicalMemorySize, 14
5 warnings
EOF

diff $TARGET/actual-output-2 $TARGET/expected-output-2 || exit 1


echo "OK."
exit 0
