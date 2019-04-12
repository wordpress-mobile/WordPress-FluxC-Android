#!/bin/bash
set -eo pipefail

# Usage: merge_properties.sh FILE1 FILE2
# 
# Given two .properties files, this script outputs a single .properties file.
# Where there are duplicate keys, the values from FILE1 are used.

FILE1="$1"
FILE2="$2"

# This does a combined unique sort of the two files, using '=' as the delimiter
# See https://stackoverflow.com/a/1915750.
sort -u -t= -k1,1 "$FILE1" "$FILE2" |
                      grep -v "#.*" |  # Remove comments
                      grep -v "^$"     # Remove empty lines
