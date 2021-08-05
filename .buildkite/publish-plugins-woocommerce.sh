#!/bin/bash

set -euo pipefail

# Retrieve data from previous steps
PUBLISHED_FLUXC_ANNOTATIONS_VERSION=$(buildkite-agent meta-data get "PUBLISHED_FLUXC_ANNOTATIONS_VERSION")
PUBLISHED_FLUXC_PROCESSOR_VERSION=$(buildkite-agent meta-data get "PUBLISHED_FLUXC_PROCESSOR_VERSION")
PUBLISHED_FLUXC_VERSION=$(buildkite-agent meta-data get "PUBLISHED_FLUXC_VERSION")

cp gradle.properties-example gradle.properties
./gradlew \
    -PfluxcAnnotationsVersion="$PUBLISHED_FLUXC_ANNOTATIONS_VERSION" \
    -PfluxcProcessorVersion="$PUBLISHED_FLUXC_PROCESSOR_VERSION" \
    -PfluxcVersion="$PUBLISHED_FLUXC_VERSION" \
    :plugins:woocommerce:prepareToPublishToS3 $(prepare_to_publish_to_s3_params) \
    :plugins:woocommerce:publish
