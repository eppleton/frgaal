#!/bin/bash
if [ "x$1" == "x" ] ; then
    TARGET=`dirname $0`/..
else
    TARGET=$1
fi;
zip -r $TARGET/compiler-`cat VERSION`-java-sources.zip openjdk/src/java.compiler openjdk/src/jdk.compiler openjdk/src/jdk.javadoc openjdk/src/jdk.internal.opt openjdk/src/jdk.internal.md openjdk/make/langtools/tools/propertiesparser build*sh scripts tests COMMIT VERSION README.md LICENSE poms/*
