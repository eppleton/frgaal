COMPILER=`ls ../../dist/compiler/compiler-*.jar | grep -v javadoc | grep -v sources`
TARGET=`dirname $0`/../../build/tests/sourceversion-api
rm -rf $TARGET
mkdir -p $TARGET/main
java -jar $COMPILER -target 8 -d $TARGET/main `find src/main/java -type f`
mkdir -p $TARGET/test

$JAVA_HOME8/bin/java -jar $COMPILER -target 8 -d $TARGET/test -classpath $TARGET/main -processor org.frgaal.tests.sourceversion.api.AP `find src/test/java -type f` >$TARGET/output8

cat >$TARGET/expected8 <<EOF
\$values[]
getLatestSupported[]
isIdentifier[interface java.lang.CharSequence]
isKeyword[interface java.lang.CharSequence, class javax.lang.model.SourceVersion]
isKeyword[interface java.lang.CharSequence]
isName[interface java.lang.CharSequence, class javax.lang.model.SourceVersion]
isName[interface java.lang.CharSequence]
latestSupported[]
latest[]
valueOf[class java.lang.String]
values[]
EOF

if [ "x`diff $TARGET/expected8 $TARGET/output8 | tee $TARGET/diff`" != "x" ] ; then
    echo "Incorrect output!";
    exit 1
fi

$JAVA_HOME21/bin/java -jar $COMPILER -target 8 -d $TARGET/test -classpath $TARGET/main -processor org.frgaal.tests.sourceversion.api.AP `find src/test/java -type f` >$TARGET/output19

cat >$TARGET/expected19 <<EOF
\$values[]
getLatestSupported[]
isIdentifier[interface java.lang.CharSequence]
isKeyword[interface java.lang.CharSequence, class javax.lang.model.SourceVersion]
isKeyword[interface java.lang.CharSequence]
isName[interface java.lang.CharSequence, class javax.lang.model.SourceVersion]
isName[interface java.lang.CharSequence]
latestSupported[]
latest[]
runtimeVersion[]
valueOf[class java.lang.Runtime\$Version]
valueOf[class java.lang.String]
values[]
EOF

if [ "x`diff $TARGET/expected19 $TARGET/output19 | tee $TARGET/diff`" != "x" ] ; then
    echo "Incorrect output!";
    exit 1
fi

echo "OK."
exit 0
