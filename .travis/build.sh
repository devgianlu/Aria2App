#!/bin/bash
set -ev

if [[ -z "${TRAVIS_TAG}" ]]; then
    ./gradlew build connectedCheck
else
    ./gradlew assembleRelease publishApk
fi