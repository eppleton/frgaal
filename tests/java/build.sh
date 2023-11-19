COMPILER=`ls ../../dist/compiler/compiler-*.jar | grep -v javadoc | grep -v sources`
TARGET=`dirname $0`/../../build/tests/java
BUILD_CLASSES=$TARGET/classes
BUILD_LIBS=$TARGET/libs
mkdir -p $BUILD_LIBS
(cd $BUILD_LIBS;  if [ ! -e junit-4.13.2.jar ] ; then wget -N https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar; fi; if [ ! -e hamcrest-core-1.3.jar ] ; then wget -N https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar; fi)
rm -rf $BUILD_CLASSES
mkdir -p $BUILD_CLASSES
java -jar $COMPILER -classpath $BUILD_LIBS/junit-4.13.2.jar:$COMPILER -target 8 -d $BUILD_CLASSES `find src -type f`
TESTS=`(cd src; java ListClasses.java)`

if java "-Djava.home.8=$JAVA_HOME8" "-Djava.home.current=$JAVA_HOME20" -classpath $BUILD_LIBS/junit-4.13.2.jar:$BUILD_LIBS/hamcrest-core-1.3.jar:$COMPILER:$BUILD_CLASSES org.junit.runner.JUnitCore $TESTS; then
    echo "OK."
    exit 0
else
    echo "Failure"
    exit 1
fi
