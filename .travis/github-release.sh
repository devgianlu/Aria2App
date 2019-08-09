#!/bin/bash
set -ev

# Download and extract aktau/github-release
curl https://github.com/aktau/github-release/releases/download/v0.7.2/linux-amd64-github-release.tar.bz2 -L --output linux-amd64-github-release.tar.bz2
tar -xvf linux-amd64-github-release.tar.bz2
mv ./bin/linux/amd64/github-release ./github-release-bin
chmod +x github-release-bin

# Split repo slug into owner and name
IFS=/ read REPO_USER REPO_NAME <<< $TRAVIS_REPO_SLUG

echo $REPO_USER

# Create release
./github-release-bin release -s "${GITHUB_OAUTH_TOKEN}" -u "${REPO_USER}" -r "${REPO_NAME}" -t "${TRAVIS_TAG}" -n "${RELEASE_NAME}" -d "${RELEASE_NOTES}" --draft

# Upload APK file
./github-release-bin upload -s "${GITHUB_OAUTH_TOKEN}" -u "${REPO_USER}" -r "${REPO_NAME}" -t "${TRAVIS_TAG}" -n "app-foss-release.apk" -f "${APK_FILE}" -R
