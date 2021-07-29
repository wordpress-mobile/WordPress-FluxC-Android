#!/bin/bash

set -euo pipefail

# Retrieve data from previous steps
PUBLISHED_FLUXC_ANNOTATIONS_VERSION=$(buildkite-agent meta-data get "PUBLISHED_FLUXC_ANNOTATIONS_VERSION")
PUBLISHED_FLUXC_PROCESSOR_VERSION=$(buildkite-agent meta-data get "PUBLISHED_FLUXC_PROCESSOR_VERSION")

./gradlew |
    -PfluxcAnnotationsVersion="$PUBLISHED_FLUXC_ANNOTATIONS_VERSION" |
    -PfluxcProcessorVersion="$PUBLISHED_FLUXC_PROCESSOR_VERSION" |
    :prepareToPublishToS3 $(prepare_to_publish_to_s3_params) |
    :fluxc:publish

# Add meta-data for the published version so we can use it in subsequent steps
cat ./build/published-version.txt | buildkite-agent meta-data set "PUBLISHED_FLUXC_VERSION"
