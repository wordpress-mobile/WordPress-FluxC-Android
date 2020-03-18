#!/bin/bash

function fetch_tags() {
    git fetch --tags
}

function latest_tag() {
    local TAG=`git describe --tags --abbrev=0 2>/dev/null`

    # Is there a need to handle this gracefully?
    if [[ -z "${TAG// }" ]]; then
        echo "Couldn't figure out the current tag: No tags found"
        exit 0
    fi

    echo $TAG
}

function parse_tag_into_parts() {
    # Split the tag using "." as delimiter. Explanation: https://stackoverflow.com/a/5257398
    PARTS=( ${1//./ } )
    local initial_parts_size=${#PARTS[@]}

    # Then split the last part using "-" as delimeter
    local last_index=$(($initial_parts_size-1))
    local sub_parts=( ${PARTS[$last_index]//-/ } )

    for ((i = 0 ; i < ${#sub_parts[@]} ; i++)); do
        PARTS[$(($last_index+$i))]=${sub_parts[i]}
    done
}

# Concerns/Questions
# Should we make sure we are on develop?
# Is there a risk that the tags we have on the current branch might not reflect the current develop?
# Should we check if there is a need for a new version by comparing the tag and the current HEAD?

echo "Fetching tags"
fetch_tags
TAG=$(latest_tag)
echo "Latest tag found is $TAG"
read -p "Does that look correct (y/n)?`echo $'\n> '`" -n 1 -r

if [[ $REPLY =~ ^[Nn]$ ]]; then
    exit 0
fi

printf "\n\n"

# Verify the tag is in expected format
if [[ ! $TAG =~ ^([0-9]+\.)?([0-9]+\.)?([0-9]+)?(\-beta\-[0-9]+)?$ ]]; then
    echo "Current tag doesn't match the expected format: \$major.\$minor.\$patch-beta-\$beta"
    exit 0
fi

parse_tag_into_parts $TAG

CURRENT_MAJOR="${PARTS[0]}"
CURRENT_MINOR="${PARTS[1]}"
CURRENT_PATCH="${PARTS[2]}"
CURRENT_BETA=${PARTS[4]}

if [[ -z "${PARTS[4]// }" ]]; then
    CURRENT_BETA=0
fi

# Summary of the rules:
# If current beta > 0, next prod minor would stay the same
# If the current beta = 0, next prod minor would be +1
# For new prod version the patch would always be 0

MINOR=$CURRENT_MINOR
if [ "$CURRENT_BETA" -eq "0" ]; then
    MINOR=$(($CURRENT_MINOR+1))
fi

NEW_PROD=$CURRENT_MAJOR.$MINOR.0
NEW_BETA=$CURRENT_MAJOR.$MINOR.$CURRENT_PATCH-beta-$(($CURRENT_BETA+1))

echo current_tag:$TAG
echo new prod would be:$NEW_PROD
echo new beta would be:$NEW_BETA

# If this works reasonably well, it should be fairly easy to:
# 1. Get confirmation from user that the new version looks good
# 2. Create git tag and push it to remote

# Anything else this script needs to do?
