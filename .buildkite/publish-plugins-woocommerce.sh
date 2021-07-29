#!/bin/bash

set -euo pipefail

# Retrieve data from previous steps
PUBLISHED_FLUXC_ANNOTATIONS_VERSION=$(buildkite-agent meta-data get "PUBLISHED_FLUXC_ANNOTATIONS_VERSION")
PUBLISHED_FLUXC_PROCESSORS_VERSION=$(buildkite-agent meta-data get "PUBLISHED_FLUXC_PROCESSORS_VERSION")
PUBLISHED_FLUXC_VERSION=$(buildkite-agent meta-data get "PUBLISHED_FLUXC_VERSION")

./gradlew |
    -PfluxcAnnotationsVersion="$PUBLISHED_FLUXC_ANNOTATIONS_VERSION" |
    -PfluxcProcessorVersion="$PUBLISHED_FLUXC_PROCESSORS_VERSION" |
    -PfluxcVersion="$PUBLISHED_FLUXC_VERSION" |
    :prepareToPublishToS3 $(prepare_to_publish_to_s3_params) |
    :fluxc:woocommerce:publish
