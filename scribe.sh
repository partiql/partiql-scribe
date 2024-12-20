#!/usr/bin/env bash

cli_path=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )

cd "$cli_path"
./gradlew :install
./build/install/scribe/bin/scribe "$@"
