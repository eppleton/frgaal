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

rm -rf build dist

./scripts/build-compiler.sh
./scripts/build-javadoc.sh
./scripts/build-do-source-zip.sh dist

cmark --to html README.md >build/README.html
htmldoc -t pdf --webpage build/README.html >dist/compiler-`cat VERSION`-README.pdf
