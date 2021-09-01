#!/bin/bash

set -euo pipefail

./gradlew \
    :fluxc-annotations:prepareToPublishToS3 $(prepare_to_publish_to_s3_params) \
    :fluxc-annotations:publish

# Add meta-data for the published version so we can use it in subsequent steps
cat ./fluxc-annotations/build/published-version.txt | buildkite-agent meta-data set "PUBLISHED_FLUXC_ANNOTATIONS_VERSION"
