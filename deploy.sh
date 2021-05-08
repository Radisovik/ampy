#!/bin/bash
./gradlew build
./gradlew jsBrowserDistribution
./gradlew distZip
gcloud app deploy .
