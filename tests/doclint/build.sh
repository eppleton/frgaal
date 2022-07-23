#!/bin/bash -x
COMPILER=`ls ../../dist/compiler/compiler-*.jar | grep -v javadoc | grep -v sources`
TARGET=`dirname $0`/../../build/tests/doclint
rm -rf $TARGET
mkdir -p $TARGET/main
if java -jar $COMPILER -XDrawDiagnostics -Xdoclint:all -target 8 -d $TARGET/main `find src/main/java -type f` >$TARGET/actual-output-1 2>&1 ; then
    echo "Incorrect result!"
    exit 1
else
    #OK
    echo >/dev/null
fi

cat >$TARGET/expected-output-1 <<EOF
Test.java:3:8: compiler.warn.proc.messager: no comment
Test.java:3:8: compiler.warn.proc.messager: use of default constructor, which does not provide a comment
Test.java:4:15: compiler.warn.proc.messager: empty <code> tag
Test.java:4:15: compiler.err.proc.messager: element not closed: code
Test.java:5:24: compiler.warn.proc.messager: no @param for args
1 error
4 warnings
EOF

diff $TARGET/actual-output-1 $TARGET/expected-output-1 || exit 1

echo "OK."
exit 0
