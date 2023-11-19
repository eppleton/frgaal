COMPILER=`ls ../../dist/compiler/compiler-*.jar | grep -v javadoc | grep -v sources`
TARGET=`dirname $0`/../../build/tests/annotations
rm -rf $TARGET
mkdir -p $TARGET/main
java -jar $COMPILER -target 8 -d $TARGET/main `find src/main/java -type f`
mkdir -p $TARGET/test
java -jar $COMPILER -target 8 -d $TARGET/test -classpath $TARGET/main -processor org.frgaal.tests.annotations.annotations.AP `find src/test/java -type f` >$TARGET/output

cat >$TARGET/expected <<EOF
RESULT:12
RESULT:13
RESULT:[add-first]
RESULT:[add-second, add-third]
EOF

if [ "x`diff $TARGET/expected $TARGET/output | tee $TARGET/diff`" != "x" ] ; then
    echo "Incorrect output!";
    exit 1
fi

echo "OK."
exit 0
