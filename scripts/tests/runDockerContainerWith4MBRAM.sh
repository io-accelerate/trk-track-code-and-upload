#!/bin/bash

set -e
set -u
set -o pipefail

docker images -f dangling=true -q | xargs -I {} docker rmi {}

PROJECT_ROOT_FOLDER=$(pwd)/../../

mkdir -p localstore

echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
echo "Running record-and-upload app inside a docker container with just 4MB of RAM"
echo "Should start after sometime (usually a long pause), if at all it starts"
echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
docker run -it \
           --rm  \
           --memory=4m \
           --memory-reservation=4m \
           --volume ${PROJECT_ROOT_FOLDER}:/current-folder \
           --workdir /current-folder \
           java:8u111-jdk \
           java -jar ./build/libs/record-and-upload-linux-0.0.20-SNAPSHOT-all.jar --config ./private/aws-test-secrets --store ./build/play

