#!/bin/bash

SRC="bin2/ches-mapper/deploy"
#SRC="release/v2.2.2"

echo "copy ches-mapper build from /opentox/opentox-ruby/www/ches-mapper/$SRC"

cd lib
#if [ -e ches-mapper.jar ]; then
#  rm ches-mapper.jar
#fi
rsync -v opentox@opentox.informatik.uni-freiburg.de:/opentox/opentox-ruby/www/ches-mapper/$SRC/ches-mapper.jar .

#if [ -d ches-mapper_lib ]; then
#  rm -rf ches-mapper_lib
#fi
rsync -vr opentox@opentox.informatik.uni-freiburg.de:/opentox/opentox-ruby/www/ches-mapper/$SRC/ches-mapper_lib .

cd ..

