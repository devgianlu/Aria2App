#!/usr/bin/env bash

set -ev

if [[ ! -z "${TRAVIS_TAG}" ]]; then
    openssl aes-256-cbc -K $encrypted_ec73a101b94f_key -iv $encrypted_ec73a101b94f_iv -in .travis/secrets.tar.enc -out .travis/secrets.tar -d
    tar xvf .travis/secrets.tar -C .travis
fi