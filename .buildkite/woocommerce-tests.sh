#!/bin/bash

set -euo pipefail

echo "--- :closed_lock_with_key: Installing Secrets"
cp gradle.properties-example gradle.properties && cp -a example/properties-example/ example/properties/

# Submitting an HTTP request to the site runs any wp_cron tasks that are scheduled, which helps ensure the test passes
wget $API_TEST_SITE_URL

echo -e "\n--- :gradle: Run Tests"
./gradlew :tests:api:test  || EXIT_CODE=$?

echo -e "\n--- :buildkite: Upload Test Results to Test Analytics"
find tests/api/build/test-results/test -name "TEST-*.xml" -exec upload_buildkite_test_analytics_junit {} $BUILDKITE_ANALYTICS_TOKEN_WOO_TESTS \;

echo -e "\n--- Emitting original exit code"
exit ${EXIT_CODE:-0}
