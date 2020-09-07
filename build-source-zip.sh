#!/bin/bash
if [ ! -d openjdk ] ; then
    git clone https://github.com/openjdk/jdk14 openjdk
fi
cd openjdk
git checkout master
git pull origin master
git checkout `cat COMMIT`
patch -p1 -i ../patches/frgaal.diff
cp LICENSE ..
cd ..
./build-do-source-zip.sh

