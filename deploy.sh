#!/bin/bash
./gradlew build
./gradlew jsBrowserDistribution
./gradlew distZip


cp -r www/* home:"$1"/www

scp  snapshotjar home:/tmp
ssh -exec unzip snapshotjar
scp -r www/* home:/home/ehubbard/amplus/amplus-1.0-SNAPSHOT/lib
