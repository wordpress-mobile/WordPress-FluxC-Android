#!/bin/bash

set -euo pipefail

echo "--- :closed_lock_with_key: Installing Secrets"
cp gradle.properties-example gradle.properties && cp -a example/properties-example/ example/properties/

echo "--- :gradle: Run Tests"
./gradlew example:testRelease fluxc:testRelease plugins:woocommerce:testRelease || EXIT_CODE=$?

echo -e "\n--- :buildkite: Upload Test Results to Test Analytics"
find example/build/test-results/testReleaseUnitTest -name "TEST-*.xml" -exec upload_buildkite_test_analytics_junit {} $BUILDKITE_ANALYTICS_TOKEN_UNIT_TESTS \;
find fluxc/build/test-results/testReleaseUnitTest -name "TEST-*.xml" -exec upload_buildkite_test_analytics_junit {} $BUILDKITE_ANALYTICS_TOKEN_UNIT_TESTS \;
find plugins/woocommerce/build/test-results/testReleaseUnitTest -name "TEST-*.xml" -exec upload_buildkite_test_analytics_junit {} $BUILDKITE_ANALYTICS_TOKEN_UNIT_TESTS \;

echo -e "\n--- Emitting original exit code"
exit ${EXIT_CODE:-0}
