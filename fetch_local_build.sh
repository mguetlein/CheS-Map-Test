#!/bin/bash

SRC="/home/martin/workspace/ches-mapper/deploy/"

echo "copy ches-mapper build from $SRC"

cd lib
#if [ -e ches-mapper.jar ]; then
#  rm ches-mapper.jar
#fi
rsync -v $SRC/ches-mapper.jar .

#if [ -d ches-mapper_lib ]; then
#  rm -rf ches-mapper_lib
#fi
rsync -vr $SRC/ches-mapper_lib .

cd ..

