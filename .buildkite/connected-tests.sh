#!/bin/bash

set -euo pipefail

echo "--- :closed_lock_with_key: Installing Secrets"
cp gradle.properties-example gradle.properties
./gradlew applyConfiguration

echo -e "\n--- :open_file_folder: Merge Properties Files"
./gradlew :example:combineTestsPropertiesWithExtraTestsProperties

echo -e "\n--- :gcloud: Logging into Google Cloud"
gcloud auth activate-service-account --key-file .configure-files/firebase.secrets.json

echo -e "\n--- :hammer_and_wrench: Building Tests"
./gradlew example:assembleDebug example:assembleDebugAndroidTest

echo -e "\n--- :firebase: Run Tests"
mkdir -p build/test-results
gcloud firebase test android run \
	--type "instrumentation" \
	--app "example/build/outputs/apk/debug/example-debug.apk" \
	--test "example/build/outputs/apk/androidTest/debug/example-debug-androidTest.apk" \
	--timeout "30m" \
	--device "model=Pixel3,version=30,locale=en,orientation=portrait" \
	--project "api-project-108380595987" \
	--verbosity info \
	--no-record-video \
	--results-history-name "fluxc-connected-tests" \
	--test-targets "class org.wordpress.android.fluxc.release.ReleaseStack_PluginTestJetpack" \
	|& tee build/test-results/firebase-test-log.txt || EXIT_CODE=$?

echo -e "--- :gcloud: Download Test Results"
TEST_BUCKET=$(cat build/test-results/firebase-test-log.txt | grep -o "gs://test\-lab\-.*/" | head -1)
gsutil cp "$TEST_BUCKET**/test_result*.xml" build/test-results/connected-tests.xml

echo -e "--- :buildkite: Upload Test Results to Test Analytics"
upload_buildkite_test_analytics_junit build/test-results/connected-tests.xml $BUILDKITE_ANALYTICS_TOKEN_CONNECTED_TESTS

echo -e "\n--- Emitting original exit code"
exit ${EXIT_CODE:-0}
