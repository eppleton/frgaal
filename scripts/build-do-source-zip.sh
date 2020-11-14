#!/bin/bash
if [ "x$1" == "x" ] ; then
    TARGET=`dirname $0`/..
else
    TARGET=$1
fi;
zip -r $TARGET/compiler-`cat VERSION`-java-sources.zip openjdk/src/java.compiler openjdk/src/jdk.compiler openjdk/make/langtools/tools/propertiesparser openjdk/make/data/symbols/ openjdk/make/langtools/src/classes/build/tools/symbolgenerator/CreateSymbols.java build*sh scripts tests COMMIT VERSION README LICENSE pom.xml
