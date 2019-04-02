#!/bin/bash
set -eo pipefail

# This script encrypts gradle.properties and tests.properties to be used by CircleCI.
# It assumes you have these files stored in ~/.mobile-secrets/android/FluxC

CREDENTIALS_DIRECTORY=~/.mobile-secrets/android/FluxC

# Load the CIRCLECI_FLUXC_ENCRYPTION_KEY environment variable
source "$CREDENTIALS_DIRECTORY/secrets.env"

encrypt() {
    openssl aes-256-cbc -md sha256 -salt -k "$CIRCLECI_FLUXC_ENCRYPTION_KEY" -in "$1"
}

decrypt() {
    openssl enc -d -aes-256-cbc -md sha256 -k $CIRCLECI_FLUXC_ENCRYPTION_KEY -in "$1"
}

# Updates the given encrypted file, if it has changed
update_encrypted_file() {
    INPUT="$1"
    OUTPUT="$2"

    decrypt "$OUTPUT" | cmp -s "$INPUT" - || encrypt "$INPUT" > "$OUTPUT"
}

# Encrypt the updated files
update_encrypted_file "$CREDENTIALS_DIRECTORY/gradle.properties" ".circleci/gradle.properties.enc"
update_encrypted_file "$CREDENTIALS_DIRECTORY/tests.properties" ".circleci/tests.properties.enc"
update_encrypted_file "$CREDENTIALS_DIRECTORY/tests.properties-extra" ".circleci/tests.properties-extra.enc"
