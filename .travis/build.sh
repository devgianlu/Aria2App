#!/bin/bash
set -ev

if [[ -z "${TRAVIS_TAG}" ]]; then
    ./gradlew assembleDebug connectedCheck
else
    ./gradlew assembleRelease publish
fi