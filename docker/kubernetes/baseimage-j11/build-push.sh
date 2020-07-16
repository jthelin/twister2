#!/bin/bash

# build Twister2 base docker image
VERSION="0.7.0"
TWISTER2_BASE_IMAGE="twister2/twister2-k8s-base-j11"

# build docker image
docker build -t $TWISTER2_BASE_IMAGE":"${VERSION} -f Dockerfile .

# check whether image build successful
return_code=$?
if [ $return_code -ne 0 ]; then
  echo "Image build unsuccessful."
  exit $return_code
fi

# push image to Dockerhub
echo "Pusing the image to Docker hub ..."
docker push $TWISTER2_BASE_IMAGE
