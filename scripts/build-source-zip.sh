#!/bin/bash
if [ ! -d openjdk ] ; then
    git clone https://github.com/openjdk/jdk18 openjdk
fi
cd openjdk
git checkout master
git pull origin master
git checkout `cat ../COMMIT`
patch -p1 -i ../patches/frgaal.diff
cp LICENSE ..
cd ..
./scripts/build-do-source-zip.sh

