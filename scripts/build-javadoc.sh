#!/bin/bash
set -e
if [ "x$JAVA_HOME21" == "x" ] ; then
    echo 'Must specify $JAVA_HOME21, pointing to JDK 21 home'
    exit 1;
fi;

if [ "x$JAVA_HOME8" == "x" ] ; then
    echo 'Must specify $JAVA_HOME8, pointing to JDK 8 home'
    exit 1;
fi;

cd `dirname $0`/..

echo "Building frgaal javadoc:"

JAVADOC_SOURCEFILES="`find openjdk/src/jdk.javadoc/share/classes -type f -not -path "*/doclint/*.java" -name "*.java" -not -name "module-info.java" -not -path "openjdk/src/jdk.javadoc/share/classes/jdk/javadoc/internal/tool/JavadocToolProvider.java"`"

rm -rf build/javadoc
mkdir -p build/javadoc/classes

$JAVA_HOME8/bin/java -jar dist/compiler/compiler-`cat VERSION`.jar -d build/javadoc/classes -Xbootclasspath/p:dist/compiler/compiler-`cat VERSION`.jar -classpath dist/compiler/compiler-`cat VERSION`.jar --limit-modules java.base $JAVADOC_SOURCEFILES

mkdir -p build/javadoc/classes/jdk/javadoc/internal/doclets/formats/html/resources/
cp -r openjdk/src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/formats/html/resources/ build/javadoc/classes/jdk/javadoc/internal/doclets/formats/html/
mkdir -p build/javadoc/classes/jdk/javadoc/internal/doclets/toolkit/resources/
cp -r openjdk/src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/toolkit/resources/ build/javadoc/classes/jdk/javadoc/internal/doclets/toolkit/
mkdir -p build/javadoc/classes/jdk/javadoc/internal/tool/resources/
cp -r openjdk/src/jdk.javadoc/share/classes/jdk/javadoc/internal/tool/resources/ build/javadoc/classes/jdk/javadoc/internal/tool/

cat >build/javadoc/manifest.mf <<EOF
EOF

rm -rf dist/javadoc
mkdir -p dist/javadoc

(cd build/javadoc/classes; $JAVA_HOME21/bin/jar -J-Xmx2048m --create -m ../manifest.mf --main-class org.frgaal.javadoc.Main -f ../../../dist/javadoc/javadoc-`cat ../../../VERSION`.jar `find . -type f`)

rm -rf build/javadoc/sources
mkdir -p build/javadoc/sources
cp -r openjdk/src/jdk.javadoc/share/classes/* build/javadoc/sources

(cd build/javadoc/sources; $JAVA_HOME21/bin/jar --create -f ../../../dist/javadoc/javadoc-`cat ../../../VERSION`-sources.jar `find . -type f`)

mkdir -p build/javadoc/javadoc
echo "No javadoc" >build/javadoc/javadoc/README

(cd build/javadoc/javadoc; $JAVA_HOME21/bin/jar --create -f ../../../dist/javadoc/javadoc-`cat ../../../VERSION`-javadoc.jar `find . -type f`)

cp poms/javadoc.xml dist/javadoc/pom.xml

