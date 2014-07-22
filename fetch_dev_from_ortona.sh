#!/bin/bash

cd lib
if [ -e ches-mapper.jar ]; then
  rm ches-mapper.jar
fi
scp opentox@opentox.informatik.uni-freiburg.de:/opentox/opentox-ruby/www/ches-mapper/bin2/ches-mapper/deploy/ches-mapper.jar .

if [ -d ches-mapper_lib ]; then
  rm -rf ches-mapper_lib
fi
scp -r opentox@opentox.informatik.uni-freiburg.de:/opentox/opentox-ruby/www/ches-mapper/bin2/ches-mapper/deploy/ches-mapper_lib .

cd ..

